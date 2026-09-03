<template>
  <el-container class="layout-container">
    <el-aside width="220px" class="aside">
      <div class="logo">EduAgent 管理后台</div>
            <el-menu
        router
        :default-active="$route.path"
      >
        <el-menu-item index="/admin/dashboard">
          <el-icon><DataAnalysis /></el-icon>
          <span>控制台</span>
        </el-menu-item>

        <el-menu-item index="/admin/users">
          <el-icon><User /></el-icon>
          <span>用户管理</span>
        </el-menu-item>

        <el-menu-item index="/admin/ai-govern">
          <el-icon><MagicStick /></el-icon>
          <span>AI 治理</span>
        </el-menu-item>

        <el-menu-item index="/admin/resources">
          <el-icon><FolderOpened /></el-icon>
          <span>资源管理</span>
        </el-menu-item>

        <el-menu-item index="/admin/reviews">
          <el-icon><DocumentChecked /></el-icon>
          <span>内容审核</span>
        </el-menu-item>

        <el-menu-item index="/admin/statistics">
          <el-icon><TrendCharts /></el-icon>
          <span>数据统计</span>
        </el-menu-item>

        <el-menu-item index="/admin/monitor">
          <el-icon><Monitor /></el-icon>
          <span>系统监控</span>
        </el-menu-item>

        <el-menu-item index="/admin/audit">
          <el-icon><Tickets /></el-icon>
          <span>审计日志</span>
        </el-menu-item>

        <el-menu-item index="/admin/settings">
          <el-icon><Setting /></el-icon>
          <span>系统设置</span>
        </el-menu-item>
      </el-menu>
    </el-aside>
    <el-container>
      <el-header class="header">
        <el-dropdown @command="handleCommand">
          <div class="user-dropdown-trigger">
            <span class="user-name">{{
                userStore.userInfo?.realName ||
                userStore.userInfo?.name ||
                '管理员'
              }}</span>
            <el-icon class="el-icon--right"><ArrowDown /></el-icon>
          </div>
          <template #dropdown>
            <el-dropdown-menu>
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
  await router.push('/admin/login')
  ElMessage.success('已退出登录')
}

</script>

<style scoped>
.layout-container { height: 100vh; }
.aside { background-color: var(--brand-navy); }
.logo { height: 60px; line-height: 60px; text-align: center; color: var(--on-dark); font-size: 18px; font-weight: 600; letter-spacing: -0.3px; }
.header { background-color: var(--canvas); border-bottom: 1px solid var(--hairline); display: flex; justify-content: flex-end; align-items: center; padding: 0 var(--space-lg); }
.user-dropdown-trigger { display: flex; align-items: center; cursor: pointer; gap: var(--space-xs); padding: var(--space-xs) 0; }
.user-name { font: var(--text-sm-medium); color: var(--charcoal); }

/* Override Element Plus menu styles */
.aside .el-menu { background: var(--brand-navy) !important; border-right: none !important; }
.aside .el-menu-item { background: transparent !important; color: var(--on-dark-muted) !important; border-radius: var(--radius-sm); margin: 2px var(--space-xs) !important; font: var(--text-sm-medium) !important; }
.aside .el-menu-item:hover { background: rgba(255,255,255,0.06) !important; color: var(--on-dark) !important; }
.aside .el-menu-item.is-active { background: var(--primary) !important; color: var(--on-dark) !important; }
.aside .el-menu-item .el-icon { color: inherit !important; }
</style>
