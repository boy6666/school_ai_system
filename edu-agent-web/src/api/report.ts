import request from '@/utils/request'

export interface ReportModuleDuration {
  module: string
  total: number
}

export interface ReportTrendItem {
  day: string
  total: number
}

export interface LearningReport {
  totalSec: number
  progress: number
  score: number
  modules: ReportModuleDuration[]
  trend: ReportTrendItem[]
  profile_suggestions?: string[] | string
  weaknesses?: string[]
}

export interface ReportStatistics extends Record<string, unknown> {}
export interface ReportComparison extends Record<string, unknown> {}

export const getLearningReport = () => {
  return request.get<unknown, LearningReport>('/edu-agent-learning/report/learning')
}

export const getStatistics = () => {
  return request.get<unknown, ReportStatistics>('/edu-agent-learning/report/statistics')
}

export const getComparison = () => {
  return request.get<unknown, ReportComparison>('/edu-agent-learning/report/comparison')
}

export const exportReport = () => {
  return request.get<unknown, Blob>('/edu-agent-learning/report/export', {
    responseType: 'blob'
  })
}