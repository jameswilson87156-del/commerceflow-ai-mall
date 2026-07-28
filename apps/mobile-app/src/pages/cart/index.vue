<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import MobileHeader from '../../components/MobileHeader.vue'
import MobileNotice from '../../components/MobileNotice.vue'
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
  catch (error) { state.value = 'error'; errorMessage.value = error instanceof ApiError ? error.message : '购物车加载失败，请重试。' }
}
async function changeQuantity(item: CartItem, delta: number) {
  const next = item.quantity + delta
  if (next < 1 || busyItemId.value) return
  busyItemId.value = item.id
  try { items.value = (await updateCartItem(item.id, next)).data; state.value = items.value.length ? 'ready' : 'empty' }
  catch (error) { errorMessage.value = error instanceof ApiError ? error.message : '数量更新失败，请重试。' }
  finally { busyItemId.value = null }
}
async function removeItem(item: CartItem) {
  if (busyItemId.value) return
  busyItemId.value = item.id
  try { items.value = (await deleteCartItem(item.id)).data; state.value = items.value.length ? 'ready' : 'empty' }
  catch (error) { errorMessage.value = error instanceof ApiError ? error.message : '删除失败，请重试。' }
  finally { busyItemId.value = null }
}
function openConfirm() { uni.navigateTo({ url: '/pages/order/confirm' }) }
function openProducts() { uni.navigateBack() }
onMounted(loadCart)
</script>

<template>
  <view class="mobile-page mobile-page--with-bottom-action cart-page">
    <MobileHeader eyebrow="COMMERCEFLOW / CART" title="购物车" subtitle="本地演示用户 · 服务端真实数据" action-label="继续选购" @action="openProducts" />
    <view class="summary-strip"><text class="summary-label">购物车商品</text><text class="summary-value">{{ count }} 件</text></view>
    <view v-if="state === 'loading'" class="panel empty-box">正在读取服务端购物车...</view>
    <view v-else-if="state === 'error'" class="error-box"><text>{{ errorMessage }}</text><button class="secondary-button retry-button" @click="loadCart">重新加载</button></view>
    <view v-else-if="state === 'empty'" class="panel empty-box"><text>购物车还是空的，先去挑选一件商品吧。</text><button class="secondary-button retry-button" @click="openProducts">去商品列表</button></view>
    <template v-else>
      <MobileNotice title="演示边界" message="商品、SKU、价格和库存均来自本地 API；这里只展示可执行的真实购物车操作。" />
      <view v-for="item in items" :key="item.id" class="cart-item panel">
        <ProductImage class="cart-image" :src="imageUrl(item)" :alt="item.productName" />
        <view class="cart-copy">
          <text class="cart-name">{{ item.productName }}</text><text class="sku-code">{{ item.skuCode }}</text><text class="cart-spec">{{ item.color }} / {{ item.size }}</text>
          <view class="price-row"><text class="cart-price">¥{{ item.unitPrice }} {{ item.currency }}</text><text class="line-total">小计 ¥{{ lineTotal(item) }}</text></view>
          <view class="cart-actions"><view class="quantity-control"><button class="quantity-button" :disabled="busyItemId === item.id || item.quantity <= 1" @click="changeQuantity(item, -1)">−</button><text class="quantity-value">{{ item.quantity }}</text><button class="quantity-button" :disabled="busyItemId === item.id || item.quantity >= item.availableStock" @click="changeQuantity(item, 1)">+</button></view><button class="remove-button" :disabled="busyItemId === item.id" @click="removeItem(item)">删除</button></view>
        </view>
      </view>
      <view v-if="errorMessage" class="error-box cart-inline-error"><text>{{ errorMessage }}</text></view>
      <view class="bottom-action cart-checkout"><view class="checkout-copy"><text class="muted">订单预计总额</text><text class="total-value">¥{{ total }} <text class="currency">CNY</text></text><text class="checkout-count">共 {{ count }} 件真实演示商品</text></view><button class="primary-button total-button" @click="openConfirm">去确认订单</button></view>
    </template>
  </view>
</template>

<style>
.summary-strip{display:flex;justify-content:space-between;align-items:center;margin:10px 0 14px;padding:0 2px}.summary-label{color:var(--cf-muted);font-size:16px}.summary-value{color:var(--cf-ink);font-size:18px;font-weight:800}.cart-item.panel{display:flex;align-items:flex-start;gap:12px;padding:13px;margin-top:12px}.cart-image{width:96px;height:96px;flex:none;border-radius:10px}.cart-copy{display:block;min-width:0;flex:1}.cart-name{display:block;color:var(--cf-ink);font-size:18px;font-weight:800;line-height:1.32}.sku-code{display:block;margin-top:4px;color:var(--cf-muted);font-family:monospace;font-size:12px;overflow-wrap:anywhere}.cart-spec{display:block;margin-top:4px;color:var(--cf-muted);font-size:14px}.price-row{display:flex;align-items:baseline;justify-content:space-between;gap:8px;margin-top:8px}.cart-price{color:var(--cf-blue);font-size:15px;font-weight:700}.line-total{color:var(--cf-ink);font-size:14px;font-weight:800;white-space:nowrap}.cart-actions{display:flex;align-items:center;justify-content:space-between;gap:10px;margin-top:11px}.quantity-control{display:grid;grid-template-columns:42px minmax(32px,1fr) 42px;align-items:center;min-width:132px;max-width:170px;border:1px solid var(--cf-line);border-radius:10px;overflow:hidden;background:var(--cf-soft)}.quantity-button{min-width:42px;height:42px;padding:0;border-radius:0;background:transparent;color:var(--cf-ink);font-size:23px;line-height:42px}.quantity-value{min-width:0;text-align:center;color:var(--cf-ink);font-size:16px;font-weight:800}.remove-button{min-width:52px;padding:8px 0;background:transparent;color:var(--cf-red);font-size:14px}.cart-inline-error{margin-top:12px}.cart-checkout{display:grid;grid-template-columns:minmax(0,1fr) minmax(128px,42%);align-items:center;gap:12px;width:100%}.checkout-copy{display:block;min-width:0}.total-value{display:block;margin-top:2px;color:var(--cf-ink);font-size:23px;font-weight:800;line-height:1.1;white-space:nowrap}.currency{color:var(--cf-muted);font-size:12px;font-weight:500}.checkout-count{display:block;margin-top:2px;color:var(--cf-muted);font-size:11px;line-height:1.2;white-space:nowrap}.total-button{min-width:0;padding-left:8px;padding-right:8px;font-size:15px}
</style>
