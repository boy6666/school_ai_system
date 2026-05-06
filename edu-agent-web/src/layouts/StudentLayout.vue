<template>
  <el-container class="student-layout">
    <el-header class="header">
      <div class="header-left">
        <h1 class="logo">EduAgent</h1>
        <el-menu
          :default-active="activeMenu"
          mode="horizontal"
          router
          class="nav-menu"
        >
          <el-menu-item index="/dashboard">首页</el-menu-item>
          <el-menu-item index="/practice">练习/题库</el-menu-item>
          <el-menu-item index="/projects">实操/项目</el-menu-item>
          <el-menu-item index="/report">学习报告</el-menu-item>
          <el-menu-item index="/messages">消息中心</el-menu-item>
        </el-menu>
      </div>
      <div class="header-right">
        <el-badge :value="messageCount" class="message-badge">
          <el-button circle @click="$router.push('/messages')">
            <el-icon><Bell /></el-icon>
          </el-button>
        </el-badge>
        <el-dropdown @command="handleCommand">
          <span class="user-info">
            <el-avatar :size="32" :src="userInfo?.avatar || ''">{{ userInfo?.name?.charAt(0) }}</el-avatar>
          </span>
          <template #dropdown>
            <el-dropdown-menu>
              <el-dropdown-item command="profile">个人中心</el-dropdown-item>
              <el-dropdown-item command="settings">设置</el-dropdown-item>
              <el-dropdown-item divided command="logout">退出登录</el-dropdown-item>
            </el-dropdown-menu>
          </template>
        </el-dropdown>
      </div>
    </el-header>
    <el-main class="main-content">
      <router-view />
    </el-main>
  </el-container>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useUserStore } from '@/stores/user'
import { Bell } from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'

const route = useRoute()
const router = useRouter()
const userStore = useUserStore()

const activeMenu = computed(() => route.path)
const userInfo = computed(() => userStore.userInfo)
const messageCount = computed(() => 3)

const handleCommand = (command: string) => {
  switch (command) {
    case 'profile':
      router.push('/profile')
      break
    case 'settings':
      router.push('/settings')
      break
    case 'logout':
      userStore.logout()
      ElMessage.success('退出登录成功')
      router.push('/login')
      break
  }
}
</script>

<style scoped>
.student-layout {
  min-height: 100vh;
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

.header-left {
  display: flex;
  align-items: center;
  gap: 30px;
}

.logo {
  font-size: 24px;
  font-weight: bold;
  color: #409eff;
  margin: 0;
}

.nav-menu {
  border-bottom: none;
}

.header-right {
  display: flex;
  align-items: center;
  gap: 15px;
}

.user-info {
  cursor: pointer;
  display: flex;
  align-items: center;
}

.main-content {
  padding: 20px;
  background: #f5f7fa;
}
</style>
