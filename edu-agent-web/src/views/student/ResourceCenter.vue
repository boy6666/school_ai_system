<template>
  <div class="resource-center-page">
    <!-- 顶部区域 -->
    <section class="page-hero">
      <div>
        <p class="eyebrow">EduAgent 资源中心</p>
        <h1>资源中心</h1>
        <p class="subtitle">
          汇聚课程、视频、文档、题库、项目与工具资源，帮助你按目标快速找到合适的学习内容。
        </p>
      </div>

      <div class="hero-card">
        <span>今日推荐</span>
        <strong>计算机组成原理</strong>
        <p>根据你的学习画像推荐</p>
      </div>
    </section>

    <!-- 搜索与筛选 -->
    <section class="filter-panel">
      <div class="search-box">
        <input
          v-model="keyword"
          type="text"
          placeholder="搜索课程、资源、知识点..."
        />
        <button @click="handleSearch">搜索</button>
      </div>

      <div class="tabs">
        <button
          v-for="item in resourceTypes"
          :key="item.value"
          :class="{ active: activeType === item.value }"
          @click="activeType = item.value"
        >
          {{ item.label }}
        </button>
      </div>

      <div class="filter-row">
      <select v-model="courseId">
          <option value="">全部课程</option>
          <option value="ai">人工智能导论</option>
          <option value="python">Python 程序设计</option>
          <option value="data-structure">数据结构</option>
          <option value="ml">机器学习</option>
          <option value="network">计算机网络</option>
        </select>
        <select v-model="difficulty">
          <option value="">全部难度</option>
          <option value="入门">入门</option>
          <option value="基础">基础</option>
          <option value="进阶">进阶</option>
          <option value="高级">高级</option>
        </select>

        <select v-model="sortType">
          <option value="hot">按热度排序</option>
          <option value="new">按最新排序</option>
          <option value="score">按评分排序</option>
        </select>
      </div>
    </section>

    <main class="content-layout">
      <!-- 资源列表 -->
      <section class="resource-list">
        <div v-if="loading" class="empty-state">
        资源加载中...
        </div>

        <div v-if="errorMessage && !loading" class="empty-state">
          {{ errorMessage }}
        </div>
        <div
          v-for="resource in filteredResources"
          v-show="!loading"
          :key="resource.id"
          class="resource-card"
          @click="goDetail(resource.id)"
        >
          <div class="cover">
            <img :src="resource.cover" :alt="resource.title" />
            <span class="type-badge">{{ resource.type }}</span>
          </div>

          <div class="card-body">
            <div class="card-title-row">
              <h3>{{ resource.title }}</h3>
              <button class="favorite" @click.stop="toggleFavorite(resource)">
                {{ resource.favorite ? '已收藏' : '收藏' }}
              </button>
            </div>

            <p class="description">{{ resource.description }}</p>
            <div class="resource-relation">
              <span>{{ resource.courseName }}</span>
              <span>{{ resource.chapterName }}</span>
            </div>
            <div class="meta-row">
              <span>{{ resource.difficulty }}</span>
              <span>⭐ {{ resource.rating }}</span>
              <span>{{ resource.views }} 人学习</span>
              <span>{{ resource.updateTime }}</span>
            </div>
          </div>
        </div>

        <div v-if="!loading && filteredResources.length === 0" class="empty-state">
          暂无匹配资源，请调整搜索条件。
        </div>
      </section>

      <!-- 右侧推荐 -->
      <aside class="side-panel">
        <div class="panel-card">
          <div class="panel-header">
            <h3>为你推荐</h3>
            <a href="javascript:void(0)">换一换</a>
          </div>

          <div
            v-for="item in recommendedResources"
            :key="item.id"
            class="recommend-item"
            @click="goDetail(item.id)"
          >
            <img :src="item.cover" :alt="item.title" />
            <div>
              <strong>{{ item.title }}</strong>
              <p>⭐ {{ item.rating }} · {{ item.views }} 人学习</p>
            </div>
          </div>
        </div>

        <div class="panel-card">
          <div class="panel-header">
            <h3>热门标签</h3>
          </div>

          <div class="tag-list">
            <span v-for="tag in hotTags" :key="tag">{{ tag }}</span>
          </div>
        </div>
      </aside>
    </main>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, ref, watch } from 'vue'
