<template>
  <div class="report-page">
    <el-row :gutter="20" class="stats-row">
      <el-col :span="8">
        <el-card class="stat-card stat-time">
          <div class="stat-content">
            <div class="stat-icon">⏱</div>
            <div class="stat-label">总学习时长</div>
            <div class="stat-value">{{ totalHours }}<span class="unit">小时</span></div>
          </div>
        </el-card>
      </el-col>
      <el-col :span="8">
        <el-card class="stat-card stat-progress">
          <div class="stat-content">
            <div class="stat-icon">📈</div>
            <div class="stat-label">学习进度</div>
            <div class="stat-value">{{ progress }}<span class="unit">%</span></div>
          </div>
        </el-card>
      </el-col>
      <el-col :span="8">
        <el-card class="stat-card stat-score">
          <div class="stat-content">
            <div class="stat-icon">🎯</div>
            <div class="stat-label">综合评分</div>
            <div class="stat-value">{{ score }}<span class="unit">分</span></div>
          </div>
        </el-card>
      </el-col>
    </el-row>

    <el-row :gutter="20" class="charts-row">
      <el-col :span="16">
        <el-card class="chart-card">
          <template #header><span>学习时长趋势</span></template>
          <div ref="trendChart" class="chart-container"></div>
        </el-card>
      </el-col>
      <el-col :span="8">
        <el-card class="chart-card">
          <template #header><span>各模块学习时长</span></template>
          <div ref="moduleChart" class="chart-container"></div>
        </el-card>
      </el-col>
    </el-row>

    <el-row :gutter="20" class="content-row">
      <el-col :span="12">
        <el-card class="content-card">
          <template #header><span>薄弱点</span></template>
          <div class="content-card-inner">
            <div v-if="weaknesses.length" class="weakness-list">
              <div v-for="w in weaknesses" :key="w" class="weakness-tag">{{ w }}</div>
            </div>
            <div v-else style="color:#999;text-align:center;padding:20px">暂无记录</div>
          </div>
        </el-card>
      </el-col>
      <el-col :span="12">
        <el-card class="content-card">
          <template #header><span>学习建议</span></template>
          <div class="content-card-inner">
            <div v-if="suggestionList.length" class="suggestion-list">
              <div v-for="(s, i) in suggestionList" :key="i" class="suggestion-item">{{ i+1 }}. {{ s }}</div>
            </div>
            <div v-else style="color:#999;text-align:center;padding:20px">暂无建议</div>
          </div>
        </el-card>
      </el-col>
    </el-row>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, nextTick } from 'vue'
import * as echarts from 'echarts'
import request from '@/utils/request'

const trendChart = ref<HTMLElement>()
const moduleChart = ref<HTMLElement>()

const totalHours = ref(0)
const progress = ref(0)
const score = ref(0)
const modules = ref<any[]>([])
const trend = ref<any[]>([])
const weaknesses = ref<string[]>([])
const suggestionList = ref<string[]>([])
const profileData = ref<any>(null)

const labelMap: Record<string, string> = { mindmap: '思维导图', quiz: '练习题目', reading: '拓展阅读', code: '代码案例' }

const initTrend = () => {
  if (!trendChart.value || !trend.value.length) return
  const chart = echarts.init(trendChart.value)
  const days = [...new Set(trend.value.map((t: any) => t.day))].sort()
  const data = days.map(d => {
    const vals = trend.value.filter((t: any) => t.day === d)
    return vals.reduce((sum: number, v: any) => sum + Math.round((v.total || 0) / 60), 0)
  })
  chart.setOption({
    tooltip: { trigger: 'axis' },
    xAxis: { type: 'category', data: days.map((d: string) => d.slice(5)) },
    yAxis: { type: 'value', name: '分钟' },
    series: [{ type: 'line', data, smooth: true, areaStyle: { color: 'rgba(64,158,255,0.2)' }, itemStyle: { color: '#409eff' } }]
  })
}

const initModuleChart = () => {
  if (!moduleChart.value || !modules.value.length) return
  const chart = echarts.init(moduleChart.value)
  const sorted = [...modules.value].sort((a, b) => b.total - a.total)
  chart.setOption({
    tooltip: { trigger: 'axis', axisPointer: { type: 'shadow' } },
    grid: { left: '3%', right: '10%', top: '8%', bottom: '8%', containLabel: true },
    xAxis: { type: 'value', name: '分钟', axisLabel: { formatter: (v: number) => Math.round(v / 60) } },
    yAxis: {
      type: 'category',
      data: sorted.map(m => labelMap[m.module] || m.module),
      axisLabel: { fontSize: 13 },
    },
    series: [{
      type: 'bar',
      data: sorted.map(m => ({
        value: Math.round(m.total / 60),
        itemStyle: {
          color: new echarts.graphic.LinearGradient(0, 0, 1, 0, [
            { offset: 0, color: '#409eff' },
            { offset: 1, color: '#79bbff' },
          ]),
          borderRadius: [0, 6, 6, 0],
        },
      })),
      barWidth: '60%',
      label: {
        show: true,
        position: 'right',
        formatter: (p: any) => p.value + '分钟',
        fontSize: 12,
        color: '#666',
      },
    }],
  })
}

