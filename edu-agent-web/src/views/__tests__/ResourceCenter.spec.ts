import {
  flushPromises,
  mount
} from '@vue/test-utils'
import {
  beforeEach,
  describe,
  expect,
  it,
  vi
} from 'vitest'
import ResourceCenter from '@/views/student/ResourceCenter.vue'

const {
  getResourceListMock,
  getFavoriteResourcesMock,
  setResourceFavoriteMock,
  pushMock
} = vi.hoisted(() => ({
  getResourceListMock: vi.fn(),
  getFavoriteResourcesMock: vi.fn(),
  setResourceFavoriteMock: vi.fn(),
  pushMock: vi.fn()
}))

vi.mock('vue-router', () => ({
  useRouter: () => ({
    push: pushMock
  })
}))

vi.mock('@/api/resource', () => ({
  getResourceList: getResourceListMock,
  getFavoriteResources: getFavoriteResourcesMock,
  setResourceFavorite: setResourceFavoriteMock
}))

const allResources = [
  {
    id: 101,
    title: 'Java集合学习资料',
    type: 'reading',
    difficulty: 'medium',
    chapter: '第3章 集合',
    content: '资源正文',
    favorites: 0
  }
]

const favoriteResources = [
  {
    id: 102,
    title: '已收藏的代码案例',
    type: 'code',
    difficulty: 'easy',
    chapter: '第4章 泛型',
    content: '代码案例正文',
    favorites: 1
  }
]

function mountPage() {
  return mount(ResourceCenter)
}

describe('学生资源中心页面', () => {
  beforeEach(() => {
    getResourceListMock.mockReset()
    getFavoriteResourcesMock.mockReset()
    setResourceFavoriteMock.mockReset()
    pushMock.mockReset()

    getResourceListMock.mockResolvedValue(allResources)
    getFavoriteResourcesMock.mockResolvedValue(
      favoriteResources
    )
  })

  it('进入页面时应查询全部资源', async () => {
    const wrapper = mountPage()
    await flushPromises()

    expect(getResourceListMock).toHaveBeenCalledOnce()
    expect(wrapper.text()).toContain(
      'Java集合学习资料'
    )
  })

  it('点击我的收藏时应查询收藏资源', async () => {
    const wrapper = mountPage()
    await flushPromises()

    const favoriteCard = wrapper
      .findAll('.mini-card')
      .find(card =>
        card.text().includes('我的收藏')
      )

    expect(favoriteCard).toBeDefined()

    await favoriteCard!.trigger('click')
    await flushPromises()

    expect(
      getFavoriteResourcesMock
    ).toHaveBeenCalledOnce()

    expect(wrapper.text()).toContain(
      '已收藏的代码案例'
    )
  })
})