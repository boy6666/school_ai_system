<template>
  <div style="padding:20px">
    <el-result icon="info" title="暂无数据" sub-title="该模块暂未接入后端">
      <template #extra>
        <el-button type="primary" @click="$router.push('/student/tutor')">去智能辅导</el-button>
      </template>
    </el-result>
  </div>
</template>
