import axios from 'axios'
import request from '@/utils/request'

const aiClient = axios.create({
  baseURL: '/ai',
  timeout: 60000,
})

export interface DimensionState {
  level: string
  level_label: string
  level_number: number
  score: number
  evidence: string[]
}

export interface ProfileData {
  // 六维层次画像
  knowledge_mastery?: DimensionState
  knowledge_mastery_label?: string
  learning_goal_clarity?: DimensionState
  learning_goal_clarity_label?: string
  cognitive_adaptation?: DimensionState
  cognitive_adaptation_label?: string
  mistake_avoidance?: DimensionState
  mistake_avoidance_label?: string
  learning_autonomy?: DimensionState
  learning_autonomy_label?: string
  overall_level?: DimensionState
  overall_level_label?: string

  // 辅助信息
  major?: string
  grade?: string
  course?: string
  topic?: string
  learning_goal?: string
  knowledge_base?: string
  current_mastery?: string
  cognitive_style?: string
  weaknesses?: string[]
  mistake_patterns?: string[]
  learning_behavior?: string
  resource_preference?: string[]
  pace?: string
  overall_type?: string
  profile_suggestions?: string[]
  conversation_count?: number
  last_updated?: string
  created_at?: string
  exists?: boolean
}

export interface ProfileChange {
  dimension: string
  from_level: string
  to_level: string
  reason: string
}

export interface ProfileChanges {
  changed_dimensions: ProfileChange[]
  has_changes: boolean
}

export const getProfileFromAI = async (studentId: string): Promise<{
  student_id: string
  profile: ProfileData
  exists: boolean
}> => {
  const res = await aiClient.get(`/profile/${studentId}`)
  return res.data
}

export const getProfile = (username: string): Promise<ProfileData> => {
  return request.get(`/profile/${username}`)
}

export const saveProfile = (username: string, profile: Record<string, any>): Promise<any> => {
  return request.post('/profile/save', { username, profile })
}

/**
 * 将AI引擎返回的画像数据同步到Java后端MySQL。
 * 异步执行，不抛出异常，失败时静默忽略。
 */
export const syncProfileToBackend = async (username: string, profile: Record<string, any>) => {
  try {
    await saveProfile(username, profile)
  } catch {
    // 静默忽略同步失败，AI引擎JSON文件仍为数据源
  }
}
