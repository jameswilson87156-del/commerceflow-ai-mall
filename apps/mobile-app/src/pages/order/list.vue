<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { getOrders } from '../../api/orders'
import { ApiError } from '../../api/runtime'
import type { OrderSummary } from '../../api/types'

const orders = ref<OrderSummary[]>([])
const state = ref<'loading' | 'ready' | 'empty' | 'error'>('loading')
const errorMessage = ref('')
async function load() {
  state.value = 'loading'; errorMessage.value = ''
  try { orders.value = (await getOrders()).data; state.value = orders.value.length ? 'ready' : 'empty' }
  catch (error) { state.value = 'error'; errorMessage.value = error instanceof ApiError ? error.message : '订单列表加载失败，请重试' }
}
function openOrder(orderNo: string) { uni.navigateTo({url: `/pages/order/detail?orderNo=${encodeURIComponent(orderNo)}`}) }
function retry() { load() }
onMounted(load)
</script>

<template>
  <view class="mobile-page order-list-page"><view class="brand-bar"><view><text class="eyebrow">COMMERCEFLOW / ORDERS</text><text class="page-title">我的订单</text><text class="page-subtitle">本地演示用户 · 只展示真实创建订单</text></view><text class="brand-mark">{{ orders.length }} 笔</text></view><view v-if="state === 'loading'" class="panel empty-box">正在读取订单列表…</view><view v-else-if="state === 'error'" class="error-box"><text>{{ errorMessage }}</text><button class="secondary-button retry-button" @click="retry">重新加载</button></view><view v-else-if="state === 'empty'" class="panel empty-box">还没有已创建订单</view><view v-else><view v-for="order in orders" :key="order.orderNo" class="order-list-item panel" @click="openOrder(order.orderNo)"><view class="order-list-top"><text class="mono">{{ order.orderNo }}</text><text class="tag tag-success">已创建</text></view><text class="order-list-time">{{ order.createdAt }}</text><view class="order-list-bottom"><text>{{ order.items.length }} 个商品 · {{ order.currency }}</text><text class="order-list-amount">¥{{ order.totalAmount }}</text></view></view></view></view>
</template>

<style>
.order-list-item{padding:20rpx 22rpx;margin-bottom:14rpx}.order-list-top{display:flex;align-items:center;justify-content:space-between}.mono{font-family:monospace;font-size:20rpx}.order-list-time{display:block;margin-top:10rpx;color:#7b8798;font-size:20rpx}.order-list-bottom{display:flex;justify-content:space-between;margin-top:18rpx;color:#657287;font-size:21rpx}.order-list-amount{color:#2563eb;font-size:27rpx;font-weight:800}
</style>
