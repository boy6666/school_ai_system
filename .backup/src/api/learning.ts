import request from '@/utils/request'

export function logStudy(params: {
  module: string
  durationSec: number
  chapterId?: number
  noteId?: number
}) {
  return request.post('/learning/log', params)
}

export function getSummary() {
  return request.get<unknown, { today: any[]; totalSec: number }>('/learning/summary')
}

export function getPath() {
  return request.get<unknown, any>('/learning/path')
}

export function updatePath(data: { pace?: string; progress?: number; steps?: string }) {
  return request.put('/learning/path', data)
}

export function regeneratePath() {
  return request.post<unknown, any>('/learning/regenerate')
}

export function updateTask(taskId: number, status: string) {
  return request.put(`/learning/task/${taskId}`, { status })
}

export function updateGoal(goal: string) {
  return request.put('/learning/goal', { goal })
}

export function getHistory() {
  return request.get<unknown, any[]>('/learning/history')
}

export function dailyTrend() { return request.get<unknown, any[]>('/learning/daily-trend') }

export function getEvaluation() { return request.get<unknown, any>('/learning/evaluation') }

export function initLearning(message?: string) {
  return request.post<unknown, any>('/learning/init', { message: message || '你好，我是新用户' })
}
