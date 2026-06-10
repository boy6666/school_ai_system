<template>
  <div class="learning-path-container">
    <el-row :gutter="20">
      <!-- 左侧主内容区 -->
      <el-col :span="16">
        <el-card class="path-card" shadow="never">
          <!-- 路径头部：目标、画像、时间等 -->
          <div class="path-header">
            <div class="header-left">
              <h2>个性化学习路径</h2>
              <div class="meta">
                <span class="label">目标：</span>
                <span class="value">掌握神经网络基础原理并完成模型实践</span>
                <el-button type="text" size="small" style="margin-left: 12px">编辑</el-button>
              </div>
              <div class="meta">
                <span class="label">学习画像：</span>
                <span class="value">逻辑分析型 · 基础较弱 · 目标导向</span>
                <el-button type="text" size="small" style="margin-left: 12px">重新规划路径</el-button>
                <el-button type="text" size="small">导出计划</el-button>
              </div>
            </div>
            <div class="header-right">
              <div class="stat">
                <div class="stat-label">目标掌握度</div>
                <div class="stat-value">≥85%</div>
              </div>
              <div class="stat">
                <div class="stat-label">预计完成时间</div>
                <div class="stat-value">2025-06-18 <span class="small">(18天后)</span></div>
              </div>
            </div>
          </div>
          <el-divider />

          <!-- 学习路径阶段 -->
          <div class="path-stages">
            <el-tabs v-model="activeStage" @tab-click="handleStageChange">
              <el-tab-pane label="今日计划" name="today"></el-tab-pane>
              <el-tab-pane label="本周路径" name="week"></el-tab-pane>
              <el-tab-pane label="考试冲刺" name="exam"></el-tab-pane>
              <el-tab-pane label="实践提升" name="practice"></el-tab-pane>
            </el-tabs>

            <!-- 阶段描述 -->
            <div class="stage-desc">基于学习画像、掌握度与目标，为你规划动态学习路线</div>

            <!-- 推荐学习时段 -->
            <div class="recommend-time">
              <el-icon><Clock /></el-icon> 推荐学习时段：每天 19:00-21:00
            </div>

            <!-- 任务列表 -->
            <div class="task-list">
              <div v-for="(task, idx) in currentTasks" :key="idx" class="task-item" :class="task.status">
                <div class="task-left">
                  <el-checkbox v-model="task.checked" @change="handleTaskCheck(task)" :disabled="task.status === 'completed'">
                    <span class="task-name">{{ task.name }}</span>
                  </el-checkbox>
                  <div class="task-meta">
                    <span class="duration">{{ task.duration }}</span>
                    <el-tag v-if="task.status === 'completed'" type="success" size="small">已完成</el-tag>
                    <el-tag v-else-if="task.status === 'in-progress'" type="warning" size="small">进行中</el-tag>
                    <el-tag v-else type="info" size="small">待开始</el-tag>
                  </div>
                </div>
                <div class="task-right">
                  <el-progress :percentage="task.progress" :stroke-width="6" :show-text="false" />
                  <span class="progress-text">{{ task.progress }}%</span>
                </div>
              </div>
            </div>

            <!-- 整体进度 -->
            <div class="overall-progress">
              <div class="progress-info">
                <span>已完成任务 {{ completedCount }}/{{ totalTasks }}</span>
                <span>总学习时长 {{ totalHours }}小时</span>
              </div>
              <el-progress :percentage="overallProgress" :stroke-width="10" :color="'#409EFF'" />
            </div>

            <!-- 路径调整建议 -->
            <el-card class="suggestion-card" shadow="never">
              <template #header>
                <span>📌 路径调整建议</span>
                <el-button type="text" style="float: right">更多 ></el-button>
              </template>
              <div class="suggestion-content">
                <div class="suggestion-item">
                  <span class="reason">检测到“反向传播算法”掌握度偏低（当前55%），建议：</span>
                  <div class="actions">
                    <el-tag size="small" type="danger">插入图解视频讲解（15分钟）</el-tag>
                    <el-tag size="small">增加专项练习（20分钟）</el-tag>
                    <el-tag size="small">延长本阶段学习时间（+30分钟）</el-tag>
                  </div>
                </div>
              </div>
            </el-card>

            <!-- 应用建议 + 阶段测评 -->
            <el-row :gutter="16">
              <el-col :span="12">
                <el-card class="app-suggestion" shadow="never">
                  <template #header><span>应用建议</span></template>
                  <div class="app-item">模型调优与超参数搜索 <span class="duration">90分钟</span></div>
                  <div class="app-item">阶段测评 <span class="duration">60分钟</span></div>
                  <div class="app-item">错题复盘与薄弱点强化 <span class="duration">30分钟</span></div>
                </el-card>
              </el-col>
              <el-col :span="12">
                <el-card class="stage-test" shadow="never">
                  <template #header><span>阶段测评</span></template>
                  <div class="test-item">知识点综合测评 <span class="duration">60分钟</span></div>
                  <div class="test-item">错题复盘与薄弱点强化 <span class="duration">30分钟</span></div>
                </el-card>
              </el-col>
            </el-row>
          </div>
        </el-card>
      </el-col>

      <!-- 右侧辅助区 -->
      <el-col :span="8">
        <!-- 当前概览卡片 -->
        <el-card class="overview-card" shadow="never">
          <template #header><span>当前概览</span></template>
          <div class="overview-stats">
            <div class="stat-circle">
              <el-progress type="circle" :percentage="72" :width="100" :stroke-width="8" color="#409EFF" />
              <div class="stat-label">当前掌握度</div>
            </div>
            <div class="stat-circle">
              <el-progress type="circle" :percentage="18" :width="100" :stroke-width="8" color="#F56C6C" />
              <div class="stat-label">学习中</div>
            </div>
            <div class="stat-circle">
              <el-progress type="circle" :percentage="10" :width="100" :stroke-width="8" color="#909399" />
              <div class="stat-label">未掌握</div>
            </div>
          </div>
        </el-card>

        <!-- 学习日历 -->
        <el-card class="calendar-card" shadow="never" style="margin-top: 20px">
          <template #header>
            <span>学习日历</span>
            <el-button type="text" style="float: right">更多 ></el-button>
          </template>
          <el-calendar v-model="calendarDate">
            <template #date-cell="{ data }">
              <div class="calendar-day" :class="{ completed: isCompletedDay(data.day), planned: isPlannedDay(data.day), needStrengthen: isNeedStrengthenDay(data.day) }">
                {{ data.day.split('-')[2] }}
              </div>
            </template>
          </el-calendar>
        </el-card>

        <!-- 为你推荐的个性化资源 -->
        <el-card class="resource-card" shadow="never" style="margin-top: 20px">
          <template #header>
            <span>为你推荐的个性化资源</span>
            <el-button type="text" style="float: right">查看全部资源 ></el-button>
          </template>
          <div class="resource-tabs">
            <el-tabs v-model="activeResourceTab">
              <el-tab-pane label="推荐文档" name="doc"></el-tab-pane>
              <el-tab-pane label="推荐视频" name="video"></el-tab-pane>
              <el-tab-pane label="推荐练习" name="exercise"></el-tab-pane>
              <el-tab-pane label="代码实操" name="code"></el-tab-pane>
            </el-tabs>
          </div>
          <div class="resource-list">
            <div v-for="res in currentResources" :key="res.name" class="resource-item">
              <div class="res-info">
                <div class="res-name">{{ res.name }}</div>
                <div class="res-meta">{{ res.duration }} · {{ res.time }}</div>
              </div>
              <el-button type="primary" size="small" plain>开始学习</el-button>
            </div>
          </div>
        </el-card>

        <!-- 动态调整记录 -->
        <el-card class="adjust-card" shadow="never" style="margin-top: 20px">
          <template #header>
            <span>动态调整记录</span>
            <el-button type="text" style="float: right">更多 ></el-button>
          </template>
          <div class="adjust-list">
            <div v-for="record in adjustRecords" :key="record.time" class="adjust-item">
              <div class="adjust-time">{{ record.time }}</div>
              <div class="adjust-content">{{ record.content }}</div>
            </div>
          </div>
        </el-card>
      </el-col>
    </el-row>
  </div>
