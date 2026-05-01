<template>
  <div class="resource-detail-page">
    <button class="back-btn" @click="router.back()">返回资源中心</button>
      <div v-if="loading" class="loading-card">
      资源详情加载中...
      </div>

      <div v-if="errorMessage && !loading" class="mock-tip">
      {{ errorMessage }}
      </div>
    <!-- 详情头部 -->
    <section class="detail-hero">
      <div class="cover-box">
        <img :src="resource.cover" :alt="resource.title" />
        <div class="play-mask">▶</div>
      </div>

      <div class="info-box">
        <div class="tag-row">
          <span>{{ resource.type }}</span>
          <span>{{ resource.difficulty }}</span>
        </div>

        <h1>{{ resource.title }}</h1>

        <p class="summary">{{ resource.description }}</p>

        <div class="stats-row">
          <div>
            <strong>{{ resource.rating }}</strong>
            <span>综合评分</span>
          </div>
          <div>
            <strong>{{ resource.views }}</strong>
            <span>学习人数</span>
          </div>
          <div>
            <strong>{{ resource.chapterCount }}</strong>
            <span>章节数量</span>
          </div>
          <div>
            <strong>{{ resource.duration }}</strong>
            <span>预计时长</span>
          </div>
        </div>

        <div class="action-row">
          <button class="primary-btn" @click="startLearning">开始学习</button>
          <button class="outline-btn" @click="addToPlan">加入学习计划</button>
          <button class="outline-btn" @click="toggleFavorite">
            {{ resource.favorite ? '已收藏' : '收藏资源' }}
          </button>
        </div>
      </div>
    </section>

    <main class="detail-layout">
      <!-- 主内容 -->
      <section class="main-content">
        <div class="tab-bar">
          <button
            v-for="tab in tabs"
            :key="tab.value"
            :class="{ active: activeTab === tab.value }"
            @click="activeTab = tab.value"
          >
            {{ tab.label }}
          </button>
        </div>

        <!-- 简介 -->
        <section v-if="activeTab === 'intro'" class="content-card">
          <h2>资源简介</h2>
          <p>
            本资源围绕 {{ resource.title }} 展开，适合希望系统学习相关知识的学生。
            内容从基础概念入手，逐步过渡到实践应用，并结合案例帮助理解重点知识。
          </p>

          <h2>你将学到</h2>
          <ul class="check-list">
            <li v-for="goal in resource.goals" :key="goal">{{ goal }}</li>
          </ul>

          <h2>适合人群</h2>
          <ul class="check-list">
            <li v-for="person in resource.suitableFor" :key="person">{{ person }}</li>
          </ul>
        </section>

        <!-- 目录 -->
        <section v-if="activeTab === 'catalog'" class="content-card">
          <h2>课程目录</h2>

          <div
            v-for="chapter in chapters"
            :key="chapter.id"
            class="chapter-item"
          >
            <div>
              <strong>{{ chapter.title }}</strong>
              <p>{{ chapter.desc }}</p>
            </div>

            <span>{{ chapter.duration }}</span>
          </div>
        </section>

        <!-- 评价 -->
        <section v-if="activeTab === 'reviews'" class="content-card">
          <h2>学习评价</h2>

          <div
            v-for="review in reviews"
            :key="review.id"
            class="review-item"
          >
            <div class="avatar">{{ review.name.slice(0, 1) }}</div>
            <div>
              <div class="review-title">
                <strong>{{ review.name }}</strong>
                <span>⭐ {{ review.score }}</span>
              </div>
              <p>{{ review.content }}</p>
            </div>
          </div>
        </section>
      </section>

      <!-- 右侧信息 -->
      <aside class="side-content">
        <div class="side-card">
          <h3>学习进度</h3>
          <div class="progress-circle">
            <strong>{{ progress }}%</strong>
            <span>当前进度</span>
          </div>
          <div class="progress-bar">
            <div :style="{ width: progress + '%' }"></div>
          </div>
        </div>

        <div class="side-card">
          <h3>资源信息</h3>

          <div class="info-line">
            <span>资源类型</span>
            <strong>{{ resource.type }}</strong>
          </div>

          <div class="info-line">
            <span>难度等级</span>
            <strong>{{ resource.difficulty }}</strong>
          </div>

          <div class="info-line">
            <span>更新时间</span>
            <strong>{{ resource.updateTime }}</strong>
          </div>

          <div class="info-line">
            <span>授课教师</span>
            <strong>{{ resource.teacher }}</strong>
          </div>
        </div>

        <div class="side-card">
          <h3>相关推荐</h3>

          <div
            v-for="item in relatedResources"
            :key="item.id"
            class="related-item"
            @click="goOtherResource(item.id)"
          >
            <img :src="item.cover" :alt="item.title" />
            <div>
              <strong>{{ item.title }}</strong>
              <p>⭐ {{ item.rating }}</p>
            </div>
          </div>
        </div>
      </aside>
    </main>
  </div>
