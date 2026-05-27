import request from '@/utils/request'

export const getCurrentPath = () => request.get('/student/learning-path/current')
