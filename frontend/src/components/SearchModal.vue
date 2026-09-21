<script setup lang="ts">
import { ref } from 'vue'
import { questionService } from '@/domain/question/service/questionService'
import type { QuestionVO } from '@/domain/question/types/types'
import { useTask } from '@/composables/useTask'
const open = ref(false)
const keyword = ref('')
const results = ref<QuestionVO[]>([])
const searched = ref(false)
const { busy, run } = useTask()
async function search() {
  if (!keyword.value.trim()) return
  await run(async () => {
    results.value = (
      await questionService.searchQuestionService({ keyword: keyword.value.trim() })
    ).data
    searched.value = true
  })
}
</script>

<template>
  <a-button @click="open = true">搜索题目</a-button>
  <a-modal v-model:open="open" title="搜索题目" :footer="null">
    <a-input-search
      v-model:value="keyword"
      placeholder="输入题目关键词"
      :loading="busy"
      enter-button
      @search="search"
    />
    <a-list
      :data-source="results"
      class="search-results"
      :locale="{ emptyText: searched ? '未找到相关题目' : '输入关键词开始搜索' }"
    >
      <template #renderItem="{ item }">
        <a-list-item>
          <RouterLink :to="`/questions/${item.questionId}`" @click="open = false">
            {{ item.title }}
          </RouterLink>
        </a-list-item>
      </template>
    </a-list>
  </a-modal>
</template>
