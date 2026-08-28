import request from '@/utils/request'

const TEACHER_SERVICE = '/edu-agent-teacher'

export interface TeacherClass {
  id: number
  name: string
  teacherId: number
  course: string
  semester: string
  status: number
  createTime: string
  studentCount: number
}

export interface ClassStudent {
  studentId: number
  studentName: string
  joinedAt: string
}

export interface CreateClassRequest {
  name: string
  course?: string
  semester?: string
}

export interface UpdateClassRequest {
  name?: string
  course?: string
  semester?: string
}

export interface AddStudentRequest {
  studentId: number
}

/** 查询当前教师本人创建的班级 */
export function getTeacherClasses(): Promise<TeacherClass[]> {
  return request.get<unknown, TeacherClass[]>(
    `${TEACHER_SERVICE}/classes`
  )
}

/** 查询班级详情 */
export function getTeacherClass(
  classId: number
): Promise<TeacherClass> {
  return request.get<unknown, TeacherClass>(
    `${TEACHER_SERVICE}/classes/${classId}`
  )
}

/** 创建班级 */
export function createTeacherClass(
  data: CreateClassRequest
): Promise<TeacherClass> {
  return request.post<unknown, TeacherClass>(
    `${TEACHER_SERVICE}/classes`,
    data
  )
}

/** 更新班级 */
export function updateTeacherClass(
  classId: number,
  data: UpdateClassRequest
): Promise<TeacherClass> {
  return request.put<unknown, TeacherClass>(
    `${TEACHER_SERVICE}/classes/${classId}`,
    data
  )
}

/** 删除班级 */
export function deleteTeacherClass(
  classId: number
): Promise<void> {
  return request.delete<unknown, void>(
    `${TEACHER_SERVICE}/classes/${classId}`
  )
}

/** 查询班级学生 */
export function getClassStudents(
  classId: number
): Promise<ClassStudent[]> {
  return request.get<unknown, ClassStudent[]>(
    `${TEACHER_SERVICE}/classes/${classId}/students`
  )
}

/** 添加学生到班级 */
export function addClassStudent(
  classId: number,
  data: AddStudentRequest
): Promise<void> {
  return request.post<unknown, void>(
    `${TEACHER_SERVICE}/classes/${classId}/students`,
    data
  )
}

/** 从班级移除学生 */
export function removeClassStudent(
  classId: number,
  studentId: number
): Promise<void> {
  return request.delete<unknown, void>(
    `${TEACHER_SERVICE}/classes/${classId}/students/${studentId}`
  )
}
export type QuestionType = 'choice' | 'code' | 'blank'

export type QuestionDifficulty =
  | 'easy'
  | 'medium'
  | 'hard'

export interface TeacherQuestion {
  id: number
  type: QuestionType
  chapter: string
  topic: string
  content: string
  options: string[]
  answer: string
  explanation: string
  difficulty: QuestionDifficulty
  creatorId: number
  createTime: string
}

export interface QuestionQuery {
  chapter?: string
  topic?: string
  type?: QuestionType
  difficulty?: QuestionDifficulty
}

export interface CreateQuestionRequest {
  type: QuestionType
  chapter?: string
  topic?: string
  content: string
  options?: string[]
  answer?: string
  explanation?: string
  difficulty?: QuestionDifficulty
}

export interface QuestionGenerateRequest {
  chapter?: string
  topic?: string
  type?: QuestionType
  difficulty?: QuestionDifficulty
  count?: number
}

/** 查询题库，可按章节、知识点、题型和难度筛选 */
export function getTeacherQuestions(
  params: QuestionQuery = {}
): Promise<TeacherQuestion[]> {
  return request.get<unknown, TeacherQuestion[]>(
    `${TEACHER_SERVICE}/questions`,
    { params }
  )
}

/** 查询题目详情 */
export function getTeacherQuestion(
  questionId: number
): Promise<TeacherQuestion> {
  return request.get<unknown, TeacherQuestion>(
    `${TEACHER_SERVICE}/questions/${questionId}`
  )
}

/** 新增题目 */
export function createTeacherQuestion(
  data: CreateQuestionRequest
): Promise<TeacherQuestion> {
  return request.post<unknown, TeacherQuestion>(
    `${TEACHER_SERVICE}/questions`,
    data
  )
}

/** 更新题目，正式契约使用完整 CreateQuestionRequest */
export function updateTeacherQuestion(
  questionId: number,
  data: CreateQuestionRequest
): Promise<TeacherQuestion> {
  return request.put<unknown, TeacherQuestion>(
    `${TEACHER_SERVICE}/questions/${questionId}`,
    data
  )
}

/** 删除题目 */
export function deleteTeacherQuestion(
  questionId: number
): Promise<void> {
  return request.delete<unknown, void>(
    `${TEACHER_SERVICE}/questions/${questionId}`
  )
}

/** AI 生成题目草稿，教师确认后再调用新增接口保存 */
export function generateTeacherQuestions(
  data: QuestionGenerateRequest
): Promise<TeacherQuestion[]> {
  return request.post<unknown, TeacherQuestion[]>(
    `${TEACHER_SERVICE}/questions/generate`,
    data
  )
}