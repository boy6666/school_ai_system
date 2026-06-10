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

        <!-- 学习建议已移至学习报告页 -->
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

/** 维度详情：使用真实分数 + 对应描述文本 */
const dimLabels = ['知识掌握度', '目标清晰度', '认知适配', '错误规避', '学习自主', '综合能力']
const dimKeys = ['knowledge_mastery', 'learning_goal_clarity', 'cognitive_adaptation',
                 'mistake_avoidance', 'learning_autonomy', 'overall_level']
const dimColors = ['#409EFF', '#67C23A', '#E6A23C', '#F56C6C', '#9C27B0', '#303133']
const dimDescKeys = ['knowledge_base', 'learning_goal', 'cognitive_style', 'mistake_patterns', 'pace', 'last_score']

const dimensionDetails = computed(() =>
  dimLabels.map((label, i) => ({
    label,
    score: radarValues.value[i],
    color: dimColors[i],
    text: formatDimText(dimDescKeys[i], profileData.value),
  }))
)

function formatDimText(key: string, p: any): string {
  if (key === 'last_score') return p.last_score != null ? p.last_score + '分' : '-'
  if (key === 'mistake_patterns') return p.mistake_patterns?.join('、') || '-'
  return p[key] || '-'
}

// suggestions 已移至学习报告页

// calcRadar 已替换为 readRealScores（从 profile_data 取真实分数）

/** 从 profile_data 读取六维真实分数 */
function readRealScores(res: any): number[] {
  const pd = res?.profile_data || {}
  const keys = ['knowledge_mastery', 'learning_goal_clarity', 'cognitive_adaptation',
                'mistake_avoidance', 'learning_autonomy', 'overall_level']
  return keys.map(k => pd[k]?.score ?? 50)
}

async function loadProfile() {
  loading.value = true
  const userId = userStore.userInfo?.id

  try {
    const res = userId ? await getProfile(userId) as ProfileData : null
    if (res && res.exists) {
      profileExists.value = true
      profileData.value = res
      radarValues.value = readRealScores(res)  // 用真实分数替换启发式估算
      return
    }
  } catch { /* not found */ }
  loading.value = false
  await nextTick()
  initRadarChart()
  return
}

// 已移至学习报告页

// 已移至学习报告页

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
/* .suggestion-list 已移至学习报告页 */
.empty-hint { text-align: center; color: #909399; padding: 24px 0; }
</style>
