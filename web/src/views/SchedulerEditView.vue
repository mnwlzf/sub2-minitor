<template>
  <div>
    <h1 class="page-title">任务编辑</h1>
    <div class="page-subtitle">默认分页展示全部定时任务，支持搜索、新增、编辑、启停和执行预览。</div>

    <el-card class="page-card scheduler-edit__card">
      <template #header>
        <div class="card-header">
          <span>任务列表</span>
          <div class="toolbar">
            <el-input v-model="filters.keyword" clearable placeholder="搜索任务名称 / 标识" class="toolbar__input" />
            <el-select v-model="filters.taskGroup" clearable placeholder="任务分组" class="toolbar__select">
              <el-option label="倍率" value="RATE" />
              <el-option label="通用" value="GENERAL" />
            </el-select>
            <el-button :icon="Search" type="primary" @click="reload">搜索</el-button>
            <el-button :icon="Plus" type="success" @click="openCreate">新增定时任务</el-button>
          </div>
        </div>
      </template>

      <el-table :data="tasks" v-loading="loading" size="small" style="width: 100%">
        <el-table-column prop="taskKey" label="任务标识" width="180" />
        <el-table-column prop="taskName" label="任务名称" width="160" />
        <el-table-column label="分组" width="120">
          <template #default="{ row }">
            {{ taskGroupLabel(row.taskGroup) }}
          </template>
        </el-table-column>
        <el-table-column prop="cronExpression" label="Cron" min-width="200" />
        <el-table-column prop="jobClass" label="执行类" min-width="280" show-overflow-tooltip />
        <el-table-column prop="isEnabled" label="状态" width="100">
          <template #default="{ row }">
            <el-tag :type="row.isEnabled ? 'success' : 'info'" effect="light">
              {{ row.isEnabled ? '启用' : '停用' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="240" fixed="right">
          <template #default="{ row }">
            <div class="action-buttons">
              <el-button type="primary" size="small" @click="openEdit(row)">编辑</el-button>
              <el-button type="success" size="small" @click="runTaskNow(row)">执行</el-button>
              <el-button :type="row.isEnabled ? 'warning' : 'info'" size="small" @click="toggleTask(row)">
              {{ row.isEnabled ? '停用' : '启用' }}
              </el-button>
            </div>
          </template>
        </el-table-column>
      </el-table>

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
      :title="dialogMode === 'create' ? '新增定时任务' : '编辑定时任务'"
      width="960px"
      class="scheduler-dialog"
    >
      <el-form ref="formRef" :model="form" :rules="rules" label-width="96px" class="scheduler-form">
        <div class="scheduler-form-grid">
          <el-form-item label="任务标识" prop="taskKey">
            <el-input v-model="form.taskKey" :disabled="dialogMode === 'edit'" />
          </el-form-item>
          <el-form-item label="任务分组" prop="taskGroup">
            <el-select v-model="form.taskGroup" placeholder="请选择分组">
              <el-option label="倍率" value="RATE" />
              <el-option label="通用" value="GENERAL" />
            </el-select>
          </el-form-item>
          <el-form-item label="任务名称" prop="taskName">
            <el-input v-model="form.taskName" />
          </el-form-item>
          <el-form-item label="启用">
            <el-switch v-model="form.isEnabled" />
          </el-form-item>
          <el-form-item label="执行类" prop="jobClass" class="form-item--full">
            <el-input v-model="form.jobClass" />
          </el-form-item>
          <el-form-item label="Cron 表达式" prop="cronExpression" class="form-item--full">
            <div class="cron-input-block">
              <el-input
                v-model="form.cronExpression"
                placeholder="例如：0 */5 * * * ?"
                class="cron-expression-input"
              />
              <div class="cron-input-hint">Quartz 格式：秒 分 时 日 月 周 [年]</div>
            </div>
          </el-form-item>
          <el-form-item label="下次执行" class="form-item--full">
            <div class="preview-list" :class="{ 'preview-list--error': Boolean(cronPreviewError) }">
              <el-tag v-if="cronPreviewLoading" type="info" effect="plain">计算中...</el-tag>
              <template v-else>
                <el-tag v-for="item in previewTimes" :key="item" effect="plain">{{ item }}</el-tag>
                <span v-if="cronPreviewError" class="preview-error">{{ cronPreviewError }}</span>
                <span v-else-if="previewTimes.length === 0" class="preview-empty">暂无预览</span>
              </template>
            </div>
          </el-form-item>
        </div>

        <el-alert
          title="Cron 表达式校验失败时无法保存，请检查语法后重试。"
          type="warning"
          :closable="false"
          show-icon
          class="cron-alert"
        />

        <el-form-item label="描述" class="form-item--full">
          <el-input v-model="form.description" type="textarea" :rows="3" />
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
import { onBeforeUnmount, onMounted, reactive, ref, watch } from 'vue'
import { ElMessage, type FormInstance, type FormRules } from 'element-plus'
import { Plus, Search } from '@element-plus/icons-vue'
import {
  listTasks,
  saveTask,
  updateTask,
  runTask,
  pauseTask,
  resumeTask,
  previewCron,
  type TaskSchedule
} from '@/api/monitor'

const loading = ref(false)
const saving = ref(false)
const tasks = ref<TaskSchedule[]>([])
const total = ref(0)
const pageNo = ref(1)
const pageSize = ref(20)
const dialogVisible = ref(false)
const dialogMode = ref<'create' | 'edit'>('create')
const previewTimes = ref<string[]>([])
const cronPreviewError = ref('')
const cronPreviewLoading = ref(false)
const formRef = ref<FormInstance>()

const filters = reactive({
  keyword: '',
  taskGroup: ''
})

const form = reactive({
  taskKey: '',
  taskName: '',
  taskGroup: 'RATE',
  cronExpression: '0 */5 * * * ?',
  jobClass: 'com.sub2.monitor.scheduler.job.BalanceChannelCollectJob',
  description: '',
  isEnabled: true
})

const rules: FormRules = {
  taskKey: [{ required: true, message: '请输入任务标识', trigger: 'blur' }],
  taskName: [{ required: true, message: '请输入任务名称', trigger: 'blur' }],
  taskGroup: [{ required: true, message: '请选择任务分组', trigger: 'change' }],
  jobClass: [{ required: true, message: '请输入执行类', trigger: 'blur' }],
  cronExpression: [
    { required: true, message: '请输入 Cron 表达式', trigger: 'blur' },
    { validator: validateCronExpression, trigger: 'blur' }
  ]
}

function resetForm() {
  form.taskKey = ''
  form.taskName = ''
  form.taskGroup = 'RATE'
  form.cronExpression = '0 */5 * * * ?'
  form.jobClass = 'com.sub2.monitor.scheduler.job.BalanceChannelCollectJob'
  form.description = ''
  form.isEnabled = true
  previewTimes.value = []
  cronPreviewError.value = ''
}

function bindRow(row: TaskSchedule) {
  form.taskKey = row.taskKey
  form.taskName = row.taskName
  form.taskGroup = row.taskGroup ?? 'RATE'
  form.cronExpression = row.cronExpression
  form.jobClass = row.jobClass
  form.description = row.description ?? ''
  form.isEnabled = row.isEnabled
  void loadPreview(row.cronExpression)
}

function openCreate() {
  dialogMode.value = 'create'
  resetForm()
  dialogVisible.value = true
}

function openEdit(row: TaskSchedule) {
  dialogMode.value = 'edit'
  bindRow(row)
  dialogVisible.value = true
}

async function loadPreview(value: string) {
  if (!value) {
    previewTimes.value = []
    cronPreviewError.value = ''
    return
  }
  cronPreviewLoading.value = true
  try {
    const response = await previewCron({ cronExpression: value, count: 5 })
    previewTimes.value = response.data.map(formatPreviewTime)
    cronPreviewError.value = ''
  } catch {
    previewTimes.value = []
    cronPreviewError.value = 'Cron 表达式无效'
  } finally {
    cronPreviewLoading.value = false
  }
}

async function validateCronExpression(_rule: unknown, value: string) {
  if (!value || !value.trim()) {
    return Promise.reject(new Error('请输入 Cron 表达式'))
  }
  try {
    await previewCron({ cronExpression: value, count: 1 })
    return Promise.resolve()
  } catch {
    return Promise.reject(new Error('Cron 表达式无效'))
  }
}

function formatPreviewTime(value: string) {
  const date = new Date(value)
  if (Number.isNaN(date.getTime())) {
    return value
  }
  const pad = (num: number) => String(num).padStart(2, '0')
  return `${date.getFullYear()}-${pad(date.getMonth() + 1)}-${pad(date.getDate())} ${pad(date.getHours())}:${pad(date.getMinutes())}:${pad(date.getSeconds())}`
}

async function reload() {
  loading.value = true
  try {
    const response = await listTasks({
      pageNo: pageNo.value,
      pageSize: pageSize.value,
      keyword: filters.keyword || undefined,
      taskGroup: filters.taskGroup || undefined
    })
    tasks.value = response.data.records
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
      taskKey: form.taskKey,
      taskName: form.taskName,
      taskGroup: form.taskGroup,
      cronExpression: form.cronExpression,
      jobClass: form.jobClass,
      description: form.description,
      isEnabled: form.isEnabled
    }
    if (dialogMode.value === 'create') {
      await saveTask(payload)
      ElMessage.success('任务已新增')
    } else {
      await updateTask(payload)
      ElMessage.success('任务已更新')
    }
    dialogVisible.value = false
    await reload()
  } finally {
    saving.value = false
  }
}

async function runTaskNow(row: TaskSchedule) {
  await runTask({ taskKey: row.taskKey, taskGroup: row.taskGroup ?? 'RATE' })
  ElMessage.success('已触发执行')
}

async function toggleTask(row: TaskSchedule) {
  if (row.isEnabled) {
    await pauseTask({ taskKey: row.taskKey, taskGroup: row.taskGroup ?? 'RATE' })
    ElMessage.success('已停用')
  } else {
    await resumeTask({ taskKey: row.taskKey, taskGroup: row.taskGroup ?? 'RATE' })
    ElMessage.success('已启用')
  }
  await reload()
}

function handleSizeChange(size: number) {
  pageSize.value = size
  pageNo.value = 1
  void reload()
}

function taskGroupLabel(value?: string) {
  if (value === 'RATE') {
    return '倍率'
  }
  if (value === 'GENERAL') {
    return '通用'
  }
  return value ?? '-'
}

let cronPreviewTimer: number | undefined
watch(
  () => form.cronExpression,
  value => {
    if (cronPreviewTimer) {
      window.clearTimeout(cronPreviewTimer)
    }
    cronPreviewTimer = window.setTimeout(() => {
      void loadPreview(value)
    }, 250)
  }
)

onBeforeUnmount(() => {
  if (cronPreviewTimer) {
    window.clearTimeout(cronPreviewTimer)
  }
})

onMounted(reload)
</script>

<style scoped>
.scheduler-edit__card {
  margin-top: 24px;
}

.scheduler-dialog :deep(.el-dialog__body) {
  padding-top: 12px;
}

.scheduler-form {
  display: block;
}

.scheduler-form-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 12px 20px;
}

