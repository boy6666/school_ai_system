import {
  afterAll,
  beforeEach,
  describe,
  expect,
  it,
  vi
} from 'vitest'
import AxiosMockAdapter from 'axios-mock-adapter'

vi.mock('element-plus', () => ({
  ElMessage: {
    error: vi.fn()
  }
}))

import request from '@/utils/request'

const mock = new AxiosMockAdapter(request)

describe('统一请求封装', () => {
  beforeEach(() => {
    mock.reset()
    localStorage.clear()
    window.history.replaceState({}, '', '/login')
  })

  afterAll(() => {
    mock.restore()
  })

  it('有 Token 时应自动添加 Authorization 请求头', async () => {
    localStorage.setItem('token', 'test-token')

    mock.onGet('/token-test').reply(config => {
      const headerValue =
        config.headers?.Authorization

      return [
        200,
        {
          code: 200,
          message: 'ok',
          data: {
            authorization:
              typeof headerValue === 'string'
                ? headerValue
                : String(headerValue || '')
          }
        }
      ]
    })

    const result = await request.get<
      unknown,
      { authorization?: string }
    >('/token-test')

    expect(result.authorization).toBe(
      'Bearer test-token'
    )
  })

  it('应解包成功响应中的 data', async () => {
    mock.onGet('/result-test').reply(200, {
      code: 200,
      message: 'ok',
      data: {
        id: 1,
        name: 'student'
      }
    })

    const result = await request.get<
      unknown,
      { id: number; name: string }
    >('/result-test')

    expect(result).toEqual({
      id: 1,
      name: 'student'
    })
  })
it('应兼容 code 为 0 的成功响应', async () => {
  mock.onGet('/code-zero-test').reply(200, {
    code: 0,
    message: 'success',
    data: {
      id: 1,
      name: 'teacher'
    }
  })

  const result = await request.get<
    unknown,
    { id: number; name: string }
  >('/code-zero-test')

  expect(result).toEqual({
    id: 1,
    name: 'teacher'
  })
})
  it('未包装 Result 的响应应直接返回原始数据', async () => {
    mock.onGet('/raw-test').reply(200, {
      success: true
    })

    const result = await request.get<
      unknown,
      { success: boolean }
    >('/raw-test')

    expect(result).toEqual({
      success: true
    })
  })

  it('业务状态码为 401 时应清除登录状态', async () => {
    setLoginState()

    mock.onGet('/business-401').reply(200, {
      code: 401,
      message: '登录已失效',
      data: null
    })

    await expect(
      request.get('/business-401')
    ).rejects.toThrow('登录已失效')

    expectLoginStateCleared()
  })

  it('HTTP 状态码为 401 时应清除登录状态', async () => {
    setLoginState()

    mock.onGet('/http-401').reply(401, {
      message: '登录已失效'
    })

    await expect(
      request.get('/http-401')
    ).rejects.toBeTruthy()

    expectLoginStateCleared()
  })
})

function setLoginState() {
  localStorage.setItem('token', 'test-token')
  localStorage.setItem(
    'roles',
    JSON.stringify(['STUDENT'])
  )
  localStorage.setItem('role', 'STUDENT')
  localStorage.setItem(
    'userInfo',
    JSON.stringify({ userId: 1 })
  )
}

function expectLoginStateCleared() {
  expect(localStorage.getItem('token')).toBeNull()
  expect(localStorage.getItem('roles')).toBeNull()
  expect(localStorage.getItem('role')).toBeNull()
  expect(localStorage.getItem('userInfo')).toBeNull()
}
