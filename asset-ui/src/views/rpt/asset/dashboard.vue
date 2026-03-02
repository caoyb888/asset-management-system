<template>
  <div class="rpt-dashboard">
    <!-- 筛选栏 -->
    <el-card class="filter-card" shadow="never">
      <el-form inline :model="filterForm">
        <el-form-item label="项目">
          <el-select
            v-model="filterForm.projectId"
            placeholder="全部项目"
            clearable
            style="width: 200px"
          >
            <el-option
              v-for="p in projectList"
              :key="p.id"
              :label="p.projectName"
              :value="p.id"
            />
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" :loading="loading" @click="loadData">
            查询
          </el-button>
          <el-button @click="resetFilter">重置</el-button>
        </el-form-item>
        <span v-if="dashboard.latestDate" class="latest-date-tip">
          <el-icon><Clock /></el-icon>
          数据更新至：<strong>{{ dashboard.latestDate }}</strong>
        </span>
      </el-form>
    </el-card>

    <!-- KPI 卡片行 -->
    <el-row :gutter="16" class="kpi-row">
      <el-col :span="8">
        <el-card class="kpi-card kpi-vacancy" shadow="never">
          <div class="kpi-title">空置率</div>
          <div class="kpi-value">{{ fmtRate(dashboard.vacancyRate) }}</div>
          <div class="kpi-changes">
            <span class="change-item" :class="trendClass(dashboard.vacancyRateYoY, true)">
              <el-icon><component :is="trendIcon(dashboard.vacancyRateYoY, true)" /></el-icon>
              同比 {{ fmtDelta(dashboard.vacancyRateYoY) }}
            </span>
            <span class="change-item" :class="trendClass(dashboard.vacancyRateMoM, true)">
              <el-icon><component :is="trendIcon(dashboard.vacancyRateMoM, true)" /></el-icon>
              环比 {{ fmtDelta(dashboard.vacancyRateMoM) }}
            </span>
          </div>
        </el-card>
      </el-col>
      <el-col :span="8">
        <el-card class="kpi-card kpi-rental" shadow="never">
          <div class="kpi-title">出租率</div>
          <div class="kpi-value">{{ fmtRate(dashboard.rentalRate) }}</div>
          <div class="kpi-changes">
            <span class="change-item" :class="trendClass(dashboard.rentalRateYoY, false)">
              <el-icon><component :is="trendIcon(dashboard.rentalRateYoY, false)" /></el-icon>
              同比 {{ fmtDelta(dashboard.rentalRateYoY) }}
            </span>
            <span class="change-item" :class="trendClass(dashboard.rentalRateMoM, false)">
              <el-icon><component :is="trendIcon(dashboard.rentalRateMoM, false)" /></el-icon>
              环比 {{ fmtDelta(dashboard.rentalRateMoM) }}
            </span>
          </div>
        </el-card>
      </el-col>
      <el-col :span="8">
        <el-card class="kpi-card kpi-opening" shadow="never">
          <div class="kpi-title">开业率</div>
          <div class="kpi-value">{{ fmtRate(dashboard.openingRate) }}</div>
          <div class="kpi-changes">
            <span class="change-item" :class="trendClass(dashboard.openingRateYoY, false)">
              <el-icon><component :is="trendIcon(dashboard.openingRateYoY, false)" /></el-icon>
              同比 {{ fmtDelta(dashboard.openingRateYoY) }}
            </span>
            <span class="change-item" :class="trendClass(dashboard.openingRateMoM, false)">
              <el-icon><component :is="trendIcon(dashboard.openingRateMoM, false)" /></el-icon>
              环比 {{ fmtDelta(dashboard.openingRateMoM) }}
            </span>
          </div>
        </el-card>
      </el-col>
    </el-row>

    <!-- 统计数字行 -->
    <el-row :gutter="16" class="stats-row">
      <el-col :span="4" v-for="stat in statCards" :key="stat.label">
        <el-card class="stat-card" shadow="never">
          <div class="stat-label">{{ stat.label }}</div>
          <div class="stat-value" :style="{ color: stat.color }">{{ stat.value }}</div>
          <div class="stat-unit">{{ stat.unit }}</div>
        </el-card>
      </el-col>
    </el-row>

    <!-- 图表行 -->
    <el-row :gutter="16" class="chart-row">
      <el-col :span="16">
        <el-card shadow="never">
          <template #header>
            <span>三率趋势（近30天）</span>
          </template>
          <div ref="trendChartRef" class="chart-container" />
        </el-card>
      </el-col>
      <el-col :span="8">
        <el-card shadow="never">
          <template #header>
            <span>项目对比</span>
          </template>
          <div ref="compareChartRef" class="chart-container" />
        </el-card>
      </el-col>
    </el-row>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, computed, onMounted, onUnmounted, nextTick, watch } from 'vue'
