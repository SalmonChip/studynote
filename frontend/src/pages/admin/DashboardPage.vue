<script setup lang="ts">
import { ref } from 'vue'
import { statisticService } from '@/domain/statistic/service/statisticService'
import { useResource } from '@/composables/useResource'
import ResourceState from '@/components/ResourceState.vue'
const page = ref(1)
const pageSize = ref(10)
const { data, loading, error, refresh } = useResource(
  () => statisticService.getStatisticService({ page: page.value, pageSize: pageSize.value }),
  [page, pageSize],
)
const columns = [
  { title: '日期', dataIndex: 'date' },
  { title: '登录次数', dataIndex: 'loginCount' },
  { title: '新增用户', dataIndex: 'registerCount' },
  { title: '累计用户', dataIndex: 'totalRegisterCount' },
  { title: '当天笔记', dataIndex: 'noteCount' },
  { title: '提交次数', dataIndex: 'submitNoteCount' },
  { title: '累计笔记', dataIndex: 'totalNoteCount' },
]
</script>
<template>
  <section class="panel">
    <h1>数据统计</h1>
    <ResourceState :loading="loading" :error="error" :empty="!data?.data.length" @retry="refresh">
      <a-table
        :columns="columns"
        :data-source="data?.data || []"
        :pagination="false"
        row-key="id"
      />
    </ResourceState>
    <div v-if="data?.pagination" class="pagination">
      <a-pagination
        v-model:current="page"
        v-model:page-size="pageSize"
        :total="data.pagination.total"
        show-size-changer
      />
    </div>
  </section>
</template>
