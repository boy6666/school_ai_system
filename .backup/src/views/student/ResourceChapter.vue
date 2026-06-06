<template>
  <div class="note-page">
    <div class="top-bar">
      <span class="back-link" @click="$router.back()">返回</span>
      <h2>{{ note?.title }}</h2>
    </div>

    <div v-if="loading" class="loading-hint">加载中...</div>
    <div v-else class="note-content" v-html="renderedContent"></div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted, onBeforeUnmount } from 'vue'
import { useRoute } from 'vue-router'
import { getNoteDetail, type NoteDetail } from '@/api/notes'
import { logStudy } from '@/api/learning'
import { renderMarkdown } from '@/utils/markdown'

const route = useRoute()
const loading = ref(true)
const note = ref<NoteDetail | null>(null)

const renderedContent = computed(() => {
  if (!note.value?.content) return ''
  return renderMarkdown(note.value.content)
})

const startTime = Date.now()

onMounted(async () => {
  const id = Number(route.params.id)
  if (id) {
    try {
      const data = await getNoteDetail(id)
      note.value = data
    } catch { note.value = null }
  }
  loading.value = false
})

// 页面离开时上传学习时长
const uploadStudyLog = () => {
  const elapsed = Math.round((Date.now() - startTime) / 1000)
  if (elapsed > 0) {
    try {
      logStudy({
        module: 'reading',
        durationSec: elapsed,
        noteId: Number(route.params.id),
      })
    } catch {}
  }
}

onBeforeUnmount(uploadStudyLog)
window.addEventListener('beforeunload', uploadStudyLog)

</script>

<style scoped>
.note-page { padding: 32px 40px; max-width: 114px; margin: 0 auto; background: #fff; min-height: 100vh; }
.top-bar { margin-bottom: 24px; }
.top-bar h2 { margin: 0 0 4px; font-size: 24px; font-weight: 700; }
.back-link { color: var(--accent); cursor: pointer; font-size: 12px; }
.loading-hint { text-align: center; padding: 64px; color: var(--text); }
.note-content { line-height: 1.8; color: var(--text-h); font-size: 16px; }
.note-content :deep(h2) { margin: 24px 0 12px; font-size: 16px; color: var(--text-h); }
.note-content :deep(h3) { margin: 16px 0 8px; font-size: 16px; color: var(--text-h); }
.note-content :deep(h4) { margin: 16px 0 8px; font-size: 16px; color: var(--text); }
.note-content :deep(p) { margin: 0 0 12px; }
.note-content :deep(li) { margin-left: 16px; }
.note-content :deep(code) { background: var(--surface); padding: 0px 4px; border-radius: 4px; font-size: 12px; }
.note-content :deep(pre) { background: var(--surface); padding: 16px; border-radius: 8px; overflow-x: auto; margin: 12px 0; }
.note-content :deep(pre code) { background: transparent; padding: 0; }
.note-content :deep(strong) { color: var(--text-h); }
</style>
