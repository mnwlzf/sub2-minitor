import { http } from './http'

export type Id = string

export interface Platform {
  id: Id
  baseUrl: string
  name: string
  type: PlatformType
  isEnabled: boolean
  rechargeAmount: number
  receivedAmount: number
}

export type PlatformType = 'sub2Api' | 'newApi'

export interface PlatformAccountSummary {
  accountId: Id
  username: string
  latestBalance: number
  todayConsume: number
  actualConsume: number
  testModel?: string
  lastCollectTime?: string
}

export interface PlatformSummary {
  platformId: Id
  platformName: string
  baseUrl: string
  type: PlatformType
  isEnabled: boolean
  rechargeAmount: number
  receivedAmount: number
  accountCount: number
  totalBalance: number
  totalTodayConsume: number
  totalPlatformDeduct: number
  totalActualConsume: number
  avgDeductRate: number
  lastCollectTime?: string
  accounts: PlatformAccountSummary[]
}

export interface PlatformBalanceTrendPoint {
  time: string
  balance: number
}

export interface PlatformBalanceTrend {
  platformId: Id
  platformName: string
  cronExpression: string
  accounts: PlatformBalanceAccountTrend[]
}

export interface PlatformBalanceAccountTrend {
  accountId: Id
  username: string
  points: PlatformBalanceTrendPoint[]
}

export function listPlatforms(params: { pageNo?: number; pageSize?: number; keyword?: string; isEnabled?: boolean }) {
  return http.get('/platforms', { params })
}

export function listPlatformSummaries(params: { pageNo?: number; pageSize?: number; keyword?: string; isEnabled?: boolean }) {
  return http.get('/platforms/summary', { params })
}

export function savePlatform(data: Omit<Platform, 'id'>) {
  return http.post('/platforms', data)
}

export function updatePlatform(data: Platform) {
  return http.put('/platforms', data)
}

export function collectPlatform(platformId: Id) {
  return http.post(`/platforms/${platformId}/collect`)
}

export function getPlatformBalanceTrend(platformId: Id, params: { limit?: number } = {}) {
  return http.get(`/platforms/${platformId}/balance-trend`, { params })
}

export interface Account {
  id: Id
  username: string
  platformId: Id
  platformName?: string
  platformBaseUrl?: string
  platformType?: PlatformType
  testModel?: string
  createTime?: string
}

export interface AccountPayload {
  id?: Id
  username: string
  password?: string
  platformId: Id
  testModel?: string
}

export function listAccounts(params: { pageNo?: number; pageSize?: number; platformId?: Id; keyword?: string }) {
  return http.get('/accounts', { params })
}

export function saveAccount(data: AccountPayload) {
  return http.post('/accounts', data)
}

export function updateAccount(data: AccountPayload) {
  return http.put('/accounts', data)
}

export function deleteAccount(id: Id) {
  return http.delete(`/accounts/${id}`)
}

export interface GroupRate {
  groupName: string
  currentRate: number
  actualRate: number
  collectTime?: string
}

export interface PlatformGroupSummary {
  platformId: Id
  platformName: string
  baseUrl: string
  type: PlatformType
  isEnabled: boolean
  rechargeAmount: number
  receivedAmount: number
  deductRate: number
  groupCount: number
  lastCollectTime?: string
  groups: GroupRate[]
}

export function listPlatformGroups(params: { pageNo?: number; pageSize?: number; keyword?: string; isEnabled?: boolean }) {
  return http.get('/platform-groups', { params })
}

export interface TaskSchedule {
  id: Id
  taskKey: string
  taskName: string
  taskGroup?: string
  cronExpression: string
  jobClass: string
  description?: string
  isEnabled: boolean
  createTime?: string
  updateTime?: string
}

export interface TaskExecutionLog {
  id: Id
  taskKey: string
  taskName: string
  cronExpression?: string
  triggerType: string
  status: string
  message?: string
  fireTime?: string
  finishTime?: string
  createTime?: string
}

export function listTasks(params: { pageNo?: number; pageSize?: number; keyword?: string; taskGroup?: string }) {
  return http.get('/tasks', { params })
}

