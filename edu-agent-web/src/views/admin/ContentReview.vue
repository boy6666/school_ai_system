<template>
  <div class="review-page">
    <section class="page-header">
      <div>
        <p class="eyebrow">管理后台</p>
        <h1>内容审核</h1>
        <p>审核 AI 生成的对话记录和学习资源，通过后展示给用户，不通过则从数据库删除。</p>
      </div>
    </section>
    <section class="stat-row" style="margin-bottom:20px">
      <div class="stat-card"><span>待审核</span><strong>{{ pendingCount }}</strong></div>
      <div class="stat-card"><span>已通过</span><strong>{{ approvedCount }}</strong></div>
    </section>
    <section class="filter-bar">
      <select v-model="typeFilter"><option value="">全部类型</option><option value="conversation">聊天对话</option><option value="mindmap">思维导图</option><option value="quiz">练习题目</option><option value="reading">拓展阅读</option><option value="code">代码案例</option></select>
      <select v-model="statusFilter"><option value="">全部状态</option><option value="published">已通过</option><option value="pending">待审核</option></select>
      <button @click="search">查询</button>
    </section>
    <div class="table-wrap">
      <table>
        <thead>
          <tr><th>ID</th><th>标题 / 内容</th><th>类型</th><th>来源</th><th>状态</th><th>时间</th><th>操作</th></tr>
        </thead>
        <tbody>
          <tr v-if="loading"><td colspan="7" class="state-cell">加载中...</td></tr>
          <tr v-else-if="!pagedList.length"><td colspan="7" class="state-cell">暂无数据</td></tr>
          <tr v-for="item in pagedList" :key="item.id">
            <td>{{ item.id }}</td>
            <td><strong class="title-cell" @click="preview(item)">{{ item.title }}</strong></td>
            <td><span class="type-tag">{{ item.typeName }}</span></td>
            <td>{{ item.source }}</td>
            <td><span :class="['status-badge', item.status==='published'?'active':'pending']">{{ item.status==='published'?'已通过':'待审核' }}</span></td>
            <td>{{ item.time }}</td>
            <td class="action-cell">
              <button class="text-btn" @click="preview(item)">预览</button>
              <button class="text-btn" v-if="item.status!=='published'" @click="approve(item)">通过</button>
              <button class="text-btn danger" @click="reject(item)">不通过</button>
            </td>
          </tr>
        </tbody>
      </table>
    </div>
    <div class="pagination" v-if="allItems.length>pageSize">
      <button :disabled="page<=1" @click="page=1;loadAll()">«</button>
      <button :disabled="page<=1" @click="page--;loadAll()">‹</button>
      <button v-for="p in pageNums" :key="p" :class="['page-btn',{active:p===page}]" @click="page=p;loadAll()">{{ p }}</button>
      <button :disabled="page>=maxPage" @click="page++;loadAll()">›</button>
      <button :disabled="page>=maxPage" @click="page=maxPage;loadAll()">»</button>
      <span class="page-info">{{ allItems.length }} 条</span>
    </div>
  </div>
</template>
<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import request from '@/utils/request'
const typeFilter=ref(''); const statusFilter=ref(''); const page=ref(1); const pageSize=ref(10); const loading=ref(false)
const allItems=ref<any[]>([])
const pageNums=computed(()=>{const p:number[]=[];const s=Math.max(1,page.value-2);const e=Math.min(maxPage.value,page.value+2);for(let i=s;i<=e;i++) p.push(i);return p})
const maxPage=computed(()=>Math.max(1,Math.ceil(allItems.value.length/pageSize.value)))
const pendingCount=computed(()=>allItems.value.filter((x:any)=>x.status!=='published').length)
const approvedCount=computed(()=>allItems.value.filter((x:any)=>x.status==='published').length)
const pagedList=computed(()=>{let list=allItems.value;if(typeFilter.value)list=list.filter((x:any)=>x.dataType===typeFilter.value);if(statusFilter.value)list=list.filter((x:any)=>x.status===statusFilter.value);const start=(page.value-1)*pageSize.value;return list.slice(start,start+pageSize.value)})
const loadAll=async()=>{loading.value=true;const items:any[]=[];page.value=1
  try{const cv:any=await request.get('/admin/conversations',{params:{page:1,pageSize:100}});(cv?.records||[]).forEach((c:any)=>{items.push({id:'c'+c.id,dataType:'conversation',typeName:'聊天对话',title:(c.question||'').substring(0,80)||'对话',source:'学生#'+c.studentId,status:c.status||'pending',time:c.createTime?c.createTime.replace('T',' ').substring(0,16):'-',raw:c})})}catch{}
  try{const rr:any=await request.get('/admin/resources',{params:{page:1,pageSize:200}});(rr?.records||[]).forEach((r:any)=>{if(!['mindmap','quiz','reading','code'].includes(r.type))return;items.push({id:'r'+r.id,dataType:r.type,typeName:({mindmap:'思维导图',quiz:'练习题目',reading:'拓展阅读',code:'代码案例'}as Record<string,string>)[r.type]||r.type,title:r.title||r.courseName||'资源',source:(r.type||'')+' · '+(r.chapterName||r.courseName||''),status:r.status==='published'?'published':'pending',time:r.createTime?r.createTime.replace('T',' ').substring(0,16):'-',raw:r})})}catch{}
  allItems.value=items.sort((a:any,b:any)=>b.id.localeCompare(a.id));loading.value=false}
const search=()=>{page.value=1;loadAll()}
const fetchAll=loadAll
const preview=(item:any)=>{window.open(item.dataType==='conversation'?'/student/tutor':`/student/resources/generate/${item.dataType}`,'_blank')}
const approve=async(item:any)=>{if(!confirm('确定通过？'))return;try{if(item.dataType==='conversation'){await request.put(`/admin/conversations/${item.raw.id}/approve`)}else{await request.put(`/admin/resources/${item.raw.id}/status`,{status:'published'})};item.status='published'}catch{}}
const reject=async(item:any)=>{if(!confirm('确定不通过？将从数据库永久删除！'))return;try{if(item.dataType==='conversation'){await request.put(`/admin/conversations/${item.raw.id}/reject`,{reason:'审核不通过'})}else{await request.delete(`/admin/resources/${item.raw.id}`)};allItems.value=allItems.value.filter((x:any)=>x.id!==item.id)}catch{}}
onMounted(loadAll)
</script>
<style scoped>
.review-page { min-height:100vh; padding:clamp(14px,2vw,28px); background:var(--surface); color:var(--charcoal); }
.page-header { display:flex; justify-content:space-between; gap:20px; padding:28px; margin-bottom:20px; border-radius:var(--radius-xl); background:linear-gradient(135deg,var(--canvas) 0%,var(--tint-mint) 100%); box-shadow:var(--shadow-subtle); }
.eyebrow { margin:0 0 8px; color:var(--primary); font-weight:700; }
.page-header h1 { margin:0; font-size:28px; color:var(--ink); }
.page-header p { color:var(--steel); }
.title-cell { cursor:pointer; color:var(--link-blue); font-weight:500; }
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
@media(max-width:640px){ .stat-row{grid-template-columns:1fr} }
</style>
