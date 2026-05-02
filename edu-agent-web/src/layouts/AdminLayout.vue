<template>
  <el-container class="admin-layout">
    <el-aside width="250px" class="sidebar">
      <div class="logo">
        <h1>EduAgent</h1>
        <p>管理后台</p>
      </div>
      <el-menu
        :default-active="activeMenu"
        router
        class="sidebar-menu"
      >
        <el-menu-item index="/admin/dashboard">
          <el-icon><DataLine /></el-icon>
          <span>管理首页</span>
        </el-menu-item>
        <el-menu-item index="/admin/statistics">
          <el-icon><TrendCharts /></el-icon>
          <span>数据统计与分析</span>
        </el-menu-item>
        <el-menu-item index="/admin/settings">
          <el-icon><Setting /></el-icon>
          <span>系统设置</span>
        </el-menu-item>
      </el-menu>
    </el-aside>
    <el-container class="main-container">
      <el-header class="header">
        <div class="header-left">
          <el-breadcrumb separator="/">
            <el-breadcrumb-item :to="{ path: '/admin/dashboard' }">首页</el-breadcrumb-item>
            <el-breadcrumb-item>{{ currentTitle }}</el-breadcrumb-item>
          </el-breadcrumb>
        </div>
        <div class="header-right">
          <el-dropdown @command="handleCommand">
            <span class="user-info">
              <el-avatar :size="32" :src="userInfo?.avatar || ''">{{ userInfo?.name?.charAt(0) }}</el-avatar>
              <span class="username">{{ userInfo?.name }}</span>
            </span>
            <template #dropdown>
              <el-dropdown-menu>
                <el-dropdown-item command="logout">退出登录</el-dropdown-item>
              </el-dropdown-menu>
            </template>
          </el-dropdown>
        </div>
      </el-header>
      <el-main class="main-content">
        <router-view />
      </el-main>
    </el-container>
  </el-container>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useUserStore } from '@/stores/user'
import { DataLine, TrendCharts, Setting } from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'

const route = useRoute()
const router = useRouter()
const userStore = useUserStore()

const activeMenu = computed(() => route.path)
const userInfo = computed(() => userStore.userInfo)
const currentTitle = computed(() => route.meta.title)

const handleCommand = (command: string) => {
  if (command === 'logout') {
    userStore.logout()
    ElMessage.success('退出登录成功')
    router.push('/login')
  }
}
</script>

<style scoped>
.admin-layout {
  min-height: 100vh;
}

.sidebar {
  background: #2c3e50;
  color: #fff;
  overflow: hidden;
}

.logo {
  padding: 20px;
  text-align: center;
  border-bottom: 1px solid rgba(255, 255, 255, 0.1);
}

.logo h1 {
  margin: 0;
  font-size: 24px;
  color: #fff;
}

.logo p {
  margin: 5px 0 0 0;
  font-size: 12px;
  color: rgba(255, 255, 255, 0.6);
}

.sidebar-menu {
  border-right: none;
  background: transparent;
}

.sidebar-menu .el-menu-item {
  color: rgba(255, 255, 255, 0.7);
}

.sidebar-menu .el-menu-item:hover,
.sidebar-menu .el-menu-item.is-active {
  background: rgba(64, 158, 255, 0.2);
  color: #fff;
}

.main-container {
  background: #f5f7fa;
}

.header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 0 20px;
  background: #fff;
  border-bottom: 1px solid #e6e6e6;
  box-shadow: 0 2px 4px rgba(0, 0, 0, 0.05);
}

.user-info {
  cursor: pointer;
  display: flex;
  align-items: center;
  gap: 10px;
}

.main-content {
  padding: 20px;
}
</style>
