<script setup lang="ts">
import { onMounted, ref } from 'vue'
import MobileHeader from '../../components/MobileHeader.vue'
import MobileBottomNav from '../../components/MobileBottomNav.vue'
import MobileStatusPill from '../../components/MobileStatusPill.vue'
import MobileMoney from '../../components/MobileMoney.vue'
import { getOrders } from '../../api/orders'
import { apiErrorMessage } from '../../api/runtime'
import { CLIENT_DATA_MODE } from '../../config/runtime'
import type { OrderSummary } from '../../api/types'

const orders = ref<OrderSummary[]>([])
const state = ref<'loading' | 'ready' | 'empty' | 'error'>('loading')
const errorMessage = ref('')
async function load() { state.value = 'loading'; errorMessage.value = ''; try { orders.value = (await getOrders()).data; state.value = orders.value.length ? 'ready' : 'empty' } catch (error) { state.value = 'error'; errorMessage.value = apiErrorMessage(error, '订单列表加载失败，请重试') } }
function openOrder(orderNo: string) { uni.navigateTo({ url: `/pages/order/detail?orderNo=${encodeURIComponent(orderNo)}` }) }
onMounted(load)
</script>

<template>
  <view class="mobile-page mobile-page--with-bottom-nav order-list-page"><MobileHeader eyebrow="COMMERCEFLOW / ORDERS" title="我的订单" :subtitle="`${CLIENT_DATA_MODE === 'LOCAL_DEMO_FIXTURE' ? 'Local Demo Fixture' : 'Real Backend'} · /api/v1/me 服务端范围`" /><view class="orders-intro"><view><text class="eyebrow">ORDER HISTORY</text><text class="orders-heading">你的真实订单</text></view><MobileStatusPill label="CREATED" tone="success" /></view><MobileNotice title="消费者接口边界" :message="`${CLIENT_DATA_MODE === 'LOCAL_DEMO_FIXTURE' ? '显式本地 Demo' : 'Real Backend'}：只读取服务端当前用户的订单，不提交 userId。`" /><view class="summary-strip"><text class="summary-label">服务端订单</text><text class="summary-value">{{ orders.length }} 笔</text></view><view v-if="state === 'loading'" class="panel empty-box">正在读取订单列表…</view><view v-else-if="state === 'error'" class="error-box"><text>{{ errorMessage }}</text><button class="secondary-button retry-button" type="button" @click="load">重新加载</button></view><view v-else-if="state === 'empty'" class="panel empty-box">还没有已创建订单</view><view v-else><button v-for="order in orders" :key="order.orderNo" class="order-list-item panel" type="button" @click="openOrder(order.orderNo)"><view class="order-list-top"><text class="mono">{{ order.orderNo }}</text><MobileStatusPill label="已创建" tone="success" /></view><text class="order-list-time">{{ order.createdAt }}</text><view class="order-list-bottom"><text>{{ order.items.length }} 个商品 · {{ order.currency }}</text><MobileMoney :amount="order.totalAmount" :currency="order.currency" compact /></view><text class="order-list-arrow" aria-hidden="true">↗</text></button></view><MobileBottomNav active="orders" /></view>
</template>

<style>
.order-list-page { background: #fbfaf7; }.order-list-page .mobile-header { margin-bottom: 27px; }
.orders-intro { display: block; margin: 0 0 26px; }.orders-heading { display: block; margin-top: 11px; color: #18231e; font-size: 31px; font-weight: 820; letter-spacing: -.05em; line-height: 1.1; }.orders-intro > .mobile-status-pill { display: none; }
.summary-strip { display: flex; justify-content: space-between; align-items: baseline; margin: 0 0 12px; border-top: 1px solid #dfe1dc; border-bottom: 1px solid #dfe1dc; padding: 12px 0; }.summary-label { color: #7d8780; font-size: 11px; }.summary-value { color: var(--cf-ink); font-size: 15px; font-weight: 850; }.order-list-item.panel { position: relative; display: block; width: 100%; margin: 0; border: 0; border-bottom: 1px solid #dfe1dc; border-radius: 0; background: transparent; padding: 16px 26px 16px 0; color: inherit; text-align: left; }.order-list-top { display: flex; align-items: center; justify-content: space-between; gap: 8px; }.mono { color: #43554c; font: 10px/1.2 ui-monospace, SFMono-Regular, Consolas, monospace; }.order-list-time { display: block; margin-top: 8px; color: #8b958e; font: 9px/1.2 ui-monospace, SFMono-Regular, Consolas, monospace; }.order-list-bottom { display: flex; align-items: baseline; justify-content: space-between; gap: 12px; margin-top: 13px; padding-top: 10px; border-top: 1px solid #eceeea; color: #68746d; font-size: 10px; }.order-list-bottom .mobile-money { color: var(--cf-blue); font-size: 15px; }.order-list-arrow { position: absolute; right: 2px; top: 50%; color: var(--cf-blue); font-size: 17px; transform: translateY(-50%); }

/* Apple-inspired order history pass: grouped rows, soft canvas, minimal metadata. */
.order-list-page { background: #f5f5f7; }
.orders-heading { color: #1d1d1f; font-size: 34px; font-weight: 700; letter-spacing: -.06em; }
.summary-strip { margin-bottom: 12px; border: 0; border-radius: 18px; background: #fff; padding: 15px 16px; }.summary-label { color: #1d1d1f; font-size: 13px; font-weight: 600; }.summary-value { color: #1d1d1f; font-size: 14px; font-weight: 600; }
.order-list-item.panel { margin: 0 0 12px; border: 0; border-radius: 20px; background: #fff; padding: 17px 38px 17px 16px; }
.mono { color: #1d1d1f; font-size: 10px; }.order-list-time { color: #6e6e73; font: 10px/1.2 -apple-system, BlinkMacSystemFont, "SF Pro Text", "PingFang SC", sans-serif; }.order-list-bottom { margin-top: 14px; border-top-color: #e8e8ed; color: #6e6e73; font-size: 11px; }.order-list-bottom .mobile-money { color: #1d1d1f; font-weight: 650; }.order-list-arrow { right: 16px; color: #0071e3; font-size: 18px; }
</style>
