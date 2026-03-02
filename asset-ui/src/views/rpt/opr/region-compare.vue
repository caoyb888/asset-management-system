<template>
  <div class="opr-region-compare">
    <!-- 筛选栏 -->
    <el-card class="filter-card" shadow="never">
      <el-form inline :model="filterForm">
        <el-form-item label="统计月份">
          <el-date-picker
            v-model="filterForm.statMonth"
            type="month"
            value-format="YYYY-MM"
            placeholder="默认最新月"
            style="width: 160px"
          />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" :loading="loading" @click="loadData">查询</el-button>
          <el-button @click="resetFilter">重置</el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <!-- 图表区 -->
    <el-row :gutter="16">
      <!-- 多项目雷达图 -->
      <el-col :span="10">
        <el-card shadow="never">
          <template #header><span>多维度综合对比（雷达图）</span></template>
          <div ref="radarChartRef" class="chart-container" />
        </el-card>
      </el-col>
      <!-- 指标柱状图切换 -->
      <el-col :span="14">
        <el-card shadow="never">
          <template #header>
            <div class="card-header-flex">
              <span>指标横向对比</span>
              <el-radio-group v-model="compareMetric" size="small" @change="updateBarChart">
                <el-radio-button value="revenue">营收</el-radio-button>
                <el-radio-button value="passenger">客流</el-radio-button>
                <el-radio-button value="avgRevenue">坪效</el-radio-button>
                <el-radio-button value="expiring">到期预警</el-radio-button>
              </el-radio-group>
            </div>
          </template>
          <div ref="barChartRef" class="chart-container" />
        </el-card>
      </el-col>
    </el-row>

    <!-- 明细汇总表格 -->
    <el-card shadow="never">
      <template #header><span>各项目指标汇总</span></template>
      <el-table :data="tableData" border stripe size="small" v-loading="loading">
        <el-table-column label="项目" min-width="150">
          <template #default="{ row }">{{ projectNameMap[row.projectId!] || `项目${row.projectId}` }}</template>
        </el-table-column>
        <el-table-column prop="statMonth" label="月份" width="100" />
        <el-table-column label="营收（万元）" align="right">
          <template #default="{ row }">{{ fmtWan(row.revenueAmount) }}</template>
        </el-table-column>
        <el-table-column label="坪效（元/㎡）" align="right">
          <template #default="{ row }">{{ fmtMoney(row.avgRevenuePerSqm) }}</template>
        </el-table-column>
        <el-table-column label="客流量" align="right">
          <template #default="{ row }">{{ fmtNum(row.passengerFlow) }}</template>
        </el-table-column>
        <el-table-column prop="changeCount" label="合同变更" align="right" width="90" />
        <el-table-column prop="terminatedContracts" label="解约数" align="right" width="80" />
        <el-table-column prop="expiringContracts" label="到期预警" align="right" width="90">
          <template #default="{ row }">
            <el-tag v-if="row.expiringContracts > 0" type="warning" size="small">
              {{ row.expiringContracts }}
            </el-tag>
            <span v-else>0</span>
          </template>
        </el-table-column>
      </el-table>
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, computed, onMounted, onUnmounted, nextTick } from 'vue'
import * as echarts from 'echarts'
import { getRegionCompare } from '@/api/rpt/operation'
import { getProjectList } from '@/api/base/project'
import type { OprRegionCompareVO } from '@/api/rpt/operation'

const loading = ref(false)
const tableData = ref<OprRegionCompareVO[]>([])
const projectList = ref<{ id: number; projectName: string; projectCode: string }[]>([])
const compareMetric = ref<'revenue' | 'passenger' | 'avgRevenue' | 'expiring'>('revenue')
const filterForm = reactive({ statMonth: undefined as string | undefined })

const projectNameMap = computed(() => {
  const map: Record<number, string> = {}
  projectList.value.forEach(p => { map[p.id] = p.projectName })
  return map
})

const radarChartRef = ref<HTMLDivElement>()
const barChartRef = ref<HTMLDivElement>()
let radarChart: echarts.ECharts | null = null
let barChart: echarts.ECharts | null = null

