<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { getOrder } from '../../api/orders'
import { ApiError, resolveImageUrl } from '../../api/runtime'
import { addCents } from '../../api/runtime-helpers.mjs'
import type { OrderSummary, OrderItem } from '../../api/types'

const order = ref<OrderSummary | null>(null)
const state = ref<'loading' | 'ready' | 'error'>('loading')
const errorMessage = ref('')
const orderNo = ref('')
async function load(no: string) {
  orderNo.value = no; state.value = 'loading'; errorMessage.value = ''
  try { order.value = (await getOrder(no)).data; state.value = 'ready' }
  catch (error) { state.value = 'error'; errorMessage.value = error instanceof ApiError ? error.message : '订单详情加载失败，请重试' }
}
function imageUrl(item: OrderItem) { return resolveImageUrl(item.imagePathSnapshot) }
function lineTotal(item: OrderItem) { return addCents('0', item.unitPrice, item.quantity) }
onMounted(async () => {
  const { queryParam } = await import('../../platform/h5')
  load(queryParam('orderNo'))
})
</script>

<template>
  <view class="mobile-page order-detail-page">
    <view v-if="state === 'loading'" class="panel empty-box">正在读取订单详情…</view>
    <view v-else-if="state === 'error'" class="error-box"><text>{{ errorMessage }}</text><button class="secondary-button retry-button" @click="load(orderNo)">重新加载</button></view>
    <template v-else-if="order"><view class="order-header panel"><view><text class="eyebrow">ORDER DETAIL</text><text class="order-no">{{ order.orderNo }}</text></view><text class="tag tag-success">已创建</text><text class="muted">CREATED · {{ order.createdAt }}</text></view><view class="section-title"><text>订单商品快照</text><text class="muted">历史下单时保存</text></view><view v-for="(item,index) in order.items" :key="`${item.skuCodeSnapshot}-${index}`" class="order-item panel"><img v-if="imageUrl(item)" class="order-image" :src="imageUrl(item)" /><view v-else class="order-image image-fallback">暂无历史图片</view><view class="order-copy"><text class="cart-name">{{ item.productNameSnapshot }}</text><text class="sku-code">{{ item.skuCodeSnapshot }}</text><text class="cart-spec">{{ item.colorSnapshot }} / {{ item.sizeSnapshot }} · 数量 {{ item.quantity }}</text><text class="cart-price">¥{{ item.unitPrice }} {{ order.currency }}</text></view><text class="line-total">¥{{ lineTotal(item) }}</text></view><view class="order-total panel"><text class="muted">订单总额</text><text class="total-value">¥{{ order.totalAmount }} <text class="currency">{{ order.currency }}</text></text></view></template>
  </view>
</template>

<style>
.order-detail-page{padding-bottom:80rpx}.order-header{display:flex;align-items:center;gap:14rpx;flex-wrap:wrap;padding:22rpx}.order-header .muted{width:100%}.order-no{display:block;margin-top:8rpx;font-family:monospace;font-size:28rpx;font-weight:800}.order-item{display:flex;align-items:center;gap:16rpx;padding:16rpx;margin-bottom:14rpx}.order-image{width:112rpx;height:112rpx;flex:none;border-radius:12rpx;background:#f3f5f8}.order-copy{min-width:0;flex:1}.order-total{display:flex;align-items:flex-end;justify-content:space-between;padding:20rpx 24rpx;margin-top:20rpx}
</style>
