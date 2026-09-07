<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import MobileHeader from '../../components/MobileHeader.vue'
import MobileStatusPill from '../../components/MobileStatusPill.vue'
import MobileMoney from '../../components/MobileMoney.vue'
import ProductImage from '../../components/ProductImage.vue'
import { getOrder } from '../../api/orders'
import { apiErrorMessage, resolveImageUrl } from '../../api/runtime'
import { addCents } from '../../api/runtime-helpers.mjs'
import { CLIENT_DATA_MODE } from '../../config/runtime'
import type { OrderSummary, OrderItem } from '../../api/types'

const order = ref<OrderSummary | null>(null)
const state = ref<'loading' | 'ready' | 'error'>('loading')
const errorMessage = ref('')
const orderNo = ref('')
const itemCount = computed(() => order.value?.items.reduce((sum, item) => sum + item.quantity, 0) || 0)
async function load(no: string) { orderNo.value = no; state.value = 'loading'; errorMessage.value = ''; try { order.value = (await getOrder(no)).data; state.value = 'ready' } catch (error) { state.value = 'error'; errorMessage.value = apiErrorMessage(error, '订单详情加载失败，请重试。') } }
function imageUrl(item: OrderItem) { return resolveImageUrl(item.imagePathSnapshot) }
function lineTotal(item: OrderItem) { return addCents('0', item.unitPrice, item.quantity) }
function goBack() { uni.navigateBack() }
onMounted(async () => { const { queryParam } = await import('../../platform/h5'); load(queryParam('orderNo')) })
</script>

<template>
  <view class="mobile-page order-detail-page">
    <MobileHeader eyebrow="COMMERCEFLOW / ORDER" title="订单详情" :subtitle="`${CLIENT_DATA_MODE === 'LOCAL_DEMO_FIXTURE' ? 'Local Demo Fixture' : 'Real Backend'} · 读取创建时保存的商品快照`" :back="true" @back="goBack" />
    <view v-if="state === 'loading'" class="panel empty-box">正在读取订单详情...</view>
    <view v-else-if="state === 'error'" class="error-box"><text>{{ errorMessage }}</text><button class="secondary-button retry-button" type="button" @click="load(orderNo)">重新加载</button></view>
    <template v-else-if="order">
      <view class="order-header panel"><view class="order-header-main"><text class="eyebrow">ORDER DETAIL</text><text class="order-no">{{ order.orderNo }}</text></view><MobileStatusPill label="CREATED / 已创建" tone="success" /><text class="order-created">创建时间 · {{ order.createdAt }}</text></view>
      <view class="order-stat-strip"><view><text>商品件数</text><strong>{{ itemCount }}</strong></view><view><text>币种</text><strong>{{ order.currency }}</strong></view><view><text>服务端快照</text><strong>已保存</strong></view></view>
      <view class="section-title section-title-stack"><text>订单商品快照</text><text class="muted">历史下单时保存</text></view>
      <view v-for="(item,index) in order.items" :key="`${item.skuCodeSnapshot}-${index}`" class="order-item panel"><ProductImage class="order-image" :src="imageUrl(item)" :alt="item.productNameSnapshot" placeholder="暂无历史图片" /><view class="order-copy"><text class="item-name">{{ item.productNameSnapshot }}</text><text class="sku-code">{{ item.skuCodeSnapshot }}</text><text class="item-spec">{{ item.colorSnapshot }} / {{ item.sizeSnapshot }}</text><view class="item-values"><text>数量 {{ item.quantity }}</text><MobileMoney :amount="item.unitPrice" :currency="order.currency" compact /><text class="item-subtotal">小计 ¥{{ lineTotal(item) }}</text></view></view></view>
      <view class="order-total panel"><text class="muted">订单总额</text><MobileMoney :amount="order.totalAmount" :currency="order.currency" /></view>
      <text class="boundary-note">商品、规格、价格和图片均为订单创建时的历史快照；本页不代表物流或支付完成。</text>
    </template>
  </view>
</template>

