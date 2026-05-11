import request from '@/utils/request'

export interface ProfileMessage {
  role: 'user' | 'assistant'
  content: string
}

export interface StudentProfile {
  studentId: number
  overallType: string
  knowledgeBase: string
  learningGoal: string
  masteryLevel: string
  cognitiveStyle: string
  errorTypes: string[]
  learningAutonomy: string
  suggestions: string[]
}

export function getProfileQuestions() {
  return request.get('/student/profile/questions')
}

export function sendProfileMessage(content: string) {
  return request.post('/student/profile/chat', { content })
}

export function extractStudentProfile(messages: ProfileMessage[]) {
  return request.post('/student/profile/extract', { messages })
}

export function getStudentProfile(studentId: number) {
  return request.get(`/student/profile/${studentId}`)
}

export function getProfileAdvice(studentId: number) {
  return request.get(`/student/profile/${studentId}/advice`)
}