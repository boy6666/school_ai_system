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
<el-form-item prop="nickname">
  <el-input
    v-model="form.nickname"
    placeholder="昵称"
    prefix-icon="UserFilled"
    size="large"
  />
</el-form-item>
        <el-form-item>
          <el-button type="primary" @click="handleRegister" :loading="loading" class="register-btn">注册</el-button>
          <el-link type="primary" @click="$router.push('/login')" style="float: right">已有账号？去登录</el-link>
        </el-form-item>
      </el-form>
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { reactive, ref } from 'vue'
import {
  ElMessage,
  type FormInstance,
  type FormRules
} from 'element-plus'
import { useRouter } from 'vue-router'
import {
  normalizeAuthUser,
  register,
  type RegisterParams
} from '@/api/auth'
import { useUserStore } from '@/stores/user'

const router = useRouter()
const userStore = useUserStore()
const formRef = ref<FormInstance>()
const loading = ref(false)

const form = reactive<RegisterParams>({
  username: '',
  password: '',
  nickname: '',
  role: 'student'
})

const rules: FormRules<RegisterParams> = {
  username: [
    {
      required: true,
      message: '请输入用户名',
      trigger: 'blur'
    },
    {
      min: 3,
      max: 20,
      message: '用户名长度需为3-20个字符',
      trigger: 'blur'
    }
  ],
  password: [
    {
      required: true,
      message: '请输入密码',
      trigger: 'blur'
    },
    {
      min: 6,
      max: 30,
      message: '密码长度需为6-30个字符',
      trigger: 'blur'
    }
  ],
  nickname: [
    {
      required: true,
      message: '请输入昵称',
      trigger: 'blur'
    }
  ]
}

const handleRegister = async () => {
  if (loading.value) return

  const valid = await formRef.value
    ?.validate()
    .catch(() => false)

  if (!valid) return

  loading.value = true

  try {
    const result = await register({ ...form })

    if (!result.token) {
      throw new Error('注册响应中缺少 token')
    }

    if (!result.userInfo) {
      throw new Error('注册响应中缺少用户信息')
    }

    const normalizedUser = normalizeAuthUser(
      result.userInfo
    )

    userStore.setToken(result.token)
    userStore.setUserInfo(normalizedUser)

    ElMessage.success('注册成功')
    await router.push('/student/dashboard')
  } catch (error: unknown) {
    userStore.logout()

    const message =
      error instanceof Error
        ? error.message
        : '注册失败，请检查填写信息或稍后重试'

    ElMessage.error(message)
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
  background: rgba(255, 255, 255, 0.96);
  backdrop-filter: blur(2px);
  box-shadow: 0 20px 35px rgba(0, 0, 0, 0.2);
}

.brand-title {
  margin-bottom: 28px;
  color: transparent;
  font-size: 28px;
  font-weight: bold;
  text-align: center;
  background: linear-gradient(135deg, #3a1c71, #d76d77, #ffaf7b);
  background-clip: text;
  -webkit-background-clip: text;
}

.register-btn {
  width: 100%;
  border: none;
  background: linear-gradient(135deg, #667eea, #764ba2);
  font-size: 16px;
  font-weight: 500;
}

.register-btn:hover {
  opacity: 0.9;
}
</style>