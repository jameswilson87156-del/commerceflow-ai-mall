<script setup lang="ts">
import { ref, shallowRef } from 'vue'
import CatalogPage from './pages/index/index.vue'
import ProductDetailPage from './pages/product/detail.vue'
import CartPage from './pages/cart/index.vue'
import OrderConfirmPage from './pages/order/confirm.vue'
import OrderResultPage from './pages/order/result.vue'
import OrderDetailPage from './pages/order/detail.vue'
import OrderListPage from './pages/order/list.vue'
import AiCustomerServicePage from './pages/ai/customer-service.vue'
import { currentHash, currentPath, startNavigationListener } from './platform/h5'

const pages: Record<string, any> = {
  '/pages/index/index': CatalogPage,
  '/pages/product/detail': ProductDetailPage,
  '/pages/cart/index': CartPage,
  '/pages/order/confirm': OrderConfirmPage,
  '/pages/order/result': OrderResultPage,
  '/pages/order/detail': OrderDetailPage,
  '/pages/order/list': OrderListPage,
  '/pages/ai/customer-service': AiCustomerServicePage
}
const activePage = shallowRef<any>(CatalogPage)
const pageKey = ref(currentHash())
function updatePage(path: string) {
  activePage.value = pages[path] || CatalogPage
  pageKey.value = currentHash()
}
updatePage(currentPath())
startNavigationListener(updatePage)
</script>

<template><component :is="activePage" :key="pageKey" /></template>
<style>
@import './styles/theme.css';
</style>
