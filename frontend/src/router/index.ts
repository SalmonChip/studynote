import { createRouter, createWebHistory } from 'vue-router'
import { useSession } from '@/stores/session'

export const router = createRouter({
  history: createWebHistory(),
  scrollBehavior(to, from, saved) {
    return saved || (to.path === from.path ? undefined : { top: 0 })
  },
  routes: [
    { path: '/', alias: '/home', component: () => import('@/pages/HomePage.vue') },
    { path: '/question-set', component: () => import('@/pages/QuestionSetPage.vue') },
    { path: '/question-list', component: () => import('@/pages/QuestionListsPage.vue') },
    { path: '/questions/:questionId(\\d+)', component: () => import('@/pages/QuestionPage.vue') },
    { path: '/user/:userId(\\d+)', component: () => import('@/pages/UserHomePage.vue') },
    { path: '/user-center', redirect: '/user-center/info' },
    {
      path: '/user-center/:section(info|collect|note|course)',
      component: () => import('@/pages/UserCenterPage.vue'),
      meta: { auth: true },
    },
    {
      path: '/messages',
      component: () => import('@/pages/MessagesPage.vue'),
      meta: { auth: true },
    },
    // 浏览秒杀活动不需要登录，后端那个接口也没标 @NeedLogin
    { path: '/seckill', component: () => import('@/pages/SeckillPage.vue') },
    {
      path: '/seckill/orders',
      component: () => import('@/pages/SeckillOrdersPage.vue'),
      meta: { auth: true },
    },
    {
      path: '/my-courses',
      component: () => import('@/pages/MyCoursesPage.vue'),
      meta: { auth: true },
    },
    { path: '/login', redirect: (to) => ({ path: '/', query: { ...to.query, login: '1' } }) },
    {
      path: '/admin',
      component: () => import('@/pages/admin/DashboardPage.vue'),
      meta: { admin: true },
    },
    {
      path: '/admin/user',
      component: () => import('@/pages/admin/UsersPage.vue'),
      meta: { admin: true },
    },
    {
      path: '/admin/category',
      component: () => import('@/pages/admin/CategoriesPage.vue'),
      meta: { admin: true },
    },
    {
      path: '/admin/question',
      component: () => import('@/pages/admin/QuestionsPage.vue'),
      meta: { admin: true },
    },
    {
      path: '/admin/question-list',
      component: () => import('@/pages/admin/QuestionListsAdminPage.vue'),
      meta: { admin: true },
    },
    {
      path: '/admin/question-list/:questionListId(\\d+)',
      component: () => import('@/pages/admin/QuestionListDetailPage.vue'),
      meta: { admin: true },
    },
    {
      path: '/admin/course',
      component: () => import('@/pages/admin/CoursesAdminPage.vue'),
      meta: { admin: true },
    },
    {
      path: '/admin/seckill-activity',
      component: () => import('@/pages/admin/SeckillActivitiesAdminPage.vue'),
      meta: { admin: true },
    },
    {
      path: '/forbidden',
      component: () => import('@/pages/StatusPage.vue'),
      props: { forbidden: true },
    },
    { path: '/:pathMatch(.*)*', component: () => import('@/pages/StatusPage.vue') },
  ],
})

router.beforeEach(async (to) => {
  const session = useSession()
  await session.restore()
  if ((to.meta.auth || to.meta.admin) && !session.loggedIn) {
    session.loginOpen = true
    return { path: '/', query: { redirect: to.fullPath, login: '1' } }
  }
  if (to.meta.admin && !session.isAdmin) return '/forbidden'
})
