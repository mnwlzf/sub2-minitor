export interface SchedulerTask {
  id?: number
  taskName: string
  taskGroup: string
  taskType: string
  baseUrl: string
  cron: string
  enabled: number
  notifyEnabled: number
  notifySceneId?: number | null
  notifyTrigger: string
  remark?: string
  createdAt?: string
  updatedAt?: string
}

export interface SchedulerTaskForm {
  taskName: string
  taskGroup: string
  taskType: string
  baseUrl: string
  cron: string
  enabled: number
  notifyEnabled: number
  notifySceneId?: number | null
  notifyTrigger: string
  remark?: string
}

export interface SchedulerTaskOption {
  label: string
  value: string
}
