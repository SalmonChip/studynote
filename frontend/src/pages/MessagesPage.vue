<script setup lang="ts">
import { computed, ref } from 'vue'
import { useRouter } from 'vue-router'
import { useSession } from '@/stores/session'
import { messageService } from '@/domain/message/service/messageService'
import type { Message } from '@/domain/message/types'
import { useResource } from '@/composables/useResource'
import { useTask } from '@/composables/useTask'
import ResourceState from '@/components/ResourceState.vue'
const router = useRouter()
const session = useSession()
const tab = ref(0)
const selected = ref<number[]>([])
const { data, loading, error, refresh } = useResource(() => messageService.getMessages())
const messages = computed(() =>
  (data.value?.data || []).filter((item) => !tab.value || item.type === tab.value),
)
const unread = computed(() => data.value?.data.filter((item) => !item.isRead).length || 0)
const { busy, run } = useTask()
async function update(action: () => Promise<unknown>) {
  return run(async () => {
    await action()
    selected.value = []
    await refresh()
    await session.refreshUnread()
  })
}
async function open(item: Message) {
  if (busy.value) return
  if (!item.isRead && !(await update(() => messageService.readMessages([item.messageId])))) return
  if (item.target?.question?.questionId)
    void router.push(`/questions/${item.target.question.questionId}`)
}
</script>
<template>
  <section class="panel page-narrow">
    <h1>消息中心</h1>
    <p class="muted">共 {{ data?.data.length || 0 }} 条消息，{{ unread }} 条未读</p>
    <div class="toolbar">
      <a-segmented
        v-model:value="tab"
        :options="[
          { label: '全部', value: 0 },
          { label: '点赞', value: 1 },
          { label: '评论', value: 2 },
          { label: '系统', value: 3 },
        ]"
      />
      <a-button :disabled="!unread || busy" @click="update(() => messageService.readAllMessages())">
        全部已读
      </a-button>
      <a-button
        :disabled="!selected.length || busy"
        @click="update(() => messageService.readMessages(selected))"
      >
        选中项已读
      </a-button>
    </div>
    <ResourceState :loading="loading" :error="error" :empty="!messages.length" @retry="refresh">
      <a-checkbox-group v-model:value="selected" style="display: block">
        <article
          v-for="item in messages"
          :key="item.messageId"
          class="message-row"
          :class="{ unread: !item.isRead }"
        >
          <div class="between">
            <div class="identity">
              <a-checkbox :value="item.messageId" :disabled="busy" />
              <a-avatar :src="item.sender?.avatar" />
              <strong>{{ item.type === 3 ? '系统通知' : item.sender?.username }}</strong>
              <a-tag v-if="!item.isRead" color="blue">未读</a-tag>
            </div>
            <a-popconfirm
              title="删除这条消息？"
              @confirm="update(() => messageService.deleteMessage(item.messageId))"
            >
              <a-button size="small" danger :disabled="busy">删除</a-button>
            </a-popconfirm>
          </div>
          <p>{{ item.content }}</p>
          <a-button v-if="item.target?.question" type="link" :disabled="busy" @click="open(item)">
            {{ item.target.question.title }}
          </a-button>
          <div class="between">
            <span class="muted">{{ item.createdAt?.replace('T', ' ').slice(0, 16) }}</span>
            <a-button
              v-if="!item.isRead"
              size="small"
              :disabled="busy"
              @click="update(() => messageService.readMessages([item.messageId]))"
            >
              标记已读
            </a-button>
          </div>
        </article>
      </a-checkbox-group>
    </ResourceState>
  </section>
</template>
