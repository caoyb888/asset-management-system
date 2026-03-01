<template>
  <div class="page-container">
    <div class="page-header">
      <div class="page-header-left">
        <h2 class="page-title">角色管理</h2>
        <p class="page-desc">管理系统角色及菜单权限分配</p>
      </div>
      <el-button type="primary" :icon="Plus" @click="openCreate">新增角色</el-button>
    </div>

    <el-card class="search-card" shadow="never">
      <el-form :model="query" inline>
        <el-form-item label="角色名称">
          <el-input v-model="query.roleName" placeholder="角色名称" clearable style="width:140px" />
        </el-form-item>
        <el-form-item label="角色编码">
          <el-input v-model="query.roleCode" placeholder="角色编码" clearable style="width:140px" />
        </el-form-item>
        <el-form-item label="状态">
          <el-select v-model="query.status" placeholder="全部" clearable style="width:100px">
            <el-option label="正常" :value="1" />
            <el-option label="停用" :value="0" />
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" :icon="Search" @click="loadData">查询</el-button>
          <el-button :icon="Refresh" @click="resetQuery">重置</el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <el-card class="table-card" shadow="never">
      <el-table :data="tableData" v-loading="loading" stripe border>
        <el-table-column prop="id"        label="ID"    width="70" />
        <el-table-column prop="roleName"  label="角色名称" min-width="140" />
        <el-table-column prop="roleCode"  label="角色编码" min-width="140" />
        <el-table-column prop="dataScope" label="数据范围" width="140">
          <template #default="{ row }">
            <el-tag size="small" type="info">{{ dataScopeMap[row.dataScope] ?? '-' }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="sortOrder" label="排序" width="80" align="center" />
        <el-table-column prop="status" label="状态" width="90" align="center">
          <template #default="{ row }">
            <el-tag :type="row.status === 1 ? 'success' : 'danger'" size="small">
              {{ row.status === 1 ? '正常' : '停用' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="createdAt" label="创建时间" width="170" />
        <el-table-column label="操作" width="260" fixed="right" align="center">
          <template #default="{ row }">
            <el-button type="primary" link size="small" @click="openEdit(row.id)">编辑</el-button>
            <el-button type="warning" link size="small" @click="openGrantMenu(row.id)">授权菜单</el-button>
            <el-button type="success" link size="small" @click="openDataScope(row.id)">数据权限</el-button>
            <el-button
              :type="row.status === 1 ? 'info' : 'success'" link size="small"
              @click="toggleStatus(row)">{{ row.status === 1 ? '停用' : '启用' }}</el-button>
            <el-button type="danger" link size="small" @click="doDelete(row.id)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>
      <el-pagination
        v-model:current-page="query.pageNum"
        v-model:page-size="query.pageSize"
        :page-sizes="[10, 20, 50, 100]"
        :total="total"
        layout="total, sizes, prev, pager, next"
        class="mt-16"
        @change="loadData"
      />
    </el-card>

    <!-- 新增/编辑弹窗 -->
    <el-dialog v-model="dialogVisible" :title="dialogTitle" width="500px" destroy-on-close>
      <el-form ref="formRef" :model="form" :rules="formRules" label-width="90px">
        <el-form-item label="角色名称" prop="roleName">
          <el-input v-model="form.roleName" placeholder="请输入角色名称" />
        </el-form-item>
        <el-form-item label="角色编码" prop="roleCode">
          <el-input v-model="form.roleCode" :disabled="!!form.id" placeholder="请输入角色编码（英文）" />
        </el-form-item>
        <el-form-item label="数据范围">
          <el-select v-model="form.dataScope" style="width:100%">
            <el-option v-for="(label, val) in dataScopeMap" :key="val" :label="label" :value="Number(val)" />
          </el-select>
        </el-form-item>
        <el-form-item label="排序">
          <el-input-number v-model="form.sortOrder" :min="0" :max="999" />
        </el-form-item>
        <el-form-item label="状态">
          <el-radio-group v-model="form.status">
            <el-radio :value="1">正常</el-radio>
            <el-radio :value="0">停用</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="备注">
          <el-input v-model="form.remark" type="textarea" :rows="2" placeholder="备注" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="submitting" @click="doSubmit">确定</el-button>
      </template>
    </el-dialog>

    <!-- 授权菜单弹窗 -->
    <el-dialog v-model="menuDialogVisible" title="授权菜单" width="480px" destroy-on-close>
      <el-tree
        ref="menuTreeRef"
        :data="menuTree"
        node-key="id"
        :props="{ label: 'menuName', children: 'children' }"
        show-checkbox
        :default-checked-keys="checkedMenuIds"
        highlight-current
        style="max-height:420px; overflow:auto"
      />
      <template #footer>
        <el-button @click="menuDialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="submitting" @click="doGrantMenu">确定</el-button>
      </template>
    </el-dialog>

    <!-- 数据权限弹窗 -->
    <el-dialog v-model="scopeDialogVisible" title="数据权限配置" width="480px" destroy-on-close>
      <el-form label-width="100px">
        <el-form-item label="数据范围">
          <el-select v-model="scopeForm.dataScope" style="width:100%" @change="onScopeChange">
            <el-option v-for="(label, val) in dataScopeMap" :key="val" :label="label" :value="Number(val)" />
          </el-select>
        </el-form-item>
        <el-form-item v-if="scopeForm.dataScope === 2" label="自定义部门">
          <div style="border:1px solid #e4e7ed; border-radius:4px; padding:8px; max-height:320px; overflow:auto; width:100%">
            <el-tree
              ref="deptTreeRef"
              :data="deptTree"
              node-key="id"
              :props="{ label: 'deptName', children: 'children' }"
              show-checkbox
              :default-checked-keys="scopeForm.deptIds"
              highlight-current
            />
          </div>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="scopeDialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="submitting" @click="doSetDataScope">确定</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Plus, Search, Refresh } from '@element-plus/icons-vue'
import { roleApi, type SysRole, type RoleCreateDTO } from '@/api/sys/role'
import { menuApi, type MenuTreeVO } from '@/api/sys/menu'
import { deptApi, type DeptTreeVO } from '@/api/sys/dept'

const loading = ref(false)
const submitting = ref(false)
const tableData = ref<SysRole[]>([])
const total = ref(0)
const query = reactive({ pageNum: 1, pageSize: 20, roleName: '', roleCode: '', status: undefined as number | undefined })

const dataScopeMap: Record<number, string> = { 1: '全部数据', 2: '自定义', 3: '本部门', 4: '本部门及以下', 5: '仅本人' }

// ─── 新增/编辑弹窗 ─────────────────────────────────────────────────────────
const dialogVisible = ref(false)
const dialogTitle = ref('')
const form = reactive<RoleCreateDTO>({ roleName: '', roleCode: '', dataScope: 1, sortOrder: 0, status: 1 })
const formRef = ref()
const formRules = {
  roleName: [{ required: true, message: '请输入角色名称', trigger: 'blur' }],
  roleCode: [{ required: true, message: '请输入角色编码', trigger: 'blur' }],
}

// ─── 授权菜单弹窗 ──────────────────────────────────────────────────────────
const menuDialogVisible = ref(false)
const menuTree = ref<MenuTreeVO[]>([])
const checkedMenuIds = ref<number[]>([])
const currentRoleId = ref<number>(0)
const menuTreeRef = ref()

// ─── 数据权限弹窗 ──────────────────────────────────────────────────────────
const scopeDialogVisible = ref(false)
const scopeForm = reactive<{ dataScope: number; deptIds: number[] }>({ dataScope: 1, deptIds: [] })
const deptTree = ref<DeptTreeVO[]>([])
const deptTreeRef = ref()

async function loadData() {
  loading.value = true
  try {
    const res: any = await roleApi.page(query)
    tableData.value = res.records ?? []
    total.value = res.total ?? 0
  } finally {
    loading.value = false
  }
}

function resetQuery() {
  Object.assign(query, { pageNum: 1, roleName: '', roleCode: '', status: undefined })
  loadData()
}

function openCreate() {
  Object.assign(form, { id: undefined, roleName: '', roleCode: '', dataScope: 1, sortOrder: 0, status: 1, remark: '' })
  dialogTitle.value = '新增角色'
  dialogVisible.value = true
}

async function openEdit(id: number) {
  const detail: any = await roleApi.getById(id)
  Object.assign(form, detail)
  dialogTitle.value = '编辑角色'
  dialogVisible.value = true
}

async function doSubmit() {
  await formRef.value?.validate()
  submitting.value = true
  try {
    form.id ? await roleApi.update(form.id, form) : await roleApi.create(form)
    ElMessage.success('操作成功')
    dialogVisible.value = false
    loadData()
  } finally {
    submitting.value = false
  }
}

async function openGrantMenu(roleId: number) {
  currentRoleId.value = roleId
  const [treeRes, detail]: any[] = await Promise.all([menuApi.tree(), roleApi.getById(roleId)])
  menuTree.value = treeRes ?? []
  checkedMenuIds.value = detail.menuIds ?? []
  menuDialogVisible.value = true
}

async function doGrantMenu() {
  const checked: number[] = menuTreeRef.value?.getCheckedKeys() ?? []
  const halfChecked: number[] = menuTreeRef.value?.getHalfCheckedKeys() ?? []
  submitting.value = true
  try {
    await roleApi.grantMenus(currentRoleId.value, [...checked, ...halfChecked])
    ElMessage.success('授权成功')
    menuDialogVisible.value = false
  } finally {
    submitting.value = false
  }
}

async function openDataScope(roleId: number) {
  currentRoleId.value = roleId
  const [detail, deptTreeData]: any[] = await Promise.all([
    roleApi.getById(roleId),
    deptApi.tree(1),
  ])
  scopeForm.dataScope = detail.dataScope ?? 1
  scopeForm.deptIds = detail.deptIds ?? []
  deptTree.value = deptTreeData ?? []
  scopeDialogVisible.value = true
}

function onScopeChange() {
  if (scopeForm.dataScope !== 2) scopeForm.deptIds = []
}

async function doSetDataScope() {
  const deptIds = scopeForm.dataScope === 2
    ? [...(deptTreeRef.value?.getCheckedKeys() ?? []), ...(deptTreeRef.value?.getHalfCheckedKeys() ?? [])]
    : []
  submitting.value = true
  try {
    await roleApi.setDataScope(currentRoleId.value, { dataScope: scopeForm.dataScope, deptIds })
    ElMessage.success('数据权限设置成功')
    scopeDialogVisible.value = false
    loadData()
  } finally {
    submitting.value = false
  }
}

async function toggleStatus(row: SysRole) {
  const s = row.status === 1 ? 0 : 1
  await roleApi.changeStatus(row.id, s)
  ElMessage.success(s === 1 ? '已启用' : '已停用')
  loadData()
}

async function doDelete(id: number) {
  await ElMessageBox.confirm('确认删除该角色？若角色下有用户或为超级管理员角色则不可删除', '警告', { type: 'warning' })
  await roleApi.delete(id)
  ElMessage.success('删除成功')
  loadData()
}

onMounted(loadData)
</script>
<style scoped lang="scss">
.mt-16 { margin-top: 16px; }
</style>
