<template>
  <div class="review-page">
    <section class="page-header">
      <div>
        <p class="eyebrow">管理后台</p>
        <h1>内容审核管理</h1>
        <p>
          审核课程资料、题库、视频、智能体生成内容，确保内容安全、准确、合规。
        </p>
      </div>
    </section>

    <section class="stat-grid">
      <div class="stat-card">
        <span>待审核</span>
        <strong>{{ stats.pending }}</strong>
      </div>

      <div class="stat-card">
        <span>已通过</span>
        <strong>{{ stats.approved }}</strong>
      </div>

      <div class="stat-card">
        <span>已拒绝</span>
        <strong>{{ stats.rejected }}</strong>
      </div>

      <div class="stat-card">
        <span>高风险</span>
        <strong>{{ stats.highRisk }}</strong>
      </div>
    </section>

    <section class="panel">
      <div class="tabs">
        <button
          v-for="tab in statusTabs"
          :key="tab.value"
          :class="{ active: status === tab.value }"
          @click="status = tab.value"
        >
          {{ tab.label }}
        </button>
      </div>

      <div class="toolbar">
        <input
          v-model="keyword"
          type="text"
          placeholder="搜索内容标题、来源、提交人..."
          @keyup.enter="fetchReviews"
        />

        <select v-model="type">
          <option value="">全部类型</option>
          <option value="课程资料">课程资料</option>
          <option value="题库">题库</option>
          <option value="视频">视频</option>
          <option value="文档">文档</option>
          <option value="智能体生成内容">智能体生成内容</option>
        </select>

        <select v-model="riskLevel">
          <option value="">全部风险</option>
          <option value="low">低风险</option>
          <option value="middle">中风险</option>
          <option value="high">高风险</option>
        </select>

        <button @click="fetchReviews">查询</button>
      </div>

      <div v-if="loading" class="state-card">
        审核内容加载中...
      </div>

      <div v-else class="review-layout">
        <div class="table-wrap">
          <table>
            <thead>
              <tr>
                <th>内容标题</th>
                <th>类型</th>
                <th>来源</th>
                <th>风险等级</th>
                <th>状态</th>
                <th>提交人</th>
                <th>提交时间</th>
                <th>操作</th>
              </tr>
            </thead>

            <tbody>
              <tr
                v-for="item in filteredReviews"
                :key="item.id"
                :class="{ active: selectedReview?.id === item.id }"
                @click="selectReview(item)"
              >
                <td>{{ item.title }}</td>
                <td>{{ item.type }}</td>
                <td>{{ item.source }}</td>
                <td>
                  <span :class="['risk-tag', item.riskLevel]">
                    {{ getRiskText(item.riskLevel) }}
                  </span>
                </td>
                <td>
                  <span :class="['status-tag', item.status]">
                    {{ getReviewStatusText(item.status) }}
                  </span>
                </td>
                <td>{{ item.submitter }}</td>
                <td>{{ item.submitTime }}</td>
                <td>
                  <button class="text-btn" @click.stop="selectReview(item)">
                    预览
                  </button>
                </td>
              </tr>
            </tbody>
          </table>

          <div v-if="filteredReviews.length === 0" class="state-card">
            暂无审核内容
          </div>
        </div>

        <aside class="preview-panel">
          <template v-if="selectedReview">
            <h3>内容预览</h3>

            <p class="preview-title">
              {{ selectedReview.title }}
            </p>

            <div class="preview-meta">
              <span>{{ selectedReview.type }}</span>
              <span>{{ selectedReview.source }}</span>
              <span>{{ getRiskText(selectedReview.riskLevel) }}</span>
            </div>

            <div class="preview-content">
              {{ selectedReview.content }}
            </div>

            <div class="review-record" v-if="selectedReview.status !== 'pending'">
              <p>审核人：{{ selectedReview.reviewer || '暂无' }}</p>
              <p>审核时间：{{ selectedReview.reviewTime || '暂无' }}</p>
              <p v-if="selectedReview.reason">拒绝原因：{{ selectedReview.reason }}</p>
            </div>

            <label>
              拒绝原因
              <textarea
                v-model="rejectReason"
                placeholder="审核拒绝时填写原因..."
              />
            </label>

            <div class="preview-actions">
              <button
                class="primary-btn"
                :disabled="selectedReview.status !== 'pending'"
                @click="handleApprove"
              >
                通过
              </button>

              <button
                class="danger-btn"
                :disabled="selectedReview.status !== 'pending'"
                @click="handleReject"
              >
                拒绝
              </button>
            </div>
          </template>

          <div v-else class="state-card">
            请选择一条内容查看详情
          </div>
        </aside>
      </div>
    </section>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, ref, watch } from 'vue'
import {
  approveReview,
  getAdminReviewList,
  rejectReview
} from '@/api/admin'

import type {
  ReviewItem,
  ReviewStatus,
  RiskLevel
} from '@/api/admin'

const loading = ref(false)
const keyword = ref('')
const status = ref('')
const type = ref('')
const riskLevel = ref('')
const rejectReason = ref('')

