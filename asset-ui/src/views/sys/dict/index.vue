<template>
  <div class="page-container">
    <div class="page-header">
      <div class="page-header-left">
        <h2 class="page-title">业务字典</h2>
        <p class="page-desc">管理系统业务字典类型及字典数据</p>
      </div>
      <el-button type="primary" :icon="Plus" @click="openCreateType">新增字典类型</el-button>
    </div>

    <el-row :gutter="16">
      <!-- 字典类型列表 -->
      <el-col :span="10">
        <el-card shadow="never">
          <template #header>
            <div style="display:flex; align-items:center; justify-content:space-between">
              <span>字典类型</span>
              <div style="display:flex; gap:8px">
                <el-input v-model="typeQuery.dictName" placeholder="名称关键字" clearable
                  style="width:120px" size="small" @input="loadTypes" @clear="loadTypes" />
                <el-select v-model="typeQuery.status" placeholder="状态" clearable
                  style="width:90px" size="small" @change="loadTypes">
                  <el-option label="正常" :value="1" />
                  <el-option label="停用" :value="0" />
                </el-select>
              </div>
            </div>
          </template>
          <el-table
            :data="typeList"
            v-loading="typeLoading"
            highlight-current-row
            stripe
            size="small"
            @current-change="onTypeSelect"
          >
            <el-table-column prop="dictName" label="字典名称" min-width="120" show-overflow-tooltip />
            <el-table-column prop="dictType" label="类型标识" min-width="120" show-overflow-tooltip />
            <el-table-column label="状态" width="70" align="center">
              <template #default="{ row }">
                <el-switch
                  v-model="row.status"
                  :active-value="1" :inactive-value="0"
                  size="small"
                  @change="(v: string | number | boolean) => doChangeStatusType(row, Number(v))"
                />
              </template>
            </el-table-column>
            <el-table-column label="操作" width="110" align="center" fixed="right">
              <template #default="{ row }">
                <el-button type="primary" link size="small" @click.stop="openEditType(row)">编辑</el-button>
                <el-button type="warning" link size="small" @click.stop="doRefreshCache(row.dictType)">刷新缓存</el-button>
                <el-button type="danger" link size="small" @click.stop="doDeleteType(row.id)">删除</el-button>
              </template>
            </el-table-column>
          </el-table>
          <el-pagination
            v-model:current-page="typeQuery.pageNum"
            v-model:page-size="typeQuery.pageSize"
            :total="typeTotal"
            layout="total, prev, pager, next"
            small
            class="mt-12"
            @change="loadTypes"
          />
        </el-card>
      </el-col>

      <!-- 字典数据列表 -->
      <el-col :span="14">
        <el-card shadow="never">
          <template #header>
            <div style="display:flex; align-items:center; justify-content:space-between">
              <span>{{ currentType ? `字典数据：${currentType.dictName}（${currentType.dictType}）` : '字典数据（请先选择左侧字典类型）' }}</span>
              <el-button v-if="currentType" type="primary" size="small" :icon="Plus" @click="openCreateData">新增数据项</el-button>
            </div>
          </template>
          <el-empty v-if="!currentType" description="点击左侧字典类型查看字典数据" :image-size="80" />
          <el-table v-else :data="dataList" v-loading="dataLoading" stripe size="small">
            <el-table-column prop="dictLabel" label="标签" min-width="100">
              <template #default="{ row }">
                <el-tag v-if="row.cssClass" :type="(row.cssClass as any)" size="small">{{ row.dictLabel }}</el-tag>
                <span v-else>{{ row.dictLabel }}</span>
              </template>
            </el-table-column>
            <el-table-column prop="dictValue" label="键值" width="100" />
            <el-table-column prop="cssClass" label="样式" width="90" align="center">
              <template #default="{ row }">
                <span v-if="row.cssClass" style="font-size:12px; color:#606266">{{ row.cssClass }}</span>
                <span v-else style="color:#c0c4cc">默认</span>
              </template>
            </el-table-column>
            <el-table-column prop="sortOrder" label="排序" width="60" align="center" />
            <el-table-column prop="remark" label="备注" min-width="100" show-overflow-tooltip />
            <el-table-column label="状态" width="70" align="center">
              <template #default="{ row }">
                <el-switch
                  v-model="row.status"
                  :active-value="1" :inactive-value="0"
                  size="small"
                  @change="(v: string | number | boolean) => doChangeStatusData(row, Number(v))"
                />
              </template>
            </el-table-column>
            <el-table-column label="操作" width="100" align="center" fixed="right">
              <template #default="{ row }">
                <el-button type="primary" link size="small" @click="openEditData(row)">编辑</el-button>
                <el-button type="danger" link size="small" @click="doDeleteData(row.id)">删除</el-button>
              </template>
            </el-table-column>
          </el-table>
        </el-card>
      </el-col>
    </el-row>

    <!-- 字典类型弹窗 -->
    <el-dialog v-model="typeDialogVisible" :title="typeDialogTitle" width="440px" destroy-on-close>
      <el-form ref="typeFormRef" :model="typeForm" :rules="typeFormRules" label-width="90px">
        <el-form-item label="字典名称" prop="dictName">
          <el-input v-model="typeForm.dictName" placeholder="请输入字典名称" />
        </el-form-item>
        <el-form-item label="类型标识" prop="dictType">
          <el-input v-model="typeForm.dictType" :disabled="!!typeForm.id" placeholder="如：sys_user_status（创建后不可改）" />
        </el-form-item>
        <el-form-item label="状态">
          <el-radio-group v-model="typeForm.status">
            <el-radio :value="1">正常</el-radio>
            <el-radio :value="0">停用</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="备注">
          <el-input v-model="typeForm.remark" type="textarea" :rows="2" placeholder="字典用途说明" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="typeDialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="submitting" @click="doSubmitType">确定</el-button>
      </template>
    </el-dialog>

    <!-- 字典数据弹窗 -->
    <el-dialog v-model="dataDialogVisible" :title="dataDialogTitle" width="440px" destroy-on-close>
      <el-form ref="dataFormRef" :model="dataForm" :rules="dataFormRules" label-width="90px">
        <el-form-item label="字典标签" prop="dictLabel">
          <el-input v-model="dataForm.dictLabel" placeholder="显示文本，如：正常" />
        </el-form-item>
        <el-form-item label="字典键值" prop="dictValue">
          <el-input v-model="dataForm.dictValue" placeholder="存储值，如：1 或 NORMAL" />
        </el-form-item>
        <el-form-item label="样式属性">
          <el-select v-model="dataForm.cssClass" clearable placeholder="el-tag 颜色" style="width:100%">
            <el-option label="默认（无色）" value="" />
            <el-option label="主要 primary（蓝）" value="primary" />
            <el-option label="成功 success（绿）" value="success" />
            <el-option label="信息 info（灰）" value="info" />
            <el-option label="警告 warning（橙）" value="warning" />
            <el-option label="危险 danger（红）" value="danger" />
          </el-select>
        </el-form-item>
        <el-form-item label="排序">
          <el-input-number v-model="dataForm.sortOrder" :min="0" :max="999" />
        </el-form-item>
        <el-form-item label="状态">
          <el-radio-group v-model="dataForm.status">
            <el-radio :value="1">正常</el-radio>
            <el-radio :value="0">停用</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="备注">
          <el-input v-model="dataForm.remark" type="textarea" :rows="2" placeholder="备注" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dataDialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="submitting" @click="doSubmitData">确定</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Plus } from '@element-plus/icons-vue'