</template>

<script setup lang="ts">
import { ref, computed } from 'vue'
import { ElMessage } from 'element-plus'

// 阶段
const activeStage = ref('today')
const activeResourceTab = ref('doc')
const calendarDate = ref(new Date())

// 各阶段任务数据（Mock）
const tasksData = {
  today: [
    { name: '机器学习基础概念回顾', duration: '30分钟', status: 'completed', progress: 100, checked: true },
    { name: '线性回归与损失函数', duration: '45分钟', status: 'completed', progress: 100, checked: true },
    { name: '梯度下降原理与实现', duration: '45分钟', status: 'completed', progress: 100, checked: true },
    { name: '反向传播算法详解', duration: '60分钟', status: 'in-progress', progress: 72, checked: false },
    { name: '激活函数与优化器对比', duration: '45分钟', status: 'pending', progress: 0, checked: false }
  ],
  week: [
    { name: '神经网络基础结构', duration: '60分钟', status: 'completed', progress: 100, checked: true },
    { name: '前向传播与反向传播推导', duration: '90分钟', status: 'in-progress', progress: 55, checked: false },
    { name: '使用PyTorch搭建MLP模型', duration: '90分钟', status: 'pending', progress: 0, checked: false },
    { name: '训练模型并可视化损失曲线', duration: '90分钟', status: 'pending', progress: 0, checked: false }
  ],
  exam: [
    { name: '知识点综合测评', duration: '60分钟', status: 'pending', progress: 0, checked: false },
    { name: '错题复盘与薄弱点强化', duration: '30分钟', status: 'pending', progress: 0, checked: false }
  ],
  practice: [
    { name: '模型调优与超参数搜索', duration: '90分钟', status: 'pending', progress: 0, checked: false },
    { name: '阶段测评', duration: '60分钟', status: 'pending', progress: 0, checked: false }
  ]
}

