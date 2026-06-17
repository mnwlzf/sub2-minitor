<template>
  <div>
    <h1 class="page-title">日志查看</h1>
    <div class="page-subtitle">查看定时任务执行记录，包括成功、失败和错误原因。</div>

    <el-card class="page-card scheduler-log__card">
      <template #header>
        <div class="card-header">
          <span>任务日志</span>
          <div class="toolbar">
            <el-select v-model="filters.taskKey" clearable placeholder="全部任务" class="toolbar__select" @change="handleFilterChange">
              <el-option label="余额渠道采集" value="balance-channel-collect" />
              <el-option label="每日数据汇总" value="daily-data-summary" />
            </el-select>
            <el-button :icon="Refresh" @click="loadLogs">刷新</el-button>
          </div>
        </div>
      </template>

      <el-table :data="logs" v-loading="loading" size="small" style="width: 100%" table-layout="fixed">
        <el-table-column prop="taskKey" label="任务标识" width="170" show-overflow-tooltip />
        <el-table-column prop="taskName" label="任务名称" width="130" show-overflow-tooltip />
        <el-table-column label="触发方式" width="110">
          <template #default="{ row }">
            {{ triggerTypeText(row.triggerType) }}
          </template>
        </el-table-column>
        <el-table-column prop="status" label="状态" width="82">
          <template #default="{ row }">
            <el-tag :type="statusTagType(row.status)" effect="light">
              {{ statusText(row.status) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="cronExpression" label="Cron" width="130" show-overflow-tooltip />
        <el-table-column prop="fireTime" label="触发时间" width="170">
          <template #default="{ row }">
            {{ formatTime(row.fireTime) }}
          </template>
        </el-table-column>
        <el-table-column prop="finishTime" label="完成时间" width="170">
          <template #default="{ row }">
            {{ formatTime(row.finishTime) }}
          </template>
        </el-table-column>
        <el-table-column prop="message" label="消息" min-width="360" show-overflow-tooltip />
      </el-table>

      <div class="scheduler-log__footer">
        <el-pagination
          v-model:current-page="pageNo"
          v-model:page-size="pageSize"
          :page-sizes="[10, 20, 50]"
          :total="total"
          layout="total, sizes, prev, pager, next"
          @current-change="loadLogs"
          @size-change="handleSizeChange"
        />
      </div>
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { Refresh } from '@element-plus/icons-vue'
import { listTaskLogs, type TaskExecutionLog } from '@/api/monitor'

const loading = ref(false)
const logs = ref<TaskExecutionLog[]>([])
const total = ref(0)
const pageNo = ref(1)
const pageSize = ref(20)
const filters = reactive({
  taskKey: ''
})

function formatTime(value?: string) {
  if (!value) {
    return '-'
  }
  const date = new Date(value)
  if (Number.isNaN(date.getTime())) {
    return value
  }
  const pad = (num: number) => String(num).padStart(2, '0')
  return `${date.getFullYear()}-${pad(date.getMonth() + 1)}-${pad(date.getDate())} ${pad(date.getHours())}:${pad(date.getMinutes())}:${pad(date.getSeconds())}`
}

function statusText(status: string) {
  if (status === 'SUCCESS') return '成功'
  if (status === 'FAILED') return '失败'
  if (status === 'RUNNING') return '执行中'
  return status || '-'
}

function statusTagType(status: string) {
  if (status === 'SUCCESS') return 'success'
  if (status === 'FAILED') return 'danger'
  if (status === 'RUNNING') return 'warning'
  return 'info'
}

function triggerTypeText(value?: string) {
  if (!value) {
    return '-'
  }
  if (value.includes('CronTrigger')) {
    return '定时触发'
  }
  if (value.includes('SimpleTrigger')) {
    return '手动执行'
  }
  return value
}

async function loadLogs() {
  loading.value = true
  try {
    const response = await listTaskLogs({
      pageNo: pageNo.value,
      pageSize: pageSize.value,
      taskKey: filters.taskKey || undefined
    })
    logs.value = response.data.records
    total.value = response.data.total
  } catch (error) {
    ElMessage.error('日志加载失败')
    throw error
  } finally {
    loading.value = false
  }
}

function handleSizeChange(size: number) {
  pageSize.value = size
  pageNo.value = 1
  void loadLogs()
}

function handleFilterChange() {
  pageNo.value = 1
  void loadLogs()
}

onMounted(loadLogs)
</script>

<style scoped>
.scheduler-log__card {
  margin-top: 24px;
}

.card-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  font-weight: 700;
}

.toolbar {
  display: flex;
  align-items: center;
  gap: 12px;
}

.toolbar__select {
  width: 180px;
}

.scheduler-log__footer {
  display: flex;
  justify-content: flex-end;
  padding-top: 16px;
}

.scheduler-log__card :deep(.el-card__body) {
  padding-top: 12px;
}

.scheduler-log__card :deep(.el-table) {
  width: 100%;
}
</style>
