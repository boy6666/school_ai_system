<template>
  <div class="register-container">
    <el-card class="register-card" shadow="hover">
      <h2 class="brand-title">学生注册</h2>
      <el-form :model="form" :rules="rules" ref="formRef">
        <el-form-item prop="username">
          <el-input v-model="form.username" placeholder="用户名" prefix-icon="User" size="large" />
        </el-form-item>
        <el-form-item prop="password">
          <el-input v-model="form.password" type="password" placeholder="密码" prefix-icon="Lock" show-password size="large" />
        </el-form-item>
        <el-form-item prop="email">
          <el-input v-model="form.email" placeholder="邮箱" prefix-icon="Message" size="large" />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="handleRegister" :loading="loading" class="register-btn">注册</el-button>
          <el-link type="primary" @click="$router.push('/login')" style="float: right">已有账号？去登录</el-link>
        </el-form-item>
      </el-form>
    </el-card>
  </div>
</template>

<script setup>
import { reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { useRouter } from 'vue-router'
import request from '@/utils/request'

const router = useRouter()
const formRef = ref()
const loading = ref(false)

const form = reactive({
  username: '',
  password: '',
  email: ''
})

const rules = {
  username: [{ required: true, message: '请输入用户名', trigger: 'blur' }],
  password: [{ required: true, message: '请输入密码', trigger: 'blur' }],
  email: [{ required: true, message: '请输入邮箱', trigger: 'blur' }, { type: 'email', message: '邮箱格式不正确', trigger: 'blur' }]
}

const handleRegister = async () => {
  await formRef.value.validate()
  loading.value = true
  try {
    const res = await request.post('/auth/register', form)
    if (res.code === 200) {
      ElMessage.success('注册成功，请登录')
      router.push('/login')
    } else {
      ElMessage.error(res.message || '注册失败')
    }
  } catch (error) {
    const msg = error.response?.data?.message || error.message || '注册失败，请重试'
    ElMessage.error(msg)
  } finally {
    loading.value = false
  }
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
  margin-bottom: 24px;
  background: linear-gradient(135deg, #3a1c71, #d76d77, #ffaf7b);
  -webkit-background-clip: text;
  background-clip: text;
  color: transparent;
}
.register-btn {
  width: 100%;
  background: linear-gradient(135deg, #667eea, #764ba2);
  border: none;
  font-size: 16px;
  font-weight: 500;
}
.register-btn:hover {
  opacity: 0.9;
}
</style>
