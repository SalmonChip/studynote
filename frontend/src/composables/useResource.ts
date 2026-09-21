import { onScopeDispose, ref, shallowRef, watch, type WatchSource } from 'vue'
import { errorText } from './useTask'

export function useResource<T>(fetcher: () => Promise<T>, dependencies: WatchSource[] = []) {
  const data = shallowRef<T>()
  const loading = ref(false)
  const error = ref('')
  let version = 0
  async function load(keepData: boolean) {
    const current = ++version
    loading.value = true
    error.value = ''
    if (!keepData) data.value = undefined
    try {
      const result = await fetcher()
      if (current === version) data.value = result
    } catch (e) {
      if (current === version) error.value = errorText(e)
    } finally {
      if (current === version) loading.value = false
    }
  }
  const refresh = () => load(true)
  watch(dependencies, () => load(false), { immediate: true })
  onScopeDispose(() => {
    version++
  })
  return { data, loading, error, refresh }
}
