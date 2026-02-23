<template>
  <div class="page-container">
    <el-card>
      <template #header>
        <div class="card-header">
          <span>收款项目管理</span>
          <el-button type="primary" :icon="Plus" @click="handleCreate">新增项目</el-button>
        </div>
      </template>

      <el-alert type="info" :closable="false" class="mb-3">
        <template #default>
          拖动行左侧 <el-icon style="vertical-align: middle"><Rank /></el-icon> 图标可调整排序，松手后自动保存
        </template>
      </el-alert>

      <VueDraggable
        v-model="list"
        target=".el-table__body tbody"
        handle=".drag-handle"
        :animation="150"
        @end="handleSortEnd"
      >
        <el-table v-loading="loading" :data="list" border stripe row-key="id">
          <el-table-column width="48" align="center">
            <template #default>
              <el-icon class="drag-handle" style="cursor: move; color: #909399"><Rank /></el-icon>
            </template>
          </el-table-column>
          <el-table-column prop="sortOrder" label="序号" width="70" align="center" />
          <el-table-column prop="itemCode" label="项目编码" width="110" />
          <el-table-column prop="itemName" label="项目名称" min-width="120" />
          <el-table-column prop="itemType" label="类型" width="110">
            <template #default="{ row }">
              <el-tag :type="itemTypeColor(row.itemType)" size="small">
                {{ ITEM_TYPE_MAP[row.itemType] ?? '-' }}
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column prop="isRequired" label="必填" width="80" align="center">
            <template #default="{ row }">
              <el-tag :type="row.isRequired === 1 ? 'danger' : 'info'" size="small">
                {{ row.isRequired === 1 ? '必填' : '选填' }}
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column prop="status" label="状态" width="90">
            <template #default="{ row }">
              <el-switch
                :model-value="row.status === 1"
                :disabled="row.itemType === 1"
                :title="row.itemType === 1 ? '租金类不可停用' : ''"
                @change="(val) => handleToggleStatus(row, val as boolean)"
              />
            </template>
          </el-table-column>
          <el-table-column label="操作" width="120" fixed="right">
            <template #default="{ row }">
              <el-button link type="primary" @click="handleEdit(row)">编辑</el-button>
              <el-button link type="danger" :disabled="row.isRequired === 1" @click="handleDelete(row)">删除</el-button>
            </template>
          </el-table-column>
        </el-table>
      </VueDraggable>
    </el-card>

    <!-- 新增/编辑弹窗 -->
    <el-dialog
      v-model="dialogVisible"
      :title="editId ? '编辑收款项目' : '新增收款项目'"
      width="480px"
      :close-on-click-modal="false"
      @close="resetForm"
    >
      <el-form ref="formRef" :model="form" :rules="rules" label-width="100px">
        <el-form-item label="项目编码" prop="itemCode">
          <el-input v-model="form.itemCode" placeholder="如 FI008" :disabled="!!editId" />
        </el-form-item>
        <el-form-item label="项目名称" prop="itemName">
          <el-input v-model="form.itemName" placeholder="如 水电费" />
        </el-form-item>
        <el-form-item label="类型" prop="itemType">
          <el-select v-model="form.itemType" placeholder="请选择" style="width: 100%" @change="onItemTypeChange">
            <el-option v-for="(label, val) in ITEM_TYPE_MAP" :key="val" :label="label" :value="Number(val)" />
          </el-select>
        </el-form-item>
        <el-form-item label="是否必填">
          <el-switch
            v-model="form.isRequiredBool"
            active-text="必填"
            inactive-text="选填"
            :disabled="form.itemType === 1"
          />
          <span v-if="form.itemType === 1" class="tip-text">租金类自动设为必填</span>
        </el-form-item>
        <el-form-item label="排序" prop="sortOrder">
          <el-input-number v-model="form.sortOrder" :min="1" :max="999" style="width: 140px" />
        </el-form-item>
        <el-form-item label="状态">
          <el-switch v-model="form.statusBool" active-text="启用" inactive-text="停用" />
        </el-form-item>
      </el-form>

      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="handleSave">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { ElMessage, ElMessageBox, type FormInstance, type FormRules } from 'element-plus'
import { Plus, Rank } from '@element-plus/icons-vue'
import { VueDraggable } from 'vue-draggable-plus'
import {
  getFeeItemAllList,
  createFeeItem,
  updateFeeItem,
  deleteFeeItem,
  toggleFeeItemStatus,
  updateFeeItemSort,
  type FeeItemVO,
  type FeeItemSaveDTO,
} from '@/api/inv/config'

