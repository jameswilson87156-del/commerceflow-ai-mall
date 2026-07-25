<script setup lang="ts">
import ProductSkuManagement from './ProductSkuManagement.vue'

const navigation = ['运营总览', '商品与SKU', '订单管理', 'AI客服']
</script>

<template>
  <div class="app-shell">
    <aside class="sidebar" aria-label="商城管理导航">
      <div class="brand-lockup"><span class="brand-mark">CF</span><div><strong>CommerceFlow</strong><small>AI Mall · 本地展示版</small></div></div>
      <p class="nav-caption">商城运营</p>
      <nav class="nav-list">
        <span v-for="item in navigation" :key="item" :class="['nav-item', { active: item === '商品与SKU' }]" :aria-current="item === '商品与SKU' ? 'page' : undefined">{{ item }}</span>
      </nav>
      <div class="sidebar-boundary"><span class="connection-dot"></span><div><strong>本地演示数据</strong><small>仅展示当前真实入口</small></div></div>
    </aside>
    <main class="main-content"><ProductSkuManagement /></main>
  </div>
</template>
