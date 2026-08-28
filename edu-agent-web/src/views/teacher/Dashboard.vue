<template>
  <div class="page-container">
    <section class="page-header">
      <div>
        <p class="eyebrow">TEACHER WORKSPACE</p>
        <h1>教师工作台</h1>
        <p class="description">
          管理班级、题库与作业，查看学生学习情况并使用 AI 教学工具。
        </p>
      </div>
    </section>

    <section class="module-section">
      <div class="section-heading">
        <div>
          <h2>教学管理</h2>
          <p>选择需要处理的教学任务。</p>
        </div>
      </div>

      <div class="module-grid">
        <button
          v-for="module in modules"
          :key="module.path"
          class="module-card"
          type="button"
          @click="router.push(module.path)"
        >
          <div class="module-icon">
            <el-icon>
              <component :is="module.icon" />
            </el-icon>
          </div>

          <div class="module-content">
            <h3>{{ module.title }}</h3>
            <p>{{ module.description }}</p>
          </div>

          <el-icon class="module-arrow">
            <ArrowRight />
          </el-icon>
        </button>
      </div>
    </section>

    <el-alert
      title="教师端功能正在按照正式接口契约逐步接入"
      description="当前工作台不展示模拟业务数据；各模块完成后将接入真实服务或契约桩进行验证。"
      type="info"
      :closable="false"
      show-icon
    />
  </div>
</template>

<script setup lang="ts">
import { useRouter } from 'vue-router'
import {
  Collection,
  Document,
  EditPen,
  FolderOpened,
  MagicStick,
  TrendCharts,
  UserFilled
} from '@element-plus/icons-vue'

interface TeacherModule {
  title: string
  description: string
  path: string
  icon: typeof UserFilled
}

const router = useRouter()

const modules: TeacherModule[] = [
  {
    title: '班级管理',
    description: '维护班级信息与班级学生。',
    path: '/teacher/classes',
    icon: UserFilled
  },
  {
    title: '题库管理',
    description: '创建、筛选和维护教学题目。',
    path: '/teacher/questions',
    icon: Collection
  },
  {
    title: '作业管理',
    description: '创建作业、选择题目并发布。',
    path: '/teacher/assignments',
    icon: Document
  },
  {
    title: '批改复核',
    description: '查看提交结果并复核成绩。',
    path: '/teacher/grades',
    icon: EditPen
  },
  {
    title: '学情看板',
    description: '分析完成率、掌握度和薄弱知识点。',
    path: '/teacher/analytics',
    icon: TrendCharts
  },
  {
    title: 'AI 助教',
    description: '开展教学答疑与成绩解读。',
    path: '/teacher/ai-tutor',
    icon: MagicStick
  },
  {
    title: '资源管理',
    description: '查询并管理教学资源。',
    path: '/teacher/resources',
    icon: FolderOpened
  }
]
</script>

<style scoped>
.page-container {
  min-height: 100%;
  padding: var(--space-xxl);
}

.page-header {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  margin-bottom: var(--space-xxl);
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

.description,
.section-heading p {
  margin: 0;
  color: var(--muted);
  font: var(--text-body);
}

.module-section {
  margin-bottom: var(--space-xl);
}

.section-heading {
  margin-bottom: var(--space-lg);
}

.section-heading h2 {
  margin-bottom: var(--space-xxs);
}

.module-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: var(--space-md);
  margin-bottom: var(--space-xl);
}

.module-card {
  display: flex;
  align-items: center;
  gap: var(--space-md);
  width: 100%;
  padding: var(--space-lg);
  text-align: left;
  cursor: pointer;
  background: var(--canvas);
  border: 1px solid var(--hairline);
  border-radius: var(--radius-lg);
  transition:
    border-color 0.2s ease,
    transform 0.2s ease,
    box-shadow 0.2s ease;
}

.module-card:hover {
  border-color: var(--primary);
  box-shadow: 0 8px 24px rgba(10, 21, 48, 0.08);
  transform: translateY(-2px);
}

.module-icon {
  display: flex;
  flex: 0 0 44px;
  align-items: center;
  justify-content: center;
  width: 44px;
  height: 44px;
  color: var(--primary);
  font-size: 22px;
  background: var(--tint-lavender);
  border-radius: var(--radius-md);
}

.module-content {
  flex: 1;
  min-width: 0;
}

.module-content h3 {
  margin: 0 0 var(--space-xxs);
}

.module-content p {
  margin: 0;
  color: var(--muted);
  font: var(--text-sm);
}

.module-arrow {
  color: var(--muted);
}

@media (max-width: 900px) {
  .module-grid {
    grid-template-columns: 1fr;
  }

  .page-container {
    padding: var(--space-lg);
  }
}
</style>