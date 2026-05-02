<template>
  <div class="admin-dashboard">
    <!-- 统计卡片 -->
    <el-row :gutter="20" class="stats-row">
      <el-col :span="6" v-for="stat in stats" :key="stat.title">
        <el-card shadow="hover" class="stat-card">
          <div class="stat-content">
            <div class="stat-icon" :style="{ background: stat.bgColor }">
              <el-icon :size="32"><component :is="stat.icon" /></el-icon>
            </div>
            <div class="stat-info">
              <div class="stat-value">{{ stat.value }}</div>
              <div class="stat-title">{{ stat.title }}</div>
            </div>
          </div>
        </el-card>
      </el-col>
    </el-row>

    <!-- 快捷入口 + 系统公告 -->
    <el-row :gutter="20">
      <el-col :span="16">
        <el-card shadow="never" class="quick-card">
          <template #header>
            <span>快捷入口</span>
          </template>
          <div class="quick-links">
            <div
              v-for="item in quickLinks"
              :key="item.name"
              class="quick-item"
              @click="goTo(item.path)"
            >
              <el-icon :size="28"><component :is="item.icon" /></el-icon>
              <span>{{ item.name }}</span>
            </div>
          </div>
        </el-card>
      </el-col>
      <el-col :span="8">
        <el-card shadow="never" class="notice-card">
          <template #header>
            <span>系统公告</span>
            <el-button type="text" style="float: right">更多</el-button>
          </template>
          <div class="notice-list">
            <div v-for="notice in notices" :key="notice.id" class="notice-item">
              <div class="notice-title">{{ notice.title }}</div>
              <div class="notice-time">{{ notice.time }}</div>
            </div>
          </div>
        </el-card>
      </el-col>
    </el-row>
  </div>
</template>

<script setup lang="ts">
import { ref } from 'vue'
import { useRouter } from 'vue-router'
import {
  User,
  Document,
  Picture,
  Cpu,
  DataAnalysis,
  Setting,
  Checked,
  Notification
} from '@element-plus/icons-vue'

const router = useRouter()

// 统计数据 Mock
const stats = ref([
  { title: '用户总数', value: '1,256', icon: 'User', bgColor: '#409EFF' },
  { title: '课程总数', value: '32', icon: 'Document', bgColor: '#67C23A' },
  { title: '资源总数', value: '128', icon: 'Picture', bgColor: '#E6A23C' },
  { title: '智能体数量', value: '8', icon: 'Cpu', bgColor: '#F56C6C' }
])

// 快捷入口
const quickLinks = ref([
  { name: '用户管理', path: '/admin/users', icon: 'User' },
  { name: '课程管理', path: '/admin/courses', icon: 'Document' },
  { name: '资源上传', path: '/admin/resources', icon: 'Upload' },
  { name: '数据报表', path: '/admin/statistics', icon: 'DataAnalysis' },
  { name: '审批中心', path: '/admin/reviews', icon: 'Checked' },
  { name: '系统设置', path: '/admin/settings', icon: 'Setting' },
  { name: '日志审计', path: '/admin/logs', icon: 'Notification' }
])

// 系统公告 Mock
const notices = ref([
  { id: 1, title: '关于系统维护的通知', time: '2024-05-15' },
  { id: 2, title: '新功能上线：智能体管理', time: '2024-05-14' },
  { id: 3, title: '平台使用规范更新', time: '2024-05-12' }
])

const goTo = (path: string) => {
  router.push(path)
}
</script>

<style scoped>
.admin-dashboard {
  padding: 20px;
  background-color: #f5f7fa;
  min-height: 100vh;
}
.stats-row {
  margin-bottom: 20px;
}
.stat-card {
  border-radius: 12px;
}
.stat-content {
  display: flex;
  align-items: center;
  justify-content: space-between;
}
.stat-icon {
  width: 60px;
  height: 60px;
  border-radius: 12px;
  display: flex;
  align-items: center;
  justify-content: center;
  color: white;
}
.stat-info {
  text-align: right;
}
.stat-value {
  font-size: 28px;
  font-weight: bold;
  margin-bottom: 8px;
}
.stat-title {
  font-size: 14px;
  color: #909399;
}
.quick-card,
.notice-card {
  border-radius: 12px;
}
.quick-links {
  display: flex;
  flex-wrap: wrap;
  gap: 20px;
}
.quick-item {
  width: 80px;
  text-align: center;
  cursor: pointer;
  transition: all 0.3s;
}
.quick-item:hover {
  transform: translateY(-5px);
}
.quick-item span {
  display: block;
  margin-top: 8px;
  font-size: 14px;
}
.notice-list {
  max-height: 300px;
  overflow-y: auto;
}
.notice-item {
  padding: 12px 0;
  border-bottom: 1px solid #ebeef5;
  cursor: pointer;
}
.notice-item:hover {
  background-color: #f5f7fa;
}
.notice-title {
  font-size: 14px;
  margin-bottom: 6px;
}
.notice-time {
  font-size: 12px;
  color: #909399;
}
</style>
