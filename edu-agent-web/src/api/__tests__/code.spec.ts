import AxiosMockAdapter from 'axios-mock-adapter'
import {
  beforeEach,
  describe,
  expect,
  it
} from 'vitest'
import request from '@/utils/request'
import { regradeCodeSubmission } from '@/api/code'

describe('代码判分服务接口契约', () => {
  const mock = new AxiosMockAdapter(request)

  beforeEach(() => {
    mock.reset()
  })

  it('应按正式路径提交教师重新判分请求', async () => {
    const submissionId = 1024
    const path =
      `/edu-agent-code/submissions/${submissionId}/regrade`

    mock.onPost(path).reply(202, {
      code: 0,
      message: 'success',
      data: {
        submissionId,
        status: 0
      }
    })

    const result = await regradeCodeSubmission(submissionId)

    expect(mock.history.post).toHaveLength(1)
    expect(mock.history.post[0]?.url).toBe(path)
    expect(mock.history.post[0]?.data).toBeUndefined()
    expect(result).toEqual({
      submissionId,
      status: 0
    })
  })
})