<template>
  <div class="merchant-page">
    <!-- 搜索栏 -->
    <el-card shadow="never" class="search-card">
      <el-form :model="query" inline @keyup.enter="handleSearch">
        <el-form-item label="商家名称">
          <el-input
            v-model="query.merchantName"
            placeholder="请输入商家名称"
            clearable
            style="width: 160px"
          />
        </el-form-item>
        <el-form-item label="所属项目">
          <el-select
            v-model="query.projectId"
            placeholder="全部项目"
            clearable
            filterable
            style="width: 180px"
          >
            <el-option
              v-for="p in projectOptions"
              :key="p.id"
              :label="p.projectName"
              :value="p.id"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="商家属性">
          <el-select v-model="query.merchantAttr" placeholder="全部" clearable style="width: 110px">
            <el-option label="个体户" :value="1" />
            <el-option label="企业" :value="2" />
          </el-select>
        </el-form-item>
        <el-form-item label="商家评级">
          <el-select v-model="query.merchantLevel" placeholder="全部" clearable style="width: 110px">
            <el-option label="优秀" :value="1" />
            <el-option label="良好" :value="2" />
            <el-option label="一般" :value="3" />
            <el-option label="差" :value="4" />
          </el-select>
        </el-form-item>
        <el-form-item label="审核状态">
          <el-select v-model="query.auditStatus" placeholder="全部" clearable style="width: 110px">
            <el-option label="待审核" :value="0" />
            <el-option label="通过" :value="1" />
            <el-option label="驳回" :value="2" />
          </el-select>
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
        <el-button type="primary" :icon="Plus" @click="handleAdd">新增商家</el-button>
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
        <el-table-column prop="merchantCode" label="商家编号" width="120" show-overflow-tooltip />
        <el-table-column prop="merchantName" label="商家名称" min-width="150" show-overflow-tooltip />
        <el-table-column prop="projectName" label="所属项目" width="150" show-overflow-tooltip />
        <el-table-column prop="merchantAttrName" label="商家属性" width="90" align="center" />
        <el-table-column prop="merchantNatureName" label="商家性质" width="90" align="center" />
        <el-table-column prop="formatType" label="经营业态" width="110" show-overflow-tooltip />
        <el-table-column prop="phone" label="手机" width="120" />
        <el-table-column label="商家评级" width="90" align="center">
          <template #default="{ row }">
            <el-tag :type="levelTagType(row.merchantLevel)" size="small">
              {{ row.merchantLevelName || '-' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="审核状态" width="90" align="center">
          <template #default="{ row }">
            <el-tag :type="auditTagType(row.auditStatus)" size="small">
              {{ row.auditStatusName || '-' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="createdAt" label="创建时间" width="160" align="center" />
        <el-table-column label="操作" width="150" align="center" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" size="small" @click="handleEdit(row)">编辑</el-button>
            <el-divider direction="vertical" />
            <el-popconfirm
              title="确认删除该商家？"
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
      width="820px"
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
            <el-form-item label="所属项目" prop="projectId">
              <el-select
                v-model="form.projectId"
                placeholder="请选择项目"
                style="width: 100%"
                filterable
              >
                <el-option
                  v-for="p in projectOptions"
                  :key="p.id"
                  :label="p.projectName"
                  :value="p.id"
                />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="商家编号" prop="merchantCode">
              <el-input v-model="form.merchantCode" placeholder="商家编号" maxlength="50" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="商家名称" prop="merchantName">
              <el-input v-model="form.merchantName" placeholder="请输入商家名称" maxlength="200" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="商家属性">
              <el-select v-model="form.merchantAttr" placeholder="请选择" clearable style="width: 100%">
                <el-option label="个体户" :value="1" />
                <el-option label="企业" :value="2" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="商家性质">
              <el-select v-model="form.merchantNature" placeholder="请选择" clearable style="width: 100%">
                <el-option label="民营" :value="1" />
                <el-option label="国营" :value="2" />
                <el-option label="外资" :value="3" />
                <el-option label="合资" :value="4" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="经营业态">
              <el-input v-model="form.formatType" placeholder="经营业态" maxlength="100" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="商家评级">
              <el-select v-model="form.merchantLevel" placeholder="请选择" clearable style="width: 100%">
                <el-option label="优秀" :value="1" />
                <el-option label="良好" :value="2" />
                <el-option label="一般" :value="3" />
                <el-option label="差" :value="4" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="审核状态">
              <el-select v-model="form.auditStatus" placeholder="请选择" clearable style="width: 100%">
                <el-option label="待审核" :value="0" />
                <el-option label="通过" :value="1" />
                <el-option label="驳回" :value="2" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="自然人">
              <el-input v-model="form.naturalPerson" placeholder="自然人姓名" maxlength="50" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="身份证号">
              <el-input v-model="form.idCard" placeholder="身份证号" maxlength="50" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="手机">
              <el-input v-model="form.phone" placeholder="手机号码" maxlength="30" />
            </el-form-item>
          </el-col>
          <el-col :span="24">
            <el-form-item label="地址">
              <el-input v-model="form.address" placeholder="地址" maxlength="500" />
            </el-form-item>
          </el-col>
        </el-row>

        <!-- 联系人 -->
        <el-divider content-position="left">联系人</el-divider>
        <div v-for="(contact, idx) in form.contacts" :key="idx" class="sub-row">
          <el-row :gutter="12" align="middle">
            <el-col :span="5">
              <el-input v-model="contact.contactName" placeholder="姓名" />
            </el-col>
            <el-col :span="5">
              <el-input v-model="contact.phone" placeholder="电话" />
            </el-col>
            <el-col :span="5">
              <el-input v-model="contact.email" placeholder="邮箱" />
            </el-col>
            <el-col :span="4">
              <el-input v-model="contact.position" placeholder="职位" />
            </el-col>
            <el-col :span="3">
              <el-checkbox v-model="contact.isPrimary" :true-value="1" :false-value="0">主要</el-checkbox>
            </el-col>
            <el-col :span="2">
              <el-button link type="danger" :icon="Delete" @click="removeContact(idx)" />
            </el-col>
          </el-row>
        </div>
        <el-button link type="primary" :icon="Plus" @click="addContact">添加联系人</el-button>

        <!-- 开票信息 -->
        <el-divider content-position="left">开票信息</el-divider>
        <div v-for="(inv, idx) in form.invoices" :key="idx" class="sub-row">
          <el-row :gutter="12">
            <el-col :span="8">
              <el-input v-model="inv.invoiceTitle" placeholder="发票抬头" />
            </el-col>
            <el-col :span="6">
              <el-input v-model="inv.taxNumber" placeholder="税号" />
            </el-col>
            <el-col :span="4">
              <el-checkbox v-model="inv.isDefault" :true-value="1" :false-value="0">默认</el-checkbox>
            </el-col>
            <el-col :span="2">
              <el-button link type="danger" :icon="Delete" @click="removeInvoice(idx)" />
            </el-col>
          </el-row>
          <el-row :gutter="12" style="margin-top: 8px">
            <el-col :span="8">
              <el-input v-model="inv.bankName" placeholder="开户银行" />
            </el-col>
            <el-col :span="8">
              <el-input v-model="inv.bankAccount" placeholder="银行账号" />
            </el-col>
            <el-col :span="8">
              <el-input v-model="inv.phone" placeholder="注册电话" />
            </el-col>
          </el-row>
        </div>
        <el-button link type="primary" :icon="Plus" @click="addInvoice">添加开票信息</el-button>
      </el-form>

      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="submitting" @click="handleSubmit">确认</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { Search, Refresh, Plus, Delete } from '@element-plus/icons-vue'
import {
  getMerchantPage,
  getMerchantDetail,
  createMerchant,
  updateMerchant,
  deleteMerchant,
} from '@/api/base/merchant'
import { getProjectList } from '@/api/base/project'

/* ------------------------------------------------------------------ */
/* 项目下拉选项                                                           */
/* ------------------------------------------------------------------ */
const projectOptions = ref<Array<{ id: number; projectName: string }>>([])

async function loadProjectOptions() {
  try {
    const res = await getProjectList()
    projectOptions.value = res ?? []
  } catch {
    projectOptions.value = []
  }
}

/* ------------------------------------------------------------------ */
/* 列表查询                                                               */
/* ------------------------------------------------------------------ */
const loading = ref(false)
const tableData = ref<any[]>([])
const total = ref(0)

const query = reactive({
  pageNum: 1,
  pageSize: 10,
  merchantName: '',
  projectId: undefined as number | undefined,
  merchantAttr: undefined as number | undefined,
  merchantNature: undefined as number | undefined,
  merchantLevel: undefined as number | undefined,
  auditStatus: undefined as number | undefined,
})

async function fetchList() {
  loading.value = true
  try {
    const res = await getMerchantPage(query)
    tableData.value = res.records ?? res.data?.records ?? []
    total.value = res.total ?? res.data?.total ?? 0
  } finally {
    loading.value = false
  }
}

function handleSearch() {
  query.pageNum = 1
  fetchList()
}

function handleReset() {
  query.pageNum = 1
  query.merchantName = ''
  query.projectId = undefined
  query.merchantAttr = undefined
  query.merchantNature = undefined
  query.merchantLevel = undefined
  query.auditStatus = undefined
  fetchList()
}

function levelTagType(level: number) {
  return level === 1 ? 'success' : level === 2 ? 'primary' : level === 3 ? 'warning' : 'danger'
}

function auditTagType(status: number) {
  return status === 1 ? 'success' : status === 2 ? 'danger' : 'info'
}

/* ------------------------------------------------------------------ */
/* 新增/编辑                                                               */
/* ------------------------------------------------------------------ */
const dialogVisible = ref(false)
const dialogTitle = ref('新增商家')
const isEdit = ref(false)
const submitting = ref(false)
const formRef = ref()

function defaultForm() {
  return {
    id: undefined as number | undefined,
    projectId: undefined as number | undefined,
    merchantCode: '',
    merchantName: '',
    merchantAttr: undefined as number | undefined,
    merchantNature: undefined as number | undefined,
    formatType: '',
    naturalPerson: '',
    idCard: '',
    address: '',
    phone: '',
    merchantLevel: undefined as number | undefined,
    auditStatus: 0 as number | undefined,
    contacts: [] as Array<{
      id?: number
      contactName: string
      phone: string
      email: string
      position: string
      isPrimary: number
    }>,
    invoices: [] as Array<{
      id?: number
      invoiceTitle: string
      taxNumber: string
      bankName: string
      bankAccount: string
      address: string
      phone: string
      isDefault: number
    }>,
  }
}

const form = reactive(defaultForm())

const formRules = {
  projectId: [{ required: true, message: '请选择所属项目', trigger: 'change' }],
  merchantName: [{ required: true, message: '请输入商家名称', trigger: 'blur' }],
}

function handleAdd() {
  isEdit.value = false
  dialogTitle.value = '新增商家'
  Object.assign(form, defaultForm())
  dialogVisible.value = true
}

async function handleEdit(row: any) {
  isEdit.value = true
  dialogTitle.value = '编辑商家'
  const data = await getMerchantDetail(row.id)
  Object.assign(form, defaultForm(), {
    ...data,
    idCard: '', // 详情中身份证已脱敏，编辑时清空让用户重新输入
    contacts: (data.contacts ?? []).map((c: any) => ({ ...c })),
    invoices: (data.invoices ?? []).map((inv: any) => ({ ...inv })),
  })
  dialogVisible.value = true
}

function resetForm() {
  formRef.value?.clearValidate()
}

function addContact() {
  form.contacts.push({ contactName: '', phone: '', email: '', position: '', isPrimary: 0 })
}

function removeContact(idx: number) {
  form.contacts.splice(idx, 1)
}

function addInvoice() {
  form.invoices.push({
    invoiceTitle: '',
    taxNumber: '',
    bankName: '',
    bankAccount: '',
    address: '',
    phone: '',
    isDefault: 0,
  })
}

function removeInvoice(idx: number) {
  form.invoices.splice(idx, 1)
}

async function handleSubmit() {
  await formRef.value?.validate()
  submitting.value = true
  try {
    if (isEdit.value && form.id) {
      await updateMerchant(form.id, form as any)
      ElMessage.success('编辑成功')
    } else {
      await createMerchant(form as any)
      ElMessage.success('新增成功')
    }
    dialogVisible.value = false
    fetchList()
  } finally {
    submitting.value = false
  }
}

/* ------------------------------------------------------------------ */
/* 删除                                                                  */
/* ------------------------------------------------------------------ */
async function handleDelete(id: number) {
  await deleteMerchant(id)
  ElMessage.success('删除成功')
  fetchList()
}

onMounted(() => {
  loadProjectOptions()
  fetchList()
})
</script>

<style scoped lang="scss">
.merchant-page {
  .search-card {
    margin-bottom: 12px;
  }
  .toolbar {
    margin-bottom: 12px;
  }
  .pagination {
    margin-top: 16px;
    display: flex;
    justify-content: flex-end;
  }
  .sub-row {
    margin-bottom: 10px;
    padding: 8px;
    background: #fafafa;
    border-radius: 4px;
  }
}
</style>
