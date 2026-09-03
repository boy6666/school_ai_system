<template>
  <el-container class="layout-container">
    <el-aside width="220px" class="aside">
      <div class="logo">EduAgent 教师端</div>

      <el-menu
        router
        :default-active="$route.path"
      >
        <el-menu-item index="/teacher/dashboard">
          <el-icon><HomeFilled /></el-icon>
          <span>教师首页</span>
        </el-menu-item>

        <el-menu-item index="/teacher/classes">
          <el-icon><UserFilled /></el-icon>
          <span>班级管理</span>
        </el-menu-item>

        <el-menu-item index="/teacher/questions">
          <el-icon><Collection /></el-icon>
          <span>题库管理</span>
        </el-menu-item>

        <el-menu-item index="/teacher/assignments">
          <el-icon><Document /></el-icon>
          <span>作业管理</span>
        </el-menu-item>

        <el-menu-item index="/teacher/grades">
          <el-icon><EditPen /></el-icon>
          <span>批改复核</span>
        </el-menu-item>

        <el-menu-item index="/teacher/analytics">
          <el-icon><TrendCharts /></el-icon>
          <span>学情看板</span>
        </el-menu-item>

        <el-menu-item index="/teacher/ai-tutor">
          <el-icon><MagicStick /></el-icon>
          <span>AI 助教</span>
        </el-menu-item>

        <el-menu-item index="/teacher/resources">
          <el-icon><FolderOpened /></el-icon>
          <span>资源管理</span>
        </el-menu-item>
      </el-menu>
    </el-aside>

    <el-container>
      <el-header class="header">
        <el-dropdown @command="handleCommand">
          <div class="user-dropdown-trigger">
            <span class="user-name">
              {{
                userStore.userInfo?.realName ||
                userStore.userInfo?.name ||
                '教师'
              }}
            </span>
            <el-icon class="el-icon--right">
              <ArrowDown />
            </el-icon>
          </div>

          <template #dropdown>
            <el-dropdown-menu>
              <el-dropdown-item command="logout">
                退出登录
              </el-dropdown-item>
            </el-dropdown-menu>
          </template>
        </el-dropdown>
      </el-header>

      <el-main>
        <router-view />
      </el-main>
    </el-container>
  </el-container>
</template>

<script setup lang="ts">
import { ElMessage } from 'element-plus'
import { useRouter } from 'vue-router'
import { useUserStore } from '@/stores/user'
import { logout as logoutRequest } from '@/api/auth'

const router = useRouter()
const userStore = useUserStore()

const handleCommand = async (
  command: string
) => {
  if (command !== 'logout') {
    return
  }

  try {
    await logoutRequest()
  } catch {
    // 服务端退出失败时仍清理本地状态
  }

  userStore.logout()
  await router.push('/login')
  ElMessage.success('已退出登录')
}
</script>

<style scoped>
.layout-container {
  height: 100vh;
}

.aside {
  background-color: var(--brand-navy);
}

.logo {
  height: 60px;
  line-height: 60px;
  text-align: center;
  color: var(--on-dark);
  font-size: 18px;
  font-weight: 600;
  letter-spacing: -0.3px;
}

.header {
  display: flex;
  align-items: center;
  justify-content: flex-end;
  padding: 0 var(--space-lg);
  background-color: var(--canvas);
  border-bottom: 1px solid var(--hairline);
}

.user-dropdown-trigger {
  display: flex;
  align-items: center;
  gap: var(--space-xs);
  padding: var(--space-xs) 0;
  cursor: pointer;
}

.user-name {
  color: var(--charcoal);
  font: var(--text-sm-medium);
}

.el-main {
  padding: 0 !important;
  background: var(--surface) !important;
}

.aside .el-menu {
  background: var(--brand-navy) !important;
  border-right: none !important;
}

.aside .el-menu-item {
  margin: 2px var(--space-xs) !important;
  color: var(--on-dark-muted) !important;
  font: var(--text-sm-medium) !important;
  background: transparent !important;
  border-radius: var(--radius-sm) !important;
}

.aside .el-menu-item:hover {
  color: var(--on-dark) !important;
  background: rgba(255, 255, 255, 0.06) !important;
}

.aside .el-menu-item.is-active {
  color: var(--on-dark) !important;
  background: var(--primary) !important;
}

.aside .el-menu-item .el-icon {
  color: inherit !important;
}
</style>