<template>
  <div class="flow-page">
    <!-- 统计卡片 -->
    <el-row :gutter="16" class="stat-row">
      <el-col :span="6">
        <el-card shadow="never" class="stat-card">
          <div class="stat-title">今日客流</div>
          <div class="stat-num primary">{{ stats.todayFlow ?? '-' }}</div>
          <div class="stat-sub">
            <span>日环比：</span>
            <span v-if="stats.dayOverDayRate != null" :class="rateClass(stats.dayOverDayRate)">
              {{ formatRate(stats.dayOverDayRate) }}
            </span>
            <span v-else class="gray">暂无数据</span>
          </div>
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card shadow="never" class="stat-card">
          <div class="stat-title">本周客流</div>
          <div class="stat-num success">{{ stats.thisWeekFlow ?? '-' }}</div>
          <div class="stat-sub">
            <span>周环比：</span>
            <span v-if="stats.weekOverWeekRate != null" :class="rateClass(stats.weekOverWeekRate)">
              {{ formatRate(stats.weekOverWeekRate) }}
            </span>
            <span v-else class="gray">暂无数据</span>
          </div>
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card shadow="never" class="stat-card">
          <div class="stat-title">近30天客流</div>
          <div class="stat-num warning">{{ stats.last30DaysFlow ?? '-' }}</div>
          <div class="stat-sub gray">过去30天累计</div>
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card shadow="never" class="stat-card">
          <div class="stat-title">昨日客流</div>
          <div class="stat-num info">{{ stats.yesterdayFlow ?? '-' }}</div>
          <div class="stat-sub gray">作为今日基准</div>
        </el-card>
      </el-col>
    </el-row>

    <!-- 趋势图 -->
    <el-card shadow="never" class="table-card">
      <div class="card-header">
        <div class="header-left">
          <span class="header-title">近30天客流趋势</span>
        </div>
        <div class="header-actions">
          <el-select v-model="statsFilter.projectId" placeholder="选择项目" clearable
            style="width:160px" @change="loadStats">
            <el-option v-for="p in projectList" :key="p.id" :label="p.projectName" :value="p.id" />
          </el-select>
        </div>
      </div>
      <div class="table-body">
        <div ref="chartRef" style="height:260px" />
      </div>
    </el-card>

    <!-- 搜索栏 -->
    <el-card shadow="never" class="filter-card">
      <el-form :model="query" inline>
        <el-form-item label="项目">
          <el-select v-model="query.projectId" placeholder="全部" clearable style="width:150px"
            @change="onProjectChange">
            <el-option v-for="p in projectList" :key="p.id" :label="p.projectName" :value="p.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="楼栋">
          <el-select v-model="query.buildingId" placeholder="全部" clearable style="width:130px"
            :disabled="!query.projectId" @change="onBuildingChange">
            <el-option v-for="b in buildingList" :key="b.id" :label="b.buildingName" :value="b.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="楼层">
          <el-select v-model="query.floorId" placeholder="全部" clearable style="width:110px"
            :disabled="!query.buildingId">
            <el-option v-for="f in floorList" :key="f.id" :label="f.floorName" :value="f.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="日期">
          <el-date-picker v-model="dateRange" type="daterange" value-format="YYYY-MM-DD"
            range-separator="~" start-placeholder="开始" end-placeholder="结束" style="width:220px" />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" :icon="Search" @click="loadList">查询</el-button>
          <el-button @click="resetQuery">重置</el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <!-- 列表卡片 -->
    <el-card shadow="never" class="table-card">
      <div class="card-header">
        <div class="header-left">
          <span class="header-title">客流填报列表</span>
          <span class="count-tag">共 {{ total }} 条</span>
        </div>
        <div class="header-actions">
          <el-upload :show-file-list="false" accept=".xlsx,.xls" :before-upload="doImport">
            <el-button :icon="Upload">导入</el-button>
          </el-upload>
          <el-button :icon="Download" @click="doExport">导出</el-button>
          <el-button type="primary" :icon="Plus" @click="openForm(null)">新增填报</el-button>
        </div>
      </div>

      <div class="table-body">
      <el-table v-loading="loading" :data="list" border stripe>
        <el-table-column type="index" label="序号" width="60" align="center" />
        <el-table-column label="项目" min-width="140" show-overflow-tooltip>
          <template #default="{ row }">{{ getProjectName(row.projectId) }}</template>
        </el-table-column>
        <el-table-column label="楼栋" width="120" show-overflow-tooltip>
          <template #default="{ row }">{{ getBuildingName(row.buildingId) || '-' }}</template>
        </el-table-column>
        <el-table-column label="楼层" width="100">
          <template #default="{ row }">{{ getFloorName(row.floorId) || '-' }}</template>
        </el-table-column>
        <el-table-column label="填报日期" prop="reportDate" width="120" align="center" />
        <el-table-column label="客流人数" prop="flowCount" width="110" align="right">
          <template #default="{ row }">
            <span style="font-weight:600;color:#409eff">{{ row.flowCount?.toLocaleString() }}</span>
          </template>
        </el-table-column>
        <el-table-column label="数据来源" width="100" align="center">
          <template #default="{ row }">
            <el-tag :type="sourceTagType(row.sourceType)" size="small">
              {{ sourceLabel(row.sourceType) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="140" fixed="right" align="center">
          <template #default="{ row }">
            <el-button link type="primary" @click="openForm(row)">编辑</el-button>
            <el-popconfirm title="确认删除该条记录？" @confirm="doDelete(row.id)">
              <template #reference>
                <el-button link type="danger">删除</el-button>
              </template>
            </el-popconfirm>
          </template>
        </el-table-column>
      </el-table>

      <div class="pagination">
        <el-pagination
          v-model:current-page="query.pageNum"
          v-model:page-size="query.pageSize"
          :total="total"
          :page-sizes="[10, 20, 50]"
          layout="total, sizes, prev, pager, next, jumper"
          background
          @change="loadList"
        />
      </div>
      </div>
    </el-card>

    <!-- 新增/编辑弹窗 -->
    <el-dialog v-model="formVisible" :title="editingId ? '编辑客流填报' : '新增客流填报'" width="500px">
      <el-form ref="formRef" :model="form" :rules="formRules" label-width="90px">
        <el-form-item label="项目" prop="projectId">
          <el-select v-model="form.projectId" placeholder="请选择项目" style="width:100%"
            @change="onFormProjectChange">
            <el-option v-for="p in projectList" :key="p.id" :label="p.projectName" :value="p.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="楼栋">
          <el-select v-model="form.buildingId" placeholder="可选" clearable style="width:100%"
            :disabled="!form.projectId" @change="onFormBuildingChange">
            <el-option v-for="b in formBuildingList" :key="b.id" :label="b.buildingName" :value="b.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="楼层">
          <el-select v-model="form.floorId" placeholder="可选" clearable style="width:100%"
            :disabled="!form.buildingId">
            <el-option v-for="f in formFloorList" :key="f.id" :label="f.floorName" :value="f.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="填报日期" prop="reportDate">
          <el-date-picker v-model="form.reportDate" type="date" value-format="YYYY-MM-DD"
            placeholder="选择日期" style="width:100%" />
        </el-form-item>
        <el-form-item label="客流人数" prop="flowCount">
          <el-input-number v-model="form.flowCount" :min="0" :max="9999999"
            placeholder="请输入" style="width:100%" controls-position="right" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="formVisible = false">取消</el-button>
        <el-button type="primary" :loading="formLoading" @click="submitForm">保存</el-button>
      </template>
    </el-dialog>

    <!-- 导入结果弹窗 -->
    <el-dialog v-model="importResultVisible" title="导入结果" width="500px">
      <el-result :icon="importResult.failCount > 0 ? 'warning' : 'success'"
        :title="`成功 ${importResult.successCount} 条，失败 ${importResult.failCount} 条`">
        <template v-if="importResult.errorList?.length" #sub-title>
          <el-scrollbar max-height="200px">
            <div v-for="(err, i) in importResult.errorList" :key="i" class="error-item">{{ err }}</div>
          </el-scrollbar>
        </template>
      </el-result>
      <template #footer>
        <el-button type="primary" @click="importResultVisible = false">确定</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, computed, onMounted, watch, nextTick } from 'vue'
import { ElMessage } from 'element-plus'
import { Search, Plus, Upload, Download } from '@element-plus/icons-vue'
import * as echarts from 'echarts/core'
import { LineChart } from 'echarts/charts'
import { GridComponent, TooltipComponent, LegendComponent } from 'echarts/components'
import { CanvasRenderer } from 'echarts/renderers'
import {
  getFlowPage, createFlow, updateFlow, deleteFlow,
  importFlowExcel, exportFlowExcel, getFlowStatistics,
  type OprPassengerFlow, type PassengerFlowStatisticsVO
} from '@/api/opr/flow'
import request from '@/api/request'

echarts.use([LineChart, GridComponent, TooltipComponent, LegendComponent, CanvasRenderer])

// ─── 基础列表状态 ───────────────────────────────────────────────────
const loading = ref(false)
const list = ref<OprPassengerFlow[]>([])
const total = ref(0)
const dateRange = ref<string[]>([])

const query = reactive({
  projectId: undefined as number | undefined,
  buildingId: undefined as number | undefined,
  floorId: undefined as number | undefined,
  startDate: '',
  endDate: '',
  pageNum: 1,
  pageSize: 20
})

// ─── 下拉数据 ─────────────────────────────────────────────────────
const projectList = ref<any[]>([])
const buildingList = ref<any[]>([])
const floorList = ref<any[]>([])
const formBuildingList = ref<any[]>([])
const formFloorList = ref<any[]>([])

// 名称缓存
const projectMap = ref<Record<number, string>>({})
const buildingMap = ref<Record<number, string>>({})
const floorMap = ref<Record<number, string>>({})

// ─── 统计状态 ─────────────────────────────────────────────────────
const stats = reactive<Partial<PassengerFlowStatisticsVO>>({})
const statsFilter = reactive({ projectId: undefined as number | undefined })

// ─── Echarts ──────────────────────────────────────────────────────
const chartRef = ref<HTMLElement>()
let chartInstance: echarts.ECharts | null = null

// ─── 表单弹窗状态 ─────────────────────────────────────────────────
const formVisible = ref(false)
const formLoading = ref(false)
const editingId = ref<number | null>(null)
const formRef = ref()
const form = reactive({
  projectId: undefined as number | undefined,
  buildingId: undefined as number | undefined,
  floorId: undefined as number | undefined,
  reportDate: '',
  flowCount: 0
})
const formRules = {
  projectId: [{ required: true, message: '请选择项目', trigger: 'change' }],
  reportDate: [{ required: true, message: '请选择填报日期', trigger: 'change' }],
  flowCount: [{ required: true, message: '请填写客流人数', trigger: 'blur' }]
}

// ─── 导入弹窗状态 ─────────────────────────────────────────────────
const importResultVisible = ref(false)
const importResult = reactive({ successCount: 0, failCount: 0, errorList: [] as string[] })

// ─────────────────────────────────────────────────────────────────
// 数据加载
// ─────────────────────────────────────────────────────────────────

async function loadProjects() {
  try {
    const res = await request.get('/api/base/projects', { params: { pageSize: 999 } })
    projectList.value = res.data?.records || []
    projectList.value.forEach((p: any) => { projectMap.value[p.id] = p.projectName })
  } catch (e) {}
}

async function loadBuildings(projectId: number) {
  try {
    const res = await request.get('/api/base/buildings', { params: { projectId, pageSize: 999 } })
    buildingList.value = res.data?.records || []
    buildingList.value.forEach((b: any) => { buildingMap.value[b.id] = b.buildingName })
  } catch (e) { buildingList.value = [] }
}

async function loadFloors(buildingId: number) {
  try {
    const res = await request.get('/api/base/floors', { params: { buildingId, pageSize: 999 } })
    floorList.value = res.data?.records || []
    floorList.value.forEach((f: any) => { floorMap.value[f.id] = f.floorName })
  } catch (e) { floorList.value = [] }
}

async function loadFormBuildings(projectId: number) {
  try {
    const res = await request.get('/api/base/buildings', { params: { projectId, pageSize: 999 } })
    formBuildingList.value = res.data?.records || []
    formBuildingList.value.forEach((b: any) => { buildingMap.value[b.id] = b.buildingName })
  } catch (e) { formBuildingList.value = [] }
}

async function loadFormFloors(buildingId: number) {
  try {
    const res = await request.get('/api/base/floors', { params: { buildingId, pageSize: 999 } })
    formFloorList.value = res.data?.records || []
    formFloorList.value.forEach((f: any) => { floorMap.value[f.id] = f.floorName })
  } catch (e) { formFloorList.value = [] }
}

async function loadList() {
  // 同步日期区间
  if (dateRange.value?.length === 2) {
    query.startDate = dateRange.value[0]
    query.endDate = dateRange.value[1]
  } else {
    query.startDate = ''
    query.endDate = ''
  }
  loading.value = true
  try {
    const res = await getFlowPage(query)
    list.value = res.data?.records || []
    total.value = res.data?.total || 0
  } finally {
    loading.value = false
  }
}

async function loadStats() {
  try {
    const res = await getFlowStatistics({ projectId: statsFilter.projectId })
    Object.assign(stats, res.data || {})
    renderChart(res.data?.trendPoints || [])
  } catch (e) {}
}

// ─────────────────────────────────────────────────────────────────
// Echarts 渲染
// ─────────────────────────────────────────────────────────────────

function renderChart(points: { date: string; flowCount: number }[]) {
  nextTick(() => {
    if (!chartRef.value) return
    if (!chartInstance) {
      chartInstance = echarts.init(chartRef.value)
    }
    const dates = points.map(p => p.date)
    const values = points.map(p => p.flowCount)
    chartInstance.setOption({
      tooltip: { trigger: 'axis', formatter: (params: any) => `${params[0].name}<br/>客流：${params[0].value} 人` },
      grid: { left: 50, right: 20, top: 20, bottom: 40 },
      xAxis: {
        type: 'category',
        data: dates,
        axisLabel: {
          interval: 4,
          rotate: 30,
          fontSize: 11
        }
      },
      yAxis: { type: 'value', name: '人次' },
      series: [{
        type: 'line',
        data: values,
        smooth: true,
        symbol: 'circle',
        symbolSize: 5,
        lineStyle: { color: '#2E75B6', width: 2 },
        areaStyle: { color: 'rgba(46,117,182,0.12)' },
        itemStyle: { color: '#2E75B6' }
      }]
    })
  })
}

// ─────────────────────────────────────────────────────────────────
// 筛选联动
// ─────────────────────────────────────────────────────────────────

function onProjectChange(val: number) {
  query.buildingId = undefined
  query.floorId = undefined
  buildingList.value = []
  floorList.value = []
  if (val) loadBuildings(val)
}

function onBuildingChange(val: number) {
  query.floorId = undefined
  floorList.value = []
  if (val) loadFloors(val)
}

function onFormProjectChange(val: number) {
  form.buildingId = undefined
  form.floorId = undefined
  formBuildingList.value = []
  formFloorList.value = []
  if (val) loadFormBuildings(val)
}

function onFormBuildingChange(val: number) {
  form.floorId = undefined
  formFloorList.value = []
  if (val) loadFormFloors(val)
}

function resetQuery() {
  query.projectId = undefined
  query.buildingId = undefined
  query.floorId = undefined
  dateRange.value = []
  query.startDate = ''
  query.endDate = ''
  query.pageNum = 1
  loadList()
}

// ─────────────────────────────────────────────────────────────────
// 表单操作
// ─────────────────────────────────────────────────────────────────

function openForm(row: OprPassengerFlow | null) {
  formRef.value?.clearValidate()
  if (row) {
    editingId.value = row.id
    form.projectId = row.projectId
    form.buildingId = row.buildingId
    form.floorId = row.floorId
    form.reportDate = row.reportDate
    form.flowCount = row.flowCount
    if (row.projectId) loadFormBuildings(row.projectId)
    if (row.buildingId) loadFormFloors(row.buildingId)
  } else {
    editingId.value = null
    form.projectId = undefined
    form.buildingId = undefined
    form.floorId = undefined
    form.reportDate = ''
    form.flowCount = 0
    formBuildingList.value = []
    formFloorList.value = []
  }
  formVisible.value = true
}

async function submitForm() {
  const valid = await formRef.value?.validate().catch(() => false)
  if (!valid) return
  formLoading.value = true
  try {
    const dto = {
      projectId: form.projectId!,
      buildingId: form.buildingId,
      floorId: form.floorId,
      reportDate: form.reportDate,
      flowCount: form.flowCount
    }
    if (editingId.value) {
      await updateFlow(editingId.value, dto)
      ElMessage.success('修改成功')
    } else {
      await createFlow(dto)
      ElMessage.success('新增成功')
    }
    formVisible.value = false
    loadList()
    loadStats()
  } catch (e: any) {
    ElMessage.error(e?.response?.data?.msg || '操作失败')
  } finally {
    formLoading.value = false
  }
}

async function doDelete(id: number) {
  try {
    await deleteFlow(id)
    ElMessage.success('删除成功')
    loadList()
    loadStats()
  } catch (e: any) {
    ElMessage.error(e?.response?.data?.msg || '删除失败')
  }
}

// ─────────────────────────────────────────────────────────────────
// 导入导出
// ─────────────────────────────────────────────────────────────────

async function doImport(file: File) {
  try {
    const res = await importFlowExcel(file)
    const data = res.data || {}
    importResult.successCount = data.successCount || 0
    importResult.failCount = data.failCount || 0
    importResult.errorList = data.errorList || []
    importResultVisible.value = true
    if (importResult.successCount > 0) {
      loadList()
      loadStats()
    }
  } catch (e: any) {
    ElMessage.error(e?.response?.data?.msg || '导入失败')
  }
  return false  // 阻止默认上传行为
}

async function doExport() {
  try {
    const res = await exportFlowExcel(query)
    const url = URL.createObjectURL(res.data as Blob)
    const a = document.createElement('a')
    a.href = url
    a.download = `客流填报_${new Date().toISOString().slice(0, 10)}.xlsx`
    a.click()
    URL.revokeObjectURL(url)
  } catch (e) {
    ElMessage.error('导出失败')
  }
}

// ─────────────────────────────────────────────────────────────────
// 工具函数
// ─────────────────────────────────────────────────────────────────

function getProjectName(id?: number) { return id ? (projectMap.value[id] || `项目${id}`) : '-' }
function getBuildingName(id?: number) { return id ? (buildingMap.value[id] || `楼栋${id}`) : '' }
function getFloorName(id?: number) { return id ? (floorMap.value[id] || `楼层${id}`) : '' }

function sourceLabel(t: number) {
  return { 1: '手动', 2: '导入', 3: '设备' }[t] ?? ''
}
function sourceTagType(t: number): 'success' | 'warning' | 'primary' | undefined {
  const m: Record<number, 'success' | 'warning' | 'primary'> = { 1: 'primary', 2: 'success', 3: 'warning' }
  return m[t]
}
function rateClass(rate: number) {
  return rate > 0 ? 'rate-up' : rate < 0 ? 'rate-down' : 'gray'
}
function formatRate(rate: number | null | undefined) {
  if (rate == null) return '-'
  const sign = rate > 0 ? '+' : ''
  return `${sign}${rate}%`
}

onMounted(async () => {
  await loadProjects()
  loadList()
  loadStats()
})
</script>

<style scoped lang="scss">
.flow-page { display: flex; flex-direction: column; gap: 16px; }

.stat-row { margin-bottom: 0; }

.stat-card {
  text-align: center;
  padding: 4px 0;
  border-radius: 12px !important;
  border: 1px solid rgba(0, 0, 0, 0.06) !important;
  transition: box-shadow 0.2s;
  &:hover { box-shadow: 0 4px 16px rgba(0, 0, 0, 0.08) !important; }
}
.stat-title { font-size: 13px; color: #909399; margin-bottom: 6px; }
.stat-num { font-size: 30px; font-weight: 700; line-height: 1.1; }
.stat-num.primary { color: #3b82f6; }
.stat-num.success { color: #10b981; }
.stat-num.warning { color: #f59e0b; }
.stat-num.info { color: #8b5cf6; }
.stat-sub { font-size: 12px; margin-top: 6px; color: #606266; }

.filter-card {
  border-radius: 12px !important;
  border: 1px solid rgba(0, 0, 0, 0.06) !important;
  transition: box-shadow 0.2s;
  &:hover { box-shadow: 0 4px 16px rgba(0, 0, 0, 0.08) !important; }
  :deep(.el-card__body) { padding: 14px 20px; }
  :deep(.el-form-item) { margin-bottom: 0; }
}

.table-card {
  border-radius: 12px !important;
  border: 1px solid rgba(0, 0, 0, 0.06) !important;
  overflow: hidden;
  :deep(.el-card__body) { padding: 0; }
}

.card-header {
  display: flex; align-items: center; justify-content: space-between;
  padding: 14px 20px; border-bottom: 1px solid #f1f5f9; background: #fff;
  .header-left { display: flex; align-items: center; gap: 10px; }
  .header-title {
    font-size: 15px; font-weight: 600; color: #1e293b;
    display: flex; align-items: center; gap: 8px;
    &::before { content: ''; display: inline-block; width: 3px; height: 16px;
      background: linear-gradient(180deg, #3b82f6, #60a5fa); border-radius: 2px; }
  }
  .count-tag {
    font-size: 12px; background: #eff6ff; color: #3b82f6;
    border: 1px solid #bfdbfe; border-radius: 10px; padding: 2px 10px; font-weight: 500;
  }
  .header-actions { display: flex; gap: 8px; align-items: center; }
}

.table-body {
  padding: 16px 20px;
  :deep(.el-table) {
    border-radius: 8px; overflow: hidden;
    .el-table__header-wrapper th.el-table__cell {
      background: #f8fafc; color: #64748b; font-weight: 600; font-size: 13px;
      border-bottom: 1px solid #e8edf3;
    }
    .el-table__row:hover > td.el-table__cell { background-color: #f0f7ff !important; }
    .el-table__row--striped > td.el-table__cell { background-color: #fafbfc; }
    td.el-table__cell { border-bottom: 1px solid #f4f6f9; }
  }
}

.pagination { margin-top: 14px; display: flex; justify-content: flex-end; }
.rate-up { color: #f56c6c; font-weight: 600; }
.rate-down { color: #10b981; font-weight: 600; }
.gray { color: #c0c4cc; }
.error-item { font-size: 13px; color: #f56c6c; padding: 2px 0; line-height: 1.6; }
</style>
