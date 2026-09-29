<script setup lang="ts">
import { computed, onUnmounted, ref } from 'vue'
import dayjs from 'dayjs'
import { userSeckillService } from '@/domain/seckill/service/seckillService'
import type { SeckillActivityDetailVO } from '@/domain/seckill/types/types'
import { useResource } from '@/composables/useResource'
import { useTask } from '@/composables/useTask'
import { useSession } from '@/stores/session'
import ResourceState from '@/components/ResourceState.vue'

const session = useSession()
const { data, loading, error, refresh } = useResource(() =>
  userSeckillService.getActivitiesService(),
)
const activities = computed(() => data.value?.data || [])
const { busy, run } = useTask()

// now 响应式，倒计时才会走
const now = ref(Date.now())
const timer = window.setInterval(() => (now.value = Date.now()), 1000)
onUnmounted(() => clearInterval(timer))

type Phase = 'upcoming' | 'active' | 'ended'

// 三种阶段都要处理
function phaseOf(item: SeckillActivityDetailVO): Phase {
  if (now.value < dayjs(item.startTime).valueOf()) return 'upcoming'
  if (now.value < dayjs(item.endTime).valueOf()) return 'active'
  return 'ended'
}

const PHASE_TEXT: Record<Phase, string> = {
  upcoming: '即将开始',
  active: '进行中',
  ended: '已结束',
}
const PHASE_COLOR: Record<Phase, string> = {
  upcoming: 'blue',
  active: 'red',
  ended: 'default',
}

function countdown(item: SeckillActivityDetailVO) {
  const diff = dayjs(item.startTime).valueOf() - now.value
  if (diff <= 0) return ''
  const h = Math.floor(diff / 3600000)
  const m = Math.floor((diff % 3600000) / 60000)
  const s = Math.floor((diff % 60000) / 1000)
  return `${h} 时 ${m} 分 ${s} 秒`
}

function timeText(value: string) {
  return dayjs(value).format('MM-DD HH:mm')
}

async function grab(item: SeckillActivityDetailVO) {
  // 未登录弹登录框
  if (!session.requireLogin()) return
  const ok = await run(async () => {
    await userSeckillService.seckillService(item.activityId)
  }, '抢购成功！订单正在异步落库，稍等 1~2 秒去「我的订单」完成支付')
  if (ok) await refresh()
}
</script>

<template>
  <section class="panel page-narrow">
    <div class="between">
      <h1>限时秒杀</h1>
      <RouterLink to="/seckill/orders">
        <a-button>我的订单</a-button>
      </RouterLink>
    </div>
    <ResourceState :loading="loading" :error="error" :empty="!activities.length" @retry="refresh">
      <div v-for="item in activities" :key="item.activityId" class="collection-row">
        <div>
          <h3>
            {{ item.courseTitle }}
            <a-tag :color="PHASE_COLOR[phaseOf(item)]">{{ PHASE_TEXT[phaseOf(item)] }}</a-tag>
          </h3>
          <p class="muted">
            原价
            <s>¥{{ Number(item.coursePrice).toFixed(2) }}</s>
            &nbsp;
            <strong>秒杀价 ¥{{ Number(item.seckillPrice).toFixed(2) }}</strong>
          </p>
          <p class="muted">
            {{ timeText(item.startTime) }} ~ {{ timeText(item.endTime) }} · 库存 {{ item.stock }} 件
          </p>
          <p v-if="phaseOf(item) === 'upcoming'" class="muted">距开始还有 {{ countdown(item) }}</p>
        </div>
        <a-button
          type="primary"
          danger
          :disabled="busy || phaseOf(item) !== 'active'"
          @click="grab(item)"
        >
          {{ phaseOf(item) === 'active' ? '立即抢购' : PHASE_TEXT[phaseOf(item)] }}
        </a-button>
      </div>
    </ResourceState>
    <a-alert
      v-if="!session.loggedIn"
      type="info"
      show-icon
      message="可以先看看有什么活动；抢购需要先登录。"
      class="error-space"
    />
    <p class="muted">
      提示：这里的「库存」是活动创建时写进 MySQL 的总量，不是实时剩余量 —— 真正的剩余库存在 Redis
      里，前端拿不到。
    </p>
  </section>
</template>
