import request from '@/utils/request'

export const getLearningReport = () => {
  return request.get('/report/learning')
}

export const getStatistics = () => {
  return request.get('/report/statistics')
}

export const getComparison = () => {
  return request.get('/report/comparison')
}

export const exportReport = () => {
  return request.get('/report/export', {
    responseType: 'blob'
  })
}
