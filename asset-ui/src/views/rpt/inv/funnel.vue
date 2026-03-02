<template>
  <div class="inv-funnel">
    <!-- 筛选栏 -->
    <el-card class="filter-card" shadow="never">
      <el-form inline :model="filterForm">
        <el-form-item label="项目">
          <el-select v-model="filterForm.projectId" placeholder="全部项目" clearable style="width: 200px">
            <el-option v-for="p in projectList" :key="p.id" :label="p.projectName" :value="p.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="统计日期">
          <el-date-picker
            v-model="filterForm.statDate"
            type="date"
            value-format="YYYY-MM-DD"
            placeholder="默认最新"
            style="width: 160px"
          />
        </el-form-item>
        <el-form-item label="时间维度">
          <el-radio-group v-model="filterForm.timeUnit" @change="loadTrend">
            <el-radio-button value="DAY">日</el-radio-button>
            <el-radio-button value="MONTH">月</el-radio-button>
            <el-radio-button value="YEAR">年</el-radio-button>
          </el-radio-group>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" :loading="loading" @click="loadAll">查询</el-button>
          <el-button @click="resetFilter">重置</el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <el-row :gutter="16">
      <!-- 漏斗图 -->
      <el-col :span="10">
        <el-card shadow="never">
          <template #header><span>客户跟进漏斗</span></template>
          <div ref="funnelChartRef" class="chart-container" />
          <!-- 转化率汇总 -->
          <el-row class="conversion-row">
            <el-col :span="8" v-for="f in funnelData" :key="f.stage" class="conversion-item">
              <div class="stage-name">{{ f.stageName }}</div>
              <div class="stage-count">{{ f.count ?? '-' }}</div>
              <div class="stage-rate" v-if="f.stage !== 'INTENTION_TOTAL'">
                转化率 <b>{{ fmtRate(f.conversionRate) }}</b>
              </div>
              <div class="stage-total" v-if="f.stage !== 'INTENTION_TOTAL'">
                综合 <b>{{ fmtRate(f.overallConversionRate) }}</b>
              </div>
            </el-col>
          </el-row>
        </el-card>
      </el-col>

      <!-- 趋势图 -->
      <el-col :span="14">
        <el-card shadow="never">
          <template #header>
            <div style="display:flex; align-items:center; justify-content:space-between">
              <span>意向客户统计趋势</span>
              <el-radio-group v-model="filterForm.compareMode" size="small" @change="loadTrend">
                <el-radio-button value="NONE">不对比</el-radio-button>
                <el-radio-button value="YOY">同比</el-radio-button>
                <el-radio-button value="MOM">环比</el-radio-button>
              </el-radio-group>
            </div>
          </template>
          <div ref="trendChartRef" class="chart-container" />
        </el-card>
      </el-col>
    </el-row>

    <!-- 明细数据表格 -->
    <el-card shadow="never">
      <template #header><span>统计明细</span></template>
      <el-table :data="trendData" border stripe size="small" v-loading="loadingTrend">
        <el-table-column prop="timeDim" label="时间" width="120" />
        <el-table-column prop="intentionCount" label="意向总数" align="right" />
        <el-table-column prop="intentionSigned" label="已签意向" align="right" />
        <el-table-column prop="newIntention" label="新增意向" align="right" />
        <el-table-column label="签约率" align="right">
          <template #default="{ row }">{{ fmtRate(row.signedRate) }}</template>
        </el-table-column>
        <el-table-column v-if="filterForm.compareMode !== 'NONE'" prop="prevIntentionCount" label="对比期意向数" align="right" />
        <el-table-column v-if="filterForm.compareMode !== 'NONE'" label="增长率" align="right">
          <template #default="{ row }">
            <span :class="growthClass(row.growthRate)">{{ fmtDelta(row.growthRate) }}</span>
          </template>
        </el-table-column>
      </el-table>
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted, onUnmounted, nextTick } from 'vue'
import * as echarts from 'echarts'
import { getFunnel, getIntentionStats } from '@/api/rpt/investment'
import { getProjectList } from '@/api/base/project'
import type { FunnelVO, IntentionStatsVO } from '@/api/rpt/investment'

const loading = ref(false)
const loadingTrend = ref(false)
const funnelData = ref<FunnelVO[]>([])
const trendData = ref<IntentionStatsVO[]>([])
const projectList = ref<{ id: number; projectName: string; projectCode: string }[]>([])
const filterForm = reactive({
  projectId: null as number | null,
  statDate: undefined as string | undefined,
  timeUnit: 'MONTH' as 'DAY' | 'MONTH' | 'YEAR',
  compareMode: 'NONE' as 'NONE' | 'YOY' | 'MOM',
})

const funnelChartRef = ref<HTMLDivElement>()
const trendChartRef = ref<HTMLDivElement>()
let funnelChart: echarts.ECharts | null = null
let trendChart: echarts.ECharts | null = null

