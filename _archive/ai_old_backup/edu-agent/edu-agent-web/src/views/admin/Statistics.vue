<template>
  <div class="statistics-page">
    <el-row :gutter="20" class="stats-row">
      <el-col :span="6">
        <el-card class="stat-card">
          <div class="stat-content">
            <div class="stat-icon users">
              <el-icon><User /></el-icon>
            </div>
            <div class="stat-info">
              <div class="stat-value">{{ stats.users }}</div>
              <div class="stat-label">注册用户</div>
              <div class="stat-trend positive">
                <el-icon><TrendCharts /></el-icon>
                <span>+12%</span>
              </div>
            </div>
          </div>
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card class="stat-card">
          <div class="stat-content">
            <div class="stat-icon online">
              <el-icon><UserFilled /></el-icon>
            </div>
            <div class="stat-info">
              <div class="stat-value">{{ stats.online }}</div>
              <div class="stat-label">在线用户</div>
              <div class="stat-trend positive">
                <el-icon><TrendCharts /></el-icon>
                <span>+8%</span>
              </div>
            </div>
          </div>
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card class="stat-card">
          <div class="stat-content">
            <div class="stat-icon exercises">
              <el-icon><DocumentChecked /></el-icon>
            </div>
            <div class="stat-info">
              <div class="stat-value">{{ stats.exercises }}</div>
              <div class="stat-label">完成练习</div>
              <div class="stat-trend positive">
                <el-icon><TrendCharts /></el-icon>
                <span>+25%</span>
              </div>
            </div>
          </div>
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card class="stat-card">
          <div class="stat-content">
            <div class="stat-icon projects">
              <el-icon><FolderChecked /></el-icon>
            </div>
            <div class="stat-info">
              <div class="stat-value">{{ stats.projects }}</div>
              <div class="stat-label">完成项目</div>
              <div class="stat-trend positive">
                <el-icon><TrendCharts /></el-icon>
                <span>+18%</span>
              </div>
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
              <span>用户增长趋势</span>
              <el-radio-group v-model="timeRange" size="small">
                <el-radio-button label="week">本周</el-radio-button>
                <el-radio-button label="month">本月</el-radio-button>
                <el-radio-button label="year">全年</el-radio-button>
              </el-radio-group>
            </div>
          </template>
          <div ref="userGrowthChart" class="chart-container"></div>
        </el-card>
      </el-col>
      <el-col :span="8">
        <el-card class="chart-card">
          <template #header>
            <span>用户分布</span>
          </template>
          <div ref="userDistributionChart" class="chart-container"></div>
        </el-card>
      </el-col>
    </el-row>

    <el-row :gutter="20" class="charts-row">
      <el-col :span="12">
        <el-card class="chart-card">
          <template #header>
            <span>学习时长统计</span>
          </template>
          <div ref="learningTimeChart" class="chart-container"></div>
        </el-card>
      </el-col>
      <el-col :span="12">
        <el-card class="chart-card">
          <template #header>
            <span>项目完成率</span>
          </template>
          <div ref="projectCompletionChart" class="chart-container"></div>
        </el-card>
      </el-col>
    </el-row>

    <el-row :gutter="20" class="content-row">
      <el-col :span="24">
        <el-card class="data-card">
          <template #header>
            <div class="card-header">
              <span>详细数据报表</span>
              <el-button type="primary" size="small" @click="exportData">导出数据</el-button>
            </div>
          </template>

          <el-tabs v-model="activeTab">
            <el-tab-pane label="用户数据" name="users">
              <div class="data-filters">
                <el-input
                  v-model="searchKeyword"
                  placeholder="搜索用户..."
                  style="width: 200px; margin-right: 15px"
                />
                <el-select v-model="filters.level" placeholder="用户等级" style="width: 150px; margin-right: 15px">
                  <el-option label="全部" value="" />
                  <el-option label="初级" value="beginner" />
                  <el-option label="中级" value="intermediate" />
                  <el-option label="高级" value="advanced" />
                </el-select>
                <el-select v-model="filters.status" placeholder="用户状态" style="width: 150px">
                  <el-option label="全部" value="" />
                  <el-option label="活跃" value="active" />
                  <el-option label="不活跃" value="inactive" />
                </el-select>
              </div>

              <el-table :data="userData" stripe style="width: 100%">
                <el-table-column prop="id" label="ID" width="80" />
                <el-table-column prop="name" label="用户名" width="120" />
                <el-table-column prop="email" label="邮箱" width="180" />
                <el-table-column prop="level" label="等级" width="100">
                  <template #default="{ row }">
                    <el-tag :type="getLevelTag(row.level)">{{ getLevelLabel(row.level) }}</el-tag>
                  </template>
                </el-table-column>
                <el-table-column prop="learningTime" label="学习时长(小时)" width="120" />
                <el-table-column prop="exercises" label="完成练习" width="100" />
                <el-table-column prop="projects" label="完成项目" width="100" />
                <el-table-column prop="lastActive" label="最后活跃时间" width="150" />
                <el-table-column prop="status" label="状态" width="80">
                  <template #default="{ row }">
                    <el-tag :type="row.status === 'active' ? 'success' : 'info'">
                      {{ row.status === 'active' ? '活跃' : '不活跃' }}
                    </el-tag>
                  </template>
                </el-table-column>
                <el-table-column label="操作" width="150">
                  <template #default>
                    <el-button link type="primary" size="small">查看详情</el-button>
                    <el-button link type="primary" size="small">编辑</el-button>
                  </template>
                </el-table-column>
              </el-table>

              <el-pagination
                v-model:current-page="pagination.page"
                v-model:page-size="pagination.size"
                :total="pagination.total"
                layout="total, prev, pager, next"
                style="margin-top: 20px; text-align: right"
              />
            </el-tab-pane>

            <el-tab-pane label="学习数据" name="learning">
              <el-table :data="learningData" stripe style="width: 100%">
                <el-table-column prop="date" label="日期" width="120" />
                <el-table-column prop="activeUsers" label="活跃用户" width="120" />
                <el-table-column prop="totalTime" label="总学习时长(小时)" width="150" />
                <el-table-column prop="avgTime" label="平均时长(小时)" width="150" />
                <el-table-column prop="completedExercises" label="完成练习" width="120" />
                <el-table-column prop="completedProjects" label="完成项目" width="120" />
                <el-table-column prop="avgAccuracy" label="平均正确率" width="120">
                  <template #default="{ row }">
                    {{ row.avgAccuracy }}
                  </template>
                </el-table-column>
                <el-table-column prop="newUsers" label="新增用户" width="100" />
              </el-table>
            </el-tab-pane>

            <el-tab-pane label="项目数据" name="projects">
              <el-table :data="projectData" stripe style="width: 100%">
                <el-table-column prop="name" label="项目名称" width="200" />
                <el-table-column prop="category" label="分类" width="100" />
                <el-table-column prop="level" label="级别" width="100" />
                <el-table-column prop="enrolled" label="参与人数" width="120" />
                <el-table-column prop="completed" label="完成人数" width="120" />
                <el-table-column prop="completionRate" label="完成率" width="100">
                  <template #default="{ row }">
                    <el-progress :percentage="row.completionRate" :width="60" />
                  </template>
                </el-table-column>
                <el-table-column prop="avgRating" label="平均评分" width="100" />
                <el-table-column prop="avgDuration" label="平均时长" width="120" />
              </el-table>
            </el-tab-pane>
          </el-tabs>
        </el-card>
      </el-col>
    </el-row>
  </div>
