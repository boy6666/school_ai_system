<template>
  <div class="analytics-page">
    <header class="page-header">
      <div>
        <p class="eyebrow">LEARNING ANALYTICS</p>
        <h1>学情看板</h1>
        <p class="description">
          查看班级掌握度、任务完成情况、薄弱知识点和学习趋势。
        </p>
      </div>

      <div class="header-actions">
        <el-select
          v-model="selectedClassId"
          placeholder="请选择班级"
          filterable
          :loading="loadingClasses"
          class="class-select"
          @change="handleClassChange"
        >
          <el-option
            v-for="item in classes"
            :key="item.id"
            :label="item.name"
            :value="item.id"
          />
        </el-select>

        <el-button
          :loading="refreshing"
          :disabled="!selectedClassId"
          @click="loadDashboard"
        >
          刷新
        </el-button>
      </div>
    </header>

    <el-skeleton
      v-if="loadingClasses"
      :rows="8"
      animated
    />
    <section
      v-else-if="classError"
      class="state-panel"
    >
      <el-result
        icon="warning"
        title="班级列表加载失败"
        :sub-title="classError"
      >
        <template #extra>
          <el-button
            type="primary"
            @click="loadClasses"
          >
            重新加载
          </el-button>
        </template>
      </el-result>
    </section>
    <el-empty
      v-else-if="classes.length === 0"
      description="暂无可查看的班级"
    />

    <section
      v-else-if="!selectedClassId"
      class="state-panel"
    >
      <el-empty description="请选择班级查看学情数据" />
    </section>

    <section
      v-else-if="errorMessage"
      class="state-panel"
    >
      <el-result
        icon="warning"
        title="学情数据加载失败"
        :sub-title="errorMessage"
      >
        <template #extra>
          <el-button
            type="primary"
            @click="loadDashboard"
          >
            重新加载
          </el-button>
        </template>
      </el-result>
    </section>

    <el-skeleton
      v-else-if="refreshing"
      :rows="10"
      animated
    />

    <template v-else-if="overview && analytics">
      <section class="overview-grid">
        <article class="metric-card">
          <span class="metric-label">班级学生</span>
          <strong class="metric-value">
            {{ overview.studentCount }}
          </strong>
          <span class="metric-unit">人</span>
        </article>

        <article class="metric-card">
          <span class="metric-label">平均掌握度</span>
          <strong class="metric-value">
            {{ formatNumber(overview.avgMastery) }}
          </strong>
        </article>

        <article class="metric-card">
          <span class="metric-label">任务完成率</span>
          <strong class="metric-value">
            {{ formatNumber(overview.completionRate) }}
          </strong>
          <span class="metric-unit">%</span>
        </article>

        <article class="metric-card">
          <span class="metric-label">活跃学生</span>
          <strong class="metric-value">
            {{ overview.activeStudents }}
          </strong>
          <span class="metric-unit">人</span>
        </article>
      </section>

      <section class="chart-grid">
        <article class="panel">
          <div class="panel-heading">
            <div>
              <h2>掌握度分布</h2>
              <p>不同掌握等级的学生人数。</p>
            </div>
          </div>

          <BaseChart
            v-if="analytics.masteryDist.length"
            :option="masteryOption"
            height="320px"
          />

          <el-empty
            v-else
            description="暂无掌握度分布"
          />
        </article>

        <article class="panel">
          <div class="panel-heading">
            <div>
              <h2>能力维度均值</h2>
              <p>班级各学习维度的平均表现。</p>
            </div>
          </div>

          <BaseChart
            v-if="dimensionEntries.length"
            :option="dimensionOption"
            height="320px"
          />

          <el-empty
            v-else
            description="暂无维度数据"
          />
        </article>
      </section>

      <section class="panel trend-panel">
        <div class="panel-heading">
          <div>
            <h2>学习活跃趋势</h2>
            <p>按日期统计班级活跃学生人数。</p>
          </div>
        </div>

        <BaseChart
          v-if="analytics.trend.length"
          :option="trendOption"
          height="340px"
        />

        <el-empty
          v-else
          description="暂无趋势数据"
        />
      </section>

      <section class="detail-grid">
        <article class="panel">
          <div class="panel-heading">
            <div>
              <h2>任务完成情况</h2>
              <p>查看学生当前进度和最近成绩。</p>
            </div>
          </div>

          <el-table
            v-if="analytics.taskCompletion.length"
            :data="analytics.taskCompletion"
            stripe
          >
            <el-table-column
              prop="studentId"
              label="学生 ID"
              min-width="110"
            />
            <el-table-column
              prop="name"
              label="姓名"
              min-width="120"
            />
            <el-table-column
              label="完成进度"
              min-width="180"
            >
              <template #default="{ row }">
                <el-progress
                  :percentage="normalizeProgress(row.progress)"
                />
              </template>
            </el-table-column>
            <el-table-column
              label="最近成绩"
              min-width="110"
            >
              <template #default="{ row }">
                {{
                  row.lastScore === null
                    ? '暂无'
                    : row.lastScore
                }}
              </template>
            </el-table-column>
          </el-table>

          <el-empty
            v-else
            description="暂无任务完成数据"
          />
        </article>

        <article class="panel">
          <div class="panel-heading">
            <div>
              <h2>薄弱知识点</h2>
              <p>按受影响学生数量查看薄弱主题。</p>
            </div>
          </div>

          <div
            v-if="analytics.weakTopics.length"
            class="topic-list"
          >
            <div
              v-for="item in analytics.weakTopics"
              :key="item.topic"
              class="topic-item"
            >
              <span>{{ item.topic }}</span>
              <el-tag type="warning">
                {{ item.count }} 人
              </el-tag>
            </div>
          </div>

          <el-empty
            v-else
            description="暂无薄弱知识点"
          />
        </article>
      </section>
    </template>
  </div>
