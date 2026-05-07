<template>
  <div class="report-page">
    <el-row :gutter="20" class="stats-row">
      <el-col :span="6">
        <el-card class="stat-card">
          <div class="stat-content">
            <div class="stat-label">总学习时长</div>
            <div class="stat-value">120<span class="unit">小时</span></div>
            <div class="stat-trend positive">
              <el-icon><TrendCharts /></el-icon>
              <span>+15%</span>
            </div>
          </div>
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card class="stat-card">
          <div class="stat-content">
            <div class="stat-label">完成练习</div>
            <div class="stat-value">45<span class="unit">题</span></div>
            <div class="stat-trend positive">
              <el-icon><TrendCharts /></el-icon>
              <span>+8%</span>
            </div>
          </div>
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card class="stat-card">
          <div class="stat-content">
            <div class="stat-label">平均正确率</div>
            <div class="stat-value">85<span class="unit">%</span></div>
            <div class="stat-trend positive">
              <el-icon><TrendCharts /></el-icon>
              <span>+5%</span>
            </div>
          </div>
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card class="stat-card">
          <div class="stat-content">
            <div class="stat-label">完成项目</div>
            <div class="stat-value">3<span class="unit">个</span></div>
            <div class="stat-trend negative">
              <el-icon><Bottom /></el-icon>
              <span>-2%</span>
            </div>
          </div>
        </el-card>
      </el-col>
    </el-row>

    <el-row :gutter="20" class="charts-row">
      <el-col :span="16">
        <el-card class="chart-card">
          <template #header>
            <div class="card-header">
              <span>学习时长趋势</span>
              <el-radio-group v-model="timeRange" size="small">
                <el-radio-button label="week">本周</el-radio-button>
                <el-radio-button label="month">本月</el-radio-button>
                <el-radio-button label="year">全年</el-radio-button>
              </el-radio-group>
            </div>
          </template>
          <div ref="learningChart" class="chart-container"></div>
        </el-card>
      </el-col>
      <el-col :span="8">
        <el-card class="chart-card">
          <template #header>
            <span>能力雷达图</span>
          </template>
          <div ref="radarChart" class="chart-container"></div>
        </el-card>
      </el-col>
    </el-row>

    <el-row :gutter="20" class="content-row">
      <el-col :span="12">
        <el-card class="content-card">
          <template #header>
            <span>学习进度</span>
          </template>
          <div class="progress-list">
            <div class="progress-item" v-for="item in learningProgress" :key="item.subject">
              <div class="progress-info">
                <span class="subject-name">{{ item.subject }}</span>
                <span class="progress-percent">{{ item.progress }}%</span>
              </div>
              <el-progress :percentage="item.progress" :color="getProgressColor(item.progress)" />
            </div>
          </div>
        </el-card>
      </el-col>
      <el-col :span="12">
        <el-card class="content-card">
          <template #header>
            <span>学习建议</span>
          </template>
          <div class="suggestion-list">
            <div class="suggestion-item" v-for="(suggestion, index) in suggestions" :key="index">
              <el-icon class="suggestion-icon" :class="suggestion.type">
                <component :is="getSuggestionIcon(suggestion.type)" />
              </el-icon>
              <div class="suggestion-content">
                <h4>{{ suggestion.title }}</h4>
                <p>{{ suggestion.description }}</p>
              </div>
            </div>
          </div>
        </el-card>
      </el-col>
    </el-row>

    <el-row :gutter="20" class="analysis-row">
      <el-col :span="24">
        <el-card class="analysis-card">
          <template #header>
            <div class="card-header">
              <span>效果评估报告</span>
              <el-button type="primary" size="small" @click="exportReport">导出报告</el-button>
            </div>
          </template>

          <el-tabs v-model="activeTab">
            <el-tab-pane label="总体分析" name="overview">
              <div class="analysis-content">
                <div class="analysis-section">
                  <h3>学习表现分析</h3>
                  <p class="analysis-text">
                    您在过去一段时间内表现优秀，学习时长稳步增长，正确率保持在较高水平。
                    特别是在前端开发方面，您的进步尤为明显，Vue、React等框架的掌握程度大幅提升。
                  </p>
                </div>

                <div class="analysis-section">
                  <h3>优势领域</h3>
                  <div class="strength-list">
                    <div class="strength-item">
                      <el-icon class="strength-icon"><SuccessFilled /></el-icon>
                      <div>
                        <strong>前端开发</strong>
                        <p>Vue、React、TypeScript等技术掌握扎实，能够独立完成复杂的前端项目</p>
                      </div>
                    </div>
                    <div class="strength-item">
                      <el-icon class="strength-icon"><SuccessFilled /></el-icon>
                      <div>
                        <strong>问题解决能力</strong>
                        <p>具备良好的问题分析和解决能力，遇到问题能够快速定位并找到解决方案</p>
                      </div>
                    </div>
                    <div class="strength-item">
                      <el-icon class="strength-icon"><SuccessFilled /></el-icon>
                      <div>
                        <strong>学习态度</strong>
                        <p>学习积极主动，能够按时完成学习任务，对待练习认真负责</p>
                      </div>
                    </div>
                  </div>
                </div>

                <div class="analysis-section">
                  <h3>提升空间</h3>
                  <div class="improvement-list">
                    <div class="improvement-item">
                      <el-icon class="improvement-icon"><WarningFilled /></el-icon>
                      <div>
                        <strong>后端开发</strong>
                        <p>建议加强对Spring Boot、Node.js等后端技术的学习，提升全栈开发能力</p>
                      </div>
                    </div>
                    <div class="improvement-item">
                      <el-icon class="improvement-icon"><WarningFilled /></el-icon>
                      <div>
                        <strong>数据库优化</strong>
                        <p>需要深入学习数据库设计和性能优化，提升数据处理能力</p>
                      </div>
                    </div>
                    <div class="improvement-item">
                      <el-icon class="improvement-icon"><WarningFilled /></el-icon>
                      <div>
                        <strong>项目管理</strong>
                        <p>建议参与更多团队项目，提升项目管理和协作能力</p>
                      </div>
                    </div>
                  </div>
                </div>

                <div class="analysis-section">
                  <h3>综合评分</h3>
                  <div class="score-overview">
                    <div class="score-circle">
                      <el-progress
                        type="circle"
                        :percentage="85"
                        :width="150"
                        :stroke-width="12"
                        color="#409eff"
                      >
                        <span class="score-text">85分</span>
                      </el-progress>
                    </div>
                    <div class="score-details">
                      <div class="score-item">
                        <span class="score-label">学习态度</span>
                        <el-rate v-model="scores.attitude" disabled show-score text-color="#ff9900" />
                      </div>
                      <div class="score-item">
                        <span class="score-label">学习效果</span>
                        <el-rate v-model="scores.effect" disabled show-score text-color="#ff9900" />
                      </div>
                      <div class="score-item">
                        <span class="score-label">进步程度</span>
                        <el-rate v-model="scores.progress" disabled show-score text-color="#ff9900" />
                      </div>
                      <div class="score-item">
                        <span class="score-label">综合能力</span>
                        <el-rate v-model="scores.ability" disabled show-score text-color="#ff9900" />
                      </div>
                    </div>
                  </div>
                </div>
              </div>
            </el-tab-pane>

            <el-tab-pane label="详细数据" name="data">
              <el-table :data="detailData" border style="width: 100%">
                <el-table-column prop="date" label="日期" width="120" />
                <el-table-column prop="subject" label="学习科目" width="120" />
                <el-table-column prop="duration" label="学习时长(小时)" width="140" />
                <el-table-column prop="exercises" label="练习数量" width="100" />
                <el-table-column prop="accuracy" label="正确率" width="100">
                  <template #default="{ row }">
                    <el-tag :type="getAccuracyType(row.accuracy)">
                      {{ row.accuracy }}
                    </el-tag>
                  </template>
                </el-table-column>
                <el-table-column prop="notes" label="学习笔记" />
              </el-table>
            </el-tab-pane>

            <el-tab-pane label="对比分析" name="compare">
              <div class="compare-section">
                <h4>与同龄人对比</h4>
                <el-row :gutter="20">
                  <el-col :span="12">
                    <div ref="compareChart" class="chart-container"></div>
                  </el-col>
                  <el-col :span="12">
                    <div class="compare-data">
                      <div class="compare-item">
                        <span class="compare-label">学习时长排名</span>
                        <span class="compare-value top">前10%</span>
                      </div>
                      <div class="compare-item">
                        <span class="compare-label">正确率排名</span>
                        <span class="compare-value top">前15%</span>
                      </div>
                      <div class="compare-item">
                        <span class="compare-label">项目完成排名</span>
                        <span class="compare-value middle">前30%</span>
                      </div>
                      <div class="compare-item">
                        <span class="compare-label">综合能力排名</span>
                        <span class="compare-value top">前12%</span>
                      </div>
                    </div>
                  </el-col>
                </el-row>
              </div>
            </el-tab-pane>
          </el-tabs>
        </el-card>
      </el-col>
    </el-row>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, nextTick } from 'vue'