</template>

<script setup lang="ts">
import { getAdminStats, getUserList } from '@/api/admin'
import { ref, reactive, onMounted, nextTick } from 'vue'

import { ElMessage } from 'element-plus'

import {
  User, UserFilled, DocumentChecked, FolderChecked, TrendCharts
} from '@element-plus/icons-vue'
import * as echarts from 'echarts'

const timeRange = ref('week')
const activeTab = ref('users')
const searchKeyword = ref('')

const filters = reactive({
  level: '',
  status: ''
})

const pagination = reactive({
  page: 1,
  size: 20,
  total: 100
})

const stats = ref<any>({ users:0, online:0, exercises:0, projects:0 })

const userGrowthChart = ref()
const userDistributionChart = ref()
const learningTimeChart = ref()
const projectCompletionChart = ref()

const userData = ref<any[]>([])
const loading = ref(false)

const learningData = ref([
  { date: '2026-05-02', activeUsers: 156, totalTime: 432, avgTime: 2.8, completedExercises: 234, completedProjects: 12, avgAccuracy: '85%', newUsers: 15 },
  { date: '2026-05-01', activeUsers: 142, totalTime: 398, avgTime: 2.8, completedExercises: 215, completedProjects: 10, avgAccuracy: '83%', newUsers: 12 },
  { date: '2026-04-30', activeUsers: 138, totalTime: 365, avgTime: 2.6, completedExercises: 198, completedProjects: 8, avgAccuracy: '84%', newUsers: 18 },
  { date: '2026-04-29', activeUsers: 135, totalTime: 352, avgTime: 2.6, completedExercises: 186, completedProjects: 9, avgAccuracy: '82%', newUsers: 14 },
  { date: '2026-04-28', activeUsers: 130, totalTime: 338, avgTime: 2.6, completedExercises: 175, completedProjects: 7, avgAccuracy: '80%', newUsers: 16 }
])

