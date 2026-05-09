<template>
  <div class="admin-dashboard">
    <el-row :gutter="20" class="stats-row">
      <el-col :span="6">
        <el-card class="stat-card">
          <div class="stat-item">
            <div class="stat-icon users">
              <el-icon><User /></el-icon>
            </div>
            <div class="stat-content">
              <div class="stat-label">用户总数</div>
              <div class="stat-value">1,234</div>
            </div>
          </div>
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card class="stat-card">
          <div class="stat-item">
            <div class="stat-icon online">
              <el-icon><UserFilled /></el-icon>
            </div>
            <div class="stat-content">
              <div class="stat-label">在线用户</div>
              <div class="stat-value">156</div>
            </div>
          </div>
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card class="stat-card">
          <div class="stat-item">
            <div class="stat-icon resources">
              <el-icon><Document /></el-icon>
            </div>
            <div class="stat-content">
              <div class="stat-label">资源总数</div>
              <div class="stat-value">567</div>
            </div>
          </div>
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card class="stat-card">
          <div class="stat-item">
            <div class="stat-icon exercises">
              <el-icon><Files /></el-icon>
            </div>
            <div class="stat-content">
              <div class="stat-label">练习完成</div>
              <div class="stat-value">8,901</div>
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
              <span>最近活动</span>
              <el-button link type="primary">查看全部</el-button>
            </div>
          </template>
          <el-table :data="recentActivities" style="width: 100%">
            <el-table-column prop="user" label="用户" width="150" />
            <el-table-column prop="action" label="操作" width="200" />
            <el-table-column prop="time" label="时间" />
          </el-table>
        </el-card>
      </el-col>
      <el-col :span="8">
        <el-card class="content-card">
          <template #header>
            <div class="card-header">
              <span>系统状态</span>
            </div>
          </template>
          <div class="system-status">
            <div class="status-item">
              <el-icon class="success"><SuccessFilled /></el-icon>
              <span>数据库连接正常</span>
            </div>
            <div class="status-item">
              <el-icon class="success"><SuccessFilled /></el-icon>
              <span>Redis 服务正常</span>
            </div>
            <div class="status-item">
              <el-icon class="success"><SuccessFilled /></el-icon>
              <span>API 服务正常</span>
            </div>
            <div class="status-item">
              <el-icon class="warning"><WarningFilled /></el-icon>
              <span>存储空间使用 75%</span>
            </div>
          </div>
        </el-card>
      </el-col>
    </el-row>
  </div>
</template>

<script setup lang="ts">
import { ref } from 'vue'
import { User, UserFilled, Document, Files, SuccessFilled, WarningFilled } from '@element-plus/icons-vue'

const recentActivities = ref([
  { user: '张三', action: '完成练习 - Vue基础', time: '2026-05-02 14:30' },
  { user: '李四', action: '提交项目 - 电商系统', time: '2026-05-02 14:25' },
  { user: '王五', action: '查看学习报告', time: '2026-05-02 14:20' },
  { user: '赵六', action: '开始新练习', time: '2026-05-02 14:15' }
])
</script>

<style scoped>
.admin-dashboard {
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

.stat-icon.users {
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
}

.stat-icon.online {
  background: linear-gradient(135deg, #f093fb 0%, #f5576c 100%);
}

.stat-icon.resources {
  background: linear-gradient(135deg, #4facfe 0%, #00f2fe 100%);
}

.stat-icon.exercises {
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

.system-status {
  padding: 10px 0;
}

.status-item {
  display: flex;
  align-items: center;
  gap: 10px;
  margin-bottom: 15px;
  padding: 10px;
  background: #f5f7fa;
  border-radius: 6px;
}

.status-item:last-child {
  margin-bottom: 0;
}

.status-item .success {
  color: #67c23a;
  font-size: 20px;
}

.status-item .warning {
  color: #e6a23c;
  font-size: 20px;
}
</style>
