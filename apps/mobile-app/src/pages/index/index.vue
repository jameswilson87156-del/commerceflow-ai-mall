<script setup lang="ts">
import { computed, onMounted, ref, watch } from 'vue'
import { getProducts } from '../../api/catalog'
import { resolveImageUrl, apiErrorMessage } from '../../api/runtime'
import ProductImage from '../../components/ProductImage.vue'
import MobileBottomNav from '../../components/MobileBottomNav.vue'
import MobileStatusPill from '../../components/MobileStatusPill.vue'
import MobileMoney from '../../components/MobileMoney.vue'
import type { Product } from '../../api/types'
import { CLIENT_DATA_MODE } from '../../config/runtime'
import { CATALOG_SORT_MODES, filterAndSortProducts, getCatalogCategories } from '../../ui/mobile-catalog-policy.mjs'

type CatalogSortMode = 'recommended' | 'price-asc' | 'price-desc' | 'name-asc'

const products = ref<Product[]>([])
const state = ref<'loading' | 'ready' | 'empty' | 'error'>('loading')
const errorMessage = ref('')
const productCount = computed(() => products.value.length)
const query = ref('')
const selectedCategory = ref('全部')
const sortMode = ref<CatalogSortMode>('recommended')
const categories = computed(() => getCatalogCategories(products.value))
const visibleProducts = computed(() => filterAndSortProducts(products.value, {
  query: query.value,
  category: selectedCategory.value,
  sort: sortMode.value
}) as Product[])
const visibleProductCount = computed(() => visibleProducts.value.length)
const sortLabel = computed(() => CATALOG_SORT_MODES.find(option => option.value === sortMode.value)?.label || '推荐排序')
const hasDiscoveryFilters = computed(() => Boolean(query.value.trim()) || selectedCategory.value !== '全部' || sortMode.value !== 'recommended')
function imageUrl(path?: string | null) { return resolveImageUrl(path) }
function lowestSku(product: Product) { return product.skus.reduce((best, sku) => Number(sku.salePrice) < Number(best.salePrice) ? sku : best, product.skus[0]) }
function openProduct(product: Product) { uni.navigateTo({ url: `/pages/product/detail?productId=${product.id}` }) }
function selectCategory(category: string) { selectedCategory.value = category }
function cycleSort() {
  const currentIndex = CATALOG_SORT_MODES.findIndex(option => option.value === sortMode.value)
  const next = CATALOG_SORT_MODES[(currentIndex + 1) % CATALOG_SORT_MODES.length]
  sortMode.value = next.value as CatalogSortMode
}
function clearDiscovery() {
  query.value = ''
  selectedCategory.value = '全部'
  sortMode.value = 'recommended'
}
async function loadProducts() {
  state.value = 'loading'; errorMessage.value = ''
  try { products.value = (await getProducts()).data; state.value = products.value.length ? 'ready' : 'empty' }
  catch (error) { state.value = 'error'; errorMessage.value = apiErrorMessage(error, '商品加载失败，请重试') }
  finally { uni.stopPullDownRefresh() }
}
watch(categories, value => {
  if (!value.includes(selectedCategory.value)) selectedCategory.value = '全部'
})
onMounted(loadProducts)
</script>

