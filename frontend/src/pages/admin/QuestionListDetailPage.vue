<script setup lang="ts">
import { computed, ref, watch } from 'vue'
import { useRoute, onBeforeRouteLeave } from 'vue-router'
import { adminQuestionListService } from '@/domain/questionList/service/questionListService'
import { questionService } from '@/domain/question/service/questionService'
import type { QuestionListItemVO } from '@/domain/questionList/types/types'
import type { QuestionVO } from '@/domain/question/types/types'
import { useResource } from '@/composables/useResource'
import { useTask } from '@/composables/useTask'
import ResourceState from '@/components/ResourceState.vue'
const route = useRoute()
const id = computed(() => Number(route.params.questionListId))
const { data, loading, error, refresh } = useResource(async () => {
  const [detail, items] = await Promise.all([
    adminQuestionListService.getQuestionListByIdService(id.value),
    adminQuestionListService.getQuestionListItemService(id.value),
  ])
  return { detail: detail.data, items: items.data }
}, [id])
const rows = ref<QuestionListItemVO[]>([])
const dirty = ref(false)
watch(data, (value) => {
  if (value) {
    rows.value = [...value.items].sort((a, b) => a.rank - b.rank)
    dirty.value = false
  }
})
const { busy, run } = useTask()
const searchTask = useTask()
const keyword = ref('')
const results = ref<QuestionVO[]>([])
const searched = ref(false)
watch(id, () => {
  results.value = []
  searched.value = false
})
onBeforeRouteLeave(() => !dirty.value || window.confirm('题单排序尚未保存，确定离开？'))
async function search() {
  await searchTask.run(async () => {
    if (!keyword.value.trim()) throw new Error('请输入题目关键词')
    results.value = (
      await questionService.searchQuestionService({ keyword: keyword.value.trim() })
    ).data
    searched.value = true
  })
}
async function add(questionId: number) {
  await run(async () => {
    await adminQuestionListService.createQuestionListItemService(id.value, questionId)
    await refresh()
  }, '题目已加入题单')
}
async function remove(questionId: number) {
  await run(async () => {
    await adminQuestionListService.deleteQuestionListItemService(id.value, questionId)
    await refresh()
  }, '题目已移出题单')
}
function move(index: number, offset: number) {
  const target = index + offset
  if (target < 0 || target >= rows.value.length || busy.value) return
  const result = [...rows.value]
  ;[result[index], result[target]] = [result[target], result[index]]
  rows.value = result
  dirty.value = true
}
async function saveOrder() {
  await run(async () => {
    await adminQuestionListService.sortQuestionListItemService({
      questionListId: id.value,
      questionIds: rows.value.map((item) => item.question.questionId),
    })
    dirty.value = false
    await refresh()
  }, '排序已保存')
}
const columns = [
  { title: '序号', key: 'rank', width: 70 },
  { title: '题目', key: 'title' },
  { title: '操作', key: 'actions', width: 230 },
]
</script>
<template>
  <section class="panel">
    <div class="between">
      <div>
        <RouterLink to="/admin/question-list">返回题单管理</RouterLink>
        <h1>{{ data?.detail.name || '题单详情' }}</h1>
      </div>
      <a-button
        type="primary"
        :disabled="!dirty || !!error || loading"
        :loading="busy"
        @click="saveOrder"
      >
        保存排序
      </a-button>
    </div>
    <p class="muted">{{ data?.detail.description }}</p>
    <a-alert
      v-if="dirty"
      type="info"
      message="排序已调整，请先保存后再添加或移除题目。"
      class="error-space"
    />
    <ResourceState :loading="loading" :error="error" :empty="!rows.length" @retry="refresh">
      <a-table
        :columns="columns"
        :data-source="rows"
        :row-key="(item: QuestionListItemVO) => item.question.questionId"
        :pagination="false"
      >
        <template #bodyCell="{ column, record, index }">
          <span v-if="column.key === 'rank'">{{ index + 1 }}</span>
          <RouterLink
            v-else-if="column.key === 'title'"
            :to="`/questions/${record.question.questionId}`"
          >
            {{ record.question.title }}
          </RouterLink>
          <a-space v-else-if="column.key === 'actions'">
            <a-button size="small" :disabled="index === 0 || busy" @click="move(index, -1)">
              上移
            </a-button>
            <a-button
              size="small"
              :disabled="index === rows.length - 1 || busy"
              @click="move(index, 1)"
            >
              下移
            </a-button>
            <a-popconfirm title="从题单移除此题？" @confirm="remove(record.question.questionId)">
              <a-button size="small" danger :disabled="dirty || busy">移除</a-button>
            </a-popconfirm>
          </a-space>
        </template>
      </a-table>
    </ResourceState>
    <a-divider />
    <h2>添加题目</h2>
    <a-input-search
      v-model:value="keyword"
      placeholder="输入题目关键词"
      :loading="searchTask.busy.value"
      enter-button
      @search="search"
    />
    <a-list v-if="searched" :data-source="results" :locale="{ emptyText: '未找到相关题目' }">
      <template #renderItem="{ item }">
        <a-list-item>
          <RouterLink :to="`/questions/${item.questionId}`">{{ item.title }}</RouterLink>
          <a-button
            :disabled="
              rows.some((row) => row.question.questionId === item.questionId) ||
              dirty ||
              busy ||
              loading ||
              !!error
            "
            @click="add(item.questionId)"
          >
            {{
              rows.some((row) => row.question.questionId === item.questionId) ? '已添加' : '添加'
            }}
          </a-button>
        </a-list-item>
      </template>
    </a-list>
  </section>
</template>
