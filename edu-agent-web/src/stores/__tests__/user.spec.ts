import {
  beforeEach,
  describe,
  expect,
  it
} from 'vitest'
import {
  createPinia,
  setActivePinia
} from 'pinia'
import {
  useUserStore,
  type UserInfo
} from '@/stores/user'
import { ROLE } from '@/utils/constants'

describe('用户登录状态管理', () => {
  beforeEach(() => {
    localStorage.clear()
    setActivePinia(createPinia())
  })

  it('应保存Token', () => {
    const store = useUserStore()

    store.setToken('test-token')

    expect(store.token).toBe('test-token')
    expect(
      localStorage.getItem('token')
    ).toBe('test-token')
  })

  it('应保存用户信息和角色数组', () => {
    const store = useUserStore()

    const userInfo: UserInfo = {
      userId: 1001,
      username: 'student01',
      realName: '测试学生',
      roles: [ROLE.STUDENT],
      onboarded: 1
    }

    store.setUserInfo(userInfo)

    const expectedUserInfo = {
      ...userInfo,
      name: '测试学生',
      role: ROLE.STUDENT
    }

    expect(store.userInfo).toEqual(
      expectedUserInfo
    )
    expect(store.roles).toEqual([
      ROLE.STUDENT
    ])
    expect(
      JSON.parse(
        localStorage.getItem('roles') || '[]'
      )
    ).toEqual([
      ROLE.STUDENT
    ])
    expect(
      JSON.parse(
        localStorage.getItem('userInfo') ||
          'null'
      )
    ).toEqual(
      expectedUserInfo
    )
  })

  it('保存新角色时应清除旧版单角色字段', () => {
    localStorage.setItem('role', 'admin')

    const store = useUserStore()

    store.setUserInfo({
      userId: 1002,
      username: 'student02',
      realName: '测试用户',
      roles: [ROLE.STUDENT],
      onboarded: 0
    })

    expect(
      localStorage.getItem('role')
    ).toBeNull()
    expect(store.roles).toEqual([
      ROLE.STUDENT
    ])
    expect(store.userInfo?.name).toBe(
      '测试用户'
    )
  })

  it('退出登录时应清除全部认证状态', () => {
    const store = useUserStore()

    store.setToken('test-token')
    store.setUserInfo({
      userId: 1003,
      username: 'admin01',
      realName: '测试管理员',
      roles: [ROLE.ADMIN],
      onboarded: 1
    })

    store.logout()

    expect(store.token).toBe('')
    expect(store.roles).toEqual([])
    expect(store.userInfo).toBeNull()

    expect(
      localStorage.getItem('token')
    ).toBeNull()
    expect(
      localStorage.getItem('roles')
    ).toBeNull()
    expect(
      localStorage.getItem('role')
    ).toBeNull()
    expect(
      localStorage.getItem('userInfo')
    ).toBeNull()
  })
})