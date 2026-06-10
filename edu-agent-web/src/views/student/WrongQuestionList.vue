<template>
  <div class="wrong-list-page">
    <div class="top-bar">
      <el-button text @click="$router.back()">← 返回</el-button>
      <h2>历史错题</h2>
    </div>

    <!-- 加载中 -->
    <div v-if="loading" class="loading-area">
      <p>加载中...</p>
    </div>

    <!-- 错题列表 -->
    <div v-else-if="questions.length" class="content-area">
      <div class="list-header">
        共 <strong>{{ questions.length }}</strong> 道错题，点击可查看详情
      </div>

      <div class="quiz-box">
        <div
          v-for="(q, i) in questions"
          :key="q.id"
          class="quiz-item"
          @click="goToDetail(q)"
        >
          <div class="q-header">
            <span class="q-num">{{ i + 1 }}</span>
            <span class="q-text">{{ q.question }}</span>
          </div>

          <div class="answer-section">
            <div class="answer-row wrong">
              <span class="answer-label">你的答案</span>
              <span class="answer-value">{{ q.userAnswer || '（未作答）' }}</span>
            </div>
            <div class="answer-row correct">
              <span class="answer-label">正确答案</span>
              <span class="answer-value">{{ q.correctAnswer }}</span>
            </div>
          </div>

          <div class="q-footer">
            <span class="go-hint">查看详细讲解 →</span>
            <el-button
              v-if="q.explanation"
              type="primary"
              size="small"
              class="continue-btn"
              @click.stop="continueToTutor(q)"
            >
              继续提问 →
            </el-button>
          </div>
        </div>
      </div>
    </div>

    <!-- 空状态 -->
    <div v-else class="content-area empty-area">
      <el-empty description="暂无错题记录，继续加油！">
        <el-button type="primary" @click="$router.back()">返回</el-button>
      </el-empty>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { getWrongQuestions } from '@/api/tutor'

const router = useRouter()
const loading = ref(true)
const questions = ref<any[]>([])

onMounted(async () => {
  try {
    const data = await getWrongQuestions()
    questions.value = data || []
  } catch {
    // 静默失败
  } finally {
    loading.value = false
  }
})

const goToDetail = (q: any) => {
  router.push(`/student/wrong-questions/${q.id}`)
}

/** 继续提问 → 跳转智能辅导（同 quiz 页逻辑） */
const continueToTutor = (q: any) => {
  const question = q.question || ''
  const userAns = q.userAnswer || ''
  const correctAns = q.correctAnswer || ''
  const explainText = q.explanation || ''

  const messages = [
    {
      role: 'user',
      content: `我遇到一道题：${question}\n我的答案：${userAns}\n正确答案：${correctAns}\n请帮我深入讲解一下。`,
      time: Date.now(),
    },
    {
      role: 'assistant',
      content: explainText,
      time: Date.now(),
    },
  ]
  localStorage.setItem('tutor_current_messages', JSON.stringify(messages))
  localStorage.removeItem('tutor_current_session')
  window.open('/student/tutor', '_self')
}
</script>

<style scoped>
.wrong-list-page { padding: 24px; max-width: 900px; margin: 0 auto; }
.top-bar { display: flex; align-items: center; gap: 12px; margin-bottom: 24px; }
.top-bar h2 { margin: 0; font-size: 18px; }

.loading-area { text-align: center; padding: 80px 0; color: #909399; }
.loading-area p { margin-top: 16px; font-size: 15px; }

.content-area { background: #fff; border-radius: 10px; padding: 24px; border: 1px solid #ebeef5; }

.list-header {
  font-size: 13px;
  color: #909399;
  margin-bottom: 16px;
  padding-bottom: 12px;
  border-bottom: 1px solid #ebeef5;
}

.quiz-item {
  background: #f8f9fb;
  padding: 16px;
  border-radius: 8px;
  margin-bottom: 10px;
  border-left: 3px solid #e64553;
  cursor: pointer;
  transition: box-shadow 0.2s;
}
.quiz-item:hover {
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.08);
}

.q-header {
  display: flex;
  align-items: flex-start;
  gap: 10px;
  font-weight: 500;
  margin-bottom: 12px;
}
.q-num {
  display: inline-flex;
  width: 26px;
  height: 26px;
  border-radius: 50%;
  background: #e64553;
  color: #fff;
  align-items: center;
  justify-content: center;
  font-size: 12px;
  flex-shrink: 0;
}
.q-text { font-size: 14px; line-height: 1.5; color: #303133; padding-top: 2px; }

.answer-section {
  display: flex;
  flex-direction: column;
  gap: 8px;
  margin-bottom: 8px;
}
.answer-row {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 8px 12px;
  border-radius: 6px;
  font-size: 13px;
}
.answer-row.wrong { background: #fef2f2; border: 1px solid #fecaca; }
.answer-row.correct { background: #f0fdf4; border: 1px solid #bbf7d0; }
.answer-label {
  font-size: 11px;
  font-weight: 600;
  padding: 2px 6px;
  border-radius: 4px;
  flex-shrink: 0;
}
.answer-row.wrong .answer-label { background: #e64553; color: #fff; }
.answer-row.correct .answer-label { background: #52c41a; color: #fff; }
.answer-value { color: #303133; }

.q-footer {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-top: 4px;
}
.go-hint { font-size: 12px; color: #409eff; }
.continue-btn { flex-shrink: 0; }

.empty-area { text-align: center; padding: 40px 0; }
</style>
