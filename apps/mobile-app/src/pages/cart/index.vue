<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import MobileHeader from '../../components/MobileHeader.vue'
import MobileNotice from '../../components/MobileNotice.vue'
import MobileStatusPill from '../../components/MobileStatusPill.vue'
import MobileMoney from '../../components/MobileMoney.vue'
import ProductImage from '../../components/ProductImage.vue'
import { getCart, updateCartItem, deleteCartItem } from '../../api/cart'
import { apiErrorMessage, resolveImageUrl } from '../../api/runtime'
import { CLIENT_DATA_MODE } from '../../config/runtime'
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
async function loadCart() { state.value = 'loading'; errorMessage.value = ''; try { items.value = (await getCart()).data; state.value = items.value.length ? 'ready' : 'empty' } catch (error) { state.value = 'error'; errorMessage.value = apiErrorMessage(error, '购物车加载失败，请重试。') } }
async function changeQuantity(item: CartItem, delta: number) { const next = item.quantity + delta; if (next < 1 || busyItemId.value) return; busyItemId.value = item.id; try { items.value = (await updateCartItem(item.id, next)).data; state.value = items.value.length ? 'ready' : 'empty' } catch (error) { errorMessage.value = apiErrorMessage(error, '数量更新失败，请重试。') } finally { busyItemId.value = null } }
async function removeItem(item: CartItem) { if (busyItemId.value) return; busyItemId.value = item.id; try { items.value = (await deleteCartItem(item.id)).data; state.value = items.value.length ? 'ready' : 'empty' } catch (error) { errorMessage.value = apiErrorMessage(error, '删除失败，请重试。') } finally { busyItemId.value = null } }
function openConfirm() { uni.navigateTo({ url: '/pages/order/confirm' }) }
function openProducts() { uni.navigateBack() }
onMounted(loadCart)
</script>

<template>
  <view class="mobile-page mobile-page--with-bottom-action cart-page">
    <MobileHeader eyebrow="COMMERCEFLOW / CART" title="购物车" :subtitle="`${CLIENT_DATA_MODE === 'LOCAL_DEMO_FIXTURE' ? 'Local Demo Fixture' : 'Real Backend'} · /api/v1/me 服务端范围`" action-label="继续选购" @action="openProducts" />
    <view class="cart-intro"><text class="eyebrow">YOUR BAG / {{ count }} ITEMS</text><text class="cart-heading">已选商品</text><text class="cart-subtitle">服务端购物车 · 本地 Showcase</text></view>
    <view class="summary-strip"><view><text class="summary-label">购物车商品</text><text class="summary-caption">可购买库存实时校验</text></view><view class="summary-side"><text class="summary-value">{{ count }} 件</text><MobileStatusPill label="已同步" tone="success" /></view></view>
    <view v-if="state === 'loading'" class="panel empty-box">正在读取服务端购物车...</view>
    <view v-else-if="state === 'error'" class="error-box"><text>{{ errorMessage }}</text><button class="secondary-button retry-button" type="button" @click="loadCart">重新加载</button></view>
    <view v-else-if="state === 'empty'" class="panel empty-box"><text>购物车还是空的，先去挑选一件商品吧。</text><button class="secondary-button retry-button" type="button" @click="openProducts">去商品列表</button></view>
    <template v-else>
      <MobileNotice title="消费者接口边界" :message="`${CLIENT_DATA_MODE === 'LOCAL_DEMO_FIXTURE' ? '显式本地 Demo' : 'Real Backend'}：商品、SKU、价格和库存来自服务端；用户身份由 /api/v1/me 范围决定。`" />
      <view v-for="item in items" :key="item.id" class="cart-item panel"><ProductImage class="cart-image" :src="imageUrl(item)" :alt="item.productName" /><view class="cart-copy"><view class="cart-item-heading"><view class="cart-item-title"><text class="cart-name">{{ item.productName }}</text><text class="sku-code">{{ item.skuCode }}</text></view><MobileStatusPill label="已选" tone="blue" /></view><text class="cart-spec">{{ item.color }} / {{ item.size }}</text><view class="price-row"><MobileMoney :amount="item.unitPrice" :currency="item.currency" compact /><text class="line-total">小计 ¥{{ lineTotal(item) }}</text></view><view class="cart-actions"><view class="quantity-control"><button class="quantity-button" type="button" :aria-label="`减少 ${item.productName} 数量`" :disabled="busyItemId === item.id || item.quantity <= 1" @click="changeQuantity(item, -1)">−</button><text class="quantity-value" aria-live="polite">{{ item.quantity }}</text><button class="quantity-button" type="button" :aria-label="`增加 ${item.productName} 数量`" :disabled="busyItemId === item.id || item.quantity >= item.availableStock" @click="changeQuantity(item, 1)">+</button></view><text class="stock-hint">库存 {{ item.availableStock }}</text><button class="remove-button" type="button" :disabled="busyItemId === item.id" @click="removeItem(item)">删除</button></view></view></view>
      <view v-if="errorMessage" class="error-box cart-inline-error"><text>{{ errorMessage }}</text></view>
      <view class="bottom-action cart-checkout"><view class="checkout-copy"><text class="muted">订单预计总额</text><MobileMoney :amount="total" currency="CNY" /><text class="checkout-count">共 {{ count }} 件真实演示商品</text></view><button class="primary-button total-button" type="button" @click="openConfirm">去确认订单</button></view>
    </template>
  </view>
</template>

