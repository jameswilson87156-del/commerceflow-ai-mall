<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { adminAccessStateLabel, AdminApiError } from './adminApi'
import { fetchOperatorOrder, fetchOperatorOrderEvidence, fetchOperatorOrders, formatOrderMoney, formatOrderTime, orderStatusLabel, requestResultLabel, type InventoryMovement, type OrderExecutionEvidence, type OrderItemSnapshot, type OrderSummary } from './orders'
import AdminBoundaryStrip from './components/AdminBoundaryStrip.vue'
import AdminSectionHeader from './components/AdminSectionHeader.vue'
import AdminStatusPill from './components/AdminStatusPill.vue'

type LoadState = 'loading' | 'ready' | 'empty' | 'error'
const query = ref('')
const orders = ref<OrderSummary[]>([])
const selectedOrderNo = ref<string | null>(null)
const selectedOrder = ref<OrderSummary | null>(null)
const evidence = ref<OrderExecutionEvidence | null>(null)
const listState = ref<LoadState>('loading')
const detailState = ref<LoadState>('loading')
const listError = ref('')
const detailError = ref('')
const listErrorState = ref('Request Failed')
const detailErrorState = ref('Request Failed')
const failedImageKeys = ref(new Set<string>())
const filteredOrders = computed(() => { const keyword = query.value.trim().toLocaleLowerCase(); return !keyword ? orders.value : orders.value.filter((order) => order.orderNo.toLocaleLowerCase().includes(keyword)) })
function firstItem(order: OrderSummary): OrderItemSnapshot | null { return order.items[0] ?? null }
function previewItems(order: OrderSummary): OrderItemSnapshot[] { return order.items.slice(0, 2) }
function itemForMovement(movement: InventoryMovement): OrderItemSnapshot | null { return evidence.value?.items.find((item) => item.skuCodeSnapshot === movement.skuCode) ?? null }
function imageKey(orderNo: string, item: Pick<OrderItemSnapshot, 'skuCodeSnapshot'> | null): string { return `${orderNo}:${item?.skuCodeSnapshot ?? 'missing'}` }
function hasImageFailure(key: string): boolean { return failedImageKeys.value.has(key) }
function markImageFailure(key: string) { failedImageKeys.value = new Set([...failedImageKeys.value, key]) }
async function requestOrders(): Promise<OrderSummary[]> { return fetchOperatorOrders() }
async function requestDetail(orderNo: string): Promise<OrderSummary> { return fetchOperatorOrder(orderNo) }
async function requestEvidence(orderNo: string): Promise<OrderExecutionEvidence> { return fetchOperatorOrderEvidence(orderNo) }
async function selectOrder(orderNo: string) {
  selectedOrderNo.value = orderNo; selectedOrder.value = orders.value.find((order) => order.orderNo === orderNo) ?? null; evidence.value = null; detailState.value = 'loading'; detailError.value = ''
  try { const [detail, executionEvidence] = await Promise.all([requestDetail(orderNo), requestEvidence(orderNo)]); selectedOrder.value = detail; evidence.value = executionEvidence; detailState.value = 'ready' }
  catch (error) { detailState.value = 'error'; detailErrorState.value = error instanceof AdminApiError ? adminAccessStateLabel(error.state) : 'Request Failed'; detailError.value = error instanceof Error ? error.message : '订单详情与执行证据加载失败' }
}
async function loadOrders() {
  listState.value = 'loading'; detailState.value = 'loading'; listError.value = ''; detailError.value = ''; listErrorState.value = 'Request Failed'; detailErrorState.value = 'Request Failed'; selectedOrder.value = null; selectedOrderNo.value = null; evidence.value = null; failedImageKeys.value = new Set()
  try { orders.value = await requestOrders(); if (orders.value.length === 0) { listState.value = 'empty'; detailState.value = 'empty'; return }; listState.value = 'ready'; await selectOrder(orders.value[0].orderNo) }
  catch (error) { listState.value = 'error'; detailState.value = 'empty'; listErrorState.value = error instanceof AdminApiError ? adminAccessStateLabel(error.state) : 'Request Failed'; listError.value = error instanceof Error ? error.message : '订单列表加载失败' }
}
onMounted(loadOrders)
</script>

