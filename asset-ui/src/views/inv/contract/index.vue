<template>
  <div class="page-container">
    <el-card>
      <template #header>
        <div class="card-header">
          <span>招商合同列表</span>
          <div class="header-actions">
            <el-button @click="router.push('/inv/contracts/from-intention')">意向转合同</el-button>
            <el-button type="primary" :icon="Plus" @click="router.push('/inv/contracts/form')">新增合同</el-button>
          </div>
        </div>
      </template>

      <!-- 搜索栏 -->
      <el-form :model="query" inline class="search-form">
        <el-form-item label="合同编号">
          <el-input v-model="query.contractCode" placeholder="合同编号" clearable />
        </el-form-item>
        <el-form-item label="商家">
          <el-input v-model="query.merchantName" placeholder="商家名称" clearable />
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

      <!-- 表格 -->
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
        <el-table-column label="操作" width="160" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" @click="handleEdit(row)">编辑</el-button>
            <el-button link type="info" @click="handleDetail(row)">详情</el-button>
          </template>
        </el-table-column>
      </el-table>

      <el-pagination
        v-model:current-page="query.page"
        v-model:page-size="query.size"
        :total="total"
        :page-sizes="[10, 20, 50]"
        layout="total, sizes, prev, pager, next"
        class="mt-4"
        @change="fetchList"
      />
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { Plus, Search, Refresh } from '@element-plus/icons-vue'
import { getContractPage, type ContractVO } from '@/api/inv/contract'

const router = useRouter()
const loading = ref(false)
const list = ref<ContractVO[]>([])
const total = ref(0)

const query = reactive({
  page: 1, size: 10,
  contractCode: '', merchantName: '',
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
  query.page = 1; query.contractCode = ''; query.merchantName = ''; query.status = undefined
  fetchList()
}

function handleEdit(row: ContractVO) {
  router.push({ path: '/inv/contracts/form', query: { id: row.id } })
}

function handleDetail(row: ContractVO) {
  router.push({ path: '/inv/contracts/form', query: { id: row.id, readonly: '1' } })
}

onMounted(fetchList)
</script>

<style scoped>
.card-header { display: flex; justify-content: space-between; align-items: center; }
.header-actions { display: flex; gap: 8px; }
.search-form { margin-bottom: 12px; }
.mt-4 { margin-top: 16px; }
.text-warning { color: #e6a23c; font-weight: 500; }
</style>
