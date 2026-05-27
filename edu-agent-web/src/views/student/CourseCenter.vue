<template>
  <div class="course-center-page">
    <section class="page-hero">
      <div>
        <p class="eyebrow">课程中心</p>
        <h1>我的课程</h1>
        <p class="subtitle">
          按课程组织学习内容，查看课程进度、章节目录、课程任务和推荐学习入口。
        </p>
      </div>

      <div class="hero-card">
        <span>当前推荐学习</span>
        <strong>人工智能导论</strong>
        <p>继续学习：第 2 章 搜索算法</p>
      </div>
    </section>

    <section class="filter-panel">
      <input
        v-model="keyword"
        type="text"
        placeholder="搜索课程名称、教师、标签..."
      />

      <select v-model="status">
        <option value="">全部状态</option>
        <option value="learning">学习中</option>
        <option value="done">已完成</option>
        <option value="not-started">未开始</option>
      </select>

      <button @click="fetchCourses">查询</button>
    </section>

    <main class="content-layout">
      <section class="course-list">
        <div v-if="loading" class="empty-state">
          课程加载中...
        </div>

        <div v-else-if="filteredCourses.length === 0" class="empty-state">
          暂无匹配课程
        </div>

        <div
          v-for="course in filteredCourses"
          v-else
          :key="course.id"
          class="course-card"
          @click="goCourse(course.id)"
        >
          <div class="cover">
            <img :src="course.cover" :alt="course.title" />
            <span :class="['status-badge', course.status]">
              {{ getStatusText(course.status) }}
            </span>
          </div>

          <div class="card-body">
            <div class="title-row">
              <h3>{{ course.title }}</h3>
              <span>{{ course.progress }}%</span>
            </div>

            <p class="description">{{ course.description }}</p>

            <div class="course-info">
              <span>授课教师：{{ course.teacher }}</span>
              <span>章节：{{ course.learnedChapters }} / {{ course.totalChapters }}</span>
              <span>时长：{{ course.learnedHours }} / {{ course.totalHours }} 小时</span>
            </div>

            <div class="progress-bar">
              <div :style="{ width: course.progress + '%' }"></div>
            </div>

            <div class="current-chapter">
              当前学习：{{ course.currentChapter }}
            </div>

            <div class="tag-list">
              <span v-for="tag in course.tags" :key="tag">
                {{ tag }}
              </span>
            </div>
          </div>
        </div>
      </section>

      <aside class="side-panel">
        <div class="side-card">
          <h3>学习入口</h3>

          <div
            v-for="course in learningCourses"
            :key="course.id"
            class="mini-course"
            @click="goCourse(course.id)"
          >
            <strong>{{ course.title }}</strong>
            <p>{{ course.currentChapter }}</p>
            <div class="progress-bar">
              <div :style="{ width: course.progress + '%' }"></div>
            </div>
          </div>
        </div>

        <div class="side-card">
          <h3>课程中心说明</h3>
          <p class="tips">
            课程中心负责“怎么学”：按课程、章节、知识点、任务和进度组织学习流程。
            资源中心负责“有什么资料”：按文档、PPT、视频、题库等类型查找材料。
          </p>
        </div>
      </aside>
    </main>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { getCourseList } from '@/api/course'
import type { CourseListItem, CourseStatus } from '@/api/course'

const router = useRouter()

const keyword = ref('')
const status = ref('')
const loading = ref(false)

