<template>
  <div class="floor-page">
    <!-- 搜索栏 -->
    <el-card shadow="never" class="search-card">
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
            style="width: 160px"
            @change="handleSearch"
          >
            <el-option
              v-for="b in buildingOptions"
              :key="b.id"
              :label="b.buildingName"
              :value="b.id"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="楼层名称">
          <el-input
            v-model="query.floorName"
            placeholder="请输入楼层名称"
            clearable
            style="width: 140px"
          />
        </el-form-item>
        <el-form-item label="状态">
          <el-select v-model="query.status" placeholder="全部" clearable style="width: 100px">
            <el-option label="启用" :value="1" />
            <el-option label="停用" :value="0" />
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
        <el-button type="primary" :icon="Plus" @click="handleAdd">新增楼层</el-button>
      </div>

      <el-table v-loading="loading" :data="tableData" border stripe row-key="id" style="width: 100%">
        <el-table-column type="index" label="序号" width="60" align="center" />
        <el-table-column prop="buildingName" label="所属楼栋" min-width="140" show-overflow-tooltip />
        <el-table-column prop="floorCode" label="楼层编码" width="100" align="center" />
        <el-table-column prop="floorName" label="楼层名称" min-width="120" show-overflow-tooltip />
        <el-table-column label="状态" width="80" align="center">
          <template #default="{ row }">
            <el-tag :type="row.status === 1 ? 'success' : 'info'" size="small">
              {{ row.statusName }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="建筑面积(㎡)" width="120" align="right">
          <template #default="{ row }">
            {{ row.buildingArea != null ? Number(row.buildingArea).toLocaleString() : '-' }}
          </template>
        </el-table-column>
        <el-table-column label="经营面积(㎡)" width="120" align="right">
          <template #default="{ row }">
            {{ row.operatingArea != null ? Number(row.operatingArea).toLocaleString() : '-' }}
          </template>
        </el-table-column>
        <el-table-column prop="remark" label="备注" min-width="120" show-overflow-tooltip />
        <el-table-column prop="createdAt" label="创建时间" width="160" align="center" />
        <el-table-column label="操作" width="150" align="center" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" size="small" @click="handleEdit(row)">编辑</el-button>
            <el-divider direction="vertical" />
            <el-popconfirm
              title="确认删除该楼层？"
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
    </el-card>

    <!-- 新增/编辑 Dialog -->
    <el-dialog
      v-model="dialogVisible"
      :title="dialogTitle"
      width="620px"
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
        <el-row :gutter="20">
          <el-col :span="24">
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
          <el-col :span="24">
            <el-form-item label="所属楼栋" prop="buildingId">
              <el-select
                v-model="form.buildingId"
                placeholder="请先选择项目"
                filterable
                style="width: 100%"
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
            <el-form-item label="楼层编码">
              <el-input v-model="form.floorCode" placeholder="如 F1 B1" maxlength="50" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="楼层名称" prop="floorName">
              <el-input v-model="form.floorName" placeholder="如 一层" maxlength="100" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="状态">
              <el-select v-model="form.status" placeholder="请选择" style="width: 100%">
                <el-option label="启用" :value="1" />
                <el-option label="停用" :value="0" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="建筑面积(㎡)">
              <el-input-number
                v-model="form.buildingArea"
                :min="0"
                :precision="2"
                :step="50"
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
                :step="50"
                style="width: 100%"
                controls-position="right"
              />
            </el-form-item>
          </el-col>
          <el-col :span="24">
            <el-form-item label="备注">
              <el-input v-model="form.remark" type="textarea" :rows="2" maxlength="500" />
            </el-form-item>
          </el-col>
          <el-col :span="24">
            <el-form-item label="平面图">
              <div class="image-upload-row">
                <el-image
                  v-if="form.imageUrl"
                  :src="form.imageUrl"
                  fit="cover"
                  style="width:180px;height:130px;border-radius:4px;border:1px solid #e4e7ed"
                  :preview-src-list="[form.imageUrl]"
                />
                <div class="image-upload-actions">
                  <el-button size="small" :icon="Upload" :loading="imageUploading" @click="floorImageInputRef?.click()">
                    {{ form.imageUrl ? '更换图片' : '上传平面图' }}
                  </el-button>
                  <el-button v-if="form.imageUrl" size="small" type="danger" plain @click="form.imageUrl = ''">清除</el-button>
                </div>
                <input ref="floorImageInputRef" type="file" accept="image/*" style="display:none" @change="handleFloorImageChange" />
              </div>
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
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { ElMessage, type FormInstance, type FormRules } from 'element-plus'
import { Search, Refresh, Plus, Upload } from '@element-plus/icons-vue'
import {
  getFloorPage,
  createFloor,
  updateFloor,
  deleteFloor,
  type FloorVO,
  type FloorQuery,
  type FloorSaveDTO,
} from '@/api/base/floor'
import { getProjectPage, type ProjectVO } from '@/api/base/project'
import { getBuildingPage, type BuildingVO } from '@/api/base/building'
import { uploadFile } from '@/api/file'
import { useAppStore } from '@/store/modules/app'

useAppStore().setPageTitle('楼层管理')

// ─────────── 联动下拉选项 ───────────
const projectOptions = ref<ProjectVO[]>([])
const buildingOptions = ref<BuildingVO[]>([]) // 搜索栏用
const formBuildingOptions = ref<BuildingVO[]>([]) // 表单内用

async function loadProjects() {
  const res = await getProjectPage({ pageNum: 1, pageSize: 200 })
  projectOptions.value = res.records
}

async function loadBuildings(projectId: number, target: 'query' | 'form') {
  const res = await getBuildingPage({ pageNum: 1, pageSize: 500, projectId })
  if (target === 'query') buildingOptions.value = res.records
  else formBuildingOptions.value = res.records
}

async function onProjectChange(val: number | null) {
  buildingOptions.value = []
  query.buildingId = null
  if (val) await loadBuildings(val, 'query')
  handleSearch()
}

async function onFormProjectChange(val: number | null) {
  formBuildingOptions.value = []
  form.buildingId = null
  if (val) await loadBuildings(val, 'form')
}

// ─────────── 列表 ───────────
const loading = ref(false)
const tableData = ref<FloorVO[]>([])
const total = ref(0)

const query = reactive<FloorQuery & { pageNum: number; pageSize: number }>({
  pageNum: 1,
  pageSize: 20,
  projectId: null,
  buildingId: null,
  floorName: '',
  floorCode: '',
  status: '',
})

async function fetchList() {
  loading.value = true
  try {
    const res = await getFloorPage({
      ...query,
      status: query.status === '' ? undefined : query.status,
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
  query.floorName = ''
  query.floorCode = ''
  query.status = ''
  query.pageNum = 1
  buildingOptions.value = []
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

const defaultForm = (): FloorSaveDTO => ({
  id: undefined,
  projectId: null,
  buildingId: null,
  floorCode: '',
  floorName: '',
  status: 1,
  buildingArea: null,
  operatingArea: null,
  remark: '',
  imageUrl: '',
})

const form = reactive<FloorSaveDTO>(defaultForm())
const dialogTitle = ref('新增楼层')

const formRules: FormRules = {
  projectId:  [{ required: true, message: '所属项目不能为空', trigger: 'change' }],
  buildingId: [{ required: true, message: '所属楼栋不能为空', trigger: 'change' }],
  floorName:  [{ required: true, message: '楼层名称不能为空', trigger: 'blur' }],
}

function handleAdd() {
  isEdit.value = false
  dialogTitle.value = '新增楼层'
  formBuildingOptions.value = []
  Object.assign(form, defaultForm())
  dialogVisible.value = true
}

async function handleEdit(row: FloorVO) {
  isEdit.value = true
  dialogTitle.value = '编辑楼层'
  Object.assign(form, {
    id: row.id,
    projectId: row.projectId,
    buildingId: row.buildingId,
    floorCode: row.floorCode,
    floorName: row.floorName,
    status: row.status,
    buildingArea: row.buildingArea,
    operatingArea: row.operatingArea,
    remark: row.remark,
    imageUrl: row.imageUrl,
  })
  // 加载对应项目的楼栋列表
  if (row.projectId) await loadBuildings(row.projectId, 'form')
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
      await updateFloor(form.id, form)
      ElMessage.success('修改成功')
    } else {
      await createFloor(form)
      ElMessage.success('新增成功')
    }
    dialogVisible.value = false
    fetchList()
  } finally {
    submitting.value = false
  }
}

// ─────────── 图片上传 ───────────
const imageUploading = ref(false)
const floorImageInputRef = ref<HTMLInputElement>()

async function handleFloorImageChange(e: Event) {
  const file = (e.target as HTMLInputElement).files?.[0]
  if (!file) return
  imageUploading.value = true
  try {
    form.imageUrl = await uploadFile(file)
    ElMessage.success('图片上传成功')
  } catch {
    ElMessage.error('图片上传失败')
  } finally {
    imageUploading.value = false
    if (floorImageInputRef.value) floorImageInputRef.value.value = ''
  }
}

// ─────────── 删除 ───────────
async function handleDelete(id: number) {
  try {
    await deleteFloor(id)
    ElMessage.success('删除成功')
    if (tableData.value.length === 1 && query.pageNum > 1) query.pageNum--
    fetchList()
  } catch {
    // 错误已由 Axios 拦截器统一处理
  }
}
</script>

<style scoped lang="scss">
.floor-page {
  display: flex;
  flex-direction: column;
  gap: 12px;

  .search-card {
    :deep(.el-form-item) {
      margin-bottom: 0;
    }
  }

  .toolbar {
    margin-bottom: 12px;
  }

  .pagination {
    margin-top: 16px;
    display: flex;
    justify-content: flex-end;
  }
}

.image-upload-row {
  display: flex;
  align-items: flex-start;
  gap: 12px;
}

.image-upload-actions {
  display: flex;
  flex-direction: column;
  gap: 8px;
}
</style>
