<template>
  <div class="rpt-shop-rental">
    <!-- 筛选栏 -->
    <el-card class="filter-card" shadow="never">
      <el-form inline :model="filterForm">
        <el-form-item label="项目">
          <el-select
            v-model="filterForm.projectId"
            placeholder="全部项目"
            clearable
            style="width: 180px"
            @change="onProjectChange"
          >
            <el-option v-for="p in projectList" :key="p.id" :label="p.projectName" :value="p.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="楼栋">
          <el-select
            v-model="filterForm.buildingId"
            placeholder="全部楼栋"
            clearable
            style="width: 140px"
            @change="onBuildingChange"
          >
            <el-option v-for="b in buildingList" :key="b.id" :label="b.buildingName" :value="b.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="楼层">
          <el-select v-model="filterForm.floorId" placeholder="全部楼层" clearable style="width: 120px">
            <el-option v-for="f in floorList" :key="f.id" :label="f.floorName" :value="f.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="业态">
          <el-input v-model="filterForm.formatType" placeholder="业态类型" clearable style="width: 120px" />
        </el-form-item>
        <el-form-item label="统计日期">
          <el-date-picker
            v-model="filterForm.statDate"
            type="date"
            placeholder="不选则取最新"
            value-format="YYYY-MM-DD"
            style="width: 160px"
          />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" :loading="loading" @click="handleSearch">查询</el-button>
          <el-button @click="resetFilter">重置</el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <!-- 汇总卡片 -->
    <el-row :gutter="12" class="summary-row">
      <el-col :span="4" v-for="s in summaryCards" :key="s.label">
        <el-card shadow="never" class="summary-card" :style="{ borderLeft: `4px solid ${s.color}` }">
          <div class="s-label">{{ s.label }}</div>
          <div class="s-value" :style="{ color: s.color }">{{ s.value }}</div>
        </el-card>
      </el-col>
    </el-row>

    <!-- 数据表格 -->
    <el-card shadow="never">
      <template #header>
        <span>商铺租赁明细</span>
        <span class="total-tip">共 {{ pagination.total }} 条</span>
      </template>
      <el-table
        v-loading="loading"
        :data="tableData"
        border
        stripe
        size="small"
      >
        <el-table-column prop="projectName" label="项目" min-width="120" show-overflow-tooltip />
        <el-table-column prop="buildingName" label="楼栋" min-width="90" />
        <el-table-column prop="floorName" label="楼层" min-width="80" />
        <el-table-column prop="formatType" label="业态" min-width="80" />
        <el-table-column prop="totalShops" label="总数" width="70" align="right" />
        <el-table-column prop="rentedShops" label="已租" width="70" align="right">
          <template #default="{ row }">
            <span class="text-primary">{{ row.rentedShops ?? '-' }}</span>
          </template>
        </el-table-column>
        <el-table-column prop="vacantShops" label="空置" width="70" align="right">
          <template #default="{ row }">
            <span :class="(row.vacantShops ?? 0) > 0 ? 'text-danger' : ''">{{ row.vacantShops ?? '-' }}</span>
          </template>
        </el-table-column>
        <el-table-column prop="decoratingShops" label="装修中" width="70" align="right">
          <template #default="{ row }">
            <span class="text-warning">{{ row.decoratingShops ?? '-' }}</span>
          </template>
        </el-table-column>
        <el-table-column prop="openedShops" label="已开业" width="70" align="right">
          <template #default="{ row }">
            <span class="text-success">{{ row.openedShops ?? '-' }}</span>
          </template>
        </el-table-column>
        <el-table-column label="总面积(㎡)" min-width="100" align="right">
          <template #default="{ row }">{{ fmtNum(row.totalArea) }}</template>
        </el-table-column>
        <el-table-column label="已租面积(㎡)" min-width="100" align="right">
          <template #default="{ row }">{{ fmtNum(row.rentedArea) }}</template>
        </el-table-column>
        <el-table-column label="出租率" min-width="100">
          <template #default="{ row }">
            <el-progress
              v-if="row.rentalRate != null"
              :percentage="Math.min(row.rentalRate, 100)"
              :format="() => `${row.rentalRate.toFixed(1)}%`"
              :stroke-width="6"
              color="#409eff"
            />
            <span v-else>-</span>
          </template>
        </el-table-column>
        <el-table-column label="空置率" min-width="100">
          <template #default="{ row }">
            <el-progress
              v-if="row.vacancyRate != null"
              :percentage="Math.min(row.vacancyRate, 100)"
              :format="() => `${row.vacancyRate.toFixed(1)}%`"
              :stroke-width="6"
              :color="row.vacancyRate > 20 ? '#f56c6c' : '#e6a23c'"
            />
            <span v-else>-</span>
          </template>
        </el-table-column>
        <el-table-column label="开业率" min-width="100">
          <template #default="{ row }">
            <el-progress
              v-if="row.openingRate != null"
              :percentage="Math.min(row.openingRate, 100)"
              :format="() => `${row.openingRate.toFixed(1)}%`"
              :stroke-width="6"
              color="#67c23a"
            />
            <span v-else>-</span>
          </template>
        </el-table-column>
      </el-table>
      <!-- 分页 -->
      <div class="pagination-wrap">
        <el-pagination
          v-model:current-page="pagination.pageNum"
          v-model:page-size="pagination.pageSize"
          :total="pagination.total"
          :page-sizes="[20, 50, 100]"
          layout="total, sizes, prev, pager, next, jumper"
          background
          @size-change="loadData"
          @current-change="loadData"
        />
      </div>
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, computed, onMounted } from 'vue'
import { getShopRental } from '@/api/rpt/asset'
import { getProjectList } from '@/api/base/project'
import { getBuildingList } from '@/api/base/building'
import { getFloorPage } from '@/api/base/floor'
import type { ShopRentalVO, AssetQueryParam } from '@/api/rpt/asset'

