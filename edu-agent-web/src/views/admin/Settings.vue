<template>
  <div class="page-container">
    <header class="page-header">
      <div>
        <p class="eyebrow">SYSTEM SETTINGS</p>
        <h1>系统设置</h1>
        <p class="description">
          统一进入AI治理、系统监控和审计功能。
        </p>
      </div>
    </header>

    <el-alert
      title="系统配置正式接口尚未提供"
      description="当前页面不展示模拟配置、用户、日志或备份数据；相关接口确定后再接入真实设置。"
      type="info"
      :closable="false"
      show-icon
      class="status-alert"
    />

    <section class="entry-grid">
      <article
        v-for="entry in entries"
        :key="entry.path"
        class="entry-card"
      >
        <div>
          <h2>{{ entry.title }}</h2>
          <p>{{ entry.description }}</p>
        </div>

        <el-button
          type="primary"
          plain
          @click="router.push(entry.path)"
        >
          进入模块
        </el-button>
      </article>
    </section>
  </div>
</template>

<script setup lang="ts">
import { useRouter } from 'vue-router'

interface SettingEntry {
  title: string
  description: string
  path: string
}

const router = useRouter()

const entries: SettingEntry[] = [
  {
    title: 'AI 与 Agent 治理',
    description: '管理智能体、模型参数和知识库索引。',
    path: '/admin/ai-govern'
  },
  {
    title: '系统监控',
    description: '查看服务健康、消息队列和缓存指标。',
    path: '/admin/monitor'
  },
  {
    title: '审计日志',
    description: '查询登录、审核和治理操作记录。',
    path: '/admin/audit'
  }
]
</script>

<style scoped>
.page-container {
  min-height: 100%;
  padding: var(--space-xxl);
}

.page-header {
  margin-bottom: var(--space-xl);
}

.eyebrow {
  margin: 0 0 var(--space-xs);
  color: var(--primary);
  font: var(--text-caption);
  letter-spacing: 0.08em;
}

.page-header h1 {
  margin-bottom: var(--space-xs);
}

.description {
  margin: 0;
  color: var(--muted);
  font: var(--text-body);
}

.status-alert {
  margin-bottom: var(--space-xl);
}

.entry-grid {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: var(--space-lg);
}

.entry-card {
  display: flex;
  min-height: 190px;
  flex-direction: column;
  align-items: flex-start;
  justify-content: space-between;
  padding: var(--space-xl);
  background: var(--canvas);
  border: 1px solid var(--hairline);
  border-radius: var(--radius-lg);
}

.entry-card h2 {
  margin: 0 0 var(--space-sm);
}

.entry-card p {
  margin: 0;
  color: var(--muted);
  font: var(--text-body);
}

@media (max-width: 900px) {
  .page-container {
    padding: var(--space-lg);
  }

  .entry-grid {
    grid-template-columns: 1fr;
  }
}
</style>