import { ElMessage } from 'element-plus'
import { TrendCharts, Bottom, SuccessFilled, WarningFilled, Warning, CircleCheckFilled } from '@element-plus/icons-vue'
import * as echarts from 'echarts'

const timeRange = ref('week')
const activeTab = ref('overview')

const learningChart = ref()
const radarChart = ref()
const compareChart = ref()

const learningProgress = ref([
  { subject: 'Vue开发', progress: 85 },
  { subject: 'React开发', progress: 72 },
  { subject: 'Spring Boot', progress: 58 },
  { subject: '数据库', progress: 65 },
  { subject: '项目实战', progress: 78 }
])

const suggestions = ref([
  {
    type: 'success',
    title: '保持优秀表现',
    description: '您在前端开发方面表现出色，继续保持学习热情，可以尝试挑战更复杂的项目'
  },
  {
    type: 'warning',
    title: '加强后端学习',
    description: '建议适当增加后端开发的学习时间，提升全栈开发能力'
  },
  {
    type: 'info',
    title: '注重实践应用',
    description: '多参与实际项目，将所学知识应用到实践中，加深理解和记忆'
  }
])

const scores = ref({
  attitude: 4.5,
  effect: 4.2,
  progress: 4.3,
  ability: 4.0
})

const detailData = ref([
  { date: '2026-05-01', subject: 'Vue开发', duration: 2.5, exercises: 5, accuracy: '90%', notes: '完成了Vue组件练习' },
  { date: '2026-04-30', subject: 'Spring Boot', duration: 1.8, exercises: 3, accuracy: '82%', notes: '学习了RESTful API' },
  { date: '2026-04-29', subject: 'React开发', duration: 2.2, exercises: 4, accuracy: '88%', notes: 'React Hooks练习' },
  { date: '2026-04-28', subject: '数据库', duration: 1.5, exercises: 6, accuracy: '85%', notes: 'SQL查询优化' },
  { date: '2026-04-27', subject: '项目实战', duration: 3.0, exercises: 2, accuracy: '95%', notes: '电商系统开发' }
])

