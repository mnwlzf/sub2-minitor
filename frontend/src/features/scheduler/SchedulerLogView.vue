<template>
  <AppShell
    title="定时任务 / 日志查看"
    subtitle="按时间范围查看任务执行结果，支持关键字筛选和拖拽横向浏览表格。"
  >
    <el-card shadow="never" class="log-filter-card">
      <template #header>
        <div class="log-panel-head">
          <span>执行日志</span>
          <div class="log-toolbar">
            <el-input
              v-model="filters.keyword"
              clearable
              placeholder="搜索任务名称 / 分组 / 结果"
              class="log-search"
              @keyup.enter="loadLogs"
            />
            <el-select v-model="filters.success" clearable placeholder="执行结果" class="log-status-filter">
              <el-option label="成功" :value="true" />
              <el-option label="失败" :value="false" />
            </el-select>
            <el-radio-group v-model="filters.rangeMode" @change="handleRangeModeChange">
              <el-radio-button label="today">今天</el-radio-button>
              <el-radio-button label="threeDays">三天</el-radio-button>
              <el-radio-button label="sevenDays">七天</el-radio-button>
              <el-radio-button label="custom">自定义</el-radio-button>
            </el-radio-group>
            <el-date-picker
              v-model="filters.dateRange"
              type="daterange"
              value-format="YYYY-MM-DD"
              range-separator="至"
              start-placeholder="开始日期"
              end-placeholder="结束日期"
              class="log-date-picker"
              @change="handleDateRangeChange"
            />
            <el-button type="primary" @click="loadLogs">搜索</el-button>
          </div>
        </div>
      </template>

      <div ref="tableScrollRef" class="log-table-scroll" @mousedown="startDragScroll">
        <el-table :data="logs" v-loading="loading" class="log-table" :row-class-name="rowClassName">
          <el-table-column prop="taskName" label="任务名称" min-width="180" show-overflow-tooltip />
          <el-table-column prop="taskGroup" label="分组" width="120" />
          <el-table-column prop="taskType" label="类型" width="140">
            <template #default="{ row }">
              {{ taskTypeLabel(row.taskType) }}
            </template>
          </el-table-column>
          <el-table-column prop="baseUrl" label="目标地址" min-width="260" show-overflow-tooltip />
          <el-table-column label="结果" width="100">
            <template #default="{ row }">
              <el-tag :type="row.success === 1 ? 'success' : 'danger'" size="small">
                {{ row.success === 1 ? '成功' : '失败' }}
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column prop="startedAt" label="开始时间" min-width="180" />
          <el-table-column prop="finishedAt" label="结束时间" min-width="180" />
          <el-table-column prop="durationMs" label="耗时" width="110">
            <template #default="{ row }">
              {{ formatDuration(row.durationMs) }}
            </template>
          </el-table-column>
          <el-table-column prop="message" label="结果摘要" min-width="280" show-overflow-tooltip />
        </el-table>
      </div>

      <el-empty v-if="!loading && !logs.length" description="暂无执行日志" :image-size="120" />
    </el-card>
  </AppShell>
</template>

<script setup lang="ts">
import { onMounted, onBeforeUnmount, reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'
import AppShell from '../../components/AppShell.vue'
import { listSchedulerTaskLogs, schedulerTaskTypeOptions } from './scheduler.api'
import type { SchedulerTaskLog } from './scheduler.types'

type RangeMode = 'today' | 'threeDays' | 'sevenDays' | 'custom'

const loading = ref(false)
const logs = ref<SchedulerTaskLog[]>([])
const tableScrollRef = ref<HTMLElement>()
const dragState = reactive({
  dragging: false,
  startX: 0,
  scrollLeft: 0,
})

const filters = reactive<{
  keyword: string
  success: boolean | null
  rangeMode: RangeMode
  dateRange: [string, string]
}>({
  keyword: '',
  success: null,
  rangeMode: 'threeDays',
  dateRange: ['', ''],
})

const formatDateValue = (date: Date) => {
  const year = date.getFullYear()
  const month = `${date.getMonth() + 1}`.padStart(2, '0')
  const day = `${date.getDate()}`.padStart(2, '0')
  return `${year}-${month}-${day}`
}

const getPresetDateRange = (mode: Exclude<RangeMode, 'custom'>): [string, string] => {
  const end = new Date()
  const start = new Date(end)
  if (mode === 'threeDays') {
    start.setDate(end.getDate() - 2)
  } else if (mode === 'sevenDays') {
    start.setDate(end.getDate() - 6)
  }
  return [formatDateValue(start), formatDateValue(end)]
}

const syncDateRangeByMode = () => {
  if (filters.rangeMode === 'custom') return
  filters.dateRange = getPresetDateRange(filters.rangeMode)
}

const loadLogs = async () => {
  loading.value = true
  try {
    const [startDate, endDate] = filters.dateRange
    logs.value = await listSchedulerTaskLogs({
      keyword: filters.keyword || undefined,
      success: filters.success,
      startDate: startDate || undefined,
      endDate: endDate || undefined,
    })
  } catch (error) {
    ElMessage.error('加载日志失败')
  } finally {
    loading.value = false
  }
}

const handleRangeModeChange = () => {
  syncDateRangeByMode()
  loadLogs()
}

const handleDateRangeChange = () => {
  filters.rangeMode = 'custom'
  loadLogs()
}

const taskTypeLabel = (value: string) => {
  return schedulerTaskTypeOptions.find((item) => item.value === value)?.label ?? value
}

const formatDuration = (value?: number | null) => {
  const ms = Number(value ?? 0)
  if (ms < 1000) return `${ms} ms`
  return `${(ms / 1000).toFixed(2)} s`
}

const rowClassName = ({ row }: { row: SchedulerTaskLog }) => {
  return row.success === 1 ? 'is-success' : 'is-failure'
}

const startDragScroll = (event: MouseEvent) => {
  const wrapper = tableScrollRef.value
  if (!wrapper || event.button !== 0) return
  event.preventDefault()
  dragState.dragging = true
  dragState.startX = event.pageX
  dragState.scrollLeft = wrapper.scrollLeft
  wrapper.classList.add('is-dragging')
  window.addEventListener('mousemove', handleDragScroll)
  window.addEventListener('mouseup', stopDragScroll)
}

const handleDragScroll = (event: MouseEvent) => {
  const wrapper = tableScrollRef.value
  if (!wrapper || !dragState.dragging) return
  const deltaX = event.pageX - dragState.startX
  wrapper.scrollLeft = dragState.scrollLeft - deltaX
}

const stopDragScroll = () => {
  const wrapper = tableScrollRef.value
  dragState.dragging = false
  wrapper?.classList.remove('is-dragging')
  window.removeEventListener('mousemove', handleDragScroll)
  window.removeEventListener('mouseup', stopDragScroll)
}

onMounted(() => {
  syncDateRangeByMode()
  loadLogs()
})

onBeforeUnmount(() => {
  stopDragScroll()
})
</script>
