<template>
  <div class="resource-page">
    <main class="main-area">
      <div class="top-bar">
        <span class="back-link" @click="$router.push('/student/resources')">← 返回章节列表</span>
        <h2>{{ currentChapter?.label }} · {{ currentChapter?.title }}</h2>
      </div>
      <p class="chapter-desc">{{ currentChapter?.desc }}</p>
      
      <div class="content-box">
        <p class="hint">👈 点击右侧资源卡片开始学习</p>
      </div>
    </main>

    <aside class="side-cards">
      <div class="side-title">本章资源</div>
      <div v-for="card in resourceCards" :key="card.key" class="mini-card" @click="goResource(card.key)">
        <div class="mini-icon">{{ card.icon }}</div>
        <div class="mini-label">{{ card.label }}</div>
      </div>
    </aside>
  </div>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'

const route = useRoute()
const router = useRouter()

const chapters = [
  { id: 1, label: '第1章', title: 'Java 基础语法', desc: '变量、数据类型、运算符、流程控制、数组与方法' },
  { id: 2, label: '第2章', title: '面向对象', desc: '类与对象、封装、继承、多态、抽象类与接口' },
  { id: 3, label: '第3章', title: '集合框架', desc: 'List、Map、Set 接口及其常用实现类详解' },
]

const resourceCards = [
  { key: 'mindmap', label: '思维导图', icon: '🧠' },
  { key: 'quiz', label: '练习题目', icon: '📝' },
  { key: 'reading', label: '拓展阅读', icon: '📖' },
  { key: 'code', label: '代码案例', icon: '💻' },
]

const chapterId = computed(() => Number(route.params.chapterId) || 1)
const currentChapter = computed(() => chapters.find(c => c.id === chapterId.value))

const goResource = (type: string) => router.push(`/student/resources/${chapterId.value}/${type}`)
</script>

<style scoped>
.resource-page {
  display: flex; gap: 28px; padding: 32px 40px;
  max-width: 1200px; margin: 0 auto;
  min-height: calc(100vh - 60px); background: #fff;
}
.main-area { flex: 1; }
.top-bar { display: flex; align-items: center; gap: 16px; margin-bottom: 8px; }
.top-bar h2 { margin: 0; font-size: 22px; color: #1a1a1a; font-weight: 700; }
.back-link { color: #4f8cff; cursor: pointer; font-size: 13px; }
.chapter-desc { color: #999; margin: 0 0 28px; font-size: 14px; }
.content-box {
  background: #f8f9fb; border-radius: 14px;
  padding: 80px; text-align: center; border: 1px dashed #dde;
}
.hint { color: #aaa; font-size: 15px; }

.side-cards {
  width: 140px; display: flex; flex-direction: column; gap: 14px;
  padding-top: 80px;
}
.side-title {
  font-size: 13px; color: #aaa; text-align: center;
  text-transform: uppercase; letter-spacing: 1px; margin-bottom: 4px;
}
.mini-card {
  width: 120px; height: 110px; border-radius: 16px;
  background: #fff; border: 1px solid #eee;
  display: flex; flex-direction: column; align-items: center;
  justify-content: center; cursor: pointer; transition: all .25s;
  gap: 10px; box-shadow: 0 1px 3px rgba(0,0,0,.04);
}
.mini-card:hover {
  border-color: #4f8cff;
  transform: translateY(-3px);
  box-shadow: 0 8px 24px rgba(79,140,255,.15);
}
.mini-icon { font-size: 34px; }
.mini-label { font-size: 13px; color: #555; font-weight: 500; }
</style>
