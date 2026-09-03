import {
  flushPromises,
  mount
} from '@vue/test-utils'
import ElementPlus from 'element-plus'
import {
  beforeEach,
  describe,
  expect,
  it,
  vi
} from 'vitest'
import ResourceGenerate from '@/views/student/ResourceGenerate.vue'

const {
  generateResourceMock,
  pushMock
} = vi.hoisted(() => ({
  generateResourceMock: vi.fn(),
  pushMock: vi.fn()
}))

vi.mock('vue-router', () => ({
  useRoute: () => ({
    params: {
      type: 'reading'
    },
    query: {
      chapterId: 'chapter-3'
    }
  }),
  useRouter: () => ({
    push: pushMock
  })
}))

vi.mock('@/stores/user', () => ({
  useUserStore: () => ({
    userInfo: {
      userId: 8
    }
  })
}))

vi.mock('@/api/resource', () => ({
  generateResource: generateResourceMock
}))

function mountPage() {
  return mount(ResourceGenerate, {
    global: {
      plugins: [ElementPlus]
    }
  })
}

function findGenerateButton(
  wrapper: ReturnType<typeof mountPage>
) {
  return wrapper
    .findAll('button')
    .find(button =>
      button.text().includes('生成资源')
    )
}

describe('学生资源生成页面', () => {
  beforeEach(() => {
    generateResourceMock.mockReset()
    pushMock.mockReset()
  })

  it('未填写章节或主题时不应发送生成请求', async () => {
    const wrapper = mountPage()
    const generateButton =
      findGenerateButton(wrapper)

    expect(generateButton).toBeDefined()

    await generateButton!.trigger('click')

    expect(
      generateResourceMock
    ).not.toHaveBeenCalled()
  })

  it('应按正式契约生成并展示资源', async () => {
    generateResourceMock.mockResolvedValue({
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
    })

    const wrapper = mountPage()

    await wrapper
      .get('input[placeholder="例如：ArrayList"]')
      .setValue('ArrayList')

    const generateButton =
      findGenerateButton(wrapper)

    expect(generateButton).toBeDefined()

    await generateButton!.trigger('click')
    await flushPromises()

    expect(
      generateResourceMock
    ).toHaveBeenCalledWith({
      userId: 8,
      chapterId: 'chapter-3',
      chapter: undefined,
      chapterName: undefined,
      topic: 'ArrayList',
      type: 'reading',
      difficulty: 'medium',
      force: false
    })

    expect(wrapper.text()).toContain(
      'ArrayList学习资料'
    )
    expect(wrapper.text()).toContain(
      '资源正文'
    )
    expect(wrapper.text()).toContain(
      'completed'
    )
  })
})