<template>
  <div class="page-container">
    <div class="page-header">
      <div class="page-header-left">
        <h2 class="page-title">系统配置与安全</h2>
        <p class="page-desc">管理系统基础参数和安全策略，修改后立即生效</p>
      </div>
      <el-button :icon="Refresh" @click="doRefreshCache" :loading="refreshing">刷新缓存</el-button>
    </div>

    <el-tabs v-model="activeTab" type="border-card" class="config-tabs">

      <!-- ── Tab 1: 基础配置 ─────────────────────────────────────────── -->
      <el-tab-pane label="基础配置" name="basic">
        <div class="tab-pane-inner" v-loading="basicLoading">
          <el-form ref="basicFormRef" :model="basicForm" label-width="160px" class="config-form">
            <el-divider content-position="left">系统信息</el-divider>
            <el-form-item label="系统名称" prop="system.name">
              <el-input v-model="basicForm['system.name']" style="width:360px" />
            </el-form-item>
            <el-form-item label="系统版本">
              <el-input v-model="basicForm['system.version']" style="width:200px" />
            </el-form-item>
            <el-form-item label="版权信息">
              <el-input v-model="basicForm['system.copyright']" style="width:360px" />
            </el-form-item>
            <el-divider content-position="left">会话与上传</el-divider>
            <el-form-item label="会话超时（分钟）">
              <el-input-number v-model="basicFormNum['session.timeout']" :min="5" :max="1440" />
              <span class="form-tip">JWT Token 有效期，修改后需重新登录</span>
            </el-form-item>
            <el-form-item label="上传大小上限（MB）">
              <el-input-number v-model="basicFormNum['upload.max_size']" :min="1" :max="500" />
            </el-form-item>
            <el-form-item label="允许的文件类型">
              <el-input v-model="basicForm['upload.allowed_types']" style="width:400px"
                placeholder="逗号分隔，如 jpg,png,pdf" />
            </el-form-item>
          </el-form>
          <div class="form-footer">
            <el-button type="primary" :loading="basicSaving" @click="saveBasic">保存基础配置</el-button>
          </div>
        </div>
      </el-tab-pane>

      <!-- ── Tab 2: 安全策略 ─────────────────────────────────────────── -->
      <el-tab-pane label="安全策略" name="security">
        <div class="tab-pane-inner" v-loading="secLoading">
          <el-form ref="secFormRef" :model="secForm" label-width="180px" class="config-form">
            <el-divider content-position="left">密码策略</el-divider>
            <el-form-item label="密码最短长度">
              <el-input-number v-model="secFormNum['password.min_length']" :min="6" :max="32" />
              <span class="form-tip">建议不低于 8 位</span>
            </el-form-item>
            <el-form-item label="密码需含数字">
              <el-switch v-model="secFormBool['password.require_number']" />
            </el-form-item>
            <el-form-item label="密码需含大写字母">
              <el-switch v-model="secFormBool['password.require_upper']" />
            </el-form-item>
            <el-form-item label="密码需含特殊字符">
              <el-switch v-model="secFormBool['password.require_special']" />
            </el-form-item>
            <el-form-item label="密码有效期（天）">
              <el-input-number v-model="secFormNum['password.expire_days']" :min="0" :max="365" />
              <span class="form-tip">0 = 永不过期</span>
            </el-form-item>

            <el-divider content-position="left">登录策略</el-divider>
            <el-form-item label="登录失败最大次数">
              <el-input-number v-model="secFormNum['login.max_fail_count']" :min="1" :max="20" />
              <span class="form-tip">超过后账号临时锁定</span>
            </el-form-item>
            <el-form-item label="锁定时长（分钟）">
              <el-input-number v-model="secFormNum['login.lock_duration']" :min="1" :max="1440" />
            </el-form-item>
            <el-form-item label="允许多端同时登录">
              <el-switch v-model="secFormBool['login.allow_multi']" />
              <span class="form-tip">关闭后同一账号只保留最新 Token</span>
            </el-form-item>
            <el-form-item label="启用登录验证码">
              <el-switch v-model="secFormBool['login.captcha_enable']" />
            </el-form-item>
          </el-form>
          <div class="form-footer">
            <el-button type="primary" :loading="secSaving" @click="saveSecurity">保存安全策略</el-button>
          </div>
        </div>
      </el-tab-pane>

      <!-- ── Tab 3: 全量参数管理 ────────────────────────────────────── -->
      <el-tab-pane label="全量参数管理" name="all">
        <div class="tab-pane-inner">
          <!-- 搜索栏 -->
          <el-form :model="query" inline class="mb-16">
            <el-form-item label="配置键">
              <el-input v-model="query.configKey" placeholder="关键字" clearable style="width:150px"
                @keyup.enter="loadList" @clear="loadList" />
            </el-form-item>
            <el-form-item label="名称">
              <el-input v-model="query.configName" placeholder="关键字" clearable style="width:150px"
                @keyup.enter="loadList" @clear="loadList" />
            </el-form-item>
            <el-form-item label="分组">
              <el-select v-model="query.configGroup" placeholder="全部" clearable style="width:110px" @change="loadList">
                <el-option v-for="(label, g) in GROUP_MAP" :key="g" :label="label" :value="g" />
              </el-select>
            </el-form-item>
            <el-form-item>
              <el-button type="primary" :icon="Search" @click="loadList">查询</el-button>
              <el-button @click="resetQuery">重置</el-button>
              <el-button type="primary" :icon="Plus" @click="openCreate">新增</el-button>
            </el-form-item>
          </el-form>

          <!-- 数据表格 -->
          <el-table :data="list" v-loading="listLoading" stripe>
            <el-table-column prop="configKey" label="配置键" width="220" />
            <el-table-column prop="configName" label="名称" width="160" />
            <el-table-column prop="configValue" label="值" min-width="150" show-overflow-tooltip />
            <el-table-column label="分组" width="100" align="center">
              <template #default="{ row }">
                <el-tag :type="groupTagType(row.configGroup)" size="small">
                  {{ GROUP_MAP[row.configGroup] ?? row.configGroup }}
                </el-tag>
              </template>
            </el-table-column>
            <el-table-column label="内置" width="70" align="center">
              <template #default="{ row }">
                <el-tag v-if="row.isBuiltIn" type="warning" size="small">内置</el-tag>
                <span v-else>-</span>
              </template>
            </el-table-column>
            <el-table-column prop="description" label="说明" min-width="150" show-overflow-tooltip />
            <el-table-column label="操作" width="120" align="center" fixed="right">
              <template #default="{ row }">
                <el-button type="primary" link size="small" @click="openEdit(row)">编辑</el-button>
                <el-button type="danger" link size="small" :disabled="!!row.isBuiltIn" @click="doDelete(row)">
                  删除
                </el-button>
              </template>
            </el-table-column>
          </el-table>

          <el-pagination
            v-model:current-page="query.pageNum"
            v-model:page-size="query.pageSize"
            :total="total"
            layout="total, sizes, prev, pager, next"
            :page-sizes="[20, 50, 100]"
            class="mt-16"
            @change="loadList"
          />
        </div>
      </el-tab-pane>
    </el-tabs>

    <!-- 新增/编辑弹窗 -->
    <el-dialog v-model="dialogVisible" :title="dialogTitle" width="520px" destroy-on-close>
      <el-form ref="formRef" :model="form" :rules="formRules" label-width="90px">
        <el-form-item label="配置键" prop="configKey">
          <el-input v-model="form.configKey" :disabled="!!form.id" placeholder="如 system.name（创建后不可改）" />
        </el-form-item>
        <el-form-item label="名称" prop="configName">
          <el-input v-model="form.configName" placeholder="如 系统名称" />
        </el-form-item>
        <el-form-item label="配置值" prop="configValue">
          <el-input v-model="form.configValue" placeholder="配置值" />
        </el-form-item>
        <el-form-item label="分组">
          <el-select v-model="form.configGroup" style="width:100%">
            <el-option v-for="(label, g) in GROUP_MAP" :key="g" :label="label" :value="g" />
          </el-select>
        </el-form-item>
        <el-form-item label="说明">
          <el-input v-model="form.description" type="textarea" :rows="2" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="submitting" @click="doSubmit">确定</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted, watch } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Plus, Search, Refresh } from '@element-plus/icons-vue'
