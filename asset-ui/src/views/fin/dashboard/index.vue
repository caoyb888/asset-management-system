<template>
  <div class="fin-dashboard">
    <!-- 顶部四卡片 -->
    <el-row :gutter="16" class="mb-4">
      <el-col :span="6">
        <el-card shadow="hover" class="stat-card">
          <div class="stat-icon blue"><el-icon size="28"><Document /></el-icon></div>
          <div class="stat-content">
            <div class="stat-label">本月应收（元）</div>
            <div class="stat-value">{{ formatAmount(summary?.monthReceivable) }}</div>
          </div>
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card shadow="hover" class="stat-card">
          <div class="stat-icon green"><el-icon size="28"><Wallet /></el-icon></div>
          <div class="stat-content">
            <div class="stat-label">本月已收（元）</div>
            <div class="stat-value text-green">{{ formatAmount(summary?.monthReceived) }}</div>
          </div>
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card shadow="hover" class="stat-card">
          <div class="stat-icon red"><el-icon size="28"><Warning /></el-icon></div>
          <div class="stat-content">
            <div class="stat-label">当前欠费（元）</div>
            <div class="stat-value text-red">{{ formatAmount(summary?.currentOverdue) }}</div>
          </div>
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card shadow="hover" class="stat-card">
          <div class="stat-icon orange"><el-icon size="28"><CircleCheck /></el-icon></div>
          <div class="stat-content">
            <div class="stat-label">本月核销（笔）</div>
            <div class="stat-value text-orange">{{ summary?.monthWriteOffCount ?? 0 }}</div>
          </div>
        </el-card>
      </el-col>
    </el-row>

    <!-- 图表区 第一行：收款趋势 + 应收费项分布 -->
    <el-row :gutter="16" class="mb-4">
      <el-col :span="16">
        <el-card>
          <template #header>
            <span class="chart-title">近12个月收款趋势</span>
          </template>
          <div ref="trendChartRef" class="chart-box" v-loading="trendLoading" />
        </el-card>
      </el-col>
      <el-col :span="8">
        <el-card>
          <template #header>
            <span class="chart-title">应收费项分布</span>
          </template>
          <div ref="feeChartRef" class="chart-box" v-loading="summaryLoading" />
        </el-card>
      </el-col>
    </el-row>

    <!-- 图表区 第二行：欠费TOP10 + 核销方式分布 -->
    <el-row :gutter="16">
      <el-col :span="16">
        <el-card>
          <template #header>
            <span class="chart-title">欠费TOP10商家</span>
          </template>
          <div ref="overdueChartRef" class="chart-box" v-loading="overdueLoading" />
        </el-card>
      </el-col>
      <el-col :span="8">
        <el-card>
          <template #header>
            <span class="chart-title">核销方式分布</span>
          </template>
          <div ref="writeOffChartRef" class="chart-box" v-loading="summaryLoading" />
        </el-card>
      </el-col>
    </el-row>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, onBeforeUnmount, nextTick } from 'vue'
import { Document, Wallet, Warning, CircleCheck } from '@element-plus/icons-vue'
import * as echarts from 'echarts/core'
import {
  LineChart,
  PieChart,
  BarChart,
} from 'echarts/charts'
import {
  TitleComponent,
  TooltipComponent,
  GridComponent,
  LegendComponent,
} from 'echarts/components'
import { CanvasRenderer } from 'echarts/renderers'
import {
  getDashboardSummary,
  getReceiptTrend,
  getOverdueTop,
  type DashboardSummaryVO,
  type ReceiptTrendVO,
  type OverdueTopVO,
} from '@/api/fin/dashboard'

echarts.use([
  LineChart, PieChart, BarChart,
  TitleComponent, TooltipComponent, GridComponent, LegendComponent,
  CanvasRenderer,
])

// ─── 状态 ────────────────────────────────────────────────────────────────────
const summaryLoading = ref(false)
const trendLoading   = ref(false)
const overdueLoading = ref(false)
const summary        = ref<DashboardSummaryVO | null>(null)

// ─── 图表 DOM 引用 ────────────────────────────────────────────────────────────
const trendChartRef   = ref<HTMLElement>()
const feeChartRef     = ref<HTMLElement>()
const overdueChartRef = ref<HTMLElement>()
const writeOffChartRef = ref<HTMLElement>()

