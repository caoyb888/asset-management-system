<template>
  <div class="page-container">
    <div class="page-header">
      <div class="page-header-left">
        <h2 class="page-title">岗位管理</h2>
        <p class="page-desc">管理系统岗位配置</p>
      </div>
      <el-button type="primary" :icon="Plus" @click="openCreate">新增岗位</el-button>
    </div>

    <el-card class="search-card" shadow="never">
      <el-form :model="query" inline>
        <el-form-item label="岗位编码">
          <el-input v-model="query.postCode" placeholder="岗位编码" clearable style="width:140px" />
        </el-form-item>
        <el-form-item label="岗位名称">
          <el-input v-model="query.postName" placeholder="岗位名称" clearable style="width:140px" />
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
        <el-table-column prop="id"       label="ID"    width="70" />
        <el-table-column prop="postCode" label="岗位编码" width="140" />
        <el-table-column prop="postName" label="岗位名称" />
        <el-table-column prop="sortOrder" label="排序" width="80" align="center" />
        <el-table-column prop="status" label="状态" width="90" align="center">
          <template #default="{ row }">
            <el-tag :type="row.status === 1 ? 'success' : 'danger'" size="small">
              {{ row.status === 1 ? '正常' : '停用' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="160" fixed="right" align="center">
          <template #default="{ row }">
            <el-button type="primary" link size="small" @click="openEdit(row)">编辑</el-button>
            <el-button
              :type="row.status === 1 ? 'info' : 'success'" link size="small"
              @click="toggleStatus(row)">{{ row.status === 1 ? '停用' : '启用' }}</el-button>
            <el-button type="danger" link size="small" @click="doDelete(row.id)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>
      <el-pagination v-model:current-page="query.pageNum" v-model:page-size="query.pageSize"
        :total="total" layout="total, prev, pager, next" class="mt-16" @change="loadData" />
    </el-card>

    <el-dialog v-model="dialogVisible" :title="dialogTitle" width="460px" destroy-on-close>
      <el-form ref="formRef" :model="form" :rules="formRules" label-width="90px">
        <el-form-item label="岗位编码" prop="postCode">
          <el-input v-model="form.postCode" :disabled="!!form.id" placeholder="请输入岗位编码" />
        </el-form-item>
        <el-form-item label="岗位名称" prop="postName">
          <el-input v-model="form.postName" placeholder="请输入岗位名称" />
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
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Plus, Search, Refresh } from '@element-plus/icons-vue'
import { postApi, type SysPost, type PostCreateDTO } from '@/api/sys/post'

const loading = ref(false)
const submitting = ref(false)
const tableData = ref<SysPost[]>([])
const total = ref(0)
const query = reactive({ pageNum: 1, pageSize: 20, postCode: '', postName: '', status: undefined as number | undefined })

const dialogVisible = ref(false)
const dialogTitle = ref('')
const form = reactive<PostCreateDTO>({ postCode: '', postName: '', sortOrder: 0, status: 1 })
const formRef = ref()
const formRules = {
  postCode: [{ required: true, message: '请输入岗位编码', trigger: 'blur' }],
  postName: [{ required: true, message: '请输入岗位名称', trigger: 'blur' }],
}

async function loadData() {
  loading.value = true
  try {
    const res: any = await postApi.page(query)
    tableData.value = res.records ?? []
    total.value = res.total ?? 0
  } finally {
    loading.value = false
  }
}

function resetQuery() {
  Object.assign(query, { pageNum: 1, postCode: '', postName: '', status: undefined })
  loadData()
}

function openCreate() {
  Object.assign(form, { id: undefined, postCode: '', postName: '', sortOrder: 0, status: 1, remark: '' })
  dialogTitle.value = '新增岗位'
  dialogVisible.value = true
}

function openEdit(row: SysPost) {
  Object.assign(form, { ...row })
  dialogTitle.value = '编辑岗位'
  dialogVisible.value = true
}

async function doSubmit() {
  await formRef.value?.validate()
  submitting.value = true
  try {
    form.id ? await postApi.update(form.id, form) : await postApi.create(form)
    ElMessage.success('操作成功')
    dialogVisible.value = false
    loadData()
  } finally {
    submitting.value = false
  }
}

async function toggleStatus(row: SysPost) {
  const s = row.status === 1 ? 0 : 1
  await postApi.changeStatus(row.id, s)
  ElMessage.success(s === 1 ? '已启用' : '已停用')
  loadData()
}

async function doDelete(id: number) {
  await ElMessageBox.confirm('确认删除该岗位？', '警告', { type: 'warning' })
  await postApi.delete(id)
  ElMessage.success('删除成功')
  loadData()
}

onMounted(loadData)
</script>
<style scoped lang="scss">
.mt-16 { margin-top: 16px; }
</style>
