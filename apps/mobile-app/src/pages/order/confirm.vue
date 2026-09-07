<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import MobileHeader from '../../components/MobileHeader.vue'
import MobileNotice from '../../components/MobileNotice.vue'
import MobileStatusPill from '../../components/MobileStatusPill.vue'
import MobileMoney from '../../components/MobileMoney.vue'
import ProductImage from '../../components/ProductImage.vue'
import { getCart } from '../../api/cart'
import { submitOrder } from '../../api/orders'
import { ApiError, apiErrorMessage, resolveImageUrl } from '../../api/runtime'
import { CLIENT_DATA_MODE } from '../../config/runtime'
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
async function loadCart() { state.value = 'loading'; errorMessage.value = ''; submitError.value = ''; try { items.value = (await getCart()).data; state.value = items.value.length ? 'ready' : 'empty' } catch (error) { state.value = 'error'; errorMessage.value = apiErrorMessage(error, '订单确认数据加载失败，请重试。') } }
async function createOrder() {
  if (!items.value.length || submitting.value) return
  if (!currentKey.value) currentKey.value = createClientIdempotencyKey()
  submitting.value = true; submitError.value = ''
  try { const response = await submitOrder(items.value, currentKey.value); uni.redirectTo({ url: `/pages/order/result?orderNo=${encodeURIComponent(response.data.orderNo)}` }); currentKey.value = '' }
  catch (error) { const apiError = error instanceof ApiError ? error : null; if (apiError?.statusCode === 409) submitError.value = '提交请求冲突，请返回购物车重新确认。'; else if (apiError?.body?.code === 'INVENTORY_INSUFFICIENT') submitError.value = '库存不足，请返回购物车调整数量。'; else submitError.value = apiErrorMessage(error, '网络暂时不可用，请使用同一提交状态重试。') }
  finally { submitting.value = false }
}
function backToCart() { uni.navigateBack() }
onMounted(loadCart)
</script>

<template>
  <view class="mobile-page mobile-page--with-bottom-action confirm-page">
    <MobileHeader eyebrow="COMMERCEFLOW / CHECKOUT" title="确认订单" subtitle="创建后状态为 CREATED" :back="true" @back="backToCart" />
    <view class="checkout-stepper" aria-label="订单流程"><view class="checkout-step is-done"><text>1</text><span>购物车</span></view><view class="checkout-step-line is-done"></view><view class="checkout-step is-current"><text>2</text><span>确认订单</span></view><view class="checkout-step-line"></view><view class="checkout-step"><text>3</text><span>创建结果</span></view></view>
    <view v-if="state === 'loading'" class="panel empty-box">正在重新读取服务端购物车...</view>
    <view v-else-if="state === 'error'" class="error-box"><text>{{ errorMessage }}</text><button class="secondary-button retry-button" type="button" @click="loadCart">重新加载</button></view>
    <view v-else-if="state === 'empty'" class="panel empty-box"><text>购物车为空，请先选择商品。</text><button class="secondary-button retry-button" type="button" @click="backToCart">返回购物车</button></view>
    <template v-else>
      <MobileNotice title="消费者接口边界" :message="`${CLIENT_DATA_MODE === 'LOCAL_DEMO_FIXTURE' ? '显式本地 Demo' : 'Real Backend'}：订单通过 /api/v1/me 创建，身份由服务端推导，页面不提交 userId。`" />
      <view class="checkout-heading"><view><text class="eyebrow">ORDER REVIEW</text><text class="checkout-title">核对商品明细</text></view><MobileStatusPill label="服务端重算" tone="blue" /></view>
      <view v-for="item in items" :key="item.id" class="confirm-item panel"><ProductImage class="confirm-image" :src="imageUrl(item)" :alt="item.productName" /><view class="confirm-copy"><text class="cart-name">{{ item.productName }}</text><text class="sku-code">{{ item.skuCode }}</text><text class="cart-spec">{{ item.color }} / {{ item.size }} · 数量 {{ item.quantity }}</text><view class="price-row"><MobileMoney :amount="item.unitPrice" :currency="item.currency" compact /><text class="line-total">小计 ¥{{ lineTotal(item) }}</text></view></view></view>
      <view class="amount-panel panel"><view class="amount-heading"><text class="muted">订单预计总额</text><MobileStatusPill label="CNY" tone="neutral" /></view><MobileMoney class="amount" :amount="total" currency="CNY" /><text class="amount-note">最终金额以服务端创建订单结果为准。</text></view>
      <MobileNotice v-if="submitError" tone="error" title="订单未创建" :message="submitError" />
      <view class="bottom-action confirm-action"><view><text class="muted">共 {{ itemCount }} 件</text><MobileMoney :amount="total" currency="CNY" compact /></view><button class="primary-button" type="button" :disabled="submitting" @click="createOrder">{{ submitting ? '订单创建中…' : '创建订单' }}</button></view>
    </template>
  </view>
</template>