export function saveTask(data: {
  taskKey: string
  taskName: string
  taskGroup: string
  cronExpression: string
  jobClass: string
  description?: string
  isEnabled?: boolean
}) {
  return http.post('/tasks', data)
}

export function updateTask(data: {
  taskKey: string
  taskName: string
  taskGroup: string
  cronExpression: string
  jobClass: string
  description?: string
  isEnabled?: boolean
}) {
  return http.put('/tasks', data)
}

export function runTask(data: { taskKey: string; taskGroup: string }) {
  return http.post('/tasks/run', data)
}

export function pauseTask(data: { taskKey: string; taskGroup: string }) {
  return http.post('/tasks/pause', data)
}

export function resumeTask(data: { taskKey: string; taskGroup: string }) {
  return http.post('/tasks/resume', data)
}

export function removeTask(data: { taskKey: string; taskGroup: string }) {
  return http.post('/tasks/remove', data)
}

export function previewCron(params: { cronExpression: string; count?: number }) {
  return http.get('/tasks/preview', { params })
}

export function initBalanceCollectionTask(data: {
  cronExpression: string
  description?: string
}) {
  return http.post('/tasks/balance-collection/init', data)
}

export function updateBalanceCollectionCron(cronExpression: string) {
  return http.put('/tasks/balance-collection/cron', null, { params: { cronExpression } })
}

export function runBalanceCollectionNow() {
  return http.post('/tasks/balance-collection/run')
}

export function listBalanceCollectionLogs(params: { pageNo?: number; pageSize?: number }) {
  return http.get('/tasks/balance-collection/logs', { params })
}

export function listTaskLogs(params: { pageNo?: number; pageSize?: number; taskKey?: string }) {
  return http.get('/tasks/logs', { params })
}

export interface MailSmtpConfig {
  id?: Id
  configName?: string
  host: string
  port: number
  username: string
  password?: string
  passwordConfigured?: boolean
  fromEmail: string
  fromName?: string
  useTls: boolean
  useSsl: boolean
  isEnabled: boolean
  isDefault?: boolean
}

export interface MailRecipient {
  id: Id
  email: string
  name?: string
  isEnabled: boolean
  createTime?: string
  updateTime?: string
}

export interface MailSceneRecipient {
  relationId: Id
  recipientId: Id
  email: string
  name?: string
  isEnabled: boolean
  recipientType: 'TO' | 'CC' | 'BCC'
}

export interface MailScene {
  id: Id
  sceneKey: string
  sceneName: string
  description?: string
  isEnabled: boolean
  recipients: MailSceneRecipient[]
}

export interface MailScenePayload {
  id?: Id
  sceneKey: string
  sceneName: string
  description?: string
  isEnabled: boolean
}

export function getMailSmtpConfig() {
  return http.get('/mail-settings/smtp')
}

export function saveMailSmtpConfig(data: MailSmtpConfig) {
  return http.put('/mail-settings/smtp', data)
}

export function testMailSmtpConfig(data: MailSmtpConfig) {
  return http.post('/mail-settings/smtp/test', data)
}

export function listMailRecipients() {
  return http.get('/mail-settings/recipients')
}

export function saveMailRecipient(data: Omit<MailRecipient, 'id'>) {
  return http.post('/mail-settings/recipients', data)
}

export function updateMailRecipient(data: MailRecipient) {
  return http.put('/mail-settings/recipients', data)
}

export function deleteMailRecipient(id: Id) {
  return http.delete(`/mail-settings/recipients/${id}`)
}

export function listMailScenes() {
  return http.get('/mail-settings/scenes')
}

export function saveMailScene(data: MailScenePayload) {
  return http.post('/mail-settings/scenes', data)
}

export function updateMailScene(data: MailScenePayload) {
  return http.put('/mail-settings/scenes', data)
}

export function deleteMailScene(id: Id) {
  return http.delete(`/mail-settings/scenes/${id}`)
}

export function addMailSceneRecipient(data: { sceneKey: string; recipientId: Id; recipientType: 'TO' | 'CC' | 'BCC' }) {
  return http.post('/mail-settings/scenes/recipients', data)
}

export function removeMailSceneRecipient(relationId: Id) {
  return http.delete(`/mail-settings/scenes/recipients/${relationId}`)
}
