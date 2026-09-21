<script setup lang="ts">
import { reactive, ref, watch } from 'vue'
import dayjs from 'dayjs'
import { useSession } from '@/stores/session'
import { userService } from '@/domain/user/service/userService'
import { httpClient } from '@/request'
import { useTask } from '@/composables/useTask'
import type { UserState } from '@/domain/user/types/types'
const session = useSession()
const editing = ref(false)
const form = reactive({
  username: '',
  gender: 3,
  birthday: '',
  email: '',
  school: '',
  signature: '',
})
function reset() {
  const user = session.user
  if (user)
    Object.assign(form, {
      username: user.username || '',
      gender: user.gender || 3,
      birthday: user.birthday || '',
      email: user.email || '',
      school: user.school || '',
      signature: user.signature || '',
    })
}
watch(() => session.user, reset, { immediate: true })
function toggleEditing() {
  editing.value = !editing.value
  reset()
}
const { busy, run } = useTask()
async function save() {
  await run(async () => {
    if (!/^[\u4e00-\u9fa5_a-zA-Z0-9]{1,16}$/.test(form.username))
      throw new Error('昵称须为 1–16 位中文、字母、数字或下划线')
    if (form.email && !/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(form.email))
      throw new Error('邮箱格式不正确')
    if (
      form.birthday &&
      (!dayjs(form.birthday).isValid() || !dayjs(form.birthday).isBefore(dayjs(), 'day'))
    )
      throw new Error('生日应为过去的日期')
    const body = { ...form, birthday: form.birthday || undefined } as Partial<UserState>
    await userService.updateMeService(body)
    if (session.user) session.user = { ...session.user, ...body }
    editing.value = false
  }, '资料已保存')
}
async function upload(event: Event) {
  const input = event.target as HTMLInputElement
  const file = input.files?.[0]
  if (!file) return
  await run(async () => {
    if (!file.type.startsWith('image/') || file.size > 5 * 1024 * 1024)
      throw new Error('请选择不超过 5MB 的图片')
    const body = new FormData()
    body.append('file', file)
    const response = await httpClient.request<{ url: string }>(['POST', '/api/users/avatar'], {
      body,
    })
    await userService.updateMeService({ avatarUrl: response.data.url })
    if (session.user) session.user = { ...session.user, avatarUrl: response.data.url }
  }, '头像已更新')
  input.value = ''
}
</script>
<template>
  <div class="profile-header">
    <a-avatar :size="64" :src="session.user?.avatarUrl">
      {{ session.user?.username?.slice(0, 1) }}
    </a-avatar>
    <div>
      <h2>{{ session.user?.username }}</h2>
      <label class="upload-button">
        更换头像
        <input type="file" accept="image/*" :disabled="busy" @change="upload" />
      </label>
    </div>
    <a-button @click="toggleEditing">{{ editing ? '取消编辑' : '编辑资料' }}</a-button>
  </div>
  <form class="form-grid" @submit.prevent="save">
    <label>
      昵称
      <a-input v-model:value="form.username" :disabled="!editing" :maxlength="16" />
    </label>
    <label>
      性别
      <a-select
        v-model:value="form.gender"
        :disabled="!editing"
        :options="[
          { value: 1, label: '男' },
          { value: 2, label: '女' },
          { value: 3, label: '保密' },
        ]"
      />
    </label>
    <label>
      生日
      <a-date-picker v-model:value="form.birthday" value-format="YYYY-MM-DD" :disabled="!editing" />
    </label>
    <label>
      邮箱
      <a-input v-model:value="form.email" :disabled="!editing" type="email" />
    </label>
    <label class="full">
      学校
      <a-input v-model:value="form.school" :disabled="!editing" :maxlength="64" />
    </label>
    <label class="full">
      个性签名
      <a-textarea v-model:value="form.signature" :disabled="!editing" :rows="3" :maxlength="128" />
    </label>
    <a-button v-if="editing" type="primary" html-type="submit" :loading="busy">保存资料</a-button>
  </form>
</template>
