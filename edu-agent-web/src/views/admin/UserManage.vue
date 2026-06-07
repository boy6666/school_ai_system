<template>
  <div class="user-page">
    <section class="page-header">
      <div>
        <p class="eyebrow">管理后台</p>
        <h1>用户管理</h1>
        <p>管理系统中的所有用户，包含管理员、教师和学生。</p>
      </div>
      <button class="primary-btn" @click="showAddDialog">+ 添加用户</button>
    </section>
    <section class="filter-bar">
      <input v-model="keyword" type="text" placeholder="搜索用户名、邮箱..." @keyup.enter="loadUsers" />
      <select v-model="roleFilter"><option value="">全部角色</option><option value="admin">管理员</option><option value="teacher">教师</option><option value="student">学生</option></select>
      <select v-model="statusFilter"><option value="">全部状态</option><option value="active">正常</option><option value="inactive">已停用</option></select>
      <button @click="loadUsers">查询</button>
    </section>
    <div class="table-wrap">
      <table>
        <thead>
          <tr><th>ID</th><th>用户名</th><th>昵称</th><th>邮箱</th><th>角色</th><th>状态</th><th>创建时间</th><th>操作</th></tr>
        </thead>
        <tbody>
          <tr v-if="loading"><td colspan="8" class="state-cell">加载中...</td></tr>
          <tr v-else-if="!users.length"><td colspan="8" class="state-cell">暂无用户</td></tr>
          <tr v-for="u in users" :key="u.id">
            <td>{{ u.id }}</td>
            <td><strong>{{ u.username }}</strong></td>
            <td>{{ u.nickname || '-' }}</td>
            <td>{{ u.email || '-' }}</td>
            <td><span :class="['status-badge', u.role]">{{ {admin:'管理员',teacher:'教师',student:'学生'}[u.role] || u.role }}</span></td>
            <td><span :class="['status-badge', u.status === 'active' ? 'active' : 'inactive']">{{ u.status === 'active' ? '正常' : '已停用' }}</span></td>
            <td>{{ u.createTime ? u.createTime.replace('T',' ').substring(0,16) : '-' }}</td>
            <td class="action-cell">
              <button class="text-btn" @click="toggleStatus(u)">{{ u.status === 'active' ? '停用' : '启用' }}</button>
              <button class="text-btn danger" @click="delUser(u)">删除</button>
            </td>
          </tr>
        </tbody>
      </table>
    </div>
    <div class="pagination" v-if="total > pageSize">
      <button :disabled="page<=1" @click="page=1;loadUsers()">«</button>
      <button :disabled="page<=1" @click="page--;loadUsers()">‹</button>
      <button v-for="p in pageNums" :key="p" :class="['page-btn',{active:p===page}]" @click="page=p;loadUsers()">{{ p }}</button>
      <button :disabled="page>=maxPage" @click="page++;loadUsers()">›</button>
      <button :disabled="page>=maxPage" @click="page=maxPage;loadUsers()">»</button>
      <span class="page-info">{{ total }} 条</span>
    </div>
    <div v-if="dialogVisible" class="dialog-mask" @click.self="dialogVisible=false">
      <div class="dialog">
        <h3>添加用户</h3>
        <div class="form-grid">
          <label>用户名 <input v-model="form.username" type="text" placeholder="必填" /></label>
          <label>昵称 <input v-model="form.nickname" type="text" /></label>
          <label>邮箱 <input v-model="form.email" type="email" /></label>
          <label>密码 <input v-model="form.password" type="password" placeholder="必填" /></label>
          <label>角色 <select v-model="form.role"><option value="student">学生</option><option value="teacher">教师</option><option value="admin">管理员</option></select></label>
        </div>
        <div class="dialog-actions">
          <button class="outline-btn" @click="dialogVisible=false">取消</button>
          <button class="primary-btn" @click="submitAdd">确定</button>
        </div>
      </div>
    </div>
  </div>
