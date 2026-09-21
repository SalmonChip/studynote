import { describe, expect, it } from 'vitest'
import { mount } from '@vue/test-utils'
import MarkdownView from './MarkdownView.vue'

describe('untrusted Markdown rendering', () => {
  it('renders headings and code without executing HTML or javascript links', () => {
    const wrapper = mount(MarkdownView, {
      props: {
        content:
          '# 学习笔记\n\n<script>alert(1)</script>\n\n[x](javascript:alert(1))\n\n```js\nconst answer = 42\n```',
      },
    })
    expect(wrapper.find('h1').text()).toBe('学习笔记')
    expect(wrapper.find('script').exists()).toBe(false)
    expect(wrapper.find('a[href^="javascript:"]').exists()).toBe(false)
    expect(wrapper.find('pre code').text()).toContain('const answer = 42')
    wrapper.unmount()
  })
})
