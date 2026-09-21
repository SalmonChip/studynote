<script setup lang="ts">
import type { QuestionWithUserStatus } from '@/domain/question/types/types'
import DifficultyTag from './DifficultyTag.vue'
defineProps<{ rows: QuestionWithUserStatus[] }>()
const columns = [
  { title: '状态', key: 'status', width: 70 },
  { title: 'ID', dataIndex: 'questionId', width: 80 },
  { title: '题目', key: 'title' },
  { title: '考点', dataIndex: 'examPoint' },
  { title: '难度', key: 'difficulty', width: 90 },
  { title: '浏览量', dataIndex: 'viewCount', width: 90 },
]
</script>
<template>
  <a-table :columns="columns" :data-source="rows" row-key="questionId" :pagination="false">
    <template #bodyCell="{ column, record }">
      <template v-if="column.key === 'title'">
        <RouterLink :to="`/questions/${record.questionId}`">{{ record.title }}</RouterLink>
      </template>
      <template v-else-if="column.key === 'status'">
        <a-tag v-if="record.userQuestionStatus?.finished" color="green">完成</a-tag>
        <span v-else class="muted">—</span>
      </template>
      <DifficultyTag v-else-if="column.key === 'difficulty'" :difficulty="record.difficulty" />
    </template>
  </a-table>
</template>
