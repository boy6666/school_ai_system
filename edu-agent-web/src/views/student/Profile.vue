<template>
  <div class="profile-container">
    <el-card>
      <template #header><span>个人中心</span></template>
      <el-form :model="form" label-width="100px">
        <el-form-item label="用户名">
          <el-input v-model="form.username" disabled />
        </el-form-item>
        <el-form-item label="昵称">
          <el-input v-model="form.nickname" />
        </el-form-item>
        <el-form-item label="邮箱">
          <el-input v-model="form.email" />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="updateProfile">保存修改</el-button>
        </el-form-item>
      </el-form>
    </el-card>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import request from '@/utils/request'
import { useUserStore } from '@/stores/user'

const userStore = useUserStore()
const form = ref({
  username: '',
  nickname: '',
  email: ''
})

const fetchUserInfo = async () => {
  try {
    const res = await request.get('/user/info')
    if (res.code === 200) {
      form.value = {
        username: res.data.username,
        nickname: res.data.nickname || '',
        email: res.data.email || ''
      }
      // 同步更新 store 中的显示名称
      userStore.setUserInfo({
        ...userStore.userInfo,
        name: form.value.nickname || form.value.username,
        nickname: form.value.nickname
      })
    }
  } catch (error) {
    ElMessage.error('获取用户信息失败')
  }
}

const updateProfile = async () => {
  try {
    const res = await request.put('/user/update', {
      nickname: form.value.nickname,
      email: form.value.email
    })
    if (res.code === 200) {
      ElMessage.success('修改成功')
      // 更新 store 中的显示名称
      userStore.setUserInfo({
        ...userStore.userInfo,
        name: form.value.nickname || form.value.username,
        nickname: form.value.nickname
      })
    } else {
      ElMessage.error(res.message || '修改失败')
    }
  } catch (error) {
    ElMessage.error('修改失败')
  }
}

onMounted(fetchUserInfo)
</script>

<style scoped>
.profile-container { padding: 20px; }
</style>
