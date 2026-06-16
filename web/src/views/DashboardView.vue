<template>
  <div>
    <div class="dashboard-header">
      <div>
        <h1 class="page-title">监控概览</h1>
        <div class="page-subtitle">按平台、账号、分组和任务状态汇总当前监控面板。</div>
      </div>
      <div class="dashboard-actions">
        <el-button :icon="Refresh" :loading="loading" @click="loadDashboard">刷新</el-button>
        <el-button :icon="Plus" type="primary" @click="router.push('/platforms')">添加平台</el-button>
      </div>
    </div>

    <section class="metric-grid">
      <article v-for="item in metrics" :key="item.label" class="metric-card">
        <div class="metric-card__meta">
          <span>{{ item.label }}</span>
          <el-tag :type="item.type" effect="light" size="small">{{ item.status }}</el-tag>
        </div>
        <strong class="metric-card__value">{{ item.value }}</strong>
        <p class="metric-card__hint">{{ item.hint }}</p>
      </article>
    </section>

    <section class="dashboard-overview">
      <el-card class="page-card panel-card">
        <template #header>
          <div class="card-header">
            <span>关键口径</span>
            <el-button text type="primary" @click="router.push('/platforms')">查看平台</el-button>
          </div>
        </template>

        <div class="summary-grid">
          <article class="summary-box summary-box--blue">
            <span class="summary-box__label">平台扣减</span>
            <strong class="summary-box__value">{{ formatPrecise(summary.platformDeduct) }}</strong>
            <small class="summary-box__sub">未折算前的今日消耗</small>
          </article>
          <article class="summary-box summary-box--green">
            <span class="summary-box__label">实际消耗</span>
            <strong class="summary-box__value">{{ formatPrecise(summary.actualConsume) }}</strong>
            <small class="summary-box__sub">按充值 / 到账比例折算</small>
          </article>
          <article class="summary-box">
            <span class="summary-box__label">平台均倍率</span>
            <strong class="summary-box__value">{{ formatRate(summary.avgDeductRate) }}</strong>
            <small class="summary-box__sub">充值 / 到账平均比例</small>
          </article>
          <article class="summary-box">
            <span class="summary-box__label">分组总数</span>
            <strong class="summary-box__value">{{ groupTotal }}</strong>
            <small class="summary-box__sub">最新已采集的分组条目</small>
          </article>
        </div>
      </el-card>

      <el-card class="page-card panel-card">
        <template #header>
          <div class="card-header">
            <span>最近分组</span>
            <el-button text type="primary" @click="router.push('/groups')">查看全部</el-button>
          </div>
        </template>

        <div v-loading="loading" class="group-overview">
          <el-empty v-if="topGroups.length === 0 && !loading" description="暂无分组数据" />
          <article v-for="group in topGroups" :key="`${group.platformId}-${group.groupName}`" class="group-overview__item">
            <div class="group-overview__main">
              <div class="group-overview__name">{{ group.platformName }}</div>
              <div class="group-overview__sub">{{ group.groupName }}</div>
            </div>
            <div class="group-overview__rate">
              <span>倍率</span>
              <strong>{{ formatRate(group.currentRate) }}</strong>
            </div>
          </article>
        </div>
      </el-card>
    </section>

    <section class="dashboard-layout">
      <el-card class="page-card panel-card">
        <template #header>
          <div class="card-header">
            <span>平台状态</span>
            <el-button text type="primary" @click="router.push('/platforms')">查看全部</el-button>
          </div>
        </template>

        <div v-loading="loading" class="platform-list">
          <el-empty v-if="platforms.length === 0 && !loading" description="暂无平台" />
          <article v-for="platform in platforms" :key="platform.platformId" class="platform-item">
            <div class="platform-item__main">
              <div class="platform-item__mark">{{ platformInitial(platform.platformName) }}</div>
              <div class="platform-item__content">
                <div class="platform-item__title-row">
                  <div class="platform-item__name">{{ platform.platformName }}</div>
                  <el-tag effect="plain" size="small">{{ platformTypeLabel(platform.type) }}</el-tag>
                </div>
                <div class="platform-item__url" :title="platform.baseUrl">{{ platform.baseUrl }}</div>
                <div class="platform-item__meta">
                  <span>{{ platform.accountCount }} 个账号</span>
                  <span>最后采集 {{ formatTime(platform.lastCollectTime) }}</span>
                </div>
              </div>
            </div>
            <div class="platform-item__amounts">
              <span>余额 {{ formatAmount(platform.totalBalance) }}</span>
              <span class="warn">平台扣减 {{ formatPrecise(platform.totalPlatformDeduct) }}</span>
              <span class="warn">实际消耗 {{ formatPrecise(platform.totalActualConsume) }}</span>
            </div>
          </article>
        </div>
      </el-card>

      <el-card class="page-card panel-card">
        <template #header>
          <div class="card-header">
            <span>调度任务</span>
            <el-button text type="primary" @click="router.push('/scheduler/edit')">任务配置</el-button>
          </div>
        </template>

        <div v-loading="loading" class="task-list">
          <el-empty v-if="tasks.length === 0 && !loading" description="暂无定时任务" />
          <article v-for="task in tasks" :key="task.id" class="task-item">
            <div>
              <div class="task-item__name">{{ task.taskName }}</div>
              <div class="task-item__cron">{{ task.cronExpression }}</div>
            </div>
            <el-tag :type="task.isEnabled ? 'success' : 'info'" effect="light" size="small">
              {{ task.isEnabled ? '启用' : '停用' }}
            </el-tag>
          </article>
        </div>
      </el-card>
    </section>

    <el-card class="page-card log-card">
      <template #header>
        <div class="card-header">
          <span>最近执行</span>
          <el-button text type="primary" @click="router.push('/scheduler/logs')">日志详情</el-button>
        </div>
      </template>

      <div v-loading="loading" class="log-list">
        <el-empty v-if="logs.length === 0 && !loading" description="暂无执行日志" />
        <article v-for="log in logs" :key="log.id" class="log-item">
          <div class="log-item__status">
            <span class="status-dot" :class="`status-dot--${log.status.toLowerCase()}`" />
            <el-tag :type="statusTagType(log.status)" effect="light" size="small">
              {{ statusText(log.status) }}
            </el-tag>
          </div>
          <div class="log-item__body">
            <div class="log-item__title">{{ log.taskName || log.taskKey }}</div>
            <div class="log-item__message">{{ log.message || '-' }}</div>
          </div>
          <time class="log-item__time">{{ formatTime(log.finishTime || log.fireTime) }}</time>
        </article>
      </div>
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { Plus, Refresh } from '@element-plus/icons-vue'
import {
  listBalanceCollectionLogs,
  listPlatformGroups,
  listPlatformSummaries,
  listTasks,
  type PlatformGroupSummary,
  type PlatformSummary,
  type PlatformType,
  type TaskExecutionLog,
  type TaskSchedule
} from '@/api/monitor'

