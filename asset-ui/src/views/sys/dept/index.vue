<template>
  <div class="page-container">
    <!-- 页头 -->
    <div class="page-header">
      <div class="page-header-left">
        <h2 class="page-title">机构管理</h2>
        <p class="page-desc">管理公司组织架构，支持多级机构树</p>
      </div>
      <div class="page-header-actions">
        <el-input
          v-model="keyword"
          placeholder="搜索机构名称"
          clearable
          :prefix-icon="Search"
          style="width:200px"
          @input="filterTable"
        />
        <el-button :icon="Expand" @click="toggleExpand(true)">展开全部</el-button>
        <el-button :icon="Fold" @click="toggleExpand(false)">折叠全部</el-button>
        <el-button type="primary" :icon="Plus" @click="openCreate(null)">新增机构</el-button>
      </div>
    </div>

    <!-- 机构树表格 -->
    <el-card shadow="never" class="table-card">
      <el-table
        ref="tableRef"
        :data="filteredData"
        v-loading="loading"
        row-key="id"
        :tree-props="{ children: 'children' }"
        border
        :default-expand-all="expandAll"
      >
        <el-table-column prop="deptName" label="机构名称" min-width="200">
          <template #default="{ row }">
            <el-icon v-if="row.status !== 1" class="icon-disabled"><WarningFilled /></el-icon>
            <span :class="{ 'text-disabled': row.status !== 1 }">{{ row.deptName }}</span>
          </template>
        </el-table-column>
        <el-table-column prop="deptCode" label="机构编码" width="140" />
        <el-table-column prop="leader"   label="负责人"   width="110" />
        <el-table-column prop="phone"    label="联系电话"  width="140" />
        <el-table-column prop="sortOrder" label="排序"   width="70" align="center" />
        <el-table-column prop="status" label="状态" width="80" align="center">
          <template #default="{ row }">
            <el-tag :type="row.status === 1 ? 'success' : 'danger'" size="small">
              {{ row.status === 1 ? '正常' : '停用' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="280" fixed="right" align="center">
          <template #default="{ row }">
            <el-button type="primary" link size="small" @click="openCreate(row)">新增子机构</el-button>
            <el-button type="primary" link size="small" @click="openEdit(row)">编辑</el-button>
            <el-button type="warning" link size="small" @click="openMove(row)">移动</el-button>
            <el-button type="info"    link size="small" @click="openUsers(row)">用户</el-button>
            <el-button type="danger"  link size="small" @click="doDelete(row.id)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-card>

    <!-- 新增/编辑弹窗 -->
    <el-dialog v-model="dialogVisible" :title="dialogTitle" width="520px" destroy-on-close>
      <el-form ref="formRef" :model="form" :rules="formRules" label-width="90px">
        <el-form-item label="上级机构">
          <el-tree-select
            v-model="form.parentId"
            :data="treeData"
            node-key="id"
            :props="{ label: 'deptName', children: 'children' }"
            placeholder="不选则为顶级机构"
            clearable
            check-strictly
            style="width:100%"
          />
        </el-form-item>
        <el-row :gutter="16">
          <el-col :span="12">
            <el-form-item label="机构名称" prop="deptName">
              <el-input v-model="form.deptName" placeholder="请输入机构名称" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="机构编码">
              <el-input v-model="form.deptCode" placeholder="请输入机构编码" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="负责人">
              <el-input v-model="form.leader" placeholder="请输入负责人" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="联系电话">
              <el-input v-model="form.phone" placeholder="请输入联系电话" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="排序">
              <el-input-number v-model="form.sortOrder" :min="0" :max="999" style="width:100%" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="状态">
              <el-radio-group v-model="form.status">
                <el-radio :value="1">正常</el-radio>
                <el-radio :value="0">停用</el-radio>
              </el-radio-group>
            </el-form-item>
          </el-col>
        </el-row>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="submitting" @click="doSubmit">确定</el-button>
      </template>
    </el-dialog>

    <!-- 移动机构弹窗 -->
    <el-dialog v-model="moveVisible" title="移动机构" width="420px" destroy-on-close>
      <el-alert type="info" :closable="false" class="mb-16">
        将「{{ moveNode?.deptName }}」移动到新的上级机构，其所有后代节点的路径将自动更新。
      </el-alert>
      <el-form label-width="90px">
        <el-form-item label="新上级机构">
          <el-tree-select
            v-model="moveTargetId"
            :data="moveTreeData"
            node-key="id"
            :props="{ label: 'deptName', children: 'children' }"
            placeholder="不选则提升为顶级"
            clearable
            check-strictly
            style="width:100%"
          />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="moveVisible = false">取消</el-button>
        <el-button type="primary" :loading="submitting" @click="doMove">确定</el-button>
      </template>
    </el-dialog>

    <!-- 机构用户抽屉 -->
    <el-drawer v-model="usersDrawerVisible" :title="`${currentDept?.deptName || ''} — 用户列表`" size="600px">
      <div class="drawer-header">
        <el-checkbox v-model="includeChildren" @change="loadDeptUsers">包含子机构用户</el-checkbox>
        <el-tag type="info" size="small">共 {{ deptUsers.length }} 人</el-tag>
      </div>
      <el-table :data="deptUsers" v-loading="usersLoading" stripe border size="small" class="mt-12">
        <el-table-column prop="username"  label="用户名"  width="120" />
        <el-table-column prop="realName"  label="姓名"    width="100" />
        <el-table-column prop="phone"     label="手机号"  width="130" />
        <el-table-column label="角色" show-overflow-tooltip>
          <template #default="{ row }">
            {{ (row.roleNames || []).join('、') || '-' }}
          </template>
        </el-table-column>
        <el-table-column prop="status" label="状态" width="70" align="center">
          <template #default="{ row }">
            <el-tag :type="row.status === 1 ? 'success' : 'danger'" size="small">
              {{ row.status === 1 ? '正常' : '停用' }}
            </el-tag>
          </template>
        </el-table-column>
      </el-table>
    </el-drawer>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, computed, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Plus, Search, Expand, Fold, WarningFilled } from '@element-plus/icons-vue'
import { deptApi, type DeptTreeVO, type DeptCreateDTO } from '@/api/sys/dept'
import type { UserDetailVO } from '@/api/sys/user'

// ─── 数据 ──────────────────────────────────────────────────────────────────

const loading    = ref(false)
const submitting = ref(false)
const treeData   = ref<DeptTreeVO[]>([])
const tableRef   = ref()
const expandAll  = ref(true)
const keyword    = ref('')

const filteredData = computed(() => {
  if (!keyword.value) return treeData.value
  return filterTree(treeData.value, keyword.value.toLowerCase())
})

function filterTree(nodes: DeptTreeVO[], kw: string): DeptTreeVO[] {
  const result: DeptTreeVO[] = []
  for (const node of nodes) {
    const match = node.deptName.toLowerCase().includes(kw)
    const filteredChildren = filterTree(node.children || [], kw)
    if (match || filteredChildren.length > 0) {
      result.push({ ...node, children: filteredChildren.length ? filteredChildren : node.children })
    }
  }
  return result
}

function filterTable() {
  // 触发 computed 重新计算即可
}

function toggleExpand(expand: boolean) {
  expandAll.value = expand
  // 遍历所有行展开/折叠
  const rows = tableRef.value?.store?.states?.data?.value || []
  rows.forEach((row: any) => tableRef.value?.toggleRowExpansion(row, expand))
}

async function loadData() {
  loading.value = true
  try {
    const res: any = await deptApi.tree()
    treeData.value = res ?? []
  } finally {
    loading.value = false
  }
}

// ─── 新增/编辑 ─────────────────────────────────────────────────────────────

const dialogVisible = ref(false)
const dialogTitle   = ref('')
const form = reactive<DeptCreateDTO>({ deptName: '', status: 1 })
const formRef = ref()
const formRules = {
  deptName: [{ required: true, message: '请输入机构名称', trigger: 'blur' }],
}

function openCreate(parent: DeptTreeVO | null) {
  Object.assign(form, {
    id: undefined,
    parentId: parent?.id ?? undefined,
    deptName: '', deptCode: '', leader: '', phone: '', email: '',
    sortOrder: 0, status: 1,
  })
  dialogTitle.value = parent ? `新增【${parent.deptName}】的子机构` : '新增机构'
  dialogVisible.value = true
}

function openEdit(row: DeptTreeVO) {
  Object.assign(form, { ...row })
  dialogTitle.value = '编辑机构'
  dialogVisible.value = true
}

async function doSubmit() {
  await formRef.value?.validate()
  submitting.value = true
  try {
    if (form.id) {
      await deptApi.update(form.id, form)
    } else {
      await deptApi.create(form)
    }
    ElMessage.success('操作成功')
    dialogVisible.value = false
    loadData()
  } finally {
    submitting.value = false
  }
}

async function doDelete(id: number) {
  await ElMessageBox.confirm('确认删除该机构？存在子机构或用户时不允许删除。', '警告', { type: 'warning' })
  await deptApi.delete(id)
  ElMessage.success('删除成功')
  loadData()
}

// ─── 移动子树 ──────────────────────────────────────────────────────────────

const moveVisible  = ref(false)
const moveNode     = ref<DeptTreeVO | null>(null)
const moveTargetId = ref<number | undefined>(undefined)

/** 过滤掉被移动节点及其后代，构建移动目标树 */
const moveTreeData = computed(() => {
  if (!moveNode.value) return treeData.value
  return excludeSubtree(treeData.value, moveNode.value.id)
})

function excludeSubtree(nodes: DeptTreeVO[], excludeId: number): DeptTreeVO[] {
  return nodes
    .filter(n => n.id !== excludeId)
    .map(n => ({ ...n, children: n.children ? excludeSubtree(n.children, excludeId) : undefined }))
}

function openMove(row: DeptTreeVO) {
  moveNode.value     = row
  moveTargetId.value = row.parentId === 0 ? undefined : row.parentId
  moveVisible.value  = true
}

async function doMove() {
  if (!moveNode.value) return
  submitting.value = true
  try {
    await deptApi.move(moveNode.value.id, moveTargetId.value ?? 0)
    ElMessage.success('移动成功，路径已自动更新')
    moveVisible.value = false
    loadData()
  } finally {
    submitting.value = false
  }
}

// ─── 机构用户 ──────────────────────────────────────────────────────────────

const usersDrawerVisible = ref(false)
const currentDept        = ref<DeptTreeVO | null>(null)
const deptUsers          = ref<UserDetailVO[]>([])
const usersLoading       = ref(false)
const includeChildren    = ref(false)

function openUsers(row: DeptTreeVO) {
  currentDept.value = row
  includeChildren.value = false
  usersDrawerVisible.value = true
  loadDeptUsers()
}

async function loadDeptUsers() {
  if (!currentDept.value) return
  usersLoading.value = true
  try {
    const res: any = await deptApi.getUsers(currentDept.value.id, includeChildren.value)
    deptUsers.value = Array.isArray(res) ? res : []
  } finally {
    usersLoading.value = false
  }
}

// ─── 初始化 ────────────────────────────────────────────────────────────────

onMounted(loadData)
</script>

<style scoped lang="scss">
.page-header-actions {
  display: flex;
  align-items: center;
  gap: 8px;
}

.text-disabled {
  color: var(--el-text-color-placeholder);
}

.icon-disabled {
  color: var(--el-color-danger);
  margin-right: 4px;
  vertical-align: middle;
}

.mb-16 { margin-bottom: 16px; }
.mt-12 { margin-top: 12px; }

.drawer-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 8px;
}
</style>