.scheduler-form-grid :deep(.el-form-item) {
  margin-bottom: 0;
}

.scheduler-form-grid :deep(.el-form-item__content) {
  min-width: 0;
}

.scheduler-form-grid :deep(.el-input),
.scheduler-form-grid :deep(.el-select) {
  width: 100%;
}

.form-item--full {
  grid-column: 1 / -1;
}

.cron-input-block {
  width: 100%;
}

.cron-expression-input {
  width: 100%;
}

.cron-input-block :deep(.el-input__inner) {
  font-family: ui-monospace, SFMono-Regular, Consolas, monospace;
}

.cron-input-hint {
  margin-top: 6px;
  font-size: 12px;
  color: #6b7280;
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
  width: 140px;
}

.pager {
  display: flex;
  justify-content: flex-end;
  padding-top: 16px;
}

.action-buttons {
  display: flex;
  align-items: center;
  gap: 10px;
}

.cron-editor {
  width: 100%;
}

.preview-list {
  flex: 1;
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  align-items: center;
  min-height: 32px;
}

.preview-empty {
  color: #6b7280;
  font-size: 13px;
}

.preview-error {
  color: #d97706;
  font-size: 13px;
}

.preview-list--error {
  align-items: center;
}

.cron-alert {
  margin-bottom: 16px;
}
</style>
