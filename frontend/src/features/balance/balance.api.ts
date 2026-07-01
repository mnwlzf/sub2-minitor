import { http } from '../../api/http'
import type { BalanceHistoryResponse } from './balance.types'

export interface BalanceHistoryParams {
  keyword?: string
  enabled?: boolean | null
  startDate?: string
  endDate?: string
}

export const listBalanceHistory = async (params: BalanceHistoryParams) => {
  const { data } = await http.get<BalanceHistoryResponse>('/collect/balances', { params })
  return data
}