import { useRouter } from 'vue-router'
import {
  getResourceList,
  updateResourceFavorite
} from '@/api/resource'

import type {
  ResourceListItem,
  ResourceSortType
} from '@/api/resource'

const router = useRouter()

const keyword = ref('')
const activeType = ref('all')
const difficulty = ref('')
const courseId = ref('')
const sortType = ref<ResourceSortType>('hot')
const loading = ref(false)
const errorMessage = ref('')
const total = ref(0)

const resourceTypes = [
  { label: '全部', value: 'all' },
  { label: '文档', value: '文档' },
  { label: 'PPT', value: 'PPT' },
  { label: '视频', value: '视频' },
  { label: '动画', value: '动画' },
  { label: '题库', value: '题库' },
  { label: '代码案例', value: '代码案例' },
  { label: '实验项目', value: '实验项目' },
  { label: '拓展阅读', value: '拓展阅读' },
  { label: '思维导图', value: '思维导图' }
]

const fallbackResources: ResourceListItem[] = [
  {
    id: 1,
    title: '搜索算法知识点讲解',
    type: '文档',
    difficulty: '基础',
    description: '系统讲解状态空间搜索、BFS、DFS、启发式搜索和 A* 算法的核心概念。',
    rating: 4.8,
    views: 1800,
    updateTime: '2024-05-14',
    cover: 'https://images.unsplash.com/photo-1456513080510-7bf3a84b82f8?w=600',
    favorite: true,
    courseId: 'ai',
    courseName: '人工智能导论',
    chapterName: '第 2 章：搜索算法',
    tags: ['搜索算法', 'BFS', 'DFS', 'A* 算法'],
    fileSize: '6.2MB'
  },
  {
    id: 2,
    title: 'A* 算法可视化动画',
    type: '动画',
    difficulty: '进阶',
    description: '通过动画演示 A* 算法的搜索过程，帮助学生理解启发式搜索和路径规划。',
    rating: 4.9,
    views: 2300,
    updateTime: '2024-05-16',
    cover: 'https://images.unsplash.com/photo-1518770660439-4636190af475?w=600',
    favorite: false,
    courseId: 'ai',
    courseName: '人工智能导论',
    chapterName: '第 2 章：搜索算法',
    tags: ['A* 算法', '搜索策略', '路径规划'],
    fileSize: '18.6MB'
  },
  {
    id: 3,
    title: 'BFS / DFS 思维导图',
    type: '思维导图',
    difficulty: '基础',
    description: '用思维导图整理 BFS、DFS 的搜索过程、适用场景、优缺点和复杂度对比。',
    rating: 4.7,
    views: 1560,
    updateTime: '2024-05-13',
    cover: 'https://images.unsplash.com/photo-1553877522-43269d4ea984?w=600',
    favorite: false,
    courseId: 'ai',
    courseName: '人工智能导论',
    chapterName: '第 2 章：搜索算法',
    tags: ['BFS', 'DFS', '思维导图'],
    fileSize: '3.8MB'
  },
  {
    id: 4,
    title: '搜索算法练习题',
    type: '题库',
    difficulty: '基础',
    description: '围绕状态空间搜索、BFS、DFS、A* 算法设计的章节练习题，适合课后巩固。',
    rating: 4.9,
    views: 2100,
    updateTime: '2024-05-12',
    cover: 'https://images.unsplash.com/photo-1434030216411-0b793f4b4173?w=600',
    favorite: false,
    courseId: 'ai',
    courseName: '人工智能导论',
    chapterName: '第 2 章：搜索算法',
    tags: ['章节练习', '搜索算法', 'A* 算法'],
    fileSize: '2.4MB'
  },
  {
    id: 5,
    title: 'Python 爬虫实操案例',
    type: '代码案例',
    difficulty: '进阶',
    description: '通过案例学习 requests、BeautifulSoup、数据清洗和结果保存的完整流程。',
    rating: 4.8,
    views: 1680,
    updateTime: '2024-05-10',
    cover: 'https://images.unsplash.com/photo-1551288049-bebda4e38f71?w=600',
    favorite: false,
    courseId: 'python',
    courseName: 'Python 程序设计',
    chapterName: '第 6 章：网络爬虫基础',
    tags: ['Python', '爬虫', '实战案例'],
    fileSize: '12.5MB'
  },
  {
    id: 6,
    title: '机器学习入门练习题',
    type: '题库',
    difficulty: '基础',
    description: '覆盖监督学习、无监督学习、模型评估、过拟合等机器学习基础知识点。',
    rating: 4.6,
    views: 1320,
    updateTime: '2024-05-09',
    cover: 'https://images.unsplash.com/photo-1620712943543-bcc4688e7485?w=600',
    favorite: false,
    courseId: 'ml',
    courseName: '机器学习',
    chapterName: '第 1 章：机器学习基础',
    tags: ['机器学习', '模型评估', '练习题'],
    fileSize: '4.1MB'
  }
]

