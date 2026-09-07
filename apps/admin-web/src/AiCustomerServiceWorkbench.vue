<script setup lang="ts">
import { computed, nextTick, onBeforeUnmount, onMounted, ref } from 'vue'
import { API_BASE } from './adminApi'
import {
  AiRequestError,
  MAX_QUESTION_LENGTH,
  aiMoney,
  askCustomerService,
  availabilityLabel,
  createClientRequestId,
  filterSkus,
  flattenSkus,
  preferredSku,
  productStatusLabel,
  requestProducts,
  statusLabel,
  traceStepLabel,
  type CustomerServiceAnswer,
  type LoadState,
  type RateLimitMetadata,
  type SelectedSku,
} from './aiCustomerService'
import { stockState, type StockState } from './catalog'

type ChatMessage = {
  id: string
  role: 'user' | 'assistant'
  content: string
  state?: 'loading' | 'error' | 'complete'
  response?: CustomerServiceAnswer
  error?: string
  rateLimited?: boolean
}

const products = ref<Awaited<ReturnType<typeof requestProducts>>>([])
const catalogState = ref<LoadState>('loading')
const catalogError = ref('')
const query = ref('')
const saleStatus = ref<'all' | 'on-sale'>('all')
const stockFilter = ref<'all' | StockState>('all')
const selectedSkuId = ref<number | null>(null)
const messages = ref<ChatMessage[]>([])
const question = ref('')
const inputError = ref('')
const switchNotice = ref('')
const isSending = ref(false)
const rateLimit = ref<RateLimitMetadata | null>(null)
const cooldownSeconds = ref(0)
let cooldownTimer: ReturnType<typeof setInterval> | null = null
const DEFAULT_COOLDOWN_SECONDS = 1
const failedImages = ref(new Set<string>())
const composer = ref<HTMLTextAreaElement | null>(null)
const messageList = ref<HTMLElement | null>(null)
const shouldFollowMessages = ref(true)

const selections = computed(() => flattenSkus(products.value))
const filteredSelections = computed(() => filterSkus(selections.value, {
  query: query.value,
  saleStatus: saleStatus.value,
  stock: stockFilter.value,
}))
const selected = computed<SelectedSku | null>(() => selections.value.find(({ sku }) => sku.id === selectedSkuId.value) ?? null)
const latestResponse = computed(() => {
  const latestAssistantMessage = [...messages.value].reverse().find((message) => message.role === 'assistant')
  return latestAssistantMessage?.state === 'complete' ? latestAssistantMessage.response ?? null : null
})
const evidenceGroups = computed(() => {
  const groups: Array<{ sourceType: 'PRODUCT' | 'SKU' | 'INVENTORY'; label: string; items: CustomerServiceAnswer['evidence'] }> = [
    { sourceType: 'PRODUCT', label: '商品事实', items: [] },
    { sourceType: 'SKU', label: 'SKU 事实', items: [] },
    { sourceType: 'INVENTORY', label: '库存事实', items: [] },
  ]
  for (const evidence of latestResponse.value?.evidence ?? []) {
    groups.find((group) => group.sourceType === evidence.sourceType)?.items.push(evidence)
  }
  return groups.filter((group) => group.items.length > 0)
})
const currentFacts = computed(() => {
  if (latestResponse.value?.businessFacts) return { facts: latestResponse.value.businessFacts, source: 'answer' as const }
  if (!selected.value) return null
  return {
    source: 'preview' as const,
    facts: {
      productId: selected.value.product.id,
      productCode: selected.value.product.productCode,
      productName: selected.value.product.name,
      productStatus: selected.value.product.status,
      productImagePath: selected.value.sku.imagePath || selected.value.product.coverImagePath,
      skuId: selected.value.sku.id,
      skuCode: selected.value.sku.skuCode,
      color: selected.value.sku.color,
      size: selected.value.sku.size,
      unitPrice: String(selected.value.sku.salePrice),
      currency: selected.value.sku.currency,
      availableStock: selected.value.sku.availableStock,
    },
  }
})
const questionCount = computed(() => question.value.length)
const canSend = computed(() => Boolean(selected.value) && question.value.trim().length > 0 && questionCount.value <= MAX_QUESTION_LENGTH && !isSending.value && cooldownSeconds.value === 0)
const javaPort = computed(() => {
  try {
    const url = new URL(API_BASE)
    return url.port || (url.protocol === 'https:' ? '443' : '80')
  } catch {
    return '未识别'
  }
})

