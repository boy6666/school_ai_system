import axios from 'axios'

const aiClient = axios.create({
  baseURL: '/ai',
  timeout: 120000,
})

export interface GenerateParams {
  chapter: string       // 章节名，如 "Java 基础语法"
  topic: string         // 知识点，如 "变量与数据类型"
  resourceType: string  // mindmap | quiz | reading | code
  level: string         // basic | medium | advanced
}

export interface GeneratedContent {
  content: string
  resourceType: string
  chapter: string
}

// 思维导图
export async function generateMindmap(params: GenerateParams): Promise<GeneratedContent> {
  const res = await aiClient.post('/resource/generate', {
    ...params,
    resourceType: 'mindmap',
    prompt: `请为"${params.chapter}"章节生成一份Mermaid思维导图。难度：${params.level}。只返回graph TD开头的Mermaid代码。`
  })
  return { content: res.data.content || res.data.final_answer || '', resourceType: 'mindmap', chapter: params.chapter }
}

// 练习题目
export async function generateQuiz(params: GenerateParams): Promise<GeneratedContent> {
  const res = await aiClient.post('/resource/generate', {
    ...params,
    resourceType: 'quiz',
    prompt: `请为"${params.chapter}"章节生成5道练习题(含答案)。难度：${params.level}。返回JSON数组，每题包含type/question/options/answer/explanation。`
  })
  return { content: res.data.content || res.data.final_answer || '[]', resourceType: 'quiz', chapter: params.chapter }
}

// 拓展阅读
export async function generateReading(params: GenerateParams): Promise<GeneratedContent> {
  const res = await aiClient.post('/resource/generate', {
    ...params,
    resourceType: 'reading',
    prompt: `请为"${params.chapter}"章节生成一份拓展阅读材料(约500字)。难度：${params.level}。包含进阶概念、实际应用场景和推荐学习资源。`
  })
  return { content: res.data.content || res.data.final_answer || '', resourceType: 'reading', chapter: params.chapter }
}

// 代码案例
export async function generateCode(params: GenerateParams): Promise<GeneratedContent> {
  const res = await aiClient.post('/resource/generate', {
    ...params,
    resourceType: 'code',
    prompt: `请为"${params.chapter}"章节生成一个Java代码案例(可运行)。难度：${params.level}。包含注释说明核心逻辑，代码量约30-80行。`
  })
  return { content: res.data.content || res.data.final_answer || '', resourceType: 'code', chapter: params.chapter }
}
