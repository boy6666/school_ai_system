import { defineStore } from 'pinia'
import { ref } from 'vue'

export const useUserStore = defineStore('user', () => {
  const token = ref(localStorage.getItem('token') || '')

  const savedUserInfo = localStorage.getItem('userInfo')
  const userInfo = ref<any>(savedUserInfo ? JSON.parse(savedUserInfo) : null)
  console.log('[DEBUG] userStore init, userInfo from localStorage:', savedUserInfo)

  const setToken = (newToken: string) => {
    token.value = newToken
    localStorage.setItem('token', newToken)
  }

  const setUserInfo = (info: any) => {
    console.log('[DEBUG] setUserInfo called with:', JSON.stringify(info))
    userInfo.value = info
    if (info?.role) localStorage.setItem('role', info.role)
    localStorage.setItem('userInfo', JSON.stringify(info))
  }

  const logout = () => {
    token.value = ''
    userInfo.value = null
    localStorage.removeItem('token'); localStorage.removeItem('role'); localStorage.removeItem('tutor_current_session'); localStorage.removeItem('tutor_current_messages')
  }

  return { token, userInfo, setToken, setUserInfo, logout }
})
