<template>
  <div class="page-container">
    <div class="page-header">
      <div class="page-header-left">
        <h2 class="page-title">日志管理</h2>
        <p class="page-desc">操作审计日志与登录记录查询</p>
      </div>
    </div>

    <el-tabs v-model="activeTab" type="border-card" class="log-tabs">

      <!-- ── Tab 1: 操作日志 ─────────────────────────────────────────── -->
      <el-tab-pane label="操作日志" name="oper">
        <div class="tab-pane-inner">
          <!-- 搜索栏 -->
          <el-card shadow="never" class="search-card mb-16">
            <el-form :model="operQuery" inline>
              <el-form-item label="模块">
                <el-input v-model="operQuery.module" placeholder="模块名称" clearable style="width:130px"
                  @keyup.enter="operSearch" @clear="operSearch" />
              </el-form-item>
              <el-form-item label="操作人">
                <el-input v-model="operQuery.operUser" placeholder="操作人" clearable style="width:130px"
                  @keyup.enter="operSearch" @clear="operSearch" />
              </el-form-item>
              <el-form-item label="状态">
                <el-select v-model="operQuery.status" clearable placeholder="全部" style="width:90px">
                  <el-option label="成功" :value="0" />
                  <el-option label="失败" :value="1" />
                </el-select>
              </el-form-item>
              <el-form-item label="操作时间">
                <el-date-picker
                  v-model="operTimeRange"
                  type="daterange"
                  range-separator="至"
                  start-placeholder="开始"
                  end-placeholder="结束"
                  value-format="YYYY-MM-DD"
                  style="width:220px"
                  @change="onOperTimeChange"
                />
              </el-form-item>
              <el-form-item>
                <el-button type="primary" :icon="Search" @click="operSearch">查询</el-button>
                <el-button :icon="Refresh" @click="operReset">重置</el-button>
                <el-button type="danger" plain :icon="Delete" @click="doClearOperLog">清空日志</el-button>
              </el-form-item>
            </el-form>
          </el-card>

          <!-- 数据表格 -->
          <el-table :data="operList" v-loading="operLoading" stripe>
            <el-table-column prop="module" label="模块" width="90" />
            <el-table-column label="操作类型" width="90" align="center">
              <template #default="{ row }">
                <el-tag size="small" :type="bizTypeTag(row.bizType)">{{ BIZ_TYPE_LABEL[row.bizType] ?? row.bizType }}</el-tag>
              </template>
            </el-table-column>
            <el-table-column label="请求方式" width="75" align="center">
              <template #default="{ row }">
                <el-tag size="small" :type="httpMethodTag(row.requestMethod)">{{ row.requestMethod }}</el-tag>
              </template>
            </el-table-column>
            <el-table-column prop="requestUrl" label="请求URL" min-width="200" show-overflow-tooltip />
            <el-table-column prop="operUser" label="操作人" width="90" />
            <el-table-column prop="operIp" label="IP地址" width="125" />
            <el-table-column label="状态" width="72" align="center">
              <template #default="{ row }">
                <el-tag :type="row.status === 0 ? 'success' : 'danger'" size="small">
                  {{ row.status === 0 ? '成功' : '失败' }}
                </el-tag>
              </template>
            </el-table-column>
            <el-table-column prop="costTime" label="耗时(ms)" width="85" align="right" />
            <el-table-column prop="operTime" label="操作时间" width="165" />
            <el-table-column label="操作" width="65" align="center" fixed="right">
              <template #default="{ row }">
                <el-button type="primary" link size="small" @click="openOperDetail(row)">详情</el-button>
              </template>
            </el-table-column>
          </el-table>

          <el-pagination
            v-model:current-page="operQuery.pageNum"
            v-model:page-size="operQuery.pageSize"
            :total="operTotal"
            :page-sizes="[20, 50, 100]"
            layout="total, sizes, prev, pager, next"
            class="mt-16"
            @change="loadOperLog"
          />
        </div>
      </el-tab-pane>

      <!-- ── Tab 2: 登录日志 ─────────────────────────────────────────── -->
      <el-tab-pane label="登录日志" name="login">
        <div class="tab-pane-inner">
          <el-card shadow="never" class="search-card mb-16">
            <el-form :model="loginQuery" inline>
              <el-form-item label="用户名">
                <el-input v-model="loginQuery.username" placeholder="用户名" clearable style="width:130px"
                  @keyup.enter="loginSearch" @clear="loginSearch" />
              </el-form-item>
              <el-form-item label="IP地址">
                <el-input v-model="loginQuery.ipAddr" placeholder="IP地址" clearable style="width:130px"
                  @keyup.enter="loginSearch" @clear="loginSearch" />
              </el-form-item>
              <el-form-item label="状态">
                <el-select v-model="loginQuery.status" clearable placeholder="全部" style="width:90px">
                  <el-option label="成功" :value="0" />
                  <el-option label="失败" :value="1" />
                </el-select>
              </el-form-item>
              <el-form-item label="登录时间">
                <el-date-picker
                  v-model="loginTimeRange"
                  type="daterange"
                  range-separator="至"
                  start-placeholder="开始"
                  end-placeholder="结束"
                  value-format="YYYY-MM-DD"
                  style="width:220px"
                  @change="onLoginTimeChange"
                />
              </el-form-item>
              <el-form-item>
                <el-button type="primary" :icon="Search" @click="loginSearch">查询</el-button>
                <el-button :icon="Refresh" @click="loginReset">重置</el-button>
                <el-button type="danger" plain :icon="Delete" @click="doClearLoginLog">清空日志</el-button>
              </el-form-item>
            </el-form>
          </el-card>

          <el-table :data="loginList" v-loading="loginLoading" stripe>
            <el-table-column prop="username" label="用户名" width="120" />
            <el-table-column prop="ipAddr" label="IP地址" width="130" />
            <el-table-column prop="browser" label="浏览器" width="120" show-overflow-tooltip />
            <el-table-column prop="os" label="操作系统" width="130" show-overflow-tooltip />
            <el-table-column label="状态" width="72" align="center">
              <template #default="{ row }">
                <el-tag :type="row.status === 0 ? 'success' : 'danger'" size="small">
                  {{ row.status === 0 ? '成功' : '失败' }}
                </el-tag>
              </template>
            </el-table-column>
            <el-table-column prop="msg" label="消息" min-width="160" show-overflow-tooltip />
            <el-table-column prop="loginTime" label="登录时间" width="170" />
          </el-table>

          <el-pagination
            v-model:current-page="loginQuery.pageNum"
            v-model:page-size="loginQuery.pageSize"
            :total="loginTotal"
            :page-sizes="[20, 50, 100]"
            layout="total, sizes, prev, pager, next"
            class="mt-16"
            @change="loadLoginLog"
          />
        </div>
      </el-tab-pane>
    </el-tabs>

    <!-- 操作日志详情弹窗 -->
    <el-dialog v-model="operDetailVisible" title="操作日志详情" width="700px" destroy-on-close>
      <el-descriptions :column="2" border size="small">
        <el-descriptions-item label="模块">{{ operCurrent?.module }}</el-descriptions-item>
        <el-descriptions-item label="操作类型">
          <el-tag size="small" :type="bizTypeTag(operCurrent?.bizType ?? '')">
            {{ BIZ_TYPE_LABEL[operCurrent?.bizType ?? ''] ?? operCurrent?.bizType }}
          </el-tag>
        </el-descriptions-item>
        <el-descriptions-item label="请求方式">
          <el-tag size="small" :type="httpMethodTag(operCurrent?.requestMethod ?? '')">
            {{ operCurrent?.requestMethod }}
          </el-tag>
        </el-descriptions-item>
        <el-descriptions-item label="状态">
          <el-tag :type="operCurrent?.status === 0 ? 'success' : 'danger'" size="small">
            {{ operCurrent?.status === 0 ? '成功' : '失败' }}
          </el-tag>
        </el-descriptions-item>
        <el-descriptions-item label="操作人">{{ operCurrent?.operUser }}</el-descriptions-item>
        <el-descriptions-item label="IP地址">{{ operCurrent?.operIp }}</el-descriptions-item>
        <el-descriptions-item label="耗时(ms)">{{ operCurrent?.costTime }}</el-descriptions-item>
        <el-descriptions-item label="操作时间">{{ operCurrent?.operTime }}</el-descriptions-item>
        <el-descriptions-item label="请求URL" :span="2">{{ operCurrent?.requestUrl }}</el-descriptions-item>
        <el-descriptions-item label="Java方法" :span="2">{{ operCurrent?.method }}</el-descriptions-item>
        <el-descriptions-item label="请求参数" :span="2">
          <pre class="log-pre">{{ operCurrent?.requestParam || '-' }}</pre>
        </el-descriptions-item>
        <el-descriptions-item v-if="operCurrent?.status === 1" label="错误信息" :span="2">
          <span class="error-text">{{ operCurrent?.errorMsg }}</span>
        </el-descriptions-item>
      </el-descriptions>
      <template #footer>
        <el-button @click="operDetailVisible = false">关闭</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, watch } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Search, Refresh, Delete } from '@element-plus/icons-vue'
