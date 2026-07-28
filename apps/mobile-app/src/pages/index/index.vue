<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { getProducts } from '../../api/catalog'
import { resolveImageUrl, ApiError } from '../../api/runtime'
import ProductImage from '../../components/ProductImage.vue'
import type { Product } from '../../api/types'

const products = ref<Product[]>([])
const state = ref<'loading' | 'ready' | 'empty' | 'error'>('loading')
const errorMessage = ref('')
const productCount = computed(() => products.value.length)

function imageUrl(path?: string | null) { return resolveImageUrl(path) }
function lowestSku(product: Product) { return product.skus.reduce((best, sku) => Number(sku.salePrice) < Number(best.salePrice) ? sku : best, product.skus[0]) }
function openProduct(product: Product) { uni.navigateTo({url: `/pages/product/detail?productId=${product.id}`}) }
async function loadProducts() {
  state.value = 'loading'; errorMessage.value = ''
  try { products.value = (await getProducts()).data; state.value = products.value.length ? 'ready' : 'empty' }
  catch (error) { state.value = 'error'; errorMessage.value = error instanceof ApiError ? error.message : '商品加载失败，请重试' }
  finally { uni.stopPullDownRefresh() }
}
onMounted(loadProducts)
</script>

<template>
  <view class="mobile-page catalog-page">
    <view class="brand-bar"><view class="brand-copy"><text class="eyebrow">COMMERCEFLOW AI MALL</text><text class="page-title">商品目录</text><text class="page-subtitle">真实本地接口 · 本地演示用户</text></view><text class="brand-mark">SHOWCASE</text></view>
    <view class="catalog-summary panel"><view class="catalog-summary__main"><text class="summary-number">{{ productCount }}</text><text class="summary-label">件在售演示商品</text></view><text class="muted">数据来自 GET /api/products</text></view>
    <view class="section-title"><text>精选商品</text><text class="muted">下拉刷新</text></view>
    <view v-if="state === 'loading'" class="panel empty-box">正在读取真实商品数据…</view>
    <view v-else-if="state === 'error'" class="error-box"><text>{{ errorMessage }}</text><button class="secondary-button retry-button" @click="loadProducts">重新加载</button></view>
    <view v-else-if="state === 'empty'" class="panel empty-box">当前没有可展示的商品</view>
    <view v-else class="product-list"><view v-for="product in products" :key="product.id" class="product-card panel" @click="openProduct(product)"><ProductImage class="product-cover-wrap" :src="imageUrl(product.coverImagePath)" :alt="product.name" /><view class="product-card-content"><view class="card-heading"><text class="product-name">{{ product.name }}</text><text :class="['tag', product.status === 'ON_SALE' ? 'tag-success' : 'tag-neutral']">{{ product.status === 'ON_SALE' ? '在售' : product.status }}</text></view><text class="product-code">{{ product.productCode }}</text><text class="product-description">{{ product.description }}</text><view class="card-meta"><text>{{ product.categoryName }}</text><text>{{ product.skus.length }} 个 SKU</text><text v-if="product.skus.length">¥{{ lowestSku(product).salePrice }} 起</text></view></view></view></view>
  </view>
</template>

<style>
.catalog-summary{display:flex;align-items:flex-start;justify-content:space-between;gap:12px;padding:16px}.catalog-summary__main{display:flex;align-items:baseline;gap:8px;min-width:0}.summary-number{color:var(--cf-blue);font-size:34px;font-weight:800}.summary-label{font-size:18px;font-weight:800;line-height:1.3}.catalog-summary .muted{max-width:132px;text-align:right;font-size:16px}.product-card{display:flex;gap:14px;padding:14px;margin-bottom:12px}.product-card-content{min-width:0;flex:1;padding:1px 0}.card-heading{display:flex;align-items:flex-start;gap:6px;min-width:0}.product-name{min-width:0;flex:1;color:var(--cf-ink);font-size:21px;font-weight:800;line-height:1.35;overflow-wrap:anywhere}.card-heading .tag{flex:none;padding:5px 8px;font-size:15px}.product-code{display:block;margin-top:5px;color:#6c7890;font-family:monospace;font-size:15px;overflow-wrap:anywhere}.product-description{display:block;margin-top:7px;color:#6c778a;font-size:16px;line-height:1.4;overflow-wrap:anywhere}.card-meta{display:flex;flex-wrap:wrap;gap:5px 10px;margin-top:10px;color:#64748b;font-size:15px;line-height:1.35}.card-meta text:last-child{color:var(--cf-blue);font-weight:800}.retry-button{width:100%;margin-top:14px}
</style>
