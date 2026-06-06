import request from '@/utils/request'

export const getProjectList = (params: any) => {
  return request.get('/project/list', { params })
}

export const getProjectDetail = (id: number) => {
  return request.get(`/project/${id}`)
}

export const joinProject = (id: number) => {
  return request.post(`/project/${id}/join`)
}

export const submitProject = (params: any) => {
  return request.post('/project/submit', params)
}

export const getProjectProgress = (id: number) => {
  return request.get(`/project/${id}/progress`)
}
