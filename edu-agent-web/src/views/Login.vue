<template>
  <div class="login-container">
    <el-card class="login-card" shadow="hover">
      <h2 class="brand-title">EduAgent 个性化学习平台</h2>
      <el-form ref="formRef" :model="form" :rules="rules">
        <el-form-item prop="username">
          <el-input
            v-model="form.username"
            placeholder="账号/学号/邮箱"
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
            @keyup.enter="handleLogin"
          />
        </el-form-item>
        <el-form-item>
          <el-button
            type="primary"
            size="large"
            class="login-btn"
            :loading="loading"
            @click="handleLogin"
          >
            登录
          </el-button>
        </el-form-item>
        <el-form-item>
          <el-link type="primary" @click="router.push('/register')">
            还没有账号？去注册
          </el-link>
          <el-link type="info" class="forgot-link">忘记密码？</el-link>
        </el-form-item>
      </el-form>
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { reactive, ref } from 'vue'
import { ElMessage, type FormInstance, type FormRules } from 'element-plus'
import { useRouter } from 'vue-router'
import { getMe, login, type LoginParams } from '@/api/auth'
import { useUserStore } from '@/stores/user'

const ROLE_ADMIN = 'ROLE_ADMIN'
const ROLE_TEACHER = 'ROLE_TEACHER'

const router = useRouter()
const userStore = useUserStore()
const formRef = ref<FormInstance>()
const loading = ref(false)

const form = reactive<LoginParams>({
  username: '',
  password: ''
})

const rules: FormRules<LoginParams> = {
  username: [{ required: true, message: '请输入账号', trigger: 'blur' }],
  password: [{ required: true, message: '请输入密码', trigger: 'blur' }]
}

const routeByRoles = async (roles: string[]) => {
  if (roles.includes(ROLE_ADMIN)) {
    await router.push('/admin/dashboard')
    return
  }

  if (roles.includes(ROLE_TEACHER)) {
    await router.push('/teacher/dashboard')
    return
  }

  await router.push('/student/dashboard')
}

const handleLogin = async () => {
  if (loading.value) return

  const valid = await formRef.value?.validate().catch(() => false)
  if (!valid) return

  loading.value = true
  try {
    // request.ts 已完成 Result.data 解包，这里拿到的就是 LoginResult。
    const result = await login({ ...form })
    if (!result.token) throw new Error('登录响应中缺少 token')

    // 先保存 token，后续 getMe 请求才能自动携带 Bearer token。
    userStore.setToken(result.token)

    const me = await getMe()
    const roles = me.roles?.length ? me.roles : result.roles

    userStore.setUserInfo({
      ...me,
      userId: me.userId ?? result.userId,
      realName: me.realName || result.realName,
      roles,
      onboarded: me.onboarded ?? result.onboarded
    })

    ElMessage.success('登录成功')
    await routeByRoles(roles)
  } catch (error: unknown) {
    // 防止 login 成功而 getMe 失败时留下不完整的登录状态。
    userStore.logout()
    const message = error instanceof Error ? error.message : '登录失败，请检查网络'
    ElMessage.error(message)
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

.login-btn {
  width: 100%;
  border: none;
  background: linear-gradient(135deg, #667eea, #764ba2);
}

.login-btn:hover {
  opacity: 0.9;
}

.forgot-link {
  margin-left: auto;
}
</style>