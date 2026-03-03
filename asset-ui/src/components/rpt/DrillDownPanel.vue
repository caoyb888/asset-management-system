<template>
  <div class="drill-panel">
    <!-- 面包屑导航 -->
    <div class="drill-header">
      <el-breadcrumb separator="/" class="drill-breadcrumb">
        <!-- 顶层入口 -->
        <el-breadcrumb-item class="crumb-link" @click="resetToTop">
          {{ topLabel }}
        </el-breadcrumb-item>
        <!-- 历史层级 -->
        <el-breadcrumb-item
          v-for="(crumb, idx) in breadcrumbs"
          :key="idx"
          :class="idx < breadcrumbs.length - 1 ? 'crumb-link' : 'crumb-current'"
          @click="idx < breadcrumbs.length - 1 && backTo(idx)"
        >
          {{ crumb.label }}
        </el-breadcrumb-item>
      </el-breadcrumb>

      <!-- 层级标签 -->
      <el-tag v-if="currentResult" type="info" size="small" class="level-tag">
        {{ currentResult.levelName }}
        <span v-if="currentResult.canDrillDown" class="level-hint">
          &nbsp;→ 点击可下钻至{{ currentResult.nextLevelName }}
        </span>
      </el-tag>
    </div>

    <!-- 顶层内容（由父组件提供 slot） -->
    <template v-if="breadcrumbs.length === 0">
      <slot name="top-level" :on-drill="handleTopDrill" />
    </template>

    <!-- 钻取层内容 -->
    <template v-else>
      <div v-loading="loading" class="drill-table-wrap">
        <el-table
          v-if="currentResult"
          :data="currentResult.rows"
          border
          stripe
          size="small"
          max-height="540"
          @row-click="handleRowClick"
          :row-class-name="rowClass"
        >
          <template v-for="col in currentResult.columns" :key="col.prop">
            <el-table-column
              :label="col.label"
              :prop="col.prop"
              :width="col.width || undefined"
              :align="col.align || 'left'"
            >
              <template v-if="col.drillable" #default="{ row }">
                <el-button
                  link
                  type="primary"
                  :disabled="!currentResult!.canDrillDown"
                  @click.stop="handleCellDrill(row, col)"
                >
                  {{ row[col.prop] ?? '-' }}
                  <el-icon v-if="currentResult!.canDrillDown" class="drill-arrow">
                    <ArrowRight />
                  </el-icon>
                </el-button>
              </template>
            </el-table-column>
          </template>
        </el-table>

        <div v-if="currentResult && currentResult.total > 0" class="drill-footer">
          共 {{ currentResult.total }} 条记录
          <span v-if="!currentResult.canDrillDown" class="leaf-hint">（叶子节点，无法继续钻取）</span>
        </div>
        <el-empty v-if="currentResult && currentResult.total === 0" description="暂无数据" :image-size="60" />
      </div>
    </template>
  </div>
</template>

<script setup lang="ts">
import { ref } from 'vue'
import { ElMessage } from 'element-plus'
import { ArrowRight } from '@element-plus/icons-vue'
import {
  drillDown,
  type DrillDownRequestDTO,
  type DrillDownResultVO,
  type DrillColumnVO,
  type DrillCrumb,
} from '@/api/rpt/drillDown'

// ── props ─────────────────────────────────────────────────────────────────────

const props = withDefaults(
  defineProps<{
    /** 报表编码（决定钻取域） */
    reportCode: string
    /** 顶层面包屑文字 */
    topLabel?: string
    /** 统计日期（资产域） */
    statDate?: string
    /** 起始月份（财务域） */
    startMonth?: string
    /** 结束月份（财务域） */
    endMonth?: string
  }>(),
  {
    topLabel: '全部',
  },
)

const emit = defineEmits<{
  /** 每次成功钻取后触发，携带结果 */
  (e: 'drilled', result: DrillDownResultVO, crumbs: DrillCrumb[]): void
  /** 重置到顶层时触发 */
  (e: 'reset'): void
}>()

// ── 状态 ──────────────────────────────────────────────────────────────────────

const loading = ref(false)
const breadcrumbs = ref<DrillCrumb[]>([])
const currentResult = ref<DrillDownResultVO | null>(null)

// ── 对外方法（供父组件通过 ref 调用） ─────────────────────────────────────────

/** 从图表 click 事件触发下钻（父组件绑定 ECharts onClick 后调用此方法） */
async function drillByChart(dimensionId: number, levelLabel?: string) {
  const fromLevel = breadcrumbs.value.length === 0 ? 1 : currentResult.value?.currentLevel ?? 1
  await executeDrill(fromLevel, dimensionId, levelLabel ?? `ID:${dimensionId}`)
}

