<template>
  <div class="page-container">
    <!-- 统计卡片 -->
    <div class="stat-cards">
      <el-card v-for="card in statCards" :key="card.label" shadow="never" class="stat-card">
        <div class="stat-card__value" :style="{ color: card.color }">{{ card.value }}</div>
        <div class="stat-card__label">{{ card.label }}</div>
      </el-card>
    </div>

    <!-- 图表区域 -->
    <div class="chart-row">
      <el-card shadow="never" class="chart-card">
        <template #header>状态分布</template>
        <div ref="pieChartRef" class="chart-container" />
      </el-card>
      <el-card shadow="never" class="chart-card">
        <template #header>审批效率</template>
        <div ref="gaugeChartRef" class="chart-container" />
      </el-card>
    </div>

    <!-- 流程监控列表 -->
    <el-card shadow="never">
      <template #header>
        <div class="card-header">
          <span>流程实例列表</span>
          <el-form :model="query" inline class="filter-inline">
            <el-form-item>
              <el-select v-model="query.businessType" clearable placeholder="业务类型" style="width: 160px">
                <el-option v-for="opt in BUSINESS_TYPE_OPTIONS" :key="opt.value" :label="opt.label" :value="opt.value" />
              </el-select>
            </el-form-item>
            <el-form-item>
              <el-select v-model="query.status" clearable placeholder="状态" style="width: 120px">
                <el-option v-for="(label, val) in STATUS_LABEL_MAP" :key="val" :label="label" :value="Number(val)" />
              </el-select>
            </el-form-item>
            <el-form-item>
              <el-button type="primary" @click="loadList">查询</el-button>
            </el-form-item>
          </el-form>
        </div>
      </template>

      <el-table :data="listData" v-loading="listLoading" stripe>
        <el-table-column label="标题" prop="title" min-width="200" show-overflow-tooltip />
        <el-table-column label="业务类型" width="140">
          <template #default="{ row }">
            <el-tag size="small" type="info">{{ row.businessTypeName }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="发起人" prop="initiatorName" width="100" />
        <el-table-column label="状态" width="100">
          <template #default="{ row }">
            <el-tag :type="STATUS_TAG_MAP[row.status]" size="small">{{ row.statusName }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="当前节点" prop="currentNodeName" width="120" />
        <el-table-column label="耗时" width="100">
          <template #default="{ row }">
            {{ row.durationMs ? formatDuration(row.durationMs) : '-' }}
          </template>
        </el-table-column>
        <el-table-column label="发起时间" prop="startedAt" width="170" />
        <el-table-column label="操作" width="100" fixed="right">
          <template #default="{ row }">
            <el-button
              v-if="row.status <= 1"
              type="danger"
              link
              size="small"
              @click="handleCancel(row)"
            >作废</el-button>
          </template>
        </el-table-column>
      </el-table>

      <el-pagination
        v-if="listTotal > 0"
        class="pagination"
        :current-page="query.pageNum"
        :page-size="query.pageSize"
        :total="listTotal"
        :page-sizes="[10, 20, 50]"
        layout="total, sizes, prev, pager, next"
        @current-change="(val: number) => { query.pageNum = val; loadList() }"
        @size-change="(val: number) => { query.pageSize = val; query.pageNum = 1; loadList() }"
      />
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, computed, onMounted, nextTick } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import * as echarts from 'echarts'
import { getStatistics, getProcessPage, cancelProcess, type ProcessInstanceVO, type ApprovalStatistics } from '@/api/workflow/process'
import { BUSINESS_TYPE_OPTIONS } from '@/api/workflow/task'

const pieChartRef = ref<HTMLDivElement>()
const gaugeChartRef = ref<HTMLDivElement>()

const stats = ref<ApprovalStatistics | null>(null)

type TagType = 'primary' | 'success' | 'warning' | 'info' | 'danger'
const STATUS_TAG_MAP: Record<number, TagType> = {
  0: 'info', 1: 'warning', 2: 'success', 3: 'danger', 4: 'info', 5: 'info',
}
const STATUS_LABEL_MAP: Record<number, string> = {
  0: '待审批', 1: '审批中', 2: '已通过', 3: '已驳回', 4: '已撤回', 5: '已作废',
}

const statCards = computed(() => {
  const s = stats.value
  if (!s) return []
  return [
    { label: '总流程数', value: s.total, color: '#409eff' },
    { label: '审批中', value: (s.pendingCount || 0) + (s.in_progressCount || 0), color: '#e6a23c' },
    { label: '通过率', value: s.approvalRate + '%', color: '#67c23a' },
    { label: '平均耗时', value: formatDuration(s.avgDurationMs), color: '#909399' },
  ]
})

// 流程列表
const listLoading = ref(false)
const listData = ref<ProcessInstanceVO[]>([])
const listTotal = ref(0)
const query = reactive({
  businessType: undefined as string | undefined,
  status: undefined as number | undefined,
  pageNum: 1,
  pageSize: 10,
})

onMounted(async () => {
  await Promise.all([loadStats(), loadList()])
})

async function loadStats() {
  try {
    stats.value = await getStatistics() as any
    await nextTick()
    renderCharts()
  } catch {
    // handled
  }
}

async function loadList() {
  listLoading.value = true
  try {
    const res = await getProcessPage(query)
    listData.value = res.records
    listTotal.value = res.total
  } catch {
    // handled
  } finally {
    listLoading.value = false
  }
}

function renderCharts() {
  const s = stats.value
  if (!s) return

  // 状态分布饼图
  if (pieChartRef.value) {
    const pie = echarts.init(pieChartRef.value)
    pie.setOption({
      tooltip: { trigger: 'item', formatter: '{b}: {c} ({d}%)' },
      legend: { bottom: 0 },
      series: [{
        type: 'pie',
        radius: ['40%', '70%'],
        avoidLabelOverlap: true,
        label: { show: true, formatter: '{b}\n{c}' },
        data: [
          { value: (s.pendingCount || 0) + (s.in_progressCount || 0), name: '审批中', itemStyle: { color: '#e6a23c' } },
          { value: s.approvedCount || 0, name: '已通过', itemStyle: { color: '#67c23a' } },
          { value: s.rejectedCount || 0, name: '已驳回', itemStyle: { color: '#f56c6c' } },
          { value: (s.revokedCount || 0) + (s.cancelledCount || 0), name: '已撤回/作废', itemStyle: { color: '#909399' } },
        ].filter(d => d.value > 0),
      }],
    })
  }

  // 通过率仪表盘
  if (gaugeChartRef.value) {
    const gauge = echarts.init(gaugeChartRef.value)
    gauge.setOption({
      series: [{
        type: 'gauge',
        startAngle: 200,
        endAngle: -20,
        min: 0,
        max: 100,
        progress: { show: true, width: 18, itemStyle: { color: '#67c23a' } },
        pointer: { show: false },
        axisLine: { lineStyle: { width: 18, color: [[1, '#e4e7ed']] } },
        axisTick: { show: false },
        splitLine: { show: false },
        axisLabel: { show: false },
        title: { offsetCenter: [0, '30%'], fontSize: 14, color: '#606266' },
        detail: {
          offsetCenter: [0, '-10%'],
          fontSize: 36,
          fontWeight: 'bold',
          color: '#303133',
          formatter: '{value}%',
        },
        data: [{ value: s.approvalRate || 0, name: '审批通过率' }],
      }],
    })
  }
}

async function handleCancel(row: ProcessInstanceVO) {
  try {
    await ElMessageBox.confirm(`确认作废流程「${row.title}」？`, '作废确认', {
      type: 'warning',
      confirmButtonText: '确认作废',
    })
    await cancelProcess(row.id)
    ElMessage.success('已作废')
    loadStats()
    loadList()
  } catch {
    // cancelled
  }
}

function formatDuration(ms: number): string {
  if (!ms || ms <= 0) return '-'
  const hours = Math.floor(ms / 3600000)
  const minutes = Math.floor((ms % 3600000) / 60000)
  if (hours > 24) return Math.floor(hours / 24) + '天' + (hours % 24) + '时'
  if (hours > 0) return hours + '时' + minutes + '分'
  return minutes + '分'
}
</script>

<style scoped>
.page-container {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.stat-cards {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 12px;
}

.stat-card {
  text-align: center;
}

.stat-card__value {
  font-size: 32px;
  font-weight: 700;
  line-height: 1.2;
}

.stat-card__label {
  font-size: 14px;
  color: #909399;
  margin-top: 4px;
}

.chart-row {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 12px;
}

.chart-container {
  height: 300px;
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  flex-wrap: wrap;
  gap: 8px;
}

.filter-inline {
  margin-bottom: -18px;
}

.pagination {
  margin-top: 12px;
  justify-content: flex-end;
}
</style>