const currentTasks = computed(() => tasksData[activeStage.value as keyof typeof tasksData] || [])

const totalTasks = computed(() => currentTasks.value.length)
const completedCount = computed(() => currentTasks.value.filter(t => t.status === 'completed').length)
const overallProgress = computed(() => (completedCount.value / totalTasks.value) * 100)
const totalHours = ref(15.6)

const handleTaskCheck = (task: any) => {
  if (!task.checked) return
  ElMessage.info(`任务 "${task.name}" 标记为已完成`)
  task.status = 'completed'
  task.progress = 100
}

const handleStageChange = () => {
  // 切换阶段时重置选中状态等
}

// 日历标记数据（示例）
const completedDays = ['2025-05-02', '2025-05-03', '2025-05-04']
const plannedDays = ['2025-05-06', '2025-05-07']
const needStrengthenDays = ['2025-05-05']

const isCompletedDay = (date: string) => completedDays.includes(date)
const isPlannedDay = (date: string) => plannedDays.includes(date)
const isNeedStrengthenDay = (date: string) => needStrengthenDays.includes(date)

// 推荐资源（根据当前阶段不同，可动态变化）
const resources = {
  doc: [
    { name: '反向传播算法图解教程', duration: '约20分钟', time: '15:32' },
    { name: '神经网络常见问题汇总', duration: '约15分钟', time: '18:45' }
  ],
  video: [
    { name: '反向传播动画演示', duration: '20分钟', time: '15:32' },
    { name: '梯度流动可视化解析', duration: '25分钟', time: '18:45' }
  ],
  exercise: [
    { name: '反向传播计算题（基础）', duration: '40分钟', time: '15:32' },
    { name: '梯度计算专项练习', duration: '30分钟', time: '18:45' }
  ],
  code: [
    { name: '实现反向传播（PyTorch）', duration: '40分钟', time: '15:32' },
    { name: '梯度检查与调试实验', duration: '30分钟', time: '18:45' }
  ]
}
const currentResources = computed(() => resources[activeResourceTab.value as keyof typeof resources] || [])

