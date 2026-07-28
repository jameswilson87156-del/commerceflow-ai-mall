<script setup lang="ts">
withDefaults(defineProps<{
  title: string
  eyebrow?: string
  subtitle?: string
  back?: boolean
  actionLabel?: string
}>(), { eyebrow: 'COMMERCEFLOW AI MALL', back: false })

const emit = defineEmits<{ back: []; action: [] }>()
</script>

<template>
  <view class="mobile-header">
    <button v-if="back" class="icon-button header-back" aria-label="返回" @click="emit('back')"><text>&lt;</text></button>
    <view class="header-copy">
      <text class="header-eyebrow">{{ eyebrow }}</text>
      <text class="header-title">{{ title }}</text>
      <text v-if="subtitle" class="header-subtitle">{{ subtitle }}</text>
    </view>
    <button v-if="actionLabel" class="header-action" @click="emit('action')">{{ actionLabel }}</button>
    <view v-else class="header-balance" />
  </view>
</template>
