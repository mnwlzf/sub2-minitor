<template>
  <div>
    <div class="page-header">
      <div>
        <h1 class="page-title">余额变化曲线</h1>
        <div class="page-subtitle">按平台分布展示账号余额，横轴时间来自余额采集任务的 Cron 表达式。</div>
      </div>
      <el-button :icon="Refresh" :loading="loading" @click="loadData">刷新</el-button>
    </div>

    <div v-loading="loading" class="trend-page">
      <el-empty v-if="!loading && platformTrends.length === 0" description="暂无余额历史" />
      <el-card v-for="platform in platformTrends" :key="platform.platformId" class="page-card trend-card">
        <template #header>
          <div class="card-header">
            <span>{{ platform.platformName }}</span>
            <span class="trend-card__cron">Cron {{ platform.cronExpression || '-' }}</span>
          </div>
        </template>

        <div class="account-chart-list">
          <article v-for="account in platform.accounts" :key="account.accountId" class="account-chart">
            <div class="account-chart__header">
              <span class="account-chart__name">{{ account.username }}</span>
              <span class="account-chart__latest">当前余额 {{ latestBalance(account.points) }}</span>
            </div>
            <div class="account-chart__body">
              <el-empty v-if="account.points.length === 0" description="暂无账号余额历史" />
              <svg v-else class="account-chart__svg" viewBox="0 0 720 220" preserveAspectRatio="none" role="img">
                <line x1="42" y1="20" x2="42" y2="176" class="account-chart__axis" />
                <line x1="42" y1="176" x2="700" y2="176" class="account-chart__axis" />
                <polyline :points="polyline(account.points)" class="account-chart__line" />
                <circle
                  v-for="point in svgPoints(account.points)"
                  :key="point.key"
                  :cx="point.x"
                  :cy="point.y"
                  r="4"
                  class="account-chart__dot"
                >
                  <title>{{ point.label }}</title>
                </circle>
                <text
                  v-for="tick in ticks(account.points)"
                  :key="tick.key"
                  :x="tick.x"
                  y="202"
                  class="account-chart__tick"
                  text-anchor="middle"
                >
                  {{ tick.label }}
                </text>
                <text x="14" y="28" class="account-chart__scale">{{ maxLabel(account.points) }}</text>
                <text x="14" y="176" class="account-chart__scale">{{ minLabel(account.points) }}</text>
              </svg>
            </div>
          </article>
        </div>
      </el-card>
    </div>
  </div>
</template>

<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { Refresh } from '@element-plus/icons-vue'
import {
  getPlatformBalanceTrend,
  listPlatformSummaries,
  type PlatformSummary,
  type PlatformBalanceTrend,
  type PlatformBalanceTrendPoint
} from '@/api/monitor'

const loading = ref(false)
const platformTrends = ref<PlatformBalanceTrend[]>([])

async function loadData() {
  loading.value = true
  try {
    const platformResponse = await listPlatformSummaries({ pageNo: 1, pageSize: 200 })
    const platforms = platformResponse.data.records as PlatformSummary[]
    const trendResponses = await Promise.all(platforms.map(platform =>
      getPlatformBalanceTrend(platform.platformId, { limit: 80 })
    ))
    platformTrends.value = trendResponses
      .map(response => response.data as PlatformBalanceTrend)
      .filter(trend => trend.accounts.some(account => account.points.length > 0))
  } finally {
    loading.value = false
  }
}

function svgPoints(points: PlatformBalanceTrendPoint[]) {
  if (points.length === 0) {
    return []
  }
  const balances = points.map(point => Number(point.balance ?? 0))
  const min = Math.min(...balances)
  const max = Math.max(...balances)
  const range = max - min || 1
  const width = 658
  const height = 156
  return points.map((point, index) => {
    const x = 42 + (points.length === 1 ? width : (index / (points.length - 1)) * width)
    const y = 176 - ((Number(point.balance ?? 0) - min) / range) * height
    return {
      key: `${point.time}-${index}`,
      x,
      y,
      label: `${formatChartTime(point.time)} ${formatAmount(point.balance)}`
    }
  })
}

function polyline(points: PlatformBalanceTrendPoint[]) {
  return svgPoints(points).map(point => `${point.x},${point.y}`).join(' ')
}

function ticks(points: PlatformBalanceTrendPoint[]) {
  if (points.length === 0) {
    return []
  }
  const indexes = Array.from(new Set([0, Math.floor((points.length - 1) / 2), points.length - 1]))
  const renderedPoints = svgPoints(points)
  return indexes.map(index => ({
    key: `${points[index].time}-${index}`,
    x: renderedPoints[index].x,
    label: formatChartTime(points[index].time)
  }))
}

function minLabel(points: PlatformBalanceTrendPoint[]) {
  const balances = points.map(point => Number(point.balance ?? 0))
  return balances.length === 0 ? '-' : Math.min(...balances).toFixed(2)
}

function maxLabel(points: PlatformBalanceTrendPoint[]) {
  const balances = points.map(point => Number(point.balance ?? 0))
  return balances.length === 0 ? '-' : Math.max(...balances).toFixed(2)
}

function latestBalance(points: PlatformBalanceTrendPoint[]) {
  if (points.length === 0) {
    return '-'
  }
  return formatAmount(points[points.length - 1].balance)
}

function formatAmount(value?: number) {
  return Number(value ?? 0).toFixed(2)
}

function formatChartTime(value?: string) {
  if (!value) {
    return '-'
  }
  const date = new Date(value)
  if (Number.isNaN(date.getTime())) {
    return value
  }
  const pad = (num: number) => String(num).padStart(2, '0')
  return `${pad(date.getMonth() + 1)}-${pad(date.getDate())} ${pad(date.getHours())}:${pad(date.getMinutes())}`
}

onMounted(loadData)
</script>

<style scoped>
.page-header {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 16px;
}

.trend-page {
  display: flex;
  flex-direction: column;
  gap: 16px;
  margin-top: 24px;
  min-height: 260px;
}

.trend-card__cron {
  color: #64748b;
  font-family: ui-monospace, SFMono-Regular, Consolas, monospace;
  font-size: 12px;
}

.account-chart-list {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 14px;
}

.account-chart {
  min-width: 0;
  border: 1px solid #edf2f7;
  border-radius: 8px;
  background: #fbfdff;
  padding: 12px;
}

.account-chart__header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  margin-bottom: 10px;
}

.account-chart__name {
  overflow: hidden;
  color: #0f172a;
  font-weight: 700;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.account-chart__latest {
  flex: 0 0 auto;
  color: #2563eb;
  font-size: 12px;
  font-weight: 700;
}

.account-chart__body {
  min-height: 240px;
}

.account-chart__svg {
  display: block;
  width: 100%;
  height: 220px;
}

.account-chart__axis {
  stroke: #dbe3ee;
  stroke-width: 1;
}

.account-chart__line {
  fill: none;
  stroke: #2563eb;
  stroke-linecap: round;
  stroke-linejoin: round;
  stroke-width: 3;
}

.account-chart__dot {
  fill: #fff;
  stroke: #2563eb;
  stroke-width: 2;
}

.account-chart__tick,
.account-chart__scale {
  fill: #64748b;
  font-size: 11px;
}
</style>
