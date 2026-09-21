<script setup lang="ts">
import { computed, ref, watch } from 'vue'
import { collectionService } from '@/domain/collection/service/collectionService'
import { useResource } from '@/composables/useResource'
import { useTask } from '@/composables/useTask'
import { useSession } from '@/stores/session'
import NoteFeed from './NoteFeed.vue'
import ResourceState from './ResourceState.vue'
const props = defineProps<{ creatorId: string }>()
const session = useSession()
const own = computed(
  () => session.loggedIn && String(session.user?.userId) === String(props.creatorId),
)
const selected = ref<number>()
const name = ref('')
const description = ref('')
const open = ref(false)
watch(
  () => props.creatorId,
  () => {
    selected.value = undefined
  },
)
const { data, loading, error, refresh } = useResource(
  () =>
    collectionService.getCollectionListService({ creatorId: props.creatorId, noteId: undefined }),
  [() => props.creatorId],
)
const { busy, run } = useTask()
async function create() {
  await run(async () => {
    if (!name.value.trim()) throw new Error('请输入收藏夹名称')
    await collectionService.createCollectionService({
      name: name.value.trim(),
      description: description.value,
    })
    open.value = false
    name.value = ''
    description.value = ''
    await refresh()
  }, '收藏夹已创建')
}
async function remove(id: number) {
  await run(async () => {
    await collectionService.deleteCollectionService(id)
    if (selected.value === id) selected.value = undefined
    await refresh()
  }, '收藏夹已删除')
}
</script>
<template>
  <div class="between">
    <h2>收藏夹</h2>
    <a-button v-if="own" type="primary" @click="open = true">新建收藏夹</a-button>
  </div>
  <ResourceState :loading="loading" :error="error" :empty="!data?.data.length" @retry="refresh">
    <div v-for="item in data?.data" :key="item.collectionId" class="collection-row">
      <div>
        <a-button type="link" @click="selected = item.collectionId">{{ item.name }}</a-button>
        <p class="muted">{{ item.description }}</p>
      </div>
      <a-popconfirm
        v-if="own"
        title="删除收藏夹？其中笔记不会被删除。"
        @confirm="remove(item.collectionId)"
      >
        <a-button danger size="small" :disabled="busy">删除</a-button>
      </a-popconfirm>
    </div>
  </ResourceState>
  <template v-if="selected">
    <a-divider />
    <div class="between">
      <h3>{{ data?.data.find((item) => item.collectionId === selected)?.name }}</h3>
      <a-button @click="selected = undefined">收起</a-button>
    </div>
    <NoteFeed :collection-id="selected" />
  </template>
  <a-modal v-model:open="open" title="创建收藏夹" :confirm-loading="busy" @ok="create">
    <div class="stack">
      <a-input v-model:value="name" placeholder="名称" :maxlength="50" />
      <a-textarea v-model:value="description" placeholder="描述（选填）" :rows="3" />
    </div>
  </a-modal>
</template>
