<template>
  <div class="page-container">
    <div class="page-header">
      <div class="page-header-left">
        <h2 class="page-title">扩展字段管理</h2>
        <p class="page-desc">为各业务模块自定义扩展字段，无需开发介入</p>
      </div>
      <el-button type="primary" :icon="Plus" @click="openCreate">新增字段</el-button>
    </div>

    <!-- 模块 Tab -->
    <el-tabs v-model="activeModule" type="border-card" @tab-change="onTabChange">
      <el-tab-pane
        v-for="mod in modules"
        :key="mod.code"
        :label="mod.label"
        :name="mod.code"
      />
    </el-tabs>

    <!-- 字段列表 -->
    <el-card shadow="never" style="margin-top: 12px">
      <div style="display:flex; align-items:center; justify-content:space-between; margin-bottom:12px">
        <el-alert type="info" :closable="false" style="padding: 4px 12px">
          <template #default>
            拖动 <el-icon style="vertical-align:middle"><Rank /></el-icon> 图标可调整排序，松手后自动保存
          </template>
        </el-alert>
        <span style="color:#909399; font-size:13px; margin-left:16px; white-space:nowrap">
          共 {{ fieldList.length }} / 20 个字段
        </span>
      </div>

      <VueDraggable
        v-model="fieldList"
        target=".el-table__body tbody"
        handle=".drag-handle"
        :animation="150"
        @end="onDragEnd"
      >
        <el-table
          ref="tableRef"
          :data="fieldList"
          v-loading="loading"
          row-key="id"
          stripe
          size="small"
        >
          <el-table-column width="48" align="center">
            <template #default>
              <el-icon class="drag-handle" style="cursor:move; color:#c0c4cc"><Rank /></el-icon>
            </template>
          </el-table-column>
          <el-table-column prop="fieldKey" label="字段标识" min-width="120" show-overflow-tooltip />
          <el-table-column prop="fieldLabel" label="显示名称" min-width="110" show-overflow-tooltip />
          <el-table-column prop="fieldType" label="类型" width="100">
            <template #default="{ row }">
              <el-tag size="small" :type="fieldTypeTagType(row.fieldType)">
                {{ fieldTypeLabel(row.fieldType) }}
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column label="必填" width="70" align="center">
            <template #default="{ row }">
              <el-icon v-if="row.required" style="color:#67c23a"><CircleCheck /></el-icon>
              <el-icon v-else style="color:#dcdfe6"><CircleClose /></el-icon>
            </template>
          </el-table-column>
          <el-table-column label="表单显示" width="80" align="center">
            <template #default="{ row }">
              <el-icon v-if="row.showInForm" style="color:#67c23a"><CircleCheck /></el-icon>
              <el-icon v-else style="color:#dcdfe6"><CircleClose /></el-icon>
            </template>
          </el-table-column>
          <el-table-column label="列表显示" width="80" align="center">
            <template #default="{ row }">
              <el-icon v-if="row.showInList" style="color:#67c23a"><CircleCheck /></el-icon>
              <el-icon v-else style="color:#dcdfe6"><CircleClose /></el-icon>
            </template>
          </el-table-column>
          <el-table-column prop="sortOrder" label="排序" width="70" align="center" />
          <el-table-column label="操作" width="110" align="center" fixed="right">
            <template #default="{ row }">
              <el-button type="primary" link size="small" @click="openEdit(row)">编辑</el-button>
              <el-button type="danger" link size="small" @click="doDelete(row)">删除</el-button>
            </template>
          </el-table-column>
        </el-table>
      </VueDraggable>
    </el-card>

    <!-- 新增/编辑弹窗 -->
    <el-dialog
      v-model="dialogVisible"
      :title="isEdit ? '编辑扩展字段' : '新增扩展字段'"
      width="840px"
      :close-on-click-modal="false"
      destroy-on-close
    >
      <div style="display:flex; gap:24px">

        <!-- 左侧表单 -->
        <div style="flex:1; min-width:0">
          <el-form
            ref="formRef"
            :model="form"
            :rules="rules"
            label-width="100px"
            size="default"
          >
            <el-row :gutter="12">
              <el-col :span="12">
                <el-form-item label="字段标识" prop="fieldKey">
                  <el-input
                    v-model="form.fieldKey"
                    :disabled="isEdit"
                    placeholder="小写字母开头，含字母/数字/下划线"
                    maxlength="64"
                  />
                </el-form-item>
              </el-col>
              <el-col :span="12">
                <el-form-item label="显示名称" prop="fieldLabel">
                  <el-input v-model="form.fieldLabel" placeholder="中文名称" maxlength="50" />
                </el-form-item>
              </el-col>
            </el-row>

            <el-row :gutter="12">
              <el-col :span="12">
                <el-form-item label="字段类型" prop="fieldType">
                  <el-select v-model="form.fieldType" style="width:100%" @change="onTypeChange">
                    <el-option
                      v-for="t in fieldTypes"
                      :key="t.value"
                      :label="t.label"
                      :value="t.value"
                    />
                  </el-select>
                </el-form-item>
              </el-col>
              <el-col :span="12">
                <el-form-item label="排序">
                  <el-input-number v-model="form.sortOrder" :min="0" :max="999" style="width:100%" />
                </el-form-item>
              </el-col>
            </el-row>

            <el-row :gutter="12">
              <el-col :span="8">
                <el-form-item label="必填">
                  <el-switch v-model="form.required" />
                </el-form-item>
              </el-col>
              <el-col :span="8">
                <el-form-item label="表单显示">
                  <el-switch v-model="form.showInForm" />
                </el-form-item>
              </el-col>
              <el-col :span="8">
                <el-form-item label="列表显示">
                  <el-switch v-model="form.showInList" />
                </el-form-item>
              </el-col>
            </el-row>

            <el-row :gutter="12">
              <el-col :span="12">
                <el-form-item label="默认值">
                  <el-input v-model="form.defaultVal" placeholder="可选" />
                </el-form-item>
              </el-col>
              <el-col :span="12">
                <el-form-item label="占位提示">
                  <el-input v-model="form.placeholder" placeholder="可选" />
                </el-form-item>
              </el-col>
            </el-row>

            <!-- text/textarea：最大长度 -->
            <el-row v-if="['text','textarea'].includes(form.fieldType!)" :gutter="12">
              <el-col :span="12">
                <el-form-item label="最大长度">
                  <el-input-number v-model="form.maxLength" :min="1" :max="65535" style="width:100%" />
                </el-form-item>
              </el-col>
            </el-row>

            <!-- number：最小/最大值 -->
            <el-row v-if="form.fieldType === 'number'" :gutter="12">
              <el-col :span="12">
                <el-form-item label="最小值">
                  <el-input-number v-model="form.minVal" style="width:100%" />
                </el-form-item>
              </el-col>
              <el-col :span="12">
                <el-form-item label="最大值">
                  <el-input-number v-model="form.maxVal" style="width:100%" />
                </el-form-item>
              </el-col>
            </el-row>

            <!-- select/radio/checkbox：选项配置 -->
            <template v-if="['select','radio','checkbox'].includes(form.fieldType!)">
              <el-divider content-position="left" style="margin: 8px 0">选项配置</el-divider>
              <div
                v-for="(opt, idx) in form.optionsJson"
                :key="idx"
                style="display:flex; gap:8px; margin-bottom:8px; align-items:center"
              >
                <el-input v-model="opt.label" placeholder="选项标签" size="small" style="flex:1" />
                <el-input v-model="opt.value" placeholder="选项值" size="small" style="flex:1" />
                <el-button type="danger" :icon="Delete" circle size="small" @click="removeOption(idx)" />
              </div>
              <el-button type="primary" link :icon="Plus" size="small" @click="addOption">
                添加选项
              </el-button>
            </template>
          </el-form>
        </div>

        <!-- 右侧预览 -->
        <div style="width:220px; flex-shrink:0; border-left:1px solid #ebeef5; padding-left:20px">
          <div style="font-size:13px; color:#606266; margin-bottom:12px; font-weight:500">实时预览</div>
          <el-form label-width="0" size="default">
            <template v-if="form.fieldLabel">
              <div style="font-size:12px; color:#909399; margin-bottom:4px">
                {{ form.fieldLabel }}
                <el-tag v-if="form.required" type="danger" size="small" style="margin-left:4px">必填</el-tag>
              </div>
              <el-input
                v-if="form.fieldType === 'text'"
                :placeholder="form.placeholder || `请输入${form.fieldLabel}`"
                :maxlength="form.maxLength"
                show-word-limit
                disabled
              />
              <el-input
                v-else-if="form.fieldType === 'textarea'"
                type="textarea"
                :rows="2"
                :placeholder="form.placeholder || `请输入${form.fieldLabel}`"
                :maxlength="form.maxLength"
                disabled
              />
              <el-input-number
                v-else-if="form.fieldType === 'number'"
                :min="form.minVal"
                :max="form.maxVal"
                controls-position="right"
                style="width:100%"
                disabled
              />
              <el-date-picker
                v-else-if="form.fieldType === 'date'"
                type="date"
                :placeholder="form.placeholder || `请选择${form.fieldLabel}`"
                style="width:100%"
                disabled
              />
              <el-select
                v-else-if="form.fieldType === 'select'"
                :placeholder="form.placeholder || `请选择${form.fieldLabel}`"
                style="width:100%"
                disabled
              >
                <el-option
                  v-for="opt in form.optionsJson"
                  :key="opt.value"
                  :label="opt.label"
                  :value="opt.value"
                />
              </el-select>
              <el-radio-group v-else-if="form.fieldType === 'radio'" disabled>
                <el-radio
                  v-for="opt in form.optionsJson"
                  :key="opt.value"
                  :value="opt.value"
                >{{ opt.label }}</el-radio>
              </el-radio-group>
              <el-checkbox-group v-else-if="form.fieldType === 'checkbox'" disabled>
                <el-checkbox
                  v-for="opt in form.optionsJson"
                  :key="opt.value"
                  :value="opt.value"
                >{{ opt.label }}</el-checkbox>
              </el-checkbox-group>
            </template>
            <el-empty v-else description="填写字段信息后预览" :image-size="60" />
          </el-form>
        </div>
      </div>

      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="doSave">确定</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { ElMessage, ElMessageBox, type FormInstance, type FormRules } from 'element-plus'