const reviews = ref<ReviewItem[]>([])
const selectedReview = ref<ReviewItem | null>(null)

const statusTabs = [
  { label: '全部', value: '' },
  { label: '待审核', value: 'pending' },
  { label: '已通过', value: 'approved' },
  { label: '已拒绝', value: 'rejected' }
]

const fallbackReviews: ReviewItem[] = [
  {
    id: 1,
    title: 'A* 算法可视化动画',
    type: '视频',
    source: '人工智能导论 / 搜索算法',
    riskLevel: 'low',
    status: 'pending',
    submitter: '王老师',
    submitTime: '2026-04-29 10:20',
    content:
      '该内容通过动画展示 A* 算法搜索路径的过程，包含启发式函数、代价函数和路径回溯说明。'
  },
  {
    id: 2,
    title: '机器学习入门练习题',
    type: '题库',
    source: '机器学习 / 基础概念',
    riskLevel: 'middle',
    status: 'pending',
    submitter: '陈老师',
    submitTime: '2026-04-29 09:40',
    content:
      '该题库包含监督学习、无监督学习、模型评估等基础题目，需要检查答案解释是否准确。'
  },
  {
    id: 3,
    title: '智能体生成：搜索算法总结',
    type: '智能体生成内容',
    source: '智能辅导问答助手',
    riskLevel: 'high',
    status: 'rejected',
    submitter: '系统生成',
    submitTime: '2026-04-28 16:10',
    reviewer: '管理员',
    reviewTime: '2026-04-28 17:00',
    reason: '存在概念表述不准确',
    content:
      '该内容由智能体自动生成，部分算法复杂度说明需要修正。'
  },
  {
    id: 4,
    title: 'Python 爬虫实操案例',
    type: '课程资料',
    source: 'Python 程序设计 / 网络数据获取',
    riskLevel: 'low',
    status: 'approved',
    submitter: '李老师',
    submitTime: '2026-04-27 14:30',
    reviewer: '管理员',
    reviewTime: '2026-04-27 15:00',
    content:
      '该资源用于教学演示，包含网页请求、数据解析和结果保存等基础代码示例。'
  }
]

const filteredReviews = computed(() => {
  return reviews.value.filter((item: ReviewItem) => {
    const matchKeyword =
      !keyword.value ||
      item.title.includes(keyword.value) ||
      item.source.includes(keyword.value) ||
      item.submitter.includes(keyword.value)

    const matchStatus = !status.value || item.status === status.value
    const matchType = !type.value || item.type === type.value
    const matchRisk = !riskLevel.value || item.riskLevel === riskLevel.value

    return matchKeyword && matchStatus && matchType && matchRisk
  })
})

const stats = computed(() => {
  return {
    pending: reviews.value.filter((item: ReviewItem) => item.status === 'pending').length,
    approved: reviews.value.filter((item: ReviewItem) => item.status === 'approved').length,
    rejected: reviews.value.filter((item: ReviewItem) => item.status === 'rejected').length,
    highRisk: reviews.value.filter((item: ReviewItem) => item.riskLevel === 'high').length
  }
})

const fetchReviews = async () => {
  loading.value = true

  try {
    const result = await getAdminReviewList({
      keyword: keyword.value,
      status: status.value,
      type: type.value,
      riskLevel: riskLevel.value
    })

    reviews.value = result.list
    selectedReview.value = result.list[0] || null
  } catch (error) {
    console.warn('内容审核接口暂不可用，使用页面静态数据：', error)

    reviews.value = fallbackReviews
    selectedReview.value = fallbackReviews[0]
  } finally {
    loading.value = false
  }
}

const selectReview = (item: ReviewItem) => {
  selectedReview.value = item
  rejectReason.value = item.reason || ''
}

const handleApprove = async () => {
  if (!selectedReview.value) return

  selectedReview.value.status = 'approved'
  selectedReview.value.reviewer = '管理员'
  selectedReview.value.reviewTime = '刚刚'
  selectedReview.value.reason = ''

  try {
    await approveReview(selectedReview.value.id)
  } catch (error) {
    console.warn('审核通过接口暂不可用，仅更新页面状态：', error)
  }
}

const handleReject = async () => {
  if (!selectedReview.value) return

  if (!rejectReason.value.trim()) {
    alert('请填写拒绝原因')
    return
  }

  selectedReview.value.status = 'rejected'
  selectedReview.value.reason = rejectReason.value
  selectedReview.value.reviewer = '管理员'
  selectedReview.value.reviewTime = '刚刚'

  try {
    await rejectReview(selectedReview.value.id, rejectReason.value)
  } catch (error) {
    console.warn('审核拒绝接口暂不可用，仅更新页面状态：', error)
  }
}

const getReviewStatusText = (value: ReviewStatus) => {
  const map: Record<ReviewStatus, string> = {
    pending: '待审核',
    approved: '已通过',
    rejected: '已拒绝'
  }

  return map[value]
}

