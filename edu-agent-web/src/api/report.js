import request from '@/utils/request'

export function generateReport(data) {
  return request.post('/reports/generate', data)
}

export function getReportList(params) {
  return request.get('/reports', { params })
}

export function getReportDetail(id) {
  return request.get(`/reports/${id}`)
}

export function deleteReport(id) {
  return request.delete(`/reports/${id}`)
}
