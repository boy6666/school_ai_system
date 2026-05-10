<template>
  <div class="projects-page">
    <el-card class="filter-card">
      <el-row :gutter="20">
        <el-col :span="6">
          <el-select v-model="filters.category" placeholder="项目分类" style="width: 100%">
            <el-option label="全部" value="" />
            <el-option label="前端项目" value="frontend" />
            <el-option label="后端项目" value="backend" />
            <el-option label="全栈项目" value="fullstack" />
          </el-select>
        </el-col>
        <el-col :span="6">
          <el-select v-model="filters.level" placeholder="项目级别" style="width: 100%">
            <el-option label="全部" value="" />
            <el-option label="入门" value="beginner" />
            <el-option label="进阶" value="intermediate" />
            <el-option label="高级" value="advanced" />
          </el-select>
        </el-col>
        <el-col :span="6">
          <el-input v-model="filters.keyword" placeholder="搜索项目..." />
        </el-col>
        <el-col :span="6">
          <el-button type="primary" @click="handleSearch">搜索</el-button>
          <el-button @click="handleReset">重置</el-button>
        </el-col>
      </el-row>
    </el-card>

    <div v-if="!isProjectDetail">
      <el-row :gutter="20">
        <el-col :span="8" v-for="project in projects" :key="project.id">
          <el-card class="project-card" @click="viewProject(project)">
            <div class="project-header">
              <el-avatar :size="50" :src="project.avatar">
                {{ project.title.charAt(0) }}
              </el-avatar>
              <div class="project-info">
                <h3>{{ project.title }}</h3>
                <el-tag :type="getLevelType(project.level)" size="small">
                  {{ getLevelLabel(project.level) }}
                </el-tag>
              </div>
            </div>

            <div class="project-desc">{{ project.description }}</div>

            <div class="project-tags">
              <el-tag
                v-for="tag in project.tags"
                :key="tag"
                size="small"
                type="info"
              >
                {{ tag }}
              </el-tag>
            </div>

            <div class="project-stats">
              <div class="stat-item">
                <el-icon><User /></el-icon>
                <span>{{ project.enrolled }} 人参与</span>
              </div>
              <div class="stat-item">
                <el-icon><Star /></el-icon>
                <span>{{ project.rating }} 分</span>
              </div>
              <div class="stat-item">
                <el-icon><Clock /></el-icon>
                <span>{{ project.duration }}</span>
              </div>
            </div>

            <div class="project-progress">
              <span class="progress-label">项目进度</span>
              <el-progress :percentage="project.progress" />
            </div>

            <el-button
              type="primary"
              size="small"
              style="width: 100%; margin-top: 15px"
              @click.stop="continueProject(project)"
            >
              {{ project.progress > 0 ? '继续学习' : '开始项目' }}
            </el-button>
          </el-card>
        </el-col>
      </el-row>

      <el-pagination
        v-model:current-page="pagination.page"
        v-model:page-size="pagination.size"
        :total="pagination.total"
        layout="total, prev, pager, next"
        style="margin-top: 20px; text-align: center"
      />
    </div>

    <div v-else class="project-detail">
      <el-card class="detail-card">
        <template #header>
          <div class="detail-header">
            <el-button @click="goBack" :icon="ArrowLeft">返回</el-button>
            <h2>{{ currentProject?.title }}</h2>
            <div class="header-actions">
              <el-button @click="toggleFavorite">
                <el-icon><Star /></el-icon>
                {{ isFavorite ? '已收藏' : '收藏' }}
              </el-button>
            </div>
          </div>
        </template>

        <el-tabs v-model="activeTab" type="border-card">
          <el-tab-pane label="项目概览" name="overview">
            <div class="overview-content">
              <div class="project-meta">
                <el-tag :type="getLevelType(currentProject?.level)">
                  {{ getLevelLabel(currentProject?.level) }}
                </el-tag>
                <el-tag type="info">{{ getCategoryLabel(currentProject?.category) }}</el-tag>
                <span class="meta-item">
                  <el-icon><User /></el-icon> {{ currentProject?.enrolled }} 人参与
                </span>
                <span class="meta-item">
                  <el-icon><Star /></el-icon> {{ currentProject?.rating }} 分
                </span>
                <span class="meta-item">
                  <el-icon><Clock /></el-icon> {{ currentProject?.duration }}
                </span>
              </div>

              <div class="project-description">
                <h3>项目描述</h3>
                <p>{{ currentProject?.description }}</p>
              </div>

              <div class="project-objectives">
                <h3>学习目标</h3>
                <ul>
                  <li v-for="(objective, index) in currentProject?.objectives" :key="index">
                    {{ objective }}
                  </li>
                </ul>
              </div>

              <div class="project-technologies">
                <h3>技术栈</h3>
                <div class="tech-tags">
                  <el-tag
                    v-for="tech in currentProject?.technologies"
                    :key="tech"
                    type="success"
                  >
                    {{ tech }}
                  </el-tag>
                </div>
              </div>
            </div>
          </el-tab-pane>

          <el-tab-pane label="项目步骤" name="steps">
            <el-timeline>
              <el-timeline-item
                v-for="(step, index) in currentProject?.steps"
                :key="index"
                :type="step.completed ? 'success' : 'primary'"
                :hollow="!step.completed"
              >
                <div class="step-item">
                  <h4>{{ step.title }}</h4>
                  <p>{{ step.description }}</p>
                  <div class="step-content" v-if="step.content">
                    {{ step.content }}
                  </div>
                  <el-button
                    v-if="!step.completed && !step.locked"
                    type="primary"
                    size="small"
                    @click="startStep(index)"
                  >
                    开始此步骤
                  </el-button>
                  <el-button
                    v-if="step.completed"
                    type="success"
                    size="small"
                    disabled
                  >
                    已完成
                  </el-button>
                  <el-tag v-if="step.locked" type="info" size="small">未解锁</el-tag>
                </div>
              </el-timeline-item>
            </el-timeline>
          </el-tab-pane>

          <el-tab-pane label="学习资源" name="resources">
            <el-table :data="currentProject?.resources" style="width: 100%">
              <el-table-column prop="title" label="资源名称" />
              <el-table-column prop="type" label="类型" width="100">
                <template #default="{ row }">
                  <el-tag size="small">{{ row.type }}</el-tag>
                </template>
              </el-table-column>
              <el-table-column prop="duration" label="时长/大小" width="120" />
              <el-table-column label="操作" width="150">
                <template #default="{ row }">
                  <el-button link type="primary" @click="viewResource(row)">
                    查看
                  </el-button>
                  <el-button link type="primary" @click="downloadResource(row)">
                    下载
                  </el-button>
                </template>
              </el-table-column>
            </el-table>
          </el-tab-pane>

          <el-tab-pane label="项目代码" name="code">
            <div class="code-section">
              <el-button type="primary" @click="openIDE">打开在线IDE</el-button>
              <el-button type="info" @click="downloadCode">下载项目代码</el-button>
              <div class="code-preview">
                <p>项目代码结构：</p>
                <pre><code>├── src/
