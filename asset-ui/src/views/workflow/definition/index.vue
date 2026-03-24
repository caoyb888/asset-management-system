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
        <el-table-column label="业务类型" width="160">
          <template #default="{ row }">
            <el-tag size="small" type="info">{{ businessTypeLabel(row.businessType) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="状态" width="90">
          <template #default="{ row }">
            <el-tag :type="row.isEnabled === 1 ? 'success' : 'danger'" size="small">
              {{ row.isEnabled === 1 ? '启用' : '禁用' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" min-width="260" fixed="right">
          <template #default="{ row }">
            <el-button type="primary" link size="small" @click="handleEdit(row)">编辑</el-button>
            <el-button
              :type="row.isEnabled === 1 ? 'warning' : 'success'"
              link size="small"
              @click="handleToggle(row)"
            >{{ row.isEnabled === 1 ? '禁用' : '启用' }}</el-button>
            <el-button type="info" link size="small" @click="handlePreview(row)">预览流程图</el-button>
            <el-button type="success" link size="small" @click="handleRedeploy(row)">重新部署</el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-card>

    <!-- ── 新增/编辑弹窗 ── -->
    <el-dialog
      v-model="editDialog.visible"
      :title="editDialog.isEdit ? '编辑流程定义' : '新增流程定义'"
      width="860px"
      :close-on-click-modal="false"
      destroy-on-close
    >
      <el-form :model="editDialog.form" label-width="110px" :rules="rules" ref="formRef">
        <el-form-item label="流程 Key" prop="processKey">
          <el-input
            v-model="editDialog.form.processKey"
            :disabled="editDialog.isEdit"
            placeholder="如 INV_INTENTION"
          />
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
        <el-form-item label="状态">
          <el-switch
            v-model="editDialog.form.isEnabled"
            :active-value="1"
            :inactive-value="0"
            active-text="启用"
            inactive-text="禁用"
          />
        </el-form-item>

        <!-- 审批链路配置（Tab 切换） -->
        <el-form-item label="审批链路">
          <div style="width: 100%">
            <el-tabs v-model="editDialog.configMode" type="border-card" style="width: 100%">
              <!-- 可视化模式 -->
              <el-tab-pane label="可视化配置" name="visual">
                <div style="padding: 8px 0">
                  <FlowNodeDesigner
                    v-model="editDialog.form.nodeConfigs"
                  />
                  <!-- 实时预览按钮 -->
                  <div style="margin-top: 10px; text-align: right">
                    <el-button
                      size="small"
                      type="info"
                      plain
                      :loading="previewLoading"
                      @click="handlePreviewByNodes"
                    >预览生成的 BPMN XML</el-button>
                  </div>
                </div>
              </el-tab-pane>

              <!-- XML 源码模式 -->
              <el-tab-pane label="XML 源码" name="xml">
                <el-input
                  v-model="editDialog.form.bpmnXml"
                  type="textarea"
                  :rows="10"
                  placeholder="粘贴 BPMN 2.0 XML 定义（将覆盖可视化配置）"
                  style="font-family: monospace; font-size: 12px"
                />
                <div style="margin-top: 6px; color: var(--el-text-color-secondary); font-size: 12px">
                  提示：直接输入 XML 时，可视化配置将被忽略，以此处内容为准。
                </div>
              </el-tab-pane>
            </el-tabs>
          </div>
        </el-form-item>
      </el-form>

      <template #footer>
        <el-button @click="editDialog.visible = false">取消</el-button>
        <el-button type="primary" :loading="editDialog.saving" @click="handleSave">保存</el-button>
      </template>
    </el-dialog>

    <!-- ── 流程图预览弹窗 ── -->
    <el-dialog
      v-model="previewDialog.visible"
      :title="'流程图预览 — ' + previewDialog.processName"
      width="960px"
    >
      <BpmnViewer v-if="previewDialog.visible && previewDialog.xml" :xml="previewDialog.xml" />
      <el-empty v-else-if="previewDialog.visible && !previewDialog.xml" description="暂无 BPMN 定义" />
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import type { FormInstance, FormRules } from 'element-plus'
import {
  listDefinitions,
  saveDefinition,
  toggleDefinition,
  previewBpmnXml,
  getDefinitionNodes,
  previewBpmnByNodes,
  redeployDefinition,
  type WfProcessDefinition,
  type NodeConfigDTO,
} from '@/api/workflow/definition'
import { BUSINESS_TYPE_OPTIONS, businessTypeLabel } from '@/api/workflow/task'
import BpmnViewer from '@/components/workflow/BpmnViewer.vue'
import FlowNodeDesigner from '@/components/workflow/FlowNodeDesigner.vue'

// ─── 表格 ──────────────────────────────────────────────────

const loading = ref(false)
const tableData = ref<WfProcessDefinition[]>([])

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

// ─── 编辑弹窗 ──────────────────────────────────────────────

const formRef = ref<FormInstance>()

const rules: FormRules = {
  processKey: [{ required: true, message: '请输入流程 Key', trigger: 'blur' }],
  processName: [{ required: true, message: '请输入流程名称', trigger: 'blur' }],
  businessType: [{ required: true, message: '请选择业务类型', trigger: 'change' }],
}

const editDialog = reactive({
  visible: false,
  isEdit: false,
  saving: false,
  configMode: 'visual' as 'visual' | 'xml',
  form: {
    id: undefined as number | undefined,
    processKey: '',
    processName: '',
    businessType: '',
    isEnabled: 1,
    nodeConfigs: [] as NodeConfigDTO[],
    bpmnXml: '',
  },
})

function handleAdd() {
  editDialog.isEdit = false
  editDialog.configMode = 'visual'
  editDialog.form = {
    id: undefined,
    processKey: '',
    processName: '',
    businessType: '',
    isEnabled: 1,
    nodeConfigs: [],
    bpmnXml: '',
  }
  editDialog.visible = true
}

async function handleEdit(row: WfProcessDefinition) {
  editDialog.isEdit = true
  editDialog.configMode = 'visual'
  editDialog.form = {
    id: row.id,
    processKey: row.processKey,
    processName: row.processName,
    businessType: row.businessType,
    isEnabled: row.isEnabled,
    nodeConfigs: [],
    bpmnXml: row.bpmnXml || '',
  }
  editDialog.visible = true

  // 异步加载节点配置（可视化回显）
  try {
    const nodes = await getDefinitionNodes(row.id)
    editDialog.form.nodeConfigs = nodes || []
    if (!nodes || nodes.length === 0) {
      // 无可视化配置时切到 XML 模式
      editDialog.configMode = row.bpmnXml ? 'xml' : 'visual'
    }
  } catch {
    editDialog.configMode = row.bpmnXml ? 'xml' : 'visual'
  }
}

async function handleSave() {
  const valid = await formRef.value?.validate().catch(() => false)
  if (!valid) return

  // 确定提交数据：XML 模式清空 nodeConfigs，可视化模式清空 bpmnXml
  const payload: Record<string, unknown> = {
    id: editDialog.form.id,
    processKey: editDialog.form.processKey,
    processName: editDialog.form.processName,
    businessType: editDialog.form.businessType,
    isEnabled: editDialog.form.isEnabled,
  }

  if (editDialog.configMode === 'xml' && editDialog.form.bpmnXml.trim()) {
    payload.bpmnXml = editDialog.form.bpmnXml
  } else {
    const coreNodes = editDialog.form.nodeConfigs.filter(
      n => n.nodeType !== 'START' && n.nodeType !== 'END',
    )
    if (coreNodes.length === 0) {
      ElMessage.warning('请至少添加一个审批节点')
      return
    }
    payload.nodeConfigs = editDialog.form.nodeConfigs
  }

  editDialog.saving = true
  try {
    await saveDefinition(payload as any)
    ElMessage.success('保存成功，正在后台部署到 Flowable 引擎...')
    editDialog.visible = false
    loadData()
  } catch {
    // handled by interceptor
  } finally {
    editDialog.saving = false
  }
}

// ─── 启用/禁用 ─────────────────────────────────────────────

async function handleToggle(row: WfProcessDefinition) {
  try {
    await toggleDefinition(row.id)
    ElMessage.success(row.isEnabled === 1 ? '已禁用' : '已启用')
    loadData()
  } catch {
    // handled by interceptor
  }
}

// ─── 流程图预览 ────────────────────────────────────────────

const previewDialog = reactive({
  visible: false,
  processName: '',
  xml: '',
})

const previewLoading = ref(false)

async function handlePreview(row: WfProcessDefinition) {
  previewDialog.processName = row.processName
  previewDialog.xml = ''
  previewDialog.visible = true
  try {
    previewDialog.xml = (await previewBpmnXml(row.processKey)) || ''
  } catch {
    // handled by interceptor
  }
}

async function handlePreviewByNodes() {
  const { processKey, processName, nodeConfigs } = editDialog.form
  const coreNodes = nodeConfigs.filter(n => n.nodeType !== 'START' && n.nodeType !== 'END')
  if (!processKey || coreNodes.length === 0) {
    ElMessage.warning('请先填写流程 Key 并添加至少一个节点')
    return
  }
  previewLoading.value = true
  try {
    const xml = await previewBpmnByNodes({ processKey, processName, nodeConfigs })
    previewDialog.processName = processName || processKey
    previewDialog.xml = xml || ''
    previewDialog.visible = true
  } catch {
    // handled by interceptor
  } finally {
    previewLoading.value = false
  }
}

// ─── 手动重新部署 ──────────────────────────────────────────

async function handleRedeploy(row: WfProcessDefinition) {
  try {
    await ElMessageBox.confirm(
      `确认将流程 [${row.processName}] 重新部署到 Flowable 引擎？已运行的流程实例不受影响。`,
      '重新部署',
      { type: 'warning', confirmButtonText: '确认部署' },
    )
    await redeployDefinition(row.id)
    ElMessage.success('部署成功')
  } catch (e: any) {
    if (e !== 'cancel') {
      // handled by interceptor
    }
  }
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
