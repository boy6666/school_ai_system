<template>
  <div class="user-manage">
    <el-row :gutter="20">
      <!-- 左侧用户列表（筛选+表格） -->
      <el-col :span="14">
        <el-card shadow="never">
          <template #header>
            <span>用户列表</span>
            <el-button type="primary" size="small" style="float: right" @click="handleAdd">新增用户</el-button>
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
import { ref } from 'vue'

const searchKeyword = ref('')
const currentPage = ref(1)
const pageSize = ref(10)
const selectedUser = ref<any>(null)

// Mock 用户列表数据
const userList = ref([
  { id: 1, name: '张三', role: '学生', college: '计算机学院', email: 'zhangsan@univ.edu.cn', studentNo: '123456', phone: '138****1234', registerTime: '2024-05-16 10:23', lastLogin: '2024-05-16 10:45', status: '启用', className: '计算机科学与技术 2021级1班' },
  { id: 2, name: '李四', role: '学生', college: '软件学院', email: 'lisi@univ.edu.cn', studentNo: '654321', phone: '139****5678', registerTime: '2024-04-10 09:12', lastLogin: '2024-05-15 14:30', status: '启用', className: '软件工程 2021级2班' }
])
const operationLogs = ref([
  { action: '新增用户', time: '2024-05-15 08:00' },
  { action: '修改用户', time: '2024-05-15 09:00' },
  { action: '删除用户', time: '2024-05-15 10:00' }
])

const fetchUsers = () => {
  // 模拟分页请求
}

const handleRowClick = (row: any) => {
  selectedUser.value = row
  // 可根据不同用户加载不同的操作记录，此处简单mock
}

const handleAdd = () => {
  // 打开新增用户弹窗
}
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