│   ├── components/
│   ├── views/
│   ├── api/
│   ├── utils/
│   └── main.js
├── public/
├── package.json
└── README.md</code></pre>
              </div>
            </div>
          </el-tab-pane>

          <el-tab-pane label="提交作业" name="submit">
            <div class="submit-section">
              <el-form :model="submitForm" label-width="100px">
                <el-form-item label="项目链接">
                  <el-input v-model="submitForm.projectUrl" placeholder="请输入项目在线地址" />
                </el-form-item>
                <el-form-item label="代码仓库">
                  <el-input v-model="submitForm.repoUrl" placeholder="请输入GitHub/Gitee地址" />
                </el-form-item>
                <el-form-item label="说明文档">
                  <el-input
                    v-model="submitForm.description"
                    type="textarea"
                    :rows="4"
                    placeholder="请描述您的项目实现思路..."
                  />
                </el-form-item>
                <el-form-item label="附件上传">
                  <el-upload
                    drag
                    action="/api/upload"
                    multiple
                  >
                    <el-icon class="el-icon--upload"><UploadFilled /></el-icon>
                    <div class="el-upload__text">
                      拖拽文件到此处或 <em>点击上传</em>
                    </div>
                  </el-upload>
                </el-form-item>
                <el-form-item>
                  <el-button type="primary" @click="handleSubmit">提交作业</el-button>
                  <el-button @click="resetForm">重置</el-button>
                </el-form-item>
              </el-form>
            </div>
          </el-tab-pane>
        </el-tabs>
      </el-card>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { User, Star, Clock, ArrowLeft, UploadFilled } from '@element-plus/icons-vue'