const getProgressColor = (percentage: number) => {
  if (percentage >= 80) return '#67c23a'
  if (percentage >= 60) return '#409eff'
  return '#e6a23c'
}

const getAccuracyType = (accuracy: string) => {
  const percent = parseInt(accuracy)
  if (percent >= 90) return 'success'
  if (percent >= 80) return 'warning'
  return 'danger'
}

const getSuggestionIcon = (type: string) => {
  const icons: Record<string, any> = {
    success: CircleCheckFilled,
    warning: WarningFilled,
    info: Warning
  }
  return icons[type] || Warning
}

const initLearningChart = () => {
  const chart = echarts.init(learningChart.value)
  const option = {
    tooltip: {
      trigger: 'axis'
    },
    xAxis: {
      type: 'category',
      data: ['周一', '周二', '周三', '周四', '周五', '周六', '周日']
    },
    yAxis: {
      type: 'value',
      name: '小时'
    },
    series: [
      {
        name: '学习时长',
        type: 'line',
        data: [2.5, 3.0, 2.8, 3.5, 2.2, 4.0, 3.8],
        smooth: true,
        areaStyle: {
          color: new echarts.graphic.LinearGradient(0, 0, 0, 1, [
            { offset: 0, color: 'rgba(64, 158, 255, 0.3)' },
            { offset: 1, color: 'rgba(64, 158, 255, 0.05)' }
          ])
        },
        itemStyle: {
          color: '#409eff'
        }
      }
    ]
  }
  chart.setOption(option)
}

const initRadarChart = () => {
  const chart = echarts.init(radarChart.value)
  const option = {
    radar: {
      indicator: [
        { name: '前端开发', max: 100 },
        { name: '后端开发', max: 100 },
        { name: '数据库', max: 100 },
        { name: '算法', max: 100 },
        { name: '项目实战', max: 100 },
        { name: '团队协作', max: 100 }
      ]
    },
    series: [
      {
        type: 'radar',
        data: [
          {
            value: [85, 58, 65, 72, 78, 70],
            name: '能力评估',
            areaStyle: {
              color: 'rgba(64, 158, 255, 0.3)'
            },
            itemStyle: {
              color: '#409eff'
            }
          }
        ]
      }
    ]
  }
  chart.setOption(option)
}

const initCompareChart = () => {
  const chart = echarts.init(compareChart.value)
  const option = {
    tooltip: {
      trigger: 'bar'
    },
    xAxis: {
      type: 'category',
      data: ['学习时长', '正确率', '项目完成', '综合能力']
    },
    yAxis: {
      type: 'value',
      max: 100
    },
    series: [
      {
        name: '我',
        type: 'bar',
        data: [85, 88, 75, 82],
        itemStyle: {
          color: '#409eff'
        }
      },
      {
        name: '平均水平',
        type: 'bar',
        data: [65, 70, 60, 68],
        itemStyle: {
          color: '#e6a23c'
        }
      }
    ]
  }
  chart.setOption(option)
}

