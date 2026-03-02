<template>
  <div class="rpt-vacancy">
    <!-- 筛选栏 -->
    <el-card class="filter-card" shadow="never">
      <el-form inline :model="filterForm">
        <el-form-item label="项目">
          <el-select v-model="filterForm.projectId" placeholder="全部项目" clearable style="width: 180px">
            <el-option v-for="p in projectList" :key="p.id" :label="p.projectName" :value="p.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="日期范围">
          <el-date-picker
            v-model="dateRange"
            type="daterange"
            range-separator="至"
            start-placeholder="开始日期"
            end-placeholder="结束日期"
            value-format="YYYY-MM-DD"
            style="width: 240px"
          />
        </el-form-item>
        <el-form-item label="时间粒度">
          <el-select v-model="filterForm.timeUnit" style="width: 100px">
            <el-option label="日" value="DAY" />
            <el-option label="周" value="WEEK" />
            <el-option label="月" value="MONTH" />
            <el-option label="年" value="YEAR" />
          </el-select>
        </el-form-item>
        <el-form-item label="对比模式">
          <el-select v-model="filterForm.compareMode" style="width: 100px">
            <el-option label="无对比" value="NONE" />
            <el-option label="同比(YoY)" value="YOY" />
            <el-option label="环比(MoM)" value="MOM" />
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" :loading="loading" @click="loadData">查询</el-button>
          <el-button @click="resetFilter">重置</el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <!-- 趋势图 -->
    <el-card shadow="never">
      <template #header>
        <span>空置率趋势</span>
        <el-tag v-if="filterForm.compareMode !== 'NONE'" type="warning" size="small" style="margin-left: 8px">
          {{ filterForm.compareMode === 'YOY' ? '同比对比中' : '环比对比中' }}
        </el-tag>
      </template>
      <div ref="chartRef" class="chart-container" />
    </el-card>

    <!-- 数据表格 -->
    <el-card shadow="never">
      <template #header>
        <span>明细数据</span>
        <span class="total-tip">共 {{ trendData.length }} 条</span>
      </template>
      <el-table :data="trendData" border stripe size="small">
        <el-table-column prop="timeDim" label="时间" min-width="120" />
        <el-table-column label="空置率" min-width="100">
          <template #default="{ row }">
            <span :class="row.value > 20 ? 'text-danger' : ''">
              {{ row.value != null ? row.value.toFixed(2) + '%' : '-' }}
            </span>
          </template>
        </el-table-column>
        <el-table-column v-if="filterForm.compareMode !== 'NONE'" label="上期值" min-width="100">
          <template #default="{ row }">
            {{ row.prevValue != null ? row.prevValue.toFixed(2) + '%' : '-' }}
          </template>
        </el-table-column>
        <el-table-column v-if="filterForm.compareMode !== 'NONE'" label="增长率" min-width="100">
          <template #default="{ row }">
            <span v-if="row.growthRate != null" :class="row.growthRate > 0 ? 'text-danger' : 'text-success'">
              {{ row.growthRate > 0 ? '+' : '' }}{{ row.growthRate.toFixed(2) }}%
            </span>
            <span v-else>-</span>
          </template>
        </el-table-column>
        <el-table-column prop="totalShops" label="商铺总数" min-width="90" />
        <el-table-column label="总面积(㎡)" min-width="110">
          <template #default="{ row }">
            {{ row.totalArea != null ? row.totalArea.toFixed(2) : '-' }}
          </template>
        </el-table-column>
      </el-table>
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, computed, onMounted, onUnmounted, nextTick, watch } from 'vue'
import * as echarts from 'echarts'
import { getVacancyRate } from '@/api/rpt/asset'
import { getProjectList } from '@/api/base/project'
import type { RateTrendVO, AssetQueryParam } from '@/api/rpt/asset'

const loading = ref(false)
const trendData = ref<RateTrendVO[]>([])
const projectList = ref<{ id: number; projectName: string; projectCode: string }[]>([])
const dateRange = ref<[string, string] | null>(null)

const filterForm = reactive<AssetQueryParam>({
  projectId: null,
  timeUnit: 'DAY',
  compareMode: 'NONE',
})

const chartRef = ref<HTMLDivElement>()
let chart: echarts.ECharts | null = null

onMounted(async () => {
  projectList.value = await getProjectList()
  await loadData()
  if (chartRef.value) chart = echarts.init(chartRef.value)
  updateChart()
  window.addEventListener('resize', handleResize)
})

onUnmounted(() => {
  chart?.dispose()
  window.removeEventListener('resize', handleResize)
})

function handleResize() { chart?.resize() }

async function loadData() {
  loading.value = true
  try {
    const params: AssetQueryParam = {
      ...filterForm,
      startDate: dateRange.value?.[0],
      endDate: dateRange.value?.[1],
    }
    trendData.value = await getVacancyRate(params)
    await nextTick()
    updateChart()
  } finally {
    loading.value = false
  }
}

function resetFilter() {
  filterForm.projectId = null
  filterForm.timeUnit = 'DAY'
  filterForm.compareMode = 'NONE'
  dateRange.value = null
  loadData()
}

function updateChart() {
  if (!chart) {
    if (chartRef.value) chart = echarts.init(chartRef.value)
    else return
  }
  const dates = trendData.value.map(d => d.timeDim)
  const hasCompare = filterForm.compareMode !== 'NONE'
  const series: echarts.SeriesOption[] = [
    {
      name: '空置率',
      type: 'line',
      smooth: true,
      data: trendData.value.map(d => d.value),
      itemStyle: { color: '#f56c6c' },
      areaStyle: { color: 'rgba(245,108,108,0.1)' },
      markLine: {
        data: [{ type: 'average', name: '平均值' }],
        lineStyle: { color: '#f56c6c', type: 'dashed' },
      },
    },
  ]
  if (hasCompare) {
    series.push({
      name: filterForm.compareMode === 'YOY' ? '同期空置率' : '上期空置率',
      type: 'line',
      smooth: true,
      data: trendData.value.map(d => d.prevValue),
      itemStyle: { color: '#e6a23c' },
      lineStyle: { type: 'dashed' },
    })
  }

  chart.setOption({
    tooltip: { trigger: 'axis', valueFormatter: (v: number) => v != null ? `${v.toFixed(2)}%` : '-' },
    legend: { data: series.map(s => s.name as string), top: 0 },
    grid: { left: 50, right: 20, top: 40, bottom: 30 },
    xAxis: { type: 'category', data: dates },
    yAxis: { type: 'value', axisLabel: { formatter: '{value}%' }, min: 0 },
    series,
  })
}
</script>

<style scoped lang="scss">
.rpt-vacancy {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.chart-container {
  height: 320px;
  width: 100%;
}

.total-tip {
  margin-left: 8px;
  font-size: 12px;
  color: #909399;
}

.text-danger { color: #f56c6c; }
.text-success { color: #67c23a; }
</style>
