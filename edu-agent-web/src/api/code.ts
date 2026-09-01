import request from '@/utils/request'

const CODE_SERVICE = '/edu-agent-code'

export type CodeSubmitStatus = 0 | 1 | 2 | 3 | 4 | 5
export type CodeJudgeMode = 'IO' | 'HARNESS'

export interface CodeFile {
  name: string
  sourceCode: string
}

export interface CodeSubmitRequest {
  studentId?: number
  assignmentId?: number
  assignmentItemId?: number
  language: string
  files?: CodeFile[]
  className: string
  sourceCode?: string
  expectedOutput?: string
  mode?: CodeJudgeMode
}

export interface CodeSubmitReceipt {
  submissionId: number
  status: CodeSubmitStatus
}

export interface CodeSubmitResult {
  submissionId: number
  status: CodeSubmitStatus
  stdout?: string
  runTimeMs?: number
  compileOk?: 0 | 1
  checkstyle?: string
  pmd?: string
  aiSuggestion?: string
  overallScore?: number
}

/** 学生提交代码，服务返回异步判分受理回执 */
export function submitCode(
  data: CodeSubmitRequest
): Promise<CodeSubmitReceipt> {
  return request.post<unknown, CodeSubmitReceipt>(
    `${CODE_SERVICE}/submit`,
    data
  )
}

/** 根据提交编号查询异步判分结果 */
export function getCodeResult(
  submissionId: number
): Promise<CodeSubmitResult> {
  return request.get<unknown, CodeSubmitResult>(
    `${CODE_SERVICE}/result/${submissionId}`
  )
}

/** 教师对已完成的代码提交发起重新判分 */
export function regradeCodeSubmission(
  submissionId: number
): Promise<CodeSubmitReceipt> {
  return request.post<unknown, CodeSubmitReceipt>(
    `${CODE_SERVICE}/submissions/${submissionId}/regrade`
  )
}