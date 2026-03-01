<template>
  <div class="user-layout">
    <!-- 左侧部门树 -->
    <el-card class="dept-tree-card" shadow="never">
      <template #header>
        <span class="tree-header">部门列表</span>
      </template>
      <el-input
        v-model="deptKeyword"
        placeholder="搜索部门"
        clearable
        :prefix-icon="Search"
        size="small"
        class="mb-12"
        @input="filterDeptTree"
      />
      <el-tree
        ref="deptTreeRef"
        :data="deptTreeData"
        :props="{ label: 'deptName', children: 'children' }"
        :filter-node-method="(val: string, data: any) => !val || data.deptName?.includes(val)"
        node-key="id"
        highlight-current
        default-expand-all
        @node-click="onDeptClick"
      />
    </el-card>

    <!-- 右侧主内容 -->
    <div class="user-main">
      <!-- 页头 -->
      <div class="page-header">
        <div class="page-header-left">
          <h2 class="page-title">用户管理</h2>
          <p class="page-desc">管理系统用户账户、角色分配</p>
        </div>
        <el-button type="primary" :icon="Plus" @click="openCreate">新增用户</el-button>
      </div>

      <!-- 搜索栏 -->
      <el-card class="search-card" shadow="never">
        <el-form :model="query" inline>
          <el-form-item label="用户名">
            <el-input v-model="query.username" placeholder="请输入用户名" clearable style="width:140px" />
          </el-form-item>
          <el-form-item label="真实姓名">
            <el-input v-model="query.realName" placeholder="请输入真实姓名" clearable style="width:140px" />
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

      <!-- 数据表格 -->
      <el-card class="table-card" shadow="never">
        <el-table :data="tableData" v-loading="loading" stripe border style="width:100%">
          <el-table-column prop="id"        label="ID"     width="70" />
          <el-table-column prop="username"  label="用户名" width="120" />
          <el-table-column prop="realName"  label="真实姓名" width="100" />
          <el-table-column prop="deptName"  label="所属部门" width="120" show-overflow-tooltip />
          <el-table-column label="角色" min-width="160" show-overflow-tooltip>
            <template #default="{ row }">
              <el-tag
                v-for="name in (row.roleNames || [])"
                :key="name"
                size="small"
                type="info"
                class="mr-4"
              >{{ name }}</el-tag>
            </template>
          </el-table-column>
          <el-table-column prop="phone"     label="手机号"  width="120" />
          <el-table-column prop="status" label="状态" width="80" align="center">
            <template #default="{ row }">
              <el-tag :type="row.status === 1 ? 'success' : 'danger'" size="small">
                {{ row.status === 1 ? '正常' : '停用' }}
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column prop="loginTime" label="最后登录" width="160" />
          <el-table-column label="操作" width="260" fixed="right" align="center">
            <template #default="{ row }">
              <el-button type="primary" link size="small" @click="openEdit(row)">编辑</el-button>
              <el-button type="warning" link size="small" @click="openResetPwd(row)">重置密码</el-button>
              <el-button
                :type="row.status === 1 ? 'info' : 'success'" link size="small"
                @click="toggleStatus(row)">{{ row.status === 1 ? '停用' : '启用' }}</el-button>
              <el-button type="danger" link size="small" @click="openForceOffline(row)">下线</el-button>
              <el-button type="danger" link size="small" @click="doDelete(row.id)">删除</el-button>
            </template>
          </el-table-column>
        </el-table>

        <el-pagination
          v-model:current-page="query.pageNum"
          v-model:page-size="query.pageSize"
          :page-sizes="[20, 50, 100]"
          :total="total"
          layout="total, sizes, prev, pager, next"
          class="mt-16"
          @change="loadData"
        />
      </el-card>
    </div>

    <!-- 新增/编辑弹窗 -->
    <el-dialog v-model="dialogVisible" :title="dialogTitle" width="600px" destroy-on-close>
      <el-form ref="formRef" :model="form" :rules="formRules" label-width="90px">
        <el-row :gutter="16">
          <el-col :span="12">
            <el-form-item label="用户名" prop="username">
              <el-input v-model="form.username" :disabled="!!form.id" placeholder="请输入用户名" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item v-if="!form.id" label="密码" prop="password">
              <el-input v-model="form.password" type="password" show-password placeholder="默认123456" />
            </el-form-item>
            <el-form-item v-else label="状态">
              <el-radio-group v-model="form.status">
                <el-radio :value="1">正常</el-radio>
                <el-radio :value="0">停用</el-radio>
              </el-radio-group>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="真实姓名">
              <el-input v-model="form.realName" placeholder="请输入真实姓名" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="手机号">
              <el-input v-model="form.phone" placeholder="请输入手机号" />
            </el-form-item>
          </el-col>
          <el-col :span="24">
            <el-form-item label="邮箱">
              <el-input v-model="form.email" placeholder="请输入邮箱" />
            </el-form-item>
          </el-col>
          <el-col :span="24">
            <el-form-item label="所属部门">
              <el-tree-select
                v-model="form.deptId"
                :data="deptTreeData"
                node-key="id"
                :props="{ label: 'deptName', children: 'children' }"
                placeholder="请选择部门"
                clearable
                check-strictly
                style="width:100%"
              />
            </el-form-item>
          </el-col>
          <el-col :span="24">
            <el-form-item label="角色">
              <el-select v-model="form.roleIds" multiple placeholder="请选择角色" style="width:100%" collapse-tags collapse-tags-tooltip>
                <el-option v-for="r in roleList" :key="r.id" :label="r.roleName" :value="r.id" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="24">
            <el-form-item label="岗位">
              <el-select v-model="form.postIds" multiple placeholder="请选择岗位" style="width:100%" collapse-tags collapse-tags-tooltip>
                <el-option v-for="p in postList" :key="p.id" :label="p.postName" :value="p.id" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col v-if="!form.id" :span="12">
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

    <!-- 重置密码弹窗 -->
    <el-dialog v-model="pwdDialogVisible" title="重置密码" width="420px" destroy-on-close>
      <el-form ref="pwdFormRef" :model="pwdForm" :rules="pwdFormRules" label-width="90px">
        <el-form-item label="新密码" prop="newPassword">
          <el-input v-model="pwdForm.newPassword" type="password" show-password placeholder="请输入新密码" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="pwdDialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="submitting" @click="doResetPwd">确定</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Plus, Search, Refresh } from '@element-plus/icons-vue'
