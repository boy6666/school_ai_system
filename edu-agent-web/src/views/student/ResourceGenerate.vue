<template>
  <div class="generate-page">
    <div class="top-bar">
      <el-button text @click="$router.back()">← 返回</el-button>
      <h2>{{ typeLabel }} · AI 生成</h2>
    </div>

    <!-- 加载中 -->
    <div v-if="loading" class="loading-area">
      <el-icon class="is-loading" :size="32"><Loading /></el-icon>
      <p>AI 正在为你生成 {{ typeLabel }}...</p>
    </div>

    <!-- 内容区 -->
    <div v-else-if="content || questions.length" class="content-area">
      <!-- 思维导图 -->
      <div v-if="resourceType === 'mindmap'" class="mindmap-box">
        <pre><code>{{ content }}</code></pre>
      </div>

      <!-- 练习题目 -->
      <div v-else-if="resourceType === 'quiz'" class="quiz-box">
        <div v-for="(q, i) in questions" :key="i" class="quiz-item">
          <div class="q-header">
            <span class="q-num">{{ i + 1 }}</span>
            <span>{{ q.question || q.title || '题目' }}</span>
          </div>
          <div v-if="q.options?.length" class="q-options">
            <div v-for="(opt, j) in q.options" :key="j" :class="['opt', { correct: q.showAnswer && j === q.answer }]">
              {{ opt }}
            </div>
          </div>
          <el-button v-if="!q.showAnswer" text type="primary" size="small" @click="q.showAnswer = true">
            显示答案
          </el-button>
          <div v-if="q.showAnswer" class="q-answer">
            答案：{{ q.options ? q.options[q.answer] : q.answer || q.explanation }}
          </div>
        </div>
      </div>

      <!-- 拓展阅读 -->
      <div v-else-if="resourceType === 'reading'" class="reading-box" v-html="content"></div>

      <!-- 代码案例 -->
      <div v-else class="code-box">
        <pre><code>{{ content }}</code></pre>
      </div>

      <!-- ===== 反馈区：记录画像 + 换个方向重新生成 ===== -->
      <div class="feedback-bar">
        <span>这个内容对你有帮助吗？</span>
        <el-button :type="liked ? 'primary' : 'default'" size="small" circle @click="like">👍</el-button>
        <el-button :type="disliked ? 'danger' : 'default'" size="small" circle @click="dislike">👎</el-button>
        <el-divider direction="vertical" />
        <span>难度：</span>
        <el-radio-group v-model="difficulty" size="small" @change="onDifficultyChange">
          <el-radio-button value="easy">太简单</el-radio-button>
          <el-radio-button value="ok">刚好</el-radio-button>
          <el-radio-button value="hard">太难</el-radio-button>
        </el-radio-group>
      </div>
    </div>

    <!-- 空状态 -->
    <div v-else class="empty-area">
      <el-empty description="点击按钮开始生成">
        <el-button type="primary" @click="doGenerate" :loading="loading">
          生成 {{ typeLabel }}
        </el-button>
      </el-empty>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { useRoute } from 'vue-router'
import { ElMessage } from 'element-plus'
import { Loading } from '@element-plus/icons-vue'
import { generateResource } from '@/api/resource'
import { saveProfile } from '@/api/profile'
import { useUserStore } from '@/stores/user'

const route = useRoute()
const userStore = useUserStore()

const resourceType = computed(() => (route.params.type as string) || 'mindmap')
const loading = ref(false)
const content = ref('')
const questions = ref<any[]>([])
const liked = ref(false)
const disliked = ref(false)
const difficulty = ref('ok')

const typeMap: Record<string, string> = {
  mindmap: '思维导图', quiz: '练习题目',
  reading: '拓展阅读', code: '代码案例',
}
const typeLabel = computed(() => typeMap[resourceType.value] || resourceType.value)

/** 调用 AI 生成资源 */
const doGenerate = async (typeOverride?: string) => {
  loading.value = true
  content.value = ''
  questions.value = []

  const params = {
    chapterId: 0,
    chapterName: 'Java 程序设计',
    topic: 'Java',
    type: typeOverride || resourceType.value,
    difficulty: 'medium',
  }

  try {
    const res = await generateResource(params)

    if (resourceType.value === 'quiz') {
      // quiz 尝试解析 JSON 题目列表
      try {
        const parsed = JSON.parse(res.content)
        questions.value = Array.isArray(parsed) ? parsed : [parsed]
        // 给每题加上 showAnswer 控制
        questions.value = questions.value.map((q: any) => ({ ...q, showAnswer: false }))
      } catch {
        content.value = res.content
      }
    } else {
      content.value = res.content
    }
  } catch {
    ElMessage.error('AI 生成失败，请重试')
  }
  loading.value = false
}

/** 难度反馈：记录画像 + 换个方向重新生成 */
const onDifficultyChange = async (val: string) => {
  // 1. 记录画像
  const userId = userStore.userInfo?.id
  if (userId) {
    try {
      await saveProfile({
        userId,
        difficulty_preference: val,
        resource_type: resourceType.value,
      })
    } catch {
      // 画像保存失败不影响核心流程
    }
  }

  // 2. AI 换个方向重新生成（不增不减难度，只是换讲解角度）
  ElMessage.info('正在根据你的反馈调整内容方向...')
  await doGenerate()
  ElMessage.success('已换个方向重新生成')
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

onMounted(() => {
  doGenerate()
})
</script>

<style scoped>
.generate-page { padding: 24px; max-width: 900px; margin: 0 auto; }
.top-bar { display: flex; align-items: center; gap: 12px; margin-bottom: 24px; }
.top-bar h2 { margin: 0; font-size: 18px; }

.loading-area { text-align: center; padding: 80px 0; color: #909399; }
.loading-area p { margin-top: 16px; font-size: 15px; }

.content-area { background: #fff; border-radius: 10px; padding: 24px; border: 1px solid #ebeef5; }

/* 思维导图 & 代码 */
.mindmap-box pre,
.code-box pre { background: #f5f7fa; padding: 16px; border-radius: 8px; overflow-x: auto; font-size: 13px; }

/* 拓展阅读 */
.reading-box { line-height: 1.8; color: #333; }
.reading-box :deep(h4) { margin: 16px 0 6px; }
.reading-box :deep(p) { margin: 0 0 10px; }

/* 练习题目 */
.quiz-item { background: #f8f9fb; padding: 16px; border-radius: 8px; margin-bottom: 10px; }
.q-header { display: flex; align-items: center; gap: 8px; font-weight: 500; }
.q-num { display: inline-flex; width: 24px; height: 24px; border-radius: 50%; background: #4f8cff; color: #fff; align-items: center; justify-content: center; font-size: 12px; flex-shrink: 0; }
.q-options { display: flex; flex-wrap: wrap; gap: 8px; margin: 10px 0 0 32px; }
.opt { padding: 6px 14px; background: #f0f2f5; border-radius: 6px; font-size: 13px; }
.opt.correct { background: #e6f7e6; color: #52c41a; font-weight: 600; }
.q-answer { margin: 8px 0 0 32px; color: #52c41a; font-weight: 500; }

/* 反馈栏 */
.feedback-bar {
  margin-top: 20px; padding: 16px; border-top: 1px solid #ebeef5;
  display: flex; align-items: center; gap: 8px; font-size: 13px; color: #606266;
  flex-wrap: wrap;
}

.empty-area { padding: 80px 0; }
</style>
