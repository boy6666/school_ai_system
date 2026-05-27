import request from '@/utils/request'

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

export interface AdminAgentListQuery {
  keyword?: string
  status?: string
  type?: string
}

export interface AdminAgentListResponse {
  list: AgentItem[]
  total?: number
}

export type ManageStatus = 'published' | 'draft' | 'reviewing' | 'offline'

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

export interface AdminManageListQuery {
  keyword?: string
  status?: string
  type?: string
  page?: number
  pageSize?: number
}

export interface AdminCourseListResponse {
  list: AdminCourseItem[]
  total?: number
}

export interface AdminResourceListResponse {
  list: AdminResourceItem[]
  total?: number
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
  content: string
  reviewer?: string
  reviewTime?: string
  reason?: string
}

export interface AdminReviewListQuery {
  keyword?: string
  status?: string
  type?: string
  riskLevel?: string
}

export interface AdminReviewListResponse {
  list: ReviewItem[]
  total?: number
}

// 系统设置相关
export const getSystemSettings = () => {
  return request.get('/admin/settings')
}

export const updateSystemSettings = (params: any) => {
  return request.put('/admin/settings', params)
}

// 用户管理相关
export const getUserList = (params: any) => {
  return request.get('/admin/users', { params })
}

export const createUser = (params: any) => {
  return request.post('/admin/users', params)
}

export const updateUser = (id: number, params: any) => {
  return request.put(`/admin/users/${id}`, params)
}

export const deleteUser = (id: number) => {
  return request.delete(`/admin/users/${id}`)
}

export const toggleUserStatus = (id: number) => {
  return request.post(`/admin/users/${id}/toggle`)
}

// 角色权限相关
export const getRoleList = () => {
  return request.get('/admin/roles')
}

export const createRole = (params: any) => {
  return request.post('/admin/roles', params)
}

export const updateRole = (id: number, params: any) => {
  return request.put(`/admin/roles/${id}`, params)
}

export const deleteRole = (id: number) => {
  return request.delete(`/admin/roles/${id}`)
}

export const getAdminAgentList = (params: AdminAgentListQuery) => {
  return request.get<unknown, AdminAgentListResponse>('/admin/agents', {
    params
  })
}

export const saveAgentConfig = (params: AgentItem) => {
  return request.put<unknown, { success: boolean }>(
    `/admin/agents/${params.id}`,
    params
  )
}

export const updateAgentStatus = (id: number, status: AgentStatus) => {
  return request.post<unknown, { success: boolean }>(
    `/admin/agents/${id}/status`,
    { status }
  )
}

export const getAdminCourseList = (params: AdminManageListQuery) => {
  return request.get<unknown, AdminCourseListResponse>('/admin/courses', {
    params
  })
}

export const getAdminResourceList = (params: AdminManageListQuery) => {
  return request.get<unknown, AdminResourceListResponse>('/admin/resources', {
    params
  })
}

export const updateCourseStatus = (id: number, status: ManageStatus) => {
  return request.post<unknown, { success: boolean }>(
    `/admin/courses/${id}/status`,
    { status }
  )
}

export const updateResourceStatus = (id: number, status: ManageStatus) => {
  return request.post<unknown, { success: boolean }>(
    `/admin/resources/${id}/status`,
    { status }
  )
}

export const getAdminReviewList = (params: AdminReviewListQuery) => {
  return request.get<unknown, AdminReviewListResponse>('/admin/reviews', {
    params
  })
}

export const approveReview = (id: number) => {
  return request.post<unknown, { success: boolean }>(
    `/admin/reviews/${id}/approve`
  )
}

export const rejectReview = (id: number, reason: string) => {
  return request.post<unknown, { success: boolean }>(
    `/admin/reviews/${id}/reject`,
    { reason }
  )
}

// 数据统计相关
export const getStatistics = () => {
  return request.get('/admin/statistics')
}

export const getUserGrowth = (params: any) => {
  return request.get('/admin/statistics/user-growth', { params })
}

export const getLearningData = (params: any) => {
  return request.get('/admin/statistics/learning', { params })
}

export const exportData = (params: any) => {
  return request.post('/admin/statistics/export', params, {
    responseType: 'blob'
  })
}

// 操作日志相关
export const getOperationLogs = (params: any) => {
  return request.get('/admin/logs', { params })
}

// 数据备份相关
export const createBackup = () => {
  return request.post('/admin/backup')
}

export const getBackupList = () => {
  return request.get('/admin/backups')
}

export const restoreBackup = (id: number) => {
  return request.post(`/admin/backup/${id}/restore`)
}

export const downloadBackup = (id: number) => {
  return request.get(`/admin/backup/${id}/download`, {
    responseType: 'blob'
  })
}

export const deleteBackup = (id: number) => {
  return request.delete(`/admin/backup/${id}`)
}
