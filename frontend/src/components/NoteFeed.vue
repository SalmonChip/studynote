<script setup lang="ts">
import { ref, watch } from 'vue'
import { noteService } from '@/domain/note/service/noteService'
import { useResource } from '@/composables/useResource'
import { useSession } from '@/stores/session'
import NoteCard from './NoteCard.vue'
import ResourceState from './ResourceState.vue'
const props = defineProps<{
  authorId?: string
  questionId?: number
  collectionId?: number
  hideQuestion?: boolean
  revision?: number
}>()
const session = useSession()
const page = ref(1)
const pageSize = ref(10)
const order = ref<'asc' | 'desc'>('desc')
watch(
  () => [props.authorId, props.questionId, props.collectionId, order.value],
  () => {
    page.value = 1
  },
  { flush: 'sync' },
)
const { data, loading, error, refresh } = useResource(
  () =>
    noteService.getNoteList({
      authorId: props.authorId,
      questionId: props.questionId,
      collectionId: props.collectionId,
      page: page.value,
      pageSize: pageSize.value,
      sort: 'create',
      order: order.value,
    }),
  [
    page,
    pageSize,
    order,
    () => props.authorId,
    () => props.questionId,
    () => props.collectionId,
    () => props.revision,
    () => session.user?.userId,
  ],
)
defineExpose({ refresh })
</script>
<template>
  <div class="between">
    <h2>学习笔记</h2>
    <a-select
      v-model:value="order"
      :options="[
        { label: '最新发布', value: 'desc' },
        { label: '最早发布', value: 'asc' },
      ]"
    />
  </div>
  <ResourceState
    :loading="loading && !data"
    :error="error"
    :empty="!data?.data.length"
    @retry="refresh"
  >
    <NoteCard
      v-for="item in data?.data"
      :key="item.noteId"
      :note="item"
      :hide-question="hideQuestion"
      @changed="refresh"
    />
  </ResourceState>
  <div v-if="data?.pagination" class="pagination">
    <a-pagination
      v-model:current="page"
      v-model:page-size="pageSize"
      :total="data.pagination.total"
      show-size-changer
    />
  </div>
</template>
