<script setup lang="ts">
import type { Comment } from '@/domain/comment/types'
import { useSession } from '@/stores/session'
const props = defineProps<{ comment: Comment; busy: boolean }>()
defineEmits<{
  reply: [comment: Comment]
  like: [comment: Comment]
  remove: [comment: Comment]
  edit: [comment: Comment]
}>()
const session = useSession()
</script>
<template>
  <article class="comment">
    <div class="identity">
      <a-avatar :src="comment.author?.avatarUrl" size="small" />
      <RouterLink :to="`/user/${comment.author?.userId}`">
        {{ comment.author?.username }}
      </RouterLink>
      <span class="muted">{{ comment.createdAt?.replace('T', ' ').slice(0, 16) }}</span>
    </div>
    <p class="comment-content">{{ comment.content }}</p>
    <a-space>
      <a-button size="small" type="text" :disabled="busy" @click="$emit('like', props.comment)">
        {{ comment.userActions?.isLiked ? '取消赞' : '赞' }} {{ comment.likeCount }}
      </a-button>
      <a-button size="small" type="text" @click="$emit('reply', props.comment)">回复</a-button>
      <template
        v-if="session.loggedIn && String(session.user?.userId) === String(comment.author?.userId)"
      >
        <a-button size="small" type="text" :disabled="busy" @click="$emit('edit', props.comment)">
          编辑
        </a-button>
        <a-popconfirm title="删除这条评论？" @confirm="$emit('remove', props.comment)">
          <a-button size="small" type="text" danger :disabled="busy">删除</a-button>
        </a-popconfirm>
      </template>
    </a-space>
    <div v-if="comment.replies?.length" class="replies">
      <CommentNode
        v-for="child in comment.replies"
        :key="child.commentId"
        :comment="child"
        :busy="busy"
        @reply="$emit('reply', $event)"
        @like="$emit('like', $event)"
        @remove="$emit('remove', $event)"
        @edit="$emit('edit', $event)"
      />
    </div>
  </article>
</template>
