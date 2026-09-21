import { ref } from 'vue'
import { message } from 'ant-design-vue'

export function errorText(error: unknown) {
  if (error instanceof Error && error.name === 'AbortError') return '请求超时，请重试'
  return error instanceof Error ? error.message : '操作失败，请重试'
}

export function useTask() {
  const busy = ref(false)
  async function run(action: () => Promise<void>, success?: string) {
    if (busy.value) return false
    busy.value = true
    try {
      await action()
      if (success) message.success(success)
      return true
    } catch (error) {
      message.error(errorText(error))
      return false
    } finally {
      busy.value = false
    }
  }
  return { busy, run }
}
