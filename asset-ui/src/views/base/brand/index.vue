<template>
  <div class="brand-page">
    <!-- 搜索栏 -->
    <el-card shadow="never" class="search-card">
      <el-form :model="query" inline @keyup.enter="handleSearch">
        <el-form-item label="品牌名称">
          <el-input
            v-model="query.brandNameCn"
            placeholder="请输入品牌名称"
            clearable
            style="width: 160px"
          />
        </el-form-item>
        <el-form-item label="所属业态">
          <el-input
            v-model="query.formatType"
            placeholder="业态"
            clearable
            style="width: 130px"
          />
        </el-form-item>
        <el-form-item label="品牌等级">
          <el-select v-model="query.brandLevel" placeholder="全部" clearable style="width: 110px">
            <el-option label="高端" :value="1" />
            <el-option label="中端" :value="2" />
            <el-option label="大众" :value="3" />
          </el-select>
        </el-form-item>
        <el-form-item label="合作关系">
          <el-select v-model="query.cooperationType" placeholder="全部" clearable style="width: 110px">
            <el-option label="直营" :value="1" />
            <el-option label="加盟" :value="2" />
            <el-option label="代理" :value="3" />
          </el-select>
        </el-form-item>
        <el-form-item label="经营性质">
          <el-select v-model="query.businessNature" placeholder="全部" clearable style="width: 110px">
            <el-option label="餐饮" :value="1" />
            <el-option label="零售" :value="2" />
            <el-option label="娱乐" :value="3" />
            <el-option label="服务" :value="4" />
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

      <el-table
        v-loading="loading"
        :data="tableData"
        border
        stripe
        row-key="id"
        style="width: 100%"
      >
        <el-table-column type="index" label="序号" width="60" align="center" />
        <el-table-column prop="brandCode" label="品牌编码" width="120" show-overflow-tooltip />
        <el-table-column prop="brandNameCn" label="品牌名(中)" min-width="140" show-overflow-tooltip />
        <el-table-column prop="brandNameEn" label="品牌名(英)" min-width="130" show-overflow-tooltip />
        <el-table-column prop="formatType" label="所属业态" width="110" show-overflow-tooltip />
        <el-table-column label="品牌等级" width="90" align="center">
          <template #default="{ row }">
            <el-tag :type="levelTagType(row.brandLevel)" size="small">
              {{ row.brandLevelName || '-' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="cooperationTypeName" label="合作关系" width="90" align="center" />
        <el-table-column prop="businessNatureName" label="经营性质" width="90" align="center" />
        <el-table-column prop="brandTypeName" label="品牌类型" width="80" align="center" />
        <el-table-column prop="groupName" label="集团名称" width="140" show-overflow-tooltip />
        <el-table-column prop="phone" label="联系电话" width="120" />
        <el-table-column prop="createdAt" label="创建时间" width="160" align="center" />
        <el-table-column label="操作" width="150" align="center" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" size="small" @click="handleEdit(row)">编辑</el-button>
            <el-divider direction="vertical" />
            <el-popconfirm
              title="确认删除该品牌？"
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
      width="800px"
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
                <el-option label="高端" :value="1" />
                <el-option label="中端" :value="2" />
                <el-option label="大众" :value="3" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="合作关系">
              <el-select v-model="form.cooperationType" placeholder="请选择" clearable style="width: 100%">
                <el-option label="直营" :value="1" />
                <el-option label="加盟" :value="2" />
                <el-option label="代理" :value="3" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="经营性质">
              <el-select v-model="form.businessNature" placeholder="请选择" clearable style="width: 100%">
                <el-option label="餐饮" :value="1" />
                <el-option label="零售" :value="2" />
                <el-option label="娱乐" :value="3" />
                <el-option label="服务" :value="4" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="连锁类型">
              <el-select v-model="form.chainType" placeholder="请选择" clearable style="width: 100%">
                <el-option label="连锁" :value="1" />
                <el-option label="单店" :value="2" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="品牌类型">
              <el-select v-model="form.brandType" placeholder="请选择" clearable style="width: 100%">
                <el-option label="MALL" :value="1" />
                <el-option label="商街" :value="2" />
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
              <el-input-number
                v-model="form.avgRent"
                :min="0"
                :precision="2"
                placeholder="元/㎡·月"
                style="width: 100%"
                controls-position="right"
              />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="最低客单价">
              <el-input-number
                v-model="form.minCustomerPrice"
                :min="0"
                :precision="2"
                placeholder="元"
                style="width: 100%"
                controls-position="right"
              />
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
              <el-input
                v-model="form.brandIntro"
                type="textarea"
                :rows="3"
                placeholder="品牌简介"
                maxlength="2000"
                show-word-limit
              />
            </el-form-item>
          </el-col>
        </el-row>

        <!-- 联系人 -->
        <el-divider content-position="left">联系人</el-divider>
        <div v-for="(contact, idx) in form.contacts" :key="idx" class="contact-row">
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
  getBrandPage,
  getBrandDetail,
  createBrand,
  updateBrand,
  deleteBrand,
} from '@/api/base/brand'

/* ------------------------------------------------------------------ */
/* 列表查询                                                               */
/* ------------------------------------------------------------------ */
const loading = ref(false)
const tableData = ref<any[]>([])
const total = ref(0)

const query = reactive({
  pageNum: 1,
  pageSize: 10,
  brandNameCn: '',
  formatType: '',
  brandLevel: undefined as number | undefined,
  cooperationType: undefined as number | undefined,
  businessNature: undefined as number | undefined,
})

async function fetchList() {
  loading.value = true
  try {
    const res = await getBrandPage(query)
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
  query.brandNameCn = ''
  query.formatType = ''
  query.brandLevel = undefined
  query.cooperationType = undefined
  query.businessNature = undefined
  fetchList()
}

function levelTagType(level: number) {
  return level === 1 ? 'danger' : level === 2 ? 'warning' : 'info'
}

/* ------------------------------------------------------------------ */
/* 新增/编辑                                                               */
/* ------------------------------------------------------------------ */
const dialogVisible = ref(false)
const dialogTitle = ref('新增品牌')
const isEdit = ref(false)
const submitting = ref(false)
const formRef = ref()

function defaultForm() {
  return {
    id: undefined as number | undefined,
    brandCode: '',
    brandNameCn: '',
    brandNameEn: '',
    formatType: '',
    brandLevel: undefined as number | undefined,
    cooperationType: undefined as number | undefined,
    businessNature: undefined as number | undefined,
    chainType: undefined as number | undefined,
    projectStage: '',
    groupName: '',
    hqAddress: '',
    mainCities: '',
    website: '',
    phone: '',
    brandType: undefined as number | undefined,
    avgRent: undefined as number | undefined,
    minCustomerPrice: undefined as number | undefined,
    brandIntro: '',
    contacts: [] as Array<{
      id?: number
      contactName: string
      phone: string
      email: string
      position: string
      isPrimary: number
    }>,
  }
}

const form = reactive(defaultForm())

const formRules = {
  brandNameCn: [{ required: true, message: '请输入品牌中文名', trigger: 'blur' }],
}

function handleAdd() {
  isEdit.value = false
  dialogTitle.value = '新增品牌'
  Object.assign(form, defaultForm())
  dialogVisible.value = true
}

async function handleEdit(row: any) {
  isEdit.value = true
  dialogTitle.value = '编辑品牌'
  // 拉取详情（含联系人）
  const data = await getBrandDetail(row.id)
  Object.assign(form, defaultForm(), {
    ...data,
    contacts: (data.contacts ?? []).map((c: any) => ({ ...c })),
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

async function handleSubmit() {
  await formRef.value?.validate()
  submitting.value = true
  try {
    if (isEdit.value && form.id) {
      await updateBrand(form.id, form)
      ElMessage.success('编辑成功')
    } else {
      await createBrand(form)
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
  await deleteBrand(id)
  ElMessage.success('删除成功')
  fetchList()
}

onMounted(() => fetchList())
</script>

<style scoped lang="scss">
.brand-page {
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
  .contact-row {
    margin-bottom: 10px;
  }
}
</style>
