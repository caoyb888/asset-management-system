<template>
  <div class="flow-designer">
    <!-- 工具栏 -->
    <div class="designer-toolbar">
      <span class="toolbar-label">节点配置</span>
      <div class="toolbar-actions">
        <el-button size="small" type="primary" plain @click="addNode('APPROVER')">
          <el-icon><Plus /></el-icon> 添加审批节点
        </el-button>
        <el-button size="small" type="warning" plain @click="addNode('CONDITION')">
          <el-icon><Filter /></el-icon> 添加条件节点
        </el-button>
        <el-button
          size="small"
          type="danger"
          plain
          :disabled="!canDelete"
          @click="deleteSelected"
        >
          <el-icon><Delete /></el-icon> 删除选中
        </el-button>
      </div>
    </div>

    <!-- 流程画布 -->
    <div class="designer-canvas" @click.self="selectedId = ''">
      <div class="node-row">
        <template v-for="(node, idx) in sortedNodes" :key="node.nodeId">
          <!-- 节点卡片 -->
          <div
            class="node-card"
            :class="[`node-type-${node.nodeType.toLowerCase()}`, { selected: selectedId === node.nodeId }]"
            @click.stop="selectedId = node.nodeId"
          >
            <!-- 卡片头部：图标 + 节点名 -->
            <div class="node-card-header">
              <el-icon class="node-icon"><component :is="nodeIcon(node.nodeType)" /></el-icon>
              <input
                v-model="node.nodeName"
                class="node-name-input"
                :readonly="node.nodeType === 'START' || node.nodeType === 'END'"
                @change="emit('update:modelValue', exportNodes())"
                @click.stop
              />
            </div>

            <!-- 卡片体：节点类型专属配置 -->
            <div class="node-card-body">
              <!-- START -->
              <template v-if="node.nodeType === 'START'">
                <div class="node-hint">流程发起</div>
              </template>

              <!-- END -->
              <template v-else-if="node.nodeType === 'END'">
                <div class="node-hint">流程结束</div>
              </template>

              <!-- APPROVER -->
              <template v-else-if="node.nodeType === 'APPROVER'">
                <!-- 审批策略 -->
                <el-select
                  v-model="node.approverStrategy"
                  size="small"
                  placeholder="审批策略"
                  style="width: 100%"
                  @change="onStrategyChange(node); emit('update:modelValue', exportNodes())"
                  @click.stop
                >
                  <el-option
                    v-for="opt in APPROVER_STRATEGY_OPTIONS"
                    :key="opt.value"
                    :label="opt.label"
                    :value="opt.value"
                  />
                </el-select>

                <!-- ROLE → 角色下拉（从系统加载） -->
                <el-select
                  v-if="node.approverStrategy === 'ROLE'"
                  v-model="node.roleCode"
                  size="small"
                  placeholder="选择角色"
                  filterable
                  style="width: 100%; margin-top: 4px"
                  @change="emit('update:modelValue', exportNodes())"
                  @click.stop
                  @visible-change="(v: boolean) => v && loadRoles()"
                >
                  <el-option
                    v-for="r in roleOptions"
                    :key="r.roleCode"
                    :label="r.roleName"
                    :value="r.roleCode"
                  >
                    <span>{{ r.roleName }}</span>
                    <span style="color: var(--el-text-color-placeholder); font-size: 11px; margin-left: 6px">{{ r.roleCode }}</span>
                  </el-option>
                </el-select>

                <!-- SPECIFIC_USER → 用户远程搜索 -->
                <el-select
                  v-if="node.approverStrategy === 'SPECIFIC_USER'"
                  v-model="node.userId"
                  size="small"
                  placeholder="搜索用户姓名"
                  filterable
                  remote
                  :remote-method="(q: string) => searchUsers(q, node)"
                  :loading="userSearchLoading"
                  style="width: 100%; margin-top: 4px"
                  @change="onUserSelect(node); emit('update:modelValue', exportNodes())"
                  @click.stop
                >
                  <el-option
                    v-for="u in userOptions"
                    :key="u.id"
                    :label="`${u.realName}（${u.username}）`"
                    :value="u.id"
                  />
                </el-select>

                <!-- 超时小时 -->
                <el-input-number
                  v-model="node.timeoutHours"
                  size="small"
                  :min="0"
                  :max="720"
                  :step="8"
                  placeholder="超时(h)"
                  controls-position="right"
                  style="width: 100%; margin-top: 4px"
                  @change="emit('update:modelValue', exportNodes())"
                  @click.stop
                />
              </template>

              <!-- CONDITION -->
              <template v-else-if="node.nodeType === 'CONDITION'">
                <el-select
                  v-model="node.conditionType"
                  size="small"
                  placeholder="条件类型"
                  style="width: 100%"
                  @change="emit('update:modelValue', exportNodes())"
                  @click.stop
                >
                  <el-option value="AMOUNT" label="金额条件" />
                  <el-option value="CUSTOM" label="自定义 EL 表达式" />
                </el-select>

                <!-- AMOUNT: 运算符 + 阈值 -->
                <template v-if="node.conditionType === 'AMOUNT'">
                  <el-select
                    v-model="node.conditionOp"
                    size="small"
                    placeholder="运算符"
                    style="width: 100%; margin-top: 4px"
                    @change="emit('update:modelValue', exportNodes())"
                    @click.stop
                  >
                    <el-option
                      v-for="op in CONDITION_OP_OPTIONS"
                      :key="op.value"
                      :label="op.label"
                      :value="op.value"
                    />
                  </el-select>
                  <el-input-number
                    v-model="node.conditionValue"
                    size="small"
                    :min="0"
                    :precision="2"
                    placeholder="金额阈值（元）"
                    controls-position="right"
                    style="width: 100%; margin-top: 4px"
                    @change="emit('update:modelValue', exportNodes())"
                    @click.stop
                  />
                </template>

                <!-- CUSTOM: EL 表达式 -->
                <el-input
                  v-else-if="node.conditionType === 'CUSTOM'"
                  v-model="node.conditionExpr"
                  size="small"
                  type="textarea"
                  :rows="2"
                  placeholder="${amount >= 100000}"
                  style="margin-top: 4px"
                  @change="emit('update:modelValue', exportNodes())"
                  @click.stop
                />
              </template>
            </div>

            <!-- 序号徽章 -->
            <div class="node-order-badge">{{ nodeOrderLabel(node) }}</div>
          </div>

          <!-- 箭头连接线 -->
          <div v-if="idx < sortedNodes.length - 1" class="node-arrow">
            <el-icon><ArrowRight /></el-icon>
          </div>
        </template>
      </div>
    </div>

    <!-- 空状态提示 -->
    <div v-if="coreNodes.length === 0" class="empty-hint">
      点击"添加审批节点"开始配置审批流程
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, watch } from 'vue'
import {
  Plus, Filter, Delete, ArrowRight,
  UserFilled, SetUp, CircleCheckFilled, Edit,
} from '@element-plus/icons-vue'
import {
  type NodeConfigDTO,
  APPROVER_STRATEGY_OPTIONS,
  CONDITION_OP_OPTIONS,
} from '@/api/workflow/definition'
import { roleApi, type SysRole } from '@/api/sys/role'
import { userApi, type UserDetailVO } from '@/api/sys/user'

