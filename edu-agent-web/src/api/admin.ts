import request from '@/utils/request'

export type ManageStatus = 'published' | 'draft' | 'offline' | 'reviewing'

export interface AdminCourseItem {
  id: number
  name: string
  code: string
  teacher: string
  department: string
  semester: string
  studentCount: number
  resourceCount: number
  status: ManageStatus
  updateTime: string
}

export interface AdminResourceItem {
  id: number
  title: string
  type: string
  courseName: string
  difficulty: string
  uploader: string
  status: ManageStatus
  updateTime: string
}

export interface AdminListQuery {
  keyword?: string
  status?: string
  type?: string
  page?: number
  pageSize?: number
}

export function getAdminCourseList(params: AdminListQuery) {
  return request.get<unknown, { list: AdminCourseItem[]; total: number }>(
    '/admin/courses',
    { params }
  )
}

export function getAdminResourceList(params: AdminListQuery) {
  return request.get<unknown, { list: AdminResourceItem[]; total: number }>(
    '/admin/resources',
    { params }
  )
}

export function updateCourseStatus(id: number, status: ManageStatus) {
  return request.post<unknown, { success: boolean }>(
    `/admin/courses/${id}/status`,
    { status }
  )
}

export function updateResourceStatus(id: number, status: ManageStatus) {
  return request.post<unknown, { success: boolean }>(
    `/admin/resources/${id}/status`,
    { status }
  )
}

export type AgentStatus = 'running' | 'stopped'

export interface AgentItem {
  id: number
  name: string
  type: string
  description: string
  model: string
  status: AgentStatus
  callCount: number
  activeUsers: number
  satisfaction: number
  solveRate: number
  tools: string[]
  promptVersion: string
  updateTime: string
}

export interface AdminAgentQuery {
  keyword?: string
  status?: string
  type?: string
}

export function getAdminAgentList(params: AdminAgentQuery) {
  return request.get<unknown, { list: AgentItem[]; total: number }>(
    '/admin/agents',
    { params }
  )
}

export function updateAgentStatus(id: number, status: AgentStatus) {
  return request.post<unknown, { success: boolean }>(
    `/admin/agents/${id}/status`,
    { status }
  )
}

export function saveAgentConfig(agent: AgentItem) {
  return request.post<unknown, { success: boolean }>(
    `/admin/agents/${agent.id}`,
    agent
  )
}

export type ReviewStatus = 'pending' | 'approved' | 'rejected'

export type RiskLevel = 'low' | 'middle' | 'high'

export interface ReviewItem {
  id: number
  title: string
  type: string
  source: string
  riskLevel: RiskLevel
  status: ReviewStatus
  submitter: string
  submitTime: string
  reviewer?: string
  reviewTime?: string
  reason?: string
  content: string
}

export interface AdminReviewQuery {
  keyword?: string
  status?: string
  type?: string
  riskLevel?: string
}

export function getAdminReviewList(params: AdminReviewQuery) {
  return request.get<unknown, { list: ReviewItem[]; total: number }>(
    '/admin/reviews',
    { params }
  )
}

export function approveReview(id: number) {
  return request.post<unknown, { success: boolean }>(
    `/admin/reviews/${id}/approve`
  )
}

export function rejectReview(id: number, reason: string) {
  return request.post<unknown, { success: boolean }>(
    `/admin/reviews/${id}/reject`,
    { reason }
  )
}