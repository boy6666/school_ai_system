<template>
  <div class="dashboard-container">
    <div class="dashboard-header">
      <div class="welcome-section">
        <h2>欢迎回来</h2>
        <p>今天也要努力学习哦！</p>
      </div>
    </div>
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

const statCards = ref([{label:'总用户',value:0},{label:'活跃',value:0},{label:'对话',value:0}])
const quickLinks = [
  {name:'智能辅导',path:'/student/tutor'},
  {name:'课程中心',path:'/student/courses'},
  {name:'资源中心',path:'/student/resources'},
  {name:'我的画像',path:'/student/profile'},
]

onMounted(async () => {
  try {
    const s = await getAdminStats()
    statCards.value = [{label:'总用户',value:s?.totalUsers??0},{label:'活跃',value:s?.activeUsers??0},{label:'对话',value:s?.totalConversations??0}]
  } catch {}
})
</script>
<style scoped>
.dashboard-container{padding:20px}
.dashboard-header{margin-bottom:20px}
.stat-card{text-align:center}
.stat-value{font-size:32px;font-weight:bold;color:#409eff}
.stat-label{color:#909399;margin-top:4px}
</style>