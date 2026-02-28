<template>
  <div class="rent-policy-page">
    <!-- 搜索栏 -->
    <el-card shadow="never" class="filter-card">
      <el-form :model="query" inline @keyup.enter="fetchList">
        <el-form-item label="政策编号">
          <el-input v-model="query.policyCode" placeholder="政策编号" clearable />
        </el-form-item>
        <el-form-item label="状态">
          <el-select v-model="query.status" placeholder="全部" clearable style="width: 120px">
            <el-option label="草稿" :value="0" />
            <el-option label="审批中" :value="1" />
            <el-option label="生效" :value="2" />
            <el-option label="驳回" :value="3" />
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
          <span class="header-title">租决政策列表</span>
          <span class="count-tag">共 {{ total }} 条</span>
        </div>
        <div class="header-actions">
          <el-button type="primary" :icon="Plus" @click="router.push('/inv/rent-policies/form')">新增政策</el-button>
        </div>
      </div>

      <div class="table-body">
      <el-table v-loading="loading" :data="list" border stripe>
        <el-table-column prop="policyCode" label="政策编号" width="150" />
        <el-table-column label="业态范围">
          <template #default="{ row }">{{ row.formatType || '-' }}</template>
        </el-table-column>
        <el-table-column label="最短/最长租期(月)" width="160" align="center">
          <template #default="{ row }">{{ row.minLeaseTerm ?? '-' }} / {{ row.maxLeaseTerm ?? '-' }}</template>
        </el-table-column>
        <el-table-column label="第一年租金(元/㎡/月)" width="170" align="right">
          <template #default="{ row }">{{ row.year1Rent != null ? Number(row.year1Rent).toFixed(2) : '-' }}</template>
        </el-table-column>
        <el-table-column prop="status" label="状态" width="100">
          <template #default="{ row }">
            <el-tag :type="STATUS_MAP[row.status]?.type ?? 'info'">{{ STATUS_MAP[row.status]?.label ?? '-' }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="160" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" @click="router.push({ path: '/inv/rent-policies/form', query: { id: row.id } })">编辑</el-button>
            <el-button link type="danger" @click="handleDelete(row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>

      <div class="pagination">
        <el-pagination
          v-model:current-page="query.page"
          v-model:page-size="query.size"
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
import { getRentPolicyPage, deleteRentPolicy, type RentPolicyVO } from '@/api/inv/rentPolicy'

const router = useRouter()
const loading = ref(false)
const list = ref<RentPolicyVO[]>([])
const total = ref(0)
const query = reactive({ page: 1, size: 10, policyCode: '', status: undefined as number | undefined })

type TagType = 'primary' | 'success' | 'warning' | 'danger' | 'info' | undefined
const STATUS_MAP: Record<number, { label: string; type: TagType }> = {
  0: { label: '草稿', type: 'info' },
  1: { label: '审批中', type: 'warning' },
  2: { label: '生效', type: 'success' },
  3: { label: '驳回', type: 'danger' },
}

async function fetchList() {
  loading.value = true
  try {
    const res = await getRentPolicyPage(query)
    list.value = res.records; total.value = res.total
  } finally { loading.value = false }
}

function handleReset() { query.page = 1; query.policyCode = ''; query.status = undefined; fetchList() }

async function handleDelete(row: RentPolicyVO) {
  await ElMessageBox.confirm(`确认删除政策 "${row.policyCode}"？`, '提示', { type: 'warning' })
  await deleteRentPolicy(row.id)
  ElMessage.success('删除成功'); fetchList()
}

onMounted(fetchList)
</script>

<style scoped lang="scss">
.rent-policy-page {
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
