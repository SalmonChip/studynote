<script setup lang="ts">
import { computed, reactive, ref, watch } from 'vue'
import { adminQuestionService } from '@/domain/question/service/questionService'
import { adminCategoryService } from '@/domain/category/service/categoryService'
import type { QuestionEntity } from '@/domain/question/types/types'
import { useResource } from '@/composables/useResource'
import { useTask } from '@/composables/useTask'
import { flattenCategories } from '@/utils/categories'
import ResourceState from '@/components/ResourceState.vue'
import DifficultyTag from '@/components/DifficultyTag.vue'
const page = ref(1)
const pageSize = ref(10)
const categoryId = ref<number>()
watch(
  categoryId,
  () => {
    page.value = 1
  },
  { flush: 'sync' },
)
const categories = useResource(() => adminCategoryService.categoriesService())
const categoryOptions = computed(() => flattenCategories(categories.data.value?.data || []))
const { data, loading, error, refresh } = useResource(
  () =>
    adminQuestionService.getQuestionList({
      categoryId: categoryId.value,
      page: page.value,
      pageSize: pageSize.value,
    }),
  [page, pageSize, categoryId],
)
const form = reactive({
  questionId: 0,
  title: '',
  examPoint: '',
  difficulty: 1,
  categoryId: undefined as number | undefined,
})
const open = ref(false)
const batchOpen = ref(false)
const markdown = ref('')
const example = '# Java\n\n## 基础\n\n- 什么是多态？（考点：面向对象）【简单】'
const { busy, run } = useTask()
const columns = [
  { title: 'ID', dataIndex: 'questionId', width: 70 },
  { title: '题目', key: 'title' },
  { title: '难度', key: 'difficulty', width: 90 },
  { title: '考点', dataIndex: 'examPoint' },
  { title: '操作', key: 'actions', width: 150 },
]
function create() {
  Object.assign(form, {
    questionId: 0,
    title: '',
    examPoint: '',
    difficulty: 1,
    categoryId: categoryId.value,
  })
  open.value = true
}
function edit(item: QuestionEntity) {
  Object.assign(form, {
    questionId: item.questionId,
    title: item.title,
    examPoint: item.examPoint || '',
    difficulty: item.difficulty,
    categoryId: item.categoryId,
  })
  open.value = true
}
async function save() {
  await run(async () => {
    if (!form.title.trim() || !form.categoryId) throw new Error('请填写题目并选择分类')
    const body = {
      title: form.title.trim(),
      examPoint: form.examPoint,
      difficulty: form.difficulty,
      categoryId: form.categoryId,
    }
    if (form.questionId)
      await adminQuestionService.updateQuestionService({ ...body, questionId: form.questionId })
    else await adminQuestionService.createQuestionService(body)
    open.value = false
    await refresh()
  }, '题目已保存')
}
async function remove(id: number) {
  await run(async () => {
    await adminQuestionService.deleteQuestionService(id)
    await refresh()
  }, '题目已删除')
}
async function importBatch() {
  await run(async () => {
    if (!markdown.value.trim()) throw new Error('请输入 Markdown')
    await adminQuestionService.createQuestionBatchService({ markdown: markdown.value })
    batchOpen.value = false
    markdown.value = ''
    await Promise.all([refresh(), categories.refresh()])
  }, '批量导入完成')
}
</script>
<template>
  <section class="panel">
    <div class="between">
      <h1>题目管理</h1>
      <a-space>
        <a-button @click="batchOpen = true">Markdown 批量导入</a-button>
        <a-button type="primary" @click="create">创建题目</a-button>
      </a-space>
    </div>
    <div class="toolbar">
      <a-select
        v-model:value="categoryId"
        placeholder="全部分类"
        allow-clear
        :options="categoryOptions"
        :loading="categories.loading.value"
      />
    </div>
    <a-alert
      v-if="categories.error.value"
      type="error"
      :message="categories.error.value"
      class="error-space"
    >
      <template #action>
        <a-button size="small" @click="categories.refresh">重试分类</a-button>
      </template>
    </a-alert>
    <ResourceState :loading="loading" :error="error" :empty="!data?.data.length" @retry="refresh">
      <a-table
        :columns="columns"
        :data-source="data?.data || []"
        row-key="questionId"
        :pagination="false"
      >
        <template #bodyCell="{ column, record }">
          <RouterLink v-if="column.key === 'title'" :to="`/questions/${record.questionId}`">
            {{ record.title }}
          </RouterLink>
          <DifficultyTag v-else-if="column.key === 'difficulty'" :difficulty="record.difficulty" />
          <a-space v-else-if="column.key === 'actions'">
            <a-button size="small" @click="edit(record)">编辑</a-button>
            <a-popconfirm title="确定删除题目？" @confirm="remove(record.questionId)">
              <a-button danger size="small" :disabled="busy">删除</a-button>
            </a-popconfirm>
          </a-space>
        </template>
      </a-table>
    </ResourceState>
    <div v-if="data?.pagination" class="pagination">
      <a-pagination
        v-model:current="page"
        v-model:page-size="pageSize"
        :total="data.pagination.total"
        show-size-changer
      />
    </div>
    <a-modal
      v-model:open="open"
      :title="form.questionId ? '编辑题目' : '创建题目'"
      :confirm-loading="busy"
      @ok="save"
    >
      <div class="stack">
        <label>
          题目
          <a-input v-model:value="form.title" :maxlength="255" />
        </label>
        <label>
          所属分类
          <a-select
            v-model:value="form.categoryId"
            style="width: 100%"
            :options="categoryOptions"
          />
        </label>
        <label>
          难度
          <a-select
            v-model:value="form.difficulty"
            style="width: 100%"
            :options="[
              { label: '简单', value: 1 },
              { label: '中等', value: 2 },
              { label: '困难', value: 3 },
            ]"
          />
        </label>
        <label>
          考点
          <a-input v-model:value="form.examPoint" :maxlength="255" />
        </label>
      </div>
    </a-modal>
    <a-modal
      v-model:open="batchOpen"
      title="批量导入题目"
      width="760px"
      :confirm-loading="busy"
      @ok="importBatch"
    >
      <p>一级标题为分类，二级标题为子分类，每条列表题目需带考点和难度。</p>
      <pre>{{ example }}</pre>
      <a-textarea v-model:value="markdown" :rows="14" placeholder="粘贴 Markdown 内容" />
    </a-modal>
  </section>
</template>
