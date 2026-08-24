<template>
  <div class="practice-page">
    <el-card class="filter-card">
      <el-row :gutter="20">
        <el-col :span="6">
          <el-select v-model="filters.subject" placeholder="选择科目" style="width: 100%">
            <el-option label="全部" value="" />
            <el-option label="前端开发" value="frontend" />
            <el-option label="后端开发" value="backend" />
            <el-option label="数据库" value="database" />
            <el-option label="项目实战" value="project" />
          </el-select>
        </el-col>
        <el-col :span="6">
          <el-select v-model="filters.difficulty" placeholder="选择难度" style="width: 100%">
            <el-option label="全部" value="" />
            <el-option label="简单" value="easy" />
            <el-option label="中等" value="medium" />
            <el-option label="困难" value="hard" />
          </el-select>
        </el-col>
        <el-col :span="6">
          <el-input v-model="filters.keyword" placeholder="搜索题目" />
        </el-col>
        <el-col :span="6">
          <el-button type="primary" @click="handleSearch">搜索</el-button>
          <el-button @click="handleReset">重置</el-button>
        </el-col>
      </el-row>
    </el-card>

    <div v-if="!isDoingPractice" v-loading="loading">
      <el-row :gutter="20" class="question-list">
        <el-col :span="24" v-for="question in questions" :key="question.id">
          <el-card class="question-card" @click="startPractice(question)">
            <div class="question-header">
              <el-tag :type="getDifficultyType(question.difficulty)">
                {{ getDifficultyLabel(question.difficulty) }}
              </el-tag>
              <el-tag type="info">{{ getSubjectLabel(question.subject) }}</el-tag>
              <span class="question-title">{{ question.title }}</span>
            </div>
            <div class="question-desc">{{ question.description }}</div>
            <div class="question-footer">
              <span class="question-stats">
                <el-icon><View /></el-icon> {{ question.views }} 次浏览
                <el-icon><User /></el-icon> {{ question.completed }} 人完成
              </span>
              <el-button type="primary" size="small">开始练习</el-button>
            </div>
          </el-card>
        </el-col>
      </el-row>

      <el-pagination
        v-model:current-page="pagination.page"
        v-model:page-size="pagination.size"
        :total="pagination.total"
        layout="total, prev, pager, next"
        style="margin-top: 20px; text-align: center"
        @size-change="handleSizeChange"
        @current-change="handlePageChange"
      />
    </div>

    <div v-else class="practice-area">
      <el-card class="question-detail-card">
        <template #header>
          <div class="detail-header">
            <div>
              <el-tag :type="getDifficultyType(currentQuestion?.difficulty)">
                {{ getDifficultyLabel(currentQuestion?.difficulty) }}
              </el-tag>
              <h3>{{ currentQuestion?.title }}</h3>
            </div>
            <el-button @click="exitPractice">退出</el-button>
          </div>
        </template>

        <div class="question-content">
          <div class="question-description">
            <h4>题目描述</h4>
            <p>{{ currentQuestion?.description }}</p>
          </div>

          <div class="question-requirements">
            <h4>要求</h4>
            <ul>
              <li v-for="(req, index) in currentQuestion?.requirements" :key="index">
                {{ req }}
              </li>
            </ul>
          </div>

          <div class="answer-area">
            <h4>答案</h4>
            <el-input
              v-model="answer"
              type="textarea"
              :rows="10"
              placeholder="请输入您的答案..."
            />
          </div>

          <div class="action-buttons">
            <el-button type="primary" :loading="submitting" @click="handleSubmit">提交答案</el-button>
            <el-button type="info" @click="handleHint">查看提示</el-button>
          </div>
        </div>
      </el-card>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { View, User } from '@element-plus/icons-vue'
import {
  getPracticeDetail,
  getPracticeList,
  submitAnswer,
  type PracticeQuestion
} from '@/api/practice'

const filters = reactive({
  subject: '',
  difficulty: '',
  keyword: ''
})

const pagination = reactive({
  page: 1,
  size: 10,
  total: 0
})

const isDoingPractice = ref(false)
const currentQuestion = ref<PracticeQuestion | null>(null)
const answer = ref('')
const submitting = ref(false)
const loading = ref(false)

const questions = ref<PracticeQuestion[]>([])

type TagType = 'success' | 'warning' | 'danger' | 'info'

const getDifficultyType = (difficulty = ''): TagType => {
  const types: Record<string, TagType> = {
    easy: 'success',
    medium: 'warning',
    hard: 'danger'
  }
  return types[difficulty] || 'info'
}

const getDifficultyLabel = (difficulty = '') => {
  const labels: Record<string, string> = {
    easy: '简单',
    medium: '中等',
    hard: '困难'
  }
  return labels[difficulty] || difficulty
}

