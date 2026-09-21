<script setup lang="ts">
defineProps<{ forbidden?: boolean }>()
</script>
<template>
  <a-result
    :status="forbidden ? '403' : '404'"
    :title="forbidden ? '无权访问' : '页面不存在'"
    :sub-title="forbidden ? '需要管理员权限才能访问此页面' : '请检查链接是否正确'"
  >
    <template #extra>
      <RouterLink to="/"><a-button type="primary">返回首页</a-button></RouterLink>
    </template>
  </a-result>
</template>
