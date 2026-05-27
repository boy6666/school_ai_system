<template>
  <el-container class="layout-container">
    <el-aside width="200px" class="aside">
      <div class="logo">EduAgent</div>
      <el-menu
        :default-active="$route.path"
        router
        background-color="#304156"
        text-color="#bfcbd9"
        active-text-color="#409EFF"
      >
        <el-menu-item index="/student/dashboard">
          <el-icon><HomeFilled /></el-icon><span>首页</span>
        </el-menu-item>
        <el-menu-item index="/student/profile/chat">
          <el-icon><ChatDotRound /></el-icon><span>学习画像</span>
        </el-menu-item>
        <el-menu-item index="/student/resources">
          <el-icon><MagicStick /></el-icon><span>资源生成</span>
        </el-menu-item>
        <el-menu-item index="/student/path">
          <el-icon><Guide /></el-icon><span>学习路径</span>
        </el-menu-item>
        <el-menu-item index="/student/tutor">
          <el-icon><Service /></el-icon><span>智能辅导</span>
        </el-menu-item>
        <el-menu-item index="/student/report">
          <el-icon><DataAnalysis /></el-icon><span>学习报告</span>
        </el-menu-item>
      </el-menu>
    </el-aside>
    <el-container>
      <el-header class="header">
        <el-dropdown @command="handleCommand">
          <div class="user-dropdown-trigger">
            <span class="user-name">{{ userStore.userInfo?.name || '学生' }}</span>
            <el-icon class="el-icon--right"><ArrowDown /></el-icon>
          </div>
          <template #dropdown>
            <el-dropdown-menu>
              <el-dropdown-item command="profile">个人中心</el-dropdown-item>
              <el-dropdown-item command="logout">退出登录</el-dropdown-item>
            </el-dropdown-menu>
          </template>
        </el-dropdown>
      </el-header>
      <el-main><router-view /></el-main>
    </el-container>
  </el-container>
</template>

<script setup lang="ts">
import { useRouter } from 'vue-router'
import { useUserStore } from '@/stores/user'
import { ElMessage } from 'element-plus'

const router = useRouter()
const userStore = useUserStore()

const handleCommand = (command: string) => {
  if (command === 'profile') {
    router.push('/student/profile')
  } else if (command === 'logout') {
    userStore.logout()
    router.push('/login')
    ElMessage.success('已退出登录')
  }
}
</script>

<style scoped>
.layout-container { height: 100vh; }
.aside { background-color: #304156; }
.logo { height: 60px; line-height: 60px; text-align: center; color: white; font-size: 20px; font-weight: bold; }
.header { background-color: #fff; border-bottom: 1px solid #e6e6e6; display: flex; justify-content: flex-end; align-items: center; padding: 0 20px; }
.user-dropdown-trigger { display: flex; align-items: center; cursor: pointer; }
.user-name { margin-right: 5px; }
</style>
