<template>
  <div class="page-container">
    <el-card>
      <template #header>
        <div class="card-header">
          <span>租决政策列表</span>
          <el-button type="primary" :icon="Plus" @click="router.push('/inv/rent-policies/form')">新增政策</el-button>
        </div>
      </template>

      <el-form :model="query" inline class="search-form">
        <el-form-item label="政策名称">
          <el-input v-model="query.policyName" placeholder="政策名称" clearable />
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

      <el-table v-loading="loading" :data="list" border stripe>
        <el-table-column prop="policyCode" label="政策编号" width="140" />
        <el-table-column prop="policyName" label="政策名称" />
        <el-table-column prop="projectName" label="适用项目" />
        <el-table-column prop="effectiveYear" label="适用年度" width="100" />
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
import { ElMessage, ElMessageBox } from 'element-plus'
import { Plus, Search, Refresh } from '@element-plus/icons-vue'
import { getRentPolicyPage, deleteRentPolicy, type RentPolicyVO } from '@/api/inv/rentPolicy'

const router = useRouter()
const loading = ref(false)
const list = ref<RentPolicyVO[]>([])
const total = ref(0)
const query = reactive({ page: 1, size: 10, policyName: '', status: undefined as number | undefined })

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

function handleReset() { query.page = 1; query.policyName = ''; query.status = undefined; fetchList() }

async function handleDelete(row: RentPolicyVO) {
  await ElMessageBox.confirm(`确认删除政策 "${row.policyName}"？`, '提示', { type: 'warning' })
  await deleteRentPolicy(row.id)
  ElMessage.success('删除成功'); fetchList()
}

onMounted(fetchList)
</script>

<style scoped>
.card-header { display: flex; justify-content: space-between; align-items: center; }
.search-form { margin-bottom: 12px; }
.mt-4 { margin-top: 16px; }
</style>