const router = useRouter()
const loading = ref(false)
const platforms = ref<PlatformSummary[]>([])
const groups = ref<PlatformGroupSummary[]>([])
const tasks = ref<TaskSchedule[]>([])
const logs = ref<TaskExecutionLog[]>([])

const enabledPlatforms = computed(() => platforms.value.filter(item => item.isEnabled))
const failedLogCount = computed(() => logs.value.filter(item => item.status === 'FAILED').length)
const groupTotal = computed(() => groups.value.reduce((sum, item) => sum + Number(item.groupCount || 0), 0))
const topGroups = computed(() => groups.value.flatMap(platform =>
  platform.groups.slice(0, 2).map(group => ({
    platformId: platform.platformId,
    platformName: platform.platformName,
    ...group
  }))
).slice(0, 6))
const summary = computed(() => {
  const totals = platforms.value.reduce((acc, platform) => {
    acc.platformDeduct += Number(platform.totalPlatformDeduct ?? 0)
    acc.actualConsume += Number(platform.totalActualConsume ?? 0)
    acc.rateSum += Number(platform.avgDeductRate ?? 0)
    acc.rateCount += platform.avgDeductRate == null ? 0 : 1
    return acc
  }, {
    platformDeduct: 0,
    actualConsume: 0,
    rateSum: 0,
    rateCount: 0
  })

  return {
    platformDeduct: totals.platformDeduct,
    actualConsume: totals.actualConsume,
    avgDeductRate: totals.rateCount > 0 ? totals.rateSum / totals.rateCount : 0
  }
})