const resources = ref<ResourceListItem[]>([])
const recommendedResources = ref<ResourceListItem[]>([])
const hotTags = ref<string[]>([])

const filteredResources = computed(() => resources.value)

const fetchResources = async () => {
  loading.value = true
  errorMessage.value = ''

const query = {
  keyword: keyword.value,
  type: activeType.value,
  difficulty: difficulty.value,
  courseId: courseId.value,
  sort: sortType.value,
  page: 1,
  pageSize: 12
}

  try {
    const result = await getResourceList(query)

    resources.value = result.list
    recommendedResources.value = result.recommended
    hotTags.value = result.hotTags
    total.value = result.total
  } catch (error) {
    console.warn('资源列表接口暂不可用，使用页面静态数据：', error)

    let list = [...fallbackResources]

    if (keyword.value) {
      list = list.filter(item => {
        return (
          item.title.includes(keyword.value) ||
          item.description.includes(keyword.value) ||
          item.courseName.includes(keyword.value) ||
          item.chapterName.includes(keyword.value) ||
          item.tags.some(tag => tag.includes(keyword.value))
        )
      })
    }

    if (activeType.value !== 'all') {
      list = list.filter(item => item.type === activeType.value)
    }

    if (difficulty.value) {
      list = list.filter(item => item.difficulty === difficulty.value)
    }

    if (courseId.value) {
      list = list.filter(item => item.courseId === courseId.value)
    }
    if (sortType.value === 'score') {
      list.sort((a, b) => b.rating - a.rating)
    }

    if (sortType.value === 'hot') {
      list.sort((a, b) => b.views - a.views)
    }

    if (sortType.value === 'new') {
      list.sort(
        (a, b) => new Date(b.updateTime).getTime() - new Date(a.updateTime).getTime()
      )
    }

    resources.value = list
    recommendedResources.value = fallbackResources.slice(0, 4)
    hotTags.value = ['Python', '机器学习', '操作系统', '计算机网络', '数据库', '算法', '深度学习', '前端开发']
    total.value = list.length
    errorMessage.value = '接口暂不可用，当前展示页面静态数据。'
  } finally {
    loading.value = false
  }
}

const handleSearch = () => {
  fetchResources()
}

const toggleFavorite = async (resource: ResourceListItem) => {
  const oldValue = resource.favorite
  resource.favorite = !resource.favorite

  try {
    await updateResourceFavorite(resource.id, resource.favorite)
  } catch (error) {
    console.warn('收藏接口暂不可用，仅更新页面状态：', error)
    resource.favorite = !oldValue
  }
}

const goDetail = (id: number) => {
  router.push(`/student/resources/${id}`)
}

watch([activeType, difficulty, courseId, sortType], () => {
  fetchResources()
})

onMounted(() => {
  fetchResources()
})
</script>

