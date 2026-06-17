<template>
  <div>
    <h1 class="page-title">邮件设置</h1>
    <div class="page-subtitle">维护 SMTP 发信服务，并按通知场景配置主送、抄送、密送收件人。</div>

    <el-tabs v-model="activeTab" class="mail-tabs">
      <el-tab-pane label="SMTP 设置" name="smtp">
        <el-card class="page-card">
          <template #header>
            <div class="card-header">
              <span>SMTP 设置</span>
              <div class="toolbar">
                <el-button :loading="testing" @click="testSmtp">测试连接</el-button>
                <el-button type="primary" :loading="savingSmtp" @click="saveSmtp">保存配置</el-button>
              </div>
            </div>
          </template>

          <el-form ref="smtpFormRef" :model="smtpForm" :rules="smtpRules" label-position="top">
            <div class="smtp-grid">
              <el-form-item label="SMTP 主机" prop="host">
                <el-input v-model="smtpForm.host" placeholder="smtp.qq.com" />
              </el-form-item>
              <el-form-item label="SMTP 端口" prop="port">
                <el-input-number v-model="smtpForm.port" :min="1" :max="65535" controls-position="right" />
              </el-form-item>
              <el-form-item label="SMTP 用户名" prop="username">
                <el-input v-model="smtpForm.username" placeholder="name@example.com" />
              </el-form-item>
              <el-form-item label="SMTP 密码" prop="password">
                <el-input
                  v-model="smtpForm.password"
                  show-password
                  type="password"
                  :placeholder="smtpForm.passwordConfigured ? '留空则保留当前密码' : '请输入 SMTP 密码'"
                />
                <div v-if="smtpForm.passwordConfigured" class="form-help">密码已配置，留空以保留当前值。</div>
              </el-form-item>
              <el-form-item label="发件人邮箱" prop="fromEmail">
                <el-input v-model="smtpForm.fromEmail" placeholder="name@example.com" />
              </el-form-item>
              <el-form-item label="发件人名称">
                <el-input v-model="smtpForm.fromName" placeholder="Sub2 Monitor" />
              </el-form-item>
            </div>

            <div class="switch-row">
              <div>
                <div class="switch-row__title">启用配置</div>
                <div class="switch-row__desc">关闭后业务发送邮件会跳过默认 SMTP。</div>
              </div>
              <el-switch v-model="smtpForm.isEnabled" />
            </div>
            <div class="switch-row">
              <div>
                <div class="switch-row__title">使用 TLS</div>
                <div class="switch-row__desc">常用于 587 端口的 STARTTLS 加密。</div>
              </div>
              <el-switch v-model="smtpForm.useTls" :disabled="smtpForm.useSsl" />
            </div>
            <div class="switch-row">
              <div>
                <div class="switch-row__title">使用 SSL</div>
                <div class="switch-row__desc">常用于 465 端口的 SSL 连接。</div>
              </div>
              <el-switch v-model="smtpForm.useSsl" :disabled="smtpForm.useTls" />
            </div>
          </el-form>
        </el-card>
      </el-tab-pane>

      <el-tab-pane label="通知收件人" name="recipients">
        <div class="mail-layout">
          <el-card class="page-card recipient-card">
            <template #header>
              <div class="card-header">
                <span>收件人</span>
                <el-button type="primary" @click="openRecipientCreate">新增收件人</el-button>
              </div>
            </template>
            <el-table :data="recipients" v-loading="loadingRecipients" size="small">
              <el-table-column prop="email" label="邮箱" min-width="220" />
              <el-table-column prop="name" label="名称" width="140" />
              <el-table-column label="状态" width="90">
                <template #default="{ row }">
                  <el-tag :type="row.isEnabled ? 'success' : 'info'" effect="light">
                    {{ row.isEnabled ? '启用' : '停用' }}
                  </el-tag>
                </template>
              </el-table-column>
              <el-table-column label="操作" width="150" fixed="right">
                <template #default="{ row }">
                  <el-button type="primary" text size="small" @click="openRecipientEdit(row)">编辑</el-button>
                  <el-button type="danger" text size="small" @click="removeRecipient(row)">删除</el-button>
                </template>
              </el-table-column>
            </el-table>
          </el-card>

          <el-card class="page-card scene-config-card">
            <template #header>
              <div class="card-header">
                <span>通知场景</span>
                <el-button type="primary" @click="openSceneCreate">新增场景</el-button>
              </div>
            </template>
            <div v-loading="loadingScenes" class="scene-list">
              <article v-for="scene in scenes" :key="scene.sceneKey" class="scene-card">
                <div class="scene-card__header">
                  <div>
                    <h2>{{ scene.sceneName }}</h2>
                    <p>{{ scene.description || scene.sceneKey }}</p>
                  </div>
                  <div class="scene-card__actions">
                    <el-tag :type="scene.isEnabled ? 'success' : 'info'" effect="light">
                      {{ scene.isEnabled ? '启用' : '停用' }}
                    </el-tag>
                    <el-button type="primary" plain size="small" @click="openSceneEdit(scene)">编辑</el-button>
                    <el-button :type="scene.isEnabled ? 'warning' : 'success'" plain size="small" @click="toggleScene(scene)">
                      {{ scene.isEnabled ? '停用' : '启用' }}
                    </el-button>
                    <el-button type="danger" plain size="small" @click="removeScene(scene)">删除</el-button>
                  </div>
                </div>
                <div class="scene-card__toolbar">
                  <el-button type="primary" plain size="small" :disabled="recipients.length === 0" @click="openSceneBind(scene.sceneKey)">
                    添加收件人
                  </el-button>
                </div>
                <div class="scene-recipient-groups">
                  <div v-for="type in recipientTypes" :key="type.value" class="scene-recipient-group">
                    <div class="scene-recipient-group__label">{{ type.label }}</div>
                    <div class="scene-recipient-group__body">
                      <span v-if="sceneRecipientsByType(scene, type.value).length === 0" class="scene-recipient-empty">
                        未配置
                      </span>
                      <el-tag
                        v-for="recipient in sceneRecipientsByType(scene, type.value)"
                        :key="recipient.relationId"
                        closable
                        effect="plain"
                        @close="removeSceneRecipient(recipient.relationId)"
                      >
                        {{ recipient.name || recipient.email }}
                        <span v-if="recipient.name" class="recipient-email">/ {{ recipient.email }}</span>
                      </el-tag>
                    </div>
                  </div>
                </div>
              </article>
            </div>
          </el-card>
        </div>
      </el-tab-pane>
    </el-tabs>

    <el-dialog v-model="recipientDialogVisible" :title="recipientDialogMode === 'create' ? '新增收件人' : '编辑收件人'" width="520px">
      <el-form ref="recipientFormRef" :model="recipientForm" :rules="recipientRules" label-width="88px">
        <el-form-item label="邮箱" prop="email">
          <el-input v-model="recipientForm.email" placeholder="name@example.com" />
        </el-form-item>
        <el-form-item label="名称">
          <el-input v-model="recipientForm.name" placeholder="例如：运维组" />
        </el-form-item>
        <el-form-item label="启用">
          <el-switch v-model="recipientForm.isEnabled" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="recipientDialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="savingRecipient" @click="saveRecipientForm">保存</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="sceneDialogVisible" :title="sceneDialogMode === 'create' ? '新增通知场景' : '编辑通知场景'" width="620px">
      <el-form ref="sceneFormRef" :model="sceneForm" :rules="sceneRules" label-width="96px">
        <el-form-item label="场景标识" prop="sceneKey">
          <el-input
            v-model="sceneForm.sceneKey"
            :disabled="sceneDialogMode === 'edit'"
            placeholder="例如：quota_warning"
          />
          <div class="form-help">仅支持小写字母、数字和下划线，新增后不建议修改。</div>
        </el-form-item>
        <el-form-item label="场景名称" prop="sceneName">
          <el-input v-model="sceneForm.sceneName" placeholder="例如：额度预警" />
        </el-form-item>
        <el-form-item label="描述">
          <el-input v-model="sceneForm.description" type="textarea" :rows="3" placeholder="说明该场景何时发送邮件" />
        </el-form-item>
        <el-form-item label="启用">
          <el-switch v-model="sceneForm.isEnabled" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="sceneDialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="savingScene" @click="saveSceneForm">保存</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="sceneBindDialogVisible" title="绑定场景收件人" width="560px">
      <el-form :model="sceneBindForm" label-width="96px">
        <el-form-item label="通知场景">
          <el-select v-model="sceneBindForm.sceneKey" placeholder="请选择场景">
            <el-option v-for="scene in scenes" :key="scene.sceneKey" :label="scene.sceneName" :value="scene.sceneKey" />
          </el-select>
        </el-form-item>
        <el-form-item label="收件人">
          <el-select v-model="sceneBindForm.recipientId" filterable placeholder="请选择收件人">
            <el-option
              v-for="recipient in recipients"
              :key="recipient.id"
              :label="`${recipient.name || recipient.email} / ${recipient.email}`"
              :value="recipient.id"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="类型">
          <el-segmented v-model="sceneBindForm.recipientType" :options="recipientTypes" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="sceneBindDialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="savingSceneBind" @click="saveSceneBind">绑定</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox, type FormInstance, type FormRules } from 'element-plus'
