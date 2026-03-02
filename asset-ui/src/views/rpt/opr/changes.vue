<template>
  <div class="opr-changes">
    <!-- 筛选栏 -->
    <el-card class="filter-card" shadow="never">
      <el-form inline :model="filterForm">
        <el-form-item label="项目">
          <el-select v-model="filterForm.projectId" placeholder="全部项目" clearable style="width: 200px">
            <el-option v-for="p in projectList" :key="p.id" :label="p.projectName" :value="p.id" />
          </el-select>
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
        <el-form-item>
          <el-button type="primary" :loading="loading" @click="loadData">查询</el-button>
          <el-button @click="resetFilter">重置</el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <!-- 图表区 -->
    <el-row :gutter="16">
      <!-- 变更次数趋势 -->
      <el-col :span="14">
        <el-card shadow="never">
          <template #header><span>合同变更次数趋势</span></template>
          <div ref="changesTrendRef" class="chart-container" />
        </el-card>
      </el-col>
      <!-- 租金影响柱状图 -->
      <el-col :span="10">
        <el-card shadow="never">
          <template #header><span>租金影响金额（万元）</span></template>
          <div ref="rentImpactRef" class="chart-container" />
        </el-card>
      </el-col>
    </el-row>

    <!-- 租金变更分析 -->
    <el-card shadow="never">
      <template #header><span>租金变更趋势分析</span></template>
      <div ref="rentChangeTrendRef" style="height: 260px; width: 100%" />
    </el-card>

    <!-- 明细表格（Tab 切换） -->
    <el-card shadow="never">
      <template #header>
        <el-radio-group v-model="activeTab" size="small">
          <el-radio-button value="changes">合同变更统计</el-radio-button>
          <el-radio-button value="rent">租金变更分析</el-radio-button>
        </el-radio-group>
      </template>

      <!-- 合同变更明细 -->
      <el-table v-if="activeTab === 'changes'" :data="changesData" border stripe size="small" v-loading="loading">
        <el-table-column prop="timeDim" label="月份" width="100" />
        <el-table-column label="项目" min-width="130">
          <template #default="{ row }">{{ projectNameMap[row.projectId!] || `项目${row.projectId}` }}</template>
        </el-table-column>
        <el-table-column prop="formatType" label="业态" width="100" />
        <el-table-column prop="changeCount" label="变更次数" align="right" width="100" />
        <el-table-column label="租金影响（万元）" align="right">
          <template #default="{ row }">
            <span :class="impactClass(row.changeRentImpact)">{{ fmtWan(row.changeRentImpact) }}</span>
          </template>
        </el-table-column>
        <el-table-column prop="prevChangeCount" label="上期次数" align="right" width="100" />
        <el-table-column label="增长率" align="right" width="90">
          <template #default="{ row }">
            <span :class="growthClass(row.changeCountGrowthRate)">{{ fmtDelta(row.changeCountGrowthRate) }}</span>
          </template>
        </el-table-column>
      </el-table>

      <!-- 租金变更明细 -->
      <el-table v-else :data="rentData" border stripe size="small" v-loading="loading">
        <el-table-column prop="timeDim" label="月份" width="100" />
        <el-table-column label="项目" min-width="130">
          <template #default="{ row }">{{ projectNameMap[row.projectId!] || `项目${row.projectId}` }}</template>
        </el-table-column>
        <el-table-column prop="formatType" label="业态" width="100" />
        <el-table-column prop="changeCount" label="变更次数" align="right" width="100" />
        <el-table-column label="租金变化（万元）" align="right">
          <template #default="{ row }">
            <span :class="impactClass(row.changeRentImpact)">{{ fmtWan(row.changeRentImpact) }}</span>
          </template>
        </el-table-column>
        <el-table-column label="均次影响（万元）" align="right">
          <template #default="{ row }">{{ fmtWan(row.avgChangeImpact) }}</template>
        </el-table-column>
        <el-table-column label="环比变化率" align="right" width="100">
          <template #default="{ row }">
            <span :class="growthClass(row.changeRentGrowthRate)">{{ fmtDelta(row.changeRentGrowthRate) }}</span>
          </template>
        </el-table-column>
      </el-table>
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, computed, onMounted, onUnmounted, nextTick } from 'vue'
import * as echarts from 'echarts'
import { getContractChanges, getRentChanges } from '@/api/rpt/operation'
import { getProjectList } from '@/api/base/project'
import type { OprContractChangeVO, OprRentChangeVO } from '@/api/rpt/operation'

