<script setup lang="ts">
import { computed } from 'vue'
import dayjs from 'dayjs'
import { userSeckillService } from '@/domain/seckill/service/seckillService'
import { SeckillOrderStatus } from '@/domain/seckill/types/types'
import type { SeckillOrderVO } from '@/domain/seckill/types/types'
import { useResource } from '@/composables/useResource'
import { useTask } from '@/composables/useTask'
import ResourceState from '@/components/ResourceState.vue'

const { data, loading, error, refresh } = useResource(() => userSeckillService.getOrdersService())
const orders = computed(() => data.value?.data || [])
const { busy, run } = useTask()

const STATUS_TEXT: Record<number, string> = {
  [SeckillOrderStatus.PENDING]: '待支付',
  [SeckillOrderStatus.PAID]: '已支付',
  [SeckillOrderStatus.CANCELLED]: '已取消',
}
const STATUS_COLOR: Record<number, string> = {
  [SeckillOrderStatus.PENDING]: 'orange',
  [SeckillOrderStatus.PAID]: 'green',
  [SeckillOrderStatus.CANCELLED]: 'default',
}

function formatTime(value: string | null) {
  return value ? dayjs(value).format('YYYY-MM-DD HH:mm:ss') : '-'
}

async function pay(order: SeckillOrderVO) {
  const ok = await run(async () => {
    await userSeckillService.payService(order.orderId)
  }, '支付成功，课程已发放')
  // 支付后刷新，订单变绿
  if (ok) await refresh()
}
</script>

<template>
  <section class="panel page-narrow">
    <div class="between">
      <h1>我的秒杀订单</h1>
      <RouterLink to="/seckill">
        <a-button>去秒杀</a-button>
      </RouterLink>
    </div>
    <p class="muted">
      抢购成功后订单是
      <b>异步落库</b>
      的，这里可能要等一两秒才出现 —— 刷新一下即可。拿到待支付的订单就能付款了。
    </p>
    <ResourceState :loading="loading" :error="error" :empty="!orders.length" @retry="refresh">
      <div v-for="order in orders" :key="order.orderId" class="collection-row">
        <div>
          <h3>
            {{ order.courseTitle || `课程 #${order.courseId}` }}
            <a-tag :color="STATUS_COLOR[order.status]">{{ STATUS_TEXT[order.status] }}</a-tag>
          </h3>
          <p class="muted">
            订单 #{{ order.orderId }} · 成交价 ¥{{ Number(order.price).toFixed(2) }}
          </p>
          <p class="muted">下单 {{ formatTime(order.createdAt) }}</p>
          <p v-if="order.paidAt" class="muted">支付 {{ formatTime(order.paidAt) }}</p>
          <p v-if="order.cancelledAt" class="muted">取消 {{ formatTime(order.cancelledAt) }}</p>
        </div>
        <a-button
          v-if="order.status === SeckillOrderStatus.PENDING"
          type="primary"
          :loading="busy"
          @click="pay(order)"
        >
          支付 ¥{{ Number(order.price).toFixed(2) }}
        </a-button>
        <RouterLink v-else-if="order.status === SeckillOrderStatus.PAID" to="/my-courses">
          <a-button>去学习</a-button>
        </RouterLink>
      </div>
    </ResourceState>
    <p class="muted">
      订单长时间不支付会被定时任务自动取消，库存同时归还到 Redis ——
      所以看到「已取消」就是超时了，重新抢一次即可。
    </p>
  </section>
</template>
