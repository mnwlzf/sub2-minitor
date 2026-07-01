<template>
  <AppShell title="邮件设置" subtitle="维护 SMTP 发信服务，并按通知场景配置主送、抄送、密送收件人。">
    <template #actions>
      <el-button :loading="loading" @click="loadAll">刷新</el-button>
    </template>

    <div class="mail-page-head">
      <h2>邮件设置</h2>
      <p>维护 SMTP 发信服务，并按通知场景配置主送、抄送、密送收件人。</p>
    </div>

    <el-tabs v-model="activeTab" class="mail-tabs">
      <el-tab-pane label="SMTP 设置" name="smtp" />
      <el-tab-pane label="通知收件人" name="recipients" />
      <el-tab-pane label="通知场景" name="scenes" />
    </el-tabs>

    <section v-show="activeTab === 'smtp'" class="mail-section">
      <div class="mail-section-card">
        <div class="mail-section-head">
          <span>SMTP 配置</span>
          <div class="mail-actions">
            <el-button @click="openSmtpDialog()">新增配置</el-button>
          </div>
        </div>
        <div class="mail-list">
          <div v-for="item in smtpConfigs" :key="item.id" class="mail-row">
            <div class="mail-main">
              <strong>{{ item.configName }}</strong>
              <span>{{ item.host }}:{{ item.port }}</span>
            </div>
            <div class="mail-meta">
              <span>发件人 {{ item.fromEmail }}</span>
              <span>{{ item.enabled === 1 ? '启用' : '停用' }}</span>
              <el-tag v-if="item.isDefault === 1" type="success" effect="light">默认</el-tag>
            </div>
            <div class="mail-ops">
              <el-button link type="primary" @click="openSmtpDialog(item)">编辑</el-button>
              <el-button link type="warning" @click="handleSmtpTest(item)">测试连接</el-button>
              <el-button link type="danger" @click="handleDeleteSmtp(item)">删除</el-button>
            </div>
          </div>
          <el-empty v-if="!smtpConfigs.length" description="暂无 SMTP 配置" />
        </div>
      </div>
    </section>

    <section v-show="activeTab === 'recipients'" class="mail-section">
      <div class="mail-section-card">
        <div class="mail-section-head">
          <span>收件人</span>
          <div class="mail-actions">
            <el-button type="primary" @click="openRecipientDialog()">新增收件人</el-button>
          </div>
        </div>
        <el-table :data="recipients" class="mail-table" stripe>
          <el-table-column prop="email" label="邮箱" min-width="220" />
          <el-table-column prop="recipientName" label="名称" min-width="180" show-overflow-tooltip />
          <el-table-column label="状态" width="100">
            <template #default="{ row }">
              <el-tag :type="row.enabled === 1 ? 'success' : 'info'" size="small">
                {{ row.enabled === 1 ? '启用' : '停用' }}
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column prop="remark" label="备注" min-width="220" show-overflow-tooltip />
          <el-table-column label="操作" width="180" fixed="right">
            <template #default="{ row }">
              <el-button link type="primary" @click="openRecipientDialog(row)">编辑</el-button>
              <el-button link type="danger" @click="handleDeleteRecipient(row)">删除</el-button>
            </template>
          </el-table-column>
        </el-table>
      </div>
    </section>

    <section v-show="activeTab === 'scenes'" class="mail-section">
      <div class="mail-section-card">
        <div class="mail-section-head">
          <span>通知场景</span>
          <div class="mail-actions">
            <el-button type="primary" @click="openSceneDialog()">新增场景</el-button>
          </div>
        </div>
        <div class="scene-grid">
          <div v-for="scene in scenes" :key="scene.id" class="scene-card">
            <div class="scene-card-head">
              <div>
                <strong>{{ scene.sceneName }}</strong>
                <p>{{ scene.description || '暂无描述' }}</p>
              </div>
              <div class="scene-tags">
                <el-tag :type="scene.enabled === 1 ? 'success' : 'info'" size="small">
                  {{ scene.enabled === 1 ? '启用' : '停用' }}
                </el-tag>
                <el-tag v-if="scene.smtpConfigName" size="small" effect="light">
                  {{ scene.smtpConfigName }}
                </el-tag>
              </div>
            </div>

            <div class="scene-body">
              <div class="scene-line">
                <span>主送</span>
                <div class="recipient-pills">
                  <el-tag v-for="item in scene.toRecipients" :key="item.id" size="small" effect="light">
                    {{ item.email }}
                  </el-tag>
                  <span v-if="!scene.toRecipients.length" class="scene-empty">未配置</span>
                </div>
              </div>
              <div class="scene-line">
                <span>抄送</span>
                <div class="recipient-pills">
                  <el-tag v-for="item in scene.ccRecipients" :key="item.id" size="small" effect="light">
                    {{ item.email }}
                  </el-tag>
                  <span v-if="!scene.ccRecipients.length" class="scene-empty">未配置</span>
                </div>
              </div>
              <div class="scene-line">
                <span>密送</span>
                <div class="recipient-pills">
                  <el-tag v-for="item in scene.bccRecipients" :key="item.id" size="small" effect="light">
                    {{ item.email }}
                  </el-tag>
                  <span v-if="!scene.bccRecipients.length" class="scene-empty">未配置</span>
                </div>
              </div>
            </div>

            <div class="scene-actions">
              <el-button size="small" type="primary" plain @click="openSceneDialog(scene)">编辑</el-button>
              <el-button size="small" type="danger" plain @click="handleDeleteScene(scene)">删除</el-button>
            </div>
          </div>
        </div>
        <el-empty v-if="!scenes.length" description="暂无通知场景" />
      </div>
    </section>

    <el-dialog v-model="smtpDialogVisible" :title="smtpDialogTitle" width="760px" destroy-on-close>
      <el-form ref="smtpFormRef" :model="smtpForm" :rules="smtpRules" label-width="120px" class="mail-form">
        <el-form-item label="配置名称" prop="configName">
          <el-input v-model="smtpForm.configName" placeholder="default" />
        </el-form-item>
        <el-form-item label="SMTP 主机" prop="host">
          <el-input v-model="smtpForm.host" placeholder="smtp.qq.com" />
        </el-form-item>
        <el-form-item label="SMTP 端口" prop="port">
          <el-input-number v-model="smtpForm.port" :min="1" :max="65535" />
        </el-form-item>
        <el-form-item label="SMTP 用户名" prop="username">
          <el-input v-model="smtpForm.username" />
        </el-form-item>
        <el-form-item label="SMTP 密码" prop="password">
          <el-input v-model="smtpForm.password" type="password" show-password />
        </el-form-item>
        <el-form-item label="发件人邮箱" prop="fromEmail">
          <el-input v-model="smtpForm.fromEmail" />
        </el-form-item>
        <el-form-item label="发件人名称" prop="fromName">
          <el-input v-model="smtpForm.fromName" />
        </el-form-item>
        <el-form-item label="备注">
          <el-input v-model="smtpForm.remark" type="textarea" :rows="3" />
        </el-form-item>
        <el-form-item label="启用配置">
          <el-switch v-model="smtpForm.enabled" active-text="启用" inactive-text="停用" />
        </el-form-item>
        <el-form-item label="使用 TLS">
          <el-switch v-model="smtpForm.useTls" active-text="启用" inactive-text="停用" />
        </el-form-item>
        <el-form-item label="使用 SSL">
          <el-switch v-model="smtpForm.useSsl" active-text="启用" inactive-text="停用" />
        </el-form-item>
        <el-form-item label="默认配置">
          <el-switch v-model="smtpForm.isDefault" active-text="设为默认" inactive-text="非默认" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="smtpDialogVisible = false">取消</el-button>
        <el-button @click="submitSmtpTest">测试连接</el-button>
        <el-button type="primary" :loading="smtpSubmitLoading" @click="submitSmtpForm">保存</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="recipientDialogVisible" :title="recipientDialogTitle" width="640px" destroy-on-close>
      <el-form ref="recipientFormRef" :model="recipientForm" :rules="recipientRules" label-width="120px" class="mail-form">
        <el-form-item label="邮箱" prop="email">
          <el-input v-model="recipientForm.email" />
        </el-form-item>
        <el-form-item label="名称">
          <el-input v-model="recipientForm.recipientName" />
        </el-form-item>
        <el-form-item label="备注">
          <el-input v-model="recipientForm.remark" type="textarea" :rows="3" />
        </el-form-item>
        <el-form-item label="启用">
          <el-switch v-model="recipientForm.enabled" active-text="启用" inactive-text="停用" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="recipientDialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="recipientSubmitLoading" @click="submitRecipientForm">保存</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="sceneDialogVisible" :title="sceneDialogTitle" width="860px" destroy-on-close>
      <el-form ref="sceneFormRef" :model="sceneForm" :rules="sceneRules" label-width="120px" class="mail-form">
        <el-form-item label="场景编码" prop="sceneCode">
          <el-input v-model="sceneForm.sceneCode" placeholder="BALANCE_ALERT" />
        </el-form-item>
        <el-form-item label="场景名称" prop="sceneName">
          <el-input v-model="sceneForm.sceneName" placeholder="余额告警" />
        </el-form-item>
        <el-form-item label="SMTP 配置">
          <el-select v-model="sceneForm.smtpConfigId" clearable placeholder="请选择 SMTP 配置">
            <el-option v-for="item in smtpConfigs" :key="item.id" :label="item.configName" :value="item.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="说明">
          <el-input v-model="sceneForm.description" type="textarea" :rows="2" />
        </el-form-item>
        <el-form-item label="邮件标题">
          <el-input v-model="sceneForm.subjectTemplate" />
        </el-form-item>
        <el-form-item label="邮件正文">
          <el-input v-model="sceneForm.contentTemplate" type="textarea" :rows="4" />
        </el-form-item>
        <el-form-item label="主送">
          <el-select v-model="sceneForm.toRecipientIds" multiple filterable placeholder="选择收件人">
            <el-option v-for="item in recipients" :key="item.id" :label="item.email" :value="item.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="抄送">
          <el-select v-model="sceneForm.ccRecipientIds" multiple filterable placeholder="选择收件人">
            <el-option v-for="item in recipients" :key="item.id" :label="item.email" :value="item.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="密送">
          <el-select v-model="sceneForm.bccRecipientIds" multiple filterable placeholder="选择收件人">
            <el-option v-for="item in recipients" :key="item.id" :label="item.email" :value="item.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="启用">
          <el-switch v-model="sceneForm.enabled" active-text="启用" inactive-text="停用" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="sceneDialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="sceneSubmitLoading" @click="submitSceneForm">保存</el-button>
      </template>
    </el-dialog>
  </AppShell>
