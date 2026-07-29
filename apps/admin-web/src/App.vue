<script setup lang="ts">
import { ref } from 'vue'
import AiCustomerServiceWorkbench from './AiCustomerServiceWorkbench.vue'
import OrderInventoryEvidence from './OrderInventoryEvidence.vue'
import OperationsOverview from './OperationsOverview.vue'
import ProductSkuManagement from './ProductSkuManagement.vue'

type View = 'overview' | 'products' | 'orders' | 'ai'
const activeView = ref<View>('overview')
const navigation: Array<{ label: string; view: View }> = [
  { label: '运营总览', view: 'overview' },
  { label: '商品与 SKU', view: 'products' },
  { label: '订单管理', view: 'orders' },
  { label: 'AI 客服', view: 'ai' },
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
          <button type="button" :class="['nav-item', 'nav-button', { active: activeView === item.view }]" :aria-current="activeView === item.view ? 'page' : undefined" @click="activeView = item.view"><i class="nav-symbol" aria-hidden="true"></i>{{ item.label }}</button>
        </template>
      </nav>
      <div class="sidebar-boundary"><span class="connection-dot"></span><div><strong>本地 Showcase</strong><small>仅展示当前真实接口与演示数据</small></div></div>
    </aside>
    <main class="main-content"><OperationsOverview v-if="activeView === 'overview'" @open-orders="activeView = 'orders'" /><ProductSkuManagement v-else-if="activeView === 'products'" /><OrderInventoryEvidence v-else-if="activeView === 'orders'" /><AiCustomerServiceWorkbench v-else /></main>
  </div>
</template>
