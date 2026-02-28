<template>
  <div class="contract-page">
    <!-- 搜索栏 -->
    <el-card shadow="never" class="filter-card">
      <el-form :model="query" inline @keyup.enter="fetchList">
        <el-form-item label="关键词">
          <el-input v-model="query.keyword" placeholder="合同编号/名称" clearable />
        </el-form-item>
        <el-form-item label="状态">
          <el-select v-model="query.status" placeholder="全部" clearable style="width: 120px">
            <el-option label="草稿" :value="0" />
            <el-option label="审批中" :value="1" />
            <el-option label="生效" :value="2" />
            <el-option label="到期" :value="3" />
            <el-option label="终止" :value="4" />
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" :icon="Search" @click="fetchList">查询</el-button>
          <el-button :icon="Refresh" @click="handleReset">重置</el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <!-- 表格 -->
    <el-card shadow="never" class="table-card">
      <div class="card-header">
        <div class="header-left">
          <span class="header-title">招商合同列表</span>
          <span class="count-tag">共 {{ total }} 条</span>
        </div>
        <div class="header-actions">
          <el-button @click="router.push('/inv/contracts/from-intention')">意向转合同</el-button>
          <el-button type="primary" :icon="Plus" @click="router.push('/inv/contracts/form')">新增合同</el-button>
        </div>
      </div>

      <div class="table-body">
      <el-table v-loading="loading" :data="list" border stripe>
        <el-table-column prop="contractCode" label="合同编号" width="160" />
        <el-table-column prop="contractName" label="合同名称" />
        <el-table-column prop="merchantName" label="商家" />
        <el-table-column prop="brandName" label="品牌" />
        <el-table-column prop="contractStart" label="开始日期" width="110" />
        <el-table-column prop="contractEnd" label="结束日期" width="110">
          <template #default="{ row }">
            <span :class="{ 'text-warning': isExpiringSoon(row.contractEnd) }">
              {{ row.contractEnd }}
            </span>
          </template>
        </el-table-column>
        <el-table-column prop="status" label="状态" width="90">
          <template #default="{ row }">
            <el-tag :type="statusTagType(row.status)">{{ statusLabel(row.status) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="200" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" @click="handleEdit(row)">编辑</el-button>
            <el-button link type="info" @click="handleDetail(row)">详情</el-button>
            <el-button v-if="row.status === 0" link type="danger" @click="handleDelete(row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>

      <div class="pagination">
        <el-pagination
          v-model:current-page="query.pageNum"
          v-model:page-size="query.pageSize"
          :total="total"
          :page-sizes="[10, 20, 50]"
          layout="total, sizes, prev, pager, next, jumper"
          background
          @change="fetchList"
        />
      </div>
      </div>
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Plus, Search, Refresh } from '@element-plus/icons-vue'
import { getContractPage, deleteContract, type ContractVO } from '@/api/inv/contract'

const router = useRouter()
const loading = ref(false)
const list = ref<ContractVO[]>([])
const total = ref(0)

const query = reactive({
  pageNum: 1, pageSize: 10,
  keyword: '',
  status: undefined as number | undefined,
})

type TagType = 'primary' | 'success' | 'warning' | 'danger' | 'info' | undefined
const STATUS_MAP: Record<number, { label: string; type: TagType }> = {
  0: { label: '草稿', type: 'info' },
  1: { label: '审批中', type: 'warning' },
  2: { label: '生效', type: 'success' },
  3: { label: '到期', type: undefined },
  4: { label: '终止', type: 'danger' },
}

const statusLabel = (s: number) => STATUS_MAP[s]?.label ?? '-'
const statusTagType = (s: number) => STATUS_MAP[s]?.type ?? 'info'

function isExpiringSoon(dateStr: string): boolean {
  if (!dateStr) return false
  const diff = new Date(dateStr).getTime() - Date.now()
  return diff > 0 && diff < 30 * 24 * 3600 * 1000
}

async function fetchList() {
  loading.value = true
  try {
    const res = await getContractPage(query)
    list.value = res.records
    total.value = res.total
  } finally {
    loading.value = false
  }
}

function handleReset() {
  query.pageNum = 1; query.keyword = ''; query.status = undefined
  fetchList()
}

function handleEdit(row: ContractVO) {
  router.push({ path: '/inv/contracts/form', query: { id: row.id } })
}

function handleDetail(row: ContractVO) {
  router.push({ path: '/inv/contracts/form', query: { id: row.id, readonly: '1' } })
}

async function handleDelete(row: ContractVO) {
  await ElMessageBox.confirm(`确认删除合同「${row.contractName}」？此操作不可恢复`, '删除确认', { type: 'warning' })
  try {
    await deleteContract(row.id)
    ElMessage.success('删除成功')
    fetchList()
  } catch (err: unknown) {
    ElMessage.error((err as { message?: string })?.message || '删除失败')
  }
}

onMounted(fetchList)
</script>

<style scoped lang="scss">
.contract-page {
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

.text-warning { color: #e6a23c; font-weight: 500; }
</style>
