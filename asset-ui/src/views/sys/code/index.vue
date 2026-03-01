<template>
  <div class="page-container">
    <div class="page-header">
      <div class="page-header-left">
        <h2 class="page-title">编码规则管理</h2>
        <p class="page-desc">配置业务编码生成规则（合同号、收款单号等）</p>
      </div>
      <el-button type="primary" :icon="Plus" @click="openCreate">新增规则</el-button>
    </div>

    <!-- 搜索栏 -->
    <el-card shadow="never" class="search-card">
      <el-form :model="query" inline>
        <el-form-item label="规则名称">
          <el-input v-model="query.ruleName" placeholder="关键字" clearable style="width:160px" @keyup.enter="loadList" @clear="loadList" />
        </el-form-item>
        <el-form-item label="规则标识">
          <el-input v-model="query.ruleKey" placeholder="如 contract" clearable style="width:140px" @keyup.enter="loadList" @clear="loadList" />
        </el-form-item>
        <el-form-item label="状态">
          <el-select v-model="query.status" placeholder="全部" clearable style="width:100px" @change="loadList">
            <el-option label="启用" :value="1" />
            <el-option label="停用" :value="0" />
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" :icon="Search" @click="loadList">查询</el-button>
          <el-button @click="resetQuery">重置</el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <!-- 数据表格 -->
    <el-card shadow="never" class="mt-16">
      <el-table :data="list" v-loading="loading" stripe>
        <el-table-column prop="ruleKey" label="规则标识" width="130" />
        <el-table-column prop="ruleName" label="规则名称" min-width="120" />
        <el-table-column label="编码格式" min-width="200">
          <template #default="{ row }">
            <el-tag type="info" class="mono-tag">{{ formatPreview(row) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="seqLength" label="序号位数" width="80" align="center" />
        <el-table-column label="重置周期" width="90" align="center">
          <template #default="{ row }">
            <span>{{ resetTypeLabel(row.resetType) }}</span>
          </template>
        </el-table-column>
        <el-table-column prop="currentSeq" label="当前序号" width="90" align="center" />
        <el-table-column label="状态" width="80" align="center">
          <template #default="{ row }">
            <el-switch
              v-model="row.status"
              :active-value="1" :inactive-value="0"
              size="small"
              @change="(v: string | number | boolean) => doChangeStatus(row, Number(v))"
            />
          </template>
        </el-table-column>
        <el-table-column prop="remark" label="备注" min-width="100" show-overflow-tooltip />
        <el-table-column label="操作" width="180" align="center" fixed="right">
          <template #default="{ row }">
            <el-button type="primary" link size="small" @click="openEdit(row)">编辑</el-button>
            <el-button type="warning" link size="small" @click="doGenerate(row.ruleKey)">预览编码</el-button>
            <el-button type="info" link size="small" @click="doResetSeq(row)">重置序号</el-button>
            <el-button type="danger" link size="small" @click="doDelete(row.id)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>

      <el-pagination
        v-model:current-page="query.pageNum"
        v-model:page-size="query.pageSize"
        :total="total"
        layout="total, sizes, prev, pager, next"
        :page-sizes="[10, 20, 50]"
        class="mt-16"
        @change="loadList"
      />
    </el-card>

    <!-- 新增/编辑弹窗 -->
    <el-dialog v-model="dialogVisible" :title="dialogTitle" width="520px" destroy-on-close>
      <el-form ref="formRef" :model="form" :rules="formRules" label-width="100px">
        <el-form-item label="规则标识" prop="ruleKey">
          <el-input v-model="form.ruleKey" :disabled="!!form.id" placeholder="如 contract（创建后不可改）" />
        </el-form-item>
        <el-form-item label="规则名称" prop="ruleName">
          <el-input v-model="form.ruleName" placeholder="如 合同编码" />
        </el-form-item>
        <el-form-item label="前缀">
          <el-input v-model="form.prefix" placeholder="如 CTR（留空则无前缀）" style="width:200px" />
        </el-form-item>
        <el-form-item label="日期格式">
          <el-select v-model="form.dateFormat" style="width:200px">
            <el-option label="年月 yyyyMM" value="yyyyMM" />
            <el-option label="年 yyyy" value="yyyy" />
            <el-option label="年月日 yyyyMMdd" value="yyyyMMdd" />
            <el-option label="不拼日期" value="" />
          </el-select>
        </el-form-item>
        <el-form-item label="分隔符">
          <el-select v-model="form.sep" style="width:120px">
            <el-option label="横线 -" value="-" />
            <el-option label="下划线 _" value="_" />
            <el-option label="无分隔" value="" />
          </el-select>
        </el-form-item>
        <el-form-item label="序号位数" prop="seqLength">
          <el-input-number v-model="form.seqLength" :min="1" :max="10" />
          <span class="form-tip">位（前补零，如4位→0001）</span>
        </el-form-item>
        <el-form-item label="重置周期" prop="resetType">
          <el-radio-group v-model="form.resetType">
            <el-radio :value="0">不重置</el-radio>
            <el-radio :value="1">按年</el-radio>
            <el-radio :value="2">按月</el-radio>
            <el-radio :value="3">按日</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="状态">
          <el-radio-group v-model="form.status">
            <el-radio :value="1">启用</el-radio>
            <el-radio :value="0">停用</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="备注">
          <el-input v-model="form.remark" type="textarea" :rows="2" placeholder="说明用途" />
        </el-form-item>

        <!-- 格式预览 -->
        <el-form-item label="格式预览">
          <el-tag type="info" class="mono-tag">{{ formatPreview(form) }}</el-tag>
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
import { ref, reactive, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Plus, Search } from '@element-plus/icons-vue'
import { codeRuleApi, type SysCodeRule, type CodeRuleCreateDTO } from '@/api/sys/code'

const loading = ref(false)
const list = ref<SysCodeRule[]>([])
const total = ref(0)
const query = reactive({ pageNum: 1, pageSize: 20, ruleName: '', ruleKey: '', status: undefined as number | undefined })

const dialogVisible = ref(false)
const dialogTitle = ref('')
const submitting = ref(false)
const formRef = ref()
const form = reactive<CodeRuleCreateDTO>({
  ruleKey: '', ruleName: '', prefix: '', dateFormat: 'yyyyMM', sep: '-', seqLength: 4, resetType: 2, status: 1,
})
const formRules = {
  ruleKey: [{ required: true, message: '请输入规则标识', trigger: 'blur' }],
  ruleName: [{ required: true, message: '请输入规则名称', trigger: 'blur' }],
  seqLength: [{ required: true, message: '请设置序号位数', trigger: 'blur' }],
  resetType: [{ required: true, message: '请选择重置周期', trigger: 'change' }],
}

const RESET_TYPE_MAP: Record<number, string> = { 0: '不重置', 1: '按年', 2: '按月', 3: '按日' }
function resetTypeLabel(v: number) { return RESET_TYPE_MAP[v] ?? '-' }

function formatPreview(rule: Partial<CodeRuleCreateDTO & SysCodeRule>) {
  const parts: string[] = []
  const sep = rule.sep ?? '-'
  if (rule.prefix) parts.push(rule.prefix)
  if (rule.dateFormat) {
    const now = new Date()
    let d = ''
    if (rule.dateFormat === 'yyyy') d = String(now.getFullYear())
    else if (rule.dateFormat === 'yyyyMM') d = `${now.getFullYear()}${String(now.getMonth() + 1).padStart(2, '0')}`
    else if (rule.dateFormat === 'yyyyMMdd') d = `${now.getFullYear()}${String(now.getMonth() + 1).padStart(2, '0')}${String(now.getDate()).padStart(2, '0')}`
    if (d) parts.push(d)
  }
  const len = rule.seqLength ?? 4
  parts.push('0'.repeat(len - 1) + '1')
  return parts.join(sep)
}

async function loadList() {
  loading.value = true
  try {
    const res: any = await codeRuleApi.page(query)
    list.value = res.records ?? []
    total.value = res.total ?? 0
  } finally {
    loading.value = false
  }
}

function resetQuery() {
  Object.assign(query, { pageNum: 1, ruleName: '', ruleKey: '', status: undefined })
  loadList()
}

function openCreate() {
  Object.assign(form, { id: undefined, ruleKey: '', ruleName: '', prefix: '', dateFormat: 'yyyyMM', sep: '-', seqLength: 4, resetType: 2, status: 1, remark: '' })
  dialogTitle.value = '新增编码规则'
  dialogVisible.value = true
}

function openEdit(row: SysCodeRule) {
  Object.assign(form, { ...row })
  dialogTitle.value = '编辑编码规则'
  dialogVisible.value = true
}

async function doSubmit() {
  await formRef.value?.validate()
  submitting.value = true
  try {
    form.id ? await codeRuleApi.update(form.id, form) : await codeRuleApi.create(form)
    ElMessage.success('操作成功')
    dialogVisible.value = false
    loadList()
  } finally {
    submitting.value = false
  }
}

async function doChangeStatus(row: SysCodeRule, status: number) {
  try {
    await codeRuleApi.changeStatus(row.id, status)
    ElMessage.success(status === 1 ? '已启用' : '已停用')
  } catch {
    row.status = status === 1 ? 0 : 1
  }
}

async function doResetSeq(row: SysCodeRule) {
  await ElMessageBox.confirm(`确认将「${row.ruleName}」序列号归零？下次生成编码将从 0001 重新开始。`, '警告', { type: 'warning' })
  await codeRuleApi.resetSeq(row.id)
  ElMessage.success('序列号已重置')
  loadList()
}

async function doGenerate(ruleKey: string) {
  const code: any = await codeRuleApi.generate(ruleKey)
  ElMessageBox.alert(`生成的编码为：<b style="font-size:20px;color:#409EFF">${code}</b>`, '预览编码', {
    dangerouslyUseHTMLString: true, confirmButtonText: '关闭',
  })
}

async function doDelete(id: number) {
  await ElMessageBox.confirm('确认删除该编码规则？', '警告', { type: 'warning' })
  await codeRuleApi.delete(id)
  ElMessage.success('删除成功')
  loadList()
}

onMounted(loadList)
</script>

<style scoped lang="scss">
.mt-16 { margin-top: 16px; }
.mono-tag { font-family: monospace; letter-spacing: 0.5px; }
.form-tip { margin-left: 8px; color: #909399; font-size: 12px; }
</style>