<template>
  <view class="mobile-page mobile-page--with-bottom-nav catalog-page">
    <view class="storefront-topbar">
      <view class="storefront-wordmark">
        <text class="storefront-wordmark__mark">CF</text>
        <view>
          <text class="storefront-wordmark__name">CommerceFlow</text>
          <text class="storefront-wordmark__meta">THE EVERYDAY EDIT</text>
        </view>
      </view>
      <text class="storefront-edition">日常精选 / 01</text>
    </view>

    <view class="storefront-hero">
      <view class="storefront-hero__copy">
        <text class="storefront-kicker">少一点繁复，多一点日常。</text>
        <text class="storefront-hero-title">日常，</text>
        <text class="storefront-hero-title storefront-hero-title--second">自有分寸</text>
        <text class="page-subtitle">衣物与随身好物，为平常的一天留一点讲究。</text>
        <text class="storefront-boundary">{{ CLIENT_DATA_MODE === 'LOCAL_DEMO_FIXTURE' ? '本地演示数据' : '商品目录 · API 数据' }}</text>
      </view>
      <button v-if="products[0] && state === 'ready'" class="storefront-feature" type="button" :aria-label="`查看${products[0].name}`" @click="openProduct(products[0])">
        <ProductImage :src="imageUrl(products[0].coverImagePath)" :alt="products[0].name" />
        <span class="storefront-feature__caption">{{ products[0].name }} <span aria-hidden="true">↗</span></span>
      </button>
    </view>

    <view class="catalog-toolbar">
      <view class="catalog-toolbar__copy">
        <text class="catalog-toolbar__title">精选好物</text>
        <text class="catalog-toolbar__count" aria-live="polite">{{ visibleProductCount }} / {{ productCount }} 件在售</text>
      </view>
      <button class="catalog-sort-button" type="button" :aria-label="`切换商品排序，当前为${sortLabel}`" @click="cycleSort">
        <text>{{ sortLabel }}</text><text aria-hidden="true"> ↕</text>
      </button>
    </view>

    <view class="catalog-discovery" aria-label="商品筛选和搜索">
      <view class="catalog-search">
        <text class="catalog-search__icon" aria-hidden="true">⌕</text>
        <input id="catalog-search" v-model="query" class="catalog-search__input" type="text" maxlength="60" placeholder="搜索商品、分类或编号" aria-label="搜索商品、分类或编号" confirm-type="search" />
        <button v-if="query" class="catalog-search__clear" type="button" aria-label="清除商品搜索" @click="query = ''">×</button>
      </view>
      <scroll-view class="catalog-category-scroller" scroll-x aria-label="商品分类">
        <view class="catalog-category-list">
          <button v-for="category in categories" :key="category" :class="['catalog-category', { 'catalog-category--active': category === selectedCategory }]" type="button" :aria-pressed="category === selectedCategory" @click="selectCategory(category)">
            {{ category }}
          </button>
        </view>
      </scroll-view>
      <view class="catalog-result-line" aria-live="polite">
        <text>{{ hasDiscoveryFilters ? `已按${selectedCategory === '全部' ? '全部分类' : selectedCategory}筛选` : '全部真实商品' }}</text>
        <text v-if="sortMode !== 'recommended'"> · {{ sortLabel }}</text>
      </view>
    </view>

    <view v-if="state === 'loading'" class="panel empty-box">正在读取真实商品数据…</view>
    <view v-else-if="state === 'error'" class="error-box"><text>{{ errorMessage }}</text><button class="secondary-button retry-button" @click="loadProducts">重新加载</button></view>
    <view v-else-if="state === 'empty'" class="panel empty-box">当前没有可展示的商品</view>
    <view v-else-if="!visibleProducts.length" class="panel empty-box catalog-filter-empty">
      <text>{{ query ? `没有找到与“${query}”相关的商品` : '当前筛选条件下没有商品' }}</text>
      <button class="secondary-button retry-button" type="button" @click="clearDiscovery">清除筛选</button>
    </view>
    <view v-else class="product-list product-grid">
      <button v-for="(product, index) in visibleProducts" :key="product.id" class="product-card store-product-card" type="button" @click="openProduct(product)">
        <view class="store-product-media">
          <ProductImage class="store-product-image" :src="imageUrl(product.coverImagePath)" :alt="product.name" />
          <text class="store-product-index">{{ String(index + 1).padStart(2, '0') }}</text>
          <MobileStatusPill :label="product.status === 'ON_SALE' ? '在售' : product.status" :tone="product.status === 'ON_SALE' ? 'success' : 'neutral'" />
        </view>
        <view class="store-product-copy">
          <text class="store-product-category">{{ product.categoryName }}</text>
          <text class="product-name">{{ product.name }}</text>
          <text class="product-description">{{ product.description }}</text>
          <view class="store-product-footer">
            <MobileMoney v-if="product.skus.length" :amount="lowestSku(product).salePrice" compact />
          <text class="store-product-arrow" aria-hidden="true">+</text>
          </view>
          <text class="store-product-meta">{{ product.skus.length }} 种规格 · 可进入详情选择</text>
        </view>
      </button>
    </view>
    <MobileBottomNav active="home" />
  </view>
</template>

