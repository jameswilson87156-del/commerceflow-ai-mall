<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { getCart, updateCartItem, deleteCartItem } from '../../api/cart'
import { ApiError, resolveImageUrl } from '../../api/runtime'
import { addCents } from '../../api/runtime-helpers.mjs'
import type { CartItem } from '../../api/types'

const items = ref<CartItem[]>([])
const state = ref<'loading' | 'ready' | 'empty' | 'error'>('loading')
const errorMessage = ref('')
const busyItemId = ref<number | null>(null)
const total = computed(() => items.value.reduce((sum, item) => addCents(sum, item.unitPrice, item.quantity), '0'))
const count = computed(() => items.value.reduce((sum, item) => sum + item.quantity, 0))
function lineTotal(item: CartItem) { return addCents('0', item.unitPrice, item.quantity) }
function imageUrl(item: CartItem) { return resolveImageUrl(item.imagePath) }
async function loadCart() {
  state.value = 'loading'; errorMessage.value = ''
  try { items.value = (await getCart()).data; state.value = items.value.length ? 'ready' : 'empty' }
  catch (error) { state.value = 'error'; errorMessage.value = error instanceof ApiError ? error.message : '购物车加载失败，请重试' }
}
async function changeQuantity(item: CartItem, delta: number) {
  const next = item.quantity + delta
  if (next < 1 || busyItemId.value) return
  busyItemId.value = item.id
  try { items.value = (await updateCartItem(item.id, next)).data; state.value = items.value.length ? 'ready' : 'empty' }
  catch (error) { uni.showToast({title: error instanceof ApiError ? error.message : '数量更新失败', icon: 'none'}) }
  finally { busyItemId.value = null }
}
async function removeItem(item: CartItem) {
  if (busyItemId.value) return
  busyItemId.value = item.id
  try { items.value = (await deleteCartItem(item.id)).data; state.value = items.value.length ? 'ready' : 'empty' }
  catch (error) { uni.showToast({title: error instanceof ApiError ? error.message : '删除失败', icon: 'none'}) }
  finally { busyItemId.value = null }
}
function openConfirm() { uni.navigateTo({url: '/pages/order/confirm'}) }
function retry() { loadCart() }
onMounted(loadCart)
</script>

<template>
  <view class="mobile-page cart-page">
    <view class="brand-bar"><view><text class="eyebrow">COMMERCEFLOW / CART</text><text class="page-title">购物车</text><text class="page-subtitle">本地演示用户 · 服务端真实数据</text></view><text class="brand-mark">{{ count }} 件</text></view>
    <view v-if="state === 'loading'" class="panel empty-box">正在读取服务端购物车…</view>
    <view v-else-if="state === 'error'" class="error-box"><text>{{ errorMessage }}</text><button class="secondary-button retry-button" @click="retry">重新加载</button></view>
    <view v-else-if="state === 'empty'" class="panel empty-box">购物车还是空的，先去挑选一件商品吧。</view>
    <template v-else>
      <view v-for="item in items" :key="item.id" class="cart-item panel"><img v-if="imageUrl(item.imagePath)" class="cart-image" :src="imageUrl(item.imagePath)" /><view v-else class="cart-image image-fallback">暂无图片</view><view class="cart-copy"><text class="cart-name">{{ item.productName }}</text><text class="sku-code">{{ item.skuCode }}</text><text class="cart-spec">{{ item.color }} / {{ item.size }}</text><text class="cart-price">¥{{ item.unitPrice }} {{ item.currency }}</text><view class="quantity-row"><button class="quantity-button" :disabled="busyItemId === item.id || item.quantity <= 1" @click="changeQuantity(item, -1)">−</button><text>{{ item.quantity }}</text><button class="quantity-button" :disabled="busyItemId === item.id || item.quantity >= item.availableStock" @click="changeQuantity(item, 1)">＋</button><button class="remove-button" :disabled="busyItemId === item.id" @click="removeItem(item)">删除</button></view></view><text class="line-total">¥{{ lineTotal(item) }}</text></view>
      <view class="cart-total panel"><view><text class="muted">订单预估总额</text><text class="total-value">¥{{ total }} <text class="currency">CNY</text></text></view><button class="primary-button total-button" @click="openConfirm">去确认订单</button></view>
    </template>
  </view>
</template>

<style>
.cart-page{padding-bottom:180rpx}.cart-item{display:flex;gap:16rpx;padding:18rpx;margin-bottom:14rpx}.cart-image{width:116rpx;height:116rpx;flex:none;border-radius:12rpx}.cart-copy{min-width:0;flex:1}.cart-name{display:block;font-size:25rpx;font-weight:800}.cart-spec{display:block;margin-top:7rpx;color:#6f7b8f;font-size:21rpx}.cart-price{display:block;margin-top:10rpx;color:#2563eb;font-size:21rpx;font-weight:700}.line-total{font-size:24rpx;font-weight:800}.quantity-row{display:flex;align-items:center;gap:14rpx;margin-top:15rpx;color:#4c596c;font-size:23rpx}.quantity-button{width:48rpx;height:48rpx;padding:0;background:#edf2f8;color:#253653;font-size:28rpx;line-height:48rpx}.remove-button{margin-left:auto;padding:0 8rpx;background:transparent;color:#b04350;font-size:20rpx}.cart-total{display:flex;align-items:center;justify-content:space-between;gap:16rpx;padding:20rpx 24rpx}.total-value{display:block;margin-top:8rpx;color:#172033;font-size:35rpx;font-weight:800}.currency{color:#7b8798;font-size:19rpx;font-weight:500}.total-button{min-width:230rpx}
</style>
