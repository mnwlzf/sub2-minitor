export interface MailSmtpConfigItem {
  id?: number
  configName: string
  host: string
  port: number
  username: string
  password?: string | null
  fromEmail: string
  fromName?: string | null
  enabled: number
  useTls: number
  useSsl: number
  isDefault: number
  remark?: string | null
  createdAt?: string
  updatedAt?: string
}

export interface MailRecipientItem {
  id?: number
  email: string
  recipientName?: string | null
  enabled: number
  remark?: string | null
  createdAt?: string
  updatedAt?: string
}

export interface MailNotifySceneRecipientItem {
  id?: number
  email: string
  recipientName?: string | null
}

export interface MailNotifySceneItem {
  id?: number
  sceneCode: string
  sceneName: string
  description?: string | null
  enabled: number
  smtpConfigId?: number | null
  smtpConfigName?: string | null
  subjectTemplate?: string | null
  contentTemplate?: string | null
  toRecipients: MailNotifySceneRecipientItem[]
  ccRecipients: MailNotifySceneRecipientItem[]
  bccRecipients: MailNotifySceneRecipientItem[]
  createdAt?: string
  updatedAt?: string
}

export interface MailSmtpConfigForm {
  configName: string
  host: string
  port: number | null
  username: string
  password: string
  fromEmail: string
  fromName: string
  enabled: boolean
  useTls: boolean
  useSsl: boolean
  isDefault: boolean
  remark: string
}

export interface MailRecipientForm {
  email: string
  recipientName: string
  enabled: boolean
  remark: string
}

export interface MailNotifySceneForm {
  sceneCode: string
  sceneName: string
  description: string
  enabled: boolean
  smtpConfigId: number | null
  subjectTemplate: string
  contentTemplate: string
  toRecipientIds: number[]
  ccRecipientIds: number[]
  bccRecipientIds: number[]
}
