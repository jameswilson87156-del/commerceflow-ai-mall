<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { getProduct } from '../../api/catalog'
import { addCartItem } from '../../api/cart'
import { ApiError, resolveImageUrl } from '../../api/runtime'
import MobileHeader from '../../components/MobileHeader.vue'
import ProductImage from '../../components/ProductImage.vue'
import type { Product } from '../../api/types'

const product = ref<Product | null>(null)
const selectedSkuId = ref<number | null>(null)
const state = ref<'loading' | 'ready' | 'error'>('loading')
const errorMessage = ref('')
const productId = ref(0)
const actionMessage = ref('')
const adding = ref(false)
const selectedSku = computed(() => product.value?.skus.find(sku => sku.id === selectedSkuId.value) || null)
const canAdd = computed(() => Boolean(product.value?.status === 'ON_SALE' && selectedSku.value && selectedSku.value.availableStock > 0 && !adding.value))

function load(id: number) {
  state.value = 'loading'
  errorMessage.value = ''
  getProduct(id).then(response => {
    product.value = response.data
    const first = response.data.skus.find(sku => sku.availableStock > 0) || response.data.skus[0]
    selectedSkuId.value = first?.id || null
    state.value = 'ready'
  }).catch(error => {
    state.value = 'error'
    errorMessage.value = error instanceof ApiError ? error.message : '商品详情加载失败，请重试'
  })
}
function chooseSku(id: number) {
  const sku = product.value?.skus.find(item => item.id === id)
  if (!sku || sku.availableStock === 0) return
  selectedSkuId.value = id
  actionMessage.value = ''
}
function imageUrl(path?: string | null) { return resolveImageUrl(path) }
function goBack() { uni.navigateBack() }
function retry() { if (productId.value) load(productId.value) }
function openCart() { uni.navigateTo({url: '/pages/cart/index'}) }
async function addToCart() {
  if (!selectedSku.value || !canAdd.value) return
  adding.value = true
  actionMessage.value = ''
  try {
    await addCartItem(selectedSku.value.id)
    actionMessage.value = '已加入服务端购物车'
    uni.showToast({title: '已加入购物车', icon: 'success'})
  } catch (error) {
    actionMessage.value = error instanceof ApiError ? error.message : '加入购物车失败，请重试'
  } finally { adding.value = false }
}
onMounted(async () => {
  const { queryParam } = await import('../../platform/h5')
  productId.value = Number(queryParam('productId'))
  load(productId.value)
})
</script>

<template>
  <view class="mobile-page detail-page">
    <MobileHeader title="商品详情" eyebrow="COMMERCEFLOW AI MALL" :back="true" @back="goBack" />
    <view v-if="state === 'loading'" class="panel empty-box">正在读取商品详情…</view>
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
          <view v-for="sku in product.skus" :key="sku.id" :class="['sku-choice', {selected: sku.id === selectedSkuId, unavailable: sku.availableStock === 0}]" @click="chooseSku(sku.id)">
            <ProductImage class="choice-image" :src="imageUrl(sku.imagePath)" :alt="`${sku.color} ${sku.size}`" />
            <view class="choice-copy"><text class="choice-color">{{ sku.color }}</text><text class="choice-size">{{ sku.size }}</text><text class="sku-code">{{ sku.skuCode }}</text></view>
            <text :class="['tag', sku.availableStock === 0 ? 'tag-danger' : sku.availableStock < 10 ? 'tag-warning' : 'tag-success']">{{ sku.availableStock === 0 ? '缺货' : sku.availableStock < 10 ? '库存偏低' : '库存正常' }}</text>
          </view>
        </view>
        <view v-if="selectedSku" class="selected-summary">
          <ProductImage class="selected-image" :src="imageUrl(selectedSku.imagePath)" :alt="selectedSku.color" />
          <view class="selected-copy"><text class="summary-label">当前选择</text><text class="selected-title">{{ selectedSku.color }} / {{ selectedSku.size }}</text><text class="sku-code">{{ selectedSku.skuCode }}</text></view>
          <view class="selected-stock"><text class="selected-price">¥{{ selectedSku.salePrice }}</text><text class="currency">{{ selectedSku.currency }}</text><text :class="['stock-copy', selectedSku.availableStock === 0 ? 'stock-danger' : selectedSku.availableStock < 10 ? 'stock-warning' : 'stock-success']">{{ selectedSku.availableStock === 0 ? '暂无库存' : `可用库存 ${selectedSku.availableStock}` }}</text></view>
        </view>
      </view>

      <view class="facts-card panel">
        <view class="section-title"><text>库存信息</text><text class="info-dot">i</text></view>
        <view class="fact-row"><text class="fact-dot fact-green" /><text>当前可用库存：{{ selectedSku?.availableStock ?? 0 }} 件</text></view>
        <view class="fact-row"><text class="fact-dot fact-green" /><text>库存数据来自 Java 商品与库存接口</text></view>
        <view class="fact-row"><text class="fact-dot fact-green" /><text>下单时会再次校验真实库存</text></view>
      </view>
      <text v-if="actionMessage" class="status-line">{{ actionMessage }}</text>
      <view class="bottom-action"><view class="detail-actions"><button class="secondary-button" @click="openCart">查看购物车</button><button class="primary-button" :disabled="!canAdd" @click="addToCart">{{ adding ? '加入中…' : selectedSku?.availableStock === 0 ? '暂无库存' : '加入购物车' }}</button></view></view>
    </template>
  </view>
