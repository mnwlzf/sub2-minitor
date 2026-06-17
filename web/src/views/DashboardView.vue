<template>
  <div>
    <div class="dashboard-header">
      <div>
        <h1 class="page-title">监控概览</h1>
        <div class="page-subtitle">优先展示余额、消耗、采集状态和失败任务，方便直接处理问题。</div>
      </div>
      <div class="dashboard-actions">
        <el-button :icon="Refresh" :loading="loading" @click="loadDashboard">刷新</el-button>
        <el-button :icon="Plus" type="primary" @click="router.push('/platforms')">添加平台</el-button>
      </div>
    </div>

    <section class="metric-grid">
      <article v-for="item in metrics" :key="item.label" class="metric-card" :class="item.className">
        <div class="metric-card__meta">
          <span>{{ item.label }}</span>
          <el-tag :type="item.type" effect="light" size="small">{{ item.status }}</el-tag>
        </div>
        <strong class="metric-card__value">{{ item.value }}</strong>
        <p class="metric-card__hint">{{ item.hint }}</p>
      </article>
    </section>

    <section class="dashboard-main">
      <el-card class="page-card panel-card">
        <template #header>
          <div class="card-header">
            <span>需要处理</span>
            <el-button text type="primary" @click="router.push('/scheduler/logs')">执行日志</el-button>
          </div>
        </template>

        <div v-loading="loading" class="issue-list">
          <el-empty v-if="issues.length === 0 && !loading" description="暂无待处理问题" />
          <article v-for="issue in issues" :key="issue.key" class="issue-item" :class="`issue-item--${issue.level}`">
            <div class="issue-item__badge">{{ issue.levelText }}</div>
            <div class="issue-item__body">
              <div class="issue-item__title">{{ issue.title }}</div>
              <div class="issue-item__desc">{{ issue.description }}</div>
            </div>
            <el-button text type="primary" @click="router.push(issue.link)">处理</el-button>
          </article>
        </div>
      </el-card>

      <el-card class="page-card panel-card">
        <template #header>
          <div class="card-header">
            <span>消耗排行</span>
            <el-button text type="primary" @click="router.push('/platforms')">平台详情</el-button>
          </div>
        </template>

        <div v-loading="loading" class="rank-list">
          <el-empty v-if="topConsumeAccounts.length === 0 && !loading" description="暂无消耗数据" />
          <article v-for="account in topConsumeAccounts" :key="account.key" class="rank-item">
            <div class="rank-item__main">
              <div class="rank-item__name">{{ account.username }}</div>
              <div class="rank-item__sub">{{ account.platformName }}</div>
            </div>
            <div class="rank-item__amount">
              <strong>{{ formatPrecise(account.actualConsume) }}</strong>
              <span>实际消耗</span>
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
          <article v-for="platform in platformCards" :key="platform.platformId" class="platform-item">
            <div class="platform-item__main">
              <div class="platform-item__mark">{{ platformInitial(platform.platformName) }}</div>
              <div class="platform-item__content">
                <div class="platform-item__title-row">
                  <div class="platform-item__name">{{ platform.platformName }}</div>
                  <el-tag :type="platform.statusType" effect="light" size="small">{{ platform.statusText }}</el-tag>
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
              <span>今日扣减 {{ formatPrecise(platform.totalPlatformDeduct) }}</span>
              <span class="warn">实际消耗 {{ formatPrecise(platform.totalActualConsume) }}</span>
            </div>
          </article>
        </div>
      </el-card>

      <el-card class="page-card panel-card">
        <template #header>
          <div class="card-header">
            <span>低余额账号</span>
            <el-button text type="primary" @click="router.push('/accounts')">账号列表</el-button>
          </div>
        </template>

        <div v-loading="loading" class="rank-list">
          <el-empty v-if="lowBalanceAccounts.length === 0 && !loading" description="暂无低余额账号" />
          <article v-for="account in lowBalanceAccounts" :key="account.key" class="rank-item rank-item--balance">
            <div class="rank-item__main">
              <div class="rank-item__name">{{ account.username }}</div>
              <div class="rank-item__sub">{{ account.platformName }}</div>
            </div>
            <div class="rank-item__amount">
              <strong>{{ formatAmount(account.latestBalance) }}</strong>
              <span>余额</span>
            </div>
          </article>
        </div>
      </el-card>
    </section>

    <section class="dashboard-layout dashboard-layout--bottom">
      <el-card class="page-card panel-card">
        <template #header>
          <div class="card-header">
            <span>倍率最高分组</span>
            <el-button text type="primary" @click="router.push('/groups')">查看全部</el-button>
          </div>
        </template>

        <div v-loading="loading" class="group-overview">
          <el-empty v-if="topGroups.length === 0 && !loading" description="暂无分组数据" />
          <article v-for="group in topGroups" :key="`${group.platformId}-${group.groupName}`" class="group-overview__item">
            <div class="group-overview__main">
              <div class="group-overview__name">{{ group.groupName }}</div>
              <div class="group-overview__sub">{{ group.platformName }}</div>
            </div>
            <div class="group-overview__rate">
              <span>实际倍率</span>
              <strong>{{ formatRate(group.actualRate) }}</strong>
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
  type PlatformAccountSummary,
  type PlatformGroupSummary,
  type PlatformSummary,
  type PlatformType,
  type TaskExecutionLog,
  type TaskSchedule
} from '@/api/monitor'

