import { beforeEach, describe, expect, it, vi } from 'vitest'
import { createPinia, setActivePinia } from 'pinia'
import { useSession } from './session'
import { userService } from '@/domain/user/service/userService'
import type { UserState } from '@/domain/user/types/types'
vi.mock('@/domain/user/service/userService', () => ({
  userService: { whoamiService: vi.fn(), loginService: vi.fn(), registerService: vi.fn() },
}))
const user = { userId: '1', username: '测试', isAdmin: 1 } as UserState
describe('session lifecycle', () => {
  beforeEach(() => {
    setActivePinia(createPinia())
    localStorage.clear()
    vi.resetAllMocks()
  })
  it('does not query whoami for visitors', async () => {
    const session = useSession()
    await session.restore()
    expect(session.ready).toBe(true)
    expect(userService.whoamiService).not.toHaveBeenCalled()
  })
  it('uses refreshed token and clears all session state on logout', async () => {
    localStorage.setItem('token', 'old')
    vi.mocked(userService.whoamiService).mockResolvedValue({
      code: 200,
      data: user,
      token: 'renewed',
    })
    const session = useSession()
    await session.restore()
    expect(session.isAdmin).toBe(true)
    expect(localStorage.getItem('token')).toBe('renewed')
    session.logout()
    expect(session.loggedIn).toBe(false)
    expect(localStorage.getItem('token')).toBeNull()
  })
  it('does not restore a session after logout while whoami is pending', async () => {
    localStorage.setItem('token', 'old')
    let resolve!: (value: { code: number; data: UserState; token: string }) => void
    vi.mocked(userService.whoamiService).mockReturnValue(
      new Promise((done) => {
        resolve = done
      }),
    )
    const session = useSession()
    const pending = session.restore()
    session.logout()
    resolve({ code: 200, data: user, token: 'late-token' })
    await pending
    expect(session.user).toBeNull()
    expect(localStorage.getItem('token')).toBeNull()
  })
})
