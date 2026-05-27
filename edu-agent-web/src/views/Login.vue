<template>
  <div class="login-container">
    <div class="login-card">
      <div class="brand">
        <h1>EduAgent</h1>
        <p>个性化学习平台</p>
        <small>让每一次学习都更有方向<br>AI 智能体帮你成长，个性化学习更高效</small>
      </div>

      <el-form :model="form" :rules="rules" ref="formRef" class="login-form">
        <h2>欢迎登录</h2>
        <p class="subtitle">登录 EduAgent 个性化学习平台</p>

        <el-form-item prop="username">
          <el-input
            v-model="form.username"
            placeholder="账号 / 学号 / 邮箱"
            prefix-icon="User"
            size="large"
          />
        </el-form-item>

        <el-form-item prop="password">
          <el-input
            v-model="form.password"
            type="password"
            placeholder="密码"
            prefix-icon="Lock"
            show-password
            size="large"
          />
        </el-form-item>

        <div class="login-options">
          <el-checkbox v-model="remember">记住我</el-checkbox>
          <el-link type="primary" :underline="'never'">忘记密码？</el-link>
        </div>

        <el-form-item>
          <el-button type="primary" size="large" @click="handleLogin" :loading="loading" class="login-btn">
            登录
          </el-button>
        </el-form-item>

        <div class="register-link">
          <span>还没有账号？</span>
          <el-link type="primary" @click="goToRegister">去注册</el-link>
        </div>

        <!-- 开发辅助：模拟登录按钮（上线前可删除） -->
        <el-divider>开发测试</el-divider>
        <el-button type="success" size="large" @click="mockLogin" plain class="mock-btn">
          模拟登录（跳过验证）
        </el-button>
      </el-form>
    </div>
  </div>
</template>

<script setup lang="ts">
import { reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { useUserStore } from '@/stores/user'
import { login } from '@/api/auth'

const router = useRouter()
const userStore = useUserStore()
const formRef = ref()
const loading = ref(false)

const form = reactive({
  username: '',
  password: ''
})

const rules = {
  username: [{ required: true, message: '请输入账号/学号/工号', trigger: 'blur' }],
  password: [{ required: true, message: '请输入密码', trigger: 'blur' }]
}

const handleLogin = async () => {
  if (!formRef.value) return
  await formRef.value.validate(async (valid: boolean) => {
    if (!valid) return
    loading.value = true
    try {
      const res = await login({ username: form.username, password: form.password })
      if (res?.token) {
        userStore.setToken(res.token)
        userStore.setUserInfo(res.userInfo || {})
        ElMessage.success('登录成功')
        const role = res?.userInfo?.role || ''
        if (role === 'admin') {
          router.push('/admin/dashboard')
        } else {
          router.push('/student/dashboard')
        }
      }
    } catch (err: any) {
      ElMessage.error(err?.response?.data?.message || err?.message || '登录失败')
    } finally {
      loading.value = false
    }
  })
}

const goToRegister = () => {
  router.push('/register')
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
  background: #fff;
  border-radius: 20px;
  padding: 40px 32px;
  box-shadow: 0 20px 40px rgba(0,0,0,0.1);
}
.brand {
  text-align: center;
  margin-bottom: 32px;
}
.brand h1 {
  font-size: 32px;
  margin: 0;
  color: #409eff;
}
.brand p {
  font-size: 16px;
  color: #666;
  margin: 8px 0 4px;
}
.brand small {
  color: #999;
  font-size: 12px;
  line-height: 1.5;
  display: inline-block;
}
.login-form h2 {
  font-size: 24px;
  text-align: center;
  margin-bottom: 8px;
}
.subtitle {
  text-align: center;
  color: #909399;
  font-size: 14px;
  margin-bottom: 28px;
}
.login-options {
  display: flex;
  justify-content: space-between;
  margin: -10px 0 20px;
}
.login-btn {
  width: 100%;
}
.register-link {
  text-align: center;
  margin-top: 16px;
  font-size: 14px;
}
.mock-btn {
  width: 100%;
  margin-top: 8px;
}
</style>