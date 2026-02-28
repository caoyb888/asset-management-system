<template>
  <div class="notice-page">
    <!-- 搜索栏 -->
    <el-card shadow="never" class="filter-card">
      <el-form :model="query" inline @keyup.enter="handleSearch">
        <el-form-item label="标题">
          <el-input v-model="query.title" placeholder="请输入标题关键字" clearable style="width: 180px" />
        </el-form-item>
        <el-form-item label="公告类型">
          <el-select v-model="query.noticeType" placeholder="全部" clearable style="width: 110px">
            <el-option label="通知" :value="1" />
            <el-option label="公告" :value="2" />
            <el-option label="政策" :value="3" />
          </el-select>
        </el-form-item>
        <el-form-item label="状态">
          <el-select v-model="query.status" placeholder="全部" clearable style="width: 110px">
            <el-option label="草稿" :value="0" />
            <el-option label="已发布" :value="1" />
            <el-option label="已下架" :value="2" />
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
      <div class="card-header">
        <div class="header-left">
          <span class="header-title">公告列表</span>
          <span class="count-tag">共 {{ total }} 条</span>
        </div>
        <div class="header-actions">
          <el-button type="primary" :icon="Plus" @click="handleAdd">新增公告</el-button>
        </div>
      </div>

      <div class="table-body">
      <el-table v-loading="loading" :data="tableData" border stripe row-key="id" style="width: 100%">
        <el-table-column type="index" label="序号" width="60" align="center" />
        <el-table-column prop="title" label="标题" min-width="200" show-overflow-tooltip />
        <el-table-column label="公告类型" width="100" align="center">
          <template #default="{ row }">
            <el-tag type="info" size="small">{{ row.noticeTypeName || '-' }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="状态" width="90" align="center">
          <template #default="{ row }">
            <el-tag :type="statusTagType(row.status)" size="small">{{ row.statusName || '-' }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="publishTime" label="发布时间" width="170" align="center" />
        <el-table-column prop="scheduledTime" label="计划发布时间" width="170" align="center" />
        <el-table-column prop="createdAt" label="创建时间" width="170" align="center" />
        <el-table-column label="已读" width="80" align="center">
          <template #default="{ row }">
            <el-popover
              :width="160"
              trigger="hover"
              @show="loadReadStats(row.id)"
            >
              <template #reference>
                <el-tag type="info" size="small" style="cursor:pointer">
                  {{ readStatsMap[row.id]?.readCount ?? '–' }} 人
                </el-tag>
              </template>
              <div style="font-size:13px">
                <div>已读人数：{{ readStatsMap[row.id]?.readCount ?? '-' }}</div>
                <div>我已阅读：{{ readStatsMap[row.id]?.currentUserRead ? '是' : '否' }}</div>
              </div>
            </el-popover>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="260" align="center" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" size="small" @click="handleEdit(row)">编辑</el-button>
            <el-divider direction="vertical" />
            <el-button v-if="row.status !== 1" link type="success" size="small" @click="handlePublish(row.id)">发布</el-button>
            <el-button v-else link type="warning" size="small" @click="handleUnpublish(row.id)">下架</el-button>
            <el-divider direction="vertical" />
            <el-button link type="info" size="small" @click="handleMarkRead(row.id)">标记已读</el-button>
            <el-divider direction="vertical" />
            <el-popconfirm title="确认删除该公告？" confirm-button-text="确认" cancel-button-text="取消" @confirm="handleDelete(row.id)">
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
    <el-dialog v-model="dialogVisible" :title="dialogTitle" width="680px" :close-on-click-modal="false" destroy-on-close @close="resetForm">
      <el-form ref="formRef" :model="form" :rules="formRules" label-width="100px">
        <el-form-item label="标题" prop="title">
          <el-input v-model="form.title" placeholder="请输入标题" maxlength="200" show-word-limit />
        </el-form-item>
        <el-form-item label="公告类型">
          <el-select v-model="form.noticeType" placeholder="请选择" clearable style="width: 160px">
            <el-option label="通知" :value="1" />
            <el-option label="公告" :value="2" />
            <el-option label="政策" :value="3" />
          </el-select>
        </el-form-item>
        <el-form-item label="计划发布时间">
          <el-date-picker
            v-model="form.scheduledTime"
            type="datetime"
            placeholder="选择计划发布时间"
            format="YYYY-MM-DD HH:mm:ss"
            value-format="YYYY-MM-DD HH:mm:ss"
            style="width: 220px"
          />
        </el-form-item>
        <el-form-item label="内容">
          <el-input v-model="form.content" type="textarea" :rows="8" placeholder="请输入公告内容" maxlength="10000" show-word-limit />
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
  getNoticePage, getNoticeById, createNotice, updateNotice, deleteNotice,
  publishNotice, unpublishNotice, markNoticeRead, getNoticeReadStats,
  type NoticeVO, type NoticeQuery, type NoticeSaveDTO, type NoticeReadStatsVO,
} from '@/api/base/notice'
import { useAppStore } from '@/store/modules/app'

useAppStore().setPageTitle('通知公告')

// ─────────── 列表 ───────────
const loading = ref(false)
const tableData = ref<NoticeVO[]>([])
const total = ref(0)

const query = reactive<NoticeQuery & { pageNum: number; pageSize: number }>({
  pageNum: 1, pageSize: 20, title: '', noticeType: '', status: '',
})

async function fetchList() {
  loading.value = true
  try {
    const res = await getNoticePage({
      ...query,
      noticeType: query.noticeType === '' ? undefined : query.noticeType,
      status: query.status === '' ? undefined : query.status,
    })
    tableData.value = res.records
    total.value = res.total
  } finally { loading.value = false }
}

function handleSearch() { query.pageNum = 1; fetchList() }
function handleReset() {
  Object.assign(query, { pageNum: 1, title: '', noticeType: '', status: '' })
  fetchList()
}

function statusTagType(status: number) {
  return (status === 1 ? 'success' : status === 2 ? 'info' : '') as any
}

// ─────────── 新增/编辑 ───────────
const dialogVisible = ref(false)
const dialogTitle = ref('新增公告')
const isEdit = ref(false)
const submitting = ref(false)
const formRef = ref<FormInstance>()
const editId = ref<number | null>(null)

const defaultForm = (): NoticeSaveDTO => ({
  title: '', content: '', noticeType: null, status: 0, scheduledTime: '',
})

const form = reactive<NoticeSaveDTO>(defaultForm())
const formRules: FormRules = {
  title: [{ required: true, message: '标题不能为空', trigger: 'blur' }],
}

function handleAdd() {
  isEdit.value = false; dialogTitle.value = '新增公告'; editId.value = null
  Object.assign(form, defaultForm()); dialogVisible.value = true
}

async function handleEdit(row: NoticeVO) {
  isEdit.value = true; dialogTitle.value = '编辑公告'; editId.value = row.id
  const data = await getNoticeById(row.id)
  Object.assign(form, { title: data.title, content: data.content, noticeType: data.noticeType, status: data.status, scheduledTime: data.scheduledTime || '' })
  dialogVisible.value = true
}

function resetForm() { formRef.value?.clearValidate() }

async function handleSubmit() {
  const valid = await formRef.value?.validate().catch(() => false)
  if (!valid) return
  submitting.value = true
  try {
    if (isEdit.value && editId.value) { await updateNotice(editId.value, form); ElMessage.success('修改成功') }
    else { await createNotice(form); ElMessage.success('新增成功') }
    dialogVisible.value = false; fetchList()
  } finally { submitting.value = false }
}

async function handleDelete(id: number) {
  try { await deleteNotice(id); ElMessage.success('删除成功'); fetchList() } catch {}
}

async function handlePublish(id: number) {
  try { await publishNotice(id); ElMessage.success('发布成功'); fetchList() } catch {}
}

async function handleUnpublish(id: number) {
  try { await unpublishNotice(id); ElMessage.success('下架成功'); fetchList() } catch {}
}

// ─────────── 已读追踪 ───────────
const readStatsMap = reactive<Record<number, NoticeReadStatsVO>>({})

async function loadReadStats(id: number) {
  if (readStatsMap[id]) return // 已缓存则不重复请求
  try {
    const stats = await getNoticeReadStats(id)
    readStatsMap[id] = stats
  } catch {}
}

async function handleMarkRead(id: number) {
  try {
    await markNoticeRead(id)
    ElMessage.success('已标记为已读')
    // 刷新该条统计
    delete readStatsMap[id]
    await loadReadStats(id)
  } catch {}
}

onMounted(() => fetchList())
</script>

<style scoped lang="scss">
.notice-page {
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
</style>
