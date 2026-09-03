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
import ResourceDetail from '@/views/student/ResourceDetail.vue'

const {
  getResourceMock,
  regenerateResourceMock,
  submitResourceFeedbackMock,
  pushMock
} = vi.hoisted(() => ({
  getResourceMock: vi.fn(),
  regenerateResourceMock: vi.fn(),
  submitResourceFeedbackMock: vi.fn(),
  pushMock: vi.fn()
}))

vi.mock('vue-router', () => ({
  useRoute: () => ({
    params: {
      id: '101'
    }
  }),
  useRouter: () => ({
    push: pushMock
  })
}))

vi.mock('@/api/resource', () => ({
  getResource: getResourceMock,
  regenerateResource: regenerateResourceMock,
  submitResourceFeedback: submitResourceFeedbackMock
}))

const resource = {
  id: 101,
  userId: 8,
  title: 'Java集合学习资料',
  type: 'reading',
  difficulty: 'medium',
  chapter: '第3章 集合',
  chapterId: 'chapter-3',
  courseName: 'Java程序设计',
  description: '集合框架学习资料',
  content: '原始资源正文',
  status: 'completed'
}

function mountPage() {
  return mount(ResourceDetail, {
    global: {
      plugins: [ElementPlus]
    }
  })
}

function findButton(
  wrapper: ReturnType<typeof mountPage>,
  text: string
) {
  return wrapper
    .findAll('button')
    .find(button => button.text().includes(text))
}

describe('学生资源详情页面', () => {
  beforeEach(() => {
    getResourceMock.mockReset()
    regenerateResourceMock.mockReset()
    submitResourceFeedbackMock.mockReset()
    pushMock.mockReset()

    getResourceMock.mockResolvedValue(resource)
  })

  it('应按正式契约重新生成资源并展示结果', async () => {
    regenerateResourceMock.mockResolvedValue({
      ...resource,
      title: '重新生成的Java集合资料',
      content: '重新生成后的资源正文'
    })

    const wrapper = mountPage()
    await flushPromises()

    const regenerateButton = findButton(
      wrapper,
      '重新生成'
    )

    expect(regenerateButton).toBeDefined()

    await regenerateButton!.trigger('click')
    await flushPromises()

    expect(
      regenerateResourceMock
    ).toHaveBeenCalledWith(101)

    expect(wrapper.text()).toContain(
      '重新生成的Java集合资料'
    )
    expect(wrapper.text()).toContain(
      '重新生成后的资源正文'
    )
  })
    it('应按正式契约提交资源反馈', async () => {
    submitResourceFeedbackMock.mockResolvedValue(
      undefined
    )

    const wrapper = mountPage()
    await flushPromises()

        const radioInputs = wrapper.findAll(
      'input[type="radio"]'
    )

    expect(radioInputs).toHaveLength(2)

    await radioInputs[0]!.setValue()
    await flushPromises()

    await wrapper
      .get(
        'input[placeholder="请输入难度反馈（可选）"]'
      )
      .setValue('内容难度合适')

    const submitButton = findButton(
      wrapper,
      '提交反馈'
    )

    expect(submitButton).toBeDefined()

    await submitButton!.trigger('click')
    await flushPromises()

    expect(
      submitResourceFeedbackMock
    ).toHaveBeenCalledWith(101, {
      liked: true,
      difficultyFeedback: '内容难度合适'
    })
  })
})