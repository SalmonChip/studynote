import { computed, ref } from 'vue'
import { defineStore } from 'pinia'
import { userService } from '@/domain/user/service/userService'
import { messageService } from '@/domain/message/service/messageService'
import type { UserState } from '@/domain/user/types/types'
import type { LoginBody, RegisterBody } from '@/domain/user/types/serviceTypes'
import { ApiError, TOKEN_KEY } from '@/request/fetchClient'

export const useSession = defineStore('session', () => {
  const user = ref<UserState | null>(null)
  const ready = ref(false)
  const loginOpen = ref(false)
  const unread = ref(0)
  const loggedIn = computed(() => !!user.value)
  const isAdmin = computed(() => Number(user.value?.isAdmin) === 1)
  let restoring: Promise<void> | undefined
  let generation = 0

  function logout() {
    generation++
    restoring = undefined
    user.value = null
    unread.value = 0
    localStorage.removeItem(TOKEN_KEY)
    localStorage.removeItem('studynote:userToken')
    localStorage.removeItem('currentUser')
  }

  async function restore(force = false) {
    if (restoring) return restoring
    if (ready.value && !force) return
    const current = generation
    restoring = (async () => {
      try {
        if (!localStorage.getItem(TOKEN_KEY)) return
        const result = await userService.whoamiService()
        if (current !== generation) return
        user.value = result.data
        if (result.token) localStorage.setItem(TOKEN_KEY, result.token)
      } catch (error) {
        if (current !== generation) return
        user.value = null
        if (error instanceof ApiError && [400, 401, 403].includes(error.code)) logout()
      } finally {
        if (current === generation) ready.value = true
      }
    })()
    await restoring
    restoring = undefined
  }

  async function login(body: LoginBody) {
    const current = ++generation
    const result = await userService.loginService(body)
    if (current !== generation) throw new Error('登录操作已取消')
    if (!result.token) throw new Error('登录响应缺少凭证')
    localStorage.setItem(TOKEN_KEY, result.token)
    user.value = result.data
    ready.value = true
    loginOpen.value = false
  }

  async function register(body: RegisterBody) {
    const result = await userService.registerService(body)
    if (result.token) {
      localStorage.setItem(TOKEN_KEY, result.token)
      await restore(true)
      if (!loggedIn.value) throw new Error('注册成功，但登录状态恢复失败，请使用新账号登录')
    } else await login({ account: body.account, password: body.password })
    loginOpen.value = !loggedIn.value
  }

  function requireLogin() {
    if (loggedIn.value) return true
    loginOpen.value = true
    return false
  }

  async function refreshUnread() {
    if (!loggedIn.value) {
      unread.value = 0
      return
    }
    const current = generation
    try {
      const result = await messageService.getUnreadCount()
      if (current === generation) unread.value = result.data || 0
    } catch {
      if (current === generation) unread.value = 0
    }
  }

  return {
    user,
    ready,
    loginOpen,
    unread,
    loggedIn,
    isAdmin,
    logout,
    restore,
    login,
    register,
    requireLogin,
    refreshUnread,
  }
})
