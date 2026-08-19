<template>
  <div class="generate-page">
    <el-button text @click="goBack">
      ← 返回资源中心
    </el-button>

    <el-result
      icon="info"
      title="资源生成功能暂未开放"
      sub-title="当前后端资源契约尚未提供AI生成、重新生成和反馈接口。接口确认后再接入该功能。"
    >
      <template #extra>
        <el-button type="primary" @click="goBack">
          查看已有资源
        </el-button>
      </template>
    </el-result>
  </div>
</template>

<script setup lang="ts">
import { useRouter } from 'vue-router'

const router = useRouter()

function goBack(): void {
  router.push('/student/resources')
}
</script>

<style scoped>
.generate-page {
  max-width: 900px;
  min-height: calc(100vh - 60px);
  margin: 0 auto;
  padding: 32px 40px;
}
</style>