// ─── Props & Emits ────────────────────────────────────────

const props = defineProps<{ modelValue: NodeConfigDTO[] }>()
const emit = defineEmits<{ (e: 'update:modelValue', nodes: NodeConfigDTO[]): void }>()

// ─── 内部状态 ──────────────────────────────────────────────

const nodes = ref<NodeConfigDTO[]>([])
const selectedId = ref('')

const sortedNodes = computed(() =>
  [...nodes.value].sort((a, b) => a.nodeOrder - b.nodeOrder),
)
const coreNodes = computed(() =>
  nodes.value.filter(n => n.nodeType !== 'START' && n.nodeType !== 'END'),
)
const canDelete = computed(() => {
  const node = nodes.value.find(n => n.nodeId === selectedId.value)
  return !!node && node.nodeType !== 'START' && node.nodeType !== 'END'
})

// ─── 角色 & 用户数据源 ─────────────────────────────────────

const roleOptions = ref<SysRole[]>([])
const rolesLoaded = ref(false)
const userOptions = ref<UserDetailVO[]>([])
const userSearchLoading = ref(false)

async function loadRoles() {
  if (rolesLoaded.value) return
  try {
    const res = await roleApi.dropdown() as any
    roleOptions.value = (Array.isArray(res) ? res : (res?.records ?? [])).filter(
      (r: SysRole) => r.status === 1,
    )
    rolesLoaded.value = true
  } catch {
    // 加载失败不阻断用户操作
  }
}

async function searchUsers(keyword: string, _node: NodeConfigDTO) {
  if (!keyword.trim()) return
  userSearchLoading.value = true
  try {
    const res = await userApi.page({ realName: keyword, pageSize: 20 }) as any
    userOptions.value = res?.records ?? []
  } catch {
    userOptions.value = []
  } finally {
    userSearchLoading.value = false
  }
}

function onUserSelect(node: NodeConfigDTO) {
  const found = userOptions.value.find(u => u.id === node.userId)
  node.userDisplayName = found ? `${found.realName}（${found.username}）` : String(node.userId)
}