import * as echarts from 'echarts'
import { Clock, Top, Bottom, Minus } from '@element-plus/icons-vue'
import { getDashboard } from '@/api/rpt/asset'
import { getProjectList } from '@/api/base/project'
import type { AssetDashboardVO } from '@/api/rpt/asset'

const loading = ref(false)
const dashboard = ref<AssetDashboardVO>({})
const projectList = ref<{ id: number; projectName: string; projectCode: string }[]>([])
const filterForm = reactive({ projectId: null as number | null })

// 统计卡片
const statCards = computed(() => [
  { label: '商铺总数', value: dashboard.value.totalShops ?? '-', unit: '间', color: '#303133' },
  { label: '已租商铺', value: dashboard.value.rentedShops ?? '-', unit: '间', color: '#409eff' },
  { label: '空置商铺', value: dashboard.value.vacantShops ?? '-', unit: '间', color: '#f56c6c' },
  { label: '装修中', value: dashboard.value.decoratingShops ?? '-', unit: '间', color: '#e6a23c' },
  { label: '已开业', value: dashboard.value.openedShops ?? '-', unit: '间', color: '#67c23a' },
  { label: '总面积', value: fmtArea(dashboard.value.totalArea), unit: '㎡', color: '#303133' },
])

// 项目名称映射
const projectNameMap = computed(() => {
  const map: Record<number, string> = {}
  projectList.value.forEach(p => { map[p.id] = p.projectName })
  return map
})

// ECharts 实例
const trendChartRef = ref<HTMLDivElement>()
const compareChartRef = ref<HTMLDivElement>()
let trendChart: echarts.ECharts | null = null
let compareChart: echarts.ECharts | null = null

onMounted(async () => {
  await loadProjects()
  await loadData()
  initCharts()
  window.addEventListener('resize', handleResize)
})

onUnmounted(() => {
  trendChart?.dispose()
  compareChart?.dispose()
  window.removeEventListener('resize', handleResize)
})

function handleResize() {
  trendChart?.resize()
  compareChart?.resize()
}

async function loadProjects() {
  projectList.value = await getProjectList()
}

async function loadData() {
  loading.value = true
  try {
    const params = filterForm.projectId ? { projectId: filterForm.projectId } : {}
    dashboard.value = await getDashboard(params)
    await nextTick()
    updateTrendChart()
    updateCompareChart()
  } finally {
    loading.value = false
  }
}

function resetFilter() {
  filterForm.projectId = null
  loadData()
}

function initCharts() {
  if (trendChartRef.value) {
    trendChart = echarts.init(trendChartRef.value)
  }
  if (compareChartRef.value) {
    compareChart = echarts.init(compareChartRef.value)
  }
}

function updateTrendChart() {
  if (!trendChart) {
    if (trendChartRef.value) trendChart = echarts.init(trendChartRef.value)
    else return
  }
  const vacancyData = dashboard.value.vacancyTrend ?? []
  const rentalData = dashboard.value.rentalTrend ?? []
  const openingData = dashboard.value.openingTrend ?? []
  const dates = vacancyData.map(d => d.timeDim)

  trendChart.setOption({
    tooltip: { trigger: 'axis', valueFormatter: (v: number) => `${v?.toFixed(2)}%` },
    legend: { data: ['空置率', '出租率', '开业率'], top: 0 },
    grid: { left: 40, right: 20, top: 40, bottom: 30 },
    xAxis: { type: 'category', data: dates, axisLabel: { rotate: 30 } },
    yAxis: { type: 'value', axisLabel: { formatter: '{value}%' } },
    series: [
      {
        name: '空置率',
        type: 'line',
        smooth: true,
        data: vacancyData.map(d => d.value),
        itemStyle: { color: '#f56c6c' },
        areaStyle: { color: 'rgba(245,108,108,0.1)' },
      },
      {
        name: '出租率',
        type: 'line',
        smooth: true,
        data: rentalData.map(d => d.value),
        itemStyle: { color: '#409eff' },
        areaStyle: { color: 'rgba(64,158,255,0.1)' },
      },
      {
        name: '开业率',
        type: 'line',
        smooth: true,
        data: openingData.map(d => d.value),
        itemStyle: { color: '#67c23a' },
        areaStyle: { color: 'rgba(103,194,58,0.1)' },
      },
    ],
  })
}