const loading = ref(false)
const changesData = ref<OprContractChangeVO[]>([])
const rentData = ref<OprRentChangeVO[]>([])
const projectList = ref<{ id: number; projectName: string; projectCode: string }[]>([])
const monthRange = ref<[string, string] | null>(null)
const activeTab = ref<'changes' | 'rent'>('changes')
const filterForm = reactive({ projectId: null as number | null })

const projectNameMap = computed(() => {
  const map: Record<number, string> = {}
  projectList.value.forEach(p => { map[p.id] = p.projectName })
  return map
})

const changesTrendRef = ref<HTMLDivElement>()
const rentImpactRef = ref<HTMLDivElement>()
const rentChangeTrendRef = ref<HTMLDivElement>()
let changesTrendChart: echarts.ECharts | null = null
let rentImpactChart: echarts.ECharts | null = null
let rentChangeTrendChart: echarts.ECharts | null = null

onMounted(async () => {
  await loadProjects()
  await loadData()
  window.addEventListener('resize', handleResize)
})
onUnmounted(() => {
  changesTrendChart?.dispose()
  rentImpactChart?.dispose()
  rentChangeTrendChart?.dispose()
  window.removeEventListener('resize', handleResize)
})
function handleResize() {
  changesTrendChart?.resize()
  rentImpactChart?.resize()
  rentChangeTrendChart?.resize()
}

async function loadProjects() {
  projectList.value = await getProjectList()
}

async function loadData() {
  loading.value = true
  try {
    const params: any = { projectId: filterForm.projectId ?? undefined }
    if (monthRange.value) {
      params.startMonth = monthRange.value[0]
      params.endMonth = monthRange.value[1]
    }
    const [changes, rent] = await Promise.all([
      getContractChanges(params),
      getRentChanges(params),
    ])
    changesData.value = changes
    rentData.value = rent
    await nextTick()
    updateChangesTrend()
    updateRentImpact()
    updateRentChangeTrend()
  } finally {
    loading.value = false
  }
}

function resetFilter() {
  filterForm.projectId = null
  monthRange.value = null
  loadData()
}

function updateChangesTrend() {
  if (!changesTrendChart && changesTrendRef.value) changesTrendChart = echarts.init(changesTrendRef.value)
  if (!changesTrendChart) return
  // 按时间聚合
  const timeMap: Record<string, { count: number; impact: number }> = {}
  changesData.value.forEach(r => {
    if (!timeMap[r.timeDim]) timeMap[r.timeDim] = { count: 0, impact: 0 }
    timeMap[r.timeDim].count += Number(r.changeCount ?? 0)
    timeMap[r.timeDim].impact += Number(r.changeRentImpact ?? 0)
  })
  const times = Object.keys(timeMap).sort()
  changesTrendChart.setOption({
    tooltip: { trigger: 'axis' },
    legend: { data: ['变更次数', '租金影响（万元）'], top: 0 },
    grid: { left: 50, right: 60, top: 40, bottom: 30 },
    xAxis: { type: 'category', data: times, axisLabel: { rotate: 30 } },
    yAxis: [
      { type: 'value', name: '次数', minInterval: 1 },
      { type: 'value', name: '万元', axisLabel: { formatter: '{value}w' } },
    ],
    series: [
      {
        name: '变更次数',
        type: 'bar',
        data: times.map(t => timeMap[t].count),
        itemStyle: { color: '#409eff' },
      },
      {
        name: '租金影响（万元）',
        type: 'line',
        smooth: true,
        yAxisIndex: 1,
        data: times.map(t => +(timeMap[t].impact / 10000).toFixed(2)),
        itemStyle: { color: '#f56c6c' },
        label: { show: false },
      },
    ],
  })
}

