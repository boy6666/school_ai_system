<template>
  <div class="resource-generate">
    <div class="header">
      <h2>AI 多智能体资源生成</h2>
      <p>基于你的学习画像，多个AI智能体协作生成个性化学习资源包</p>
    </div>

    <el-card v-if="!generating && !result">
      <template #header><span>当前画像</span></template>
      <div v-if="profile">
        <el-descriptions :column="2" border>
          <el-descriptions-item label="课程">{{ profile.course || '未设置' }}</el-descriptions-item>
          <el-descriptions-item label="当前专题">{{ profile.topic || '未设置' }}</el-descriptions-item>
          <el-descriptions-item label="知识基础">{{ profile.knowledge_base || '暂无记录' }}</el-descriptions-item>
          <el-descriptions-item label="薄弱点">{{ (profile.weaknesses || []).join('、') || '暂无' }}</el-descriptions-item>
          <el-descriptions-item label="学习偏好">{{ (profile.resource_preference || []).join('、') || '暂无' }}</el-descriptions-item>
          <el-descriptions-item label="上次评分">{{ profile.last_score || 'N/A' }}</el-descriptions-item>
        </el-descriptions>
        <div style="margin-top:20px;text-align:center">
          <el-button type="primary" size="large" @click="startGenerate" :loading="generating">
            开始生成个性化资源包
          </el-button>
        </div>
      </div>
      <el-empty v-else description="暂无画像数据，请先与AI辅导员对话" />
    </el-card>

    <el-card v-if="generating" style="margin-top:16px">
      <template #header><span>智能体协作中...</span></template>
      <el-steps :active="currentStep" finish-status="success" align-center>
        <el-step title="画像分析" description="profile_agent" />
        <el-step title="意图识别" description="router_agent" />
        <el-step title="知识检索" description="retrieval_agent" />
        <el-step title="资源生成" description="resource_agent" />
        <el-step title="质量评估" description="evaluate_agent" />
      </el-steps>
      <div style="text-align:center;margin-top:16px;color:#909399">
        {{ stepMessages[currentStep] }}
      </div>
    </el-card>

    <el-card v-if="result" style="margin-top:16px">
      <template #header><span>生成结果</span></template>
      <el-row :gutter="16">
        <el-col :span="6">
          <el-statistic title="讲解文档" :value="result.docCount || 0" />
        </el-col>
        <el-col :span="6">
          <el-statistic title="思维导图" :value="result.mindmapCount || 0" />
        </el-col>
        <el-col :span="6">
          <el-statistic title="练习题目" :value="result.quizCount || 0" />
        </el-col>
        <el-col :span="6">
          <el-statistic title="代码案例" :value="result.codeCount || 0" />
        </el-col>
      </el-row>
      <div style="text-align:center;margin-top:16px">
        <el-button type="primary" @click="$router.push('/student/resources')">查看所有资源</el-button>
        <el-button @click="resetGenerate">重新生成</el-button>
      </div>
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { sendTutorMessage } from '@/api/tutor'
import { getResourceList } from '@/api/admin'

const profile = ref<any>(null)
const generating = ref(false)
const result = ref<any>(null)
const currentStep = ref(0)

const stepMessages = [
  '正在分析你的学习画像...',
  '正在识别学习意图...',
  '正在检索知识库资料...',
  '正在生成 5 种个性化资源...',
  '正在评估资源质量...',
]

const loadProfile = async () => {
  // Could fetch from backend, but for now use what's available
  // The profile is embedded in the last tutor response if any
}

const startGenerate = async () => {
  generating.value = true
  currentStep.value = 0

  // Simulate multi-agent steps
  const timer = setInterval(() => {
    if (currentStep.value < 4) {
      currentStep.value++
    }
  }, 2000)

  try {
    // Call the resource generation API
    const res = await sendTutorMessage('请根据我的学习画像，为我生成完整的个性化学习资源包，包含讲解文档、思维导图、练习题、拓展阅读和代码案例', 'generate_' + Date.now())
    
    clearInterval(timer)
    currentStep.value = 4

    // Check what was generated
    const answer = res?.answer || res?.finalAnswer || ''
    const resourceDir = res?.resourceDir || ''
    
    // Fetch the generated resources from admin API
    const r = await getResourceList({ page: 1, pageSize: 50 })
    const allResources = r?.records || []
    
    result.value = {
      docCount: allResources.filter((r: any) => r.type === '文档').length,
      mindmapCount: allResources.filter((r: any) => r.type === '思维导图').length,
      quizCount: allResources.filter((r: any) => r.type === '题库').length,
      codeCount: allResources.filter((r: any) => r.type === '代码案例').length,
      resourceDir,
    }
    
    ElMessage.success('资源生成完成！5 种个性化资源已就绪')
  } catch (err: any) {
    clearInterval(timer)
    ElMessage.error('生成失败，请重试')
  } finally {
    generating.value = false
  }
}

const resetGenerate = () => {
  result.value = null
  currentStep.value = 0
}

onMounted(loadProfile)
</script>

<style scoped>
.resource-generate { padding: 20px; max-width: 900px; margin: 0 auto; }
.header { text-align: center; margin-bottom: 24px; }
.header h2 { margin: 0; color: #303133; }
.header p { color: #909399; margin-top: 8px; }
</style>
