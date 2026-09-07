<script setup lang="ts">
withDefaults(defineProps<{
  label: string
  tone?: 'blue' | 'green' | 'amber' | 'red' | 'neutral'
  dot?: boolean
}>(), { tone: 'neutral', dot: true })
</script>

<template>
  <span :class="['status-pill', `status-pill--${tone}`]">
    <span v-if="dot" class="status-pill__dot" aria-hidden="true"></span>
    {{ label }}
  </span>
</template>