import { Plus, Delete, CircleCheck, CircleClose, Rank } from '@element-plus/icons-vue'
import { VueDraggable } from 'vue-draggable-plus'
import { extFieldApi, type ExtFieldDef, type ExtFieldCreateDTO } from '@/api/sys/extField'
import { clearExtFieldCache } from '@/composables/useExtFields'

// ── 模块定义 ─────────────────────────────────────────────────────────────────

const modules = [
  { code: 'project',    label: '项目' },
  { code: 'shop',       label: '商铺' },
  { code: 'brand',      label: '品牌' },
  { code: 'merchant',   label: '商家' },
  { code: 'intention',  label: '意向' },
  { code: 'contract',   label: '合同' },
  { code: 'ledger',     label: '台账' },
  { code: 'change',     label: '变更' },
  { code: 'receivable', label: '应收' },
  { code: 'receipt',    label: '收款' },
]

const fieldTypes = [
  { value: 'text',     label: '单行文本' },
  { value: 'textarea', label: '多行文本' },
  { value: 'number',   label: '数字' },
  { value: 'date',     label: '日期' },
  { value: 'select',   label: '下拉单选' },
  { value: 'radio',    label: '单选按钮' },
  { value: 'checkbox', label: '多选' },
]

function fieldTypeLabel(type: string) {
  return fieldTypes.find(t => t.value === type)?.label ?? type
}

