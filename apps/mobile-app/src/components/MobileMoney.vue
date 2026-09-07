<script setup lang="ts">
defineProps<{ amount: string | number; currency?: string; compact?: boolean }>()
</script>

<template><text :class="['mobile-money', { 'mobile-money--compact': compact }]">¥{{ amount }}<text class="mobile-money__currency">{{ currency || 'CNY' }}</text></text></template>