const loading = ref(false)
const tableData = ref<ShopRentalVO[]>([])
const projectList = ref<{ id: number; projectName: string; projectCode: string }[]>([])
const buildingList = ref<{ id: number; buildingName: string }[]>([])
const floorList = ref<{ id: number; floorName: string }[]>([])

const filterForm = reactive<AssetQueryParam>({
  projectId: null,
  buildingId: null,
  floorId: null,
  formatType: '',
  statDate: undefined,
})

const pagination = reactive({ pageNum: 1, pageSize: 20, total: 0 })

// 汇总卡片（当前页聚合）
const summaryCards = computed(() => {
  const total = tableData.value.reduce((acc, r) => acc + (r.totalShops ?? 0), 0)
  const rented = tableData.value.reduce((acc, r) => acc + (r.rentedShops ?? 0), 0)
  const vacant = tableData.value.reduce((acc, r) => acc + (r.vacantShops ?? 0), 0)
  const deco = tableData.value.reduce((acc, r) => acc + (r.decoratingShops ?? 0), 0)
  const opened = tableData.value.reduce((acc, r) => acc + (r.openedShops ?? 0), 0)
  const totalArea = tableData.value.reduce((acc, r) => acc + (r.totalArea ?? 0), 0)
  return [
    { label: '商铺总数', value: total + ' 间', color: '#303133' },
    { label: '已租', value: rented + ' 间', color: '#409eff' },
    { label: '空置', value: vacant + ' 间', color: '#f56c6c' },
    { label: '装修中', value: deco + ' 间', color: '#e6a23c' },
    { label: '已开业', value: opened + ' 间', color: '#67c23a' },
    { label: '总面积', value: totalArea.toFixed(0) + ' ㎡', color: '#909399' },
  ]
})

onMounted(async () => {
  projectList.value = await getProjectList()
  await loadData()
})

async function onProjectChange(val: number | null) {
  filterForm.buildingId = null
  filterForm.floorId = null
  buildingList.value = []
  floorList.value = []
  if (val) {
    buildingList.value = await getBuildingList(val)
  }
}

async function onBuildingChange(val: number | null) {
  filterForm.floorId = null
  floorList.value = []
  if (val) {
    const result = await getFloorPage({ buildingId: val, pageSize: 100 })
    floorList.value = result.records.map(f => ({ id: f.id, floorName: f.floorName }))
  }
}

function handleSearch() {
  pagination.pageNum = 1
  loadData()
}

function resetFilter() {
  filterForm.projectId = null
  filterForm.buildingId = null
  filterForm.floorId = null
  filterForm.formatType = ''
  filterForm.statDate = undefined
  buildingList.value = []
  floorList.value = []
  pagination.pageNum = 1
  loadData()
}

async function loadData() {
  loading.value = true
  try {
    const params: AssetQueryParam = {
      projectId: filterForm.projectId,
      buildingId: filterForm.buildingId,
      floorId: filterForm.floorId,
      formatType: filterForm.formatType || undefined,
      statDate: filterForm.statDate,
      pageNum: pagination.pageNum,
      pageSize: pagination.pageSize,
    }
    const result = await getShopRental(params)
    tableData.value = result.records
    pagination.total = result.total
  } finally {
    loading.value = false
  }
}

function fmtNum(v?: number | null) {
  return v != null ? v.toFixed(2) : '-'
}
</script>

<style scoped lang="scss">
.rpt-shop-rental {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.summary-row {
  margin: 0 !important;
}

.summary-card {
  text-align: center;
  .s-label { font-size: 12px; color: #909399; margin-bottom: 4px; }
  .s-value { font-size: 20px; font-weight: 700; }
}

.total-tip {
  margin-left: 8px;
  font-size: 12px;
  color: #909399;
}

.pagination-wrap {
  margin-top: 16px;
  display: flex;
  justify-content: flex-end;
}

.text-primary { color: #409eff; }
.text-danger { color: #f56c6c; }
.text-warning { color: #e6a23c; }
.text-success { color: #67c23a; }
</style>
