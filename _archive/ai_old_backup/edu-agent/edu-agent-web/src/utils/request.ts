import axios from 'axios'

type ApiResult<T> = {
  code: number
  message: string
  data: T
}

const request = axios.create({
  baseURL: import.meta.env.VITE_API_BASE_URL || '/api',
  timeout: 120000
})

request.interceptors.request.use(config => {
  const token = localStorage.getItem('token')

  if (token) {
    config.headers.set('Authorization', `Bearer ${token}`)
  }

  return config
})

request.interceptors.response.use(
  response => {
    const responseData = response.data

    if (
      responseData &&
      typeof responseData === 'object' &&
      'code' in responseData &&
      'data' in responseData
    ) {
      const apiResult = responseData as ApiResult<unknown>

      if (apiResult.code !== 200) {
        return Promise.reject(new Error(apiResult.message || '接口请求失败'))
      }

      return apiResult.data
    }

    return responseData
  },
  error => {
    console.error('接口请求错误：', error)
    return Promise.reject(error)
  }
)

export default request