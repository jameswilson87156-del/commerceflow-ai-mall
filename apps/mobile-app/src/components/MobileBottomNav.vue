<script setup lang="ts">
withDefaults(defineProps<{ active: 'home' | 'orders' | 'ai' }>(), { active: 'home' })
function go(url: string) { uni.reLaunch({ url }) }
</script>

<template>
  <view class="mobile-bottom-nav" aria-label="商城主导航">
    <button type="button" :class="['mobile-bottom-nav__item', { active: active === 'home' }]" :aria-current="active === 'home' ? 'page' : undefined" @click="go('/pages/index/index')"><view class="mobile-bottom-nav__icon mobile-bottom-nav__icon--home" aria-hidden="true" /><text>商城</text></button>
    <button type="button" :class="['mobile-bottom-nav__item', { active: active === 'orders' }]" :aria-current="active === 'orders' ? 'page' : undefined" @click="go('/pages/order/list')"><view class="mobile-bottom-nav__icon mobile-bottom-nav__icon--orders" aria-hidden="true" /><text>订单</text></button>
    <button type="button" :class="['mobile-bottom-nav__item', { active: active === 'ai' }]" :aria-current="active === 'ai' ? 'page' : undefined" @click="go('/pages/ai/customer-service')"><view class="mobile-bottom-nav__icon mobile-bottom-nav__icon--ai" aria-hidden="true" /><text>AI 助手</text></button>
  </view>
</template>
