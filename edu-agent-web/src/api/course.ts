import request from '@/utils/request'

export type CourseStatus = 'not-started' | 'learning' | 'done'

export type ChapterStatus = 'not-started' | 'learning' | 'done'

export type CourseResourceType =
  | '文档'
  | 'PPT'
  | '视频'
  | '动画'
  | '题库'
  | '代码案例'
  | '实验项目'
  | '拓展阅读'
  | '思维导图'

export type ResourceDifficulty = '入门' | '基础' | '进阶' | '高级'

export interface CourseListItem {
  id: string
  title: string
  teacher: string
  description: string
  cover: string
  progress: number
  totalChapters: number
  learnedChapters: number
  totalHours: number
  learnedHours: number
  currentChapter: string
  status: CourseStatus
  tags: string[]
}

export interface CourseListQuery {
  keyword?: string
  status?: string
}

export interface CourseListResponse {
  list: CourseListItem[]
  total: number
}

export interface CourseResource {
  id: number
  title: string
  type: CourseResourceType
  difficulty: ResourceDifficulty
  duration: string
}

export interface CourseChapter {
  id: number
  title: string
  description: string
  duration: string
  progress: number
  status: ChapterStatus
  knowledgePoints: string[]
  resources: CourseResource[]
}

export interface CourseTask {
  id: number
  title: string
  type: '章节学习' | '课后练习' | '项目任务'
  deadline: string
  status: 'todo' | 'doing' | 'done'
}

export interface CourseDetail {
  id: string
  title: string
  teacher: string
  description: string
  cover: string
  progress: number
  learnedHours: number
  totalHours: number
  totalChapters: number
  currentChapterId: number
  tags: string[]
  chapters: CourseChapter[]
  tasks: CourseTask[]
}

export function getCourseList(params: CourseListQuery) {
  return request.get<unknown, CourseListResponse>('/edu-agent-learning/courses', {
    params
  })
}

export function getCourseDetail(id: string) {
  return request.get<unknown, CourseDetail>(`/edu-agent-learning/courses/${id}`)
}

export function updateChapterProgress(
  courseId: string,
  chapterId: number,
  progress: number
) {
  return request.post<unknown, { success: boolean }>(
    `/edu-agent-learning/courses/${courseId}/chapters/${chapterId}/progress`,
    { progress }
  )
}

export function saveCourseNote(
  courseId: string,
  chapterId: number,
  content: string
) {
  return request.post<unknown, { success: boolean }>(
    `/edu-agent-learning/courses/${courseId}/chapters/${chapterId}/note`,
    { content }
  )
}