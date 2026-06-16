<template>
  <div>
    <h1 class="page-title">平台管理</h1>
    <div class="page-subtitle">每个平台占一行，展示平台账号、余额和今日消耗。</div>

    <el-card class="page-card platform-page__card">
      <template #header>
        <div class="card-header">
          <span>平台列表</span>
          <div class="toolbar">
            <el-input v-model="filters.keyword" clearable placeholder="搜索平台名称 / 地址" class="toolbar__input" />
            <el-select v-model="filters.isEnabled" clearable placeholder="状态" class="toolbar__select">
              <el-option label="启用" :value="true" />
              <el-option label="停用" :value="false" />
            </el-select>
            <el-button :icon="Search" type="primary" @click="reload">搜索</el-button>
            <el-button :icon="Plus" type="success" @click="openCreate">添加平台</el-button>
          </div>
        </div>
      </template>

      <section class="consume-summary">
        <div class="consume-summary__main">
          <span class="consume-summary__label">今日总消耗</span>
          <div class="consume-summary__amounts">
            <span>平台 <strong>{{ formatPreciseAmount(todayConsumeSummary.platformDeduct, 4) }}</strong></span>
            <span>实际 <strong>{{ formatPreciseAmount(todayConsumeSummary.actualConsume, 4) }}</strong></span>
          </div>
          <div class="consume-summary__sub">平台扣减 / 实际扣减</div>
        </div>
        <div class="consume-summary__actions">
          <el-tag type="danger" effect="light">异常 {{ abnormalCount }}</el-tag>
          <el-button :icon="Refresh" type="primary" :loading="collectAllLoading" @click="collectAllVisiblePlatforms">
            全部采集
          </el-button>
        </div>
      </section>

      <div v-loading="loading" class="platform-list">
        <el-empty v-if="platforms.length === 0 && !loading" description="暂无平台" />
        <article v-for="platform in platforms" :key="platform.platformId" class="platform-card">
          <header class="platform-card__header">
            <div class="platform-card__identity">
              <div class="platform-card__mark">{{ platformInitial(platform.platformName) }}</div>
              <div class="platform-card__title-block">
                <div class="platform-card__title-row">
                  <h2 class="platform-card__title">{{ platform.platformName }}</h2>
                  <el-tag effect="plain" size="small">
                    {{ platformTypeLabel(platform.type) }}
                  </el-tag>
                  <el-tag :type="platform.isEnabled ? 'success' : 'info'" effect="light" size="small">
                    {{ platform.isEnabled ? '监控中' : '未监控' }}
                  </el-tag>
                </div>
                <div class="platform-card__sub">{{ platform.baseUrl }}</div>
              </div>
            </div>
            <div class="platform-card__actions">
              <el-button
                type="success"
                plain
                size="small"
                :loading="collectLoadingIds.has(platform.platformId)"
                @click="collectSinglePlatform(platform)"
              >
                采集
              </el-button>
              <div class="monitor-toggle">
                <span class="monitor-toggle__label">{{ platform.isEnabled ? '监控中' : '未监控' }}</span>
                <el-switch
                  v-model="platform.isEnabled"
                  :loading="toggleLoadingIds.has(platform.platformId)"
                  @change="handleMonitorToggle(platform, $event)"
                />
              </div>
              <el-button type="primary" text size="small" @click="openEditBySummary(platform)">编辑</el-button>
            </div>
          </header>

          <section class="platform-card__overview">
            <div class="overview-box">
              <span class="overview-box__label">最后采集</span>
              <strong class="overview-box__value overview-box__value--time">
                {{ formatDateTime(platform.lastCollectTime) }}
              </strong>
            </div>
            <div class="overview-box">
              <span class="overview-box__label">账号数</span>
              <strong class="overview-box__value">{{ platform.accountCount }}</strong>
            </div>
            <div class="overview-box">
              <span class="overview-box__label">总余额</span>
              <strong class="overview-box__value">{{ formatAmount(platform.totalBalance) }}</strong>
            </div>
            <div class="overview-box">
              <span class="overview-box__label">平台消耗</span>
              <strong class="overview-box__value overview-box__value--warn">
                {{ formatAmount(platform.totalPlatformDeduct) }}
              </strong>
            </div>
            <div class="overview-box">
              <span class="overview-box__label">实际消耗</span>
              <strong class="overview-box__value overview-box__value--danger">
                {{ formatAmount(platform.totalActualConsume) }}
              </strong>
            </div>
            <div class="overview-box">
              <span class="overview-box__label">充值 / 到账</span>
              <strong class="overview-box__value">
                {{ formatAmount(platform.rechargeAmount) }} / {{ formatAmount(platform.receivedAmount) }}
              </strong>
            </div>
          </section>

          <section class="platform-card__accounts">
            <div class="section-title">账号明细</div>
            <div class="account-list">
              <article v-for="account in platform.accounts" :key="account.accountId" class="account-item">
                <div class="account-item__main">
                  <div class="account-item__mark">{{ platformInitial(account.username) }}</div>
                  <div class="account-item__body">
                    <div class="account-item__name">{{ account.username }}</div>
                    <div class="account-item__model">{{ account.testModel || '未设置测试模型' }}</div>
                  </div>
                </div>
                <div class="account-item__stats">
                  <div class="account-stat">
                    <span class="account-stat__label">余额</span>
                    <span class="account-stat__value">{{ formatAmount(account.latestBalance) }}</span>
                  </div>
                  <div class="account-stat">
                    <span class="account-stat__label">今日消耗</span>
                    <span class="account-stat__value account-stat__value--warn">
                      {{ formatAmount(account.todayConsume) }}
                    </span>
                  </div>
                  <div class="account-stat">
                    <span class="account-stat__label">实际消耗</span>
                    <span class="account-stat__value account-stat__value--danger">
                      {{ formatAmount(account.actualConsume) }}
                    </span>
                  </div>
                </div>
              </article>
            </div>
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

    <el-dialog
      v-model="dialogVisible"
      :title="dialogMode === 'create' ? '添加平台' : '编辑平台'"
      width="720px"
      class="platform-dialog"
    >
      <el-form ref="formRef" :model="form" :rules="rules" label-width="96px" class="platform-form">
        <el-form-item label="平台名称" prop="name">
          <el-input v-model="form.name" placeholder="例如：NewAPI 主站" />
        </el-form-item>
        <el-form-item label="平台地址" prop="baseUrl">
          <el-input v-model="form.baseUrl" placeholder="https://example.com" />
        </el-form-item>
        <el-form-item label="平台类型" prop="type">
          <el-select v-model="form.type" placeholder="请选择平台类型">
            <el-option label="Sub2Api" value="sub2Api" />
            <el-option label="NewApi" value="newApi" />
          </el-select>
        </el-form-item>
        <div class="platform-form__grid">
          <el-form-item label="充值金额" prop="rechargeAmount">
            <el-input-number v-model="form.rechargeAmount" :precision="2" :min="0" controls-position="right" />
          </el-form-item>
          <el-form-item label="到账金额" prop="receivedAmount">
            <el-input-number v-model="form.receivedAmount" :precision="2" :min="0" controls-position="right" />
          </el-form-item>
        </div>
        <el-form-item label="是否监控">
          <el-switch
            v-model="form.isEnabled"
            active-text="监控"
            inactive-text="不监控"
            inline-prompt
          />
        </el-form-item>
      </el-form>

      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="saveForm">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import { ElMessage, type FormInstance, type FormRules } from 'element-plus'
