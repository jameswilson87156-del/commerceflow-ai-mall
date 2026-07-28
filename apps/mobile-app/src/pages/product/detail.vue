<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { getProduct } from '../../api/catalog'
import { addCartItem } from '../../api/cart'
import { ApiError, resolveImageUrl } from '../../api/runtime'
import MobileHeader from '../../components/MobileHeader.vue'
import MobileNotice from '../../components/MobileNotice.vue'
import ProductImage from '../../components/ProductImage.vue'
import { getStockPresentation, isPurchasableSku } from '../../ui/mobile-commerce-policy.mjs'
import type { Product } from '../../api/types'

const product = ref<Product | null>(null)
const selectedSkuId = ref<number | null>(null)
const state = ref<'loading' | 'ready' | 'error'>('loading')
const errorMessage = ref('')
const productId = ref(0)
const actionMessage = ref('')
const actionTone = ref<'success' | 'error'>('success')
const adding = ref(false)
const selectedSku = computed(() => product.value?.skus.find(sku => sku.id === selectedSkuId.value) || null)
const selectedStock = computed(() => getStockPresentation(selectedSku.value?.availableStock))
const canAdd = computed(() => Boolean(product.value?.status === 'ON_SALE' && selectedSku.value && isPurchasableSku(selectedSku.value.availableStock) && !adding.value))

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
    errorMessage.value = error instanceof ApiError ? error.message : '商品详情加载失败，请重试'
  })
}
function chooseSku(id: number) {
  const sku = product.value?.skus.find(item => item.id === id)
  if (!sku) return
  selectedSkuId.value = id
  actionMessage.value = ''
}
function imageUrl(path?: string | null) { return resolveImageUrl(path) }
function goBack() { uni.navigateBack() }
function retry() { if (productId.value) load(productId.value) }
function openCart() { uni.navigateTo({ url: '/pages/cart/index' }) }
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
    actionMessage.value = error instanceof ApiError ? error.message : '加入购物车失败，请重试。'
  } finally {
    adding.value = false
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
    <MobileHeader title="商品详情" eyebrow="COMMERCEFLOW AI MALL" :back="true" @back="goBack" />
    <view v-if="state === 'loading'" class="panel empty-box">正在读取商品详情...</view>
    <view v-else-if="state === 'error'" class="error-box"><text>{{ errorMessage }}</text><button class="secondary-button retry-button" @click="retry">重新加载</button></view>
    <template v-else-if="product">
      <view class="product-hero panel">
        <ProductImage class="hero-image" :src="imageUrl(selectedSku?.imagePath || product.coverImagePath)" :alt="product.name" />
        <view class="hero-copy">
          <text class="category-label">{{ product.categoryName }}</text>
          <text class="detail-title">{{ product.name }}</text>
          <text class="product-code">{{ product.productCode }}</text>
          <text class="detail-description">{{ product.description }}</text>
          <view class="hero-price"><text>¥{{ selectedSku?.salePrice }}</text><text class="currency">{{ selectedSku?.currency }}</text></view>
          <text :class="['tag', product.status === 'ON_SALE' ? 'tag-success' : 'tag-neutral']">{{ product.status === 'ON_SALE' ? '在售' : product.status }}</text>
        </view>
      </view>

      <view class="spec-panel panel">
        <view class="section-title"><text>选择规格</text><text class="muted">{{ product.skus.length }} 个 SKU</text></view>
        <view class="sku-grid">
          <view v-for="sku in product.skus" :key="sku.id" :class="['sku-choice', { selected: sku.id === selectedSkuId, unavailable: !isPurchasableSku(sku.availableStock) }]" @click="chooseSku(sku.id)">
            <ProductImage class="choice-image" :src="imageUrl(sku.imagePath)" :alt="`${sku.color} ${sku.size}`" />
            <view class="choice-copy"><text class="choice-color">{{ sku.color }}</text><text class="choice-size">{{ sku.size }}</text><text class="sku-code">{{ sku.skuCode }}</text></view>
            <text :class="['tag', `tag-${getStockPresentation(sku.availableStock).tone}`]">{{ getStockPresentation(sku.availableStock).label }}</text>
          </view>
        </view>
        <view v-if="selectedSku" class="selected-summary">
          <ProductImage class="selected-image" :src="imageUrl(selectedSku.imagePath)" :alt="selectedSku.color" />
          <view class="selected-copy"><text class="summary-label">当前选择</text><text class="selected-title">{{ selectedSku.color }} / {{ selectedSku.size }}</text><text class="sku-code">{{ selectedSku.skuCode }}</text></view>
          <view class="selected-stock"><text class="selected-price">¥{{ selectedSku.salePrice }}</text><text class="currency">{{ selectedSku.currency }}</text><text :class="['stock-copy', `stock-${selectedStock.tone}`]">{{ selectedStock.available ? `可用库存 ${selectedSku.availableStock}` : '暂无库存' }}</text></view>
        </view>
      </view>

      <view class="facts-card panel">
        <view class="section-title"><text>库存事实说明</text><text class="info-dot">i</text></view>
        <view class="fact-row"><text class="fact-dot fact-green" /><text>当前可用库存：{{ selectedSku?.availableStock ?? 0 }} 件</text></view>
        <view class="fact-row"><text class="fact-dot fact-green" /><text>库存数据来自 Java 商品与库存接口</text></view>
        <view class="fact-row"><text class="fact-dot fact-green" /><text>下单时会再次校验真实库存</text></view>
      </view>
      <button class="secondary-button ai-entry" :disabled="!selectedSku" @click="openAiCustomerService">AI 商品问答</button>
      <MobileNotice v-if="actionMessage" :tone="actionTone" title="购物车操作" :message="actionMessage" />
      <view class="bottom-action"><view class="detail-actions"><button class="secondary-button" @click="openCart">查看购物车</button><button class="primary-button" :disabled="!canAdd" @click="addToCart">{{ adding ? '加入中...' : selectedSku?.availableStock === 0 ? '暂无库存' : '加入购物车' }}</button></view></view>
    </template>
  </view>
</template>

<style>
.detail-page .product-hero.panel{display:flex;flex-direction:column;gap:14px;padding:14px;overflow:hidden}.detail-page .product-hero .hero-image{width:100%;height:220px;flex:none;border-radius:12px}.hero-copy{display:block;min-width:0;padding:0 2px}.category-label{display:block;color:var(--cf-blue);font-size:15px;font-weight:700}.detail-title{display:block;margin-top:6px;color:var(--cf-ink);font-size:25px;font-weight:800;line-height:1.28}.product-code{display:block;margin-top:5px;color:#6e7b90;font-family:monospace;font-size:13px;overflow-wrap:anywhere}.detail-description{display:block;margin-top:8px;color:#66758a;font-size:15px;line-height:1.45}.hero-price{display:flex;align-items:baseline;gap:7px;margin-top:11px;color:var(--cf-blue);font-size:28px;font-weight:800}.hero-price .currency{color:var(--cf-muted);font-size:14px;font-weight:600}.hero-copy>.tag{margin-top:9px}.spec-panel{margin-top:14px;padding:0 14px 14px}.spec-panel .section-title{margin:0;padding:15px 2px 11px}.sku-grid{display:flex;flex-direction:column;gap:9px}.sku-choice{display:flex;align-items:center;gap:10px;min-height:74px;padding:8px 10px;border:1px solid var(--cf-line);border-radius:12px;background:#fff}.sku-choice.selected{border:2px solid var(--cf-blue);background:var(--cf-blue-soft)}.sku-choice.unavailable{opacity:.55}.choice-image{width:54px;height:54px;flex:none;border-radius:10px}.choice-copy{display:block;min-width:0;flex:1}.choice-color{display:inline-block;font-size:17px;font-weight:800}.choice-size{display:inline-block;margin-left:7px;color:#53647c;font-size:15px}.choice-copy .sku-code{margin-top:3px;white-space:nowrap;overflow:hidden;text-overflow:ellipsis}.selected-summary{display:flex;align-items:center;gap:10px;margin-top:12px;padding:12px;border:1px solid #dce5f0;border-radius:12px;background:#fbfdff}.selected-image{width:64px;height:64px;flex:none;border-radius:10px}.selected-copy{display:block;min-width:0;flex:1}.summary-label{display:block;color:var(--cf-muted);font-size:13px}.selected-title{display:block;margin-top:3px;font-size:18px;font-weight:800}.selected-copy .sku-code{margin-top:4px}.selected-stock{display:block;min-width:78px;text-align:right}.selected-price{display:block;color:var(--cf-blue);font-size:20px;font-weight:800}.selected-stock .currency{display:block;font-size:13px}.stock-copy{display:block;margin-top:6px;font-size:13px;font-weight:700}.stock-success{color:var(--cf-green)}.stock-warning{color:var(--cf-orange)}.stock-danger{color:var(--cf-red)}.facts-card{margin-top:14px;padding:0 16px 15px}.facts-card .section-title{margin:0;padding:15px 0 9px}.info-dot{display:flex;align-items:center;justify-content:center;width:24px;height:24px;border:2px solid var(--cf-blue);border-radius:50%;color:var(--cf-blue);font-size:14px;font-weight:800}.fact-row{display:flex;align-items:flex-start;gap:9px;margin-top:8px;color:#4d5f78;font-size:15px;line-height:1.42}.fact-dot{width:8px;height:8px;flex:none;margin-top:6px;border-radius:50%}.fact-green{background:#19ae79}.ai-entry{width:100%;margin-top:14px;border:1px solid #cfe0ff}.detail-actions{display:grid;grid-template-columns:minmax(0,1fr) minmax(0,1fr);gap:10px;width:100%}.detail-actions button{min-width:0;padding-left:8px;padding-right:8px;font-size:16px}
</style>
