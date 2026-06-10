import request from '@/utils/request'

export interface GenerateParams {
  chapter: string       // 章节名，如 "Java 基础语法"
  topic: string         // 知识点，如 "变量与数据类型"
  resourceType: string  // mindmap | quiz | reading | code
  level: string         // basic | medium | advanced
  difficulty?: string   // easy | medium | hard
  chapterId?: number    // 章节ID（用于DB关联）
  chapterName?: string  // 章节名
}

export interface GeneratedContent {
  content: string
  resourceType: string
  chapter: string
  difficulty?: string
}

export interface ChapterResource {
  id: number
  title: string
  type: string           // mindmap | quiz | reading | code
  difficulty: string     // easy | medium | hard
  content: string
  courseId: string
  courseName: string
  status: string
  views: number
  favorites: number
  rating: number
  createTime: string
  updateTime: string
}

/** 获取章节的所有资源 */
export function getChapterResources(chapterId: number): Promise<ChapterResource[]> {
  return request.get(`/resources/chapter/${chapterId}`)
}

/** 获取章节特定类型资源（有缓存则直接返回，无则AI生成） */
export function getChapterResource(
  chapterId: number,
  type: string,
  difficulty: string = 'medium',
  chapterName: string = '',
  topic: string = ''
): Promise<ChapterResource> {
  return request.get(`/resources/chapter/${chapterId}/${type}`, {
    params: { difficulty, chapterName, topic }
  })
}

/** 获取单个资源 */
export function getResource(id: number): Promise<ChapterResource> {
  return request.get(`/resources/${id}`)
}

/** AI生成资源（前端主动触发） */
export function generateResource(params: {
  chapterId: number
  chapterName: string
  topic: string
  type: string
  difficulty: string
}): Promise<ChapterResource> {
  return request.post('/resources/generate', params)
}

/** 重新生成资源（换难度） */
export function regenerateResource(id: number, difficulty: string): Promise<ChapterResource> {
  return request.post(`/resources/${id}/regenerate`, { difficulty })
}

/** 保存用户反馈 */
export function saveResourceFeedback(id: number, feedback: {
  liked?: boolean
  difficulty?: string
}): Promise<void> {
  return request.post(`/resources/${id}/feedback`, feedback)
}

// ===== 兼容旧版直接调用（内部转调后端） =====

const typeLabelMap: Record<string, string> = {
  mindmap: '思维导图', quiz: '练习题目', reading: '拓展阅读', code: '代码案例',
}

/** @deprecated 使用 getChapterResource 替代 */
export async function generateMindmap(params: GenerateParams): Promise<GeneratedContent> {
  const res = await generateResource({
    chapterId: params.chapterId || 0,
    chapterName: params.chapterName || params.chapter || '',
    topic: params.topic || params.chapter || '',
    type: 'mindmap',
    difficulty: params.difficulty || mapLevel(params.level),
  })
  return { content: res.content, resourceType: 'mindmap', chapter: params.chapter, difficulty: res.difficulty }
}

/** @deprecated 使用 getChapterResource 替代 */
export async function generateQuiz(params: GenerateParams): Promise<GeneratedContent> {
  const res = await generateResource({
    chapterId: params.chapterId || 0,
    chapterName: params.chapterName || params.chapter || '',
    topic: params.topic || params.chapter || '',
    type: 'quiz',
    difficulty: params.difficulty || mapLevel(params.level),
  })
  return { content: res.content, resourceType: 'quiz', chapter: params.chapter, difficulty: res.difficulty }
}

/** @deprecated 使用 getChapterResource 替代 */
export async function generateReading(params: GenerateParams): Promise<GeneratedContent> {
  const res = await generateResource({
    chapterId: params.chapterId || 0,
    chapterName: params.chapterName || params.chapter || '',
    topic: params.topic || params.chapter || '',
    type: 'reading',
    difficulty: params.difficulty || mapLevel(params.level),
  })
  return { content: res.content, resourceType: 'reading', chapter: params.chapter, difficulty: res.difficulty }
}

/** @deprecated 使用 getChapterResource 替代 */
export async function generateCode(params: GenerateParams): Promise<GeneratedContent> {
  const res = await generateResource({
    chapterId: params.chapterId || 0,
    chapterName: params.chapterName || params.chapter || '',
    topic: params.topic || params.chapter || '',
    type: 'code',
    difficulty: params.difficulty || mapLevel(params.level),
  })
  return { content: res.content, resourceType: 'code', chapter: params.chapter, difficulty: res.difficulty }
}

function mapLevel(level: string): string {
  const map: Record<string, string> = {
    basic: 'easy',
    medium: 'medium',
    advanced: 'hard',
  }
  return map[level] || 'medium'
}
