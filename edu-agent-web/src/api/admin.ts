import request from '@/utils/request'

// ===== 用户管理 =====
export interface UserInfo {
  id: number
  username: string
  nickname: string
  email: string
  phone: string
  role: string
  status: string
  lastLoginTime: string
  createTime: string
}

export interface PageResult<T> {
  records: T[]
  total: number
  page: number
  pageSize: number
}

export function getUserList(params: { page: number; pageSize: number; keyword?: string }) {
  return request.get<unknown, PageResult<UserInfo>>('/admin/users', { params })
}

export function updateUserRole(userId: number, role: string) {
  return request.put<unknown, void>(`/admin/users/${userId}/role`, { role })
}

export function deleteUser(userId: number) {
  return request.delete(`/admin/users/${userId}`)
}

// ===== 统计 =====
export interface AdminStats {
  totalUsers?: number
  activeUsers?: number
  totalConversations?: number
  todayConversations?: number
}

export function getAdminStats() {
  return request.get<unknown, AdminStats>('/admin/stats')
}

// ===== 智能体管理 =====
export interface AgentConfig {
  id: number
  name: string
  type: string
  model: string
  configJson: string
  status: string
  createTime: string
}

export function getAgentList(params: { page: number; pageSize: number; keyword?: string }) {
  return request.get<unknown, PageResult<AgentConfig>>('/admin/agents', { params })
}

export function createAgent(data: Partial<AgentConfig>) {
  return request.post<unknown, AgentConfig>('/admin/agents', data)
}

export function updateAgent(id: number, data: Partial<AgentConfig>) {
  return request.put<unknown, void>(`/admin/agents/${id}`, data)
}

export function deleteAgent(id: number) {
  return request.delete(`/admin/agents/${id}`)
}

// ===== 内容审核 =====
export interface ReviewConversation {
  id: number
  studentId: number
  sessionId: string
  question: string
  answer: string
  intent: string
  evaluationReport: string
  resourceDir: string
  createTime: string
}

export interface ReviewStats {
  total?: number
  today?: number
  byExplain?: number
  byQuiz?: number
}

export function getConversationList(params: { page: number; pageSize: number; keyword?: string; intent?: string }) {
  return request.get<unknown, PageResult<ReviewConversation>>('/admin/conversations', { params })
}

export function flagConversation(id: number, flag: string) {
  return request.put<unknown, void>(`/admin/conversations/${id}/flag`, { flag })
}

export function getReviewStats() {
  return request.get<unknown, ReviewStats>('/admin/conversations/stats')
}

// ===== 资源管理 =====
export interface ResourceItem {
  id: number
  title: string
  type: string
  difficulty: string
  description: string
  content: string
  fileUrl: string
  cover: string
  author: string
  rating: number
  views: number
  status: string
  courseId: string
  courseName: string
  tags: string
  createTime: string
}

export function getResourceList(params: { page: number; pageSize: number; keyword?: string; type?: string; status?: string }) {
  return request.get<unknown, PageResult<ResourceItem>>('/admin/resources', { params })
}

export function createResource(data: Partial<ResourceItem>) {
  return request.post<unknown, ResourceItem>('/admin/resources', data)
}

export function updateResource(id: number, data: Partial<ResourceItem>) {
  return request.put<unknown, void>(`/admin/resources/${id}`, data)
}

export function deleteResource(id: number) {
  return request.delete(`/admin/resources/${id}`)
}

// ===== 系统设置 =====
export interface SystemSetting {
  id: number
  settingKey: string
  settingValue: string
  description: string
}

export function getSettings() {
  return request.get<unknown, SystemSetting[]>('/admin/settings')
}

export function updateSetting(key: string, value: string) {
  return request.put<unknown, void>('/admin/settings', { key, value })
}
