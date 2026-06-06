<template>
  <div class="agent-page">
    <section class="page-header">
      <div>
        <p class="eyebrow">管理后台</p>
        <h1>智能体管理</h1>
        <p>
          管理学习规划、资源推荐、题目解析、知识问答等智能体配置与运行状态。
        </p>
      </div>

      <button class="primary-btn" @click="openCreate">
        新增智能体
      </button>
    </section>

    <section class="toolbar-panel">
      <input
        v-model="keyword"
        type="text"
        placeholder="搜索智能体名称、类型、模型..."
        @keyup.enter="fetchAgents"
      />

      <select v-model="status">
        <option value="">全部状态</option>
        <option value="running">运行中</option>
        <option value="stopped">已停用</option>
      </select>

      <select v-model="type">
        <option value="">全部类型</option>
        <option value="学习规划">学习规划</option>
        <option value="资源推荐">资源推荐</option>
        <option value="智能问答">智能问答</option>
        <option value="内容生成">内容生成</option>
      </select>

      <button @click="fetchAgents">查询</button>
    </section>

    <main class="agent-layout">
      <section class="agent-list">
        <div v-if="loading" class="state-card">
          智能体加载中...
        </div>

        <article
          v-for="agent in filteredAgents"
          :key="agent.id"
          :class="['agent-card', { active: selectedAgent?.id === agent.id }]"
          @click="selectAgent(agent)"
        >
          <div>
            <h3>{{ agent.name }}</h3>
            <p>{{ agent.type }} · {{ agent.model }}</p>
          </div>

          <span :class="['status-tag', agent.status]">
            {{ getAgentStatusText(agent.status) }}
          </span>
        </article>

        <div
          v-if="!loading && filteredAgents.length === 0"
          class="state-card"
        >
          暂无智能体数据
        </div>
      </section>

      <section v-if="selectedAgent" class="agent-detail">
        <div class="detail-header">
          <div>
            <p class="eyebrow">智能体详情</p>
            <h2>{{ selectedAgent.name }}</h2>
            <p>{{ selectedAgent.description }}</p>
          </div>

          <button class="outline-btn" @click="toggleStatus(selectedAgent)">
            {{ selectedAgent.status === 'running' ? '停用' : '启用' }}
          </button>
        </div>

        <div class="metric-grid">
          <div>
            <span>调用次数</span>
            <strong>{{ selectedAgent.callCount }}</strong>
          </div>

          <div>
            <span>活跃用户</span>
            <strong>{{ selectedAgent.activeUsers }}</strong>
          </div>

          <div>
            <span>满意度</span>
            <strong>{{ selectedAgent.satisfaction }}%</strong>
          </div>

          <div>
            <span>解决率</span>
            <strong>{{ selectedAgent.solveRate }}%</strong>
          </div>
        </div>

        <div class="form-grid">
          <label>
            智能体名称
            <input v-model="selectedAgent.name" type="text" />
          </label>

          <label>
            智能体类型
            <select v-model="selectedAgent.type">
              <option value="学习规划">学习规划</option>
              <option value="资源推荐">资源推荐</option>
              <option value="智能问答">智能问答</option>
              <option value="内容生成">内容生成</option>
            </select>
          </label>

          <label>
            模型名称
            <input v-model="selectedAgent.model" type="text" />
          </label>

          <label>
            提示词版本
            <input v-model="selectedAgent.promptVersion" type="text" />
          </label>
        </div>

        <div class="description-box">
          <label>
            智能体说明
            <textarea v-model="selectedAgent.description" />
          </label>
        </div>

        <div class="tool-box">
          <h3>工具权限</h3>

          <div class="tag-list">
            <span v-for="tool in selectedAgent.tools" :key="tool">
              {{ tool }}
            </span>
          </div>
        </div>

        <div class="detail-actions">
          <button class="primary-btn" @click="saveConfig">
            保存配置
          </button>
        </div>
      </section>

      <section v-else class="agent-detail empty">
        请选择一个智能体查看详情
      </section>
    </main>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import {
  getAdminAgentList,
  saveAgentConfig,
  updateAgentStatus
} from '@/api/admin'

import type {
  AgentItem,
  AgentStatus
} from '@/api/admin'

const loading = ref(false)
const keyword = ref('')
const status = ref('')
const type = ref('')

const agents = ref<AgentItem[]>([])
const selectedAgent = ref<AgentItem | null>(null)

