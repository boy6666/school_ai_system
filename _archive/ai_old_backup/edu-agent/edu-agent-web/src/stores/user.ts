import { defineStore } from 'pinia'
import { ref } from 'vue'

export const useUserStore = defineStore('user', () => {
  const token = ref(localStorage.getItem('token') || '')
  const userInfo = ref<any>(null)

  const setToken = (newToken: string) => {
    token.value = newToken
    localStorage.setItem('token', newToken)
  }

  const setUserInfo = (info: any) => {
    userInfo.value = info
    if (info?.role) localStorage.setItem('role', info.role)
  }

  const logout = () => {
    token.value = ''
    userInfo.value = null
    localStorage.removeItem('token'); localStorage.removeItem('role'); localStorage.removeItem('tutor_current_session'); localStorage.removeItem('tutor_current_messages')
  }

  return { token, userInfo, setToken, setUserInfo, logout }
})