import { dictApi, type SysDictType, type SysDictData, type DictTypeCreateDTO, type DictDataCreateDTO } from '@/api/sys/dict'
import { clearDictCache } from '@/composables/useDict'

// ─── 字典类型 ───────────────────────────────────────────────────────────────
const typeLoading = ref(false)
const typeList = ref<SysDictType[]>([])
const typeTotal = ref(0)
const typeQuery = reactive({ pageNum: 1, pageSize: 20, dictName: '', dictType: '', status: undefined as number | undefined })
const currentType = ref<SysDictType | null>(null)

// ─── 字典数据 ───────────────────────────────────────────────────────────────
const dataLoading = ref(false)
const dataList = ref<SysDictData[]>([])

const submitting = ref(false)

// ─── 字典类型弹窗 ───────────────────────────────────────────────────────────
const typeDialogVisible = ref(false)
const typeDialogTitle = ref('')
const typeForm = reactive<DictTypeCreateDTO>({ dictName: '', dictType: '', status: 1 })
const typeFormRef = ref()
const typeFormRules = {
  dictName: [{ required: true, message: '请输入字典名称', trigger: 'blur' }],
  dictType: [{ required: true, message: '请输入类型标识', trigger: 'blur' }],
}

// ─── 字典数据弹窗 ───────────────────────────────────────────────────────────
const dataDialogVisible = ref(false)
const dataDialogTitle = ref('')
const dataForm = reactive<DictDataCreateDTO>({ dictType: '', dictLabel: '', dictValue: '', cssClass: '', sortOrder: 0, status: 1 })
const dataFormRef = ref()
const dataFormRules = {
  dictLabel: [{ required: true, message: '请输入字典标签', trigger: 'blur' }],
  dictValue: [{ required: true, message: '请输入字典键值', trigger: 'blur' }],
}