onMounted(async () => {
  await loadProjects()
  await loadAll()
  window.addEventListener('resize', handleResize)
})
onUnmounted(() => {
  funnelChart?.dispose()
  trendChart?.dispose()
  window.removeEventListener('resize', handleResize)
})
function handleResize() {
  funnelChart?.resize()
  trendChart?.resize()
}

async function loadProjects() {
  projectList.value = await getProjectList()
}

async function loadAll() {
  loading.value = true
  try {
    const base = {
      projectId: filterForm.projectId ?? undefined,
      statDate: filterForm.statDate,
    }
    const [funnel] = await Promise.all([
      getFunnel(base),
      loadTrend(),
    ])
    funnelData.value = funnel
    await nextTick()
    updateFunnelChart()
  } finally {
    loading.value = false
  }
}

async function loadTrend() {
  loadingTrend.value = true
  try {
    trendData.value = await getIntentionStats({
      projectId: filterForm.projectId ?? undefined,
      timeUnit: filterForm.timeUnit,
      compareMode: filterForm.compareMode,
    })
    await nextTick()
    updateTrendChart()
  } finally {
    loadingTrend.value = false
  }
}

function resetFilter() {
  filterForm.projectId = null
  filterForm.statDate = undefined
  filterForm.timeUnit = 'MONTH'
  filterForm.compareMode = 'NONE'
  loadAll()
}

function updateFunnelChart() {
  if (!funnelChart && funnelChartRef.value) funnelChart = echarts.init(funnelChartRef.value)
  if (!funnelChart) return
  funnelChart.setOption({
    tooltip: {
      trigger: 'item',
      formatter: (p: any) => {
        const d = funnelData.value[p.dataIndex]
        if (!d) return ''
        let txt = `<b>${p.name}</b><br/>数量：${d.count ?? '-'}`
        if (d.conversionRate != null) txt += `<br/>阶段转化率：${fmtRate(d.conversionRate)}`
        if (d.overallConversionRate != null) txt += `<br/>综合转化率：${fmtRate(d.overallConversionRate)}`
        return txt
      },
    },
    series: [
      {
        type: 'funnel',
        left: '5%',
        width: '90%',
        top: 20,
        bottom: 10,
        sort: 'descending',
        gap: 8,
        label: { show: true, position: 'inside', fontSize: 13, formatter: '{b}\n{c}' },
        itemStyle: { borderWidth: 0 },
        data: funnelData.value.map(f => ({ name: f.stageName, value: f.count ?? 0 })),
        color: ['#409eff', '#67c23a', '#e6a23c'],
      },
    ],
  })
}

function updateTrendChart() {
  if (!trendChart && trendChartRef.value) trendChart = echarts.init(trendChartRef.value)
  if (!trendChart) return
  const data = trendData.value
  const hasPrev = filterForm.compareMode !== 'NONE'
  const compareName = filterForm.compareMode === 'YOY' ? '同比意向数' : '环比意向数'
  trendChart.setOption({
    tooltip: { trigger: 'axis' },
    legend: {
      data: ['意向总数', '新增意向', ...(hasPrev ? [compareName] : [])],
      top: 0,
    },
    grid: { left: 45, right: 20, top: 40, bottom: 30 },
    xAxis: { type: 'category', data: data.map(d => d.timeDim), axisLabel: { rotate: 30 } },
    yAxis: { type: 'value', minInterval: 1 },
    series: [
      {
        name: '意向总数',
        type: 'bar',
        data: data.map(d => d.intentionCount),
        itemStyle: { color: '#409eff' },
      },
      {
        name: '新增意向',
        type: 'line',
        smooth: true,
        data: data.map(d => d.newIntention),
        itemStyle: { color: '#e6a23c' },
        areaStyle: { color: 'rgba(230,162,60,0.1)' },
      },
      ...(hasPrev
        ? [{
            name: compareName,
            type: 'line' as const,
            smooth: true,
            data: data.map(d => d.prevIntentionCount),
            itemStyle: { color: '#909399' },
            lineStyle: { type: 'dashed' as const },
          }]
        : []),
    ],
  })
}

function fmtRate(v?: number | null) {
  return v != null ? `${Number(v).toFixed(2)}%` : '-'
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
.inv-funnel {
  display: flex;
  flex-direction: column;
  gap: 16px;
}
.chart-container { height: 300px; width: 100%; }
.conversion-row {
  margin-top: 12px;
  .conversion-item {
    text-align: center;
    padding: 4px 0;
    border-right: 1px solid #ebeef5;
    &:last-child { border-right: none; }
    .stage-name { font-size: 12px; color: #909399; }
    .stage-count { font-size: 22px; font-weight: 700; color: #303133; margin: 4px 0; }
    .stage-rate, .stage-total { font-size: 12px; color: #606266; }
    b { color: #409eff; }
  }
}
.text-up { color: #67c23a; }
.text-down { color: #f56c6c; }
</style>
