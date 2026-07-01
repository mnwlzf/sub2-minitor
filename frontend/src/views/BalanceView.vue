<template>
  <AppShell title="余额查看" subtitle="按平台查看账号余额历史，数据来自最近的余额采集记录。">
    <template #actions>
      <el-button :loading="loading" @click="loadBalances">刷新</el-button>
    </template>

    <div class="balance-page-head">
      <h2>余额查看</h2>
      <p>按平台查看账号余额历史，X 轴按实际采集时间展示，Y 轴为余额金额。</p>
    </div>

    <el-card shadow="never" class="balance-filter-card">
      <div class="balance-filter-row">
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
          class="balance-date-picker"
          @change="handleDateRangeChange"
        />
      </div>
    </el-card>

    <div v-loading="loading" class="balance-platform-list">
      <el-card v-for="platform in platforms" :key="platform.platformId" shadow="never" class="balance-platform-card">
        <template #header>
          <div class="balance-platform-head">
            <div>
              <h3>{{ platform.platformName }}</h3>
              <p>{{ platform.baseUrl }}</p>
            </div>
            <span>采集任务 -</span>
          </div>
        </template>

        <div class="balance-account-grid">
          <div v-for="account in platform.accounts" :key="accountKey(account)" class="balance-chart-card">
            <div class="balance-chart-head">
              <strong>{{ account.accountIdentity || '未知账号' }}</strong>
              <el-tag type="primary" effect="light">当前余额 {{ formatMoney(account.currentBalance) }}</el-tag>
            </div>
            <BalanceChart :points="account.points" />
            <div class="balance-chart-summary">
              <span>今日消耗 {{ formatMoney(account.todayConsumption) }}</span>
              <span>今日到账 {{ formatMoney(account.todayRecharge) }}</span>
            </div>
          </div>
        </div>

        <el-empty v-if="!platform.accounts?.length" description="暂无余额记录" :image-size="120" />
      </el-card>
    </div>
  </AppShell>
</template>

<script setup lang="ts">
import * as echarts from 'echarts'
import { computed, defineComponent, h, nextTick, onBeforeUnmount, onMounted, reactive, ref, watch } from 'vue'
import { ElMessage } from 'element-plus'
import AppShell from '../components/AppShell.vue'
import { listBalanceHistory } from '../api/balance'
import type { BalancePoint, PlatformBalanceItem } from '../types/balance'
import type { AccountBalanceItem } from '../types/balance'

type RangeMode = 'today' | 'threeDays' | 'sevenDays' | 'custom'