function updateRentImpact() {
  if (!rentImpactChart && rentImpactRef.value) rentImpactChart = echarts.init(rentImpactRef.value)
  if (!rentImpactChart) return
  // 按项目聚合租金影响
  const projMap: Record<string, number> = {}
  changesData.value.forEach(r => {
    const key = projectNameMap.value[r.projectId!] || `项目${r.projectId}`
    projMap[key] = (projMap[key] ?? 0) + Number(r.changeRentImpact ?? 0)
  })
  const entries = Object.entries(projMap).sort((a, b) => Math.abs(b[1]) - Math.abs(a[1]))
  rentImpactChart.setOption({
    tooltip: {
      trigger: 'axis',
      formatter: (params: any) => {
        const p = params[0]
        return `${p.name}<br/>租金影响：${(p.value / 10000).toFixed(2)}万元`
      },
    },
    grid: { left: 120, right: 30, top: 20, bottom: 30 },
    xAxis: { type: 'value', name: '万元', axisLabel: { formatter: (v: number) => (v / 10000).toFixed(0) } },
    yAxis: { type: 'category', data: entries.map(e => e[0]), axisLabel: { width: 100, overflow: 'truncate' } },
    series: [
      {
        type: 'bar',
        data: entries.map(e => e[1]),
        itemStyle: {
          color: (p: any) => p.data >= 0 ? '#67c23a' : '#f56c6c',
        },
        label: { show: true, position: 'right', formatter: (p: any) => `${(p.value / 10000).toFixed(1)}w` },
      },
    ],
  })
}

function updateRentChangeTrend() {
  if (!rentChangeTrendChart && rentChangeTrendRef.value) rentChangeTrendChart = echarts.init(rentChangeTrendRef.value)
  if (!rentChangeTrendChart) return
  const timeMap: Record<string, { impact: number; count: number }> = {}
  rentData.value.forEach(r => {
    if (!timeMap[r.timeDim]) timeMap[r.timeDim] = { impact: 0, count: 0 }
    timeMap[r.timeDim].impact += Number(r.changeRentImpact ?? 0)
    timeMap[r.timeDim].count += Number(r.changeCount ?? 0)
  })
  const times = Object.keys(timeMap).sort()
  rentChangeTrendChart.setOption({
    tooltip: { trigger: 'axis' },
    legend: { data: ['租金变化（万元）'], top: 0 },
    grid: { left: 65, right: 20, top: 40, bottom: 30 },
    xAxis: { type: 'category', data: times, axisLabel: { rotate: 30 } },
    yAxis: { type: 'value', name: '万元' },
    series: [
      {
        name: '租金变化（万元）',
        type: 'bar',
        data: times.map(t => +(timeMap[t].impact / 10000).toFixed(2)),
        itemStyle: {
          color: (p: any) => p.data >= 0 ? '#67c23a' : '#f56c6c',
        },
        label: { show: true, position: (p: any) => p.data >= 0 ? 'top' : 'bottom', formatter: (p: any) => `${p.value}w` },
      },
    ],
  })
}

function fmtWan(v?: number | null) {
  if (v == null) return '-'
  return (Number(v) / 10000).toFixed(2)
}
function fmtDelta(v?: number | null) {
  if (v == null) return '-'
  return `${Number(v) > 0 ? '+' : ''}${Number(v).toFixed(2)}%`
}
function growthClass(v?: number | null) {
  if (v == null) return ''
  return Number(v) > 0 ? 'text-up' : Number(v) < 0 ? 'text-down' : ''
}
function impactClass(v?: number | null) {
  if (v == null) return ''
  return Number(v) > 0 ? 'text-up' : Number(v) < 0 ? 'text-down' : ''
}
</script>

<style scoped lang="scss">
.opr-changes {
  display: flex;
  flex-direction: column;
  gap: 16px;
}
.chart-container { height: 280px; width: 100%; }
.text-up { color: #67c23a; font-weight: 600; }
.text-down { color: #f56c6c; font-weight: 600; }
</style>
