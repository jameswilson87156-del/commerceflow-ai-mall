<script setup lang="ts">
import { computed, nextTick, onMounted, onUnmounted, ref } from 'vue'
import { askCustomerService } from '../../api/ai'
import type { CustomerServiceAnswer } from '../../api/ai'
import { getProduct } from '../../api/catalog'
import { ApiError, resolveImageUrl } from '../../api/runtime'
import type { Product, RateLimitMeta, Sku } from '../../api/types'
import { DEMO_USER_ID } from '../../config/runtime'
import MobileHeader from '../../components/MobileHeader.vue'
import MobileNotice from '../../components/MobileNotice.vue'
import ProductImage from '../../components/ProductImage.vue'
import {
  MAX_QUESTION_LENGTH,
  cooldownSeconds,
  clearQuestionAfterSuccess,
  createAskPayload,
  createClientRequestId,
  hasRedisQuota,
  isFailOpen,
  nextCooldown,
  pendingQuestionForAttempt,
  retryQuestion,
  traceLabel
} from '../../ui/mobile-ai-customer-service.mjs'

type PageState = 'loading' | 'ready' | 'error'
type ChatTurn = { id: string; question: string; answer: CustomerServiceAnswer }

const product = ref<Product | null>(null)
const selectedSku = ref<Sku | null>(null)
const state = ref<PageState>('loading')
const errorMessage = ref('')
const question = ref('')
const pendingQuestion = ref('')
const sending = ref(false)
const turns = ref<ChatTurn[]>([])
const latestAnswer = ref<CustomerServiceAnswer | null>(null)
const rateLimit = ref<RateLimitMeta>({})
const cooldownRemaining = ref(0)
const requestFailure = ref<{ question: string; message: string } | null>(null)
const rateLimitFailure = ref<{ question: string; message: string } | null>(null)
const factsExpanded = ref(false)
const evidenceExpanded = ref(false)
const traceExpanded = ref(false)
const demoExpanded = ref(false)
const productId = ref(0)
const skuId = ref(0)
let cooldownTimer: ReturnType<typeof setInterval> | undefined

const quickQuestions = [
  '这款商品现在多少钱？',
  '有哪些颜色和尺码？',
  '当前库存还有多少？',
  '现在可以购买吗？',
  'SKU 编码是什么？'
]

const canSend = computed(() => state.value === 'ready' && Boolean(selectedSku.value) && !sending.value && cooldownRemaining.value === 0)
const productImage = computed(() => resolveImageUrl(selectedSku.value?.imagePath || product.value?.coverImagePath))
const providerLabel = computed(() => {
  if (!latestAnswer.value) return ''
  return `${latestAnswer.value.provider.name} / ${latestAnswer.value.provider.mode}`
})
const isDegraded = computed(() => isFailOpen(rateLimit.value))
const quotaAvailable = computed(() => hasRedisQuota(rateLimit.value))
const selectedStatus = computed(() => selectedSku.value?.availableStock === 0 ? '当前缺货' : product.value?.status === 'ON_SALE' ? '在售' : '未在售')
const questionCount = computed(() => question.value.length)

function randomUuid() {
  const cryptoApi = globalThis.crypto
  return typeof cryptoApi?.randomUUID === 'function' ? cryptoApi.randomUUID() : ''
}

function loadSelection() {
  state.value = 'loading'
  errorMessage.value = ''
  getProduct(productId.value).then(response => {
    const sku = response.data.skus.find(item => item.id === skuId.value) || null
    if (!sku) {
      throw new Error('所选 SKU 已不存在或不属于当前商品')
    }
    product.value = response.data
    selectedSku.value = sku
    state.value = 'ready'
  }).catch(error => {
    state.value = 'error'
    errorMessage.value = error instanceof ApiError ? error.message : error instanceof Error ? error.message : '商品信息加载失败，请重试。'
  })
}

function clearCooldown() {
  if (cooldownTimer) clearInterval(cooldownTimer)
  cooldownTimer = undefined
  cooldownRemaining.value = 0
}

function startCooldown(seconds: number) {
  if (cooldownTimer) clearInterval(cooldownTimer)
  cooldownRemaining.value = Math.max(0, seconds)
  cooldownTimer = setInterval(() => {
    cooldownRemaining.value = nextCooldown(cooldownRemaining.value)
    if (cooldownRemaining.value === 0) clearCooldown()
  }, 1000)
}