import { userApi, type UserDetailVO, type UserCreateDTO } from '@/api/sys/user'
import { deptApi, type DeptTreeVO } from '@/api/sys/dept'
import { roleApi, type SysRole } from '@/api/sys/role'
import { postApi, type SysPost } from '@/api/sys/post'

// ─── 部门树 ────────────────────────────────────────────────────────────────

const deptTreeData = ref<DeptTreeVO[]>([])
const deptTreeRef = ref()
const deptKeyword = ref('')

async function loadDeptTree() {
  const res: any = await deptApi.tree(1)
  deptTreeData.value = Array.isArray(res) ? res : []
}

function filterDeptTree(val: string) {
  deptTreeRef.value?.filter(val)
}

function onDeptClick(node: DeptTreeVO) {
  query.deptId = node.id
  query.pageNum = 1
  loadData()
}

// ─── 角色/岗位下拉数据 ─────────────────────────────────────────────────────

const roleList = ref<SysRole[]>([])
const postList = ref<SysPost[]>([])

async function loadRolesAndPosts() {
  const [rRes, pRes]: any[] = await Promise.all([roleApi.list(), postApi.list()])
  roleList.value = Array.isArray(rRes) ? rRes : []
  postList.value = Array.isArray(pRes) ? pRes : []
}

// ─── 表格 ──────────────────────────────────────────────────────────────────

const loading = ref(false)
const tableData = ref<UserDetailVO[]>([])
const total = ref(0)
const query = reactive({
  pageNum: 1,
  pageSize: 20,
  username: '',
  realName: '',
  status: undefined as number | undefined,
  deptId: undefined as number | undefined,
})

async function loadData() {
  loading.value = true
  try {
    const res: any = await userApi.page(query)
    tableData.value = res.records ?? []
    total.value = res.total ?? 0
  } finally {
    loading.value = false
  }
}