<style>
.cart-page { background: #fbfaf7; }
.cart-page .mobile-header { margin-bottom: 28px; }
.cart-intro { display: block; margin: 0 0 27px; }
.cart-heading { display: block; margin-top: 11px; color: #18231e; font-size: 31px; font-weight: 820; letter-spacing: -.05em; line-height: 1.1; }
.cart-subtitle { display: block; margin-top: 9px; color: #7d8780; font-size: 12px; line-height: 1.4; }
.summary-strip { display: flex; align-items: center; justify-content: space-between; gap: 12px; margin: 0 0 13px; border-top: 1px solid #dfe1dc; border-bottom: 1px solid #dfe1dc; padding: 12px 0; }
.summary-label, .summary-caption { display: block; color: #7d8780; font-size: 11px; line-height: 1.25; }
.summary-caption { margin-top: 4px; color: #a0a7a1; font: 9px/1.2 ui-monospace, SFMono-Regular, Consolas, monospace; }
.summary-side { display: flex; align-items: center; gap: 8px; }
.summary-value { color: var(--cf-ink); font-size: 15px; font-weight: 850; }
.summary-side .mobile-status-pill { padding: 4px 6px; font-size: 9px; }
.cart-item.panel { display: grid; grid-template-columns: 76px minmax(0, 1fr); gap: 12px; align-items: start; margin: 0; border: 0; border-bottom: 1px solid #dfe1dc; border-radius: 0; background: transparent; padding: 15px 0 17px; }
.cart-image { width: 76px; height: 76px; flex: none; border-radius: 0; background: #f0f0ec; }
.cart-copy { display: block; min-width: 0; }
.cart-item-heading { display: flex; align-items: flex-start; justify-content: space-between; gap: 7px; }
.cart-item-title { min-width: 0; flex: 1; }
.cart-name { display: block; min-width: 0; color: var(--cf-ink); font-size: 15px; font-weight: 850; line-height: 1.25; }
.cart-item-heading .mobile-status-pill { flex: none; padding: 4px 6px; font-size: 9px; }
.sku-code { display: block; margin-top: 5px; color: #8b958e; font: 9px/1.2 ui-monospace, SFMono-Regular, Consolas, monospace; overflow-wrap: anywhere; }
.cart-spec { display: block; margin-top: 7px; color: #68746d; font-size: 11px; }
.price-row { display: flex; align-items: baseline; justify-content: space-between; gap: 8px; margin-top: 10px; }
.price-row .mobile-money { color: var(--cf-blue); font-size: 16px; }
.line-total { color: var(--cf-ink); font-size: 11px; font-weight: 850; white-space: nowrap; }
.cart-actions { display: flex; align-items: center; gap: 8px; margin-top: 11px; }
.quantity-control { display: grid; grid-template-columns: 27px 27px 27px; align-items: center; border: 1px solid #dfe3de; border-radius: 4px; overflow: hidden; background: transparent; }
.quantity-button { min-width: 27px; height: 27px; padding: 0; background: transparent; color: var(--cf-ink); font-size: 16px; line-height: 27px; }
.quantity-value { min-width: 0; text-align: center; color: var(--cf-ink); font-size: 11px; font-weight: 850; }
.stock-hint { flex: 1; color: #8a938d; font: 9px/1.2 ui-monospace, SFMono-Regular, Consolas, monospace; }
.remove-button { min-width: 32px; padding: 5px 0; background: transparent; color: var(--cf-red); font-size: 10px; }
.cart-inline-error { margin-top: 12px; }
.cart-checkout { display: grid; grid-template-columns: minmax(0, 1fr) 150px; align-items: center; gap: 12px; width: 100%; }
.bottom-action.cart-checkout { display: grid; }
.checkout-copy { display: block; min-width: 0; }
.checkout-copy .mobile-money { display: block; margin-top: 3px; color: var(--cf-ink); font-size: 21px; }
.checkout-count { display: block; margin-top: 3px; color: var(--cf-muted); font: 9px/1.2 ui-monospace, SFMono-Regular, Consolas, monospace; white-space: nowrap; }
.total-button { min-width: 0; padding-left: 8px; padding-right: 8px; font-size: 13px; }

/* Apple-inspired bag pass: white product rows on a quiet system canvas. */
.cart-page { background: #f5f5f7; }
.cart-page .mobile-header { margin-bottom: 18px; }
.cart-intro { margin-bottom: 25px; }
.cart-heading { color: #1d1d1f; font-size: 34px; font-weight: 700; letter-spacing: -.06em; }
.cart-subtitle { color: #6e6e73; font-size: 13px; }
.summary-strip { margin-bottom: 12px; border: 0; border-radius: 18px; background: #fff; padding: 15px 16px; }
.summary-label { color: #1d1d1f; font-size: 13px; font-weight: 600; }
.summary-caption { color: #6e6e73; font: 11px/1.25 -apple-system, BlinkMacSystemFont, "SF Pro Text", "PingFang SC", sans-serif; }
.summary-value { color: #1d1d1f; font-size: 14px; font-weight: 600; }
.cart-item.panel { margin: 0 0 12px; border: 0; border-radius: 20px; background: #fff; padding: 14px; }
.cart-image { border-radius: 14px; background: #f5f5f7; }
.cart-name { color: #1d1d1f; font-size: 16px; font-weight: 600; letter-spacing: -.02em; }
.sku-code { color: #6e6e73; }
.cart-spec { color: #6e6e73; }
.price-row .mobile-money { color: #1d1d1f; font-weight: 600; }
.line-total { color: #1d1d1f; font-weight: 600; }
.quantity-control { border-color: #d2d2d7; border-radius: 999px; background: #f5f5f7; }
.quantity-button { color: #1d1d1f; }
.quantity-value { color: #1d1d1f; }
.stock-hint { color: #6e6e73; }
.remove-button { color: #c9343f; }
.cart-checkout { grid-template-columns: minmax(0, 1fr) 150px; }
.checkout-copy .mobile-money { color: #1d1d1f; font-weight: 650; }
.total-button { font-size: 13px; }
</style>
