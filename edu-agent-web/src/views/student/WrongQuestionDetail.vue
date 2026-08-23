<template>
  <div class="detail-page">
    <div class="top-bar">
      <el-button text @click="$router.back()">← 返回</el-button>
      <h2>错题详情</h2>
    </div>

    <!-- 加载中 -->
    <div v-if="loading" class="loading-area">
      <p>加载中...</p>
    </div>

    <!-- 内容区 -->
    <div v-else-if="question" class="content-area">
      <div class="wrong-badge">历史错题</div>

      <div class="q-item">
        <div class="q-header">
          <span class="q-num">1</span>
          <span class="q-text">{{ question.question }}</span>
        </div>

        <div class="answer-section">
          <div class="answer-row wrong">
            <span class="answer-label">你的答案</span>
            <span class="answer-value">{{ question.userAnswer || '（未作答）' }}</span>
          </div>
          <div class="answer-row correct">
            <span class="answer-label">正确答案</span>
            <span class="answer-value">{{ question.correctAnswer }}</span>
          </div>
        </div>

        <!-- AI 讲解 -->
        <div v-if="question.explanation" class="explain-box">
          <div class="explain-header">📖 讲解</div>
          <div class="explain-body">{{ question.explanation }}</div>
        </div>

        <!-- 继续提问 -->
        <div v-if="question.explanation" class="q-continue">
          <el-button type="primary" size="small" @click="continueToTutor">
            继续提问 →
          </el-button>
        </div>
      </div>
    </div>

    <!-- 错误状态 -->
    <div v-else class="empty-area">
      <el-empty description="错题不存在">
        <el-button type="primary" @click="$router.back()">返回</el-button>
      </el-empty>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { getWrongQuestionById, type WrongQuestionItem } from '@/api/tutor'

const route = useRoute()
const router = useRouter()
const loading = ref(true)
const question = ref<WrongQuestionItem | null>(null)

onMounted(async () => {
  const id = Number(route.params.id)
  if (!id) {
    loading.value = false
    return
  }
  try {
    const data = await getWrongQuestionById(id)
    question.value = data
  } catch {
    ElMessage.error('加载错题失败')
  } finally {
    loading.value = false
  }
})

/** 继续提问 → 跳转智能辅导 */
const continueToTutor = () => {
  const q = question.value
  if (!q) return
  const prompt = `我遇到一道题：${q.question}\n我的答案：${q.userAnswer || '未作答'}\n正确答案：${q.correctAnswer}\n请帮我深入讲解一下。`
  router.push({
    path: '/student/tutor',
    query: { prompt }
  })
}
</script>

<style scoped>
.detail-page { padding: 24px; max-width: 900px; margin: 0 auto; }
.top-bar { display: flex; align-items: center; gap: 12px; margin-bottom: 24px; }
.top-bar h2 { margin: 0; font-size: 18px; }

.loading-area { text-align: center; padding: 80px 0; color: #909399; }
.loading-area p { margin-top: 16px; font-size: 15px; }

.content-area { background: #fff; border-radius: 10px; padding: 24px; border: 1px solid #ebeef5; }

.wrong-badge {
  display: inline-block;
  background: #fde8e8;
  color: #e64553;
  font-size: 12px;
  font-weight: 600;
  padding: 4px 12px;
  border-radius: 12px;
  margin-bottom: 20px;
}

.q-item { padding: 0; }

.q-header {
  display: flex;
  align-items: flex-start;
  gap: 10px;
  font-weight: 500;
  margin-bottom: 20px;
}
.q-num {
  display: inline-flex;
  width: 28px;
  height: 28px;
  border-radius: 50%;
  background: #e64553;
  color: #fff;
  align-items: center;
  justify-content: center;
  font-size: 13px;
  flex-shrink: 0;
}
.q-text { font-size: 16px; line-height: 1.6; color: #303133; padding-top: 3px; }

.answer-section {
  margin-left: 38px;
  display: flex;
  flex-direction: column;
  gap: 10px;
  margin-bottom: 20px;
}
.answer-row {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 10px 14px;
  border-radius: 8px;
  font-size: 14px;
}
.answer-row.wrong {
  background: #fef2f2;
  border: 1px solid #fecaca;
}
.answer-row.correct {
  background: #f0fdf4;
  border: 1px solid #bbf7d0;
}
.answer-label {
  font-size: 12px;
  font-weight: 600;
  padding: 2px 8px;
  border-radius: 4px;
  flex-shrink: 0;
}
.answer-row.wrong .answer-label { background: #e64553; color: #fff; }
.answer-row.correct .answer-label { background: #52c41a; color: #fff; }
.answer-value { color: #303133; }

.explain-box {
  margin-left: 38px;
  margin-top: 20px;
  background: #f8faff;
  border: 1px solid #e8e8e8;
  border-radius: 10px;
  overflow: hidden;
}
.explain-header {
  font-size: 14px;
  font-weight: 600;
  color: #4f8cff;
  padding: 12px 16px;
  background: #f8faff;
  border-bottom: 1px solid #e8e8e8;
}
.explain-body {
  padding: 14px 16px;
  font-size: 14px;
  line-height: 1.8;
  color: #333;
  white-space: pre-wrap;
}

.q-continue {
  margin-left: 38px;
  margin-top: 16px;
}
.empty-area { padding: 80px 0; }
</style>