</template>

<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox, type FormInstance, type FormRules } from 'element-plus'
import AppShell from '../components/AppShell.vue'
import {
  createMailRecipient,
  createMailScene,
  createMailSmtpConfig,
  deleteMailRecipient,
  deleteMailScene,
  deleteMailSmtpConfig,
  listMailRecipients,
  listMailScenes,
  listMailSmtpConfigs,
  testMailSmtpConfig,
  updateMailRecipient,
  updateMailScene,
  updateMailSmtpConfig,
} from '../api/mail'
import type {
  MailNotifySceneForm,
  MailNotifySceneItem,
  MailRecipientForm,
  MailRecipientItem,
  MailSmtpConfigForm,
  MailSmtpConfigItem,
} from '../types/mail'

const loading = ref(false)
const activeTab = ref('smtp')

const smtpConfigs = ref<MailSmtpConfigItem[]>([])
const recipients = ref<MailRecipientItem[]>([])
const scenes = ref<MailNotifySceneItem[]>([])

const smtpDialogVisible = ref(false)
const recipientDialogVisible = ref(false)
const sceneDialogVisible = ref(false)

const smtpSubmitLoading = ref(false)
const recipientSubmitLoading = ref(false)
const sceneSubmitLoading = ref(false)

const smtpEditingId = ref<number | null>(null)
const recipientEditingId = ref<number | null>(null)
const sceneEditingId = ref<number | null>(null)

