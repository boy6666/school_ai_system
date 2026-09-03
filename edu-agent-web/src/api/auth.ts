import request from '@/utils/request'
import type { UserInfo } from '@/stores/user'

export interface LoginParams {
  username: string
  password: string
}

export interface LoginResult {
  token: string
  userId: number
  roles: string[]
  realName: string
  onboarded?: number
}

export interface RegisterParams {
  username: string
  password: string
  realName?: string
  email?: string
  phone?: string
  role?: string
}

export interface RefreshParams {
  token: string
}

/**
 * request.ts 已配置 baseURL=/api，因此这里不能再写 /api，
 * 最终请求地址为 /api/edu-agent-auth/**。
 */
export const login = (params: LoginParams) => {
  return request.post<unknown, LoginResult>('/edu-agent-auth/login', params)
}

export const register = (params: RegisterParams) => {
  return request.post<unknown, LoginResult>(
    '/edu-agent-auth/register',
    params
  )
}

export const getMe = () => {
  return request.get<unknown, UserInfo>('/edu-agent-auth/me')
}

export const refresh = (params: RefreshParams) => {
  return request.post<unknown, LoginResult>(
    '/edu-agent-auth/refresh',
    params
  )
}
