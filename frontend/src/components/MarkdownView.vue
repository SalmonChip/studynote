<script setup lang="ts">
import { computed } from 'vue'
import MarkdownIt from 'markdown-it'
import DOMPurify from 'dompurify'
import hljs from 'highlight.js/lib/common'
import 'highlight.js/styles/github.css'
import { message } from 'ant-design-vue'
const props = defineProps<{ content: string }>()
const md = new MarkdownIt({
  html: false,
  linkify: true,
  breaks: true,
  highlight(code, language) {
    return language && hljs.getLanguage(language) ? hljs.highlight(code, { language }).value : ''
  },
})
const html = computed(() => DOMPurify.sanitize(md.render(props.content || '')))
async function copy(event: MouseEvent) {
  const element = event.target as HTMLElement
  const pre = element.closest('pre')
  if (!pre) return
  try {
    await navigator.clipboard.writeText(pre.textContent || '')
    message.success('代码已复制')
  } catch {
    message.error('复制失败，请手动选择代码')
  }
}
</script>

<template><div class="markdown-body" @dblclick="copy" v-html="html"></div></template>
