import request from '@/utils/request'

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