function updateCompareChart() {
  if (!compareChart) {
    if (compareChartRef.value) compareChart = echarts.init(compareChartRef.value)
    else return
  }
  const comparisons = dashboard.value.projectComparison ?? []
  const names = comparisons.map(c => projectNameMap.value[c.projectId] || `项目${c.projectId}`)

  compareChart.setOption({
    tooltip: { trigger: 'axis', valueFormatter: (v: number) => `${v?.toFixed(2)}%` },
    legend: { data: ['空置率', '出租率', '开业率'], top: 0 },
    grid: { left: 50, right: 10, top: 40, bottom: 60 },
    xAxis: { type: 'category', data: names, axisLabel: { rotate: 30, width: 60, overflow: 'truncate' } },
    yAxis: { type: 'value', axisLabel: { formatter: '{value}%' } },
    series: [
      {
        name: '空置率',
        type: 'bar',
        data: comparisons.map(c => c.vacancyRate),
        itemStyle: { color: '#f56c6c' },
      },
      {
        name: '出租率',
        type: 'bar',
        data: comparisons.map(c => c.rentalRate),
        itemStyle: { color: '#409eff' },
      },
      {
        name: '开业率',
        type: 'bar',
        data: comparisons.map(c => c.openingRate),
        itemStyle: { color: '#67c23a' },
      },
    ],
  })
}

// ─── 格式化工具 ───
function fmtRate(v?: number | null) {
  return v != null ? `${v.toFixed(2)}%` : '-'
}

function fmtDelta(v?: number | null) {
  if (v == null) return '-'
  return `${v > 0 ? '+' : ''}${v.toFixed(2)}%`
}

function fmtArea(v?: number | null) {
  if (v == null) return '-'
  return v >= 10000 ? (v / 10000).toFixed(2) + 'w' : v.toFixed(0)
}

/** reverseGood: 空置率上升是坏事 */
function trendClass(v?: number | null, reverseGood = false) {
  if (v == null) return 'change-neutral'
  const up = v > 0
  const good = reverseGood ? !up : up
  return good ? 'change-up' : (v < 0 ? 'change-down' : 'change-neutral')
}

function trendIcon(v?: number | null, reverseGood = false) {
  if (v == null || v === 0) return Minus
  return v > 0 ? Top : Bottom
}
</script>

<style scoped lang="scss">
.rpt-dashboard {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.filter-card {
  .latest-date-tip {
    margin-left: 16px;
    font-size: 13px;
    color: #909399;
    display: inline-flex;
    align-items: center;
    gap: 4px;
  }
}

.kpi-row,
.stats-row,
.chart-row {
  margin: 0 !important;
}

.kpi-card {
  text-align: center;
  padding: 8px 0;
  border-top: 4px solid;

  .kpi-title {
    font-size: 14px;
    color: #606266;
    margin-bottom: 8px;
  }

  .kpi-value {
    font-size: 36px;
    font-weight: 700;
    margin-bottom: 12px;
  }

  .kpi-changes {
    display: flex;
    justify-content: center;
    gap: 20px;
    font-size: 12px;
  }

  &.kpi-vacancy {
    border-color: #f56c6c;
    .kpi-value { color: #f56c6c; }
  }
  &.kpi-rental {
    border-color: #409eff;
    .kpi-value { color: #409eff; }
  }
  &.kpi-opening {
    border-color: #67c23a;
    .kpi-value { color: #67c23a; }
  }
}

.change-item {
  display: inline-flex;
  align-items: center;
  gap: 2px;
  &.change-up { color: #f56c6c; }
  &.change-down { color: #67c23a; }
  &.change-neutral { color: #909399; }
}

.stat-card {
  text-align: center;
  .stat-label { font-size: 12px; color: #909399; margin-bottom: 6px; }
  .stat-value { font-size: 24px; font-weight: 700; }
  .stat-unit { font-size: 12px; color: #c0c4cc; margin-top: 2px; }
}

.chart-container {
  height: 320px;
  width: 100%;
}
</style>