const metrics = computed(() => [
  {
    label: '平台总数',
    value: String(platforms.value.length),
    status: `${enabledPlatforms.value.length} 监控中`,
    type: enabledPlatforms.value.length > 0 ? 'success' : 'info',
    hint: '当前已维护的平台配置'
  },
  {
    label: '账号总数',
    value: String(platforms.value.reduce((sum, item) => sum + Number(item.accountCount || 0), 0)),
    status: '平台聚合',
    type: 'info',
    hint: '所有平台下的账号总和'
  },
  {
    label: '分组总数',
    value: String(groupTotal.value),
    status: '最新倍率',
    type: 'warning',
    hint: '按平台采集到的分组记录'
  },
  {
    label: '最近执行',
    value: String(logs.value.length),
    status: failedLogCount.value > 0 ? `${failedLogCount.value} 失败` : '正常',
    type: failedLogCount.value > 0 ? 'danger' : 'success',
    hint: '最近 10 条任务日志'
  }
])

async function loadDashboard() {
  loading.value = true
  try {
    const [platformResponse, groupResponse, taskResponse, logResponse] = await Promise.all([
      listPlatformSummaries({ pageNo: 1, pageSize: 100 }),
      listPlatformGroups({ pageNo: 1, pageSize: 100 }),
      listTasks({ pageNo: 1, pageSize: 20 }),
      listBalanceCollectionLogs({ pageNo: 1, pageSize: 10 })
    ])
    platforms.value = platformResponse.data.records
    groups.value = groupResponse.data.records
    tasks.value = taskResponse.data.records
    logs.value = logResponse.data.records
  } finally {
    loading.value = false
  }
}

function platformInitial(name?: string) {
  return (name || 'P').trim().slice(0, 1).toUpperCase()
}

function formatAmount(value?: number) {
  return Number(value ?? 0).toFixed(2)
}

function formatPrecise(value?: number) {
  return Number(value ?? 0).toFixed(4)
}

function formatRate(value?: number) {
  return Number(value ?? 0).toFixed(4)
}