const fallbackCourses: CourseListItem[] = [
  {
    id: 'ai',
    title: '人工智能导论',
    teacher: '王老师',
    description: '系统学习人工智能基本概念、搜索算法、机器学习、神经网络和大模型应用。',
    cover: 'https://images.unsplash.com/photo-1677442136019-21780ecad995?w=700',
    progress: 45,
    totalChapters: 5,
    learnedChapters: 2,
    totalHours: 18,
    learnedHours: 8,
    currentChapter: '第 2 章：搜索算法',
    status: 'learning',
    tags: ['人工智能', '搜索算法', '机器学习']
  },
  {
    id: 'python',
    title: 'Python 程序设计',
    teacher: '李老师',
    description: '从基础语法、函数、文件操作到网络爬虫和数据处理的完整 Python 学习课程。',
    cover: 'https://images.unsplash.com/photo-1526379095098-d400fd0bf935?w=700',
    progress: 70,
    totalChapters: 8,
    learnedChapters: 6,
    totalHours: 24,
    learnedHours: 16,
    currentChapter: '第 6 章：网络爬虫基础',
    status: 'learning',
    tags: ['Python', '编程基础', '爬虫']
  },
  {
    id: 'data-structure',
    title: '数据结构',
    teacher: '陈老师',
    description: '学习线性表、栈、队列、树、图、查找和排序等核心数据结构知识。',
    cover: 'https://images.unsplash.com/photo-1515879218367-8466d910aaa4?w=700',
    progress: 0,
    totalChapters: 7,
    learnedChapters: 0,
    totalHours: 20,
    learnedHours: 0,
    currentChapter: '第 1 章：线性表',
    status: 'not-started',
    tags: ['数据结构', '算法基础', '编程能力']
  },
  {
    id: 'ml',
    title: '机器学习',
    teacher: '赵老师',
    description: '学习监督学习、无监督学习、模型评估、特征工程和基础神经网络方法。',
    cover: 'https://images.unsplash.com/photo-1551288049-bebda4e38f71?w=700',
    progress: 20,
    totalChapters: 6,
    learnedChapters: 1,
    totalHours: 22,
    learnedHours: 4,
    currentChapter: '第 1 章：机器学习基础',
    status: 'learning',
    tags: ['机器学习', '模型评估', '数据分析']
  },
  {
    id: 'network',
    title: '计算机网络',
    teacher: '周老师',
    description: '学习网络体系结构、TCP/IP、HTTP、路由、交换和网络安全基础。',
    cover: 'https://images.unsplash.com/photo-1558494949-ef010cbdcc31?w=700',
    progress: 35,
    totalChapters: 6,
    learnedChapters: 2,
    totalHours: 18,
    learnedHours: 6,
    currentChapter: '第 2 章：传输层协议',
    status: 'learning',
    tags: ['计算机网络', 'TCP/IP', 'HTTP']
  }
]

const courses = ref<CourseListItem[]>([])

const filteredCourses = computed(() => {
  return courses.value.filter(course => {
    const matchKeyword =
      !keyword.value ||
      course.title.includes(keyword.value) ||
      course.teacher.includes(keyword.value) ||
      course.tags.some(tag => tag.includes(keyword.value))

    const matchStatus = !status.value || course.status === status.value

    return matchKeyword && matchStatus
  })
})

const learningCourses = computed(() => {
  return courses.value.filter(course => course.status === 'learning').slice(0, 3)
})

const fetchCourses = async () => {
  loading.value = true

  try {
    const result = await getCourseList({
      keyword: keyword.value,
      status: status.value
    })

    courses.value = result.list
  } catch (error) {
    console.warn('课程列表接口暂不可用，使用页面静态数据：', error)
    courses.value = fallbackCourses
  } finally {
    loading.value = false
  }
}

const goCourse = (id: string) => {
  router.push(`/student/courses/${id}`)
}

const getStatusText = (value: CourseStatus) => {
  const map: Record<CourseStatus, string> = {
    'not-started': '未开始',
    learning: '学习中',
    done: '已完成'
  }

  return map[value]
}

onMounted(() => {
  fetchCourses()
})
</script>

<style scoped>
.course-center-page {
  min-height: 100vh;
  padding: clamp(14px, 2vw, 28px);
  background: #f5f8ff;
  color: #1f2a44;
  overflow-x: hidden;
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
  color: #1769ff;
  font-weight: 700;
}

.page-hero h1 {
  margin: 0;
  font-size: 32px;
}

.subtitle {
  max-width: 680px;
  color: #667085;
  line-height: 1.7;
}

.hero-card {
  min-width: 240px;
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
  color: #1769ff;
  font-size: 20px;
}

.hero-card p {
  margin: 0;
  color: #667085;
}

