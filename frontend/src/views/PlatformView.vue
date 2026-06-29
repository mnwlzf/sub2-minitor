<template>
  <AppShell title="平台管理" subtitle="每个平台占一行，展示平台账号、余额和今日消耗。">
    <el-card shadow="never" class="platform-panel">
      <template #header>
        <div class="platform-panel-head">
          <span>平台列表</span>
          <div class="platform-toolbar">
            <el-input
              v-model="filters.keyword"
              clearable
              placeholder="搜索平台名称 / 地址"
              class="platform-search"
              @keyup.enter="loadPlatforms"
            />
            <el-select v-model="filters.enabled" clearable placeholder="状态" class="platform-status-filter">
              <el-option label="启用" :value="true" />
              <el-option label="停用" :value="false" />
            </el-select>
            <el-button type="primary" @click="loadPlatforms">搜索</el-button>
            <el-button type="success" @click="openCreate">添加平台</el-button>
          </div>
        </div>
      </template>

      <div class="platform-summary">
        <div>
          <div class="summary-title">今日总消耗</div>
          <div class="summary-line">
            <span>平台 {{ formatMoney(summary.platformConsumption) }}</span>
            <span>实际 {{ formatMoney(summary.actualConsumption) }}</span>
          </div>
          <div class="summary-sub">平台扣减 / 实际扣减</div>
        </div>
        <div class="summary-actions">
          <el-tag type="danger" effect="light">异常 {{ summary.abnormalCount }}</el-tag>
          <el-button :loading="loading" type="primary" @click="collectAll">全部采集</el-button>
        </div>
      </div>

      <div v-loading="loading" class="platform-list">
        <div v-for="platform in platformList" :key="platform.id" class="platform-item">
          <div class="platform-item-head">
            <div class="platform-title">
              <div class="platform-avatar">{{ platformAvatar(platform.platformName) }}</div>
              <div>
                <div class="platform-name-line">
                  <span class="platform-name">{{ platform.platformName }}</span>
                  <el-tag size="small">{{ platform.type }}</el-tag>
                  <el-tag :type="platform.enabled ? 'success' : 'info'" size="small">
                    {{ platform.enabled ? '监控中' : '未监控' }}
                  </el-tag>
                </div>
                <div class="platform-url">{{ platform.baseUrl }}</div>
              </div>
            </div>
            <div class="platform-actions">
              <el-button size="small" type="success" plain @click="handleCollect(platform)">采集</el-button>
              <span>{{ platform.enabled ? '监控中' : '未监控' }}</span>
              <el-switch
                :model-value="platform.enabled"
                @change="(value: boolean) => handleToggle(platform, value)"
              />
              <el-button link type="primary" @click="openEdit(platform)">编辑</el-button>
            </div>
          </div>

          <div class="platform-metrics">
            <div class="metric-box">
              <span>账号数</span>
              <strong>{{ platform.accountCount ?? 0 }}</strong>
            </div>
            <div class="metric-box">
              <span>总余额</span>
              <strong>{{ formatMoney(platform.totalBalance) }}</strong>
            </div>
            <div class="metric-box">
              <span>平台消耗</span>
              <strong class="danger">{{ formatMoney(platform.platformConsumption) }}</strong>
            </div>
            <div class="metric-box">
              <span>实际消耗</span>
              <strong class="danger">{{ formatMoney(platform.actualConsumption) }}</strong>
            </div>
            <div class="metric-box">
              <span>充值 / 到账</span>
              <strong>{{ formatMoney(platform.rechargeAmount) }} / {{ formatMoney(platform.arrivalAmount) }}</strong>
            </div>
          </div>

          <div class="platform-account">
            <div class="account-section-title">账号明细</div>
            <div class="account-placeholder">
              <span>最近采集</span>
              <strong>{{ platform.lastCollectedAt ? formatDate(platform.lastCollectedAt) : '未采集' }}</strong>
            </div>
          </div>
        </div>
      </div>
    </el-card>

    <el-dialog v-model="dialogVisible" :title="dialogTitle" width="620px" destroy-on-close>
      <el-form ref="formRef" class="task-form" :model="formState" :rules="rules" label-width="110px">
        <el-form-item label="平台名称" prop="platformName">
          <el-input v-model="formState.platformName" placeholder="例如 咕咕鸟" />
        </el-form-item>
        <el-form-item label="平台地址" prop="baseUrl">
          <el-input v-model="formState.baseUrl" placeholder="https://example.com" />
        </el-form-item>
        <el-form-item label="平台类型" prop="type">
          <el-select v-model="formState.type" placeholder="请选择平台类型">
            <el-option label="NewApi" value="NEWAPI" />
            <el-option label="Sub2Api" value="SUB2API" />
          </el-select>
        </el-form-item>
        <el-form-item label="状态" prop="enabled">
          <el-switch v-model="formState.enabled" active-text="启用" inactive-text="停用" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="submitLoading" @click="submitForm">保存</el-button>
      </template>
    </el-dialog>
  </AppShell>
