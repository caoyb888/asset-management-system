<template>
  <div
    ref="containerRef"
    class="chart-container"
    :style="{ height: height, width: '100%' }"
    v-loading="loading"
  />
</template>

<script setup lang="ts">
import { ref, watch, onMounted, onUnmounted, nextTick } from 'vue'
import * as echarts from 'echarts'

const props = withDefaults(
  defineProps<{
    /** ECharts 配置项，响应式，变更时自动 setOption */
    // eslint-disable-next-line @typescript-eslint/no-explicit-any
    option: Record<string, any> | null
    /** 容器高度，默认 300px */
    height?: string
    /** 是否显示加载遮罩 */
    loading?: boolean
    /** ECharts 主题名称，默认不使用主题 */
    theme?: string | null
    /** setOption 时是否合并（notMerge=false 则合并，true 则替换）*/
    notMerge?: boolean
  }>(),
  {
    height: '300px',
    loading: false,
    theme: null,
    notMerge: false,
  },
)

const emit = defineEmits<{
  /** 图表初始化完成后触发，携带 ECharts 实例 */
  ready: [chart: echarts.ECharts]
  /** ECharts click 事件透传 */
  click: [params: unknown]
}>()

const containerRef = ref<HTMLDivElement>()
let chart: echarts.ECharts | null = null

// ─────────────────── Lifecycle ───────────────────

onMounted(async () => {
  await nextTick()
  initChart()
  window.addEventListener('resize', handleResize)
})

onUnmounted(() => {
  chart?.dispose()
  chart = null
  window.removeEventListener('resize', handleResize)
})

// ─────────────────── Watch ───────────────────

watch(
  () => props.option,
  (newOption) => {
    if (!chart) {
      initChart()
      return
    }
    if (newOption) {
      chart.setOption(newOption, { notMerge: props.notMerge, lazyUpdate: true })
    } else {
      chart.clear()
    }
  },
  { deep: false },
)

watch(
  () => props.theme,
  () => {
    // 主题切换需要重新初始化
    chart?.dispose()
    chart = null
    initChart()
  },
)

// ─────────────────── Methods ───────────────────

function initChart() {
  if (!containerRef.value) return
  chart = echarts.init(containerRef.value, props.theme ?? undefined)
  chart.on('click', (params) => emit('click', params))
  if (props.option) {
    chart.setOption(props.option, { notMerge: props.notMerge })
  }
  emit('ready', chart)
}

function handleResize() {
  chart?.resize()
}

// ─────────────────── Expose ───────────────────

/** 暴露 ECharts 实例供父组件使用（如手动 setOption、getDataURL） */
defineExpose({
  getChart: () => chart,
  resize: () => chart?.resize(),
  clear: () => chart?.clear(),
})
</script>

<style scoped>
.chart-container {
  position: relative;
}
</style>