import { Plus, Refresh, Search } from '@element-plus/icons-vue'
import {
  collectPlatform,
  listPlatformSummaries,
  savePlatform,
  updatePlatform,
  type Id,
  type PlatformSummary,
  type PlatformType
} from '@/api/monitor'

const loading = ref(false)
const saving = ref(false)
const toggleLoadingIds = ref(new Set<Id>())
const collectLoadingIds = ref(new Set<Id>())
const collectAllLoading = ref(false)
const platforms = ref<PlatformSummary[]>([])
const total = ref(0)
const pageNo = ref(1)
const pageSize = ref(20)
const dialogVisible = ref(false)
const dialogMode = ref<'create' | 'edit'>('create')
const formRef = ref<FormInstance>()

const filters = reactive<{
  keyword: string
  isEnabled?: boolean
}>({
  keyword: '',
  isEnabled: undefined
})

const form = reactive({
  id: '',
  name: '',
  baseUrl: '',
  type: 'sub2Api' as PlatformType,
  rechargeAmount: 0,
  receivedAmount: 0,
  isEnabled: true
})

const todayConsumeSummary = computed(() => platforms.value.reduce((summary, platform) => {
  summary.platformDeduct += Number(platform.totalPlatformDeduct ?? platform.totalTodayConsume ?? 0)
  summary.actualConsume += Number(platform.totalActualConsume ?? platform.totalTodayConsume ?? 0)
  return summary
}, {
  platformDeduct: 0,
  actualConsume: 0
}))

