<script setup lang="ts">
import { computed, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import {
  adminQuestionListService,
  userQuestionListService,
} from '@/domain/questionList/service/questionListService'
import { useResource } from '@/composables/useResource'
import { useSession } from '@/stores/session'
import ResourceState from '@/components/ResourceState.vue'
import QuestionRows from '@/components/QuestionRows.vue'
const route = useRoute()
const router = useRouter()
const session = useSession()
const id = computed(() => Number(route.query.questionListId) || undefined)
const lists = useResource(() => adminQuestionListService.getQuestionListService())
const selected = computed(() =>
  lists.data.value?.data.find((item) => item.questionListId === id.value),
)
const page = ref(1)
const pageSize = ref(10)
watch(
  id,
  () => {
    page.value = 1
  },
  { flush: 'sync' },
)
const { data, loading, error, refresh } = useResource(
  async () =>
    id.value
      ? userQuestionListService.getQuestionListByIdService({
          questionListId: id.value,
          page: page.value,
          pageSize: pageSize.value,
        })
      : undefined,
  [id, page, pageSize, () => session.user?.userId],
)
const rows = computed(
  () =>
    data.value?.data.map((item) => ({
      ...item.question,
      userQuestionStatus: item.userQuestionStatus,
    })) || [],
)
</script>
<template>
  <div class="sidebar-layout">
    <aside class="panel">
      <h2>学习题单</h2>
      <ResourceState
        :loading="lists.loading.value"
        :error="lists.error.value"
        :empty="!lists.data.value?.data.length"
        @retry="lists.refresh"
      >
        <div v-for="type in [1, 2]" :key="type">
          <h3>{{ type === 1 ? '普通题单' : '训练营题单' }}</h3>
          <button
            v-for="item in lists.data.value?.data.filter((list) => list.type === type)"
            :key="item.questionListId"
            class="tree-button"
            :class="{ active: id === item.questionListId }"
            @click="router.replace({ query: { questionListId: item.questionListId } })"
          >
            {{ item.name }}
          </button>
        </div>
      </ResourceState>
    </aside>
    <section class="panel">
      <h1>{{ selected?.name || '题单' }}</h1>
      <p v-if="selected?.description" class="muted">{{ selected.description }}</p>
      <a-empty v-if="!id" description="请选择左侧题单" />
      <ResourceState
        v-else
        :loading="loading"
        :error="error"
        :empty="!rows.length"
        @retry="refresh"
      >
        <QuestionRows :rows="rows" />
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
