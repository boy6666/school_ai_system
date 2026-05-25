<template>
  <div class="page">
    <el-card>
      <template #header><span>系统设置</span></template>
      <el-table :data="list" stripe v-loading="loading">
        <el-table-column prop="settingKey" label="配置项" width="200" />
        <el-table-column prop="settingValue" label="值" />
        <el-table-column prop="description" label="说明" min-width="200" />
        <el-table-column label="操作" width="100">
          <template #default="{row}">
            <el-button text type="primary" size="small" @click="handleEdit(row)">修改</el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { getSettings, updateSetting } from '@/api/admin'

const list = ref<any[]>([])
const loading = ref(false)

const load = async () => {
  loading.value = true
  try {
    list.value = await getSettings() || []
  } catch { list.value = [] }
  loading.value = false
}

const handleEdit = async (row: any) => {
  try {
    const { value } = await ElMessageBox.prompt('输入新值', '修改 ' + row.settingKey, { inputValue: row.settingValue })
    if (value !== null) {
      await updateSetting(row.settingKey, value || '')
      ElMessage.success('已更新')
      load()
    }
  } catch {}
}

onMounted(load)
</script>
<style scoped>.page{padding:20px}</style>