function fieldTypeTagType(type: string): '' | 'success' | 'warning' | 'info' | 'danger' {
  const map: Record<string, '' | 'success' | 'warning' | 'info' | 'danger'> = {
    text: '', textarea: 'info', number: 'warning',
    date: 'success', select: '', radio: 'success', checkbox: 'danger',
  }
  return map[type] ?? ''
}

// ── 列表状态 ─────────────────────────────────────────────────────────────────

const activeModule = ref('project')
const fieldList = ref<ExtFieldDef[]>([])
const loading = ref(false)

async function loadList() {
  loading.value = true
  try {
    fieldList.value = await extFieldApi.list(activeModule.value)
  } finally {
    loading.value = false
  }
}

function onTabChange() {
  loadList()
}

onMounted(() => loadList())

// ── 拖拽排序 ─────────────────────────────────────────────────────────────────

async function onDragEnd() {
  const items = fieldList.value.map((f, idx) => ({ id: f.id, sortOrder: idx }))
  try {
    await extFieldApi.updateSort(items)
    fieldList.value.forEach((f, idx) => { f.sortOrder = idx })
    clearExtFieldCache(activeModule.value)
    ElMessage.success('排序已保存')
  } catch {
    ElMessage.error('排序保存失败')
    loadList()
  }
}

// ── 弹窗 / 表单 ───────────────────────────────────────────────────────────────

