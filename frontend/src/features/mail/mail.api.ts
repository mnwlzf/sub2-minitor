import { http } from '../../api/http'
import type {
  MailNotifySceneForm,
  MailNotifySceneItem,
  MailRecipientForm,
  MailRecipientItem,
  MailSmtpConfigForm,
  MailSmtpConfigItem,
} from './mail.types'

export const listMailSmtpConfigs = async () => {
  const { data } = await http.get<MailSmtpConfigItem[]>('/mail/smtp-configs')
  return data
}

export const createMailSmtpConfig = async (payload: MailSmtpConfigForm) => {
  const { data } = await http.post<MailSmtpConfigItem>('/mail/smtp-configs', payload)
  return data
}

export const updateMailSmtpConfig = async (id: number, payload: MailSmtpConfigForm) => {
  const { data } = await http.put<MailSmtpConfigItem>(`/mail/smtp-configs/${id}`, payload)
  return data
}

export const deleteMailSmtpConfig = async (id: number) => {
  await http.delete(`/mail/smtp-configs/${id}`)
}

export const testMailSmtpConfig = async (payload: MailSmtpConfigForm) => {
  await http.post('/mail/smtp-configs/test', payload)
}

export const listMailRecipients = async () => {
  const { data } = await http.get<MailRecipientItem[]>('/mail/recipients')
  return data
}

export const createMailRecipient = async (payload: MailRecipientForm) => {
  const { data } = await http.post<MailRecipientItem>('/mail/recipients', payload)
  return data
}

export const updateMailRecipient = async (id: number, payload: MailRecipientForm) => {
  const { data } = await http.put<MailRecipientItem>(`/mail/recipients/${id}`, payload)
  return data
}

export const deleteMailRecipient = async (id: number) => {
  await http.delete(`/mail/recipients/${id}`)
}

export const listMailScenes = async () => {
  const { data } = await http.get<MailNotifySceneItem[]>('/mail/scenes')
  return data
}

export const createMailScene = async (payload: MailNotifySceneForm) => {
  const { data } = await http.post<MailNotifySceneItem>('/mail/scenes', payload)
  return data
}

export const updateMailScene = async (id: number, payload: MailNotifySceneForm) => {
  const { data } = await http.put<MailNotifySceneItem>(`/mail/scenes/${id}`, payload)
  return data
}

export const deleteMailScene = async (id: number) => {
  await http.delete(`/mail/scenes/${id}`)
}
