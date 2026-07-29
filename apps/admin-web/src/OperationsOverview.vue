<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { fetchOperationsOverview, formatOperationsMoney, type OperationsOverview } from './operations'

const emit = defineEmits<{ (event: 'open-orders'): void }>()
const data = ref<OperationsOverview | null>(null)
const loading = ref(true)
const error = ref('')
const refreshedAt = computed(() => data.value ? new Intl.DateTimeFormat('zh-CN', { dateStyle: 'medium', timeStyle: 'medium' }).format(new Date(data.value.generatedAt)) : '')

async function load() {
  loading.value = true
  error.value = ''
  try { data.value = await fetchOperationsOverview() }
  catch (cause) { error.value = cause instanceof Error ? cause.message : '运营总览加载失败' }
  finally { loading.value = false }
}

function statusLabel(status: string) { return status === 'CREATED' ? '已创建' : status }
function stockLabel(level: string) { return level === 'OUT_OF_STOCK' ? '缺货' : level === 'LOW_STOCK' ? '库存偏低' : '库存正常' }
onMounted(load)
</script>

<template>
  <section class="operations-page" aria-labelledby="operations-title">
    <header class="operations-heading">
      <div><p class="eyebrow">LOCAL SHOWCASE / REAL READ MODEL</p><h1 id="operations-title">运营总览</h1><p class="subtitle">本地 Showcase 数据，仅展示当前 MySQL、ai_trace 和运行时配置可证明的事实。</p></div>
      <div class="operations-actions"><small v-if="data">最近刷新：{{ refreshedAt }}</small><button type="button" class="secondary-button" :disabled="loading" @click="load">{{ loading ? '加载中…' : '刷新' }}</button></div>
    </header>
    <section v-if="loading" class="state-panel" data-testid="operations-loading"><span class="state-mark loading-mark">…</span><div><strong>正在读取本地事实</strong><p>运营总览不会使用前端预置指标。</p></div></section>
    <section v-else-if="error" class="state-panel error-state" data-testid="operations-error"><span class="state-mark">!</span><div><strong>运营总览暂不可用</strong><p>{{ error }}</p></div><button type="button" class="secondary-button" @click="load">重试</button></section>
    <template v-else-if="data">
      <section class="overview-metrics" data-testid="operations-summary">
        <article><small>商品数</small><strong>{{ data.summary.productCount }}</strong><span>在售 {{ data.summary.onSaleProductCount }} 个</span></article>
        <article><small>SKU 数</small><strong>{{ data.summary.skuCount }}</strong><span>低库存 {{ data.summary.lowStockSkuCount }} 个</span></article>
        <article><small>可用库存</small><strong>{{ data.summary.availableStockTotal }}</strong><span>当前库存合计</span></article>
        <article><small>已创建订单</small><strong>{{ data.summary.createdOrderCount }}</strong><span>仅 CREATED</span></article>
        <article><small>本地订单金额</small><strong>{{ formatOperationsMoney(data.summary.createdOrderAmount, 'CNY') }}</strong><span>非商业收入</span></article>
        <article><small>AI 交互</small><strong>{{ data.summary.aiInteractionCount }}</strong><span>来自 ai_trace</span></article>
      </section>
      <section class="operations-grid">
        <article class="panel operations-panel"><div class="panel-title-row"><div><p class="eyebrow">真实订单</p><h2>最近已创建订单</h2></div><span class="read-only-badge">只读</span></div>
          <div v-if="data.recentOrders.length" class="overview-list"><button v-for="order in data.recentOrders" :key="order.orderNo" type="button" class="overview-row" @click="emit('open-orders')"><span><strong class="mono">{{ order.orderNo }}</strong><small>{{ new Intl.DateTimeFormat('zh-CN', { dateStyle: 'short', timeStyle: 'short' }).format(new Date(order.createdAt)) }}</small></span><span><small>{{ order.itemCount }} 件 · {{ statusLabel(order.status) }}</small><strong class="money">{{ formatOperationsMoney(order.totalAmount, order.currency) }}</strong></span></button></div>
          <p v-else class="overview-empty" data-testid="operations-orders-empty">当前本地数据还没有已创建订单。</p>
        </article>
        <article class="panel operations-panel"><div class="panel-title-row"><div><p class="eyebrow">真实库存</p><h2>低库存 SKU</h2></div><span class="read-only-badge">阈值 &lt; 30</span></div>
          <div v-if="data.lowStockSkus.length" class="overview-list"><div v-for="sku in data.lowStockSkus" :key="sku.skuId" class="overview-row static-row"><span><strong>{{ sku.productName }}</strong><small class="mono">{{ sku.skuCode }} · {{ sku.color }} / {{ sku.size }}</small></span><span><small :class="['stock-badge', sku.stockLevel === 'OUT_OF_STOCK' ? 'out' : 'low']">{{ stockLabel(sku.stockLevel) }}</small><strong>{{ sku.availableStock }}</strong></span></div></div>
          <p v-else class="overview-empty" data-testid="operations-stock-empty">当前没有低库存 SKU。</p>
        </article>
        <article class="panel operations-panel"><div class="panel-title-row"><div><p class="eyebrow">AI TRACE</p><h2>AI 状态摘要</h2></div><span class="read-only-badge">{{ data.aiSummary.providerMode }}</span></div>
          <dl class="overview-facts"><div><dt>Provider</dt><dd>{{ data.aiSummary.provider }}</dd></div><div><dt>回答</dt><dd>{{ data.aiSummary.answeredCount }}</dd></div><div><dt>降级</dt><dd>{{ data.aiSummary.fallbackCount }}</dd></div><div><dt>不支持</dt><dd>{{ data.aiSummary.unsupportedCount }}</dd></div><div><dt>Provider 错误</dt><dd>{{ data.aiSummary.providerErrorCount }}</dd></div></dl>
        </article>
        <article class="panel operations-panel"><div class="panel-title-row"><div><p class="eyebrow">RUNTIME BOUNDARY</p><h2>系统与边界</h2></div><span class="read-only-badge">{{ data.runtimeBoundary.dataScope }}</span></div>
          <dl class="overview-facts boundary-facts"><div><dt>AI</dt><dd>commerceflow-mock / {{ data.runtimeBoundary.aiMode }}</dd></div><div><dt>限流</dt><dd>Redis Lua 固定窗口 · {{ data.runtimeBoundary.rateLimitLimit }} 次 / {{ data.runtimeBoundary.rateLimitWindowSeconds }} 秒</dd></div><div><dt>策略</dt><dd>{{ data.runtimeBoundary.rateLimitFailurePolicy }}</dd></div><div><dt>订单</dt><dd>{{ data.runtimeBoundary.orderStatusScope }}</dd></div><div><dt>身份</dt><dd>{{ data.runtimeBoundary.authenticationMode }}</dd></div><div><dt>移动端</dt><dd>{{ data.runtimeBoundary.mobileRuntime }} · 非 H5 {{ data.runtimeBoundary.nonH5Runtime }}</dd></div></dl>
        </article>
      </section>
    </template>
  </section>
</template>