const abnormalCount = computed(() => platforms.value.filter(platform => {
  const platformDeduct = Number(platform.totalPlatformDeduct ?? platform.totalTodayConsume ?? 0)
  const actualConsume = Number(platform.totalActualConsume ?? platform.totalTodayConsume ?? 0)
  return platformDeduct < 0 || actualConsume < 0
}).length)

const rules: FormRules = {
  name: [{ required: true, message: '请输入平台名称', trigger: 'blur' }],
  baseUrl: [{ required: true, message: '请输入平台地址', trigger: 'blur' }],
  type: [{ required: true, message: '请选择平台类型', trigger: 'change' }],
  rechargeAmount: [{ required: true, message: '请输入充值金额', trigger: 'change' }],
  receivedAmount: [{ required: true, message: '请输入到账金额', trigger: 'change' }]
}

function resetForm() {
  form.id = ''
  form.name = ''
  form.baseUrl = ''
  form.type = 'sub2Api'
  form.rechargeAmount = 0
  form.receivedAmount = 0
  form.isEnabled = true
}

function openCreate() {
  dialogMode.value = 'create'
  resetForm()
  dialogVisible.value = true
}

function openEditBySummary(row: PlatformSummary) {
  dialogMode.value = 'edit'
  form.id = row.platformId
  form.name = row.platformName
  form.baseUrl = row.baseUrl
  form.type = row.type
  form.rechargeAmount = Number(row.rechargeAmount ?? 0)
  form.receivedAmount = Number(row.receivedAmount ?? 0)
  form.isEnabled = row.isEnabled
  dialogVisible.value = true
}