// ─── 字典 ───
type TagType = 'primary' | 'success' | 'warning' | 'danger' | 'info' | undefined
const ITEM_TYPE_MAP: Record<number, string> = { 1: '租金类', 2: '保证金类', 3: '服务费类' }
const itemTypeColor = (t: number): TagType => ({ 1: 'primary', 2: 'warning', 3: 'info' } as Record<number, TagType>)[t]

// ─── 数据 ───
const loading = ref(false)
const list = ref<FeeItemVO[]>([])

async function fetchList() {
  loading.value = true
  try { list.value = await getFeeItemAllList() }
  finally { loading.value = false }
}

// ─── 拖拽排序 ───
async function handleSortEnd() {
  // 重新计算 sortOrder（1-based），提交到后端
  const items = list.value.map((item, idx) => ({ id: item.id, sortOrder: idx + 1 }))
  // 本地先更新显示值
  list.value.forEach((item, idx) => { item.sortOrder = idx + 1 })
  try {
    await updateFeeItemSort(items)
    ElMessage.success('排序已保存')
  } catch {
    // 失败则重新拉取恢复
    fetchList()
  }
}

// ─── 弹窗 ───
const dialogVisible = ref(false)
const saving = ref(false)
const editId = ref<number | null>(null)
const formRef = ref<FormInstance>()

interface FormModel {
  itemCode: string
  itemName: string
  itemType: number | null
  isRequiredBool: boolean
  sortOrder: number
  statusBool: boolean
}

const defaultForm = (): FormModel => ({
  itemCode: '', itemName: '', itemType: null,
  isRequiredBool: false, sortOrder: list.value.length + 1, statusBool: true,
})

const form = ref<FormModel>(defaultForm())

const rules: FormRules = {
  itemCode: [{ required: true, message: '请输入项目编码', trigger: 'blur' }],
  itemName: [{ required: true, message: '请输入项目名称', trigger: 'blur' }],
  itemType: [{ required: true, message: '请选择类型', trigger: 'change' }],
  sortOrder: [{ required: true, message: '请输入排序值', trigger: 'blur' }],
}

function onItemTypeChange(val: number) {
  // 租金类自动必填
  if (val === 1) form.value.isRequiredBool = true
}

function resetForm() {
  editId.value = null
  form.value = defaultForm()
  formRef.value?.clearValidate()
}

function handleCreate() {
  resetForm()
  form.value.sortOrder = list.value.length + 1
  dialogVisible.value = true
}

function handleEdit(row: FeeItemVO) {
  editId.value = row.id
  form.value = {
    itemCode: row.itemCode,
    itemName: row.itemName,
    itemType: row.itemType,
    isRequiredBool: row.isRequired === 1,
    sortOrder: row.sortOrder,
    statusBool: row.status === 1,
  }
  dialogVisible.value = true
}

async function handleSave() {
  await formRef.value?.validate()
  saving.value = true
  try {
    const dto: FeeItemSaveDTO = {
      itemCode: form.value.itemCode,
      itemName: form.value.itemName,
      itemType: form.value.itemType!,
      isRequired: form.value.isRequiredBool ? 1 : 0,
      sortOrder: form.value.sortOrder,
      status: form.value.statusBool ? 1 : 0,
    }
    if (editId.value) {
      await updateFeeItem(editId.value, { ...dto, id: editId.value })
    } else {
      await createFeeItem(dto)
    }
    ElMessage.success('保存成功')
    dialogVisible.value = false
    fetchList()
  } finally {
    saving.value = false
  }
}

async function handleToggleStatus(row: FeeItemVO, enabled: boolean) {
  await toggleFeeItemStatus(row.id, enabled ? 1 : 0)
  row.status = enabled ? 1 : 0
  ElMessage.success(enabled ? '已启用' : '已停用')
}

async function handleDelete(row: FeeItemVO) {
  await ElMessageBox.confirm(`确认删除 "${row.itemName}"？`, '提示', { type: 'warning' })
  await deleteFeeItem(row.id)
  ElMessage.success('删除成功')
  fetchList()
}

onMounted(fetchList)
</script>

<style scoped>
.card-header { display: flex; justify-content: space-between; align-items: center; }
.mb-3 { margin-bottom: 12px; }
.tip-text { margin-left: 8px; font-size: 12px; color: #909399; }
</style>
