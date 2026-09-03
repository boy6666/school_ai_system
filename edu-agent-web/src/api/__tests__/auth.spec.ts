import AxiosMockAdapter from 'axios-mock-adapter'
import {
  beforeEach,
  describe,
  expect,
  it
} from 'vitest'
import request from '@/utils/request'
import {
  login,
  logout,
  markOnboardDone,
  normalizeAuthUser,
  refresh,
  register
} from '@/api/auth'

describe('认证服务接口契约', () => {
  const mock = new AxiosMockAdapter(request)

  beforeEach(() => {
    mock.reset()
  })

  it('应按正式契约提交登录请求并读取用户信息', async () => {
    const path = '/edu-agent-auth/login'
    const payload = {
      username: 'student01',
      password: 'student123'
    }

    mock.onPost(path).reply(200, {
      code: 0,
      message: 'success',
      data: {
        token: 'login-token',
        userInfo: {
          id: 1,
          username: 'student01',
          nickname: '小明',
          email: 'xm@example.com',
          phone: null,
          avatar: null,
          role: 'student',
          onboarded: 0,
          status: 'active',
          createTime: '2026-09-03T21:00:00'
        }
      }
    })

    const result = await login(payload)

    expect(mock.history.post).toHaveLength(1)
    expect(mock.history.post[0]?.url).toBe(path)
    expect(
      JSON.parse(mock.history.post[0]?.data)
    ).toEqual(payload)
    expect(result.token).toBe('login-token')
    expect(result.userInfo.id).toBe(1)
    expect(result.userInfo.role).toBe('student')
    expect(result.userInfo.status).toBe('active')
  })

  it('应将正式用户信息转换为前端角色结构', () => {
    const result = normalizeAuthUser({
      id: 2,
      username: 'teacher01',
      nickname: '测试教师',
      email: 'teacher@example.com',
      phone: null,
      avatar: null,
      role: 'teacher',
      onboarded: 1,
      status: 'active'
    })

    expect(result.id).toBe(2)
    expect(result.userId).toBe(2)
    expect(result.nickname).toBe('测试教师')
    expect(result.realName).toBe('测试教师')
    expect(result.name).toBe('测试教师')
    expect(result.roles).toEqual(['ROLE_TEACHER'])
    expect(result.role).toBe('ROLE_TEACHER')
    expect(result.status).toBe('active')
  })

  it('应按正式契约提交注册请求并接收登录信息', async () => {
    const path = '/edu-agent-auth/register'
    const payload = {
      username: 'teacher01',
      password: 'teacher123',
      nickname: '测试教师',
      email: 'teacher01@example.com',
      role: 'teacher' as const
    }

    mock.onPost(path).reply(200, {
      code: 0,
      message: 'success',
      data: {
        token: 'register-token',
        userInfo: {
          id: 3,
          username: 'teacher01',
          nickname: '测试教师',
          email: 'teacher01@example.com',
          phone: null,
          avatar: null,
          role: 'teacher',
          onboarded: 0,
          status: 'active'
        }
      }
    })

    const result = await register(payload)

    expect(mock.history.post).toHaveLength(1)
    expect(mock.history.post[0]?.url).toBe(path)
    expect(
      JSON.parse(mock.history.post[0]?.data)
    ).toEqual(payload)
    expect(result.token).toBe('register-token')
    expect(result.userInfo.role).toBe('teacher')
  })

  it('应携带Token刷新认证信息', async () => {
    const path = '/edu-agent-auth/refresh'
    const payload = {
      token: 'old-token'
    }

    mock.onPost(path).reply(200, {
      code: 0,
      message: 'success',
      data: {
        token: 'refreshed-token',
        userInfo: {
          id: 1,
          username: 'student01',
          nickname: '小明',
          email: 'xm@example.com',
          phone: null,
          avatar: null,
          role: 'student',
          onboarded: 1,
          status: 'active'
        }
      }
    })

    const result = await refresh(payload)

    expect(mock.history.post).toHaveLength(1)
    expect(mock.history.post[0]?.url).toBe(path)
    expect(
      JSON.parse(mock.history.post[0]?.data)
    ).toEqual(payload)
    expect(result.token).toBe('refreshed-token')
    expect(result.userInfo.id).toBe(1)
  })

  it('应调用正式退出接口且不发送请求体', async () => {
    const path = '/edu-agent-auth/logout'

    mock.onPost(path).reply(200, {
      code: 0,
      message: 'success',
      data: null
    })

    await logout()

    expect(mock.history.post).toHaveLength(1)
    expect(mock.history.post[0]?.url).toBe(path)
    expect(mock.history.post[0]?.data).toBeUndefined()
  })

  it('应调用正式引导完成接口', async () => {
    const path = '/edu-agent-auth/onboard-done'

    mock.onPost(path).reply(200, {
      code: 0,
      message: 'success',
      data: null
    })

    await markOnboardDone()

    expect(mock.history.post).toHaveLength(1)
    expect(mock.history.post[0]?.url).toBe(path)
    expect(mock.history.post[0]?.data).toBeUndefined()
  })
})