// 动态调整记录
const adjustRecords = ref<any[]>([])
</script>

<style scoped>
.learning-path-container {
  padding: 20px;
  background-color: #f5f7fa;
  min-height: 100vh;
}
.path-card {
  border-radius: 16px;
}
.path-header {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
}
.header-left h2 {
  margin: 0 0 12px 0;
}
.meta {
  margin-bottom: 8px;
  font-size: 14px;
}
.meta .label {
  color: #909399;
}
.meta .value {
  color: #303133;
  font-weight: 500;
}
.header-right {
  display: flex;
  gap: 24px;
}
.stat {
  text-align: center;
}
.stat-label {
  font-size: 12px;
  color: #909399;
}
.stat-value {
  font-size: 20px;
  font-weight: bold;
  color: #409eff;
}
.stat-value .small {
  font-size: 12px;
  font-weight: normal;
}
.stage-desc {
  margin: 12px 0;
  color: #606266;
  font-size: 14px;
}
.recommend-time {
  background: #ecf5ff;
  padding: 8px 12px;
  border-radius: 8px;
  margin-bottom: 20px;
  color: #409eff;
  font-size: 14px;
}
.task-list {
  margin-bottom: 20px;
}
.task-item {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 12px 0;
  border-bottom: 1px solid #ebeef5;
}
.task-left {
  flex: 1;
}
.task-name {
  margin-left: 8px;
  font-weight: 500;
}
.task-meta {
  margin-left: 28px;
  font-size: 12px;
  color: #909399;
}
.duration {
  margin-right: 12px;
}
.task-right {
  display: flex;
  align-items: center;
  gap: 12px;
  width: 120px;
}
.progress-text {
  font-size: 12px;
  color: #606266;
}
.task-item.completed .task-name {
  text-decoration: line-through;
  color: #909399;
}
.overall-progress {
  margin: 16px 0;
  padding: 12px;
  background: #fafafa;
  border-radius: 8px;
}
.progress-info {
  display: flex;
  justify-content: space-between;
  margin-bottom: 8px;
  font-size: 14px;
}
.suggestion-card {
  margin: 16px 0;
  background: #fff9e6;
}
.suggestion-content .suggestion-item {
  font-size: 14px;
}
.suggestion-item .reason {
  color: #e6a23c;
}
.actions {
  margin-top: 8px;
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}
.app-suggestion, .stage-test {
  background: #f0f9ff;
}
.app-item, .test-item {
  display: flex;
  justify-content: space-between;
  padding: 8px 0;
  border-bottom: 1px dashed #dcdfe6;
}
.overview-stats {
  display: flex;
  justify-content: space-around;
  text-align: center;
}
.stat-circle {
  text-align: center;
}
.calendar-day {
  text-align: center;
  border-radius: 50%;
  width: 28px;
  height: 28px;
  line-height: 28px;
  margin: 0 auto;
}
.calendar-day.completed {
  background-color: #67c23a;
  color: white;
}
.calendar-day.planned {
  background-color: #409eff;
  color: white;
}
.calendar-day.needStrengthen {
  background-color: #e6a23c;
  color: white;
}
.resource-list {
  margin-top: 12px;
}
.resource-item {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 12px 0;
  border-bottom: 1px solid #ebeef5;
}
.res-name {
  font-weight: 500;
  margin-bottom: 4px;
}
.res-meta {
  font-size: 12px;
  color: #909399;
}
.adjust-list {
  max-height: 300px;
  overflow-y: auto;
}
.adjust-item {
  padding: 12px 0;
  border-bottom: 1px solid #ebeef5;
}
.adjust-time {
  font-size: 12px;
  color: #909399;
  margin-bottom: 6px;
}
.adjust-content {
  font-size: 14px;
}
</style>
