import { http } from './http'
import type { BalanceHistoryResponse } from '../types/balance'

export const listBalanceHistory = async (params: { keyword?: string; enabled?: boolean | null }) => {
  const { data } = await http.get<BalanceHistoryResponse>('/collect/balances', { params })
  return data
}