const getSubjectLabel = (subject: string) => {
  const labels: Record<string, string> = {
    frontend: '前端开发',
    backend: '后端开发',
    database: '数据库',
    project: '项目实战'
  }
  return labels[subject] || subject
}

const handleSearch = () => {
  pagination.page = 1
  loadQuestions()
}

const handleReset = () => {
  filters.subject = ''
  filters.difficulty = ''
  filters.keyword = ''
  pagination.page = 1
  loadQuestions()
}

const handleSizeChange = (size: number) => {
  pagination.size = size
  loadQuestions()
}

const handlePageChange = (page: number) => {
  pagination.page = page
  loadQuestions()
}

const loadQuestions = async () => {
  loading.value = true
  try {
    const result = await getPracticeList({
      subject: filters.subject || undefined,
      difficulty: filters.difficulty || undefined,
      keyword: filters.keyword || undefined,
      page: pagination.page,
      pageSize: pagination.size
    })
    questions.value = result.records
    pagination.total = result.total
  } catch {
    questions.value = []
    pagination.total = 0
    ElMessage.error('练习列表加载失败，请稍后重试')
  } finally {
    loading.value = false
  }
}

const startPractice = async (question: PracticeQuestion) => {
  try {
    currentQuestion.value = await getPracticeDetail(question.id)
    isDoingPractice.value = true
    answer.value = ''
  } catch {
    ElMessage.error('练习详情加载失败，请稍后重试')
  }
}

const exitPractice = () => {
  ElMessageBox.confirm('确定要退出练习吗？您的进度将不会保存。', '提示', {
    confirmButtonText: '确定',
    cancelButtonText: '取消',
    type: 'warning'
  }).then(() => {
    isDoingPractice.value = false
    currentQuestion.value = null
    answer.value = ''
  }).catch(() => {})
}

const handleSubmit = async () => {
  if (!answer.value.trim()) {
    ElMessage.warning('请输入您的答案')
    return
  }

  if (!currentQuestion.value) return

  submitting.value = true
  try {
    const result = await submitAnswer({
      questionId: currentQuestion.value.id,
      answer: answer.value.trim()
    })
    const details = [
      typeof result.score === 'number' ? `得分：${result.score}分` : '',
      result.evaluation || result.explanation || ''
    ].filter(Boolean).join('\n')
    await ElMessageBox.alert(details || '答案已提交，评分结果暂未返回。', '练习结果', {
      confirmButtonText: '确定'
    })
    isDoingPractice.value = false
    currentQuestion.value = null
    answer.value = ''
    await loadQuestions()
  } catch {
    ElMessage.error('答案提交失败，请稍后重试')
  } finally {
    submitting.value = false
  }
}

const handleHint = () => {
  ElMessage.info('练习提示接口暂未开放')
}

loadQuestions()
</script>

<style scoped>
.practice-page {
  padding: 20px;
}

.filter-card {
  margin-bottom: 20px;
  border-radius: 8px;
}

.question-list {
  margin-bottom: 20px;
}

.question-card {
  margin-bottom: 20px;
  border-radius: 8px;
  cursor: pointer;
  transition: transform 0.3s, box-shadow 0.3s;
}

.question-card:hover {
  transform: translateY(-3px);
  box-shadow: 0 6px 16px rgba(0, 0, 0, 0.12);
}

.question-header {
  display: flex;
  align-items: center;
  gap: 10px;
  margin-bottom: 15px;
}

.question-title {
  font-weight: bold;
  font-size: 16px;
  color: #333;
}

.question-desc {
  color: #666;
  margin-bottom: 15px;
  line-height: 1.6;
}

.question-footer {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.question-stats {
  color: #999;
  font-size: 14px;
}

.question-stats .el-icon {
  margin-right: 4px;
}

.question-stats .el-icon + .el-icon {
  margin-left: 15px;
}

.practice-area {
  animation: fadeIn 0.3s;
}

@keyframes fadeIn {
  from {
    opacity: 0;
    transform: translateY(10px);
  }
  to {
    opacity: 1;
    transform: translateY(0);
  }
}

.question-detail-card {
  border-radius: 8px;
}

.detail-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.detail-header h3 {
  margin: 10px 0 0 0;
  font-size: 18px;
}

.question-content h4 {
  margin: 20px 0 10px 0;
  color: #333;
}

.question-description p {
  color: #666;
  line-height: 1.8;
  padding: 15px;
  background: #f5f7fa;
  border-radius: 6px;
}

.question-requirements ul {
  list-style: none;
  padding: 0;
}

.question-requirements li {
  padding: 8px 0 8px 20px;
  position: relative;
  color: #666;
}

.question-requirements li:before {
  content: '•';
  position: absolute;
  left: 0;
  color: #409eff;
  font-weight: bold;
}

.answer-area {
  margin-top: 20px;
}

.action-buttons {
  margin-top: 20px;
  display: flex;
  gap: 10px;
}
</style>