<script setup lang="ts">
import { computed, ref, watch } from 'vue'
import { useRoute, onBeforeRouteLeave } from 'vue-router'
import { questionService } from '@/domain/question/service/questionService'
import { noteService } from '@/domain/note/service/noteService'
import { useResource } from '@/composables/useResource'
import { useTask } from '@/composables/useTask'
import { useSession } from '@/stores/session'
import NoteFeed from '@/components/NoteFeed.vue'
import DifficultyTag from '@/components/DifficultyTag.vue'
import MarkdownEditor from '@/components/MarkdownEditor.vue'
import ResourceState from '@/components/ResourceState.vue'
const route = useRoute()
const id = computed(() => Number(route.params.questionId))
const session = useSession()
const { data, loading, error, refresh } = useResource(
  () => questionService.getQuestionByIdService(id.value),
  [id, () => session.user?.userId],
)
const editorOpen = ref(false)
const content = ref('')
const initial = ref('')
const revision = ref(0)
const { busy, run } = useTask()
watch(id, () => {
  editorOpen.value = false
  content.value = ''
  initial.value = ''
})
onBeforeRouteLeave(
  () =>
    !editorOpen.value ||
    content.value === initial.value ||
    window.confirm('笔记尚未保存，确定离开？'),
)
function openEditor() {
  if (!session.requireLogin()) return
  content.value = data.value?.data.userNote?.content || ''
  initial.value = content.value
  editorOpen.value = true
}
async function save() {
  if (!session.requireLogin()) return
  await run(async () => {
    if (!content.value.trim()) throw new Error('笔记内容不能为空')
    const note = data.value?.data.userNote
    const body = { questionId: id.value, content: content.value }
    if (note?.finished && note.noteId) await noteService.updateNoteService(note.noteId, body)
    else await noteService.createNoteService(body)
    editorOpen.value = false
    revision.value++
    await refresh()
  }, '笔记已保存')
}
</script>
<template>
  <div class="page-narrow">
    <section class="panel">
      <ResourceState :loading="loading" :error="error" @retry="refresh">
        <h1>{{ data?.data.title }}</h1>
        <div class="toolbar">
          <DifficultyTag :difficulty="data?.data.difficulty" />
          <span class="muted">浏览 {{ data?.data.viewCount || 0 }}</span>
          <a-tag v-if="data?.data.userNote?.finished" color="green">已完成</a-tag>
        </div>
        <p v-if="data?.data.examPoint">考点：{{ data.data.examPoint }}</p>
        <a-button type="primary" @click="openEditor">
          {{ data?.data.userNote?.finished ? '编辑我的笔记' : '写笔记' }}
        </a-button>
      </ResourceState>
    </section>
    <section v-if="editorOpen" class="panel">
      <MarkdownEditor v-model="content" />
      <div class="note-actions">
        <a-button type="primary" :loading="busy" @click="save">保存笔记</a-button>
        <a-button :disabled="busy" @click="editorOpen = false">取消</a-button>
      </div>
    </section>
    <section class="panel">
      <NoteFeed :question-id="id" hide-question :revision="revision" />
    </section>
  </div>
</template>
