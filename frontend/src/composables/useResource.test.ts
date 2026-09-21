import { effectScope, nextTick, ref } from 'vue'
import { describe, expect, it } from 'vitest'
import { flushPromises } from '@vue/test-utils'
import { useResource } from './useResource'

describe('resource lifecycle', () => {
  it('ignores stale results when filters change', async () => {
    const filter = ref(1)
    const resolvers: ((value: string) => void)[] = []
    const scope = effectScope()
    const state = scope.run(() =>
      useResource(() => new Promise<string>((resolve) => resolvers.push(resolve)), [filter]),
    )!
    filter.value = 2
    await nextTick()
    resolvers[1]('new')
    await flushPromises()
    resolvers[0]('old')
    await flushPromises()
    expect(state.data.value).toBe('new')
    expect(state.loading.value).toBe(false)
    scope.stop()
  })
  it('keeps content on manual refresh and exposes failures for retry', async () => {
    let fail = false
    const scope = effectScope()
    const state = scope.run(() =>
      useResource(async () => {
        if (fail) throw new Error('offline')
        return 'saved'
      }),
    )!
    await flushPromises()
    fail = true
    await state.refresh()
    expect(state.data.value).toBe('saved')
    expect(state.error.value).toBe('offline')
    fail = false
    await state.refresh()
    expect(state.error.value).toBe('')
    scope.stop()
  })
})
