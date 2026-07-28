<script setup lang="ts">
import { onMounted, ref } from 'vue'
import MobileHeader from '../../components/MobileHeader.vue'
import ProductImage from '../../components/ProductImage.vue'
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
  catch (error) { state.value = 'error'; errorMessage.value = error instanceof ApiError ? error.message : '订单详情加载失败，请重试。' }
}
function imageUrl(item: OrderItem) { return resolveImageUrl(item.imagePathSnapshot) }
function lineTotal(item: OrderItem) { return addCents('0', item.unitPrice, item.quantity) }
function goBack() { uni.navigateBack() }
onMounted(async () => { const { queryParam } = await import('../../platform/h5'); load(queryParam('orderNo')) })
</script>

<template>
  <view class="mobile-page order-detail-page">
    <MobileHeader eyebrow="COMMERCEFLOW / ORDER" title="订单详情" subtitle="读取创建时保存的商品快照" :back="true" @back="goBack" />
    <view v-if="state === 'loading'" class="panel empty-box">正在读取订单详情...</view>
    <view v-else-if="state === 'error'" class="error-box"><text>{{ errorMessage }}</text><button class="secondary-button retry-button" @click="load(orderNo)">重新加载</button></view>
    <template v-else-if="order">
      <view class="order-header panel"><view class="order-header-main"><text class="eyebrow">ORDER DETAIL</text><text class="order-no">{{ order.orderNo }}</text></view><text class="tag tag-success">已创建</text><text class="order-created">CREATED · {{ order.createdAt }}</text></view>
      <view class="section-title section-title-stack"><text>订单商品快照</text><text class="muted">历史下单时保存</text></view>
      <view v-for="(item,index) in order.items" :key="`${item.skuCodeSnapshot}-${index}`" class="order-item panel"><ProductImage class="order-image" :src="imageUrl(item)" :alt="item.productNameSnapshot" placeholder="暂无历史图片" /><view class="order-copy"><text class="item-name">{{ item.productNameSnapshot }}</text><text class="sku-code">{{ item.skuCodeSnapshot }}</text><text class="item-spec">{{ item.colorSnapshot }} / {{ item.sizeSnapshot }}</text><view class="item-values"><text>数量 {{ item.quantity }}</text><text>单价 ¥{{ item.unitPrice }} {{ order.currency }}</text><text class="item-subtotal">小计 ¥{{ lineTotal(item) }}</text></view></view></view>
      <view class="order-total panel"><text class="muted">订单总额</text><text class="total-value">¥{{ order.totalAmount }} <text class="currency">{{ order.currency }}</text></text></view>
    </template>
  </view>
</template>

<style>
.order-detail-page{padding-bottom:32px}.order-header.panel{display:grid;grid-template-columns:minmax(0,1fr) auto;gap:10px;padding:16px}.order-header-main{min-width:0}.order-no{display:block;margin-top:6px;font-family:monospace;font-size:17px;font-weight:800;overflow-wrap:anywhere}.order-created{grid-column:1 / -1;color:var(--cf-muted);font-size:13px;line-height:1.42;overflow-wrap:anywhere}.section-title-stack{align-items:flex-start;margin:17px 0 10px}.section-title-stack .muted{display:block;margin-top:3px;font-size:13px}.order-item.panel{display:flex;align-items:flex-start;gap:12px;padding:13px;margin-top:12px}.order-image{width:92px;height:92px;flex:none;border-radius:10px}.order-copy{display:block;min-width:0;flex:1}.item-name{display:block;color:var(--cf-ink);font-size:18px;font-weight:800;line-height:1.3}.sku-code{display:block;margin-top:4px;color:var(--cf-muted);font-family:monospace;font-size:12px;overflow-wrap:anywhere}.item-spec{display:block;margin-top:4px;color:var(--cf-muted);font-size:14px}.item-values{display:grid;grid-template-columns:minmax(0,1fr) minmax(0,1fr);gap:5px 10px;margin-top:9px;color:#4d5f78;font-size:13px;line-height:1.35}.item-subtotal{grid-column:1 / -1;color:var(--cf-ink);font-weight:800}.order-total.panel{display:flex;align-items:flex-end;justify-content:space-between;gap:12px;padding:16px;margin-top:15px}.total-value{color:var(--cf-ink);font-size:25px;font-weight:800}.currency{color:var(--cf-muted);font-size:12px;font-weight:500}
</style>
