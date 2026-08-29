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
  title: string
  type: ResourceType | string
  difficulty: ResourceDifficulty | string
  chapter: string
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
  userId?: number
  chapterId?: string
  courseName?: string
  description?: string
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