import axios from 'axios'

const aiClient = axios.create({
  baseURL: '/ai',
  timeout: 120000,
})

export interface TutorMessage {
  id: number
  role: 'user' | 'assistant'
  content: string
  time: string
}

export interface TutorSuggestion {
  id: number
  title: string
  prompt: string
}

export interface TutorSession {
  messages: TutorMessage[]
  suggestions: TutorSuggestion[]
}

export interface ChatResponse {
  intent: string
  final_answer: string
  profile: Record<string, any>
  profile_changes: {
    changed_dimensions: Array<{
      dimension: string
      from_level: string
      to_level: string
      reason: string
    }>
    has_changes: boolean
  }
  resources: any
  learning_path: any
  evaluation_report: any
}

export function getTutorSession(): Promise<TutorSession> {
  // 不再需要从后端获取会话，前端自主管理
  return Promise.resolve({
    messages: [
      {
        id: 1,
        role: 'assistant',
        content: '你好，我是你的 AI 学习助手。你可以问我课程知识点、题目解析、学习计划或资源推荐。我们的对话将自动帮助你构建学习画像。',
        time: new Date().toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' }),
      },
    ],
    suggestions: [
      { id: 1, title: '解释 A* 算法核心思想', prompt: '请用简单例子解释 A* 算法的核心思想。' },
      { id: 2, title: '对比 BFS 和 DFS', prompt: 'BFS 和 DFS 的区别是什么？适合哪些场景？' },
      { id: 3, title: '生成本周学习计划', prompt: '请根据人工智能导论课程帮我生成本周学习计划。' },
      { id: 4, title: '推荐搜索算法资源', prompt: '推荐几个学习搜索算法的资源。' },
    ],
  })
}

// 生成持久的会话ID（页面级，不随每次请求变化）
let persistentSessionId: string | null = null
function getSessionId(): string {
  if (!persistentSessionId) {
    persistentSessionId = `web_${Date.now()}_${Math.random().toString(36).slice(2, 8)}`
  }
  return persistentSessionId
}

export function resetSessionId(): void {
  persistentSessionId = null
}

export async function sendTutorMessage(
  content: string,
  studentId: string = 'student001',
): Promise<{
  message: TutorMessage
  profile: Record<string, any>
  profileChanges: ChatResponse['profile_changes']
}> {
  const res = await aiClient.post<ChatResponse>('/chat', {
    user_input: content,
    student_id: studentId,
    session_id: getSessionId(),
  })

  const data = res.data

  return {
    message: {
      id: Date.now(),
      role: 'assistant',
      content: data.final_answer || 'AI 未能生成回复，请重试。',
      time: new Date().toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' }),
    },
    profile: data.profile,
    profileChanges: data.profile_changes,
  }
}
