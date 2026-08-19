<template>
  <div class="resource-page">
    <main class="main-area">
      <div>
        <div class="page-title">
          <h2>学习资源中心</h2>
          <p>查看思维导图、练习题、拓展阅读和代码案例</p>
        </div>

        <div v-if="loading" class="content-area">
          正在加载资源……
        </div>

        <div v-else-if="errorMessage" class="content-area">
          <p>{{ errorMessage }}</p>
          <button type="button" @click="loadResources">
            重新加载
          </button>
        </div>

        <div v-else-if="filteredResources.length === 0" class="content-area">
          暂无符合条件的学习资源
        </div>

        <div v-else class="chapter-list">
          <div
            v-for="resource in filteredResources"
            :key="resource.id"
            class="chapter-card"
            @click="openResource(resource.id)"
          >
            <div class="ch-left">
              {{ getTypeIcon(resource.type) }}
            </div>

            <div class="ch-body">
              <h3>{{ resource.title }}</h3>
              <p>
                {{ getTypeLabel(resource.type) }}
                · {{ getDifficultyLabel(resource.difficulty) }}
                <template v-if="resource.chapter">
                  · {{ resource.chapter }}
                </template>
              </p>
            </div>

            <button
              type="button"
              :disabled="favoriteLoadingId === resource.id"
              style="margin-right: 12px"
              @click.stop="toggleFavorite(resource)"
            >
              {{ resource.favorites ? '取消收藏' : '收藏' }}
            </button>

            <div class="ch-arrow">→</div>
          </div>
        </div>
      </div>

    </main>

    <aside class="side-cards">
      <div class="side-title">资源类型</div>

      <div
        v-for="card in resourceCards"
        :key="card.key"
        class="mini-card"
        :style="selectedType === card.key
          ? 'border-color:#4f8cff;background:#f5f8ff'
          : ''"
        @click="selectedType = card.key"
      >
        <div class="mini-icon">{{ card.icon }}</div>
        <div class="mini-label">{{ card.label }}</div>
      </div>
    </aside>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'

import {
  getResourceList,
  setResourceFavorite,
  type ResourceVO
} from '@/api/resource'

type ResourceFilter =
  | 'all'
  | 'mindmap'
  | 'quiz'
  | 'reading'
  | 'code'

const router = useRouter()
const resources = ref<ResourceVO[]>([])
const selectedType = ref<ResourceFilter>('all')
const loading = ref(false)
const errorMessage = ref('')
const favoriteLoadingId = ref<number | null>(null)

const resourceCards: Array<{
  key: ResourceFilter
  label: string
  icon: string
}> = [
  { key: 'all', label: '全部资源', icon: '📚' },
  { key: 'mindmap', label: '思维导图', icon: '🧠' },
  { key: 'quiz', label: '练习题目', icon: '📝' },
  { key: 'reading', label: '拓展阅读', icon: '📖' },
  { key: 'code', label: '代码案例', icon: '💻' }
]

const filteredResources = computed(() => {
  if (selectedType.value === 'all') {
    return resources.value
  }

  return resources.value.filter(
    resource => resource.type === selectedType.value
  )
})

function getTypeLabel(type: string): string {
  const labels: Record<string, string> = {
    mindmap: '思维导图',
    quiz: '练习题目',
    reading: '拓展阅读',
    code: '代码案例'
  }

  return labels[type] ?? type ?? '其他资源'
}

function getTypeIcon(type: string): string {
  const icons: Record<string, string> = {
    mindmap: '🧠',
    quiz: '📝',
    reading: '📖',
    code: '💻'
  }

  return icons[type] ?? '📚'
}

function getDifficultyLabel(difficulty: string): string {
  const labels: Record<string, string> = {
    easy: '简单',
    medium: '中等',
    hard: '困难'
  }

  return labels[difficulty] ?? difficulty ?? '未设置难度'
}

async function loadResources(): Promise<void> {
  loading.value = true
  errorMessage.value = ''

  try {
    const data = await getResourceList()
    resources.value = Array.isArray(data) ? data : []
  } catch (error) {
    console.error('加载资源列表失败：', error)
    resources.value = []
    errorMessage.value = '资源加载失败，请稍后重试'
  } finally {
    loading.value = false
  }
}

function openResource(id: number): void {
  router.push(`/student/resources/${id}`)
}

async function toggleFavorite(resource: ResourceVO): Promise<void> {
  favoriteLoadingId.value = resource.id

  const nextFavorite = !Boolean(resource.favorites)

  try {
    await setResourceFavorite(resource.id, nextFavorite)
    resource.favorites = nextFavorite ? 1 : 0
  } catch (error) {
    console.error('更新资源收藏状态失败：', error)
    errorMessage.value = '收藏状态更新失败，请稍后重试'
  } finally {
    favoriteLoadingId.value = null
  }
}

onMounted(loadResources)
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
