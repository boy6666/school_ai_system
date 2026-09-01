import request from '@/utils/request'

const CODE_SERVICE = '/edu-agent-code'

export interface CodeSubmitReceipt {
  submissionId: number
  status: number
}

/** 教师对已完成的代码提交发起重新判分 */
export function regradeCodeSubmission(
  submissionId: number
): Promise<CodeSubmitReceipt> {
  return request.post<unknown, CodeSubmitReceipt>(
    `${CODE_SERVICE}/submissions/${submissionId}/regrade`
  )
}