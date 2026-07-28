<script setup lang="ts">
import { onMounted, ref } from 'vue'
import MobileHeader from '../../components/MobileHeader.vue'
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
function openOrder(orderNo: string) { uni.navigateTo({ url: `/pages/order/detail?orderNo=${encodeURIComponent(orderNo)}` }) }
onMounted(load)
</script>

<template>
  <view class="mobile-page order-list-page"><MobileHeader eyebrow="COMMERCEFLOW / ORDERS" title="我的订单" subtitle="只展示真实创建订单" /><view class="summary-strip"><text class="summary-label">服务端订单</text><text class="summary-value">{{ orders.length }} 笔</text></view><view v-if="state === 'loading'" class="panel empty-box">正在读取订单列表…</view><view v-else-if="state === 'error'" class="error-box"><text>{{ errorMessage }}</text><button class="secondary-button retry-button" @click="load">重新加载</button></view><view v-else-if="state === 'empty'" class="panel empty-box">还没有已创建订单</view><view v-else><view v-for="order in orders" :key="order.orderNo" class="order-list-item panel" @click="openOrder(order.orderNo)"><view class="order-list-top"><text class="mono">{{ order.orderNo }}</text><text class="tag tag-success">已创建</text></view><text class="order-list-time">{{ order.createdAt }}</text><view class="order-list-bottom"><text>{{ order.items.length }} 个商品 · {{ order.currency }}</text><text class="order-list-amount">¥{{ order.totalAmount }}</text></view></view></view></view>
</template>

<style>
.order-list-page{padding-bottom:60px}.summary-strip{display:flex;justify-content:space-between;align-items:center;margin:10px 0 18px;padding:0 4px}.summary-label{color:var(--cf-muted);font-size:22px}.summary-value{color:var(--cf-ink);font-size:25px;font-weight:800}.order-list-item{padding:18px 20px;margin-bottom:14px}.order-list-top{display:flex;align-items:center;justify-content:space-between}.mono{font-family:monospace;font-size:19px}.order-list-time{display:block;margin-top:9px;color:var(--cf-muted);font-size:20px}.order-list-bottom{display:flex;justify-content:space-between;margin-top:16px;color:var(--cf-muted);font-size:21px}.order-list-amount{color:var(--cf-blue);font-size:26px;font-weight:800}
</style>
