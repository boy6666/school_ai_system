import request from '@/utils/request'
import type { PageResult } from '@/utils/request'

export interface ProjectListParams {
  category?: string
  level?: string
  keyword?: string
  page: number
  pageSize: number
}

export interface ProjectStep {
  title: string
  description: string
  content?: string
  completed: boolean
  locked: boolean
}

export interface ProjectResource {
  title: string
  type: string
  duration: string
  url?: string
  downloadUrl?: string
}

export interface ProjectItem {
  id: number
  title: string
  description: string
  avatar?: string
  category: string
  level: string
  tags: string[]
  enrolled: number
  rating: number
  duration: string
  progress: number
  objectives?: string[]
  technologies?: string[]
  steps?: ProjectStep[]
  resources?: ProjectResource[]
}

export interface ProjectSubmitParams {
  projectId: number
  projectUrl: string
  repoUrl: string
  description: string
}

export interface ProjectProgress {
  progress: number
}

export const getProjectList = (params: ProjectListParams) => {
  return request.get<unknown, PageResult<ProjectItem>>('/edu-agent-learning/project/list', {
    params
  })
}

export const getProjectDetail = (id: number) => {
  return request.get<unknown, ProjectItem>(`/edu-agent-learning/project/${id}`)
}

export const joinProject = (id: number) => {
  return request.post<unknown, ProjectItem>(`/edu-agent-learning/project/${id}/join`)
}

export const submitProject = (params: ProjectSubmitParams) => {
  return request.post<unknown, void>('/edu-agent-learning/project/submit', params)
}

export const getProjectProgress = (id: number) => {
  return request.get<unknown, ProjectProgress>(`/edu-agent-learning/project/${id}/progress`)
}