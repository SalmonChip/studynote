<script setup lang="ts">
defineProps<{ loading?: boolean; error?: string; empty?: boolean }>()
defineEmits<{ retry: [] }>()
</script>

<template>
  <a-alert v-if="error" type="error" show-icon :message="error">
    <template #action><a-button size="small" @click="$emit('retry')">重试</a-button></template>
  </a-alert>
  <a-skeleton v-else-if="loading" active />
  <a-empty v-else-if="empty" description="暂无数据" />
  <slot v-else />
</template>
