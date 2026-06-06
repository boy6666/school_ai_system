import axios from 'axios'
import request from '@/utils/request'

const aiClient = axios.create({
  baseURL: '/ai',
  timeout: 60000,
})

export interface ProfileData {
  major?: string
  grade?: string
  course?: string
  topic?: string
  knowledge_base?: string
  learning_goal?: string
  current_mastery?: string
  cognitive_style?: string
  weaknesses?: string[]
  mistake_patterns?: string[]
  learning_behavior?: string
  resource_preference?: string[]
  pace?: string
  overall_type?: string
  profile_suggestions?: string[]
  last_score?: number
  last_suggestion?: string
  last_updated?: string
  exists?: boolean
}

export interface ProfileBuildParams {
  student_id: string
  learning_goal: string
  knowledge_base: string[]
  current_mastery: string
  cognitive_style: string
  mistake_patterns: string[]
  learning_behavior: string
  daily_hours: number
}

export const buildProfile = async (params: ProfileBuildParams): Promise<{ student_id: string; profile: ProfileData }> => {
  const res = await aiClient.post('/profile/build', params)
  return res.data
}

export const getProfileFromAI = async (studentId: string): Promise<{ student_id: string; profile: ProfileData; exists: boolean }> => {
  const res = await aiClient.get(`/profile/${studentId}`)
  return res.data
}

export const getProfile = (id: number): Promise<any> => {
  console.log('[DEBUG] getProfile called with id =', id, '(type:', typeof id, ')')
  return request.get(`/profile/${id}`)
}

export const saveProfile = (data: Record<string, any>): Promise<any> => {
  return request.post('/profile', data)
}