async function reload() {
  loading.value = true
  try {
    const response = await listPlatformSummaries({
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

async function saveForm() {
  saving.value = true
  try {
    const valid = await formRef.value?.validate().catch(() => false)
    if (!valid) {
      return
    }
    const payload = {
      name: form.name,
      baseUrl: form.baseUrl,
      type: form.type,
      rechargeAmount: form.rechargeAmount,
      receivedAmount: form.receivedAmount,
      isEnabled: form.isEnabled
    }
    if (dialogMode.value === 'create') {
      await savePlatform(payload)
      ElMessage.success('平台已添加')
    } else {
      await updatePlatform({ id: form.id, ...payload })
      ElMessage.success('平台已更新')
    }
    dialogVisible.value = false
    await reload()
  } finally {
    saving.value = false
  }
}

async function togglePlatformMonitor(platform: PlatformSummary, isEnabled: boolean) {
  const previousValue = !isEnabled
  toggleLoadingIds.value.add(platform.platformId)
  try {
    await updatePlatform({
      id: platform.platformId,
      name: platform.platformName,
      baseUrl: platform.baseUrl,
      type: platform.type,
      rechargeAmount: platform.rechargeAmount,
      receivedAmount: platform.receivedAmount,
      isEnabled
    })
    platform.isEnabled = isEnabled
    ElMessage.success(isEnabled ? '平台已加入监控' : '平台已停止监控')
  } catch (error) {
    platform.isEnabled = previousValue
    ElMessage.error('监控状态更新失败')
  } finally {
    toggleLoadingIds.value.delete(platform.platformId)
  }
}

function handleMonitorToggle(platform: PlatformSummary, value: string | number | boolean) {
  void togglePlatformMonitor(platform, Boolean(value))
}

async function collectSinglePlatform(platform: PlatformSummary) {
  collectLoadingIds.value.add(platform.platformId)
  try {
    await collectPlatform(platform.platformId)
    ElMessage.success('已开始采集该平台')
  } catch (error) {
    ElMessage.error('启动采集失败')
  } finally {
    collectLoadingIds.value.delete(platform.platformId)
  }
}

async function collectAllVisiblePlatforms() {
  collectAllLoading.value = true
  try {
    await Promise.all(platforms.value.map(platform => collectPlatform(platform.platformId)))
    ElMessage.success('已开始采集当前列表平台')
  } catch (error) {
    ElMessage.error('启动全部采集失败')
  } finally {
    collectAllLoading.value = false
  }
}

function handleSizeChange(size: number) {
  pageSize.value = size
  pageNo.value = 1
  void reload()
}

function platformInitial(name?: string) {
  return (name || 'P').trim().slice(0, 1).toUpperCase()
}

function formatAmount(value?: number) {
  return Number(value ?? 0).toFixed(2)
}

function formatPreciseAmount(value?: number, precision = 2) {
  return Number(value ?? 0).toFixed(precision)
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

function platformTypeLabel(type?: PlatformType) {
  if (type === 'newApi') {
    return 'NewApi'
  }
  return 'Sub2Api'
}

onMounted(reload)
</script>

<style scoped>
.platform-page__card {
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

.pager {
  display: flex;
  justify-content: flex-end;
  padding-top: 16px;
}

.consume-summary {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
  border-bottom: 1px solid #eef2f7;
  margin: -4px -20px 18px;
  padding: 0 20px 16px;
}

.consume-summary__main {
  min-width: 0;
}

.consume-summary__label {
  color: #64748b;
  font-size: 13px;
  font-weight: 700;
}

.consume-summary__amounts {
  display: flex;
  flex-wrap: wrap;
  gap: 14px;
  margin-top: 8px;
  color: #64748b;
  font-size: 13px;
}

.consume-summary__amounts strong {
  color: #0f172a;
  font-size: 14px;
}

.consume-summary__sub {
  margin-top: 6px;
  color: #94a3b8;
  font-size: 12px;
}

.consume-summary__actions {
  display: flex;
  flex: 0 0 auto;
  align-items: center;
  gap: 12px;
}

.platform-list {
  display: flex;
  flex-direction: column;
  gap: 14px;
  min-height: 180px;
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
  gap: 12px;
}

.platform-card__actions {
  display: flex;
  flex: 0 0 auto;
  align-items: center;
  gap: 12px;
}

.monitor-toggle {
  display: inline-flex;
  align-items: center;
  gap: 8px;
  color: #475569;
  font-size: 12px;
}

.monitor-toggle__label {
  min-width: 42px;
  text-align: right;
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

.platform-card__overview {
  display: grid;
  grid-template-columns: repeat(6, minmax(0, 1fr));
  gap: 10px;
  margin-top: 16px;
}

.overview-box {
  border-radius: 6px;
  background: #f8fafc;
  padding: 12px;
}

.overview-box__label {
  color: #64748b;
  font-size: 12px;
}

.overview-box__value {
  display: block;
  margin-top: 8px;
  color: #111827;
  font-size: 18px;
  line-height: 1.1;
}

.overview-box__value--warn {
  color: #b45309;
}

.overview-box__value--danger {
  color: #dc2626;
}

.overview-box__value--time {
  font-size: 14px;
  line-height: 1.25;
}

.platform-card__accounts {
  margin-top: 16px;
}

.section-title {
  margin-bottom: 10px;
  color: #0f172a;
  font-size: 13px;
  font-weight: 700;
}

.account-list {
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.account-item {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 14px;
  border: 1px solid #edf2f7;
  border-radius: 8px;
  background: #fbfdff;
  padding: 12px;
}

.account-item__main {
  display: flex;
  min-width: 0;
  align-items: center;
  gap: 10px;
}

.account-item__mark {
  display: grid;
  width: 34px;
  height: 34px;
  flex: 0 0 auto;
  place-items: center;
  border-radius: 8px;
  background: #f1f5f9;
  color: #334155;
  font-weight: 700;
}

.account-item__body {
  min-width: 0;
}

.account-item__name {
  overflow: hidden;
  color: #0f172a;
  font-weight: 700;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.account-item__model {
  overflow: hidden;
  margin-top: 4px;
  color: #64748b;
  font-size: 12px;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.account-item__stats {
  display: flex;
  gap: 10px;
}

.account-stat {
  min-width: 110px;
  border-radius: 6px;
  background: #f8fafc;
  padding: 10px 12px;
  text-align: right;
}

.account-stat__label {
  display: block;
  color: #64748b;
  font-size: 12px;
}

.account-stat__value {
  display: block;
  margin-top: 6px;
  color: #111827;
  font-size: 16px;
  font-weight: 700;
}

.account-stat__value--warn {
  color: #b45309;
}

.account-stat__value--danger {
  color: #dc2626;
}

.platform-dialog :deep(.el-dialog__body) {
  padding-top: 12px;
}

.platform-form__grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 16px;
}

.platform-form__grid :deep(.el-input-number) {
  width: 100%;
}
</style>
