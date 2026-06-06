import request from '@/utils/request'

export type ResourceType =
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

export type ResourceSortType = 'hot' | 'new' | 'score'

export interface ResourceListQuery {
  keyword?: string
  type?: string
  difficulty?: string
  courseId?: string
  sort?: ResourceSortType
  page?: number
  pageSize?: number
}

export interface ResourceListItem {
  id: number
  title: string
  type: ResourceType
  difficulty: ResourceDifficulty
  description: string
  rating: number
  views: number
  updateTime: string
  cover: string
  favorite: boolean

  courseId: string
  courseName: string
  chapterName: string
  tags: string[]
  fileSize: string
}

export interface ResourceListResponse {
  list: ResourceListItem[]
  total: number
  recommended: ResourceListItem[]
  hotTags: string[]
}

export interface ResourceChapter {
  id: number
  title: string
  desc: string
  duration: string
}

export interface ResourceReview {
  id: number
  name: string
  score: number
  content: string
}

export interface ResourceDetailItem extends ResourceListItem {
  chapterCount: number
  duration: string
  teacher: string
  progress: number
  goals: string[]
  suitableFor: string[]
  chapters: ResourceChapter[]
  reviews: ResourceReview[]
}

export function getResourceList(params: ResourceListQuery) {
  return request.get<unknown, ResourceListResponse>('/resources', {
    params
  })
}

export function getResourceDetail(id: number) {
  return request.get<unknown, ResourceDetailItem>(`/resources/${id}`)
}

export function getRelatedResources(id: number) {
  return request.get<unknown, ResourceListItem[]>(`/resources/${id}/related`)
}

export function updateResourceFavorite(id: number, favorite: boolean) {
  return request.post<unknown, { favorite: boolean }>(
    `/resources/${id}/favorite`,
    { favorite }
  )
}

export function addResourceToPlan(id: number) {
  return request.post<unknown, { success: boolean }>(
    `/resources/${id}/plan`
  )
}