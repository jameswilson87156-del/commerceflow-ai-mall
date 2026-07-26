<script setup lang="ts">
import ProductSkuManagement from './ProductSkuManagement.vue'

const navigation = ['运营总览', '商品与 SKU', '订单管理', 'AI 客服']
</script>

<template>
  <div class="app-shell">
    <aside class="sidebar" aria-label="商城管理导航">
      <div class="brand-lockup">
        <span class="brand-mark" aria-hidden="true"><i></i><i></i><i></i></span>
        <div><strong>CommerceFlow AI Mall</strong><small>电商与 AI 客服协同平台</small></div>
      </div>
      <p class="nav-caption">工作台</p>
      <nav class="nav-list">
        <span v-for="item in navigation" :key="item" :class="['nav-item', { active: item === '商品与 SKU' }]" :aria-current="item === '商品与 SKU' ? 'page' : undefined"><i class="nav-symbol" aria-hidden="true"></i>{{ item }}</span>
      </nav>
      <div class="sidebar-boundary"><span class="connection-dot"></span><div><strong>本地 Showcase</strong><small>仅展示当前真实接口与演示数据</small></div></div>
    </aside>
    <main class="main-content"><ProductSkuManagement /></main>
  </div>
</template>