</template>

<script setup lang="ts">
import { reactive, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import {
  addResourceToPlan,
  getRelatedResources,
  getResourceDetail,
  updateResourceFavorite
} from '@/api/resource'

import type {
  ResourceChapter,
  ResourceDetailItem,
  ResourceListItem,
  ResourceReview
} from '@/api/resource'

const route = useRoute()
const router = useRouter()

const activeTab = ref('intro')
const loading = ref(false)
const errorMessage = ref('')
const progress = ref(0)

const tabs = [
  { label: '简介', value: 'intro' },
  { label: '目录', value: 'catalog' },
  { label: '评价', value: 'reviews' }
]

const createEmptyResource = (): ResourceDetailItem => ({
  id: 0,
  title: '',
  type: '课程',
  difficulty: '基础',
  description: '',
  rating: 0,
  views: 0,
  updateTime: '',
  cover: '',
  favorite: false,
  chapterCount: 0,
  duration: '',
  teacher: '',
  progress: 0,
  goals: [],
  suitableFor: [],
  chapters: [],
  reviews: []
})

const fallbackResource: ResourceDetailItem = {
  id: 1,
  title: '计算机组成原理：CPU 指令系统详解',
  type: '课程',
  difficulty: '基础',
  description:
    '系统学习 CPU 工作原理、指令系统、存储结构与输入输出机制，帮助学生建立完整的计算机底层知识框架。',
  rating: 4.9,
  views: 2300,
  chapterCount: 8,
  duration: '3小时45分钟',
  updateTime: '2024-05-16',
  teacher: '李老师',
  cover: 'https://images.unsplash.com/photo-1518770660439-4636190af475?w=900',
  favorite: false,
  progress: 32,
  goals: [
    '理解 CPU 的基本结构和运行流程',
    '掌握指令系统的组成与执行过程',
    '理解存储系统与总线通信机制',
    '能够分析简单指令的执行过程'
  ],
  suitableFor: [
    '计算机相关专业本科生',
    '正在学习计算机组成原理的学生',
    '希望补充底层基础知识的学习者'
  ],
  chapters: [
    {
      id: 1,
      title: '第 1 章：计算机系统概述',
      desc: '认识计算机硬件组成、软件系统和基本工作过程。',
      duration: '25 分钟'
    },
    {
      id: 2,
      title: '第 2 章：CPU 基本结构',
      desc: '学习运算器、控制器、寄存器组和数据通路。',
      duration: '38 分钟'
    },
    {
      id: 3,
      title: '第 3 章：指令系统',
      desc: '理解指令格式、寻址方式和指令分类。',
      duration: '45 分钟'
    },
    {
      id: 4,
      title: '第 4 章：指令执行过程',
      desc: '分析取指、译码、执行和写回过程。',
      duration: '42 分钟'
    },
    {
      id: 5,
      title: '第 5 章：存储系统',
      desc: '学习主存、高速缓存和存储层次结构。',
      duration: '50 分钟'
    }
  ],
  reviews: [
    {
      id: 1,
      name: '张同学',
      score: 5,
      content: '讲解很清楚，配合图示之后更容易理解 CPU 指令执行过程。'
    },
    {
      id: 2,
      name: '李同学',
      score: 4.8,
      content: '适合复习计算机组成原理，章节安排比较合理。'
    },
    {
      id: 3,
      name: '王同学',
      score: 4.9,
      content: '内容比较系统，适合作为课程学习的补充资料。'
    }
  ]
}

const fallbackRelatedResources: ResourceListItem[] = [
  {
    id: 2,
    title: 'Python 数据分析实战案例',
    type: '项目',
    difficulty: '进阶',
    description: '通过真实数据案例学习 Pandas、可视化分析和数据报告输出。',
    rating: 4.8,
    views: 1800,
    updateTime: '2024-05-14',
    cover: 'https://images.unsplash.com/photo-1551288049-bebda4e38f71?w=300',
    favorite: true
  },
  {
    id: 3,
    title: '数据库 SQL 经典题库',
    type: '题库',
    difficulty: '基础',
    description: '覆盖查询、连接、聚合、子查询、事务等数据库核心知识点。',
    rating: 4.7,
    views: 1560,
    updateTime: '2024-05-13',
    cover: 'https://images.unsplash.com/photo-1544383835-bda2bc66a55d?w=300',
    favorite: false
  },
  {
    id: 4,
    title: '神经网络反向传播可视化讲解',
    type: '视频',
    difficulty: '高级',
    description: '结合图解动画理解神经网络训练过程和梯度反向传播机制。',
    rating: 4.9,
    views: 2100,
    updateTime: '2024-05-12',
    cover: 'https://images.unsplash.com/photo-1677442136019-21780ecad995?w=300',
    favorite: false
  }
]

const resource = reactive<ResourceDetailItem>(createEmptyResource())
const chapters = ref<ResourceChapter[]>([])
const reviews = ref<ResourceReview[]>([])
const relatedResources = ref<ResourceListItem[]>([])

const setResourceDetail = (
  detail: ResourceDetailItem,
  related: ResourceListItem[]
) => {
  Object.assign(resource, detail)
  chapters.value = detail.chapters
  reviews.value = detail.reviews
  progress.value = detail.progress
  relatedResources.value = related
}

const fetchResourceDetail = async (id: number) => {
  loading.value = true
  errorMessage.value = ''

  try {
    const detail = await getResourceDetail(id)
    const related = await getRelatedResources(id)

    setResourceDetail(detail, related)
  } catch (error) {
    console.warn('资源详情接口暂不可用，使用页面静态数据：', error)

    setResourceDetail(
      {
        ...fallbackResource,
        id
      },
      fallbackRelatedResources
    )

    errorMessage.value = '接口暂不可用，当前展示页面静态数据。'
  } finally {
    loading.value = false
  }
}

const startLearning = () => {
  router.push(`/student/courses/${resource.id}`)
}

const addToPlan = async () => {
  try {
    await addResourceToPlan(resource.id)
    alert('已加入学习计划')
  } catch (error) {
    console.warn('加入学习计划接口暂不可用：', error)
    alert('已加入学习计划')
  }
}

const toggleFavorite = async () => {
  const oldValue = resource.favorite
  resource.favorite = !resource.favorite

  try {
    await updateResourceFavorite(resource.id, resource.favorite)
  } catch (error) {
    console.warn('收藏接口暂不可用，仅更新页面状态：', error)
    resource.favorite = !oldValue
  }
}

const goOtherResource = (id: number) => {
  router.push(`/student/resources/${id}`)
}

watch(
  () => route.params.id,
  id => {
    fetchResourceDetail(Number(id || 1))
  },
  {
    immediate: true
  }
)
</script>

<style scoped>
.loading-card,
.mock-tip {
  padding: 14px 18px;
  margin-bottom: 16px;
  border-radius: 14px;
  color: #1769ff;
  background: #eef5ff;
}

.resource-detail-page {
  min-height: 100vh;
  padding: 28px;
  background: #f5f8ff;
  color: #1f2a44;
}

.back-btn {
  margin-bottom: 18px;
  padding: 10px 16px;
  border: 1px solid #dbe4f3;
  border-radius: 12px;
  color: #1769ff;
  background: #ffffff;
  cursor: pointer;
}

.detail-hero {
  display: grid;
  grid-template-columns: 420px 1fr;
  gap: 24px;
  padding: 24px;
  margin-bottom: 22px;
  border-radius: 24px;
  background: #ffffff;
  box-shadow: 0 12px 30px rgba(32, 88, 180, 0.08);
}

.cover-box {
  position: relative;
  height: 280px;
  overflow: hidden;
  border-radius: 20px;
}

.cover-box img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.play-mask {
  position: absolute;
  inset: 0;
  display: flex;
  justify-content: center;
  align-items: center;
  color: #ffffff;
  font-size: 48px;
  background: rgba(0, 0, 0, 0.25);
}

.info-box h1 {
  margin: 14px 0;
  font-size: 30px;
}

.tag-row {
  display: flex;
  gap: 10px;
}

.tag-row span {
  padding: 6px 12px;
  border-radius: 999px;
  color: #1769ff;
  background: #eef5ff;
  font-size: 13px;
}

.summary {
  color: #667085;
  line-height: 1.7;
}

.stats-row {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 14px;
  margin: 22px 0;
}

.stats-row div {
  padding: 16px;
  border-radius: 16px;
  background: #f7faff;
}

.stats-row strong {
  display: block;
  margin-bottom: 6px;
  color: #1769ff;
  font-size: 22px;
}

.stats-row span {
  color: #75849a;
  font-size: 13px;
}

.action-row {
  display: flex;
  flex-wrap: wrap;
  gap: 12px;
}

.primary-btn,
.outline-btn {
  height: 42px;
  padding: 0 22px;
  border-radius: 12px;
  cursor: pointer;
}

.primary-btn {
  border: none;
  color: #ffffff;
  background: #1769ff;
}

.outline-btn {
  border: 1px solid #dbe4f3;
  color: #1769ff;
  background: #ffffff;
}

.detail-layout {
  display: grid;
  grid-template-columns: 1fr 320px;
  gap: 22px;
}

.main-content,
.side-card {
  border-radius: 22px;
  background: #ffffff;
  box-shadow: 0 10px 26px rgba(32, 88, 180, 0.06);
}

.tab-bar {
  display: flex;
  gap: 8px;
  padding: 18px 20px 0;
}

.tab-bar button {
  padding: 10px 18px;
  border: none;
  border-radius: 999px;
  color: #52637a;
  background: #f1f5fb;
  cursor: pointer;
}

.tab-bar button.active {
  color: #ffffff;
  background: #1769ff;
}

.content-card {
  padding: 24px;
}

.content-card h2 {
  margin: 8px 0 14px;
  font-size: 20px;
}

.content-card p {
  color: #667085;
  line-height: 1.8;
}

.check-list {
  padding: 0;
  margin: 0 0 24px;
  list-style: none;
}

.check-list li {
  position: relative;
  padding-left: 28px;
  margin-bottom: 12px;
  color: #4c5f78;
}

.check-list li::before {
  position: absolute;
  left: 0;
  content: '✓';
  color: #18b66a;
  font-weight: 700;
}

.chapter-item {
  display: flex;
  justify-content: space-between;
  gap: 20px;
  padding: 18px 0;
  border-bottom: 1px solid #eef2f8;
}

.chapter-item:last-child {
  border-bottom: none;
}

.chapter-item p {
  margin: 8px 0 0;
  color: #75849a;
}

.chapter-item span {
  flex-shrink: 0;
  color: #1769ff;
}

.review-item {
  display: flex;
  gap: 14px;
  padding: 18px 0;
  border-bottom: 1px solid #eef2f8;
}

.review-item:last-child {
  border-bottom: none;
}

.avatar {
  width: 42px;
  height: 42px;
  border-radius: 50%;
  display: flex;
  justify-content: center;
  align-items: center;
  color: #ffffff;
  background: #1769ff;
}

.review-title {
  display: flex;
  gap: 12px;
  margin-bottom: 6px;
}

.review-title span {
  color: #f59e0b;
}

.side-content {
  display: flex;
  flex-direction: column;
  gap: 18px;
}

.side-card {
  padding: 20px;
}

.side-card h3 {
  margin: 0 0 16px;
}

.progress-circle {
  width: 132px;
  height: 132px;
  margin: 0 auto 18px;
  border: 12px solid #e6f0ff;
  border-top-color: #1769ff;
  border-radius: 50%;
  display: flex;
  flex-direction: column;
  justify-content: center;
  align-items: center;
  color: #1769ff;
}

.progress-circle strong {
  font-size: 28px;
}

.progress-circle span {
  color: #75849a;
  font-size: 13px;
}

.progress-bar {
  height: 8px;
  overflow: hidden;
  border-radius: 999px;
  background: #e9eef7;
}

.progress-bar div {
  height: 100%;
  border-radius: 999px;
  background: #1769ff;
}

.info-line {
  display: flex;
  justify-content: space-between;
  padding: 12px 0;
  border-bottom: 1px solid #eef2f8;
}

.info-line:last-child {
  border-bottom: none;
}

.info-line span {
  color: #75849a;
}

.related-item {
  display: flex;
  gap: 12px;
  padding: 12px 0;
  border-bottom: 1px solid #eef2f8;
  cursor: pointer;
}

.related-item:last-child {
  border-bottom: none;
}

.related-item img {
  width: 58px;
  height: 58px;
  border-radius: 12px;
  object-fit: cover;
}

.related-item strong {
  display: block;
  margin-bottom: 6px;
  font-size: 14px;
}

.related-item p {
  margin: 0;
  color: #75849a;
  font-size: 13px;
}

@media (max-width: 1080px) {
  .detail-hero,
  .detail-layout {
    grid-template-columns: 1fr;
  }

  .stats-row {
    grid-template-columns: repeat(2, 1fr);
  }
}

.resource-detail-page {
  width: 100%;
  max-width: 100%;
  padding: clamp(14px, 2vw, 28px);
  overflow-x: hidden;
}

.detail-hero,
.detail-layout,
.main-content,
.side-content,
.side-card {
  min-width: 0;
}

.detail-hero {
  grid-template-columns: minmax(280px, 420px) minmax(0, 1fr);
}

.cover-box {
  height: auto;
  aspect-ratio: 3 / 2;
}

.stats-row {
  grid-template-columns: repeat(auto-fit, minmax(120px, 1fr));
}

.action-row {
  align-items: center;
}

.primary-btn,
.outline-btn {
  white-space: nowrap;
}

/* 1180 以下：详情头部和右侧栏改成单列 */
@media (max-width: 1180px) {
  .detail-hero {
    grid-template-columns: 1fr;
  }

  .cover-box {
    max-height: 360px;
  }

  .detail-layout {
    grid-template-columns: 1fr;
  }

  .side-content {
    display: grid;
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }

  .side-card:last-child {
    grid-column: 1 / -1;
  }
}

/* 768 以下：内容整体压缩 */
@media (max-width: 768px) {
  .resource-detail-page {
    padding: 12px;
  }

  .detail-hero {
    padding: 16px;
    border-radius: 18px;
  }

  .info-box h1 {
    font-size: 24px;
  }

  .summary {
    font-size: 14px;
  }

  .stats-row {
    grid-template-columns: repeat(2, minmax(0, 1fr));
    gap: 10px;
  }

  .stats-row div {
    padding: 12px;
  }

  .stats-row strong {
    font-size: 20px;
  }

  .action-row {
    flex-direction: column;
    align-items: stretch;
  }

  .primary-btn,
  .outline-btn {
    width: 100%;
  }

  .tab-bar {
    overflow-x: auto;
    padding: 14px 14px 0;
  }

  .tab-bar button {
    flex-shrink: 0;
  }

  .content-card {
    padding: 16px;
  }

  .chapter-item {
    flex-direction: column;
    gap: 8px;
  }

  .side-content {
    grid-template-columns: 1fr;
  }

  .side-card:last-child {
    grid-column: auto;
  }
}

/* 480 以下：手机窄屏 */
@media (max-width: 480px) {
  .back-btn {
    width: 100%;
  }

  .cover-box {
    aspect-ratio: 16 / 10;
  }

  .play-mask {
    font-size: 36px;
  }

  .tag-row {
    flex-wrap: wrap;
  }

  .info-box h1 {
    font-size: 22px;
  }

  .stats-row {
    grid-template-columns: 1fr;
  }

  .review-item {
    align-items: flex-start;
  }

  .review-title {
    flex-direction: column;
    gap: 4px;
  }
}
</style>