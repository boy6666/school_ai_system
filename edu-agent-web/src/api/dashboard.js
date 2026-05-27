import request from '@/utils/request'

export const getDashboardStats = () => request.get('/student/dashboard/stats')
