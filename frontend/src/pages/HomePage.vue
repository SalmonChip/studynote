<script setup lang="ts">
import NoteFeed from '@/components/NoteFeed.vue'
import ActivityPanel from '@/components/ActivityPanel.vue'
import ResourceState from '@/components/ResourceState.vue'
import { noteService } from '@/domain/note/service/noteService'
import { useResource } from '@/composables/useResource'
import { useSession } from '@/stores/session'
const session = useSession()
const { data, loading, error, refresh } = useResource(() => noteService.getNoteRankListService())
</script>
<template>
  <div class="columns">
    <section class="panel"><NoteFeed /></section>
    <aside>
      <section class="panel">
        <h2>笔记排行榜</h2>
        <ResourceState
          :loading="loading"
          :error="error"
          :empty="!data?.data.length"
          @retry="refresh"
        >
          <div v-for="item in data?.data" :key="item.userId" class="rank-item">
            <strong>{{ item.rank }}</strong>
            <a-avatar :src="item.avatarUrl" size="small" />
            <RouterLink :to="`/user/${item.userId}`">{{ item.username }}</RouterLink>
            <span class="count muted">{{ item.noteCount }} 篇</span>
          </div>
        </ResourceState>
      </section>
      <ActivityPanel v-if="session.loggedIn" :key="session.user?.userId" />
    </aside>
  </div>
</template>
