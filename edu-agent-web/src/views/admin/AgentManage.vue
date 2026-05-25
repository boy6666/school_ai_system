<template>
  <div class="page">
    <el-card>
      <template #header>
        <div style="display:flex;justify-content:space-between">
          <span>智能体管理</span>
          <el-button type="primary" size="small" @click="handleAdd">新增智能体</el-button>
        </div>
      </template>
      <el-table :data="list" stripe v-loading="loading">
        <el-table-column prop="id" label="ID" width="60" />
        <el-table-column prop="name" label="名称" />
        <el-table-column prop="type" label="类型" width="120" />
        <el-table-column prop="model" label="模型" width="120" />
        <el-table-column prop="status" label="状态" width="80">
          <template #default="{row}">
            <el-tag :type="row.status==='active'?'success':'info'">{{ row.status }}</el-tag>
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
import { getAgentList, deleteAgent, createAgent, updateAgent } from '@/api/admin'

const list = ref<any[]>([])
const loading = ref(false)
const page = ref(1)
const total = ref(0)

const load = async () => {
  loading.value = true
  try {
    const r = await getAgentList({ page: page.value, pageSize: 10 })
    list.value = r.records || []
    total.value = r.total || 0
  } catch { list.value = []; total.value = 0 }
  loading.value = false
}

const handleAdd = async () => {
  try {
    const { value } = await ElMessageBox.prompt('输入名称,类型,模型 (逗号分隔)', '新增', { confirmButtonText: '确定' })
    const [name, type, model] = (value || '').split(',')
    if (name && type && model) {
      await createAgent({ name: name.trim(), type: type.trim(), model: model.trim(), status: 'active' })
      ElMessage.success('创建成功')
      load()
    }
  } catch {}
}

const handleEdit = async (row: any) => {
  try {
    const { value } = await ElMessageBox.prompt('输入新状态 (active/inactive)', '编辑', { inputValue: row.status })
    if (value) {
      await updateAgent(row.id, { status: value })
      ElMessage.success('更新成功')
      load()
    }
  } catch {}
}

const handleDelete = async (id: number) => {
  try {
    await ElMessageBox.confirm('确认删除？', '警告', { type: 'warning' })
    await deleteAgent(id)
    ElMessage.success('已删除')
    load()
  } catch {}
}

onMounted(load)
</script>
<style scoped>.page{padding:20px}</style>
