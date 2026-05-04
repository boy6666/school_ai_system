<template>
  <div class="course-detail-page">
    <section class="course-hero">
      <div>
        <p class="eyebrow">课堂 / 学习空间</p>
        <h1>{{ course.title }}</h1>
        <p class="subtitle">{{ course.description }}</p>

        <div class="course-meta">
          <span>授课教师：{{ course.teacher }}</span>
          <span>章节数：{{ course.totalChapters }}</span>
          <span>已学：{{ course.learnedHours }} / {{ course.totalHours }} 小时</span>
        </div>

        <div class="tag-list">
          <span v-for="tag in course.tags" :key="tag">
            {{ tag }}
          </span>
        </div>
      </div>

      <div class="progress-card">
        <strong>{{ course.progress }}%</strong>
        <span>课程进度</span>
        <div class="progress-bar">
          <div :style="{ width: course.progress + '%' }"></div>
        </div>
      </div>
    </section>

    <main class="learning-layout">
      <aside class="chapter-panel">
        <h3>章节目录</h3>

        <div
          v-for="chapter in course.chapters"
          :key="chapter.id"
          class="chapter-item"
          :class="{ active: currentChapter?.id === chapter.id }"
          @click="selectChapter(chapter)"
        >
          <div>
            <strong>{{ chapter.title }}</strong>
            <p>{{ chapter.duration }} · {{ getChapterStatusText(chapter.status) }}</p>
          </div>
          <span>{{ chapter.progress }}%</span>
        </div>
      </aside>

      <section class="learning-main">
        <div v-if="loading" class="state-card">
          课程加载中...
        </div>

        <template v-else-if="currentChapter">
          <section class="content-card">
            <div class="section-title">
              <div>
                <p class="eyebrow">当前章节</p>
                <h2>{{ currentChapter.title }}</h2>
              </div>

              <button @click="finishChapter">
                标记完成
              </button>
            </div>

            <p class="chapter-desc">
              {{ currentChapter.description }}
            </p>

            <div class="video-box">
              <div class="play-icon">▶</div>
              <p>学习内容区：这里可以接入视频、动画、文档预览或 Markdown 内容</p>
            </div>
          </section>

          <section class="content-card">
            <h3>知识点</h3>
            <div class="point-list">
              <span v-for="point in currentChapter.knowledgePoints" :key="point">
                {{ point }}
              </span>
            </div>
          </section>

          <section class="content-card">
            <h3>章节资源</h3>

            <div
              v-for="resource in currentChapter.resources"
              :key="resource.id"
              class="resource-row"
              @click="goResource(resource.id)"
            >
              <div>
                <strong>{{ resource.title }}</strong>
                <p>{{ resource.type }} · {{ resource.difficulty }} · {{ resource.duration }}</p>
              </div>

              <button>查看资源</button>
            </div>

            <div v-if="currentChapter.resources.length === 0" class="mini-empty">
              暂无章节资源
            </div>
          </section>

          <section class="content-card">
            <h3>学习笔记</h3>
            <textarea
              v-model="note"
              placeholder="记录本章重点、疑问或课堂总结..."
            />
            <button class="save-btn" @click="saveNote">
              保存笔记
            </button>
          </section>
        </template>

        <div v-else class="state-card">
          暂无章节内容
        </div>
      </section>

      <aside class="side-panel">
        <div class="side-card">
          <h3>课程任务</h3>

          <div
            v-for="task in course.tasks"
            :key="task.id"
            class="task-item"
          >
            <strong>{{ task.title }}</strong>
            <p>{{ task.type }} · {{ task.deadline }}</p>
            <span :class="['task-status', task.status]">
              {{ getTaskStatusText(task.status) }}
            </span>
          </div>

          <div v-if="course.tasks.length === 0" class="mini-empty">
            暂无课程任务
          </div>
        </div>

        <div class="side-card">
          <h3>学习建议</h3>
          <p class="suggestion">
            课程学习空间负责“怎么学”：按章节、知识点、任务和进度推进学习。
            如果只想查找某个资料，可以从章节资源跳转到资源详情页。
          </p>
        </div>
      </aside>
    </main>
  </div>
</template>

