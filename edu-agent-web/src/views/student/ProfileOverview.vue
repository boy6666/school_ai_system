<template>
  <div class="profile-overview">
    <!-- 顶部：综合层级 + 对话统计 -->
    <div class="top-bar">
      <div class="level-badge" :class="overallClass">
        <span class="level-icon">{{ overallIcon }}</span>
        <span class="level-text">{{ overallLabel }}</span>
      </div>
      <div class="stats-row">
        <div class="stat-item">
          <span class="stat-num">{{ profileData.conversation_count || 0 }}</span>
          <span class="stat-label">累计对话</span>
        </div>
        <div class="stat-item">
          <span class="stat-num">{{ lastUpdated }}</span>
          <span class="stat-label">最近更新</span>
        </div>
      </div>
    </div>

    <el-row :gutter="20">
      <!-- 左侧：雷达图 -->
      <el-col :span="14">
        <el-card class="radar-card" shadow="never">
          <template #header>
            <span>六维学习画像</span>
            <el-tag v-if="profileExists" type="success" size="small">AI 分析</el-tag>
            <el-tag v-else type="info" size="small">等待对话生成</el-tag>
          </template>
          <div v-if="loading" class="chart-placeholder">
            <el-icon class="is-loading" :size="32"><Loading /></el-icon>
            <p>加载画像数据中...</p>
          </div>
          <div v-else ref="radarChartRef" style="height: 420px; width: 100%"></div>
        </el-card>
      </el-col>

      <!-- 右侧：维度层次详情 -->
      <el-col :span="10">
        <el-card class="dimension-card" shadow="never">
          <template #header><span>维度层次</span></template>
          <div class="dimension-list">
            <div v-for="dim in dimensionList" :key="dim.key" class="dim-item">
              <div class="dim-header">
                <span class="dim-name">{{ dim.label }}</span>
                <div class="level-indicators">
                  <span
                    v-for="lvl in [1, 2, 3]"
                    :key="lvl"
                    :class="['level-dot', { active: dim.levelNumber >= lvl }]"
                    :style="{ background: dim.levelNumber >= lvl ? dim.color : '#e4e7ed' }"
                  >
                    {{ lvl === 1 ? '入门' : lvl === 2 ? '熟练' : '精通' }}
                  </span>
                </div>
              </div>
              <el-progress
                :percentage="dim.score"
                :stroke-width="8"
                :color="dim.color"
              />
              <div class="dim-evidence" v-if="dim.evidence.length">
                <span class="evidence-label">对话证据：</span>
                <span class="evidence-text">"{{ dim.evidence.slice(-1)[0] }}"</span>
              </div>
            </div>
          </div>
        </el-card>

        <el-card class="suggestion-card" shadow="never" style="margin-top: 20px">
          <template #header>
            <span>个性化学习建议</span>
          </template>
          <ul class="suggestion-list">
            <li v-for="(s, idx) in suggestions" :key="idx">{{ s }}</li>
            <li v-if="!suggestions.length">在智能辅导中多交流，AI将自动为你生成学习建议</li>
          </ul>
        </el-card>
      </el-col>
    </el-row>

    <!-- 辅助信息 -->
    <el-card class="info-card" shadow="never" style="margin-top: 20px">
      <template #header><span>画像基本信息</span></template>
      <el-descriptions :column="3" border v-if="profileExists">
        <el-descriptions-item label="专业">{{ profileData.major || '-' }}</el-descriptions-item>
        <el-descriptions-item label="年级">{{ profileData.grade || '-' }}</el-descriptions-item>
        <el-descriptions-item label="课程">{{ profileData.course || '-' }}</el-descriptions-item>
        <el-descriptions-item label="当前知识点">{{ profileData.topic || '-' }}</el-descriptions-item>
        <el-descriptions-item label="学习节奏">{{ profileData.pace || '-' }}</el-descriptions-item>
        <el-descriptions-item label="认知风格">{{ profileData.cognitive_style || '-' }}</el-descriptions-item>
        <el-descriptions-item label="薄弱点">
          <el-tag v-for="w in profileData.weaknesses" :key="w" size="small" type="warning" style="margin-right: 4px">{{ w }}</el-tag>
          <span v-if="!profileData.weaknesses?.length">-</span>
        </el-descriptions-item>
        <el-descriptions-item label="偏好的资源">
          <el-tag v-for="r in profileData.resource_preference" :key="r" size="small" style="margin-right: 4px">{{ r }}</el-tag>
          <span v-if="!profileData.resource_preference?.length">-</span>
        </el-descriptions-item>
        <el-descriptions-item label="学习目标">{{ profileData.learning_goal || '在辅导对话中自然表达' }}</el-descriptions-item>
      </el-descriptions>
      <div v-else class="empty-hint">
        <p>尚未构建画像。去<a href="/student/tutor" style="color:#409eff">智能辅导</a>与AI对话，画像将在后台自动生成。</p>
      </div>
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted, nextTick } from 'vue'
import * as echarts from 'echarts'
import { Loading } from '@element-plus/icons-vue'
import { getProfileFromAI, type ProfileData } from '@/api/profile'
import { useUserStore } from '@/stores/user'

const userStore = useUserStore()
const radarChartRef = ref<HTMLElement>()
const loading = ref(true)
const profileExists = ref(false)
const profileData = ref<ProfileData>({})

const sixAxes = ['知识掌握度', '目标清晰度', '认知适配', '错误规避', '学习自主', '综合能力']

const dimensionKeys = [
  'knowledge_mastery',
  'learning_goal_clarity',
  'cognitive_adaptation',
  'mistake_avoidance',
  'learning_autonomy',
  'overall_level',
] as const

