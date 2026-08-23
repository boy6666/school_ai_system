import { defineStore } from 'pinia'
import { ref } from 'vue'

export interface UserInfo {
  id?: number
  userId?: number
  username?: string
  realName?: string
  name?: string
  roles: string[]
  role?: string
  onboarded?: number
  avatar?: string
  email?: string
  phone?: string
}

function readUserInfo(): UserInfo | null {
  const saved = localStorage.getItem('userInfo')
  if (!saved) return null

  try {
    const parsed = JSON.parse(saved) as Partial<UserInfo>
    return {
      ...parsed,
      roles: Array.isArray(parsed.roles) ? parsed.roles : [],
      role: parsed.role || parsed.roles?.[0]
    }
  } catch {
    localStorage.removeItem('userInfo')
    return null
  }
}

export const useUserStore = defineStore('user', () => {
  const token = ref(localStorage.getItem('token') || '')
  const userInfo = ref<UserInfo | null>(readUserInfo())

  function setToken(newToken: string): void {
    token.value = newToken
    localStorage.setItem('token', newToken)
  }

  function setUserInfo(info: Partial<UserInfo>): void {
    const roles = Array.isArray(info.roles) ? info.roles : []
    const normalized: UserInfo = {
      ...info,
      roles,
      role: info.role || roles[0]
    }

    userInfo.value = normalized
    localStorage.setItem('roles', JSON.stringify(normalized.roles))
    localStorage.setItem('userInfo', JSON.stringify(normalized))
  }

  function setOnboarded(onboarded: number): void {
    if (!userInfo.value) return
    setUserInfo({ ...userInfo.value, onboarded })
  }

  function logout(): void {
    token.value = ''
    userInfo.value = null

    localStorage.removeItem('token')
    localStorage.removeItem('roles')
    localStorage.removeItem('userInfo')
    localStorage.removeItem('tutor_current_session')
    localStorage.removeItem('tutor_current_messages')
  }

  return {
    token,
    userInfo,
    setToken,
    setUserInfo,
    setOnboarded,
    logout
  }
})