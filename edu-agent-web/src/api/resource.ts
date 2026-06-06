import request from '@/utils/request'

export interface GenerateParams {
  chapter: string
  topic: string
  resourceType: string
  level: string
}

export interface GeneratedContent {
  content: string
  resourceType: string
  chapter: string
  exists?: boolean
}

async function generateViaBackend(type: string, title: string, chapterName: string, difficulty: string, studentId: any): Promise<GeneratedContent> {
  const res = await request.post<unknown, any>('/resources/generate', {
    studentId, type, title, chapterName, difficulty
  })
  return { content: res?.content || '', resourceType: type, chapter: chapterName, exists: res?.exists }
}

export async function generateMindmap(params: GenerateParams, studentId?: any): Promise<GeneratedContent> {
  return generateViaBackend('mindmap', params.topic, params.chapter, params.level, studentId)
}

export async function generateQuiz(params: GenerateParams, studentId?: any): Promise<GeneratedContent> {
  return generateViaBackend('quiz', params.topic, params.chapter, params.level, studentId)
}

export async function generateReading(params: GenerateParams, studentId?: any): Promise<GeneratedContent> {
  return generateViaBackend('reading', params.topic, params.chapter, params.level, studentId)
}

export async function generateCode(params: GenerateParams, studentId?: any): Promise<GeneratedContent> {
  return generateViaBackend('code', params.topic, params.chapter, params.level, studentId)
}

export interface AdjustDifficultyParams {
  studentId: number
  type: string        // mindmap / quiz / reading / code
  chapterName: string
  title: string
  direction: 'up' | 'down'   // up=生成更难(用户说简单), down=生成更简单(用户说困难)
  currentDifficulty: string   // 当前难度 "简单"/"适合"/"困难"
}

export interface AdjustDifficultyResult {
  id: number
  content: string
  difficulty: string
  exists: boolean
}

/**
 * 调整资源难度
 * 用户觉得太简单 → direction=up (生成更难)
 * 用户觉得太困难 → direction=down (生成更简单)
 * AI 生成 → 存 DB → 读 DB → 返回
 */
export async function adjustDifficulty(params: AdjustDifficultyParams): Promise<AdjustDifficultyResult> {
  const res = await request.post<unknown, any>('/resources/adjust-difficulty', params)
  return {
    id: res?.id || 0,
    content: res?.content || '',
    difficulty: res?.difficulty || params.currentDifficulty,
    exists: res?.exists || false
  }
}
