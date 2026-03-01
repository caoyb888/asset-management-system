<template>
  <div class="page-container">
    <div class="page-header">
      <div class="page-header-left">
        <h2 class="page-title">分类管理</h2>
        <p class="page-desc">维护资产分类、业态分类等层级分类体系</p>
      </div>
      <el-button type="primary" :icon="Plus" @click="openCreate(null)">新增根分类</el-button>
    </div>

    <el-row :gutter="16">
      <!-- 左侧：分类维度选择 + 操作 -->
      <el-col :span="5">
        <el-card shadow="never">
          <template #header>
            <div style="display:flex; align-items:center; justify-content:space-between">
              <span>分类维度</span>
              <el-button type="primary" size="small" text @click="addTypeDialogVisible = true">+新建维度</el-button>
            </div>
          </template>
          <div v-if="typeList.length === 0" class="type-empty">暂无维度，请新建</div>
          <div
            v-for="t in typeList" :key="t"
            class="type-item"
            :class="{ active: currentType === t }"
            @click="selectType(t)"
          >
            <el-icon><FolderOpened v-if="currentType === t" /><Folder v-else /></el-icon>
            <span>{{ TYPE_LABEL_MAP[t] ?? t }}</span>
          </div>
        </el-card>
      </el-col>

      <!-- 右侧：分类树表格 -->
      <el-col :span="19">
        <el-card shadow="never">
          <template #header>
            <div style="display:flex; align-items:center; justify-content:space-between">
              <div style="display:flex; gap:8px; align-items:center">
                <span>{{ currentType ? `${TYPE_LABEL_MAP[currentType] ?? currentType} 分类树` : '分类树（请先选择左侧维度）' }}</span>
                <el-input v-if="currentType" v-model="nameFilter" placeholder="名称过滤" clearable style="width:140px" size="small" />
              </div>
              <div style="display:flex; gap:8px">
                <el-button v-if="currentType" size="small" @click="toggleExpand(true)">全部展开</el-button>
                <el-button v-if="currentType" size="small" @click="toggleExpand(false)">全部收起</el-button>
              </div>
            </div>
          </template>

          <el-empty v-if="!currentType" description="点击左侧分类维度查看分类树" :image-size="80" />

          <el-table
            v-else
            ref="tableRef"
            :data="filteredTree"
            v-loading="treeLoading"
            row-key="id"
            :tree-props="{ children: 'children' }"
            default-expand-all
            stripe
          >
            <el-table-column prop="categoryCode" label="编码" width="120" />
            <el-table-column prop="categoryName" label="名称" min-width="160" />
            <el-table-column prop="level" label="层级" width="70" align="center">
              <template #default="{ row }">
                <el-tag size="small" :type="['', 'primary', 'success', 'warning', 'danger'][row.level] as any || 'info'">
                  L{{ row.level }}
                </el-tag>
              </template>
            </el-table-column>
            <el-table-column prop="sortOrder" label="排序" width="70" align="center" />
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
            <el-table-column prop="remark" label="备注" min-width="120" show-overflow-tooltip />
            <el-table-column label="操作" width="180" align="center" fixed="right">
              <template #default="{ row }">
                <el-button type="primary" link size="small" @click="openCreate(row)">子节点</el-button>
                <el-button type="primary" link size="small" @click="openEdit(row)">编辑</el-button>
                <el-button type="danger" link size="small" @click="doDelete(row)">删除</el-button>
              </template>
            </el-table-column>
          </el-table>
        </el-card>
      </el-col>
    </el-row>

    <!-- 新增维度弹窗 -->
    <el-dialog v-model="addTypeDialogVisible" title="新建分类维度" width="400px" destroy-on-close>
      <el-form ref="typeFormRef" :model="typeForm" :rules="typeFormRules" label-width="90px">
        <el-form-item label="维度标识" prop="key">
          <el-input v-model="typeForm.key" placeholder="英文，如 asset_type" />
        </el-form-item>
        <el-form-item label="显示名称" prop="label">
          <el-input v-model="typeForm.label" placeholder="如 资产分类" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="addTypeDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="doAddType">确定</el-button>
      </template>
    </el-dialog>

    <!-- 分类节点弹窗 -->
    <el-dialog v-model="dialogVisible" :title="dialogTitle" width="480px" destroy-on-close>
      <el-form ref="formRef" :model="form" :rules="formRules" label-width="90px">
        <el-form-item label="所属维度">
          <el-input :value="TYPE_LABEL_MAP[form.categoryType] ?? form.categoryType" disabled />
        </el-form-item>
        <el-form-item v-if="form.parentId" label="父节点">
          <el-input :value="parentName" disabled />
        </el-form-item>
        <el-form-item label="分类编码" prop="categoryCode">
          <el-input v-model="form.categoryCode" placeholder="如 RE-COM" />
        </el-form-item>
        <el-form-item label="分类名称" prop="categoryName">
          <el-input v-model="form.categoryName" placeholder="如 商业地产" />
        </el-form-item>
        <el-form-item label="排序">
          <el-input-number v-model="form.sortOrder" :min="0" :max="999" />
        </el-form-item>
        <el-form-item label="状态">
          <el-radio-group v-model="form.status">
            <el-radio :value="1">启用</el-radio>
            <el-radio :value="0">停用</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="备注">
          <el-input v-model="form.remark" type="textarea" :rows="2" />
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
import { ref, reactive, computed, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Plus, Folder, FolderOpened } from '@element-plus/icons-vue'
import { categoryApi, type CategoryTreeVO, type CategoryCreateDTO } from '@/api/sys/category'