type TagType = 'success' | 'warning' | 'info' | 'primary' | 'danger'
type IssueLevel = 'danger' | 'warning'

interface AccountWithPlatform extends PlatformAccountSummary {
  key: string
  platformId: string
  platformName: string
}

interface IssueItem {
  key: string
  level: IssueLevel
  levelText: string
  title: string
  description: string
  link: string
}

const router = useRouter()
const loading = ref(false)
const platforms = ref<PlatformSummary[]>([])
const groups = ref<PlatformGroupSummary[]>([])
const tasks = ref<TaskSchedule[]>([])
const logs = ref<TaskExecutionLog[]>([])

const enabledPlatforms = computed(() => platforms.value.filter(item => item.isEnabled))
const disabledPlatformCount = computed(() => platforms.value.length - enabledPlatforms.value.length)
const failedLogCount = computed(() => logs.value.filter(item => item.status === 'FAILED').length)
const runningLogCount = computed(() => logs.value.filter(item => item.status === 'RUNNING').length)
const enabledTaskCount = computed(() => tasks.value.filter(item => item.isEnabled).length)
const totalAccountCount = computed(() => platforms.value.reduce((sum, item) => sum + Number(item.accountCount || 0), 0))
const totalBalance = computed(() => platforms.value.reduce((sum, item) => sum + Number(item.totalBalance || 0), 0))
const totalActualConsume = computed(() => platforms.value.reduce((sum, item) => sum + Number(item.totalActualConsume || 0), 0))
const allAccounts = computed<AccountWithPlatform[]>(() => platforms.value.flatMap(platform =>
  (platform.accounts || []).map(account => ({
    ...account,
    key: `${platform.platformId}-${account.accountId}`,
    platformId: platform.platformId,
    platformName: platform.platformName
  }))
))
const zeroBalanceAccounts = computed(() => allAccounts.value.filter(account => Number(account.latestBalance || 0) <= 0))
const lowBalanceAccounts = computed(() => allAccounts.value
  .filter(account => Number(account.latestBalance || 0) > 0)
  .sort((a, b) => Number(a.latestBalance || 0) - Number(b.latestBalance || 0))
  .slice(0, 8))
const topConsumeAccounts = computed(() => allAccounts.value
  .filter(account => Number(account.actualConsume || 0) > 0)
  .sort((a, b) => Number(b.actualConsume || 0) - Number(a.actualConsume || 0))
  .slice(0, 8))
