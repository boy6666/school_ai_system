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

export interface HistoryItem {
  answer: string
  intent: string
}

export function sendTutorMessage(message: string, sessionId?: string) {
  return request.post<unknown, TutorReply>('/tutor/chat', {
    message,
    sessionId
  })
}

export function getTutorHistory(sessionId?: string) {
  return request.get<unknown, HistoryItem[]>('/tutor/history', {
    params: { sessionId }
  })
}