let trendChart:    echarts.ECharts | null = null
let feeChart:      echarts.ECharts | null = null
let overdueChart:  echarts.ECharts | null = null
let writeOffChart: echarts.ECharts | null = null

// ─── 格式化 ──────────────────────────────────────────────────────────────────
function formatAmount(v?: number | null) {
  if (v == null) return '0.00'
  return Number(v).toLocaleString('zh-CN', { minimumFractionDigits: 2, maximumFractionDigits: 2 })
}

// ─── 加载汇总数据（卡片 + 两饼图）────────────────────────────────────────────
async function loadSummary() {
  summaryLoading.value = true
  try {
    const res: any = await getDashboardSummary()
    summary.value = res ?? null
    await nextTick()
    renderFeeChart(summary.value?.feeTypeDistribution ?? [])
    renderWriteOffChart(summary.value?.writeOffTypeDistribution ?? [])
  } finally {
    summaryLoading.value = false
  }
}

// ─── 加载收款趋势 ─────────────────────────────────────────────────────────────
async function loadTrend() {
  trendLoading.value = true
  try {
    const res: any = await getReceiptTrend()
    const data: ReceiptTrendVO[] = res ?? []
    await nextTick()
    renderTrendChart(data)
  } finally {
    trendLoading.value = false
  }
}

// ─── 加载欠费TOP10 ───────────────────────────────────────────────────────────
async function loadOverdueTop() {
  overdueLoading.value = true
  try {
    const res: any = await getOverdueTop()
    const data: OverdueTopVO[] = res ?? []
    await nextTick()
    renderOverdueChart(data)
  } finally {
    overdueLoading.value = false
  }
}

// ─── 收款趋势折线图 ───────────────────────────────────────────────────────────
function renderTrendChart(data: ReceiptTrendVO[]) {
  if (!trendChartRef.value) return
  if (!trendChart) trendChart = echarts.init(trendChartRef.value)
  trendChart.setOption({
    tooltip: { trigger: 'axis', valueFormatter: (v: number) => '¥' + v.toLocaleString('zh-CN') },
    grid: { left: 60, right: 20, top: 20, bottom: 40 },
    xAxis: {
      type: 'category',
      data: data.map(d => d.month),
      axisLabel: { rotate: 30, fontSize: 11 },
    },
    yAxis: {
      type: 'value',
      axisLabel: { formatter: (v: number) => (v >= 10000 ? (v / 10000).toFixed(1) + '万' : String(v)) },
    },
    series: [{
      type: 'line',
      data: data.map(d => d.amount),
      smooth: true,
      symbol: 'circle',
      symbolSize: 6,
      lineStyle: { color: '#2e75b6', width: 2 },
      itemStyle: { color: '#2e75b6' },
      areaStyle: { color: { type: 'linear', x: 0, y: 0, x2: 0, y2: 1, colorStops: [{ offset: 0, color: 'rgba(46,117,182,0.3)' }, { offset: 1, color: 'rgba(46,117,182,0)' }] } },
    }],
  })
}

// ─── 应收费项分布饼图 ─────────────────────────────────────────────────────────
function renderFeeChart(data: { name: string; value: number }[]) {
  if (!feeChartRef.value) return
  if (!feeChart) feeChart = echarts.init(feeChartRef.value)
  feeChart.setOption({
    tooltip: { trigger: 'item', formatter: '{b}: ¥{c} ({d}%)' },
    legend: { orient: 'vertical', right: 10, top: 'middle', textStyle: { fontSize: 11 } },
    series: [{
      type: 'pie',
      radius: ['40%', '70%'],
      center: ['40%', '50%'],
      data: data.length ? data : [{ name: '暂无数据', value: 1 }],
      label: { show: false },
      emphasis: { label: { show: true, fontSize: 12, fontWeight: 'bold' } },
    }],
  })
}

