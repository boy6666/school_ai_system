import request from '@/utils/request'

export const getPracticeList = (params: any) => {
  return request.get('/practice/list', { params })
}

export const getPracticeDetail = (id: number) => {
  return request.get(`/practice/${id}`)
}

export const submitAnswer = (params: any) => {
  return request.post('/practice/submit', params)
}

export const getPracticeProgress = () => {
  return request.get('/practice/progress')
}
