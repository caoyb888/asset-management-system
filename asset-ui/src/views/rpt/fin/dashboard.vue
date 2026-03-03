<template>
  <div class="fin-dashboard">
    <!-- 通用筛选栏 -->
    <ReportFilterBar
      v-model="filter"
      :fields="['project', 'compareMode']"
      :loading="loading"
      :latest-date="dashboard.latestMonth ?? undefined"
      @search="loadData"
      @reset="loadData"
    />

    <!-- KPI 卡片 -->
    <el-row :gutter="16" class="kpi-row">
      <el-col :span="6">
        <MetricCard
          label="应收总额（万元）"
          :value="dashboard.totalReceivable"
          :wan="true"
          color="#409eff"
          :yoy="dashboard.receivableYoY"
          :loading="loading"
        />
      </el-col>
      <el-col :span="6">
        <MetricCard
          label="已收金额（万元）"
          :value="dashboard.totalReceived"
          :wan="true"
          color="#67c23a"
          :yoy="dashboard.receivedYoY"
          :sub="`收缴率：${fmtPct(dashboard.avgCollectionRate)}`"
          :loading="loading"
        />
      </el-col>
      <el-col :span="6">
        <MetricCard
          label="欠款总额（万元）"
          :value="dashboard.totalOutstanding"
          :wan="true"
          color="#e6a23c"
          :sub="`逾期金额：${fmtWan(dashboard.totalOverdue)}万元`"
          :loading="loading"
        />
      </el-col>
      <el-col :span="6">
        <MetricCard
          label="逾期率"
          :value="dashboard.avgOverdueRate"
          unit="%"
          color="#f56c6c"
          :yoy="dashboard.overdueRateYoY"
          :reverse-good="true"
          :sub="`保证金：${fmtWan(dashboard.totalDepositBalance)}万元`"
          :loading="loading"
        />
      </el-col>
    </el-row>

    <!-- 趋势图：应收/已收 + 收缴率 -->
    <el-row :gutter="16" class="chart-row">
      <el-col :span="16">
        <el-card shadow="never">
          <template #header><span>应收/已收/欠款趋势（近12月）</span></template>
          <ChartContainer :option="trendOption" height="280px" :loading="loading" />
        </el-card>
      </el-col>
      <el-col :span="8">
        <el-card shadow="never">
          <template #header><span>收缴率趋势（近12月）</span></template>
          <ChartContainer :option="rateOption" height="280px" :loading="loading" />
        </el-card>
      </el-col>
    </el-row>

    <!-- 账龄分布 + 欠款TOP10 -->
    <el-row :gutter="16" class="chart-row">
      <el-col :span="10">
        <el-card shadow="never">
          <template #header><span>账龄分布</span></template>
          <ChartContainer :option="agingOption" height="280px" :loading="loading" />
        </el-card>
      </el-col>
      <el-col :span="14">
        <el-card shadow="never">
          <template #header><span>欠款TOP10商家</span></template>
          <DrillTable
            :data="(dashboard.overdueTop10 ?? []) as any[]"
            :columns="top10Columns"
            :loading="loading"
            :max-height="320"
            @drill="handleDrill"
          >
            <template #outstanding="{ row }">
              <span class="text-down">{{ fmtMoney(row.totalOutstanding) }}</span>
            </template>
            <template #overdue="{ row }">
              <span class="text-down">{{ fmtMoney(overdueTotal(row)) }}</span>
            </template>
          </DrillTable>
        </el-card>
      </el-col>
    </el-row>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, computed, watch } from 'vue'
import { useRouter } from 'vue-router'
import { ReportFilterBar, MetricCard, ChartContainer, DrillTable } from '@/components/rpt'
import type { FilterState, DrillColumn, DrillEvent } from '@/components/rpt'
import { getFinDashboard } from '@/api/rpt/finance'
import type { FinDashboardVO, FinAgingAnalysisVO } from '@/api/rpt/finance'

const router = useRouter()
const loading = ref(false)
const dashboard = ref<FinDashboardVO>({
  latestMonth: null, totalReceivable: null, totalReceived: null,
  totalOutstanding: null, totalOverdue: null, avgCollectionRate: null,
  avgOverdueRate: null, totalDepositBalance: null, totalPrepayBalance: null,
  receivableYoY: null, receivedYoY: null, collectionRateYoY: null,
  overdueRateYoY: null, financeTrend: [], agingSummary: null, overdueTop10: [],
})

const filter = reactive<FilterState>({
  projectId: null,
  compareMode: 'NONE',
})

// ─── 图表配置（响应式计算）───

const trendOption = computed(() => {
  const data = dashboard.value.financeTrend ?? []
  return {
    tooltip: { trigger: 'axis' },
    legend: { data: ['应收（万元）', '已收（万元）', '欠款（万元）'], top: 0 },
    grid: { left: 60, right: 20, top: 40, bottom: 30 },
    xAxis: { type: 'category', data: data.map(d => d.timeDim), axisLabel: { rotate: 30 } },
    yAxis: { type: 'value', name: '万元' },
    series: [
      {
        name: '应收（万元）',
        type: 'line', smooth: true,
        data: data.map(d => toWan(d.receivableAmount)),
        itemStyle: { color: '#409eff' },
      },
      {
        name: '已收（万元）',
        type: 'line', smooth: true,
        data: data.map(d => toWan(d.receivedAmount)),
        itemStyle: { color: '#67c23a' },
        areaStyle: { color: 'rgba(103,194,58,0.1)' },
      },
      {
        name: '欠款（万元）',
        type: 'bar',
        data: data.map(d => toWan(d.outstandingAmount)),
        itemStyle: { color: '#e6a23c' },
      },
    ],
  }
})

