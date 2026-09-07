<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import {
  filterProducts,
  formatMoney,
  LOW_STOCK_THRESHOLD,
  minSalePrice,
  productStatusLabel,
  productStock,
  stockLabel,
  stockState,
  type Product,
  type Sku,
} from './catalog'
import AdminBoundaryStrip from './components/AdminBoundaryStrip.vue'
import AdminSectionHeader from './components/AdminSectionHeader.vue'
import AdminStatusPill from './components/AdminStatusPill.vue'

const API_BASE = import.meta.env.VITE_API_BASE || 'http://localhost:8080/api'
type LoadState = 'loading' | 'ready' | 'empty' | 'error'
const products = ref<Product[]>([])
const selectedProduct = ref<Product | null>(null)
const selectedProductId = ref<number | null>(null)
const selectedSkuId = ref<number | null>(null)
const listState = ref<LoadState>('loading')
const detailState = ref<LoadState>('loading')
const listError = ref('')
const detailError = ref('')
const query = ref('')
const category = ref('all')
const inventoryFilter = ref<'all' | 'normal' | 'low' | 'out'>('all')
const categories = computed(() => [...new Set(products.value.map((product) => product.categoryName))])
const filteredProducts = computed(() => filterProducts(products.value, { query: query.value, category: category.value, stockState: inventoryFilter.value }))
const selectedSku = computed<Sku | null>(() => selectedProduct.value?.skus.find((sku) => sku.id === selectedSkuId.value) ?? null)
const productImage = computed(() => selectedSku.value?.imagePath || selectedProduct.value?.coverImagePath || '')

function responseError(response: Response): Error { return new Error(`请求失败（HTTP ${response.status}）`) }
async function requestProducts(): Promise<Product[]> { const response = await fetch(`${API_BASE}/products`); if (!response.ok) throw responseError(response); return response.json() as Promise<Product[]> }
async function requestProductDetail(productId: number): Promise<Product> { const response = await fetch(`${API_BASE}/products/${productId}`); if (!response.ok) throw responseError(response); return response.json() as Promise<Product> }
function selectSku(skuId: number) { selectedSkuId.value = skuId }
async function selectProduct(productId: number) {
  selectedProductId.value = productId
  selectedProduct.value = products.value.find((product) => product.id === productId) ?? null
  selectedSkuId.value = selectedProduct.value?.skus[0]?.id ?? null
  detailState.value = 'loading'; detailError.value = ''
  try { selectedProduct.value = await requestProductDetail(productId); selectedSkuId.value = selectedProduct.value.skus[0]?.id ?? null; detailState.value = 'ready' }
  catch (error) { detailState.value = 'error'; detailError.value = error instanceof Error ? error.message : '商品详情加载失败' }
}
async function loadCatalog() {
  listState.value = 'loading'; detailState.value = 'loading'; listError.value = ''; selectedProduct.value = null; selectedProductId.value = null; selectedSkuId.value = null
  try { products.value = await requestProducts(); if (products.value.length === 0) { listState.value = 'empty'; detailState.value = 'empty'; return }; listState.value = 'ready'; await selectProduct(products.value[0].id) }
  catch (error) { listState.value = 'error'; detailState.value = 'empty'; listError.value = error instanceof Error ? error.message : '商品数据加载失败' }
}
onMounted(loadCatalog)
</script>

