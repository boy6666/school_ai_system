<template>
  <div class="page-container">
    <header class="page-header">
      <div>
        <p class="eyebrow">ADMIN WORKSPACE</p>
        <h1>管理控制台</h1>
        <p class="description">
          进入用户、资源、内容、AI治理及系统运维模块。
        </p>
      </div>
    </header>

    <el-alert
      title="运营统计正式接口尚未提供"
      description="当前控制台不展示占位数字，统计数据以后端正式服务返回结果为准。"
      type="info"
      :closable="false"
      show-icon
      class="status-alert"
    />

    <section class="module-grid">
      <button
        v-for="module in modules"
        :key="module.path"
        class="module-card"
        type="button"
        @click="router.push(module.path)"
      >
        <h2>{{ module.title }}</h2>
        <p>{{ module.description }}</p>
        <span>进入模块 →</span>
      </button>
    </section>
  </div>
</template>

<script setup lang="ts">
import { useRouter } from 'vue-router'

interface AdminModule {
  title: string
  description: string
  path: string
}

const router = useRouter()

const modules: AdminModule[] = [
  {
    title: '用户管理',
    description: '管理平台用户、角色和账号状态。',
    path: '/admin/users'
  },
  {
    title: 'AI 与 Agent 治理',
    description: '管理智能体、模型配置和知识库。',
    path: '/admin/ai-govern'
  },
  {
    title: '资源管理',
    description: '查看和治理平台教学资源。',
    path: '/admin/resources'
  },
  {
    title: '内容审核',
    description: '处理需要审核的学习与对话内容。',
    path: '/admin/reviews'
  },
  {
    title: '数据统计',
    description: '查看平台运营与资源统计。',
    path: '/admin/statistics'
  },
  {
    title: '系统监控',
    description: '查看服务健康和基础设施指标。',
    path: '/admin/monitor'
  },
  {
    title: '审计日志',
    description: '查询平台关键治理操作记录。',
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

.module-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: var(--space-lg);
}

.module-card {
  padding: var(--space-xl);
  text-align: left;
  cursor: pointer;
  background: var(--canvas);
  border: 1px solid var(--hairline);
  border-radius: var(--radius-lg);
}

.module-card:hover {
  border-color: var(--primary);
}

.module-card h2 {
  margin: 0 0 var(--space-sm);
}

.module-card p {
  margin: 0 0 var(--space-lg);
  color: var(--muted);
}

.module-card span {
  color: var(--primary);
  font: var(--text-sm-medium);
}

@media (max-width: 900px) {
  .page-container {
    padding: var(--space-lg);
  }

  .module-grid {
    grid-template-columns: 1fr;
  }
}
</style>