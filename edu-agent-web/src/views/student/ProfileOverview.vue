<template>
  <div class="profile-overview">
    <!-- 综合类型标签 -->
    <div class="type-banner" v-if="profileExists && profileData.overall_type">
      <div class="type-tag" :class="typeClass">{{ profileData.overall_type }}</div>
      <span class="type-desc">{{ typeDescription }}</span>
    </div>

    <el-row :gutter="20">
      <el-col :span="14">
        <el-card class="radar-card" shadow="never">
          <template #header>
            <span>六维学习画像</span>
            <el-tag v-if="profileExists" type="success" size="small">AI 分析</el-tag>
            <el-tag v-else type="info" size="small">示例数据</el-tag>
          </template>
          <div v-if="loading" class="chart-placeholder">
            <el-icon class="is-loading" :size="32"><Loading /></el-icon>
            <p>加载画像数据中...</p>
          </div>
          <div v-else ref="radarChartRef" style="height: 420px; width: 100%"></div>
        </el-card>
      </el-col>

      <el-col :span="10">
        <el-card class="dimension-card" shadow="never">
          <template #header><span>维度详情</span></template>
          <div class="dimension-details">
            <div class="dim-row" v-for="dim in dimensionDetails" :key="dim.label">
              <div class="dim-name">{{ dim.label }}</div>
              <el-progress :percentage="dim.score" :stroke-width="8" :color="dim.color" />
              <div class="dim-text">{{ dim.text }}</div>
            </div>
          </div>
        </el-card>

        <el-card class="suggestion-card" shadow="never" style="margin-top: 20px">
          <template #header>
            <span>个性化学习建议</span>
            <el-button type="text" style="float: right" @click="refreshSuggestions">刷新</el-button>
          </template>
          <ul class="suggestion-list">
            <li v-for="(s, idx) in suggestions" :key="idx">{{ s }}</li>
          </ul>
        </el-card>
      </el-col>
    </el-row>

    <!-- 补充信息 -->
    <el-card class="info-card" shadow="never" style="margin-top: 20px">
      <template #header><span>画像基本信息</span></template>
      <el-descriptions :column="3" border v-if="profileExists">
        <el-descriptions-item label="专业">{{ profileData.major || '-' }}</el-descriptions-item>
        <el-descriptions-item label="年级">{{ profileData.grade || '-' }}</el-descriptions-item>
        <el-descriptions-item label="课程">{{ profileData.course || '-' }}</el-descriptions-item>
        <el-descriptions-item label="当前知识点">{{ profileData.topic || '-' }}</el-descriptions-item>
        <el-descriptions-item label="学习节奏">{{ profileData.pace || '-' }}</el-descriptions-item>
        <el-descriptions-item label="最近评估分">{{ profileData.last_score || '-' }}</el-descriptions-item>
        <el-descriptions-item label="综合类型">
          <el-tag :type="typeTagType" size="small">{{ profileData.overall_type || '-' }}</el-tag>
        </el-descriptions-item>
        <el-descriptions-item label="最近更新">{{ profileData.last_updated || '-' }}</el-descriptions-item>
      </el-descriptions>
      <div v-else class="empty-hint">尚未构建画像，请先在"学习画像"中完成对话</div>
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted, nextTick } from 'vue'
import * as echarts from 'echarts'
import { Loading } from '@element-plus/icons-vue'
import { getProfile, type ProfileData } from '@/api/profile'
import { useUserStore } from '@/stores/user'
import request from '@/utils/request'

const userStore = useUserStore()
const radarChartRef = ref<HTMLElement>()
const loading = ref(true)
const profileExists = ref(false)
const profileData = ref<ProfileData>({})

const sixAxes = ['基础扎实度', '目标明确度', '知识掌握度', '方法适配度', '错误规避力', '学习自主性']
const radarValues = ref<number[]>([50, 50, 50, 50, 50, 50])

const typeClass = computed(() => {
  if (profileData.value.overall_type === '进阶拓展型') return 'type-advanced'
  if (profileData.value.overall_type === '稳定提升型') return 'type-stable'
  return 'type-basic'
})

const typeTagType = computed(() => {
  if (profileData.value.overall_type === '进阶拓展型') return 'success'
  if (profileData.value.overall_type === '稳定提升型') return ''
  return 'warning'
})

const typeDescription = computed(() => {
  if (profileData.value.overall_type === '进阶拓展型') return '基础扎实、自主性强，适合挑战高级内容和项目实战'
  if (profileData.value.overall_type === '稳定提升型') return '有一定基础，按部就班提升中，需要结构化学习路径'
  return '基础薄弱，需要系统性地补充核心知识和加强练习'
})

const dimensionDetails = computed(() => [
  { label: '知识基础', score: radarValues.value[0], color: '#409EFF', text: profileData.value.knowledge_base || '-' },
  { label: '学习目标', score: radarValues.value[1], color: '#67C23A', text: profileData.value.learning_goal || '-' },
  { label: '当前掌握度', score: radarValues.value[2], color: '#E6A23C', text: profileData.value.current_mastery || '-' },
  { label: '认知风格', score: radarValues.value[3], color: '#F56C6C', text: profileData.value.cognitive_style || '-' },
  { label: '易错点类型', score: radarValues.value[4], color: '#909399', text: profileData.value.mistake_patterns?.join('、') || '-' },
  { label: '学习行为', score: radarValues.value[5], color: '#9C27B0', text: profileData.value.learning_behavior || '-' },
])