function updateRateLimit(meta: RateLimitMeta) {
  rateLimit.value = meta
}

function scrollChatToLatest() {
  nextTick(() => {
    if (typeof document === 'undefined') return
    const panel = document.querySelector('.chat-panel')
    if (panel) panel.scrollTop = panel.scrollHeight
  })
}

function ask(rawQuestion: string) {
  const trimmed = pendingQuestionForAttempt(rawQuestion)
  if (!trimmed || !canSend.value || !selectedSku.value) return
  pendingQuestion.value = trimmed
  question.value = trimmed
  sending.value = true
  requestFailure.value = null
  rateLimitFailure.value = null
  const payload = createAskPayload({
    userId: DEMO_USER_ID,
    productId: productId.value,
    skuId: selectedSku.value.id,
    question: trimmed,
    clientRequestId: createClientRequestId(randomUuid)
  })
  askCustomerService(payload).then(result => {
    updateRateLimit(result.rateLimit)
    latestAnswer.value = result.answer
    turns.value.push({ id: result.answer.traceId, question: trimmed, answer: result.answer })
    const cleared = clearQuestionAfterSuccess()
    question.value = cleared.question
    pendingQuestion.value = cleared.pendingQuestion
    scrollChatToLatest()
  }).catch(error => {
    if (error instanceof ApiError && error.statusCode === 429) {
      updateRateLimit(error.rateLimit)
      latestAnswer.value = null
      const seconds = cooldownSeconds(error.rateLimit, error.body || {})
      startCooldown(seconds)
      rateLimitFailure.value = { question: pendingQuestion.value, message: '当前请求已被 Redis 限流保护拦截，请等待倒计时结束后重试。' }
      scrollChatToLatest()
      return
    }
    requestFailure.value = { question: pendingQuestion.value, message: error instanceof ApiError ? error.message : '本地服务暂时不可用，请稍后重试。' }
    scrollChatToLatest()
  }).finally(() => { sending.value = false })
}

function sendQuestion() { ask(question.value) }
function sendQuick(questionText: string) {
  if (!canSend.value) return
  question.value = questionText
  ask(questionText)
}
function retryFailed() {
  if (!requestFailure.value || !canSend.value) return
  ask(retryQuestion(pendingQuestion.value))
}
function retryRateLimited() {
  if (!rateLimitFailure.value || !canSend.value) return
  ask(retryQuestion(pendingQuestion.value))
}
function goBack() { uni.navigateBack() }
function retryLoad() { loadSelection() }
function resetForSkuChange() {
  turns.value = []
  latestAnswer.value = null
  question.value = ''
  pendingQuestion.value = ''
  requestFailure.value = null
  rateLimitFailure.value = null
  clearCooldown()
}
function statusCopy(status: string) {
  const labels: Record<string, string> = {
    ANSWERED: '已回答',
    UNSUPPORTED_QUESTION: '当前不支持',
    INSUFFICIENT_CONTEXT: '信息不足',
    FALLBACK_ANSWER: 'Java 事实降级回答'
  }
  return labels[status] || status
}
function resetTime() {
  if (!quotaAvailable.value || !rateLimit.value.reset) return ''
  return new Date(rateLimit.value.reset * 1000).toLocaleTimeString('zh-CN', { hour: '2-digit', minute: '2-digit', second: '2-digit' })
}

onMounted(async () => {
  const { queryParam } = await import('../../platform/h5')
  productId.value = Number(queryParam('productId'))
  skuId.value = Number(queryParam('skuId'))
  resetForSkuChange()
  if (!productId.value || !skuId.value) {
    state.value = 'error'
    errorMessage.value = '缺少商品或 SKU 标识，无法加载真实商品事实。'
    return
  }
  loadSelection()
})
onUnmounted(clearCooldown)
</script>

