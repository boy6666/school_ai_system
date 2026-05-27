<template>
  <div class="report-page">
    <el-card>
      <template #header>
        <span>学习报告</span>
        <el-button type="primary" style="float: right" @click="openGenerateDialog">生成报告</el-button>
      </template>
      <el-table :data="reports" stripe>
        <el-table-column prop="title" label="标题" />
        <el-table-column prop="periodStart" label="开始日期" />
        <el-table-column prop="periodEnd" label="结束日期" />
        <el-table-column prop="createTime" label="创建时间" />
        <el-table-column label="操作">
          <template #default="{ row }">
            <el-button link type="primary" @click="viewReport(row)">查看</el-button>
            <el-button link type="danger" @click="removeReport(row.id)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>
      <el-pagination
        v-model:current-page="pagination.page"
        v-model:page-size="pagination.size"
        :total="pagination.total"
        @current-change="fetchReports"
      />
    </el-card>

    <el-dialog v-model="dialogVisible" title="生成报告">
      <el-form :model="form" label-width="100px">
        <el-form-item label="标题" required>
          <el-input v-model="form.title" />
        </el-form-item>
        <el-form-item label="开始日期">
          <el-date-picker v-model="form.periodStart" type="date" placeholder="选择开始日期" />
        </el-form-item>
        <el-form-item label="结束日期">
          <el-date-picker v-model="form.periodEnd" type="date" placeholder="选择结束日期" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" @click="generate">生成</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { getReportList, generateReport, deleteReport } from '@/api/report'

const reports = ref([])
const pagination = ref({ page: 1, size: 10, total: 0 })
const dialogVisible = ref(false)
const form = ref({ title: '', periodStart: '', periodEnd: '' })

const fetchReports = async () => {
  try {
    const res = await getReportList({ page: pagination.value.page, size: pagination.value.size })
    reports.value = res.data.records || []
    pagination.value.total = res.data.total || 0
  } catch (error) {
    ElMessage.error('加载失败')
  }
}

const openGenerateDialog = () => {
  form.value = { title: '', periodStart: '', periodEnd: '' }
  dialogVisible.value = true
}

const generate = async () => {
  if (!form.value.title) {
    ElMessage.warning('请输入标题')
    return
  }
  try {
    await generateReport(form.value)
    ElMessage.success('生成成功')
    dialogVisible.value = false
    fetchReports()
  } catch (error) {
    ElMessage.error('生成失败')
  }
}

const viewReport = (row) => {
  // 可以跳转到详情页或下载，简单弹窗显示 content
  ElMessage.info(`报告内容：${row.content || '暂无内容'}`)
}

const removeReport = async (id) => {
  await ElMessageBox.confirm('确定删除吗？', '提示', { type: 'warning' })
  await deleteReport(id)
  ElMessage.success('删除成功')
  fetchReports()
}

onMounted(fetchReports)
</script>

<style scoped>
.report-page { padding: 20px; }
</style>
