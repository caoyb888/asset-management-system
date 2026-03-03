<template>
  <div class="drill-table">
    <!-- 面包屑导航 -->
    <div v-if="crumbs.length > 0" class="drill-breadcrumb">
      <el-breadcrumb separator="/">
        <el-breadcrumb-item
          class="crumb-link"
          @click="backTo(-1)"
        >全部</el-breadcrumb-item>
        <el-breadcrumb-item
          v-for="(c, idx) in crumbs"
          :key="idx"
          :class="idx < crumbs.length - 1 ? 'crumb-link' : ''"
          @click="idx < crumbs.length - 1 && backTo(idx)"
        >{{ c.label }}</el-breadcrumb-item>
      </el-breadcrumb>
    </div>

    <!-- 表格 -->
    <el-table
      :data="data"
      :loading="loading"
      v-loading="loading"
      border
      stripe
      size="small"
      :max-height="maxHeight"
      :row-class-name="rowClassName"
      @row-click="handleRowClick"
    >
      <template v-for="col in columns" :key="col.prop ?? col.slot">
        <!-- 自定义渲染列 -->
        <el-table-column
          v-if="col.slot"
          :label="col.label"
          :width="col.width"
          :min-width="col.minWidth"
          :align="col.align ?? 'left'"
          :fixed="col.fixed"
        >
          <template #default="{ row, $index }">
            <slot :name="col.slot!" :row="row" :index="$index" />
          </template>
        </el-table-column>

        <!-- 可下钻列 -->
        <el-table-column
          v-else-if="col.drillable"
          :label="col.label"
          :prop="col.prop"
          :width="col.width"
          :min-width="col.minWidth"
          :align="col.align ?? 'left'"
          :fixed="col.fixed"
        >
          <template #default="{ row }">
            <el-button
              link
              type="primary"
              class="drill-cell"
              :disabled="!isDrillable(row, col)"
              @click.stop="handleDrillCell(row, col)"
            >
              {{ row[col.prop!] ?? '-' }}
              <el-icon v-if="isDrillable(row, col)" class="drill-icon"><ArrowRight /></el-icon>
            </el-button>
          </template>
        </el-table-column>

        <!-- 普通列 -->
        <el-table-column
          v-else
          :label="col.label"
          :prop="col.prop"
          :width="col.width"
          :min-width="col.minWidth"
          :align="col.align ?? 'left'"
          :fixed="col.fixed"
          :formatter="col.formatter"
        />
      </template>

      <!-- 操作列 -->
      <el-table-column v-if="showActionCol" label="操作" width="80" fixed="right">
        <template #default="{ row }">
          <el-button
            size="small"
            type="primary"
            link
            @click.stop="handleDrillRow(row)"
          >
            <el-icon><Expand /></el-icon>
            下钻
          </el-button>
        </template>
      </el-table-column>
    </el-table>
  </div>
</template>

<script setup lang="ts">
import { ref, computed } from 'vue'
import { ArrowRight, Expand } from '@element-plus/icons-vue'

// ─────────────────── 类型 ───────────────────

export interface DrillColumn {
  /** 字段名 */
  prop?: string
  /** 具名插槽名称（与 prop 二选一） */
  slot?: string
  /** 列标题 */
  label: string
  width?: string | number
  minWidth?: string | number
  align?: 'left' | 'center' | 'right'
  fixed?: 'left' | 'right' | boolean
  /** 是否支持点击下钻 */
  drillable?: boolean
  /** 判断此行此列是否可下钻（返回 false 则禁用） */
  drillableFn?: (row: Record<string, unknown>) => boolean
  /** el-table column formatter */
  formatter?: (row: unknown, column: unknown, val: unknown) => string
}

export interface DrillCrumb {
  label: string
  data?: unknown
}

export interface DrillEvent {
  row: Record<string, unknown>
  column?: DrillColumn
  /** 触发方式：'row'=行点击，'cell'=单元格点击，'action'=操作按钮 */
  source: 'row' | 'cell' | 'action'
  /** 当前层级深度（0=顶层） */
  depth: number
}

// ─────────────────── Props / Emits ───────────────────

const props = withDefaults(
  defineProps<{
    data: Record<string, unknown>[]
    columns: DrillColumn[]
    loading?: boolean
    maxHeight?: string | number
    /** 是否显示操作列中的「下钻」按钮 */
    showActionCol?: boolean
    /** 点击行是否触发下钻（默认 false，需显式开启） */
    rowClickDrill?: boolean
    /** 面包屑标题（外部控制，用于多层下钻） */
    crumbs?: DrillCrumb[]
  }>(),
  {
    loading: false,
    maxHeight: 520,
    showActionCol: false,
    rowClickDrill: false,
    crumbs: () => [],
  },
)

const emit = defineEmits<{
  drill: [event: DrillEvent]
  'back-to': [index: number]
}>()

// ─────────────────── Methods ───────────────────

function isDrillable(row: Record<string, unknown>, col: DrillColumn) {
  if (!col.drillable) return false
  if (col.drillableFn) return col.drillableFn(row)
  return true
}

function rowClassName({ row }: { row: Record<string, unknown> }) {
  if (props.rowClickDrill) return 'row-drillable'
  return ''
}

function handleRowClick(row: Record<string, unknown>) {
  if (!props.rowClickDrill) return
  emit('drill', { row, source: 'row', depth: props.crumbs.length })
}

function handleDrillCell(row: Record<string, unknown>, column: DrillColumn) {
  emit('drill', { row, column, source: 'cell', depth: props.crumbs.length })
}

function handleDrillRow(row: Record<string, unknown>) {
  emit('drill', { row, source: 'action', depth: props.crumbs.length })
}

function backTo(index: number) {
  emit('back-to', index)
}
</script>

<style scoped lang="scss">
.drill-table {
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.drill-breadcrumb {
  padding: 6px 0 2px;
  font-size: 13px;

  :deep(.crumb-link .el-breadcrumb__inner) {
    cursor: pointer;
    color: #409eff;
    &:hover { text-decoration: underline; }
  }
}

.drill-cell {
  display: inline-flex;
  align-items: center;
  gap: 2px;
  padding: 0;
  font-size: 13px;
}

.drill-icon {
  font-size: 11px;
  opacity: 0.7;
}

:deep(.row-drillable) {
  cursor: pointer;
  &:hover td { background-color: #ecf5ff !important; }
}
</style>
