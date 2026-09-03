import request from '@/utils/request'
import type { PageResult } from '@/utils/request'

export interface PracticeListParams {
  subject?: string
  difficulty?: string
  keyword?: string
  page: number
  pageSize: number
}

export interface PracticeQuestion {
  id: number
  title: string
  description: string
  subject: string
  difficulty: string
  requirements?: string[]
  views?: number
  completed?: number
}

export interface PracticeSubmitParams {
  questionId: number
  answer: string
}

export interface PracticeSubmitResult {
  score?: number
  evaluation?: string
  explanation?: string
}

export interface PracticeProgress {
  completed: number
  total: number
  progress: number
}

export const getPracticeList = (params: PracticeListParams) => {
  return request.get<unknown, PageResult<PracticeQuestion>>(
    '/edu-agent-learning/practice/list',
    { params }
  )
}

export const getPracticeDetail = (id: number) => {
  return request.get<unknown, PracticeQuestion>(`/edu-agent-learning/practice/${id}`)
}

export const submitAnswer = (params: PracticeSubmitParams) => {
  return request.post<unknown, PracticeSubmitResult>(
    '/edu-agent-learning/practice/submit',
    params
  )
}

export const getPracticeProgress = () => {
  return request.get<unknown, PracticeProgress>('/edu-agent-learning/practice/progress')
}