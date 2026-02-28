<template>
  <div class="project-page">
    <!-- 搜索栏 -->
    <el-card shadow="never" class="filter-card">
      <el-form :model="query" inline @keyup.enter="handleSearch">
        <el-form-item label="项目名称">
          <el-input v-model="query.projectName" placeholder="请输入项目名称" clearable style="width: 180px" />
        </el-form-item>
        <el-form-item label="项目编号">
          <el-input v-model="query.projectCode" placeholder="请输入项目编号" clearable style="width: 150px" />
        </el-form-item>
        <el-form-item label="运营状态">
          <el-select v-model="query.operationStatus" placeholder="全部" clearable style="width: 120px">
            <el-option v-for="item in OPERATION_STATUS_OPTIONS" :key="item.value" :label="item.label" :value="item.value" />
          </el-select>
        </el-form-item>
        <el-form-item label="省份">
          <el-input v-model="query.province" placeholder="省份" clearable style="width: 110px" />
        </el-form-item>
        <el-form-item label="城市">
          <el-input v-model="query.city" placeholder="城市" clearable style="width: 110px" />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" :icon="Search" @click="handleSearch">查询</el-button>
          <el-button :icon="Refresh" @click="handleReset">重置</el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <!-- 操作栏 + 表格 -->
    <el-card shadow="never" class="table-card">
      <div class="card-header">
        <div class="header-left">
          <span class="header-title">项目列表</span>
          <span class="count-tag">共 {{ total }} 条</span>
        </div>
        <div class="header-actions">
          <el-button type="primary" :icon="Plus" @click="handleAdd">新增项目</el-button>
        </div>
      </div>

      <div class="table-body">
      <el-table v-loading="loading" :data="tableData" border stripe row-key="id" style="width: 100%">
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
        <el-table-column label="操作" width="200" align="center" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" size="small" @click="handleDetail(row)">详情</el-button>
            <el-divider direction="vertical" />
            <el-button link type="primary" size="small" @click="handleEdit(row)">编辑</el-button>
            <el-divider direction="vertical" />
            <el-popconfirm title="确认删除该项目？" confirm-button-text="确认" cancel-button-text="取消" @confirm="handleDelete(row.id)">
              <template #reference>
                <el-button link type="danger" size="small">删除</el-button>
              </template>
            </el-popconfirm>
          </template>
        </el-table-column>
      </el-table>

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
      </div>
    </el-card>

    <!-- 新增/编辑 Dialog -->
    <el-dialog v-model="dialogVisible" :title="dialogTitle" width="760px" :close-on-click-modal="false" destroy-on-close @close="resetForm">
      <el-form ref="formRef" :model="form" :rules="formRules" label-width="100px" label-position="right">
        <el-row :gutter="20">
          <el-col :span="12">
            <el-form-item label="项目编号" prop="projectCode">
              <el-input v-model="form.projectCode" placeholder="请输入项目编号" :disabled="isEdit" maxlength="50" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="项目名称" prop="projectName">
              <el-input v-model="form.projectName" placeholder="请输入项目名称" maxlength="200" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="所属公司" prop="companyId">
              <el-select v-model="form.companyId" placeholder="请选择所属公司" style="width: 100%" filterable>
                <el-option v-for="c in companyOptions" :key="c.id" :label="c.companyName" :value="c.id" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="运营状态" prop="operationStatus">
              <el-select v-model="form.operationStatus" placeholder="请选择" style="width: 100%">
                <el-option v-for="item in OPERATION_STATUS_OPTIONS" :key="item.value" :label="item.label" :value="item.value" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="产权性质">
              <el-select v-model="form.propertyType" placeholder="请选择" clearable style="width: 100%">
                <el-option label="国有" :value="1" /><el-option label="集体" :value="2" />
                <el-option label="私有" :value="3" /><el-option label="其他" :value="4" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="经营类型">
              <el-select v-model="form.businessType" placeholder="请选择" clearable style="width: 100%">
                <el-option label="自持" :value="1" /><el-option label="租赁" :value="2" /><el-option label="合作" :value="3" />
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
              <el-input-number v-model="form.buildingArea" :min="0" :precision="2" :step="100" style="width: 100%" controls-position="right" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="经营面积(㎡)">
              <el-input-number v-model="form.operatingArea" :min="0" :precision="2" :step="100" style="width: 100%" controls-position="right" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="开业时间">
              <el-date-picker v-model="form.openingDate" type="date" placeholder="选择开业日期" format="YYYY-MM-DD" value-format="YYYY-MM-DD" style="width: 100%" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="负责人">
              <el-select v-model="form.managerId" placeholder="请选择负责人" style="width: 100%" clearable filterable>
                <el-option v-for="u in userOptions" :key="u.id" :label="u.realName || u.username" :value="u.id" />
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

    <!-- 项目详情 Drawer -->
    <el-drawer
      v-model="detailDrawerVisible"
      :title="`项目详情 - ${currentProject?.projectName || ''}`"
      size="720px"
      destroy-on-close
    >
      <el-tabs v-model="detailTab" @tab-change="onDetailTabChange">
        <!-- Tab0: 项目图片 -->
        <el-tab-pane label="项目图片" name="images">
          <div class="tab-toolbar">
            <el-button type="primary" size="small" :icon="Upload" :loading="imageUploading" @click="imageInputRef?.click()">上传图片</el-button>
            <input ref="imageInputRef" type="file" accept="image/*" style="display:none" @change="handleImageFileChange" />
          </div>
          <div v-if="currentProjectImages.length" class="image-list">
            <div v-for="(img, idx) in currentProjectImages" :key="idx" class="image-item">
              <el-image :src="img.url" fit="cover" style="width:140px;height:100px;border-radius:4px;display:block" :preview-src-list="currentProjectImages.map(i => i.url)" :initial-index="idx" />
              <div class="image-item-name" :title="img.name">{{ img.name }}</div>
              <el-popconfirm title="确认删除该图片？" @confirm="handleDeleteImage(idx)">
                <template #reference>
                  <el-button link type="danger" size="small" style="margin-top:2px">删除</el-button>
                </template>
              </el-popconfirm>
            </div>
          </div>
          <el-empty v-else description="暂无图片" :image-size="80" />
        </el-tab-pane>

        <!-- Tab1: 合同甲方信息 -->
        <el-tab-pane label="合同甲方" name="contract">
          <div class="tab-toolbar">
            <template v-if="!contractEditing">
              <el-button type="primary" size="small" @click="contractEditing = true">编辑</el-button>
            </template>
            <template v-else>
              <el-button type="primary" size="small" :loading="contractSaving" @click="saveContract">保存</el-button>
              <el-button size="small" @click="contractEditing = false">取消</el-button>
            </template>
          </div>
          <el-form :model="contractForm" label-width="110px" class="detail-form">
            <el-row :gutter="16">
              <el-col :span="12">
                <el-form-item label="甲方名称">
                  <el-input v-model="contractForm.partyAName" :disabled="!contractEditing" placeholder="甲方名称" />
                </el-form-item>
              </el-col>
              <el-col :span="12">
                <el-form-item label="甲方简称">
                  <el-input v-model="contractForm.partyAAbbr" :disabled="!contractEditing" placeholder="甲方简称" />
                </el-form-item>
              </el-col>
              <el-col :span="12">
                <el-form-item label="法定代表人">
                  <el-input v-model="contractForm.legalRepresentative" :disabled="!contractEditing" placeholder="法定代表人" />
                </el-form-item>
              </el-col>
              <el-col :span="12">
                <el-form-item label="联系电话">
                  <el-input v-model="contractForm.partyAPhone" :disabled="!contractEditing" placeholder="联系电话" />
                </el-form-item>
              </el-col>
              <el-col :span="12">
                <el-form-item label="营业执照号">
                  <el-input v-model="contractForm.businessLicense" :disabled="!contractEditing" placeholder="营业执照号" />
                </el-form-item>
              </el-col>
              <el-col :span="12">
                <el-form-item label="邮箱">
                  <el-input v-model="contractForm.email" :disabled="!contractEditing" placeholder="邮箱" />
                </el-form-item>
              </el-col>
              <el-col :span="24">
                <el-form-item label="甲方地址">
                  <el-input v-model="contractForm.partyAAddress" :disabled="!contractEditing" placeholder="甲方地址" />
                </el-form-item>
              </el-col>
            </el-row>
          </el-form>
        </el-tab-pane>

        <!-- Tab2: 财务联系人 -->
        <el-tab-pane label="财务联系人" name="financeContacts">
          <div class="tab-toolbar">
            <el-button type="primary" size="small" :icon="Plus" @click="openFinanceContactDialog()">新增联系人</el-button>
          </div>
          <el-table v-loading="fcLoading" :data="financeContacts" border size="small" style="width:100%">
            <el-table-column prop="contactName" label="姓名" width="100" />
            <el-table-column prop="phone" label="电话" width="130" />
            <el-table-column prop="email" label="邮箱" min-width="150" show-overflow-tooltip />
            <el-table-column prop="creditCode" label="信用代码" width="160" show-overflow-tooltip />
            <el-table-column prop="sealType" label="印章类型" width="100" />
            <el-table-column label="操作" width="110" align="center">
              <template #default="{ row }">
                <el-button link type="primary" size="small" @click="openFinanceContactDialog(row)">编辑</el-button>
                <el-divider direction="vertical" />
                <el-popconfirm title="确认删除？" @confirm="deleteFinanceContact(row.id)">
                  <template #reference>
                    <el-button link type="danger" size="small">删除</el-button>
                  </template>
                </el-popconfirm>
              </template>
            </el-table-column>
          </el-table>
        </el-tab-pane>

        <!-- Tab3: 银行账号 -->
        <el-tab-pane label="银行账号" name="banks">
          <div class="tab-toolbar">
            <el-button type="primary" size="small" :icon="Plus" @click="openBankDialog()">新增账号</el-button>
          </div>
          <el-table v-loading="bankLoading" :data="projectBanks" border size="small" style="width:100%">
            <el-table-column prop="accountName" label="账户名称" min-width="140" show-overflow-tooltip />
            <el-table-column prop="bankName" label="开户银行" min-width="150" show-overflow-tooltip />
            <el-table-column prop="bankAccount" label="银行账号" min-width="160" show-overflow-tooltip />
            <el-table-column label="默认" width="70" align="center">
              <template #default="{ row }">
                <el-tag :type="row.isDefault === 1 ? 'success' : 'info'" size="small">
                  {{ row.isDefault === 1 ? '是' : '否' }}
                </el-tag>
              </template>
            </el-table-column>
            <el-table-column label="操作" width="110" align="center">
              <template #default="{ row }">
                <el-button link type="primary" size="small" @click="openBankDialog(row)">编辑</el-button>
                <el-divider direction="vertical" />
                <el-popconfirm title="确认删除？" @confirm="deleteBank(row.id)">
                  <template #reference>
                    <el-button link type="danger" size="small">删除</el-button>
                  </template>
                </el-popconfirm>
              </template>
            </el-table-column>
          </el-table>
        </el-tab-pane>
      </el-tabs>
    </el-drawer>

    <!-- 财务联系人 Dialog -->
    <el-dialog v-model="fcDialogVisible" :title="fcEditId ? '编辑财务联系人' : '新增财务联系人'" width="480px" :close-on-click-modal="false" destroy-on-close>
      <el-form ref="fcFormRef" :model="fcForm" :rules="fcFormRules" label-width="90px">
        <el-form-item label="联系人姓名" prop="contactName">
          <el-input v-model="fcForm.contactName" placeholder="必填" maxlength="50" />
        </el-form-item>
        <el-form-item label="联系电话">
          <el-input v-model="fcForm.phone" placeholder="联系电话" maxlength="30" />
        </el-form-item>
        <el-form-item label="邮箱">
          <el-input v-model="fcForm.email" placeholder="邮箱" maxlength="100" />
        </el-form-item>
        <el-form-item label="信用代码">
          <el-input v-model="fcForm.creditCode" placeholder="信用代码" maxlength="50" />
        </el-form-item>
        <el-form-item label="印章类型">
          <el-input v-model="fcForm.sealType" placeholder="印章类型" maxlength="50" />
        </el-form-item>
        <el-form-item label="印章说明">
          <el-input v-model="fcForm.sealDesc" placeholder="印章说明" maxlength="200" type="textarea" :rows="2" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="fcDialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="fcSaving" @click="submitFinanceContact">保存</el-button>
      </template>
    </el-dialog>

    <!-- 银行账号 Dialog -->
    <el-dialog v-model="bankDialogVisible" :title="bankEditId ? '编辑银行账号' : '新增银行账号'" width="440px" :close-on-click-modal="false" destroy-on-close>
      <el-form ref="bankFormRef" :model="bankForm" :rules="bankFormRules" label-width="90px">
        <el-form-item label="账户名称" prop="accountName">
          <el-input v-model="bankForm.accountName" placeholder="必填" maxlength="100" />
        </el-form-item>
        <el-form-item label="开户银行" prop="bankName">
          <el-input v-model="bankForm.bankName" placeholder="必填" maxlength="100" />
        </el-form-item>
        <el-form-item label="银行账号" prop="bankAccount">
          <el-input v-model="bankForm.bankAccount" placeholder="必填" maxlength="50" />
        </el-form-item>
        <el-form-item label="默认账户">
          <el-switch v-model="bankForm.isDefault" :active-value="1" :inactive-value="0" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="bankDialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="bankSaving" @click="submitBank">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { ElMessage, type FormInstance, type FormRules } from 'element-plus'
