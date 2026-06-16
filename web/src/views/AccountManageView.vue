<template>
  <div>
    <h1 class="page-title">账号管理</h1>
    <div class="page-subtitle">按平台维护采集账号，密码不会回显，编辑时留空表示不修改。</div>

    <el-card class="page-card account-page__card">
      <template #header>
        <div class="card-header">
          <span>账号列表</span>
          <div class="toolbar">
            <el-select v-model="filters.platformId" clearable filterable placeholder="筛选平台" class="toolbar__select">
              <el-option
                v-for="platform in platforms"
                :key="platform.id"
                :label="platform.name"
                :value="platform.id"
              />
            </el-select>
            <el-input v-model="filters.keyword" clearable placeholder="搜索账号" class="toolbar__input" />
            <el-button :icon="Search" type="primary" @click="reload">搜索</el-button>
            <el-button :icon="Plus" type="success" @click="openCreate">添加账号</el-button>
          </div>
        </div>
      </template>

      <div v-loading="loading" class="account-list">
        <el-empty v-if="accounts.length === 0 && !loading" description="暂无账号" />
        <article v-for="account in accounts" :key="account.id" class="account-card">
          <div class="account-card__main">
            <div class="account-card__mark">{{ initial(account.username) }}</div>
            <div class="account-card__body">
              <div class="account-card__title-row">
                <h2 class="account-card__title">{{ account.username }}</h2>
                <el-tag effect="plain" size="small">{{ platformTypeLabel(account.platformType) }}</el-tag>
              </div>
              <div class="account-card__meta">{{ account.platformName || '未关联平台' }}</div>
            </div>
          </div>
          <div class="account-card__detail">
            <div class="detail-item">
              <span>测试模型</span>
              <strong>{{ account.testModel || '未设置' }}</strong>
            </div>
            <div class="detail-item">
              <span>创建时间</span>
              <strong>{{ formatDateTime(account.createTime) }}</strong>
            </div>
          </div>
          <div class="account-card__actions">
            <el-button type="primary" text size="small" @click="openEdit(account)">编辑</el-button>
            <el-button type="danger" text size="small" @click="removeAccount(account)">删除</el-button>
          </div>
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
      :title="dialogMode === 'create' ? '添加账号' : '编辑账号'"
      width="640px"
      class="account-dialog"
    >
      <el-form ref="formRef" :model="form" :rules="rules" label-width="96px">
        <el-form-item label="所属平台" prop="platformId">
          <el-select v-model="form.platformId" filterable placeholder="请选择平台">
            <el-option
              v-for="platform in platforms"
              :key="platform.id"
              :label="`${platform.name} / ${platformTypeLabel(platform.type)}`"
              :value="platform.id"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="账号" prop="username">
          <el-input v-model="form.username" placeholder="请输入账号" />
        </el-form-item>
        <el-form-item label="密码" prop="password">
          <el-input
            v-model="form.password"
            show-password
            type="password"
            :placeholder="dialogMode === 'create' ? '请输入密码' : '留空则不修改密码'"
          />
        </el-form-item>
        <el-form-item label="测试模型">
          <el-input v-model="form.testModel" placeholder="例如：gpt-4o-mini" />
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
import { ElMessage, ElMessageBox, type FormInstance, type FormRules } from 'element-plus'
import { Plus, Search } from '@element-plus/icons-vue'
import {
  deleteAccount,
  listAccounts,
  listPlatforms,
  saveAccount,
  updateAccount,
  type Account,
  type Platform,
  type PlatformType
} from '@/api/monitor'

const loading = ref(false)
const saving = ref(false)
const accounts = ref<Account[]>([])
const platforms = ref<Platform[]>([])
const total = ref(0)
const pageNo = ref(1)
const pageSize = ref(20)
const dialogVisible = ref(false)
const dialogMode = ref<'create' | 'edit'>('create')
const formRef = ref<FormInstance>()

const filters = reactive<{
  platformId?: number
  keyword: string
}>({
  platformId: undefined,
  keyword: ''
})

const form = reactive({
  id: 0,
  platformId: undefined as number | undefined,
  username: '',
  password: '',
  testModel: ''
})

const rules = computed<FormRules>(() => ({
  platformId: [{ required: true, message: '请选择平台', trigger: 'change' }],
  username: [{ required: true, message: '请输入账号', trigger: 'blur' }],
  password: dialogMode.value === 'create'
    ? [{ required: true, message: '请输入密码', trigger: 'blur' }]
    : []
}))

