import request from '@/utils/request'
import type { UserInfo } from '@/stores/user'

export type AuthRole =
  | 'student'
  | 'teacher'
  | 'admin'

export interface LoginParams {
  username: string
  password: string
}

/** Auth 服务正式返回的用户信息 */
export interface AuthUserInfo {
  id: number
  username: string
  nickname?: string
  email?: string
  phone?: string | null
  avatar?: string | null
  role: AuthRole
  onboarded: number
  status: string
  createTime?: string
  lastLoginTime?: string | null
}

/** 登录、注册和刷新接口统一返回此结构 */
export interface LoginResult {
  token: string
  userInfo: AuthUserInfo
}

export interface RegisterParams {
  username: string
  password: string
  nickname?: string
  email?: string
  role?: AuthRole
}

export interface RefreshParams {
  token: string
}

/** 将后端单角色转换成前端路由使用的 ROLE_* 形式 */
export function normalizeAuthRole(
  role: AuthRole
): string {
  return `ROLE_${role.toUpperCase()}`
}

/** 将 Auth 响应转换为现有前端统一用户结构 */
export function normalizeAuthUser(
  userInfo: AuthUserInfo
): UserInfo {
  const normalizedRole = normalizeAuthRole(
    userInfo.role
  )

  return {
    id: userInfo.id,
    userId: userInfo.id,
    username: userInfo.username,
    nickname: userInfo.nickname,
    realName: userInfo.nickname,
    name: userInfo.nickname || userInfo.username,
    roles: [normalizedRole],
    role: normalizedRole,
    status: userInfo.status,
    onboarded: userInfo.onboarded,
    avatar: userInfo.avatar || undefined,
    email: userInfo.email,
    phone: userInfo.phone || undefined,
    createTime: userInfo.createTime,
    lastLoginTime: userInfo.lastLoginTime
  }
}

/**
 * request.ts 已配置 baseURL=/api，
 * 最终地址为 /api/edu-agent-auth/**。
 */
export const login = (
  params: LoginParams
): Promise<LoginResult> => {
  return request.post<unknown, LoginResult>(
    '/edu-agent-auth/login',
    params
  )
}

export const register = (
  params: RegisterParams
): Promise<LoginResult> => {
  return request.post<unknown, LoginResult>(
    '/edu-agent-auth/register',
    params
  )
}

export const refresh = (
  params: RefreshParams
): Promise<LoginResult> => {
  return request.post<unknown, LoginResult>(
    '/edu-agent-auth/refresh',
    params
  )
}

export const logout = (): Promise<void> => {
  return request.post<unknown, void>(
    '/edu-agent-auth/logout'
  )
}

export const markOnboardDone = (): Promise<void> => {
  return request.post<unknown, void>(
    '/edu-agent-auth/onboard-done'
  )
}