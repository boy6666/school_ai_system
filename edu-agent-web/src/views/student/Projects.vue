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

    <div v-if="!isProjectDetail" v-loading="loading">
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
        @size-change="handleSizeChange"
        @current-change="handlePageChange"
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
              <el-empty description="项目代码接口暂未开放" />
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
                  <el-alert title="附件上传接口暂未开放" type="info" :closable="false" />
                </el-form-item>
                <el-form-item>
                  <el-button type="primary" :loading="submitting" @click="handleSubmit">提交作业</el-button>
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
import { reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { User, Star, Clock, ArrowLeft } from '@element-plus/icons-vue'
import {
  getProjectDetail,
  getProjectList,
  joinProject,
  submitProject,
  type ProjectItem,
  type ProjectResource
} from '@/api/project'

const filters = reactive({ category: '', level: '', keyword: '' })
const pagination = reactive({ page: 1, size: 9, total: 0 })
const projects = ref<ProjectItem[]>([])
const loading = ref(false)
const submitting = ref(false)
const isProjectDetail = ref(false)
const currentProject = ref<ProjectItem | null>(null)
const activeTab = ref('overview')
const isFavorite = ref(false)

const submitForm = reactive({ projectUrl: '', repoUrl: '', description: '' })

type TagType = 'success' | 'warning' | 'danger' | 'info'
const getLevelType = (level = ''): TagType => {
  const types: Record<string, TagType> = {
    beginner: 'success', intermediate: 'warning', advanced: 'danger'
  }
  return types[level] || 'info'
}

const getLevelLabel = (level = '') => {
  const labels: Record<string, string> = {
    beginner: '入门', intermediate: '进阶', advanced: '高级'
  }
  return labels[level] || level
}

const getCategoryLabel = (category = '') => {
  const labels: Record<string, string> = {
    frontend: '前端项目', backend: '后端项目', fullstack: '全栈项目'
  }
  return labels[category] || category
}

const loadProjects = async () => {
  loading.value = true
  try {
    const result = await getProjectList({
      category: filters.category || undefined,
      level: filters.level || undefined,
      keyword: filters.keyword || undefined,
      page: pagination.page,
      pageSize: pagination.size
    })
    projects.value = result.records
    pagination.total = result.total
  } catch {
    projects.value = []
    pagination.total = 0
    ElMessage.error('项目列表加载失败，请稍后重试')
  } finally {
    loading.value = false
  }
}

const handleSearch = () => { pagination.page = 1; loadProjects() }
const handleReset = () => {
  filters.category = ''; filters.level = ''; filters.keyword = ''; pagination.page = 1
  loadProjects()
}
const handleSizeChange = (size: number) => { pagination.size = size; loadProjects() }
const handlePageChange = (page: number) => { pagination.page = page; loadProjects() }

const viewProject = async (project: ProjectItem) => {
  try {
    currentProject.value = await getProjectDetail(project.id)
    isProjectDetail.value = true
    activeTab.value = 'overview'
  } catch {
    ElMessage.error('项目详情加载失败，请稍后重试')
  }
}

const continueProject = async (project: ProjectItem) => {
  try {
    currentProject.value = project.progress > 0
      ? await getProjectDetail(project.id)
      : await joinProject(project.id)
    isProjectDetail.value = true
    activeTab.value = project.progress > 0 ? 'steps' : 'overview'
    await loadProjects()
  } catch {
    ElMessage.error(project.progress > 0 ? '项目加载失败，请稍后重试' : '加入项目失败，请稍后重试')
  }
}

const goBack = () => {
  isProjectDetail.value = false; currentProject.value = null; activeTab.value = 'overview'
}
const toggleFavorite = () => ElMessage.info('项目收藏接口暂未开放')
const startStep = (_index: number) => ElMessage.info('项目步骤更新接口暂未开放')

const openResourceUrl = (url?: string) => {
  if (!url) { ElMessage.info('该资源暂未提供访问地址'); return }
  window.open(url, '_blank', 'noopener,noreferrer')
}
const viewResource = (resource: ProjectResource) => openResourceUrl(resource.url)
const downloadResource = (resource: ProjectResource) => openResourceUrl(resource.downloadUrl)
const openIDE = () => ElMessage.info('在线 IDE 接口暂未开放')
const downloadCode = () => ElMessage.info('项目代码下载接口暂未开放')

const handleSubmit = async () => {
  if (!currentProject.value) return
  if (!submitForm.projectUrl.trim() && !submitForm.repoUrl.trim()) {
    ElMessage.warning('请至少填写项目链接或代码仓库地址')
    return
  }
  try {
    await ElMessageBox.confirm('确定要提交作业吗？', '提示', {
      confirmButtonText: '确定', cancelButtonText: '取消', type: 'warning'
    })
  } catch { return }

  submitting.value = true
  try {
    await submitProject({
      projectId: currentProject.value.id,
      projectUrl: submitForm.projectUrl.trim(),
      repoUrl: submitForm.repoUrl.trim(),
      description: submitForm.description.trim()
    })
    ElMessage.success('作业提交成功')
    resetForm()
  } catch {
    ElMessage.error('作业提交失败，请稍后重试')
  } finally {
    submitting.value = false
  }
}

const resetForm = () => {
  submitForm.projectUrl = ''; submitForm.repoUrl = ''; submitForm.description = ''
}

loadProjects()
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