<template>
  <div class="shop-page">
    <!-- 搜索栏 -->
    <el-card shadow="never" class="filter-card">
      <el-form :model="query" inline @keyup.enter="handleSearch">
        <el-form-item label="所属项目">
          <el-select
            v-model="query.projectId"
            placeholder="请选择项目"
            clearable
            filterable
            style="width: 180px"
            @change="onProjectChange"
          >
            <el-option
              v-for="p in projectOptions"
              :key="p.id"
              :label="p.projectName"
              :value="p.id"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="所属楼栋">
          <el-select
            v-model="query.buildingId"
            placeholder="请选择楼栋"
            clearable
            filterable
            style="width: 150px"
            @change="onBuildingChange"
          >
            <el-option
              v-for="b in buildingOptions"
              :key="b.id"
              :label="b.buildingName"
              :value="b.id"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="所在楼层">
          <el-select
            v-model="query.floorId"
            placeholder="请选择楼层"
            clearable
            filterable
            style="width: 130px"
            @change="handleSearch"
          >
            <el-option
              v-for="f in floorOptions"
              :key="f.id"
              :label="f.floorName"
              :value="f.id"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="铺位号">
          <el-input
            v-model="query.shopCode"
            placeholder="铺位号"
            clearable
            style="width: 120px"
          />
        </el-form-item>
        <el-form-item label="商铺状态">
          <el-select v-model="query.shopStatus" placeholder="全部" clearable style="width: 100px">
            <el-option
              v-for="item in SHOP_STATUS_OPTIONS"
              :key="item.value"
              :label="item.label"
              :value="item.value"
            />
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
          <span class="header-title">商铺列表</span>
          <span class="count-tag">共 {{ total }} 条</span>
        </div>
        <div class="header-actions">
          <el-button type="primary" :icon="Plus" @click="handleAdd">新增商铺</el-button>
          <el-button type="success" plain @click="openMergeDialog">合并商铺</el-button>
          <el-button :icon="Upload" @click="importDialogVisible = true">批量导入</el-button>
          <el-button :icon="Download" @click="handleDownloadTemplate">下载模板</el-button>
        </div>
      </div>

      <div class="table-body">
      <el-table v-loading="loading" :data="tableData" border stripe row-key="id" style="width: 100%">
        <el-table-column type="index" label="序号" width="60" align="center" />
        <el-table-column prop="shopCode" label="铺位号" width="100" show-overflow-tooltip />
        <el-table-column prop="projectName" label="所属项目" min-width="140" show-overflow-tooltip />
        <el-table-column prop="buildingName" label="楼栋" width="100" show-overflow-tooltip />
        <el-table-column prop="floorName" label="楼层" width="80" align="center" />
        <el-table-column label="商铺类型" width="90" align="center">
          <template #default="{ row }">
            <el-tag type="info" size="small">{{ row.shopTypeName || '-' }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="商铺状态" width="90" align="center">
          <template #default="{ row }">
            <el-tag :type="shopStatusTagType(row.shopStatus)" size="small">
              {{ row.shopStatusName }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="计租面积(㎡)" width="120" align="right">
          <template #default="{ row }">
            {{ row.rentArea != null ? Number(row.rentArea).toLocaleString() : '-' }}
          </template>
        </el-table-column>
        <el-table-column label="建筑面积(㎡)" width="120" align="right">
          <template #default="{ row }">
            {{ row.buildingArea != null ? Number(row.buildingArea).toLocaleString() : '-' }}
          </template>
        </el-table-column>
        <el-table-column prop="signedFormat" label="签约业态" width="120" show-overflow-tooltip />
        <el-table-column prop="ownerName" label="业主名称" width="110" show-overflow-tooltip />
        <el-table-column prop="createdAt" label="创建时间" width="160" align="center" />
        <el-table-column label="操作" width="220" align="center" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" size="small" @click="handleEdit(row)">编辑</el-button>
            <el-divider direction="vertical" />
            <el-button link type="warning" size="small" @click="openSplitDialog(row)">拆分</el-button>
            <el-divider direction="vertical" />
            <el-popconfirm
              title="确认删除该商铺？"
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
        <!-- 位置信息 -->
        <el-divider content-position="left">位置信息</el-divider>
        <el-row :gutter="20">
          <el-col :span="12">
            <el-form-item label="所属项目" prop="projectId">
              <el-select
                v-model="form.projectId"
                placeholder="请选择项目"
                filterable
                style="width: 100%"
                @change="onFormProjectChange"
              >
                <el-option
                  v-for="p in projectOptions"
                  :key="p.id"
                  :label="p.projectName"
                  :value="p.id"
                />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="所属楼栋" prop="buildingId">
              <el-select
                v-model="form.buildingId"
                placeholder="请先选择项目"
                filterable
                style="width: 100%"
                @change="onFormBuildingChange"
              >
                <el-option
                  v-for="b in formBuildingOptions"
                  :key="b.id"
                  :label="b.buildingName"
                  :value="b.id"
                />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="所在楼层" prop="floorId">
              <el-select
                v-model="form.floorId"
                placeholder="请先选择楼栋"
                filterable
                style="width: 100%"
              >
                <el-option
                  v-for="f in formFloorOptions"
                  :key="f.id"
                  :label="f.floorName"
                  :value="f.id"
                />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="铺位号" prop="shopCode">
              <el-input v-model="form.shopCode" placeholder="请输入铺位号" maxlength="50" />
            </el-form-item>
          </el-col>
        </el-row>

        <!-- 基本属性 -->
        <el-divider content-position="left">基本属性</el-divider>
        <el-row :gutter="20">
          <el-col :span="12">
            <el-form-item label="商铺类型">
              <el-select v-model="form.shopType" placeholder="请选择" clearable style="width: 100%">
                <el-option label="临街" :value="1" />
                <el-option label="内铺" :value="2" />
                <el-option label="专柜" :value="3" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="商铺状态">
              <el-select v-model="form.shopStatus" placeholder="请选择" style="width: 100%">
                <el-option
                  v-for="item in SHOP_STATUS_OPTIONS"
                  :key="item.value"
                  :label="item.label"
                  :value="item.value"
                />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="计租面积(㎡)">
              <el-input-number
                v-model="form.rentArea"
                :min="0"
                :precision="2"
                style="width: 100%"
                controls-position="right"
              />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="实测面积(㎡)">
              <el-input-number
                v-model="form.measuredArea"
                :min="0"
                :precision="2"
                style="width: 100%"
                controls-position="right"
              />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="建筑面积(㎡)">
              <el-input-number
                v-model="form.buildingArea"
                :min="0"
                :precision="2"
                style="width: 100%"
                controls-position="right"
              />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="经营面积(㎡)">
              <el-input-number
                v-model="form.operatingArea"
                :min="0"
                :precision="2"
                style="width: 100%"
                controls-position="right"
              />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="规划业态">
              <el-input v-model="form.plannedFormat" placeholder="规划业态" maxlength="100" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="签约业态">
              <el-input v-model="form.signedFormat" placeholder="签约业态" maxlength="100" />
            </el-form-item>
          </el-col>
        </el-row>

        <!-- 计率设置 -->
        <el-divider content-position="left">计率设置</el-divider>
        <el-row :gutter="20">
          <el-col :span="8">
            <el-form-item label="计入招商率">
              <el-switch
                v-model="form.countLeasingRate"
                :active-value="1"
                :inactive-value="0"
              />
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="计入出租率">
              <el-switch
                v-model="form.countRentalRate"
                :active-value="1"
                :inactive-value="0"
              />
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="计入开业率">
              <el-switch
                v-model="form.countOpeningRate"
                :active-value="1"
                :inactive-value="0"
              />
            </el-form-item>
          </el-col>
        </el-row>

        <!-- 业主信息 -->
        <el-divider content-position="left">业主信息</el-divider>
        <el-row :gutter="20">
          <el-col :span="8">
            <el-form-item label="业主名称">
              <el-input v-model="form.ownerName" placeholder="业主名称" maxlength="100" />
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="业主联系人">
              <el-input v-model="form.ownerContact" placeholder="联系人" maxlength="50" />
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="业主电话">
              <el-input v-model="form.ownerPhone" placeholder="电话" maxlength="30" />
            </el-form-item>
          </el-col>
        </el-row>
      </el-form>

      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="submitting" @click="handleSubmit">
          {{ isEdit ? '保存修改' : '确认新增' }}
        </el-button>
      </template>
    </el-dialog>
    <!-- 拆分商铺 Dialog -->
    <el-dialog v-model="splitDialogVisible" title="拆分商铺" width="600px" :close-on-click-modal="false" destroy-on-close>
      <div class="split-source" style="margin-bottom: 12px">
        <el-descriptions :column="3" border size="small">
          <el-descriptions-item label="铺位号">{{ splitSourceShop?.shopCode }}</el-descriptions-item>
          <el-descriptions-item label="计租面积(㎡)">{{ splitSourceShop?.rentArea ?? '-' }}</el-descriptions-item>
          <el-descriptions-item label="签约业态">{{ splitSourceShop?.signedFormat || '-' }}</el-descriptions-item>
        </el-descriptions>
      </div>
      <div class="split-sub-list">
        <div v-for="(sub, idx) in splitSubShops" :key="idx" class="split-sub-item">
          <el-card shadow="never" size="small" style="margin-bottom: 8px">
            <div style="display:flex;align-items:center;justify-content:space-between;margin-bottom:8px">
              <span style="font-weight:500">子商铺 {{ idx + 1 }}</span>
              <el-button v-if="splitSubShops.length > 2" link type="danger" size="small" @click="removeSplitSub(idx)">删除</el-button>
            </div>
            <el-row :gutter="12">
              <el-col :span="8">
                <el-form-item label="铺位号" :required="true">
                  <el-input v-model="sub.shopCode" placeholder="铺位号" size="small" />
                </el-form-item>
              </el-col>
              <el-col :span="8">
                <el-form-item label="计租面积">
                  <el-input-number v-model="sub.rentArea" :min="0" :precision="2" size="small" style="width:100%" controls-position="right" />
                </el-form-item>
              </el-col>
              <el-col :span="8">
                <el-form-item label="业态">
                  <el-input v-model="sub.plannedFormat" placeholder="业态" size="small" />
                </el-form-item>
              </el-col>
            </el-row>
          </el-card>
        </div>
      </div>
      <el-button style="width:100%;margin-bottom:8px" @click="addSplitSub">+ 添加子商铺</el-button>
      <template #footer>
        <el-button @click="splitDialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="splitSaving" @click="handleSplit">确认拆分</el-button>
      </template>
    </el-dialog>

    <!-- 批量导入 Dialog -->
    <el-dialog
      v-model="importDialogVisible"
      title="批量导入商铺"
      width="560px"
      :close-on-click-modal="false"
      destroy-on-close
      @close="resetImport"
    >
      <el-alert type="info" :closable="false" style="margin-bottom: 12px">
        请先下载模板，按模板格式填写后上传。项目编码、楼栋编码、楼层编码必须与系统中一致。
      </el-alert>
      <el-upload
        ref="importUploadRef"
        drag
        :auto-upload="false"
        accept=".xlsx,.xls"
        :limit="1"
        :on-change="onImportFileChange"
        :on-remove="() => (importFile = null)"
        style="width: 100%"
      >
        <el-icon style="font-size: 48px; color: #c0c4cc"><Upload /></el-icon>
        <div style="margin-top: 8px">将文件拖到此处，或 <em>点击上传</em></div>
        <template #tip>
          <div style="font-size: 12px; color: #909399; margin-top: 4px">
            仅支持 .xlsx / .xls 格式
          </div>
        </template>
      </el-upload>

      <div v-if="importResult" class="import-result">
        <el-alert
          :type="importResult.failCount === 0 ? 'success' : 'warning'"
          :title="`导入完成：成功 ${importResult.successCount} 条，失败 ${importResult.failCount} 条`"
          :closable="false"
        />
        <el-table
          v-if="importResult.errors.length"
          :data="importResult.errors.map((e, i) => ({ index: i, msg: e }))"
          size="small"
          max-height="200"
          style="margin-top: 8px"
        >
          <el-table-column type="index" width="50" />
          <el-table-column prop="msg" label="错误信息" show-overflow-tooltip />
        </el-table>
      </div>

      <template #footer>
        <el-button @click="importDialogVisible = false">关闭</el-button>
        <el-button type="primary" :loading="importing" :disabled="!importFile" @click="handleImport">
          开始导入
        </el-button>
      </template>
    </el-dialog>

    <!-- 合并商铺 Dialog -->
    <el-dialog v-model="mergeDialogVisible" title="合并商铺" width="520px" :close-on-click-modal="false" destroy-on-close>
      <el-form ref="mergeFormRef" :model="mergeForm" :rules="mergeFormRules" label-width="100px">
        <el-form-item label="选择商铺" prop="shopIds">
          <el-select v-model="mergeForm.shopIds" multiple placeholder="请选择要合并的商铺（至少2个）" style="width:100%" filterable>
            <el-option v-for="shop in tableData" :key="shop.id" :label="`${shop.shopCode}(${shop.buildingName}/${shop.floorName})`" :value="shop.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="合并后铺位号" prop="targetShopCode">
          <el-input v-model="mergeForm.targetShopCode" placeholder="合并后的铺位号" maxlength="50" />
        </el-form-item>
        <el-form-item label="合并后业态">
          <el-input v-model="mergeForm.targetFormat" placeholder="合并后的业态" maxlength="100" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="mergeDialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="mergeSaving" @click="handleMerge">确认合并</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { ElMessage, type FormInstance, type FormRules } from 'element-plus'
import { Search, Refresh, Plus, Upload, Download } from '@element-plus/icons-vue'
import {
  getShopPage,
  createShop,
  updateShop,
  deleteShop,
  splitShop,
  mergeShop,
  importShops,
  downloadShopTemplate,
  type ShopVO,
  type ShopQuery,
  type ShopSaveDTO,
} from '@/api/base/shop'
import { getProjectPage, type ProjectVO } from '@/api/base/project'
import { getBuildingPage, type BuildingVO } from '@/api/base/building'
import { getFloorPage, type FloorVO } from '@/api/base/floor'
import { useAppStore } from '@/store/modules/app'

useAppStore().setPageTitle('商铺管理')

// ─────────── 枚举常量 ───────────
const SHOP_STATUS_OPTIONS = [
  { label: '空置', value: 0 },
  { label: '在租', value: 1 },
  { label: '自用', value: 2 },
  { label: '预留', value: 3 },
]

function shopStatusTagType(status: number) {
  return (({ 0: 'info', 1: 'success', 2: 'warning', 3: 'danger' } as Record<number, string>)[status] ?? 'info') as 'success' | 'warning' | 'info' | 'danger'
}

// ─────────── 联动下拉选项 ───────────
const projectOptions = ref<ProjectVO[]>([])
const buildingOptions = ref<BuildingVO[]>([])
const floorOptions = ref<FloorVO[]>([])
const formBuildingOptions = ref<BuildingVO[]>([])
const formFloorOptions = ref<FloorVO[]>([])

async function loadProjects() {
  const res = await getProjectPage({ pageNum: 1, pageSize: 200 })
  projectOptions.value = res.records
}

async function loadBuildings(projectId: number, target: 'query' | 'form') {
  const res = await getBuildingPage({ pageNum: 1, pageSize: 500, projectId })
  if (target === 'query') buildingOptions.value = res.records
  else formBuildingOptions.value = res.records
}

async function loadFloors(buildingId: number, target: 'query' | 'form') {
  const res = await getFloorPage({ pageNum: 1, pageSize: 500, buildingId })
  if (target === 'query') floorOptions.value = res.records
  else formFloorOptions.value = res.records
}

async function onProjectChange(val: number | null) {
  buildingOptions.value = []
  floorOptions.value = []
  query.buildingId = null
  query.floorId = null
  if (val) await loadBuildings(val, 'query')
  handleSearch()
}

async function onBuildingChange(val: number | null) {
  floorOptions.value = []
  query.floorId = null
  if (val) await loadFloors(val, 'query')
  handleSearch()
}

async function onFormProjectChange(val: number | null) {
  formBuildingOptions.value = []
  formFloorOptions.value = []
  form.buildingId = null
  form.floorId = null
  if (val) await loadBuildings(val, 'form')
}

async function onFormBuildingChange(val: number | null) {
  formFloorOptions.value = []
  form.floorId = null
  if (val) await loadFloors(val, 'form')
}

// ─────────── 列表 ───────────
const loading = ref(false)
const tableData = ref<ShopVO[]>([])
const total = ref(0)

const query = reactive<ShopQuery & { pageNum: number; pageSize: number }>({
  pageNum: 1,
  pageSize: 20,
  projectId: null,
  buildingId: null,
  floorId: null,
  shopCode: '',
  shopStatus: '',
  shopType: '',
  signedFormat: '',
})

async function fetchList() {
  loading.value = true
  try {
    const res = await getShopPage({
      ...query,
      shopStatus: query.shopStatus === '' ? undefined : query.shopStatus,
      shopType: query.shopType === '' ? undefined : query.shopType,
    })
    tableData.value = res.records
    total.value = res.total
  } finally {
    loading.value = false
  }
}

function handleSearch() {
  query.pageNum = 1
  fetchList()
}

function handleReset() {
  query.projectId = null
  query.buildingId = null
  query.floorId = null
  query.shopCode = ''
  query.shopStatus = ''
  query.shopType = ''
  query.signedFormat = ''
  query.pageNum = 1
  buildingOptions.value = []
  floorOptions.value = []
  fetchList()
}

onMounted(() => {
  loadProjects()
  fetchList()
})

// ─────────── 新增/编辑 Dialog ───────────
const dialogVisible = ref(false)
const isEdit = ref(false)
const submitting = ref(false)
const formRef = ref<FormInstance>()

const defaultForm = (): ShopSaveDTO => ({
  id: undefined,
  projectId: null,
  buildingId: null,
  floorId: null,
  shopCode: '',
  shopType: null,
  rentArea: null,
  measuredArea: null,
  buildingArea: null,
  operatingArea: null,
  shopStatus: 0,
  countLeasingRate: 1,
  countRentalRate: 1,
  countOpeningRate: 1,
  signedFormat: '',
  plannedFormat: '',
  ownerName: '',
  ownerContact: '',
  ownerPhone: '',
})

const form = reactive<ShopSaveDTO>(defaultForm())
const dialogTitle = ref('新增商铺')

const formRules: FormRules = {
  projectId:  [{ required: true, message: '所属项目不能为空', trigger: 'change' }],
  buildingId: [{ required: true, message: '所属楼栋不能为空', trigger: 'change' }],
  floorId:    [{ required: true, message: '所在楼层不能为空', trigger: 'change' }],
  shopCode:   [{ required: true, message: '铺位号不能为空', trigger: 'blur' }],
}

function handleAdd() {
  isEdit.value = false
  dialogTitle.value = '新增商铺'
  formBuildingOptions.value = []
  formFloorOptions.value = []
  Object.assign(form, defaultForm())
  dialogVisible.value = true
}

async function handleEdit(row: ShopVO) {
  isEdit.value = true
  dialogTitle.value = '编辑商铺'
  Object.assign(form, {
    id: row.id,
    projectId: row.projectId,
    buildingId: row.buildingId,
    floorId: row.floorId,
    shopCode: row.shopCode,
    shopType: row.shopType,
    rentArea: row.rentArea,
    measuredArea: row.measuredArea,
    buildingArea: row.buildingArea,
    operatingArea: row.operatingArea,
    shopStatus: row.shopStatus,
    countLeasingRate: row.countLeasingRate,
    countRentalRate: row.countRentalRate,
    countOpeningRate: row.countOpeningRate,
    signedFormat: row.signedFormat,
    plannedFormat: row.plannedFormat,
    ownerName: row.ownerName,
    ownerContact: row.ownerContact,
    ownerPhone: row.ownerPhone,
  })
  // 加载级联选项
  if (row.projectId) {
    await loadBuildings(row.projectId, 'form')
  }
  if (row.buildingId) {
    await loadFloors(row.buildingId, 'form')
  }
  dialogVisible.value = true
}

function resetForm() {
  formRef.value?.clearValidate()
  Object.assign(form, defaultForm())
}

async function handleSubmit() {
  const valid = await formRef.value?.validate().catch(() => false)
  if (!valid) return

  submitting.value = true
  try {
    if (isEdit.value && form.id) {
      await updateShop(form.id, form)
      ElMessage.success('修改成功')
    } else {
      await createShop(form)
      ElMessage.success('新增成功')
    }
    dialogVisible.value = false
    fetchList()
  } finally {
    submitting.value = false
  }
}

// ─────────── 删除 ───────────
async function handleDelete(id: number) {
  try {
    await deleteShop(id)
    ElMessage.success('删除成功')
    if (tableData.value.length === 1 && query.pageNum > 1) query.pageNum--
    fetchList()
  } catch {
    // 错误已由 Axios 拦截器统一处理
  }
}

// ─────────── 拆分商铺 ───────────
const splitDialogVisible = ref(false)
const splitSaving = ref(false)
const splitSourceShop = ref<ShopVO | null>(null)

interface SplitSubShop {
  shopCode: string
  rentArea: number | null
  plannedFormat: string
}

const splitSubShops = ref<SplitSubShop[]>([])

function openSplitDialog(row: ShopVO) {
  splitSourceShop.value = row
  splitSubShops.value = [
    { shopCode: '', rentArea: null, plannedFormat: '' },
    { shopCode: '', rentArea: null, plannedFormat: '' },
  ]
  splitDialogVisible.value = true
}

function addSplitSub() {
  splitSubShops.value.push({ shopCode: '', rentArea: null, plannedFormat: '' })
}

function removeSplitSub(idx: number) {
  splitSubShops.value.splice(idx, 1)
}

async function handleSplit() {
  if (!splitSourceShop.value) return
  if (splitSubShops.value.length < 2) {
    ElMessage.warning('至少需要拆分成2个子商铺')
    return
  }
  if (splitSubShops.value.some(s => !s.shopCode.trim())) {
    ElMessage.warning('子商铺铺位号不能为空')
    return
  }
  splitSaving.value = true
  try {
    await splitShop({
      sourceShopId: splitSourceShop.value.id,
      subShops: splitSubShops.value.map(s => ({
        shopCode: s.shopCode,
        rentArea: s.rentArea,
        plannedFormat: s.plannedFormat || undefined,
      })),
    })
    ElMessage.success('拆分成功')
    splitDialogVisible.value = false
    fetchList()
  } finally { splitSaving.value = false }
}

// ─────────── Excel 批量导入 ───────────
const importDialogVisible = ref(false)
const importing = ref(false)
const importFile = ref<File | null>(null)
const importResult = ref<{ successCount: number; failCount: number; errors: string[] } | null>(null)
const importUploadRef = ref()

function onImportFileChange(uploadFile: any) {
  importFile.value = uploadFile.raw ?? null
}

function resetImport() {
  importFile.value = null
  importResult.value = null
  importUploadRef.value?.clearFiles()
}

async function handleImport() {
  if (!importFile.value) return
  importing.value = true
  try {
    const res = await importShops(importFile.value)
    importResult.value = res
    if (res.failCount === 0) {
      ElMessage.success(`导入成功，共 ${res.successCount} 条`)
    } else {
      ElMessage.warning(`导入完成，成功 ${res.successCount} 条，失败 ${res.failCount} 条`)
    }
    fetchList()
  } finally {
    importing.value = false
  }
}

async function handleDownloadTemplate() {
  try {
    const blob = await downloadShopTemplate() as unknown as Blob
    const url = URL.createObjectURL(blob)
    const a = document.createElement('a')
    a.href = url
    a.download = 'shop_import_template.xlsx'
    a.click()
    URL.revokeObjectURL(url)
  } catch {
    ElMessage.error('模板下载失败')
  }
}

// ─────────── 合并商铺 ───────────
const mergeDialogVisible = ref(false)
const mergeSaving = ref(false)
const mergeFormRef = ref<FormInstance>()
const mergeForm = reactive({ shopIds: [] as number[], targetShopCode: '', targetFormat: '' })
const mergeFormRules: FormRules = {
  shopIds:         [{ required: true, type: 'array', min: 2, message: '至少选择2个商铺', trigger: 'change' }],
  targetShopCode:  [{ required: true, message: '合并后铺位号不能为空', trigger: 'blur' }],
}

function openMergeDialog() {
  Object.assign(mergeForm, { shopIds: [], targetShopCode: '', targetFormat: '' })
  mergeDialogVisible.value = true
}

async function handleMerge() {
  const valid = await mergeFormRef.value?.validate().catch(() => false)
  if (!valid) return
  mergeSaving.value = true
  try {
    await mergeShop({
      shopIds: mergeForm.shopIds,
      targetShopCode: mergeForm.targetShopCode,
      targetFormat: mergeForm.targetFormat || undefined,
    })
    ElMessage.success('合并成功')
    mergeDialogVisible.value = false
    fetchList()
  } finally { mergeSaving.value = false }
}
</script>

<style scoped lang="scss">
.shop-page {
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

.import-result {
  margin-top: 12px;
}
</style>