<style>
.order-detail-page { background: #fbfaf7; }.order-detail-page .mobile-header { margin-bottom: 26px; }
.order-header.panel { display: grid; grid-template-columns: minmax(0, 1fr) auto; gap: 10px; margin: 0; border: 0; border-top: 1px solid #dfe1dc; border-bottom: 1px solid #dfe1dc; border-radius: 0; background: transparent; padding: 15px 0 16px; }.order-header-main { min-width: 0; }.order-no { display: block; margin-top: 7px; color: var(--cf-ink); font: 850 13px/1.3 ui-monospace, SFMono-Regular, Consolas, monospace; overflow-wrap: anywhere; }.order-created { grid-column: 1 / -1; color: #89938c; font: 9px/1.42 ui-monospace, SFMono-Regular, Consolas, monospace; overflow-wrap: anywhere; }.order-stat-strip { display: grid; grid-template-columns: repeat(3, minmax(0, 1fr)); gap: 0; margin-top: 18px; border-bottom: 1px solid #dfe1dc; }.order-stat-strip view { display: grid; gap: 5px; min-width: 0; border-right: 1px solid #dfe1dc; padding: 0 10px 13px 0; }.order-stat-strip view + view { padding-left: 10px; }.order-stat-strip view:last-child { border-right: 0; }.order-stat-strip text { color: #89938c; font: 9px/1.25 ui-monospace, SFMono-Regular, Consolas, monospace; }.order-stat-strip strong { color: var(--cf-ink); font-size: 13px; }.section-title-stack { align-items: flex-start; margin: 25px 0 10px; }.section-title-stack .muted { display: block; margin-top: 4px; font-size: 10px; }.order-item.panel { display: grid; grid-template-columns: 76px minmax(0, 1fr); align-items: start; gap: 12px; margin: 0; border: 0; border-bottom: 1px solid #dfe1dc; border-radius: 0; background: transparent; padding: 15px 0 17px; }.order-image { width: 76px; height: 76px; flex: none; border-radius: 0; background: #f0f0ec; }.order-copy { display: block; min-width: 0; }.item-name { display: block; color: var(--cf-ink); font-size: 15px; font-weight: 850; line-height: 1.3; }.sku-code { display: block; margin-top: 5px; color: #8b958e; font: 9px/1.2 ui-monospace, SFMono-Regular, Consolas, monospace; overflow-wrap: anywhere; }.item-spec { display: block; margin-top: 7px; color: #68746d; font-size: 11px; }.item-values { display: grid; grid-template-columns: minmax(0, 1fr) minmax(0, 1fr); gap: 5px 10px; align-items: baseline; margin-top: 10px; color: #68746d; font: 10px/1.35 ui-monospace, SFMono-Regular, Consolas, monospace; }.item-values .mobile-money { color: var(--cf-blue); font-size: 15px; }.item-subtotal { grid-column: 1 / -1; color: var(--cf-ink); font-size: 11px; font-weight: 850; }.order-total.panel { display: flex; align-items: baseline; justify-content: space-between; gap: 12px; margin-top: 27px; border: 0; border-top: 1px solid #dfe1dc; border-bottom: 1px solid #dfe1dc; border-radius: 0; background: transparent; padding: 15px 0 16px; }.order-total .mobile-money { color: var(--cf-blue); }.boundary-note { display: block; margin: 14px 4px 20px; color: #89938c; font: 9px/1.45 ui-monospace, SFMono-Regular, Consolas, monospace; }

/* Apple-inspired order pass: history is easy to scan and the evidence remains legible. */
.order-detail-page { background: #f5f5f7; }
.order-header.panel { margin: 0 0 12px; border: 0; border-radius: 20px; background: #fff; padding: 18px; }
.order-no { color: #1d1d1f; font-weight: 600; }
.order-created { color: #6e6e73; font: 10px/1.4 -apple-system, BlinkMacSystemFont, "SF Pro Text", "PingFang SC", sans-serif; }
.order-stat-strip { margin-top: 0; border: 0; border-radius: 20px; background: #fff; padding: 17px 16px; }
.order-stat-strip view { border-right-color: #e8e8ed; padding-bottom: 0; }.order-stat-strip view + view { padding-left: 12px; }.order-stat-strip text { color: #6e6e73; font: 10px/1.25 -apple-system, BlinkMacSystemFont, "SF Pro Text", "PingFang SC", sans-serif; }.order-stat-strip strong { color: #1d1d1f; font-size: 14px; font-weight: 600; }
.section-title-stack { margin-top: 24px; }
.order-item.panel { margin: 0 0 12px; border: 0; border-radius: 20px; background: #fff; padding: 14px; }
.order-image { border-radius: 14px; background: #f5f5f7; }
.item-name { color: #1d1d1f; font-size: 16px; font-weight: 600; }
.item-spec { color: #6e6e73; }
.item-values { color: #6e6e73; font-family: -apple-system, BlinkMacSystemFont, "SF Pro Text", "PingFang SC", sans-serif; }
.item-values .mobile-money { color: #1d1d1f; font-weight: 600; }.item-subtotal { color: #1d1d1f; font-family: -apple-system, BlinkMacSystemFont, "SF Pro Text", "PingFang SC", sans-serif; font-weight: 600; }
.order-total.panel { margin-top: 18px; border: 0; border-radius: 20px; background: #fff; padding: 18px; }.order-total .mobile-money { color: #1d1d1f; font-weight: 650; }
.boundary-note { color: #6e6e73; font: 10px/1.45 -apple-system, BlinkMacSystemFont, "SF Pro Text", "PingFang SC", sans-serif; }
</style>
