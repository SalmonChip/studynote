<script setup lang="ts">
defineProps<{ difficulty?: number }>()
</script>
<template>
  <a-tag :color="difficulty === 1 ? 'green' : difficulty === 2 ? 'orange' : 'red'">
    {{ difficulty === 1 ? '简单' : difficulty === 2 ? '中等' : difficulty === 3 ? '困难' : '未知' }}
  </a-tag>
</template>
