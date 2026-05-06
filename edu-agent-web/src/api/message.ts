import request from '@/utils/request'

export const getMessageList = (params: any) => {
  return request.get('/message/list', { params })
}

export const getMessageDetail = (id: number) => {
  return request.get(`/message/${id}`)
}

export const markAsRead = (id: number) => {
  return request.post(`/message/${id}/read`)
}

export const markAllRead = () => {
  return request.post('/message/read-all')
}

export const deleteMessage = (id: number) => {
  return request.delete(`/message/${id}`)
}

export const sendMessage = (params: any) => {
  return request.post('/message/send', params)
}

export const getConversationList = () => {
  return request.get('/message/conversations')
}

export const getConversationMessages = (id: number) => {
  return request.get(`/message/conversation/${id}`)
}