const quickQuestions = [
  '这个商品多少钱？',
  '这个 SKU 是什么颜色和尺码？',
  '现在还有库存吗？',
  '这个商品还能购买吗？',
  '商品的 SKU 编码是什么？',
]

function imageKey(path: string | null | undefined): string {
  return path || 'missing-image'
}

function imageFailed(path: string | null | undefined): boolean {
  return failedImages.value.has(imageKey(path))
}

function markImageFailure(path: string | null | undefined) {
  failedImages.value = new Set([...failedImages.value, imageKey(path)])
}

function answerTone(status: CustomerServiceAnswer['answerStatus']): string {
  if (status === 'UNSUPPORTED_QUESTION' || status === 'INSUFFICIENT_CONTEXT') return 'amber'
  if (status === 'FALLBACK_ANSWER' || status === 'PROVIDER_ERROR') return 'red'
  return 'green'
}

function traceTone(status: string): string {
  if (status === 'FALLBACK') return 'amber'
  if (status === 'ERROR') return 'red'
  return 'green'
}

function providerDescription(response: CustomerServiceAnswer | null): string {
  if (!response) return '等待首次请求'
  if (response.provider.mode === 'MOCK') return '本地确定性 Mock，未调用外部大模型'
  if (response.provider.mode === 'FALLBACK') return 'Python 服务不可用或超时，Java 基于当前商品事实安全降级'
  return '当前响应来自已配置 Provider'
}

function setQuickQuestion(value: string) {
  question.value = value
  inputError.value = ''
  void nextTick(() => composer.value?.focus())
}

function formatResetTime(resetEpochSeconds: number | null): string | null {
  if (!resetEpochSeconds) return null
  return new Date(resetEpochSeconds * 1000).toLocaleTimeString('zh-CN', { hour: '2-digit', minute: '2-digit', second: '2-digit' })
}

function messageListIsNearBottom(): boolean {
  const container = messageList.value
  if (!container) return true
  return container.scrollHeight - container.scrollTop - container.clientHeight <= 24
}

function rememberMessageScrollPosition() {
  shouldFollowMessages.value = messageListIsNearBottom()
}

function scrollMessagesToLatest(force = false) {
  void nextTick(() => {
    const container = messageList.value
    if (!container || (!force && !shouldFollowMessages.value)) return
    container.scrollTop = container.scrollHeight
  })
}

function clearCooldown() {
  if (cooldownTimer) clearInterval(cooldownTimer)
  cooldownTimer = null
  cooldownSeconds.value = 0
}

function startCooldown(seconds: number | null) {
  clearCooldown()
  cooldownSeconds.value = Math.max(1, seconds ?? DEFAULT_COOLDOWN_SECONDS)
  cooldownTimer = setInterval(() => {
    cooldownSeconds.value = Math.max(0, cooldownSeconds.value - 1)
    if (cooldownSeconds.value === 0) clearCooldown()
  }, 1000)
}

function updateRateLimit(metadata: RateLimitMetadata) {
  rateLimit.value = metadata
  if (metadata.mode !== 'redis') clearCooldown()
}

function selectSku(selection: SelectedSku) {
  if (isSending.value || selectedSkuId.value === selection.sku.id) return
  const hadConversation = messages.value.length > 0
  selectedSkuId.value = selection.sku.id
  messages.value = []
  inputError.value = ''
  switchNotice.value = hadConversation ? '已切换 SKU，已清空当前浏览器会话，避免旧回答与新商品混淆。' : ''
}

async function loadCatalog() {
  catalogState.value = 'loading'
  catalogError.value = ''
  products.value = []
  selectedSkuId.value = null
  failedImages.value = new Set()
  try {
    const catalog = await requestProducts()
    products.value = catalog
    if (catalog.length === 0 || flattenSkus(catalog).length === 0) {
      catalogState.value = 'empty'
      return
    }
    selectedSkuId.value = preferredSku(flattenSkus(catalog))?.sku.id ?? null
    catalogState.value = 'ready'
  } catch (error) {
    catalogState.value = 'error'
    catalogError.value = error instanceof Error ? error.message : '商品与 SKU 加载失败'
  }
}

function submitFromKeyboard() {
  void sendQuestion()
}