import { configApi, type SysConfig, type SysConfigCreateDTO } from '@/api/sys/config'

type TagType = 'success' | 'primary' | 'warning' | 'info' | 'danger' | undefined

const GROUP_MAP: Record<string, string> = {
  basic: '基础配置', security: '安全策略', upload: '上传配置', other: '其他',
}
function groupTagType(g: string): TagType {
  const m: Record<string, TagType> = { basic: 'primary', security: 'danger', upload: 'success', other: 'info' }
  return m[g] ?? 'info'
}

// ── Tab 切换 ───────────────────────────────────────────────────────────
const activeTab = ref('basic')
watch(activeTab, (tab) => {
  if (tab === 'basic' && basicLoaded.value === false) loadBasic()
  if (tab === 'security' && secLoaded.value === false) loadSecurity()
  if (tab === 'all') loadList()
})

// ── 基础配置 ───────────────────────────────────────────────────────────
const basicLoading = ref(false)
const basicSaving = ref(false)
const basicLoaded = ref(false)
const basicFormRef = ref()
const basicForm = reactive<Record<string, string>>({})
const basicFormNum = reactive<Record<string, number>>({})
const BASIC_STR_KEYS = ['system.name', 'system.version', 'system.copyright', 'upload.allowed_types']
const BASIC_NUM_KEYS = ['session.timeout', 'upload.max_size']