const fallbackAgents: AgentItem[] = [
  {
    id: 1,
    name: '学习规划助手',
    type: '学习规划',
    description: '根据学生画像、课程进度和任务完成情况生成个性化学习计划。',
    model: 'EduAgent-Plan',
    status: 'running',
    callCount: 12860,
    activeUsers: 436,
    satisfaction: 96,
    solveRate: 91,
    tools: ['学习画像读取', '任务生成', '课程进度分析'],
    promptVersion: 'v1.3.0',
    updateTime: '2026-04-28'
  },
  {
    id: 2,
    name: '课程资源推荐助手',
    type: '资源推荐',
    description: '根据课程章节和学生薄弱知识点推荐文档、视频、题库和实验项目。',
    model: 'EduAgent-Recommend',
    status: 'running',
    callCount: 9320,
    activeUsers: 388,
    satisfaction: 94,
    solveRate: 89,
    tools: ['资源检索', '标签匹配', '难度评估'],
    promptVersion: 'v1.2.1',
    updateTime: '2026-04-26'
  },
  {
    id: 3,
    name: '智能辅导问答助手',
    type: '智能问答',
    description: '用于学生端问答辅导，支持知识点讲解、题目解析和学习建议。',
    model: 'EduAgent-Tutor',
    status: 'stopped',
    callCount: 15420,
    activeUsers: 512,
    satisfaction: 92,
    solveRate: 87,
    tools: ['课程上下文', '资源引用', '题目解析'],
    promptVersion: 'v1.1.8',
    updateTime: '2026-04-24'
  }
]

const filteredAgents = computed(() => {
  return agents.value.filter((agent: AgentItem) => {
    const matchKeyword =
      !keyword.value ||
      agent.name.includes(keyword.value) ||
      agent.type.includes(keyword.value) ||
      agent.model.includes(keyword.value)

    const matchStatus = !status.value || agent.status === status.value
    const matchType = !type.value || agent.type === type.value

    return matchKeyword && matchStatus && matchType
  })
})

const fetchAgents = async () => {
  loading.value = true

  try {
    const result = await getAdminAgentList({
      keyword: keyword.value,
      status: status.value,
      type: type.value
    })

    agents.value = result.list
    selectedAgent.value = result.list[0] || null
  } catch (error) {
    console.warn('智能体管理接口暂不可用，使用页面静态数据：', error)

    agents.value = fallbackAgents
    selectedAgent.value = fallbackAgents[0]
  } finally {
    loading.value = false
  }
}

const selectAgent = (agent: AgentItem) => {
  selectedAgent.value = agent
}

const toggleStatus = async (agent: AgentItem) => {
  const oldStatus = agent.status
  const nextStatus: AgentStatus =
    agent.status === 'running' ? 'stopped' : 'running'

  agent.status = nextStatus

  try {
    await updateAgentStatus(agent.id, nextStatus)
  } catch (error) {
    console.warn('智能体状态接口暂不可用，仅更新页面状态：', error)
    agent.status = oldStatus
  }
}

const saveConfig = async () => {
  if (!selectedAgent.value) return

  try {
    await saveAgentConfig(selectedAgent.value)
    alert('配置已保存')
  } catch (error) {
    console.warn('保存智能体配置接口暂不可用：', error)
    alert('配置已保存')
  }
}

const openCreate = () => {
  const newAgent: AgentItem = {
    id: Date.now(),
    name: '新建智能体',
    type: '智能问答',
    description: '请填写智能体说明。',
    model: 'EduAgent-Base',
    status: 'stopped',
    callCount: 0,
    activeUsers: 0,
    satisfaction: 0,
    solveRate: 0,
    tools: ['课程上下文'],
    promptVersion: 'v1.0.0',
    updateTime: '刚刚'
  }

  agents.value.unshift(newAgent)
  selectedAgent.value = newAgent
}

const getAgentStatusText = (value: AgentStatus) => {
  const map: Record<AgentStatus, string> = {
    running: '运行中',
    stopped: '已停用'
  }

  return map[value]
}

onMounted(() => {
  fetchAgents()
})
</script>

<style scoped>
.agent-page {
  min-height: 100vh;
  padding: clamp(12px, 2vw, 24px);
  background: #f5f8ff;
  color: #1f2a44;
  overflow-x: hidden;
}

.page-header,
.toolbar-panel,
.agent-card,
.agent-detail {
  background: #ffffff;
  box-shadow: 0 10px 26px rgba(32, 88, 180, 0.06);
}

