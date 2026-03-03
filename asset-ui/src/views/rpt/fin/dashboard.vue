<template>
  <div class="fin-dashboard">
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
            <el-option v-for="p in projectList" :key="p.id" :label="p.projectName" :value="p.id" />
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" :loading="loading" @click="loadData">查询</el-button>
          <el-button @click="resetFilter">重置</el-button>
        </el-form-item>
        <span v-if="dashboard.latestMonth" class="latest-tip">
          <el-icon><Clock /></el-icon>
          数据更新至：<strong>{{ dashboard.latestMonth }}</strong>
        </span>
      </el-form>
    </el-card>

    <!-- KPI 卡片 -->
    <el-row :gutter="16" class="kpi-row">
      <el-col :span="6" v-for="kpi in kpiCards" :key="kpi.label">
        <el-card class="kpi-card" shadow="never" :style="{ borderTop: `4px solid ${kpi.color}` }">
          <div class="kpi-title">{{ kpi.label }}</div>
          <div class="kpi-value" :style="{ color: kpi.color }">{{ kpi.value }}</div>
          <div v-if="kpi.yoy != null" class="kpi-yoy" :class="kpi.yoyClass">
            <el-icon><component :is="kpi.yoyIcon" /></el-icon>
            同比 {{ fmtDelta(kpi.yoy) }}
          </div>
          <div v-if="kpi.sub" class="kpi-sub">{{ kpi.sub }}</div>
        </el-card>
      </el-col>
    </el-row>

    <!-- 趋势图：应收/已收趋势 + 收缴率趋势 -->
    <el-row :gutter="16" class="chart-row">
      <el-col :span="16">
        <el-card shadow="never">
          <template #header><span>应收/已收/欠款趋势（近12月）</span></template>
          <div ref="trendChartRef" class="chart-container" />
        </el-card>
      </el-col>
      <el-col :span="8">
        <el-card shadow="never">
          <template #header><span>收缴率趋势（近12月）</span></template>
          <div ref="rateChartRef" class="chart-container" />
        </el-card>
      </el-col>
    </el-row>

    <!-- 账龄分布 + 欠款TOP10 -->
    <el-row :gutter="16" class="chart-row">
      <el-col :span="10">
        <el-card shadow="never">
          <template #header><span>账龄分布</span></template>
          <div ref="agingChartRef" class="chart-container" />
        </el-card>
      </el-col>
      <el-col :span="14">
        <el-card shadow="never">
          <template #header>
            <div class="card-header-flex">
              <span>欠款TOP10商家</span>
            </div>
          </template>
          <el-table :data="dashboard.overdueTop10 ?? []" size="small" border>
            <el-table-column prop="merchantId" label="商家ID" width="80" />
            <el-table-column label="欠款合计（元）" align="right">
              <template #default="{ row }">
                <span class="text-down">{{ fmtMoney(row.totalOutstanding) }}</span>
              </template>
            </el-table-column>
            <el-table-column label="30天内（元）" align="right">
              <template #default="{ row }">{{ fmtMoney(row.within30) }}</template>
            </el-table-column>
            <el-table-column label="逾期（元）" align="right">
              <template #default="{ row }">
                <span class="text-down">{{ fmtMoney((row.days3160 ?? 0) + (row.days6190 ?? 0) + (row.days91180 ?? 0) + (row.days181365 ?? 0) + (row.over365 ?? 0)) }}</span>
              </template>
            </el-table-column>
          </el-table>
        </el-card>
      </el-col>
    </el-row>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, computed, onMounted, onUnmounted, nextTick } from 'vue'
import * as echarts from 'echarts'
import { Clock, Top, Bottom, Minus } from '@element-plus/icons-vue'
import { getFinDashboard } from '@/api/rpt/finance'
import { getProjectList } from '@/api/base/project'
import type { FinDashboardVO } from '@/api/rpt/finance'

const loading = ref(false)
const dashboard = ref<FinDashboardVO>({
  latestMonth: null, totalReceivable: null, totalReceived: null,
  totalOutstanding: null, totalOverdue: null, avgCollectionRate: null,
  avgOverdueRate: null, totalDepositBalance: null, totalPrepayBalance: null,
  receivableYoY: null, receivedYoY: null, collectionRateYoY: null,
  overdueRateYoY: null, financeTrend: [], agingSummary: null, overdueTop10: [],
})
const projectList = ref<{ id: number; projectName: string; projectCode: string }[]>([])
const filterForm = reactive({ projectId: null as number | null })

