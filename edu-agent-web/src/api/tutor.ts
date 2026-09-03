import request from '@/utils/request'

export interface TutorReply {
  intent?: string
  final_answer?: string
  profile?: Record<string, unknown>
  resources?: unknown
  learning_path?: unknown
  safety_report?: unknown
  evaluation_report?: unknown
  resource_dir?: string
  profile_complete?: boolean
}

export interface TutorHistoryItem {
  role: 'user' | 'assistant'
  content: string
  time?: number | string
}

export function sendTutorMessage(
  message: string,
  studentId: string,
  sessionId?: string,
  profile?: Record<string, unknown>
): Promise<TutorReply> {
  return request.post<unknown, TutorReply>(
    '/edu-agent-ai/chat',
    {
      user_input: message,
      student_id: studentId,
      session_id: sessionId,
      profile
    }
  )
}

export function getTutorHistory(sessionId?: string) {
  return request.get<unknown, TutorHistoryItem[]>('/edu-agent-learning/conversations', {
    params: { sessionId }
  })
}

export interface TutorSession {
  sessionId: string
  title: string
  time: string
}

export function getSessions() {
  return request.get<unknown, TutorSession[]>('/edu-agent-learning/conversations/sessions')
}

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
  return request.post<unknown, ExplainResult>('/edu-agent-ai/explain', params)
}

export interface AnsweredItem {
  question: string
  userAnswer: string
  correctAnswer: string
  isCorrect: number
  explanation: string
}

export function getAnsweredQuestions(resourceId: number) {
  return request.get<unknown, AnsweredItem[]>('/edu-agent-learning/quiz/answered', {
    params: { resourceId }
  })
}

export interface WrongQuestionItem {
  id: number
  question: string
  questionType: string
  userAnswer: string
  correctAnswer: string
  explanation: string
  createTime: string
}

export function getWrongQuestions() {
  return request.get<unknown, WrongQuestionItem[]>('/edu-agent-learning/quiz/wrong-questions')
}

export function getWrongQuestionById(id: number) {
  return request.get<unknown, WrongQuestionItem>(
    `/edu-agent-learning/quiz/wrong-questions/${id}`
  )
}