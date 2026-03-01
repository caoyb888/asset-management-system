<template>
  <div class="page-container">
    <div class="page-header">
      <div class="page-header-left">
        <h2 class="page-title">机构管理</h2>
        <p class="page-desc">管理公司组织架构，支持多级部门树</p>
      </div>
      <el-button type="primary" :icon="Plus" @click="openCreate(null)">新增部门</el-button>
    </div>

    <el-card shadow="never">
      <el-table
        :data="treeData"
        v-loading="loading"
        row-key="id"
        :tree-props="{ children: 'children' }"
        border
        default-expand-all
      >
        <el-table-column prop="deptName" label="部门名称" min-width="200" />
        <el-table-column prop="deptCode" label="部门编码" width="140" />
        <el-table-column prop="leader"   label="负责人"  width="120" />
        <el-table-column prop="phone"    label="联系电话" width="140" />
        <el-table-column prop="sortOrder" label="排序" width="80" align="center" />
        <el-table-column prop="status" label="状态" width="90" align="center">
          <template #default="{ row }">
            <el-tag :type="row.status === 1 ? 'success' : 'danger'" size="small">
              {{ row.status === 1 ? '正常' : '停用' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="200" fixed="right" align="center">
          <template #default="{ row }">
            <el-button type="primary" link size="small" @click="openEdit(row)">编辑</el-button>
            <el-button type="success" link size="small" @click="openCreate(row)">新增子部门</el-button>
            <el-button type="danger" link size="small" @click="doDelete(row.id)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-card>

    <!-- 新增/编辑弹窗 -->
    <el-dialog v-model="dialogVisible" :title="dialogTitle" width="500px" destroy-on-close>
      <el-form ref="formRef" :model="form" :rules="formRules" label-width="90px">
        <el-form-item label="上级部门">
          <el-tree-select
            v-model="form.parentId"
            :data="treeSelectData"
            node-key="id"
            :props="{ label: 'deptName', children: 'children' }"
            placeholder="请选择上级部门（不选则为顶级）"
            clearable
            style="width:100%"
          />
        </el-form-item>
        <el-form-item label="部门名称" prop="deptName">
          <el-input v-model="form.deptName" placeholder="请输入部门名称" />
        </el-form-item>
        <el-form-item label="部门编码">
          <el-input v-model="form.deptCode" placeholder="请输入部门编码" />
        </el-form-item>
        <el-form-item label="负责人">
          <el-input v-model="form.leader" placeholder="请输入负责人姓名" />
        </el-form-item>
        <el-form-item label="联系电话">
          <el-input v-model="form.phone" placeholder="请输入联系电话" />
        </el-form-item>
        <el-form-item label="排序">
          <el-input-number v-model="form.sortOrder" :min="0" :max="999" />
        </el-form-item>
        <el-form-item label="状态">
          <el-radio-group v-model="form.status">
            <el-radio :value="1">正常</el-radio>
            <el-radio :value="0">停用</el-radio>
          </el-radio-group>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="submitting" @click="doSubmit">确定</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Plus } from '@element-plus/icons-vue'
import { deptApi, type DeptTreeVO, type DeptCreateDTO } from '@/api/sys/dept'

const loading = ref(false)
const submitting = ref(false)
const treeData = ref<DeptTreeVO[]>([])
const treeSelectData = ref<DeptTreeVO[]>([])

const dialogVisible = ref(false)
const dialogTitle = ref('')
const form = reactive<DeptCreateDTO>({ deptName: '', status: 1 })
const formRef = ref()
const formRules = { deptName: [{ required: true, message: '请输入部门名称', trigger: 'blur' }] }

async function loadData() {
  loading.value = true
  try {
    const res: any = await deptApi.tree()
    treeData.value = res ?? []
    treeSelectData.value = res ?? []
  } finally {
    loading.value = false
  }
}

function openCreate(parent: DeptTreeVO | null) {
  Object.assign(form, { id: undefined, parentId: parent?.id, deptName: '', deptCode: '', leader: '', phone: '', sortOrder: 0, status: 1 })
  dialogTitle.value = parent ? `新增【${parent.deptName}】的子部门` : '新增部门'
  dialogVisible.value = true
}

function openEdit(row: DeptTreeVO) {
  Object.assign(form, { ...row })
  dialogTitle.value = '编辑部门'
  dialogVisible.value = true
}

async function doSubmit() {
  await formRef.value?.validate()
  submitting.value = true
  try {
    if (form.id) {
      await deptApi.update(form.id, form)
    } else {
      await deptApi.create(form)
    }
    ElMessage.success('操作成功')
    dialogVisible.value = false
    loadData()
  } finally {
    submitting.value = false
  }
}

async function doDelete(id: number) {
  await ElMessageBox.confirm('确认删除该部门？存在子部门或用户时不允许删除', '警告', { type: 'warning' })
  await deptApi.delete(id)
  ElMessage.success('删除成功')
  loadData()
}

onMounted(loadData)
</script>
