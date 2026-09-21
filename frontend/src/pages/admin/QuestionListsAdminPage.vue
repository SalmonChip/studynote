<script setup lang="ts">
import { reactive, ref } from 'vue'
import { adminQuestionListService } from '@/domain/questionList/service/questionListService'
import type { QuestionListEntity } from '@/domain/questionList/types/types'
import { useResource } from '@/composables/useResource'
import { useTask } from '@/composables/useTask'
import ResourceState from '@/components/ResourceState.vue'
const { data, loading, error, refresh } = useResource(() =>
  adminQuestionListService.getQuestionListService(),
)
const form = reactive({ questionListId: 0, name: '', description: '', type: 1 })
const open = ref(false)
const { busy, run } = useTask()
const columns = [
  { title: 'ID', dataIndex: 'questionListId', width: 80 },
  { title: '名称', key: 'name' },
  { title: '类型', key: 'type' },
  { title: '描述', dataIndex: 'description' },
  { title: '操作', key: 'actions', width: 150 },
]
function create() {
  Object.assign(form, { questionListId: 0, name: '', description: '', type: 1 })
  open.value = true
}
function edit(item: QuestionListEntity) {
  Object.assign(form, {
    questionListId: item.questionListId,
    name: item.name,
    description: item.description,
    type: item.type,
  })
  open.value = true
}
async function save() {
  await run(async () => {
    if (!form.name.trim()) throw new Error('请输入题单名称')
    const body = { name: form.name.trim(), description: form.description, type: form.type }
    if (form.questionListId)
      await adminQuestionListService.updateQuestionListService(form.questionListId, body)
    else await adminQuestionListService.createQuestionListService(body)
    open.value = false
    await refresh()
  }, '题单已保存')
}
async function remove(id: number) {
  await run(async () => {
    await adminQuestionListService.deleteQuestionListService(id)
    await refresh()
  }, '题单已删除')
}
</script>
<template>
  <section class="panel">
    <div class="between">
      <h1>题单管理</h1>
      <a-button type="primary" @click="create">创建题单</a-button>
    </div>
    <ResourceState :loading="loading" :error="error" :empty="!data?.data.length" @retry="refresh">
      <a-table
        :columns="columns"
        :data-source="data?.data || []"
        row-key="questionListId"
        :pagination="false"
      >
        <template #bodyCell="{ column, record }">
          <RouterLink
            v-if="column.key === 'name'"
            :to="`/admin/question-list/${record.questionListId}`"
          >
            {{ record.name }}
          </RouterLink>
          <a-tag v-else-if="column.key === 'type'" :color="record.type === 2 ? 'purple' : 'blue'">
            {{ record.type === 2 ? '训练营' : '普通题单' }}
          </a-tag>
          <a-space v-else-if="column.key === 'actions'">
            <a-button size="small" @click="edit(record)">编辑</a-button>
            <a-popconfirm
              title="删除题单及其中的题目关联？原题目不会被删除。"
              @confirm="remove(record.questionListId)"
            >
              <a-button danger size="small" :disabled="busy">删除</a-button>
            </a-popconfirm>
          </a-space>
        </template>
      </a-table>
    </ResourceState>
    <a-modal
      v-model:open="open"
      :title="form.questionListId ? '编辑题单' : '创建题单'"
      :confirm-loading="busy"
      @ok="save"
    >
      <div class="stack">
        <label>
          名称
          <a-input v-model:value="form.name" :maxlength="32" />
        </label>
        <label>
          类型
          <a-select
            v-model:value="form.type"
            style="width: 100%"
            :options="[
              { label: '普通题单', value: 1 },
              { label: '训练营', value: 2 },
            ]"
          />
        </label>
        <label>
          描述
          <a-textarea v-model:value="form.description" :rows="4" />
        </label>
      </div>
    </a-modal>
  </section>
</template>