<template>
  <view class="mobile-page mobile-page--with-bottom-action ai-customer-service-page">
    <MobileHeader title="AI 商品客服" eyebrow="COMMERCEFLOW AI MALL" subtitle="本地确定性 Mock，未调用外部大模型" :back="true" @back="goBack" />
    <view v-if="state === 'loading'" class="panel empty-box">正在读取当前商品与 SKU 事实...</view>
    <view v-else-if="state === 'error'" class="error-box"><text>{{ errorMessage }}</text><button class="secondary-button retry-button" @click="retryLoad">重新加载</button></view>
    <template v-else-if="product && selectedSku">
      <view class="ai-context panel">
        <ProductImage class="ai-context-image" :src="productImage" :alt="product.name" />
        <view class="ai-context-copy">
          <text class="context-category">{{ product.categoryName }}</text>
          <text class="context-title">{{ product.name }}</text>
          <text class="context-spec">{{ selectedSku.color }} / {{ selectedSku.size }} · {{ selectedSku.skuCode }}</text>
          <view class="context-bottom"><text class="context-price">¥{{ selectedSku.salePrice }}</text><text class="context-currency">{{ selectedSku.currency }}</text><text class="tag" :class="selectedSku.availableStock === 0 ? 'tag-danger' : 'tag-success'">{{ selectedStatus }} · 库存 {{ selectedSku.availableStock }}</text></view>
        </view>
      </view>

      <MobileNotice v-if="isDegraded" tone="warning" title="限流保护暂时降级" message="Redis 当前不可用，本次请求仍由本地 Java 与 Mock 链路完成；页面不会展示伪造的配额数据。" />
      <view v-if="quotaAvailable" class="quota-bar panel">
        <view class="quota-heading"><text>{{ cooldownRemaining > 0 ? '已触发限流' : 'Redis 限流正常' }}</text><text>本窗口限额 {{ rateLimit.limit }} 次</text></view>
        <view class="quota-values"><text>剩余 <text class="quota-number">{{ rateLimit.remaining }}</text> 次</text><text>重置 {{ resetTime() }}</text><text v-if="cooldownRemaining > 0">Retry-After {{ cooldownRemaining }} 秒</text></view>
      </view>

      <view v-if="turns.length === 0 && !rateLimitFailure" class="quick-panel panel">
        <text class="quick-title">基于当前 SKU 的真实商品问答</text>
        <view class="quick-list"><button v-for="item in quickQuestions" :key="item" class="quick-button" :disabled="!canSend" @click="sendQuick(item)">{{ item }}</button></view>
      </view>

      <view class="chat-panel panel">
        <text class="chat-welcome">你好，我只根据 Java 查询到的当前商品、SKU 与库存事实回答价格、规格、库存、可购买性和 SKU 编码。</text>
        <view v-for="turn in turns" :key="turn.id" class="chat-turn">
          <view class="chat-bubble chat-bubble--user"><text>{{ turn.question }}</text></view>
          <view class="chat-bubble chat-bubble--assistant"><text class="answer-status">{{ statusCopy(turn.answer.answerStatus) }}</text><text>{{ turn.answer.answer }}</text><text class="provider-meta">{{ turn.answer.provider.name }} / {{ turn.answer.provider.mode }} · {{ turn.answer.latencyMs }} ms</text></view>
        </view>
        <view v-if="sending" class="chat-bubble chat-bubble--assistant"><text>正在查询本地商品事实并调用本地 Mock...</text></view>
        <view v-if="requestFailure" class="chat-error"><text>“{{ requestFailure.question }}” 未获得回答：{{ requestFailure.message }}</text><button class="secondary-button" :disabled="!canSend" @click="retryFailed">重试请求</button></view>
        <view v-if="rateLimitFailure" class="chat-error chat-error--rate"><text class="rate-title">请求过于频繁</text><text>{{ rateLimitFailure.message }}</text><text class="pending-question">待重试问题：{{ pendingQuestion }}</text><text>{{ cooldownRemaining }} 秒后可再次发送</text><button class="secondary-button" :disabled="!canSend" @click="retryRateLimited">{{ cooldownRemaining > 0 ? `${cooldownRemaining} 秒后可发送` : '重试请求' }}</button></view>
      </view>

      <view v-if="latestAnswer" class="ai-detail-stack">
        <view class="collapsible-card panel"><button class="collapse-toggle" @click="factsExpanded = !factsExpanded"><text>Business Facts</text><text>{{ factsExpanded ? '收起' : '展开' }}</text></button><view v-if="factsExpanded" class="collapse-body"><text>商品：{{ latestAnswer.businessFacts.productName }}</text><text>SKU：{{ latestAnswer.businessFacts.skuCode }}</text><text>规格：{{ latestAnswer.businessFacts.color }} / {{ latestAnswer.businessFacts.size }}</text><text>售价：¥{{ latestAnswer.businessFacts.unitPrice }} {{ latestAnswer.businessFacts.currency }}</text><text>当前库存：{{ latestAnswer.businessFacts.availableStock }}</text><text>商品状态：{{ latestAnswer.businessFacts.productStatus }}</text></view></view>
        <view class="collapsible-card panel"><button class="collapse-toggle" @click="evidenceExpanded = !evidenceExpanded"><text>Evidence（Java 真实来源）</text><text>{{ evidenceExpanded ? '收起' : '展开' }}</text></button><view v-if="evidenceExpanded" class="collapse-body"><view v-for="item in latestAnswer.evidence" :key="`${item.field}-${item.sourceId}`" class="evidence-row"><text>{{ item.displayName }}：{{ item.value }}</text><text class="source-meta">{{ item.sourceType }} #{{ item.sourceId }}</text></view></view></view>
        <view class="collapsible-card panel"><button class="collapse-toggle" @click="traceExpanded = !traceExpanded"><text>Trace（{{ latestAnswer.trace.length }} 步）</text><text>{{ traceExpanded ? '收起' : '展开' }}</text></button><view v-if="traceExpanded" class="collapse-body"><view v-for="item in latestAnswer.trace" :key="item.step" class="trace-row"><text>{{ traceLabel(item.step) }} · {{ item.status }}</text><text>{{ item.durationMs }} ms</text><text class="source-meta">{{ item.detail }}</text></view></view></view>
        <view class="collapsible-card panel"><button class="collapse-toggle" @click="demoExpanded = !demoExpanded"><text>开发演示信息</text><text>{{ demoExpanded ? '收起' : '展开' }}</text></button><view v-if="demoExpanded" class="collapse-body"><text>Provider：{{ providerLabel }}</text><text>traceId：{{ latestAnswer.traceId }}</text><text>fallbackUsed：{{ latestAnswer.fallbackUsed ? 'true' : 'false' }}</text><text v-if="latestAnswer.warning" class="source-meta">{{ latestAnswer.warning }}</text></view></view>
      </view>
    </template>

    <view v-if="state === 'ready'" class="bottom-action ai-composer"><view class="composer-wrap"><textarea v-model="question" class="question-input" :maxlength="MAX_QUESTION_LENGTH" :disabled="!canSend" placeholder="请输入商品价格、规格、库存、可购买性或 SKU 编码问题" confirm-type="send" @confirm="sendQuestion" /><view class="composer-footer"><text>{{ questionCount }}/{{ MAX_QUESTION_LENGTH }}</text><button class="primary-button" :disabled="!canSend || !question.trim()" @click="sendQuestion">{{ cooldownRemaining > 0 ? `${cooldownRemaining}秒后可发送` : sending ? '发送中...' : '发送' }}</button></view></view></view>
  </view>