const kpiCards = computed(() => [
  {
    label: '应收总额（万元）',
    value: fmtWan(dashboard.value.totalReceivable),
    color: '#409eff',
    yoy: dashboard.value.receivableYoY,
    yoyClass: yoyClass(dashboard.value.receivableYoY, false),
    yoyIcon: yoyIcon(dashboard.value.receivableYoY),
    sub: null,
  },
  {
    label: '已收金额（万元）',
    value: fmtWan(dashboard.value.totalReceived),
    color: '#67c23a',
    yoy: dashboard.value.receivedYoY,
    yoyClass: yoyClass(dashboard.value.receivedYoY, false),
    yoyIcon: yoyIcon(dashboard.value.receivedYoY),
    sub: `收缴率：${fmtPct(dashboard.value.avgCollectionRate)}`,
  },
  {
    label: '欠款总额（万元）',
    value: fmtWan(dashboard.value.totalOutstanding),
    color: '#e6a23c',
    yoy: null,
    yoyClass: '',
    yoyIcon: Minus,
    sub: `逾期金额：${fmtWan(dashboard.value.totalOverdue)}万元`,
  },
  {
    label: '逾期率',
    value: fmtPct(dashboard.value.avgOverdueRate),
    color: '#f56c6c',
    yoy: dashboard.value.overdueRateYoY,
    yoyClass: yoyClass(dashboard.value.overdueRateYoY, true),
    yoyIcon: yoyIcon(dashboard.value.overdueRateYoY),
    sub: `保证金：${fmtWan(dashboard.value.totalDepositBalance)}万元`,
  },
])

const trendChartRef = ref<HTMLDivElement>()
const rateChartRef = ref<HTMLDivElement>()
const agingChartRef = ref<HTMLDivElement>()
let trendChart: echarts.ECharts | null = null
let rateChart: echarts.ECharts | null = null
let agingChart: echarts.ECharts | null = null

onMounted(async () => {
  await loadProjects()
  await loadData()
  window.addEventListener('resize', handleResize)
})
onUnmounted(() => {
  trendChart?.dispose()
  rateChart?.dispose()
  agingChart?.dispose()
  window.removeEventListener('resize', handleResize)
})
function handleResize() {
  trendChart?.resize()
  rateChart?.resize()
  agingChart?.resize()
}

async function loadProjects() {
  projectList.value = await getProjectList()
}

async function loadData() {
  loading.value = true
  try {
    const params = filterForm.projectId ? { projectId: filterForm.projectId } : {}
    dashboard.value = await getFinDashboard(params)
    await nextTick()
    initCharts()
  } finally {
    loading.value = false
  }
}

function resetFilter() {
  filterForm.projectId = null
  loadData()
}

