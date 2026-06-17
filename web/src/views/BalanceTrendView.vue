<template>
  <div>
    <div class="page-header">
      <div>
        <h1 class="page-title">余额查看</h1>
        <div class="page-subtitle">按平台查看账号余额历史，数据来自最近的余额采集记录。</div>
      </div>
      <el-button :icon="Refresh" :loading="loading" @click="loadData">刷新</el-button>
    </div>

    <div v-loading="loading" class="trend-page">
      <el-empty v-if="!loading && platformTrends.length === 0" description="暂无账号余额" />
      <el-card v-for="platform in platformTrends" :key="platform.platformId" class="page-card trend-card">
        <template #header>
          <div class="card-header">
            <span>{{ platform.platformName }}</span>
            <span class="trend-card__cron">采集任务 {{ platform.cronExpression || '-' }}</span>
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
              <div v-else class="account-chart__plot">
                <svg class="account-chart__svg" viewBox="0 0 720 180" preserveAspectRatio="none" role="img">
                  <defs>
                    <linearGradient :id="`balance-fill-${account.accountId}`" x1="0" y1="0" x2="0" y2="1">
                      <stop offset="0%" stop-color="#2563eb" stop-opacity="0.16" />
                      <stop offset="100%" stop-color="#2563eb" stop-opacity="0.02" />
                    </linearGradient>
                  </defs>
                  <line x1="58" y1="30" x2="692" y2="30" class="account-chart__grid" />
                  <line x1="58" y1="84" x2="692" y2="84" class="account-chart__grid" />
                  <line x1="58" y1="138" x2="692" y2="138" class="account-chart__grid" />
                  <line x1="58" y1="28" x2="58" y2="138" class="account-chart__axis" />
                  <line x1="58" y1="138" x2="692" y2="138" class="account-chart__axis" />
                  <polygon
                    :points="areaPoints(account.points)"
                    :fill="`url(#balance-fill-${account.accountId})`"
                    class="account-chart__area"
                  />
                  <polyline :points="polyline(account.points)" class="account-chart__line" />
                  <circle
                    v-for="point in svgPoints(account.points)"
                    :key="point.key"
                    :cx="point.x"
                    :cy="point.y"
                    r="2.2"
                    class="account-chart__dot"
                    :class="{ 'account-chart__dot--active': tooltip.visible && tooltip.key === point.key }"
                  />
                  <circle
                    v-for="point in svgPoints(account.points)"
                    :key="`${point.key}-hit`"
                    :cx="point.x"
                    :cy="point.y"
                    r="9"
                    class="account-chart__hit"
                    @mouseenter="showTooltip(account.username, point, $event)"
                    @mousemove="moveTooltip($event)"
                    @mouseleave="hideTooltip"
                  />
                  <text
                    v-for="tick in ticks(account.points)"
                    :key="tick.key"
                    :x="tick.x"
                    y="166"
                    class="account-chart__tick"
                    text-anchor="middle"
                  >
                    {{ tick.label }}
                  </text>
                  <text x="16" y="34" class="account-chart__scale">{{ maxLabel(account.points) }}</text>
                  <text x="16" y="88" class="account-chart__scale">{{ midLabel(account.points) }}</text>
                  <text x="16" y="138" class="account-chart__scale">{{ minLabel(account.points) }}</text>
                </svg>
              </div>
            </div>
          </article>
        </div>
      </el-card>
    </div>
    <div
      v-if="tooltip.visible"
      class="chart-tooltip"
      :style="{ left: `${tooltip.x}px`, top: `${tooltip.y}px` }"
    >
      <div class="chart-tooltip__name">{{ tooltip.username }}</div>
      <div class="chart-tooltip__row">
        <span>时间</span>
        <strong>{{ tooltip.time }}</strong>
      </div>
      <div class="chart-tooltip__row">
        <span>余额</span>
        <strong>{{ tooltip.balance }}</strong>
      </div>
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
const tooltip = ref({
  visible: false,
  username: '',
  time: '',
  balance: '',
  key: '',
  x: 0,
  y: 0
})

