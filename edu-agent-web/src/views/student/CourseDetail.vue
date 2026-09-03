<template>
  <div class="course-detail-page">
    <div v-if="loading" class="state-card">
      课程加载中...
    </div>

    <div v-else-if="errorMessage" class="state-card">
      <p>{{ errorMessage }}</p>
      <button type="button" @click="fetchCourse(currentCourseId)">
        重新加载
      </button>
    </div>

    <template v-else>
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
        <template v-if="currentChapter">
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

        <!-- 学习资源统一从正式资源服务读取。 -->
        <div class="side-card resource-side-card">
          <div class="resource-side-header">
            <h3>学习资源</h3>
            <span class="chapter-tag">{{ currentChapter?.title }}</span>
          </div>
          <p class="suggestion">
            资源列表、详情和收藏均从正式资源服务获取。
          </p>
          <button class="view-btn" @click="router.push('/student/resources')">
            打开资源中心
          </button>
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
    </template>
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
const currentCourseId = computed(() => String(route.params.id || ''))

const loading = ref(false)
const errorMessage = ref('')
const note = ref('')
const currentChapterId = ref(1)

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
  errorMessage.value = ''
  Object.assign(course, createEmptyCourse())

  try {
    const result = await getCourseDetail(id)
    setCourse(result)
  } catch (error) {
    console.error('加载课程详情失败：', error)
    errorMessage.value = '课程详情加载失败，请稍后重试'
  } finally {
    loading.value = false
  }
}

const selectChapter = (chapter: CourseChapter) => {
  currentChapterId.value = chapter.id
}

const finishChapter = async () => {
  if (!currentChapter.value) return

  try {
    await updateChapterProgress(course.id, currentChapter.value.id, 100)
    currentChapter.value.progress = 100
    currentChapter.value.status = 'done'

    const doneCount = course.chapters.filter(item => item.status === 'done').length
    course.progress = course.chapters.length
      ? Math.round((doneCount / course.chapters.length) * 100)
      : 0
  } catch (error) {
    console.error('更新章节进度失败：', error)
    alert('章节进度保存失败，请稍后重试')
  }
}

const saveNote = async () => {
  if (!currentChapter.value) return

  try {
    await saveCourseNote(course.id, currentChapter.value.id, note.value)
    alert('笔记已保存')
  } catch (error) {
    console.error('保存笔记失败：', error)
    alert('笔记保存失败，请稍后重试')
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
  currentCourseId,
  id => {
    if (id) {
      fetchCourse(id)
    } else {
      errorMessage.value = '课程编号无效'
    }
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

/* ===== 学习资源（右侧面板）===== */

/* AI 生成中加载状态 */
.res-mini-card.is-loading {
  background: linear-gradient(135deg, #eef5ff 0%, #f7faff 100%);
  border-color: #1769ff;
}

.res-loading {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 10px;
  padding: 24px 0;
}

.res-loading-icon {
  font-size: 24px;
  animation: pulse 1.2s ease-in-out infinite;
}

.res-loading-text {
  font-size: 14px;
  font-weight: 600;
  color: #1769ff;
}

@keyframes pulse {
  0%, 100% { transform: scale(1); opacity: 1; }
  50% { transform: scale(1.15); opacity: 0.7; }
}
.resource-side-card {
  padding: 16px;
}

.resource-side-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 12px;
  gap: 8px;
}

.resource-side-header h3 {
  margin: 0;
  font-size: 15px;
}

.chapter-tag {
  padding: 3px 8px;
  border-radius: 999px;
  background: #eef5ff;
  color: #1769ff;
  font-size: 11px;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
  max-width: 120px;
}

.res-mini-card {
  padding: 12px;
  margin-bottom: 10px;
  border-radius: 14px;
  background: #f7faff;
  border: 1px solid transparent;
  transition: border-color 0.2s;
}

.res-mini-card:last-child {
  margin-bottom: 0;
}

.res-mini-card:hover {
  border-color: #d0e1ff;
}

.res-mini-top {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 6px;
}

.res-icon {
  font-size: 18px;
  line-height: 1;
}

.res-mini-info {
  display: flex;
  align-items: center;
  gap: 6px;
  flex-wrap: wrap;
}

.res-mini-info strong {
  font-size: 13px;
}

.diff-tag {
  padding: 2px 7px;
  border-radius: 999px;
  font-size: 11px;
  font-weight: 600;
}

.diff-tag.easy {
  color: #16a34a;
  background: #ecfdf3;
}

.diff-tag.medium {
  color: #1769ff;
  background: #eef5ff;
}

.diff-tag.hard {
  color: #d97706;
  background: #fff7ed;
}

.res-mini-summary {
  margin: 6px 0 0 26px;
  color: #75849a;
  font-size: 12px;
  line-height: 1.5;
display: -webkit-box;
line-clamp: 2;
-webkit-line-clamp: 2;
-webkit-box-orient: vertical;
  overflow: hidden;
}

.res-mini-actions {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-top: 10px;
  padding-top: 8px;
  border-top: 1px solid #e8eef7;
}

.view-btn {
  border: none;
  border-radius: 8px;
  padding: 4px 12px;
  background: #1769ff;
  color: #fff;
  font-size: 12px;
  cursor: pointer;
}

.diff-btns {
  display: flex;
  gap: 4px;
}

.diff-btn {
  border: 1px solid #dbe4f3;
  border-radius: 8px;
  padding: 3px 10px;
  background: #fff;
  color: #667085;
  font-size: 11px;
  cursor: pointer;
  transition: all 0.15s;
}

.diff-btn:hover {
  border-color: #1769ff;
  color: #1769ff;
}

.diff-btn.active {
  background: #1769ff;
  border-color: #1769ff;
  color: #fff;
}

.diff-btn:disabled {
  opacity: 0.5;
  cursor: not-allowed;
}

.resource-side-footer {
  margin-top: 12px;
  padding-top: 10px;
  border-top: 1px solid #eef2f8;
  text-align: center;
}

.resource-side-footer span {
  color: #a0afc0;
  font-size: 11px;
  line-height: 1.5;
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