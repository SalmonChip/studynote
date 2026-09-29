<script setup lang="ts">
import { computed, reactive, ref } from 'vue'
import dayjs from 'dayjs'
import { adminCourseService, adminSeckillService } from '@/domain/seckill/service/seckillService'
import type { SeckillActivityVO } from '@/domain/seckill/types/types'
import { useResource } from '@/composables/useResource'
import { useTask } from '@/composables/useTask'
import ResourceState from '@/components/ResourceState.vue'

const { data, loading, error, refresh } = useResource(() =>
  adminSeckillService.getActivitiesService(),
)
const activities = computed(() => data.value?.data || [])

// 一次拉全量课程，建内存索引
const { data: courseData } = useResource(() => adminCourseService.getCoursesService())
const courseOptions = computed(
  () => courseData.value?.data.map((c) => ({ label: c.title, value: c.courseId })) || [],
)
const courseMap = computed(
  () => new Map((courseData.value?.data || []).map((c) => [c.courseId, c.title])),
)
function titleOf(courseId: number) {
  return courseMap.value.get(courseId) ?? `课程 #${courseId}`
}

const form = reactive({
  activityId: 0,
  courseId: undefined as number | undefined,
  seckillPrice: 0,
  stock: 1,
  // value-format 后是字符串，格式与后端一致
  startTime: '',
  endTime: '',
  status: 1,
})
const open = ref(false)
const { busy, run } = useTask()

const columns = [
  { title: 'ID', dataIndex: 'activityId', width: 70 },
  { title: '课程', key: 'course', width: 180 },
  { title: '秒杀价', key: 'seckillPrice', width: 110 },
  { title: '库存', dataIndex: 'stock', width: 80 },
  { title: '开始', key: 'startTime', width: 150 },
  { title: '结束', key: 'endTime', width: 150 },
  { title: '状态', key: 'status', width: 90 },
  { title: '操作', key: 'actions', width: 220 },
]

/** dayjs 格式化时间 */
function formatTime(value: string) {
  return value ? dayjs(value).format('YYYY-MM-DD HH:mm') : '-'
}

function create() {
  Object.assign(form, {
    activityId: 0,
    courseId: undefined,
    seckillPrice: 0,
    stock: 1,
    startTime: '',
    endTime: '',
    status: 1,
  })
  open.value = true
}

function edit(item: SeckillActivityVO) {
  Object.assign(form, {
    activityId: item.activityId,
    courseId: item.courseId,
    seckillPrice: Number(item.seckillPrice),
    stock: item.stock,
    // ISO 转回入参格式
    startTime: item.startTime ? dayjs(item.startTime).format('YYYY-MM-DD HH:mm:ss') : '',
    endTime: item.endTime ? dayjs(item.endTime).format('YYYY-MM-DD HH:mm:ss') : '',
    status: item.status,
  })
  open.value = true
}

async function save() {
  await run(async () => {
    if (!form.courseId) throw new Error('请选择课程')
    if (form.seckillPrice < 0) throw new Error('秒杀价不能为负')
    if (form.stock < 1) throw new Error('库存必须大于 0')
    if (!form.startTime || !form.endTime) throw new Error('请选择开始和结束时间')
    // 前端先拦结束时间
    if (!dayjs(form.endTime).isAfter(dayjs(form.startTime)))
      throw new Error('结束时间必须晚于开始时间')
    const body = {
      courseId: form.courseId,
      seckillPrice: form.seckillPrice,
      stock: form.stock,
      startTime: form.startTime,
      endTime: form.endTime,
      // status 必须显式传
      status: form.status,
    }
    if (form.activityId) await adminSeckillService.updateActivityService(form.activityId, body)
    else await adminSeckillService.createActivityService(body)
    open.value = false
    await refresh()
  }, '活动已保存')
}

async function warmUp(activityId: number) {
  await run(async () => {
    await adminSeckillService.warmUpService(activityId)
  }, '预热完成，Redis 库存已就绪')
}

async function remove(activityId: number) {
  await run(async () => {
    await adminSeckillService.deleteActivityService(activityId)
    await refresh()
  }, '活动已删除')
}
</script>

<template>
  <section class="panel">
    <div class="between">
      <h1>秒杀管理</h1>
      <a-button type="primary" @click="create">上架秒杀活动</a-button>
    </div>
    <p class="muted">
      活动建好并上架后，记得点一次「预热」—— 它把库存和已下单用户写进 Redis，
      不预热的话开抢时谁也抢不到。
    </p>
    <ResourceState :loading="loading" :error="error" :empty="!activities.length" @retry="refresh">
      <a-table
        :columns="columns"
        :data-source="activities"
        row-key="activityId"
        :pagination="false"
      >
        <template #bodyCell="{ column, record }">
          <span v-if="column.key === 'course'">{{ titleOf(record.courseId) }}</span>
          <span v-else-if="column.key === 'seckillPrice'">
            ¥{{ Number(record.seckillPrice).toFixed(2) }}
          </span>
          <span v-else-if="column.key === 'startTime'">{{ formatTime(record.startTime) }}</span>
          <span v-else-if="column.key === 'endTime'">{{ formatTime(record.endTime) }}</span>
          <a-tag
            v-else-if="column.key === 'status'"
            :color="record.status === 1 ? 'green' : 'default'"
          >
            {{ record.status === 1 ? '上架' : '下架' }}
          </a-tag>
          <a-space v-else-if="column.key === 'actions'">
            <a-button size="small" :disabled="busy" @click="warmUp(record.activityId)">
              预热
            </a-button>
            <a-button size="small" @click="edit(record)">编辑</a-button>
            <a-popconfirm
              title="删除活动？Redis 里预热过的 key 不会自动清理。"
              @confirm="remove(record.activityId)"
            >
              <a-button danger size="small" :disabled="busy">删除</a-button>
            </a-popconfirm>
          </a-space>
        </template>
      </a-table>
    </ResourceState>
    <a-modal
      v-model:open="open"
      :title="form.activityId ? '编辑秒杀活动' : '上架秒杀活动'"
      :confirm-loading="busy"
      @ok="save"
    >
      <div class="stack">
        <label>
          课程
          <a-select
            v-model:value="form.courseId"
            style="width: 100%"
            placeholder="选择要秒杀的课程"
            :options="courseOptions"
          />
        </label>
        <label>
          秒杀价（元）
          <a-input-number
            v-model:value="form.seckillPrice"
            :min="0"
            :precision="2"
            style="width: 100%"
          />
        </label>
        <label>
          库存
          <a-input-number v-model:value="form.stock" :min="1" style="width: 100%" />
        </label>
        <label>
          开始时间
          <a-date-picker
            v-model:value="form.startTime"
            show-time
            value-format="YYYY-MM-DD HH:mm:ss"
            style="width: 100%"
          />
        </label>
        <label>
          结束时间
          <a-date-picker
            v-model:value="form.endTime"
            show-time
            value-format="YYYY-MM-DD HH:mm:ss"
            style="width: 100%"
          />
        </label>
        <label>
          状态
          <a-select
            v-model:value="form.status"
            style="width: 100%"
            :options="[
              { label: '上架', value: 1 },
              { label: '下架', value: 0 },
            ]"
          />
        </label>
      </div>
    </a-modal>
  </section>
</template>
