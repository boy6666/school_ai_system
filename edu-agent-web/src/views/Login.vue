cd ~/edu-agent/edu-agent-web

cat > src/views/Login.vue <<'EOF'
<template>
  <div class="login-container">
    <el-card class="login-card">
      <h2>EduAgent 个性化学习平台</h2>
      <el-form :model="form" :rules="rules" ref="formRef">
        <el-form-item prop="username">
          <el-input v-model="form.username" placeholder="账号/学号/邮箱" prefix-icon="User" />
        </el-form-item>
        <el-form-item prop="password">
          <el-input v-model="form.password" type="password" placeholder="密码" prefix-icon="Lock" show-password />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="handleLogin" :loading="loading" style="width: 100%">登录</el-button>
        </el-form-item>
        <el-form-item>
          <el-button type="success" @click="mockLogin" style="width: 100%">模拟登录（跳过验证）</el-button>
        </el-form-item>
        <el-form-item>
          <el-link type="primary" @click="$router.push('/register')">还没有账号？去注册</el-link>
          <el-link type="info" style="float: right">忘记密码？</el-link>
        </el-form-item>
      </el-form>
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { useRouter } from 'vue-router'
import { useUserStore } from '@/stores/user'

const router = useRouter()
const userStore = useUserStore()
const formRef = ref()
const loading = ref(false)

const form = reactive({ username: '', password: '' })
const rules = {
  username: [{ required: true, message: '请输入账号', trigger: 'blur' }],
  password: [{ required: true, message: '请输入密码', trigger: 'blur' }]
}

// 真实登录（对接后端）
const handleLogin = async () => {
  await formRef.value.validate()
  loading.value = true
  try {
    // 模拟后端请求，实际应调用 request.post('/login', form)
    // 这里暂时模拟成功
    const mockRes = {
      code: 1,
      data: { id: 1, username: form.username, name: '测试学生', token: 'mock-token' }
    }
    if (mockRes.code === 1) {
      userStore.setToken(mockRes.data.token)
      userStore.setUserInfo(mockRes.data)
      ElMessage.success('登录成功')
      router.push('/student/dashboard')
    }
  } catch (error) {
    console.error(error)
  } finally {
    loading.value = false
  }
}

// 模拟登录：直接设置 token 并跳转
const mockLogin = () => {
  userStore.setToken('mock-token-123')
  userStore.setUserInfo({ id: 1, username: 'demo', name: '演示学生', token: 'mock-token-123' })
  ElMessage.success('模拟登录成功')
  router.push('/student/dashboard')
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
  width: 400px;
  padding: 20px;
}
</style>
EOF

# 确保 Element Plus 图标库已注册（已有）
# 重启开发服务器
npm run dev