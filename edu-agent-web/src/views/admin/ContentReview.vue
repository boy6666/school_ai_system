<template>
  <div class="page">
    <el-row :gutter="20">
      <el-col :span="6" v-for="c in cards" :key="c.label">
        <el-card shadow="hover"><div style="text-align:center"><div style="font-size:28px;color:#409eff">{{ c.value }}</div><div style="color:#909399">{{ c.label }}</div></div></el-card>
      </el-col>
    </el-row>
    <el-card style="margin-top:20px">
      <template #header>
        <div style="display:flex;justify-content:space-between">
          <span>对话审核</span>
          <el-input v-model="keyword" placeholder="搜索内容" style="width:200px" clearable @keyup.enter="load" />
        </div>
      </template>
      <el-table :data="list" stripe v-loading="loading" max-height="500">
        <el-table-column prop="id" label="ID" width="60" />
        <el-table-column prop="studentId" label="学生ID" width="80" />
        <el-table-column prop="question" label="问题" min-width="150" show-overflow-tooltip />
        <el-table-column prop="answer" label="回答" min-width="200" show-overflow-tooltip />
        <el-table-column prop="intent" label="意图" width="80" />
        <el-table-column prop="createTime" label="时间" width="160" />
        <el-table-column label="标记" width="100">
          <template #default="{row}">
            <el-tag v-if="row.resourceDir" type="warning" size="small">{{ row.resourceDir }}</el-tag>
            <el-tag v-else type="success" size="small">正常</el-tag>
          </template>
        </el-table-column>
      </el-table>
      <el-pagination style="margin-top:16px;text-align:right" v-model:current-page="page" :total="total" :page-size="10" layout="total,prev,pager,next" @change="load" />
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { getConversationList, getReviewStats } from '@/api/admin'

const list = ref<any[]>([])
const loading = ref(false)
const page = ref(1)
const total = ref(0)
const keyword = ref('')
const cards = ref([{label:'总对话',value:0},{label:'今日',value:0},{label:'讲解',value:0},{label:'练习',value:0}])

const load = async () => {
  loading.value = true
  try {
    const r = await getConversationList({ page: page.value, pageSize: 10, keyword: keyword.value })
    list.value = r.records || []
    total.value = r.total || 0
  } catch { list.value = []; total.value = 0 }
  try {
    const s = await getReviewStats()
    cards.value = [
      {label:'总对话',value:s?.total??0},
      {label:'今日',value:s?.today??0},
      {label:'讲解',value:s?.byExplain??0},
      {label:'练习',value:s?.byQuiz??0},
    ]
  } catch {}
  loading.value = false
}

onMounted(load)
</script>
<style scoped>.page{padding:20px}</style>
