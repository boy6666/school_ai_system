<template>
  <div style="padding:20px">
    <el-card>
      <template #header><span>资源中心</span></template>
      <el-empty v-if="!loading && list.length===0" description="暂无资源" />
      <div v-else v-loading="loading">
        <el-card v-for="item in list" :key="item.id" shadow="hover" style="margin-bottom:12px">
          <div style="display:flex;justify-content:space-between">
            <span style="font-weight:bold">{{ item.title }}</span>
            <el-tag size="small">{{ item.type }}</el-tag>
          </div>
          <div style="color:#909399;margin-top:4px">{{ item.description || '暂无描述' }}</div>
        </el-card>
      </div>
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { getResourceList } from '@/api/admin'

const list = ref<any[]>([])
const loading = ref(false)
onMounted(async () => {
  loading.value=true
  try {
    const r = await getResourceList({page:1,pageSize:20})
    list.value = r?.records||[]
  } catch { list.value=[] }
  loading.value=false
})
</script>