const suggestions = ref<string[]>([])

function calcRadar(p: ProfileData): number[] {
  const scores = [50, 50, 50, 50, 50, 50]
  if (p.knowledge_base && p.knowledge_base.length > 10) scores[0] = 65
  if (p.learning_goal && p.learning_goal.length > 5) scores[1] = 70
  if (p.current_mastery) {
    const m = p.current_mastery.match(/(\d+)\s*分/)
    scores[2] = m ? parseInt(m[1]) : 60
  }
  if (p.cognitive_style && p.cognitive_style.length > 2) scores[3] = 65
  if (p.mistake_patterns?.length) scores[4] = 40 + p.mistake_patterns.length * 10
  if (p.learning_behavior && p.learning_behavior.length > 5) scores[5] = 65
  return scores.map(s => Math.min(100, s))
}

async function loadProfile() {
  loading.value = true
  const userId = userStore.userInfo?.id

  try {
    const res = userId ? await getProfile(userId) as ProfileData : null
    if (res && res.exists) {
      profileExists.value = true
      profileData.value = res
      radarValues.value = calcRadar(res)
      suggestions.value = res.profile_suggestions?.length ? res.profile_suggestions : buildDefaultSuggestions()
      return
    }
  } catch { /* not found */ }

  suggestions.value = buildDefaultSuggestions()
  loading.value = false
  await nextTick()
  initRadarChart()
  return
}

function buildDefaultSuggestions() {
  return [
    '从Java基础语法开始，每天坚持30分钟代码练习',
    '使用思维导图整理面向对象三大特性',
    '通过实战项目理解集合框架和多线程',
    '阅读《深入理解Java虚拟机》补充JVM知识',
  ]
}

async function refreshSuggestions() {
  const userId = userStore.userInfo?.id
  if (!userId) return
  try {
    const res: any = await request.post('/profile/generate-suggestions', { userId })
    if (res?.suggestions?.length) {
      suggestions.value = res.suggestions
      console.log('[画像] ✅ AI 建议已刷新:', res.suggestions)
    }
  } catch (e) {
    console.warn('[画像] ⚠️ AI 建议生成失败，使用本地:', e)
    suggestions.value = [
      '针对薄弱知识点，每天做3道相关编程练习',
      '结合视频教程和官方文档构建知识体系',
      '参与开源项目，在实际场景中巩固所学',
      '使用AI智能体进行自适应题目练习和纠错',
    ]
  }
}

function initRadarChart() {
  if (!radarChartRef.value) return
  const chart = echarts.init(radarChartRef.value)
  const option = {
    radar: {
      indicator: sixAxes.map(name => ({ name, max: 100 })),
      shape: 'circle',
      center: ['50%', '50%'],
      radius: '65%',
      name: { textStyle: { fontSize: 14, color: '#303133' } },
    },
    series: [{
      type: 'radar',
      data: [{
        value: radarValues.value,
        name: profileExists.value ? '当前水平' : '示例',
        areaStyle: { color: 'rgba(64, 158, 255, 0.2)' },
        lineStyle: { color: '#409EFF', width: 2 },
        itemStyle: { color: '#409EFF' },
      }],
    }],
  }
  chart.setOption(option)
  window.addEventListener('resize', () => chart.resize())
}

onMounted(async () => {
  await loadProfile()
  loading.value = false
  await nextTick()
  initRadarChart()
})
</script>

<style scoped>
.profile-overview { padding: 20px; background-color: #f5f7fa; min-height: 100vh; }
.type-banner { display: flex; align-items: center; gap: 16px; background: #fff; border-radius: 16px; padding: 16px 24px; margin-bottom: 20px; box-shadow: 0 1px 3px rgba(0,0,0,0.05); }
.type-tag { font-size: 20px; font-weight: 700; padding: 6px 20px; border-radius: 24px; color: #fff; }
.type-basic { background: linear-gradient(135deg, #E6A23C, #F39C12); }
.type-stable { background: linear-gradient(135deg, #409EFF, #337ECC); }
.type-advanced { background: linear-gradient(135deg, #67C23A, #45A834); }
.type-desc { font-size: 14px; color: #606266; }
.radar-card, .dimension-card, .suggestion-card, .info-card { border-radius: 16px; }
.chart-placeholder { height: 420px; display: flex; flex-direction: column; align-items: center; justify-content: center; color: #909399; }
.chart-placeholder p { margin-top: 12px; }
.dimension-details { display: flex; flex-direction: column; gap: 16px; }
.dim-row { display: flex; flex-direction: column; gap: 4px; }
.dim-name { font-weight: 600; font-size: 13px; color: #303133; }
.dim-text { font-size: 12px; color: #909399; margin-top: 2px; }
.suggestion-list { padding-left: 20px; margin: 0; }
.suggestion-list li { margin-bottom: 12px; line-height: 1.5; color: #4a5568; }
.empty-hint { text-align: center; color: #909399; padding: 24px 0; }
</style>
