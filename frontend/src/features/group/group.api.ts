import { http } from '../../api/http'
import type { CollectGroupResponse } from './group.types'

export const listCollectGroups = async (params: { keyword?: string; enabled?: boolean | null }) => {
  const { data } = await http.get<CollectGroupResponse>('/collect/groups', { params })
  return data
}

