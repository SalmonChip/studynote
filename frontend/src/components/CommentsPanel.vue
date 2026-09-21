<script setup lang="ts">
import { ref } from 'vue'
import type { Comment } from '@/domain/comment/types'
import { commentService } from '@/domain/comment/service/commentService'
import { useResource } from '@/composables/useResource'
import { useTask } from '@/composables/useTask'
import { useSession } from '@/stores/session'
import ResourceState from './ResourceState.vue'
import CommentNode from './CommentNode.vue'
const props = defineProps<{ noteId: number }>()
const emit = defineEmits<{ changed: [] }>()
const session = useSession()
const page = ref(1)
const content = ref('')
const replyTo = ref<Comment>()
const editing = ref<Comment>()
const { data, loading, error, refresh } = useResource(
  () => commentService.getCommentsService({ noteId: props.noteId, page: page.value, pageSize: 10 }),
  [() => props.noteId, page],
)
const { busy, run } = useTask()
function cancelReply() {
  replyTo.value = undefined
  editing.value = undefined
  content.value = ''
}
function reply(item: Comment) {
  if (!session.requireLogin()) return
  replyTo.value = item
  editing.value = undefined
  content.value = ''
}
function edit(item: Comment) {
  editing.value = item
  replyTo.value = undefined
  content.value = item.content
}
async function submit() {
  if (!session.requireLogin()) return
  await run(async () => {
    if (!content.value.trim()) throw new Error('请输入评论内容')
    if (editing.value)
      await commentService.updateComment(editing.value.commentId, content.value.trim())
    else
      await commentService.createCommentService({
        noteId: props.noteId,
        parentId: replyTo.value?.commentId,
        content: content.value.trim(),
      })
    content.value = ''
    replyTo.value = undefined
    editing.value = undefined
    await refresh()
    emit('changed')
  }, '评论已保存')
}
async function like(item: Comment) {
  if (!session.requireLogin()) return
  await run(async () => {
    await (
      item.userActions?.isLiked
        ? commentService.unlikeCommentService
        : commentService.likeCommentService
    )(item.commentId)
    await refresh()
  })
}
async function remove(item: Comment) {
  await run(async () => {
    await commentService.deleteComment(item.commentId)
    await refresh()
    emit('changed')
  }, '评论已删除')
}
</script>
<template>
  <div class="stack">
    <a-alert
      v-if="replyTo || editing"
      :message="editing ? '编辑评论' : `回复 ${replyTo?.author?.username}`"
      closable
      @close="cancelReply"
    />
    <a-textarea v-model:value="content" :rows="3" :maxlength="2000" placeholder="写下你的评论…" />
    <a-button type="primary" :loading="busy" @click="submit">
      {{ editing ? '保存修改' : '发表评论' }}
    </a-button>
    <ResourceState :loading="loading" :error="error" :empty="!data?.data.length" @retry="refresh">
      <div>
        <CommentNode
          v-for="item in data?.data"
          :key="item.commentId"
          :comment="item"
          :busy="busy"
          @reply="reply"
          @like="like"
          @remove="remove"
          @edit="edit"
        />
      </div>
    </ResourceState>
    <a-pagination
      v-if="data?.pagination"
      v-model:current="page"
      :total="data.pagination.total"
      :page-size="10"
      :show-size-changer="false"
      size="small"
    />
  </div>
</template>
