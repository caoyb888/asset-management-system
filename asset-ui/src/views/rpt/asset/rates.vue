<template>
  <div class="rpt-rates">
    <!-- 指标切换 Tab -->
    <el-card shadow="never" class="tab-card">
      <el-tabs v-model="activeTab" @tab-change="onTabChange">
        <el-tab-pane label="出租率" name="rental" />
        <el-tab-pane label="开业率" name="opening" />
      </el-tabs>
    </el-card>

    <!-- 筛选栏 -->
    <el-card class="filter-card" shadow="never">
      <el-form inline :model="filterForm">
        <el-form-item label="项目">
          <el-select
            v-model="filterForm.projectId"
            placeholder="全部项目"
            clearable
            style="width: 180px"
            @change="onProjectChange"
          >
            <el-option v-for="p in projectList" :key="p.id" :label="p.projectName" :value="p.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="楼栋">
          <el-select
            v-model="filterForm.buildingId"
            placeholder="全部楼栋"
            clearable
            style="width: 140px"
          >
            <el-option v-for="b in buildingList" :key="b.id" :label="b.buildingName" :value="b.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="日期范围">
          <el-date-picker
            v-model="dateRange"
            type="daterange"
            range-separator="至"
            start-placeholder="开始"
            end-placeholder="结束"
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
          <el-select v-model="filterForm.compareMode" style="width: 110px">
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
        <span>{{ activeTab === 'rental' ? '出租率' : '开业率' }}趋势</span>
        <el-tag v-if="filterForm.compareMode !== 'NONE'" type="warning" size="small" style="margin-left: 8px">
          {{ filterForm.compareMode === 'YOY' ? '同比对比中' : '环比对比中' }}
        </el-tag>
      </template>
      <div ref="chartRef" class="chart-container" />
    </el-card>

    <!-- 数据摘要 -->
    <el-row :gutter="16">
      <el-col :span="8">
        <el-card shadow="never" class="summary-card">
          <div class="summary-label">当前值</div>
          <div class="summary-value primary">{{ latestValue }}</div>
        </el-card>
      </el-col>
      <el-col :span="8">
        <el-card shadow="never" class="summary-card">
          <div class="summary-label">区间最高</div>
          <div class="summary-value success">{{ maxValue }}</div>
        </el-card>
      </el-col>
      <el-col :span="8">
        <el-card shadow="never" class="summary-card">
          <div class="summary-label">区间最低</div>
          <div class="summary-value warning">{{ minValue }}</div>
        </el-card>
      </el-col>
    </el-row>

    <!-- 明细表格 -->
    <el-card shadow="never">
      <template #header>
        <span>明细数据</span>
        <span class="total-tip">共 {{ trendData.length }} 条</span>
      </template>
      <el-table :data="trendData" border stripe size="small" max-height="400">
        <el-table-column prop="timeDim" label="时间" min-width="120" />
        <el-table-column :label="activeTab === 'rental' ? '出租率' : '开业率'" min-width="100">
          <template #default="{ row }">
            <el-progress
              v-if="row.value != null"
              :percentage="Math.min(row.value, 100)"
              :format="() => `${row.value.toFixed(2)}%`"
              :color="activeTab === 'rental' ? '#409eff' : '#67c23a'"
              :stroke-width="8"
            />
            <span v-else>-</span>
          </template>
        </el-table-column>
        <el-table-column v-if="filterForm.compareMode !== 'NONE'" label="上期值" min-width="100">
          <template #default="{ row }">
            {{ row.prevValue != null ? row.prevValue.toFixed(2) + '%' : '-' }}
          </template>
        </el-table-column>
        <el-table-column v-if="filterForm.compareMode !== 'NONE'" label="增长率" min-width="100">
          <template #default="{ row }">
            <span v-if="row.growthRate != null" :class="row.growthRate >= 0 ? 'text-success' : 'text-danger'">
              {{ row.growthRate >= 0 ? '+' : '' }}{{ row.growthRate.toFixed(2) }}%
            </span>
            <span v-else>-</span>
          </template>
        </el-table-column>
        <el-table-column prop="totalShops" label="商铺总数" min-width="90" />
        <el-table-column label="已租商铺" min-width="90">
          <template #default="{ row }">{{ row.rentedShops ?? '-' }}</template>
        </el-table-column>
      </el-table>
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, computed, onMounted, onUnmounted, nextTick } from 'vue'
import * as echarts from 'echarts'
import { getRentalRate, getOpeningRate } from '@/api/rpt/asset'
import { getProjectList } from '@/api/base/project'
import { getBuildingList } from '@/api/base/building'
import type { RateTrendVO, AssetQueryParam } from '@/api/rpt/asset'

