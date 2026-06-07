<template>
  <div class="admin-dashboard">
    <section class="page-header" style="margin-bottom:24px">
      <div>
        <p class="eyebrow">管理后台</p>
        <h1>控制台</h1>
      </div>
    </section>
    <section class="stat-row">
      <div class="stat-card"><div class="stat-value">{{ stats.totalUsers }}</div><div class="stat-label">注册用户</div></div>
      <div class="stat-card"><div class="stat-value">{{ stats.activeUsers }}</div><div class="stat-label">活跃用户</div></div>
      <div class="stat-card"><div class="stat-value">{{ stats.totalConversations }}</div><div class="stat-label">总对话数</div></div>
      <div class="stat-card"><div class="stat-value">{{ stats.todayConversations }}</div><div class="stat-label">今日对话</div></div>
    </section>
    <el-row :gutter="20">
      <el-col :span="16">
        <div class="card-flat">
          <h4 style="margin-bottom:16px">快捷入口</h4>
          <div class="quick-links">
            <div v-for="item in quickLinks" :key="item.name" class="quick-item" @click="goTo(item.path)">
              <el-icon :size="24"><component :is="item.icon" /></el-icon>
              <span>{{ item.name }}</span>
            </div>
          </div>
        </div>
      </el-col>
      <el-col :span="8">
        <div class="card-flat">
          <h4 style="margin-bottom:16px">系统公告</h4>
          <div class="notice-list">
            <div v-for="notice in notices" :key="notice.id" class="notice-item">
              <div class="notice-title">{{ notice.title }}</div>
              <div class="notice-time">{{ notice.time }}</div>
            </div>
            <div v-if="!notices.length" style="color:var(--muted);font:var(--text-sm);padding:12px 0">暂无公告</div>
          </div>
        </div>
      </el-col>
    </el-row>
  </div>
</template>
<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import request from '@/utils/request'
import { User, Monitor, Upload, DataAnalysis, Checked, Setting, Tickets } from '@element-plus/icons-vue'
const router = useRouter()
const stats = ref({ totalUsers: 0, activeUsers: 0, totalConversations: 0, todayConversations: 0 })
const quickLinks = [
  { name: '用户管理', path: '/admin/users', icon: User },
  { name: '资源管理', path: '/admin/resources', icon: Upload },
  { name: '内容审核', path: '/admin/reviews', icon: Checked },
  { name: '数据统计', path: '/admin/statistics', icon: DataAnalysis },
  { name: '系统设置', path: '/admin/settings', icon: Setting },
]
const notices = ref<any[]>([])
const goTo = (path: string) => router.push(path)
onMounted(async () => {
  try {
    const s: any = await request.get('/admin/stats')
    if (s) stats.value = { totalUsers: s.totalUsers||0, activeUsers: s.activeUsers||0, totalConversations: s.totalConversations||0, todayConversations: s.todayConversations||0 }
  } catch {}
})
</script>
<style scoped>
.admin-dashboard { padding: var(--space-xl); background: var(--surface); min-height: 100vh; }
.page-header { padding: 24px 28px; border-radius: var(--radius-xl); background: linear-gradient(135deg,var(--canvas) 0%,var(--tint-lavender) 100%); box-shadow: var(--shadow-subtle); }
.eyebrow { margin:0 0 4px; color:var(--primary); font-weight:700; font-size:12px; letter-spacing:1px; text-transform:uppercase; }
.page-header h1 { margin:0; font:var(--text-h2); color:var(--ink); }
.quick-links { display:flex; flex-wrap:wrap; gap:16px; }
.quick-item { width:80px; text-align:center; cursor:pointer; transition:transform .2s; color:var(--steel); font:var(--text-sm); }
.quick-item:hover { transform:translateY(-4px); color:var(--primary); }
.quick-item span { display:block; margin-top:6px; }
.notice-list { max-height:300px; overflow-y:auto; }
.notice-item { padding:12px 0; border-bottom:1px solid var(--hairline-soft); cursor:pointer; }
.notice-item:hover { background:var(--surface); }
.notice-title { font:var(--text-sm-medium); color:var(--charcoal); margin-bottom:4px; }
.notice-time { font:var(--text-micro); color:var(--muted); }
</style>