</template>
<script setup lang="ts">
import {
  computed,
  onMounted,
  ref
} from 'vue'
import type { EChartsOption } from 'echarts'
import BaseChart from '@/components/BaseChart.vue'
import {
  getClassAnalytics,
  getClassOverview,
  getTeacherClasses
} from '@/api/teacher'
import type {
  ClassAnalytics,
  ClassOverview,
  TeacherClass
} from '@/api/teacher'

const classes = ref<TeacherClass[]>([])
const selectedClassId = ref<number>()
const overview = ref<ClassOverview>()
const analytics = ref<ClassAnalytics>()

const loadingClasses = ref(false)
const refreshing = ref(false)
const classError = ref('')
const errorMessage = ref('')

const dimensionEntries = computed(() =>
  Object.entries(analytics.value?.dimensionAvg ?? {})
)

const masteryOption = computed<EChartsOption>(() => ({
  tooltip: {
    trigger: 'item'
  },
  legend: {
    bottom: 0
  },
  color: [
    '#5b45dc',
    '#7d6ce8',
    '#a798f0',
    '#d0c7f8'
  ],
  series: [
    {
      name: '学生人数',
      type: 'pie',
      radius: ['42%', '68%'],
      center: ['50%', '44%'],
      data: (analytics.value?.masteryDist ?? []).map(
        item => ({
          name: item.level,
          value: item.count
        })
      ),
      label: {
        formatter: '{b}: {c}'
      }
    }
  ]
}))

const dimensionOption = computed<EChartsOption>(() => ({
  tooltip: {
    trigger: 'axis',
    axisPointer: {
      type: 'shadow'
    }
  },
  grid: {
    left: 20,
    right: 24,
    top: 20,
    bottom: 20,
    containLabel: true
  },
  xAxis: {
    type: 'value'
  },
  yAxis: {
    type: 'category',
    data: dimensionEntries.value.map(
      ([name]) => name
    )
  },
  series: [
    {
      name: '平均值',
      type: 'bar',
      data: dimensionEntries.value.map(
        ([, value]) => value
      ),
      itemStyle: {
        color: '#5b45dc',
        borderRadius: [0, 6, 6, 0]
      }
    }
  ]
}))

const trendOption = computed<EChartsOption>(() => ({
  tooltip: {
    trigger: 'axis'
  },
  grid: {
    left: 20,
    right: 24,
    top: 30,
    bottom: 20,
    containLabel: true
  },
  xAxis: {
    type: 'category',
    boundaryGap: false,
    data: (analytics.value?.trend ?? []).map(
      item => item.day
    )
  },
  yAxis: {
    type: 'value',
    minInterval: 1
  },
  series: [
    {
      name: '活跃学生',
      type: 'line',
      smooth: true,
      data: (analytics.value?.trend ?? []).map(
        item => item.activeStudents
      ),
      symbolSize: 8,
      lineStyle: {
        width: 3,
        color: '#5b45dc'
      },
      itemStyle: {
        color: '#5b45dc'
      },
      areaStyle: {
        color: 'rgba(91, 69, 220, 0.12)'
      }
    }
  ]
}))

function formatNumber(value: number) {
  if (!Number.isFinite(value)) return '-'

  return Number.isInteger(value)
    ? String(value)
    : value.toFixed(1)
}

function normalizeProgress(value: number) {
  if (!Number.isFinite(value)) return 0

  return Math.min(100, Math.max(0, value))
}

function getErrorMessage(
  error: unknown,
  fallback: string
) {
  if (
    error instanceof Error &&
    error.message.trim()
  ) {
    return error.message
  }

  return fallback
}

