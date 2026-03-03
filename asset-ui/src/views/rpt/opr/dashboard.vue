<template>
  <div class="opr-dashboard">
    <!-- 通用筛选栏 -->
    <ReportFilterBar
      v-model="filter"
      :fields="['project']"
      :loading="loading"
      :latest-date="dashboard.latestMonth ?? undefined"
      @search="loadData"
      @reset="loadData"
    />

    <!-- KPI 卡片 -->
    <el-row :gutter="16" class="kpi-row">
      <el-col :span="6">
        <MetricCard
          label="本月营收（万元）"
          :value="dashboard.totalRevenue"
          :wan="true"
          color="#409eff"
          :yoy="dashboard.revenueYoY"
          :sub="`浮动租金：${fmtWan(dashboard.floatingRentAmount)}万元`"
          :loading="loading"
        />
      </el-col>
      <el-col :span="6">
        <MetricCard
          label="均坪效（元/㎡/月）"
          :value="dashboard.avgRevenuePerSqm"
          :precision="2"
          color="#67c23a"
          :yoy="dashboard.avgRevenuePerSqmYoY"
          :loading="loading"
        />
      </el-col>
      <el-col :span="6">
        <MetricCard
          label="客流量（万人次）"
          :value="dashboard.passengerFlow"
          :wan="true"
          color="#e6a23c"
          :yoy="dashboard.passengerFlowYoY"
          :loading="loading"
        />
      </el-col>
      <el-col :span="6">
        <MetricCard
          label="合同变更 / 解约"
          :value="`${dashboard.changeCount ?? '-'} / ${dashboard.terminatedContracts ?? '-'}`"
          color="#f56c6c"
          :sub="`30天内到期：${dashboard.expiringWithin30 ?? '-'} 份`"
          :loading="loading"
        />
      </el-col>
    </el-row>

    <!-- 趋势图：营收 + 客流 -->
    <el-row :gutter="16" class="chart-row">
      <el-col :span="12">
        <el-card shadow="never">
          <template #header><span>营收趋势（近12月）</span></template>
          <ChartContainer :option="revenueOption" height="280px" :loading="loading" />
        </el-card>
      </el-col>
      <el-col :span="12">
        <el-card shadow="never">
          <template #header><span>客流趋势（近12月）</span></template>
          <ChartContainer :option="passengerOption" height="280px" :loading="loading" />
        </el-card>
      </el-col>
    </el-row>

    <!-- 到期预警 -->
    <el-card shadow="never">
      <template #header>
        <div class="card-header-flex">
          <span>合同到期预警</span>
          <el-tag type="danger" v-if="dashboard.expiringWithin30">
            30天内到期：{{ dashboard.expiringWithin30 }} 份
          </el-tag>
        </div>
      </template>
      <ChartContainer :option="warningOption" height="220px" :loading="loading" />
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, computed } from 'vue'
import { ReportFilterBar, MetricCard, ChartContainer } from '@/components/rpt'
import type { FilterState } from '@/components/rpt'
import { getOprDashboard } from '@/api/rpt/operation'
import type { OprDashboardVO } from '@/api/rpt/operation'

const loading = ref(false)
const dashboard = ref<OprDashboardVO>({
  latestMonth: null, totalRevenue: null, floatingRentAmount: null,
  avgRevenuePerSqm: null, passengerFlow: null, changeCount: null,
  terminatedContracts: null, revenueYoY: null, passengerFlowYoY: null,
  avgRevenuePerSqmYoY: null, expiringWithin30: null, expiringWithin60: null,
  expiringWithin90: null, revenueTrend: [], passengerTrend: [], projectComparison: [],
})

const filter = reactive<FilterState>({ projectId: null })

// ─── 图表配置 ───

const revenueOption = computed(() => {
  const data = dashboard.value.revenueTrend ?? []
  return {
    tooltip: { trigger: 'axis' },
    legend: { data: ['营收（万元）', '浮动租金（万元）'], top: 0 },
    grid: { left: 55, right: 20, top: 40, bottom: 30 },
    xAxis: { type: 'category', data: data.map(d => d.timeDim), axisLabel: { rotate: 30 } },
    yAxis: { type: 'value', name: '万元' },
    series: [
      {
        name: '营收（万元）', type: 'line', smooth: true,
        data: data.map(d => d.revenueAmount != null ? +(Number(d.revenueAmount) / 10000).toFixed(2) : null),
        itemStyle: { color: '#409eff' },
        areaStyle: { color: 'rgba(64,158,255,0.1)' },
      },
      {
        name: '浮动租金（万元）', type: 'bar',
        data: data.map(d => d.floatingRentAmount != null ? +(Number(d.floatingRentAmount) / 10000).toFixed(2) : null),
        itemStyle: { color: '#67c23a' },
      },
    ],
  }
})

const passengerOption = computed(() => {
  const data = dashboard.value.passengerTrend ?? []
  return {
    tooltip: { trigger: 'axis' },
    grid: { left: 55, right: 20, top: 20, bottom: 30 },
    xAxis: { type: 'category', data: data.map(d => d.timeDim), axisLabel: { rotate: 30 } },
    yAxis: { type: 'value', name: '人次' },
    series: [
      {
        name: '客流量', type: 'bar',
        data: data.map(d => d.passengerFlow),
        itemStyle: { color: '#e6a23c' },
      },
      {
        name: '日均客流', type: 'line', smooth: true,
        data: data.map(d => d.avgDailyPassenger),
        itemStyle: { color: '#f56c6c' },
      },
    ],
  }
})

const warningOption = computed(() => ({
  tooltip: { trigger: 'axis' },
  legend: { data: ['30天内', '31-60天', '61-90天'], top: 0 },
  grid: { left: 50, right: 20, top: 40, bottom: 30 },
  xAxis: { type: 'category', data: ['当前汇总'] },
  yAxis: { type: 'value', name: '合同数', minInterval: 1 },
  series: [
    {
      name: '30天内', type: 'bar', stack: 'expiring',
      data: [dashboard.value.expiringWithin30 ?? 0],
      itemStyle: { color: '#f56c6c' },
      label: { show: true, position: 'inside' },
    },
    {
      name: '31-60天', type: 'bar', stack: 'expiring',
      data: [Math.max(0, (dashboard.value.expiringWithin60 ?? 0) - (dashboard.value.expiringWithin30 ?? 0))],
      itemStyle: { color: '#e6a23c' },
      label: { show: true, position: 'inside' },
    },
    {
      name: '61-90天', type: 'bar', stack: 'expiring',
      data: [Math.max(0, (dashboard.value.expiringWithin90 ?? 0) - (dashboard.value.expiringWithin60 ?? 0))],
      itemStyle: { color: '#67c23a' },
      label: { show: true, position: 'inside' },
    },
  ],
}))

// ─── Data Loading ───

async function loadData() {
  loading.value = true
  try {
    const params = filter.projectId ? { projectId: filter.projectId } : {}
    dashboard.value = await getOprDashboard(params)
  } finally {
    loading.value = false
  }
}

loadData()

function fmtWan(v?: number | null) {
  if (v == null) return '-'
  return (Number(v) / 10000).toFixed(2)
}
</script>

<style scoped lang="scss">
.opr-dashboard {
  display: flex;
  flex-direction: column;
  gap: 16px;
}
.kpi-row, .chart-row { margin: 0 !important; }
.card-header-flex {
  display: flex;
  align-items: center;
  justify-content: space-between;
}
</style>
