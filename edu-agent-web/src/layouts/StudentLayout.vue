<template>
  <!-- 引导层：新用户先完成画像采集 -->
  <OnboardOverlay v-if="showOnboard" @done="onOnboardDone" />

  <!-- 主系统 -->
  <el-container class="layout-container" v-else>
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
        <el-menu-item index="/student/resources">
          <el-icon><MagicStick /></el-icon><span>资源中心</span>
        </el-menu-item>
        <el-menu-item index="/student/path">
          <el-icon><Guide /></el-icon><span>学习路径</span>
        </el-menu-item>
        <el-menu-item index="/student/tutor">
          <el-icon><Service /></el-icon><span>智能辅导</span>
        </el-menu-item>
                <el-menu-item index="/student/code-practice">
          <el-icon><EditPen /></el-icon><span>代码练习</span>
        </el-menu-item>
        <el-menu-item index="/student/profile">
          <el-icon><ChatDotRound /></el-icon><span>学习画像</span>
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
            <span class="user-name">{{ userStore.userInfo?.realName || userStore.userInfo?.name || '学生' }}</span>
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
import { computed } from 'vue'
import { useRouter } from 'vue-router'
import { useUserStore } from '@/stores/user'
import { ElMessage } from 'element-plus'
import OnboardOverlay from '@/views/student/OnboardOverlay.vue'

const router = useRouter()
const userStore = useUserStore()

const showOnboard = computed(
  () => userStore.userInfo?.onboarded === 0
)

const onOnboardDone = () => {
  userStore.setOnboarded(1)
}

const handleCommand = (command: string) => {
  if (command === 'profile') {
    router.push('/student/profile/settings')
  } else if (command === 'logout') {
    userStore.logout()
    router.push('/login')
    ElMessage.success('已退出登录')
  }
}
</script>

<style scoped>
.layout-container { height: 100vh; }
.aside { background-color: var(--brand-navy); }
.logo { height: 60px; line-height: 60px; text-align: center; color: var(--on-dark); font-size: 18px; font-weight: 600; letter-spacing: -0.3px; }
.header { background-color: var(--canvas); border-bottom: 1px solid var(--hairline); display: flex; justify-content: flex-end; align-items: center; padding: 0 var(--space-lg); }
.user-dropdown-trigger { display: flex; align-items: center; cursor: pointer; gap: var(--space-xs); padding: var(--space-xs) 0; }
.user-name { font: var(--text-sm-medium); color: var(--charcoal); }

.el-main { background: var(--surface) !important; padding: 0 !important; }

/* Element Plus menu overrides */
.aside .el-menu { background: var(--brand-navy) !important; border-right: none !important; }
.aside .el-menu-item { background: transparent !important; color: var(--on-dark-muted) !important; border-radius: var(--radius-sm) !important; margin: 2px var(--space-xs) !important; font: var(--text-sm-medium) !important; }
.aside .el-menu-item:hover { background: rgba(255,255,255,0.06) !important; color: var(--on-dark) !important; }
.aside .el-menu-item.is-active { background: var(--primary) !important; color: var(--on-dark) !important; }
.aside .el-menu-item .el-icon { color: inherit !important; }
</style>