const rateOption = computed(() => {
  const data = dashboard.value.financeTrend ?? []
  return {
    tooltip: {
      trigger: 'axis',
      formatter: (params: any) =>
        (params as any[]).map((p: any) => `${p.marker}${p.seriesName}：${p.value != null ? Number(p.value).toFixed(2) + '%' : '-'}`).join('<br/>'),
    },
    legend: { data: ['收缴率(%)', '逾期率(%)'], top: 0 },
    grid: { left: 50, right: 20, top: 40, bottom: 30 },
    xAxis: { type: 'category', data: data.map(d => d.timeDim), axisLabel: { rotate: 30 } },
    yAxis: { type: 'value', name: '%', max: 100 },
    series: [
      {
        name: '收缴率(%)', type: 'line', smooth: true,
        data: data.map(d => d.collectionRate != null ? Number(d.collectionRate) : null),
        itemStyle: { color: '#67c23a' },
      },
      {
        name: '逾期率(%)', type: 'line', smooth: true,
        data: data.map(d => d.overdueRate != null ? Number(d.overdueRate) : null),
        itemStyle: { color: '#f56c6c' },
      },
    ],
  }
})

const agingOption = computed(() => {
  const s = dashboard.value.agingSummary
  if (!s) return {}
  const buckets = [
    { name: '30天内', value: Number(s.within30 ?? 0), color: '#409eff' },
    { name: '31-60天', value: Number(s.days3160 ?? 0), color: '#67c23a' },
    { name: '61-90天', value: Number(s.days6190 ?? 0), color: '#e6a23c' },
    { name: '91-180天', value: Number(s.days91180 ?? 0), color: '#f56c6c' },
    { name: '181-365天', value: Number(s.days181365 ?? 0), color: '#c0392b' },
    { name: '365天以上', value: Number(s.over365 ?? 0), color: '#2c3e50' },
  ].filter(d => d.value > 0)
  return {
    tooltip: {
      trigger: 'item',
      formatter: (p: any) =>
        `${p.name}<br/>金额：${(p.value / 10000).toFixed(2)}万元<br/>占比：${p.percent}%`,
    },
    legend: { orient: 'vertical', left: 'left', top: 'middle', type: 'scroll', textStyle: { fontSize: 12 } },
    color: buckets.map(b => b.color),
    series: [{
      type: 'pie', radius: ['40%', '70%'], center: ['65%', '55%'],
      data: buckets,
      label: { formatter: '{b}\n{d}%', fontSize: 11 },
    }],
  }
})

// ─── DrillTable 列定义 ───

const top10Columns: DrillColumn[] = [
  { prop: 'merchantId', label: '商家ID', width: 90, drillable: true },
  { slot: 'outstanding', label: '欠款合计（元）', align: 'right', minWidth: 120 },
  { prop: 'within30', label: '30天内（元）', align: 'right', width: 110,
    formatter: (_r, _c, val) => fmtMoney(val as number) },
  { slot: 'overdue', label: '逾期（元）', align: 'right', width: 110 },
]

// ─── Events ───

function handleDrill(event: DrillEvent) {
  const row = event.row as unknown as FinAgingAnalysisVO
  if (row.merchantId) {
    router.push({ path: '/fin/receivables', query: { merchantId: String(row.merchantId) } })
  }
}

// ─── Data Loading ───

async function loadData() {
  loading.value = true
  try {
    const params: any = {}
    if (filter.projectId) params.projectId = filter.projectId
    if (filter.compareMode && filter.compareMode !== 'NONE') params.compareMode = filter.compareMode
    dashboard.value = await getFinDashboard(params)
  } finally {
    loading.value = false
  }
}

loadData()

// ─── Utils ───

function toWan(v?: number | null) {
  if (v == null) return null
  return +(Number(v) / 10000).toFixed(2)
}
function fmtWan(v?: number | null) {
  if (v == null) return '-'
  return (Number(v) / 10000).toFixed(2)
}
function fmtPct(v?: number | null) {
  return v != null ? `${Number(v).toFixed(2)}%` : '-'
}
function fmtMoney(v?: number | null) {
  if (v == null || Number(v) === 0) return '-'
  return Number(v).toLocaleString('zh-CN', { minimumFractionDigits: 2 })
}
function overdueTotal(row: any) {
  return (row.days3160 ?? 0) + (row.days6190 ?? 0) + (row.days91180 ?? 0) + (row.days181365 ?? 0) + (row.over365 ?? 0)
}
</script>

<style scoped lang="scss">
.fin-dashboard {
  display: flex;
  flex-direction: column;
  gap: 16px;
}
.kpi-row, .chart-row { margin: 0 !important; }
.text-down { color: #f56c6c; font-weight: 600; }
</style>
