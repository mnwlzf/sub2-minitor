import { http } from '../../api/http'
import type { AccountForm, AccountItem } from './account.types'

export const listAccounts = async (params: { platformId?: number | null; keyword?: string }) => {
  const { data } = await http.get<AccountItem[]>('/monitor/accounts', { params })
  return data
}

export const createAccount = async (payload: AccountForm) => {
  const { data } = await http.post<AccountItem>('/monitor/accounts', payload)
  return data
}

export const updateAccount = async (id: number, payload: AccountForm) => {
  const { data } = await http.put<AccountItem>(`/monitor/accounts/${id}`, payload)
  return data
}

export const deleteAccount = async (id: number) => {
  await http.delete(`/monitor/accounts/${id}`)
}
