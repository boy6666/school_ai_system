<template>
  <div
    ref="chartContainer"
    class="base-chart"
    :style="{ height }"
  ></div>
</template>

<script setup lang="ts">
import {
  nextTick,
  onBeforeUnmount,
  onMounted,
  ref,
  watch
} from 'vue'
import * as echarts from 'echarts'
import type { EChartsOption } from 'echarts'

const props = withDefaults(
  defineProps<{
    option: EChartsOption
    height?: string
  }>(),
  {
    height: '360px'
  }
)

const chartContainer = ref<HTMLElement>()
let chart: echarts.ECharts | null = null
let resizeObserver: ResizeObserver | null = null

function renderChart() {
  if (!chartContainer.value) return

  if (!chart) {
    chart = echarts.init(chartContainer.value)
  }

  chart.setOption(props.option, true)
}

function resizeChart() {
  chart?.resize()
}

onMounted(async () => {
  await nextTick()
  renderChart()

  if (chartContainer.value) {
    resizeObserver = new ResizeObserver(resizeChart)
    resizeObserver.observe(chartContainer.value)
  }

  window.addEventListener('resize', resizeChart)
})

watch(
  () => props.option,
  async () => {
    await nextTick()
    renderChart()
  },
  {
    deep: true
  }
)

onBeforeUnmount(() => {
  window.removeEventListener('resize', resizeChart)
  resizeObserver?.disconnect()
  resizeObserver = null
  chart?.dispose()
  chart = null
})
</script>

<style scoped>
.base-chart {
  width: 100%;
  min-height: 240px;
}
</style>