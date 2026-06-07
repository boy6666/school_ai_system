import request from '@/utils/request'

export type TaskStatus = 'todo' | 'doing' | 'done'
export type TaskPriority = 'high' | 'middle' | 'low'

export interface LearningTaskItem {
  id: number
  title: string
  courseName: string
  chapterName: string
  startTime: string
  endTime: string
  priority: TaskPriority
  status: TaskStatus
  progress: number
}

export interface LearningTaskSummary {
  todayCount: number
  weekCount: number
  doneCount: number
  averageProgress: number
}

export interface LearningTaskResponse {
  summary: LearningTaskSummary
  list: LearningTaskItem[]
}

export interface LearningTaskQuery {
  keyword?: string
  status?: string
  priority?: string
}

export function getLearningTasks(params: LearningTaskQuery) {
  return request.get<unknown, LearningTaskResponse>('/student/tasks', {
    params
  })
}

export function updateLearningTaskStatus(id: number, status: TaskStatus) {
  return request.post<unknown, { success: boolean }>(
    `/student/tasks/${id}/status`,
    { status }
  )
}