const getRiskText = (value: RiskLevel) => {
  const map: Record<RiskLevel, string> = {
    low: '低风险',
    middle: '中风险',
    high: '高风险'
  }

  return map[value]
}

watch(status, () => {
  fetchReviews()
})

onMounted(() => {
  fetchReviews()
})
</script>

<style scoped>
.review-page {
  min-height: 100vh;
  padding: clamp(14px, 2vw, 28px);
  background: #f5f8ff;
  color: #1f2a44;
  overflow-x: hidden;
}

.page-header,
.stat-card,
.panel,
.preview-panel {
  background: #ffffff;
  box-shadow: 0 10px 26px rgba(32, 88, 180, 0.06);
}

.page-header {
  padding: 28px;
  margin-bottom: 20px;
  border-radius: 24px;
  background: linear-gradient(135deg, #ffffff 0%, #eaf2ff 100%);
}

.eyebrow {
  margin: 0 0 8px;
  color: #1769ff;
  font-weight: 700;
}

.page-header h1 {
  margin: 0;
}

.page-header p {
  color: #667085;
  line-height: 1.7;
}

.stat-grid {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 16px;
  margin-bottom: 20px;
}

.stat-card {
  padding: 20px;
  border-radius: 20px;
}

.stat-card span {
  color: #667085;
}

.stat-card strong {
  display: block;
  margin-top: 10px;
  color: #1769ff;
  font-size: 30px;
}

.panel {
  padding: 20px;
  border-radius: 22px;
}

.tabs,
.toolbar {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
  margin-bottom: 18px;
}

.tabs button,
.toolbar button,
.primary-btn,
.danger-btn {
  height: 40px;
  padding: 0 18px;
  border: none;
  border-radius: 12px;
  cursor: pointer;
}

.tabs button {
  color: #52637a;
  background: #f1f5fb;
}

.tabs button.active,
.toolbar button,
.primary-btn {
  color: #ffffff;
  background: #1769ff;
}

.danger-btn {
  color: #ffffff;
  background: #ef4444;
}

button:disabled {
  opacity: 0.5;
  cursor: not-allowed;
}

.toolbar input,
.toolbar select,
.preview-panel textarea {
  border: 1px solid #dbe4f3;
  border-radius: 12px;
  outline: none;
}

.toolbar input,
.toolbar select {
  height: 40px;
  padding: 0 12px;
}

.toolbar input {
  min-width: 260px;
  flex: 1;
}

.review-layout {
  display: grid;
  grid-template-columns: minmax(0, 1fr) 340px;
  gap: 18px;
}

.table-wrap {
  overflow-x: auto;
}

table {
  width: 100%;
  min-width: 900px;
  border-collapse: collapse;
}

th,
td {
  padding: 14px 12px;
  border-bottom: 1px solid #eef2f8;
  text-align: left;
  font-size: 14px;
}

th {
  color: #667085;
  background: #f7faff;
}

tr.active {
  background: #f7faff;
}

.risk-tag,
.status-tag {
  padding: 5px 10px;
  border-radius: 999px;
  font-size: 12px;
}

.risk-tag.low,
.status-tag.approved {
  color: #15803d;
  background: #ecfdf3;
}

.risk-tag.middle,
.status-tag.pending {
  color: #b45309;
  background: #fff7ed;
}

.risk-tag.high,
.status-tag.rejected {
  color: #b91c1c;
  background: #fef2f2;
}

.text-btn {
  border: none;
  color: #1769ff;
  background: transparent;
  cursor: pointer;
}

.preview-panel {
  min-width: 0;
  padding: 18px;
  border-radius: 20px;
}

.preview-panel h3 {
  margin: 0 0 14px;
}

.preview-title {
  font-weight: 700;
}

.preview-meta {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}

.preview-meta span {
  padding: 5px 10px;
  border-radius: 999px;
  color: #1769ff;
  background: #eef5ff;
  font-size: 12px;
}

.preview-content,
.review-record {
  margin: 16px 0;
  padding: 14px;
  border-radius: 14px;
  color: #52637a;
  background: #f7faff;
  line-height: 1.7;
}

.review-record p {
  margin: 4px 0;
}

.preview-panel label {
  display: grid;
  gap: 8px;
  color: #52637a;
}

.preview-panel textarea {
  min-height: 90px;
  padding: 12px;
  resize: vertical;
}

.preview-actions {
  display: flex;
  gap: 12px;
  margin-top: 14px;
}

.state-card {
  padding: 40px;
  text-align: center;
  color: #75849a;
}

@media (max-width: 1120px) {
  .review-layout {
    grid-template-columns: 1fr;
  }
}

@media (max-width: 760px) {
  .stat-grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }

  .toolbar input {
    min-width: 100%;
  }
}

@media (max-width: 520px) {
  .review-page {
    padding: 12px;
  }

  .stat-grid {
    grid-template-columns: 1fr;
  }

  .preview-actions {
    flex-direction: column;
  }

  .primary-btn,
  .danger-btn {
    width: 100%;
  }
}
</style>