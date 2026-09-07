<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { getProduct } from '../../api/catalog'
import { addCartItem } from '../../api/cart'
import { apiErrorMessage, resolveImageUrl } from '../../api/runtime'
import MobileNotice from '../../components/MobileNotice.vue'
import MobileStatusPill from '../../components/MobileStatusPill.vue'
import MobileMoney from '../../components/MobileMoney.vue'
import ProductImage from '../../components/ProductImage.vue'
import { CLIENT_DATA_MODE } from '../../config/runtime'
import { getStockPresentation, isPurchasableSku } from '../../ui/mobile-commerce-policy.mjs'
import type { Product, Sku } from '../../api/types'

const product = ref<Product | null>(null)
const selectedSkuId = ref<number | null>(null)
const state = ref<'loading' | 'ready' | 'error'>('loading')
const errorMessage = ref('')
const productId = ref(0)
const actionMessage = ref('')
const actionTone = ref<'success' | 'error'>('success')
const adding = ref(false)
const buying = ref(false)
const showFacts = ref(false)

const selectedSku = computed(() => product.value?.skus.find(sku => sku.id === selectedSkuId.value) || null)
const selectedStock = computed(() => getStockPresentation(selectedSku.value?.availableStock))
const canAdd = computed(() => Boolean(product.value?.status === 'ON_SALE' && selectedSku.value && isPurchasableSku(selectedSku.value.availableStock) && !adding.value && !buying.value))
const canBuy = computed(() => canAdd.value)
const productStatusLabel = computed(() => product.value?.status === 'ON_SALE' ? '在售' : product.value?.status || '状态未知')

function load(id: number) {
  state.value = 'loading'
  errorMessage.value = ''
  getProduct(id).then(response => {
    product.value = response.data
    const first = response.data.skus.find(sku => isPurchasableSku(sku.availableStock)) || response.data.skus[0]
    selectedSkuId.value = first?.id || null
    state.value = 'ready'
  }).catch(error => {
    state.value = 'error'
    errorMessage.value = apiErrorMessage(error, '商品详情加载失败，请重试')
  })
}

function chooseSku(id: number) {
  const sku = product.value?.skus.find(item => item.id === id)
  if (!sku || !isPurchasableSku(sku.availableStock)) return
  selectedSkuId.value = id
  actionMessage.value = ''
}

function imageUrl(path?: string | null) {
  return resolveImageUrl(path)
}

function skuStock(sku: Sku) {
  return getStockPresentation(sku.availableStock)
}

function goBack() {
  uni.navigateBack()
}

function retry() {
  if (productId.value) load(productId.value)
}

function openCart() {
  uni.navigateTo({ url: '/pages/cart/index' })
}

function openAiCustomerService() {
  if (!selectedSku.value || !product.value) return
  uni.navigateTo({ url: `/pages/ai/customer-service?productId=${product.value.id}&skuId=${selectedSku.value.id}` })
}

async function addToCart() {
  if (!selectedSku.value || !canAdd.value) return
  adding.value = true
  actionMessage.value = ''
  try {
    await addCartItem(selectedSku.value.id)
    actionTone.value = 'success'
    actionMessage.value = '已加入服务端购物车。'
  } catch (error) {
    actionTone.value = 'error'
    actionMessage.value = apiErrorMessage(error, '加入购物车失败，请重试。')
  } finally {
    adding.value = false
  }
}

async function buyNow() {
  if (!selectedSku.value || !canBuy.value) return
  buying.value = true
  actionMessage.value = ''
  try {
    await addCartItem(selectedSku.value.id)
    uni.navigateTo({ url: '/pages/order/confirm' })
  } catch (error) {
    actionTone.value = 'error'
    actionMessage.value = apiErrorMessage(error, '无法进入订单确认，请重试。')
  } finally {
    buying.value = false
  }
}

onMounted(async () => {
  const { queryParam } = await import('../../platform/h5')
  productId.value = Number(queryParam('productId'))
  load(productId.value)
})
</script>

