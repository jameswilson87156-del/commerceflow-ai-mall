<script setup lang="ts">
import { computed, ref } from 'vue'
import AdminAuthControl from './components/AdminAuthControl.vue'
import { isOidcAuthEnabled } from './oidc'
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
const activeNavigation = computed(() => navigation.find((item) => item.view === activeView.value) ?? navigation[0])
const oidcEnabled = isOidcAuthEnabled()
const navigationIcons: Record<View, string> = {
  overview: 'M3 3h7v7H3z M14 3h7v7h-7z M3 14h7v7H3z M14 14h7v7h-7z',
  products: 'm12 3 9 5v9l-9 5-9-5V8z M3 8l9 5 9-5 M12 13v9 M7 5.8l9 5',
  orders: 'M6 3h12v18l-3-2-3 2-3-2-3 2z M9 8h6 M9 12h6',
  ai: 'M4 4h16v13H9l-5 4z M8 9h8 M8 13h5',
}
const viewDescriptions: Record<View, string> = {
  overview: '关键经营信号与运行边界',
  products: '商品主数据、SKU 与库存事实',
  orders: '订单快照、扣减与事务证据',
  ai: '基于业务事实的商品问答',
}
</script>

<template>
  <div class="app-shell">
    <aside class="sidebar" aria-label="商城管理导航">
      <div class="brand-lockup">
        <span class="brand-mark" aria-hidden="true"><i></i><i></i><i></i></span>
        <div><strong>CommerceFlow</strong><small>AI Mall Operations</small></div>
      </div>

      <div class="workspace-label"><span class="workspace-label__dot"></span><span>{{ oidcEnabled ? 'OIDC OPERATOR' : 'LOCAL SHOWCASE' }}</span></div>
      <p class="nav-caption">工作台</p>
      <nav class="nav-list">
        <template v-for="item in navigation" :key="item.label">
          <button
            type="button"
            :class="['nav-item', 'nav-button', { active: activeView === item.view }]"
            :aria-current="activeView === item.view ? 'page' : undefined"
            @click="activeView = item.view"
          >
            <svg class="nav-glyph" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5" stroke-linejoin="round" aria-hidden="true"><path :d="navigationIcons[item.view]" /></svg>{{ item.label }}
          </button>
        </template>
      </nav>

      <div class="sidebar-footer">
        <div class="sidebar-boundary"><div><strong>服务端数据工作区</strong><small>连接结果以各页面请求为准</small></div></div>
        <div class="sidebar-user"><span class="avatar">O</span><div><strong>Operator Workbench</strong><small>{{ oidcEnabled ? '身份由 OIDC JWT + OperatorScope 判定' : '身份由后端 OperatorScope 判定' }}</small></div><span class="more-dots" aria-hidden="true">···</span></div>
      </div>
    </aside>

    <main class="main-content">
      <header class="context-bar">
        <div class="context-bar__crumbs"><span>CommerceFlow AI Mall</span><span class="context-bar__slash">/</span><strong>{{ activeNavigation.label }}</strong></div>
        <div class="context-bar__meta"><span class="context-bar__description">{{ viewDescriptions[activeNavigation.view] }}</span><span class="context-status"><span class="connection-dot"></span>{{ oidcEnabled ? 'OIDC Operator API' : 'Operator API' }}</span><AdminAuthControl /><span class="context-avatar">O</span></div>
      </header>
      <div class="main-stage">
        <OperationsOverview v-if="activeView === 'overview'" @open-orders="activeView = 'orders'" />
        <ProductSkuManagement v-else-if="activeView === 'products'" />
        <OrderInventoryEvidence v-else-if="activeView === 'orders'" />
        <AiCustomerServiceWorkbench v-else />
      </div>
    </main>
  </div>
</template>