interface Project {
  id: number
  title: string
  description: string
  avatar: string
  category: string
  level: string
  tags: string[]
  enrolled: number
  rating: number
  duration: string
  progress: number
  objectives: string[]
  technologies: string[]
  steps: Step[]
  resources: Resource[]
}

interface Step {
  title: string
  description: string
  content?: string
  completed: boolean
  locked: boolean
}

interface Resource {
  title: string
  type: string
  duration: string
}

const filters = reactive({
  category: '',
  level: '',
  keyword: ''
})

const pagination = reactive({
  page: 1,
  size: 9,
  total: 45
})

const isProjectDetail = ref(false)
const currentProject = ref<Project | null>(null)
const activeTab = ref('overview')
const isFavorite = ref(false)

const submitForm = reactive({
  projectUrl: '',
  repoUrl: '',
  description: ''
})

const projects = ref<Project[]>([
  {
    id: 1,
    title: '电商管理系统',
    description: '从零开始构建一个完整的电商管理系统，包含商品管理、订单处理、用户系统等核心功能',
    avatar: '',
    category: 'fullstack',
    level: 'intermediate',
    tags: ['Vue', 'Spring Boot', 'MySQL', '电商'],
    enrolled: 1234,
    rating: 4.8,
    duration: '40小时',
    progress: 35,
    objectives: [
      '掌握前后端分离架构设计',
      '学习RESTful API设计规范',
      '实现完整的CRUD操作',
      '掌握数据库设计和优化',
      '学习项目部署流程'
    ],
    technologies: ['Vue 3', 'Element Plus', 'Spring Boot', 'MySQL', 'Redis', 'Nginx'],
    steps: [
      {
        title: '项目初始化',
        description: '搭建开发环境和项目骨架',
        completed: true,
        locked: false
      },
      {
        title: '数据库设计',
        description: '设计并创建数据库表结构',
        completed: true,
        locked: false
      },
      {
        title: '后端API开发',
        description: '实现商品、订单、用户等API接口',
        completed: false,
        locked: false
      },
      {
        title: '前端页面开发',
        description: '开发管理后台页面',
        completed: false,
        locked: true
      },
      {
        title: '系统测试',
        description: '进行功能测试和性能优化',
        completed: false,
        locked: true
      }
    ],
    resources: [
      { title: '项目需求文档', type: '文档', duration: 'PDF' },
      { title: '数据库设计图', type: '文档', duration: 'PDF' },
      { title: 'API接口文档', type: '文档', duration: 'PDF' },
      { title: '前端开发视频', type: '视频', duration: '2小时' },
      { title: '后端开发视频', type: '视频', duration: '3小时' }
    ]
  },
  {
    id: 2,
    title: '在线学习平台',
    description: '开发一个在线学习平台，支持视频课程播放、作业提交、在线讨论等功能',
    avatar: '',
    category: 'fullstack',
    level: 'advanced',
    tags: ['React', 'Node.js', 'MongoDB', '在线教育'],
    enrolled: 876,
    rating: 4.9,
    duration: '50小时',
    progress: 0,
    objectives: [
      '学习大型项目架构设计',
      '掌握视频流处理技术',
      '实现实时通信功能',
      '学习用户权限管理',
      '掌握性能优化技巧'
    ],
    technologies: ['React', 'Node.js', 'MongoDB', 'Socket.io', 'FFmpeg'],
    steps: [
      {
        title: '需求分析',
        description: '分析项目需求和技术选型',
        completed: false,
        locked: false
      },
      {
        title: '架构设计',
        description: '设计系统架构和数据库模型',
        completed: false,
        locked: true
      },
      {
        title: '后端开发',
        description: '实现核心业务逻辑',
        completed: false,
        locked: true
      },
      {
        title: '前端开发',
        description: '开发用户界面',
        completed: false,
        locked: true
      },
      {
        title: '集成测试',
        description: '进行系统集成测试',
        completed: false,
        locked: true
      }
    ],
    resources: [
      { title: '技术选型分析', type: '文档', duration: 'PDF' },
      { title: '架构设计文档', type: '文档', duration: 'PDF' }
    ]
  },
  {
    id: 3,
    title: '博客系统',
    description: '构建一个现代化的博客系统，包含文章管理、评论系统、标签分类等功能',
    avatar: '',
    category: 'frontend',
    level: 'beginner',
    tags: ['Vue', 'Vite', 'TypeScript', '博客'],
    enrolled: 2345,
    rating: 4.7,
    duration: '20小时',
    progress: 0,
    objectives: [
      '学习Vue 3组合式API',
      '掌握TypeScript基础',
      '学习路由和状态管理',
      '掌握Markdown渲染',
      '学习响应式布局'
    ],
    technologies: ['Vue 3', 'Vite', 'TypeScript', 'Vue Router', 'Pinia', 'markdown-it'],
    steps: [
      {
        title: '项目搭建',
        description: '使用Vite创建项目',
        completed: false,
        locked: false
      },
      {
        title: '路由配置',
        description: '配置Vue Router',
        completed: false,
        locked: true
      },
      {
        title: '状态管理',
        description: '使用Pinia管理状态',
        completed: false,
        locked: true
      },
      {
        title: '页面开发',
        description: '开发各个页面组件',
        completed: false,
        locked: true
      }
    ],
    resources: [
      { title: 'Vue 3教程', type: '文档', duration: 'PDF' },
      { title: 'TypeScript入门', type: '视频', duration: '1小时' }
    ]
  }
])