<style>
.confirm-page { background: #fbfaf7; }
.confirm-page .mobile-header { margin-bottom: 25px; }
.checkout-stepper { display: flex; align-items: flex-start; justify-content: center; gap: 8px; margin: 2px 0 25px; }
.checkout-step { display: grid; justify-items: center; gap: 5px; color: #8a938d; font: 9px/1.2 ui-monospace, SFMono-Regular, Consolas, monospace; }
.checkout-step text { display: grid; width: 23px; height: 23px; place-items: center; border-radius: 50%; background: #ecefea; color: #77837b; font: 800 10px/1 -apple-system, BlinkMacSystemFont, "Segoe UI", sans-serif; }
.checkout-step.is-current { color: var(--cf-blue); font-weight: 800; }.checkout-step.is-current text { background: var(--cf-blue); color: #fff; }.checkout-step.is-done text { background: var(--cf-green-soft); color: var(--cf-green); }
.checkout-step-line { width: 31px; height: 1px; margin-top: 11px; background: #cfd6d1; }.checkout-step-line.is-done { background: #9bd0ba; }
.checkout-heading { display: flex; align-items: flex-end; justify-content: space-between; gap: 12px; margin: 24px 0 11px; border-top: 1px solid #dfe1dc; padding-top: 14px; }
.checkout-title { display: block; margin-top: 8px; color: var(--cf-ink); font-size: 23px; font-weight: 820; letter-spacing: -.04em; }
.confirm-item.panel { display: grid; grid-template-columns: 76px minmax(0, 1fr); align-items: start; gap: 12px; margin: 0; border: 0; border-bottom: 1px solid #dfe1dc; border-radius: 0; background: transparent; padding: 15px 0 17px; }
.confirm-image { width: 76px; height: 76px; flex: none; border-radius: 0; background: #f0f0ec; }.confirm-copy { display: block; min-width: 0; }.cart-name { display: block; color: var(--cf-ink); font-size: 15px; font-weight: 850; line-height: 1.28; }.sku-code { display: block; margin-top: 5px; color: #8b958e; font: 9px/1.2 ui-monospace, SFMono-Regular, Consolas, monospace; overflow-wrap: anywhere; }.cart-spec { display: block; margin-top: 7px; color: #68746d; font-size: 11px; }.price-row { display: flex; align-items: baseline; justify-content: space-between; gap: 8px; margin-top: 10px; }.price-row .mobile-money { color: var(--cf-blue); font-size: 16px; }.line-total { color: var(--cf-ink); font-size: 11px; font-weight: 850; white-space: nowrap; }
.amount-panel.panel { margin-top: 27px; border: 0; border-top: 1px solid #dfe1dc; border-bottom: 1px solid #dfe1dc; border-radius: 0; background: transparent; padding: 15px 0 16px; }.amount-heading { display: flex; align-items: center; justify-content: space-between; gap: 8px; }.amount { display: block; margin-top: 9px; color: var(--cf-ink); font-size: 28px; }.amount .mobile-money__currency { color: var(--cf-muted); }.amount-note { display: block; margin-top: 7px; color: var(--cf-muted); font-size: 11px; line-height: 1.42; }
.confirm-action { display: grid; grid-template-columns: minmax(0, 1fr) 150px; align-items: center; gap: 12px; width: 100%; }.confirm-action > view { display: block; min-width: 0; }.confirm-action .mobile-money { display: block; margin-top: 3px; color: var(--cf-ink); }.confirm-action button { min-width: 0; padding-left: 8px; padding-right: 8px; font-size: 13px; }
.bottom-action.confirm-action { display: grid; }

/* Apple-inspired checkout pass: one calm review surface and one clear action. */
.confirm-page { background: #f5f5f7; }
.checkout-stepper { margin-bottom: 23px; }
.checkout-step text { background: #fff; color: #6e6e73; }
.checkout-step.is-current text { background: #0071e3; color: #fff; }
.checkout-step.is-done text { background: #e9f7ed; color: #248a3d; }
.checkout-step-line { background: #d2d2d7; }.checkout-step-line.is-done { background: #a9d9b7; }
.checkout-heading { margin-top: 22px; border-top: 0; padding-top: 0; }
.checkout-title { color: #1d1d1f; font-size: 25px; font-weight: 700; letter-spacing: -.045em; }
.confirm-item.panel { margin: 0 0 12px; border: 0; border-radius: 20px; background: #fff; padding: 14px; }
.confirm-image { border-radius: 14px; background: #f5f5f7; }
.confirm-copy .cart-name { color: #1d1d1f; font-size: 16px; font-weight: 600; }
.confirm-copy .cart-spec { color: #6e6e73; }
.confirm-copy .price-row .mobile-money { color: #1d1d1f; font-weight: 600; }
.amount-panel.panel { margin-top: 18px; border: 0; border-radius: 20px; background: #fff; padding: 18px; }
.amount { color: #1d1d1f; font-weight: 650; }
.amount-note { color: #6e6e73; }
.confirm-action .mobile-money { color: #1d1d1f; font-weight: 650; }
</style>