.filter-panel {
  display: flex;
  flex-wrap: wrap;
  gap: 12px;
  padding: 18px;
  margin-bottom: 20px;
  border-radius: 20px;
  background: #ffffff;
  box-shadow: 0 10px 26px rgba(32, 88, 180, 0.06);
}

.filter-panel input,
.filter-panel select {
  height: 40px;
  padding: 0 12px;
  border: 1px solid #dbe4f3;
  border-radius: 12px;
  outline: none;
}

.filter-panel input {
  flex: 1;
  min-width: 260px;
}

.filter-panel button {
  height: 40px;
  padding: 0 18px;
  border: none;
  border-radius: 12px;
  color: #ffffff;
  background: #1769ff;
  cursor: pointer;
}

.content-layout {
  display: grid;
  grid-template-columns: minmax(0, 1fr) 320px;
  gap: 20px;
}

.course-list {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(320px, 1fr));
  gap: 18px;
  min-width: 0;
}

.course-card {
  overflow: hidden;
  border-radius: 20px;
  background: #ffffff;
  box-shadow: 0 10px 26px rgba(32, 88, 180, 0.06);
  cursor: pointer;
  transition: all 0.2s ease;
}

.course-card:hover {
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

.status-badge {
  position: absolute;
  top: 12px;
  left: 12px;
  padding: 5px 10px;
  border-radius: 999px;
  color: #ffffff;
  font-size: 12px;
}

.status-badge.learning {
  background: #1769ff;
}

.status-badge.done {
  background: #16a34a;
}

.status-badge.not-started {
  background: #64748b;
}

.card-body {
  padding: 18px;
}

.title-row {
  display: flex;
  justify-content: space-between;
  gap: 12px;
}

.title-row h3 {
  margin: 0;
  font-size: 18px;
}

.title-row span {
  color: #1769ff;
  font-weight: 700;
}

.description {
  min-height: 48px;
  color: #667085;
  line-height: 1.6;
}

.course-info {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  color: #75849a;
  font-size: 13px;
}

.progress-bar {
  height: 8px;
  margin: 14px 0;
  overflow: hidden;
  border-radius: 999px;
  background: #e8eef7;
}

.progress-bar div {
  height: 100%;
  border-radius: 999px;
  background: #1769ff;
}

.current-chapter {
  padding: 10px 12px;
  margin-bottom: 12px;
  border-radius: 12px;
  color: #1769ff;
  background: #eef5ff;
  font-size: 13px;
}

.tag-list {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}

.tag-list span {
  padding: 5px 9px;
  border-radius: 999px;
  color: #52637a;
  background: #f1f5fb;
  font-size: 12px;
}

.side-panel {
  display: flex;
  flex-direction: column;
  gap: 18px;
}

.side-card {
  padding: 18px;
  border-radius: 20px;
  background: #ffffff;
  box-shadow: 0 10px 26px rgba(32, 88, 180, 0.06);
}

.side-card h3 {
  margin: 0 0 16px;
}

.mini-course {
  padding: 12px;
  margin-bottom: 10px;
  border-radius: 14px;
  background: #f7faff;
  cursor: pointer;
}

.mini-course strong {
  font-size: 14px;
}

.mini-course p,
.tips {
  margin: 6px 0 0;
  color: #75849a;
  font-size: 13px;
  line-height: 1.7;
}

.empty-state {
  grid-column: 1 / -1;
  padding: 60px;
  text-align: center;
  border-radius: 20px;
  color: #75849a;
  background: #ffffff;
}

@media (max-width: 1180px) {
  .content-layout {
    grid-template-columns: 1fr;
  }

  .side-panel {
    display: grid;
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
}

@media (max-width: 760px) {
  .page-hero {
    flex-direction: column;
    padding: 22px;
  }

  .hero-card {
    min-width: 0;
  }

  .course-list {
    grid-template-columns: 1fr;
  }

  .side-panel {
    grid-template-columns: 1fr;
  }

  .filter-panel input,
  .filter-panel select,
  .filter-panel button {
    width: 100%;
  }
}

@media (max-width: 480px) {
  .course-center-page {
    padding: 12px;
  }

  .page-hero h1 {
    font-size: 24px;
  }
}
</style>