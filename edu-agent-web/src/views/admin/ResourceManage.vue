<template>
  <div class="page-container">
    <header class="page-header">
      <div>
        <p class="eyebrow">RESOURCE GOVERNANCE</p>
        <h1>资源管理</h1>
        <p class="description">
          查询、审核并治理平台教学资源。
        </p>
      </div>
    </header>

    <el-alert
      title="管理员资源治理接口尚未提供"
      description="当前暂无可审核资源；待正式契约提供后接入资源列表、审核、上下线和统计功能。"
      type="info"
      :closable="false"
      show-icon
      class="status-alert"
    />

    <section class="content-card">
      <el-empty description="暂无可展示的资源治理数据" />
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

.status-alert {
  margin-bottom: var(--space-xl);
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