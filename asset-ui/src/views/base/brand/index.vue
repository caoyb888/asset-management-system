<template>
  <div class="brand-page">
    <!-- 搜索栏 -->
    <el-card shadow="never" class="search-card">
      <el-form :model="query" inline @keyup.enter="handleSearch">
        <el-form-item label="品牌名称">
          <el-input v-model="query.brandNameCn" placeholder="请输入品牌名称" clearable style="width: 160px" />
        </el-form-item>
        <el-form-item label="所属业态">
          <el-input v-model="query.formatType" placeholder="业态" clearable style="width: 130px" />
        </el-form-item>
        <el-form-item label="品牌等级">
          <el-select v-model="query.brandLevel" placeholder="全部" clearable style="width: 110px">
            <el-option label="高端" :value="1" /><el-option label="中端" :value="2" /><el-option label="大众" :value="3" />
          </el-select>
        </el-form-item>
        <el-form-item label="合作关系">
          <el-select v-model="query.cooperationType" placeholder="全部" clearable style="width: 110px">
            <el-option label="直营" :value="1" /><el-option label="加盟" :value="2" /><el-option label="代理" :value="3" />
          </el-select>
        </el-form-item>
        <el-form-item label="经营性质">
          <el-select v-model="query.businessNature" placeholder="全部" clearable style="width: 110px">
            <el-option label="餐饮" :value="1" /><el-option label="零售" :value="2" />
            <el-option label="娱乐" :value="3" /><el-option label="服务" :value="4" />
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
        <el-button type="primary" :icon="Plus" @click="handleAdd">新增品牌</el-button>
      </div>

      <el-table v-loading="loading" :data="tableData" border stripe row-key="id" style="width: 100%">
        <el-table-column type="index" label="序号" width="60" align="center" />
        <el-table-column prop="brandCode" label="品牌编码" width="120" show-overflow-tooltip />
        <el-table-column prop="brandNameCn" label="品牌名(中)" min-width="140" show-overflow-tooltip />
        <el-table-column prop="brandNameEn" label="品牌名(英)" min-width="130" show-overflow-tooltip />
        <el-table-column prop="formatType" label="所属业态" width="110" show-overflow-tooltip />
        <el-table-column label="品牌等级" width="90" align="center">
          <template #default="{ row }">
            <el-tag :type="levelTagType(row.brandLevel)" size="small">{{ row.brandLevelName || '-' }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="cooperationTypeName" label="合作关系" width="90" align="center" />
        <el-table-column prop="businessNatureName" label="经营性质" width="90" align="center" />
        <el-table-column prop="brandTypeName" label="品牌类型" width="80" align="center" />
        <el-table-column prop="groupName" label="集团名称" width="140" show-overflow-tooltip />
        <el-table-column prop="phone" label="联系电话" width="120" />
        <el-table-column prop="createdAt" label="创建时间" width="160" align="center" />
        <el-table-column label="操作" width="200" align="center" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" size="small" @click="openContactsDialog(row)">联系人</el-button>
            <el-divider direction="vertical" />
            <el-button link type="primary" size="small" @click="handleEdit(row)">编辑</el-button>
            <el-divider direction="vertical" />
            <el-popconfirm title="确认删除该品牌？" confirm-button-text="确认" cancel-button-text="取消" @confirm="handleDelete(row.id)">
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
    </el-card>

    <!-- 新增/编辑 Dialog -->
    <el-dialog v-model="dialogVisible" :title="dialogTitle" width="800px" :close-on-click-modal="false" destroy-on-close @close="resetForm">
      <el-form ref="formRef" :model="form" :rules="formRules" label-width="100px" label-position="right">
        <el-row :gutter="20">
          <el-col :span="12">
            <el-form-item label="品牌编码" prop="brandCode">
              <el-input v-model="form.brandCode" placeholder="请输入品牌编码" maxlength="50" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="品牌名(中)" prop="brandNameCn">
              <el-input v-model="form.brandNameCn" placeholder="请输入中文品牌名" maxlength="200" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="品牌名(英)">
              <el-input v-model="form.brandNameEn" placeholder="英文品牌名" maxlength="200" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="所属业态">
              <el-input v-model="form.formatType" placeholder="业态" maxlength="100" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="品牌等级">
              <el-select v-model="form.brandLevel" placeholder="请选择" clearable style="width: 100%">
                <el-option label="高端" :value="1" /><el-option label="中端" :value="2" /><el-option label="大众" :value="3" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="合作关系">
              <el-select v-model="form.cooperationType" placeholder="请选择" clearable style="width: 100%">
                <el-option label="直营" :value="1" /><el-option label="加盟" :value="2" /><el-option label="代理" :value="3" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="经营性质">
              <el-select v-model="form.businessNature" placeholder="请选择" clearable style="width: 100%">
                <el-option label="餐饮" :value="1" /><el-option label="零售" :value="2" />
                <el-option label="娱乐" :value="3" /><el-option label="服务" :value="4" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="连锁类型">
              <el-select v-model="form.chainType" placeholder="请选择" clearable style="width: 100%">
                <el-option label="连锁" :value="1" /><el-option label="单店" :value="2" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="品牌类型">
              <el-select v-model="form.brandType" placeholder="请选择" clearable style="width: 100%">
                <el-option label="MALL" :value="1" /><el-option label="商街" :value="2" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="联系电话">
              <el-input v-model="form.phone" placeholder="联系电话" maxlength="30" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="集团名称">
              <el-input v-model="form.groupName" placeholder="集团名称" maxlength="200" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="项目阶段">
              <el-input v-model="form.projectStage" placeholder="项目阶段" maxlength="50" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="平均租金">
              <el-input-number v-model="form.avgRent" :min="0" :precision="2" placeholder="元/㎡·月" style="width: 100%" controls-position="right" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="最低客单价">
              <el-input-number v-model="form.minCustomerPrice" :min="0" :precision="2" placeholder="元" style="width: 100%" controls-position="right" />
            </el-form-item>
          </el-col>
          <el-col :span="24">
            <el-form-item label="总部地址">
              <el-input v-model="form.hqAddress" placeholder="总部地址" maxlength="500" />
            </el-form-item>
          </el-col>
          <el-col :span="24">
            <el-form-item label="主要城市">
              <el-input v-model="form.mainCities" placeholder="主要分布城市" maxlength="500" />
            </el-form-item>
          </el-col>
          <el-col :span="24">
            <el-form-item label="网址">
              <el-input v-model="form.website" placeholder="官网地址" maxlength="300" />
            </el-form-item>
          </el-col>
          <el-col :span="24">
            <el-form-item label="品牌简介">
              <el-input v-model="form.brandIntro" type="textarea" :rows="3" placeholder="品牌简介" maxlength="2000" show-word-limit />
            </el-form-item>
          </el-col>
        </el-row>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="submitting" @click="handleSubmit">确认</el-button>
      </template>
    </el-dialog>

    <!-- 品牌联系人管理 Dialog -->
    <el-dialog v-model="contactsDialogVisible" :title="`联系人管理 - ${currentBrand?.brandNameCn || ''}`" width="700px" destroy-on-close>
      <div class="tab-toolbar">
        <el-button type="primary" size="small" :icon="Plus" @click="openContactEditDialog()">新增联系人</el-button>
      </div>
      <el-table v-loading="contactsLoading" :data="brandContacts" border size="small" style="width:100%">
        <el-table-column prop="contactName" label="姓名" width="100" />
        <el-table-column prop="phone" label="电话" width="130" />
        <el-table-column prop="email" label="邮箱" min-width="160" show-overflow-tooltip />
        <el-table-column prop="position" label="职位" width="110" />
        <el-table-column label="主要联系人" width="90" align="center">
          <template #default="{ row }">
            <el-tag :type="row.isPrimary === 1 ? 'success' : 'info'" size="small">{{ row.isPrimary === 1 ? '是' : '否' }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="110" align="center">
          <template #default="{ row }">
            <el-button link type="primary" size="small" @click="openContactEditDialog(row)">编辑</el-button>
            <el-divider direction="vertical" />
            <el-popconfirm title="确认删除该联系人？" @confirm="handleDeleteContact(row.id)">
              <template #reference>
                <el-button link type="danger" size="small">删除</el-button>
              </template>
            </el-popconfirm>
          </template>
        </el-table-column>
      </el-table>
    </el-dialog>

    <!-- 联系人编辑 Dialog -->
    <el-dialog v-model="contactEditDialogVisible" :title="contactEditId ? '编辑联系人' : '新增联系人'" width="440px" :close-on-click-modal="false" destroy-on-close>
      <el-form ref="contactFormRef" :model="contactForm" :rules="contactFormRules" label-width="90px">
        <el-form-item label="姓名" prop="contactName">
          <el-input v-model="contactForm.contactName" placeholder="必填" maxlength="50" />
        </el-form-item>
        <el-form-item label="电话">
          <el-input v-model="contactForm.phone" placeholder="联系电话" maxlength="30" />
        </el-form-item>
        <el-form-item label="邮箱">
          <el-input v-model="contactForm.email" placeholder="邮箱" maxlength="100" />
        </el-form-item>
        <el-form-item label="职位">
          <el-input v-model="contactForm.position" placeholder="职位" maxlength="50" />
        </el-form-item>
        <el-form-item label="主要联系人">
          <el-switch v-model="contactForm.isPrimary" :active-value="1" :inactive-value="0" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="contactEditDialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="contactSaving" @click="submitContact">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { ElMessage, type FormInstance, type FormRules } from 'element-plus'
import { Search, Refresh, Plus } from '@element-plus/icons-vue'
import {
  getBrandPage, getBrandDetail, createBrand, updateBrand, deleteBrand,
  getBrandContacts, addBrandContact, updateBrandContact, deleteBrandContact,
  type BrandVO, type BrandContactVO,
} from '@/api/base/brand'
import { useAppStore } from '@/store/modules/app'

useAppStore().setPageTitle('品牌管理')

// ─────────── 列表 ───────────
const loading = ref(false)
const tableData = ref<BrandVO[]>([])
const total = ref(0)

const query = reactive({
  pageNum: 1, pageSize: 10,
  brandNameCn: '', formatType: '',
  brandLevel: undefined as number | undefined,
  cooperationType: undefined as number | undefined,
  businessNature: undefined as number | undefined,
})

async function fetchList() {
  loading.value = true
  try {
    const res = await getBrandPage(query)
    tableData.value = res.records ?? (res as any).data?.records ?? []
    total.value = res.total ?? (res as any).data?.total ?? 0
  } finally { loading.value = false }
}

function handleSearch() { query.pageNum = 1; fetchList() }
function handleReset() {
  Object.assign(query, { pageNum: 1, brandNameCn: '', formatType: '', brandLevel: undefined, cooperationType: undefined, businessNature: undefined })
  fetchList()
}

function levelTagType(level: number) {
  return (level === 1 ? 'danger' : level === 2 ? 'warning' : 'info') as any
}

// ─────────── 新增/编辑 ───────────
const dialogVisible = ref(false)
const dialogTitle = ref('新增品牌')
const isEdit = ref(false)
const submitting = ref(false)
const formRef = ref<FormInstance>()

function defaultForm() {
  return {
    id: undefined as number | undefined,
    brandCode: '', brandNameCn: '', brandNameEn: '', formatType: '',
    brandLevel: undefined as number | undefined,
    cooperationType: undefined as number | undefined,
    businessNature: undefined as number | undefined,
    chainType: undefined as number | undefined,
    projectStage: '', groupName: '', hqAddress: '', mainCities: '',
    website: '', phone: '',
    brandType: undefined as number | undefined,
    avgRent: undefined as number | undefined,
    minCustomerPrice: undefined as number | undefined,
    brandIntro: '',
  }
}

const form = reactive(defaultForm())
const formRules: FormRules = {
  brandNameCn: [{ required: true, message: '请输入品牌中文名', trigger: 'blur' }],
}

function handleAdd() {
  isEdit.value = false; dialogTitle.value = '新增品牌'
  Object.assign(form, defaultForm()); dialogVisible.value = true
}

async function handleEdit(row: BrandVO) {
  isEdit.value = true; dialogTitle.value = '编辑品牌'
  const data = await getBrandDetail(row.id)
  Object.assign(form, defaultForm(), data)
  dialogVisible.value = true
}

function resetForm() { formRef.value?.clearValidate() }

async function handleSubmit() {
  const valid = await formRef.value?.validate().catch(() => false)
  if (!valid) return
  submitting.value = true
  try {
    if (isEdit.value && form.id) { await updateBrand(form.id, form); ElMessage.success('编辑成功') }
    else { await createBrand(form); ElMessage.success('新增成功') }
    dialogVisible.value = false; fetchList()
  } finally { submitting.value = false }
}

async function handleDelete(id: number) {
  try { await deleteBrand(id); ElMessage.success('删除成功'); fetchList() } catch {}
}

// ─────────── 品牌联系人管理 ───────────
const contactsDialogVisible = ref(false)
const contactsLoading = ref(false)
const currentBrand = ref<BrandVO | null>(null)
const brandContacts = ref<BrandContactVO[]>([])

const contactEditDialogVisible = ref(false)
const contactSaving = ref(false)
const contactEditId = ref<number | null>(null)
const contactFormRef = ref<FormInstance>()
const contactForm = reactive({ contactName: '', phone: '', email: '', position: '', isPrimary: 0 })
const contactFormRules: FormRules = {
  contactName: [{ required: true, message: '姓名不能为空', trigger: 'blur' }],
}

async function openContactsDialog(row: BrandVO) {
  currentBrand.value = row
  contactsDialogVisible.value = true
  await loadContacts(row.id)
}

async function loadContacts(brandId: number) {
  contactsLoading.value = true
  try { brandContacts.value = await getBrandContacts(brandId) }
  finally { contactsLoading.value = false }
}

function openContactEditDialog(row?: BrandContactVO) {
  contactEditId.value = row?.id ?? null
  Object.assign(contactForm, { contactName: '', phone: '', email: '', position: '', isPrimary: 0 })
  if (row) Object.assign(contactForm, row)
  contactEditDialogVisible.value = true
}

async function submitContact() {
  const valid = await contactFormRef.value?.validate().catch(() => false)
  if (!valid) return
  contactSaving.value = true
  const brandId = currentBrand.value!.id
  try {
    if (contactEditId.value) {
      await updateBrandContact(brandId, contactEditId.value, contactForm)
      ElMessage.success('修改成功')
    } else {
      await addBrandContact(brandId, contactForm)
      ElMessage.success('新增成功')
    }
    contactEditDialogVisible.value = false
    await loadContacts(brandId)
  } finally { contactSaving.value = false }
}

async function handleDeleteContact(cid: number) {
  try {
    await deleteBrandContact(currentBrand.value!.id, cid)
    ElMessage.success('删除成功')
    await loadContacts(currentBrand.value!.id)
  } catch {}
}

onMounted(() => fetchList())
</script>

<style scoped lang="scss">
.brand-page {
  display: flex;
  flex-direction: column;
  gap: 12px;

  .search-card :deep(.el-form-item) { margin-bottom: 0; }
  .toolbar { margin-bottom: 12px; }
  .pagination { margin-top: 16px; display: flex; justify-content: flex-end; }
}

.tab-toolbar { margin-bottom: 12px; }
</style>