import {
  logApi, loginLogApi,
  type SysOperLog, type OperLogQueryDTO,
  type SysLoginLog, type LoginLogQueryDTO,
} from '@/api/sys/log'

type TagType = 'success' | 'primary' | 'warning' | 'info' | 'danger' | undefined

// ── 操作类型标签映射（enum OperType 名称） ─────────────────────────────
const BIZ_TYPE_LABEL: Record<string, string> = {
  CREATE: '新增', UPDATE: '修改', DELETE: '删除',
  QUERY: '查询', EXPORT: '导出', IMPORT: '导入', OTHER: '其他',
}
function bizTypeTag(t: string): TagType {
  const m: Record<string, TagType> = {
    CREATE: 'success', UPDATE: 'warning', DELETE: 'danger',
    QUERY: 'info', EXPORT: 'primary', IMPORT: 'primary', OTHER: undefined,
  }
  return m[t]
}
function httpMethodTag(m: string): TagType {
  const map: Record<string, TagType> = {
    GET: 'info', POST: 'success', PUT: 'warning', DELETE: 'danger', PATCH: 'warning',
  }
  return map[m]
}

// ── Tab 切换 ───────────────────────────────────────────────────────────
const activeTab = ref('oper')
watch(activeTab, (tab) => {
  if (tab === 'oper' && operList.value.length === 0) loadOperLog()
  if (tab === 'login' && loginList.value.length === 0) loadLoginLog()
})

