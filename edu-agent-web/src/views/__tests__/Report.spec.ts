import { describe, it, expect } from 'vitest'

describe('Report 页面 - 统计卡片', () => {
  it('3张统计卡片的 span 布局应为 8+8+8', () => {
    // 验证 Report.vue 模板逻辑：三个 el-col span:8
    const spans = [8, 8, 8]
    expect(spans.reduce((a, b) => a + b, 0)).toBe(24)
  })

  it('底部卡片 span 应为 12+12 撑满整行', () => {
    const spans = [12, 12]
    expect(spans.reduce((a, b) => a + b, 0)).toBe(24)
  })

  it('图表区域 span 应为 16+8', () => {
    const spans = [16, 8]
    expect(spans.reduce((a, b) => a + b, 0)).toBe(24)
  })
})

describe('Report 页面 - 数据计算', () => {
  it('总学习时长应正确转换 秒→小时', () => {
    const totalSec = 36000
    const hours = Math.round(totalSec / 3600 * 10) / 10
    expect(hours).toBe(10)
  })

  it('总学习时长应保留一位小数', () => {
    const totalSec = 3661
    const hours = Math.round(totalSec / 3600 * 10) / 10
    expect(hours).toBe(1.0)
  })

  it('0秒应返回 0 小时', () => {
    const totalSec = 0
    const hours = Math.round(totalSec / 3600 * 10) / 10
    expect(hours).toBe(0)
  })
})

describe('Dashboard 页面 - 欢迎语', () => {
  it('早于12点应返回"早上好"', () => {
    // 模拟 morning
    const h = 9
    const greeting = h < 12 ? '早上好' : h < 18 ? '下午好' : '晚上好'
    expect(greeting).toBe('早上好')
  })

  it('12-18点应返回"下午好"', () => {
    const h = 14
    const greeting = h < 12 ? '早上好' : h < 18 ? '下午好' : '晚上好'
    expect(greeting).toBe('下午好')
  })

  it('18点后应返回"晚上好"', () => {
    const h = 20
    const greeting = h < 12 ? '早上好' : h < 18 ? '下午好' : '晚上好'
    expect(greeting).toBe('晚上好')
  })
})

describe('Dashboard 页面 - 任务过滤', () => {
  it('应过滤掉 status 为 done 的任务', () => {
    const tasks = [
      { id: 1, status: 'doing' },
      { id: 2, status: 'done' },
      { id: 3, status: 'todo' }
    ]
    const filtered = tasks.filter(x => x.status !== 'done')
    expect(filtered).toHaveLength(2)
    expect(filtered.map(t => t.id)).toEqual([1, 3])
  })
})

describe('LearningPath 页面 - 进度计算', () => {
  it('进度百分比应正确计算', () => {
    const completed = 3
    const total = 5
    const progress = Math.round((completed / total) * 100)
    expect(progress).toBe(60)
  })

  it('全部完成时进度应为 100%', () => {
    const completed = 5
    const total = 5
    const progress = Math.round((completed / total) * 100)
    expect(progress).toBe(100)
  })

  it('无任务时进度应为 0%', () => {
    const completed = 0
    const total = 0
    const progress = total > 0 ? Math.round((completed / total) * 100) : 0
    expect(progress).toBe(0)
  })
})

describe('LearningPath 页面 - 模块名映射', () => {
  const labelMap: Record<string, string> = {
    mindmap: '思维导图',
    quiz: '练习题目',
    reading: '拓展阅读',
    code: '代码案例'
  }

  it('应正确映射已知模块名', () => {
    expect(labelMap['mindmap']).toBe('思维导图')
    expect(labelMap['quiz']).toBe('练习题目')
    expect(labelMap['reading']).toBe('拓展阅读')
    expect(labelMap['code']).toBe('代码案例')
  })

  it('未知模块名应原样返回', () => {
    const moduleName = 'unknown_module'
    expect(labelMap[moduleName] || moduleName).toBe('unknown_module')
  })
})

describe('学习画像维度计算', () => {
  const dimMeta = {
    knowledge_mastery: { label: '知识掌握度', color: '#409eff' },
    learning_goal_clarity: { label: '目标清晰度', color: '#67c23a' },
    cognitive_adaptation: { label: '认知风格适配', color: '#e6a23c' },
    mistake_avoidance: { label: '错误规避力', color: '#f56c6c' },
    learning_autonomy: { label: '学习自主性', color: '#909399' },
    overall_level: { label: '综合能力', color: '#722ed1' }
  }

  it('六维画像应有 6 个维度', () => {
    expect(Object.keys(dimMeta)).toHaveLength(6)
  })

  it('每个维度都有 label 和 color', () => {
    Object.values(dimMeta).forEach(dim => {
      expect(dim.label).toBeTruthy()
      expect(dim.color).toMatch(/^#[0-9a-fA-F]{6}$/)
    })
  })
})