onMounted(async () => {
  try {
    const res: any = await request.get('/dashboard/report')
    if (res) {
      const sec = res.totalSec || 0
      totalHours.value = Math.round(sec / 3600 * 10) / 10
      progress.value = res.progress || 0
      score.value = res.score || 0
      modules.value = Array.isArray(res.modules) ? res.modules : []
      trend.value = Array.isArray(res.trend) ? res.trend : []
      const suggs = res.profile_suggestions
      suggestionList.value = typeof suggs === 'string' ? suggs.split('\n').filter(Boolean) : Array.isArray(suggs) ? suggs : []
      weaknesses.value = Array.isArray(res.weaknesses) ? res.weaknesses : []
      if (res.profile_data) profileData.value = res.profile_data
    }
  } catch {}
  await nextTick()
  initTrend()
  initModuleChart()
})
</script>

<style scoped>
.report-page {
  padding: 28px 32px;
  background: #f0f2f5;
  min-height: calc(100vh - 60px);
}

/* ===== 顶部统计卡片 ===== */
.stats-row { margin-bottom: 24px; }
.stat-card {
  border-radius: 16px !important;
  border: none !important;
  box-shadow: 0 2px 12px rgba(0,0,0,0.06) !important;
  transition: transform 0.25s, box-shadow 0.25s;
  overflow: hidden;
  position: relative;
}
.stat-card:hover { transform: translateY(-3px); box-shadow: 0 8px 24px rgba(0,0,0,0.1) !important; }
.stat-card::before {
  content: ''; position: absolute; top: 0; left: 0; right: 0;
  height: 4px;
}
.stat-time::before { background: linear-gradient(90deg, #409eff, #79bbff); }
.stat-progress::before { background: linear-gradient(90deg, #67c23a, #95de64); }
.stat-score::before { background: linear-gradient(90deg, #e6a23c, #f5d06e); }
.stat-content { padding: 28px 0 22px; }
.stat-icon { font-size: 28px; margin-bottom: 6px; }
.stat-label { font-size: 14px; color: #909399; margin-bottom: 8px; font-weight: 500; letter-spacing: 0.5px; }
.stat-value { font-size: 36px; font-weight: 700; color: #1a1a2e; }
.stat-value .unit { font-size: 14px; color: #909399; margin-left: 4px; font-weight: 400; }

/* ===== 图表卡片 ===== */
.charts-row { margin-bottom: 24px; }
.chart-card {
  border-radius: 16px !important;
  border: none !important;
  box-shadow: 0 2px 12px rgba(0,0,0,0.06) !important;
  overflow: hidden;
}
.chart-card :deep(.el-card__header) {
  border-bottom: 1px solid #f0f0f0;
  padding: 16px 20px;
  font-weight: 600;
  font-size: 15px;
  color: #1a1a2e;
  background: #fafbfc;
}
.chart-container { width: 100%; height: 300px; }

/* ===== 底部内容卡片 ===== */
.content-row { margin-bottom: 24px; }
.content-card {
  border-radius: 16px !important;
  border: none !important;
  box-shadow: 0 2px 12px rgba(0,0,0,0.06) !important;
  min-height: 200px;
  overflow: hidden;
}
.content-card :deep(.el-card__header) {
  border-bottom: 1px solid #f0f0f0;
  padding: 16px 20px;
  font-weight: 600;
  font-size: 15px;
  color: #1a1a2e;
  background: #fafbfc;
}

/* 薄弱点标签 */
.content-card { border-radius: 16px !important; border: none !important; box-shadow: 0 2px 12px rgba(0,0,0,0.06) !important; overflow: hidden; }
.content-card :deep(.el-card__body) { padding: 0 !important; }
.content-card-inner { padding: 16px 20px; min-height: 180px; display: flex; flex-direction: column; }

/* 薄弱点标签 */
.weakness-list { display: flex; flex-wrap: wrap; align-content: flex-start; gap: 10px; flex: 1; }
.weakness-tag {
  background: linear-gradient(135deg, #fff5f5, #ffe8e8);
  color: #e64553;
  padding: 8px 16px;
  border-radius: 20px;
  font-size: 13px;
  font-weight: 500;
  border: 1px solid #fdd5d5;
  transition: transform 0.2s;
  height: fit-content;
}
.weakness-tag:hover { transform: scale(1.05); }

/* 学习建议 */
.suggestion-list { display: flex; flex-direction: column; gap: 10px; flex: 1; }
.suggestion-item {
  padding: 12px 16px;
  background: linear-gradient(135deg, #f0f6ff, #e8f0fe);
  border-radius: 10px;
  color: #2c3e50;
  line-height: 1.5;
  font-size: 13px;
  border-left: 3px solid #409eff;
  transition: transform 0.2s;
  flex: 1;
  display: flex;
  align-items: center;
}
.suggestion-item:hover { transform: translateX(4px); }

</style>