<template>
  <view class="mobile-page mobile-page--with-bottom-action detail-page">
    <view class="detail-nav" aria-label="商品详情导航">
      <button class="detail-nav__button detail-nav__button--back" type="button" aria-label="返回" @click="goBack">
        <text aria-hidden="true">‹</text>
      </button>
      <view class="detail-nav__brand">
        <view class="detail-nav__mark" aria-hidden="true">CF</view>
        <view>
          <text class="detail-nav__name">CommerceFlow</text>
          <text class="detail-nav__context">{{ CLIENT_DATA_MODE === 'LOCAL_DEMO_FIXTURE' ? 'Local Demo Fixture' : 'Real Backend' }}</text>
        </view>
      </view>
      <button class="detail-nav__button detail-nav__button--bag" type="button" aria-label="打开购物袋" @click="openCart">
        <view class="bag-icon" aria-hidden="true" />
        <text>购物袋</text>
      </button>
    </view>

    <view v-if="state === 'loading'" class="detail-state detail-state--loading">
      <view class="detail-skeleton detail-skeleton--stage" />
      <view class="detail-skeleton detail-skeleton--line detail-skeleton--line-short" />
      <view class="detail-skeleton detail-skeleton--title" />
      <view class="detail-skeleton detail-skeleton--line" />
      <text class="detail-state__hint">正在读取商品详情…</text>
    </view>

    <view v-else-if="state === 'error'" class="detail-state detail-state--error">
      <text class="detail-state__eyebrow">PRODUCT UNAVAILABLE</text>
      <text class="detail-state__title">暂时无法读取商品</text>
      <text class="detail-state__message">{{ errorMessage }}</text>
      <button class="detail-outline-button" type="button" @click="retry">重新加载</button>
    </view>

    <template v-else-if="product">
      <view class="detail-content">
        <view class="product-stage">
          <view class="product-stage__header">
            <text class="product-stage__category">{{ product.categoryName }}</text>
            <MobileStatusPill :label="productStatusLabel" :tone="product.status === 'ON_SALE' ? 'success' : 'neutral'" />
          </view>
          <ProductImage
            class="detail-stage-image"
            :src="imageUrl(selectedSku?.imagePath || product.coverImagePath)"
            :alt="product.name"
          />
          <view class="product-stage__footer">
            <text>{{ selectedSku ? `${selectedSku.color} · ${selectedSku.size}` : '商品图片' }}</text>
            <text>{{ selectedSku ? selectedSku.skuCode : product.productCode }}</text>
          </view>
        </view>

        <view class="product-intro">
          <text class="product-intro__eyebrow">精选商品</text>
          <text class="product-title">{{ product.name }}</text>
          <text class="product-description">{{ product.description }}</text>
          <view class="product-price-row">
            <MobileMoney :amount="selectedSku?.salePrice || '-'" :currency="selectedSku?.currency || 'CNY'" />
            <view class="product-price-state">
              <text class="product-price-state__dot" :class="`product-price-state__dot--${selectedStock.tone}`" />
              <text>{{ selectedSku ? selectedStock.label : '请选择规格' }}</text>
            </view>
          </view>
        </view>

        <view class="detail-section variant-section">
          <view class="detail-section__heading">
            <view>
              <text class="detail-section__eyebrow">选择规格</text>
              <text class="detail-section__title">颜色与尺码</text>
            </view>
            <text class="detail-section__count">{{ product.skus.length }} 款</text>
          </view>

          <view class="variant-list">
            <button
              v-for="sku in product.skus"
              :key="sku.id"
              :class="['variant-row', { selected: sku.id === selectedSkuId, unavailable: !isPurchasableSku(sku.availableStock) }]"
              type="button"
              :disabled="!isPurchasableSku(sku.availableStock)"
              :aria-label="`${sku.color}，${sku.size}，${skuStock(sku).label}`"
              :aria-pressed="sku.id === selectedSkuId"
              @click="chooseSku(sku.id)"
            >
              <ProductImage class="variant-thumb" :src="imageUrl(sku.imagePath)" :alt="`${sku.color} ${sku.size}`" />
              <view class="variant-main">
                <view class="variant-name-row">
                  <text class="variant-color">{{ sku.color }}</text>
                  <text class="variant-size">{{ sku.size }}</text>
                </view>
              </view>
              <view class="variant-end">
                <text :class="['variant-status', `variant-status--${skuStock(sku).tone}`]">{{ skuStock(sku).label }}</text>
                <view class="variant-radio" :class="{ 'variant-radio--selected': sku.id === selectedSkuId }" aria-hidden="true">
                  <text v-if="sku.id === selectedSkuId">✓</text>
                </view>
              </view>
            </button>
          </view>

          <view v-if="selectedSku" class="selection-summary">
            <view class="selection-summary__copy">
              <text class="selection-summary__label">已选择</text>
              <text class="selection-summary__value">{{ selectedSku.color }} · {{ selectedSku.size }}</text>
            </view>
            <view class="selection-summary__stock">
              <text class="selection-summary__label">当前库存</text>
              <text :class="['selection-summary__value', `selection-summary__value--${selectedStock.tone}`]">{{ selectedSku.availableStock }} 件</text>
            </view>
          </view>
        </view>

        <view class="detail-section facts-section">
          <button class="facts-toggle" type="button" :aria-expanded="showFacts" @click="showFacts = !showFacts">
            <view>
              <text class="detail-section__eyebrow">商品事实</text>
              <text class="facts-toggle__title">库存与购买说明</text>
            </view>
            <text class="facts-toggle__icon" aria-hidden="true">{{ showFacts ? '−' : '+' }}</text>
          </button>
          <view v-if="showFacts" class="facts-panel">
            <view class="fact-line"><text class="fact-line__mark">✓</text><text>当前可用库存：{{ selectedSku?.availableStock ?? 0 }} 件</text></view>
            <view class="fact-line"><text class="fact-line__mark">✓</text><text>库存数据来自 Java 商品与库存接口</text></view>
            <view class="fact-line"><text class="fact-line__mark">✓</text><text>下单时会再次校验真实库存</text></view>
            <view class="fact-line fact-line--technical"><text class="fact-line__mark">·</text><text>Product code：{{ product.productCode }} · SKU：{{ selectedSku?.skuCode || '未选择' }}</text></view>
          </view>
        </view>

        <button class="ai-entry" type="button" :disabled="!selectedSku" @click="openAiCustomerService">
          <view class="ai-entry__symbol" aria-hidden="true">?</view>
          <view class="ai-entry__copy">
            <text class="ai-entry__title">对商品有疑问？</text>
            <text class="ai-entry__subtitle">询问 AI 商品助手</text>
          </view>
          <text class="ai-entry__arrow" aria-hidden="true">›</text>
        </button>

        <MobileNotice v-if="actionMessage" class="detail-feedback" :tone="actionTone" title="购物车操作" :message="actionMessage" />
      </view>

      <view class="bottom-action detail-bottom-bar">
        <view class="detail-actions">
          <button class="bag-action" type="button" @click="openCart">
            <view class="bag-action__icon" aria-hidden="true" />
            <text>购物袋</text>
          </button>
          <button class="secondary-button detail-secondary-action" type="button" :disabled="!canAdd" @click="addToCart">
            {{ adding ? '加入中…' : '加入购物车' }}
          </button>
          <button class="primary-button detail-primary-action" type="button" :disabled="!canBuy" @click="buyNow">
            {{ buying ? '准备中…' : selectedSku?.availableStock === 0 ? '暂无库存' : '立即购买' }}
          </button>
        </view>
      </view>
    </template>
  </view>
