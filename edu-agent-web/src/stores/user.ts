import { defineStore } from 'pinia'
import { computed, ref } from 'vue'

export interface UserInfo {
  id?: number
  userId?: number
  username?: string
  nickname?: string
  realName?: string
  name?: string
  roles: string[]
  role?: string
  status?: string
  onboarded?: number
  avatar?: string
  email?: string
  phone?: string
  createTime?: string
  lastLoginTime?: string | null
}

function readUserInfo(): UserInfo | null {
  const saved = localStorage.getItem('userInfo')
  if (!saved) return null

  try {
    const parsed = JSON.parse(
      saved
    ) as Partial<UserInfo>

    const normalizedRoles = Array.isArray(
      parsed.roles
    )
      ? parsed.roles
      : []

    return {
      ...parsed,
      roles: normalizedRoles,
      role: parsed.role || normalizedRoles[0],
      name:
        parsed.name ||
        parsed.nickname ||
        parsed.realName ||
        parsed.username
    }
  } catch {
    localStorage.removeItem('userInfo')
    return null
  }
}

export const useUserStore = defineStore(
  'user',
  () => {
    const token = ref(
      localStorage.getItem('token') || ''
    )

    const userInfo = ref<UserInfo | null>(
      readUserInfo()
    )

    const roles = computed(
      () => userInfo.value?.roles ?? []
    )

    function setToken(
      newToken: string
    ): void {
      token.value = newToken
      localStorage.setItem('token', newToken)
    }

    function setUserInfo(
      info: Partial<UserInfo>
    ): void {
      const normalizedRoles = Array.isArray(
        info.roles
      )
        ? info.roles
        : []

      const normalized: UserInfo = {
        ...info,
        roles: normalizedRoles,
        role: info.role || normalizedRoles[0],
        name:
          info.name ||
          info.nickname ||
          info.realName ||
          info.username
      }

      userInfo.value = normalized

      localStorage.removeItem('role')
      localStorage.setItem(
        'roles',
        JSON.stringify(normalized.roles)
      )
      localStorage.setItem(
        'userInfo',
        JSON.stringify(normalized)
      )
    }

    function setOnboarded(
      onboarded: number
    ): void {
      if (!userInfo.value) return

      setUserInfo({
        ...userInfo.value,
        onboarded
      })
    }

    function logout(): void {
      token.value = ''
      userInfo.value = null

      localStorage.removeItem('token')
      localStorage.removeItem('roles')
      localStorage.removeItem('role')
      localStorage.removeItem('userInfo')
    }

    return {
      token,
      roles,
      userInfo,
      setToken,
      setUserInfo,
      setOnboarded,
      logout
    }
  }
)