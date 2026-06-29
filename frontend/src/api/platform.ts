import { http } from './http'
import type { PlatformForm, PlatformItem, PlatformSummaryResponse } from '../types/platform'

export const listPlatforms = async (params: { keyword?: string; enabled?: boolean | null }) => {
  const { data } = await http.get<PlatformSummaryResponse>('/monitor/platforms', { params })
  return data
}

export const createPlatform = async (payload: PlatformForm) => {
  const { data } = await http.post<PlatformItem>('/monitor/platforms', payload)
  return data
}

export const updatePlatform = async (id: number, payload: PlatformForm) => {
  const { data } = await http.put<PlatformItem>(`/monitor/platforms/${id}`, payload)
  return data
}

export const deletePlatform = async (id: number) => {
  await http.delete(`/monitor/platforms/${id}`)
}

export const enablePlatform = async (id: number) => {
  await http.post(`/monitor/platforms/${id}/enable`)
}

export const disablePlatform = async (id: number) => {
  await http.post(`/monitor/platforms/${id}/disable`)
}

export const collectPlatform = async (id: number) => {
  await http.post(`/monitor/platforms/${id}/collect`)
}