</template>

<style>
.detail-page {
  --detail-canvas: #f5f5f7;
  --detail-surface: #ffffff;
  --detail-ink: #1d1d1f;
  --detail-muted: #6e6e73;
  --detail-line: #d2d2d7;
  --detail-blue: #0071e3;
  --detail-success: #248a3d;
  --detail-warning: #a05a00;
  --detail-danger: #c9343f;
  background: var(--detail-canvas);
  color: var(--detail-ink);
}

.detail-nav {
  display: flex;
  align-items: center;
  justify-content: space-between;
  min-height: 44px;
  margin: -2px 0 18px;
}

.detail-nav__button {
  display: flex;
  align-items: center;
  justify-content: center;
  flex: none;
  width: 36px;
  height: 36px;
  padding: 0;
  border: 1px solid var(--detail-line);
  border-radius: 50%;
  background: rgba(255, 255, 255, .84);
  color: var(--detail-ink);
  box-shadow: none;
}

.detail-nav__button--back text {
  display: block;
  margin-top: -3px;
  font-size: 28px;
  font-weight: 300;
  line-height: 1;
}

.detail-nav__button--bag {
  width: auto;
  gap: 7px;
  padding: 0 12px;
  border-radius: 999px;
  color: var(--detail-ink);
  font-size: 12px;
  font-weight: 600;
  white-space: nowrap;
}

