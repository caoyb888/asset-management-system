<template>
  <div class="project-page">
    <!-- 搜索栏 -->
    <el-card shadow="never" class="search-card">
      <el-form :model="query" inline @keyup.enter="handleSearch">
        <el-form-item label="项目名称">
          <el-input
            v-model="query.projectName"
            placeholder="请输入项目名称"
            clearable
            style="width: 180px"
          />
        </el-form-item>
        <el-form-item label="项目编号">
          <el-input
            v-model="query.projectCode"
            placeholder="请输入项目编号"
            clearable
            style="width: 150px"
          />
        </el-form-item>
        <el-form-item label="运营状态">
          <el-select
            v-model="query.operationStatus"
            placeholder="全部"
            clearable
            style="width: 120px"
          >
            <el-option
              v-for="item in OPERATION_STATUS_OPTIONS"
              :key="item.value"
              :label="item.label"
              :value="item.value"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="省份">
          <el-input
            v-model="query.province"
            placeholder="省份"
            clearable
            style="width: 110px"
          />
        </el-form-item>
        <el-form-item label="城市">
          <el-input
            v-model="query.city"
            placeholder="城市"
            clearable
            style="width: 110px"
          />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" :icon="Search" @click="handleSearch">查询</el-button>
          <el-button :icon="Refresh" @click="handleReset">重置</el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <!-- 操作栏 + 表格 -->
    <el-card shadow="never" class="table-card">
      <div class="toolbar">
        <el-button type="primary" :icon="Plus" @click="handleAdd">新增项目</el-button>
      </div>

      <el-table
        v-loading="loading"
        :data="tableData"
        border
        stripe
        row-key="id"
        style="width: 100%"
      >
        <el-table-column type="index" label="序号" width="60" align="center" />
        <el-table-column prop="projectCode" label="项目编号" width="130" show-overflow-tooltip />
        <el-table-column prop="projectName" label="项目名称" min-width="160" show-overflow-tooltip />
        <el-table-column prop="companyName" label="所属公司" width="160" show-overflow-tooltip />
        <el-table-column label="地区" width="130">
          <template #default="{ row }">
            {{ [row.province, row.city].filter(Boolean).join(' · ') || '-' }}
          </template>
        </el-table-column>
        <el-table-column label="运营状态" width="100" align="center">
          <template #default="{ row }">
            <el-tag :type="statusTagType(row.operationStatus)" size="small">
              {{ row.operationStatusName }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="propertyTypeName" label="产权性质" width="90" align="center" />
        <el-table-column prop="businessTypeName" label="经营类型" width="90" align="center" />
        <el-table-column label="建筑面积(㎡)" width="120" align="right">
          <template #default="{ row }">
            {{ row.buildingArea != null ? Number(row.buildingArea).toLocaleString() : '-' }}
          </template>
        </el-table-column>
        <el-table-column label="经营面积(㎡)" width="120" align="right">
          <template #default="{ row }">
            {{ row.operatingArea != null ? Number(row.operatingArea).toLocaleString() : '-' }}
          </template>
        </el-table-column>
        <el-table-column prop="openingDate" label="开业时间" width="110" align="center" />
        <el-table-column prop="managerName" label="负责人" width="90" align="center" />
        <el-table-column prop="createdAt" label="创建时间" width="160" align="center" />
        <el-table-column label="操作" width="150" align="center" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" size="small" @click="handleEdit(row)">
              编辑
            </el-button>
            <el-divider direction="vertical" />
            <el-popconfirm
              title="确认删除该项目？"
              confirm-button-text="确认"
              cancel-button-text="取消"
              @confirm="handleDelete(row.id)"
            >
              <template #reference>
                <el-button link type="danger" size="small">删除</el-button>
              </template>
            </el-popconfirm>
          </template>
        </el-table-column>
      </el-table>

      <!-- 分页 -->
      <div class="pagination">
        <el-pagination
          v-model:current-page="query.pageNum"
          v-model:page-size="query.pageSize"
          :page-sizes="[10, 20, 50, 100]"
          :total="total"
          layout="total, sizes, prev, pager, next, jumper"
          background
          @size-change="fetchList"
          @current-change="fetchList"
        />
      </div>
    </el-card>

    <!-- 新增/编辑 Dialog -->
    <el-dialog
      v-model="dialogVisible"
      :title="dialogTitle"
      width="760px"
      :close-on-click-modal="false"
      destroy-on-close
      @close="resetForm"
    >
      <el-form
        ref="formRef"
        :model="form"
        :rules="formRules"
        label-width="100px"
        label-position="right"
      >
        <el-row :gutter="20">
          <el-col :span="12">
            <el-form-item label="项目编号" prop="projectCode">
              <el-input
                v-model="form.projectCode"
                placeholder="请输入项目编号"
                :disabled="isEdit"
                maxlength="50"
              />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="项目名称" prop="projectName">
              <el-input v-model="form.projectName" placeholder="请输入项目名称" maxlength="200" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="所属公司" prop="companyId">
              <el-select
                v-model="form.companyId"
                placeholder="请选择所属公司"
                style="width: 100%"
                filterable
              >
                <el-option
                  v-for="c in companyOptions"
                  :key="c.id"
                  :label="c.companyName"
                  :value="c.id"
                />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="运营状态" prop="operationStatus">
              <el-select v-model="form.operationStatus" placeholder="请选择" style="width: 100%">
                <el-option
                  v-for="item in OPERATION_STATUS_OPTIONS"
                  :key="item.value"
                  :label="item.label"
                  :value="item.value"
                />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="产权性质">
              <el-select v-model="form.propertyType" placeholder="请选择" clearable style="width: 100%">
                <el-option label="国有" :value="1" />
                <el-option label="集体" :value="2" />
                <el-option label="私有" :value="3" />
                <el-option label="其他" :value="4" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="经营类型">
              <el-select v-model="form.businessType" placeholder="请选择" clearable style="width: 100%">
                <el-option label="自持" :value="1" />
                <el-option label="租赁" :value="2" />
                <el-option label="合作" :value="3" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="所在省份">
              <el-input v-model="form.province" placeholder="省份" maxlength="50" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="所在城市">
              <el-input v-model="form.city" placeholder="城市" maxlength="50" />
            </el-form-item>
          </el-col>
          <el-col :span="24">
            <el-form-item label="项目地址">
              <el-input v-model="form.address" placeholder="详细地址" maxlength="500" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="建筑面积(㎡)">
              <el-input-number
                v-model="form.buildingArea"
                :min="0"
                :precision="2"
                :step="100"
                placeholder="建筑面积"
                style="width: 100%"
                controls-position="right"
              />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="经营面积(㎡)">
              <el-input-number
                v-model="form.operatingArea"
                :min="0"
                :precision="2"
                :step="100"
                placeholder="经营面积"
                style="width: 100%"
                controls-position="right"
              />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="开业时间">
              <el-date-picker
                v-model="form.openingDate"
                type="date"
                placeholder="选择开业日期"
                format="YYYY-MM-DD"
                value-format="YYYY-MM-DD"
                style="width: 100%"
              />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="负责人">
              <el-select
                v-model="form.managerId"
                placeholder="请选择负责人"
                style="width: 100%"
                clearable
                filterable
              >
                <el-option
                  v-for="u in userOptions"
                  :key="u.id"
                  :label="u.realName || u.username"
                  :value="u.id"
                />
              </el-select>
            </el-form-item>
          </el-col>
        </el-row>
      </el-form>

      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="submitting" @click="handleSubmit">
          {{ isEdit ? '保存修改' : '确认新增' }}
        </el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { ElMessage, type FormInstance, type FormRules } from 'element-plus'
import { Search, Refresh, Plus } from '@element-plus/icons-vue'
import {
  getProjectPage,
  createProject,
  updateProject,
  deleteProject,
  type ProjectVO,
  type ProjectQuery,
  type ProjectSaveDTO,
} from '@/api/base/project'
import { getCompanyList, type CompanyOption } from '@/api/base/company'
import { getUserList, type UserOption } from '@/api/base/user'
import { useAppStore } from '@/store/modules/app'

useAppStore().setPageTitle('项目管理')

// ─────────── 枚举常量 ───────────
const OPERATION_STATUS_OPTIONS = [
  { label: '筹备', value: 0 },
  { label: '开业', value: 1 },
  { label: '停业', value: 2 },
]

function statusTagType(status: number | null) {
  return ({ 0: 'info', 1: 'success', 2: 'danger' }[status ?? -1] ?? 'info') as 'success' | 'info' | 'danger'
}

// ─────────── 列表 ───────────
const loading = ref(false)
const tableData = ref<ProjectVO[]>([])
const total = ref(0)

const query = reactive<ProjectQuery & { pageNum: number; pageSize: number }>({
  pageNum: 1,
  pageSize: 20,
  projectName: '',
  projectCode: '',
  operationStatus: '',
  province: '',
  city: '',
})

async function fetchList() {
  loading.value = true
  try {
    const res = await getProjectPage({
      ...query,
      operationStatus: query.operationStatus === '' ? undefined : query.operationStatus,
    })
    tableData.value = res.records
    total.value = res.total
  } finally {
    loading.value = false
  }
}

function handleSearch() {
  query.pageNum = 1
  fetchList()
}

function handleReset() {
  query.projectName = ''
  query.projectCode = ''
  query.operationStatus = ''
  query.province = ''
  query.city = ''
  query.pageNum = 1
  fetchList()
}

// ─────────── 公司 / 用户 下拉选项 ───────────
const companyOptions = ref<CompanyOption[]>([])
const userOptions = ref<UserOption[]>([])

async function loadOptions() {
  const [companies, users] = await Promise.all([getCompanyList(), getUserList()])
  companyOptions.value = companies
  userOptions.value = users
}

onMounted(() => {
  fetchList()
  loadOptions()
})

// ─────────── 新增/编辑 Dialog ───────────
const dialogVisible = ref(false)
const isEdit = ref(false)
const submitting = ref(false)
const formRef = ref<FormInstance>()

const defaultForm = (): ProjectSaveDTO => ({
  id: undefined,
  projectCode: '',
  projectName: '',
  companyId: null,
  province: '',
  city: '',
  address: '',
  propertyType: null,
  businessType: null,
  buildingArea: null,
  operatingArea: null,
  operationStatus: 0,
  openingDate: '',
  managerId: null,
})

const form = reactive<ProjectSaveDTO>(defaultForm())

const dialogTitle = ref('新增项目')

const formRules: FormRules = {
  projectCode: [{ required: true, message: '项目编号不能为空', trigger: 'blur' }],
  projectName: [{ required: true, message: '项目名称不能为空', trigger: 'blur' }],
  companyId:   [{ required: true, message: '所属公司不能为空', trigger: 'change' }],
}

function handleAdd() {
  isEdit.value = false
  dialogTitle.value = '新增项目'
  Object.assign(form, defaultForm())
  dialogVisible.value = true
}

function handleEdit(row: ProjectVO) {
  isEdit.value = true
  dialogTitle.value = '编辑项目'
  Object.assign(form, {
    id: row.id,
    projectCode: row.projectCode,
    projectName: row.projectName,
    companyId: row.companyId,
    province: row.province,
    city: row.city,
    address: row.address,
    propertyType: row.propertyType,
    businessType: row.businessType,
    buildingArea: row.buildingArea,
    operatingArea: row.operatingArea,
    operationStatus: row.operationStatus,
    openingDate: row.openingDate,
    managerId: row.managerId,
  })
  dialogVisible.value = true
}

function resetForm() {
  formRef.value?.clearValidate()
  Object.assign(form, defaultForm())
}

async function handleSubmit() {
  const valid = await formRef.value?.validate().catch(() => false)
  if (!valid) return

  submitting.value = true
  try {
    if (isEdit.value && form.id) {
      await updateProject(form.id, form)
      ElMessage.success('修改成功')
    } else {
      await createProject(form)
      ElMessage.success('新增成功')
    }
    dialogVisible.value = false
    fetchList()
  } finally {
    submitting.value = false
  }
}

// ─────────── 删除 ───────────
async function handleDelete(id: number) {
  try {
    await deleteProject(id)
    ElMessage.success('删除成功')
    // 若当前页只有一条，回到上一页
    if (tableData.value.length === 1 && query.pageNum > 1) {
      query.pageNum--
    }
    fetchList()
  } catch {
    // 错误已由 Axios 拦截器统一处理
  }
}
</script>

<style scoped lang="scss">
.project-page {
  display: flex;
  flex-direction: column;
  gap: 12px;

  .search-card {
    :deep(.el-form-item) {
      margin-bottom: 0;
    }
  }

  .toolbar {
    margin-bottom: 12px;
  }

  .pagination {
    margin-top: 16px;
    display: flex;
    justify-content: flex-end;
  }
}
</style>
