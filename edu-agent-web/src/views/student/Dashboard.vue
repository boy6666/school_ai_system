<template>
  <div class="dashboard-container">
    <div class="dashboard-header">
      <div class="welcome-section">
        <h2>{{ greeting }}，{{ userName || '同学' }}</h2>
        <p>今天也要继续努力的学习哦！</p>
      </div>
    </div>

    <el-row :gutter="20">
      <el-col :span="12">
        <!-- 今日任务 -->
        <el-card class="card" shadow="never">
          <template #header><span>📋 今日任务</span></template>
          <div v-if="tasks.length" v-for="t in tasks" :key="t.id" class="item">
            <span class="item-name">{{ t.title }}</span>
            <span class="item-extra">{{ t.priority === 'high' ? '重要' : '' }}</span>
          </div>
          <div v-else class="empty">暂无任务</div>
        </el-card>

        <!-- 学习回顾 -->
        <el-card class="card" shadow="never" style="margin-top:16px">
          <template #header>
            <span>📚 学习回顾</span>
            <el-button type="text" style="float:right" @click="generateReview" :loading="reviewLoading" v-if="!reviewLoading">AI 生成</el-button>
          </template>
          <div v-if="reviewResult" class="ai-summary-box">
            <div class="ai-summary-text">{{ reviewResult.feedback || reviewResult.summary }}</div>
            <div v-if="reviewResult.focusNext" class="ai-summary-item">🎯 重点：{{ reviewResult.focusNext }}</div>
          </div>
          <div v-else>
            <div v-if="reviewList.length" v-for="r in reviewList" :key="r.module" class="item">
              <span>{{ labelMap[r.module] || r.module }}</span>
              <span>{{ Math.round(r.total / 60) }} 分钟</span>
            </div>
            <div v-else class="empty">暂无学习记录</div>
          </div>
        </el-card>

        <!-- 学习目标 -->
        <el-card class="card" shadow="never" style="margin-top:16px">
          <template #header><span>🎯 学习目标</span></template>
          <div class="two-col">
            <div class="tc"><div class="tv">{{ totalHours }}</div><div class="tl">总学习时长(小时)</div></div>
            <div class="tc"><div class="tv">{{ totalScore || '-' }}</div><div class="tl">综合评分</div></div>
          </div>
        </el-card>
      </el-col>

      <el-col :span="12">
        <!-- 学习总结 -->
        <el-card class="card" shadow="never">
          <template #header>
            <span>📝 学习总结</span>
            <el-button type="text" style="float:right" @click="generateAiSummary" :loading="aiLoading">AI 生成</el-button>
          </template>
          <div v-if="aiSummary" class="ai-summary-box">
            <div class="ai-summary-text">{{ aiSummary.summary }}</div>
            <div v-if="aiSummary.strengths" class="ai-summary-item"><span class="ai-label">👍 优点：</span>{{ aiSummary.strengths }}</div>
            <div v-if="aiSummary.weaknessAnalysis" class="ai-summary-item"><span class="ai-label">📉 不足：</span>{{ aiSummary.weaknessAnalysis }}</div>
            <div v-if="aiSummary.suggestion" class="ai-summary-item"><span class="ai-label">💡 建议：</span>{{ aiSummary.suggestion }}</div>
            <div v-if="aiSummary.focusNext" class="ai-summary-item"><span class="ai-label">🎯 重点：</span>{{ aiSummary.focusNext }}</div>
            <!-- 综合评分已移除 -->
          </div>
          <div v-else class="grid-2">
            <div v-for="s in summaryDims" :key="s.label" class="gi">
              <div class="gl">{{ s.label }}</div>
              <el-progress :percentage="s.score" :stroke-width="8" />
            </div>
          </div>
        </el-card>

        <!-- 学习评价 -->
        <el-card class="card" shadow="never" style="margin-top:16px">
          <template #header><span>⭐ 学习评价</span></template>
          <div v-if="evalItems.length" v-for="e in evalItems" :key="e.label" class="eval-row">
            <span class="eval-l">{{ e.label }}：</span><span>{{ e.value }}</span>
          </div>
          <div v-else class="empty">暂无评价</div>
        </el-card>

        <!-- 学习节奏 -->
        <el-card class="card" shadow="never" style="margin-top:16px">
          <template #header><span>⏱️ 学习节奏</span></template>
          <div class="two-col">
            <div class="tc"><div class="tv o">{{ evalData?.pace || profile.pace || '待定' }}</div><div class="tl">学习节奏</div></div>
            <div class="tc"><div class="tv o">{{ evalData?.course || profile.course || '待定' }}</div><div class="tl">当前课程</div></div>
          </div>
        </el-card>

        <!-- 学习计划 -->
        <el-card class="card" shadow="never" style="margin-top:16px">
          <template #header><span>📅 学习计划</span></template>
          <div class="two-col">
            <div class="tc"><div class="tv o">{{ pathData?.goal || profile.learning_goal || '待定' }}</div><div class="tl">学习目标</div></div>
            <div class="tc"><div class="tv o">{{ profile.topic || '待定' }}</div><div class="tl">当前主题</div></div>
          </div>
        </el-card>
      </el-col>
    </el-row>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { getProfile, type ProfileData } from '@/api/profile'
