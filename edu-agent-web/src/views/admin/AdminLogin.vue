<template>
  <div class="admin-login-container">
    <div class="login-card">
      <h1>EduAgent 管理后台</h1>
      <p class="subtitle">高等教育个性化学习智能体系统</p>

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
      </el-form>

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
import request from '@/utils/request'

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
    const res = await request.post('/auth/login', form)
    if (res.token) {
      userStore.setToken(res.token)
      userStore.setUserInfo(res.userInfo || {})
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
</script>

<style scoped>
.admin-login-container { min-height: 100vh; display: flex; align-items: center; justify-content: center; background: linear-gradient(135deg, var(--brand-navy-deep) 0%, var(--brand-navy) 100%); }
.login-card { width: 420px; background: var(--canvas); border-radius: var(--radius-xl); padding: var(--space-xxxl) var(--space-xxl); text-align: center; box-shadow: var(--shadow-modal); }
h1 { font: var(--text-h2); color: var(--ink); margin: 0 0 var(--space-xxs); }
.subtitle { color: var(--steel); font: var(--text-sm); margin: 0 0 var(--space-xl); }
.login-form { margin-top: var(--space-md); }
.login-btn { width: 100%; }
.footer-links { margin-top: var(--space-xl); font-size: 12px; color: var(--steel); }
.copyright { margin-top: var(--space-md); font-size: 12px; color: var(--muted); }
</style>