async function sendQuestion(retryMessageId?: string) {
  if (isSending.value || cooldownSeconds.value > 0) return
  const normalized = question.value.trim()
  if (!normalized) {
    inputError.value = '请输入商品问题后再发送。'
    return
  }
  if (normalized.length > MAX_QUESTION_LENGTH) {
    inputError.value = `问题不能超过 ${MAX_QUESTION_LENGTH} 个字符。`
    return
  }
  if (!selected.value) {
    inputError.value = '请先从真实商品接口返回的列表中选择一个 SKU。'
    return
  }

  inputError.value = ''
  switchNotice.value = ''
  const clientRequestId = createClientRequestId()
  let assistantId: string
  if (retryMessageId) {
    if (!messages.value.some((message) => message.id === retryMessageId)) return
    rememberMessageScrollPosition()
    assistantId = retryMessageId
    messages.value = messages.value.map((message) => message.id === assistantId
      ? { ...message, state: 'loading', error: undefined, rateLimited: false }
      : message)
    scrollMessagesToLatest()
  } else {
    messages.value.push({ id: `user-${clientRequestId}`, role: 'user', content: normalized, state: 'complete' })
    assistantId = `assistant-${clientRequestId}`
    messages.value.push({ id: assistantId, role: 'assistant', content: '', state: 'loading' })
    scrollMessagesToLatest(true)
  }

  isSending.value = true
  try {
    const result = await askCustomerService({
      productId: selected.value.product.id,
      skuId: selected.value.sku.id,
      question: normalized,
      clientRequestId,
    })
    updateRateLimit(result.rateLimit)
    rememberMessageScrollPosition()
    messages.value = messages.value.map((message) => message.id === assistantId
      ? { ...message, response: result.answer, content: result.answer.answer, state: 'complete', error: undefined, rateLimited: false }
      : message)
    question.value = ''
    scrollMessagesToLatest()
  } catch (error) {
    const rateLimited = error instanceof AiRequestError && error.status === 429
    if (rateLimited) {
      updateRateLimit(error.rateLimit)
      startCooldown(error.retryAfterSeconds)
    }
    rememberMessageScrollPosition()
    messages.value = messages.value.map((message) => message.id === assistantId
      ? {
          ...message,
          state: 'error',
          error: rateLimited ? undefined : error instanceof Error ? error.message : 'AI 请求失败，请重试。',
          rateLimited,
        }
      : message)
    scrollMessagesToLatest()
  } finally {
    isSending.value = false
  }
}

onMounted(loadCatalog)
onBeforeUnmount(clearCooldown)
</script>

