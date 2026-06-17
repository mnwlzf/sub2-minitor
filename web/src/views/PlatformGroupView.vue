<template>
  <div>
    <h1 class="page-title">分组查看</h1>
    <div class="page-subtitle">按平台展示最新采集到的分组与倍率。</div>

    <el-card class="page-card group-page__card">
      <template #header>
        <div class="card-header">
          <span>平台分组</span>
          <div class="toolbar">
            <el-input v-model="filters.keyword" clearable placeholder="搜索平台名称 / 地址" class="toolbar__input" />
            <el-select v-model="filters.isEnabled" clearable placeholder="状态" class="toolbar__select">
              <el-option label="监控中" :value="true" />
              <el-option label="未监控" :value="false" />
            </el-select>
            <el-button :icon="Search" type="primary" @click="reload">搜索</el-button>
          </div>
        </div>
      </template>

      <div v-loading="loading" class="platform-list">
        <el-empty v-if="platforms.length === 0 && !loading" description="暂无分组数据" />
        <article v-for="platform in platforms" :key="platform.platformId" class="platform-card">
          <header class="platform-card__header">
            <div class="platform-card__identity">
              <div class="platform-card__mark">{{ initial(platform.platformName) }}</div>
              <div class="platform-card__title-block">
                <div class="platform-card__title-row">
                  <h2 class="platform-card__title">{{ platform.platformName }}</h2>
                  <el-tag effect="plain" size="small">{{ platformTypeLabel(platform.type) }}</el-tag>
                  <el-tag :type="platform.isEnabled ? 'success' : 'info'" effect="light" size="small">
                    {{ platform.isEnabled ? '监控中' : '未监控' }}
                  </el-tag>
                </div>
                <div class="platform-card__sub">{{ platform.baseUrl }}</div>
              </div>
            </div>
            <div class="platform-card__stats">
              <div class="stat-box stat-box--wide">
                <span>充值 / 到账</span>
                <strong>{{ formatAmount(platform.rechargeAmount) }} / {{ formatAmount(platform.receivedAmount) }}</strong>
              </div>
              <div class="stat-box">
                <span>折算比例</span>
                <strong>{{ formatRate(platform.deductRate) }}</strong>
              </div>
              <div class="stat-box">
                <span>分组数</span>
                <strong>{{ platform.groupCount }}</strong>
              </div>
              <div class="stat-box stat-box--time">
                <span>最后采集</span>
                <strong>{{ formatDateTime(platform.lastCollectTime) }}</strong>
              </div>
            </div>
          </header>

          <section class="group-list">
            <el-empty v-if="platform.groups.length === 0" description="暂无分组" />
            <article
              v-for="group in platform.groups"
              :key="group.groupName"
              class="group-item"
              :class="{ 'group-item--key': group.keyGroup }"
            >
              <div class="group-item__main">
                <div class="group-item__name">
                  {{ group.groupName }}
                  <el-tag v-if="group.keyGroup" type="warning" effect="light" size="small">
                    密钥 {{ group.keyCount || 0 }}
                  </el-tag>
                </div>
                <div class="group-item__time">{{ formatDateTime(group.collectTime) }}</div>
              </div>
              <div class="group-item__rates">
                <span>平台 {{ formatRate(group.currentRate) }}</span>
                <strong>实际 {{ formatRate(group.actualRate) }}</strong>
              </div>
            </article>
          </section>
        </article>
      </div>

      <div class="pager">
        <el-pagination
          v-model:current-page="pageNo"
          v-model:page-size="pageSize"
          :page-sizes="[10, 20, 50]"
          :total="total"
          layout="total, sizes, prev, pager, next"
          @current-change="reload"
          @size-change="handleSizeChange"
        />
      </div>
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import { Search } from '@element-plus/icons-vue'
import { listPlatformGroups, type PlatformGroupSummary, type PlatformType } from '@/api/monitor'

const loading = ref(false)
const platforms = ref<PlatformGroupSummary[]>([])
const total = ref(0)
const pageNo = ref(1)
const pageSize = ref(20)

const filters = reactive<{
  keyword: string
  isEnabled?: boolean
}>({
  keyword: '',
  isEnabled: undefined
})

async function reload() {
  loading.value = true
  try {
    const response = await listPlatformGroups({
      pageNo: pageNo.value,
      pageSize: pageSize.value,
      keyword: filters.keyword || undefined,
      isEnabled: filters.isEnabled
    })
    platforms.value = response.data.records
    total.value = response.data.total
  } finally {
    loading.value = false
  }
}

