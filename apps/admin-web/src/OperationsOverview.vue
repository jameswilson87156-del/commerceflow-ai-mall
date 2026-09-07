<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { adminAccessStateLabel, AdminApiError } from './adminApi'
import { fetchOperationsOverview, formatOperationsMoney, type OperationsOverview } from './operations'
import AdminBoundaryStrip from './components/AdminBoundaryStrip.vue'
import AdminSectionHeader from './components/AdminSectionHeader.vue'
import AdminStatusPill from './components/AdminStatusPill.vue'

const emit = defineEmits<{ (event: 'open-orders'): void }>()
const data = ref<OperationsOverview | null>(null)
const loading = ref(true)
const error = ref('')
const accessState = ref('Request Failed')
const refreshedAt = computed(() => data.value ? new Intl.DateTimeFormat('zh-CN', { dateStyle: 'medium', timeStyle: 'medium' }).format(new Date(data.value.generatedAt)) : '')

async function load() {
  loading.value = true
  error.value = ''
  accessState.value = 'Request Failed'
  try { data.value = await fetchOperationsOverview() }
  catch (cause) { accessState.value = cause instanceof AdminApiError ? adminAccessStateLabel(cause.state) : 'Request Failed'; error.value = cause instanceof Error ? cause.message : '运营总览加载失败' }
  finally { loading.value = false }
}

function statusLabel(status: string) { return status === 'CREATED' ? '已创建' : status }
function stockLabel(level: string) { return level === 'OUT_OF_STOCK' ? '缺货' : level === 'LOW_STOCK' ? '库存偏低' : '库存正常' }
onMounted(load)
</script>