import {
  addMailSceneRecipient,
  deleteMailScene,
  deleteMailRecipient,
  getMailSmtpConfig,
  listMailRecipients,
  listMailScenes,
  removeMailSceneRecipient as deleteSceneRecipient,
  saveMailRecipient,
  saveMailScene,
  saveMailSmtpConfig,
  testMailSmtpConfig,
  updateMailRecipient,
  updateMailScene,
  type Id,
  type MailRecipient,
  type MailScene,
  type MailSmtpConfig
} from '@/api/monitor'

const activeTab = ref('smtp')
const testing = ref(false)
const savingSmtp = ref(false)
const loadingRecipients = ref(false)
const loadingScenes = ref(false)
const savingRecipient = ref(false)
const savingScene = ref(false)
const savingSceneBind = ref(false)
const recipients = ref<MailRecipient[]>([])
const scenes = ref<MailScene[]>([])
const smtpFormRef = ref<FormInstance>()
const recipientFormRef = ref<FormInstance>()
const sceneFormRef = ref<FormInstance>()

const smtpForm = reactive<MailSmtpConfig>({
  configName: '默认 SMTP',
  host: '',
  port: 465,
  username: '',
  password: '',
  passwordConfigured: false,
  fromEmail: '',
  fromName: 'Sub2 Monitor',
  useTls: false,
  useSsl: true,
  isEnabled: true,
  isDefault: true
})

