import request from '@/utils/request'

export interface LoginParams {
  username: string
  password: string
}

export interface LoginResponse {
  token: string
  userInfo: any
}

export const login = (params: LoginParams) => {
  return request.post<LoginResponse>('/auth/login', params)
}

export const logout = () => {
  return request.post('/auth/logout')
}

export const register = (params: any) => {
  return request.post('/auth/register', params)
}

export const refresh = () => {
  return request.post('/auth/refresh')
}
