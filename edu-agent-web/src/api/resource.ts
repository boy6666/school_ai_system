import request from '@/utils/request'

export type ResourceType =
  | 'mindmap'
  | 'quiz'
  | 'reading'
  | 'code'

export type ResourceDifficulty =
  | 'easy'
  | 'medium'
  | 'hard'

export interface ResourceVO {
  id: number
  userId?: number
  title: string
  type: ResourceType | string
  difficulty: ResourceDifficulty | string
  chapter: string
  chapterId?: string
  courseName?: string
  description?: string
  content: string
  status: string
  errorMsg?: string
  rating?: number
  views?: number
  favorites?: number
  createTime?: string
}
export interface ResourceEntity
  extends ResourceVO {
  updateTime?: string
}

export interface CreateResourceRequest {
  userId?: number
  title: string
  type: ResourceType | string
  difficulty?: ResourceDifficulty | string
  chapter?: string
  chapterId?: string
  courseName?: string
  description?: string
  content?: string
  status?: string
  errorMsg?: string
  rating?: number
  views?: number
  favorites?: number
}

export interface ResourceGenerateRequest {
  userId?: number
  chapter?: string
  chapterName?: string
  topic?: string
  type?: ResourceType | string
  difficulty?: ResourceDifficulty | string
  force?: boolean
  chapterId?: string
}

export interface ResourceFeedbackRequest {
  liked?: boolean
  difficultyFeedback?: string
}

/** 查询资源列表 */
export function getResourceList(): Promise<ResourceVO[]> {
  return request.get<unknown, ResourceVO[]>(
    '/edu-agent-resource'
  )
}

/** 新增资源 */
export function createResource(
  data: CreateResourceRequest
): Promise<ResourceEntity> {
  return request.post<unknown, ResourceEntity>(
    '/edu-agent-resource',
    data
  )
}

/** 查询资源详情 */
export function getResource(
  id: number
): Promise<ResourceVO> {
  return request.get<unknown, ResourceVO>(
    `/edu-agent-resource/${id}`
  )
}

/** 删除资源 */
export function deleteResource(
  id: number
): Promise<void> {
  return request.delete<unknown, void>(
    `/edu-agent-resource/${id}`
  )
}

/** 设置或取消收藏 */
export function setResourceFavorite(
  id: number,
  favorite: boolean
): Promise<void> {
  return request.post<unknown, void>(
    `/edu-agent-resource/${id}/favorite`,
    null,
    {
      params: { favorite }
    }
  )
}

/** 根据正式契约生成学习资源 */
export function generateResource(
  data: ResourceGenerateRequest
): Promise<ResourceVO> {
  return request.post<unknown, ResourceVO>(
    '/edu-agent-resource/generate',
    data
  )
}

/** 重新生成指定资源 */
export function regenerateResource(
  id: number
): Promise<ResourceVO> {
  return request.post<unknown, ResourceVO>(
    `/edu-agent-resource/${id}/regenerate`
  )
}

/** 提交资源使用反馈 */
export function submitResourceFeedback(
  id: number,
  data: ResourceFeedbackRequest
): Promise<void> {
  return request.post<unknown, void>(
    `/edu-agent-resource/${id}/feedback`,
    data
  )
}

/** 查询当前用户收藏的资源 */
export function getFavoriteResources(): Promise<ResourceVO[]> {
  return request.get<unknown, ResourceVO[]>(
    '/edu-agent-resource/favorites/mine'
  )
}

/** 查询指定章节的资源 */
export function getChapterResources(
  chapterId: string
): Promise<ResourceVO[]> {
  return request.get<unknown, ResourceVO[]>(
    `/edu-agent-resource/chapter/${chapterId}`
  )
}

/** 按章节和类型查询资源 */
export function getChapterResourcesByType(
  chapterId: string,
  type: ResourceType | string
): Promise<ResourceVO[]> {
  return request.get<unknown, ResourceVO[]>(
    `/edu-agent-resource/chapter/${chapterId}/${type}`
  )
}
