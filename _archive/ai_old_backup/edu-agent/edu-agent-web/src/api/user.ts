import request from '@/utils/request'

export const getUserInfo = () => {
  return request.get('/user/info')
}

export const updateUserInfo = (params: any) => {
  return request.put('/user/info', params)
}

export const updateAvatar = (file: File) => {
  const formData = new FormData()
  formData.append('file', file)
  return request.post('/user/avatar', formData, {
    headers: {
      'Content-Type': 'multipart/form-data'
    }
  })
}

export const changePassword = (params: any) => {
  return request.post('/user/change-password', params)
}
