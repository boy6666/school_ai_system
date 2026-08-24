import request from '@/utils/request'

export interface StudyLogParams {
  module: string
  durationSec: number
  chapterId?: number
  noteId?: number
}

export interface StudySummary {
  today: Record<string, unknown>[]
  totalSec: number
}

export interface LearningPathTask {
  title: string
  duration?: number
  status?: number
  progress?: number
}

export interface LearningPathStage {
  name: string
  tasks?: LearningPathTask[]
}

export interface LearningPathData {
  goal?: string
  estimatedCompletion?: string
  totalHours?: number
  suggestions?: string
  applicationAdvice?: string
  examAdvice?: string
  recommendTime?: string
  stages?: LearningPathStage[]
  progress?: number
}

export interface DashboardReviewItem {
  module: string
  total: number
}

export interface DashboardSummary {
  totalSec: number
  today: DashboardReviewItem[]
}

export interface DashboardTask {
  id: number | string
  title: string
  priority?: string
  status?: string
}

export interface DashboardEvaluationDimension {
  score?: number
}

export interface DashboardEvaluation extends Record<string, unknown> {
  learning_goal?: string
  cognitive_style?: string
  course?: string
  weaknesses?: string[] | string
  pace?: string
}

export interface DashboardAiSummary {
  summary?: string
  feedback?: string
  strengths?: string
  weaknessAnalysis?: string
  suggestion?: string
  focusNext?: string
}

export interface LearningPathUpdateResult {
  progress?: number
}

export function logStudy(params: StudyLogParams) {
  return request.post<unknown, void>('/edu-agent-learning/learning/log', params)
}

export function getSummary() {
  return request.get<unknown, StudySummary>('/edu-agent-learning/learning/summary')
}

export function getPath() {
  return request.get<unknown, LearningPathData>('/edu-agent-learning/learning/path')
}

export function regeneratePath() {
  return request.post<unknown, LearningPathData>('/edu-agent-learning/learning/regenerate')
}

export function updateTask(taskId: number, status: string) {
  return request.put<unknown, void>(`/edu-agent-learning/learning/task/${taskId}`, { status })
}

export function updateGoal(goal: string) {
  return request.put<unknown, void>('/edu-agent-learning/learning/goal', { goal })
}

export function getHistory() {
  return request.get<unknown, Record<string, unknown>[]>('/edu-agent-learning/learning/history')
}

export function dailyTrend() {
  return request.get<unknown, Record<string, unknown>[]>('/edu-agent-learning/learning/daily-trend')
}

export function getEvaluation() {
  return request.get<unknown, Record<string, unknown>>('/edu-agent-learning/learning/evaluation')
}

export function initLearning(message?: string) {
  return request.post<unknown, Record<string, unknown>>('/edu-agent-learning/learning/init', {
    message: message || '你好，我是新用户'
  })
}

export function getLearningPath() {
  return request.get<unknown, LearningPathData>('/edu-agent-learning/learning-path/current')
}

export function generateLearningPath() {
  return request.post<unknown, LearningPathData>('/edu-agent-learning/learning-path/generate')
}

export function updateTaskStatus(stageName: string, taskTitle: string, completed: boolean) {
  return request.put<unknown, LearningPathUpdateResult>(
    '/edu-agent-learning/learning-path/task',
    { stageName, taskTitle, completed }
  )
}

export function getDashboardSummary() {
  return request.get<unknown, DashboardSummary>('/edu-agent-learning/dashboard/summary')
}

export function getDashboardTasks() {
  return request.get<unknown, DashboardTask[]>('/edu-agent-learning/dashboard/tasks')
}

export function getDashboardPath() {
  return request.get<unknown, LearningPathData>('/edu-agent-learning/dashboard/path')
}

export function getDashboardEvaluation() {
  return request.get<unknown, DashboardEvaluation>('/edu-agent-learning/dashboard/evaluation')
}

export function getDashboardAiSummary() {
  return request.get<unknown, DashboardAiSummary>('/edu-agent-learning/dashboard/ai-summary')
}

export function generateDashboardAiSummary() {
  return request.post<unknown, DashboardAiSummary>('/edu-agent-learning/dashboard/ai-summary')
}

export function getDashboardLearningReview() {
  return request.get<unknown, DashboardAiSummary>('/edu-agent-learning/dashboard/learning-review')
}

export function generateDashboardLearningReview() {
  return request.post<unknown, DashboardAiSummary>('/edu-agent-learning/dashboard/learning-review')
}