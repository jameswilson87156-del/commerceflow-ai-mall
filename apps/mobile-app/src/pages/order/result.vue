<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import MobileHeader from '../../components/MobileHeader.vue'
import MobileStatusPill from '../../components/MobileStatusPill.vue'
import MobileMoney from '../../components/MobileMoney.vue'
import { getOrder } from '../../api/orders'
import { apiErrorMessage } from '../../api/runtime'
import { CLIENT_DATA_MODE } from '../../config/runtime'
import type { OrderSummary } from '../../api/types'

const order = ref<OrderSummary | null>(null)
const state = ref<'loading' | 'ready' | 'error'>('loading')
const errorMessage = ref('')
const orderNo = ref('')
const itemCount = computed(() => order.value?.items.reduce((sum, item) => sum + item.quantity, 0) || 0)
async function load(requestedOrderNo: string) { orderNo.value = requestedOrderNo; state.value = 'loading'; errorMessage.value = ''; try { order.value = (await getOrder(orderNo.value)).data; state.value = 'ready' } catch (error) { state.value = 'error'; errorMessage.value = apiErrorMessage(error, '订单结果加载失败，请重试。') } }
function openDetail() { if (order.value) uni.redirectTo({ url: `/pages/order/detail?orderNo=${encodeURIComponent(order.value.orderNo)}` }) }
function backToProducts() { uni.reLaunch({ url: '/pages/index/index' }) }
function retry() { if (orderNo.value) load(orderNo.value) }
onMounted(async () => { const { queryParam } = await import('../../platform/h5'); load(queryParam('orderNo')) })
</script>

<template>
  <view class="mobile-page result-page mobile-page--result">
    <MobileHeader eyebrow="COMMERCEFLOW / RESULT" title="订单结果" :subtitle="`${CLIENT_DATA_MODE === 'LOCAL_DEMO_FIXTURE' ? 'Local Demo Fixture' : 'Real Backend'} · 服务端已返回真实创建结果`" />
    <view class="checkout-stepper checkout-stepper--result" aria-label="订单流程"><view class="checkout-step is-done"><text>1</text><span>购物车</span></view><view class="checkout-step-line is-done"></view><view class="checkout-step is-done"><text>2</text><span>确认订单</span></view><view class="checkout-step-line is-done"></view><view class="checkout-step is-current"><text>3</text><span>创建结果</span></view></view>
    <view v-if="state === 'loading'" class="panel empty-box">正在读取真实订单结果...</view>
    <view v-else-if="state === 'error'" class="error-box"><text>{{ errorMessage }}</text><button class="secondary-button retry-button" type="button" @click="retry">重新加载</button></view>
    <view v-else-if="order" class="result-shell"><view class="success-mark" aria-hidden="true">✓</view><MobileStatusPill label="CREATED / 已创建" tone="success" /><text class="result-title">订单已创建</text><text class="result-subtitle">商品已按真实库存完成一次服务端创建</text><view class="result-card panel"><view class="result-row"><text class="muted">订单号</text><text class="mono">{{ order.orderNo }}</text></view><view class="result-row"><text class="muted">总金额</text><MobileMoney :amount="order.totalAmount" :currency="order.currency" compact /></view><view class="result-row"><text class="muted">商品件数</text><text>{{ itemCount }} 件</text></view><view class="result-row"><text class="muted">创建时间</text><text class="result-value">{{ order.createdAt }}</text></view><view class="result-row"><text class="muted">状态</text><MobileStatusPill label="已创建" tone="success" /></view></view><view class="result-actions"><button class="primary-button" type="button" @click="openDetail">查看订单详情</button><button class="secondary-button" type="button" @click="backToProducts">返回商品列表</button></view><text class="boundary-note">本页面不代表支付、发货或物流完成。</text></view>
  </view>
</template>

