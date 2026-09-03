import AxiosMockAdapter from 'axios-mock-adapter'
import {
  afterAll,
  beforeEach,
  describe,
  expect,
  it
} from 'vitest'
import request from '@/utils/request'
import { sendTutorMessage } from '@/api/tutor'

describe('AI助教接口契约', () => {
  const mock = new AxiosMockAdapter(request)

  beforeEach(() => {
    mock.reset()
  })

  afterAll(() => {
    mock.restore()
  })

  it('应按近期AI契约发送消息并读取回复', async () => {
    const path = '/edu-agent-ai/chat'
    const profile = {
      course: 'Java程序设计'
    }

    mock.onPost(path).reply(200, {
      code: 0,
      message: 'success',
      data: {
        intent: 'explain',
        final_answer: '多态是面向对象的重要特性。',
        profile,
        resources: [],
        learning_path: null,
        safety_report: null,
        evaluation_report: null,
        resource_dir: null,
        profile_complete: false
      }
    })

    const result = await sendTutorMessage(
      '请解释Java多态',
      '8',
      'session-1',
      profile
    )

    expect(mock.history.post).toHaveLength(1)
    expect(mock.history.post[0]?.url).toBe(path)
    expect(
      JSON.parse(mock.history.post[0]?.data)
    ).toEqual({
      user_input: '请解释Java多态',
      student_id: '8',
      session_id: 'session-1',
      profile
    })
    expect(result.final_answer).toBe(
      '多态是面向对象的重要特性。'
    )
  })
})