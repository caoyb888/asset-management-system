<template>
  <div class="page-container">
    <div class="page-header">
      <div class="page-header-left">
        <h2 class="page-title">菜单管理</h2>
        <p class="page-desc">管理系统菜单结构、路由和按钮级权限</p>
      </div>
      <el-button type="primary" :icon="Plus" @click="openCreate(null)">新增菜单</el-button>
    </div>

    <!-- 搜索栏 -->
    <el-card class="search-card" shadow="never">
      <el-form inline>
        <el-form-item label="菜单名称">
          <el-input v-model="filterKeyword" placeholder="菜单名称关键字" clearable style="width:180px"
            @input="onFilter" @clear="onFilter" />
        </el-form-item>
        <el-form-item label="类型">
          <el-select v-model="filterType" placeholder="全部" clearable style="width:100px" @change="loadData">
            <el-option label="目录" value="M" />
            <el-option label="菜单" value="C" />
            <el-option label="按钮" value="F" />
          </el-select>
        </el-form-item>
        <el-form-item label="状态">
          <el-select v-model="filterStatus" placeholder="全部" clearable style="width:100px" @change="loadData">
            <el-option label="正常" :value="1" />
            <el-option label="停用" :value="0" />
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-button :icon="Refresh" @click="resetFilter">重置</el-button>
          <el-button link @click="toggleExpand(true)">全部展开</el-button>
          <el-button link @click="toggleExpand(false)">全部折叠</el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <!-- 树形表格 -->
    <el-card class="table-card" shadow="never">
      <el-table
        ref="tableRef"
        :data="filteredData"
        v-loading="loading"
        row-key="id"
        :tree-props="{ children: 'children' }"
        border
        default-expand-all
      >
        <el-table-column prop="menuName" label="菜单名称" min-width="200">
          <template #default="{ row }">
            <el-icon v-if="row.icon" style="vertical-align:middle; margin-right:4px">
              <component :is="row.icon" />
            </el-icon>
            <span>{{ row.menuName }}</span>
          </template>
        </el-table-column>
        <el-table-column prop="menuType" label="类型" width="80" align="center">
          <template #default="{ row }">
            <el-tag size="small" :type="typeTagMap[row.menuType]?.type as any">
              {{ typeTagMap[row.menuType]?.label }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="icon" label="图标" width="90" align="center">
          <template #default="{ row }">
            <span v-if="row.icon" style="font-size:12px; color:#606266">{{ row.icon }}</span>
            <span v-else style="color:#c0c4cc">-</span>
          </template>
        </el-table-column>
        <el-table-column prop="path" label="路由地址" min-width="160" show-overflow-tooltip />
        <el-table-column prop="component" label="组件路径" min-width="160" show-overflow-tooltip />
        <el-table-column prop="perms" label="权限标识" min-width="160" show-overflow-tooltip />
        <el-table-column prop="sortOrder" label="排序" width="70" align="center" />
        <el-table-column label="显示" width="80" align="center">
          <template #default="{ row }">
            <el-switch
              v-if="row.menuType !== 'F'"
              v-model="row.visible"
              :active-value="1" :inactive-value="0"
              size="small"
              @change="(v: string | number | boolean) => doChangeVisible(row, Number(v))"
            />
            <span v-else style="color:#c0c4cc">-</span>
          </template>
        </el-table-column>
        <el-table-column label="状态" width="80" align="center">
          <template #default="{ row }">
            <el-switch
              v-model="row.status"
              :active-value="1" :inactive-value="0"
              size="small"
              @change="(v: string | number | boolean) => doChangeStatus(row, Number(v))"
            />
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

    <!-- 新增/编辑弹窗 -->
    <el-dialog v-model="dialogVisible" :title="dialogTitle" width="580px" destroy-on-close>
      <el-form ref="formRef" :model="form" :rules="formRules" label-width="90px">
        <el-form-item label="上级菜单">
          <el-tree-select
            v-model="form.parentId"
            :data="parentSelectData"
            node-key="id"
            :props="{ label: 'menuName', children: 'children' }"
            :filter-node-method="(val: string, data: any) => !val || data.menuName?.includes(val)"
            placeholder="不选则为顶级菜单"
            clearable filterable style="width:100%"
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
          <el-input v-model="form.icon" placeholder="Element Plus 图标组件名称，如 House" />
        </el-form-item>
        <el-form-item v-if="form.menuType !== 'F'" label="路由地址">
          <el-input v-model="form.path" placeholder="/sys/users" />
        </el-form-item>
        <el-form-item v-if="form.menuType === 'C'" label="组件路径">
          <el-input v-model="form.component" placeholder="sys/user/index（不含 views/ 和 .vue）" />
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
        <el-form-item label="备注">
          <el-input v-model="form.remark" type="textarea" :rows="2" placeholder="备注说明" />
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
import { ref, reactive, computed, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Plus, Refresh } from '@element-plus/icons-vue'
import { menuApi, type MenuTreeVO, type MenuCreateDTO } from '@/api/sys/menu'

const loading = ref(false)
const submitting = ref(false)
const tableRef = ref()
const allData = ref<MenuTreeVO[]>([])

// ─── 过滤条件 ───────────────────────────────────────────────────────────────
const filterKeyword = ref('')
const filterType = ref<string | undefined>(undefined)
const filterStatus = ref<number | undefined>(undefined)

const typeTagMap: Record<string, { label: string; type: string }> = {
  M: { label: '目录', type: '' },
  C: { label: '菜单', type: 'primary' },
  F: { label: '按钮', type: 'warning' },
}

/** 递归过滤树节点 */
function filterTree(nodes: MenuTreeVO[]): MenuTreeVO[] {
  const result: MenuTreeVO[] = []
  for (const node of nodes) {
    const nameMatch = !filterKeyword.value || node.menuName.includes(filterKeyword.value)
    const typeMatch = !filterType.value || node.menuType === filterType.value
    const statusMatch = filterStatus.value === undefined || node.status === filterStatus.value
    const children = filterTree(node.children ?? [])
    if ((nameMatch && typeMatch && statusMatch) || children.length > 0) {
      result.push({ ...node, children: children.length > 0 ? children : (node.children?.length ? node.children : undefined) })
    }
  }
  return result
}

const filteredData = computed(() => {
  if (!filterKeyword.value && filterType.value === undefined && filterStatus.value === undefined) return allData.value
  return filterTree(allData.value)
})

/** 父级选择器只包含目录和菜单（不包含按钮） */
function filterNonButton(nodes: MenuTreeVO[]): MenuTreeVO[] {
  return nodes
    .filter(n => n.menuType !== 'F')
    .map(n => ({ ...n, children: n.children ? filterNonButton(n.children) : undefined }))
}
const parentSelectData = computed(() => filterNonButton(allData.value))

// ─── 弹窗表单 ───────────────────────────────────────────────────────────────
const dialogVisible = ref(false)
const dialogTitle = ref('')
const form = reactive<MenuCreateDTO>({ menuName: '', menuType: 'C', sortOrder: 0, visible: 1, status: 1 })
const formRef = ref()
const formRules = {
  menuName: [{ required: true, message: '请输入菜单名称', trigger: 'blur' }],
  menuType: [{ required: true, message: '请选择菜单类型', trigger: 'change' }],
}

async function loadData() {
  loading.value = true
  try {
    const res: any = await menuApi.tree()
    allData.value = res ?? []
  } finally {
    loading.value = false
  }
}

function onFilter() {
  // filteredData 是 computed，自动响应
}

function resetFilter() {
  filterKeyword.value = ''
  filterType.value = undefined
  filterStatus.value = undefined
}

function toggleExpand(expand: boolean) {
  const table = tableRef.value
  if (!table) return
  const setExpand = (nodes: MenuTreeVO[]) => {
    nodes.forEach(node => {
      table.toggleRowExpansion(node, expand)
      if (node.children?.length) setExpand(node.children)
    })
  }
  setExpand(allData.value)
}

function openCreate(parent: MenuTreeVO | null) {
  Object.assign(form, {
    id: undefined, parentId: parent?.id ?? undefined,
    menuName: '', menuType: 'C', icon: '', path: '',
    component: '', perms: '', sortOrder: 0, visible: 1, status: 1, remark: '',
  })
  dialogTitle.value = parent ? `新增子菜单（${parent.menuName}）` : '新增顶级菜单'
  dialogVisible.value = true
}

function openEdit(row: MenuTreeVO) {
  Object.assign(form, {
    id: row.id, parentId: row.parentId === 0 ? undefined : row.parentId,
    menuName: row.menuName, menuType: row.menuType, icon: row.icon ?? '',
    path: row.path ?? '', component: row.component ?? '',
    perms: row.perms ?? '', sortOrder: row.sortOrder ?? 0,
    visible: row.visible ?? 1, status: row.status ?? 1, remark: row.remark ?? '',
  })
  dialogTitle.value = '编辑菜单'
  dialogVisible.value = true
}

async function doSubmit() {
  await formRef.value?.validate()
  submitting.value = true
  try {
    const payload = { ...form, parentId: form.parentId ?? 0 }
    form.id ? await menuApi.update(form.id, payload) : await menuApi.create(payload)
    ElMessage.success('操作成功')
    dialogVisible.value = false
    loadData()
  } finally {
    submitting.value = false
  }
}

async function doChangeStatus(row: MenuTreeVO, status: number) {
  try {
    await menuApi.changeStatus(row.id, status)
    ElMessage.success(status === 1 ? '已启用' : '已停用')
  } catch {
    row.status = status === 1 ? 0 : 1  // 回滚
  }
}

async function doChangeVisible(row: MenuTreeVO, visible: number) {
  try {
    await menuApi.changeVisible(row.id, visible)
    ElMessage.success(visible === 1 ? '已设为显示' : '已设为隐藏')
  } catch {
    row.visible = visible === 1 ? 0 : 1  // 回滚
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