const smtpRules: FormRules = {
  host: [{ required: true, message: '请输入 SMTP 主机', trigger: 'blur' }],
  port: [{ required: true, message: '请输入 SMTP 端口', trigger: 'blur' }],
  username: [{ required: true, message: '请输入 SMTP 用户名', trigger: 'blur' }],
  fromEmail: [{ required: true, message: '请输入发件人邮箱', trigger: 'blur' }]
}

const recipientDialogVisible = ref(false)
const recipientDialogMode = ref<'create' | 'edit'>('create')
const recipientForm = reactive({
  id: '' as Id,
  email: '',
  name: '',
  isEnabled: true
})
const recipientRules: FormRules = {
  email: [{ required: true, message: '请输入邮箱', trigger: 'blur' }]
}

const sceneDialogVisible = ref(false)
const sceneDialogMode = ref<'create' | 'edit'>('create')
const sceneForm = reactive({
  id: '' as Id,
  sceneKey: '',
  sceneName: '',
  description: '',
  isEnabled: true
})
const sceneRules: FormRules = {
  sceneKey: [
    { required: true, message: '请输入场景标识', trigger: 'blur' },
    {
      pattern: /^[a-z][a-z0-9_]{1,99}$/,
      message: '只能使用小写字母、数字和下划线，并以小写字母开头',
      trigger: 'blur'
    }
  ],
  sceneName: [{ required: true, message: '请输入场景名称', trigger: 'blur' }]
}

const sceneBindDialogVisible = ref(false)
const sceneBindForm = reactive({
  sceneKey: '',
  recipientId: '' as Id,
  recipientType: 'TO' as 'TO' | 'CC' | 'BCC'
})
const recipientTypes = [
  { label: '主送', value: 'TO' },
  { label: '抄送', value: 'CC' },
  { label: '密送', value: 'BCC' }
] as const

async function loadSmtp() {
  const response = await getMailSmtpConfig()
  if (!response.data) {
    return
  }
  Object.assign(smtpForm, response.data, { password: '' })
}

