<template>
  <div class="receivable-calendar">
    <!-- 日历主体 -->
    <el-calendar v-model="calendarDate">
      <template #date-cell="{ data }">
        <div
          class="calendar-cell"
          :class="getCellClass(data.day)"
          @click="handleDateClick(data.day)"
        >
          <div class="cell-date">{{ data.day.split('-').pop() }}</div>
          <template v-if="getDayPlans(data.day).length > 0">
            <div class="cell-amount">
              ¥{{ formatAmount(getDayTotal(data.day)) }}
            </div>
            <div class="cell-count">{{ getDayPlans(data.day).length }}笔</div>
          </template>
        </div>
      </template>
    </el-calendar>

    <!-- 图例说明 -->
    <div class="legend">
      <span class="legend-item legend-item--pending">待收</span>
      <span class="legend-item legend-item--received">已收</span>
      <span class="legend-item legend-item--overdue">逾期</span>
      <span class="legend-item legend-item--void">作废</span>
    </div>

    <!-- 日期明细抽屉 -->
    <el-drawer
      v-model="drawerVisible"
      :title="`${selectedDate} 应收明细`"
      size="400px"
      direction="rtl"
    >
      <div v-if="selectedDayPlans.length === 0" style="padding: 20px; text-align: center; color: #909399;">
        当日无应收记录
      </div>
      <el-table v-else :data="selectedDayPlans" border size="small">
        <el-table-column label="费项" prop="feeName" min-width="100" />
        <el-table-column label="金额" prop="amount" width="110" align="right">
          <template #default="{ row }">
            <span style="font-weight: 600;">¥{{ formatAmount(row.amount) }}</span>
          </template>
        </el-table-column>
        <el-table-column label="已收" prop="receivedAmount" width="110" align="right">
          <template #default="{ row }">
            <span>¥{{ formatAmount(row.receivedAmount) }}</span>
          </template>
        </el-table-column>
        <el-table-column label="状态" width="80" align="center">
          <template #default="{ row }">
            <el-tag :type="statusTagType(row.status)" size="small">
              {{ statusLabel(row.status) }}
            </el-tag>
          </template>
        </el-table-column>
      </el-table>

      <!-- 当日汇总 -->
      <div v-if="selectedDayPlans.length > 0" class="drawer-summary">
        <span>当日应收合计：</span>
        <span class="summary-amount">¥{{ formatAmount(getDayTotal(selectedDate)) }}</span>
      </div>
    </el-drawer>
  </div>
</template>

<script setup lang="ts">
import { ref, computed } from 'vue'
import type { OprReceivablePlan } from '@/api/opr/ledger'

// ─── Props ────────────────────────────────────────────────
const props = withDefaults(defineProps<{
  /** 应收计划列表 */
  receivables?: OprReceivablePlan[]
  /** 显示年份（默认当前年） */
  year?: number
  /** 显示月份（1-12，默认当前月） */
  month?: number
}>(), {
  receivables: () => [],
  year: () => new Date().getFullYear(),
  month: () => new Date().getMonth() + 1,
})

// ─── Emits ────────────────────────────────────────────────
const emit = defineEmits<{
  /** 点击日期：返回日期字符串和当日应收列表 */
  (e: 'date-click', date: string, plans: OprReceivablePlan[]): void
}>()

// ─── 状态 ─────────────────────────────────────────────────
const calendarDate = ref(new Date(props.year, props.month - 1, 1))
const drawerVisible = ref(false)
const selectedDate = ref('')

// ─── 计算属性 ─────────────────────────────────────────────
/** 按日期分组的应收计划 Map */
const plansByDate = computed<Map<string, OprReceivablePlan[]>>(() => {
  const map = new Map<string, OprReceivablePlan[]>()
  for (const plan of props.receivables) {
    // 应收计划以到期日（dueDate）为日历日期
    const date = plan.dueDate?.substring(0, 10)
    if (!date) continue
    const existing = map.get(date) ?? []
    existing.push(plan)
    map.set(date, existing)
  }
  return map
})

