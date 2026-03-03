<template>
  <el-card
    class="metric-card"
    shadow="never"
    :style="accentStyle"
    v-loading="loading"
  >
    <div class="mc-label">{{ label }}</div>

    <div class="mc-value" :style="valueStyle">
      <template v-if="value != null">
        {{ formattedValue }}
        <span v-if="unit" class="mc-unit">{{ unit }}</span>
      </template>
      <span v-else class="mc-empty">-</span>
    </div>

    <!-- 同比行 -->
    <div v-if="yoy !== undefined" class="mc-compare-row">
      <span class="mc-compare-item" :class="deltaClass(yoy, reverseGood)">
        <el-icon><component :is="deltaIcon(yoy, reverseGood)" /></el-icon>
        同比 {{ fmtDelta(yoy) }}
      </span>
      <span v-if="mom !== undefined" class="mc-compare-item" :class="deltaClass(mom, reverseGood)">
        <el-icon><component :is="deltaIcon(mom, reverseGood)" /></el-icon>
        环比 {{ fmtDelta(mom) }}
      </span>
    </div>

    <!-- 附加说明行 -->
    <div v-if="sub" class="mc-sub">{{ sub }}</div>

    <!-- 默认插槽：额外内容 -->
    <slot />
  </el-card>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import { Top, Bottom, Minus } from '@element-plus/icons-vue'

const props = withDefaults(
  defineProps<{
    /** 指标名称 */
    label: string
    /** 指标值（传 null/undefined 显示"-"） */
    value?: number | string | null
    /** 单位（如"万元"、"%"），显示在数值后 */
    unit?: string
    /** 同比增长率（百分比值，如 12.5 代表 +12.5%） */
    yoy?: number | null
    /** 环比增长率 */
    mom?: number | null
    /**
     * 是否「越低越好」（如空置率、逾期率）
     * true：下降=绿色，上升=红色
     * false（默认）：上升=绿色，下降=红色
     */
    reverseGood?: boolean
    /** 顶部强调色，默认 #409eff */
    color?: string
    /** 小字补充信息，显示在同比行下方 */
    sub?: string | null
    /** 加载中状态 */
    loading?: boolean
    /** 数字格式化精度，默认 2 */
    precision?: number
    /** 是否将数值除以10000显示（配合 unit="万元" 使用） */
    wan?: boolean
  }>(),
  {
    reverseGood: false,
    color: '#409eff',
    loading: false,
    precision: 2,
    wan: false,
  },
)

const accentStyle = computed(() => ({
  borderTop: `4px solid ${props.color}`,
}))

const valueStyle = computed(() => ({
  color: props.color,
}))

const formattedValue = computed(() => {
  if (props.value == null) return '-'
  if (typeof props.value === 'string') return props.value
  const n = props.wan ? Number(props.value) / 10000 : Number(props.value)
  return n.toFixed(props.precision)
})

function fmtDelta(v?: number | null) {
  if (v == null) return '-'
  return `${Number(v) > 0 ? '+' : ''}${Number(v).toFixed(2)}%`
}

function deltaClass(v?: number | null, reverseGood = false) {
  if (v == null) return 'neutral'
  const up = Number(v) > 0
  const good = reverseGood ? !up : up
  if (Number(v) === 0) return 'neutral'
  return good ? 'up' : 'down'
}

function deltaIcon(v?: number | null, reverseGood = false) {
  if (v == null || Number(v) === 0) return Minus
  return Number(v) > 0 ? Top : Bottom
}
</script>

<style scoped lang="scss">
.metric-card {
  text-align: center;
  padding: 4px 0;

  :deep(.el-card__body) { padding: 16px 12px 12px; }
}

.mc-label {
  font-size: 13px;
  color: #606266;
  margin-bottom: 8px;
}

.mc-value {
  font-size: 28px;
  font-weight: 700;
  line-height: 1.2;
  margin-bottom: 10px;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.mc-unit {
  font-size: 14px;
  font-weight: 400;
  margin-left: 2px;
}

.mc-empty {
  color: #c0c4cc;
}

.mc-compare-row {
  display: flex;
  justify-content: center;
  gap: 12px;
  flex-wrap: wrap;
  margin-bottom: 4px;
}

.mc-compare-item {
  display: inline-flex;
  align-items: center;
  gap: 2px;
  font-size: 12px;

  &.up { color: #67c23a; }
  &.down { color: #f56c6c; }
  &.neutral { color: #909399; }
}

.mc-sub {
  font-size: 12px;
  color: #c0c4cc;
  margin-top: 4px;
}
</style>