// ─── 维度管理 ────────────────────────────────────────────────────────────────
const TYPE_LABEL_MAP = reactive<Record<string, string>>({
  asset_type: '资产分类',
  format: '业态分类',
  area: '区域分类',
})

const typeList = ref<string[]>([])
const currentType = ref<string>('')

const addTypeDialogVisible = ref(false)
const typeFormRef = ref()
const typeForm = reactive({ key: '', label: '' })
const typeFormRules = {
  key: [{ required: true, message: '请输入维度标识', trigger: 'blur' }],
  label: [{ required: true, message: '请输入显示名称', trigger: 'blur' }],
}

function doAddType() {
  typeFormRef.value?.validate((valid: boolean) => {
    if (!valid) return
    if (!typeList.value.includes(typeForm.key)) {
      typeList.value.push(typeForm.key)
    }
    TYPE_LABEL_MAP[typeForm.key] = typeForm.label
    addTypeDialogVisible.value = false
    selectType(typeForm.key)
  })
}

// ─── 分类树 ──────────────────────────────────────────────────────────────────
const treeLoading = ref(false)
const tree = ref<CategoryTreeVO[]>([])
const nameFilter = ref('')
const tableRef = ref()

const filteredTree = computed(() => {
  if (!nameFilter.value) return tree.value
  return filterNodes(tree.value, nameFilter.value)
})

function filterNodes(nodes: CategoryTreeVO[], kw: string): CategoryTreeVO[] {
  return nodes.reduce<CategoryTreeVO[]>((acc, node) => {
    const filteredChildren = filterNodes(node.children ?? [], kw)
    if (node.categoryName.includes(kw) || node.categoryCode.includes(kw) || filteredChildren.length > 0) {
      acc.push({ ...node, children: filteredChildren })
    }
    return acc
  }, [])
}

async function selectType(type: string) {
  currentType.value = type
  await loadTree()
}

async function loadTree() {
  if (!currentType.value) return
  treeLoading.value = true
  try {
    const res: any = await categoryApi.tree({ categoryType: currentType.value })
    tree.value = res ?? []
  } finally {
    treeLoading.value = false
  }
}

function toggleExpand(expand: boolean) {
  const rows = (tableRef.value as any)?.$el?.querySelectorAll('.el-table__expand-icon') ?? []
  for (const row of rows) {
    const isExpanded = row.classList.contains('el-table__expand-icon--expanded')
    if ((expand && !isExpanded) || (!expand && isExpanded)) {
      (row as HTMLElement).click()
    }
  }
}

async function loadTypeList() {
  const res: any = await categoryApi.listTypes()
  const fromServer: string[] = res ?? []
  // 合并服务端已有维度
  fromServer.forEach(t => { if (!typeList.value.includes(t)) typeList.value.push(t) })
  if (typeList.value.length > 0 && !currentType.value) {
    await selectType(typeList.value[0])
  }
}

// ─── 分类节点 CRUD ────────────────────────────────────────────────────────────
const dialogVisible = ref(false)
const dialogTitle = ref('')
const submitting = ref(false)
const formRef = ref()
const parentName = ref('')
const form = reactive<CategoryCreateDTO>({
  categoryType: '', parentId: undefined, categoryCode: '', categoryName: '', sortOrder: 0, status: 1,
})
const formRules = {
  categoryCode: [{ required: true, message: '请输入分类编码', trigger: 'blur' }],
  categoryName: [{ required: true, message: '请输入分类名称', trigger: 'blur' }],
}

function openCreate(parentRow: CategoryTreeVO | null) {
  Object.assign(form, {
    id: undefined,
    categoryType: currentType.value,
    parentId: parentRow?.id ?? 0,
    categoryCode: '',
    categoryName: '',
    sortOrder: 0,
    status: 1,
    remark: '',
  })
  parentName.value = parentRow ? `${parentRow.categoryCode} ${parentRow.categoryName}` : ''
  dialogTitle.value = parentRow ? `新增子分类（${parentRow.categoryName}）` : '新增根分类'
  dialogVisible.value = true
}

function openEdit(row: CategoryTreeVO) {
  Object.assign(form, { ...row })
  parentName.value = ''
  dialogTitle.value = '编辑分类'
  dialogVisible.value = true
}

async function doSubmit() {
  await formRef.value?.validate()
  submitting.value = true
  try {
    form.id ? await categoryApi.update(form.id, form) : await categoryApi.create(form)
    ElMessage.success('操作成功')
    dialogVisible.value = false
    loadTree()
  } finally {
    submitting.value = false
  }
}

async function doChangeStatus(row: CategoryTreeVO, status: number) {
  try {
    await categoryApi.changeStatus(row.id, status)
    ElMessage.success(status === 1 ? '已启用' : '已停用')
  } catch {
    row.status = status === 1 ? 0 : 1
  }
}

async function doDelete(row: CategoryTreeVO) {
  await ElMessageBox.confirm(
    `确认删除分类「${row.categoryName}」？仅支持删除叶子节点。`,
    '警告', { type: 'warning' }
  )
  await categoryApi.delete(row.id)
  ElMessage.success('删除成功')
  loadTree()
}

onMounted(loadTypeList)
</script>

<style scoped lang="scss">
.type-empty { color: #c0c4cc; font-size: 13px; text-align: center; padding: 20px 0; }
.type-item {
  display: flex; align-items: center; gap: 6px;
  padding: 10px 12px; cursor: pointer; border-radius: 6px;
  color: #606266; font-size: 14px; transition: all 0.2s;
  &:hover { background: #f0f7ff; color: #409eff; }
  &.active { background: #ecf5ff; color: #409eff; font-weight: 600; }
}
</style>
