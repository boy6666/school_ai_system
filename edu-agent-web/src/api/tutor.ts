import request from '@/utils/request'

export interface ChatRequest {
  message: string
  sessionId?: string
}

export interface TutorReply {
  answer: string
  intent: string
  routeReason: string
  evaluation: string
  resourceDir: string
}

export function sendTutorMessage(message: string, sessionId?: string, isOnboarding?: boolean, profile?: any) {
  return request.post<unknown, TutorReply>('/tutor/chat', {
    message,
    sessionId,
    isOnboarding: isOnboarding || false,
    profile: profile || undefined
  })
}

export function getTutorHistory(sessionId?: string) {
  return request.get<unknown, any[]>('/tutor/history', {
    params: { sessionId }
  })
}

export interface TutorSession {
  sessionId: string
  title: string
  time: string
}

export function getSessions() {
  return request.get<unknown, TutorSession[]>('/tutor/sessions')
}

/** 提交答案并获取 AI 讲解 */
export interface ExplainRequest {
  resourceId?: number
  question: string
  questionType: string
  userAnswer: string
  correctAnswer: string
  isCorrect: boolean
}

export interface ExplainResult {
  correct: boolean
  explanation: string
}

export function getExplain(params: ExplainRequest) {
  return request.post<unknown, ExplainResult>('/tutor/explain', params)
}

/** 获取已作答的题目列表 */
export interface AnsweredItem {
  question: string
  userAnswer: string
  correctAnswer: string
  isCorrect: number
  explanation: string
}

export function getAnsweredQuestions(resourceId: number) {
  return request.get<unknown, AnsweredItem[]>('/quiz/answered', {
    params: { resourceId }
  })
}
