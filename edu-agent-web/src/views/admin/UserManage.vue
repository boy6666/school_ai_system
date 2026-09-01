<template>
  <div class="page-container">
    <header class="page-header">
      <div>
        <p class="eyebrow">USER GOVERNANCE</p>
        <h1>用户管理</h1>
        <p class="description">
          查询平台用户，并管理账号角色与启停状态。
        </p>
      </div>
    </header>

     <el-alert
      title="管理员用户正式接口尚未提供"
      description="当前暂无可展示用户数据；待正式契约提供后接入用户查询、角色与状态管理功能。"
      type="info"
      :closable="false"
      show-icon
      class="status-alert"
    />

    <section class="content-card">
      <el-empty
        description="暂无可展示的用户数据"
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