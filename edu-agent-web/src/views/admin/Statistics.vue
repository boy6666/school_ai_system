<template>
  <div style="padding:20px">
    <el-row :gutter="20">
      <el-col :span="6" v-for="card in statsCards" :key="card.label">
        <el-card shadow="hover" style="text-align:center">
          <div style="font-size:28px;font-weight:bold;color:#409eff">{{ card.value }}</div>
          <div style="color:#909399;margin-top:4px">{{ card.label }}</div>
        </el-card>
      </el-col>
    </el-row>

    <el-card style="margin-top:20px">
      <template #header><span>用户列表</span></template>
      <el-table :data="userList" stripe v-loading="userLoading">
        <el-table-column prop="id" label="ID" width="60" />
        <el-table-column prop="username" label="用户名" />
        <el-table-column prop="nickname" label="昵称" />
        <el-table-column prop="role" label="角色" width="80" />
        <el-table-column prop="email" label="邮箱" />
        <el-table-column prop="lastLoginTime" label="最后登录" width="160" />
        <el-table-column prop="createTime" label="注册时间" width="160" />
      </el-table>
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { getAdminStats, getUserList } from '@/api/admin'

const statsCards = ref([{label:'总用户',value:0},{label:'活跃',value:0},{label:'对话',value:0},{label:'今日',value:0}])
const userList = ref<any[]>([])
const userLoading = ref(false)

onMounted(async () => {
  try {
    const s = await getAdminStats()
    statsCards.value = [
      {label:'总用户',value:s?.totalUsers??0},
      {label:'活跃',value:s?.activeUsers??0},
      {label:'对话',value:s?.totalConversations??0},
      {label:'今日',value:s?.todayConversations??0},
    ]
  } catch {}
  userLoading.value = true
  try {
    const r = await getUserList({page:1,pageSize:20})
    userList.value = r?.records??[]
  } catch { userList.value = [] }
  userLoading.value = false
})
</script>
