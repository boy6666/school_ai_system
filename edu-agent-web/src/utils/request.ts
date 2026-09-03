import axios from 'axios'
import type {
  AxiosInstance,
  InternalAxiosRequestConfig
} from 'axios'
import { ElMessage } from 'element-plus'

export interface ApiResult<T = unknown> {
  code: number
  message: string
  data: T
}

export interface PageResult<T = unknown> {
  records: T[]
  total: number
  page: number
  pageSize: number
}

const request: AxiosInstance = axios.create({
  baseURL: import.meta.env.VITE_API_BASE_URL || '/api',
  timeout: 120000
})

request.interceptors.request.use(
  (config: InternalAxiosRequestConfig) => {
    const token = localStorage.getItem('token')

    if (token) {
      config.headers.set('Authorization', `Bearer ${token}`)
    }

    return config
  }
)

function clearLoginState() {
  localStorage.removeItem('token')
  localStorage.removeItem('roles')
  localStorage.removeItem('role')
  localStorage.removeItem('userInfo')
}

function goToLogin() {
  const isAdminPath = window.location.pathname.startsWith('/admin')
  const loginPath = isAdminPath ? '/admin/login' : '/login'

  if (window.location.pathname !== loginPath) {
    window.location.href = loginPath
  }
}

function handleUnauthorized(message?: string) {
  clearLoginState()
  ElMessage.error(message || '登录状态已失效，请重新登录')
  goToLogin()
}

request.interceptors.response.use(
  response => {
    const body = response.data

    if (
      body &&
      typeof body === 'object' &&
      'code' in body &&
      'data' in body
    ) {
      const result = body as ApiResult<unknown>

      if (result.code === 401) {
        handleUnauthorized(result.message)
        return Promise.reject(
          new Error(result.message || '登录状态已失效')
        )
      }

      if (result.code !== 0 && result.code !== 200) {
        const message = result.message || '请求失败'
        ElMessage.error(message)
        return Promise.reject(new Error(message))
      }

      return result.data
    }

    // 文件流或后端未包装Result时，直接返回原始响应数据
    return body
  },
  error => {
    if (error?.response?.status === 401) {
      const message =
        error?.response?.data?.message ||
        '登录状态已失效，请重新登录'

      handleUnauthorized(message)
    } else {
      const message =
        error?.response?.data?.message ||
        error?.message ||
        '网络异常，请稍后重试'

      ElMessage.error(message)
    }

    return Promise.reject(error)
  }
)

export default request