<style scoped>
.resource-center-page {
  min-height: 100vh;
  padding: 28px;
  background: #f5f8ff;
  color: #1f2a44;
}

.page-hero {
  display: flex;
  justify-content: space-between;
  gap: 24px;
  padding: 32px;
  margin-bottom: 20px;
  border-radius: 24px;
  background: linear-gradient(135deg, #ffffff 0%, #eaf2ff 100%);
  box-shadow: 0 12px 30px rgba(32, 88, 180, 0.08);
}

.eyebrow {
  margin: 0 0 8px;
  color: #1a73e8;
  font-weight: 700;
}

.page-hero h1 {
  margin: 0;
  font-size: 32px;
}

.subtitle {
  max-width: 640px;
  color: #667085;
  line-height: 1.7;
}

.hero-card {
  min-width: 220px;
  padding: 20px;
  border-radius: 20px;
  background: #ffffff;
  box-shadow: 0 10px 24px rgba(47, 94, 180, 0.1);
}

.hero-card span {
  color: #667085;
  font-size: 14px;
}

.hero-card strong {
  display: block;
  margin: 10px 0;
  color: #0f4fd8;
  font-size: 20px;
}

.hero-card p {
  margin: 0;
  color: #667085;
}

.filter-panel {
  padding: 20px;
  margin-bottom: 20px;
  border-radius: 20px;
  background: #ffffff;
  box-shadow: 0 10px 26px rgba(32, 88, 180, 0.06);
}

.search-box {
  display: flex;
  gap: 12px;
  margin-bottom: 18px;
}

.search-box input {
  flex: 1;
  height: 42px;
  padding: 0 16px;
  border: 1px solid #dbe4f3;
  border-radius: 12px;
  outline: none;
}

.search-box button {
  width: 96px;
  border: none;
  border-radius: 12px;
  color: #ffffff;
  background: #1769ff;
  cursor: pointer;
}

.tabs {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
  margin-bottom: 16px;
}

.tabs button {
  padding: 9px 16px;
  border: 1px solid #dbe4f3;
  border-radius: 999px;
  background: #ffffff;
  color: #52637a;
  cursor: pointer;
}

.tabs button.active {
  color: #ffffff;
  border-color: #1769ff;
  background: #1769ff;
}

.filter-row {
  display: flex;
  gap: 12px;
}

.filter-row select {
  height: 38px;
  padding: 0 12px;
  border: 1px solid #dbe4f3;
  border-radius: 10px;
  color: #52637a;
  background: #ffffff;
}

.content-layout {
  display: grid;
  grid-template-columns: 1fr 300px;
  gap: 20px;
}

.resource-list {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 18px;
}

.resource-card {
  overflow: hidden;
  border-radius: 20px;
  background: #ffffff;
  box-shadow: 0 10px 26px rgba(32, 88, 180, 0.06);
  cursor: pointer;
  transition: all 0.2s ease;
}

.resource-card:hover {
  transform: translateY(-4px);
  box-shadow: 0 16px 34px rgba(32, 88, 180, 0.12);
}

.cover {
  position: relative;
  height: 160px;
  overflow: hidden;
}

.cover img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.type-badge {
  position: absolute;
  top: 12px;
  left: 12px;
  padding: 5px 10px;
  border-radius: 999px;
  color: #ffffff;
  font-size: 12px;
  background: #1769ff;
}

.card-body {
  padding: 18px;
}

.card-title-row {
  display: flex;
  justify-content: space-between;
  gap: 12px;
}

.card-title-row h3 {
  margin: 0;
  font-size: 18px;
}

.favorite {
  flex-shrink: 0;
  height: 30px;
  padding: 0 12px;
  border: 1px solid #dbe4f3;
  border-radius: 999px;
  color: #1769ff;
  background: #f7faff;
  cursor: pointer;
}

.description {
  min-height: 48px;
  color: #667085;
  line-height: 1.6;
}

.meta-row {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
  color: #75849a;
  font-size: 13px;
}

.side-panel {
  display: flex;
  flex-direction: column;
  gap: 18px;
}

.panel-card {
  padding: 18px;
  border-radius: 20px;
  background: #ffffff;
  box-shadow: 0 10px 26px rgba(32, 88, 180, 0.06);
}

.panel-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 14px;
}

