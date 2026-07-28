<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import MobileHeader from '../../components/MobileHeader.vue'
import ProductImage from '../../components/ProductImage.vue'
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
  catch (error) { uni.showToast({ title: error instanceof ApiError ? error.message : '数量更新失败', icon: 'none' }) }
  finally { busyItemId.value = null }
}
async function removeItem(item: CartItem) {
  if (busyItemId.value) return
  busyItemId.value = item.id
  try { items.value = (await deleteCartItem(item.id)).data; state.value = items.value.length ? 'ready' : 'empty' }
  catch (error) { uni.showToast({ title: error instanceof ApiError ? error.message : '删除失败', icon: 'none' }) }
  finally { busyItemId.value = null }
}
function openConfirm() { uni.navigateTo({ url: '/pages/order/confirm' }) }
function openProducts() { uni.navigateBack() }
onMounted(loadCart)
</script>

<template>
  <view class="mobile-page cart-page">
    <MobileHeader eyebrow="COMMERCEFLOW / CART" title="购物车" subtitle="本地演示用户 · 服务端真实数据" action-label="继续逛" @action="openProducts" />
    <view class="summary-strip"><text class="summary-label">购物车商品</text><text class="summary-value">{{ count }} 件</text></view>
    <view v-if="state === 'loading'" class="panel empty-box">正在读取服务端购物车…</view>
    <view v-else-if="state === 'error'" class="error-box"><text>{{ errorMessage }}</text><button class="secondary-button retry-button" @click="loadCart">重新加载</button></view>
    <view v-else-if="state === 'empty'" class="panel empty-box"><text>购物车还是空的，先去挑选一件商品吧。</text><button class="secondary-button retry-button" @click="openProducts">去商品列表</button></view>
    <template v-else>
      <view class="demo-note panel"><text class="note-title">演示边界</text><text class="muted">商品、SKU、价格和库存均来自本地 API；这里只展示可执行的真实购物车操作。</text></view>
      <view v-for="item in items" :key="item.id" class="cart-item panel">
        <ProductImage class="cart-image" :src="imageUrl(item)" :alt="item.productName" />
        <view class="cart-copy"><text class="cart-name">{{ item.productName }}</text><text class="sku-code">{{ item.skuCode }}</text><text class="cart-spec">{{ item.color }} / {{ item.size }}</text><text class="cart-price">¥{{ item.unitPrice }} {{ item.currency }}</text><view class="quantity-row"><button class="quantity-button" :disabled="busyItemId === item.id || item.quantity <= 1" @click="changeQuantity(item, -1)">−</button><text class="quantity-value">{{ item.quantity }}</text><button class="quantity-button" :disabled="busyItemId === item.id || item.quantity >= item.availableStock" @click="changeQuantity(item, 1)">＋</button><button class="remove-button" :disabled="busyItemId === item.id" @click="removeItem(item)">删除</button></view></view>
        <text class="line-total">¥{{ lineTotal(item) }}</text>
      </view>
      <view class="cart-total panel"><view><text class="muted">订单预估总额</text><text class="total-value">¥{{ total }} <text class="currency">CNY</text></text></view><button class="primary-button total-button" @click="openConfirm">去确认订单</button></view>
    </template>
  </view>
</template>

<style>
.cart-page{padding-bottom:190px}.summary-strip{display:flex;justify-content:space-between;align-items:center;margin:10px 0 18px;padding:0 4px}.summary-label{color:var(--cf-muted);font-size:22px}.summary-value{color:var(--cf-ink);font-size:25px;font-weight:800}.demo-note{padding:18px 20px;margin-bottom:16px}.note-title{display:block;color:var(--cf-blue);font-size:23px;font-weight:800}.cart-item{display:flex;align-items:flex-start;gap:16px;padding:16px;margin-bottom:14px}.cart-image{width:116px;height:116px;flex:none;border-radius:12px}.cart-copy{min-width:0;flex:1}.cart-name{display:block;font-size:25px;font-weight:800;line-height:1.35}.sku-code{display:block;margin-top:6px;color:var(--cf-muted);font-family:monospace;font-size:18px}.cart-spec{display:block;margin-top:6px;color:var(--cf-muted);font-size:21px}.cart-price{display:block;margin-top:9px;color:var(--cf-blue);font-size:21px;font-weight:700}.line-total{flex:none;color:var(--cf-ink);font-size:23px;font-weight:800}.quantity-row{display:flex;align-items:center;gap:12px;margin-top:14px;color:var(--cf-muted);font-size:22px}.quantity-button{width:48px;height:48px;padding:0;background:var(--cf-soft);color:var(--cf-ink);font-size:27px;line-height:48px}.quantity-value{min-width:24px;text-align:center}.remove-button{margin-left:auto;padding:0 8px;background:transparent;color:var(--cf-red);font-size:20px}.cart-total{display:flex;align-items:center;justify-content:space-between;gap:14px;padding:18px 20px}.total-value{display:block;margin-top:7px;color:var(--cf-ink);font-size:34px;font-weight:800}.currency{color:var(--cf-muted);font-size:18px;font-weight:500}.total-button{min-width:0;flex:0 1 48%;padding-left:12px;padding-right:12px}
</style>
