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

    <div v-if="!isDoingPractice">
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
        style="margin-top: 16px; text-align: center"
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
            <el-button @click="handleSubmit">提交答案</el-button>
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

interface Question {
  id: number
  title: string
  description: string
  subject: string
  difficulty: string
  requirements: string[]
  views: number
  completed: number
}

const filters = reactive({
  subject: '',
  difficulty: '',
  keyword: ''
})

const pagination = reactive({
  page: 1,
  size: 10,
  total: 50
})

const isDoingPractice = ref(false)
const currentQuestion = ref<Question | null>(null)
const answer = ref('')

const questions = ref<Question[]>([
  {
    id: 1,
    title: 'Vue组件基础练习',
    description: '创建一个简单的计数器组件，包含增加、减少、重置功能',
    subject: 'frontend',
    difficulty: 'easy',
    requirements: [
      '使用Vue 3组合式API',
      '实现计数器状态管理',
      '添加样式美化'
    ],
    views: 1234,
    completed: 567
  },
  {
    id: 2,
    title: 'RESTful API设计',
    description: '设计一个用户管理系统的RESTful API接口',
    subject: 'backend',
    difficulty: 'medium',
    requirements: [
      '设计CRUD接口',
      '使用Spring Boot实现',
      '添加参数验证'
    ],
    views: 987,
    completed: 234
  },
  {
    id: 3,
    title: '数据库查询优化',
    description: '优化一个复杂的SQL查询语句',
    subject: 'database',
    difficulty: 'hard',
    requirements: [
      '分析查询性能问题',
      '设计优化方案',
      '验证优化效果'
    ],
    views: 654,
    completed: 123
  },
  {
    id: 4,
    title: '电商系统前端开发',
    description: '开发一个电商系统的前端页面',
    subject: 'project',
    difficulty: 'medium',
    requirements: [
      '使用Vue 3 + Element Plus',
      '实现商品展示、购物车功能',
      '对接后端API'
    ],
    views: 876,
    completed: 345
  },
  {
    id: 5,
    title: 'React组件通信',
    description: '实现父子组件、兄弟组件之间的通信',
    subject: 'frontend',
    difficulty: 'easy',
    requirements: [
      '使用Context API',
      '实现事件冒泡和捕获',
      '添加错误处理'
    ],
    views: 1123,
    completed: 456
  }
])

const getDifficultyType = (difficulty = '') => {
  const types: Record<string, any> = {
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
  ElMessage.success('搜索功能开发中...')
}

const handleReset = () => {
  filters.subject = ''
  filters.difficulty = ''
  filters.keyword = ''
  ElMessage.success('重置成功')
}

const handleSizeChange = (size: number) => {
  pagination.size = size
}

const handlePageChange = (page: number) => {
  pagination.page = page
}

const startPractice = (question: Question) => {
  currentQuestion.value = question
  isDoingPractice.value = true
  answer.value = ''
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

const handleSubmit = () => {
  if (!answer.value.trim()) {
    ElMessage.warning('请输入您的答案')
    return
  }

  ElMessage.success('答案提交成功！系统正在评分...')

  setTimeout(() => {
    ElMessageBox.alert(
      '您的答案已提交！\n\n得分：85分\n评价：思路清晰，实现基本正确，建议在错误处理方面进一步完善。',
      '练习结果',
      {
        confirmButtonText: '查看详情',
        callback: () => {
          isDoingPractice.value = false
          currentQuestion.value = null
          answer.value = ''
        }
      }
    )
  }, 2000)
}

const handleHint = () => {
  if (currentQuestion.value) {
    ElMessageBox.alert(
      '提示：\n1. 先理解题目需求\n2. 设计算法思路\n3. 逐步实现功能\n4. 测试边界情况',
      '练习提示',
      {
        confirmButtonText: '知道了'
      }
    )
  }
}
</script>

<style scoped>
.practice-page {
  padding: 16px;
}

.filter-card {
  margin-bottom: 16px;
  border-radius: 8px;
}

.question-list {
  margin-bottom: 16px;
}

.question-card {
  margin-bottom: 16px;
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
  gap: 8px;
  margin-bottom: 16px;
}

.question-title {
  font-weight: bold;
  font-size: 16px;
  color: #333;
}

.question-desc {
  color: #666;
  margin-bottom: 16px;
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
  margin-left: 16px;
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
  margin: 8px 0 0 0;
  font-size: 18px;
}

.question-content h4 {
  margin: 16px 0 8px 0;
  color: #333;
}

.question-description p {
  color: #666;
  line-height: 1.8;
  padding: 16px;
  background: #f5f7fa;
  border-radius: 6px;
}

.question-requirements ul {
  list-style: none;
  padding: 0;
}

.question-requirements li {
  padding: 8px 0 8px 16px;
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
  margin-top: 16px;
}

.action-buttons {
  margin-top: 16px;
  display: flex;
  gap: 8px;
}
</style>
