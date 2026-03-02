<template>
  <div class="opr-revenue">
    <!-- 筛选栏 -->
    <el-card class="filter-card" shadow="never">
      <el-form inline :model="filterForm">
        <el-form-item label="项目">
          <el-select v-model="filterForm.projectId" placeholder="全部项目" clearable style="width: 200px">
            <el-option v-for="p in projectList" :key="p.id" :label="p.projectName" :value="p.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="业态">
          <el-input v-model="filterForm.formatType" placeholder="全部业态" clearable style="width: 130px" />
        </el-form-item>
        <el-form-item label="月份范围">
          <el-date-picker
            v-model="monthRange"
            type="monthrange"
            value-format="YYYY-MM"
            start-placeholder="开始月份"
            end-placeholder="结束月份"
            style="width: 240px"
          />
        </el-form-item>
        <el-form-item label="对比">
          <el-radio-group v-model="filterForm.compareMode" @change="loadData">
            <el-radio-button value="NONE">不对比</el-radio-button>
            <el-radio-button value="YOY">同比</el-radio-button>
            <el-radio-button value="MOM">环比</el-radio-button>
          </el-radio-group>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" :loading="loading" @click="loadData">查询</el-button>
          <el-button @click="resetFilter">重置</el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <!-- 图表区 -->
    <el-row :gutter="16">
      <!-- 营收趋势折线图 -->
      <el-col :span="16">
        <el-card shadow="never">
          <template #header>
            <div class="card-header-flex">
              <span>营收趋势分析</span>
              <el-radio-group v-model="chartType" size="small" @change="updateTrendChart">
                <el-radio-button value="line">折线</el-radio-button>
                <el-radio-button value="bar">柱状</el-radio-button>
              </el-radio-group>
            </div>
          </template>
          <div ref="trendChartRef" class="chart-container" />
        </el-card>
      </el-col>
      <!-- 业态分布饼图 -->
      <el-col :span="8">
        <el-card shadow="never">
          <template #header><span>业态营收占比</span></template>
          <div ref="pieChartRef" class="chart-container" />
        </el-card>
      </el-col>
    </el-row>

    <!-- 明细表格 -->
    <el-card shadow="never">
      <template #header><span>营收汇总明细</span></template>
      <el-table :data="tableData" border stripe size="small" v-loading="loading">
        <el-table-column prop="timeDim" label="月份" width="100" />
        <el-table-column label="项目" min-width="130">
          <template #default="{ row }">{{ projectNameMap[row.projectId!] || `项目${row.projectId}` }}</template>
        </el-table-column>
        <el-table-column prop="formatType" label="业态" width="100" />
        <el-table-column label="营收（万元）" align="right">
          <template #default="{ row }">{{ fmtWan(row.revenueAmount) }}</template>
        </el-table-column>
        <el-table-column label="浮动租金（万元）" align="right">
          <template #default="{ row }">{{ fmtWan(row.floatingRentAmount) }}</template>
        </el-table-column>
        <el-table-column label="坪效（元/㎡）" align="right">
          <template #default="{ row }">{{ fmtMoney(row.avgRevenuePerSqm) }}</template>
        </el-table-column>
        <el-table-column v-if="filterForm.compareMode !== 'NONE'" label="对比期（万元）" align="right">
          <template #default="{ row }">{{ fmtWan(row.prevRevenueAmount) }}</template>
        </el-table-column>
        <el-table-column v-if="filterForm.compareMode !== 'NONE'" label="增长率" align="right" width="100">
          <template #default="{ row }">
            <span :class="growthClass(row.revenueGrowthRate)">{{ fmtDelta(row.revenueGrowthRate) }}</span>
          </template>
        </el-table-column>
      </el-table>
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, computed, onMounted, onUnmounted, nextTick } from 'vue'
import * as echarts from 'echarts'
import { getRevenueSummary } from '@/api/rpt/operation'
import { getProjectList } from '@/api/base/project'
import type { OprRevenueSummaryVO } from '@/api/rpt/operation'

const loading = ref(false)
const tableData = ref<OprRevenueSummaryVO[]>([])
const projectList = ref<{ id: number; projectName: string; projectCode: string }[]>([])
const monthRange = ref<[string, string] | null>(null)
const chartType = ref<'line' | 'bar'>('line')
const filterForm = reactive({
  projectId: null as number | null,
  formatType: '',
  compareMode: 'NONE' as 'NONE' | 'YOY' | 'MOM',
})

const projectNameMap = computed(() => {
  const map: Record<number, string> = {}
  projectList.value.forEach(p => { map[p.id] = p.projectName })
  return map
})

const trendChartRef = ref<HTMLDivElement>()
const pieChartRef = ref<HTMLDivElement>()
let trendChart: echarts.ECharts | null = null
let pieChart: echarts.ECharts | null = null

onMounted(async () => {
  await loadProjects()
  await loadData()
  window.addEventListener('resize', handleResize)
})
onUnmounted(() => {
  trendChart?.dispose()
  pieChart?.dispose()
  window.removeEventListener('resize', handleResize)
})
function handleResize() {
  trendChart?.resize()
  pieChart?.resize()
}