const dimensionColors = ['#409EFF', '#67C23A', '#E6A23C', '#F56C6C', '#9C27B0', '#FF5722']

const dimLabels: Record<string, string> = {
  knowledge_mastery: '知识掌握度',
  learning_goal_clarity: '学习目标清晰度',
  cognitive_adaptation: '认知风格适配',
  mistake_avoidance: '错误规避力',
  learning_autonomy: '学习自主性',
  overall_level: '综合能力',
}

const dimensionList = computed(() =>
  dimensionKeys.map((key, i) => {
    const dim = (profileData.value as any)[key] || { level: 'level_1', level_label: '入门', level_number: 1, score: 30, evidence: [] }
    return {
      key,
      label: dimLabels[key] || key,
      level: dim.level || 'level_1',
      levelLabel: dim.level_label || '入门',
      levelNumber: dim.level_number || 1,
      score: dim.score || 30,
      evidence: dim.evidence || [],
      color: dimensionColors[i],
    }
  })
)

const radarValues = computed(() => dimensionList.value.map(d => d.score))

const overallDim = computed(() => {
  const dim = (profileData.value as any)['overall_level']
  return dim || { level_label: '入门', level_number: 1 }
})

const overallLabel = computed(() => overallDim.value.level_label || '入门')
const overallClass = computed(() => {
  const lvl = overallDim.value.level_number || 1
  if (lvl === 3) return 'level-advanced'
  if (lvl === 2) return 'level-stable'
  return 'level-basic'
})
const overallIcon = computed(() => {
  const lvl = overallDim.value.level_number || 1
  if (lvl === 3) return '🚀'
  if (lvl === 2) return '📈'
  return '🌱'
})

const lastUpdated = computed(() => {
  const t = profileData.value.last_updated
  if (!t) return '-'
  return t.slice(0, 10)
})

const suggestions = computed(() => profileData.value.profile_suggestions || [])

async function loadProfile() {
  loading.value = true
  const username = userStore.userInfo?.username || 'student001'

  try {
    const aiRes = await getProfileFromAI(username)
    if (aiRes.exists && aiRes.profile) {
      profileExists.value = true
      profileData.value = aiRes.profile
      return
    }
  } catch {
    // use empty state
  }

  loading.value = false
  await nextTick()
  initRadarChart()
}

function initRadarChart() {
  if (!radarChartRef.value) return
  const chart = echarts.init(radarChartRef.value)
  const option = {
    radar: {
      indicator: sixAxes.map(name => ({ name, max: 100 })),
      shape: 'polygon',
      center: ['50%', '50%'],
      radius: '65%',
      name: { textStyle: { fontSize: 13, color: '#303133' } },
    },
    series: [{
      type: 'radar',
      data: [{
        value: radarValues.value,
        name: '当前水平',
        areaStyle: { color: 'rgba(64, 158, 255, 0.2)' },
        lineStyle: { color: '#409EFF', width: 2 },
        itemStyle: { color: '#409EFF' },
      }],
    }],
  }
  chart.setOption(option)
  window.addEventListener('resize', () => chart.resize())
}

let chartInited = false
onMounted(async () => {
  await loadProfile()
  loading.value = false
  await nextTick()
  initRadarChart()
})
</script>

<style scoped>
.profile-overview { padding: 20px; background-color: #f5f7fa; min-height: 100vh; }

.top-bar {
  display: flex; align-items: center; gap: 24px;
  background: #fff; border-radius: 16px; padding: 16px 24px;
  margin-bottom: 20px; box-shadow: 0 1px 3px rgba(0,0,0,0.05);
}
.level-badge {
  display: flex; align-items: center; gap: 8px;
  padding: 8px 20px; border-radius: 24px; color: #fff; font-weight: 700; font-size: 18px;
}
.level-basic { background: linear-gradient(135deg, #E6A23C, #F39C12); }
.level-stable { background: linear-gradient(135deg, #409EFF, #337ECC); }
.level-advanced { background: linear-gradient(135deg, #67C23A, #45A834); }
.stats-row { display: flex; gap: 32px; margin-left: auto; }
.stat-item { text-align: center; }
.stat-num { display: block; font-size: 20px; font-weight: 700; color: #303133; }
.stat-label { font-size: 12px; color: #909399; }

.radar-card, .dimension-card, .suggestion-card, .info-card { border-radius: 16px; }
.chart-placeholder { height: 420px; display: flex; flex-direction: column; align-items: center; justify-content: center; color: #909399; }
.chart-placeholder p { margin-top: 12px; }

.dimension-list { display: flex; flex-direction: column; gap: 18px; }
.dim-item { padding-bottom: 12px; border-bottom: 1px solid #f0f0f0; }
.dim-item:last-child { border-bottom: none; }
.dim-header { display: flex; justify-content: space-between; align-items: center; margin-bottom: 8px; }
.dim-name { font-weight: 600; font-size: 14px; color: #303133; }
.level-indicators { display: flex; gap: 4px; }
.level-dot {
  padding: 2px 8px; border-radius: 10px; font-size: 11px;
  color: #c0c4cc; background: #e4e7ed; transition: all 0.3s;
}
.level-dot.active { color: #fff; font-weight: 600; }
.dim-evidence { margin-top: 6px; font-size: 12px; }
.evidence-label { color: #909399; }
.evidence-text { color: #409eff; font-style: italic; }

.suggestion-list { padding-left: 20px; margin: 0; }
.suggestion-list li { margin-bottom: 10px; line-height: 1.5; color: #4a5568; }
.empty-hint { text-align: center; color: #909399; padding: 24px 0; }
</style>
