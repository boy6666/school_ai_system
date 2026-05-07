<template>
  <div class="register-container">
    <el-card class="register-card" shadow="hover">
      <h2 class="title">选择您的身份</h2>
      <p class="subtitle">请选择您的身份以继续使用平台</p>

      <div class="identity-options">
        <div class="identity-card" @click="selectIdentity('student')">
          <div class="icon-wrapper"><el-icon :size="48"><User /></el-icon></div>
          <h3>我是学生</h3>
          <p>个性化学习与资源推荐</p>
        </div>

        <div class="identity-card" @click="selectIdentity('admin')">
          <div class="icon-wrapper"><el-icon :size="48"><Management /></el-icon></div>
          <h3>我是管理员</h3>
          <p>管理平台与资源推荐</p>
        </div>
      </div>

      <div class="footer">
        <el-link type="primary" @click="goToLogin">已绑定账号？去登录</el-link>
      </div>
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { ElMessage } from 'element-plus'
import { useRouter } from 'vue-router'

const router = useRouter()

const selectIdentity = (identity: string) => {
  if (identity === 'student') {
    ElMessage.info('学生注册功能开发中，即将开放')
    // TODO: router.push('/register/student')
  } else {
    ElMessage.info('管理员注册功能开发中，即将开放')
    // TODO: router.push('/register/admin')
  }
}

const goToLogin = () => {
  router.push('/login')
}
</script>

<style scoped>
.register-container {
  display: flex;
  justify-content: center;
  align-items: center;
  height: 100vh;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
}
.register-card {
  width: 800px;
  padding: 40px 20px;
  border-radius: 16px;
  text-align: center;
}
.title {
  font-size: 28px;
  font-weight: bold;
  margin-bottom: 12px;
  color: #303133;
}
.subtitle {
  font-size: 14px;
  color: #909399;
  margin-bottom: 40px;
}
.identity-options {
  display: flex;
  justify-content: center;
  gap: 40px;
  margin-bottom: 48px;
  flex-wrap: wrap;
}
.identity-card {
  width: 240px;
  padding: 32px 16px;
  border: 1px solid #ebeef5;
  border-radius: 16px;
  cursor: pointer;
  transition: all 0.3s ease;
  background-color: #fff;
}
.identity-card:hover {
  transform: translateY(-6px);
  box-shadow: 0 12px 24px rgba(0, 0, 0, 0.1);
  border-color: #409eff;
}
.icon-wrapper {
  width: 80px;
  height: 80px;
  margin: 0 auto 20px;
  background: #eef2ff;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  color: #409eff;
}
.identity-card h3 {
  font-size: 20px;
  font-weight: 600;
  margin-bottom: 12px;
  color: #303133;
}
.identity-card p {
  font-size: 14px;
  color: #606266;
  margin: 0;
}
.footer {
  margin-top: 20px;
}
</style>