async function loadPlatforms() {
  const response = await listPlatforms({ pageNo: 1, pageSize: 200 })
  platforms.value = response.data.records
}

async function reload() {
  loading.value = true
  try {
    const response = await listAccounts({
      pageNo: pageNo.value,
      pageSize: pageSize.value,
      platformId: filters.platformId,
      keyword: filters.keyword || undefined
    })
    accounts.value = response.data.records
    total.value = response.data.total
  } finally {
    loading.value = false
  }
}

function resetForm() {
  form.id = 0
  form.platformId = filters.platformId
  form.username = ''
  form.password = ''
  form.testModel = ''
}

function openCreate() {
  dialogMode.value = 'create'
  resetForm()
  dialogVisible.value = true
}

function openEdit(account: Account) {
  dialogMode.value = 'edit'
  form.id = account.id
  form.platformId = account.platformId
  form.username = account.username
  form.password = ''
  form.testModel = account.testModel || ''
  dialogVisible.value = true
}

async function saveForm() {
  saving.value = true
  try {
    const valid = await formRef.value?.validate().catch(() => false)
    if (!valid || form.platformId == null) {
      return
    }
    const payload = {
      platformId: form.platformId,
      username: form.username,
      password: form.password || undefined,
      testModel: form.testModel || undefined
    }
    if (dialogMode.value === 'create') {
      await saveAccount(payload)
      ElMessage.success('账号已添加')
    } else {
      await updateAccount({ id: form.id, ...payload })
      ElMessage.success('账号已更新')
    }
    dialogVisible.value = false
    await reload()
  } finally {
    saving.value = false
  }
}

async function removeAccount(account: Account) {
  await ElMessageBox.confirm(`确认删除账号 ${account.username}？`, '删除账号', {
    type: 'warning',
    confirmButtonText: '删除',
    cancelButtonText: '取消'
  })
  await deleteAccount(account.id)
  ElMessage.success('账号已删除')
  await reload()
}

function handleSizeChange(size: number) {
  pageSize.value = size
  pageNo.value = 1
  void reload()
}

function initial(value?: string) {
  return (value || 'A').trim().slice(0, 1).toUpperCase()
}

function platformTypeLabel(type?: PlatformType) {
  if (type === 'newApi') {
    return 'NewApi'
  }
  return 'Sub2Api'
}

function formatDateTime(value?: string) {
  if (!value) {
    return '-'
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

onMounted(async () => {
  await loadPlatforms()
  await reload()
})
</script>

<style scoped>
.account-page__card {
  margin-top: 24px;
}

.toolbar {
  display: flex;
  gap: 12px;
  align-items: center;
}

.toolbar__select {
  width: 220px;
}

.toolbar__input {
  width: 220px;
}

.account-list {
  display: flex;
  min-height: 180px;
  flex-direction: column;
  gap: 12px;
}

.account-card {
  display: grid;
  grid-template-columns: minmax(260px, 1.3fr) minmax(320px, 1fr) auto;
  gap: 16px;
  align-items: center;
  border: 1px solid #e5e7eb;
  border-radius: 8px;
  background: #fff;
  padding: 14px 16px;
  box-shadow: 0 8px 22px rgb(31 41 55 / 5%);
}

.account-card__main {
  display: flex;
  min-width: 0;
  align-items: center;
  gap: 12px;
}

.account-card__mark {
  display: grid;
  width: 38px;
  height: 38px;
  flex: 0 0 auto;
  place-items: center;
  border-radius: 8px;
  background: #eef6ff;
  color: #2563eb;
  font-weight: 700;
}

.account-card__body {
  min-width: 0;
}

.account-card__title-row {
  display: flex;
  align-items: center;
  gap: 8px;
}

.account-card__title {
  overflow: hidden;
  margin: 0;
  color: #0f172a;
  font-size: 15px;
  font-weight: 700;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.account-card__meta {
  overflow: hidden;
  margin-top: 5px;
  color: #64748b;
  font-size: 12px;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.account-card__detail {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 10px;
}

.detail-item {
  border-radius: 6px;
  background: #f8fafc;
  padding: 10px 12px;
}

.detail-item span {
  display: block;
  color: #64748b;
  font-size: 12px;
}

.detail-item strong {
  overflow: hidden;
  display: block;
  margin-top: 6px;
  color: #111827;
  font-size: 14px;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.account-card__actions {
  display: flex;
  justify-content: flex-end;
  gap: 8px;
}

.pager {
  display: flex;
  justify-content: flex-end;
  padding-top: 16px;
}

.account-dialog :deep(.el-select) {
  width: 100%;
}
</style>