const projectData = ref([
  { name: '电商管理系统', category: '全栈', level: '进阶', enrolled: 234, completed: 89, completionRate: 38, avgRating: 4.8, avgDuration: '40h' },
  { name: '在线学习平台', category: '全栈', level: '高级', enrolled: 156, completed: 45, completionRate: 29, avgRating: 4.9, avgDuration: '50h' },
  { name: '博客系统', category: '前端', level: '入门', enrolled: 345, completed: 234, completionRate: 68, avgRating: 4.7, avgDuration: '20h' },
  { name: 'RESTful API设计', category: '后端', level: '中级', enrolled: 189, completed: 98, completionRate: 52, avgRating: 4.5, avgDuration: '15h' },
  { name: '数据库优化', category: '数据库', level: '高级', enrolled: 87, completed: 32, completionRate: 37, avgRating: 4.6, avgDuration: '25h' }
])

const getLevelTag = (level: string) => {
  const tags: Record<string, any> = {
    beginner: 'success',
    intermediate: 'warning',
    advanced: 'danger'
  }
  return tags[level] || 'info'
}

const getLevelLabel = (level: string) => {
  const labels: Record<string, string> = {
    beginner: '初级',
    intermediate: '中级',
    advanced: '高级'
  }
  return labels[level] || level
}

const initUserGrowthChart = () => {
  const chart = echarts.init(userGrowthChart.value)
  const option = {
    tooltip: {
      trigger: 'axis'
    },
    legend: {
      data: ['新增用户', '活跃用户']
    },
    xAxis: {
      type: 'category',
      data: ['周一', '周二', '周三', '周四', '周五', '周六', '周日']
    },
    yAxis: {
      type: 'value'
    },
    series: [
      {
        name: '新增用户',
        type: 'line',
        data: [15, 12, 18, 14, 16, 20, 22],
        smooth: true,
        itemStyle: {
          color: '#409eff'
        }
      },
      {
        name: '活跃用户',
        type: 'line',
        data: [130, 142, 138, 135, 156, 148, 145],
        smooth: true,
        itemStyle: {
          color: '#67c23a'
        }
      }
    ]
  }
  chart.setOption(option)
}