// ── 操作日志 ───────────────────────────────────────────────────────────
const operLoading = ref(false)
const operList = ref<SysOperLog[]>([])
const operTotal = ref(0)
const operTimeRange = ref<[string, string] | null>(null)
const operQuery = reactive<OperLogQueryDTO & { pageNum: number; pageSize: number }>({
  pageNum: 1, pageSize: 20, module: '', operUser: '', status: undefined,
  timeFrom: undefined, timeTo: undefined,
})
const operDetailVisible = ref(false)
const operCurrent = ref<SysOperLog | null>(null)

async function loadOperLog() {
  operLoading.value = true
  try {
    const res: any = await logApi.page(operQuery)
    operList.value = res.records ?? []
    operTotal.value = res.total ?? 0
  } finally {
    operLoading.value = false
  }
}

function operSearch() { operQuery.pageNum = 1; loadOperLog() }
function operReset() {
  Object.assign(operQuery, { module: '', operUser: '', status: undefined, timeFrom: undefined, timeTo: undefined })
  operTimeRange.value = null
  operSearch()
}
function onOperTimeChange(val: [string, string] | null) {
  operQuery.timeFrom = val ? val[0] : undefined
  operQuery.timeTo   = val ? val[1] : undefined
}
function openOperDetail(row: SysOperLog) { operCurrent.value = row; operDetailVisible.value = true }

async function doClearOperLog() {
  await ElMessageBox.confirm('确认清空所有操作日志？此操作不可恢复！', '警告', { type: 'warning' })
  await logApi.clearAll()
  ElMessage.success('操作日志已清空')
  loadOperLog()
}

// ── 登录日志 ───────────────────────────────────────────────────────────
const loginLoading = ref(false)
const loginList = ref<SysLoginLog[]>([])
const loginTotal = ref(0)
const loginTimeRange = ref<[string, string] | null>(null)
const loginQuery = reactive<LoginLogQueryDTO & { pageNum: number; pageSize: number }>({
  pageNum: 1, pageSize: 20, username: '', ipAddr: '', status: undefined,
  timeFrom: undefined, timeTo: undefined,
})

async function loadLoginLog() {
  loginLoading.value = true
  try {
    const res: any = await loginLogApi.page(loginQuery)
    loginList.value = res.records ?? []
    loginTotal.value = res.total ?? 0
  } finally {
    loginLoading.value = false
  }
}

function loginSearch() { loginQuery.pageNum = 1; loadLoginLog() }
function loginReset() {
  Object.assign(loginQuery, { username: '', ipAddr: '', status: undefined, timeFrom: undefined, timeTo: undefined })
  loginTimeRange.value = null
  loginSearch()
}
function onLoginTimeChange(val: [string, string] | null) {
  loginQuery.timeFrom = val ? val[0] : undefined
  loginQuery.timeTo   = val ? val[1] : undefined
}

async function doClearLoginLog() {
  await ElMessageBox.confirm('确认清空所有登录日志？此操作不可恢复！', '警告', { type: 'warning' })
  await loginLogApi.clearAll()
  ElMessage.success('登录日志已清空')
  loadLoginLog()
}

// 初始加载操作日志
loadOperLog()
</script>

<style scoped lang="scss">
.log-tabs {
  margin-top: 16px;
  :deep(.el-tabs__content) { padding: 0; }
}

.tab-pane-inner {
  padding: 20px 24px 8px;
}

.search-card {
  :deep(.el-card__body) { padding: 16px 20px 4px; }
}

.log-pre {
  margin: 0;
  font-size: 12px;
  font-family: 'Courier New', monospace;
  white-space: pre-wrap;
  word-break: break-all;
  max-height: 200px;
  overflow-y: auto;
  background: #f5f7fa;
  padding: 8px;
  border-radius: 4px;
}

.error-text {
  color: var(--el-color-danger);
  font-size: 13px;
}

.mb-16 { margin-bottom: 16px; }
.mt-16 { margin-top: 16px; }
</style>