</template>
<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import request from '@/utils/request'
const keyword = ref(''); const roleFilter = ref(''); const statusFilter = ref('')
const page = ref(1); const pageSize = ref(10); const total = ref(0); const loading = ref(false)
const users = ref<any[]>([]); const dialogVisible = ref(false)
const form = ref({ username:'', nickname:'', email:'', password:'', role:'student' })
const maxPage = computed(()=>Math.max(1,Math.ceil(total.value/pageSize.value)))
const pageNums = computed(()=>{ const p:number[]=[]; const s=Math.max(1,page.value-2); const e=Math.min(maxPage.value,page.value+2); for(let i=s;i<=e;i++) p.push(i); return p })
const loadUsers = async()=>{ loading.value=true; try{ const r:any=await request.get('/admin/users',{params:{page:page.value,pageSize:pageSize.value,keyword:keyword.value||undefined}}); users.value=r?.records||[]; total.value=r?.total||0 }catch{}; loading.value=false }
const showAddDialog=()=>{ form.value={username:'',nickname:'',email:'',password:'',role:'student'}; dialogVisible.value=true }
const submitAdd=async()=>{ if(!form.value.username||!form.value.password) return alert('必填'); try{ await request.post('/auth/register',form.value); alert('创建成功'); dialogVisible.value=false; loadUsers() }catch(err:any){ alert(err?.response?.data?.message||'失败') } }
const toggleStatus=async(u:any)=>{ const next=u.status==='active'?'inactive':'active'; if(!confirm(`确定${next==='active'?'启用':'停用'} ${u.username}？`)) return; try{ await request.put(`/admin/users/${u.id}/role`,{status:next}); u.status=next }catch{} }
const delUser=async(u:any)=>{ if(!confirm(`确定删除 ${u.username}？不可恢复！`)) return; try{ await request.delete(`/admin/users/${u.id}`); loadUsers() }catch{} }
onMounted(loadUsers)
</script>
<style scoped>
.user-page { min-height: 100vh; padding: clamp(14px,2vw,28px); background: var(--surface); color: var(--charcoal); }
.page-header { display:flex; justify-content:space-between; gap:20px; padding:28px; margin-bottom:20px; border-radius:var(--radius-xl); background:linear-gradient(135deg,var(--canvas) 0%,var(--tint-lavender) 100%); box-shadow:var(--shadow-subtle); }
.eyebrow { margin:0 0 8px; color:var(--primary); font-weight:700; }
.page-header h1 { margin:0; font-size:28px; color:var(--ink); }
.page-header p { color:var(--steel); }
.primary-btn { height:40px; padding:0 18px; border-radius:var(--radius-md); cursor:pointer; font:var(--text-button); border:none; color:var(--on-primary); background:var(--primary); }
.outline-btn { height:40px; padding:0 18px; border-radius:var(--radius-md); cursor:pointer; font:var(--text-button); border:1px solid var(--hairline-strong); color:var(--primary); background:var(--canvas); }
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
.page-btn:disabled { opacity:0.35; cursor:not-allowed; }
.page-btn.active { background:var(--primary); color:var(--on-primary); border-color:var(--primary); }
.page-info { margin-left:8px; color:var(--stone); }
.dialog-mask { position:fixed; inset:0; z-index:20; display:flex; align-items:center; justify-content:center; padding:16px; background:rgba(15,23,42,0.35); }
.dialog { width:min(480px,100%); padding:24px; border-radius:var(--radius-xl); background:var(--canvas); }
.dialog h3 { margin:0 0 16px; font:var(--text-h3); }
.form-grid { display:grid; gap:14px; }
.form-grid label { display:grid; gap:6px; color:var(--slate); font:var(--text-sm); }
.form-grid input,.form-grid select { height:40px; padding:0 12px; border:1px solid var(--hairline-strong); border-radius:var(--radius-md); outline:none; background:var(--canvas); font:var(--text-sm); color:var(--charcoal); }
.dialog-actions { display:flex; justify-content:flex-end; gap:10px; margin-top:18px; }
@media(max-width:760px){ .page-header{flex-direction:column} }
</style>
