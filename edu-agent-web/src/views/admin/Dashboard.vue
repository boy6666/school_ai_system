<template>
  <div class="admin-dashboard">
    <!-- 统计卡片 7 3 9 2（示例数据） -->
    <el-row :gutter="20" class="stats-row">
      <el-col :span="6">
        <el-card shadow="hover" class="stat-card">
          <div class="stat-content">
            <div class="stat-number">7</div>
            <div class="stat-title">今日访问</div>
          </div>
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card shadow="hover" class="stat-card">
          <div class="stat-content">
            <div class="stat-number">3</div>
            <div class="stat-title">待审批</div>
          </div>
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card shadow="hover" class="stat-card">
          <div class="stat-content">
            <div class="stat-number">9</div>
            <div class="stat-title">新用户</div>
          </div>
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card shadow="hover" class="stat-card">
          <div class="stat-content">
            <div class="stat-number">2</div>
            <div class="stat-title">待处理报告</div>
          </div>
        </el-card>
      </el-col>
    </el-row>

    <el-row :gutter="20">
      <!-- 快捷入口 -->
      <el-col :span="16">
        <el-card shadow="never" class="quick-card">
          <template #header><span>快捷入口</span></template>
          <div class="quick-links">
            <div v-for="item in quickLinks" :key="item.name" class="quick-item" @click="goTo(item.path)">
              <el-icon :size="28"><component :is="item.icon" /></el-icon>
              <span>{{ item.name }}</span>
            </div>
          </div>
        </el-card>
      </el-col>
      <!-- 系统公告 -->
      <el-col :span="8">
        <el-card shadow="never" class="notice-card">
          <template #header><span>系统公告</span><el-button type="text" style="float: right">更多</el-button></template>
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
import { useRouter } from 'vue-router'
import {
  User, Monitor, Upload, DataAnalysis, Checked, Setting, Tickets
} from '@element-plus/icons-vue'

const router = useRouter()
const quickLinks = [
  { name: '用户管理', path: '/admin/users', icon: 'User' },
  { name: '课程管理', path: '/admin/courses', icon: 'Monitor' },
  { name: '资源上传', path: '/admin/resources', icon: 'Upload' },
  { name: '数据报表', path: '/admin/data-report', icon: 'DataAnalysis' },
  { name: '审批中心', path: '/admin/approvals', icon: 'Checked' },
  { name: '系统设置', path: '/admin/settings', icon: 'Setting' },
  { name: '日志审计', path: '/admin/logs', icon: 'Tickets' }
]
const notices = [
  { id: 1, title: '关于系统维护的通知', time: '2024-05-15' },
  { id: 2, title: '新功能上线：智能体管理', time: '2024-05-14' },
  { id: 3, title: '平台使用规范更新', time: '2024-05-12' }
]
const goTo = (path: string) => router.push(path)
</script>

<style scoped>
.admin-dashboard { padding: 20px; background-color: #f5f7fa; min-height: 100vh; }
.stats-row { margin-bottom: 20px; }
.stat-card { border-radius: 12px; text-align: center; padding: 20px 0; }
.stat-number { font-size: 36px; font-weight: bold; color: #409eff; }
.stat-title { font-size: 14px; color: #909399; margin-top: 8px; }
.quick-card, .notice-card { border-radius: 12px; }
.quick-links { display: flex; flex-wrap: wrap; gap: 24px; }
.quick-item { width: 80px; text-align: center; cursor: pointer; transition: transform 0.2s; }
.quick-item:hover { transform: translateY(-4px); }
.quick-item span { display: block; margin-top: 8px; font-size: 14px; }
.notice-list { max-height: 300px; overflow-y: auto; }
.notice-item { padding: 12px 0; border-bottom: 1px solid #eee; cursor: pointer; }
.notice-item:hover { background: #f5f7fa; }
.notice-title { font-size: 14px; margin-bottom: 6px; }
.notice-time { font-size: 12px; color: #909399; }
</style>