async function saveSmtp() {
  savingSmtp.value = true
  try {
    const valid = await smtpFormRef.value?.validate().catch(() => false)
    if (!valid) {
      return
    }
    const response = await saveMailSmtpConfig({ ...smtpForm })
    Object.assign(smtpForm, response.data, { password: '' })
    ElMessage.success('SMTP 配置已保存')
  } finally {
    savingSmtp.value = false
  }
}

async function testSmtp() {
  testing.value = true
  try {
    await testMailSmtpConfig({ ...smtpForm })
    ElMessage.success('SMTP 连接成功')
  } finally {
    testing.value = false
  }
}

async function loadRecipients() {
  loadingRecipients.value = true
  try {
    const response = await listMailRecipients()
    recipients.value = response.data
  } finally {
    loadingRecipients.value = false
  }
}

async function loadScenes() {
  loadingScenes.value = true
  try {
    const response = await listMailScenes()
    scenes.value = response.data
    if (!sceneBindForm.sceneKey && scenes.value.length > 0) {
      sceneBindForm.sceneKey = scenes.value[0].sceneKey
    }
  } finally {
    loadingScenes.value = false
  }
}

function openRecipientCreate() {
  recipientDialogMode.value = 'create'
  recipientForm.id = ''
  recipientForm.email = ''
  recipientForm.name = ''
  recipientForm.isEnabled = true
  recipientDialogVisible.value = true
}

function openRecipientEdit(row: MailRecipient) {
  recipientDialogMode.value = 'edit'
  recipientForm.id = row.id
  recipientForm.email = row.email
  recipientForm.name = row.name || ''
  recipientForm.isEnabled = row.isEnabled
  recipientDialogVisible.value = true
}

async function saveRecipientForm() {
  savingRecipient.value = true
  try {
    const valid = await recipientFormRef.value?.validate().catch(() => false)
    if (!valid) {
      return
    }
    if (recipientDialogMode.value === 'create') {
      await saveMailRecipient({
        email: recipientForm.email,
        name: recipientForm.name,
        isEnabled: recipientForm.isEnabled
      })
      ElMessage.success('收件人已新增')
    } else {
      await updateMailRecipient({ ...recipientForm })
      ElMessage.success('收件人已更新')
    }
    recipientDialogVisible.value = false
    await loadRecipients()
    await loadScenes()
  } finally {
    savingRecipient.value = false
  }
}

async function removeRecipient(row: MailRecipient) {
  await ElMessageBox.confirm(`确认删除收件人 ${row.email}？`, '删除收件人', {
    type: 'warning',
    confirmButtonText: '删除',
    cancelButtonText: '取消'
  })
  await deleteMailRecipient(row.id)
  ElMessage.success('收件人已删除')
  await loadRecipients()
  await loadScenes()
}

function openSceneCreate() {
  sceneDialogMode.value = 'create'
  sceneForm.id = ''
  sceneForm.sceneKey = ''
  sceneForm.sceneName = ''
  sceneForm.description = ''
  sceneForm.isEnabled = true
  sceneDialogVisible.value = true
}

function openSceneEdit(scene: MailScene) {
  sceneDialogMode.value = 'edit'
  sceneForm.id = scene.id
  sceneForm.sceneKey = scene.sceneKey
  sceneForm.sceneName = scene.sceneName
  sceneForm.description = scene.description || ''
  sceneForm.isEnabled = scene.isEnabled
  sceneDialogVisible.value = true
}

async function saveSceneForm() {
  savingScene.value = true
  try {
    const valid = await sceneFormRef.value?.validate().catch(() => false)
    if (!valid) {
      return
    }
    if (sceneDialogMode.value === 'create') {
      await saveMailScene({ ...sceneForm })
      ElMessage.success('通知场景已新增')
    } else {
      await updateMailScene({ ...sceneForm })
      ElMessage.success('通知场景已更新')
    }
    sceneDialogVisible.value = false
    await loadScenes()
  } finally {
    savingScene.value = false
  }
}

async function toggleScene(scene: MailScene) {
  await updateMailScene({
    id: scene.id,
    sceneKey: scene.sceneKey,
    sceneName: scene.sceneName,
    description: scene.description,
    isEnabled: !scene.isEnabled
  })
  ElMessage.success(scene.isEnabled ? '通知场景已停用' : '通知场景已启用')
  await loadScenes()
}

