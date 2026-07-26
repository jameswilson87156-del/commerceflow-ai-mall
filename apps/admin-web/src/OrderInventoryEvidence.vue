<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import {
  formatOrderMoney,
  formatOrderTime,
  orderStatusLabel,
  requestResultLabel,
  type OrderExecutionEvidence,
  type OrderSummary,
} from './orders'

const API_BASE = import.meta.env.VITE_API_BASE || 'http://localhost:8080/api'

type LoadState = 'loading' | 'ready' | 'empty' | 'error'

const userId = ref('1')
const query = ref('')
const orders = ref<OrderSummary[]>([])
const selectedOrderNo = ref<string | null>(null)
const selectedOrder = ref<OrderSummary | null>(null)
const evidence = ref<OrderExecutionEvidence | null>(null)
const listState = ref<LoadState>('loading')
const detailState = ref<LoadState>('loading')
const listError = ref('')
const detailError = ref('')

const filteredOrders = computed(() => {
  const keyword = query.value.trim().toLocaleLowerCase()
  return !keyword ? orders.value : orders.value.filter((order) => order.orderNo.toLocaleLowerCase().includes(keyword))
})

function responseError(response: Response): Error {
  return new Error(`请求失败（HTTP ${response.status}）`)
}

async function requestOrders(): Promise<OrderSummary[]> {
  const response = await fetch(`${API_BASE}/orders?userId=${encodeURIComponent(userId.value)}`)
  if (!response.ok) throw responseError(response)
  return response.json() as Promise<OrderSummary[]>
}

async function requestDetail(orderNo: string): Promise<OrderSummary> {
  const response = await fetch(`${API_BASE}/orders/${encodeURIComponent(orderNo)}`)
  if (!response.ok) throw responseError(response)
  return response.json() as Promise<OrderSummary>
}

async function requestEvidence(orderNo: string): Promise<OrderExecutionEvidence> {
  const response = await fetch(`${API_BASE}/orders/${encodeURIComponent(orderNo)}/execution-evidence`)
  if (!response.ok) throw responseError(response)
  return response.json() as Promise<OrderExecutionEvidence>
}

async function selectOrder(orderNo: string) {
  selectedOrderNo.value = orderNo
  selectedOrder.value = orders.value.find((order) => order.orderNo === orderNo) ?? null
  evidence.value = null
  detailState.value = 'loading'
  detailError.value = ''
  try {
    const [detail, executionEvidence] = await Promise.all([requestDetail(orderNo), requestEvidence(orderNo)])
    selectedOrder.value = detail
    evidence.value = executionEvidence
    detailState.value = 'ready'
  } catch (error) {
    detailState.value = 'error'
    detailError.value = error instanceof Error ? error.message : '订单执行证据加载失败'
  }
}

async function loadOrders() {
  listState.value = 'loading'
  detailState.value = 'loading'
  listError.value = ''
  detailError.value = ''
  selectedOrder.value = null
  selectedOrderNo.value = null
  evidence.value = null
  try {
    orders.value = await requestOrders()
    if (orders.value.length === 0) {
      listState.value = 'empty'
      detailState.value = 'empty'
      return
    }
    listState.value = 'ready'
    await selectOrder(orders.value[0].orderNo)
  } catch (error) {
    listState.value = 'error'
    detailState.value = 'empty'
    listError.value = error instanceof Error ? error.message : '订单列表加载失败'
  }
}

onMounted(loadOrders)
</script>

