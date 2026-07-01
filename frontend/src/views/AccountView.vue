<template>
  <AppShell title="账号管理" subtitle="按平台维护采集账号，密码不会回显，编辑时留空表示不修改。">
    <el-card shadow="never" class="account-panel">
      <template #header>
        <div class="account-panel-head">
          <span>账号列表</span>
          <div class="account-toolbar">
            <el-select v-model="filters.platformId" clearable filterable placeholder="筛选平台" class="account-platform-filter">
              <el-option
                v-for="platform in platformOptions"
                :key="platform.id"
                :label="platformLabel(platform)"
                :value="platform.id"
              />
            </el-select>
            <el-input
              v-model="filters.keyword"
              clearable
              placeholder="搜索账号"
              class="account-search"
              @keyup.enter="loadAccounts"
            />
            <el-button :icon="Search" type="primary" @click="loadAccounts">搜索</el-button>
            <el-button :icon="Plus" type="success" @click="openCreate">添加账号</el-button>
          </div>
        </div>
      </template>

      <div v-loading="loading" class="account-manage-list">
        <div v-for="account in accountList" :key="account.id" class="account-manage-item">
          <div class="account-main">
            <div class="account-avatar">{{ accountAvatar(account) }}</div>
            <div class="account-info">
              <div class="account-title-line">
                <span class="account-name">{{ accountIdentity(account) }}</span>
                <el-tag v-if="account.platformType" size="small">{{ formatType(account.platformType) }}</el-tag>
              </div>
              <div class="account-subline">{{ account.platformName || '未绑定平台' }}</div>
            </div>
          </div>
          <div class="account-field">
            <span>测试模型</span>
            <strong>{{ account.testModel || '未设置' }}</strong>
          </div>
          <div class="account-field">
            <span>采集状态</span>
            <strong>{{ account.isCollect ? '采集中' : '未采集' }}</strong>
          </div>
          <div class="account-actions">
            <el-button link type="primary" @click="openEdit(account)">编辑</el-button>
            <el-button link type="danger" @click="handleDelete(account)">删除</el-button>
          </div>
        </div>
        <el-empty v-if="!loading && !accountList.length" description="暂无账号" />
      </div>
    </el-card>

    <el-dialog v-model="dialogVisible" :title="dialogTitle" width="640px" destroy-on-close>
      <el-form ref="formRef" class="task-form" :model="formState" :rules="rules" label-width="110px">
        <el-form-item label="所属平台" prop="platformId">
          <el-select v-model="formState.platformId" filterable placeholder="请选择平台">
            <el-option
              v-for="platform in platformOptions"
              :key="platform.id"
              :label="platformLabel(platform)"
              :value="platform.id"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="账号" prop="username">
          <el-input v-model="formState.username" placeholder="请输入账号" />
        </el-form-item>
        <el-form-item label="邮箱">
          <el-input v-model="formState.email" placeholder="请输入邮箱，账号为空时可用邮箱登录" />
        </el-form-item>
        <el-form-item label="密码" prop="password">
          <el-input
            v-model="formState.password"
            show-password
            type="password"
            :placeholder="editingId ? '留空表示不修改密码' : '请输入密码'"
          />
        </el-form-item>
        <el-form-item label="测试模型">
          <el-input v-model="formState.testModel" placeholder="例如：gpt-4o-mini" />
        </el-form-item>
        <el-form-item label="采集">
          <el-switch v-model="formState.isCollect" active-text="启用" inactive-text="停用" />
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
import { ElMessage, ElMessageBox, type FormInstance, type FormRules } from 'element-plus'
import { Plus, Search } from '@element-plus/icons-vue'
import AppShell from '../components/AppShell.vue'
import { createAccount, deleteAccount, listAccounts, updateAccount } from '../api/account'
import { listPlatforms } from '../api/platform'
import type { AccountForm, AccountItem } from '../types/account'
import type { PlatformItem } from '../types/platform'

const loading = ref(false)
const submitLoading = ref(false)
const dialogVisible = ref(false)
const editingId = ref<number | null>(null)
const accountList = ref<AccountItem[]>([])
const platformOptions = ref<PlatformItem[]>([])
const formRef = ref<FormInstance>()

const filters = reactive<{ platformId: number | null; keyword: string }>({
  platformId: null,
  keyword: '',
})

const defaultForm: AccountForm = {
  platformId: null,
  username: '',
  email: '',
  password: '',
  testModel: '',
  isCollect: true,
}

const formState = reactive<AccountForm>({ ...defaultForm })

const rules = computed<FormRules<AccountForm>>(() => ({
  platformId: [{ required: true, message: '请选择平台', trigger: 'change' }],
  username: [
    {
      validator: (_rule, value, callback) => {
        if (String(value || '').trim() || formState.email.trim()) {
          callback()
          return
        }
        callback(new Error('请输入账号或邮箱'))
      },
      trigger: 'blur',
    },
  ],
  password: [
    {
      validator: (_rule, value, callback) => {
        if (editingId.value || String(value || '').trim()) {
          callback()
          return
        }
        callback(new Error('请输入密码'))
      },
      trigger: 'blur',
    },
  ],
}))

const dialogTitle = computed(() => (editingId.value ? '编辑账号' : '添加账号'))

const loadPlatforms = async () => {
  const data = await listPlatforms({})
  platformOptions.value = data.items ?? []
}

const loadAccounts = async () => {
  loading.value = true
  try {
    accountList.value = await listAccounts({
      platformId: filters.platformId,
      keyword: filters.keyword || undefined,
    })
  } catch (error) {
    ElMessage.error('加载账号列表失败')
  } finally {
    loading.value = false
  }
}

const openCreate = () => {
  Object.assign(formState, defaultForm)
  editingId.value = null
  dialogVisible.value = true
}

const openEdit = (account: AccountItem) => {
  editingId.value = account.id ?? null
  Object.assign(formState, {
    platformId: account.platformId ?? null,
    username: account.username ?? '',
    email: account.email ?? '',
    password: '',
    testModel: account.testModel ?? '',
    isCollect: account.isCollect ?? true,
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
      await updateAccount(editingId.value, { ...formState })
      ElMessage.success('账号已更新')
    } else {
      await createAccount({ ...formState })
      ElMessage.success('账号已添加')
    }
    dialogVisible.value = false
    await loadAccounts()
  } catch (error) {
    ElMessage.error('保存账号失败')
  } finally {
    submitLoading.value = false
  }
}

const handleDelete = async (account: AccountItem) => {
  if (!account.id) return
  try {
    await ElMessageBox.confirm(`确认删除账号 ${accountIdentity(account)}？`, '删除账号', {
      confirmButtonText: '删除',
      cancelButtonText: '取消',
      type: 'warning',
    })
    await deleteAccount(account.id)
    ElMessage.success('账号已删除')
    await loadAccounts()
  } catch (error) {
    if (error !== 'cancel') {
      ElMessage.error('删除账号失败')
    }
  }
}

const accountIdentity = (account: AccountItem) => account.username || account.email || '-'
const accountAvatar = (account: AccountItem) => accountIdentity(account).slice(0, 1).toUpperCase()
const platformLabel = (platform: PlatformItem) => `${platform.platformName} / ${formatType(platform.type)}`
const formatType = (type?: string | null) => {
  const upper = String(type || '').toUpperCase()
  if (upper === 'SUB2API') return 'Sub2Api'
  if (upper === 'NEWAPI') return 'NewApi'
  return type || '-'
}

onMounted(async () => {
  try {
    await loadPlatforms()
  } catch (error) {
    ElMessage.error('加载平台下拉失败')
  }
  await loadAccounts()
})
</script>