function handleSizeChange(size: number) {
  pageSize.value = size
  pageNo.value = 1
  void reload()
}

function initial(value?: string) {
  return (value || 'P').trim().slice(0, 1).toUpperCase()
}

function platformTypeLabel(type?: PlatformType) {
  if (type === 'newApi') {
    return 'NewApi'
  }
  return 'Sub2Api'
}

function formatRate(value?: number) {
  return Number(value ?? 0).toFixed(4)
}

function formatAmount(value?: number) {
  return Number(value ?? 0).toFixed(2)
}

function formatDateTime(value?: string) {
  if (!value) {
    return '未采集'
  }
  const date = new Date(value)
  if (Number.isNaN(date.getTime())) {
    return value
  }
  return date.toLocaleString('zh-CN', {
    year: 'numeric',
    month: '2-digit',
    day: '2-digit',
    hour: '2-digit',
    minute: '2-digit',
    hour12: false
  })
}

onMounted(reload)
</script>

<style scoped>
.group-page__card {
  margin-top: 24px;
}

.toolbar {
  display: flex;
  gap: 12px;
  align-items: center;
}

.toolbar__input {
  width: 260px;
}

.toolbar__select {
  width: 120px;
}

.platform-list {
  display: flex;
  min-height: 180px;
  flex-direction: column;
  gap: 14px;
}

.platform-card {
  border: 1px solid #e5e7eb;
  border-radius: 8px;
  background: #fff;
  padding: 16px;
  box-shadow: 0 8px 22px rgb(31 41 55 / 5%);
}

.platform-card__header {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 16px;
}

.platform-card__identity {
  display: flex;
  min-width: 0;
  align-items: center;
  gap: 12px;
}

.platform-card__mark {
  display: grid;
  width: 40px;
  height: 40px;
  flex: 0 0 auto;
  place-items: center;
  border-radius: 8px;
  background: #eef6ff;
  color: #2563eb;
  font-weight: 700;
}

.platform-card__title-block {
  min-width: 0;
}

.platform-card__title-row {
  display: flex;
  align-items: center;
  gap: 10px;
}

.platform-card__title {
  overflow: hidden;
  margin: 0;
  color: #0f172a;
  font-size: 16px;
  font-weight: 700;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.platform-card__sub {
  overflow: hidden;
  margin-top: 6px;
  color: #64748b;
  font-size: 12px;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.platform-card__stats {
  display: flex;
  flex: 0 0 auto;
  gap: 10px;
}

.stat-box {
  min-width: 96px;
  border-radius: 6px;
  background: #f8fafc;
  padding: 10px 12px;
  text-align: right;
}

.stat-box--time {
  min-width: 150px;
}

.stat-box--wide {
  min-width: 136px;
}

.stat-box span {
  display: block;
  color: #64748b;
  font-size: 12px;
}

.stat-box strong {
  display: block;
  margin-top: 6px;
  color: #111827;
  font-size: 14px;
}

.group-list {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 10px;
  margin-top: 16px;
}

.group-list :deep(.el-empty) {
  grid-column: 1 / -1;
  padding: 18px 0;
}

.group-item {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 10px;
  border: 1px solid #edf2f7;
  border-radius: 8px;
  background: #fbfdff;
  padding: 12px;
}

.group-item--key {
  border-color: #facc15;
  background: #fffbeb;
}

.group-item__main {
  min-width: 0;
}

.group-item__name {
  display: flex;
  overflow: hidden;
  align-items: center;
  gap: 6px;
  color: #0f172a;
  font-size: 13px;
  font-weight: 700;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.group-item__time {
  overflow: hidden;
  margin-top: 5px;
  color: #64748b;
  font-size: 12px;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.group-item__rates {
  display: flex;
  flex: 0 0 auto;
  flex-direction: column;
  align-items: flex-end;
  gap: 5px;
}

.group-item__rates span {
  color: #64748b;
  font-size: 12px;
}

.group-item__rates strong {
  border-radius: 6px;
  background: #ecfdf5;
  color: #047857;
  padding: 6px 8px;
  font-size: 13px;
}

.pager {
  display: flex;
  justify-content: flex-end;
  padding-top: 16px;
}
</style>