// ─── 欠费TOP10水平柱状图 ──────────────────────────────────────────────────────
function renderOverdueChart(data: OverdueTopVO[]) {
  if (!overdueChartRef.value) return
  if (!overdueChart) overdueChart = echarts.init(overdueChartRef.value)

  const sorted = [...data].reverse() // 从小到大排列，最大值在顶部
  overdueChart.setOption({
    tooltip: { trigger: 'axis', valueFormatter: (v: number) => '¥' + v.toLocaleString('zh-CN') },
    grid: { left: 120, right: 40, top: 20, bottom: 30 },
    xAxis: {
      type: 'value',
      axisLabel: { formatter: (v: number) => (v >= 10000 ? (v / 10000).toFixed(1) + '万' : String(v)) },
    },
    yAxis: {
      type: 'category',
      data: sorted.map(d => d.merchantName || `商家${d.merchantId}`),
      axisLabel: { fontSize: 11, width: 100, overflow: 'truncate' },
    },
    series: [{
      type: 'bar',
      data: sorted.map(d => d.overdueAmount),
      itemStyle: { color: '#f56c6c' },
      barMaxWidth: 24,
      label: {
        show: true,
        position: 'right',
        formatter: (p: any) => p.value >= 10000 ? (p.value / 10000).toFixed(1) + '万' : p.value,
        fontSize: 11,
      },
    }],
  })
}

// ─── 核销方式分布饼图 ─────────────────────────────────────────────────────────
function renderWriteOffChart(data: { name: string; value: number }[]) {
  if (!writeOffChartRef.value) return
  if (!writeOffChart) writeOffChart = echarts.init(writeOffChartRef.value)
  const colors = ['#2e75b6', '#67c23a', '#e6a23c', '#f56c6c']
  writeOffChart.setOption({
    tooltip: { trigger: 'item', formatter: '{b}: {c}笔 ({d}%)' },
    legend: { orient: 'vertical', right: 10, top: 'middle', textStyle: { fontSize: 11 } },
    series: [{
      type: 'pie',
      radius: ['40%', '70%'],
      center: ['40%', '50%'],
      color: colors,
      data: data.length ? data : [{ name: '暂无数据', value: 1 }],
      label: { show: false },
      emphasis: { label: { show: true, fontSize: 12, fontWeight: 'bold' } },
    }],
  })
}

// ─── 响应式缩放 ──────────────────────────────────────────────────────────────
function handleResize() {
  trendChart?.resize()
  feeChart?.resize()
  overdueChart?.resize()
  writeOffChart?.resize()
}

// ─── 生命周期 ─────────────────────────────────────────────────────────────────
onMounted(async () => {
  await Promise.all([loadSummary(), loadTrend(), loadOverdueTop()])
  window.addEventListener('resize', handleResize)
})

onBeforeUnmount(() => {
  window.removeEventListener('resize', handleResize)
  trendChart?.dispose()
  feeChart?.dispose()
  overdueChart?.dispose()
  writeOffChart?.dispose()
})
</script>

<style scoped>
.fin-dashboard {
  padding: 16px;
}
.mb-4 {
  margin-bottom: 16px;
}

/* ── 统计卡片 ─────────────────────────────────── */
.stat-card :deep(.el-card__body) {
  display: flex;
  align-items: center;
  gap: 16px;
  padding: 20px 24px;
}
.stat-icon {
  width: 52px;
  height: 52px;
  border-radius: 12px;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}
.stat-icon.blue   { background: rgba(46, 117, 182, 0.12); color: #2e75b6; }
.stat-icon.green  { background: rgba(103, 194, 58, 0.12);  color: #67c23a; }
.stat-icon.red    { background: rgba(245, 108, 108, 0.12); color: #f56c6c; }
.stat-icon.orange { background: rgba(230, 162, 60, 0.12);  color: #e6a23c; }

.stat-content {
  flex: 1;
  min-width: 0;
}
.stat-label {
  font-size: 13px;
  color: #909399;
  margin-bottom: 6px;
}
.stat-value {
  font-size: 22px;
  font-weight: 700;
  color: #303133;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}
.text-green  { color: #67c23a; }
.text-red    { color: #f56c6c; }
.text-orange { color: #e6a23c; }

/* ── 图表容器 ─────────────────────────────────── */
.chart-title {
  font-size: 14px;
  font-weight: 600;
  color: #303133;
}
.chart-box {
  height: 300px;
}
</style>
