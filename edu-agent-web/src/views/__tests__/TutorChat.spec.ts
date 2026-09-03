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
import TutorChat from '@/views/student/TutorChat.vue'

const {
  getSessionsMock,
  getTutorHistoryMock,
  sendTutorMessageMock
} = vi.hoisted(() => ({
  getSessionsMock: vi.fn(),
  getTutorHistoryMock: vi.fn(),
  sendTutorMessageMock: vi.fn()
}))

vi.mock('vue-router', () => ({
  useRoute: () => ({
    query: {}
  })
}))

vi.mock('@/stores/user', () => ({
  useUserStore: () => ({
    userInfo: {
      userId: 8
    }
  })
}))

vi.mock('@/api/tutor', () => ({
  getSessions: getSessionsMock,
  getTutorHistory: getTutorHistoryMock,
  sendTutorMessage: sendTutorMessageMock
}))

function mountPage() {
  return mount(TutorChat, {
    global: {
      plugins: [ElementPlus]
    }
  })
}

describe('AI助教对话页面', () => {
  beforeEach(() => {
    getSessionsMock.mockReset()
    getTutorHistoryMock.mockReset()
    sendTutorMessageMock.mockReset()

    getSessionsMock.mockResolvedValue([])
    getTutorHistoryMock.mockResolvedValue([])
  })

  it('应提交当前学生编号并展示AI回复', async () => {
    sendTutorMessageMock.mockResolvedValue({
      intent: 'explain',
      final_answer: '多态允许父类引用指向子类对象。'
    })

    const wrapper = mountPage()
    await flushPromises()

    await wrapper
      .get('input[placeholder="输入你的学习问题..."]')
      .setValue('请解释Java多态')

    const sendButton = wrapper
      .findAll('button')
      .find(button => button.text().includes('发送'))

    expect(sendButton).toBeDefined()

    await sendButton!.trigger('click')
    await flushPromises()

    expect(sendTutorMessageMock).toHaveBeenCalledWith(
      '请解释Java多态',
      '8',
      expect.stringMatching(/^session_/)
    )
    expect(wrapper.text()).toContain(
      '多态允许父类引用指向子类对象。'
    )
  })
})