const stalePlatforms = computed(() => enabledPlatforms.value.filter(platform => isCollectStale(platform.lastCollectTime)))
const topGroups = computed(() => groups.value.flatMap(platform =>
  platform.groups.map(group => ({
    platformId: platform.platformId,
    platformName: platform.platformName,
    ...group
  }))
).sort((a, b) => Number(b.actualRate || 0) - Number(a.actualRate || 0)).slice(0, 8))
const platformCards = computed(() => platforms.value.map(platform => {
  const stale = platform.isEnabled && isCollectStale(platform.lastCollectTime)
  const statusText = !platform.isEnabled ? '未监控' : stale ? '采集滞后' : '监控中'
  const statusType: TagType = !platform.isEnabled ? 'info' : stale ? 'warning' : 'success'

  return {
    ...platform,
    statusText,
    statusType
  }
}))
const issues = computed<IssueItem[]>(() => {
  const items: IssueItem[] = []

  if (failedLogCount.value > 0) {
    items.push({
      key: 'failed-logs',
      level: 'danger',
      levelText: '失败',
      title: `${failedLogCount.value} 条最近任务执行失败`,
      description: '优先查看采集任务是否失败，避免余额和倍率数据继续失真。',
      link: '/scheduler/logs'
    })
  }

  if (zeroBalanceAccounts.value.length > 0) {
    items.push({
      key: 'zero-balance',
      level: 'danger',
      levelText: '余额',
      title: `${zeroBalanceAccounts.value.length} 个账号余额为 0`,
      description: '这些账号可能已经无法继续消费，需要充值或停用。',
      link: '/accounts'
    })
  }

  if (stalePlatforms.value.length > 0) {
    items.push({
      key: 'stale-platforms',
      level: 'warning',
      levelText: '采集',
      title: `${stalePlatforms.value.length} 个平台超过 24 小时未采集`,
      description: stalePlatforms.value.map(item => item.platformName).slice(0, 3).join('、'),
      link: '/platforms'
    })
  }

  if (disabledPlatformCount.value > 0) {
    items.push({
      key: 'disabled-platforms',
      level: 'warning',
      levelText: '停用',
      title: `${disabledPlatformCount.value} 个平台未开启监控`,
      description: '确认是否为主动停用，避免遗漏平台数据。',
      link: '/platforms'
    })
  }

  if (enabledTaskCount.value === 0 && tasks.value.length > 0) {
    items.push({
      key: 'no-enabled-task',
      level: 'warning',
      levelText: '任务',
      title: '当前没有启用的调度任务',
      description: '余额、渠道和倍率不会自动更新。',
      link: '/scheduler/edit'
    })
  }

  return items.slice(0, 6)
})
const metrics = computed(() => [
  {
    label: '当前总余额',
    value: formatAmount(totalBalance.value),
    status: `${totalAccountCount.value} 账号`,
    type: totalBalance.value > 0 ? 'success' : 'danger',
    className: '',
    hint: `${platforms.value.length} 个平台，${enabledPlatforms.value.length} 个监控中`
  },
  {
    label: '今日实际消耗',
    value: formatPrecise(totalActualConsume.value),
    status: '已折算',
    type: totalActualConsume.value > 0 ? 'warning' : 'info',
    className: '',
    hint: '按平台充值 / 到账比例折算后的消耗'
  },
  {
    label: '待处理问题',
    value: String(issues.value.length),
    status: issues.value.length > 0 ? '需处理' : '正常',
    type: issues.value.length > 0 ? 'danger' : 'success',
    className: issues.value.length > 0 ? 'metric-card--danger' : '',
    hint: '汇总失败任务、零余额、采集滞后和停用平台'
  },
  {
    label: '调度状态',
    value: `${enabledTaskCount.value}/${tasks.value.length}`,
    status: failedLogCount.value > 0 ? `${failedLogCount.value} 失败` : runningLogCount.value > 0 ? '执行中' : '正常',
    type: failedLogCount.value > 0 ? 'danger' : runningLogCount.value > 0 ? 'warning' : 'success',
    className: '',
    hint: '启用任务数 / 任务总数，结合最近执行结果判断'
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

function isCollectStale(value?: string) {
  if (!value) {
    return true
  }
  const date = new Date(value)
  if (Number.isNaN(date.getTime())) {
    return false
  }
  return Date.now() - date.getTime() > 24 * 60 * 60 * 1000
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

.metric-card--danger {
  border-color: #fecaca;
  background: #fff7f7;
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
  color: #64748b;
  font-size: 12px;
  line-height: 1.5;
}

.dashboard-main {
  display: grid;
  grid-template-columns: minmax(0, 1.15fr) minmax(360px, 0.85fr);
  gap: 16px;
  margin-bottom: 16px;
}

.dashboard-layout {
  display: grid;
  grid-template-columns: minmax(0, 1.35fr) minmax(360px, 0.65fr);
  gap: 16px;
  margin-bottom: 16px;
}

.dashboard-layout--bottom {
  grid-template-columns: minmax(0, 1fr) minmax(360px, 0.7fr);
}

.panel-card {
  min-width: 0;
}

.issue-list,
.rank-list,
.group-overview,
.task-list,
.log-list {
  min-height: 180px;
}

.issue-list,
.rank-list,
.task-list,
.log-list,
.group-overview {
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.issue-item {
  display: grid;
  grid-template-columns: 48px minmax(0, 1fr) 52px;
  gap: 12px;
  align-items: center;
  border: 1px solid #fed7aa;
  border-radius: 8px;
  background: #fffaf0;
  padding: 12px;
}

.issue-item--danger {
  border-color: #fecaca;
  background: #fff7f7;
}

.issue-item__badge {
  display: grid;
  width: 42px;
  height: 42px;
  place-items: center;
  border-radius: 8px;
  background: #f97316;
  color: #fff;
  font-size: 13px;
  font-weight: 700;
}

.issue-item--danger .issue-item__badge {
  background: #dc2626;
}

.issue-item__body {
  min-width: 0;
}

.issue-item__title {
  overflow: hidden;
  color: #0f172a;
  font-weight: 700;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.issue-item__desc {
  overflow: hidden;
  margin-top: 5px;
  color: #64748b;
  font-size: 12px;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.rank-item,
.group-overview__item,
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

.rank-item--balance {
  background: #f8fafc;
}

.rank-item__main,
.group-overview__main {
  min-width: 0;
}

.rank-item__name,
.group-overview__name,
.task-item__name {
  overflow: hidden;
  color: #0f172a;
  font-weight: 700;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.rank-item__sub,
.group-overview__sub,
.task-item__cron {
  overflow: hidden;
  margin-top: 5px;
  color: #64748b;
  font-size: 12px;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.task-item__cron {
  font-family: ui-monospace, SFMono-Regular, Consolas, monospace;
}

.rank-item__amount,
.group-overview__rate {
  display: flex;
  min-width: 112px;
  flex-direction: column;
  align-items: flex-end;
  color: #64748b;
  font-size: 12px;
}

.rank-item__amount strong,
.group-overview__rate strong {
  color: #0f172a;
  font-size: 18px;
  line-height: 1.2;
}

.platform-list {
  display: grid;
  min-height: 180px;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 12px;
}

.platform-list :deep(.el-empty),
.issue-list :deep(.el-empty),
.rank-list :deep(.el-empty),
.task-list :deep(.el-empty),
.log-list :deep(.el-empty),
.group-overview :deep(.el-empty) {
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
  min-width: 0;
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