</template>

<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import { ElMessage, type FormInstance, type FormRules } from 'element-plus'
import AppShell from '../components/AppShell.vue'
import type { PlatformForm, PlatformItem, PlatformSummary } from '../types/platform'
import {
  collectPlatform,
  createPlatform,
  disablePlatform,
  enablePlatform,
  listPlatforms,
  updatePlatform,
} from '../api/platform'

const loading = ref(false)
const submitLoading = ref(false)
const dialogVisible = ref(false)
const editingId = ref<number | null>(null)
const platformList = ref<PlatformItem[]>([])
const summary = ref<PlatformSummary>({
  platformCount: 0,
  enabledCount: 0,
  accountCount: 0,
  abnormalCount: 0,
  platformConsumption: 0,
  actualConsumption: 0,
})
const formRef = ref<FormInstance>()

const filters = reactive<{ keyword: string; enabled: boolean | null }>({
  keyword: '',
  enabled: null,
})

const defaultForm: PlatformForm = {
  platformName: '',
  baseUrl: '',
  type: 'NEWAPI',
  enabled: true,
}

const formState = reactive<PlatformForm>({ ...defaultForm })

const rules: FormRules<PlatformForm> = {
  platformName: [{ required: true, message: '请输入平台名称', trigger: 'blur' }],
  baseUrl: [{ required: true, message: '请输入平台地址', trigger: 'blur' }],
  type: [{ required: true, message: '请选择平台类型', trigger: 'change' }],
}

const dialogTitle = computed(() => (editingId.value ? '编辑平台' : '添加平台'))

const loadPlatforms = async () => {
  loading.value = true
  try {
    const data = await listPlatforms({
      keyword: filters.keyword || undefined,
      enabled: filters.enabled,
    })
    platformList.value = data.items ?? []
    summary.value = data.summary
  } catch (error) {
    ElMessage.error('加载平台列表失败')
  } finally {
    loading.value = false
  }
}

const openCreate = () => {
  Object.assign(formState, defaultForm)
  editingId.value = null
  dialogVisible.value = true
}

const openEdit = (platform: PlatformItem) => {
  editingId.value = platform.id ?? null
  Object.assign(formState, {
    platformName: platform.platformName,
    baseUrl: platform.baseUrl,
    type: platform.type,
    enabled: platform.enabled,
  })
  dialogVisible.value = true
}

const submitForm = async () => {
  if (!formRef.value) return
  const valid = await formRef.value.validate().catch(() => false)
  if (!valid) return

  submitLoading.value = true
  try {
    if (editingId.value) {
      await updatePlatform(editingId.value, { ...formState })
      ElMessage.success('平台已更新')
    } else {
      await createPlatform({ ...formState })
      ElMessage.success('平台已添加')
    }
    dialogVisible.value = false
    await loadPlatforms()
  } catch (error) {
    ElMessage.error('保存平台失败')
  } finally {
    submitLoading.value = false
  }
}

const handleToggle = async (platform: PlatformItem, enabled: boolean) => {
  try {
    if (!platform.id) return
    if (enabled) {
      await enablePlatform(platform.id)
    } else {
      await disablePlatform(platform.id)
    }
    ElMessage.success(enabled ? '平台已启用' : '平台已停用')
    await loadPlatforms()
  } catch (error) {
    ElMessage.error('更新平台状态失败')
  }
}

const handleCollect = async (platform: PlatformItem) => {
  try {
    if (!platform.id) return
    await collectPlatform(platform.id)
    ElMessage.success('已触发采集')
    await loadPlatforms()
  } catch (error) {
    ElMessage.error('采集失败')
  }
}

const collectAll = async () => {
  for (const platform of platformList.value.filter((item) => item.enabled)) {
    await handleCollect(platform)
  }
}

const formatMoney = (value?: number) => Number(value ?? 0).toFixed(2)
const platformAvatar = (name: string) => name?.slice(0, 1) || 'P'
const formatDate = (value: string) => value.replace('T', ' ').slice(0, 16)

onMounted(loadPlatforms)
</script>
