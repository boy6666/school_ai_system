import AxiosMockAdapter from 'axios-mock-adapter'
import {
  beforeEach,
  describe,
  expect,
  it
} from 'vitest'
import request from '@/utils/request'
import {
  getCodeResult,
  regradeCodeSubmission,
  submitCode
} from '@/api/code'

describe('代码判分服务接口契约', () => {
  const mock = new AxiosMockAdapter(request)

  beforeEach(() => {
    mock.reset()
  })

  it('应按正式路径提交学生代码判分请求', async () => {
    const path = '/edu-agent-code/submit'
    const payload = {
      language: 'java',
      className: 'Main',
      sourceCode:
        'public class Main { public static void main(String[] args) {} }',
      expectedOutput: '',
      mode: 'IO' as const
    }

    mock.onPost(path).reply(202, {
      code: 0,
      message: 'success',
      data: {
        submissionId: 1024,
        status: 0
      }
    })

    const result = await submitCode(payload)

    expect(mock.history.post).toHaveLength(1)
    expect(mock.history.post[0]?.url).toBe(path)
    expect(
      JSON.parse(mock.history.post[0]?.data || '{}')
    ).toEqual(payload)
    expect(result).toEqual({
      submissionId: 1024,
      status: 0
    })
  })

  it('应按正式路径查询代码判分结果', async () => {
    const submissionId = 1024
    const path =
      `/edu-agent-code/result/${submissionId}`

    mock.onGet(path).reply(200, {
      code: 0,
      message: 'success',
      data: {
        submissionId,
        status: 2,
        stdout: 'success',
        runTimeMs: 120,
        compileOk: 1,
        checkstyle: '{}',
        pmd: '{}',
        aiSuggestion: '代码结构清晰',
        overallScore: 95
      }
    })

    const result = await getCodeResult(submissionId)

    expect(mock.history.get).toHaveLength(1)
    expect(mock.history.get[0]?.url).toBe(path)
    expect(result).toEqual({
      submissionId,
      status: 2,
      stdout: 'success',
      runTimeMs: 120,
      compileOk: 1,
      checkstyle: '{}',
      pmd: '{}',
      aiSuggestion: '代码结构清晰',
      overallScore: 95
    })
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

    const result =
      await regradeCodeSubmission(submissionId)

    expect(mock.history.post).toHaveLength(1)
    expect(mock.history.post[0]?.url).toBe(path)
    expect(mock.history.post[0]?.data).toBeUndefined()
    expect(result).toEqual({
      submissionId,
      status: 0
    })
  })
})