.detail-nav__brand {
  display: flex;
  align-items: center;
  gap: 8px;
  min-width: 0;
}

.detail-nav__mark {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 27px;
  height: 27px;
  border-radius: 8px;
  background: var(--detail-ink);
  color: #fff;
  font: 800 9px/1 ui-monospace, SFMono-Regular, Consolas, monospace;
  letter-spacing: -.04em;
}

.detail-nav__name,
.detail-nav__context {
  display: block;
}

.detail-nav__name {
  color: var(--detail-ink);
  font-size: 12px;
  font-weight: 700;
  line-height: 1.1;
}

.detail-nav__context {
  margin-top: 2px;
  color: var(--detail-muted);
  font: 8px/1.1 ui-monospace, SFMono-Regular, Consolas, monospace;
  letter-spacing: .08em;
}

.bag-icon,
.bag-action__icon {
  position: relative;
  display: block;
  width: 16px;
  height: 15px;
  border: 1.4px solid currentColor;
  border-radius: 4px;
}

.bag-icon::before,
.bag-action__icon::before {
  position: absolute;
  top: -5px;
  left: 3px;
  width: 7px;
  height: 6px;
  border: 1.4px solid currentColor;
  border-bottom: 0;
  border-radius: 6px 6px 0 0;
  content: "";
}

.detail-content {
  width: 100%;
  max-width: 560px;
  margin: 0 auto;
}

.product-stage {
  overflow: hidden;
  border: 1px solid rgba(210, 210, 215, .72);
  border-radius: 28px;
  background: var(--detail-surface);
}

.product-stage__header,
.product-stage__footer {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  padding: 14px 16px;
}

.product-stage__header {
  min-height: 46px;
  border-bottom: 1px solid rgba(210, 210, 215, .62);
}