import { Search, Refresh, Plus, Upload } from '@element-plus/icons-vue'
import {
  getProjectPage, getProjectById, createProject, updateProject, deleteProject,
  getProjectContract, saveProjectContract,
  getProjectFinanceContacts, addProjectFinanceContact, updateProjectFinanceContact, deleteProjectFinanceContact,
  getProjectBanks, addProjectBank, updateProjectBank, deleteProjectBank,
  addProjectImage, deleteProjectImage,
  type ProjectVO, type ProjectQuery, type ProjectSaveDTO, type ImageUrl,
  type ProjectContractVO, type ProjectFinanceContactVO, type ProjectBankVO,
} from '@/api/base/project'
import { uploadFile } from '@/api/file'
import { getCompanyList, type CompanyOption } from '@/api/base/company'
import { getUserList, type UserOption } from '@/api/base/user'
import { useAppStore } from '@/store/modules/app'

useAppStore().setPageTitle('项目管理')

// ─────────── 枚举常量 ───────────
const OPERATION_STATUS_OPTIONS = [
  { label: '筹备', value: 0 }, { label: '开业', value: 1 }, { label: '停业', value: 2 },
]

function statusTagType(status: number | null) {
  return ({ 0: 'info', 1: 'success', 2: 'danger' }[status ?? -1] ?? 'info') as any
}

