<template>
  <div class="page-container">
    <header class="page-header">
      <div>
        <p class="eyebrow">OPERATION STATISTICS</p>
        <h1>数据统计</h1>
        <p class="description">
          查看用户增长、资源分布及平台运营趋势。
        </p>
      </div>
    </header>

    <el-alert
      title="管理员统计正式接口尚未提供"
      description="当前页面不展示占位数字或前端推算数据；正式接口提供后统一使用BaseChart渲染真实统计结果。"
      type="info"
      :closable="false"
      show-icon
      class="status-alert"
    />

    <section class="content-card">
      <el-empty description="暂无可展示的运营统计数据" />
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