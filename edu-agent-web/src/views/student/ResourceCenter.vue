<template>
  <div class="resource-page">
    <main class="main-area">
      <div class="page-title" v-if="!curCat">
        <h2>📚 Java 程序设计</h2>
        <p>选择章节，点击右侧卡片开始学习</p>
      </div>

      <!-- 章节列表 -->
      <div class="chapter-list" v-if="!curCat">
        <div v-for="cat in cats" :key="cat.category" class="chapter-card" @click="enterCat(cat)">
          <div class="ch-left">
            <div class="ch-num">{{ cats.indexOf(cat) + 1 }}</div>
          </div>
          <div class="ch-body">
            <h3>{{ cat.label }}</h3>
            <p>{{ cat.count }} 个小节</p>
          </div>
          <div class="ch-arrow">→</div>
        </div>
      </div>

      <!-- 小节列表 -->
      <div class="chapter-list" v-if="curCat && !curNote">
        <div class="back" @click="curCat=null">← 返回</div>
        <div class="page-title">
          <h2>{{ curCat.label }}</h2>
        </div>
        <div v-for="n in notes" :key="n.id" class="chapter-card" @click="enterNote(n)">
          <div class="ch-left" style="background:#5b8def">
            <span>📄</span>
          </div>
          <div class="ch-body">
            <h3>{{ n.title }}</h3>
          </div>
          <div class="ch-arrow">→</div>
        </div>
      </div>

      <!-- 文章内容 -->
      <div v-if="curNote" class="content-area">
        <div class="back" @click="curNote=null">← 小节</div>
        <h2>{{ curNote.title }}</h2>
        <div class="md" v-html="html"></div>
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
import { ref, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { marked } from 'marked'
import { markedHighlight } from 'marked-highlight'
import hljs from 'highlight.js'
import 'highlight.js/styles/atom-one-dark.css'
import { getCategories, getNotes, getNoteDetail } from '@/api/notes'

marked.use(markedHighlight({
  langPrefix: 'hljs language-',
  highlight(code: string, lang: string) {
    if (lang && hljs.getLanguage(lang)) {
      return hljs.highlight(code, { language: lang }).value
    }
    return hljs.highlightAuto(code).value
  }
}))
marked.setOptions({ breaks: true, gfm: true })

const router = useRouter()

const cats = ref<any[]>([])
const curCat = ref<any>(null)
const notes = ref<any[]>([])
const curNote = ref<any>(null)

const resourceCards = [
  { key: 'mindmap', label: '思维导图', icon: '🧠' },
  { key: 'quiz', label: '练习题目', icon: '📝' },
  { key: 'reading', label: '拓展阅读', icon: '📖' },
  { key: 'code', label: '代码案例', icon: '💻' },
]

const html = computed(() => curNote.value?.content ? marked.parse(curNote.value.content) : '')

const enterCat = async (cat: any) => {
  curCat.value = cat
  try {
    const n = await getNotes(cat.category)
    notes.value = Array.isArray(n) ? n : []
  } catch { notes.value = [] }
}

const enterNote = async (item: any) => {
  curNote.value = item
  if (!item.content && item.id) {
    try {
      const d = await getNoteDetail(item.id)
      if (d) curNote.value = d
    } catch {}
  }
}

const goResource = (key: string) => {
  // 如果已选中章节，把 category 作为 chapterId 传过去，让生成页优先查DB缓存
  const chapterId = curCat.value?.category
  const query = chapterId ? `?chapterId=${chapterId}` : ''
  router.push(`/student/resources/generate/${key}${query}`)
}

onMounted(async () => {
  try {
    const c = await getCategories()
    if (Array.isArray(c)) cats.value = c
  } catch {}
})
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
.chapter-card:hover { border-color: #4f8cff; transform: translateX(4px); }
.ch-left {
  width: 44px; height: 44px; border-radius: 12px;
  background: #4f8cff; color: #fff;
  display: flex; align-items: center; justify-content: center;
  font-weight: 700; font-size: 15px; flex-shrink: 0;
}
.ch-body { flex: 1; }
.ch-body h3 { margin: 0; font-size: 16px; color: #1a1a1a; }
.ch-body p { margin: 4px 0 0; font-size: 13px; color: #999; }
.ch-arrow { font-size: 18px; color: #ccc; }
.back { padding: 8px 0; cursor: pointer; color: #4f8cff; font-size: 14px; margin-bottom: 8px; }
.back:hover { text-decoration: underline; }
.content-area { background: #fff; border-radius: 12px; padding: 32px 40px; border: 1px solid #ebeef5; margin-top: 12px; }
.content-area h2 { font-size: 26px; margin: 0 0 8px; color: #1a1a1a; font-weight: 700; }

/* Markdown 渲染优化 */
.md { line-height: 1.9; font-size: 15px; color: #333; padding: 4px 0; }
.md :deep(h1) { font-size: 24px; margin: 28px 0 14px; font-weight: 700; color: #1a1a1a; padding-bottom: 8px; border-bottom: 2px solid #409eff; }
.md :deep(h2) { font-size: 20px; margin: 24px 0 12px; font-weight: 600; color: #1a1a1a; }
.md :deep(h3) { font-size: 17px; margin: 20px 0 10px; font-weight: 600; color: #2c3e50; }
.md :deep(h4) { font-size: 15px; margin: 16px 0 8px; font-weight: 600; color: #2c3e50; }
.md :deep(p) { margin: 0 0 14px; }
.md :deep(ul), .md :deep(ol) { margin: 8px 0 14px; padding-left: 24px; }
.md :deep(li) { margin: 4px 0; }
.md :deep(blockquote) {
  margin: 14px 0; padding: 12px 18px; border-left: 4px solid #409eff;
  background: #f8faff; color: #555; border-radius: 0 8px 8px 0;
}
.md :deep(code) {
  font-family: 'JetBrains Mono', 'Fira Code', monospace;
  background: #f0f2f5; padding: 2px 7px; border-radius: 4px;
  font-size: 13px; color: #d63384;
}
.md :deep(pre) {
  background: #1e1e2e; color: #cdd6f4; padding: 18px;
  border-radius: 10px; overflow-x: auto; margin: 16px 0;
  font-size: 13px; line-height: 1.7; border: 1px solid #2d2d3d;
}
.md :deep(pre code) { background: none; padding: 0; color: inherit; font-size: inherit; }
.md :deep(a) { color: #409eff; text-decoration: none; }
.md :deep(a:hover) { text-decoration: underline; }
.md :deep(img) { max-width: 100%; border-radius: 8px; margin: 14px 0; box-shadow: 0 2px 8px rgba(0,0,0,.08); }
.md :deep(hr) { border: none; border-top: 1px solid #e4e7ed; margin: 28px 0; }
.md :deep(strong) { color: #1a1a1a; }
.md :deep(table) {
  width: 100%; border-collapse: collapse; margin: 14px 0; font-size: 14px;
}
.md :deep(th), .md :deep(td) {
  border: 1px solid #e4e7ed; padding: 10px 14px; text-align: left;
}
.md :deep(th) { background: #f5f7fa; font-weight: 600; color: #1a1a1a; }
.md :deep(tr:nth-child(even)) { background: #fafafa; }
.side-cards { width: 150px; flex-shrink: 0; position: sticky; top: 32px; align-self: flex-start; }
.side-title { font-size: 14px; font-weight: 600; color: #1a1a1a; margin-bottom: 14px; }
.mini-card {
  display: flex; align-items: center; gap: 10px;
  padding: 14px 16px; border-radius: 12px; border: 1px solid #eee;
  cursor: pointer; transition: all .2s; margin-bottom: 8px;
}
.mini-card:hover { border-color: #4f8cff; background: #f5f8ff; }
.mini-icon { font-size: 22px; }
.mini-label { font-size: 13px; font-weight: 500; color: #333; }
</style>
