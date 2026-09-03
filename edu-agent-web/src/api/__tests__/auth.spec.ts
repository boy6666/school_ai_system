import AxiosMockAdapter from 'axios-mock-adapter'
import {
  beforeEach,
  describe,
  expect,
  it
} from 'vitest'
import request from '@/utils/request'
import {
  getMe,
  login,
  refresh,
  register
} from '@/api/auth'

describe('认证服务接口契约', () => {
  const mock = new AxiosMockAdapter(request)

  beforeEach(() => {
    mock.reset()
  })

  it('应按正式路径提交登录请求', async () => {
    const path = '/edu-agent-auth/login'
    const payload = {
      username: 'admin',
      password: 'test-password'
    }

    mock.onPost(path).reply(200, {
      code: 0,
      message: 'success',
      data: {
        token: 'test-token',
        userId: 1,
        roles: ['ROLE_ADMIN'],
        realName: '管理员'
      }
    })

    const result = await login(payload)

    expect(mock.history.post).toHaveLength(1)
    expect(mock.history.post[0]?.url).toBe(path)
    expect(
      JSON.parse(mock.history.post[0]?.data)
    ).toEqual(payload)
    expect(result.roles).toContain('ROLE_ADMIN')
  })

  it('应携带正式路径查询当前用户信息', async () => {
    const path = '/edu-agent-auth/me'

    mock.onGet(path).reply(200, {
      code: 0,
      message: 'success',
      data: {
        userId: 1,
        username: 'admin',
        realName: '管理员',
        roles: ['ROLE_ADMIN'],
        status: 1,
        email: 'admin@example.com',
        phone: '13800000000'
      }
    })

    const result = await getMe()

    expect(mock.history.get).toHaveLength(1)
    expect(mock.history.get[0]?.url).toBe(path)
    expect(result.roles).toEqual(['ROLE_ADMIN'])
    expect(result.status).toBe(1)
  })

  it('应按正式契约提交注册请求', async () => {
    const path = '/edu-agent-auth/register'
    const payload = {
      username: 'teacher01',
      password: 'teacher123',
      realName: '测试教师',
      email: 'teacher01@example.com',
      phone: '13800000000',
      role: 'teacher'
    }

    mock.onPost(path).reply(200, {
      code: 0,
      message: 'success',
      data: {
        token: 'register-token',
        userId: 3,
        roles: ['ROLE_TEACHER'],
        realName: '测试教师'
      }
    })

    const result = await register(payload)

    expect(mock.history.post).toHaveLength(1)
    expect(mock.history.post[0]?.url).toBe(path)
    expect(
      JSON.parse(mock.history.post[0]?.data)
    ).toEqual(payload)
    expect(result.roles).toEqual(['ROLE_TEACHER'])
  })

  it('应携带Token刷新认证信息', async () => {
    const path = '/edu-agent-auth/refresh'
    const payload = {
      token: 'expired-token'
    }

    mock.onPost(path).reply(200, {
      code: 0,
      message: 'success',
      data: {
        token: 'refreshed-token',
        userId: 1,
        roles: ['ROLE_ADMIN'],
        realName: '管理员'
      }
    })

    const result = await refresh(payload)

    expect(mock.history.post).toHaveLength(1)
    expect(mock.history.post[0]?.url).toBe(path)
    expect(
      JSON.parse(mock.history.post[0]?.data)
    ).toEqual(payload)
    expect(result.token).toBe('refreshed-token')
    expect(result.roles).toEqual(['ROLE_ADMIN'])
  })
})
