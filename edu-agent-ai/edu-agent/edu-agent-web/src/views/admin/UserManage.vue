<template>
  <div class="user-manage">
    <el-row :gutter="20">
      <!-- 左侧用户列表（筛选+表格） -->
      <el-col :span="14">
        <el-card shadow="never">
          <template #header>
            <span>用户列表</span>
            <el-button type="primary" size="small" style="float: right" @click="handleAdd">新增用户</el-button>
          
  <el-dialog v-model="dialogVisible" title="新增用户" width="400px">
    <el-form :model="form" label-width="80px">
      <el-form-item label="用户名"><el-input v-model="form.username" /></el-form-item>
      <el-form-item label="密码"><el-input v-model="form.password" type="password" /></el-form-item>
      <el-form-item label="昵称"><el-input v-model="form.nickname" /></el-form-item>
      <el-form-item label="角色">
        <el-select v-model="form.role" style="width:100%">
          <el-option label="学生" value="student" />
          <el-option label="教师" value="teacher" />
          <el-option label="管理员" value="admin" />
        </el-select>
      </el-form-item>
    </el-form>
    <template #footer>
      <el-button @click="dialogVisible = false">取消</el-button>
      <el-button type="primary" @click="submitAdd">确定</el-button>
    </template>
  </el-dialog>

</template>
          <el-input v-model="searchKeyword" placeholder="搜索姓名/学号" prefix-icon="Search" clearable style="margin-bottom: 16px" />
          <el-table :data="userList" stripe @row-click="handleRowClick">
            <el-table-column prop="name" label="姓名" />
            <el-table-column prop="role" label="身份" />
            <el-table-column prop="college" label="学院" />
            <el-table-column prop="email" label="邮箱" />
          </el-table>
          <el-pagination
            v-model:current-page="currentPage"
            v-model:page-size="pageSize"
            :total="1256"
            layout="total, prev, pager, next"
            @current-change="fetchUsers"
          />
        </el-card>
      </el-col>
      <!-- 右侧用户详情 -->
      <el-col :span="10">
        <el-card shadow="never" v-if="selectedUser">
          <template #header><span>用户详情</span></template>
          <div class="user-detail">
            <div class="detail-header">
              <el-avatar :size="64" :src="selectedUser.avatar || 'https://cube.elemecdn.com/0/88/03b6d3b6a6f4e6b8b6c0e6b4d6b6e6b6.png'" />
              <div class="header-info">
                <h3>{{ selectedUser.name }}</h3>
                <p>{{ selectedUser.role }} · {{ selectedUser.college }}</p>
                <p>{{ selectedUser.email }}</p>
              </div>
            </div>
            <el-divider />
            <div class="info-section">
              <div class="info-item"><span class="label">学号</span><span>{{ selectedUser.studentNo || '-' }}</span></div>
              <div class="info-item"><span class="label">手机号</span><span>{{ selectedUser.phone || '-' }}</span></div>
              <div class="info-item"><span class="label">注册时间</span><span>{{ selectedUser.registerTime }}</span></div>
              <div class="info-item"><span class="label">最后登录</span><span>{{ selectedUser.lastLogin }}</span></div>
              <div class="info-item"><span class="label">状态</span><el-tag :type="selectedUser.status === '启用' ? 'success' : 'danger'">{{ selectedUser.status }}</el-tag></div>
              <div class="info-item"><span class="label">所属班级</span><span>{{ selectedUser.className }}</span></div>
            </div>
            <el-divider />
            <div class="operation-log">
              <div class="log-title">操作记录</div>
              <el-table :data="operationLogs" size="small">
                <el-table-column prop="action" label="操作" />
                <el-table-column prop="time" label="操作记录" width="150" />
              </el-table>
            </div>
          </div>
        </el-card>
        <el-empty v-else description="请点击左侧用户查看详情" />
      </el-col>
    </el-row>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { getUserList, deleteUser } from '@/api/admin'
import { register } from '@/api/auth'

const searchKeyword = ref('')
const currentPage = ref(1)
const pageSize = ref(10)
const selectedUser = ref<any>(null)

const userList = ref<any[]>([])
const loading = ref(false)
const total = ref(0)

// 新增用户弹窗
const dialogVisible = ref(false)
const form = ref({ username: '', password: '', nickname: '', role: 'student' })

const loadUsers = async () => {
  loading.value = true
  try {
    const r = await getUserList({ page: currentPage.value, pageSize: pageSize.value, keyword: searchKeyword.value || undefined })
    userList.value = r?.records || []
    total.value = r?.total || 0
  } catch { userList.value = []; total.value = 0 }
  loading.value = false
}

const handleAdd = () => {
  form.value = { username: '', password: '', nickname: '', role: 'student' }
  dialogVisible.value = true
}

const submitAdd = async () => {
  if (!form.value.username || !form.value.password) {
    ElMessage.warning('用户名和密码必填')
    return
  }
  try {
    await register(form.value)
    ElMessage.success('创建成功')
    dialogVisible.value = false
    loadUsers()
  } catch (err: any) {
    ElMessage.error(err?.response?.data?.message || '创建失败')
  }
}

const handleDelete = async (row: any) => {
  try {
    await ElMessageBox.confirm(`确定删除"${row.username}"？`, '确认', { type: 'warning' })
    await deleteUser(row.id)
    ElMessage.success('已删除')
    loadUsers()
  } catch {}
}

const handleRowClick = (row: any) => {
  selectedUser.value = row
}

onMounted(loadUsers)
</script>


<style scoped>
.user-manage { padding: 20px; background-color: #f5f7fa; min-height: 100vh; }
.user-detail .detail-header { display: flex; gap: 16px; align-items: center; }
.header-info h3 { margin: 0 0 4px 0; }
.header-info p { margin: 4px 0; color: #606266; }
.info-item { display: flex; margin-bottom: 12px; }
.info-item .label { width: 80px; color: #909399; }
.log-title { font-weight: bold; margin-bottom: 12px; }
</style>