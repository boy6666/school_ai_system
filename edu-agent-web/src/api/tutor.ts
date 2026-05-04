import request from '@/utils/request'

export interface TutorMessage {
  id: number
  role: 'user' | 'assistant'
  content: string
  time: string
}

export interface TutorSuggestion {
  id: number
  title: string
  prompt: string
}

export interface TutorSession {
  messages: TutorMessage[]
  suggestions: TutorSuggestion[]
}

export function getTutorSession() {
  return request.get<unknown, TutorSession>('/student/tutor/session')
}

export function sendTutorMessage(content: string) {
  return request.post<unknown, TutorMessage>('/student/tutor/chat', {
    content
  })
}