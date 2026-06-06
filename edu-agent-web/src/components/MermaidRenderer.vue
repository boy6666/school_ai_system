<template>
  <div class="mermaid-wrapper">
    <div class="mermaid-toolbar" v-if="svg">
      <el-button-group size="small">
        <el-button @click="zoomIn" :disabled="zoom >= 3">➕ 放大</el-button>
        <el-button @click="zoomReset">📐 适应</el-button>
        <el-button @click="zoomOut" :disabled="zoom <= 0.3">➖ 缩小</el-button>
      </el-button-group>
      <el-button size="small" @click="downloadPng" style="margin-left:8px">⬇ 下载图片</el-button>
      <span class="zoom-label">{{ Math.round(zoom * 100) }}%</span>
    </div>

    <div
      class="mermaid-viewport"
      ref="viewportRef"
      @wheel.prevent="onWheel"
      @mousedown="onDragStart"
      @mousemove="onDragMove"
      @mouseup="onDragEnd"
      @mouseleave="onDragEnd"
    >
      <div v-if="loading" class="mermaid-loading">渲染中...</div>
      <div
        v-show="!loading && svg"
        class="mermaid-svg"
        :style="{ transform: `scale(${zoom})`, transformOrigin: 'center center', cursor: dragging ? 'grabbing' : 'grab' }"
        v-html="svg"
        ref="svgWrapper"
      ></div>
      <div v-if="!loading && !svg && !errorMsg" class="mermaid-loading">暂无内容</div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, watch, nextTick } from 'vue'

const props = defineProps<{ code: string }>()

const viewportRef = ref<HTMLElement>()
const svgWrapper = ref<HTMLElement>()
const svg = ref('')
const loading = ref(true)
const errorMsg = ref('')
const zoom = ref(1)
const dragging = ref(false)
const dragStart = ref({ x: 0, y: 0, sx: 0, sy: 0 })
const scrollPos = ref({ x: 0, y: 0 })

const render = async () => {
  if (!props.code) return
  loading.value = true
  errorMsg.value = ''
  svg.value = ''
  zoom.value = 1
  try {
    const mermaid = await import('mermaid')
    mermaid.default.initialize({
      startOnLoad: false,
      theme: 'default',
      securityLevel: 'loose',
      fontFamily: '"PingFang SC", "Microsoft YaHei", sans-serif',
    })
    const id = 'mermaid-' + Date.now()
    const { svg: result } = await mermaid.default.render(id, props.code)
    svg.value = result
  } catch (e: any) {
    errorMsg.value = String(e)
  }
  loading.value = false
  await nextTick()
  // 渲染完后自适应
  setTimeout(autoFit, 100)
}

const autoFit = () => {
  zoom.value = 1
}

const zoomIn = () => { zoom.value = Math.min(5, zoom.value + 0.3) }
const zoomOut = () => { zoom.value = Math.max(0.5, zoom.value - 0.3) }
const zoomReset = () => autoFit()

const onWheel = (e: WheelEvent) => {
  const delta = e.deltaY > 0 ? -0.1 : 0.1
  zoom.value = Math.max(0.5, Math.min(5, zoom.value + delta))
}

const onDragStart = (e: MouseEvent) => {
  if (!viewportRef.value) return
  dragging.value = true
  dragStart.value = { x: e.clientX, y: e.clientY }
  scrollPos.value = { x: viewportRef.value.scrollLeft, y: viewportRef.value.scrollTop }
}

const onDragMove = (e: MouseEvent) => {
  if (!dragging.value || !viewportRef.value) return
  const dx = e.clientX - dragStart.value.x
  const dy = e.clientY - dragStart.value.y
  viewportRef.value.scrollLeft = scrollPos.value.x - dx
  viewportRef.value.scrollTop = scrollPos.value.y - dy
}

const onDragEnd = () => { dragging.value = false }

const downloadPng = async () => {
  const sw = svgWrapper.value
  if (!sw) return
  const svgEl = sw.querySelector('svg')
  if (!svgEl) return
  const clone = svgEl.cloneNode(true) as SVGElement
  const box = svgEl.getBoundingClientRect()
  const width = box.width || 800
  const height = box.height || 600
  clone.setAttribute('width', String(width * 2))
  clone.setAttribute('height', String(height * 2))
  const serializer = new XMLSerializer()
  const svgStr = serializer.serializeToString(clone)
  const canvas = document.createElement('canvas')
  canvas.width = width * 2
  canvas.height = height * 2
  const ctx = canvas.getContext('2d')
  if (!ctx) return
  const img = new Image()
  const blob = new Blob([svgStr], { type: 'image/svg+xml;charset=utf-8' })
  const url = URL.createObjectURL(blob)
  img.onload = () => {
    ctx.fillStyle = '#fff'; ctx.fillRect(0, 0, canvas.width, canvas.height)
    ctx.drawImage(img, 0, 0, canvas.width, canvas.height)
    URL.revokeObjectURL(url)
    canvas.toBlob((b) => {
      if (!b) return
      const a = document.createElement('a')
      a.href = URL.createObjectURL(b)
      a.download = 'mindmap.png'
      a.click()
    })
  }
  img.src = url
}

watch(() => props.code, render)
onMounted(render)
</script>

<style scoped>
.mermaid-wrapper {
  display: flex;
  flex-direction: column;
  gap: 8px;
}
.mermaid-toolbar {
  display: flex;
  align-items: center;
  gap: 4px;
  flex-wrap: wrap;
}
.zoom-label {
  font-size: 12px;
  color: #909399;
  margin-left: 8px;
  min-width: 40px;
}
.mermaid-viewport {
  width: 100%;
  min-height: 116px;
  max-height: 70vh;
  overflow: auto;
  border: 0px solid #e8e8e8;
  border-radius: 12px;
  background: #fafafa;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 16px;
  position: relative;
  user-select: none;
}
.mermaid-loading {
  text-align: center;
  padding: 64px 16px;
  color: #909399;
  font-size: 16px;
}
.mermaid-svg {
  transition: transform 0.1s ease;
  display: flex;
  align-items: center;
  justify-content: center;
}
.mermaid-svg :deep(svg) {
  max-width: none !important;
  height: auto;
}
</style>