const dialogVisible = ref(false)
const isEdit = ref(false)
const saving = ref(false)
const formRef = ref<FormInstance>()

interface FormModel extends ExtFieldCreateDTO {
  id?: number
  optionsJson: { label: string; value: string }[]
}

const form = reactive<FormModel>({
  id: undefined,
  moduleCode: 'project',
  fieldKey: '',
  fieldLabel: '',
  fieldType: 'text',
  optionsJson: [],
  required: false,
  sortOrder: 0,
  showInList: false,
  showInForm: true,
  defaultVal: undefined,
  placeholder: undefined,
  maxLength: undefined,
  minVal: undefined,
  maxVal: undefined,
})

const rules: FormRules = {
  fieldKey: [
    { required: true, message: '请输入字段标识', trigger: 'blur' },
    {
      pattern: /^[a-z][a-z0-9_]{0,62}$/,
      message: '小写字母开头，仅含小写字母/数字/下划线，不超过64字符',
      trigger: 'blur',
    },
  ],
  fieldLabel: [{ required: true, message: '请输入显示名称', trigger: 'blur' }],
  fieldType:  [{ required: true, message: '请选择字段类型', trigger: 'change' }],
}

function resetForm() {
  Object.assign(form, {
    id: undefined,
    moduleCode: activeModule.value,
    fieldKey: '',
    fieldLabel: '',
    fieldType: 'text',
    optionsJson: [],
    required: false,
    sortOrder: fieldList.value.length,
    showInList: false,
    showInForm: true,
    defaultVal: undefined,
    placeholder: undefined,
    maxLength: undefined,
    minVal: undefined,
    maxVal: undefined,
  })
}

function openCreate() {
  isEdit.value = false
  resetForm()
  dialogVisible.value = true
}

function openEdit(row: ExtFieldDef) {
  isEdit.value = true
  Object.assign(form, {
    id: row.id,
    moduleCode: row.moduleCode,
    fieldKey: row.fieldKey,
    fieldLabel: row.fieldLabel,
    fieldType: row.fieldType,
    optionsJson: row.optionsJson ? JSON.parse(JSON.stringify(row.optionsJson)) : [],
    required: row.required,
    sortOrder: row.sortOrder,
    showInList: row.showInList,
    showInForm: row.showInForm,
    defaultVal: row.defaultVal ?? undefined,
    placeholder: row.placeholder ?? undefined,
    maxLength: row.maxLength ?? undefined,
    minVal: row.minVal ?? undefined,
    maxVal: row.maxVal ?? undefined,
  })
  dialogVisible.value = true
}

function onTypeChange() {
  form.optionsJson = []
  form.maxLength = undefined
  form.minVal = undefined
  form.maxVal = undefined
}

function addOption() {
  form.optionsJson.push({ label: '', value: '' })
}

function removeOption(idx: number) {
  form.optionsJson.splice(idx, 1)
}

async function doSave() {
  await formRef.value?.validate()
  saving.value = true
  try {
    const payload: ExtFieldCreateDTO = { ...form }
    if (!['select', 'radio', 'checkbox'].includes(form.fieldType!)) {
      payload.optionsJson = []
    }
    if (isEdit.value && form.id) {
      await extFieldApi.update(form.id, payload)
      ElMessage.success('修改成功')
    } else {
      await extFieldApi.create(payload)
      ElMessage.success('新增成功')
    }
    dialogVisible.value = false
    clearExtFieldCache(activeModule.value)
    loadList()
  } catch (err: any) {
    ElMessage.error(err?.message || '操作失败')
  } finally {
    saving.value = false
  }
}

// ── 删除 ─────────────────────────────────────────────────────────────────────

async function doDelete(row: ExtFieldDef) {
  await ElMessageBox.confirm(
    `确认删除字段「${row.fieldLabel}」？<br>
    <span style="color:#e6a23c; font-size:12px">删除后已有数据中的对应字段不再显示，历史数据不受影响</span>`,
    '删除确认',
    {
      type: 'warning',
      dangerouslyUseHTMLString: true,
      confirmButtonText: '确认删除',
      confirmButtonClass: 'el-button--danger',
    }
  )
  try {
    await extFieldApi.delete(row.id)
    ElMessage.success('删除成功')
    clearExtFieldCache(activeModule.value)
    loadList()
  } catch {
    ElMessage.error('删除失败')
  }
}
</script>
