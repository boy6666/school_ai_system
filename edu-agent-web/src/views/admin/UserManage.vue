<template>
  <div style="padding:20px">
    <el-card>
      <template #header>
        <div style="display:flex;justify-content:space-between;align-items:center">
          <span>用户管理</span>
          <el-input v-model="keyword" placeholder="搜索用户名" style="width:200px" clearable @keyup.enter="load" />
        </div>
      </template>
      <el-table :data="list" stripe v-loading="loading">
        <el-table-column prop="id" label="ID" width="60" />
        <el-table-column prop="username" label="用户名" />
        <el-table-column prop="nickname" label="昵称" />
        <el-table-column prop="role" label="角色" width="80">
          <template #default="{row}">
            <el-tag :type="row.role==='admin'?'danger':row.role==='teacher'?'warning':'success'" size="small">{{ row.role }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="email" label="邮箱" min-width="150" />
        <el-table-column prop="status" label="状态" width="80">
          <template #default="{row}">
            <el-tag :type="row.status==='active'?'success':'info'" size="small">{{ row.status==='active'?'正常':'禁用' }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="lastLoginTime" label="最后登录" width="160" />
        <el-table-column prop="createTime" label="注册时间" width="160" />
        <el-table-column label="操作" width="120">
          <template #default="{row}">
            <el-button text type="danger" size="small" @click="handleDelete(row)">删除</el-button>
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
import { getUserList, deleteUser } from '@/api/admin'

const list = ref<any[]>([])
const loading = ref(false)
const page = ref(1)
const total = ref(0)
const keyword = ref('')

const load = async () => {
  loading.value = true
  try {
    const r = await getUserList({ page: page.value, pageSize: 10, keyword: keyword.value || undefined })
    list.value = r.records || []
    total.value = r.total || 0
  } catch { list.value = []; total.value = 0 }
  loading.value = false
}

const handleDelete = async (row: any) => {
  try {
    await ElMessageBox.confirm(`确定删除 "${row.username}"？`, '确认删除', { type: 'warning' })
    await deleteUser(row.id)
    ElMessage.success('已删除')
    load()
  } catch {}
}

onMounted(load)
</script>
