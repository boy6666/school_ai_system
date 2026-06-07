<template>
  <div class="statistics-page">
    <el-row :gutter="20" class="stats-row">
      <el-col :span="6">
        <el-card class="stat-card">
          <div class="stat-content">
            <div class="stat-icon users"><el-icon><User /></el-icon></div>
            <div class="stat-info"><div class="stat-value">{{ stats.users }}</div><div class="stat-label">注册用户</div></div>
          </div>
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card class="stat-card">
          <div class="stat-content">
            <div class="stat-icon online"><el-icon><UserFilled /></el-icon></div>
            <div class="stat-info"><div class="stat-value">{{ stats.online }}</div><div class="stat-label">活跃用户</div></div>
          </div>
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card class="stat-card">
          <div class="stat-content">
            <div class="stat-icon exercises"><el-icon><DocumentChecked /></el-icon></div>
            <div class="stat-info"><div class="stat-value">{{ stats.exercises }}</div><div class="stat-label">总对话数</div></div>
          </div>
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card class="stat-card">
          <div class="stat-content">
            <div class="stat-icon projects"><el-icon><FolderChecked /></el-icon></div>
            <div class="stat-info"><div class="stat-value">{{ stats.projects }}</div><div class="stat-label">今日对话</div></div>
          </div>
        </el-card>
      </el-col>
    </el-row>
    <el-row :gutter="20" class="charts-row">
      <el-col :span="12"><el-card class="chart-card"><template #header><span>用户分布（按角色）</span></template><div ref="userChart" class="chart-container"></div></el-card></el-col>
      <el-col :span="12"><el-card class="chart-card"><template #header><span>资源类型分布</span></template><div ref="resourceChart" class="chart-container"></div></el-card></el-col>
    </el-row>
    <el-row :gutter="20" class="charts-row">
      <el-col :span="12"><el-card class="chart-card"><template #header><span>对话趋势（近7天）</span></template><div ref="convChart" class="chart-container"></div></el-card></el-col>
      <el-col :span="12"><el-card class="chart-card"><template #header><span>任务完成率</span></template><div ref="taskChart" class="chart-container"></div></el-card></el-col>
    </el-row>
  </div>
</template>
<script setup lang="ts">
import { ref, onMounted, nextTick } from 'vue'
import request from '@/utils/request'
import { User, UserFilled, DocumentChecked, FolderChecked } from '@element-plus/icons-vue'
import * as echarts from 'echarts'
const stats=ref({users:0,online:0,exercises:0,projects:0})
const userChart=ref<HTMLElement>(); const resourceChart=ref<HTMLElement>(); const convChart=ref<HTMLElement>(); const taskChart=ref<HTMLElement>()
let chartData={adminCount:0,studentCount:0,resources:[] as any[],convs:[] as any[],tasks:[] as any[]}
const initCharts=()=>{
  console.log('[统计] 图表数据:',JSON.stringify({resources:chartData.resources.length,convs:chartData.convs.length,tasks:chartData.tasks.length,admin:chartData.adminCount,student:chartData.studentCount}))
  chartData.adminCount=chartData.adminCount||0; chartData.studentCount=chartData.studentCount||0
  if(userChart.value){echarts.init(userChart.value).setOption({tooltip:{trigger:'item'},series:[{type:'pie',radius:['40%','70%'],data:[{value:chartData.adminCount,name:'管理员',itemStyle:{color:'#f56c6c'}},{value:chartData.studentCount,name:'学生',itemStyle:{color:'#5645d4'}}],label:{show:true,formatter:'{b}: {c}'}}]})}
  if(resourceChart.value){const m:{[k:string]:number}={mindmap:0,quiz:0,reading:0,code:0};chartData.resources.forEach((r:any)=>{if(m[r.type]!==undefined)m[r.type]++});echarts.init(resourceChart.value).setOption({tooltip:{trigger:'axis'},xAxis:{type:'category',data:['思维导图','练习题目','拓展阅读','代码案例']},yAxis:{type:'value'},series:[{type:'bar',data:[m.mindmap,m.quiz,m.reading,m.code],itemStyle:{color:'#5645d4'}}]})}
  if(convChart.value){const days:string[]=[];const counts:number[]=[];const now=new Date();for(let i=6;i>=0;i--){const d=new Date(now);d.setDate(d.getDate()-i);const key=d.toISOString().substring(0,10);days.push(key.substring(5));counts.push(chartData.convs.filter((c:any)=>{const t=c.createTime||'';return t.substring(0,10)===key}).length)};echarts.init(convChart.value).setOption({tooltip:{trigger:'axis'},xAxis:{type:'category',data:days},yAxis:{type:'value'},series:[{type:'line',data:counts,smooth:true,areaStyle:{color:'rgba(86,69,212,0.2)'},itemStyle:{color:'#5645d4'}}]})}
  if(taskChart.value){const done=chartData.tasks.filter((t:any)=>t.status==='completed'||t.status==='2'||t.status==='done').length;const pending=chartData.tasks.length-done;echarts.init(taskChart.value).setOption({tooltip:{trigger:'item'},series:[{type:'pie',data:[{value:done,name:'已完成',itemStyle:{color:'#67c23a'}},{value:pending,name:'未完成',itemStyle:{color:'#e6a23c'}}],label:{show:true,formatter:'{b}: {c}'}}]})}
}
onMounted(async()=>{try{const s:any=await request.get('/admin/stats');if(s){stats.value={users:s.totalUsers||0,online:s.activeUsers||0,exercises:s.totalConversations||0,projects:s.todayConversations||0};chartData.adminCount=s.adminCount||0;chartData.studentCount=s.studentCount||0}}catch{}
  try{const[resR,resC,resT]=await Promise.allSettled([request.get('/admin/resources',{params:{page:1,pageSize:500}}),request.get('/admin/conversations',{params:{page:1,pageSize:500}}),request.get('/dashboard/tasks')]);if(resR.status==='fulfilled')chartData.resources=(resR.value as any)?.records||[];if(resC.status==='fulfilled')chartData.convs=(resC.value as any)?.records||[];if(resT.status==='fulfilled')chartData.tasks=(resT.value as any)||[]}catch{}
  nextTick(initCharts)
})
</script>
<style scoped>
.statistics-page{padding:20px;background:var(--surface);min-height:100vh}
.stats-row{margin-bottom:20px}
.stat-card{border-radius:var(--radius-lg);transition:transform .3s}
.stat-card:hover{transform:translateY(-4px);box-shadow:0 6px 16px rgba(0,0,0,.1)}
.stat-content{display:flex;align-items:center;gap:16px;padding:16px 0}
.stat-icon{width:52px;height:52px;border-radius:var(--radius-lg);display:flex;align-items:center;justify-content:center;font-size:24px;color:#fff}
.stat-icon.users{background:linear-gradient(135deg,#667eea,#764ba2)}
.stat-icon.online{background:linear-gradient(135deg,#f093fb,#f5576c)}
.stat-icon.exercises{background:linear-gradient(135deg,#4facfe,#00f2fe)}
.stat-icon.projects{background:linear-gradient(135deg,#43e97b,#38f9d7)}
.stat-info{flex:1}
.stat-value{font:600 26px/1.2 var(--font-sans);color:var(--ink)}
.stat-label{font:var(--text-sm);color:var(--steel);margin-top:4px}
.charts-row{margin-bottom:20px}
.chart-card{border-radius:var(--radius-lg);min-height:380px}
.chart-container{width:100%;height:300px}
</style>
