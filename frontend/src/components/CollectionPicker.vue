<script setup lang="ts">
import { ref, watch } from 'vue'
import { collectionService } from '@/domain/collection/service/collectionService'
import { useResource } from '@/composables/useResource'
import { useTask } from '@/composables/useTask'
import { useSession } from '@/stores/session'
import ResourceState from './ResourceState.vue'
const props = defineProps<{ noteId: number }>()
const emit = defineEmits<{ saved: []; close: [] }>()
const session = useSession()
const selected = ref<number[]>([])
const name = ref('')
const { data, loading, error, refresh } = useResource(
  () =>
    collectionService.getCollectionListService({
      noteId: props.noteId,
      creatorId: session.user?.userId,
    }),
  [() => props.noteId],
)
watch(data, (value) => {
  selected.value = (value?.data || [])
    .filter((item) => item.noteStatus?.isCollected)
    .map((item) => item.collectionId)
})
const { busy, run } = useTask()
async function save() {
  if (loading.value || error.value || !data.value) return
  await run(async () => {
    const collections = data
      .value!.data.filter(
        (item) => selected.value.includes(item.collectionId) !== !!item.noteStatus?.isCollected,
      )
      .map((item) => ({
        collectionId: item.collectionId,
        action: selected.value.includes(item.collectionId)
          ? ('create' as const)
          : ('delete' as const),
      }))
    if (collections.length)
      await collectionService.batchUpdateCollectionService({ noteId: props.noteId, collections })
    emit('saved')
    emit('close')
  }, '收藏已更新')
}
async function create() {
  await run(async () => {
    if (!name.value.trim()) throw new Error('请输入收藏夹名称')
    await collectionService.createCollectionService({ name: name.value.trim() })
    name.value = ''
    await refresh()
  }, '收藏夹已创建')
}
</script>
<template>
  <a-modal
    :open="true"
    title="选择收藏夹"
    :confirm-loading="busy"
    :ok-button-props="{ disabled: loading || !!error }"
    @ok="save"
    @cancel="emit('close')"
  >
    <ResourceState :loading="loading" :error="error" :empty="!data?.data.length" @retry="refresh">
      <a-checkbox-group v-model:value="selected" class="stack" :disabled="busy">
        <a-checkbox v-for="item in data?.data" :key="item.collectionId" :value="item.collectionId">
          {{ item.name }}
        </a-checkbox>
      </a-checkbox-group>
    </ResourceState>
    <a-divider />
    <div class="toolbar">
      <a-input v-model:value="name" placeholder="新收藏夹名称" :maxlength="50" />
      <a-button :loading="busy" @click="create">新建收藏夹</a-button>
    </div>
  </a-modal>
</template>
