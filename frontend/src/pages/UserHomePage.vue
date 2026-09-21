<script setup lang="ts">
import { computed, ref } from 'vue'
import { useRoute } from 'vue-router'
import { userService } from '@/domain/user/service/userService'
import { useResource } from '@/composables/useResource'
import ResourceState from '@/components/ResourceState.vue'
import NoteFeed from '@/components/NoteFeed.vue'
import CollectionsPanel from '@/components/CollectionsPanel.vue'
const route = useRoute()
const id = computed(() => String(route.params.userId))
const tab = ref('notes')
const { data, loading, error, refresh } = useResource(
  () => userService.getUserService(id.value),
  [id],
)
</script>
<template>
  <div class="page-narrow">
    <section class="panel">
      <ResourceState :loading="loading" :error="error" @retry="refresh">
        <div class="profile-header">
          <a-avatar :src="data?.data.avatarUrl" :size="72" />
          <div>
            <h1>{{ data?.data.username }}</h1>
            <p>{{ data?.data.signature || '还没有填写个性签名' }}</p>
            <p class="muted">{{ data?.data.school }}</p>
          </div>
        </div>
      </ResourceState>
    </section>
    <section class="panel">
      <a-segmented
        v-model:value="tab"
        :options="[
          { label: '学习笔记', value: 'notes' },
          { label: '收藏夹', value: 'collections' },
        ]"
        class="toolbar"
      />
      <NoteFeed v-if="tab === 'notes'" :author-id="id" />
      <CollectionsPanel v-else :creator-id="id" />
    </section>
  </div>
</template>