async function removeScene(scene: MailScene) {
  await ElMessageBox.confirm(`确认删除通知场景 ${scene.sceneName}？绑定的收件人关系也会一并删除。`, '删除通知场景', {
    type: 'warning',
    confirmButtonText: '删除',
    cancelButtonText: '取消'
  })
  await deleteMailScene(scene.id)
  ElMessage.success('通知场景已删除')
  await loadScenes()
}

function openSceneBind(sceneKey: string) {
  sceneBindForm.sceneKey = sceneKey
  sceneBindForm.recipientId = recipients.value[0]?.id || ''
  sceneBindForm.recipientType = 'TO'
  sceneBindDialogVisible.value = true
}

function sceneRecipientsByType(scene: MailScene, recipientType: 'TO' | 'CC' | 'BCC') {
  return scene.recipients.filter(recipient => recipient.recipientType === recipientType)
}

async function saveSceneBind() {
  if (!sceneBindForm.sceneKey || !sceneBindForm.recipientId) {
    ElMessage.warning('请选择场景和收件人')
    return
  }
  savingSceneBind.value = true
  try {
    await addMailSceneRecipient({ ...sceneBindForm })
    sceneBindDialogVisible.value = false
    ElMessage.success('收件人已绑定')
    await loadScenes()
  } finally {
    savingSceneBind.value = false
  }
}

async function removeSceneRecipient(relationId: Id) {
  await deleteSceneRecipient(relationId)
  ElMessage.success('绑定关系已移除')
  await loadScenes()
}

onMounted(async () => {
  await Promise.all([loadSmtp(), loadRecipients(), loadScenes()])
})
</script>

<style scoped>
.mail-tabs {
  margin-top: 20px;
}

.toolbar {
  display: flex;
  align-items: center;
  gap: 10px;
}

.smtp-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 18px 30px;
}

.smtp-grid :deep(.el-input-number) {
  width: 100%;
}

.form-help {
  margin-top: 6px;
  color: #64748b;
  font-size: 12px;
}

.switch-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 18px;
  border-top: 1px solid #edf2f7;
  padding: 18px 0;
}

.switch-row__title {
  color: #0f172a;
  font-weight: 700;
}

.switch-row__desc {
  margin-top: 5px;
  color: #64748b;
  font-size: 13px;
}

.mail-layout {
  display: flex;
  flex-direction: column;
  gap: 18px;
}

.recipient-card :deep(.el-card__body) {
  padding-top: 10px;
}

.scene-list {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  min-height: 240px;
  gap: 14px;
}

.scene-card {
  min-width: 0;
  border: 1px solid #e5e7eb;
  border-radius: 8px;
  background: #fff;
  padding: 14px;
}

.scene-card__header {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 12px;
}

.scene-card__actions {
  display: flex;
  flex: 0 0 auto;
  flex-wrap: wrap;
  justify-content: flex-end;
  gap: 6px;
}

.scene-card h2 {
  margin: 0;
  color: #0f172a;
  font-size: 15px;
}

.scene-card p {
  margin: 6px 0 0;
  color: #64748b;
  font-size: 12px;
}

.scene-card__toolbar {
  display: flex;
  justify-content: flex-end;
  margin-top: 12px;
}

.scene-recipient-groups {
  display: flex;
  flex-direction: column;
  gap: 10px;
  margin-top: 12px;
}

.scene-recipient-group {
  display: grid;
  grid-template-columns: 56px minmax(0, 1fr);
  gap: 10px;
  align-items: flex-start;
}

.scene-recipient-group__label {
  border-radius: 6px;
  background: #f1f5f9;
  color: #475569;
  font-size: 12px;
  font-weight: 700;
  line-height: 24px;
  text-align: center;
}

.scene-recipient-group__body {
  display: flex;
  min-height: 24px;
  flex-wrap: wrap;
  gap: 8px;
  align-items: center;
}

.scene-recipient-empty {
  color: #94a3b8;
  font-size: 12px;
  line-height: 24px;
}

.recipient-email {
  color: #64748b;
}

:deep(.el-select) {
  width: 100%;
}

@media (max-width: 1500px) {
  .scene-list {
    grid-template-columns: minmax(0, 1fr);
  }
}
</style>
