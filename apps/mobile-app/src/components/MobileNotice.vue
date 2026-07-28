<script setup lang="ts">
withDefaults(defineProps<{
  title: string
  message: string
  tone?: 'info' | 'success' | 'warning' | 'error'
}>(), {tone: 'info'})
</script>

<template>
  <view :class="['mobile-notice', `mobile-notice--${tone}`]">
    <text class="mobile-notice__title">{{ title }}</text>
    <text class="mobile-notice__body">{{ message }}</text>
  </view>
</template>