const getLevelType = (level = '') => {
  const types: Record<string, any> = {
    beginner: 'success',
    intermediate: 'warning',
    advanced: 'danger'
  }
  return types[level] || 'info'
}

const getLevelLabel = (level = '') => {
  const labels: Record<string, string> = {
    beginner: '入门',
    intermediate: '进阶',
    advanced: '高级'
  }
  return labels[level] || level
}

const getCategoryLabel = (category = '') => {
  const labels: Record<string, string> = {
    frontend: '前端项目',
    backend: '后端项目',
    fullstack: '全栈项目'
  }
  return labels[category] || category
}

const handleSearch = () => {
  ElMessage.success('搜索功能开发中...')
}

const handleReset = () => {
  filters.category = ''
  filters.level = ''
  filters.keyword = ''
  ElMessage.success('重置成功')
}

const viewProject = (project: Project) => {
  currentProject.value = project
  isProjectDetail.value = true
  activeTab.value = 'overview'
}

const continueProject = (project: Project) => {
  viewProject(project)
  if (project.progress > 0) {
    activeTab.value = 'steps'
  }
}

const goBack = () => {
  isProjectDetail.value = false
  currentProject.value = null
  activeTab.value = 'overview'
}

const toggleFavorite = () => {
  isFavorite.value = !isFavorite.value
  ElMessage.success(isFavorite.value ? '收藏成功' : '取消收藏成功')
}

const startStep = (index: number) => {
  if (currentProject.value) {
    ElMessage.success(`开始步骤：${currentProject.value.steps[index].title}`)
  }
}

const viewResource = (resource: Resource) => {
  ElMessage.success(`查看资源：${resource.title}`)
}

const downloadResource = (resource: Resource) => {
  ElMessage.success(`开始下载：${resource.title}`)
}

const openIDE = () => {
  ElMessage.success('正在打开在线IDE...')
}

const downloadCode = () => {
  ElMessage.success('正在下载项目代码...')
}

const handleSubmit = () => {
  ElMessageBox.confirm('确定要提交作业吗？', '提示', {
    confirmButtonText: '确定',
    cancelButtonText: '取消',
    type: 'warning'
  }).then(() => {
    ElMessage.success('作业提交成功！')
    resetForm()
  }).catch(() => {})
}