const smtpFormRef = ref<FormInstance>()
const recipientFormRef = ref<FormInstance>()
const sceneFormRef = ref<FormInstance>()

const smtpDefaultForm: MailSmtpConfigForm = {
  configName: 'default',
  host: '',
  port: 465,
  username: '',
  password: '',
  fromEmail: '',
  fromName: 'Sub2 Monitor',
  enabled: true,
  useTls: false,
  useSsl: true,
  isDefault: true,
  remark: '',
}

const recipientDefaultForm: MailRecipientForm = {
  email: '',
  recipientName: '',
  enabled: true,
  remark: '',
}

const sceneDefaultForm: MailNotifySceneForm = {
  sceneCode: '',
  sceneName: '',
  description: '',
  enabled: true,
  smtpConfigId: null,
  subjectTemplate: '',
  contentTemplate: '',
  toRecipientIds: [],
  ccRecipientIds: [],
  bccRecipientIds: [],
}

const smtpForm = reactive<MailSmtpConfigForm>({ ...smtpDefaultForm })
const recipientForm = reactive<MailRecipientForm>({ ...recipientDefaultForm })
const sceneForm = reactive<MailNotifySceneForm>({ ...sceneDefaultForm })

const smtpRules: FormRules<MailSmtpConfigForm> = {
  configName: [{ required: true, message: '请输入配置名称', trigger: 'blur' }],
  host: [{ required: true, message: '请输入 SMTP 主机', trigger: 'blur' }],
  port: [{ required: true, message: '请输入 SMTP 端口', trigger: 'change' }],
  username: [{ required: true, message: '请输入 SMTP 用户名', trigger: 'blur' }],
  fromEmail: [{ required: true, message: '请输入发件人邮箱', trigger: 'blur' }],
}

