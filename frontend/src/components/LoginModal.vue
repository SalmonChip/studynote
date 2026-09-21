<script setup lang="ts">
import { onUnmounted, reactive, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { message } from 'ant-design-vue'
import { useSession } from '@/stores/session'
import { userService } from '@/domain/user/service/userService'
import { useTask } from '@/composables/useTask'
const session = useSession()
const route = useRoute()
const router = useRouter()
const mode = ref('login')
const form = reactive({
  identity: '',
  account: '',
  username: '',
  email: '',
  password: '',
  verifyCode: '',
})
const { busy, run } = useTask()
const sending = useTask()
const remaining = ref(0)
let interval: number | undefined
onUnmounted(() => clearInterval(interval))
watch(
  () => session.loginOpen,
  (open) => {
    if (!open) form.password = ''
  },
)
async function sendCode() {
  if (remaining.value || sending.busy.value) return
  if (!/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(form.email)) {
    message.warning('请输入有效邮箱')
    return
  }
  await sending.run(async () => {
    await userService.sendVerifyCode({ email: form.email, type: 'REGISTER' })
    remaining.value = 60
    interval = window.setInterval(() => {
      if (--remaining.value <= 0) clearInterval(interval)
    }, 1000)
  }, '验证码已发送')
}
async function submit() {
  await run(
    async () => {
      if (!form.password) throw new Error('请输入密码')
      if (mode.value === 'login') {
        const identity = form.identity.trim()
        if (!identity) throw new Error('请输入账号或邮箱')
        await session.login({
          ...(identity.includes('@') ? { email: identity } : { account: identity }),
          password: form.password,
        })
      } else {
        if (!/^[a-zA-Z0-9_]{6,32}$/.test(form.account))
          throw new Error('账号须为 6–32 位字母、数字或下划线')
        if (!/^[\u4e00-\u9fa5_a-zA-Z0-9.-]{1,16}$/.test(form.username))
          throw new Error('昵称格式不正确，长度应为 1–16 位')
        if (form.password.length < 6 || form.password.length > 32)
          throw new Error('密码长度应为 6–32 位')
        if (!/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(form.email) || form.verifyCode.length !== 6)
          throw new Error('请输入有效邮箱和 6 位验证码')
        await session.register({
          account: form.account,
          username: form.username,
          email: form.email,
          password: form.password,
          verifyCode: form.verifyCode,
        })
      }
      if (session.loggedIn) {
        const redirect = route.query.redirect
        await router.replace(
          typeof redirect === 'string' && redirect.startsWith('/') && !redirect.startsWith('//')
            ? redirect
            : {
                path: route.path,
                query: { ...route.query, login: undefined, redirect: undefined },
              },
        )
      }
    },
    mode.value === 'login' ? '登录成功' : '注册成功',
  )
}
</script>

<template>
  <a-modal
    v-model:open="session.loginOpen"
    title="欢迎来到拾题社区"
    :footer="null"
    :mask-closable="!busy"
  >
    <a-segmented
      v-model:value="mode"
      block
      :options="[
        { label: '登录', value: 'login' },
        { label: '注册', value: 'register' },
      ]"
    />
    <form class="stack auth-form" @submit.prevent="submit">
      <label v-if="mode === 'login'">
        账号或邮箱
        <a-input v-model:value="form.identity" autocomplete="username" />
      </label>
      <template v-else>
        <label>
          账号
          <a-input v-model:value="form.account" autocomplete="username" :maxlength="32" />
        </label>
        <label>
          昵称
          <a-input v-model:value="form.username" :maxlength="16" />
        </label>
        <label>
          邮箱
          <a-input v-model:value="form.email" type="email" autocomplete="email" />
        </label>
        <label>
          验证码
          <div class="toolbar">
            <a-input v-model:value="form.verifyCode" :maxlength="6" />
            <a-button :disabled="remaining > 0" :loading="sending.busy.value" @click="sendCode">
              {{ remaining ? `${remaining} 秒` : '发送验证码' }}
            </a-button>
          </div>
        </label>
      </template>
      <label>
        密码
        <a-input-password
          v-model:value="form.password"
          :autocomplete="mode === 'login' ? 'current-password' : 'new-password'"
        />
      </label>
      <a-button type="primary" html-type="submit" :loading="busy" block>
        {{ mode === 'login' ? '登录' : '注册' }}
      </a-button>
    </form>
  </a-modal>
</template>