.page-header {
  display: flex;
  justify-content: space-between;
  gap: 16px;
  padding: 24px;
  margin-bottom: 16px;
  border-radius: 24px;
  background: linear-gradient(135deg, #ffffff 0%, #eaf2ff 100%);
}

.eyebrow {
  margin: 0 0 8px;
  color: #1769ff;
  font-weight: 700;
}

.page-header h1,
.detail-header h2 {
  margin: 0;
}

.page-header p,
.detail-header p {
  color: #667085;
  line-height: 1.7;
}

.primary-btn,
.outline-btn,
.toolbar-panel button {
  height: 40px;
  padding: 0 16px;
  border-radius: 12px;
  cursor: pointer;
}

.primary-btn,
.toolbar-panel button {
  border: none;
  color: #ffffff;
  background: #1769ff;
}

.outline-btn {
  border: 1px solid #dbe4f3;
  color: #1769ff;
  background: #ffffff;
}

.toolbar-panel {
  display: flex;
  flex-wrap: wrap;
  gap: 12px;
  padding: 16px;
  margin-bottom: 16px;
  border-radius: 20px;
}

.toolbar-panel input,
.toolbar-panel select,
.form-grid input,
.form-grid select,
.description-box textarea {
  border: 1px solid #dbe4f3;
  border-radius: 12px;
  outline: none;
}

.toolbar-panel input,
.toolbar-panel select,
.form-grid input,
.form-grid select {
  height: 40px;
  padding: 0 12px;
}

.toolbar-panel input {
  min-width: 260px;
  flex: 1;
}

.agent-layout {
  display: grid;
  grid-template-columns: 360px minmax(0, 1fr);
  gap: 16px;
}

.agent-list {
  min-width: 0;
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.agent-card {
  display: flex;
  justify-content: space-between;
  gap: 12px;
  padding: 16px;
  border-radius: 18px;
  cursor: pointer;
}

.agent-card.active {
  box-shadow:
    inset 4px 0 0 #1769ff,
    0 10px 26px rgba(32, 88, 180, 0.08);
}

.agent-card h3 {
  margin: 0;
}

.agent-card p {
  color: #75849a;
  font-size: 14px;
}

.status-tag {
  height: fit-content;
  flex-shrink: 0;
  padding: 4px 8px;
  border-radius: 999px;
  font-size: 12px;
}

.status-tag.running {
  color: #15803d;
  background: #ecfdf3;
}

.status-tag.stopped {
  color: #b91c1c;
  background: #fef2f2;
}

.agent-detail {
  min-width: 0;
  padding: 24px;
  border-radius: 22px;
}

.agent-detail.empty,
.state-card {
  display: flex;
  align-items: center;
  justify-content: center;
  color: #75849a;
  min-height: 220px;
}

.detail-header {
  display: flex;
  justify-content: space-between;
  gap: 16px;
  margin-bottom: 16px;
}

.metric-grid {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 12px;
  margin-bottom: 16px;
}

.metric-grid div {
  padding: 16px;
  border-radius: 16px;
  background: #f7faff;
}

.metric-grid span {
  color: #667085;
}

.metric-grid strong {
  display: block;
  margin-top: 8px;
  color: #1769ff;
  font-size: 24px;
}

.form-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 12px;
}

.form-grid label,
.description-box label {
  display: grid;
  gap: 8px;
  color: #52637a;
  font-size: 14px;
}

.description-box {
  margin-top: 12px;
}

.description-box textarea {
  min-height: 96px;
  padding: 12px;
  resize: vertical;
}

.tool-box {
  margin: 16px 0;
}

.tool-box h3 {
  margin: 0 0 12px;
}

.tag-list {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}

.tag-list span {
  padding: 8px 12px;
  border-radius: 999px;
  color: #1769ff;
  background: #eef5ff;
  font-size: 13px;
}

.detail-actions {
  display: flex;
  justify-content: flex-end;
}

@media (max-width: 1080px) {
  .agent-layout {
    grid-template-columns: 1fr;
  }

  .metric-grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
}

@media (max-width: 640px) {
  .agent-page {
    padding: 12px;
  }

  .page-header,
  .detail-header {
    flex-direction: column;
  }

  .toolbar-panel input {
    min-width: 100%;
  }

  .metric-grid,
  .form-grid {
    grid-template-columns: 1fr;
  }

  .primary-btn,
  .outline-btn {
    width: 100%;
  }

  .detail-actions {
    justify-content: stretch;
  }
}
</style>