function resetQuery() {
  Object.assign(query, { pageNum: 1, username: '', realName: '', status: undefined, deptId: undefined })
  deptTreeRef.value?.setCurrentKey(null)
  loadData()
}

// ─── 新增/编辑 ─────────────────────────────────────────────────────────────

const submitting = ref(false)
const dialogVisible = ref(false)
const dialogTitle = ref('')
const form = reactive<UserCreateDTO & { id?: number }>({ username: '', status: 1, roleIds: [], postIds: [] })
const formRef = ref()
const formRules = {
  username: [{ required: true, message: '请输入用户名', trigger: 'blur' }],
}

function openCreate() {
  Object.assign(form, {
    id: undefined, username: '', password: '',
    realName: '', phone: '', email: '', avatar: '',
    deptId: undefined, status: 1,
    roleIds: [], postIds: [],
  })
  dialogTitle.value = '新增用户'
  dialogVisible.value = true
}

async function openEdit(row: UserDetailVO) {
  const detail: any = await userApi.getById(row.id)
  Object.assign(form, {
    id: detail.id,
    username: detail.username,
    realName: detail.realName,
    phone: detail.phone,
    email: detail.email,
    avatar: detail.avatar,
    deptId: detail.deptId,
    status: detail.status,
    roleIds: detail.roleIds ?? [],
    postIds: detail.postIds ?? [],
  })
  dialogTitle.value = '编辑用户'
  dialogVisible.value = true
}

async function doSubmit() {
  await formRef.value?.validate()
  submitting.value = true
  try {
    if (form.id) {
      await userApi.update(form.id, form)
    } else {
      await userApi.create(form)
    }
    ElMessage.success('操作成功')
    dialogVisible.value = false
    loadData()
  } finally {
    submitting.value = false
  }
}

// ─── 重置密码 ──────────────────────────────────────────────────────────────

const pwdDialogVisible = ref(false)
const pwdForm = reactive({ userId: 0, newPassword: '' })
const pwdFormRef = ref()
const pwdFormRules = {
  newPassword: [{ required: true, message: '请输入新密码', trigger: 'blur' }],
}

function openResetPwd(row: UserDetailVO) {
  pwdForm.userId = row.id
  pwdForm.newPassword = ''
  pwdDialogVisible.value = true
}

async function doResetPwd() {
  await pwdFormRef.value?.validate()
  submitting.value = true
  try {
    await userApi.resetPassword({ userId: pwdForm.userId, newPassword: pwdForm.newPassword })
    ElMessage.success('密码重置成功')
    pwdDialogVisible.value = false
  } finally {
    submitting.value = false
  }
}

// ─── 状态切换 / 强制下线 / 删除 ────────────────────────────────────────────

async function toggleStatus(row: UserDetailVO) {
  const newStatus = row.status === 1 ? 0 : 1
  await userApi.changeStatus(row.id, newStatus)
  ElMessage.success(newStatus === 1 ? '已启用' : '已停用')
  loadData()
}

async function openForceOffline(row: UserDetailVO) {
  await ElMessageBox.confirm(`确认将用户 "${row.username}" 强制下线？`, '提示', { type: 'warning' })
  await userApi.forceOffline(row.id)
  ElMessage.success('已强制下线')
}

async function doDelete(id: number) {
  await ElMessageBox.confirm('确认删除该用户？', '警告', { type: 'warning' })
  await userApi.delete(id)
  ElMessage.success('删除成功')
  loadData()
}

// ─── 初始化 ────────────────────────────────────────────────────────────────

onMounted(() => {
  loadDeptTree()
  loadRolesAndPosts()
  loadData()
})
</script>

<style scoped lang="scss">
.user-layout {
  display: flex;
  gap: 16px;
  align-items: flex-start;
}

.dept-tree-card {
  width: 220px;
  flex-shrink: 0;

  :deep(.el-card__header) {
    padding: 12px 16px;
  }
}

.tree-header {
  font-size: 14px;
  font-weight: 600;
  color: var(--el-text-color-primary);
}

.user-main {
  flex: 1;
  min-width: 0;
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.mb-12 { margin-bottom: 12px; }
.mr-4  { margin-right: 4px; }
.mt-16 { margin-top: 16px; }
</style>