const loading = ref(false)
const platforms = ref<PlatformBalanceItem[]>([])
const filters = reactive<{
  keyword: string
  enabled: boolean | null
  rangeMode: RangeMode
  dateRange: [string, string]
}>({
  keyword: '',
  enabled: null,
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

const loadBalances = async () => {
  loading.value = true
  try {
    const [startDate, endDate] = filters.dateRange
    const data = await listBalanceHistory({
      keyword: filters.keyword || undefined,
      enabled: filters.enabled,
      startDate: startDate || undefined,
      endDate: endDate || undefined,
    })
    platforms.value = data.items ?? []
  } catch (error) {
    ElMessage.error('加载余额失败')
  } finally {
    loading.value = false
  }
}

const handleRangeModeChange = () => {
  syncDateRangeByMode()
  loadBalances()
}

const handleDateRangeChange = () => {
  filters.rangeMode = 'custom'
  loadBalances()
}

const formatMoney = (value?: number | null) => Number(value ?? 0).toFixed(2)
const accountKey = (account: AccountBalanceItem) => `${account.accountId ?? 'unknown'}:${account.accountIdentity ?? ''}`
const isToday = (value: string | number | Date) => {
  const date = new Date(value)
  if (Number.isNaN(date.getTime())) {
    return false
  }
  const today = new Date()
  return (
    date.getFullYear() === today.getFullYear() &&
    date.getMonth() === today.getMonth() &&
    date.getDate() === today.getDate()
  )
}
const formatChartTime = (value: string | number | Date) => {
  const date = new Date(value)
  if (Number.isNaN(date.getTime())) {
    return String(value)
  }
  const month = `${date.getMonth() + 1}`.padStart(2, '0')
  const day = `${date.getDate()}`.padStart(2, '0')
  const hour = `${date.getHours()}`.padStart(2, '0')
  const minute = `${date.getMinutes()}`.padStart(2, '0')
  if (isToday(value)) {
    return `${hour}:${minute}`
  }
  return `${month}月${day}日 ${hour}:${minute}`
}

const BalanceChart = defineComponent({
  props: {
    points: {
      type: Array<BalancePoint>,
      required: true,
    },
  },
  setup(props) {
    const chartRef = ref<HTMLDivElement>()
    let chart: echarts.ECharts | null = null

    const chartCategories = computed(() => props.points.map((point) => point.collectedAt))
    const chartData = computed(() => props.points.map((point) => Number(point.balance ?? 0)))
    const todayRange = computed(() => {
      const todayIndexes = chartCategories.value
        .map((value, index) => (isToday(value) ? index : -1))
        .filter((index) => index >= 0)
      if (todayIndexes.length) {
        return {
          startValue: todayIndexes[0],
          endValue: todayIndexes[todayIndexes.length - 1],
        }
      }
      return {
        startValue: Math.max(chartCategories.value.length - 12, 0),
        endValue: Math.max(chartCategories.value.length - 1, 0),
      }
    })

    const renderChart = async () => {
      await nextTick()
      if (!chartRef.value) return
      if (!chart) {
        chart = echarts.init(chartRef.value)
      }
      chart.setOption({
        grid: { left: 44, right: 18, top: 24, bottom: 28 },
        tooltip: {
          trigger: 'axis',
          formatter: (params: unknown) => {
            const items = Array.isArray(params) ? params : [params]
            const first = items[0] as { axisValue?: string } | undefined
            const title = first?.axisValue ? formatChartTime(first.axisValue) : ''
            const lines = items.map((item) => {
              const point = item as { marker?: string; seriesName?: string; value?: number }
              return `${point.marker ?? ''}${point.seriesName ?? '余额'}：${Number(point.value ?? 0).toFixed(4)}`
            })
            return [title, ...lines].join('<br/>')
          },
          valueFormatter: (value: number) => value.toFixed(4),
        },
        xAxis: {
          type: 'category',
          data: chartCategories.value,
          boundaryGap: false,
          axisLine: { lineStyle: { color: '#d8e2ef' } },
          axisTick: { alignWithLabel: true },
          axisLabel: {
            color: '#53627c',
            hideOverlap: true,
            formatter: (value: string) => formatChartTime(value),
          },
        },
        yAxis: {
          type: 'value',
          name: '余额',
          scale: true,
          axisLabel: { color: '#53627c' },
          splitLine: { lineStyle: { color: '#edf2f7' } },
        },
        dataZoom: [
          {
            type: 'inside',
            xAxisIndex: 0,
            filterMode: 'none',
            zoomOnMouseWheel: true,
            moveOnMouseMove: true,
            moveOnMouseWheel: true,
            startValue: todayRange.value.startValue,
            endValue: todayRange.value.endValue,
          },
        ],
        series: [
          {
            name: '余额',
            type: 'line',
            data: chartData.value,
            showSymbol: chartData.value.length <= 12,
            smooth: false,
            lineStyle: { color: '#3478ff', width: 3 },
            areaStyle: { color: 'rgba(52, 120, 255, 0.14)' },
          },
        ],
      })
    }

    const resizeChart = () => chart?.resize()

    watch(() => props.points, renderChart, { deep: true })
    onMounted(() => {
      renderChart()
      window.addEventListener('resize', resizeChart)
    })
    onBeforeUnmount(() => {
      window.removeEventListener('resize', resizeChart)
      chart?.dispose()
      chart = null
    })

    return () => h('div', { ref: chartRef, class: 'balance-chart' })
  },
})

onMounted(() => {
  syncDateRangeByMode()
  loadBalances()
})
</script>
