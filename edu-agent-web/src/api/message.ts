import request from '@/utils/request'
import type { PageResult } from '@/utils/request'

export interface MessageAttachment {
  name: string
  url: string
}

export interface MessageItem {
  id: number
  title: string
  content: string
  type: string
  read: boolean
  time: string
  attachment?: MessageAttachment
}

export interface ChatMessage {
  content: string
  time: string
  isMine: boolean
}

export interface ConversationItem {
  id: number
  name: string
  avatar?: string
  lastMessage: string
  time: string
  unread: number
  messages?: ChatMessage[]
}

export interface MessageListParams {
  type?: string
  status?: string
  page: number
  pageSize: number
}

export interface SendMessageParams {
  conversationId: number
  content: string
}

export const getMessageList = (params: MessageListParams) => {
  return request.get<unknown, PageResult<MessageItem>>('/edu-agent-learning/message/list', {
    params
  })
}

export const getMessageDetail = (id: number) => {
  return request.get<unknown, MessageItem>(`/edu-agent-learning/message/${id}`)
}

export const markAsRead = (id: number) => {
  return request.post<unknown, void>(`/edu-agent-learning/message/${id}/read`)
}

export const markAllRead = () => {
  return request.post<unknown, void>('/edu-agent-learning/message/read-all')
}

export const deleteMessage = (id: number) => {
  return request.delete<unknown, void>(`/edu-agent-learning/message/${id}`)
}

export const sendMessage = (params: SendMessageParams) => {
  return request.post<unknown, ChatMessage>('/edu-agent-learning/message/send', params)
}

export const getConversationList = () => {
  return request.get<unknown, ConversationItem[]>('/edu-agent-learning/message/conversations')
}

export const getConversationMessages = (id: number) => {
  return request.get<unknown, ChatMessage[]>(`/edu-agent-learning/message/conversation/${id}`)
}