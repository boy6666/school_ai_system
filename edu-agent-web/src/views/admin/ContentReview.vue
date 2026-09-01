<template>
  <div class="page-container">
    <header class="page-header">
      <div>
        <p class="eyebrow">CONTENT REVIEW</p>
        <h1>内容审核</h1>
        <p class="description">
          审核平台学习内容、对话内容与教学资源。
        </p>
      </div>
    </header>

    <el-alert
      title="内容审核正式接口尚未提供"
      description="当前暂无待审核内容；待正式契约提供后接入审核列表、通过、拒绝及拒绝原因提交功能。"
      type="info"
      :closable="false"
      show-icon
      class="status-alert"
    />

    <section class="content-card">
      <el-empty description="暂无可展示的内容审核数据" />
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