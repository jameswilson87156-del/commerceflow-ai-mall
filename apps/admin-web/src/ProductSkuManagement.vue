<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import {
  filterProducts,
  formatMoney,
  LOW_STOCK_THRESHOLD,
  productStock,
  stockLabel,
  stockState,
  type Product,
} from './catalog'

const API_BASE = import.meta.env.VITE_API_BASE || 'http://localhost:8080/api'

type LoadState = 'loading' | 'ready' | 'empty' | 'error'

const products = ref<Product[]>([])
const selectedProduct = ref<Product | null>(null)
const selectedProductId = ref<number | null>(null)
const listState = ref<LoadState>('loading')
const detailState = ref<LoadState>('loading')
const listError = ref('')
const detailError = ref('')
const query = ref('')
const category = ref('all')
const inventoryFilter = ref<'all' | 'normal' | 'low' | 'out'>('all')

const categories = computed(() => [...new Set(products.value.map((product) => product.categoryName))])
const filteredProducts = computed(() => filterProducts(products.value, {
  query: query.value,
  category: category.value,
  stockState: inventoryFilter.value,
}))

function responseError(response: Response): Error {
  return new Error(`请求失败（HTTP ${response.status}）`)
}

async function requestProducts(): Promise<Product[]> {
  const response = await fetch(`${API_BASE}/products`)
  if (!response.ok) throw responseError(response)
  return response.json() as Promise<Product[]>
}

async function requestProductDetail(productId: number): Promise<Product> {
  const response = await fetch(`${API_BASE}/products/${productId}`)
  if (!response.ok) throw responseError(response)
  return response.json() as Promise<Product>
}

async function selectProduct(productId: number) {
  selectedProductId.value = productId
  selectedProduct.value = products.value.find((product) => product.id === productId) ?? null
  detailState.value = 'loading'
  detailError.value = ''
  try {
    selectedProduct.value = await requestProductDetail(productId)
    detailState.value = 'ready'
  } catch (error) {
    detailState.value = 'error'
    detailError.value = error instanceof Error ? error.message : '商品详情加载失败'
  }
}

async function loadCatalog() {
  listState.value = 'loading'
  detailState.value = 'loading'
  listError.value = ''
  selectedProduct.value = null
  try {
    products.value = await requestProducts()
    if (products.value.length === 0) {
      listState.value = 'empty'
      detailState.value = 'empty'
      return
    }
    listState.value = 'ready'
    await selectProduct(products.value[0].id)
  } catch (error) {
    listState.value = 'error'
    detailState.value = 'empty'
    listError.value = error instanceof Error ? error.message : '商品数据加载失败'
  }
}

onMounted(loadCatalog)
</script>