function formatTime(value?: string) {
  if (!value) {
    return '-'
  }
  const date = new Date(value)
  if (Number.isNaN(date.getTime())) {
    return value.replace('T', ' ').replace('+08:00', '')
  }
  const pad = (num: number) => String(num).padStart(2, '0')
  return `${date.getMonth() + 1}-${pad(date.getDate())} ${pad(date.getHours())}:${pad(date.getMinutes())}`
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

function platformTypeLabel(type?: PlatformType) {
  if (type === 'newApi') {
    return 'NewApi'
  }
  return 'Sub2Api'
}

onMounted(loadDashboard)
</script>

<style scoped>
.dashboard-header {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 16px;
}

.dashboard-actions {
  display: flex;
  gap: 10px;
}

.metric-grid {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 14px;
  margin: 24px 0;
}

.metric-card {
  min-height: 132px;
  border: 1px solid #e5e7eb;
  border-radius: 8px;
  background: #fff;
  padding: 16px;
  box-shadow: 0 10px 30px rgb(31 41 55 / 5%);
}

.metric-card__meta {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  color: #64748b;
  font-size: 13px;
}

.metric-card__value {
  display: block;
  margin-top: 16px;
  color: #0f172a;
  font-size: 28px;
  line-height: 1;
}

.metric-card__hint {
  margin: 12px 0 0;
  color: #94a3b8;
  font-size: 12px;
}

.dashboard-overview {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 16px;
  margin-bottom: 16px;
}

.dashboard-layout {
  display: grid;
  grid-template-columns: minmax(0, 1.35fr) minmax(360px, 0.65fr);
  gap: 16px;
  margin-bottom: 16px;
}

.panel-card {
  min-width: 0;
}

.summary-grid {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 12px;
}

.summary-box {
  border-radius: 8px;
  background: #f8fafc;
  padding: 14px;
}

.summary-box--blue {
  background: #eef6ff;
}

.summary-box--green {
  background: #ecfdf5;
}

.summary-box__label {
  color: #64748b;
  font-size: 12px;
}

.summary-box__value {
  display: block;
  margin-top: 10px;
  color: #0f172a;
  font-size: 22px;
  font-weight: 700;
}

.summary-box__sub {
  display: block;
  margin-top: 6px;
  color: #94a3b8;
  font-size: 12px;
}

.group-overview {
  display: flex;
  flex-direction: column;
  gap: 10px;
  min-height: 180px;
}

.group-overview__item {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  border: 1px solid #edf2f7;
  border-radius: 8px;
  background: #fbfdff;
  padding: 12px;
}

.group-overview__main {
  min-width: 0;
}

.group-overview__name {
  overflow: hidden;
  color: #0f172a;
  font-weight: 700;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.group-overview__sub {
  overflow: hidden;
  margin-top: 4px;
  color: #64748b;
  font-size: 12px;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.group-overview__rate {
  display: flex;
  align-items: baseline;
  gap: 8px;
  color: #64748b;
}

.group-overview__rate strong {
  color: #0f172a;
  font-size: 18px;
}

.platform-list,
.task-list,
.log-list {
  min-height: 180px;
}

.platform-list {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 12px;
}

.platform-list :deep(.el-empty),
.task-list :deep(.el-empty),
.log-list :deep(.el-empty) {
  grid-column: 1 / -1;
}

.platform-item {
  min-width: 0;
  border: 1px solid #edf2f7;
  border-radius: 8px;
  background: #fbfdff;
  padding: 12px;
}

.platform-item__main {
  display: flex;
  min-width: 0;
  align-items: center;
  gap: 10px;
}

.platform-item__mark {
  display: grid;
  width: 34px;
  height: 34px;
  flex: 0 0 auto;
  place-items: center;
  border-radius: 8px;
  background: #e8f3ff;
  color: #2563eb;
  font-weight: 700;
}

.platform-item__content {
  min-width: 0;
}

.platform-item__title-row {
  display: flex;
  align-items: center;
  gap: 8px;
}

.platform-item__name {
  overflow: hidden;
  color: #0f172a;
  font-weight: 700;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.platform-item__url {
  overflow: hidden;
  margin-top: 4px;
  color: #64748b;
  font-size: 12px;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.platform-item__meta {
  display: flex;
  flex-wrap: wrap;
  gap: 12px;
  margin-top: 6px;
  color: #64748b;
  font-size: 12px;
}

.platform-item__amounts {
  display: flex;
  flex-wrap: wrap;
  justify-content: space-between;
  gap: 8px;
  margin-top: 12px;
  color: #475569;
  font-size: 12px;
}

.platform-item__amounts .warn {
  color: #b45309;
}

.task-list {
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.task-item {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  border: 1px solid #edf2f7;
  border-radius: 8px;
  background: #fbfdff;
  padding: 12px;
}

.task-item__name {
  color: #0f172a;
  font-weight: 700;
}

.task-item__cron {
  margin-top: 5px;
  color: #64748b;
  font-family: ui-monospace, SFMono-Regular, Consolas, monospace;
  font-size: 12px;
}

.log-list {
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.log-item {
  display: grid;
  grid-template-columns: 92px minmax(0, 1fr) 96px;
  gap: 12px;
  align-items: center;
  border: 1px solid #edf2f7;
  border-radius: 8px;
  background: #fbfdff;
  padding: 12px;
}

.log-item__status {
  display: flex;
  align-items: center;
  gap: 8px;
}

.status-dot {
  width: 8px;
  height: 8px;
  border-radius: 999px;
  background: #94a3b8;
}

.status-dot--success {
  background: #22c55e;
}

.status-dot--failed {
  background: #ef4444;
}

.status-dot--running {
  background: #f59e0b;
}

.log-item__body {
  min-width: 0;
}

.log-item__title {
  overflow: hidden;
  color: #0f172a;
  font-weight: 700;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.log-item__message {
  overflow: hidden;
  margin-top: 4px;
  color: #64748b;
  font-size: 12px;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.log-item__time {
  color: #64748b;
  font-size: 12px;
  text-align: right;
}
</style>
