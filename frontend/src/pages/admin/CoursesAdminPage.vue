<script setup lang="ts">
import { computed, reactive, ref } from 'vue'
import { adminCourseService } from '@/domain/seckill/service/seckillService'
import type { CourseVO } from '@/domain/seckill/types/types'
import { useResource } from '@/composables/useResource'
import { useTask } from '@/composables/useTask'
import ResourceState from '@/components/ResourceState.vue'

const { data, loading, error, refresh } = useResource(() => adminCourseService.getCoursesService())
const courses = computed(() => data.value?.data || [])

/** 新建/编辑共用表单 */
const form = reactive({
  courseId: 0,
  title: '',
  description: '',
  coverUrl: '',
  price: 0,
  status: 1,
})
const open = ref(false)
const { busy, run } = useTask()

const columns = [
  { title: 'ID', dataIndex: 'courseId', width: 80 },
  { title: '标题', dataIndex: 'title' },
  { title: '价格', key: 'price', width: 120 },
  { title: '状态', key: 'status', width: 100 },
  { title: '操作', key: 'actions', width: 160 },
]

function create() {
  Object.assign(form, {
    courseId: 0,
    title: '',
    description: '',
    coverUrl: '',
    price: 0,
    status: 1,
  })
  open.value = true
}

function edit(item: CourseVO) {
  // null 兜底成空串
  Object.assign(form, {
    courseId: item.courseId,
    title: item.title,
    description: item.description ?? '',
    coverUrl: item.coverUrl ?? '',
    price: Number(item.price),
    status: item.status,
  })
  open.value = true
}

async function save() {
  await run(async () => {
    if (!form.title.trim()) throw new Error('请输入课程标题')
    if (form.price < 0) throw new Error('价格不能为负')
    const body = {
      title: form.title.trim(),
      description: form.description,
      coverUrl: form.coverUrl,
      price: form.price,
      // status 必须显式传
      status: form.status,
    }
    if (form.courseId) await adminCourseService.updateCourseService(form.courseId, body)
    else await adminCourseService.createCourseService(body)
    open.value = false
    await refresh()
  }, '课程已保存')
}

async function remove(courseId: number) {
  await run(async () => {
    await adminCourseService.deleteCourseService(courseId)
    await refresh()
  }, '课程已删除')
}
</script>

<template>
  <section class="panel">
    <div class="between">
      <h1>课程管理</h1>
      <a-button type="primary" @click="create">上架课程</a-button>
    </div>
    <ResourceState :loading="loading" :error="error" :empty="!courses.length" @retry="refresh">
      <a-table :columns="columns" :data-source="courses" row-key="courseId" :pagination="false">
        <template #bodyCell="{ column, record }">
          <span v-if="column.key === 'price'">¥{{ Number(record.price).toFixed(2) }}</span>
          <a-tag
            v-else-if="column.key === 'status'"
            :color="record.status === 1 ? 'green' : 'default'"
          >
            {{ record.status === 1 ? '上架' : '下架' }}
          </a-tag>
          <a-space v-else-if="column.key === 'actions'">
            <a-button size="small" @click="edit(record)">编辑</a-button>
            <a-popconfirm
              title="删除课程？若已有秒杀活动引用它，那条活动会变成悬空引用。"
              @confirm="remove(record.courseId)"
            >
              <a-button danger size="small" :disabled="busy">删除</a-button>
            </a-popconfirm>
          </a-space>
        </template>
      </a-table>
    </ResourceState>
    <a-modal
      v-model:open="open"
      :title="form.courseId ? '编辑课程' : '上架课程'"
      :confirm-loading="busy"
      @ok="save"
    >
      <div class="stack">
        <label>
          标题
          <a-input v-model:value="form.title" :maxlength="128" />
        </label>
        <label>
          价格（元）
          <a-input-number v-model:value="form.price" :min="0" :precision="2" style="width: 100%" />
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
        <label>
          封面图 URL
          <a-input v-model:value="form.coverUrl" placeholder="https://..." />
        </label>
        <label>
          描述
          <a-textarea v-model:value="form.description" :rows="4" />
        </label>
      </div>
    </a-modal>
  </section>
</template>