import {
  generateDashboardAiSummary,
  generateDashboardLearningReview,
  getDashboardAiSummary,
  getDashboardEvaluation,
  getDashboardLearningReview,
  getDashboardPath,
  getDashboardSummary,
  getDashboardTasks,
  type DashboardAiSummary,
  type DashboardEvaluation,
  type DashboardTask,
  type DashboardReviewItem,
  type LearningPathData
} from '@/api/learning'
import { useUserStore } from '@/stores/user'

const userStore = useUserStore()
const userName = computed(() => userStore.userInfo?.realName || userStore.userInfo?.username || '')

const greeting = computed(() => {
  const h = new Date().getHours()
  return h < 12 ? '早上好' : h < 18 ? '下午好' : '晚上好'
})

const labelMap: Record<string, string> = { mindmap: '思维导图', quiz: '练习题目', reading: '拓展阅读', code: '代码案例' }

// AI 学习总结
const aiLoading = ref(false)
const aiSummary = ref<DashboardAiSummary | null>(null)

async function generateAiSummary() {
  aiLoading.value = true
  try {
    aiSummary.value = await generateDashboardAiSummary()
  } catch {
    ElMessage.error('AI 学习总结生成失败，请稍后重试')
  } finally {
    aiLoading.value = false
  }
}

// API 数据
const tasks = ref<DashboardTask[]>([])
const reviewList = ref<DashboardReviewItem[]>([])
const totalHours = ref(0)
const totalScore = ref<number | null>(null)
const profile = ref<ProfileData>({})
const pathData = ref<LearningPathData>({})
const evalData = ref<DashboardEvaluation | null>(null)

const summaryDims = computed(() => {
  const d = evalData.value
  if (!d) return []
  const items = [
    { label: '知识掌握度', key: 'knowledge_mastery' },
    { label: '学习自主性', key: 'learning_autonomy' },
    { label: '目标清晰度', key: 'learning_goal_clarity' },
    { label: '错误规避力', key: 'mistake_avoidance' },
    { label: '综合能力', key: 'overall_level' },
  ]
  return items.flatMap((item) => {
    const dimension = d[item.key]
    if (!dimension || typeof dimension !== 'object' || !('score' in dimension)) return []
    const score = (dimension as { score?: unknown }).score
    return typeof score === 'number' ? [{ label: item.label, score }] : []
  })
})

// AI 学习回顾
const reviewLoading = ref(false)
const reviewResult = ref<DashboardAiSummary | null>(null)
async function generateReview() {
  reviewLoading.value = true
  try {
    reviewResult.value = await generateDashboardLearningReview()
  } catch {
    ElMessage.error('AI 学习回顾生成失败，请稍后重试')
  } finally {
    reviewLoading.value = false
  }
}

const evalItems = computed(() => {
  const p = profile.value
  const e = evalData.value
  const items: { label: string; value: string }[] = []
  const goal = e?.learning_goal || p.learning_goal
  const style = e?.cognitive_style || p.cognitive_style
  const course = e?.course || p.course
  const wk = e?.weaknesses || p.weaknesses
  if (goal) items.push({ label: '学习目标', value: goal })
  if (style) items.push({ label: '认知风格', value: style })
  if (course) items.push({ label: '当前课程', value: course })
  if (wk) {
    const w = Array.isArray(wk) ? wk.join('、') : String(wk)
    if (w && w !== '[]') items.push({ label: '薄弱环节', value: w })
  }
  return items
})

