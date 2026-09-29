<script setup lang="ts">
import { computed } from 'vue'
import dayjs from 'dayjs'
import { userCourseService } from '@/domain/seckill/service/seckillService'
import { UserCourseSource } from '@/domain/seckill/types/types'
import { useResource } from '@/composables/useResource'
import ResourceState from './ResourceState.vue'

const { data, loading, error, refresh } = useResource(() => userCourseService.getMyCoursesService())
const courses = computed(() => data.value?.data || [])

const SOURCE_TEXT: Record<number, string> = {
  [UserCourseSource.SECKILL]: '秒杀获得',
  [UserCourseSource.PURCHASE]: '购买',
  [UserCourseSource.GRANT]: '后台发放',
}

// dayjs 转本地时区
function formatTime(value: string) {
  return value ? dayjs(value).format('YYYY-MM-DD HH:mm') : '-'
}
</script>

<template>
  <div class="between">
    <h2>我的课程</h2>
    <RouterLink to="/seckill">
      <a-button>去秒杀</a-button>
    </RouterLink>
  </div>
  <ResourceState :loading="loading" :error="error" :empty="!courses.length" @retry="refresh">
    <div v-for="course in courses" :key="course.courseId" class="collection-row">
      <div>
        <h3>
          {{ course.title }}
          <a-tag color="blue">{{ SOURCE_TEXT[course.source] || '未知来源' }}</a-tag>
        </h3>
        <p v-if="course.description" class="muted">{{ course.description }}</p>
        <p class="muted">
          ¥{{ Number(course.price).toFixed(2) }} · 获得于 {{ formatTime(course.createTime) }}
        </p>
      </div>
    </div>
  </ResourceState>
  <p class="muted">课程是通过「我的秒杀订单」支付成功后自动发放的。</p>
</template>
