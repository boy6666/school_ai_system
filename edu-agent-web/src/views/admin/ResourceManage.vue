<template>
  <div class="page">
    <el-card>
      <template #header>
        <div style="display:flex;justify-content:space-between">
          <span>资源管理</span>
          <el-button type="primary" size="small" @click="handleAdd">新增资源</el-button>
        </div>
      </template>
      <el-table :data="list" stripe v-loading="loading">
        <el-table-column prop="id" label="ID" width="60" />
        <el-table-column prop="title" label="标题" min-width="150" show-overflow-tooltip />
        <el-table-column prop="type" label="类型" width="80" />
        <el-table-column prop="difficulty" label="难度" width="80" />
        <el-table-column prop="author" label="作者" width="100" />
        <el-table-column prop="views" label="浏览" width="70" />
        <el-table-column prop="status" label="状态" width="80">
          <template #default="{row}">
            <el-tag :type="row.status==='published'?'success':'info'" size="small">{{ row.status }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="createTime" label="创建时间" width="160" />
        <el-table-column label="操作" width="150">
          <template #default="{row}">
            <el-button text type="primary" size="small" @click="handleEdit(row)">编辑</el-button>
            <el-button text type="danger" size="small" @click="handleDelete(row.id)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>
      <el-pagination style="margin-top:16px;text-align:right" v-model:current-page="page" :total="total" :page-size="10" layout="total,prev,pager,next" @change="load" />
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { getResourceList, deleteResource, createResource, updateResource } from '@/api/admin'

const list = ref<any[]>([])
const loading = ref(false)
const page = ref(1)
const total = ref(0)

const load = async () => {
  loading.value = true
  try {
    const r = await getResourceList({ page: page.value, pageSize: 10 })
    list.value = r.records || []
    total.value = r.total || 0
  } catch { list.value = []; total.value = 0 }
  loading.value = false
}

const handleAdd = async () => {
  try {
    const { value } = await ElMessageBox.prompt('输入标题', '新增资源')
    if (value) {
      await createResource({ title: value, type: '文档', difficulty: '基础', status: 'draft' })
      ElMessage.success('创建成功')
      load()
    }
  } catch {}
}

const handleEdit = async (row: any) => {
  try {
    const { value } = await ElMessageBox.prompt('输入新标题', '编辑', { inputValue: row.title })
    if (value) {
      await updateResource(row.id, { title: value })
      ElMessage.success('更新成功')
      load()
    }
  } catch {}
}

const handleDelete = async (id: number) => {
  try {
    await ElMessageBox.confirm('确认删除？', '警告', { type: 'warning' })
    await deleteResource(id)
    ElMessage.success('已删除')
    load()
  } catch {}
}

onMounted(load)
</script>
<style scoped>.page{padding:20px}</style>
