<template>
  <div class="manage-page">
    <section class="page-header">
      <div>
        <p class="eyebrow">管理后台</p>
        <h1>资源管理</h1>
        <p>管理 AI 生成的思维导图、练习题目、拓展阅读、代码案例等资源。</p>
      </div>
    </section>
    <section class="stat-row" style="margin-bottom:20px">
      <div class="stat-card"><span>资源总数</span><strong>{{ resourceList.length }}</strong></div>
      <div class="stat-card"><span>待审核</span><strong>{{ pendingCount }}</strong></div>
      <div class="stat-card"><span>已通过</span><strong>{{ approvedCount }}</strong></div>
    </section>
    <section class="filter-bar">
      <select v-model="typeFilter"><option value="">全部类型</option><option value="mindmap">思维导图</option><option value="quiz">练习题目</option><option value="reading">拓展阅读</option><option value="code">代码案例</option></select>
      <select v-model="statusFilter"><option value="">全部状态</option><option value="published">已通过</option><option value="draft">待审核</option></select>
      <button @click="search">查询</button>
    </section>
    <div class="table-wrap">
      <table>
        <thead>
          <tr><th>ID</th><th>标题</th><th>类型</th><th>章节</th><th>状态</th><th>创建时间</th><th>操作</th></tr>
        </thead>
        <tbody>
          <tr v-if="loading"><td colspan="7" class="state-cell">加载中...</td></tr>
          <tr v-else-if="!pagedList.length"><td colspan="7" class="state-cell">暂无数据</td></tr>
          <tr v-for="r in pagedList" :key="r.id">
            <td>{{ r.id }}</td>
            <td><strong>{{ r.title }}</strong></td>
            <td><span class="type-tag">{{ {mindmap:'思维导图',quiz:'练习题目',reading:'拓展阅读',code:'代码案例'}[r.type] || r.type }}</span></td>
            <td>{{ r.chapterName || r.courseName || '-' }}</td>
            <td><span :class="['status-badge', r.status==='published'?'active':'pending']">{{ r.status==='published'?'已通过':'待审核' }}</span></td>
            <td>{{ r.createTime ? r.createTime.replace('T',' ').substring(0,16) : '-' }}</td>
            <td class="action-cell">
              <button class="text-btn" @click="preview(r)">预览</button>
              <button class="text-btn" v-if="r.status!=='published'" @click="approve(r)">通过</button>
              <button class="text-btn danger" @click="remove(r)">删除</button>
            </td>
          </tr>
        </tbody>
      </table>
    </div>
    <div class="pagination" v-if="resourceList.length>pageSize">
      <button :disabled="page<=1" @click="page=1;fetchData()">«</button>
      <button :disabled="page<=1" @click="page--;fetchData()">‹</button>
      <button v-for="p in pageNums" :key="p" :class="['page-btn',{active:p===page}]" @click="page=p;fetchData()">{{ p }}</button>
      <button :disabled="page>=maxPage" @click="page++;fetchData()">›</button>
      <button :disabled="page>=maxPage" @click="page=maxPage;fetchData()">»</button>
      <span class="page-info">{{ resourceList.length }} 条</span>
    </div>
  </div>
</template>
<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import request from '@/utils/request'
const typeFilter=ref(''); const statusFilter=ref(''); const page=ref(1); const pageSize=ref(10); const loading=ref(false)
const resourceList=ref<any[]>([])
const pageNums=computed(()=>{const p:number[]=[];const s=Math.max(1,page.value-2);const e=Math.min(maxPage.value,page.value+2);for(let i=s;i<=e;i++) p.push(i);return p})
const maxPage=computed(()=>Math.max(1,Math.ceil(resourceList.value.length/pageSize.value)))
const pagedList=computed(()=>{const start=(page.value-1)*pageSize.value;return resourceList.value.slice(start,start+pageSize.value)})
const pendingCount=computed(()=>resourceList.value.filter((r:any)=>r.status!=='published').length)
const approvedCount=computed(()=>resourceList.value.filter((r:any)=>r.status==='published').length)
const fetchData=async()=>{loading.value=true;page.value=1;try{const res:any=await request.get('/admin/resources',{params:{page:1,pageSize:200,type:typeFilter.value||undefined}});const list=res?.records||[];resourceList.value=list.filter((r:any)=>{const t=!typeFilter.value||r.type===typeFilter.value;const s=!statusFilter.value||r.status===statusFilter.value;return t&&s})}catch{resourceList.value=[]};loading.value=false}
const search=()=>{page.value=1;fetchData()}
const preview=(r:any)=>{window.open(`/student/resources/generate/${r.type}`,'_blank')}
const approve=async(r:any)=>{try{await request.put(`/admin/resources/${r.id}/status`,{status:'published'});r.status='published'}catch{}}
const remove=async(r:any)=>{if(!confirm(`确定删除「${r.title}」？`))return;try{await request.delete(`/admin/resources/${r.id}`);resourceList.value=resourceList.value.filter((x:any)=>x.id!==r.id)}catch{}}
onMounted(fetchData)
</script>
<style scoped>
.manage-page { min-height:100vh; padding:clamp(14px,2vw,28px); background:var(--surface); color:var(--charcoal); }
.page-header { display:flex; justify-content:space-between; gap:20px; padding:28px; margin-bottom:20px; border-radius:var(--radius-xl); background:linear-gradient(135deg,var(--canvas) 0%,var(--tint-sky) 100%); box-shadow:var(--shadow-subtle); }
.eyebrow { margin:0 0 8px; color:var(--primary); font-weight:700; }
.page-header h1 { margin:0; font-size:28px; color:var(--ink); }
.page-header p { color:var(--steel); }
.type-tag { display:inline-block; padding:2px 8px; border-radius:var(--radius-full); font:var(--text-caption); background:var(--tint-lavender); color:var(--primary-deep); }
.table-wrap { background:var(--canvas); border:1px solid var(--hairline); border-radius:var(--radius-lg); overflow-x:auto; margin-bottom:var(--space-lg); }
table { width:100%; border-collapse:collapse; min-width:700px; }
th,td { padding:12px 14px; text-align:left; border-bottom:1px solid var(--hairline-soft); font:var(--text-sm); }
th { color:var(--steel); background:var(--surface-soft); font:var(--text-sm-medium); }
.state-cell { text-align:center; color:var(--muted); padding:40px; }
.action-cell { white-space:nowrap; }
.text-btn { border:none; background:transparent; cursor:pointer; font-size:13px; margin-right:10px; color:var(--link-blue); }
.text-btn.danger { color:var(--error); }
.pagination { display:flex; flex-wrap:wrap; justify-content:center; align-items:center; gap:4px; margin-top:20px; }
.page-btn { border:1px solid var(--hairline); border-radius:var(--radius-sm); background:var(--canvas); cursor:pointer; font-size:14px; min-width:34px; height:34px; display:flex; align-items:center; justify-content:center; padding:0 8px; color:var(--charcoal); }
.page-btn.active { background:var(--primary); color:var(--on-primary); border-color:var(--primary); }
.page-info { margin-left:8px; color:var(--stone); }
</style>