// ─────────── 列表 ───────────
const loading = ref(false)
const tableData = ref<ProjectVO[]>([])
const total = ref(0)

const query = reactive<ProjectQuery & { pageNum: number; pageSize: number }>({
  pageNum: 1, pageSize: 20, projectName: '', projectCode: '', operationStatus: '', province: '', city: '',
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

function handleSearch() { query.pageNum = 1; fetchList() }
function handleReset() {
  query.projectName = ''; query.projectCode = ''; query.operationStatus = ''
  query.province = ''; query.city = ''; query.pageNum = 1; fetchList()
}

// ─────────── 公司/用户下拉 ───────────
const companyOptions = ref<CompanyOption[]>([])
const userOptions = ref<UserOption[]>([])

async function loadOptions() {
  const [companies, users] = await Promise.all([getCompanyList(), getUserList()])
  companyOptions.value = companies
  userOptions.value = users
}

onMounted(() => { fetchList(); loadOptions() })

// ─────────── 新增/编辑 Dialog ───────────
const dialogVisible = ref(false)
const isEdit = ref(false)
const submitting = ref(false)
const formRef = ref<FormInstance>()

const defaultForm = (): ProjectSaveDTO => ({
  id: undefined, projectCode: '', projectName: '', companyId: null,
  province: '', city: '', address: '', propertyType: null, businessType: null,
  buildingArea: null, operatingArea: null, operationStatus: 0, openingDate: '', managerId: null,
})

const form = reactive<ProjectSaveDTO>(defaultForm())
const dialogTitle = ref('新增项目')

const formRules: FormRules = {
  projectCode: [{ required: true, message: '项目编号不能为空', trigger: 'blur' }],
  projectName: [{ required: true, message: '项目名称不能为空', trigger: 'blur' }],
  companyId:   [{ required: true, message: '所属公司不能为空', trigger: 'change' }],
}

function handleAdd() {
  isEdit.value = false; dialogTitle.value = '新增项目'
  Object.assign(form, defaultForm()); dialogVisible.value = true
}

function handleEdit(row: ProjectVO) {
  isEdit.value = true; dialogTitle.value = '编辑项目'
  Object.assign(form, {
    id: row.id, projectCode: row.projectCode, projectName: row.projectName,
    companyId: row.companyId, province: row.province, city: row.city, address: row.address,
    propertyType: row.propertyType, businessType: row.businessType,
    buildingArea: row.buildingArea, operatingArea: row.operatingArea,
    operationStatus: row.operationStatus, openingDate: row.openingDate, managerId: row.managerId,
  })
  dialogVisible.value = true
}

function resetForm() { formRef.value?.clearValidate(); Object.assign(form, defaultForm()) }

async function handleSubmit() {
  const valid = await formRef.value?.validate().catch(() => false)
  if (!valid) return
  submitting.value = true
  try {
    if (isEdit.value && form.id) {
      await updateProject(form.id, form); ElMessage.success('修改成功')
    } else {
      await createProject(form); ElMessage.success('新增成功')
    }
    dialogVisible.value = false; fetchList()
  } finally { submitting.value = false }
}

async function handleDelete(id: number) {
  try {
    await deleteProject(id); ElMessage.success('删除成功')
    if (tableData.value.length === 1 && query.pageNum > 1) query.pageNum--
    fetchList()
  } catch {}
}

// ═══════════════════════════════════════════════════════
// 项目详情 Drawer
// ═══════════════════════════════════════════════════════
const detailDrawerVisible = ref(false)
const detailTab = ref('contract')
const currentProject = ref<ProjectVO | null>(null)

async function handleDetail(row: ProjectVO) {
  currentProject.value = row
  detailTab.value = 'images'
  detailDrawerVisible.value = true
  syncProjectImages()
}

async function onDetailTabChange(tab: string | number) {
  const id = currentProject.value?.id
  if (!id) return
  if (tab === 'images') {
    // 刷新图片列表（从详情接口重新获取确保最新）
    try {
      const latest = await getProjectById(id)
      currentProject.value = latest
    } catch {}
    syncProjectImages()
  } else if (tab === 'contract') await loadContractInfo(id)
  else if (tab === 'financeContacts') await loadFinanceContacts(id)
  else if (tab === 'banks') await loadBanks(id)
}

// ─────────── 项目图片 ───────────
const currentProjectImages = ref<ImageUrl[]>([])
const imageUploading = ref(false)
const imageInputRef = ref<HTMLInputElement>()

function syncProjectImages() {
  currentProjectImages.value = currentProject.value?.imageUrls ?? []
}

async function handleImageFileChange(e: Event) {
  const file = (e.target as HTMLInputElement).files?.[0]
  if (!file) return
  imageUploading.value = true
  try {
    const url = await uploadFile(file)
    await addProjectImage(currentProject.value!.id, { url, name: file.name.replace(/\.[^.]+$/, '') })
    // 重新加载项目详情以同步图片列表
    const latest = await getProjectById(currentProject.value!.id)
    currentProject.value = latest
    syncProjectImages()
    ElMessage.success('图片上传成功')
  } catch {
    ElMessage.error('图片上传失败')
  } finally {
    imageUploading.value = false
    if (imageInputRef.value) imageInputRef.value.value = ''
  }
}

async function handleDeleteImage(index: number) {
  try {
    await deleteProjectImage(currentProject.value!.id, index)
    const latest = await getProjectById(currentProject.value!.id)
    currentProject.value = latest
    syncProjectImages()
    ElMessage.success('删除成功')
  } catch {}
}

// ─────────── 合同甲方 ───────────
const contractEditing = ref(false)
const contractSaving = ref(false)
const contractForm = reactive<ProjectContractVO>({})

async function loadContractInfo(id: number) {
  contractEditing.value = false
  try {
    const res = await getProjectContract(id)
    Object.assign(contractForm, res || {})
  } catch {}
}

async function saveContract() {
  contractSaving.value = true
  try {
    await saveProjectContract(currentProject.value!.id, contractForm)
    ElMessage.success('保存成功')
    contractEditing.value = false
  } finally { contractSaving.value = false }
}

// ─────────── 财务联系人 ───────────
const fcLoading = ref(false)
const financeContacts = ref<ProjectFinanceContactVO[]>([])
const fcDialogVisible = ref(false)
const fcSaving = ref(false)
const fcEditId = ref<number | null>(null)
const fcFormRef = ref<FormInstance>()
const fcForm = reactive({
  contactName: '', phone: '', email: '', creditCode: '', sealType: '', sealDesc: '',
})
const fcFormRules: FormRules = {
  contactName: [{ required: true, message: '联系人姓名不能为空', trigger: 'blur' }],
}

async function loadFinanceContacts(id: number) {
  fcLoading.value = true
  try { financeContacts.value = await getProjectFinanceContacts(id) }
  finally { fcLoading.value = false }
}

function openFinanceContactDialog(row?: ProjectFinanceContactVO) {
  fcEditId.value = row?.id ?? null
  Object.assign(fcForm, { contactName: '', phone: '', email: '', creditCode: '', sealType: '', sealDesc: '' })
  if (row) Object.assign(fcForm, row)
  fcDialogVisible.value = true
}

async function submitFinanceContact() {
  const valid = await fcFormRef.value?.validate().catch(() => false)
  if (!valid) return
  fcSaving.value = true
  const id = currentProject.value!.id
  try {
    if (fcEditId.value) {
      await updateProjectFinanceContact(id, fcEditId.value, fcForm)
      ElMessage.success('修改成功')
    } else {
      await addProjectFinanceContact(id, fcForm)
      ElMessage.success('新增成功')
    }
    fcDialogVisible.value = false
    await loadFinanceContacts(id)
  } finally { fcSaving.value = false }
}

async function deleteFinanceContact(cid: number) {
  try {
    await deleteProjectFinanceContact(currentProject.value!.id, cid)
    ElMessage.success('删除成功')
    await loadFinanceContacts(currentProject.value!.id)
  } catch {}
}

// ─────────── 银行账号 ───────────
const bankLoading = ref(false)
const projectBanks = ref<ProjectBankVO[]>([])
const bankDialogVisible = ref(false)
const bankSaving = ref(false)
const bankEditId = ref<number | null>(null)
const bankFormRef = ref<FormInstance>()
const bankForm = reactive({ bankName: '', bankAccount: '', accountName: '', isDefault: 0 })
const bankFormRules: FormRules = {
  accountName: [{ required: true, message: '账户名称不能为空', trigger: 'blur' }],
  bankName:    [{ required: true, message: '开户银行不能为空', trigger: 'blur' }],
  bankAccount: [{ required: true, message: '银行账号不能为空', trigger: 'blur' }],
}

async function loadBanks(id: number) {
  bankLoading.value = true
  try { projectBanks.value = await getProjectBanks(id) }
  finally { bankLoading.value = false }
}

function openBankDialog(row?: ProjectBankVO) {
  bankEditId.value = row?.id ?? null
  Object.assign(bankForm, { bankName: '', bankAccount: '', accountName: '', isDefault: 0 })
  if (row) Object.assign(bankForm, row)
  bankDialogVisible.value = true
}

async function submitBank() {
  const valid = await bankFormRef.value?.validate().catch(() => false)
  if (!valid) return
  bankSaving.value = true
  const id = currentProject.value!.id
  try {
    if (bankEditId.value) {
      await updateProjectBank(id, bankEditId.value, bankForm)
      ElMessage.success('修改成功')
    } else {
      await addProjectBank(id, bankForm)
      ElMessage.success('新增成功')
    }
    bankDialogVisible.value = false
    await loadBanks(id)
  } finally { bankSaving.value = false }
}

async function deleteBank(bid: number) {
  try {
    await deleteProjectBank(currentProject.value!.id, bid)
    ElMessage.success('删除成功')
    await loadBanks(currentProject.value!.id)
  } catch {}
}
</script>

<style scoped lang="scss">
.project-page {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.filter-card {
  border-radius: 12px !important;
  border: 1px solid rgba(0, 0, 0, 0.06) !important;
  transition: box-shadow 0.2s;
  &:hover { box-shadow: 0 4px 16px rgba(0, 0, 0, 0.08) !important; }
  :deep(.el-card__body) { padding: 14px 20px; }
  :deep(.el-form-item) { margin-bottom: 0; }
}

.table-card {
  border-radius: 12px !important;
  border: 1px solid rgba(0, 0, 0, 0.06) !important;
  overflow: hidden;
  :deep(.el-card__body) { padding: 0; }
}

.card-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 14px 20px;
  border-bottom: 1px solid #f1f5f9;
  background: #fff;

  .header-left {
    display: flex;
    align-items: center;
    gap: 10px;
  }

  .header-title {
    font-size: 15px;
    font-weight: 600;
    color: #1e293b;
    display: flex;
    align-items: center;
    gap: 8px;
    &::before {
      content: '';
      display: inline-block;
      width: 3px;
      height: 16px;
      background: linear-gradient(180deg, #3b82f6, #60a5fa);
      border-radius: 2px;
    }
  }

  .count-tag {
    font-size: 12px;
    background: #eff6ff;
    color: #3b82f6;
    border: 1px solid #bfdbfe;
    border-radius: 10px;
    padding: 2px 10px;
    font-weight: 500;
  }

  .header-actions {
    display: flex;
    gap: 8px;
    align-items: center;
  }
}

.table-body {
  padding: 16px 20px;

  :deep(.el-table) {
    border-radius: 8px;
    overflow: hidden;

    .el-table__header-wrapper th.el-table__cell {
      background: #f8fafc;
      color: #64748b;
      font-weight: 600;
      font-size: 13px;
      border-bottom: 1px solid #e8edf3;
    }

    .el-table__row:hover > td.el-table__cell {
      background-color: #f0f7ff !important;
    }

    .el-table__row--striped > td.el-table__cell {
      background-color: #fafbfc;
    }

    td.el-table__cell {
      border-bottom: 1px solid #f4f6f9;
    }
  }
}

.pagination {
  margin-top: 14px;
  display: flex;
  justify-content: flex-end;
}

// Drawer 内样式
.tab-toolbar {
  margin-bottom: 12px;
  display: flex;
  gap: 8px;
}

.detail-form { margin-top: 8px; }

.image-list {
  display: flex;
  flex-wrap: wrap;
  gap: 12px;
}

.image-item {
  display: flex;
  flex-direction: column;
  align-items: center;
  width: 140px;
}

.image-item-name {
  font-size: 12px;
  color: #94a3b8;
  margin-top: 4px;
  max-width: 140px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
</style>
