<template>
  <div class="news-page">
    <!-- 搜索栏 -->
    <el-card shadow="never" class="search-card">
      <el-form :model="query" inline @keyup.enter="handleSearch">
        <el-form-item label="标题">
          <el-input v-model="query.title" placeholder="请输入标题关键字" clearable style="width: 180px" />
        </el-form-item>
        <el-form-item label="资讯分类">
          <el-select v-model="query.category" placeholder="全部" clearable style="width: 120px">
            <el-option label="新闻" :value="1" />
            <el-option label="政策" :value="2" />
            <el-option label="招商动态" :value="3" />
            <el-option label="服务指南" :value="4" />
          </el-select>
        </el-form-item>
        <el-form-item label="状态">
          <el-select v-model="query.status" placeholder="全部" clearable style="width: 100px">
            <el-option label="草稿" :value="0" />
            <el-option label="上架" :value="1" />
            <el-option label="下架" :value="2" />
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
        <el-button type="primary" :icon="Plus" @click="handleAdd">新增资讯</el-button>
      </div>

      <el-table v-loading="loading" :data="tableData" border stripe row-key="id" style="width: 100%">
        <el-table-column type="index" label="序号" width="60" align="center" />
        <el-table-column prop="title" label="标题" min-width="200" show-overflow-tooltip />
        <el-table-column label="资讯分类" width="110" align="center">
          <template #default="{ row }">
            <el-tag type="info" size="small">{{ row.categoryName || '-' }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="状态" width="90" align="center">
          <template #default="{ row }">
            <el-tag :type="statusTagType(row.status)" size="small">{{ row.statusName || '-' }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="publishTime" label="发布时间" width="170" align="center" />
        <el-table-column prop="createdAt" label="创建时间" width="170" align="center" />
        <el-table-column label="操作" width="220" align="center" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" size="small" @click="handleEdit(row)">编辑</el-button>
            <el-divider direction="vertical" />
            <el-button v-if="row.status !== 1" link type="success" size="small" @click="handlePublish(row.id)">上架</el-button>
            <el-button v-else link type="warning" size="small" @click="handleUnpublish(row.id)">下架</el-button>
            <el-divider direction="vertical" />
            <el-popconfirm title="确认删除该资讯？" confirm-button-text="确认" cancel-button-text="取消" @confirm="handleDelete(row.id)">
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
    <el-dialog v-model="dialogVisible" :title="dialogTitle" width="700px" :close-on-click-modal="false" destroy-on-close @close="resetForm">
      <el-form ref="formRef" :model="form" :rules="formRules" label-width="100px">
        <el-form-item label="标题" prop="title">
          <el-input v-model="form.title" placeholder="请输入标题" maxlength="200" show-word-limit />
        </el-form-item>
        <el-form-item label="资讯分类">
          <el-select v-model="form.category" placeholder="请选择" clearable style="width: 160px">
            <el-option label="新闻" :value="1" />
            <el-option label="政策" :value="2" />
            <el-option label="招商动态" :value="3" />
            <el-option label="服务指南" :value="4" />
          </el-select>
        </el-form-item>
        <el-form-item label="发布时间">
          <el-date-picker
            v-model="form.publishTime"
            type="datetime"
            placeholder="选择发布时间（可选）"
            format="YYYY-MM-DD HH:mm:ss"
            value-format="YYYY-MM-DD HH:mm:ss"
            style="width: 220px"
          />
        </el-form-item>
        <el-form-item label="内容">
          <el-input v-model="form.content" type="textarea" :rows="8" placeholder="请输入资讯内容" maxlength="10000" show-word-limit />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="submitting" @click="handleSubmit">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { ElMessage, type FormInstance, type FormRules } from 'element-plus'
import { Search, Refresh, Plus } from '@element-plus/icons-vue'
import {
  getNewsPage, getNewsById, createNews, updateNews, deleteNews, publishNews, unpublishNews,
  type NewsVO, type NewsQuery, type NewsSaveDTO,
} from '@/api/base/news'
import { useAppStore } from '@/store/modules/app'

useAppStore().setPageTitle('新闻资讯')

// ─────────── 列表 ───────────
const loading = ref(false)
const tableData = ref<NewsVO[]>([])
const total = ref(0)

const query = reactive<NewsQuery & { pageNum: number; pageSize: number }>({
  pageNum: 1, pageSize: 20, title: '', category: '', status: '',
})

async function fetchList() {
  loading.value = true
  try {
    const res = await getNewsPage({
      ...query,
      category: query.category === '' ? undefined : query.category,
      status: query.status === '' ? undefined : query.status,
    })
    tableData.value = res.records
    total.value = res.total
  } finally { loading.value = false }
}

function handleSearch() { query.pageNum = 1; fetchList() }
function handleReset() {
  Object.assign(query, { pageNum: 1, title: '', category: '', status: '' })
  fetchList()
}

function statusTagType(status: number) {
  return (status === 1 ? 'success' : status === 2 ? 'info' : '') as any
}

// ─────────── 新增/编辑 ───────────
const dialogVisible = ref(false)
const dialogTitle = ref('新增资讯')
const isEdit = ref(false)
const submitting = ref(false)
const formRef = ref<FormInstance>()
const editId = ref<number | null>(null)

const defaultForm = (): NewsSaveDTO => ({
  title: '', content: '', category: null, status: 0, publishTime: '',
})

const form = reactive<NewsSaveDTO>(defaultForm())
const formRules: FormRules = {
  title: [{ required: true, message: '标题不能为空', trigger: 'blur' }],
}

function handleAdd() {
  isEdit.value = false; dialogTitle.value = '新增资讯'; editId.value = null
  Object.assign(form, defaultForm()); dialogVisible.value = true
}

async function handleEdit(row: NewsVO) {
  isEdit.value = true; dialogTitle.value = '编辑资讯'; editId.value = row.id
  const data = await getNewsById(row.id)
  Object.assign(form, { title: data.title, content: data.content, category: data.category, status: data.status, publishTime: data.publishTime || '' })
  dialogVisible.value = true
}

function resetForm() { formRef.value?.clearValidate() }

async function handleSubmit() {
  const valid = await formRef.value?.validate().catch(() => false)
  if (!valid) return
  submitting.value = true
  try {
    if (isEdit.value && editId.value) { await updateNews(editId.value, form); ElMessage.success('修改成功') }
    else { await createNews(form); ElMessage.success('新增成功') }
    dialogVisible.value = false; fetchList()
  } finally { submitting.value = false }
}

async function handleDelete(id: number) {
  try { await deleteNews(id); ElMessage.success('删除成功'); fetchList() } catch {}
}

async function handlePublish(id: number) {
  try { await publishNews(id); ElMessage.success('上架成功'); fetchList() } catch {}
}

async function handleUnpublish(id: number) {
  try { await unpublishNews(id); ElMessage.success('下架成功'); fetchList() } catch {}
}

onMounted(() => fetchList())
</script>

<style scoped lang="scss">
.news-page {
  display: flex;
  flex-direction: column;
  gap: 12px;

  .search-card :deep(.el-form-item) { margin-bottom: 0; }
  .toolbar { margin-bottom: 12px; }
  .pagination { margin-top: 16px; display: flex; justify-content: flex-end; }
}
</style>
