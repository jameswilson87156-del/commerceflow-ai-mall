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
  catch (error) { state.value = 'error'; errorMessage.value = error instanceof ApiError ? error.message : '订单结果加载失败，请重试。' }
}
function openDetail() { if (order.value) uni.redirectTo({ url: `/pages/order/detail?orderNo=${encodeURIComponent(order.value.orderNo)}` }) }
function backToProducts() { uni.reLaunch({ url: '/pages/index/index' }) }
function retry() { if (orderNo.value) load(orderNo.value) }
onMounted(async () => { const { queryParam } = await import('../../platform/h5'); load(queryParam('orderNo')) })
</script>

<template>
  <view class="mobile-page result-page">
    <MobileHeader eyebrow="COMMERCEFLOW / RESULT" title="订单结果" subtitle="服务端已返回真实创建结果" />
    <view v-if="state === 'loading'" class="panel empty-box">正在读取真实订单结果...</view>
    <view v-else-if="state === 'error'" class="error-box"><text>{{ errorMessage }}</text><button class="secondary-button retry-button" @click="retry">重新加载</button></view>
    <view v-else-if="order" class="result-shell"><view class="success-mark">✓</view><text class="result-title">订单已创建</text><text class="result-subtitle">当前状态为 CREATED / 已创建</text><view class="result-card panel"><view class="result-row"><text class="muted">订单号</text><text class="mono">{{ order.orderNo }}</text></view><view class="result-row"><text class="muted">总金额</text><text class="result-amount">¥{{ order.totalAmount }} {{ order.currency }}</text></view><view class="result-row"><text class="muted">商品件数</text><text>{{ itemCount }} 件</text></view><view class="result-row"><text class="muted">创建时间</text><text class="result-value">{{ order.createdAt }}</text></view><view class="result-row"><text class="muted">状态</text><text class="tag tag-success">已创建</text></view></view><view class="result-actions"><button class="primary-button" @click="openDetail">查看订单详情</button><button class="secondary-button" @click="backToProducts">返回商品列表</button></view><text class="boundary-note">本页面不代表支付、发货或物流完成。</text></view>
  </view>
</template>

<style>
.result-page{min-height:100vh}.result-shell{width:100%;min-width:0;padding-top:26px;text-align:center}.success-mark{display:flex;align-items:center;justify-content:center;width:72px;height:72px;margin:0 auto 14px;border-radius:50%;background:var(--cf-green-soft);color:var(--cf-green);font-size:44px;font-weight:800}.result-title{display:block;color:var(--cf-ink);font-size:28px;font-weight:800}.result-subtitle{display:block;margin-top:7px;color:var(--cf-muted);font-size:15px}.result-card{width:100%;min-width:0;margin-top:20px;padding:6px 16px;text-align:left}.result-row{display:grid;grid-template-columns:88px minmax(0,1fr);align-items:center;gap:12px;padding:14px 0;border-bottom:1px solid var(--cf-line);font-size:15px}.result-row:last-child{border-bottom:0}.result-row>:last-child{text-align:right;min-width:0;overflow-wrap:anywhere}.result-amount{font-size:18px;font-weight:800}.mono{font-family:monospace;font-size:13px}.result-value{font-size:13px}.result-actions{display:grid;grid-template-columns:minmax(0,1fr) minmax(0,1fr);gap:10px;margin-top:16px}.result-actions button{min-width:0;padding-left:7px;padding-right:7px;font-size:14px}.boundary-note{display:block;margin:16px 4px 20px;color:var(--cf-muted);font-size:13px;line-height:1.45}
</style>