const resetForm = () => {
  submitForm.projectUrl = ''
  submitForm.repoUrl = ''
  submitForm.description = ''
}
</script>

<style scoped>
.projects-page {
  padding: 20px;
}

.filter-card {
  margin-bottom: 20px;
  border-radius: 8px;
}

.project-card {
  margin-bottom: 20px;
  border-radius: 8px;
  cursor: pointer;
  transition: transform 0.3s, box-shadow 0.3s;
  height: 100%;
}

.project-card:hover {
  transform: translateY(-5px);
  box-shadow: 0 8px 20px rgba(0, 0, 0, 0.15);
}

.project-header {
  display: flex;
  gap: 15px;
  margin-bottom: 15px;
}

.project-info {
  flex: 1;
}

.project-info h3 {
  margin: 0 0 5px 0;
  font-size: 16px;
  color: #333;
}

.project-desc {
  color: #666;
  font-size: 14px;
  line-height: 1.6;
  margin-bottom: 15px;
  min-height: 40px;
}

.project-tags {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  margin-bottom: 15px;
}

.project-stats {
  display: flex;
  justify-content: space-between;
  padding: 10px 0;
  border-top: 1px solid #eee;
  border-bottom: 1px solid #eee;
  margin-bottom: 15px;
}

.stat-item {
  display: flex;
  align-items: center;
  gap: 5px;
  font-size: 14px;
  color: #666;
}

.project-progress {
  margin-bottom: 10px;
}

.progress-label {
  font-size: 14px;
  color: #666;
  margin-bottom: 5px;
  display: block;
}

.project-detail {
  animation: fadeIn 0.3s;
}

@keyframes fadeIn {
  from {
    opacity: 0;
    transform: translateY(10px);
  }
  to {
    opacity: 1;
    transform: translateY(0);
  }
}

.detail-card {
  border-radius: 8px;
}

.detail-header {
  display: flex;
  align-items: center;
  gap: 15px;
}

.detail-header h2 {
  margin: 0;
  flex: 1;
  font-size: 20px;
}

.overview-content {
  padding: 20px 0;
}

.project-meta {
  display: flex;
  gap: 15px;
  align-items: center;
  margin-bottom: 20px;
  padding-bottom: 20px;
  border-bottom: 1px solid #eee;
}

.meta-item {
  display: flex;
  align-items: center;
  gap: 5px;
  color: #666;
  font-size: 14px;
}

.project-description h3 {
  margin-bottom: 10px;
  color: #333;
}

.project-description p {
  color: #666;
  line-height: 1.8;
}

.project-objectives {
  margin-top: 30px;
}

.project-objectives h3 {
  margin-bottom: 15px;
  color: #333;
}

.project-objectives ul {
  list-style: none;
  padding: 0;
}

.project-objectives li {
  padding: 10px 0 10px 20px;
  position: relative;
  color: #666;
}

.project-objectives li:before {
  content: '✓';
  position: absolute;
  left: 0;
  color: #67c23a;
  font-weight: bold;
}

.project-technologies {
  margin-top: 30px;
}

.project-technologies h3 {
  margin-bottom: 15px;
  color: #333;
}

.tech-tags {
  display: flex;
  gap: 10px;
  flex-wrap: wrap;
}

.step-item {
  padding: 15px;
  background: #f5f7fa;
  border-radius: 8px;
}

.step-item h4 {
  margin: 0 0 10px 0;
  color: #333;
}

.step-item p {
  color: #666;
  margin-bottom: 15px;
}

.step-content {
  padding: 10px;
  background: #fff;
  border-radius: 6px;
  margin-bottom: 15px;
  color: #666;
}

.code-section {
  padding: 20px 0;
}

.code-section .el-button {
  margin-right: 15px;
}

.code-preview {
  margin-top: 20px;
  padding: 15px;
  background: #282c34;
  border-radius: 8px;
  color: #abb2bf;
}

.code-preview pre {
  margin: 0;
  font-family: 'Courier New', monospace;
}

.submit-section {
  padding: 20px 0;
}

.submit-section .el-form {
  max-width: 800px;
}
</style>