<template>
  <section class="ai-workbench-page" aria-labelledby="ai-workbench-title">
    <header class="ai-workbench-heading">
      <div>
        <p class="eyebrow">AI 商品客服 / Operator 真实链路</p>
        <h1 id="ai-workbench-title">AI 商品客服</h1>
        <p class="subtitle">基于真实商品、SKU、价格和库存事实生成可追溯回答；管理端请求需要后端 Operator 身份</p>
      </div>
      <p class="ai-heading-boundary">仅展示 Operator API 返回的真实结果；未授权或后端失败时不回填 Mock 数据</p>
    </header>

    <div v-if="catalogState === 'loading'" class="state-panel" data-testid="ai-catalog-loading" role="status" aria-live="polite">
      <span class="state-mark loading-mark" aria-hidden="true"></span>
      <div><strong>正在加载真实商品与 SKU</strong><p>正在请求本地 Java 商品接口，不展示前端伪造商品。</p></div>
    </div>
    <div v-else-if="catalogState === 'error'" class="state-panel error-state" data-testid="ai-catalog-error" role="alert" aria-live="assertive">
      <span class="state-mark" aria-hidden="true">!</span>
      <div><strong>商品与 SKU 加载失败</strong><p>{{ catalogError }}</p></div>
      <button class="secondary-button" type="button" data-testid="ai-catalog-retry" @click="loadCatalog">重新加载</button>
    </div>
    <div v-else-if="catalogState === 'empty'" class="state-panel" data-testid="ai-catalog-empty">
      <span class="state-mark" aria-hidden="true">0</span>
      <div><strong>当前没有可用于客服问答的 SKU</strong><p>接口返回了空列表，因此不会填充示例商品或虚构库存。</p></div>
      <button class="secondary-button" type="button" data-testid="ai-catalog-retry" @click="loadCatalog">重新加载</button>
    </div>

    <div v-else class="ai-workbench-layout" data-testid="ai-three-column-layout">
      <aside class="ai-selector-panel" data-testid="ai-sku-selector-panel" aria-labelledby="ai-selector-title">
        <div class="panel-title-row">
          <div><p class="eyebrow">真实选择</p><h2 id="ai-selector-title">商品与 SKU</h2></div>
          <span class="result-count">{{ filteredSelections.length }} 个</span>
        </div>
        <div class="ai-selector-filters" aria-label="商品与 SKU 筛选">
          <label class="search-field" for="ai-search-input"><span>搜索</span><input id="ai-search-input" v-model="query" type="search" placeholder="商品名称或 SKU 编码" data-testid="ai-search-input"></label>
          <label for="ai-sale-filter"><span>商品状态</span><select id="ai-sale-filter" v-model="saleStatus" data-testid="ai-sale-filter"><option value="all">全部真实状态</option><option value="on-sale">仅在售</option></select></label>
          <label for="ai-stock-filter"><span>库存状态</span><select id="ai-stock-filter" v-model="stockFilter" data-testid="ai-stock-filter"><option value="all">全部库存</option><option value="normal">库存正常</option><option value="low">库存偏低</option><option value="out">暂无库存</option></select></label>
        </div>
        <div v-if="filteredSelections.length" class="ai-sku-list">
          <button
            v-for="selection in filteredSelections"
            :key="selection.sku.id"
            :class="['ai-sku-card', { selected: selectedSkuId === selection.sku.id }]"
            type="button"
            :disabled="isSending"
            :data-testid="`ai-sku-${selection.sku.id}`"
            @click="selectSku(selection)"
          >
            <span v-if="selection.sku.imagePath && !imageFailed(selection.sku.imagePath)" class="ai-sku-image-wrap"><img :src="selection.sku.imagePath" :alt="`${selection.product.name} ${selection.sku.color} ${selection.sku.size}`" @error="markImageFailure(selection.sku.imagePath)"></span>
            <span v-else class="ai-image-placeholder" data-testid="ai-image-placeholder">图片不可用</span>
            <span class="ai-sku-card-copy">
              <strong>{{ selection.product.name }}</strong>
              <small class="mono">{{ selection.sku.skuCode }}</small>
              <span>{{ selection.sku.color }} · {{ selection.sku.size }}</span>
              <b>{{ aiMoney(selection.sku.salePrice, selection.sku.currency) }} <small>{{ selection.sku.currency }}</small></b>
              <span class="ai-card-meta"><em :class="['stock-badge', stockState(selection.sku.availableStock)]">{{ availabilityLabel(selection) }}</em><em :class="['ai-sale-badge', { off: selection.product.status !== 'ON_SALE' }]">{{ productStatusLabel(selection.product.status) }}</em></span>
            </span>
          </button>
        </div>
        <p v-else class="ai-filter-empty" data-testid="ai-filter-empty">没有匹配当前搜索或筛选条件的真实 SKU。</p>
      </aside>

      <section class="ai-chat-panel" data-testid="ai-chat-panel" aria-labelledby="ai-chat-title">
        <div class="panel-title-row">
          <div><p class="eyebrow">单轮问答</p><h2 id="ai-chat-title">AI 客服工作区</h2></div>
          <span v-if="selected" class="ai-selected-chip">SKU {{ selected.sku.id }}</span>
        </div>
        <div class="ai-selected-context" v-if="selected">
          <span>{{ selected.product.name }}</span><strong>{{ selected.sku.color }} · {{ selected.sku.size }}</strong><small class="mono">{{ selected.sku.skuCode }}</small>
        </div>
        <div class="ai-chat-status-stack">
          <div v-if="switchNotice" class="ai-switch-notice" role="status" aria-live="polite">{{ switchNotice }}</div>
          <section v-if="rateLimit" :class="['ai-rate-limit-inline', rateLimit.mode, { 'is-throttled': rateLimit.mode === 'redis' && cooldownSeconds > 0 }]" data-testid="ai-rate-limit-inline" aria-live="polite">
            <template v-if="rateLimit.mode === 'redis' && cooldownSeconds > 0">
              <strong>请求过于频繁</strong>
              <span>本窗口限额 {{ rateLimit.limit }} 次</span>
              <span>剩余 {{ rateLimit.remaining }} 次</span>
              <span data-testid="ai-inline-countdown">Retry-After {{ cooldownSeconds }} 秒</span>
              <span v-if="formatResetTime(rateLimit.resetEpochSeconds)">重置时间 {{ formatResetTime(rateLimit.resetEpochSeconds) }}</span>
            </template>
            <template v-else-if="rateLimit.mode === 'redis'">
              <strong>Redis 限流正常</strong>
              <span>本窗口限额 {{ rateLimit.limit }} 次</span>
              <span>剩余 {{ rateLimit.remaining }} 次</span>
              <span v-if="formatResetTime(rateLimit.resetEpochSeconds)">重置时间 {{ formatResetTime(rateLimit.resetEpochSeconds) }}</span>
            </template>
            <template v-else-if="rateLimit.mode === 'degraded'">
              <strong>限流保护暂时降级</strong>
              <span>本次继续按本地 Showcase 链路处理，未展示未经证实的额度。</span>
            </template>
            <template v-else>
              <strong>限流保护已关闭</strong>
            </template>
          </section>
        </div>
        <div v-if="messages.length === 0" class="ai-welcome" data-testid="ai-welcome">
          <strong>从真实 SKU 事实开始</strong>
          <p>选择左侧商品或 SKU，然后询问价格、规格、库存或是否可购买。</p>
          <div class="quick-question-list" aria-label="快捷问题">
            <button v-for="item in quickQuestions" :key="item" type="button" :disabled="isSending" @click="setQuickQuestion(item)">{{ item }}</button>
          </div>
        </div>
        <div v-else ref="messageList" class="ai-message-list" data-testid="ai-message-list" aria-live="polite" @scroll="rememberMessageScrollPosition">
          <article v-for="message in messages" :key="message.id" :class="['ai-message', message.role, message.state]">
            <p class="ai-message-role">{{ message.role === 'user' ? '用户问题' : 'AI 商品客服' }}</p>
            <template v-if="message.role === 'user'"><p class="ai-message-text">{{ message.content }}</p></template>
            <template v-else-if="message.state === 'loading'"><p class="ai-message-text">正在请求 Java 商品事实与本地 Mock Provider…</p></template>
            <template v-else-if="message.state === 'error'">
              <template v-if="message.rateLimited">
                <p class="ai-rate-limit-error-title" data-testid="ai-rate-limit-error-title">请求过于频繁</p>
                <p class="ai-message-text">当前请求已被 Redis 限流保护拦截，请等待倒计时结束后重试。</p>
                <p v-if="cooldownSeconds > 0" class="ai-boundary-copy" data-testid="ai-rate-limit-countdown">{{ cooldownSeconds }} 秒后可再次发送</p>
              </template>
              <p v-else class="ai-message-text">{{ message.error }}</p>
              <button class="secondary-button" type="button" :disabled="cooldownSeconds > 0 || isSending" @click="sendQuestion(message.id)">{{ cooldownSeconds > 0 ? `${cooldownSeconds} 秒后可发送` : '重试请求' }}</button>
            </template>
            <template v-else-if="message.response">
              <p class="ai-message-text">{{ message.content }}</p>
              <p v-if="message.response.answerStatus === 'UNSUPPORTED_QUESTION'" class="ai-boundary-copy">当前仅支持商品价格、规格、库存、SKU 和可购买状态问题。</p>
              <p v-if="message.response.answerStatus === 'INSUFFICIENT_CONTEXT'" class="ai-boundary-copy">当前事实不足，页面不会补造或猜测答案。</p>
              <p v-if="message.response.fallbackUsed" class="ai-fallback-copy">Python 服务不可用或超时时，回答由 Java 基于当前商品事实安全降级生成。</p>
              <p v-if="message.response.warning" class="ai-warning-copy" role="note">{{ message.response.warning }}</p>
              <div class="ai-answer-meta">
                <span :class="['ai-status-badge', answerTone(message.response.answerStatus)]">{{ statusLabel(message.response.answerStatus) }}</span>
                <span>{{ message.response.provider.name }}</span><span>{{ message.response.provider.mode }}</span><span>fallback {{ message.response.fallbackUsed ? '是' : '否' }}</span><span>{{ message.response.latencyMs }} ms</span><span class="mono ai-trace-id" :title="message.response.traceId">{{ message.response.traceId }}</span>
              </div>
            </template>
          </article>
        </div>
        <form class="ai-composer" @submit.prevent="sendQuestion()">
          <label for="ai-question">商品问题</label>
          <textarea id="ai-question" ref="composer" v-model="question" :maxlength="MAX_QUESTION_LENGTH" :disabled="isSending || !selected" placeholder="例如：这件灰色 L 码 T 恤现在还有库存吗？" data-testid="ai-question-input" @keydown.enter.exact.prevent="submitFromKeyboard"></textarea>
          <div class="ai-composer-footer"><span v-if="inputError" class="ai-input-error" role="alert" aria-live="assertive">{{ inputError }}</span><span v-else>Enter 发送，Shift+Enter 换行</span><span>{{ questionCount }} / {{ MAX_QUESTION_LENGTH }}</span></div>
          <button class="primary-button ai-send-button" type="submit" :disabled="!canSend" data-testid="ai-send-button">{{ isSending ? '正在请求…' : cooldownSeconds > 0 ? `${cooldownSeconds} 秒后可发送` : '发送问题' }}</button>
        </form>
      </section>

      <aside class="ai-facts-panel" data-testid="ai-facts-evidence-panel" aria-labelledby="ai-facts-title">
        <section class="ai-rate-limit-card" data-testid="ai-rate-limit-card" aria-live="polite">
          <p class="eyebrow">请求保护</p>
          <template v-if="rateLimit?.mode === 'redis'">
            <strong>Redis 限流正常</strong>
            <p>本窗口限额 {{ rateLimit.limit }} 次，剩余 {{ rateLimit.remaining }} 次。</p>
            <small v-if="formatResetTime(rateLimit.resetEpochSeconds)">将在 {{ formatResetTime(rateLimit.resetEpochSeconds) }} 重置</small>
          </template>
          <template v-else-if="rateLimit?.mode === 'degraded'">
            <strong>限流保护暂时降级</strong>
            <p>本次请求仍按本地演示链路处理，未显示未经证实的剩余额度。</p>
          </template>
          <template v-else-if="rateLimit?.mode === 'disabled'">
            <strong>限流保护已关闭</strong>
            <p>当前本地配置未启用 Redis 限流。</p>
          </template>
          <template v-else>
            <strong>等待首次 AI 请求</strong>
            <p>首次真实响应后显示当前请求保护状态。</p>
          </template>
        </section>
        <section class="ai-provider-card" data-testid="ai-provider-card">
          <p class="eyebrow">Provider 状态</p>
          <strong>{{ latestResponse?.provider.name ?? '等待首次请求' }}</strong>
          <p>{{ providerDescription(latestResponse) }}</p>
          <dl v-if="latestResponse"><div><dt>模式</dt><dd>{{ latestResponse.provider.mode }}</dd></div><div><dt>外部网络</dt><dd>未使用</dd></div><div><dt>真实 API Key</dt><dd>未配置</dd></div><div><dt>Fallback</dt><dd>{{ latestResponse.fallbackUsed ? '是' : '否' }}</dd></div><div><dt>延迟</dt><dd>{{ latestResponse.latencyMs }} ms</dd></div></dl>
        </section>

        <section class="ai-fact-section" aria-labelledby="ai-facts-title">
          <div class="panel-title-row"><div><p class="eyebrow">业务事实</p><h2 id="ai-facts-title">本次商品事实</h2></div><span class="ai-data-source">{{ currentFacts?.source === 'answer' ? '本次回答依据' : '当前选择预览' }}</span></div>
          <template v-if="currentFacts">
            <div class="ai-fact-product">
              <img v-if="currentFacts.facts.productImagePath && !imageFailed(currentFacts.facts.productImagePath)" :src="currentFacts.facts.productImagePath" :alt="currentFacts.facts.productName" @error="markImageFailure(currentFacts.facts.productImagePath)">
              <span v-else class="ai-image-placeholder">图片不可用</span>
              <div><strong>{{ currentFacts.facts.productName }}</strong><small class="mono">{{ currentFacts.facts.productCode || '-' }}</small></div>
            </div>
            <dl class="ai-facts-grid"><div><dt>商品状态</dt><dd>{{ productStatusLabel(currentFacts.facts.productStatus) }} <small class="mono">{{ currentFacts.facts.productStatus }}</small></dd></div><div><dt>SKU 编码</dt><dd class="mono">{{ currentFacts.facts.skuCode }}</dd></div><div><dt>颜色 / 尺码</dt><dd>{{ currentFacts.facts.color }} / {{ currentFacts.facts.size }}</dd></div><div><dt>售价 / 币种</dt><dd>{{ aiMoney(currentFacts.facts.unitPrice, currentFacts.facts.currency) }} <small class="mono">{{ currentFacts.facts.currency }}</small></dd></div><div><dt>当前库存</dt><dd>{{ currentFacts.facts.availableStock }}</dd></div><div><dt>productId / skuId</dt><dd class="mono">{{ currentFacts.facts.productId }} / {{ currentFacts.facts.skuId }}</dd></div></dl>
          </template>
          <p v-else class="ai-empty-copy">请先选择来自真实商品接口的 SKU。</p>
        </section>

        <section class="ai-evidence-section" aria-labelledby="ai-evidence-title">
          <div class="panel-title-row"><div><p class="eyebrow">Java 事实证据</p><h2 id="ai-evidence-title">事实证据</h2></div><span v-if="latestResponse" class="result-count">{{ latestResponse.evidence.length }} 条</span></div>
          <div v-if="evidenceGroups.length" class="ai-evidence-groups">
            <div v-for="group in evidenceGroups" :key="group.sourceType" class="ai-evidence-group">
              <strong>{{ group.label }} <small class="mono">{{ group.sourceType }}</small></strong>
              <dl><div v-for="item in group.items" :key="`${item.sourceType}-${item.field}`"><dt :title="`${item.displayName} (${item.field})`">{{ item.displayName }} <small class="mono">{{ item.field }}</small></dt><dd :title="`${item.value} (${item.sourceType} / ${item.sourceId})`">{{ item.value }}<small class="mono">{{ item.sourceType }} / {{ item.sourceId }}</small></dd></div></dl>
            </div>
          </div>
          <p v-else class="ai-empty-copy" data-testid="ai-evidence-empty">发送问题后仅展示 Java 返回的实际 Evidence。</p>
        </section>

        <section class="ai-trace-section" aria-labelledby="ai-trace-title">
          <div class="panel-title-row"><div><p class="eyebrow">调用 Trace</p><h2 id="ai-trace-title">调用追踪</h2></div><span v-if="latestResponse" class="result-count">{{ latestResponse.trace.length }} 步</span></div>
          <ol v-if="latestResponse?.trace.length" class="ai-trace-list">
            <li v-for="step in latestResponse.trace" :key="step.step" :class="traceTone(step.status)" :data-testid="`ai-trace-${step.step}`"><span class="ai-trace-dot"></span><div><strong>{{ traceStepLabel(step.step, step.displayName) }}</strong><small>{{ step.status }} · {{ step.durationMs }} ms</small><p :title="step.detail">{{ step.detail }}</p></div></li>
          </ol>
          <p v-else class="ai-empty-copy" data-testid="ai-trace-empty">等待真实请求后展示后端返回的 Trace 步骤。</p>
        </section>

        <details class="ai-developer-notes">
          <summary>开发演示信息（只读）</summary>
          <dl><div><dt>外部 Java 接口</dt><dd class="mono">POST {{ API_BASE }}/v1/operator/ai/customer-service/ask</dd></div><div><dt>Java → Python 内部接口</dt><dd class="mono">POST /internal/ai/customer-service/answer</dd></div><div><dt>身份边界</dt><dd>OperatorScope（服务端推导）</dd></div><div><dt>当前 productId / skuId</dt><dd>{{ selected?.product.id ?? '-' }} / {{ selected?.sku.id ?? '-' }}</dd></div><div><dt>最近 clientRequestId</dt><dd class="mono">{{ latestResponse?.clientRequestId ?? '-' }}</dd></div><div><dt>最近 traceId</dt><dd class="mono">{{ latestResponse?.traceId ?? '-' }}</dd></div><div><dt>Provider Mode</dt><dd>{{ latestResponse?.provider.mode ?? '-' }}</dd></div><div><dt>当前 Java 端口</dt><dd>{{ javaPort }}</dd></div><div><dt>当前 Python 端口</dt><dd>8000（本地 P4B 默认配置）</dd></div></dl>
        </details>
      </aside>
    </div>
  </section>
</template>