</template>

<style>
.detail-page{padding-bottom:184px}.product-hero{display:flex;gap:16px;padding:14px;overflow:hidden}.hero-image{width:49%;height:248px;flex:none;border-radius:16px}.hero-copy{min-width:0;flex:1;padding:6px 4px}.category-label{display:block;color:var(--cf-blue);font-size:19px;font-weight:700}.detail-title{display:block;margin-top:9px;color:var(--cf-ink);font-size:29px;font-weight:800;line-height:1.25}.product-code{display:block;margin-top:7px;color:#6e7b90;font-family:monospace;font-size:18px}.detail-description{display:block;margin-top:11px;color:#66758a;font-size:20px;line-height:1.4}.hero-price{display:flex;align-items:baseline;gap:8px;margin-top:14px;color:var(--cf-blue);font-size:32px;font-weight:800}.hero-price .currency{color:var(--cf-muted);font-size:18px;font-weight:600}.hero-copy>.tag{margin-top:10px}.spec-panel{margin-top:14px;padding:0 14px 14px}.spec-panel .section-title{margin:0;padding:16px 2px 12px}.sku-grid{display:flex;flex-direction:column;gap:9px}.sku-choice{display:flex;align-items:center;gap:10px;min-height:80px;padding:8px 10px;border:1px solid var(--cf-line);border-radius:14px;background:#fff}.sku-choice.selected{border:2px solid var(--cf-blue);background:var(--cf-blue-soft)}.sku-choice.unavailable{opacity:.55}.choice-image{width:58px;height:58px;flex:none;border-radius:10px}.choice-copy{min-width:0;flex:1}.choice-color{display:inline-block;font-size:22px;font-weight:800}.choice-size{display:inline-block;margin-left:8px;color:#53647c;font-size:20px}.choice-copy .sku-code{margin-top:3px;white-space:nowrap;overflow:hidden;text-overflow:ellipsis}.selected-summary{display:flex;align-items:center;gap:12px;margin-top:12px;padding:12px;border:1px solid #dce5f0;border-radius:14px;background:#fbfdff}.selected-image{width:72px;height:72px;flex:none;border-radius:10px}.selected-copy{min-width:0;flex:1}.summary-label{display:block;color:var(--cf-muted);font-size:18px}.selected-title{display:block;margin-top:3px;font-size:23px;font-weight:800}.selected-copy .sku-code{margin-top:4px}.selected-stock{text-align:right}.selected-price{display:block;color:var(--cf-blue);font-size:26px;font-weight:800}.selected-stock .currency{display:block}.stock-copy{display:block;margin-top:7px;font-size:18px;font-weight:700}.stock-success{color:var(--cf-green)}.stock-warning{color:var(--cf-orange)}.stock-danger{color:var(--cf-red)}.facts-card{margin-top:14px;padding:0 18px 16px}.facts-card .section-title{margin:0;padding:16px 0 10px}.info-dot{display:flex;align-items:center;justify-content:center;width:30px;height:30px;border:2px solid var(--cf-blue);border-radius:50%;color:var(--cf-blue);font-size:18px;font-weight:800}.fact-row{display:flex;align-items:flex-start;gap:10px;margin-top:9px;color:#4d5f78;font-size:20px;line-height:1.4}.fact-dot{width:10px;height:10px;flex:none;margin-top:8px;border-radius:50%}.fact-green{background:#19ae79}.detail-actions{display:flex;gap:12px}.detail-actions button{flex:1}
</style>