<style>
.catalog-page { background: #fbfaf7; }
.storefront-topbar { display: flex; align-items: center; justify-content: space-between; gap: 12px; min-height: 42px; margin: 0 0 35px; border-bottom: 1px solid #e6e5df; padding-bottom: 13px; }
.storefront-wordmark { display: flex; align-items: center; gap: 9px; min-width: 0; }
.storefront-wordmark__mark { display: grid; width: 28px; height: 28px; place-items: center; border-radius: 50%; background: var(--cf-blue); color: #fff; font-size: 10px; font-weight: 900; letter-spacing: -.04em; }
.storefront-wordmark__name, .storefront-wordmark__meta { display: block; }
.storefront-wordmark__name { color: var(--cf-ink); font-size: 11px; font-weight: 900; letter-spacing: .12em; line-height: 1; }
.storefront-wordmark__meta { margin-top: 5px; color: #8a928d; font: 9px/1 ui-monospace, SFMono-Regular, Consolas, monospace; letter-spacing: .08em; }
.storefront-topbar .mobile-status-pill { flex: none; }
.storefront-hero { margin: 0 0 42px; }
.storefront-kicker { display: block; color: var(--cf-blue); font: 800 10px/1.2 ui-monospace, SFMono-Regular, Consolas, monospace; letter-spacing: .12em; }
.storefront-hero .page-title { max-width: 310px; margin-top: 14px; color: #18231e; font-size: 35px; font-weight: 820; letter-spacing: -.06em; line-height: 1.08; }
.storefront-hero .page-subtitle { max-width: 280px; margin-top: 12px; color: #68746d; font-size: 13px; line-height: 1.55; }
.storefront-boundary { display: block; margin-top: 19px; color: #99a09a; font: 9px/1.3 ui-monospace, SFMono-Regular, Consolas, monospace; letter-spacing: .04em; }
.catalog-toolbar { display: flex; align-items: flex-end; justify-content: space-between; gap: 12px; border-top: 1px solid #dfe1dc; padding: 13px 0 10px; }
.catalog-toolbar__copy { display: flex; align-items: baseline; gap: 9px; min-width: 0; }
.catalog-toolbar__title { color: var(--cf-ink); font-size: 15px; font-weight: 850; }
.catalog-toolbar__count, .catalog-toolbar__sort { color: #8a938d; font: 10px/1.3 ui-monospace, SFMono-Regular, Consolas, monospace; }
.catalog-toolbar__sort { flex: none; letter-spacing: .05em; }
.product-grid { display: grid; grid-template-columns: repeat(2, minmax(0, 1fr)); gap: 26px 11px; }
.store-product-card { display: block; width: 100%; min-width: 0; padding: 0; border: 0; background: transparent; color: inherit; text-align: left; }
.store-product-media { position: relative; width: 100%; aspect-ratio: 1 / 1.08; overflow: hidden; background: #f0f0ec; }
.store-product-image { width: 100%; height: 100%; }
.store-product-image.image-frame { border-radius: 0; }
.store-product-media .mobile-status-pill { position: absolute; top: 8px; left: 8px; padding: 4px 6px; font-size: 9px; }
.store-product-index { position: absolute; right: 9px; bottom: 8px; color: rgba(27, 42, 34, .55); font: 10px/1 ui-monospace, SFMono-Regular, Consolas, monospace; }
.store-product-copy { min-width: 0; padding-top: 11px; }
.store-product-category { display: block; overflow: hidden; color: #7c867f; font: 9px/1.2 ui-monospace, SFMono-Regular, Consolas, monospace; letter-spacing: .05em; text-overflow: ellipsis; text-transform: uppercase; white-space: nowrap; }
.store-product-card .product-name { display: -webkit-box; min-width: 0; margin-top: 6px; overflow: hidden; color: #1c2822; font-size: 15px; font-weight: 820; line-height: 1.25; -webkit-box-orient: vertical; -webkit-line-clamp: 2; }
.store-product-card .product-description { display: -webkit-box; min-height: 30px; margin-top: 7px; overflow: hidden; color: #7c8780; font-size: 11px; line-height: 1.4; -webkit-box-orient: vertical; -webkit-line-clamp: 2; }
.store-product-footer { display: flex; align-items: center; justify-content: space-between; gap: 8px; margin-top: 11px; }
.store-product-footer .mobile-money { color: var(--cf-blue); font-size: 16px; }
.store-product-arrow { display: grid; width: 25px; height: 25px; place-items: center; border: 1px solid #d7ddd8; border-radius: 50%; color: var(--cf-blue); font-size: 15px; line-height: 1; }
.store-product-meta { display: block; margin-top: 8px; overflow: hidden; color: #a0a7a1; font: 9px/1.2 ui-monospace, SFMono-Regular, Consolas, monospace; text-overflow: ellipsis; white-space: nowrap; }
.store-product-card:active .store-product-image { opacity: .82; }
.retry-button { width: 100%; margin-top: 14px; }
@media (max-width: 380px) { .storefront-hero .page-title { font-size: 31px; }.catalog-toolbar__sort { display: none; }.store-product-card .product-name { font-size: 14px; }.store-product-card .product-description { font-size: 10px; } }

/* Apple-inspired storefront pass: the product image carries the page, UI chrome stays quiet. */
.catalog-page { background: #f5f5f7; }
.storefront-topbar { min-height: 44px; margin-bottom: 47px; border-bottom: 0; padding-bottom: 0; }
.storefront-wordmark { gap: 9px; }
.storefront-wordmark__mark { width: 27px; height: 27px; border-radius: 8px; background: #1d1d1f; font-size: 9px; letter-spacing: -.06em; }
.storefront-wordmark__name { color: #1d1d1f; font-size: 14px; font-weight: 650; letter-spacing: -.025em; }
.storefront-wordmark__meta { margin-top: 4px; color: #6e6e73; font: 10px/1 -apple-system, BlinkMacSystemFont, "SF Pro Text", "PingFang SC", sans-serif; letter-spacing: 0; }
.storefront-topbar .mobile-status-pill { border: 1px solid #d7eadb; background: #fff; color: #248a3d; }
.storefront-hero { margin-bottom: 43px; }
.storefront-kicker { color: #6e6e73; font: 600 11px/1.2 -apple-system, BlinkMacSystemFont, "SF Pro Text", "PingFang SC", sans-serif; letter-spacing: 0; }
.storefront-hero-title { display: block; margin-top: 14px; color: #1d1d1f; font-size: 40px; font-weight: 700; letter-spacing: -.07em; line-height: 1.04; }
.storefront-hero-title--second { margin-top: 0; }
.storefront-hero .page-subtitle { max-width: 280px; margin-top: 17px; color: #6e6e73; font-size: 14px; line-height: 1.5; }
.storefront-boundary { margin-top: 21px; color: #8d8d92; font: 10px/1.3 -apple-system, BlinkMacSystemFont, "SF Pro Text", "PingFang SC", sans-serif; letter-spacing: 0; }
.catalog-toolbar { border-top: 0; border-bottom: 1px solid #d2d2d7; padding: 0 0 14px; }
.catalog-toolbar__title { color: #1d1d1f; font-size: 18px; font-weight: 650; letter-spacing: -.025em; }
.catalog-toolbar__count { color: #6e6e73; font: 12px/1.2 -apple-system, BlinkMacSystemFont, "SF Pro Text", "PingFang SC", sans-serif; }
.catalog-toolbar__sort { color: #0071e3; font: 12px/1.2 -apple-system, BlinkMacSystemFont, "SF Pro Text", "PingFang SC", sans-serif; letter-spacing: 0; }
.product-grid { gap: 22px 12px; }
.store-product-card { border-radius: 20px; }
.store-product-card--feature { grid-column: 1 / -1; }
.store-product-media { aspect-ratio: 1 / 1.04; border-radius: 20px; background: #fff; }
.store-product-image.image-frame { border-radius: 20px; background: #fff; }
.store-product-media .mobile-status-pill { top: 12px; left: 12px; border: 1px solid rgba(210, 210, 215, .9); background: rgba(255, 255, 255, .88); color: #248a3d; }
.store-product-index { display: none; }
.store-product-copy { padding: 15px 3px 2px; }
.store-product-category { color: #6e6e73; font: 11px/1.2 -apple-system, BlinkMacSystemFont, "SF Pro Text", "PingFang SC", sans-serif; letter-spacing: 0; text-transform: none; }
.store-product-card .product-name { margin-top: 7px; color: #1d1d1f; font-size: 16px; font-weight: 650; letter-spacing: -.025em; line-height: 1.25; }
.store-product-card .product-description { min-height: 0; margin-top: 6px; color: #6e6e73; font-size: 12px; line-height: 1.4; }
.store-product-footer { margin-top: 12px; }
.store-product-footer .mobile-money { color: #1d1d1f; font-size: 17px; font-weight: 650; }
.store-product-arrow { width: 27px; height: 27px; border-color: #d2d2d7; color: #0071e3; font-size: 18px; font-weight: 350; }
.store-product-meta { margin-top: 8px; color: #8d8d92; font: 10px/1.2 -apple-system, BlinkMacSystemFont, "SF Pro Text", "PingFang SC", sans-serif; }
.store-product-card--feature .store-product-media { aspect-ratio: 1.62 / 1; }
.store-product-card--feature .store-product-media { height: 220px; aspect-ratio: auto; }
.store-product-card--feature .store-product-image { height: 220px; }
.store-product-card--feature .store-product-image > img { object-fit: contain; }
.store-product-card--feature .store-product-copy { padding-top: 17px; }
.store-product-card--feature .product-name { font-size: 22px; font-weight: 650; letter-spacing: -.04em; }
.store-product-card--feature .product-description { max-width: 280px; font-size: 13px; }
.store-product-card--feature .store-product-footer .mobile-money { font-size: 20px; }
.store-product-card--feature .store-product-arrow { width: 30px; height: 30px; }
@media (max-width: 380px) { .storefront-hero-title { font-size: 36px; }.store-product-card--feature .product-name { font-size: 20px; }.store-product-card .product-name { font-size: 15px; }.store-product-card .product-description { font-size: 11px; } }

/* EC-UI-02: discovery controls stay local to the real product response. */
.catalog-discovery { margin: 17px 0 24px; }
.catalog-search { display: flex; align-items: center; gap: 8px; min-height: 44px; padding: 0 12px; border: 1px solid #d2d2d7; border-radius: 14px; background: #fff; }
.catalog-search:focus-within { border-color: #0071e3; box-shadow: 0 0 0 3px rgba(0, 113, 227, .12); }
.catalog-search__icon { flex: none; color: #6e6e73; font-size: 21px; line-height: 1; transform: translateY(-1px); }
.catalog-search__input { min-width: 0; flex: 1; height: 42px; padding: 0; border: 0; outline: 0; background: transparent; color: #1d1d1f; font-size: 14px; line-height: 42px; }
.catalog-search__input::placeholder { color: #8d8d92; }
.catalog-search__clear { display: grid; flex: none; width: 24px; height: 24px; place-items: center; border-radius: 50%; background: #e8e8ed; color: #6e6e73; font-size: 17px; line-height: 1; }
.catalog-category-scroller { width: 100%; margin-top: 13px; white-space: nowrap; }
.catalog-category-list { display: inline-flex; gap: 7px; min-width: 100%; padding: 1px 1px 3px; }
.catalog-category { min-height: 32px; padding: 6px 13px; border: 1px solid #d2d2d7; border-radius: 999px; background: #fff; color: #6e6e73; font-size: 12px; line-height: 1.2; white-space: nowrap; }
.catalog-category--active { border-color: #1d1d1f; background: #1d1d1f; color: #fff; }
.catalog-category:focus-visible, .catalog-sort-button:focus-visible, .catalog-search__clear:focus-visible { outline: 3px solid rgba(0, 113, 227, .28); outline-offset: 2px; }
.catalog-result-line { margin-top: 10px; color: #8d8d92; font-size: 11px; line-height: 1.35; }
.catalog-sort-button { display: inline-flex; align-items: center; gap: 2px; min-height: 30px; padding: 4px 0 4px 8px; background: transparent; color: #0071e3; font-size: 12px; line-height: 1.2; white-space: nowrap; }
.catalog-filter-empty { margin-top: 8px; }
.catalog-filter-empty .retry-button { max-width: 220px; margin-right: auto; margin-left: auto; }
@media (min-width: 760px) {
  .catalog-search { max-width: 460px; }
  .catalog-category-scroller { max-width: 100%; }
}
</style>