const loading = ref(false)
const activeTab = ref<'rental' | 'opening'>('rental')
const trendData = ref<RateTrendVO[]>([])
const projectList = ref<{ id: number; projectName: string; projectCode: string }[]>([])
const buildingList = ref<{ id: number; buildingName: string }[]>([])
const dateRange = ref<[string, string] | null>(null)

const filterForm = reactive<AssetQueryParam>({
  projectId: null,
  buildingId: null,
  timeUnit: 'DAY',
  compareMode: 'NONE',
})

const chartRef = ref<HTMLDivElement>()
let chart: echarts.ECharts | null = null

// 统计摘要
const validValues = computed(() => trendData.value.map(d => d.value).filter(v => v != null) as number[])
const latestValue = computed(() => {
  const v = validValues.value[validValues.value.length - 1]
  return v != null ? `${v.toFixed(2)}%` : '-'
})
const maxValue = computed(() => {
  if (!validValues.value.length) return '-'
  return `${Math.max(...validValues.value).toFixed(2)}%`
})
const minValue = computed(() => {
  if (!validValues.value.length) return '-'
  return `${Math.min(...validValues.value).toFixed(2)}%`
})

onMounted(async () => {
  projectList.value = await getProjectList()
  await loadData()
  if (chartRef.value) chart = echarts.init(chartRef.value)
  updateChart()
  window.addEventListener('resize', handleResize)
})

async function onProjectChange(val: number | null) {
  filterForm.buildingId = null
  buildingList.value = val ? await getBuildingList(val) : []
}

onUnmounted(() => {
  chart?.dispose()
  window.removeEventListener('resize', handleResize)
})

function handleResize() { chart?.resize() }

async function onTabChange() {
  await loadData()
}

async function loadData() {
  loading.value = true
  try {
    const params: AssetQueryParam = {
      ...filterForm,
      startDate: dateRange.value?.[0],
      endDate: dateRange.value?.[1],
    }
    if (activeTab.value === 'rental') {
      trendData.value = await getRentalRate(params)
    } else {
      trendData.value = await getOpeningRate(params)
    }
    await nextTick()
    updateChart()
  } finally {
    loading.value = false
  }
}

function resetFilter() {
  filterForm.projectId = null
  filterForm.buildingId = null
  filterForm.timeUnit = 'DAY'
  filterForm.compareMode = 'NONE'
  dateRange.value = null
  buildingList.value = []
  loadData()
}

function updateChart() {
  if (!chart) {
    if (chartRef.value) chart = echarts.init(chartRef.value)
    else return
  }
  const color = activeTab.value === 'rental' ? '#409eff' : '#67c23a'
  const label = activeTab.value === 'rental' ? '出租率' : '开业率'
  const dates = trendData.value.map(d => d.timeDim)
  const hasCompare = filterForm.compareMode !== 'NONE'

  const series: echarts.SeriesOption[] = [
    {
      name: label,
      type: 'line',
      smooth: true,
      data: trendData.value.map(d => d.value),
      itemStyle: { color },
      areaStyle: { color: color + '1a' },
      markLine: {
        data: [{ type: 'average', name: '均值' }],
        lineStyle: { color, type: 'dashed' },
      },
    },
  ]
  if (hasCompare) {
    const cmpLabel = filterForm.compareMode === 'YOY' ? `同期${label}` : `上期${label}`
    series.push({
      name: cmpLabel,
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
    yAxis: { type: 'value', axisLabel: { formatter: '{value}%' }, min: 0, max: 100 },
    series,
  })
}
</script>

<style scoped lang="scss">
.rpt-rates {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.tab-card :deep(.el-card__body) {
  padding-bottom: 0;
}

.chart-container {
  height: 320px;
  width: 100%;
}

.summary-card {
  text-align: center;
  .summary-label { font-size: 12px; color: #909399; margin-bottom: 8px; }
  .summary-value { font-size: 28px; font-weight: 700; }
  .primary { color: #409eff; }
  .success { color: #67c23a; }
  .warning { color: #e6a23c; }
}

.total-tip {
  margin-left: 8px;
  font-size: 12px;
  color: #909399;
}

.text-danger { color: #f56c6c; }
.text-success { color: #67c23a; }
</style>
