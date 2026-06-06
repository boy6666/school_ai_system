<template>
  <div class="login-container">
    <el-card class="login-card" shadow="hover">
      <h2 class="brand-title">EduAgent 个性化学习平台</h2>
      <el-form :model="form" :rules="rules" ref="formRef">
        <el-form-item prop="username">
          <el-input v-model="form.username" placeholder="账号/学号/邮箱" prefix-icon="User" size="large" />
        </el-form-item>
        <el-form-item prop="password">
          <el-input v-model="form.password" type="password" placeholder="密码" prefix-icon="Lock" show-password size="large" />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" size="large" @click="handleLogin" :loading="loading" class="login-btn">登录</el-button>
        </el-form-item>
        <el-form-item>
          <el-link type="primary" @click="$router.push('/register')">还没有账号？去注册</el-link>
          <el-link type="info" style="float: right">忘记密码？</el-link>
        </el-form-item>
      </el-form>
    </el-card>
  </div>
</template>

<script setup>
import { reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { useRouter } from 'vue-router'
import { useUserStore } from '@/stores/user'
import request from '@/utils/request'

const router = useRouter()
const userStore = useUserStore()
const formRef = ref()
const loading = ref(false)

const form = reactive({ username: '', password: '' })
const rules = {
  username: [{ required: true, message: '请输入账号', trigger: 'blur' }],
  password: [{ required: true, message: '请输入密码', trigger: 'blur' }]
}

const handleLogin = async () => {
  await formRef.value.validate()
  loading.value = true
  try {
    const res = await request.post('/auth/login', form)
    if (res.token) {
      userStore.setToken(res.token)
      userStore.setUserInfo(res.userInfo || {})
      // 从后端确认引导状态
      if (res.userInfo?.onboarded === 1) {
        localStorage.setItem('tutor_init_done', '1')
      }
      ElMessage.success('登录成功')
      const role = res?.userInfo?.role || ''
      if (role === 'admin') {
        router.push('/admin/dashboard')
      } else {
        router.push('/student/dashboard')
      }
    }
  } catch (error) {
    ElMessage.error(error.response?.data?.message || '登录失败，请检查网络')
  } finally {
    loading.value = false
  }
}
</script>

<style scoped>
.login-container {
  display: flex;
  justify-content: center;
  align-items: center;
  height: 100vh;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
}
.login-card {
  width: 480px;
  padding: 32px 24px;
  border-radius: 20px;
  background: rgba(255,255,255,0.96);
  backdrop-filter: blur(2px);
  box-shadow: 0 20px 35px rgba(0,0,0,0.2);
}
.brand-title {
  text-align: center;
  font-size: 28px;
  font-weight: bold;
  margin-bottom: 28px;
  background: linear-gradient(135deg, #3a1c71, #d76d77, #ffaf7b);
  -webkit-background-clip: text;
  background-clip: text;
  color: transparent;
}
.login-btn {
  width: 100%;
  background: linear-gradient(135deg, #667eea, #764ba2);
  border: none;
}
.login-btn:hover {
  opacity: 0.9;
}
</style>