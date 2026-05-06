<template>
  <div class="admin-login-container">
    <div class="login-card">
      <h1>EduAgent 管理后台</h1>
      <p class="subtitle">高等教育个性化学习智能体系统</p>
      <div class="slogan">安全 · 高效 · 智能管理</div>
      <div class="desc">为高校提供数据驱动的学习管理与决策支持</div>

      <el-form :model="form" :rules="rules" ref="formRef" class="login-form">
        <el-form-item prop="username">
          <el-input v-model="form.username" placeholder="管理员账号" prefix-icon="User" size="large" />
        </el-form-item>
        <el-form-item prop="password">
          <el-input v-model="form.password" type="password" placeholder="密码" prefix-icon="Lock" show-password size="large" />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" size="large" @click="handleLogin" :loading="loading" class="login-btn">登录</el-button>
        </el-form-item>
        <el-form-item>
          <el-button type="success" size="large" @click="mockLogin" plain class="mock-btn">模拟登录（跳过验证）</el-button>
        </el-form-item>
      </el-form>

      <div class="other-login">
        <span>其他登录方式</span>
        <div class="icons">
          <el-link :underline="'never'">SSO单点登录</el-link>
          <el-link :underline="'never'">扫码登录</el-link>
        </div>
      </div>
      <div class="footer-links">
        <el-link :underline="'never'">用户协议</el-link> & <el-link :underline="'never'">隐私政策</el-link>
      </div>
      <div class="copyright">© 2025 EduAgent. 保留所有权利。</div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { useUserStore } from '@/stores/user'

const router = useRouter()
const userStore = useUserStore()
const formRef = ref()
const loading = ref(false)

const form = reactive({ username: '', password: '' })
const rules = {
  username: [{ required: true, message: '请输入管理员账号', trigger: 'blur' }],
  password: [{ required: true, message: '请输入密码', trigger: 'blur' }]
}

const handleLogin = async () => {
  await formRef.value.validate()
  loading.value = true
  try {
    // 模拟登录，实际应调用后端接口 /admin/login
    const mockRes = { code: 1, data: { id: 1, username: form.username, name: '系统管理员', token: 'admin-token' } }
    if (mockRes.code === 1) {
      userStore.setToken(mockRes.data.token)
      userStore.setUserInfo(mockRes.data)
      ElMessage.success('登录成功')
      router.push('/admin/dashboard')
    } else {
      ElMessage.error('账号或密码错误')
    }
  } catch (error) {
    ElMessage.error('登录失败')
  } finally {
    loading.value = false
  }
}

// 模拟登录：直接设置 token 并跳转
const mockLogin = () => {
  userStore.setToken('admin-token')
  userStore.setUserInfo({ id: 1, username: 'admin', name: '系统管理员', token: 'admin-token' })
  ElMessage.success('模拟登录成功')
  router.push('/admin/dashboard')
}
</script>

<style scoped>
.admin-login-container {
  display: flex;
  justify-content: center;
  align-items: center;
  height: 100vh;
  background: linear-gradient(135deg, #1e2a3a 0%, #0f1722 100%);
}
.login-card {
  width: 480px;
  background: #fff;
  border-radius: 16px;
  padding: 40px 32px;
  text-align: center;
  box-shadow: 0 20px 40px rgba(0,0,0,0.2);
}
h1 {
  font-size: 28px;
  color: #2c3e50;
  margin-bottom: 8px;
}
.subtitle {
  color: #909399;
  font-size: 14px;
  margin-bottom: 20px;
}
.slogan {
  font-size: 18px;
  font-weight: 500;
  color: #409eff;
  margin-bottom: 8px;
}
.desc {
  font-size: 12px;
  color: #606266;
  margin-bottom: 30px;
}
.login-form {
  margin-top: 20px;
}
.login-btn, .mock-btn {
  width: 100%;
}
.mock-btn {
  margin-top: 10px;
}
.other-login {
  margin-top: 30px;
  padding-top: 20px;
  border-top: 1px solid #eee;
  color: #909399;
}
.icons {
  margin-top: 12px;
  display: flex;
  justify-content: center;
  gap: 24px;
}
.footer-links {
  margin-top: 20px;
  font-size: 12px;
}
.copyright {
  margin-top: 20px;
  font-size: 12px;
  color: #c0c4cc;
}
</style>