</template>

<style>
.ai-customer-service-page{padding-bottom:calc(146px + env(safe-area-inset-bottom))}.ai-context{display:flex;gap:12px;padding:12px}.ai-context-image{width:94px;height:94px;flex:none;border-radius:12px}.ai-context-copy{min-width:0;flex:1}.context-category{display:block;color:var(--cf-blue);font-size:12px;font-weight:800}.context-title{display:block;margin-top:3px;color:var(--cf-ink);font-size:18px;font-weight:800;line-height:1.3}.context-spec{display:block;margin-top:5px;color:#607087;font-family:ui-monospace,SFMono-Regular,Consolas,monospace;font-size:12px;overflow-wrap:anywhere}.context-bottom{display:flex;align-items:center;flex-wrap:wrap;gap:5px;margin-top:8px}.context-price{color:var(--cf-blue);font-size:20px;font-weight:800}.context-currency{color:var(--cf-muted);font-size:12px}.context-bottom .tag{padding:4px 7px;font-size:11px}.quota-bar{display:flex;flex-wrap:wrap;gap:6px 10px;margin-top:12px;padding:10px 12px;color:#45627f;font-size:12px;font-weight:700;line-height:1.4}.quick-panel{margin-top:12px;padding:13px}.quick-title{display:block;color:var(--cf-ink);font-size:15px;font-weight:800}.quick-list{display:flex;flex-wrap:wrap;gap:8px;margin-top:10px}.quick-button{min-height:34px;padding:6px 9px;border:1px solid #cfe0ff;border-radius:9px;background:var(--cf-blue-soft);color:#145bc8;font-size:13px;line-height:1.2}.chat-panel{display:block;max-height:332px;margin-top:12px;padding:13px;overflow-y:auto}.chat-welcome{display:block;padding:10px;border-radius:10px;background:#f3f6fa;color:#53647c;font-size:13px;line-height:1.45}.chat-turn{display:block;margin-top:10px}.chat-bubble{display:block;max-width:92%;padding:10px 11px;border-radius:12px;font-size:14px;line-height:1.5;overflow-wrap:anywhere}.chat-bubble--user{margin-left:auto;background:var(--cf-blue);color:#fff}.chat-bubble--assistant{background:#eef8f4;color:#245540}.answer-status{display:block;margin-bottom:4px;color:var(--cf-green);font-size:12px;font-weight:800}.provider-meta{display:block;margin-top:7px;color:#5f8173;font-size:11px}.chat-error{display:flex;flex-direction:column;gap:9px;margin-top:10px;padding:11px;border:1px solid #efc2c7;border-radius:12px;background:var(--cf-red-soft);color:var(--cf-red);font-size:14px;line-height:1.45}.chat-error .secondary-button{width:100%;min-height:40px}.chat-error--rate{border-color:#f1d89b;background:var(--cf-orange-soft);color:#89520e}.rate-title{font-size:16px;font-weight:800}.ai-detail-stack{display:flex;flex-direction:column;gap:10px;margin-top:12px}.collapsible-card{overflow:hidden}.collapse-toggle{display:flex;align-items:center;justify-content:space-between;width:100%;min-height:48px;padding:10px 13px;background:#fff;color:var(--cf-ink);font-size:15px;font-weight:800;text-align:left}.collapse-body{display:flex;flex-direction:column;gap:7px;padding:0 13px 13px;color:#4d5f78;font-size:13px;line-height:1.4;overflow-wrap:anywhere}.evidence-row,.trace-row{display:flex;flex-wrap:wrap;justify-content:space-between;gap:3px 8px;padding:8px;border-radius:9px;background:#f6f8fb}.source-meta{display:block;width:100%;color:#718097;font-size:11px}.ai-composer{height:132px;min-height:132px;max-height:132px;padding-top:8px;padding-bottom:calc(8px + env(safe-area-inset-bottom))}.composer-wrap{width:100%}.question-input{display:block;width:100%;height:49px;min-height:49px;padding:9px 11px;border:1px solid #cfd9e5;border-radius:10px;background:#fff;color:var(--cf-ink);font:14px/1.35 -apple-system,BlinkMacSystemFont,"Segoe UI","Microsoft YaHei",sans-serif;resize:none}.composer-footer{display:flex;align-items:center;justify-content:space-between;gap:10px;margin-top:6px;color:var(--cf-muted);font-size:12px}.composer-footer .primary-button{width:116px;min-height:40px;padding:7px 9px;font-size:14px}@media (min-width:420px){.chat-panel{max-height:370px}.ai-context-image{width:102px;height:102px}}
.bottom-action.ai-composer{height:132px!important;min-height:132px!important;max-height:132px!important}
.ai-context.panel{display:flex!important}
.ai-context-copy{display:block}
.quota-bar{display:flex;flex-direction:column;align-items:stretch;gap:7px}
.quota-heading,.quota-values{display:flex;align-items:center;justify-content:space-between;gap:8px;min-width:0}
.quota-heading>text:first-child{color:var(--cf-ink);font-weight:800}
.quota-heading>text:last-child{color:var(--cf-muted);font-size:11px;font-weight:700;white-space:nowrap}
.quota-values{flex-wrap:wrap;color:#45627f;font-size:12px}
.quota-number{color:var(--cf-ink);font-weight:800}
.pending-question{padding:8px;border-radius:9px;background:rgba(255,255,255,.55);font-weight:700;overflow-wrap:anywhere}
</style>