<template>
  <section class="operations-page" aria-labelledby="operations-title">
    <header class="page-hero page-hero--operations">
      <div>
        <p class="eyebrow">OPERATIONS / REAL READ MODEL</p>
        <h1 id="operations-title">运营总览</h1>
        <p class="subtitle">关注订单变化、库存预警与客服处理情况。</p>
      </div>
      <div class="page-hero__aside">
        <AdminStatusPill label="只读 Showcase" tone="blue" />
        <span v-if="data" class="refresh-stamp">刷新于 {{ refreshedAt }}</span>
        <button type="button" class="secondary-button" :disabled="loading" @click="load">{{ loading ? '加载中…' : '刷新数据' }}</button>
      </div>
    </header>

    <AdminBoundaryStrip label="Operator 数据边界" message="指标来自 MySQL 读模型、ai_trace 与运行时配置；请求需要后端 OperatorScope，页面不生成未经证明的商业指标或虚构订单。" tone="blue" />

    <section v-if="loading" class="state-panel state-panel--prominent" data-testid="operations-loading" role="status">
      <span class="state-mark loading-mark">…</span><div><strong>正在读取服务端事实</strong><p>Operator 运营总览不会使用前端预置指标。</p></div>
    </section>
    <section v-else-if="error" class="state-panel error-state state-panel--prominent" data-testid="operations-error" role="alert">
      <span class="state-mark">!</span><div><strong>运营总览暂不可用</strong><small class="access-state" data-testid="operations-access-state">{{ accessState }}</small><p>{{ error }}</p></div><button type="button" class="secondary-button" @click="load">重试</button>
    </section>

    <template v-else-if="data">
      <section class="signal-grid" data-testid="operations-summary" aria-label="运营事实摘要">
        <article class="signal-card signal-card--primary"><span class="signal-kicker">CATALOG</span><small>在售商品</small><strong>{{ data.summary.onSaleProductCount }}<em> / {{ data.summary.productCount }}</em></strong><span>商品数 · {{ data.summary.skuCount }} 个 SKU</span></article>
        <article class="signal-card"><span class="signal-kicker">INVENTORY</span><small>可用库存</small><strong>{{ data.summary.availableStockTotal }}</strong><span>低库存 {{ data.summary.lowStockSkuCount }} 个</span></article>
        <article class="signal-card"><span class="signal-kicker">ORDERS</span><small>已创建订单</small><strong>{{ data.summary.createdOrderCount }}</strong><span>状态范围 · CREATED</span></article>
        <article class="signal-card signal-card--money"><span class="signal-kicker">CNY LEDGER</span><small>本地订单金额</small><strong>{{ formatOperationsMoney(data.summary.createdOrderAmount, 'CNY') }}</strong><span>非商业收入统计</span></article>
        <article class="signal-card signal-card--ai"><span class="signal-kicker">AI TRACE</span><small>AI 交互</small><strong>{{ data.summary.aiInteractionCount }}</strong><span>已写入 ai_trace</span></article>
      </section>

      <section class="operations-grid operations-grid--command">
        <article class="panel operations-panel operations-panel--orders">
          <AdminSectionHeader eyebrow="REAL ORDERS" title="最近已创建订单" description="从订单读模型打开完整执行证据。" :count="`${data.recentOrders.length} 笔`">
            <AdminStatusPill label="只读" tone="neutral" />
          </AdminSectionHeader>
          <div v-if="data.recentOrders.length" class="overview-list">
            <button v-for="order in data.recentOrders" :key="order.orderNo" type="button" class="overview-row overview-row--featured" @click="emit('open-orders')">
              <span class="overview-row__identity"><strong class="mono">{{ order.orderNo }}</strong><small>{{ new Intl.DateTimeFormat('zh-CN', { dateStyle: 'short', timeStyle: 'short' }).format(new Date(order.createdAt)) }}</small></span>
              <span class="overview-row__value"><small>{{ order.itemCount }} 件 · {{ statusLabel(order.status) }}</small><strong class="money">{{ formatOperationsMoney(order.totalAmount, order.currency) }}</strong></span><span class="row-arrow" aria-hidden="true">↗</span>
            </button>
          </div>
          <p v-else class="overview-empty" data-testid="operations-orders-empty">当前本地数据还没有已创建订单。</p>
        </article>

        <article class="panel operations-panel operations-panel--inventory">
          <AdminSectionHeader eyebrow="REAL INVENTORY" title="库存预警" description="阈值低于 30 的真实 SKU。" :count="`${data.lowStockSkus.length} 项`">
            <AdminStatusPill label="阈值 &lt; 30" tone="amber" />
          </AdminSectionHeader>
          <div v-if="data.lowStockSkus.length" class="overview-list">
            <div v-for="sku in data.lowStockSkus" :key="sku.skuId" class="overview-row static-row">
              <span class="overview-row__identity"><strong>{{ sku.productName }}</strong><small class="mono">{{ sku.skuCode }} · {{ sku.color }} / {{ sku.size }}</small></span>
              <span class="overview-row__value"><small :class="['stock-badge', sku.stockLevel === 'OUT_OF_STOCK' ? 'out' : 'low']">{{ stockLabel(sku.stockLevel) }}</small><strong>{{ sku.availableStock }}</strong></span>
            </div>
          </div>
          <p v-else class="overview-empty" data-testid="operations-stock-empty">当前没有低库存 SKU。</p>
        </article>

        <article class="panel operations-panel operations-panel--ai">
          <AdminSectionHeader eyebrow="AI TRACE" title="AI 状态摘要" description="仅呈现最近真实链路统计。" :count="data.aiSummary.providerMode">
            <AdminStatusPill :label="data.aiSummary.provider" tone="green" />
          </AdminSectionHeader>
          <dl class="overview-facts overview-facts--cards"><div><dt>回答</dt><dd>{{ data.aiSummary.answeredCount }}</dd></div><div><dt>降级</dt><dd>{{ data.aiSummary.fallbackCount }}</dd></div><div><dt>不支持</dt><dd>{{ data.aiSummary.unsupportedCount }}</dd></div><div><dt>Provider 错误</dt><dd>{{ data.aiSummary.providerErrorCount }}</dd></div></dl>
        </article>

        <details class="panel operations-panel operations-panel--boundary">
          <summary class="runtime-summary"><span>系统与数据边界</span><AdminStatusPill :label="data.runtimeBoundary.dataScope" tone="blue" /></summary>
          <dl class="overview-facts boundary-facts"><div><dt>AI</dt><dd>commerceflow-mock / {{ data.runtimeBoundary.aiMode }}</dd></div><div><dt>限流</dt><dd>Redis Lua 固定窗口 · {{ data.runtimeBoundary.rateLimitLimit }} 次 / {{ data.runtimeBoundary.rateLimitWindowSeconds }} 秒</dd></div><div><dt>策略</dt><dd>{{ data.runtimeBoundary.rateLimitFailurePolicy }}</dd></div><div><dt>订单</dt><dd>{{ data.runtimeBoundary.orderStatusScope }}</dd></div><div><dt>身份</dt><dd>{{ data.runtimeBoundary.authenticationMode }}</dd></div><div><dt>移动端</dt><dd>{{ data.runtimeBoundary.mobileRuntime }} · 非 H5 {{ data.runtimeBoundary.nonH5Runtime }}</dd></div></dl>
        </details>
      </section>
    </template>
  </section>
</template>