onMounted(async () => {
  await loadProjects()
  await loadData()
  window.addEventListener('resize', handleResize)
})
onUnmounted(() => {
  radarChart?.dispose()
  barChart?.dispose()
  window.removeEventListener('resize', handleResize)
})
function handleResize() {
  radarChart?.resize()
  barChart?.resize()
}

async function loadProjects() {
  projectList.value = await getProjectList()
}

async function loadData() {
  loading.value = true
  try {
    tableData.value = await getRegionCompare({
      statMonth: filterForm.statMonth,
    })
    await nextTick()
    updateRadarChart()
    updateBarChart()
  } finally {
    loading.value = false
  }
}

function resetFilter() {
  filterForm.statMonth = undefined
  loadData()
}

function updateRadarChart() {
  if (!radarChart && radarChartRef.value) radarChart = echarts.init(radarChartRef.value)
  if (!radarChart) return
  const data = tableData.value
  const names = data.map(r => projectNameMap.value[r.projectId!] || `项目${r.projectId}`)
  // 归一化到百分制（取各项目得分字段）
  radarChart.setOption({
    tooltip: {},
    legend: { data: names, top: 0, type: 'scroll' },
    radar: {
      indicator: [
        { name: '营收', max: 100 },
        { name: '客流', max: 100 },
        { name: '坪效', max: 100 },
        { name: '变更管控', max: 100 },
        { name: '合同稳健', max: 100 },
      ],
      center: ['50%', '55%'],
      radius: '65%',
    },
    series: [
      {
        type: 'radar',
        data: data.map(r => ({
          name: projectNameMap.value[r.projectId!] || `项目${r.projectId}`,
          value: [
            r.revenueScore ?? 0,
            r.passengerScore ?? 0,
            r.avgRevenueScore ?? 0,
            r.changeCount != null ? Math.max(0, 100 - Number(r.changeCount) * 5) : 50,
            r.terminatedContracts != null ? Math.max(0, 100 - Number(r.terminatedContracts) * 10) : 80,
          ],
        })),
      },
    ],
  })
}

function updateBarChart() {
  if (!barChart && barChartRef.value) barChart = echarts.init(barChartRef.value)
  if (!barChart) return
  const data = tableData.value
  const names = data.map(r => projectNameMap.value[r.projectId!] || `项目${r.projectId}`)

  const metricMap: Record<string, { getter: (r: OprRegionCompareVO) => number | null; name: string; unit: string; color: string }> = {
    revenue: { getter: r => r.revenueAmount, name: '营收（万元）', unit: '万元', color: '#409eff' },
    passenger: { getter: r => r.passengerFlow, name: '客流量（人次）', unit: '人次', color: '#e6a23c' },
    avgRevenue: { getter: r => r.avgRevenuePerSqm, name: '坪效（元/㎡）', unit: '元/㎡', color: '#67c23a' },
    expiring: { getter: r => r.expiringContracts, name: '到期合同数', unit: '份', color: '#f56c6c' },
  }
  const m = metricMap[compareMetric.value]
  const values = data.map(r => {
    const v = m.getter(r)
    if (v == null) return null
    if (compareMetric.value === 'revenue') return +(Number(v) / 10000).toFixed(2)
    return Number(v)
  })

  barChart.setOption({
    tooltip: {
      trigger: 'axis',
      valueFormatter: (v: any) => `${v} ${m.unit}`,
    },
    grid: { left: 50, right: 20, top: 20, bottom: 60 },
    xAxis: { type: 'category', data: names, axisLabel: { rotate: 30, width: 80, overflow: 'truncate' } },
    yAxis: { type: 'value', name: m.unit },
    series: [
      {
        name: m.name,
        type: 'bar',
        data: values,
        itemStyle: { color: m.color },
        label: { show: true, position: 'top' },
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
function fmtNum(v?: number | null) {
  return v != null ? Number(v).toLocaleString('zh-CN') : '-'
}
</script>

<style scoped lang="scss">
.opr-region-compare {
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
</style>
