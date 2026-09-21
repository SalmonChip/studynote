<script setup lang="ts">
import { reactive, ref } from 'vue'
import { adminUserService } from '@/domain/user/service/userService'
import type { UserListQueryParams } from '@/domain/user/types/serviceTypes'
import { useResource } from '@/composables/useResource'
import ResourceState from '@/components/ResourceState.vue'
const draft = reactive({
  userId: '',
  username: '',
  account: '',
  isAdmin: undefined as number | undefined,
  isBanned: undefined as number | undefined,
})
const query = ref<UserListQueryParams>({ page: 1, pageSize: 10 })
const { data, loading, error, refresh } = useResource(
  () => adminUserService.getUserListService(query.value),
  [query],
)
function search() {
  query.value = { ...draft, page: 1, pageSize: query.value.pageSize }
}
function reset() {
  Object.assign(draft, {
    userId: '',
    username: '',
    account: '',
    isAdmin: undefined,
    isBanned: undefined,
  })
  search()
}
const columns = [
  { title: '用户 ID', dataIndex: 'userId' },
  { title: '账号', dataIndex: 'account' },
  { title: '昵称', key: 'username' },
  { title: '管理员', key: 'admin' },
  { title: '状态', key: 'banned' },
]
</script>
<template>
  <section class="panel">
    <h1>用户管理</h1>
    <form class="toolbar" @submit.prevent="search">
      <a-input v-model:value="draft.userId" placeholder="用户 ID" />
      <a-input v-model:value="draft.account" placeholder="账号" />
      <a-input v-model:value="draft.username" placeholder="昵称" />
      <a-select
        v-model:value="draft.isAdmin"
        placeholder="管理员"
        allow-clear
        :options="[
          { label: '是', value: 1 },
          { label: '否', value: 0 },
        ]"
      />
      <a-select
        v-model:value="draft.isBanned"
        placeholder="封禁状态"
        allow-clear
        :options="[
          { label: '正常', value: 0 },
          { label: '已封禁', value: 1 },
        ]"
      />
      <a-button html-type="submit" type="primary">筛选</a-button>
      <a-button @click="reset">重置</a-button>
    </form>
    <ResourceState :loading="loading" :error="error" :empty="!data?.data.length" @retry="refresh">
      <a-table
        :columns="columns"
        :data-source="data?.data || []"
        :pagination="false"
        row-key="userId"
      >
        <template #bodyCell="{ column, record }">
          <RouterLink v-if="column.key === 'username'" :to="`/user/${record.userId}`">
            {{ record.username }}
          </RouterLink>
          <a-tag v-else-if="column.key === 'admin'" :color="record.isAdmin ? 'blue' : 'default'">
            {{ record.isAdmin ? '是' : '否' }}
          </a-tag>
          <a-tag v-else-if="column.key === 'banned'" :color="record.isBanned ? 'red' : 'green'">
            {{ record.isBanned ? '已封禁' : '正常' }}
          </a-tag>
        </template>
      </a-table>
    </ResourceState>
    <div v-if="data?.pagination" class="pagination">
      <a-pagination
        :current="query.page"
        :page-size="query.pageSize"
        :total="data.pagination.total"
        show-size-changer
        @change="(page: number, pageSize: number) => (query = { ...query, page, pageSize })"
      />
    </div>
  </section>
</template>
