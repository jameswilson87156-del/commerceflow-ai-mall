<script setup lang="ts">
import { ref } from 'vue'
import AiCustomerServiceWorkbench from './AiCustomerServiceWorkbench.vue'
import OrderInventoryEvidence from './OrderInventoryEvidence.vue'
import ProductSkuManagement from './ProductSkuManagement.vue'

const activeView = ref<'products' | 'orders' | 'ai'>('products')
const navigation = [
  { label: '运营总览' },
  { label: '商品与 SKU', view: 'products' as const },
  { label: '订单管理', view: 'orders' as const },
  { label: 'AI 客服', view: 'ai' as const },
]
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
        <template v-for="item in navigation" :key="item.label">
          <button v-if="item.view" type="button" :class="['nav-item', 'nav-button', { active: activeView === item.view }]" :aria-current="activeView === item.view ? 'page' : undefined" @click="activeView = item.view"><i class="nav-symbol" aria-hidden="true"></i>{{ item.label }}</button>
          <span v-else class="nav-item"><i class="nav-symbol" aria-hidden="true"></i>{{ item.label }}</span>
        </template>
      </nav>
      <div class="sidebar-boundary"><span class="connection-dot"></span><div><strong>本地 Showcase</strong><small>仅展示当前真实接口与演示数据</small></div></div>
    </aside>
    <main class="main-content"><ProductSkuManagement v-if="activeView === 'products'" /><OrderInventoryEvidence v-else-if="activeView === 'orders'" /><AiCustomerServiceWorkbench v-else /></main>
  </div>
</template>