<style>
.result-page { background: #fbfaf7; }
.result-page .mobile-header { margin-bottom: 25px; }
.checkout-stepper { display: flex; align-items: flex-start; justify-content: center; gap: 8px; margin: 2px 0 27px; }.checkout-step { display: grid; justify-items: center; gap: 5px; color: #8a938d; font: 9px/1.2 ui-monospace, SFMono-Regular, Consolas, monospace; }.checkout-step text { display: grid; width: 23px; height: 23px; place-items: center; border-radius: 50%; background: #ecefea; color: #77837b; font: 800 10px/1 -apple-system, BlinkMacSystemFont, "Segoe UI", sans-serif; }.checkout-step.is-current { color: var(--cf-blue); font-weight: 800; }.checkout-step.is-current text { background: var(--cf-blue); color: #fff; }.checkout-step.is-done text { background: var(--cf-green-soft); color: var(--cf-green); }.checkout-step-line { width: 31px; height: 1px; margin-top: 11px; background: #cfd6d1; }.checkout-step-line.is-done { background: #9bd0ba; }
.result-shell { width: 100%; min-width: 0; border-top: 1px solid #dfe1dc; padding-top: 22px; text-align: center; }.success-mark { display: flex; align-items: center; justify-content: center; width: 58px; height: 58px; margin: 0 auto 12px; border: 1px solid #bfe2d0; border-radius: 50%; background: var(--cf-green-soft); color: var(--cf-green); font-size: 34px; font-weight: 850; }.result-shell > .mobile-status-pill { margin-bottom: 11px; }.result-title { display: block; color: var(--cf-ink); font-size: 28px; font-weight: 820; letter-spacing: -.05em; }.result-subtitle { display: block; margin-top: 8px; color: var(--cf-muted); font-size: 12px; line-height: 1.4; }.result-card.panel { width: 100%; min-width: 0; margin-top: 22px; border: 1px solid #dfe3de; border-radius: 4px; background: #fff; padding: 3px 15px; text-align: left; }.result-row { display: grid; grid-template-columns: 78px minmax(0, 1fr); align-items: center; gap: 12px; padding: 13px 0; border-bottom: 1px solid #eceeea; font-size: 12px; }.result-row:last-child { border-bottom: 0; }.result-row > :last-child { min-width: 0; overflow-wrap: anywhere; text-align: right; }.result-row .mobile-money { color: var(--cf-ink); }.result-value { font-size: 10px; }.result-actions { display: grid; grid-template-columns: minmax(0, 1fr) minmax(0, 1fr); gap: 8px; margin-top: 15px; }.result-actions button { min-width: 0; padding-left: 7px; padding-right: 7px; font-size: 12px; }.boundary-note { display: block; margin: 15px 4px 20px; color: #89938c; font: 9px/1.45 ui-monospace, SFMono-Regular, Consolas, monospace; }

/* Apple-inspired result pass: quiet confirmation, no dashboard chrome. */
.result-page { background: #f5f5f7; }
.result-shell { border-top: 0; padding-top: 8px; }
.success-mark { width: 64px; height: 64px; border: 0; background: #fff; color: #248a3d; font-size: 34px; font-weight: 600; box-shadow: 0 4px 18px rgba(0, 0, 0, .05); }
.result-title { color: #1d1d1f; font-size: 30px; font-weight: 700; letter-spacing: -.055em; }
.result-subtitle { color: #6e6e73; font-size: 13px; }
.result-card.panel { margin-top: 24px; border: 0; border-radius: 20px; background: #fff; padding: 4px 18px; }
.result-row { border-bottom-color: #e8e8ed; padding: 14px 0; font-size: 13px; }
.result-row .mobile-money { color: #1d1d1f; font-weight: 650; }
.result-actions { gap: 10px; margin-top: 16px; }
.result-actions button { font-size: 13px; }
.boundary-note { color: #6e6e73; font: 10px/1.45 -apple-system, BlinkMacSystemFont, "SF Pro Text", "PingFang SC", sans-serif; }
</style>