<template>
  <section class="catalog-page" aria-labelledby="catalog-page-title">
    <header class="catalog-heading">
      <div>
        <p class="eyebrow">商品目录 / 本地真实接口</p>
        <h1 id="catalog-page-title">商品与 SKU 管理</h1>
        <p class="subtitle">读取本地商城接口中的商品、SKU、价格与可用库存。当前为只读演示，不提供编辑或批量操作。</p>
      </div>
      <div class="count-card" aria-label="真实商品数量">
        <span>真实商品数量</span>
        <strong data-testid="product-count">{{ listState === 'ready' ? products.length : '—' }}</strong>
        <small>来自 GET /api/products</small>
      </div>
    </header>

    <div v-if="listState === 'loading'" class="state-panel" data-testid="loading-state" role="status">
      <span class="state-mark loading-mark" aria-hidden="true"></span>
      <div><strong>正在加载真实商品数据</strong><p>正在请求本地 Product 与 SKU 数据。</p></div>
    </div>

    <div v-else-if="listState === 'error'" class="state-panel error-state" data-testid="error-state" role="alert">
      <span class="state-mark" aria-hidden="true">!</span>
      <div><strong>商品数据加载失败</strong><p>{{ listError }}</p></div>
      <button class="secondary-button" type="button" @click="loadCatalog">重新加载</button>
    </div>

    <div v-else-if="listState === 'empty'" class="state-panel" data-testid="empty-state">
      <span class="state-mark" aria-hidden="true">0</span>
      <div><strong>当前没有可展示的商品</strong><p>接口返回了空商品列表，因此不展示示例商品或虚构库存。</p></div>
      <button class="secondary-button" type="button" @click="loadCatalog">重新加载</button>
    </div>

    <template v-else>
      <section class="filter-bar" aria-label="真实商品本地筛选">
        <label class="search-field">
          <span>搜索</span>
          <input v-model="query" type="search" placeholder="搜索商品名称或 SKU 编码" data-testid="search-input">
        </label>
        <label>
          <span>分类</span>
          <select v-model="category" data-testid="category-filter">
            <option value="all">全部分类</option>
            <option v-for="item in categories" :key="item" :value="item">{{ item }}</option>
          </select>
        </label>
        <label>
          <span>库存状态</span>
          <select v-model="inventoryFilter" data-testid="stock-filter">
            <option value="all">全部库存状态</option>
            <option value="normal">库存正常</option>
            <option value="low">库存偏低</option>
            <option value="out">缺货</option>
          </select>
        </label>
        <p class="filter-note">筛选在浏览器中基于本次真实接口返回结果执行。</p>
      </section>

      <div class="catalog-layout">
        <section class="panel product-panel" aria-labelledby="product-list-title">
          <div class="panel-title-row">
            <div><p class="eyebrow">商品列表</p><h2 id="product-list-title">商品列表</h2></div>
            <span class="result-count">{{ filteredProducts.length }} 项结果</span>
          </div>
          <div v-if="filteredProducts.length" class="product-list">
            <button
              v-for="product in filteredProducts"
              :key="product.id"
              :class="['product-card', { selected: selectedProductId === product.id }]"
              type="button"
              :data-testid="`product-${product.id}`"
              @click="selectProduct(product.id)"
            >
              <span class="product-token" aria-hidden="true">{{ product.name.slice(0, 1) }}</span>
              <span class="product-copy">
                <strong>{{ product.name }}</strong>
                <small>productId: {{ product.id }} · {{ product.categoryName }}</small>
                <em>{{ product.description }}</em>
              </span>
              <span class="product-metrics">
                <small>{{ product.skus.length }} 个 SKU</small>
                <strong>{{ productStock(product) }} 件</strong>
              </span>
            </button>
          </div>
          <div v-else class="filtered-empty" data-testid="filtered-empty">没有匹配当前搜索或筛选条件的真实商品。</div>
        </section>

        <section class="detail-stack" aria-live="polite">
          <div v-if="detailState === 'loading'" class="panel detail-state" data-testid="detail-loading">正在加载所选商品详情…</div>
          <div v-else-if="detailState === 'error'" class="panel detail-state error-state" data-testid="detail-error">
            <strong>商品详情加载失败</strong><p>{{ detailError }}</p>
            <button v-if="selectedProduct" class="secondary-button" type="button" @click="selectProduct(selectedProduct!.id)">重新加载详情</button>
          </div>
          <template v-else-if="selectedProduct">
            <section class="panel selected-product" aria-labelledby="selected-product-title">
              <div class="panel-title-row">
                <div><p class="eyebrow">当前商品</p><h2 id="selected-product-title">当前选中商品详情</h2></div>
                <span class="read-only-badge">只读数据</span>
              </div>
              <div class="detail-grid">
                <div class="detail-identity"><span class="identity-mark" aria-hidden="true">P</span><span><small>商品名称</small><strong>{{ selectedProduct.name }}</strong></span></div>
                <dl><div><dt>productId</dt><dd>{{ selectedProduct.id }}</dd></div><div><dt>分类</dt><dd>{{ selectedProduct.categoryName }}</dd></div><div class="wide"><dt>描述</dt><dd>{{ selectedProduct.description }}</dd></div></dl>
              </div>
            </section>

            <section class="panel sku-panel" aria-labelledby="sku-table-title">
              <div class="panel-title-row"><div><p class="eyebrow">SKU 与库存</p><h2 id="sku-table-title">当前商品 SKU 列表</h2></div><span class="result-count">{{ selectedProduct.skus.length }} 个 SKU</span></div>
              <div class="table-wrap">
                <table>
                  <thead><tr><th>skuId</th><th>skuCode</th><th>颜色</th><th>尺寸</th><th>售价</th><th>货币</th><th>可用库存</th><th>库存状态</th></tr></thead>
                  <tbody>
                    <tr v-for="sku in selectedProduct.skus" :key="sku.id" :data-testid="`sku-${sku.id}`">
                      <td class="mono">{{ sku.id }}</td><td class="mono strong-cell">{{ sku.skuCode }}</td><td>{{ sku.color }}</td><td>{{ sku.size }}</td><td class="money">{{ formatMoney(sku.salePrice, sku.currency) }}</td><td class="mono">{{ sku.currency }}</td><td class="stock-value">{{ sku.availableStock }}</td><td><span :class="['stock-badge', stockState(sku.availableStock)]">{{ stockLabel(sku.availableStock) }}</span></td>
                    </tr>
                  </tbody>
                </table>
              </div>
            </section>

            <aside class="fact-panel" aria-label="库存事实说明">
              <span class="fact-icon" aria-hidden="true">i</span>
              <div><strong>库存事实说明</strong><p><code>availableStock</code> 来自 Java 商品接口。标签规则仅用于前端展示：0 为缺货，1 至 {{ LOW_STOCK_THRESHOLD - 1 }} 为库存偏低，其他为库存正常。</p></div>
            </aside>
          </template>
        </section>
      </div>
    </template>
  </section>
</template>
