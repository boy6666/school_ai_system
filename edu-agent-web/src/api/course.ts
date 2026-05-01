import request from '@/utils/request'

export interface CourseResource {
  id: number
  title: string
  type: string
  difficulty: string
  duration: string
}

export interface CourseChapter {
  id: number
  title: string
  description: string
  duration: string
  progress: number
  status: 'not-started' | 'learning' | 'done'
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
  chapters: CourseChapter[]
  tasks: CourseTask[]
}

export function getCourseDetail(id: string) {
  return request.get<unknown, CourseDetail>(`/courses/${id}`)
}

export function updateChapterProgress(courseId: string, chapterId: number, progress: number) {
  return request.post<unknown, { success: boolean }>(
    `/courses/${courseId}/chapters/${chapterId}/progress`,
    { progress }
  )
}

export function saveCourseNote(courseId: string, chapterId: number, content: string) {
  return request.post<unknown, { success: boolean }>(
    `/courses/${courseId}/chapters/${chapterId}/note`,
    { content }
  )
}