// ─── 初始化 & 同步 ─────────────────────────────────────────

function initFromProps(vals: NodeConfigDTO[]) {
  if (vals.length === 0) {
    nodes.value = [makeStart(), makeEnd()]
  } else {
    nodes.value = vals.map(n => ({ ...n }))
    ensureStartEnd()
  }
}

watch(
  () => props.modelValue,
  (vals) => {
    if (JSON.stringify(vals) !== JSON.stringify(exportNodes())) {
      initFromProps(vals)
    }
  },
  { immediate: true, deep: true },
)

// ─── 节点操作 ──────────────────────────────────────────────

function addNode(type: 'APPROVER' | 'CONDITION') {
  const id = `node_${Date.now()}`
  const insertAfterOrder = insertPosition()
  nodes.value
    .filter(n => n.nodeType !== 'START' && n.nodeType !== 'END' && n.nodeOrder > insertAfterOrder)
    .forEach(n => n.nodeOrder++)

  const newNode: NodeConfigDTO = {
    nodeId: id,
    nodeType: type,
    nodeName: type === 'APPROVER' ? '审批节点' : '条件节点',
    nodeOrder: insertAfterOrder + 1,
    approverStrategy: type === 'APPROVER' ? 'DEPT_LEADER' : undefined,
    conditionType: type === 'CONDITION' ? 'AMOUNT' : undefined,
    conditionOp: type === 'CONDITION' ? 'GTE' : undefined,
  }
  nodes.value.push(newNode)
  selectedId.value = id
  emit('update:modelValue', exportNodes())
}

function deleteSelected() {
  if (!canDelete.value) return
  nodes.value = nodes.value.filter(n => n.nodeId !== selectedId.value)
  reorderCore()
  selectedId.value = ''
  emit('update:modelValue', exportNodes())
}

function insertPosition(): number {
  if (!selectedId.value) {
    return Math.max(0, ...coreNodes.value.map(n => n.nodeOrder))
  }
  const sel = nodes.value.find(n => n.nodeId === selectedId.value)
  if (!sel || sel.nodeType === 'END') {
    return Math.max(0, ...coreNodes.value.map(n => n.nodeOrder))
  }
  return sel.nodeOrder
}

function reorderCore() {
  const core = nodes.value
    .filter(n => n.nodeType !== 'START' && n.nodeType !== 'END')
    .sort((a, b) => a.nodeOrder - b.nodeOrder)
  core.forEach((n, i) => (n.nodeOrder = i + 1))
}

function ensureStartEnd() {
  if (!nodes.value.find(n => n.nodeType === 'START')) nodes.value.unshift(makeStart())
  if (!nodes.value.find(n => n.nodeType === 'END')) nodes.value.push(makeEnd())
}

function makeStart(): NodeConfigDTO {
  return { nodeId: 'start', nodeType: 'START', nodeName: '发起申请', nodeOrder: 0 }
}
function makeEnd(): NodeConfigDTO {
  return { nodeId: 'end', nodeType: 'END', nodeName: '审批完成', nodeOrder: 99 }
}

// ─── 导出（去除仅前端用字段） ──────────────────────────────

function exportNodes(): NodeConfigDTO[] {
  return nodes.value.map(n => {
    const out: NodeConfigDTO = {
      nodeId: n.nodeId,
      nodeType: n.nodeType,
      nodeName: n.nodeName,
      nodeOrder: n.nodeOrder,
    }
    if (n.approverStrategy) out.approverStrategy = n.approverStrategy
    if (n.roleCode) out.roleCode = n.roleCode
    if (n.userId) out.userId = n.userId
    if (n.timeoutHours) out.timeoutHours = n.timeoutHours
    if (n.conditionType) out.conditionType = n.conditionType
    if (n.conditionOp) out.conditionOp = n.conditionOp
    if (n.conditionValue != null) out.conditionValue = n.conditionValue
    if (n.conditionExpr) out.conditionExpr = n.conditionExpr
    if (n.remark) out.remark = n.remark
    return out
  })
}

// ─── 策略切换清空无关字段 ─────────────────────────────────

function onStrategyChange(node: NodeConfigDTO) {
  node.roleCode = undefined
  node.userId = undefined
  node.userDisplayName = undefined
}

// ─── 展示辅助 ──────────────────────────────────────────────

function nodeIcon(type: string) {
  switch (type) {
    case 'START': return CircleCheckFilled
    case 'END':   return CircleCheckFilled
    case 'APPROVER': return UserFilled
    case 'CONDITION': return SetUp
    default: return Edit
  }
}
function nodeOrderLabel(node: NodeConfigDTO): string {
  if (node.nodeType === 'START') return 'S'
  if (node.nodeType === 'END') return 'E'
  return String(node.nodeOrder)
}
</script>

