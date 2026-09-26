<script setup lang="ts">
import { useRoute } from 'vue-router'
import { useSession } from '@/stores/session'
import ProfileForm from '@/components/ProfileForm.vue'
import NoteFeed from '@/components/NoteFeed.vue'
import CollectionsPanel from '@/components/CollectionsPanel.vue'
import MyCoursesPanel from '@/components/MyCoursesPanel.vue'
import { noteService } from '@/domain/note/service/noteService'
import { useTask } from '@/composables/useTask'
import { downloadMarkdown } from '@/utils/download'
const route = useRoute()
const session = useSession()
const { busy, run } = useTask()
async function download() {
  await run(async () => {
    downloadMarkdown((await noteService.downloadNoteService()).data.markdown)
  })
}
</script>
<template>
  <section v-if="session.user" class="panel page-narrow">
    <h1>个人中心</h1>
    <nav class="tabs">
      <RouterLink to="/user-center/info">个人资料</RouterLink>
      <RouterLink to="/user-center/note">我的笔记</RouterLink>
      <RouterLink to="/user-center/collect">我的收藏</RouterLink>
      <RouterLink to="/user-center/course">我的课程</RouterLink>
    </nav>
    <ProfileForm v-if="route.params.section === 'info'" />
    <template v-else-if="route.params.section === 'note'">
      <div class="toolbar">
        <a-button :loading="busy" @click="download">导出全部笔记 Markdown</a-button>
      </div>
      <NoteFeed :author-id="String(session.user.userId)" />
    </template>
    <!-- 必须用 v-else-if，否则 CollectionsPanel 会把 course 分支也吞掉 -->
    <CollectionsPanel
      v-else-if="route.params.section === 'collect'"
      :creator-id="String(session.user.userId)"
    />
    <MyCoursesPanel v-else />
  </section>
</template>