<template>
  <section class="catalog-page" aria-labelledby="catalog-page-title">
    <header class="page-hero page-hero--catalog">
      <div><p class="eyebrow">CATALOG / PRODUCT MASTER</p><h1 id="catalog-page-title">商品与 SKU</h1><p class="subtitle">从商品主数据进入规格、价格和实时可用库存。</p></div>
      <div class="page-hero__aside"><AdminStatusPill label="真实接口" tone="green" /><p class="catalog-source">GET /api/products<br>GET /api/products/{productId}</p></div>
    </header>
    <AdminBoundaryStrip label="只读事实面" message="图片与字段来自 Java 商品接口；选择 SKU 只改变当前视图，不修改商品、价格或库存。" tone="green" />

    <div v-if="listState === 'loading'" class="state-panel state-panel--prominent" data-testid="loading-state" role="status"><span class="state-mark loading-mark" aria-hidden="true"></span><div><strong>正在加载真实商品数据</strong><p>正在请求本地 Product、SKU 与 Inventory 数据。</p></div></div>
    <div v-else-if="listState === 'error'" class="state-panel error-state state-panel--prominent" data-testid="error-state" role="alert"><span class="state-mark" aria-hidden="true">!</span><div><strong>商品数据加载失败</strong><p>{{ listError }}</p></div><button class="secondary-button" type="button" @click="loadCatalog">重新加载</button></div>
    <div v-else-if="listState === 'empty'" class="state-panel state-panel--prominent" data-testid="empty-state"><span class="state-mark" aria-hidden="true">0</span><div><strong>当前没有可展示的商品</strong><p>接口返回了空商品列表，因此不展示示例商品或虚构库存。</p></div><button class="secondary-button" type="button" @click="loadCatalog">重新加载</button></div>

    <template v-else>
      <section class="filter-bar catalog-filter-bar" aria-label="商品本地搜索与筛选">
        <label class="search-field"><span>查找商品或 SKU</span><input v-model="query" type="search" placeholder="搜索商品名称或 SKU 编码" data-testid="search-input"></label>
        <label><span>商品分类</span><select v-model="category" data-testid="category-filter"><option value="all">全部分类</option><option v-for="item in categories" :key="item" :value="item">{{ item }}</option></select></label>
        <label><span>库存状态</span><select v-model="inventoryFilter" data-testid="stock-filter"><option value="all">全部库存状态</option><option value="normal">库存正常</option><option value="low">库存偏低</option><option value="out">缺货</option></select></label>
      </section>

      <div class="catalog-layout catalog-layout--master-detail">
        <section class="product-panel product-panel--rail" aria-labelledby="product-list-title">
          <AdminSectionHeader eyebrow="PRODUCT MASTER" title="商品列表" description="点击一项查看完整 SKU 事实。" :count="`${filteredProducts.length} 项`">
            <AdminStatusPill label="API" tone="green" />
          </AdminSectionHeader>
          <div v-if="filteredProducts.length" class="product-list">
            <button v-for="product in filteredProducts" :key="product.id" :class="['product-card', { selected: selectedProductId === product.id } ]" type="button" :data-testid="`product-${product.id}`" @click="selectProduct(product.id)">
              <img class="product-card-image" :src="product.coverImagePath" :alt="`${product.name} 商品图`" :data-testid="`product-image-${product.id}`">
              <span class="product-copy"><small class="mono">{{ product.productCode }}</small><strong>{{ product.name }}</strong><em>{{ product.categoryName }}</em></span>
              <span class="product-metrics"><span><small>SKU</small><strong>{{ product.skus.length }}</strong></span><span><small>起售价</small><strong>{{ minSalePrice(product) === null ? '-' : formatMoney(minSalePrice(product)!, product.skus[0]?.currency || 'CNY') }}</strong></span><span><small>库存</small><strong>{{ productStock(product) }}</strong></span></span>
            </button>
          </div>
          <div v-else class="filtered-empty" data-testid="filtered-empty">没有匹配当前搜索或筛选条件的真实商品。</div>
        </section>

        <section class="detail-stack" aria-live="polite">
          <div v-if="detailState === 'loading'" class="panel detail-state" data-testid="detail-loading"><span class="loading-line"></span>正在加载所选商品详情…</div>
          <div v-else-if="detailState === 'error'" class="panel detail-state error-state" data-testid="detail-error"><strong>商品详情加载失败</strong><p>{{ detailError }}</p><button v-if="selectedProduct" class="secondary-button" type="button" @click="selectProduct(selectedProduct.id)">重新加载详情</button></div>
          <template v-else-if="selectedProduct">
            <section class="panel selected-product selected-product--hero" aria-labelledby="selected-product-title">
              <div class="detail-overline"><span>SELECTED PRODUCT</span><AdminStatusPill :label="productStatusLabel(selectedProduct.status)" :tone="selectedProduct.status === 'ON_SALE' ? 'green' : 'neutral'" /></div>
              <div class="detail-grid">
                <div class="product-hero-image-wrap"><img class="product-hero-image" :src="productImage" :alt="`${selectedProduct.name} 当前 SKU 图`" data-testid="product-main-image"></div>
                <div class="detail-copy"><div class="detail-title-row"><div><small class="mono">{{ selectedProduct.productCode }}</small><h2 id="selected-product-title">{{ selectedProduct.name }}</h2></div></div><p>{{ selectedProduct.description }}</p><dl><div><dt>productId</dt><dd>{{ selectedProduct.id }}</dd></div><div><dt>分类</dt><dd>{{ selectedProduct.categoryName }}</dd></div><div><dt>当前 skuId</dt><dd>{{ selectedSku?.id ?? '-' }}</dd></div><div><dt>当前 skuCode</dt><dd class="mono">{{ selectedSku?.skuCode ?? '-' }}</dd></div></dl></div>
              </div>
            </section>

            <section class="panel sku-panel" aria-labelledby="sku-table-title">
              <AdminSectionHeader eyebrow="VARIANTS / INVENTORY" title="SKU 与库存" description="选中行会同步上方商品图片和事实摘要。" :count="`${selectedProduct.skus.length} 个 SKU`"><AdminStatusPill label="实时库存" tone="green" /></AdminSectionHeader>
              <div class="table-wrap"><table><thead><tr><th>skuId</th><th>skuCode</th><th>颜色</th><th>尺寸</th><th>salePrice</th><th>currency</th><th>availableStock</th><th>库存状态</th></tr></thead><tbody><tr v-for="sku in selectedProduct.skus" :key="sku.id" :class="{ 'selected-sku-row': selectedSkuId === sku.id }" :data-testid="`sku-${sku.id}`" tabindex="0" @click="selectSku(sku.id)" @keydown.enter="selectSku(sku.id)"><td class="mono">{{ sku.id }}</td><td class="mono strong-cell">{{ sku.skuCode }}</td><td><span class="sku-color"><img :src="sku.imagePath" :alt="`${sku.color} SKU 图`" :data-testid="`sku-image-${sku.id}`"><span>{{ sku.color }}</span></span></td><td>{{ sku.size }}</td><td class="money">{{ formatMoney(sku.salePrice, sku.currency) }}</td><td class="mono">{{ sku.currency }}</td><td class="stock-value">{{ sku.availableStock }}</td><td><span :class="['stock-badge', stockState(sku.availableStock)]">{{ stockLabel(sku.availableStock) }}</span></td></tr></tbody></table></div>
            </section>

            <AdminBoundaryStrip label="库存事实说明" :message="`availableStock 来自 Java 商品接口。0 为缺货，1 至 ${LOW_STOCK_THRESHOLD - 1} 为库存偏低，其他为库存正常；页面不会修改后端库存事实。`" tone="green" />
          </template>
        </section>
      </div>
    </template>
  </section>
</template>