const recipientRules: FormRules<MailRecipientForm> = {
  email: [{ required: true, message: '请输入邮箱', trigger: 'blur' }],
}

const sceneRules: FormRules<MailNotifySceneForm> = {
  sceneCode: [{ required: true, message: '请输入场景编码', trigger: 'blur' }],
  sceneName: [{ required: true, message: '请输入场景名称', trigger: 'blur' }],
}

const smtpDialogTitle = computed(() => (smtpEditingId.value ? '编辑 SMTP 配置' : '新增 SMTP 配置'))
const recipientDialogTitle = computed(() => (recipientEditingId.value ? '编辑收件人' : '新增收件人'))
const sceneDialogTitle = computed(() => (sceneEditingId.value ? '编辑通知场景' : '新增通知场景'))

const loadAll = async () => {
  loading.value = true
  try {
    const [smtpData, recipientData, sceneData] = await Promise.all([
      listMailSmtpConfigs(),
      listMailRecipients(),
      listMailScenes(),
    ])
    smtpConfigs.value = smtpData ?? []
    recipients.value = recipientData ?? []
    scenes.value = sceneData ?? []
  } catch (error) {
    ElMessage.error('加载邮件设置失败')
  } finally {
    loading.value = false
  }
}

const resetSmtpForm = () => Object.assign(smtpForm, smtpDefaultForm)
const resetRecipientForm = () => Object.assign(recipientForm, recipientDefaultForm)
const resetSceneForm = () => Object.assign(sceneForm, sceneDefaultForm)

const openSmtpDialog = (item?: MailSmtpConfigItem) => {
  resetSmtpForm()
  smtpEditingId.value = item?.id ?? null
  if (item) {
    Object.assign(smtpForm, {
      configName: item.configName ?? '',
      host: item.host ?? '',
      port: item.port ?? 465,
      username: item.username ?? '',
      password: item.password ?? '',
      fromEmail: item.fromEmail ?? '',
      fromName: item.fromName ?? '',
      enabled: item.enabled === 1,
      useTls: item.useTls === 1,
      useSsl: item.useSsl === 1,
      isDefault: item.isDefault === 1,
      remark: item.remark ?? '',
    })
  }
  smtpDialogVisible.value = true
}

const openRecipientDialog = (item?: MailRecipientItem) => {
  resetRecipientForm()
  recipientEditingId.value = item?.id ?? null
  if (item) {
    Object.assign(recipientForm, {
      email: item.email ?? '',
      recipientName: item.recipientName ?? '',
      enabled: item.enabled === 1,
      remark: item.remark ?? '',
    })
  }
  recipientDialogVisible.value = true
}

const openSceneDialog = (item?: MailNotifySceneItem) => {
  resetSceneForm()
  sceneEditingId.value = item?.id ?? null
  if (item) {
    Object.assign(sceneForm, {
      sceneCode: item.sceneCode ?? '',
      sceneName: item.sceneName ?? '',
      description: item.description ?? '',
      enabled: item.enabled === 1,
      smtpConfigId: item.smtpConfigId ?? null,
      subjectTemplate: item.subjectTemplate ?? '',
      contentTemplate: item.contentTemplate ?? '',
      toRecipientIds: (item.toRecipients ?? []).map((entry) => entry.id).filter((value): value is number => !!value),
      ccRecipientIds: (item.ccRecipients ?? []).map((entry) => entry.id).filter((value): value is number => !!value),
      bccRecipientIds: (item.bccRecipients ?? []).map((entry) => entry.id).filter((value): value is number => !!value),
    })
  }
  sceneDialogVisible.value = true
}

const submitSmtpForm = async () => {
  if (!smtpFormRef.value) return
  const valid = await smtpFormRef.value.validate().catch(() => false)
  if (!valid) return
  smtpSubmitLoading.value = true
  try {
    const payload: MailSmtpConfigForm = {
      ...smtpForm,
      port: Number(smtpForm.port ?? 0),
      enabled: smtpForm.enabled,
      useTls: smtpForm.useTls,
      useSsl: smtpForm.useSsl,
      isDefault: smtpForm.isDefault,
      password: smtpForm.password,
    }
    if (smtpEditingId.value) {
      await updateMailSmtpConfig(smtpEditingId.value, payload)
      ElMessage.success('SMTP 配置已更新')
    } else {
      await createMailSmtpConfig(payload)
      ElMessage.success('SMTP 配置已创建')
    }
    smtpDialogVisible.value = false
    await loadAll()
  } catch (error) {
    ElMessage.error('保存 SMTP 配置失败')
  } finally {
    smtpSubmitLoading.value = false
  }
}

