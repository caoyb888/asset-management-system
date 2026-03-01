<template>
  <div class="page-container">
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
          <el-input v-model="query.username" placeholder="请输入用户名" clearable style="width:160px" />
        </el-form-item>
        <el-form-item label="真实姓名">
          <el-input v-model="query.realName" placeholder="请输入真实姓名" clearable style="width:160px" />
        </el-form-item>
        <el-form-item label="状态">
          <el-select v-model="query.status" placeholder="全部" clearable style="width:110px">
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
        <el-table-column prop="username"  label="用户名" width="130" />
        <el-table-column prop="realName"  label="真实姓名" width="110" />
        <el-table-column prop="deptName"  label="所属部门" width="130" show-overflow-tooltip />
        <el-table-column prop="phone"     label="手机号"  width="130" />
        <el-table-column prop="email"     label="邮箱"   show-overflow-tooltip />
        <el-table-column prop="status" label="状态" width="90" align="center">
          <template #default="{ row }">
            <el-tag :type="row.status === 1 ? 'success' : 'danger'" size="small">
              {{ row.status === 1 ? '正常' : '停用' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="loginTime" label="最后登录" width="170" />
        <el-table-column prop="createdAt" label="创建时间" width="170" />
        <el-table-column label="操作" width="220" fixed="right" align="center">
          <template #default="{ row }">
            <el-button type="primary" link size="small" @click="openEdit(row)">编辑</el-button>
            <el-button type="warning" link size="small" @click="openResetPwd(row)">重置密码</el-button>
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
        :page-sizes="[20, 50, 100]"
        :total="total"
        layout="total, sizes, prev, pager, next"
        class="mt-16"
        @change="loadData"
      />
    </el-card>

    <!-- 新增/编辑弹窗 -->
    <el-dialog v-model="dialogVisible" :title="dialogTitle" width="560px" destroy-on-close>
      <el-form ref="formRef" :model="form" :rules="formRules" label-width="90px">
        <el-form-item label="用户名" prop="username">
          <el-input v-model="form.username" :disabled="!!form.id" placeholder="请输入用户名" />
        </el-form-item>
        <el-form-item v-if="!form.id" label="密码" prop="password">
          <el-input v-model="form.password" type="password" show-password placeholder="请输入初始密码" />
        </el-form-item>
        <el-form-item label="真实姓名">
          <el-input v-model="form.realName" placeholder="请输入真实姓名" />
        </el-form-item>
        <el-form-item label="手机号">
          <el-input v-model="form.phone" placeholder="请输入手机号" />
        </el-form-item>
        <el-form-item label="邮箱">
          <el-input v-model="form.email" placeholder="请输入邮箱" />
        </el-form-item>
        <el-form-item label="状态">
          <el-radio-group v-model="form.status">
            <el-radio :value="1">正常</el-radio>
            <el-radio :value="0">停用</el-radio>
          </el-radio-group>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="submitting" @click="doSubmit">确定</el-button>
      </template>
    </el-dialog>

    <!-- 重置密码弹窗 -->
    <el-dialog v-model="pwdDialogVisible" title="重置密码" width="400px" destroy-on-close>
      <el-form ref="pwdFormRef" :model="pwdForm" label-width="90px">
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

const loading = ref(false)
const submitting = ref(false)
const tableData = ref<UserDetailVO[]>([])
const total = ref(0)
const query = reactive({ pageNum: 1, pageSize: 20, username: '', realName: '', status: undefined as number | undefined })

const dialogVisible = ref(false)
const dialogTitle = ref('')
const form = reactive<UserCreateDTO>({ username: '', status: 1 })
const formRef = ref()
const formRules = {
  username: [{ required: true, message: '请输入用户名', trigger: 'blur' }],
  password: [{ required: true, message: '请输入初始密码', trigger: 'blur' }],
}

const pwdDialogVisible = ref(false)
const pwdForm = reactive({ userId: 0, newPassword: '' })
const pwdFormRef = ref()

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
  Object.assign(query, { pageNum: 1, username: '', realName: '', status: undefined })
  loadData()
}

function openCreate() {
  Object.assign(form, { id: undefined, username: '', password: '', realName: '', phone: '', email: '', status: 1 })
  dialogTitle.value = '新增用户'
  dialogVisible.value = true
}

function openEdit(row: UserDetailVO) {
  Object.assign(form, { id: row.id, username: row.username, realName: row.realName, phone: row.phone, email: row.email, status: row.status })
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

function openResetPwd(row: UserDetailVO) {
  pwdForm.userId = row.id
  pwdForm.newPassword = ''
  pwdDialogVisible.value = true
}

async function doResetPwd() {
  submitting.value = true
  try {
    await userApi.resetPassword(pwdForm)
    ElMessage.success('密码重置成功')
    pwdDialogVisible.value = false
  } finally {
    submitting.value = false
  }
}

async function toggleStatus(row: UserDetailVO) {
  const newStatus = row.status === 1 ? 0 : 1
  await userApi.changeStatus(row.id, newStatus)
  ElMessage.success(newStatus === 1 ? '已启用' : '已停用')
  loadData()
}

async function doDelete(id: number) {
  await ElMessageBox.confirm('确认删除该用户？', '警告', { type: 'warning' })
  await userApi.delete(id)
  ElMessage.success('删除成功')
  loadData()
}

onMounted(loadData)
</script>

<style scoped lang="scss">
@use '@/styles/variables' as *;
.mt-16 { margin-top: 16px; }
</style>
