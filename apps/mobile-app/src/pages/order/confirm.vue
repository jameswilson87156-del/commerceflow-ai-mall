<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import MobileHeader from '../../components/MobileHeader.vue'
import MobileNotice from '../../components/MobileNotice.vue'
import ProductImage from '../../components/ProductImage.vue'
import { getCart } from '../../api/cart'
import { submitOrder } from '../../api/orders'
import { ApiError, resolveImageUrl } from '../../api/runtime'
import { addCents, createClientIdempotencyKey } from '../../api/runtime-helpers.mjs'
import type { CartItem } from '../../api/types'

const items = ref<CartItem[]>([])
const state = ref<'loading' | 'ready' | 'empty' | 'error'>('loading')
const errorMessage = ref('')
const submitting = ref(false)
const submitError = ref('')
const currentKey = ref('')
const total = computed(() => items.value.reduce((sum, item) => addCents(sum, item.unitPrice, item.quantity), '0'))
const itemCount = computed(() => items.value.reduce((sum, item) => sum + item.quantity, 0))
function lineTotal(item: CartItem) { return addCents('0', item.unitPrice, item.quantity) }
function imageUrl(item: CartItem) { return resolveImageUrl(item.imagePath) }
async function loadCart() {
  state.value = 'loading'; errorMessage.value = ''; submitError.value = ''
  try { items.value = (await getCart()).data; state.value = items.value.length ? 'ready' : 'empty' }
  catch (error) { state.value = 'error'; errorMessage.value = error instanceof ApiError ? error.message : '订单确认数据加载失败，请重试。' }
}
async function createOrder() {
  if (!items.value.length || submitting.value) return
  if (!currentKey.value) currentKey.value = createClientIdempotencyKey()
  submitting.value = true; submitError.value = ''
  try {
    const response = await submitOrder(items.value, currentKey.value)
    uni.redirectTo({ url: `/pages/order/result?orderNo=${encodeURIComponent(response.data.orderNo)}` })
    currentKey.value = ''
  } catch (error) {
    const apiError = error instanceof ApiError ? error : null
    if (apiError?.statusCode === 409) submitError.value = '提交请求冲突，请返回购物车重新确认。'
    else if (apiError?.body?.code === 'INVENTORY_INSUFFICIENT') submitError.value = '库存不足，请返回购物车调整数量。'
    else submitError.value = apiError?.message || '网络暂时不可用，请使用同一提交状态重试。'
  } finally { submitting.value = false }
}
function backToCart() { uni.navigateBack() }
onMounted(loadCart)
</script>

<template>
  <view class="mobile-page mobile-page--with-bottom-action confirm-page">
    <MobileHeader eyebrow="COMMERCEFLOW / CHECKOUT" title="确认订单" subtitle="创建后状态为 CREATED" :back="true" @back="backToCart" />
    <view v-if="state === 'loading'" class="panel empty-box">正在重新读取服务端购物车...</view>
    <view v-else-if="state === 'error'" class="error-box"><text>{{ errorMessage }}</text><button class="secondary-button retry-button" @click="loadCart">重新加载</button></view>
    <view v-else-if="state === 'empty'" class="panel empty-box"><text>购物车为空，请先选择商品。</text><button class="secondary-button retry-button" @click="backToCart">返回购物车</button></view>
    <template v-else>
      <MobileNotice title="本地演示用户" message="订单仅使用 userId=1，不代表真实登录用户或真实交易。" />
      <view class="section-title"><text>商品明细</text><text class="muted">共 {{ itemCount }} 件</text></view>
      <view v-for="item in items" :key="item.id" class="confirm-item panel"><ProductImage class="confirm-image" :src="imageUrl(item)" :alt="item.productName" /><view class="confirm-copy"><text class="cart-name">{{ item.productName }}</text><text class="sku-code">{{ item.skuCode }}</text><text class="cart-spec">{{ item.color }} / {{ item.size }} · 数量 {{ item.quantity }}</text><view class="price-row"><text class="cart-price">¥{{ item.unitPrice }} {{ item.currency }}</text><text class="line-total">小计 ¥{{ lineTotal(item) }}</text></view></view></view>
      <view class="amount-panel panel"><text class="muted">总金额</text><text class="amount">¥{{ total }} <text class="currency">CNY</text></text><text class="amount-note">最终金额以服务端创建订单结果为准。</text></view>
      <MobileNotice v-if="submitError" tone="error" title="订单未创建" :message="submitError" />
      <view class="bottom-action confirm-action"><view><text class="muted">共 {{ itemCount }} 件</text><text class="confirm-total">¥{{ total }} CNY</text></view><button class="primary-button" :disabled="submitting" @click="createOrder">{{ submitting ? '订单创建中...' : '创建订单' }}</button></view>
    </template>
  </view>
</template>

<style>
.confirm-page .section-title{margin:16px 0 10px}.confirm-item.panel{display:flex;align-items:flex-start;gap:12px;padding:13px;margin-top:12px}.confirm-image{width:96px;height:96px;flex:none;border-radius:10px}.confirm-copy{display:block;min-width:0;flex:1}.price-row{display:flex;align-items:baseline;justify-content:space-between;gap:8px;margin-top:9px}.cart-name{display:block;color:var(--cf-ink);font-size:18px;font-weight:800;line-height:1.32}.sku-code{display:block;margin-top:4px;color:var(--cf-muted);font-family:monospace;font-size:12px;overflow-wrap:anywhere}.cart-spec{display:block;margin-top:4px;color:var(--cf-muted);font-size:14px}.cart-price{color:var(--cf-blue);font-size:15px;font-weight:700}.line-total{color:var(--cf-ink);font-size:14px;font-weight:800;white-space:nowrap}.amount-panel{padding:16px;margin-top:14px}.amount{display:block;margin-top:5px;color:var(--cf-ink);font-size:27px;font-weight:800}.currency{color:var(--cf-muted);font-size:12px;font-weight:500}.amount-note{display:block;margin-top:6px;color:var(--cf-muted);font-size:13px;line-height:1.42}.confirm-action{display:grid;grid-template-columns:minmax(0,1fr) minmax(136px,42%);align-items:center;gap:12px;width:100%}.confirm-action>view{display:block;min-width:0}.confirm-total{display:block;margin-top:2px;color:var(--cf-ink);font-size:19px;font-weight:800;line-height:1.15;white-space:nowrap}.confirm-action button{min-width:0;padding-left:8px;padding-right:8px;font-size:15px}
</style>
