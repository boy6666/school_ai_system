<template>
  <div class="page-container">
    <header class="page-header">
      <div>
        <p class="eyebrow">AI GOVERNANCE</p>
        <h1>AI 与 Agent 治理</h1>
        <p class="description">
          管理教学智能体、模型配置与知识库索引。
        </p>
      </div>
    </header>

    <section class="content-card">
      <el-empty
        description="AI治理正式接口尚未提供，当前暂无可展示配置"
      />
    </section>
  </div>
</template>

<script setup lang="ts">
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

.content-card {
  min-height: 420px;
  padding: var(--space-xl);
  background: var(--canvas);
  border: 1px solid var(--hairline);
  border-radius: var(--radius-lg);
}

@media (max-width: 900px) {
  .page-container {
    padding: var(--space-lg);
  }
}
</style>