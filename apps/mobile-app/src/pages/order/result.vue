<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import MobileHeader from '../../components/MobileHeader.vue'
import { getOrder } from '../../api/orders'
import { ApiError } from '../../api/runtime'
import type { OrderSummary } from '../../api/types'

const order = ref<OrderSummary | null>(null)
const state = ref<'loading' | 'ready' | 'error'>('loading')
const errorMessage = ref('')
const orderNo = ref('')
const itemCount = computed(() => order.value?.items.reduce((sum, item) => sum + item.quantity, 0) || 0)
async function load(requestedOrderNo: string) {
  orderNo.value = requestedOrderNo; state.value = 'loading'; errorMessage.value = ''
  try { order.value = (await getOrder(orderNo.value)).data; state.value = 'ready' }
  catch (error) { state.value = 'error'; errorMessage.value = error instanceof ApiError ? error.message : '订单结果加载失败，请重试' }
}
function openDetail() { if (order.value) uni.redirectTo({ url: `/pages/order/detail?orderNo=${encodeURIComponent(order.value.orderNo)}` }) }
function backToProducts() { uni.reLaunch({ url: '/pages/index/index' }) }
function retry() { if (orderNo.value) load(orderNo.value) }
onMounted(async () => {
  const { queryParam } = await import('../../platform/h5')
  load(queryParam('orderNo'))
})
</script>

<template>
  <view class="mobile-page result-page">
    <MobileHeader eyebrow="COMMERCEFLOW / RESULT" title="订单结果" subtitle="服务端已返回真实创建结果" />
    <view v-if="state === 'loading'" class="panel empty-box">正在读取真实订单结果…</view>
    <view v-else-if="state === 'error'" class="error-box"><text>{{ errorMessage }}</text><button class="secondary-button retry-button" @click="retry">重新加载</button></view>
    <view v-else-if="order" class="result-shell"><view class="success-mark">✓</view><text class="result-title">订单已创建</text><text class="result-subtitle">当前状态为 CREATED / 已创建</text><view class="result-card panel"><view class="result-row"><text class="muted">订单号</text><text class="mono">{{ order.orderNo }}</text></view><view class="result-row"><text class="muted">总金额</text><text class="result-amount">¥{{ order.totalAmount }} {{ order.currency }}</text></view><view class="result-row"><text class="muted">商品件数</text><text>{{ itemCount }} 件</text></view><view class="result-row"><text class="muted">创建时间</text><text>{{ order.createdAt }}</text></view><view class="result-row"><text class="muted">状态</text><text class="tag tag-success">已创建</text></view></view><view class="result-actions"><button class="primary-button" @click="openDetail">查看订单详情</button><button class="secondary-button" @click="backToProducts">返回商品列表</button></view><text class="boundary-note">本页面不代表支付、发货或物流完成。</text></view>
  </view>
</template>

<style>
.result-page{min-height:100vh}.result-shell{width:100%;padding-top:42px;text-align:center}.success-mark{display:flex;align-items:center;justify-content:center;width:88px;height:88px;margin:0 auto 20px;border-radius:50%;background:var(--cf-green-soft);color:var(--cf-green);font-size:55px;font-weight:800}.result-title{display:block;color:var(--cf-ink);font-size:36px;font-weight:800}.result-subtitle{display:block;margin-top:10px;color:var(--cf-muted);font-size:22px}.result-card{margin-top:28px;padding:8px 20px;text-align:left}.result-row{display:flex;justify-content:space-between;gap:18px;padding:17px 0;border-bottom:1px solid var(--cf-line);font-size:21px}.result-row:last-child{border-bottom:0}.result-amount{font-size:26px;font-weight:800}.mono{font-family:monospace;font-size:19px}.result-actions{display:flex;gap:12px;margin-top:18px}.result-actions button{flex:1;min-width:0;padding-left:8px;padding-right:8px;font-size:20px}.boundary-note{display:block;margin-top:18px;color:var(--cf-muted);font-size:19px}
</style>