<script setup lang="ts">
import { computed, reactive, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import {
  getCourseDetail,
  saveCourseNote,
  updateChapterProgress
} from '@/api/course'

import type {
  CourseChapter,
  CourseDetail
} from '@/api/course'

const route = useRoute()
const router = useRouter()

const loading = ref(false)
const note = ref('')
const currentChapterId = ref(1)

const fallbackCourseMap: Record<string, CourseDetail> = {
  ai: {
    id: 'ai',
    title: '人工智能导论',
    teacher: '王老师',
    description:
      '系统学习人工智能基本概念、搜索算法、机器学习、神经网络和大模型应用。',
    cover: '',
    progress: 45,
    learnedHours: 8,
    totalHours: 18,
    totalChapters: 5,
    currentChapterId: 2,
    tags: ['人工智能', '搜索算法', '机器学习'],
    chapters: [
      {
        id: 1,
        title: '第 1 章：人工智能概述',
        description: '了解人工智能的发展历史、核心概念、主要分支和典型应用场景。',
        duration: '45 分钟',
        progress: 100,
        status: 'done',
        knowledgePoints: ['人工智能定义', '发展阶段', '应用场景'],
        resources: [
          {
            id: 101,
            title: '人工智能概述讲解文档',
            type: '文档',
            difficulty: '入门',
            duration: '15 分钟'
          },
          {
            id: 102,
            title: '人工智能发展时间线思维导图',
            type: '思维导图',
            difficulty: '入门',
            duration: '8 分钟'
          }
        ]
      },
      {
        id: 2,
        title: '第 2 章：搜索算法',
        description:
          '学习 BFS、DFS、启发式搜索和 A* 算法，理解问题求解中的状态空间搜索思想。',
        duration: '70 分钟',
        progress: 45,
        status: 'learning',
        knowledgePoints: ['BFS', 'DFS', '启发式搜索', 'A* 算法'],
        resources: [
          {
            id: 1,
            title: '搜索算法知识点讲解',
            type: '文档',
            difficulty: '基础',
            duration: '20 分钟'
          },
          {
            id: 2,
            title: 'A* 算法可视化动画',
            type: '动画',
            difficulty: '进阶',
            duration: '12 分钟'
          },
          {
            id: 4,
            title: '搜索算法练习题',
            type: '题库',
            difficulty: '基础',
            duration: '25 分钟'
          }
        ]
      },
      {
        id: 3,
        title: '第 3 章：机器学习基础',
        description:
          '学习监督学习、无监督学习、训练集、测试集、模型评估和过拟合等基础概念。',
        duration: '80 分钟',
        progress: 0,
        status: 'not-started',
        knowledgePoints: ['监督学习', '无监督学习', '模型评估', '过拟合'],
        resources: [
          {
            id: 6,
            title: '机器学习入门练习题',
            type: '题库',
            difficulty: '基础',
            duration: '30 分钟'
          }
        ]
      }
    ],
    tasks: [
      {
        id: 1,
        title: '完成搜索算法章节学习',
        type: '章节学习',
        deadline: '今天 22:00',
        status: 'doing'
      },
      {
        id: 2,
        title: '提交搜索算法练习题',
        type: '课后练习',
        deadline: '明天 18:00',
        status: 'todo'
      },
      {
        id: 3,
        title: '人工智能应用案例分析',
        type: '项目任务',
        deadline: '本周五',
        status: 'todo'
      }
    ]
  },
  python: {
    id: 'python',
    title: 'Python 程序设计',
    teacher: '李老师',
    description:
      '从基础语法、函数、文件操作到网络爬虫和数据处理的完整 Python 学习课程。',
    cover: '',
    progress: 70,
    learnedHours: 16,
    totalHours: 24,
    totalChapters: 8,
    currentChapterId: 2,
    tags: ['Python', '编程基础', '爬虫'],
    chapters: [
      {
        id: 1,
        title: '第 1 章：Python 基础语法',
        description: '学习变量、数据类型、条件判断、循环和基础输入输出。',
        duration: '60 分钟',
        progress: 100,
        status: 'done',
        knowledgePoints: ['变量', '数据类型', '条件判断', '循环'],
        resources: [
          {
            id: 201,
            title: 'Python 基础语法讲解文档',
            type: '文档',
            difficulty: '入门',
            duration: '20 分钟'
          }
        ]
      },
      {
        id: 2,
        title: '第 6 章：网络爬虫基础',
        description:
          '学习 requests、BeautifulSoup、网页解析、数据清洗和结果保存的完整流程。',
        duration: '90 分钟',
        progress: 70,
        status: 'learning',
        knowledgePoints: ['requests', 'BeautifulSoup', '网页解析', '数据清洗'],
        resources: [
          {
            id: 5,
            title: 'Python 爬虫实操案例',
            type: '代码案例',
            difficulty: '进阶',
            duration: '45 分钟'
          }
        ]
      }
    ],
    tasks: [
      {
        id: 1,
        title: '整理 Python 爬虫案例笔记',
        type: '章节学习',
        deadline: '今天 20:00',
        status: 'doing'
      }
    ]
  }
}

const createEmptyCourse = (): CourseDetail => ({
  id: '',
  title: '',
  teacher: '',
  description: '',
  cover: '',
  progress: 0,
  learnedHours: 0,
  totalHours: 0,
  totalChapters: 0,
  currentChapterId: 1,
  tags: [],
  chapters: [],
  tasks: []
})

const course = reactive<CourseDetail>(createEmptyCourse())

const currentChapter = computed(() => {
  return course.chapters.find(item => item.id === currentChapterId.value)
})

const setCourse = (data: CourseDetail) => {
  Object.assign(course, data)
  currentChapterId.value = data.currentChapterId || data.chapters[0]?.id || 1
}

const fetchCourse = async (id: string) => {
  loading.value = true

  try {
    const result = await getCourseDetail(id)
    setCourse(result)
  } catch (error) {
    console.warn('课程详情接口暂不可用，使用页面静态数据：', error)
    setCourse(fallbackCourseMap[id] || fallbackCourseMap.ai)
  } finally {
    loading.value = false
  }
}

const selectChapter = (chapter: CourseChapter) => {
  currentChapterId.value = chapter.id
}

const finishChapter = async () => {
  if (!currentChapter.value) return

  currentChapter.value.progress = 100
  currentChapter.value.status = 'done'

  const doneCount = course.chapters.filter(item => item.status === 'done').length
  course.progress = Math.round((doneCount / course.chapters.length) * 100)

  try {
    await updateChapterProgress(course.id, currentChapter.value.id, 100)
  } catch (error) {
    console.warn('更新章节进度接口暂不可用，仅更新页面状态：', error)
  }
}

const saveNote = async () => {
  if (!currentChapter.value) return

  try {
    await saveCourseNote(course.id, currentChapter.value.id, note.value)
    alert('笔记已保存')
  } catch (error) {
    console.warn('保存笔记接口暂不可用，仅更新页面状态：', error)
    alert('笔记已保存')
  }
}

const goResource = (id: number) => {
  router.push(`/student/resources/${id}`)
}

const getChapterStatusText = (status: CourseChapter['status']) => {
  const map: Record<CourseChapter['status'], string> = {
    'not-started': '未开始',
    learning: '学习中',
    done: '已完成'
  }

  return map[status]
}

const getTaskStatusText = (status: string) => {
  const map: Record<string, string> = {
    todo: '待完成',
    doing: '进行中',
    done: '已完成'
  }

  return map[status]
}

watch(
  () => route.params.id,
  id => {
    fetchCourse(String(id || 'ai'))
  },
  {
    immediate: true
  }
)
</script>

<style scoped>
.course-detail-page {
  min-height: 100vh;
  padding: clamp(14px, 2vw, 28px);
  background: #f5f8ff;
  color: #1f2a44;
  overflow-x: hidden;
}

.course-hero {
  display: grid;
  grid-template-columns: minmax(0, 1fr) 260px;
  gap: 20px;
  padding: 28px;
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

.course-hero h1 {
  margin: 0;
  font-size: 32px;
}

.subtitle {
  color: #667085;
  line-height: 1.7;
}

.course-meta,
.tag-list,
.point-list {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
}

.course-meta {
  margin-bottom: 12px;
}

.course-meta span,
.tag-list span,
.point-list span {
  padding: 7px 12px;
  border-radius: 999px;
  color: #1769ff;
  background: #eef5ff;
  font-size: 13px;
}

.progress-card {
  padding: 20px;
  border-radius: 20px;
  background: #ffffff;
}

.progress-card strong {
  display: block;
  font-size: 36px;
  color: #1769ff;
}

.progress-card span {
  color: #667085;
}

.progress-bar {
  height: 8px;
  margin-top: 18px;
  overflow: hidden;
  border-radius: 999px;
  background: #e8eef7;
}

.progress-bar div {
  height: 100%;
  border-radius: 999px;
  background: #1769ff;
}

.learning-layout {
  display: grid;
  grid-template-columns: 280px minmax(0, 1fr) 300px;
  gap: 20px;
}

.chapter-panel,
.content-card,
.side-card,
.state-card {
  border-radius: 20px;
  background: #ffffff;
  box-shadow: 0 10px 26px rgba(32, 88, 180, 0.06);
}

.chapter-panel {
  height: fit-content;
  padding: 18px;
}

.chapter-panel h3,
.content-card h3,
.side-card h3 {
  margin: 0 0 16px;
}

.chapter-item {
  display: flex;
  justify-content: space-between;
  gap: 12px;
  padding: 14px;
  margin-bottom: 10px;
  border-radius: 14px;
  cursor: pointer;
  background: #f7faff;
}

.chapter-item.active {
  color: #1769ff;
  background: #eef5ff;
  box-shadow: inset 3px 0 0 #1769ff;
}

.chapter-item strong {
  font-size: 14px;
}

.chapter-item p {
  margin: 6px 0 0;
  color: #75849a;
  font-size: 12px;
}

.learning-main {
  min-width: 0;
  display: flex;
  flex-direction: column;
  gap: 18px;
}

.content-card,
.state-card {
  padding: 20px;
}

.section-title {
  display: flex;
  justify-content: space-between;
  gap: 16px;
}

.section-title h2 {
  margin: 0;
}

.section-title button,
.resource-row button,
.save-btn {
  border: none;
  border-radius: 12px;
  color: #ffffff;
  background: #1769ff;
  cursor: pointer;
}

.section-title button {
  height: 38px;
  padding: 0 16px;
}

.chapter-desc {
  color: #667085;
  line-height: 1.7;
}

.video-box {
  height: 280px;
  border-radius: 18px;
  background: linear-gradient(135deg, #1f2a44, #1769ff);
  color: #ffffff;
  display: flex;
  flex-direction: column;
  justify-content: center;
  align-items: center;
  gap: 10px;
  text-align: center;
}

.play-icon {
  width: 72px;
  height: 72px;
  border-radius: 50%;
  background: rgba(255, 255, 255, 0.22);
  display: flex;
  justify-content: center;
  align-items: center;
  font-size: 34px;
}

.resource-row {
  display: flex;
  justify-content: space-between;
  gap: 14px;
  padding: 14px 0;
  border-bottom: 1px solid #eef2f8;
  cursor: pointer;
}

.resource-row:last-child {
  border-bottom: none;
}

.resource-row p {
  margin: 6px 0 0;
  color: #75849a;
  font-size: 13px;
}

.resource-row button {
  flex-shrink: 0;
  height: 34px;
  padding: 0 12px;
}

textarea {
  width: 100%;
  min-height: 120px;
  padding: 14px;
  border: 1px solid #dbe4f3;
  border-radius: 14px;
  outline: none;
  resize: vertical;
}

.save-btn {
  height: 38px;
  padding: 0 18px;
  margin-top: 12px;
}

.side-panel {
  min-width: 0;
  display: flex;
  flex-direction: column;
  gap: 18px;
}

.side-card {
  padding: 18px;
}

.task-item {
  padding: 14px;
  margin-bottom: 10px;
  border-radius: 14px;
  background: #f7faff;
}

.task-item p,
.suggestion,
.mini-empty {
  margin: 6px 0 0;
  color: #75849a;
  font-size: 13px;
  line-height: 1.7;
}

.task-status {
  display: inline-block;
  margin-top: 10px;
  padding: 4px 9px;
  border-radius: 999px;
  font-size: 12px;
}

.task-status.todo {
  color: #b45309;
  background: #fff7ed;
}

.task-status.doing {
  color: #1769ff;
  background: #eef5ff;
}

.task-status.done {
  color: #15803d;
  background: #ecfdf3;
}

@media (max-width: 1180px) {
  .learning-layout {
    grid-template-columns: 240px minmax(0, 1fr);
  }

  .side-panel {
    grid-column: 1 / -1;
    display: grid;
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
}

@media (max-width: 860px) {
  .course-hero,
  .learning-layout {
    grid-template-columns: 1fr;
  }

  .side-panel {
    grid-template-columns: 1fr;
  }

  .section-title,
  .resource-row {
    flex-direction: column;
  }

  .resource-row button,
  .section-title button {
    width: fit-content;
  }
}

@media (max-width: 520px) {
  .course-detail-page {
    padding: 12px;
  }

  .course-hero {
    padding: 20px;
    border-radius: 18px;
  }

  .course-hero h1 {
    font-size: 24px;
  }

  .video-box {
    height: 220px;
  }
}
</style>