const initUserDistributionChart = () => {
  const chart = echarts.init(userDistributionChart.value)
  const option = {
    tooltip: {
      trigger: 'item'
    },
    legend: {
      orient: 'vertical',
      right: 10,
      top: 'center'
    },
    series: [
      {
        name: '用户分布',
        type: 'pie',
        radius: ['40%', '70%'],
        avoidLabelOverlap: false,
        itemStyle: {
          borderRadius: 10,
          borderColor: '#fff',
          borderWidth: 2
        },
        label: {
          show: false,
          position: 'center'
        },
        emphasis: {
          label: {
            show: true,
            fontSize: 20,
            fontWeight: 'bold'
          }
        },
        data: [
          { value: 456, name: '初级用户' },
          { value: 532, name: '中级用户' },
          { value: 246, name: '高级用户' }
        ]
      }
    ]
  }
  chart.setOption(option)
}

const initLearningTimeChart = () => {
  const chart = echarts.init(learningTimeChart.value)
  const option = {
    tooltip: {
      trigger: 'axis'
    },
    xAxis: {
      type: 'category',
      data: ['Vue', 'React', 'Spring Boot', 'Node.js', '数据库', '项目实战']
    },
    yAxis: {
      type: 'value',
      name: '小时'
    },
    series: [
      {
        name: '学习时长',
        type: 'bar',
        data: [35, 28, 32, 24, 26, 45],
        itemStyle: {
          color: '#409eff'
        }
      }
    ]
  }
  chart.setOption(option)
}

const initProjectCompletionChart = () => {
  const chart = echarts.init(projectCompletionChart.value)
  const option = {
    tooltip: {
      trigger: 'axis'
    },
    xAxis: {
      type: 'category',
      data: ['电商系统', '学习平台', '博客系统', 'API设计', '数据库优化']
    },
    yAxis: {
      type: 'value',
      max: 100,
      name: '%'
    },
    series: [
      {
        name: '完成率',
        type: 'bar',
        data: [38, 29, 68, 52, 37],
        itemStyle: {
          color: (params: any) => {
            const colors = ['#67c23a', '#e6a23c', '#67c23a', '#e6a23c', '#f56c6c']
            return colors[params.dataIndex] || '#409eff'
          }
        }
      }
    ]
  }
  chart.setOption(option)
}

const exportData = () => {
  ElMessage.success('数据导出功能开发中...')
}

onMounted(() => {
  nextTick(() => {
    initUserGrowthChart()
    initUserDistributionChart()
    initLearningTimeChart()
    initProjectCompletionChart()
  })
})
</script>

<style scoped>
.statistics-page {
  padding: 20px;
}

.stats-row {
  margin-bottom: 20px;
}

.stat-card {
  border-radius: 8px;
  transition: transform 0.3s, box-shadow 0.3s;
}

.stat-card:hover {
  transform: translateY(-5px);
  box-shadow: 0 6px 16px rgba(0, 0, 0, 0.15);
}

.stat-content {
  display: flex;
  align-items: center;
  gap: 20px;
  padding: 20px 0;
}

.stat-icon {
  width: 60px;
  height: 60px;
  border-radius: 12px;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 28px;
  color: #fff;
}

.stat-icon.users {
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
}

.stat-icon.online {
  background: linear-gradient(135deg, #f093fb 0%, #f5576c 100%);
}

.stat-icon.exercises {
  background: linear-gradient(135deg, #4facfe 0%, #00f2fe 100%);
}

.stat-icon.projects {
  background: linear-gradient(135deg, #43e97b 0%, #38f9d7 100%);
}

.stat-info {
  flex: 1;
}

.stat-value {
  font-size: 28px;
  font-weight: bold;
  color: #333;
  margin-bottom: 8px;
}

.stat-label {
  font-size: 14px;
  color: #666;
  margin-bottom: 8px;
}

.stat-trend {
  display: flex;
  align-items: center;
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
.data-card {
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

.content-row {
  margin-bottom: 20px;
}

.data-filters {
  display: flex;
  margin-bottom: 20px;
  padding-bottom: 20px;
  border-bottom: 1px solid #eee;
}
</style>
