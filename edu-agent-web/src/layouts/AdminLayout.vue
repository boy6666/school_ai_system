<template>
  <el-container class="layout-container">
    <el-aside width="220px" class="aside">
      <div class="logo">EduAgent 管理后台</div>
      <el-menu
        :default-active="$route.path"
        router
        background-color="#304156"
        text-color="#bfcbd9"
        active-text-color="#409EFF"
      >
        <el-menu-item index="/admin/dashboard">
          <el-icon><HomeFilled /></el-icon><span>首页</span>
        </el-menu-item>
        <el-menu-item index="/admin/users">
          <el-icon><User /></el-icon><span>用户管理</span>
        </el-menu-item>
        <el-menu-item index="/admin/courses">
          <el-icon><Monitor /></el-icon><span>课程/资源</span>
        </el-menu-item>
        <el-menu-item index="/admin/learning-analysis">
          <el-icon><DataAnalysis /></el-icon><span>学习分析</span>
        </el-menu-item>
        <el-menu-item index="/admin/data-report">
          <el-icon><Document /></el-icon><span>数据报表</span>
        </el-menu-item>
        <el-menu-item index="/admin/agents">
          <el-icon><Cpu /></el-icon><span>智能体管理</span>
        </el-menu-item>
        <el-menu-item index="/admin/approvals">
          <el-icon><Checked /></el-icon><span>审批管理</span>
        </el-menu-item>
        <el-menu-item index="/admin/settings">
          <el-icon><Setting /></el-icon><span>系统设置</span>
        </el-menu-item>
        <el-menu-item index="/admin/logs">
          <el-icon><Tickets /></el-icon><span>日志审计</span>
        </el-menu-item>
      </el-menu>
    </el-aside>
    <el-container>
      <el-header class="header">
        <!-- 修复 el-dropdown：默认插槽用一个 div 包裹，下拉菜单使用 #dropdown 插槽 -->
        <el-dropdown @command="handleCommand">
          <div class="user-dropdown-trigger">
            <span class="user-name">{{ userStore.userInfo?.name || '管理员' }}</span>
            <el-icon class="el-icon--right"><ArrowDown /></el-icon>
          </div>
          <template #dropdown>
            <el-dropdown-menu>
              <el-dropdown-item command="logout">退出登录</el-dropdown-item>
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
import { useRouter } from 'vue-router'
import { useUserStore } from '@/stores/user'
import { ElMessage } from 'element-plus'

const router = useRouter()
const userStore = useUserStore()

const handleCommand = (command: string) => {
  if (command === 'logout') {
    userStore.logout()
    router.push('/admin/login')
    ElMessage.success('已退出登录')
  }
}
</script>

<style scoped>
.layout-container {
  height: 100vh;
}
.aside {
  background-color: #304156;
}
.logo {
  height: 60px;
  line-height: 60px;
  text-align: center;
  color: white;
  font-size: 20px;
  font-weight: bold;
}
.header {
  background-color: #fff;
  border-bottom: 1px solid #e6e6e6;
  display: flex;
  justify-content: flex-end;
  align-items: center;
  padding: 0 20px;
}
.user-dropdown-trigger {
  display: flex;
  align-items: center;
  cursor: pointer;
}
.user-name {
  margin-right: 5px;
}
</style>