<template>
  <AppShell
    title="定时任务 / 任务编辑"
    subtitle="默认分页展示全部定时任务，支持搜索、新增、编辑、启停和执行预览。"
  >
    <el-card shadow="never" class="task-panel">
      <template #header>
        <div class="task-panel-head">
          <span>任务列表</span>
          <div class="task-toolbar">
            <el-input
              v-model="filters.keyword"
              clearable
              placeholder="搜索任务名称 / 标识"
              class="task-search"
              @keyup.enter="handleSearch"
            />
            <el-select v-model="filters.group" clearable placeholder="任务分组" class="task-group-filter">
              <el-option v-for="group in groupOptions" :key="group" :label="group" :value="group" />
            </el-select>
            <el-button type="primary" @click="handleSearch">搜索</el-button>
            <el-button type="success" @click="openCreate">新增定时任务</el-button>
          </div>
        </div>
      </template>

      <el-table :data="pagedTasks" v-loading="loading" class="task-table">
        <el-table-column prop="taskName" label="任务标识" min-width="180" show-overflow-tooltip />
        <el-table-column label="任务名称" min-width="180" show-overflow-tooltip>
          <template #default="{ row }">
            {{ taskTypeLabel(row.taskType) }}
          </template>
        </el-table-column>
        <el-table-column prop="taskGroup" label="分组" width="120" />
        <el-table-column prop="cron" label="Cron" min-width="160" show-overflow-tooltip />
        <el-table-column label="通知场景" min-width="170" show-overflow-tooltip>
          <template #default="{ row }">
            {{ row.notifyEnabled === 1 ? notifySceneLabel(row.notifySceneId) : '-' }}
          </template>
        </el-table-column>
        <el-table-column label="执行类" min-width="260" show-overflow-tooltip>
          <template #default="{ row }">
            {{ executionClass(row.taskType) }}
          </template>
        </el-table-column>
        <el-table-column label="状态" width="100">
          <template #default="{ row }">
            <el-tag :type="row.enabled === 1 ? 'success' : 'info'" size="small">
              {{ row.enabled === 1 ? '启用' : '停用' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="280" fixed="right">
          <template #default="{ row }">
            <el-button size="small" type="primary" @click="openEdit(row)">编辑</el-button>
            <el-button size="small" type="success" @click="handleTrigger(row)">执行</el-button>
            <el-button
              size="small"
              :type="row.enabled === 1 ? 'warning' : 'success'"
              @click="handleToggle(row)"
            >
              {{ row.enabled === 1 ? '停用' : '启用' }}
            </el-button>
            <el-button size="small" type="danger" plain @click="handleDelete(row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>

      <div class="task-pagination">
        <span>Total {{ filteredTasks.length }}</span>
        <el-pagination
          v-model:current-page="pagination.page"
          v-model:page-size="pagination.pageSize"
          :total="filteredTasks.length"
          :page-sizes="[10, 20, 50]"
          layout="sizes, prev, pager, next"
        />
      </div>
    </el-card>

    <el-dialog v-model="dialogVisible" :title="dialogTitle" width="760px" destroy-on-close>
      <el-form
        ref="formRef"
        class="task-form"
        :model="formState"
        :rules="rules"
        label-width="128px"
        label-position="right"
      >
        <el-form-item label="任务名称" prop="taskName">
          <el-input v-model="formState.taskName" placeholder="例如 sub2-login" />
        </el-form-item>
        <el-form-item label="任务分组" prop="taskGroup">
          <el-input v-model="formState.taskGroup" placeholder="默认 monitor" />
        </el-form-item>
        <el-form-item label="任务类型" prop="taskType">
          <el-select v-model="formState.taskType" placeholder="请选择任务类型">
            <el-option
              v-for="option in taskTypeOptions"
              :key="option.value"
              :label="option.label"
              :value="option.value"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="目标地址" prop="baseUrl">
          <el-input v-model="formState.baseUrl" placeholder="https://example.com" />
        </el-form-item>
        <el-form-item label="Cron 表达式" prop="cron">
          <el-input v-model="formState.cron" placeholder="0 */30 * * * ?" />
        </el-form-item>
        <el-form-item label="状态" prop="enabled">
          <el-switch
            v-model="enabledSwitch"
            :active-value="true"
            :inactive-value="false"
            active-text="启用"
            inactive-text="停用"
          />
        </el-form-item>
        <el-divider class="task-form-divider" content-position="left">通知配置</el-divider>
        <el-form-item label="发送通知" prop="notifyEnabled">
          <el-switch
            v-model="notifyEnabledSwitch"
            :active-value="true"
            :inactive-value="false"
            active-text="启用"
            inactive-text="停用"
          />
        </el-form-item>
        <el-form-item v-if="formState.notifyEnabled === 1" label="通知场景" prop="notifySceneId">
          <el-select v-model="formState.notifySceneId" placeholder="请选择通知场景">
            <el-option
              v-for="scene in notifySceneOptions"
              :key="scene.value"
              :label="scene.label"
              :value="scene.value"
            />
          </el-select>
        </el-form-item>
        <el-form-item v-if="formState.notifyEnabled === 1" label="触发时机" prop="notifyTrigger">
          <el-radio-group v-model="formState.notifyTrigger">
            <el-radio-button label="FAILURE">失败</el-radio-button>
            <el-radio-button label="SUCCESS">成功</el-radio-button>
            <el-radio-button label="ALWAYS">总是</el-radio-button>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="备注" prop="remark">
          <el-input v-model="formState.remark" type="textarea" :rows="3" placeholder="可选" />
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
import AppShell from '../components/AppShell.vue'
import type { SchedulerTask, SchedulerTaskForm } from '../types/scheduler'
import {
  createSchedulerTask,
  deleteSchedulerTask,
  listSchedulerTasks,
  pauseSchedulerTask,
  resumeSchedulerTask,
  schedulerTaskTypeOptions,
  triggerSchedulerTask,
  updateSchedulerTask,
} from '../api/scheduler'

const loading = ref(false)
const submitLoading = ref(false)
const dialogVisible = ref(false)
const editingId = ref<number | null>(null)
const taskList = ref<SchedulerTask[]>([])
const formRef = ref<FormInstance>()
const filters = reactive({
  keyword: '',
  group: '',
})
const pagination = reactive({
  page: 1,
  pageSize: 20,
})

const defaultForm: SchedulerTaskForm = {
  taskName: 'data-collect',
  taskGroup: 'monitor',
  taskType: 'DATA_COLLECT',
  baseUrl: '',
  cron: '0 */30 * * * ?',
  enabled: 1,
  notifyEnabled: 0,
  notifySceneId: null,
  notifyTrigger: 'FAILURE',
  remark: '',
}

const formState = reactive<SchedulerTaskForm>({ ...defaultForm })

const rules: FormRules<SchedulerTaskForm> = {
  taskName: [{ required: true, message: '请输入任务名称', trigger: 'blur' }],
  taskGroup: [{ required: true, message: '请输入任务分组', trigger: 'blur' }],
  taskType: [{ required: true, message: '请选择任务类型', trigger: 'change' }],
  cron: [{ required: true, message: '请输入 Cron 表达式', trigger: 'blur' }],
  notifySceneId: [
    {
      validator: (_rule, value, callback) => {
        if (formState.notifyEnabled === 1 && !value) {
          callback(new Error('请选择通知场景'))
          return
        }
        callback()
      },
      trigger: 'change',
    },
  ],
}

const taskTypeOptions = schedulerTaskTypeOptions
const notifySceneOptions = [
  { label: '余额告警', value: 1 },
  { label: '采集失败', value: 2 },
  { label: '每日报表', value: 3 },
  { label: '渠道及倍率变更', value: 4 },
  { label: '账号异常', value: 5 },
]

const dialogTitle = computed(() => (editingId.value ? '编辑任务' : '新增任务'))
const groupOptions = computed(() => {
  return Array.from(new Set(taskList.value.map((item) => item.taskGroup).filter(Boolean)))
})
const filteredTasks = computed(() => {
  const keyword = filters.keyword.trim().toLowerCase()
  return taskList.value.filter((task) => {
    const matchKeyword =
      !keyword ||
      task.taskName.toLowerCase().includes(keyword) ||
      taskTypeLabel(task.taskType).toLowerCase().includes(keyword)
    const matchGroup = !filters.group || task.taskGroup === filters.group
    return matchKeyword && matchGroup
  })
})
const pagedTasks = computed(() => {
  const start = (pagination.page - 1) * pagination.pageSize
  return filteredTasks.value.slice(start, start + pagination.pageSize)
})
const enabledSwitch = computed({
  get: () => formState.enabled === 1,
  set: (value: boolean) => {
    formState.enabled = value ? 1 : 0
  },
})
const notifyEnabledSwitch = computed({
  get: () => formState.notifyEnabled === 1,
  set: (value: boolean) => {
    formState.notifyEnabled = value ? 1 : 0
    if (!value) {
      formState.notifySceneId = null
      formState.notifyTrigger = 'FAILURE'
    }
  },
})

const loadTasks = async () => {
  loading.value = true
  try {
    taskList.value = await listSchedulerTasks()
    if ((pagination.page - 1) * pagination.pageSize >= taskList.value.length) {
      pagination.page = 1
    }
  } catch (error) {
    ElMessage.error('加载任务列表失败')
  } finally {
    loading.value = false
  }
}

const handleSearch = () => {
  pagination.page = 1
}

const resetForm = () => {
  Object.assign(formState, defaultForm)
  editingId.value = null
}

const openCreate = () => {
  resetForm()
  dialogVisible.value = true
}

const openEdit = (row: SchedulerTask) => {
  editingId.value = row.id ?? null
  Object.assign(formState, {
    taskName: row.taskName,
    taskGroup: row.taskGroup,
    taskType: row.taskType,
    baseUrl: row.baseUrl,
    cron: row.cron,
    enabled: row.enabled ?? 1,
    notifyEnabled: row.notifyEnabled ?? 0,
    notifySceneId: row.notifySceneId ?? null,
    notifyTrigger: row.notifyTrigger ?? 'FAILURE',
    remark: row.remark ?? '',
  })
  dialogVisible.value = true
}

const submitForm = async () => {
  if (!formRef.value) return
  const valid = await formRef.value.validate().catch(() => false)
  if (!valid) {
    return
  }

  submitLoading.value = true
  try {
    if (editingId.value) {
      await updateSchedulerTask(editingId.value, { ...formState })
      ElMessage.success('任务已更新')
    } else {
      await createSchedulerTask({ ...formState })
      ElMessage.success('任务已创建')
    }
    dialogVisible.value = false
    await loadTasks()
  } catch (error) {
    ElMessage.error('保存任务失败')
  } finally {
    submitLoading.value = false
  }
}

const handleDelete = async (row: SchedulerTask) => {
  try {
    await ElMessageBox.confirm(`确认删除任务「${row.taskName}」？`, '删除任务', { type: 'warning' })
    await deleteSchedulerTask(row.id!)
    ElMessage.success('任务已删除')
    await loadTasks()
  } catch (error) {
    if (error !== 'cancel') {
      ElMessage.error('删除任务失败')
    }
  }
}

const handleTrigger = async (row: SchedulerTask) => {
  try {
    await triggerSchedulerTask(row.id!)
    ElMessage.success('已触发执行')
  } catch (error) {
    ElMessage.error('触发执行失败')
  }
}

const handleToggle = async (row: SchedulerTask) => {
  try {
    if (row.enabled === 1) {
      await pauseSchedulerTask(row.id!)
      ElMessage.success('任务已停用')
    } else {
      await resumeSchedulerTask(row.id!)
      ElMessage.success('任务已启用')
    }
    await loadTasks()
  } catch (error) {
    ElMessage.error('更新任务状态失败')
  }
}

const taskTypeLabel = (value: string) => {
  return taskTypeOptions.find((item) => item.value === value)?.label ?? value
}

const notifySceneLabel = (value?: number | null) => {
  if (!value) {
    return '未配置'
  }
  return notifySceneOptions.find((item) => item.value === value)?.label ?? `场景 ${value}`
}

const executionClass = (taskType: string) => {
  const classMap: Record<string, string> = {
    DATA_COLLECT: 'com.sub2.monitor.scheduler.job.CollectJob',
  }
  return classMap[taskType] ?? 'com.sub2.monitor.scheduler.job.CollectJob'
}

onMounted(loadTasks)
</script>