const selectedDayPlans = computed<OprReceivablePlan[]>(() =>
  selectedDate.value ? (plansByDate.value.get(selectedDate.value) ?? []) : []
)

// ─── 方法 ─────────────────────────────────────────────────
function getDayPlans(day: string): OprReceivablePlan[] {
  return plansByDate.value.get(day) ?? []
}

function getDayTotal(day: string): number {
  return getDayPlans(day).reduce((sum, p) => sum + Number(p.amount ?? 0), 0)
}

/** 返回日期格子的 CSS class（按状态决定颜色）*/
function getCellClass(day: string): string {
  const plans = getDayPlans(day)
  if (plans.length === 0) return ''
  // 优先展示最高严重度状态
  if (plans.some(p => p.status === 2)) return 'cell--overdue'  // 逾期
  if (plans.some(p => p.status === 3)) return 'cell--void'     // 作废
  if (plans.every(p => p.status === 1)) return 'cell--received' // 全部已收
  return 'cell--pending' // 待收/部分收
}

function handleDateClick(day: string) {
  const plans = getDayPlans(day)
  selectedDate.value = day
  drawerVisible.value = true
  emit('date-click', day, plans)
}

// ─── 工具函数 ─────────────────────────────────────────────
function formatAmount(val: number | undefined | null): string {
  if (val == null) return '0.00'
  return Number(val).toLocaleString('zh-CN', { minimumFractionDigits: 2, maximumFractionDigits: 2 })
}

type TagType = 'primary' | 'success' | 'warning' | 'danger' | 'info' | undefined
const STATUS_TAG: Record<number, TagType> = {
  0: 'primary',   // 待收
  1: 'success',   // 已收
  2: 'danger',    // 逾期
  3: 'info',      // 作废
}
function statusTagType(status: number): TagType { return STATUS_TAG[status] }
function statusLabel(status: number): string {
  const m: Record<number, string> = { 0: '待收', 1: '已收', 2: '逾期', 3: '作废' }
  return m[status] ?? '未知'
}
</script>

<style scoped>
.receivable-calendar {
  position: relative;
}

/* 日历格子 */
.calendar-cell {
  height: 100%;
  min-height: 60px;
  padding: 2px 4px;
  cursor: pointer;
  border-radius: 4px;
  transition: background 0.15s;
}

.calendar-cell:hover {
  background: #f0f2f5;
}

.cell-date {
  font-size: 12px;
  color: #606266;
}

.cell-amount {
  font-size: 11px;
  font-weight: 700;
  line-height: 1.3;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.cell-count {
  font-size: 10px;
  color: #909399;
}

/* 状态颜色 */
.cell--pending {
  background: #ecf5ff;
}
.cell--pending .cell-amount {
  color: #409eff;
}

.cell--received {
  background: #f0f9eb;
}
.cell--received .cell-amount {
  color: #67c23a;
}

.cell--overdue {
  background: #fef0f0;
}
.cell--overdue .cell-amount {
  color: #f56c6c;
}

.cell--void {
  background: #f4f4f5;
}
.cell--void .cell-amount {
  color: #909399;
}

/* 图例 */
.legend {
  display: flex;
  gap: 16px;
  padding: 8px 0;
  justify-content: flex-end;
  font-size: 12px;
}

.legend-item {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  padding: 2px 8px;
  border-radius: 3px;
}

.legend-item--pending {
  background: #ecf5ff;
  color: #409eff;
}
.legend-item--received {
  background: #f0f9eb;
  color: #67c23a;
}
.legend-item--overdue {
  background: #fef0f0;
  color: #f56c6c;
}
.legend-item--void {
  background: #f4f4f5;
  color: #909399;
}

/* 抽屉内汇总 */
.drawer-summary {
  margin-top: 16px;
  padding: 12px 16px;
  background: #f5f7fa;
  border-radius: 6px;
  display: flex;
  justify-content: space-between;
  align-items: center;
  font-size: 14px;
}

.summary-amount {
  font-size: 18px;
  font-weight: 700;
  color: #f56c6c;
}
</style>
