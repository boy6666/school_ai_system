<template>
  <div class="dashboard">
    <el-row :gutter="20" class="stats-row">
      <el-col :span="6">
        <el-card class="stat-card">
          <div class="stat-item">
            <div class="stat-icon study-time">
              <el-icon><Timer /></el-icon>
            </div>
            <div class="stat-content">
              <div class="stat-label">学习时长</div>
              <div class="stat-value">120h</div>
            </div>
          </div>
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card class="stat-card">
          <div class="stat-item">
            <div class="stat-icon complete-count">
              <el-icon><DocumentChecked /></el-icon>
            </div>
            <div class="stat-content">
              <div class="stat-label">完成练习</div>
              <div class="stat-value">45</div>
            </div>
          </div>
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card class="stat-card">
          <div class="stat-item">
            <div class="stat-icon project-count">
              <el-icon><FolderChecked /></el-icon>
            </div>
            <div class="stat-content">
              <div class="stat-label">项目案例</div>
              <div class="stat-value">8</div>
            </div>
          </div>
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card class="stat-card">
          <div class="stat-item">
            <div class="stat-icon accuracy">
              <el-icon><TrendCharts /></el-icon>
            </div>
            <div class="stat-content">
              <div class="stat-label">正确率</div>
              <div class="stat-value">85%</div>
            </div>
          </div>
        </el-card>
      </el-col>
    </el-row>

    <el-row :gutter="20" class="content-row">
      <el-col :span="16">
        <el-card class="content-card">
          <template #header>
            <div class="card-header">
              <span>学习进度</span>
              <el-button link type="primary">查看详情</el-button>
            </div>
          </template>
          <div class="learning-progress">
            <div class="progress-item">
              <div class="progress-label">前端开发</div>
              <el-progress :percentage="75" />
            </div>
            <div class="progress-item">
              <div class="progress-label">后端开发</div>
              <el-progress :percentage="60" />
            </div>
            <div class="progress-item">
              <div class="progress-label">数据库</div>
              <el-progress :percentage="45" />
            </div>
            <div class="progress-item">
              <div class="progress-label">项目实战</div>
              <el-progress :percentage="80" />
            </div>
          </div>
        </el-card>
      </el-col>
      <el-col :span="8">
        <el-card class="content-card">
          <template #header>
            <div class="card-header">
              <span>快速入口</span>
            </div>
          </template>
          <div class="quick-actions">
            <el-button type="primary" @click="$router.push('/practice')">
              开始练习
            </el-button>
            <el-button type="success" @click="$router.push('/projects')">
              项目实战
            </el-button>
            <el-button type="warning" @click="$router.push('/report')">
              查看报告
            </el-button>
            <el-button type="info" @click="$router.push('/messages')">
              消息中心
            </el-button>
          </div>
        </el-card>
      </el-col>
    </el-row>
  </div>
</template>

<script setup lang="ts">
import { Timer, DocumentChecked, FolderChecked, TrendCharts } from '@element-plus/icons-vue'
</script>

<style scoped>
.dashboard {
  padding: 20px;
}

.stats-row {
  margin-bottom: 20px;
}

.stat-card {
  border-radius: 8px;
  transition: transform 0.3s;
}

.stat-card:hover {
  transform: translateY(-5px);
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.15);
}

.stat-item {
  display: flex;
  align-items: center;
  gap: 20px;
}

.stat-icon {
  width: 60px;
  height: 60px;
  border-radius: 12px;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 30px;
  color: #fff;
}

.stat-icon.study-time {
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
}

.stat-icon.complete-count {
  background: linear-gradient(135deg, #f093fb 0%, #f5576c 100%);
}

.stat-icon.project-count {
  background: linear-gradient(135deg, #4facfe 0%, #00f2fe 100%);
}

.stat-icon.accuracy {
  background: linear-gradient(135deg, #43e97b 0%, #38f9d7 100%);
}

.stat-content {
  flex: 1;
}

.stat-label {
  font-size: 14px;
  color: #666;
  margin-bottom: 8px;
}

.stat-value {
  font-size: 28px;
  font-weight: bold;
  color: #333;
}

.content-row {
  margin-top: 20px;
}

.content-card {
  border-radius: 8px;
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.learning-progress {
  padding: 10px 0;
}

.progress-item {
  margin-bottom: 20px;
}

.progress-item:last-child {
  margin-bottom: 0;
}

.progress-label {
  margin-bottom: 10px;
  font-weight: 500;
}

.quick-actions {
  display: flex;
  flex-direction: column;
  gap: 15px;
  padding: 10px 0;
}

.quick-actions .el-button {
  height: 50px;
  font-size: 16px;
  border-radius: 8px;
}
</style>