async function loadBasic() {
  basicLoading.value = true
  try {
    const configs: any[] = await configApi.listByGroup('basic') as unknown as SysConfig[]
    configs.forEach((c: SysConfig) => {
      if (BASIC_STR_KEYS.includes(c.configKey)) basicForm[c.configKey] = c.configValue
      if (BASIC_NUM_KEYS.includes(c.configKey)) basicFormNum[c.configKey] = Number(c.configValue) || 0
    })
    basicLoaded.value = true
  } finally {
    basicLoading.value = false
  }
}

async function saveBasic() {
  basicSaving.value = true
  try {
    const configs: any[] = await configApi.listByGroup('basic') as unknown as SysConfig[]
    const idMap: Record<string, number> = {}
    configs.forEach((c: SysConfig) => { idMap[c.configKey] = c.id })

    const updates: Promise<any>[] = []
    BASIC_STR_KEYS.forEach(k => {
      if (idMap[k] !== undefined) updates.push(configApi.update(idMap[k], { configKey: k, configName: '', configValue: basicForm[k] ?? '' }))
    })
    BASIC_NUM_KEYS.forEach(k => {
      if (idMap[k] !== undefined) updates.push(configApi.update(idMap[k], { configKey: k, configName: '', configValue: String(basicFormNum[k] ?? 0) }))
    })
    await Promise.all(updates)
    ElMessage.success('基础配置已保存')
  } finally {
    basicSaving.value = false
  }
}

// ── 安全策略 ───────────────────────────────────────────────────────────
const secLoading = ref(false)
const secSaving = ref(false)
const secLoaded = ref(false)
const secFormRef = ref()
const secForm = reactive<Record<string, string>>({})
const secFormNum = reactive<Record<string, number>>({})
const secFormBool = reactive<Record<string, boolean>>({})
const SEC_NUM_KEYS  = ['password.min_length', 'password.expire_days', 'login.max_fail_count', 'login.lock_duration']
const SEC_BOOL_KEYS = ['password.require_number', 'password.require_upper', 'password.require_special', 'login.allow_multi', 'login.captcha_enable']

async function loadSecurity() {
  secLoading.value = true
  try {
    const configs: any[] = await configApi.listByGroup('security') as unknown as SysConfig[]
    configs.forEach((c: SysConfig) => {
      if (SEC_NUM_KEYS.includes(c.configKey))  secFormNum[c.configKey]  = Number(c.configValue) || 0
      if (SEC_BOOL_KEYS.includes(c.configKey)) secFormBool[c.configKey] = c.configValue === 'true'
    })
    secLoaded.value = true
  } finally {
    secLoading.value = false
  }
}

