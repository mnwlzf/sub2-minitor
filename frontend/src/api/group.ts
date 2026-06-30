import { http } from './http'
import type { CollectGroupResponse } from '../types/group'

export const listCollectGroups = async (params: { keyword?: string; enabled?: boolean | null }) => {
  const { data } = await http.get<CollectGroupResponse>('/collect/groups', { params })
  return data
}
