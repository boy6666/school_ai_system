import request from '@/utils/request'

export interface ProfileScore {
  score?: number
}

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
  profile_data?: Record<string, ProfileScore>
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

export interface ProfileBuildResult {
  student_id: string
  profile: ProfileData
}

export interface AiProfileResult extends ProfileBuildResult {
  exists: boolean
}

export const buildProfile = (params: ProfileBuildParams) => {
  return request.post<unknown, ProfileBuildResult>('/edu-agent-ai/profile/build', params)
}

export const getProfileFromAI = (studentId: string) => {
  return request.get<unknown, AiProfileResult>(`/edu-agent-ai/profile/${studentId}`)
}

export const getProfile = (id: number) => {
  return request.get<unknown, ProfileData>(`/edu-agent-learning/profile/${id}`)
}

export const saveProfile = (data: Record<string, unknown>) => {
  return request.post<unknown, ProfileData>('/edu-agent-learning/profile', data)
}