<template>
  <section class="order-page" aria-labelledby="order-page-title">
    <header class="page-hero page-hero--orders">
      <div><p class="eyebrow">OPERATOR / EXECUTION EVIDENCE</p><h1 id="order-page-title">订单执行证据</h1><p class="subtitle">Operator 身份通过后，读取跨用户订单、商品快照、库存扣减和幂等事务边界。</p></div>
      <div class="page-hero__aside"><AdminStatusPill label="Operator read" tone="green" /><span class="catalog-source">GET /api/v1/operator/orders<br>GET /api/v1/operator/orders/{orderNo}/execution-evidence</span></div>
    </header>
    <AdminBoundaryStrip label="Operator 数据边界" message="跨用户订单读取只允许通过后端 OperatorScope；左侧选择订单，右侧按‘订单摘要 → 商品快照 → 库存 movement’阅读，页面不提交 userId。" tone="amber" />

    <section class="order-filter-bar order-filter-bar--card" aria-label="订单真实查询条件">
      <div class="operator-scope-copy"><span>查询范围</span><strong>OperatorScope · 全部已创建订单</strong><small>身份与授权由 Java 后端判定</small></div>
      <label class="order-search"><span>订单号搜索</span><input v-model="query" type="search" placeholder="搜索已加载的真实订单号" data-testid="order-search"></label>
      <button type="button" class="primary-button" data-testid="load-orders" @click="loadOrders">加载真实订单</button>
    </section>

    <div v-if="listState === 'loading'" class="state-panel state-panel--prominent" data-testid="order-loading" role="status"><span class="state-mark loading-mark" aria-hidden="true"></span><div><strong>正在加载真实订单</strong><p>正在请求本地 Java 订单接口。</p></div></div>
    <div v-else-if="listState === 'error'" class="state-panel error-state state-panel--prominent" data-testid="order-error" role="alert"><span class="state-mark" aria-hidden="true">!</span><div><strong>订单列表加载失败</strong><small class="access-state" data-testid="order-list-access-state">{{ listErrorState }}</small><p>{{ listError }}</p></div><button class="secondary-button" type="button" @click="loadOrders">重新加载</button></div>
    <div v-else-if="listState === 'empty'" class="state-panel state-panel--prominent" data-testid="order-empty"><span class="state-mark" aria-hidden="true">0</span><div><strong>当前 Operator 范围没有已创建订单</strong><p>接口返回空列表，页面不会生成示例订单。</p></div><button class="secondary-button" type="button" @click="loadOrders">重新加载</button></div>

    <template v-else>
      <div class="order-layout order-layout--evidence">
        <section class="order-list-panel panel" aria-labelledby="order-list-title">
          <AdminSectionHeader eyebrow="OPERATOR ORDER INDEX" title="已创建订单" description="选择一笔订单查看服务端证据。" :count="`${filteredOrders.length} 笔`"><AdminStatusPill label="只读" tone="neutral" /></AdminSectionHeader>
          <div v-if="filteredOrders.length" class="order-list">
            <button v-for="order in filteredOrders" :key="order.orderNo" type="button" :class="['order-card', { selected: selectedOrderNo === order.orderNo }]" :data-testid="`order-${order.orderNo}`" @click="selectOrder(order.orderNo)">
              <span v-if="previewItems(order).length" :class="['order-card-media', { 'is-stack': previewItems(order).length > 1 }]">
                <template v-for="(item, itemIndex) in previewItems(order)" :key="item.skuCodeSnapshot"><img v-if="item.imagePathSnapshot && !hasImageFailure(imageKey(order.orderNo, item))" class="order-card-image" :src="item.imagePathSnapshot" :alt="`${item.productNameSnapshot} 商品缩略图`" :data-testid="itemIndex === 0 ? `order-thumbnail-${order.orderNo}` : `order-thumbnail-${order.orderNo}-${item.skuCodeSnapshot}`" @error="markImageFailure(imageKey(order.orderNo, item))"><span v-else class="order-image-placeholder" :data-testid="`order-thumbnail-placeholder-${order.orderNo}-${item.skuCodeSnapshot}`">{{ item.imagePathSnapshot ? '图片加载失败' : '暂无快照图片' }}</span></template>
              </span>
              <span v-else class="order-image-placeholder">暂无商品</span>
              <span class="order-card-content"><span class="order-card-top"><strong class="mono">{{ order.orderNo }}</strong><span class="created-status">{{ orderStatusLabel(order.status) }}</span></span><strong class="order-card-product">{{ firstItem(order)?.productNameSnapshot ?? '暂无商品快照' }}</strong><span class="order-card-summary"><span>{{ order.items.length > 1 ? `共 ${order.items.length} 件` : '共 1 件' }}</span><strong>{{ formatOrderMoney(order.totalAmount, order.currency) }}</strong></span><small>{{ formatOrderTime(order.createdAt) }}</small></span>
            </button>
          </div>
          <div v-else class="filtered-empty" data-testid="order-filtered-empty">没有匹配订单号的真实订单。</div>
          <p class="order-list-total" data-testid="order-list-total">共 {{ orders.length }} 笔服务端订单</p>
        </section>

        <section class="order-detail-stack" aria-live="polite">
          <div v-if="detailState === 'loading'" class="panel detail-state" data-testid="order-detail-loading"><span class="loading-line"></span>正在加载订单详情与执行证据…</div>
          <div v-else-if="detailState === 'error'" class="panel detail-state error-state" data-testid="order-detail-error"><strong>订单执行证据加载失败</strong><small class="access-state" data-testid="order-detail-access-state">{{ detailErrorState }}</small><p>{{ detailError }}</p><button v-if="selectedOrderNo" class="secondary-button" type="button" @click="selectOrder(selectedOrderNo)">重新加载详情</button></div>
          <template v-else-if="selectedOrder && evidence">
            <section class="panel order-overview order-overview--evidence" aria-labelledby="order-overview-title">
              <div class="detail-overline"><span>SELECTED ORDER</span><AdminStatusPill :label="`${selectedOrder.status} / ${orderStatusLabel(selectedOrder.status)}`" tone="green" /></div>
              <div class="order-overview-title-row"><div><h2 id="order-overview-title" class="order-number">{{ selectedOrder.orderNo }}</h2><p>创建于 {{ formatOrderTime(selectedOrder.createdAt) }}</p></div><strong class="order-total-hero">{{ formatOrderMoney(selectedOrder.totalAmount, selectedOrder.currency) }}</strong></div>
              <div class="order-overview-grid"><div><span>用户 ID <small>userId</small></span><strong>{{ selectedOrder.userId }}</strong></div><div><span>币种 <small>currency</small></span><strong class="mono">{{ selectedOrder.currency }}</strong></div><div><span>创建时间 <small>createdAt</small></span><strong>{{ formatOrderTime(selectedOrder.createdAt) }}</strong></div><div><span>订单结果</span><strong>{{ requestResultLabel(evidence.requestResult) }}</strong></div></div>
            </section>

            <section class="panel order-items-panel" aria-labelledby="order-items-title">
              <AdminSectionHeader eyebrow="ORDER ITEM SNAPSHOT" title="订单商品快照" description="创建时写入的商品名称、规格、价格和图片。" :count="`${evidence.items.length} 项`"><AdminStatusPill label="历史快照" tone="blue" /></AdminSectionHeader>
              <div class="table-wrap"><table class="order-item-table"><thead><tr><th>商品图片</th><th>商品名称快照</th><th>SKU 编码快照</th><th>颜色</th><th>尺寸</th><th>单价</th><th>数量</th><th>小计</th></tr></thead><tbody><tr v-for="item in evidence.items" :key="`${item.skuCodeSnapshot}-${item.quantity}`"><td><img v-if="item.imagePathSnapshot && !hasImageFailure(imageKey(selectedOrder.orderNo, item))" class="order-item-image" :src="item.imagePathSnapshot" :alt="`${item.productNameSnapshot} 商品图片`" :data-testid="`order-item-image-${item.skuCodeSnapshot}`" @error="markImageFailure(imageKey(selectedOrder.orderNo, item))"><span v-else class="order-image-placeholder table-placeholder" :data-testid="item.imagePathSnapshot ? `image-failed-${item.skuCodeSnapshot}` : `image-missing-${item.skuCodeSnapshot}`">{{ item.imagePathSnapshot ? '图片加载失败' : '暂无快照图片' }}</span></td><td class="strong-cell">{{ item.productNameSnapshot }}</td><td class="mono">{{ item.skuCodeSnapshot }}</td><td>{{ item.colorSnapshot }}</td><td>{{ item.sizeSnapshot }}</td><td class="money">{{ formatOrderMoney(item.unitPrice, evidence.currency) }}</td><td>{{ item.quantity }}</td><td class="order-money">{{ formatOrderMoney(item.subtotal, evidence.currency) }}</td></tr></tbody></table></div>
            </section>

            <section class="panel execution-panel" aria-labelledby="execution-title">
              <AdminSectionHeader eyebrow="INVENTORY MOVEMENT / TRANSACTION" title="库存扣减与事务结果" description="条件 UPDATE、订单和 movement 处于同一事务。"><AdminStatusPill label="数据库记录" tone="green" /></AdminSectionHeader>
              <div class="movement-wrap"><table class="movement-table"><thead><tr><th>商品</th><th><span>SKU ID</span><small>skuId</small></th><th><span>SKU 编码</span><small>skuCode</small></th><th>扣减前</th><th>本次扣减</th><th>扣减后</th><th><span>变动类型</span><small>movementType</small></th><th>执行时间</th></tr></thead><tbody><tr v-for="movement in evidence.inventoryMovements" :key="movement.movementId"><td><span v-if="itemForMovement(movement)" class="movement-item-media"><img v-if="itemForMovement(movement)?.imagePathSnapshot && !hasImageFailure(imageKey(selectedOrder.orderNo, itemForMovement(movement)))" class="movement-item-image" :src="itemForMovement(movement)?.imagePathSnapshot ?? ''" :alt="`${itemForMovement(movement)?.productNameSnapshot} 商品缩略图`" :data-testid="`movement-item-image-${movement.skuCode}`" @error="markImageFailure(imageKey(selectedOrder.orderNo, itemForMovement(movement)))"><span v-else class="movement-item-name">{{ itemForMovement(movement)?.productNameSnapshot }}</span></span><span v-else class="movement-item-name">未匹配商品快照</span></td><td class="mono">{{ movement.skuId }}</td><td class="mono strong-cell">{{ movement.skuCode }}</td><td>{{ movement.stockBefore }}</td><td class="deduct-value">-{{ movement.quantity }}</td><td class="stock-value">{{ movement.stockAfter }}</td><td class="mono">{{ movement.movementType }}</td><td>{{ formatOrderTime(movement.createdAt) }}</td></tr></tbody></table></div>
              <div class="idempotency-grid"><div><span>Idempotency-Key</span><strong class="mono">{{ evidence.idempotencyKey }}</strong></div><div><span>订单初始结果</span><strong>{{ requestResultLabel(evidence.requestResult) }}</strong></div></div>
              <p class="transaction-note">库存条件 UPDATE、订单、OrderItem 图片快照、库存 movement 与购物车清理处于同一事务。</p>
              <details class="developer-notes"><summary>开发演示信息</summary><p><code>GET /api/v1/operator/orders</code>、<code>GET /api/v1/operator/orders/{orderNo}</code> 与 <code>GET /api/v1/operator/orders/{orderNo}/execution-evidence</code> 提供页面事实；每次请求由后端 OperatorScope 授权。相同 Idempotency-Key 和相同请求体会返回原订单，不产生新订单、明细、库存扣减或 movement；相同 Key 不同请求体返回 409。</p></details>
            </section>
          </template>
        </section>
      </div>
    </template>
  </section>
</template>
