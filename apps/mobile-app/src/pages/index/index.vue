<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { getProducts } from '../../api/catalog'
import { resolveImageUrl, ApiError } from '../../api/runtime'
import type { Product } from '../../api/types'

const products = ref<Product[]>([])
const state = ref<'loading' | 'ready' | 'empty' | 'error'>('loading')
const errorMessage = ref('')
const demoUserLabel = '本地演示用户'
const productCount = computed(() => products.value.length)

function imageUrl(path?: string | null) { return resolveImageUrl(path) }
function lowestSku(product: Product) { return product.skus.reduce((best, sku) => Number(sku.salePrice) < Number(best.salePrice) ? sku : best, product.skus[0]) }
function openProduct(product: Product) { uni.navigateTo({url: `/pages/product/detail?productId=${product.id}`}) }
async function loadProducts() {
  state.value = 'loading'
  errorMessage.value = ''
  try {
    const response = await getProducts()
    products.value = response.data
    state.value = products.value.length ? 'ready' : 'empty'
  } catch (error) {
    state.value = 'error'
    errorMessage.value = error instanceof ApiError ? error.message : '商品加载失败，请重试'
  } finally { uni.stopPullDownRefresh() }
}
function retry() { loadProducts() }
onMounted(loadProducts)
</script>

<template>
  <view class="mobile-page">
    <view class="brand-bar">
      <view><text class="eyebrow">COMMERCEFLOW AI MALL</text><text class="page-title">商品目录</text><text class="page-subtitle">真实本地接口 · {{ demoUserLabel }}</text></view>
      <text class="brand-mark">SHOWCASE</text>
    </view>
    <view class="catalog-summary panel"><text class="summary-number">{{ productCount }}</text><text class="summary-label">件在售演示商品</text><text class="muted">数据来自 GET /api/products</text></view>
    <view class="section-title"><text>精选商品</text><text class="muted">下拉刷新</text></view>
    <view v-if="state === 'loading'" class="panel empty-box">正在读取真实商品数据…</view>
    <view v-else-if="state === 'error'" class="error-box"><text>{{ errorMessage }}</text><button class="secondary-button retry-button" @click="retry">重新加载</button></view>
    <view v-else-if="state === 'empty'" class="panel empty-box">当前没有可展示的商品</view>
    <view v-else class="product-list">
      <view v-for="product in products" :key="product.id" class="product-card panel" @click="openProduct(product)">
        <view v-if="product.coverImagePath" class="product-cover-wrap"><img class="product-cover" :src="imageUrl(product.coverImagePath)" /></view>
        <view v-else class="product-cover-wrap image-fallback">暂无商品图片</view>
        <view class="product-card-content"><view class="card-heading"><text class="product-name">{{ product.name }}</text><text :class="['tag', product.status === 'ON_SALE' ? 'tag-success' : 'tag-neutral']">{{ product.status === 'ON_SALE' ? '在售' : product.status }}</text></view><text class="product-code">{{ product.productCode }}</text><text class="product-description">{{ product.description }}</text><view class="card-meta"><text>{{ product.categoryName }}</text><text>{{ product.skus.length }} 个 SKU</text><text v-if="product.skus.length">¥{{ lowestSku(product).salePrice }} 起</text></view></view>
      </view>
    </view>
  </view>
</template>

<style>
.catalog-summary{display:flex;align-items:baseline;gap:12rpx;padding:22rpx 24rpx}.summary-number{color:#2563eb;font-size:44rpx;font-weight:800}.summary-label{font-size:24rpx;font-weight:700}.catalog-summary .muted{margin-left:auto}.product-card{display:flex;gap:20rpx;padding:18rpx;margin-bottom:16rpx}.product-cover-wrap{width:190rpx;height:190rpx;flex:none;border-radius:14rpx;overflow:hidden;background:#f3f5f8}.product-cover{display:block;width:100%;height:100%}.product-card-content{min-width:0;flex:1;padding:4rpx 0}.card-heading{display:flex;align-items:flex-start;gap:10rpx}.product-name{flex:1;color:#172033;font-size:28rpx;font-weight:800;line-height:1.35}.product-code{display:block;margin-top:8rpx;color:#6c7890;font-family:monospace;font-size:19rpx}.product-description{display:block;margin-top:12rpx;color:#6c778a;font-size:22rpx;line-height:1.45}.card-meta{display:flex;flex-wrap:wrap;gap:10rpx 18rpx;margin-top:16rpx;color:#64748b;font-size:20rpx}.card-meta text:last-child{color:#2563eb;font-weight:700}.retry-button{display:block;width:100%;margin-top:18rpx}
</style>
