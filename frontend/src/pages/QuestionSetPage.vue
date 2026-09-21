<script setup lang="ts">
import { computed, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { categoryService } from '@/domain/category/service/categoryService'
import { questionService } from '@/domain/question/service/questionService'
import { useResource } from '@/composables/useResource'
import { useSession } from '@/stores/session'
import ResourceState from '@/components/ResourceState.vue'
import QuestionRows from '@/components/QuestionRows.vue'
const route = useRoute()
const router = useRouter()
const session = useSession()
const categoryId = computed(() => Number(route.query.categoryId) || undefined)
const categories = useResource(() => categoryService.list())
const page = ref(1)
const pageSize = ref(10)
const sort = ref<'view' | 'difficulty'>()
const order = ref<'asc' | 'desc'>('desc')
watch(
  [categoryId, sort, order],
  () => {
    page.value = 1
  },
  { flush: 'sync' },
)
const { data, loading, error, refresh } = useResource(
  () =>
    questionService.getQuestionListService({
      categoryId: categoryId.value,
      page: page.value,
      pageSize: pageSize.value,
      sort: sort.value,
      order: sort.value ? order.value : undefined,
    }),
  [categoryId, page, pageSize, sort, order, () => session.user?.userId],
)
function select(keys: (string | number)[]) {
  void router.replace({
    query: { ...route.query, categoryId: keys[0] ? String(keys[0]) : undefined },
  })
}
</script>
<template>
  <div class="sidebar-layout">
    <aside class="panel">
      <h2>题目分类</h2>
      <a-button type="link" @click="select([])">全部题目</a-button>
      <ResourceState
        :loading="categories.loading.value"
        :error="categories.error.value"
        @retry="categories.refresh"
      >
        <a-tree
          :tree-data="categories.data.value?.data || []"
          :field-names="{ key: 'categoryId', title: 'name', children: 'children' }"
          :selected-keys="categoryId ? [categoryId] : []"
          @select="select"
        />
      </ResourceState>
    </aside>
    <section class="panel">
      <div class="between">
        <h1>题库</h1>
        <div class="toolbar">
          <a-select
            v-model:value="sort"
            allow-clear
            placeholder="默认排序"
            :options="[
              { label: '按浏览量', value: 'view' },
              { label: '按难度', value: 'difficulty' },
            ]"
          />
          <a-select
            v-model:value="order"
            :options="[
              { label: '降序', value: 'desc' },
              { label: '升序', value: 'asc' },
            ]"
          />
        </div>
      </div>
      <ResourceState :loading="loading" :error="error" :empty="!data?.data.length" @retry="refresh">
        <QuestionRows :rows="data?.data || []" />
      </ResourceState>
      <div v-if="data?.pagination" class="pagination">
        <a-pagination
          v-model:current="page"
          v-model:page-size="pageSize"
          :total="data.pagination.total"
          show-size-changer
        />
      </div>
    </section>
  </div>
</template>
