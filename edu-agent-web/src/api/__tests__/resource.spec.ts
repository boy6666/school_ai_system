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
import {
  createResource,
  deleteResource,
  generateResource,
  getChapterResources,
  getChapterResourcesByType,
  getFavoriteResources,
  getResource,
  getResourceList,
  regenerateResource,
  setResourceFavorite,
  submitResourceFeedback
} from '@/api/resource'

const mock = new AxiosMockAdapter(request)
const resourcePath = '/edu-agent-resource'

describe('资源服务接口契约', () => {
  beforeEach(() => {
    mock.reset()
  })

  afterAll(() => {
    mock.restore()
  })

  it('应按正式路径查询资源列表', async () => {
    const resources = [
      {
        id: 101,
        title: 'Java 多态学习资料',
        type: 'reading',
        difficulty: 'medium',
        chapter: '第2章 面向对象',
        content: '多态相关学习内容',
        status: 'completed',
        rating: 4.8,
        views: 20,
        favorites: 3,
        createTime: '2026-08-29T12:00:00'
      }
    ]

    mock.onGet(resourcePath).reply(200, {
      code: 0,
      message: 'success',
      data: resources
    })

    const result = await getResourceList()

    expect(mock.history.get[0]?.url).toBe(
      resourcePath
    )
    expect(result).toEqual(resources)
  })

  it('应使用正式请求体创建资源', async () => {
    const createData = {
      title: 'Java 集合讲义',
      type: 'reading',
      difficulty: 'medium',
      chapter: '第3章 集合',
      chapterId: 'chapter-3',
      courseName: 'Java 程序设计',
      description: '集合框架教学资料',
      content: '集合框架正文',
      status: 'completed'
    }

    const createdResource = {
      id: 102,
      userId: 8,
      ...createData,
      rating: 0,
      views: 0,
      favorites: 0,
      createTime: '2026-08-29T12:30:00',
      updateTime: '2026-08-29T12:30:00'
    }

    mock.onPost(resourcePath).reply(200, {
      code: 0,
      message: 'success',
      data: createdResource
    })

    const result = await createResource(createData)

    expect(mock.history.post[0]?.url).toBe(
      resourcePath
    )
    expect(
      JSON.parse(mock.history.post[0]?.data)
    ).toEqual(createData)
    expect(result).toEqual(createdResource)
  })

  it('应按正式路径查询资源详情', async () => {
    const detailPath = `${resourcePath}/101`
    const resource = {
      id: 101,
      title: 'Java 多态学习资料',
      type: 'reading',
      difficulty: 'medium',
      chapter: '第2章 面向对象',
      content: '多态相关学习内容',
      status: 'completed'
    }

    mock.onGet(detailPath).reply(200, {
      code: 0,
      message: 'success',
      data: resource
    })

    const result = await getResource(101)

    expect(mock.history.get[0]?.url).toBe(
      detailPath
    )
    expect(result).toEqual(resource)
  })

  it('应按正式契约收藏并删除资源', async () => {
    const favoritePath =
      `${resourcePath}/101/favorite`
    const detailPath = `${resourcePath}/101`

    mock.onPost(favoritePath).reply(200, {
      code: 0,
      message: 'success',
      data: null
    })

    mock.onDelete(detailPath).reply(200, {
      code: 0,
      message: 'success',
      data: null
    })

    await setResourceFavorite(101, true)
    await deleteResource(101)

    expect(mock.history.post[0]?.url).toBe(
      favoritePath
    )
    expect(mock.history.post[0]?.params).toEqual({
      favorite: true
    })
    expect(mock.history.delete[0]?.url).toBe(
      detailPath
    )
  })
    it('应按正式契约生成并重新生成资源', async () => {
    const generatePath = `${resourcePath}/generate`
    const regeneratePath =
      `${resourcePath}/101/regenerate`

    const generateData = {
      userId: 8,
      chapter: '第3章 集合',
      chapterName: 'Java集合框架',
      topic: 'ArrayList',
      type: 'reading',
      difficulty: 'medium',
      force: false,
      chapterId: 'chapter-3'
    }

    const generatedResource = {
      id: 101,
      userId: 8,
      title: 'ArrayList学习资料',
      type: 'reading',
      difficulty: 'medium',
      chapter: '第3章 集合',
      chapterId: 'chapter-3',
      courseName: 'Java程序设计',
      description: 'ArrayList相关学习资料',
      content: '资源正文',
      status: 'completed'
    }

    mock.onPost(generatePath).reply(200, {
      code: 0,
      message: 'success',
      data: generatedResource
    })

    mock.onPost(regeneratePath).reply(200, {
      code: 0,
      message: 'success',
      data: generatedResource
    })

    const generated = await generateResource(
      generateData
    )
    const regenerated = await regenerateResource(101)

    expect(mock.history.post).toHaveLength(2)
    expect(mock.history.post[0]?.url).toBe(
      generatePath
    )
    expect(
      JSON.parse(mock.history.post[0]?.data)
    ).toEqual(generateData)
    expect(mock.history.post[1]?.url).toBe(
      regeneratePath
    )
    expect(mock.history.post[1]?.data).toBeUndefined()
    expect(generated).toEqual(generatedResource)
    expect(regenerated).toEqual(generatedResource)
  })
    it('应按正式契约提交反馈并查询我的收藏', async () => {
    const feedbackPath =
      `${resourcePath}/101/feedback`
    const favoritesPath =
      `${resourcePath}/favorites/mine`

    const feedbackData = {
      liked: true,
      difficultyFeedback: '难度适中'
    }

    const favoriteResources = [
      {
        id: 101,
        title: 'ArrayList学习资料',
        type: 'reading',
        difficulty: 'medium',
        chapter: '第3章 集合',
        content: '资源正文',
        status: 'completed'
      }
    ]

    mock.onPost(feedbackPath).reply(200, {
      code: 0,
      message: 'success',
      data: null
    })

    mock.onGet(favoritesPath).reply(200, {
      code: 0,
      message: 'success',
      data: favoriteResources
    })

    await submitResourceFeedback(
      101,
      feedbackData
    )
    const result = await getFavoriteResources()

    expect(mock.history.post).toHaveLength(1)
    expect(mock.history.post[0]?.url).toBe(
      feedbackPath
    )
    expect(
      JSON.parse(mock.history.post[0]?.data)
    ).toEqual(feedbackData)
    expect(mock.history.get[0]?.url).toBe(
      favoritesPath
    )
    expect(result).toEqual(favoriteResources)
  })
    it('应按正式路径查询章节资源', async () => {
    const chapterId = 'chapter-3'
    const chapterPath =
      `${resourcePath}/chapter/${chapterId}`
    const typePath =
      `${chapterPath}/reading`

    const resources = [
      {
        id: 101,
        title: 'ArrayList学习资料',
        type: 'reading',
        difficulty: 'medium',
        chapter: '第3章 集合',
        content: '资源正文',
        status: 'completed'
      }
    ]

    mock.onGet(chapterPath).reply(200, {
      code: 0,
      message: 'success',
      data: resources
    })

    mock.onGet(typePath).reply(200, {
      code: 0,
      message: 'success',
      data: resources
    })

    const chapterResources =
      await getChapterResources(chapterId)
    const typedResources =
      await getChapterResourcesByType(
        chapterId,
        'reading'
      )

    expect(mock.history.get).toHaveLength(2)
    expect(mock.history.get[0]?.url).toBe(
      chapterPath
    )
    expect(mock.history.get[1]?.url).toBe(
      typePath
    )
    expect(chapterResources).toEqual(resources)
    expect(typedResources).toEqual(resources)
  })
})