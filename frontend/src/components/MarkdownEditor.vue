<script setup lang="ts">
import { ref } from 'vue'
import MarkdownView from './MarkdownView.vue'
import { userService } from '@/domain/user/service/userService'
import { useTask } from '@/composables/useTask'
const value = defineModel<string>({ required: true })
const preview = ref(false)
const input = ref<HTMLTextAreaElement>()
const { busy, run } = useTask()
function insert(before: string, after = '') {
  const start = input.value?.selectionStart ?? value.value.length
  const end = input.value?.selectionEnd ?? start
  value.value =
    value.value.slice(0, start) +
    before +
    value.value.slice(start, end) +
    after +
    value.value.slice(end)
}
function upload(event: Event) {
  const field = event.target as HTMLInputElement
  const file = field.files?.[0]
  if (!file) return
  void run(async () => {
    if (!file.type.startsWith('image/')) throw new Error('请选择图片')
    if (file.size > 10 * 1024 * 1024) throw new Error('图片不能超过 10MB')
    const body = new FormData()
    body.append('file', file)
    const result = await userService.uploadImageService(body)
    insert(`![图片](${result.data.url})`)
  })
  field.value = ''
}
</script>

<template>
  <div class="editor">
    <div class="toolbar">
      <a-button @click="insert('**', '**')">加粗</a-button>
      <a-button @click="insert('*', '*')">斜体</a-button>
      <a-button @click="insert('\n## ')">标题</a-button>
      <a-button @click="insert('\n- ')">列表</a-button>
      <a-button @click="insert('\n```\n', '\n```\n')">代码</a-button>
      <a-button @click="insert('[链接](', ')')">链接</a-button>
      <label class="upload-button">
        {{ busy ? '上传中…' : '插入图片' }}
        <input type="file" accept="image/*" :disabled="busy" @change="upload" />
      </label>
      <a-switch v-model:checked="preview" checked-children="预览" un-checked-children="编辑" />
    </div>
    <MarkdownView v-if="preview" :content="value" />
    <textarea
      v-else
      ref="input"
      v-model="value"
      aria-label="Markdown 笔记内容"
      placeholder="使用 Markdown 记录你的思考…"
    />
  </div>
</template>