async function saveSecurity() {
  secSaving.value = true
  try {
    const configs: any[] = await configApi.listByGroup('security') as unknown as SysConfig[]
    const idMap: Record<string, number> = {}
    configs.forEach((c: SysConfig) => { idMap[c.configKey] = c.id })

    const updates: Promise<any>[] = []
    SEC_NUM_KEYS.forEach(k => {
      if (idMap[k] !== undefined) updates.push(configApi.update(idMap[k], { configKey: k, configName: '', configValue: String(secFormNum[k] ?? 0) }))
    })
    SEC_BOOL_KEYS.forEach(k => {
      if (idMap[k] !== undefined) updates.push(configApi.update(idMap[k], { configKey: k, configName: '', configValue: String(secFormBool[k] ?? false) }))
    })
    await Promise.all(updates)
    ElMessage.success('安全策略已保存')
  } finally {
    secSaving.value = false
  }
}

// ── 全量参数管理（CRUD 表格） ────────────────────────────────────────
const listLoading = ref(false)
const list = ref<SysConfig[]>([])
const total = ref(0)
const query = reactive({
  pageNum: 1, pageSize: 20,
  configKey: '', configName: '',
  configGroup: '',
})

async function loadList() {
  listLoading.value = true
  try {
    const res: any = await configApi.page(query)
    list.value = res.records ?? []
    total.value = res.total ?? 0
  } finally {
    listLoading.value = false
  }
}

function resetQuery() {
  Object.assign(query, { pageNum: 1, configKey: '', configName: '', configGroup: '' })
  loadList()
}

// CRUD 弹窗
const dialogVisible = ref(false)
const dialogTitle = ref('')
const submitting = ref(false)
const formRef = ref()
const form = reactive<SysConfigCreateDTO>({
  configKey: '', configName: '', configValue: '', configGroup: 'other',
})
const formRules = {
  configKey:   [{ required: true, message: '请输入配置键', trigger: 'blur' }],
  configName:  [{ required: true, message: '请输入名称', trigger: 'blur' }],
  configValue: [{ required: true, message: '请输入配置值', trigger: 'blur' }],
}

function openCreate() {
  Object.assign(form, { id: undefined, configKey: '', configName: '', configValue: '', configGroup: 'other', description: '' })
  dialogTitle.value = '新增参数'
  dialogVisible.value = true
}

function openEdit(row: SysConfig) {
  Object.assign(form, { id: row.id, configKey: row.configKey, configName: row.configName, configValue: row.configValue, configGroup: row.configGroup, description: row.description ?? '' })
  dialogTitle.value = '编辑参数'
  dialogVisible.value = true
}

async function doSubmit() {
  await formRef.value?.validate()
  submitting.value = true
  try {
    form.id ? await configApi.update(form.id, form) : await configApi.create(form)
    ElMessage.success('操作成功')
    dialogVisible.value = false
    loadList()
  } finally {
    submitting.value = false
  }
}

async function doDelete(row: SysConfig) {
  await ElMessageBox.confirm(`确认删除参数「${row.configName}」？`, '警告', { type: 'warning' })
  await configApi.delete(row.id)
  ElMessage.success('删除成功')
  loadList()
}

// ── 刷新缓存 ──────────────────────────────────────────────────────────
const refreshing = ref(false)
async function doRefreshCache() {
  refreshing.value = true
  try {
    await configApi.refresh()
    ElMessage.success('缓存已刷新')
    // 重新加载当前 Tab 数据
    basicLoaded.value = false
    secLoaded.value = false
    if (activeTab.value === 'basic') loadBasic()
    else if (activeTab.value === 'security') loadSecurity()
    else loadList()
  } finally {
    refreshing.value = false
  }
}

onMounted(loadBasic)
</script>

<style scoped lang="scss">
.config-tabs {
  margin-top: 16px;
  :deep(.el-tabs__content) { padding: 0; }
}

.tab-pane-inner {
  padding: 24px 32px 8px;
}

.config-form {
  max-width: 700px;
  .el-divider { margin: 24px 0 20px; }
}

.form-tip {
  margin-left: 10px;
  color: #909399;
  font-size: 12px;
}

.form-footer {
  margin-top: 24px;
  padding-top: 16px;
  border-top: 1px solid #f0f0f0;
}

.mb-16 { margin-bottom: 16px; }
.mt-16 { margin-top: 16px; }
</style>