function initCharts() {
  // 应收/已收/欠款趋势
  if (!trendChart && trendChartRef.value) trendChart = echarts.init(trendChartRef.value)
  if (trendChart) {
    const data = dashboard.value.financeTrend ?? []
    trendChart.setOption({
      tooltip: { trigger: 'axis' },
      legend: { data: ['应收（万元）', '已收（万元）', '欠款（万元）'], top: 0 },
      grid: { left: 60, right: 20, top: 40, bottom: 30 },
      xAxis: { type: 'category', data: data.map(d => d.timeDim), axisLabel: { rotate: 30 } },
      yAxis: { type: 'value', name: '万元' },
      series: [
        {
          name: '应收（万元）',
          type: 'line',
          smooth: true,
          data: data.map(d => fmtWanNum(d.receivableAmount)),
          itemStyle: { color: '#409eff' },
        },
        {
          name: '已收（万元）',
          type: 'line',
          smooth: true,
          data: data.map(d => fmtWanNum(d.receivedAmount)),
          itemStyle: { color: '#67c23a' },
          areaStyle: { color: 'rgba(103,194,58,0.1)' },
        },
        {
          name: '欠款（万元）',
          type: 'bar',
          data: data.map(d => fmtWanNum(d.outstandingAmount)),
          itemStyle: { color: '#e6a23c' },
        },
      ],
    })
  }

  // 收缴率折线图
  if (!rateChart && rateChartRef.value) rateChart = echarts.init(rateChartRef.value)
  if (rateChart) {
    const data = dashboard.value.financeTrend ?? []
    rateChart.setOption({
      tooltip: {
        trigger: 'axis',
        formatter: (params: any[]) =>
          params.map(p => `${p.marker}${p.seriesName}：${p.value != null ? Number(p.value).toFixed(2) + '%' : '-'}`).join('<br/>'),
      },
      legend: { data: ['收缴率(%)', '逾期率(%)'], top: 0 },
      grid: { left: 50, right: 20, top: 40, bottom: 30 },
      xAxis: { type: 'category', data: data.map(d => d.timeDim), axisLabel: { rotate: 30 } },
      yAxis: { type: 'value', name: '%', max: 100 },
      series: [
        {
          name: '收缴率(%)',
          type: 'line',
          smooth: true,
          data: data.map(d => d.collectionRate != null ? Number(d.collectionRate) : null),
          itemStyle: { color: '#67c23a' },
        },
        {
          name: '逾期率(%)',
          type: 'line',
          smooth: true,
          data: data.map(d => d.overdueRate != null ? Number(d.overdueRate) : null),
          itemStyle: { color: '#f56c6c' },
        },
      ],
    })
  }

  // 账龄分布饼图
  if (!agingChart && agingChartRef.value) agingChart = echarts.init(agingChartRef.value)
  if (agingChart) {
    const s = dashboard.value.agingSummary
    const agingData = s
      ? [
          { name: '30天内', value: Number(s.within30 ?? 0) },
          { name: '31-60天', value: Number(s.days3160 ?? 0) },
          { name: '61-90天', value: Number(s.days6190 ?? 0) },
          { name: '91-180天', value: Number(s.days91180 ?? 0) },
          { name: '181-365天', value: Number(s.days181365 ?? 0) },
          { name: '365天以上', value: Number(s.over365 ?? 0) },
        ].filter(d => d.value > 0)
      : []
    agingChart.setOption({
      tooltip: {
        trigger: 'item',
        formatter: (p: any) =>
          `${p.name}<br/>金额：${(p.value / 10000).toFixed(2)}万元<br/>占比：${p.percent}%`,
      },
      legend: { orient: 'vertical', left: 'left', top: 'middle', type: 'scroll', textStyle: { fontSize: 12 } },
      color: ['#409eff', '#e6a23c', '#f56c6c', '#c0392b', '#8e44ad', '#2c3e50'],
      series: [
        {
          type: 'pie',
          radius: ['40%', '70%'],
          center: ['65%', '55%'],
          data: agingData,
          label: { formatter: '{b}\n{d}%', fontSize: 11 },
        },
      ],
    })
  }
}

function fmtWanNum(v?: number | null) {
  if (v == null) return null
  return +(Number(v) / 10000).toFixed(2)
}
function fmtWan(v?: number | null) {
  if (v == null) return '-'
  return (Number(v) / 10000).toFixed(2)
}
function fmtMoney(v?: number | null) {
  return v != null ? Number(v).toLocaleString('zh-CN', { minimumFractionDigits: 2 }) : '-'
}
function fmtPct(v?: number | null) {
  return v != null ? `${Number(v).toFixed(2)}%` : '-'
}
function fmtDelta(v?: number | null) {
  if (v == null) return '-'
  return `${Number(v) > 0 ? '+' : ''}${Number(v).toFixed(2)}%`
}
function yoyClass(v?: number | null, reverseGood = false) {
  if (v == null) return 'change-neutral'
  const up = Number(v) > 0
  const good = reverseGood ? !up : up
  return good ? 'change-up' : Number(v) < 0 ? 'change-down' : 'change-neutral'
}
function yoyIcon(v?: number | null) {
  if (v == null || Number(v) === 0) return Minus
  return Number(v) > 0 ? Top : Bottom
}
</script>

<style scoped lang="scss">
.fin-dashboard {
  display: flex;
  flex-direction: column;
  gap: 16px;
}
.filter-card .latest-tip {
  margin-left: 16px;
  font-size: 13px;
  color: #909399;
  display: inline-flex;
  align-items: center;
  gap: 4px;
}
.kpi-row, .chart-row { margin: 0 !important; }
.kpi-card {
  text-align: center;
  padding: 4px 0;
  .kpi-title { font-size: 13px; color: #606266; margin-bottom: 6px; }
  .kpi-value { font-size: 28px; font-weight: 700; margin-bottom: 8px; }
  .kpi-yoy {
    font-size: 12px;
    display: inline-flex;
    align-items: center;
    gap: 2px;
    &.change-up { color: #67c23a; }
    &.change-down { color: #f56c6c; }
    &.change-neutral { color: #909399; }
  }
  .kpi-sub { font-size: 12px; color: #c0c4cc; margin-top: 4px; }
}
.card-header-flex {
  display: flex;
  align-items: center;
  justify-content: space-between;
}
.chart-container { height: 280px; width: 100%; }
.text-down { color: #f56c6c; font-weight: 600; }
</style>
