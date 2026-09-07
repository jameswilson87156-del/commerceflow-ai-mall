<script setup lang="ts">
withDefaults(defineProps<{
  label: string
  tone?: 'success' | 'warning' | 'danger' | 'neutral' | 'blue'
}>(), { tone: 'neutral' })
</script>

<template><text :class="['mobile-status-pill', `mobile-status-pill--${tone}`]"><text class="mobile-status-pill__dot"></text>{{ label }}</text></template>
