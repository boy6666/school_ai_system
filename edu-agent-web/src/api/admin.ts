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