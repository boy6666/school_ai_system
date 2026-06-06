<template>
  <div class="generate-page">
    <div class="top-bar">
      <el-button text @click="$router.back()">← 返回</el-button>
      <h2>{{ typeLabel }} · AI 生成</h2>
    </div>

    <div v-if="loading" class="loading-area">
      <el-icon class="is-loading" :size="32"><Loading /></el-icon>
      <p>AI 正在为你生成 {{ typeLabel }}...</p>
    </div>

    <div v-else-if="content" class="content-area">
      <div v-if="resourceType === 'mindmap'" class="mindmap-box">
        <pre><code>{{ content }}</code></pre>
      </div>
      <div v-else-if="resourceType === 'quiz'" class="quiz-box">
        <pre>{{ content }}</pre>
      </div>
      <div v-else-if="resourceType === 'reading'" class="reading-box">
        <div v-html="content"></div>
      </div>
      <div v-else class="code-box">
        <pre><code>{{ content }}</code></pre>
      </div>

      <!-- 反馈区 -->
      <div class="feedback-bar">
        <span>这个内容对你有帮助吗？</span>
        <el-button :type="liked ? 'primary' : 'default'" size="small" circle @click="like">
          👍
        </el-button>
        <el-button :type="disliked ? 'danger' : 'default'" size="small" circle @click="dislike">
          👎
        </el-button>
        <el-divider direction="vertical" />
        <span>难度：</span>
        <el-radio-group v-model="difficulty" size="small" @change="onDifficultyChange">
          <el-radio-button value="easy">太简单</el-radio-button>
          <el-radio-button value="ok">刚好</el-radio-button>
          <el-radio-button value="hard">太难</el-radio-button>
        </el-radio-group>
      </div>
    </div>

    <div v-else class="empty-area">
      <el-empty description="点击按钮开始生成">
        <el-button type="primary" @click="doGenerate" :loading="loading">生成 {{ typeLabel }}</el-button>
      </el-empty>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { useRoute } from 'vue-router'
import { ElMessage } from 'element-plus'
import { Loading } from '@element-plus/icons-vue'
import { generateMindmap, generateQuiz, generateReading, generateCode } from '@/api/resource'

const route = useRoute()
const resourceType = computed(() => (route.params.type as string) || 'mindmap')
const loading = ref(false)
const content = ref('')
const liked = ref(false)
const disliked = ref(false)
const difficulty = ref('')

const typeMap: Record<string, string> = {
  mindmap: '思维导图', quiz: '练习题目',
  reading: '拓展阅读', code: '代码案例',
}
const typeLabel = computed(() => typeMap[resourceType.value] || resourceType.value)

const params = { chapter: 'Java 程序设计', topic: '通用', resourceType: resourceType.value, level: 'basic' }

const doGenerate = async () => {
  loading.value = true
  try {
    const fn: Record<string, Function> = {
      mindmap: generateMindmap, quiz: generateQuiz,
      reading: generateReading, code: generateCode,
    }
    const result = await (fn[resourceType.value] || generateMindmap)(params)
    content.value = result.content
  } catch {
    ElMessage.error('AI 生成失败')
  }
  loading.value = false
}

const like = () => {
  liked.value = !liked.value
  if (liked.value) disliked.value = false
  ElMessage.success('感谢反馈！')
}

const dislike = () => {
  disliked.value = !disliked.value
  if (disliked.value) liked.value = false
  ElMessage.info('我们会优化内容')
}

const onDifficultyChange = () => {
  ElMessage.success('难度反馈已记录')
}

onMounted(() => {
  doGenerate()
})
</script>

<style scoped>
.generate-page { padding: 24px; max-width: 900px; margin: 0 auto; }
.top-bar { display: flex; align-items: center; gap: 12px; margin-bottom: 24px; }
.top-bar h2 { margin: 0; font-size: 18px; }
.loading-area { text-align: center; padding: 80px 0; color: #909399; }
.content-area { background: #fff; border-radius: 10px; padding: 24px; border: 1px solid #ebeef5; }
.mindmap-box pre, .code-box pre { background: #f5f7fa; padding: 16px; border-radius: 8px; overflow-x: auto; font-size: 13px; }
.quiz-box pre { white-space: pre-wrap; }
.reading-box { line-height: 1.8; }
.feedback-bar {
  margin-top: 20px; padding: 16px; border-top: 1px solid #ebeef5;
  display: flex; align-items: center; gap: 8px; font-size: 13px; color: #606266;
}
.empty-area { padding: 80px 0; }
</style>
