import request from '@/utils/request'

export function sendMessage(data) {
  return request.post('/tutor/chat', data)
}