.product-stage__category {
  min-width: 0;
  overflow: hidden;
  color: var(--detail-muted);
  font-size: 12px;
  font-weight: 600;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.detail-stage-image {
  width: 100%;
  height: 310px;
  background: var(--detail-surface);
}

.detail-stage-image.image-frame {
  border-radius: 0;
}

.detail-stage-image.image-frame > img {
  object-fit: contain;
  padding: 15px 28px;
}

.product-stage__footer {
  border-top: 1px solid rgba(210, 210, 215, .62);
  color: var(--detail-muted);
  font: 10px/1.25 ui-monospace, SFMono-Regular, Consolas, monospace;
}

.product-stage__footer text:last-child {
  min-width: 0;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.product-intro {
  padding: 25px 2px 0;
}

.product-intro__eyebrow,
.detail-section__eyebrow {
  display: block;
  color: var(--detail-muted);
  font-size: 11px;
  font-weight: 600;
  line-height: 1.2;
}

.product-title {
  display: block;
  max-width: 100%;
  margin-top: 9px;
  color: var(--detail-ink);
  font-size: 31px;
  font-weight: 700;
  letter-spacing: -.055em;
  line-height: 1.12;
  overflow-wrap: anywhere;
}

.product-description {
  display: block;
  max-width: 360px;
  margin-top: 11px;
  color: var(--detail-muted);
  font-size: 14px;
  line-height: 1.52;
  overflow-wrap: anywhere;
}

.product-price-row {
  display: flex;
  align-items: baseline;
  justify-content: space-between;
  gap: 12px;
  margin-top: 21px;
}

.product-price-row .mobile-money {
  color: var(--detail-ink);
  font-size: 31px;
  font-weight: 700;
  letter-spacing: -.035em;
}

.product-price-state {
  display: flex;
  align-items: center;
  gap: 6px;
  flex: none;
  color: var(--detail-muted);
  font-size: 11px;
  font-weight: 600;
}

.product-price-state__dot {
  width: 7px;
  height: 7px;
  border-radius: 50%;
  background: currentColor;
}

.product-price-state__dot--success { color: var(--detail-success); }
.product-price-state__dot--warning { color: var(--detail-warning); }
.product-price-state__dot--danger { color: var(--detail-danger); }

.detail-section {
  margin-top: 31px;
}

.detail-section__heading,
.facts-toggle {
  display: flex;
  align-items: flex-end;
  justify-content: space-between;
  gap: 16px;
  width: 100%;
  padding: 0 0 12px;
  border-bottom: 1px solid var(--detail-line);
  background: transparent;
  color: var(--detail-ink);
  text-align: left;
}

.detail-section__title,
.facts-toggle__title {
  display: block;
  margin-top: 5px;
  color: var(--detail-ink);
  font-size: 18px;
  font-weight: 650;
  letter-spacing: -.025em;
  line-height: 1.2;
}

.detail-section__count {
  flex: none;
  color: var(--detail-muted);
  font-size: 12px;
  line-height: 1.2;
}

.variant-list {
  overflow: hidden;
  margin-top: 12px;
  border: 1px solid rgba(210, 210, 215, .9);
  border-radius: 20px;
  background: var(--detail-surface);
}

.variant-row {
  display: grid;
  grid-template-columns: 58px minmax(0, 1fr) auto;
  align-items: center;
  gap: 12px;
  width: 100%;
  min-height: 80px;
  padding: 10px 12px;
  border-bottom: 1px solid rgba(210, 210, 215, .68);
  background: var(--detail-surface);
  color: var(--detail-ink);
  text-align: left;
  transition: background-color .18s ease, box-shadow .18s ease, opacity .18s ease;
}

.variant-row:last-child {
  border-bottom: 0;
}

.variant-row.selected {
  background: #fbfcff;
  box-shadow: inset 3px 0 var(--detail-blue);
}

.variant-row.unavailable {
  opacity: .46;
}

.variant-thumb {
  width: 58px;
  height: 58px;
  border-radius: 14px;
  background: #f5f5f7;
}

.variant-thumb.image-frame > img {
  object-fit: contain;
  padding: 5px;
}

.variant-main {
  min-width: 0;
}

.variant-name-row {
  display: flex;
  align-items: baseline;
  gap: 8px;
  min-width: 0;
}

.variant-color {
  min-width: 0;
  overflow: hidden;
  color: var(--detail-ink);
  font-size: 15px;
  font-weight: 650;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.variant-size {
  flex: none;
  color: var(--detail-muted);
  font-size: 12px;
}

.variant-code {
  display: block;
  margin-top: 6px;
  overflow: hidden;
  color: var(--detail-muted);
  font: 10px/1.2 ui-monospace, SFMono-Regular, Consolas, monospace;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.variant-end {
  display: grid;
  justify-items: end;
  gap: 8px;
  min-width: 55px;
}

.variant-status {
  display: flex;
  align-items: center;
  gap: 5px;
  color: var(--detail-muted);
  font-size: 10px;
  font-weight: 600;
  white-space: nowrap;
}

.variant-status::before {
  width: 6px;
  height: 6px;
  border-radius: 50%;
  background: currentColor;
  content: "";
}

.variant-status--success { color: var(--detail-success); }
.variant-status--warning { color: var(--detail-warning); }
.variant-status--danger { color: var(--detail-danger); }

.variant-radio {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 20px;
  height: 20px;
  border: 1.5px solid #b9bbc1;
  border-radius: 50%;
  color: #fff;
  font-size: 12px;
  font-weight: 800;
}

.variant-radio--selected {
  border-color: var(--detail-blue);
  background: var(--detail-blue);
}

.selection-summary {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 16px;
  margin-top: 13px;
  padding: 14px 0 0;
}

.selection-summary__stock {
  flex: none;
  text-align: right;
}

.selection-summary__label {
  display: block;
  color: var(--detail-muted);
  font-size: 11px;
  line-height: 1.25;
}

.selection-summary__value {
  display: block;
  margin-top: 4px;
  color: var(--detail-ink);
  font-size: 13px;
  font-weight: 650;
  line-height: 1.25;
}

.selection-summary__value--success { color: var(--detail-success); }
.selection-summary__value--warning { color: var(--detail-warning); }
.selection-summary__value--danger { color: var(--detail-danger); }

.facts-section {
  margin-top: 28px;
}

.facts-toggle {
  align-items: center;
  padding: 15px 0;
}

.facts-toggle__icon {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 24px;
  height: 24px;
  flex: none;
  border: 1px solid var(--detail-line);
  border-radius: 50%;
  color: var(--detail-muted);
  font-size: 18px;
  font-weight: 300;
  line-height: 1;
}

.facts-panel {
  padding: 13px 0 2px;
}

.fact-line {
  display: flex;
  align-items: flex-start;
  gap: 8px;
  margin-top: 9px;
  color: var(--detail-muted);
  font-size: 12px;
  line-height: 1.45;
}

.fact-line:first-child {
  margin-top: 0;
}

.fact-line__mark {
  flex: none;
  color: var(--detail-success);
  font-weight: 700;
}

.fact-line--technical {
  color: #8a8a90;
  font-family: ui-monospace, SFMono-Regular, Consolas, monospace;
  font-size: 10px;
}

.fact-line--technical .fact-line__mark {
  color: #8a8a90;
}

.ai-entry {
  display: flex;
  align-items: center;
  width: 100%;
  gap: 11px;
  margin-top: 26px;
  padding: 14px 2px;
  border-top: 1px solid var(--detail-line);
  border-bottom: 1px solid var(--detail-line);
  background: transparent;
  color: var(--detail-ink);
  text-align: left;
}

.ai-entry__symbol {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 29px;
  height: 29px;
  flex: none;
  border-radius: 50%;
  background: #eef5ff;
  color: var(--detail-blue);
  font-size: 16px;
}

.ai-entry__copy {
  min-width: 0;
  flex: 1;
}

.ai-entry__title,
.ai-entry__subtitle {
  display: block;
}

.ai-entry__title {
  color: var(--detail-ink);
  font-size: 13px;
  font-weight: 650;
  line-height: 1.25;
}

.ai-entry__subtitle {
  margin-top: 3px;
  color: var(--detail-muted);
  font-size: 11px;
  line-height: 1.25;
}

.ai-entry__arrow {
  flex: none;
  color: var(--detail-muted);
  font-size: 24px;
  font-weight: 300;
  line-height: 1;
}

.ai-entry[disabled] {
  opacity: .46;
}

.detail-feedback {
  margin-top: 18px;
}

.detail-state {
  width: 100%;
  padding-top: 12px;
}

.detail-state--error {
  display: grid;
  justify-items: start;
  padding: 34px 4px;
}

.detail-state__eyebrow {
  color: var(--detail-muted);
  font: 10px/1.2 ui-monospace, SFMono-Regular, Consolas, monospace;
  letter-spacing: .07em;
}

.detail-state__title {
  display: block;
  margin-top: 12px;
  color: var(--detail-ink);
  font-size: 25px;
  font-weight: 700;
  letter-spacing: -.04em;
  line-height: 1.15;
}

.detail-state__message {
  display: block;
  margin-top: 10px;
  color: var(--detail-muted);
  font-size: 13px;
  line-height: 1.5;
}

.detail-outline-button {
  min-height: 44px;
  margin-top: 22px;
  padding: 0 18px;
  border: 1px solid var(--detail-line);
  border-radius: 999px;
  background: #fff;
  color: var(--detail-blue);
  font-size: 13px;
  font-weight: 650;
}

.detail-skeleton {
  overflow: hidden;
  position: relative;
  border-radius: 12px;
  background: #e9e9ed;
}

.detail-skeleton::after {
  position: absolute;
  inset: 0;
  background: linear-gradient(90deg, transparent, rgba(255, 255, 255, .52), transparent);
  content: "";
  animation: detail-skeleton-shimmer 1.2s ease-in-out infinite;
}

.detail-skeleton--stage {
  width: 100%;
  height: 310px;
  border-radius: 28px;
}

.detail-skeleton--line {
  width: 70%;
  height: 13px;
  margin-top: 24px;
}

.detail-skeleton--line-short {
  width: 30%;
  margin-top: 24px;
}

.detail-skeleton--title {
  width: 86%;
  height: 34px;
  margin-top: 10px;
  border-radius: 10px;
}

.detail-state__hint {
  display: block;
  margin-top: 20px;
  color: var(--detail-muted);
  font-size: 12px;
}

.detail-bottom-bar {
  height: 78px;
  min-height: 78px;
  max-height: 78px;
  padding-top: 8px;
  padding-bottom: calc(8px + env(safe-area-inset-bottom));
  background: rgba(255, 255, 255, .88);
  border-top: 1px solid rgba(210, 210, 215, .86);
  box-shadow: 0 -8px 24px rgba(0, 0, 0, .06);
  backdrop-filter: blur(20px);
}

.detail-actions {
  display: grid;
  grid-template-columns: 48px minmax(0, 1fr) minmax(0, 1fr);
  align-items: center;
  gap: 9px;
  width: 100%;
}

.bag-action {
  display: grid;
  place-content: center;
  gap: 5px;
  min-width: 0;
  min-height: 50px;
  padding: 4px 0;
  background: transparent;
  color: var(--detail-muted);
  font-size: 10px;
  font-weight: 650;
  line-height: 1;
}

.bag-action__icon {
  margin: 0 auto;
}

.detail-secondary-action,
.detail-primary-action {
  min-width: 0;
  min-height: 48px;
  padding: 8px 8px;
  font-size: 13px;
  font-weight: 650;
}

.detail-secondary-action {
  border: 1px solid var(--detail-line);
  background: #fff;
  color: var(--detail-blue);
}

.detail-primary-action {
  background: var(--detail-blue);
  color: #fff;
  box-shadow: none;
}

@keyframes detail-skeleton-shimmer {
  from { transform: translateX(-100%); }
  to { transform: translateX(100%); }
}

@media (max-width: 360px) {
  .detail-nav__button--bag {
    padding-right: 9px;
    padding-left: 9px;
  }

  .detail-nav__button--bag text {
    display: none;
  }

  .detail-stage-image,
  .detail-skeleton--stage {
    height: 276px;
  }

  .product-title {
    font-size: 28px;
  }

  .product-price-row .mobile-money {
    font-size: 28px;
  }

  .detail-actions {
    grid-template-columns: 42px minmax(0, 1fr) minmax(0, 1fr);
    gap: 6px;
  }

  .detail-secondary-action,
  .detail-primary-action {
    padding-right: 5px;
    padding-left: 5px;
    font-size: 12px;
  }
}

@media (prefers-reduced-motion: reduce) {
  .variant-row,
  .detail-skeleton::after {
    transition: none;
    animation: none;
  }
}
</style>
