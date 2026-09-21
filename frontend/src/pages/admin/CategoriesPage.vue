<script setup lang="ts">
import { computed, reactive, ref } from 'vue'
import { adminCategoryService } from '@/domain/category/service/categoryService'
import type { CategoryTree } from '@/domain/category/types/types'
import { useResource } from '@/composables/useResource'
import { useTask } from '@/composables/useTask'
import { flattenCategories } from '@/utils/categories'
import ResourceState from '@/components/ResourceState.vue'
const { data, loading, error, refresh } = useResource(() =>
  adminCategoryService.categoriesService(),
)
const options = computed(() => [
  { label: '顶级分类', value: 0 },
  ...flattenCategories(data.value?.data || []),
])
const form = reactive({ categoryId: 0, parentCategoryId: 0, name: '' })
const open = ref(false)
const { busy, run } = useTask()
const columns = [
  { title: '分类名称', dataIndex: 'name' },
  { title: '分类 ID', dataIndex: 'categoryId' },
  { title: '操作', key: 'actions' },
]
function create(parentCategoryId = 0) {
  Object.assign(form, { categoryId: 0, parentCategoryId, name: '' })
  open.value = true
}
function edit(item: CategoryTree) {
  Object.assign(form, {
    categoryId: item.categoryId,
    parentCategoryId: item.parentCategoryId,
    name: item.name,
  })
  open.value = true
}
async function save() {
  await run(async () => {
    if (!form.name.trim()) throw new Error('请输入分类名称')
    if (form.categoryId)
      await adminCategoryService.updateCategoryService({ ...form, name: form.name.trim() })
    else
      await adminCategoryService.createCategoryService({
        parentCategoryId: form.parentCategoryId,
        name: form.name.trim(),
      })
    open.value = false
    await refresh()
  }, '分类已保存')
}
async function remove(item: CategoryTree) {
  await run(async () => {
    await adminCategoryService.deleteCategoryService(item.categoryId)
    await refresh()
  }, '分类已删除')
}
</script>
<template>
  <section class="panel">
    <div class="between">
      <h1>分类管理</h1>
      <a-button type="primary" @click="create()">创建分类</a-button>
    </div>
    <ResourceState :loading="loading" :error="error" :empty="!data?.data.length" @retry="refresh">
      <a-table
        :columns="columns"
        :data-source="data?.data || []"
        row-key="categoryId"
        :pagination="false"
      >
        <template #bodyCell="{ column, record }">
          <a-space v-if="column.key === 'actions'">
            <a-button size="small" @click="create(record.categoryId)">添加子分类</a-button>
            <a-button size="small" @click="edit(record)">编辑</a-button>
            <a-popconfirm title="删除分类可能影响其下题目，确定删除？" @confirm="remove(record)">
              <a-button size="small" danger :disabled="busy">删除</a-button>
            </a-popconfirm>
          </a-space>
        </template>
      </a-table>
    </ResourceState>
    <a-modal
      v-model:open="open"
      :title="form.categoryId ? '修改分类' : '创建分类'"
      :confirm-loading="busy"
      @ok="save"
    >
      <div class="stack">
        <label>
          上级分类
          <a-select
            v-model:value="form.parentCategoryId"
            style="width: 100%"
            :disabled="!!form.categoryId"
            :options="options"
          />
        </label>
        <label>
          名称
          <a-input v-model:value="form.name" :maxlength="32" />
        </label>
      </div>
    </a-modal>
  </section>
</template>
