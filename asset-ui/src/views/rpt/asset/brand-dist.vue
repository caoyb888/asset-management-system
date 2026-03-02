<template>
  <div class="rpt-brand-dist">
    <!-- 筛选栏 -->
    <el-card class="filter-card" shadow="never">
      <el-form inline :model="filterForm">
        <el-form-item label="项目">
          <el-select v-model="filterForm.projectId" placeholder="全部项目" clearable style="width: 180px">
            <el-option v-for="p in projectList" :key="p.id" :label="p.projectName" :value="p.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="统计日期">
          <el-date-picker
            v-model="filterForm.statDate"
            type="date"
            placeholder="不选则取最新"
            value-format="YYYY-MM-DD"
            style="width: 160px"
          />
        </el-form-item>
        <el-form-item label="图表类型">
          <el-radio-group v-model="chartType">
            <el-radio-button label="pie">饼图</el-radio-button>
            <el-radio-button label="treemap">树状图</el-radio-button>
          </el-radio-group>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" :loading="loading" @click="loadData">查询</el-button>
          <el-button @click="resetFilter">重置</el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <el-row :gutter="16">
      <!-- 图表 -->
      <el-col :span="12">
        <el-card shadow="never">
          <template #header>
            <span>业态分布（{{ chartType === 'pie' ? '按商铺数' : '按面积' }}）</span>
          </template>
          <div ref="chartRef" class="chart-container" />
        </el-card>
      </el-col>

      <!-- 数据表格 -->
      <el-col :span="12">
        <el-card shadow="never">
          <template #header>
            <span>业态明细</span>
            <span class="total-tip">共 {{ brandData.length }} 个业态</span>
          </template>
          <el-table :data="brandData" border stripe size="small">
            <el-table-column prop="formatName" label="业态" min-width="100">
              <template #default="{ row }">
                {{ row.formatName || row.formatType || '未分类' }}
              </template>
            </el-table-column>
            <el-table-column prop="totalShops" label="商铺总数" min-width="80" align="right" />
            <el-table-column prop="rentedShops" label="已租商铺" min-width="80" align="right" />
            <el-table-column label="商铺占比" min-width="120">
              <template #default="{ row }">
                <div class="pct-bar">
                  <el-progress
                    :percentage="row.shopPercentage ?? 0"
                    :format="() => `${(row.shopPercentage ?? 0).toFixed(1)}%`"
                    :stroke-width="8"
                    color="#409eff"
                  />
                </div>
              </template>
            </el-table-column>
            <el-table-column label="面积(㎡)" min-width="100" align="right">
              <template #default="{ row }">
                {{ row.totalArea != null ? row.totalArea.toFixed(2) : '-' }}
              </template>
            </el-table-column>
            <el-table-column label="面积占比" min-width="120">
              <template #default="{ row }">
                <div class="pct-bar">
                  <el-progress
                    :percentage="row.areaPercentage ?? 0"
                    :format="() => `${(row.areaPercentage ?? 0).toFixed(1)}%`"
                    :stroke-width="8"
                    color="#67c23a"
                  />
                </div>
              </template>
            </el-table-column>
          </el-table>
        </el-card>
      </el-col>
    </el-row>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, watch, onMounted, onUnmounted, nextTick } from 'vue'
import * as echarts from 'echarts'
import { getBrandDistribution } from '@/api/rpt/asset'
import { getProjectList } from '@/api/base/project'
import type { BrandDistributionVO, AssetQueryParam } from '@/api/rpt/asset'

const loading = ref(false)
const brandData = ref<BrandDistributionVO[]>([])
const projectList = ref<{ id: number; projectName: string; projectCode: string }[]>([])
const chartType = ref<'pie' | 'treemap'>('pie')
const filterForm = reactive<AssetQueryParam>({ projectId: null, statDate: undefined })

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

watch(chartType, () => updateChart())

async function loadData() {
  loading.value = true
  try {
    brandData.value = await getBrandDistribution({
      projectId: filterForm.projectId,
      statDate: filterForm.statDate,
    })
    await nextTick()
    updateChart()
  } finally {
    loading.value = false
  }
}

function resetFilter() {
  filterForm.projectId = null
  filterForm.statDate = undefined
  loadData()
}

function updateChart() {
  if (!chart) {
    if (chartRef.value) chart = echarts.init(chartRef.value)
    else return
  }
  const items = brandData.value.map(d => ({
    name: d.formatName || d.formatType || '未分类',
    shopValue: d.totalShops ?? 0,
    areaValue: d.totalArea ?? 0,
  }))

  if (chartType.value === 'pie') {
    chart.setOption({
      tooltip: {
        trigger: 'item',
        formatter: (params: any) => `${params.name}<br/>商铺数: ${params.value} (${params.percent}%)`,
      },
      legend: { orient: 'vertical', left: 'left', type: 'scroll' },
      series: [
        {
          name: '业态分布',
          type: 'pie',
          radius: ['40%', '70%'],
          center: ['60%', '50%'],
          data: items.map(i => ({ name: i.name, value: i.shopValue })),
          emphasis: { itemStyle: { shadowBlur: 10, shadowOffsetX: 0, shadowColor: 'rgba(0,0,0,0.5)' } },
          label: { formatter: '{b}\n{d}%' },
        },
      ],
    })
  } else {
    chart.setOption({
      tooltip: {
        formatter: (params: any) => `${params.name}<br/>面积: ${params.value?.toFixed(2)}㎡`,
      },
      series: [
        {
          name: '业态分布',
          type: 'treemap',
          roam: false,
          data: items.map(i => ({ name: i.name, value: i.areaValue })),
          label: { show: true, formatter: '{b}\n{c}㎡' },
          breadcrumb: { show: false },
        },
      ],
    })
  }
}
</script>

<style scoped lang="scss">
.rpt-brand-dist {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.chart-container {
  height: 400px;
  width: 100%;
}

.total-tip {
  margin-left: 8px;
  font-size: 12px;
  color: #909399;
}

.pct-bar {
  padding-right: 8px;
}
</style>