const submitSmtpTest = async () => {
  const payload: MailSmtpConfigForm = {
    ...smtpForm,
    port: Number(smtpForm.port ?? 0),
    enabled: true,
    useTls: smtpForm.useTls,
    useSsl: smtpForm.useSsl,
    isDefault: false,
    password: smtpForm.password,
  }
  try {
    await testMailSmtpConfig(payload)
    ElMessage.success('SMTP 连接测试成功')
  } catch (error) {
    ElMessage.error('SMTP 连接测试失败')
  }
}

const submitRecipientForm = async () => {
  if (!recipientFormRef.value) return
  const valid = await recipientFormRef.value.validate().catch(() => false)
  if (!valid) return
  recipientSubmitLoading.value = true
  try {
    const payload: MailRecipientForm = { ...recipientForm, enabled: recipientForm.enabled }
    if (recipientEditingId.value) {
      await updateMailRecipient(recipientEditingId.value, payload)
      ElMessage.success('收件人已更新')
    } else {
      await createMailRecipient(payload)
      ElMessage.success('收件人已创建')
    }
    recipientDialogVisible.value = false
    await loadAll()
  } catch (error) {
    ElMessage.error('保存收件人失败')
  } finally {
    recipientSubmitLoading.value = false
  }
}

const submitSceneForm = async () => {
  if (!sceneFormRef.value) return
  const valid = await sceneFormRef.value.validate().catch(() => false)
  if (!valid) return
  sceneSubmitLoading.value = true
  try {
    const payload: MailNotifySceneForm = {
      ...sceneForm,
      enabled: sceneForm.enabled,
      smtpConfigId: sceneForm.smtpConfigId,
      toRecipientIds: [...sceneForm.toRecipientIds],
      ccRecipientIds: [...sceneForm.ccRecipientIds],
      bccRecipientIds: [...sceneForm.bccRecipientIds],
    }
    if (sceneEditingId.value) {
      await updateMailScene(sceneEditingId.value, payload)
      ElMessage.success('通知场景已更新')
    } else {
      await createMailScene(payload)
      ElMessage.success('通知场景已创建')
    }
    sceneDialogVisible.value = false
    await loadAll()
  } catch (error) {
    ElMessage.error('保存通知场景失败')
  } finally {
    sceneSubmitLoading.value = false
  }
}

const handleDeleteSmtp = async (item: MailSmtpConfigItem) => {
  if (!item.id) return
  try {
    await ElMessageBox.confirm(`确认删除 SMTP 配置「${item.configName}」？`, '删除配置', { type: 'warning' })
    await deleteMailSmtpConfig(item.id)
    ElMessage.success('SMTP 配置已删除')
    await loadAll()
  } catch (error) {
    if (error !== 'cancel') ElMessage.error('删除失败')
  }
}

const handleDeleteRecipient = async (item: MailRecipientItem) => {
  if (!item.id) return
  try {
    await ElMessageBox.confirm(`确认删除收件人 ${item.email}？`, '删除收件人', { type: 'warning' })
    await deleteMailRecipient(item.id)
    ElMessage.success('收件人已删除')
    await loadAll()
  } catch (error) {
    if (error !== 'cancel') ElMessage.error('删除失败')
  }
}

const handleDeleteScene = async (item: MailNotifySceneItem) => {
  if (!item.id) return
  try {
    await ElMessageBox.confirm(`确认删除通知场景「${item.sceneName}」？`, '删除场景', { type: 'warning' })
    await deleteMailScene(item.id)
    ElMessage.success('通知场景已删除')
    await loadAll()
  } catch (error) {
    if (error !== 'cancel') ElMessage.error('删除失败')
  }
}

const handleSmtpTest = async (item: MailSmtpConfigItem) => {
  try {
    await testMailSmtpConfig({
      configName: item.configName,
      host: item.host,
      port: item.port,
      username: item.username,
      password: item.password ?? '',
      fromEmail: item.fromEmail,
      fromName: item.fromName ?? '',
      enabled: true,
      useTls: item.useTls === 1,
      useSsl: item.useSsl === 1,
      isDefault: false,
      remark: item.remark ?? '',
    })
    ElMessage.success('SMTP 连接测试成功')
  } catch (error) {
    ElMessage.error('SMTP 连接测试失败')
  }
}

onMounted(loadAll)
</script>
