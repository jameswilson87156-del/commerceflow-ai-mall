<script setup lang="ts">
import { computed, ref, watch } from 'vue'

const props = withDefaults(defineProps<{
  src?: string | null
  alt?: string
  placeholder?: string
}>(), {alt: '商品图片', placeholder: '图片暂不可用'})

const failed = ref(!props.src)
const imageSource = computed(() => props.src || '')
watch(() => props.src, value => { failed.value = !value })
function markFailed() { failed.value = true }
</script>

<template>
  <view class="image-frame">
    <img v-if="imageSource && !failed" :src="imageSource" :alt="props.alt" @error="markFailed" />
    <view v-else class="image-placeholder"><text>{{ props.placeholder }}</text></view>
  </view>
</template>
