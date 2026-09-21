<script setup lang="ts">
import { computed, ref, watch } from 'vue'
import type { NoteWithRelations } from '@/domain/note/types/serviceTypes'
import { noteService } from '@/domain/note/service/noteService'
import { noteLikeService } from '@/domain/noteLike/service/noteLikeService'
import { useSession } from '@/stores/session'
import { useTask } from '@/composables/useTask'
import { downloadMarkdown } from '@/utils/download'
import MarkdownView from './MarkdownView.vue'
import MarkdownEditor from './MarkdownEditor.vue'
import CollectionPicker from './CollectionPicker.vue'
import CommentsPanel from './CommentsPanel.vue'
const props = defineProps<{ note: NoteWithRelations; hideQuestion?: boolean }>()
const emit = defineEmits<{ changed: [] }>()
const session = useSession()
const expanded = ref(false)
const collectionOpen = ref(false)
const commentsOpen = ref(false)
const editOpen = ref(false)
const draft = ref('')
const liked = ref(false)
const likes = ref(0)
watch(
  () => props.note,
  (note) => {
    liked.value = !!note.userActions?.isLiked
    likes.value = note.likeCount || 0
  },
  { immediate: true },
)
const own = computed(
  () => session.loggedIn && String(session.user?.userId) === String(props.note.author?.userId),
)
const { busy, run } = useTask()
function startEdit() {
  draft.value = props.note.content
  editOpen.value = true
}
async function like() {
  if (!session.requireLogin()) return
  await run(async () => {
    await (liked.value ? noteLikeService.unLikeService : noteLikeService.likeService)(
      props.note.noteId,
    )
    likes.value = Math.max(0, likes.value + (liked.value ? -1 : 1))
    liked.value = !liked.value
  })
}
async function save() {
  await run(async () => {
    if (!draft.value.trim()) throw new Error('笔记内容不能为空')
    await noteService.updateNoteService(props.note.noteId, {
      questionId: props.note.question.questionId,
      content: draft.value,
    })
    editOpen.value = false
    emit('changed')
  }, '笔记已保存')
}
async function remove() {
  await run(async () => {
    await noteService.deleteNoteService(props.note.noteId)
    emit('changed')
  }, '笔记已删除')
}
</script>
<template>
  <article class="note-card">
    <div class="identity">
      <a-avatar :src="note.author?.avatarUrl">{{ note.author?.username?.slice(0, 1) }}</a-avatar>
      <div>
        <RouterLink :to="`/user/${note.author?.userId}`">{{ note.author?.username }}</RouterLink>
        <div class="muted">{{ note.createdAt?.replace('T', ' ').slice(0, 16) }}</div>
      </div>
    </div>
    <RouterLink
      v-if="!hideQuestion"
      class="note-title"
      :to="`/questions/${note.question?.questionId}`"
    >
      {{ note.question?.title }}
    </RouterLink>
    <MarkdownView :content="note.needCollapsed && !expanded ? note.displayContent : note.content" />
    <a-button v-if="note.needCollapsed" type="link" @click="expanded = !expanded">
      {{ expanded ? '收起' : '展开全文' }}
    </a-button>
    <div class="note-actions">
      <a-button type="text" :loading="busy" @click="like">
        {{ liked ? '已赞' : '点赞' }} {{ likes }}
      </a-button>
      <a-button type="text" @click="session.requireLogin() && (collectionOpen = true)">
        {{ note.userActions?.isCollected ? '已收藏' : '收藏' }} {{ note.collectCount || 0 }}
      </a-button>
      <a-button type="text" @click="commentsOpen = true">
        评论 {{ note.commentCount || 0 }}
      </a-button>
      <a-button
        type="text"
        @click="downloadMarkdown(note.content, `${note.question?.title || '笔记'}.md`)"
      >
        下载
      </a-button>
      <template v-if="own">
        <a-button type="text" @click="startEdit">编辑</a-button>
        <a-popconfirm title="确定删除这条笔记？" @confirm="remove">
          <a-button type="text" danger :disabled="busy">删除</a-button>
        </a-popconfirm>
      </template>
    </div>
    <CollectionPicker
      v-if="collectionOpen"
      :note-id="note.noteId"
      @close="collectionOpen = false"
      @saved="emit('changed')"
    />
    <a-drawer v-model:open="commentsOpen" title="评论" :width="520" destroy-on-close>
      <CommentsPanel v-if="commentsOpen" :note-id="note.noteId" @changed="emit('changed')" />
    </a-drawer>
    <a-modal
      v-model:open="editOpen"
      title="编辑笔记"
      width="900px"
      :confirm-loading="busy"
      @ok="save"
    >
      <MarkdownEditor v-if="editOpen" v-model="draft" />
    </a-modal>
  </article>
</template>
