<template>
  <div class="register-form-container">
    <el-card class="register-card" shadow="hover">
      <h2 class="title">{{ isStudent ? '学生注册' : '管理员注册' }}</h2>
      <p class="subtitle">创建您的 EduAgent 账号</p>

      <el-form
        ref="formRef"
        :model="form"
        :rules="rules"
        label-width="80px"
        class="register-form"
        @submit.prevent="handleRegister"
      >
        <el-form-item label="用户名" prop="username">
          <el-input v-model="form.username" placeholder="请输入用户名" />
        </el-form-item>

        <el-form-item label="昵称" prop="nickname">
          <el-input v-model="form.nickname" placeholder="请输入昵称" />
        </el-form-item>

        <el-form-item label="密码" prop="password">
          <el-input v-model="form.password" type="password" placeholder="请输入密码" show-password />
        </el-form-item>

        <el-form-item label="确认密码" prop="confirmPassword">
          <el-input v-model="form.confirmPassword" type="password" placeholder="请再次输入密码" show-password />
        </el-form-item>

        <el-form-item label="邮箱" prop="email">
          <el-input v-model="form.email" placeholder="请输入邮箱（选填）" />
        </el-form-item>

        <el-form-item>
          <el-button type="primary" class="submit-btn" :loading="loading" @click="handleRegister">
            注册
          </el-button>
        </el-form-item>
      </el-form>

      <div class="footer">
        <el-link type="primary" @click="goToLogin">已有账号？去登录</el-link>
      </div>
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, computed } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { ElMessage } from 'element-plus'
import { register } from '@/api/auth'

const router = useRouter()
const route = useRoute()
const formRef = ref()
const loading = ref(false)

const isStudent = computed(() => route.query.role !== 'admin')

const form = reactive({
  username: '',
  nickname: '',
  password: '',
  confirmPassword: '',
  email: '',
})

const validatePass = (_rule: any, value: string, callback: any) => {
  if (value === '') {
    callback(new Error('请再次输入密码'))
  } else if (value !== form.password) {
    callback(new Error('两次密码不一致'))
  } else {
    callback()
  }
}

const rules = {
  username: [
    { required: true, message: '请输入用户名', trigger: 'blur' },
    { min: 3, max: 20, message: '用户名长度 3-20 个字符', trigger: 'blur' },
  ],
  nickname: [
    { required: true, message: '请输入昵称', trigger: 'blur' },
  ],
  password: [
    { required: true, message: '请输入密码', trigger: 'blur' },
    { min: 6, max: 30, message: '密码长度 6-30 个字符', trigger: 'blur' },
  ],
  confirmPassword: [
    { required: true, validator: validatePass, trigger: 'blur' },
  ],
}

const handleRegister = async () => {
  if (!formRef.value) return
  await formRef.value.validate(async (valid: boolean) => {
    if (!valid) return

    loading.value = true
    try {
      await register({
        username: form.username,
        password: form.password,
        nickname: form.nickname,
        email: form.email,
        role: isStudent.value ? 'student' : 'admin',
      })
      ElMessage.success('注册成功，即将跳转登录')
      setTimeout(() => router.push('/login'), 1000)
    } catch (err: any) {
      const msg = err?.response?.data?.message || err?.message || '注册失败'
      ElMessage.error(msg)
    } finally {
      loading.value = false
    }
  })
}

const goToLogin = () => {
  router.push('/login')
}
</script>

<style scoped>
.register-form-container {
  display: flex;
  justify-content: center;
  align-items: center;
  min-height: 100vh;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  padding: 16px;
}
.register-card {
  width: 460px;
  padding: 40px 32px;
  border-radius: 16px;
  text-align: center;
}
.title { font-size: 24px; font-weight: bold; margin-bottom: 8px; color: #303133; }
.subtitle { font-size: 13px; color: #909399; margin-bottom: 32px; }
.register-form { text-align: left; }
.submit-btn { width: 100%; margin-top: 8px; }
.footer { margin-top: 16px; }
</style>