.panel-header h3 {
  margin: 0;
}

.panel-header a {
  color: #1769ff;
  font-size: 14px;
  text-decoration: none;
}

.recommend-item {
  display: flex;
  gap: 12px;
  padding: 12px 0;
  border-bottom: 1px solid #eef2f8;
  cursor: pointer;
}

.recommend-item:last-child {
  border-bottom: none;
}

.recommend-item img {
  width: 56px;
  height: 56px;
  border-radius: 12px;
  object-fit: cover;
}

.recommend-item strong {
  display: block;
  margin-bottom: 6px;
  font-size: 14px;
}

.recommend-item p {
  margin: 0;
  color: #75849a;
  font-size: 12px;
}

.tag-list {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
}

.tag-list span {
  padding: 7px 12px;
  border-radius: 999px;
  color: #1769ff;
  background: #eef5ff;
  font-size: 13px;
}

.empty-state {
  grid-column: 1 / -1;
  padding: 60px;
  text-align: center;
  border-radius: 20px;
  color: #75849a;
  background: #ffffff;
}

/* 大屏到中屏：资源卡片自动适配 */
.resource-center-page {
  width: 100%;
  max-width: 100%;
  padding: clamp(14px, 2vw, 28px);
  overflow-x: hidden;
}

.page-hero,
.filter-panel,
.content-layout,
.resource-list,
.side-panel {
  min-width: 0;
}

.resource-list {
  grid-template-columns: repeat(auto-fit, minmax(280px, 1fr));
}

.cover {
  height: auto;
  aspect-ratio: 16 / 9;
}

.search-box input {
  min-width: 0;
}

/* 1200 以下：右侧推荐栏下移 */
@media (max-width: 1200px) {
  .content-layout {
    grid-template-columns: 1fr;
  }

  .side-panel {
    display: grid;
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
}

/* 900 以下：首页头部纵向排列 */
@media (max-width: 900px) {
  .page-hero {
    flex-direction: column;
    padding: 24px;
  }

  .hero-card {
    width: 100%;
    min-width: 0;
  }

  .filter-row {
    flex-wrap: wrap;
  }

  .filter-row select {
    flex: 1;
    min-width: 150px;
  }

  .side-panel {
    grid-template-columns: 1fr;
  }
}

/* 640 以下：搜索栏和卡片适配手机 */
@media (max-width: 640px) {
  .resource-center-page {
    padding: 12px;
  }

  .page-hero {
    padding: 20px;
    border-radius: 18px;
  }

  .page-hero h1 {
    font-size: 26px;
  }

  .subtitle {
    font-size: 14px;
  }

  .filter-panel {
    padding: 14px;
    border-radius: 16px;
  }

  .search-box {
    flex-direction: column;
  }

  .search-box button {
    width: 100%;
    height: 40px;
  }

  .tabs {
    flex-wrap: nowrap;
    overflow-x: auto;
    padding-bottom: 4px;
  }

  .tabs button {
    flex-shrink: 0;
  }

  .resource-list {
    grid-template-columns: 1fr;
  }

  .card-title-row {
    flex-direction: column;
  }

  .favorite {
    width: fit-content;
  }

  .meta-row {
    gap: 8px;
    font-size: 12px;
  }

  .panel-card {
    padding: 14px;
  }
}

/* 420 以下：进一步压缩间距 */
@media (max-width: 420px) {
  .page-hero h1 {
    font-size: 24px;
  }

  .card-body {
    padding: 14px;
  }

  .card-title-row h3 {
    font-size: 16px;
  }

  .description {
    font-size: 14px;
  }
}

.resource-relation {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  margin: 10px 0;
}

.resource-relation span {
  padding: 5px 9px;
  border-radius: 999px;
  color: #1769ff;
  background: #eef5ff;
  font-size: 12px;
}
</style>