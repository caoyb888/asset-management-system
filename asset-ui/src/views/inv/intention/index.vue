<template>
  <div class="page-container">
    <el-card>
      <template #header>
        <div class="card-header">
          <span>意向协议列表</span>
          <el-button type="primary" :icon="Plus" @click="handleCreate">新增意向</el-button>
        </div>
      </template>

      <!-- 搜索栏 -->
      <el-form :model="query" inline class="search-form">
        <el-form-item label="项目">
          <el-input v-model="query.projectName" placeholder="项目名称" clearable />
        </el-form-item>
        <el-form-item label="状态">
          <el-select v-model="query.status" placeholder="全部" clearable style="width: 120px">
            <el-option label="草稿" :value="0" />
            <el-option label="审批中" :value="1" />
            <el-option label="审批通过" :value="2" />
            <el-option label="驳回" :value="3" />
            <el-option label="已转合同" :value="4" />
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" :icon="Search" @click="fetchList">查询</el-button>
          <el-button :icon="Refresh" @click="handleReset">重置</el-button>
        </el-form-item>
      </el-form>

      <!-- 表格 -->
      <el-table v-loading="loading" :data="list" border stripe>
        <el-table-column prop="intentionCode" label="意向编号" width="160" />
        <el-table-column prop="projectName" label="项目" />
        <el-table-column prop="merchantName" label="商家" />
        <el-table-column prop="brandName" label="品牌" />
        <el-table-column prop="status" label="状态" width="100">
          <template #default="{ row }">
            <el-tag :type="statusTagType(row.status)">{{ statusLabel(row.status) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="createdAt" label="创建时间" width="160" />
        <el-table-column label="操作" width="200" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" @click="handleEdit(row)">编辑</el-button>
            <el-button
              v-if="row.status === 2"
              link
              type="success"
              @click="handleConvert(row)"
            >转合同</el-button>
            <el-button link type="danger" @click="handleDelete(row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>

      <!-- 分页 -->
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
import { getIntentionPage, deleteIntention, type IntentionVO } from '@/api/inv/intention'

const router = useRouter()
const loading = ref(false)
const list = ref<IntentionVO[]>([])
const total = ref(0)

const query = reactive({
  page: 1,
  size: 10,
  projectName: '',
  status: undefined as number | undefined,
})

type TagType = 'primary' | 'success' | 'warning' | 'danger' | 'info' | undefined
const STATUS_MAP: Record<number, { label: string; type: TagType }> = {
  0: { label: '草稿', type: 'info' },
  1: { label: '审批中', type: 'warning' },
  2: { label: '审批通过', type: 'success' },
  3: { label: '驳回', type: 'danger' },
  4: { label: '已转合同', type: undefined },
}

const statusLabel = (s: number) => STATUS_MAP[s]?.label ?? '-'
const statusTagType = (s: number) => STATUS_MAP[s]?.type ?? 'info'

async function fetchList() {
  loading.value = true
  try {
    const res = await getIntentionPage(query)
    list.value = res.records
    total.value = res.total
  } finally {
    loading.value = false
  }
}

function handleReset() {
  query.page = 1
  query.projectName = ''
  query.status = undefined
  fetchList()
}

function handleCreate() {
  router.push('/inv/intentions/form')
}

function handleEdit(row: IntentionVO) {
  router.push({ path: '/inv/intentions/form', query: { id: row.id } })
}

function handleConvert(row: IntentionVO) {
  router.push({ path: '/inv/contracts/from-intention', query: { intentionId: row.id } })
}

async function handleDelete(row: IntentionVO) {
  await ElMessageBox.confirm(`确认删除意向协议 "${row.intentionCode}"？`, '提示', { type: 'warning' })
  await deleteIntention(row.id)
  ElMessage.success('删除成功')
  fetchList()
}

onMounted(fetchList)
</script>

<style scoped>
.card-header { display: flex; justify-content: space-between; align-items: center; }
.search-form { margin-bottom: 12px; }
.mt-4 { margin-top: 16px; }
</style>
