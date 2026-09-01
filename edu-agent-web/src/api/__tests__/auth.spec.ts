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
  login
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
        onboarded: 1
      }
    })

    const result = await getMe()

    expect(mock.history.get).toHaveLength(1)
    expect(mock.history.get[0]?.url).toBe(path)
    expect(result.roles).toEqual(['ROLE_ADMIN'])
  })
})