<style scoped>
.flow-designer {
  border: 1px solid var(--el-border-color);
  border-radius: 8px;
  overflow: hidden;
  background: var(--el-fill-color-blank);
}

/* ── 工具栏 ── */
.designer-toolbar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 8px 12px;
  background: var(--el-fill-color-light);
  border-bottom: 1px solid var(--el-border-color);
}
.toolbar-label {
  font-size: 13px;
  color: var(--el-text-color-secondary);
  font-weight: 500;
}
.toolbar-actions { display: flex; gap: 8px; }

/* ── 画布 ── */
.designer-canvas {
  padding: 24px 16px;
  overflow-x: auto;
  min-height: 180px;
}
.node-row {
  display: flex;
  align-items: center;
  width: max-content;
  min-width: 100%;
}

/* ── 箭头 ── */
.node-arrow {
  display: flex;
  align-items: center;
  color: var(--el-text-color-placeholder);
  font-size: 20px;
  padding: 0 4px;
  flex-shrink: 0;
}

/* ── 节点卡片通用 ── */
.node-card {
  position: relative;
  width: 172px;
  flex-shrink: 0;
  border-radius: 8px;
  border: 2px solid transparent;
  cursor: pointer;
  transition: box-shadow 0.2s, border-color 0.2s;
  background: #fff;
  box-shadow: 0 1px 4px rgba(0, 0, 0, 0.08);
  user-select: none;
}
.node-card:hover { box-shadow: 0 2px 8px rgba(0, 0, 0, 0.14); }

/* 颜色主题 */
.node-type-start   { border-color: #67c23a; }
.node-type-end     { border-color: #909399; }
.node-type-approver { border-color: #409eff; }
.node-type-condition { border-color: #e6a23c; }

.node-type-start.selected   { box-shadow: 0 0 0 3px rgba(103,194,58,0.25); }
.node-type-end.selected     { box-shadow: 0 0 0 3px rgba(144,147,153,0.25); }
.node-type-approver.selected { box-shadow: 0 0 0 3px rgba(64,158,255,0.3); }
.node-type-condition.selected { box-shadow: 0 0 0 3px rgba(230,162,60,0.3); }

/* ── 卡片头部 ── */
.node-card-header {
  display: flex;
  align-items: center;
  gap: 6px;
  padding: 8px 10px 6px;
  border-bottom: 1px solid var(--el-border-color-lighter);
}
.node-type-start .node-card-header     { background: rgba(103,194,58,0.08); }
.node-type-end .node-card-header       { background: rgba(144,147,153,0.08); }
.node-type-approver .node-card-header  { background: rgba(64,158,255,0.08); }
.node-type-condition .node-card-header { background: rgba(230,162,60,0.08); }

.node-icon { flex-shrink: 0; font-size: 14px; }
.node-type-start .node-icon     { color: #67c23a; }
.node-type-end .node-icon       { color: #909399; }
.node-type-approver .node-icon  { color: #409eff; }
.node-type-condition .node-icon { color: #e6a23c; }

.node-name-input {
  flex: 1;
  min-width: 0;
  border: none;
  outline: none;
  font-size: 12px;
  font-weight: 600;
  color: var(--el-text-color-primary);
  background: transparent;
  padding: 0;
  cursor: pointer;
}
.node-name-input:not([readonly]):focus {
  cursor: text;
  border-bottom: 1px solid var(--el-color-primary);
}
.node-name-input[readonly] { cursor: default; }

/* ── 卡片体 ── */
.node-card-body {
  padding: 8px 10px;
  font-size: 12px;
  min-height: 60px;
}
.node-hint {
  color: var(--el-text-color-placeholder);
  font-size: 11px;
  text-align: center;
  margin-top: 4px;
}

/* ── 序号徽章 ── */
.node-order-badge {
  position: absolute;
  top: -8px;
  right: -8px;
  width: 18px;
  height: 18px;
  border-radius: 50%;
  background: var(--el-color-primary);
  color: #fff;
  font-size: 10px;
  font-weight: 700;
  display: flex;
  align-items: center;
  justify-content: center;
}
.node-type-start .node-order-badge    { background: #67c23a; }
.node-type-end .node-order-badge      { background: #909399; }
.node-type-condition .node-order-badge { background: #e6a23c; }

/* ── 空状态 ── */
.empty-hint {
  text-align: center;
  color: var(--el-text-color-placeholder);
  font-size: 12px;
  padding: 0 0 16px;
}
</style>