<template>
  <section class="order-page" aria-labelledby="order-page-title">
    <header class="order-heading">
      <div>
        <p class="eyebrow">订单执行 / 本地真实证据</p>
        <h1 id="order-page-title">订单管理与库存执行证据</h1>
        <p class="subtitle">查看已创建订单、OrderItem 快照、库存扣减记录与幂等初始结果</p>
      </div>
      <p class="catalog-source">GET /api/orders · GET /api/orders/{orderNo}/execution-evidence</p>
    </header>

    <section class="order-filter-bar" aria-label="订单真实查询条件">
      <label><span>用户 userId</span><input v-model="userId" inputmode="numeric" type="number" min="1" data-testid="user-id-input"></label>
      <button type="button" class="primary-button" data-testid="load-orders" @click="loadOrders">加载订单</button>
      <label class="order-search"><span>订单号搜索</span><input v-model="query" type="search" placeholder="搜索已加载的真实订单号" data-testid="order-search"></label>
    </section>

    <div v-if="listState === 'loading'" class="state-panel" data-testid="order-loading" role="status">
      <span class="state-mark loading-mark" aria-hidden="true"></span><div><strong>正在加载真实订单</strong><p>正在请求本地 Java 订单接口。</p></div>
    </div>
    <div v-else-if="listState === 'error'" class="state-panel error-state" data-testid="order-error" role="alert">
      <span class="state-mark" aria-hidden="true">!</span><div><strong>订单列表加载失败</strong><p>{{ listError }}</p></div><button class="secondary-button" type="button" @click="loadOrders">重新加载</button>
    </div>
    <div v-else-if="listState === 'empty'" class="state-panel" data-testid="order-empty">
      <span class="state-mark" aria-hidden="true">0</span><div><strong>当前用户没有已创建订单</strong><p>接口返回空列表，页面不会生成示例订单。</p></div><button class="secondary-button" type="button" @click="loadOrders">重新加载</button>
    </div>

    <template v-else>
      <div class="order-layout">
        <section class="order-list-panel" aria-labelledby="order-list-title">
          <div class="panel-title-row"><div><p class="eyebrow">真实订单列表</p><h2 id="order-list-title">已创建订单</h2></div><span class="result-count">{{ filteredOrders.length }} 笔</span></div>
          <div v-if="filteredOrders.length" class="order-list">
            <button v-for="order in filteredOrders" :key="order.orderNo" type="button" :class="['order-card', { selected: selectedOrderNo === order.orderNo }]" :data-testid="`order-${order.orderNo}`" @click="selectOrder(order.orderNo)">
              <span class="order-card-top"><strong class="mono">{{ order.orderNo }}</strong><span class="created-status">{{ orderStatusLabel(order.status) }}</span></span>
              <span class="order-card-summary"><span>userId {{ order.userId }}</span><strong>{{ formatOrderMoney(order.totalAmount, order.currency) }}</strong></span>
              <small>{{ formatOrderTime(order.createdAt) }}</small>
            </button>
          </div>
          <div v-else class="filtered-empty" data-testid="order-filtered-empty">没有匹配订单号的真实订单。</div>
        </section>

        <section class="order-detail-stack" aria-live="polite">
          <div v-if="detailState === 'loading'" class="panel detail-state" data-testid="order-detail-loading">正在加载订单详情与执行证据…</div>
          <div v-else-if="detailState === 'error'" class="panel detail-state error-state" data-testid="order-detail-error"><strong>订单执行证据加载失败</strong><p>{{ detailError }}</p><button v-if="selectedOrderNo" class="secondary-button" type="button" @click="selectOrder(selectedOrderNo)">重新加载详情</button></div>
          <template v-else-if="selectedOrder && evidence">
            <section class="panel order-overview" aria-labelledby="order-overview-title">
              <div class="panel-title-row"><div><p class="eyebrow">当前订单</p><h2 id="order-overview-title">订单基本信息</h2></div><span class="created-status">{{ selectedOrder.status }} / {{ orderStatusLabel(selectedOrder.status) }}</span></div>
              <div class="order-overview-grid">
                <div><span>订单号</span><strong class="mono">{{ selectedOrder.orderNo }}</strong></div><div><span>userId</span><strong>{{ selectedOrder.userId }}</strong></div><div><span>订单金额</span><strong class="order-money">{{ formatOrderMoney(selectedOrder.totalAmount, selectedOrder.currency) }}</strong></div><div><span>currency</span><strong class="mono">{{ selectedOrder.currency }}</strong></div><div class="wide"><span>createdAt</span><strong>{{ formatOrderTime(selectedOrder.createdAt) }}</strong></div>
              </div>
            </section>

            <section class="panel order-items-panel" aria-labelledby="order-items-title">
              <div class="panel-title-row"><div><p class="eyebrow">OrderItem 快照</p><h2 id="order-items-title">商品与 SKU 快照</h2></div><span class="result-count">{{ evidence.items.length }} 项</span></div>
              <div class="table-wrap"><table><thead><tr><th>商品快照</th><th>skuCode 快照</th><th>颜色</th><th>尺寸</th><th>unitPrice</th><th>数量</th><th>小计</th></tr></thead><tbody><tr v-for="item in evidence.items" :key="`${item.skuCodeSnapshot}-${item.quantity}`"><td class="strong-cell">{{ item.productNameSnapshot }}</td><td class="mono">{{ item.skuCodeSnapshot }}</td><td>{{ item.colorSnapshot }}</td><td>{{ item.sizeSnapshot }}</td><td class="money">{{ formatOrderMoney(item.unitPrice, evidence.currency) }}</td><td>{{ item.quantity }}</td><td class="order-money">{{ formatOrderMoney(item.subtotal, evidence.currency) }}</td></tr></tbody></table></div>
            </section>

            <section class="panel execution-panel" aria-labelledby="execution-title">
              <div class="panel-title-row"><div><p class="eyebrow">库存扣减证据</p><h2 id="execution-title">库存变动与幂等结果</h2></div><span class="evidence-badge">数据库记录</span></div>
              <div class="movement-wrap"><table><thead><tr><th>skuId</th><th>skuCode</th><th>扣减前</th><th>本次扣减</th><th>扣减后</th><th>movementType</th><th>执行时间</th></tr></thead><tbody><tr v-for="movement in evidence.inventoryMovements" :key="movement.movementId"><td class="mono">{{ movement.skuId }}</td><td class="mono strong-cell">{{ movement.skuCode }}</td><td>{{ movement.stockBefore }}</td><td class="deduct-value">-{{ movement.quantity }}</td><td class="stock-value">{{ movement.stockAfter }}</td><td class="mono">{{ movement.movementType }}</td><td>{{ formatOrderTime(movement.createdAt) }}</td></tr></tbody></table></div>
              <div class="idempotency-grid"><div><span>Idempotency-Key</span><strong class="mono">{{ evidence.idempotencyKey }}</strong></div><div><span>初始请求结果</span><strong>{{ requestResultLabel(evidence.requestResult) }}</strong></div><div><span>幂等重放</span><strong>{{ evidence.idempotencyReplay ? '重放请求' : '非重放记录' }}</strong></div></div>
              <p class="transaction-note">库存条件 UPDATE、订单、快照、库存 movement 与购物车清理处于同一事务。相同 Key 且相同请求返回本订单，不会重复扣库存或写入 movement；相同 Key 不同请求返回 409。</p>
            </section>
          </template>
        </section>
      </div>
    </template>
  </section>
</template>
