import request from '@/utils/request'

export interface ChapterItem {
  id: number
  category: string
  label: string
  count: number
}

export interface NoteItem {
  id: number
  title: string
  category: string
}

export interface NoteDetail {
  id: number
  category: string
  title: string
  content: string
  createdAt: string
}

export function getCategories() {
  return request.get<unknown, ChapterItem[]>('/notes/categories')
}

export function getNotes(category: string) {
  return request.get<unknown, NoteItem[]>('/notes', { params: { category } })
}

export function getNoteDetail(id: number) {
  return request.get<unknown, NoteDetail>(`/notes/${id}`)
}
