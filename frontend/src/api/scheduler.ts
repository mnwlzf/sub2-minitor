import { http } from './http'
import type { SchedulerTask, SchedulerTaskForm, SchedulerTaskOption } from '../types/scheduler'

export const schedulerTaskTypeOptions: SchedulerTaskOption[] = [
  { label: '数据采集', value: 'DATA_COLLECT' },
]

export const listSchedulerTasks = async () => {
  const { data } = await http.get<SchedulerTask[]>('/scheduler/tasks')
  return data
}

export const createSchedulerTask = async (payload: SchedulerTaskForm) => {
  const { data } = await http.post<SchedulerTask>('/scheduler/tasks', payload)
  return data
}

export const updateSchedulerTask = async (id: number, payload: SchedulerTaskForm) => {
  const { data } = await http.put<SchedulerTask>(`/scheduler/tasks/${id}`, payload)
  return data
}

export const deleteSchedulerTask = async (id: number) => {
  await http.delete(`/scheduler/tasks/${id}`)
}

export const pauseSchedulerTask = async (id: number) => {
  await http.post(`/scheduler/tasks/${id}/pause`)
}

export const resumeSchedulerTask = async (id: number) => {
  await http.post(`/scheduler/tasks/${id}/resume`)
}

export const triggerSchedulerTask = async (id: number) => {
  await http.post(`/scheduler/tasks/${id}/trigger`)
}

export const syncSchedulerTasks = async () => {
  await http.post('/scheduler/tasks/sync')
}
