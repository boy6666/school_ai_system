<template>
  <div class="resource-center">
    <div class="page-header">
      <h2>AI 多智能体 · 个性化学习资源</h2>
      <p>讲解文档 + 思维导图 + 练习题目 + 拓展阅读 + 代码案例</p>
      <el-button type="primary" size="large" @click="startGenerate" :loading="generating" style="margin-top:12px">
        {{ generating ? '多智能体协作中...' : 'AI 生成资源' }}
      </el-button>
    </div>

    <!-- 多智能体协作步骤 -->
    <el-alert v-if="generating" title="智能体协作流程" type="info" :closable="false" style="margin-bottom: 16px">
      <template #default>
        <div style="font-size:12px;color:#606266">
          画像分析 → 意图识别 → 知识检索 → <strong>资源生成</strong> → 质量评估
        </div>
      </template>
    </el-alert>

    <div class="type-filter">
      <el-radio-group v-model="activeType" size="default">
        <el-radio-button value="">全部</el-radio-button>
        <el-radio-button value="文档">讲解文档</el-radio-button>
        <el-radio-button value="思维导图">思维导图</el-radio-button>
        <el-radio-button value="题库">练习题目</el-radio-button>
        <el-radio-button value="拓展阅读">拓展阅读</el-radio-button>
        <el-radio-button value="代码案例">代码案例</el-radio-button>
      </el-radio-group>
    </div>

    <div v-if="loading" style="text-align:center;padding:40px;color:#909399">加载中...</div>

    <el-empty v-else-if="filteredResources.length === 0" description="暂无资源">
      <el-button type="primary" @click="startGenerate">AI 生成资源</el-button>
    </el-empty>

    <el-row v-else :gutter="16">
      <el-col v-for="item in filteredResources" :key="item.id" :span="8" style="margin-bottom:16px">
        <el-card shadow="hover">
          <el-tag :type="tagType(item.type)" size="small">{{ item.type }}</el-tag>
          <div style="font-weight:bold;margin-top:8px">{{ item.title }}</div>
          <div style="color:#909399;font-size:13px;margin-top:4px">{{ item.description || '' }}</div>
        </el-card>
      </el-col>
    </el-row>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, computed } from 'vue'
import { ElMessage } from 'element-plus'
import { getAdminResourceList, generateResources } from '@/api/admin'

const resources = ref<any[]>([])
const loading = ref(false)
const generating = ref(false)
const activeType = ref('')

const filteredResources = computed(() => {
  if (!activeType.value) return resources.value
  return resources.value.filter((r: any) => r.type === activeType.value)
})

const tagType = (type: string) => {
  const map: Record<string, string> = {
    '文档': 'success', '思维导图': 'warning', '题库': 'danger',
    '拓展阅读': 'info', '代码案例': '',
  }
  return map[type] || ''
}

const loadResources = async () => {
  loading.value = true
  try {
    const r = await getAdminResourceList({ page: 1, pageSize: 50 })
    resources.value = r?.records || []
  } catch { resources.value = [] }
  loading.value = false
}

const startGenerate = async () => {
  generating.value = true
  try {
    const res = await generateResources('9')
    ElMessage.success(`生成完成！导入 ${res?.imported || 0} 个资源`)
    await loadResources()
  } catch (err: any) {
    ElMessage.error('生成失败，请确认 AI 引擎正在运行')
  } finally {
    generating.value = false
  }
}

onMounted(loadResources)
</script>

<style scoped>
.resource-center { padding: 16px; max-width: 1200px; margin: 0 auto; }
.page-header { text-align: center; margin-bottom: 16px; }
.page-header h2 { margin: 0; }
.page-header p { color: #909399; margin-top: 8px; }
.type-filter { text-align: center; margin-bottom: 16px; }
</style>
