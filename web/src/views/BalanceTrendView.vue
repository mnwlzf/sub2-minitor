<template>
  <div>
    <div class="page-header">
      <div>
        <h1 class="page-title">余额变化曲线</h1>
        <div class="page-subtitle">按平台查看余额历史，时间轴与当前采集任务的 Cron 节奏一致。</div>
      </div>
      <div class="page-actions">
        <el-select v-model="selectedPlatformId" placeholder="选择平台" filterable class="page-actions__select" @change="loadTrend">
          <el-option
            v-for="platform in platforms"
            :key="platform.platformId"
            :label="platform.platformName"
            :value="platform.platformId"
          />
        </el-select>
        <el-button :icon="Refresh" :loading="loading" @click="loadData">刷新</el-button>
      </div>
    </div>

    <el-card class="page-card trend-page__card">
      <template #header>
        <div class="card-header">
          <span>{{ trend?.platformName || '余额曲线' }}</span>
          <span class="trend-page__cron">Cron {{ trend?.cronExpression || '-' }}</span>
        </div>
      </template>

      <div v-loading="loading" class="trend-page__chart">
        <el-empty v-if="!loading && trendPoints.length === 0" description="暂无余额历史" />
        <svg v-else class="trend-page__svg" viewBox="0 0 720 280" preserveAspectRatio="none" role="img">
          <line x1="42" y1="24" x2="42" y2="220" class="trend-page__axis" />
          <line x1="42" y1="220" x2="700" y2="220" class="trend-page__axis" />
          <polyline :points="polyline" class="trend-page__line" />
          <circle
            v-for="point in svgPoints"
            :key="point.key"
            :cx="point.x"
            :cy="point.y"
            r="4"
            class="trend-page__dot"
          >
            <title>{{ point.label }}</title>
          </circle>
          <text
            v-for="tick in ticks"
            :key="tick.key"
            :x="tick.x"
            y="248"
            class="trend-page__tick"
            text-anchor="middle"
          >
            {{ tick.label }}
          </text>
          <text x="14" y="32" class="trend-page__scale">{{ maxLabel }}</text>
          <text x="14" y="220" class="trend-page__scale">{{ minLabel }}</text>
        </svg>
      </div>
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { Refresh } from '@element-plus/icons-vue'
import { getPlatformBalanceTrend, listPlatformSummaries, type Id, type PlatformBalanceTrend, type PlatformSummary } from '@/api/monitor'

const loading = ref(false)
const platforms = ref<PlatformSummary[]>([])
const selectedPlatformId = ref<Id>('')
const trend = ref<PlatformBalanceTrend>()

const trendPoints = computed(() => trend.value?.points ?? [])

const svgPoints = computed(() => {
  const points = trendPoints.value
  if (points.length === 0) {
    return []
  }
  const balances = points.map(point => Number(point.balance ?? 0))
  const min = Math.min(...balances)
  const max = Math.max(...balances)
  const range = max - min || 1
  const width = 658
  const height = 196
  return points.map((point, index) => {
    const x = 42 + (points.length === 1 ? width : (index / (points.length - 1)) * width)
    const y = 220 - ((Number(point.balance ?? 0) - min) / range) * height
    return {
      key: `${point.time}-${index}`,
      x,
      y,
      label: `${formatChartTime(point.time)} ${formatAmount(point.balance)}`
    }
  })
})

const polyline = computed(() => svgPoints.value.map(point => `${point.x},${point.y}`).join(' '))

const ticks = computed(() => {
  const points = trendPoints.value
  if (points.length === 0) {
    return []
  }
  const indexes = Array.from(new Set([0, Math.floor((points.length - 1) / 2), points.length - 1]))
  return indexes.map(index => ({
    key: `${points[index].time}-${index}`,
    x: svgPoints.value[index].x,
    label: formatChartTime(points[index].time)
  }))
})

const minLabel = computed(() => {
  const balances = trendPoints.value.map(point => Number(point.balance ?? 0))
  return balances.length === 0 ? '-' : Math.min(...balances).toFixed(2)
})

const maxLabel = computed(() => {
  const balances = trendPoints.value.map(point => Number(point.balance ?? 0))
  return balances.length === 0 ? '-' : Math.max(...balances).toFixed(2)
})

async function loadData() {
  loading.value = true
  try {
    const response = await listPlatformSummaries({ pageNo: 1, pageSize: 200 })
    platforms.value = response.data.records
    if (!selectedPlatformId.value && platforms.value.length > 0) {
      selectedPlatformId.value = platforms.value[0].platformId
    }
    await loadTrend()
  } finally {
    loading.value = false
  }
}

async function loadTrend() {
  if (!selectedPlatformId.value) {
    trend.value = undefined
    return
  }
  const response = await getPlatformBalanceTrend(selectedPlatformId.value, { limit: 80 })
  trend.value = response.data
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

.page-actions {
  display: flex;
  align-items: center;
  gap: 12px;
}

.page-actions__select {
  width: 260px;
}

.trend-page__card {
  margin-top: 24px;
}

.trend-page__cron {
  color: #64748b;
  font-family: ui-monospace, SFMono-Regular, Consolas, monospace;
  font-size: 12px;
}

.trend-page__chart {
  min-height: 340px;
  border: 1px solid #edf2f7;
  border-radius: 8px;
  background: #fbfdff;
  padding: 12px;
}

.trend-page__svg {
  display: block;
  width: 100%;
  height: 280px;
}

.trend-page__axis {
  stroke: #dbe3ee;
  stroke-width: 1;
}

.trend-page__line {
  fill: none;
  stroke: #2563eb;
  stroke-linecap: round;
  stroke-linejoin: round;
  stroke-width: 3;
}

.trend-page__dot {
  fill: #fff;
  stroke: #2563eb;
  stroke-width: 2;
}

.trend-page__tick,
.trend-page__scale {
  fill: #64748b;
  font-size: 11px;
}
</style>
