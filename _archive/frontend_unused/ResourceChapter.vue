<template>
  <div class="chapter-resource">
    <div class="top-bar">
      <span class="back-link" @click="$router.back()">← 返回</span>
      <h2>{{ chapter?.label }} · {{ chapter?.title }} — {{ typeLabel }}</h2>
    </div>

    <div v-if="loading" class="loading-area">
      <p>🤖 AI 正在为你生成 {{ typeLabel }}...</p>
    </div>

    <template v-else>
      <!-- 思维导图 -->
      <div v-if="resourceType === 'mindmap'" class="content-card">
        <h3>🧠 思维导图</h3>
        <div class="mindmap-box">
          <pre><code>{{ content }}</code></pre>
        </div>
      </div>

      <!-- 练习题目 -->
      <div v-if="resourceType === 'quiz'" class="content-card">
        <h3>📝 练习题目</h3>
        <div v-for="(q, i) in visibleQuestions" :key="i" class="quiz-item">
          <div class="q-header">
            <span class="q-num">{{ i + 1 }}</span>
            <span>{{ q.question || q.title || '题目' }}</span>
            <el-button text size="small" type="danger" @click="hideQuestion(i)">不喜欢</el-button>
          </div>
          <div class="q-options" v-if="q.options && q.options.length">
            <div v-for="(opt, j) in q.options" :key="j" :class="['opt', { correct: q.showAnswer && j === q.answer }]">
              {{ opt }}
            </div>
          </div>
          <el-button v-if="!q.showAnswer" text type="primary" size="small" @click="q.showAnswer = true">显示答案</el-button>
          <div v-if="q.showAnswer" class="q-answer">
            答案：{{ q.options ? q.options[q.answer] : q.answer || q.explanation }}
          </div>
        </div>
      </div>

      <!-- 拓展阅读 -->
      <div v-if="resourceType === 'reading'" class="content-card">
        <h3>📖 拓展阅读</h3>
        <div class="reading-box" v-html="content"></div>
      </div>

      <!-- 代码案例 -->
      <div v-if="resourceType === 'code'" class="content-card">
        <h3>💻 代码案例</h3>
        <div class="code-box">
          <pre><code>{{ content }}</code></pre>
        </div>
      </div>

      <!-- 反馈 -->
      <div class="feedback-bar">
        <span>对内容满意吗？</span>
        <el-button :type="liked ? 'primary' : ''" size="small" circle @click="like">👍</el-button>
        <el-button :type="disliked ? 'danger' : ''" size="small" circle @click="dislike">👎</el-button>
        <el-divider direction="vertical" />
        <el-radio-group v-model="difficulty" size="small">
          <el-radio-button value="easy">太简单</el-radio-button>
          <el-radio-button value="ok">刚好</el-radio-button>
          <el-radio-button value="hard">太难</el-radio-button>
        </el-radio-group>
      </div>
    </template>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, reactive, onMounted } from 'vue'
import { useRoute } from 'vue-router'
import { ElMessage } from 'element-plus'
import { generateMindmap, generateQuiz, generateReading, generateCode } from '@/api/resource'

const route = useRoute()

const chapterId = computed(() => Number(route.params.chapterId) || 1)
const resourceType = computed(() => (route.params.type as string) || 'mindmap')
const loading = ref(true)
const content = ref('')
const questions = ref<any[]>([])
const hiddenSet = ref<Set<number>>(new Set())
const liked = ref(false)
const disliked = ref(false)
const difficulty = ref('ok')

const typeMap: Record<string, string> = {
  mindmap: '思维导图', quiz: '练习题目', reading: '拓展阅读', code: '代码案例',
}
const typeLabel = computed(() => typeMap[resourceType.value] || resourceType.value)

const chapters = [
  { id: 1, label: '第1章', title: 'Java 基础语法', desc: '变量、数据类型、运算符、流程控制' },
  { id: 2, label: '第2章', title: '面向对象', desc: '类与对象、继承、多态、接口' },
  { id: 3, label: '第3章', title: '集合框架', desc: 'List、Map、Set 及常用实现类' },
]
const chapter = computed(() => chapters.find(c => c.id === chapterId.value))

const visibleQuestions = computed(() => {
  return questions.value.filter((_, i) => !hiddenSet.value.has(i)).slice(0, 3)
})

const hideQuestion = (i: number) => {
  hiddenSet.value = new Set([...hiddenSet.value, i])
}

const like = () => {
  liked.value = !liked.value
  if (liked.value) disliked.value = false
}

const dislike = () => {
  disliked.value = !disliked.value
  if (disliked.value) liked.value = false
}

const loadContent = async () => {
  loading.value = true
  const ch = chapter.value
  const params = {
    chapter: ch?.title || 'Java',
    topic: ch?.title || 'Java',
    resourceType: resourceType.value,
    level: 'basic',
  }
  try {
    switch (resourceType.value) {
      case 'mindmap': {
        const r = await generateMindmap(params)
        content.value = r.content
        break
      }
      case 'reading': {
        const r = await generateReading(params)
        content.value = r.content
        break
      }
      case 'code': {
        const r = await generateCode(params)
        content.value = r.content
        break
      }
      case 'quiz': {
        const r = await generateQuiz(params)
        try {
          const parsed = JSON.parse(r.content)
          questions.value = Array.isArray(parsed) ? parsed : [parsed]
        } catch {
          questions.value = [
            { question: 'AI 生成中遇到问题，请重试', options: [], answer: 0, showAnswer: false }
          ]
        }
        break
      }
    }
  } catch {
    ElMessage.error('AI 生成失败，请重试')
  }
  loading.value = false
}

onMounted(loadContent)
</script>

<style scoped>
.chapter-resource { padding: 32px 40px; max-width: 900px; margin: 0 auto; background: #fff; min-height: 100vh; }
.top-bar { margin-bottom: 24px; }
.top-bar h2 { margin: 0; font-size: 20px; font-weight: 700; }
.back-link { color: #4f8cff; cursor: pointer; font-size: 13px; }
.loading-area { text-align: center; padding: 80px 0; color: #909399; font-size: 15px; }
.content-card { background: #f8f9fb; border-radius: 14px; padding: 24px; margin-bottom: 20px; }
.content-card h3 { margin: 0 0 16px; font-size: 17px; }
.mindmap-box pre, .code-box pre { background: #fff; padding: 16px; border-radius: 8px; overflow-x: auto; font-size: 13px; }
.reading-box { line-height: 1.8; color: #333; }
.reading-box :deep(h4) { margin: 16px 0 6px; }
.reading-box :deep(p) { margin: 0 0 10px; }
.quiz-item { background: #fff; padding: 16px; border-radius: 8px; margin-bottom: 10px; }
.q-header { display: flex; align-items: center; gap: 8px; font-weight: 500; }
.q-num { display: inline-flex; width: 24px; height: 24px; border-radius: 50%; background: #4f8cff; color: #fff; align-items: center; justify-content: center; font-size: 12px; flex-shrink: 0; }
.q-options { display: flex; flex-wrap: wrap; gap: 8px; margin: 10px 0 0 32px; }
.opt { padding: 6px 14px; background: #f0f2f5; border-radius: 6px; font-size: 13px; }
.opt.correct { background: #e6f7e6; color: #52c41a; font-weight: 600; }
.q-answer { margin: 8px 0 0 32px; color: #52c41a; font-weight: 500; }
.feedback-bar { display: flex; align-items: center; gap: 8px; padding: 16px; background: #f8f9fb; border-radius: 12px; font-size: 13px; }
</style>