async function loadProjects() {
  projectList.value = await getProjectList()
}

async function loadData() {
  loading.value = true
  try {
    const params: any = {
      projectId: filterForm.projectId ?? undefined,
      formatType: filterForm.formatType || undefined,
      compareMode: filterForm.compareMode,
    }
    if (monthRange.value) {
      params.startMonth = monthRange.value[0]
      params.endMonth = monthRange.value[1]
    }
    tableData.value = await getRevenueSummary(params)
    await nextTick()
    updateTrendChart()
    updatePieChart()
  } finally {
    loading.value = false
  }
}

function resetFilter() {
  filterForm.projectId = null
  filterForm.formatType = ''
  filterForm.compareMode = 'NONE'
  monthRange.value = null
  loadData()
}

function updateTrendChart() {
  if (!trendChart && trendChartRef.value) trendChart = echarts.init(trendChartRef.value)
  if (!trendChart) return
  // 按时间维度聚合营收
  const timeMap: Record<string, { cur: number; prev: number | null }> = {}
  tableData.value.forEach(r => {
    if (!timeMap[r.timeDim]) timeMap[r.timeDim] = { cur: 0, prev: null }
    timeMap[r.timeDim].cur += Number(r.revenueAmount ?? 0)
    if (r.prevRevenueAmount != null) {
      timeMap[r.timeDim].prev = (timeMap[r.timeDim].prev ?? 0) + Number(r.prevRevenueAmount)
    }
  })
  const times = Object.keys(timeMap).sort()
  const curData = times.map(t => +(timeMap[t].cur / 10000).toFixed(2))
  const prevData = times.map(t => timeMap[t].prev != null ? +(timeMap[t].prev! / 10000).toFixed(2) : null)
  const hasPrev = filterForm.compareMode !== 'NONE'
  const compareName = filterForm.compareMode === 'YOY' ? '同比期（万元）' : '环比期（万元）'

  trendChart.setOption({
    tooltip: { trigger: 'axis' },
    legend: { data: ['营收（万元）', ...(hasPrev ? [compareName] : [])], top: 0 },
    grid: { left: 55, right: 20, top: 40, bottom: 30 },
    xAxis: { type: 'category', data: times, axisLabel: { rotate: 30 } },
    yAxis: { type: 'value', name: '万元' },
    series: [
      {
        name: '营收（万元）',
        type: chartType.value,
        smooth: true,
        data: curData,
        itemStyle: { color: '#409eff' },
        areaStyle: chartType.value === 'line' ? { color: 'rgba(64,158,255,0.1)' } : undefined,
      },
      ...(hasPrev
        ? [{
            name: compareName,
            type: chartType.value as any,
            smooth: true,
            data: prevData,
            itemStyle: { color: '#c0c4cc' },
            lineStyle: chartType.value === 'line' ? { type: 'dashed' as const } : undefined,
          }]
        : []),
    ],
  })
}

function updatePieChart() {
  if (!pieChart && pieChartRef.value) pieChart = echarts.init(pieChartRef.value)
  if (!pieChart) return
  // 按业态聚合
  const formatMap: Record<string, number> = {}
  tableData.value.forEach(r => {
    const key = r.formatType || '未分类'
    formatMap[key] = (formatMap[key] ?? 0) + Number(r.revenueAmount ?? 0)
  })
  const pieData = Object.entries(formatMap)
    .map(([name, value]) => ({ name, value: +(value / 10000).toFixed(2) }))
    .sort((a, b) => b.value - a.value)
  pieChart.setOption({
    tooltip: {
      trigger: 'item',
      formatter: (p: any) => `${p.name}<br/>营收：${p.value}万元<br/>占比：${p.percent}%`,
    },
    legend: { orient: 'vertical', left: 'left', top: 'middle', type: 'scroll' },
    series: [
      {
        type: 'pie',
        radius: ['40%', '70%'],
        center: ['65%', '55%'],
        data: pieData,
        label: { formatter: '{b}\n{d}%', fontSize: 11 },
      },
    ],
  })
}

function fmtWan(v?: number | null) {
  if (v == null) return '-'
  return (Number(v) / 10000).toFixed(2)
}
function fmtMoney(v?: number | null) {
  return v != null ? Number(v).toFixed(2) : '-'
}
function fmtDelta(v?: number | null) {
  if (v == null) return '-'
  return `${Number(v) > 0 ? '+' : ''}${Number(v).toFixed(2)}%`
}
function growthClass(v?: number | null) {
  if (v == null) return ''
  return Number(v) > 0 ? 'text-up' : Number(v) < 0 ? 'text-down' : ''
}
</script>

<style scoped lang="scss">
.opr-revenue {
  display: flex;
  flex-direction: column;
  gap: 16px;
}
.card-header-flex {
  display: flex;
  align-items: center;
  justify-content: space-between;
}
.chart-container { height: 300px; width: 100%; }
.text-up { color: #67c23a; font-weight: 600; }
.text-down { color: #f56c6c; font-weight: 600; }
</style>
