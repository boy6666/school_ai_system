<template>
  <div class="resource-page">
    <main class="main-area">
      <div class="page-title">
        <h2>📚 Java 程序设计</h2>
        <p>选择章节，点击右侧卡片开始学习</p>
      </div>
      <div class="chapter-list">
        <div v-for="ch in chapters" :key="ch.id" class="chapter-card" @click="goChapter(ch.id)">
          <div class="ch-left">
            <div class="ch-num">{{ ch.id }}</div>
          </div>
          <div class="ch-body">
            <h3>{{ ch.label }} · {{ ch.title }}</h3>
            <p>{{ ch.desc }}</p>
          </div>
          <div class="ch-arrow">→</div>
        </div>
      </div>
    </main>

    <aside class="side-cards">
      <div class="side-title">学习资源</div>
      <div v-for="card in resourceCards" :key="card.key" class="mini-card" @click="goResource(card.key)">
        <div class="mini-icon">{{ card.icon }}</div>
        <div class="mini-label">{{ card.label }}</div>
      </div>
    </aside>
  </div>
</template>

<script setup lang="ts">
import { useRouter } from 'vue-router'

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

const goChapter = (id: number) => router.push(`/student/resources/${id}`)
const goResource = (type: string) => router.push(`/student/resources/generate/${type}`)
</script>

<style scoped>
.resource-page {
  display: flex; gap: 28px; padding: 32px 40px;
  max-width: 1200px; margin: 0 auto;
  min-height: calc(100vh - 60px); background: #fff;
}

.main-area { flex: 1; }
.page-title { margin-bottom: 28px; }
.page-title h2 { font-size: 24px; margin: 0; color: #1a1a1a; font-weight: 700; }
.page-title p { color: #999; margin: 6px 0 0; font-size: 14px; }

.chapter-list { display: flex; flex-direction: column; gap: 14px; }
.chapter-card {
  display: flex; align-items: center; gap: 18px;
  padding: 22px 28px; background: #fff;
  border-radius: 14px; border: 1px solid #eee;
  cursor: pointer; transition: all .25s;
  box-shadow: 0 1px 3px rgba(0,0,0,.04);
}
.chapter-card:hover {
  border-color: #4f8cff;
  box-shadow: 0 6px 20px rgba(79,140,255,.12);
  transform: translateX(4px);
}
.ch-left { flex-shrink: 0; }
.ch-num {
  width: 52px; height: 52px; border-radius: 14px;
  background: linear-gradient(135deg, #4f8cff, #6ba3ff);
  color: #fff; font-size: 20px; font-weight: 700;
  display: flex; align-items: center; justify-content: center;
}
.ch-body { flex: 1; }
.ch-body h3 { margin: 0; font-size: 17px; color: #1a1a1a; font-weight: 600; }
.ch-body p { margin: 6px 0 0; font-size: 13px; color: #999; line-height: 1.5; }
.ch-arrow { color: #ccc; font-size: 22px; font-weight: 300; }

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
