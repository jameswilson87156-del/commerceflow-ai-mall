<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { getProduct } from '../../api/catalog'
import { addCartItem } from '../../api/cart'
import { ApiError, resolveImageUrl } from '../../api/runtime'
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

function load(productId: number) {
  state.value = 'loading'
  getProduct(productId).then(response => {
    product.value = response.data
    const first = response.data.skus.find(sku => sku.availableStock > 0) || response.data.skus[0]
    selectedSkuId.value = first?.id || null
    state.value = 'ready'
  }).catch(error => {
    state.value = 'error'
    errorMessage.value = error instanceof ApiError ? error.message : '商品详情加载失败，请重试'
  })
}
function chooseSku(id: number) { selectedSkuId.value = id; actionMessage.value = '' }
function imageUrl(path?: string | null) { return resolveImageUrl(path || product.value?.coverImagePath) }
async function addToCart() {
  if (!selectedSku.value || !canAdd.value) return
  adding.value = true; actionMessage.value = ''
  try {
    await addCartItem(selectedSku.value.id)
    actionMessage.value = '已加入服务端购物车'
    uni.showToast({title: '已加入购物车', icon: 'success'})
  } catch (error) { actionMessage.value = error instanceof ApiError ? error.message : '加入购物车失败，请重试' }
  finally { adding.value = false }
}
function retry() { if (productId.value) load(productId.value) }
function openCart() { uni.navigateTo({url: '/pages/cart/index'}) }
onMounted(async () => {
  const { queryParam } = await import('../../platform/h5')
  productId.value = Number(queryParam('productId'))
  load(productId.value)
})
</script>

<template>
  <view class="mobile-page detail-page">
    <view v-if="state === 'loading'" class="panel empty-box">正在读取商品详情…</view>
    <view v-else-if="state === 'error'" class="error-box"><text>{{ errorMessage }}</text><button class="secondary-button retry-button" @click="retry">重新加载</button></view>
    <template v-else-if="product">
      <view class="detail-hero panel"><img v-if="imageUrl(selectedSku?.imagePath || product.coverImagePath)" class="detail-image" :src="imageUrl(selectedSku?.imagePath || product.coverImagePath)" /><view v-else class="detail-image image-fallback">暂无商品图片</view><view class="detail-copy"><text class="eyebrow">{{ product.categoryName }}</text><text class="detail-title">{{ product.name }}</text><text class="product-code">{{ product.productCode }}</text><text class="detail-description">{{ product.description }}</text><text :class="['tag', product.status === 'ON_SALE' ? 'tag-success' : 'tag-neutral']">{{ product.status === 'ON_SALE' ? '在售' : product.status }}</text></view></view>
      <view class="section-title"><text>选择规格</text><text class="muted">{{ product.skus.length }} 个 SKU</text></view>
      <view class="sku-list">
        <view v-for="sku in product.skus" :key="sku.id" :class="['sku-option panel', {selected: sku.id === selectedSkuId, unavailable: sku.availableStock === 0}]" @click="chooseSku(sku.id)"><view class="sku-image-wrap"><img v-if="sku.imagePath" class="sku-image" :src="imageUrl(sku.imagePath)" /><view v-else class="sku-image image-fallback">无图</view></view><view class="sku-copy"><text class="sku-title">{{ sku.color }} / {{ sku.size }}</text><text class="sku-code">{{ sku.skuCode }}</text><text class="sku-price">¥{{ sku.salePrice }} {{ sku.currency }}</text></view><view class="sku-stock"><text :class="['tag', sku.availableStock === 0 ? 'tag-danger' : sku.availableStock < 10 ? 'tag-warning' : 'tag-success']">{{ sku.availableStock === 0 ? '暂无库存' : `库存 ${sku.availableStock}` }}</text><text class="radio">{{ sku.id === selectedSkuId ? '✓' : '' }}</text></view></view>
      </view>
      <view v-if="selectedSku" class="selection-note">当前选择：{{ selectedSku.color }} / {{ selectedSku.size }} · SKU {{ selectedSku.skuCode }} · {{ selectedSku.availableStock === 0 ? '暂无库存' : `可用库存 ${selectedSku.availableStock}` }}</view>
      <view v-if="actionMessage" class="status-line">{{ actionMessage }}</view>
      <view class="bottom-action"><view class="detail-actions"><button class="secondary-button" @click="openCart">查看购物车</button><button class="primary-button" :disabled="!canAdd" @click="addToCart">{{ adding ? '加入中…' : selectedSku?.availableStock === 0 ? '暂无库存' : '加入购物车' }}</button></view></view>
    </template>
  </view>
</template>

<style>
.detail-page{padding-bottom:190rpx}.detail-hero{overflow:hidden}.detail-image{display:block;width:100%;height:420rpx;background:#f2f4f7}.detail-copy{padding:24rpx}.detail-title{display:block;margin-top:10rpx;font-size:38rpx;font-weight:800;line-height:1.2}.detail-description{display:block;margin:16rpx 0;color:#68758a;font-size:23rpx;line-height:1.55}.sku-option{display:flex;align-items:center;gap:16rpx;padding:16rpx;margin-bottom:14rpx}.sku-option.selected{border:2rpx solid #2563eb;background:#f8fbff}.sku-option.unavailable{opacity:.68}.sku-image-wrap,.sku-image{width:92rpx;height:92rpx;flex:none;border-radius:10rpx;overflow:hidden}.sku-copy{min-width:0;flex:1}.sku-title{display:block;font-size:25rpx;font-weight:700}.sku-code{display:block;margin-top:6rpx;color:#6d7a8e;font-family:monospace;font-size:18rpx}.sku-price{display:block;margin-top:8rpx;color:#2563eb;font-size:21rpx;font-weight:700}.sku-stock{display:flex;flex-direction:column;align-items:flex-end;gap:12rpx}.radio{display:flex;align-items:center;justify-content:center;width:34rpx;height:34rpx;border:2rpx solid #b7c1d1;border-radius:50%;color:#2563eb;font-weight:800}.selected .radio{border-color:#2563eb}.selection-note{padding:18rpx 20rpx;border-radius:12rpx;background:#eef5ff;color:#315eae;font-size:21rpx;line-height:1.45}.detail-actions{display:flex;gap:16rpx}.detail-actions button{flex:1}
</style>