/** 手动重置到顶层 */
function reset() {
  resetToTop()
}

defineExpose({ drillByChart, reset })

// ── 内部方法 ──────────────────────────────────────────────────────────────────

/** 顶层 slot 触发的钻取（fromLevel=1，parent=项目级） */
function handleTopDrill(dimensionId: number, label?: string) {
  executeDrill(1, dimensionId, label ?? `#${dimensionId}`)
}

/** 表格行点击（仅当行点击模式，通常由 slot 替换） */
function handleRowClick(row: Record<string, any>) {
  if (!currentResult.value?.canDrillDown) return
  // 找到第一个 drillable 列
  const drillCol = currentResult.value.columns.find(c => c.drillable)
  if (drillCol) {
    handleCellDrill(row, drillCol)
  }
}

/** 可下钻单元格点击 */
function handleCellDrill(row: Record<string, any>, col: DrillColumnVO) {
  if (!currentResult.value?.canDrillDown) return
  const dimId = Number(row[col.drillIdField ?? col.prop])
  const labelVal = row[col.prop]
  const fromLevel = currentResult.value.currentLevel
  executeDrill(fromLevel, dimId, labelVal ? String(labelVal) : `#${dimId}`)
}

/** 点击面包屑回退到历史层级 */
function backTo(index: number) {
  const target = breadcrumbs.value[index]
  breadcrumbs.value = breadcrumbs.value.slice(0, index + 1)
  // 使用快照数据恢复，无需重新请求
  if (target.resultSnapshot) {
    currentResult.value = target.resultSnapshot
  } else {
    // 没有快照则重新请求
    executeDrillRequest(target.requestSnapshot, target.label)
  }
}

/** 重置到顶层 */
function resetToTop() {
  breadcrumbs.value = []
  currentResult.value = null
  emit('reset')
}

/** 核心钻取执行 */
async function executeDrill(fromLevel: number, dimensionId: number, label: string) {
  const dto: DrillDownRequestDTO = {
    reportCode: props.reportCode,
    fromLevel,
    dimensionId,
    statDate: props.statDate,
    startMonth: props.startMonth,
    endMonth: props.endMonth,
  }
  await executeDrillRequest(dto, label)
}

async function executeDrillRequest(dto: DrillDownRequestDTO, label: string) {
  loading.value = true
  try {
    const res = await drillDown(dto)
    const result: DrillDownResultVO = (res as any).data ?? res

    // 将当前层数据快照存入面包屑（用于回退时恢复）
    const crumb: DrillCrumb = {
      level: dto.fromLevel,
      levelName: result.levelName,
      dimensionId: dto.dimensionId,
      label,
      requestSnapshot: { ...dto },
      resultSnapshot: result,
    }

    // 检查是否回退到已有层级
    const existingIdx = breadcrumbs.value.findIndex(
      c => c.level === dto.fromLevel && c.dimensionId === dto.dimensionId,
    )
    if (existingIdx >= 0) {
      breadcrumbs.value = breadcrumbs.value.slice(0, existingIdx + 1)
    } else {
      breadcrumbs.value.push(crumb)
    }

    currentResult.value = result
    emit('drilled', result, breadcrumbs.value)
  } catch (e: any) {
    ElMessage.error(e?.message ?? '钻取失败，请稍后重试')
  } finally {
    loading.value = false
  }
}

/** 行样式：有可钻取列时加 cursor:pointer */
function rowClass({ row }: { row: any }) {
  return currentResult.value?.canDrillDown ? 'row-drillable' : ''
}
</script>

<style scoped lang="scss">
.drill-panel {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.drill-header {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 6px 0;
  border-bottom: 1px solid #ebeef5;
}

.drill-breadcrumb {
  flex: 1;
  font-size: 13px;

  :deep(.crumb-link .el-breadcrumb__inner) {
    cursor: pointer;
    color: #409eff;
    font-weight: 500;
    &:hover { text-decoration: underline; }
  }

  :deep(.crumb-current .el-breadcrumb__inner) {
    color: #606266;
    cursor: default;
    font-weight: 500;
  }
}

.level-tag {
  white-space: nowrap;
}

.level-hint {
  font-size: 11px;
  color: #909399;
}

.drill-table-wrap {
  position: relative;
  min-height: 80px;
}

.drill-footer {
  margin-top: 8px;
  font-size: 12px;
  color: #909399;
}

.leaf-hint {
  margin-left: 4px;
  color: #f56c6c;
}

.drill-arrow {
  font-size: 11px;
  margin-left: 2px;
  opacity: 0.7;
}

:deep(.row-drillable) {
  cursor: pointer;
  &:hover td { background-color: #ecf5ff !important; }
}
</style>
