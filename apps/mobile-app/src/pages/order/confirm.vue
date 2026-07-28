<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
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
  catch (error) { state.value = 'error'; errorMessage.value = error instanceof ApiError ? error.message : '订单确认数据加载失败，请重试' }
}
async function createOrder() {
  if (!items.value.length || submitting.value) return
  if (!currentKey.value) currentKey.value = createClientIdempotencyKey()
  submitting.value = true; submitError.value = ''
  try {
    const response = await submitOrder(items.value, currentKey.value)
    uni.redirectTo({url: `/pages/order/result?orderNo=${encodeURIComponent(response.data.orderNo)}`})
    currentKey.value = ''
  } catch (error) {
    const apiError = error instanceof ApiError ? error : null
    if (apiError?.statusCode === 409) submitError.value = '提交状态冲突，请返回购物车重新确认。'
    else if (apiError?.body?.code === 'INVENTORY_INSUFFICIENT') submitError.value = '库存不足，请返回购物车调整数量。'
    else submitError.value = apiError?.message || '网络暂时不可用，请使用同一提交状态重试。'
  } finally { submitting.value = false }
}
function retry() { loadCart() }
function backToCart() { uni.navigateBack() }
onMounted(loadCart)
</script>

<template>
  <view class="mobile-page confirm-page">
    <view class="brand-bar"><view><text class="eyebrow">COMMERCEFLOW / CHECKOUT</text><text class="page-title">确认订单</text><text class="page-subtitle">本地演示用户 · 创建后状态为 CREATED</text></view><text class="brand-mark">{{ itemCount }} 件</text></view>
    <view v-if="state === 'loading'" class="panel empty-box">正在重新读取服务端购物车…</view>
    <view v-else-if="state === 'error'" class="error-box"><text>{{ errorMessage }}</text><button class="secondary-button retry-button" @click="retry">重新加载</button></view>
    <view v-else-if="state === 'empty'" class="panel empty-box"><text>购物车为空，请先选择商品。</text><button class="secondary-button retry-button" @click="backToCart">返回购物车</button></view>
    <template v-else>
      <view class="demo-note panel"><text class="note-title">本地演示用户</text><text class="muted">订单只使用 userId=1，不代表真实登录。</text></view>
      <view class="section-title"><text>商品明细</text><text class="muted">重新读取自服务端购物车</text></view>
      <view v-for="item in items" :key="item.id" class="confirm-item panel"><img v-if="imageUrl(item)" class="confirm-image" :src="imageUrl(item)" /><view v-else class="confirm-image image-fallback">暂无图片</view><view class="confirm-copy"><text class="cart-name">{{ item.productName }}</text><text class="sku-code">{{ item.skuCode }}</text><text class="cart-spec">{{ item.color }} / {{ item.size }} · 数量 {{ item.quantity }}</text><text class="cart-price">¥{{ item.unitPrice }} {{ item.currency }}</text></view><text class="line-total">¥{{ lineTotal(item) }}</text></view>
      <view class="amount-panel panel"><text class="muted">总金额</text><text class="amount">¥{{ total }} <text class="currency">CNY</text></text><text class="muted">最终金额以服务端创建订单结果为准</text></view>
      <view v-if="submitError" class="error-box"><text>{{ submitError }}</text><text v-if="currentKey" class="retry-key">本次提交状态已保留，可重试同一请求。</text></view>
      <view class="bottom-action"><button class="primary-button" :disabled="submitting" @click="createOrder">{{ submitting ? '订单创建中…' : '创建订单' }}</button></view>
    </template>
  </view>
</template>

<style>
.confirm-page{padding-bottom:180rpx}.demo-note{padding:18rpx 22rpx}.note-title{display:block;color:#2355c7;font-size:24rpx;font-weight:800}.confirm-item{display:flex;align-items:center;gap:16rpx;padding:16rpx;margin-bottom:14rpx}.confirm-image{width:112rpx;height:112rpx;flex:none;border-radius:12rpx;background:#f3f5f8}.confirm-copy{min-width:0;flex:1}.amount-panel{display:flex;align-items:flex-end;gap:14rpx;flex-wrap:wrap;padding:22rpx;margin-top:18rpx}.amount{color:#172033;font-size:38rpx;font-weight:800}.amount-panel .muted:last-child{width:100%}.retry-key{display:block;margin-top:12rpx;color:#a33f4b;font-size:20rpx}
</style>
