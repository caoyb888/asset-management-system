<template>
  <div class="page-container">
    <div class="page-header">
      <div class="page-header-left">
        <h2 class="page-title">操作日志</h2>
        <p class="page-desc">查询系统操作审计日志</p>
      </div>
      <el-button type="danger" plain :icon="Delete" @click="doClearAll">清空日志</el-button>
    </div>

    <!-- 搜索栏 -->
    <el-card shadow="never" class="search-card">
      <el-form :model="query" inline>
        <el-form-item label="模块">
          <el-input v-model="query.module" placeholder="模块名称" clearable style="width:140px" />
        </el-form-item>
        <el-form-item label="操作人">
          <el-input v-model="query.operUser" placeholder="操作人" clearable style="width:140px" />
        </el-form-item>
        <el-form-item label="状态">
          <el-select v-model="query.status" clearable placeholder="全部" style="width:100px">
            <el-option label="成功" :value="0" />
            <el-option label="失败" :value="1" />
          </el-select>
        </el-form-item>
        <el-form-item label="操作时间">
          <el-date-picker
            v-model="timeRange"
            type="daterange"
            range-separator="至"
            start-placeholder="开始日期"
            end-placeholder="结束日期"
            value-format="YYYY-MM-DD"
            style="width:240px"
            @change="onTimeRangeChange"
          />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" :icon="Search" @click="doSearch">查询</el-button>
          <el-button :icon="Refresh" @click="doReset">重置</el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <!-- 数据表格 -->
    <el-card shadow="never" class="table-card">
      <el-table :data="list" v-loading="loading" stripe>
        <el-table-column prop="module" label="模块" width="100" />
        <el-table-column prop="bizType" label="操作类型" width="90" align="center">
          <template #default="{ row }">
            <el-tag size="small" :type="bizTypeTagType(row.bizType)">{{ row.bizType }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="requestMethod" label="请求方式" width="80" align="center">
          <template #default="{ row }">
            <el-tag size="small" :type="methodTagType(row.requestMethod)">{{ row.requestMethod }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="requestUrl" label="请求URL" min-width="180" show-overflow-tooltip />
        <el-table-column prop="operUser" label="操作人" width="100" />
        <el-table-column prop="operIp" label="IP地址" width="130" />
        <el-table-column prop="status" label="状态" width="80" align="center">
          <template #default="{ row }">
            <el-tag :type="row.status === 0 ? 'success' : 'danger'" size="small">
              {{ row.status === 0 ? '成功' : '失败' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="costTime" label="耗时(ms)" width="90" align="right" />
        <el-table-column prop="operTime" label="操作时间" width="170" />
        <el-table-column label="操作" width="70" align="center">
          <template #default="{ row }">
            <el-button type="primary" link size="small" @click="openDetail(row)">详情</el-button>
          </template>
        </el-table-column>
      </el-table>

      <el-pagination
        v-model:current-page="query.pageNum"
        v-model:page-size="query.pageSize"
        :total="total"
        :page-sizes="[20, 50, 100]"
        layout="total, sizes, prev, pager, next"
        class="mt-16"
        @change="loadData"
      />
    </el-card>

    <!-- 详情弹窗 -->
    <el-dialog v-model="detailVisible" title="日志详情" width="680px" destroy-on-close>
      <el-descriptions :column="2" border>
        <el-descriptions-item label="模块">{{ current?.module }}</el-descriptions-item>
        <el-descriptions-item label="操作类型">{{ current?.bizType }}</el-descriptions-item>
        <el-descriptions-item label="请求方式">{{ current?.requestMethod }}</el-descriptions-item>
        <el-descriptions-item label="状态">
          <el-tag :type="current?.status === 0 ? 'success' : 'danger'" size="small">
            {{ current?.status === 0 ? '成功' : '失败' }}
          </el-tag>
        </el-descriptions-item>
        <el-descriptions-item label="操作人">{{ current?.operUser }}</el-descriptions-item>
        <el-descriptions-item label="IP地址">{{ current?.operIp }}</el-descriptions-item>
        <el-descriptions-item label="耗时(ms)">{{ current?.costTime }}</el-descriptions-item>
        <el-descriptions-item label="操作时间">{{ current?.operTime }}</el-descriptions-item>
        <el-descriptions-item label="请求URL" :span="2">{{ current?.requestUrl }}</el-descriptions-item>
        <el-descriptions-item label="方法" :span="2">{{ current?.method }}</el-descriptions-item>
        <el-descriptions-item label="请求参数" :span="2">
          <pre class="log-pre">{{ formatJson(current?.requestParam) }}</pre>
        </el-descriptions-item>
        <el-descriptions-item v-if="current?.status === 1" label="错误信息" :span="2">
          <span class="error-text">{{ current?.errorMsg }}</span>
        </el-descriptions-item>
      </el-descriptions>
      <template #footer>
        <el-button @click="detailVisible = false">关闭</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Search, Refresh, Delete } from '@element-plus/icons-vue'
import { logApi, type SysOperLog, type OperLogQueryDTO } from '@/api/sys/log'

const loading = ref(false)
const list = ref<SysOperLog[]>([])
const total = ref(0)
const timeRange = ref<[string, string] | null>(null)

const query = reactive<OperLogQueryDTO & { pageNum: number; pageSize: number }>({
  pageNum: 1,
  pageSize: 20,
  module: '',
  operUser: '',
  status: undefined,
  timeFrom: undefined,
  timeTo: undefined,
})

const detailVisible = ref(false)
const current = ref<SysOperLog | null>(null)

async function loadData() {
  loading.value = true
  try {
    const res: any = await logApi.page(query)
    list.value = res.records ?? []
    total.value = res.total ?? 0
  } finally {
    loading.value = false
  }
}

function doSearch() {
  query.pageNum = 1
  loadData()
}

function doReset() {
  query.module = ''
  query.operUser = ''
  query.status = undefined
  query.timeFrom = undefined
  query.timeTo = undefined
  timeRange.value = null
  doSearch()
}

function onTimeRangeChange(val: [string, string] | null) {
  if (val) {
    query.timeFrom = val[0]
    query.timeTo = val[1]
  } else {
    query.timeFrom = undefined
    query.timeTo = undefined
  }
}

async function doClearAll() {
  await ElMessageBox.confirm('确认清空所有操作日志？此操作不可恢复。', '警告', { type: 'warning' })
  await logApi.clearAll()
  ElMessage.success('日志已清空')
  loadData()
}

function openDetail(row: SysOperLog) {
  current.value = row
  detailVisible.value = true
}

function bizTypeTagType(bizType: string) {
  const map: Record<string, string> = {
    INSERT: 'success',
    UPDATE: 'warning',
    DELETE: 'danger',
    QUERY: 'info',
    IMPORT: 'primary',
    EXPORT: 'primary',
  }
  return (map[bizType] ?? '') as any
}

function methodTagType(method: string) {
  const map: Record<string, string> = {
    GET: 'info',
    POST: 'success',
    PUT: 'warning',
    DELETE: 'danger',
    PATCH: 'warning',
  }
  return (map[method] ?? '') as any
}

function formatJson(str?: string) {
  if (!str) return '-'
  try {
    return JSON.stringify(JSON.parse(str), null, 2)
  } catch {
    return str
  }
}

loadData()
</script>

<style scoped lang="scss">
.log-pre {
  margin: 0;
  font-size: 12px;
  white-space: pre-wrap;
  word-break: break-all;
  max-height: 200px;
  overflow-y: auto;
}

.error-text {
  color: var(--el-color-danger);
  font-size: 13px;
}

.mt-16 {
  margin-top: 16px;
}
</style>
