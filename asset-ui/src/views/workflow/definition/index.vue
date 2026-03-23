<template>
  <div class="page-container">
    <el-card shadow="never">
      <template #header>
        <div class="card-header">
          <span>流程定义管理</span>
          <el-button type="primary" @click="handleAdd">新增流程定义</el-button>
        </div>
      </template>

      <el-table :data="tableData" v-loading="loading" stripe>
        <el-table-column label="流程 Key" prop="processKey" width="200" />
        <el-table-column label="流程名称" prop="processName" width="180" />
        <el-table-column label="业务类型" prop="businessType" width="180">
          <template #default="{ row }">
            <el-tag size="small" type="info">{{ businessTypeLabel(row.businessType) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="审批人策略" width="120">
          <template #default="{ row }">
            {{ approverStrategyLabel(row.approverStrategy) }}
          </template>
        </el-table-column>
        <el-table-column label="状态" width="100">
          <template #default="{ row }">
            <el-tag :type="row.isEnabled === 1 ? 'success' : 'danger'" size="small">
              {{ row.isEnabled === 1 ? '启用' : '禁用' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="版本" prop="version" width="70" />
        <el-table-column label="操作" width="240" fixed="right">
          <template #default="{ row }">
            <el-button type="primary" link size="small" @click="handleEdit(row)">编辑</el-button>
            <el-button
              :type="row.isEnabled === 1 ? 'warning' : 'success'"
              link
              size="small"
              @click="handleToggle(row)"
            >
              {{ row.isEnabled === 1 ? '禁用' : '启用' }}
            </el-button>
            <el-button type="info" link size="small" @click="handlePreview(row)">预览流程图</el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-card>

    <!-- 编辑弹窗 -->
    <el-dialog v-model="editDialog.visible" :title="editDialog.isEdit ? '编辑流程定义' : '新增流程定义'" width="700px">
      <el-form :model="editDialog.form" label-width="110px" :rules="rules" ref="formRef">
        <el-form-item label="流程 Key" prop="processKey">
          <el-input v-model="editDialog.form.processKey" :disabled="editDialog.isEdit" placeholder="如 INV_INTENTION" />
        </el-form-item>
        <el-form-item label="流程名称" prop="processName">
          <el-input v-model="editDialog.form.processName" placeholder="如 意向协议审批" />
        </el-form-item>
        <el-form-item label="业务类型" prop="businessType">
          <el-select v-model="editDialog.form.businessType" placeholder="选择业务类型" style="width: 100%">
            <el-option
              v-for="opt in BUSINESS_TYPE_OPTIONS"
              :key="opt.value"
              :label="opt.label"
              :value="opt.value"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="审批人策略" prop="approverStrategy">
          <el-select v-model="editDialog.form.approverStrategy" placeholder="选择策略" style="width: 100%">
            <el-option
              v-for="opt in APPROVER_STRATEGY_OPTIONS"
              :key="opt.value"
              :label="opt.label"
              :value="opt.value"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="策略参数">
          <el-input v-model="editDialog.form.approverConfig" type="textarea" :rows="2" placeholder='JSON 配置，如 {"roleCode":"APPROVAL_MANAGER"}' />
        </el-form-item>
        <el-form-item label="BPMN XML">
          <el-input v-model="editDialog.form.bpmnXml" type="textarea" :rows="8" placeholder="粘贴 BPMN 2.0 XML 定义" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="editDialog.visible = false">取消</el-button>
        <el-button type="primary" :loading="editDialog.saving" @click="handleSave">保存</el-button>
      </template>
    </el-dialog>

    <!-- 流程图预览弹窗 -->
    <el-dialog v-model="previewDialog.visible" :title="'流程图预览 - ' + previewDialog.processName" width="900px">
      <BpmnViewer v-if="previewDialog.visible && previewDialog.xml" :xml="previewDialog.xml" />
      <el-empty v-else-if="previewDialog.visible && !previewDialog.xml" description="暂无 BPMN 定义" />
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import type { FormInstance, FormRules } from 'element-plus'
import {
  listDefinitions,
  saveDefinition,
  toggleDefinition,
  previewBpmnXml,
  APPROVER_STRATEGY_OPTIONS,
  approverStrategyLabel,
  type WfProcessDefinition,
} from '@/api/workflow/definition'
import { BUSINESS_TYPE_OPTIONS, businessTypeLabel } from '@/api/workflow/task'
import BpmnViewer from '@/components/workflow/BpmnViewer.vue'

const loading = ref(false)
const tableData = ref<WfProcessDefinition[]>([])
const formRef = ref<FormInstance>()

const rules: FormRules = {
  processKey: [{ required: true, message: '请输入流程 Key', trigger: 'blur' }],
  processName: [{ required: true, message: '请输入流程名称', trigger: 'blur' }],
  businessType: [{ required: true, message: '请选择业务类型', trigger: 'change' }],
  approverStrategy: [{ required: true, message: '请选择审批人策略', trigger: 'change' }],
}

const editDialog = reactive({
  visible: false,
  isEdit: false,
  saving: false,
  form: {
    id: undefined as number | undefined,
    processKey: '',
    processName: '',
    businessType: '',
    approverStrategy: 'ROLE',
    approverConfig: '',
    bpmnXml: '',
    isEnabled: 1,
    version: 0,
  },
})

const previewDialog = reactive({
  visible: false,
  processName: '',
  xml: '',
})

onMounted(() => loadData())

async function loadData() {
  loading.value = true
  try {
    tableData.value = await listDefinitions()
  } catch {
    // handled by interceptor
  } finally {
    loading.value = false
  }
}

function handleAdd() {
  editDialog.isEdit = false
  editDialog.form = {
    id: undefined,
    processKey: '',
    processName: '',
    businessType: '',
    approverStrategy: 'ROLE',
    approverConfig: '',
    bpmnXml: '',
    isEnabled: 1,
    version: 0,
  }
  editDialog.visible = true
}

function handleEdit(row: WfProcessDefinition) {
  editDialog.isEdit = true
  editDialog.form = { ...row, bpmnXml: row.bpmnXml || '', approverConfig: row.approverConfig || '' }
  editDialog.visible = true
}

async function handleSave() {
  const valid = await formRef.value?.validate().catch(() => false)
  if (!valid) return

  editDialog.saving = true
  try {
    await saveDefinition(editDialog.form)
    ElMessage.success('保存成功')
    editDialog.visible = false
    loadData()
  } catch {
    // handled by interceptor
  } finally {
    editDialog.saving = false
  }
}

async function handleToggle(row: WfProcessDefinition) {
  try {
    await toggleDefinition(row.id)
    ElMessage.success(row.isEnabled === 1 ? '已禁用' : '已启用')
    loadData()
  } catch {
    // handled by interceptor
  }
}

async function handlePreview(row: WfProcessDefinition) {
  previewDialog.processName = row.processName
  try {
    const xml = await previewBpmnXml(row.processKey)
    previewDialog.xml = xml || ''
  } catch {
    previewDialog.xml = ''
  }
  previewDialog.visible = true
}
</script>

<style scoped>
.page-container {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}
</style>
