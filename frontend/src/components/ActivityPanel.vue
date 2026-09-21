<script setup lang="ts">
import { computed } from 'vue'
import dayjs from 'dayjs'
import { noteService } from '@/domain/note/service/noteService'
import { useResource } from '@/composables/useResource'
import ResourceState from './ResourceState.vue'
const { data, loading, error, refresh } = useResource(async () => {
  const [heat, top] = await Promise.all([
    noteService.getHeatMapService(),
    noteService.getTop3CountService(),
  ])
  return { heat: heat.data, top: top.data }
})
const cells = computed(() => {
  const counts = new Map(
    data.value?.heat.map((item) => [dayjs(item.date).format('YYYY-MM-DD'), item.count]) || [],
  )
  return Array.from({ length: 182 }, (_, i) => {
    const date = dayjs()
      .subtract(181 - i, 'day')
      .format('YYYY-MM-DD')
    return { date, count: counts.get(date) || 0 }
  })
})
</script>
<template>
  <section class="panel">
    <h2>学习记录</h2>
    <ResourceState :loading="loading" :error="error" @retry="refresh">
      <p>本月前三：{{ data?.top.thisMonthTop3Count || 0 }} 次</p>
      <p>上月前三：{{ data?.top.lastMonthTop3Count || 0 }} 次</p>
      <div class="heatmap" aria-label="最近半年笔记提交热力图">
        <span
          v-for="cell in cells"
          :key="cell.date"
          class="heat-cell"
          :class="`heat-${Math.min(cell.count, 4)}`"
          :title="`${cell.date}：${cell.count} 篇笔记`"
        />
      </div>
    </ResourceState>
  </section>
</template>