const exportReport = () => {
  ElMessage.success('报告导出功能开发中...')
}

onMounted(() => {
  nextTick(() => {
    initLearningChart()
    initRadarChart()
    initCompareChart()
  })
})
</script>

<style scoped>
.report-page {
  padding: 20px;
}

.stats-row {
  margin-bottom: 20px;
}

.stat-card {
  border-radius: 8px;
  text-align: center;
}

.stat-content {
  padding: 20px 0;
}

.stat-label {
  font-size: 14px;
  color: #666;
  margin-bottom: 10px;
}

.stat-value {
  font-size: 32px;
  font-weight: bold;
  color: #333;
  margin-bottom: 10px;
}

.stat-value .unit {
  font-size: 16px;
  color: #999;
  margin-left: 5px;
}

.stat-trend {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 5px;
  font-size: 14px;
}

.stat-trend.positive {
  color: #67c23a;
}

.stat-trend.negative {
  color: #f56c6c;
}

.charts-row {
  margin-bottom: 20px;
}

.chart-card,
.content-card,
.analysis-card {
  border-radius: 8px;
  min-height: 400px;
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.chart-container {
  width: 100%;
  height: 300px;
}

.content-row,
.analysis-row {
  margin-bottom: 20px;
}

.progress-list {
  padding: 10px 0;
}

.progress-item {
  margin-bottom: 20px;
}

.progress-item:last-child {
  margin-bottom: 0;
}

.progress-info {
  display: flex;
  justify-content: space-between;
  margin-bottom: 8px;
}

.subject-name {
  font-weight: 500;
  color: #333;
}

.progress-percent {
  color: #666;
  font-size: 14px;
}

.suggestion-list {
  padding: 10px 0;
}

.suggestion-item {
  display: flex;
  gap: 15px;
  margin-bottom: 20px;
  padding: 15px;
  background: #f5f7fa;
  border-radius: 8px;
}

.suggestion-icon {
  font-size: 24px;
  flex-shrink: 0;
}

.suggestion-icon.success {
  color: #67c23a;
}

.suggestion-icon.warning {
  color: #e6a23c;
}

.suggestion-icon.info {
  color: #409eff;
}

.suggestion-content h4 {
  margin: 0 0 8px 0;
  color: #333;
  font-size: 15px;
}

.suggestion-content p {
  margin: 0;
  color: #666;
  font-size: 14px;
  line-height: 1.6;
}

.analysis-content {
  padding: 20px 0;
}

.analysis-section {
  margin-bottom: 40px;
}

.analysis-section:last-child {
  margin-bottom: 0;
}

.analysis-section h3 {
  color: #333;
  margin-bottom: 15px;
  font-size: 18px;
}

.analysis-text {
  color: #666;
  line-height: 1.8;
  font-size: 15px;
}

.strength-list,
.improvement-list {
  display: flex;
  flex-direction: column;
  gap: 20px;
}

.strength-item,
.improvement-item {
  display: flex;
  gap: 15px;
  padding: 20px;
  border-radius: 8px;
}

.strength-item {
  background: #f0f9ff;
  border-left: 4px solid #67c23a;
}

.improvement-item {
  background: #fef9f0;
  border-left: 4px solid #e6a23c;
}

.strength-icon,
.improvement-icon {
  font-size: 24px;
  flex-shrink: 0;
}

.strength-icon {
  color: #67c23a;
}

.improvement-icon {
  color: #e6a23c;
}

.strength-item strong,
.improvement-item strong {
  display: block;
  margin-bottom: 8px;
  color: #333;
}

.strength-item p,
.improvement-item p {
  margin: 0;
  color: #666;
  line-height: 1.6;
}

.score-overview {
  display: flex;
  gap: 40px;
  align-items: flex-start;
  flex-wrap: wrap;
}

.score-circle {
  flex-shrink: 0;
}

.score-text {
  font-size: 24px;
  font-weight: bold;
  color: #409eff;
}

.score-details {
  flex: 1;
}

.score-item {
  display: flex;
  align-items: center;
  margin-bottom: 15px;
}

.score-item:last-child {
  margin-bottom: 0;
}

.score-label {
  width: 100px;
  color: #333;
  font-weight: 500;
}

.compare-section {
  padding: 20px 0;
}

.compare-section h4 {
  color: #333;
  margin-bottom: 20px;
}

.compare-data {
  padding: 20px;
}

.compare-item {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 15px 0;
  border-bottom: 1px solid #eee;
}

.compare-item:last-child {
  border-bottom: none;
}

.compare-label {
  color: #666;
}

.compare-value {
  font-weight: bold;
  font-size: 16px;
}

.compare-value.top {
  color: #67c23a;
}

.compare-value.middle {
  color: #e6a23c;
}
</style>
