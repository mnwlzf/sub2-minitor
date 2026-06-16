<template>
  <div>
    <h1 class="page-title">日志查看</h1>
    <div class="page-subtitle">查看基础任务「余额渠道采集」的执行记录。</div>

    <el-card class="page-card scheduler-log__card">
      <template #header>
        <div class="card-header">
          <span>任务日志</span>
          <el-button :icon="Refresh" text @click="loadLogs">刷新</el-button>
        </div>
      </template>

      <el-table :data="logs" v-loading="loading" size="small" style="width: 100%">
        <el-table-column prop="taskKey" label="任务标识" width="160" />
        <el-table-column prop="taskName" label="任务名称" width="140" />
        <el-table-column prop="triggerType" label="触发方式" width="120" />
        <el-table-column prop="status" label="状态" width="100">
          <template #default="{ row }">
            <el-tag :type="statusTagType(row.status)" effect="light">
              {{ statusText(row.status) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="cronExpression" label="Cron" min-width="180" />
        <el-table-column prop="fireTime" label="触发时间" width="180">
          <template #default="{ row }">
            {{ formatTime(row.fireTime) }}
          </template>
        </el-table-column>
        <el-table-column prop="finishTime" label="完成时间" width="180">
          <template #default="{ row }">
            {{ formatTime(row.finishTime) }}
          </template>
        </el-table-column>
        <el-table-column prop="message" label="消息" min-width="260" show-overflow-tooltip />
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
import { onMounted, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { Refresh } from '@element-plus/icons-vue'
import { listBalanceCollectionLogs, type TaskExecutionLog } from '@/api/monitor'

const loading = ref(false)
const logs = ref<TaskExecutionLog[]>([])
const total = ref(0)
const pageNo = ref(1)
const pageSize = ref(20)

function formatTime(value?: string) {
  return value ? value.replace('T', ' ').replace('+08:00', '') : '-'
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

async function loadLogs() {
  loading.value = true
  try {
    const response = await listBalanceCollectionLogs({
      pageNo: pageNo.value,
      pageSize: pageSize.value
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

.scheduler-log__footer {
  display: flex;
  justify-content: flex-end;
  padding-top: 16px;
}
</style>
