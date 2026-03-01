<template>
  <div class="page-container">
    <div class="page-header">
      <div class="page-header-left">
        <h2 class="page-title">菜单管理</h2>
        <p class="page-desc">管理系统菜单结构和按钮权限</p>
      </div>
      <el-button type="primary" :icon="Plus" @click="openCreate(null)">新增菜单</el-button>
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
        <el-table-column prop="menuName" label="菜单名称" min-width="200" />
        <el-table-column prop="menuType" label="类型" width="80" align="center">
          <template #default="{ row }">
            <el-tag size="small" :type="typeTagMap[row.menuType]?.type">{{ typeTagMap[row.menuType]?.label }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="icon"      label="图标"  width="100" />
        <el-table-column prop="path"      label="路由地址" show-overflow-tooltip />
        <el-table-column prop="perms"     label="权限标识" show-overflow-tooltip />
        <el-table-column prop="sortOrder" label="排序" width="70" align="center" />
        <el-table-column prop="visible" label="显示" width="80" align="center">
          <template #default="{ row }">
            <el-tag :type="row.visible === 1 ? 'success' : 'info'" size="small">
              {{ row.visible === 1 ? '显示' : '隐藏' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="status" label="状态" width="80" align="center">
          <template #default="{ row }">
            <el-tag :type="row.status === 1 ? 'success' : 'danger'" size="small">
              {{ row.status === 1 ? '正常' : '停用' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="200" fixed="right" align="center">
          <template #default="{ row }">
            <el-button type="primary" link size="small" @click="openEdit(row)">编辑</el-button>
            <el-button v-if="row.menuType !== 'F'" type="success" link size="small" @click="openCreate(row)">新增子项</el-button>
            <el-button type="danger" link size="small" @click="doDelete(row.id)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-card>

    <el-dialog v-model="dialogVisible" :title="dialogTitle" width="560px" destroy-on-close>
      <el-form ref="formRef" :model="form" :rules="formRules" label-width="90px">
        <el-form-item label="上级菜单">
          <el-tree-select
            v-model="form.parentId"
            :data="treeSelectData"
            node-key="id"
            :props="{ label: 'menuName', children: 'children' }"
            placeholder="请选择上级菜单（不选则为顶级）"
            clearable style="width:100%"
            :filter-node-method="filterNode"
            filterable
          />
        </el-form-item>
        <el-form-item label="菜单类型" prop="menuType">
          <el-radio-group v-model="form.menuType">
            <el-radio value="M">目录</el-radio>
            <el-radio value="C">菜单</el-radio>
            <el-radio value="F">按钮</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="菜单名称" prop="menuName">
          <el-input v-model="form.menuName" placeholder="请输入菜单名称" />
        </el-form-item>
        <el-form-item v-if="form.menuType !== 'F'" label="图标">
          <el-input v-model="form.icon" placeholder="Element Plus 图标名称" />
        </el-form-item>
        <el-form-item v-if="form.menuType !== 'F'" label="路由地址">
          <el-input v-model="form.path" placeholder="/sys/users" />
        </el-form-item>
        <el-form-item v-if="form.menuType === 'C'" label="组件路径">
          <el-input v-model="form.component" placeholder="sys/user/index" />
        </el-form-item>
        <el-form-item label="权限标识">
          <el-input v-model="form.perms" placeholder="sys:user:list" />
        </el-form-item>
        <el-form-item label="排序">
          <el-input-number v-model="form.sortOrder" :min="0" :max="999" />
        </el-form-item>
        <el-form-item v-if="form.menuType !== 'F'" label="是否显示">
          <el-radio-group v-model="form.visible">
            <el-radio :value="1">显示</el-radio>
            <el-radio :value="0">隐藏</el-radio>
          </el-radio-group>
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
import { menuApi, type MenuTreeVO, type MenuCreateDTO } from '@/api/sys/menu'

const loading = ref(false)
const submitting = ref(false)
const treeData = ref<MenuTreeVO[]>([])
const treeSelectData = ref<MenuTreeVO[]>([])

const typeTagMap: Record<string, { label: string; type: '' | 'primary' | 'success' | 'warning' | 'danger' | 'info' }> = {
  M: { label: '目录', type: '' },
  C: { label: '菜单', type: 'primary' },
  F: { label: '按钮', type: 'warning' },
}

const dialogVisible = ref(false)
const dialogTitle = ref('')
const form = reactive<MenuCreateDTO>({ menuName: '', menuType: 'C', sortOrder: 0, visible: 1, status: 1 })
const formRef = ref()
const formRules = {
  menuName: [{ required: true, message: '请输入菜单名称', trigger: 'blur' }],
  menuType: [{ required: true, message: '请选择菜单类型', trigger: 'change' }],
}

function filterNode(value: string, data: MenuTreeVO) {
  return data.menuName.includes(value)
}

async function loadData() {
  loading.value = true
  try {
    const res: any = await menuApi.tree()
    treeData.value = res ?? []
    treeSelectData.value = res ?? []
  } finally {
    loading.value = false
  }
}

function openCreate(parent: MenuTreeVO | null) {
  Object.assign(form, { id: undefined, parentId: parent?.id, menuName: '', menuType: 'C', icon: '', path: '', component: '', perms: '', sortOrder: 0, visible: 1, status: 1 })
  dialogTitle.value = parent ? `新增子菜单` : '新增菜单'
  dialogVisible.value = true
}

function openEdit(row: MenuTreeVO) {
  Object.assign(form, { ...row })
  dialogTitle.value = '编辑菜单'
  dialogVisible.value = true
}

async function doSubmit() {
  await formRef.value?.validate()
  submitting.value = true
  try {
    form.id ? await menuApi.update(form.id, form) : await menuApi.create(form)
    ElMessage.success('操作成功')
    dialogVisible.value = false
    loadData()
  } finally {
    submitting.value = false
  }
}

async function doDelete(id: number) {
  await ElMessageBox.confirm('确认删除该菜单？存在子菜单时不允许删除', '警告', { type: 'warning' })
  await menuApi.delete(id)
  ElMessage.success('删除成功')
  loadData()
}

onMounted(loadData)
</script>
