<script setup lang="ts">
import { computed, onUnmounted, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import zhCN from 'ant-design-vue/es/locale/zh_CN'
import { useSession } from '@/stores/session'
import LoginModal from '@/components/LoginModal.vue'
import SearchModal from '@/components/SearchModal.vue'
const route = useRoute()
const router = useRouter()
const session = useSession()
const smoke = import.meta.env.MODE === 'smoke'
const admin = computed(() => route.path.startsWith('/admin') && session.isAdmin)
const links = [
  ['/admin', '仪表盘'],
  ['/admin/user', '用户管理'],
  ['/admin/category', '分类管理'],
  ['/admin/question', '题目管理'],
  ['/admin/question-list', '题单管理'],
  ['/admin/course', '课程管理'],
  ['/admin/seckill-activity', '秒杀管理'],
]
watch(
  () => route.query.login,
  (value) => {
    if (value && !session.loggedIn) session.loginOpen = true
  },
  { immediate: true },
)
watch(
  () => session.loggedIn,
  (loggedIn) => {
    void session.refreshUnread()
    if (!loggedIn && (route.meta.auth || route.meta.admin)) void router.replace('/')
  },
  { immediate: true },
)
const timer = window.setInterval(() => {
  if (document.visibilityState === 'visible') void session.refreshUnread()
}, 60000)
onUnmounted(() => clearInterval(timer))
function logout() {
  session.logout()
  void router.push('/')
}
</script>

<template>
  <a-config-provider
    :locale="zhCN"
    :theme="{ token: { colorPrimary: '#1677ff', borderRadius: 8 } }"
  >
    <header class="site-header">
      <div class="header-inner">
        <RouterLink class="brand" to="/">拾题社区</RouterLink>
        <nav class="main-nav" aria-label="主导航">
          <RouterLink to="/home">首页</RouterLink>
          <RouterLink to="/question-set">题库</RouterLink>
          <RouterLink to="/question-list">题单</RouterLink>
          <RouterLink to="/seckill">秒杀</RouterLink>
          <RouterLink v-if="session.loggedIn" to="/my-courses">我的课程</RouterLink>
        </nav>
        <div class="header-actions">
          <SearchModal />
          <template v-if="session.loggedIn">
            <a-badge :count="session.unread"><RouterLink to="/messages">消息</RouterLink></a-badge>
            <a-dropdown :trigger="['click']">
              <button class="avatar-button" aria-label="用户菜单">
                <a-avatar :src="session.user?.avatarUrl">
                  {{ session.user?.username?.slice(0, 1) }}
                </a-avatar>
              </button>
              <template #overlay>
                <a-menu>
                  <a-menu-item key="home">
                    <RouterLink :to="`/user/${session.user?.userId}`">个人主页</RouterLink>
                  </a-menu-item>
                  <a-menu-item key="center">
                    <RouterLink to="/user-center/info">个人中心</RouterLink>
                  </a-menu-item>
                  <a-menu-item key="orders">
                    <RouterLink to="/seckill/orders">我的秒杀订单</RouterLink>
                  </a-menu-item>
                  <a-menu-item key="courses">
                    <RouterLink to="/my-courses">我的课程</RouterLink>
                  </a-menu-item>
                  <a-menu-item v-if="session.isAdmin" key="admin">
                    <RouterLink to="/admin">管理后台</RouterLink>
                  </a-menu-item>
                  <a-menu-item key="logout" @click="logout">退出登录</a-menu-item>
                </a-menu>
              </template>
            </a-dropdown>
          </template>
          <a-button v-else type="primary" @click="session.loginOpen = true">登录 / 注册</a-button>
        </div>
      </div>
    </header>
    <div class="app-body" :class="{ 'admin-body': admin }">
      <aside v-if="admin" class="admin-nav">
        <RouterLink
          v-for="[path, label] in links"
          :key="path"
          :to="path"
          :class="{
            active: route.path === path || (path !== '/admin' && route.path.startsWith(path + '/')),
          }"
        >
          {{ label }}
        </RouterLink>
      </aside>
      <main>
        <a-alert
          v-if="smoke"
          type="info"
          message="本地测试数据，修改仅保存在内存中"
          class="error-space"
        />
        <RouterView />
      </main>
    </div>
    <LoginModal />
  </a-config-provider>
</template>