async function loadClasses() {
  loadingClasses.value = true
  classError.value = ''

  try {
    classes.value = await getTeacherClasses()

    if (classes.value.length > 0) {
      selectedClassId.value = classes.value[0]?.id
      await loadDashboard()
    }
  } catch (error) {
    classes.value = []
    classError.value = getErrorMessage(
      error,
      '班级列表加载失败'
    )
  } finally {
    loadingClasses.value = false
  }
}

async function loadDashboard() {
  if (!selectedClassId.value) return

  refreshing.value = true
  errorMessage.value = ''
  overview.value = undefined
  analytics.value = undefined

  try {
    const [overviewResult, analyticsResult] =
      await Promise.all([
        getClassOverview(selectedClassId.value),
        getClassAnalytics(selectedClassId.value)
      ])

    overview.value = overviewResult
    analytics.value = analyticsResult
  } catch (error) {
    errorMessage.value = getErrorMessage(
      error,
      '请检查教师服务或网关是否可用'
    )
  } finally {
    refreshing.value = false
  }
}

function handleClassChange() {
  loadDashboard()
}

onMounted(() => {
  loadClasses()
})
</script>
<style scoped>
.analytics-page {
  min-height: 100%;
  padding: 36px 48px 56px;
  background: var(--canvas);
}

.page-header {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: var(--space-lg);
  margin-bottom: 28px;
}

.eyebrow {
  margin: 0 0 8px;
  color: var(--primary);
  font-size: 13px;
  font-weight: 700;
  letter-spacing: 1.4px;
}

.page-header h1 {
  margin: 0;
  color: var(--ink);
  font-size: 38px;
  line-height: 1.2;
}

.description {
  margin: 10px 0 0;
  color: var(--muted);
  font-size: 16px;
}

.header-actions {
  display: flex;
  align-items: center;
  gap: 12px;
}

.class-select {
  width: 260px;
}

.state-panel {
  min-height: 420px;
  display: flex;
  align-items: center;
  justify-content: center;
  border: 1px solid var(--hairline);
  border-radius: var(--radius-md);
  background: var(--surface);
}

.overview-grid {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 18px;
  margin-bottom: 18px;
}

.metric-card {
  position: relative;
  min-height: 132px;
  padding: 24px;
  overflow: hidden;
  border: 1px solid var(--hairline);
  border-radius: var(--radius-md);
  background: var(--surface);
  box-shadow: var(--shadow-sm);
}

.metric-card::after {
  position: absolute;
  right: -24px;
  bottom: -42px;
  width: 110px;
  height: 110px;
  border-radius: 50%;
  background: rgba(91, 69, 220, 0.08);
  content: '';
}

.metric-label {
  display: block;
  margin-bottom: 16px;
  color: var(--muted);
  font-size: 14px;
}

.metric-value {
  position: relative;
  z-index: 1;
  color: var(--ink);
  font-size: 34px;
  line-height: 1;
}

.metric-unit {
  position: relative;
  z-index: 1;
  margin-left: 6px;
  color: var(--muted);
  font-size: 14px;
}

.chart-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 18px;
  margin-bottom: 18px;
}

.panel {
  padding: 24px;
  border: 1px solid var(--hairline);
  border-radius: var(--radius-md);
  background: var(--surface);
  box-shadow: var(--shadow-sm);
}

.panel-heading {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 16px;
  margin-bottom: 18px;
}

.panel-heading h2 {
  margin: 0;
  color: var(--ink);
  font-size: 20px;
}

.panel-heading p {
  margin: 6px 0 0;
  color: var(--muted);
  font-size: 14px;
}

.trend-panel {
  margin-bottom: 18px;
}

.detail-grid {
  display: grid;
  grid-template-columns: minmax(0, 2fr) minmax(280px, 1fr);
  gap: 18px;
}

.topic-list {
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.topic-item {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
  padding: 14px 16px;
  border: 1px solid var(--hairline);
  border-radius: var(--radius-sm);
  color: var(--ink);
  background: var(--canvas);
}

:deep(.el-table) {
  --el-table-border-color: var(--hairline);
  --el-table-header-bg-color: var(--canvas);
}

:deep(.el-progress__text) {
  min-width: 42px;
  color: var(--muted);
}

@media (max-width: 1100px) {
  .overview-grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }

  .detail-grid {
    grid-template-columns: 1fr;
  }
}

@media (max-width: 820px) {
  .analytics-page {
    padding: 28px 24px 40px;
  }

  .page-header {
    flex-direction: column;
  }

  .header-actions {
    width: 100%;
  }

  .class-select {
    flex: 1;
    width: auto;
  }

  .chart-grid {
    grid-template-columns: 1fr;
  }
}

@media (max-width: 560px) {
  .analytics-page {
    padding: 24px 16px 36px;
  }

  .overview-grid {
    grid-template-columns: 1fr;
  }

  .header-actions {
    align-items: stretch;
    flex-direction: column;
  }

  .class-select {
    width: 100%;
  }

  .panel {
    padding: 18px;
  }
}
</style>