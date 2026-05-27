<template>
  <div class="learning-task">
    <el-card>
      <template #header>
        <span>学习任务</span>
        <el-button type="primary" style="float: right" @click="openCreateDialog">新建任务</el-button>
      </template>
      <el-table :data="tasks" stripe>
        <el-table-column prop="title" label="标题" />
        <el-table-column prop="description" label="描述" />
        <el-table-column prop="dueDate" label="截止时间" />
        <el-table-column prop="status" label="状态">
          <template #default="{ row }">
            <el-tag :type="row.status === 2 ? 'success' : 'info'">
              {{ row.status === 2 ? '已完成' : '未完成' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作">
          <template #default="{ row }">
            <el-button link type="primary" @click="editTask(row)">编辑</el-button>
            <el-button link type="danger" @click="removeTask(row.id)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>
      <el-pagination
        v-model:current-page="pagination.page"
        v-model:page-size="pagination.size"
        :total="pagination.total"
        @current-change="fetchTasks"
      />
    </el-card>

    <el-dialog v-model="dialogVisible" :title="dialogTitle">
      <el-form :model="form" ref="formRef" label-width="100px">
        <el-form-item label="标题" prop="title" required>
          <el-input v-model="form.title" />
        </el-form-item>
        <el-form-item label="描述" prop="description">
          <el-input type="textarea" v-model="form.description" />
        </el-form-item>
        <el-form-item label="截止时间" prop="dueDate">
          <el-date-picker v-model="form.dueDate" type="datetime" placeholder="选择日期时间" />
        </el-form-item>
        <el-form-item label="优先级" prop="priority">
          <el-select v-model="form.priority">
            <el-option :value="0" label="普通" />
            <el-option :value="1" label="重要" />
            <el-option :value="2" label="紧急" />
          </el-select>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" @click="saveTask">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { getTaskList, createTask, updateTask, deleteTask } from '@/api/task'

const tasks = ref([])
const pagination = ref({ page: 1, size: 10, total: 0 })
const dialogVisible = ref(false)
const dialogTitle = ref('新建任务')
const form = ref({ title: '', description: '', dueDate: null, priority: 0 })
const editingId = ref(null)

const fetchTasks = async () => {
  try {
    const res = await getTaskList({ page: pagination.value.page, size: pagination.value.size })
    tasks.value = res.data.records || []
    pagination.value.total = res.data.total || 0
  } catch (error) {
    ElMessage.error('加载失败')
  }
}

const openCreateDialog = () => {
  dialogTitle.value = '新建任务'
  form.value = { title: '', description: '', dueDate: null, priority: 0 }
  editingId.value = null
  dialogVisible.value = true
}

const editTask = (row) => {
  dialogTitle.value = '编辑任务'
  form.value = { ...row }
  editingId.value = row.id
  dialogVisible.value = true
}

const saveTask = async () => {
  try {
    if (editingId.value) {
      await updateTask(editingId.value, form.value)
      ElMessage.success('更新成功')
    } else {
      await createTask(form.value)
      ElMessage.success('创建成功')
    }
    dialogVisible.value = false
    fetchTasks()
  } catch (error) {
    ElMessage.error('操作失败')
  }
}

const removeTask = async (id) => {
  await ElMessageBox.confirm('确定删除吗？', '提示', { type: 'warning' })
  await deleteTask(id)
  ElMessage.success('删除成功')
  fetchTasks()
}

onMounted(fetchTasks)
</script>

<style scoped>
.learning-task { padding: 20px; }
</style>