// ─── 字典类型操作 ───────────────────────────────────────────────────────────
async function loadTypes() {
  typeLoading.value = true
  try {
    const res: any = await dictApi.pageType(typeQuery)
    typeList.value = res.records ?? []
    typeTotal.value = res.total ?? 0
  } finally {
    typeLoading.value = false
  }
}

async function onTypeSelect(row: SysDictType | null) {
  currentType.value = row
  if (!row) { dataList.value = []; return }
  dataLoading.value = true
  try {
    const res: any = await dictApi.listAllData(row.dictType)
    dataList.value = res ?? []
  } finally {
    dataLoading.value = false
  }
}

function openCreateType() {
  Object.assign(typeForm, { id: undefined, dictName: '', dictType: '', status: 1, remark: '' })
  typeDialogTitle.value = '新增字典类型'
  typeDialogVisible.value = true
}

function openEditType(row: SysDictType) {
  Object.assign(typeForm, { ...row })
  typeDialogTitle.value = '编辑字典类型'
  typeDialogVisible.value = true
}

async function doSubmitType() {
  await typeFormRef.value?.validate()
  submitting.value = true
  try {
    typeForm.id ? await dictApi.updateType(typeForm.id, typeForm) : await dictApi.createType(typeForm)
    ElMessage.success('操作成功')
    typeDialogVisible.value = false
    loadTypes()
  } finally {
    submitting.value = false
  }
}

async function doChangeStatusType(row: SysDictType, status: number) {
  try {
    await dictApi.changeStatusType(row.id, status)
    ElMessage.success(status === 1 ? '已启用' : '已停用')
  } catch {
    row.status = status === 1 ? 0 : 1
  }
}

async function doDeleteType(id: number) {
  await ElMessageBox.confirm('确认删除该字典类型？将同时删除该类型下的所有字典数据', '警告', { type: 'warning' })
  await dictApi.deleteType(id)
  ElMessage.success('删除成功')
  if (currentType.value?.id === id) { currentType.value = null; dataList.value = [] }
  loadTypes()
}

async function doRefreshCache(dictType: string) {
  await dictApi.refreshCache(dictType)
  clearDictCache(dictType)
  ElMessage.success('缓存已刷新')
}

// ─── 字典数据操作 ───────────────────────────────────────────────────────────
function openCreateData() {
  Object.assign(dataForm, {
    id: undefined, dictType: currentType.value?.dictType,
    dictLabel: '', dictValue: '', cssClass: '', sortOrder: 0, status: 1, remark: '',
  })
  dataDialogTitle.value = '新增字典数据'
  dataDialogVisible.value = true
}

function openEditData(row: SysDictData) {
  Object.assign(dataForm, { ...row, cssClass: row.cssClass ?? '' })
  dataDialogTitle.value = '编辑字典数据'
  dataDialogVisible.value = true
}

async function doSubmitData() {
  await dataFormRef.value?.validate()
  submitting.value = true
  try {
    dataForm.id ? await dictApi.updateData(dataForm.id, dataForm) : await dictApi.createData(dataForm)
    ElMessage.success('操作成功')
    dataDialogVisible.value = false
    if (currentType.value) onTypeSelect(currentType.value)
  } finally {
    submitting.value = false
  }
}

async function doChangeStatusData(row: SysDictData, status: number) {
  try {
    await dictApi.changeStatusData(row.id, status)
    ElMessage.success(status === 1 ? '已启用' : '已停用')
  } catch {
    row.status = status === 1 ? 0 : 1
  }
}

async function doDeleteData(id: number) {
  await ElMessageBox.confirm('确认删除该字典数据项？', '警告', { type: 'warning' })
  await dictApi.deleteData(id)
  ElMessage.success('删除成功')
  if (currentType.value) onTypeSelect(currentType.value)
}

onMounted(loadTypes)
</script>

<style scoped lang="scss">
.mt-12 { margin-top: 12px; }
</style>
