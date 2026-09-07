<script setup lang="ts">
withDefaults(defineProps<{
  label: string
  message: string
  tone?: 'blue' | 'green' | 'amber' | 'red'
}>(), { tone: 'blue' })
</script>

<template>
  <details :class="['boundary-strip', `boundary-strip--${tone}`]" :open="tone === 'red' || tone === 'amber'">
    <summary>{{ label }}<span class="boundary-strip__hint">数据说明</span></summary>
    <p>{{ message }}</p>
  </details>
</template>
