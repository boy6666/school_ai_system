<template>
  <div class="admin-dashboard">
    <el-row :gutter="20">
      <el-col :span="6" v-for="card in statCards" :key="card.label">
        <el-card shadow="hover" class="stat-card">
          <div class="stat-value">{{ card.value }}</div>
          <div class="stat-label">{{ card.label }}</div>
        </el-card>
      </el-col>
    </el-row>
    <el-row :gutter="20" style="margin-top:20px">
      <el-col :span="12">
        <el-card shadow="never">
          <template #header><span>快捷入口</span></template>
          <el-button style="width:100%;margin:4px 0" v-for="link in quickLinks" :key="link.path" @click="$router.push(link.path)">{{ link.name }}</el-button>
        </el-card>
      </el-col>
    </el-row>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { getAdminStats } from '@/api/admin'

const statCards = ref([
  { label: '总用户数', value: 0 },
  { label: '活跃用户', value: 0 },
  { label: '总对话数', value: 0 },
  { label: '今日对话', value: 0 },
])

const quickLinks = [
  { name: '用户管理', path: '/admin/users' },
  { name: '资源管理', path: '/admin/resources' },
  { name: '内容审核', path: '/admin/reviews' },
  { name: '系统设置', path: '/admin/settings' },
]

onMounted(async () => {
  try {
    const s = await getAdminStats()
    statCards.value = [
      { label: '总用户数', value: s?.totalUsers ?? 0 },
      { label: '活跃用户', value: s?.activeUsers ?? 0 },
      { label: '总对话数', value: s?.totalConversations ?? 0 },
      { label: '今日对话', value: s?.todayConversations ?? 0 },
    ]
  } catch {}
})
</script>

<style scoped>
.admin-dashboard { padding: 20px; }
.stat-card { text-align: center; }
.stat-value { font-size: 32px; font-weight: bold; color: #409eff; }
.stat-label { font-size: 14px; color: #909399; margin-top: 8px; }
</style>
