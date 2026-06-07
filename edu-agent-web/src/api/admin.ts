import request from '@/utils/request'

// ===== 通用 =====
export interface PageResult<T> { records: T[]; total: number; page: number; pageSize: number }

// ===== 统计 (Dashboard + Statistics) =====
export interface AdminStats { totalUsers?: number; activeUsers?: number; totalConversations?: number; todayConversations?: number }
export function getAdminStats() { return request.get<unknown, AdminStats>('/admin/stats') }

// ===== 用户管理 (UserManage + Statistics) =====
export interface UserInfo { id: number; username: string; nickname: string; email: string; phone: string; role: string; status: string; lastLoginTime: string; createTime: string }
export function getUserList(params: { page: number; pageSize: number; keyword?: string }) { return request.get<unknown, PageResult<UserInfo>>('/admin/users', { params }) }
export function updateUserRole(userId: number, role: string) { return request.put(`/admin/users/${userId}/role`, { role }) }
export function deleteUser(userId: number) { return request.delete(`/admin/users/${userId}`) }

// ===== 智能体管理 (AgentManage) =====
export type AgentStatus = 'active' | 'inactive'
export interface AgentItem { id: number; name: string; type: string; model: string; configJson: string; status: AgentStatus; createTime: string }
export function getAdminAgentList(params: { page: number; pageSize: number; keyword?: string; status?: string; type?: string }) { return request.get<unknown, PageResult<AgentItem>>('/admin/agents', { params }) }
export function saveAgentConfig(data: Partial<AgentItem>) { return request.post<unknown, AgentItem>('/admin/agents', data) }
export function updateAgentStatus(id: number, status: AgentStatus) { return request.put(`/admin/agents/${id}/status`, { status }) }

// ===== 内容审核 (ContentReview) =====
export type ReviewStatus = 'pending' | 'approved' | 'rejected'
export type RiskLevel = 'low' | 'medium' | 'high'
export interface ReviewItem { id: number; studentId: number; question: string; answer: string; intent: string; status: ReviewStatus; riskLevel: RiskLevel; createTime: string }
export function getAdminReviewList(params: { page: number; pageSize: number; keyword?: string }) { return request.get<unknown, PageResult<ReviewItem>>('/admin/conversations', { params }) }
export function approveReview(id: number) { return request.put(`/admin/conversations/${id}/approve`) }
export function rejectReview(id: number, reason?: string) { return request.put(`/admin/conversations/${id}/reject`, { reason }) }

// ===== 资源管理 (ResourceManage) =====
export type ManageStatus = 'published' | 'draft' | 'archived' | 'offline' | 'reviewing'
export interface AdminCourseItem { id: number; title: string; type: string; status: ManageStatus; createTime: string }
export interface AdminResourceItem { id: number; title: string; type: string; status: ManageStatus; createTime: string }
export function getAdminCourseList(params: { page: number; pageSize: number; keyword?: string }) { return request.get<unknown, PageResult<AdminCourseItem>>('/admin/resources', { params: { ...params, type: 'course' } }) }
export function getAdminResourceList(params: { page: number; pageSize: number; keyword?: string }) { return request.get<unknown, PageResult<AdminResourceItem>>('/admin/resources', { params }) }
export function updateCourseStatus(id: number, status: ManageStatus) { return request.put(`/admin/resources/${id}/status`, { status }) }
export function updateResourceStatus(id: number, status: ManageStatus) { return request.put(`/admin/resources/${id}/status`, { status }) }

// ===== 系统设置 (Settings) =====
export interface SystemSetting { id: number; settingKey: string; settingValue: string; description: string }
export function getSettings() { return request.get<unknown, SystemSetting[]>('/admin/settings') }
export function updateSetting(key: string, value: string) { return request.put('/admin/settings', { key, value }) }

// ===== 资源生成 =====
export function generateResources(studentId: string) {
  return request.post<unknown, any>('/admin/resources/generate', { studentId })
}