async function loadData() {
  loading.value = true
  try {
    const platformResponse = await listPlatformSummaries({ pageNo: 1, pageSize: 200 })
    const platforms = platformResponse.data.records as PlatformSummary[]
    const trendResponses = await Promise.all(platforms.map(platform =>
      getPlatformBalanceTrend(platform.platformId, { limit: 36 })
    ))
    platformTrends.value = trendResponses
      .map(response => response.data as PlatformBalanceTrend)
      .filter(trend => trend.accounts.length > 0)
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
  const range = max - min
  const left = 58
  const bottom = 138
  const width = 634
  const height = 108
  return points.map((point, index) => {
    const x = left + (points.length === 1 ? width : (index / (points.length - 1)) * width)
    const y = range === 0
      ? 84
      : bottom - ((Number(point.balance ?? 0) - min) / range) * height
    return {
      key: `${point.time}-${index}`,
      x,
      y,
      time: formatChartTime(point.time),
      balance: formatAmount(point.balance)
    }
  })
}

function areaPoints(points: PlatformBalanceTrendPoint[]) {
  const renderedPoints = svgPoints(points)
  if (renderedPoints.length === 0) {
    return ''
  }
  const baseline = 138
  const first = renderedPoints[0]
  const last = renderedPoints[renderedPoints.length - 1]
  const line = renderedPoints.map(point => `${point.x},${point.y}`).join(' ')
  return `${first.x},${baseline} ${line} ${last.x},${baseline}`
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

function midLabel(points: PlatformBalanceTrendPoint[]) {
  const balances = points.map(point => Number(point.balance ?? 0))
  if (balances.length === 0) {
    return '-'
  }
  const min = Math.min(...balances)
  const max = Math.max(...balances)
  return ((min + max) / 2).toFixed(2)
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

function showTooltip(
  username: string,
  point: { key: string; time: string; balance: string },
  event: MouseEvent
) {
  tooltip.value.visible = true
  tooltip.value.username = username
  tooltip.value.time = point.time
  tooltip.value.balance = point.balance
  tooltip.value.key = point.key
  moveTooltip(event)
}

function moveTooltip(event: MouseEvent) {
  const width = 190
  const height = 104
  const gap = 14
  const viewportPadding = 12
  let x = event.clientX + gap
  let y = event.clientY - height - gap
  if (x + width > window.innerWidth - viewportPadding) {
    x = event.clientX - width - gap
  }
  if (y < viewportPadding) {
    y = event.clientY + gap
  }
  tooltip.value.x = Math.max(viewportPadding, x)
  tooltip.value.y = Math.max(viewportPadding, y)
}

function hideTooltip() {
  tooltip.value.visible = false
  tooltip.value.key = ''
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
  gap: 18px;
  margin-top: 20px;
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
  gap: 18px;
}

.account-chart {
  min-width: 0;
  overflow: hidden;
  border: 1px solid #e6edf5;
  border-radius: 8px;
  background: linear-gradient(180deg, #ffffff 0%, #fbfdff 100%);
  padding: 16px 16px 12px;
}

.account-chart__header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  margin-bottom: 8px;
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
  border-radius: 999px;
  background: #eff6ff;
  color: #1d4ed8;
  font-size: 12px;
  font-weight: 700;
  line-height: 24px;
  padding: 0 10px;
}

.account-chart__body {
  min-height: 176px;
}

.account-chart__plot {
  position: relative;
  min-height: 176px;
}

.account-chart__svg {
  display: block;
  width: 100%;
  height: 176px;
}

.account-chart__axis {
  stroke: #d7e0ec;
  stroke-width: 1;
}

.account-chart__grid {
  stroke: #eef3f8;
  stroke-width: 1;
}

.account-chart__area {
  pointer-events: none;
}

.account-chart__line {
  fill: none;
  stroke: #2f6fec;
  stroke-linecap: round;
  stroke-linejoin: round;
  stroke-width: 2.25;
  filter: drop-shadow(0 2px 3px rgb(37 99 235 / 12%));
  pointer-events: none;
}

.account-chart__dot {
  fill: #2f6fec;
  opacity: 0.18;
  stroke: #fff;
  stroke-width: 1.5;
  pointer-events: none;
  transition: opacity 0.15s ease, r 0.15s ease, stroke-width 0.15s ease;
}

.account-chart__dot--active {
  opacity: 1;
  r: 4.5;
  stroke-width: 2;
}

.account-chart__hit {
  fill: transparent;
  cursor: crosshair;
  pointer-events: all;
}

.account-chart__tick,
.account-chart__scale {
  fill: #71839b;
  font-size: 11px;
}

@media (max-width: 1500px) {
  .account-chart-list {
    grid-template-columns: minmax(0, 1fr);
  }
}

.chart-tooltip {
  position: fixed;
  z-index: 3000;
  width: 190px;
  border: 1px solid #dbeafe;
  border-radius: 8px;
  background: #fff;
  box-shadow: 0 12px 28px rgb(15 23 42 / 14%);
  color: #0f172a;
  font-size: 12px;
  pointer-events: none;
}

.chart-tooltip__name {
  padding: 8px 10px;
  border-bottom: 1px solid #edf2f7;
  font-weight: 700;
}

.chart-tooltip__row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
  padding: 7px 10px;
}

.chart-tooltip__row span {
  color: #64748b;
}

.chart-tooltip__row strong {
  color: #2563eb;
  font-weight: 700;
}
</style>