onMounted(async () => {
  const userId = userStore.userInfo?.id ?? userStore.userInfo?.userId

  // Step 1: 并行加载 DB 数据
  const [summaryRes, tasksRes, pathRes, profileRes] = await Promise.allSettled([
    getDashboardSummary(),
    getDashboardTasks(),
    getDashboardPath(),
    userId ? getProfile(userId) : Promise.resolve(null),
  ])

  if (summaryRes.status === 'fulfilled' && summaryRes.value) {
    const s = summaryRes.value
    totalHours.value = Math.round((s.totalSec || 0) / 3600 * 10) / 10
    reviewList.value = s.today || []
  }

  if (pathRes.status === 'fulfilled' && pathRes.value) {
    pathData.value = pathRes.value
    if (pathData.value?.progress !== undefined) totalScore.value = pathData.value.progress
  }

  if (tasksRes.status === 'fulfilled' && tasksRes.value) {
    tasks.value = tasksRes.value.filter((task) => task.status !== 'done')
  }

  if (profileRes.status === 'fulfilled') {
    const p = profileRes.value
    if (p && p.exists !== false) {
      profile.value = p
      totalScore.value = p.last_score ?? null
    }
  }

  // Step 2: 从 DB 读取已有评价、AI 总结和学习回顾
  const [evaluationRes, aiSummaryRes, learningReviewRes] = await Promise.allSettled([
    getDashboardEvaluation(),
    getDashboardAiSummary(),
    getDashboardLearningReview()
  ])
  if (evaluationRes.status === 'fulfilled' && Object.keys(evaluationRes.value).length) {
    evalData.value = evaluationRes.value
  }
  if (aiSummaryRes.status === 'fulfilled' && aiSummaryRes.value.summary) {
    aiSummary.value = aiSummaryRes.value
  }
  if (learningReviewRes.status === 'fulfilled' && learningReviewRes.value.summary) {
    reviewResult.value = learningReviewRes.value
  }

  const primaryRequests = [summaryRes, tasksRes, pathRes, profileRes]
  if (primaryRequests.some((result) => result.status === 'rejected')) {
    ElMessage.warning('部分首页数据加载失败，请稍后刷新重试')
  }
  const supplementalRequests = [evaluationRes, aiSummaryRes, learningReviewRes]
  if (supplementalRequests.some((result) => result.status === 'rejected')) {
    ElMessage.warning('部分学习评价与总结加载失败，请稍后重试')
  }
})
</script>

<style scoped>
.dashboard-container { padding: 20px; background: #f5f7fa; min-height: 100vh; }
.dashboard-header { display: flex; justify-content: space-between; align-items: center; margin-bottom: 20px; background: #fff; padding: 16px 24px; border-radius: 16px; }
.welcome-section h2 { margin: 0 0 6px; font-size: 22px; }
.welcome-section p { margin: 0; color: #666; }
.card { border-radius: 16px; }
.item { display: flex; justify-content: space-between; padding: 10px 0; border-bottom: 1px solid #f0f0f0; }
.item:last-child { border-bottom: none; }
.item-name { font-weight: 500; }
.item-extra { color: #e6a23c; font-size: 12px; }
.empty { color: #c0c4cc; text-align: center; padding: 20px 0; font-size: 14px; }
.two-col { display: flex; justify-content: space-around; text-align: center; }
.tv { font-size: 26px; font-weight: bold; color: #409eff; }
.tv.o { color: #e6a23c; font-size: 22px; }
.tl { font-size: 13px; color: #909399; margin-top: 4px; }
.grid-2 { display: flex; flex-wrap: wrap; gap: 14px; }
.gi { width: calc(50% - 7px); }
.gl { margin-bottom: 4px; font-size: 14px; }
.eval-row { font-size: 14px; padding: 4px 0; }
.eval-l { font-weight: 600; color: #606266; width: 90px; display: inline-block; }

/* AI 学习总结 */
.ai-summary-box { padding: 4px 0; }
.ai-summary-text { font-size: 14px; line-height: 1.7; color: #303133; margin-bottom: 12px; padding: 12px; background: #f0f9ff; border-radius: 8px; }
.ai-summary-item { font-size: 13px; line-height: 1.6; color: #606266; margin-bottom: 8px; }
.ai-label { font-weight: 600; color: #303133; }
/* .ai-summary-score 已移除 */
</style>