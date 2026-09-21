export function downloadMarkdown(markdown: string, name = '学习笔记.md') {
  const url = URL.createObjectURL(new Blob([markdown], { type: 'text/markdown;charset=utf-8' }))
  const link = document.createElement('a')
  link.href = url
  link.download = name.replace(/[\\/:*?"<>|]/g, '_')
  link.click()
  setTimeout(() => URL.revokeObjectURL(url), 1000)
}
