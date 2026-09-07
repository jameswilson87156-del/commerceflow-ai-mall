<script setup lang="ts">
withDefaults(defineProps<{
  eyebrow: string
  title: string
  description?: string
  count?: string
}>(), { description: '', count: '' })
</script>

<template>
  <div class="section-header">
    <div class="section-header__copy">
      <p class="eyebrow">{{ eyebrow }}</p>
      <h2>{{ title }}</h2>
      <p v-if="description" class="section-header__description">{{ description }}</p>
    </div>
    <div class="section-header__aside">
      <span v-if="count" class="result-count">{{ count }}</span>
      <slot />
    </div>
  </div>
</template>
