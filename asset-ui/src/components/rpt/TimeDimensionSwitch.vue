<template>
  <div class="time-dim-switch">
    <!-- 时间维度切换 -->
    <div v-if="showTimeUnit" class="switch-group">
      <span class="switch-label">时间维度</span>
      <el-radio-group :model-value="timeUnit" size="small" @change="(v: any) => onUnitChange(v as TimeUnit)">
        <el-radio-button v-if="units.includes('DAY')" value="DAY">日</el-radio-button>
        <el-radio-button v-if="units.includes('WEEK')" value="WEEK">周</el-radio-button>
        <el-radio-button v-if="units.includes('MONTH')" value="MONTH">月</el-radio-button>
        <el-radio-button v-if="units.includes('YEAR')" value="YEAR">年</el-radio-button>
      </el-radio-group>
    </div>

    <!-- 同比/环比开关 -->
    <div v-if="showCompare" class="switch-group">
      <span class="switch-label">对比模式</span>
      <el-radio-group :model-value="compareMode" size="small" @change="(v: any) => onCompareModeChange(v as CompareMode)">
        <el-radio-button value="NONE">不对比</el-radio-button>
        <el-radio-button value="YOY">
          <el-tooltip content="与去年同期对比" placement="top">
            <span>同比</span>
          </el-tooltip>
        </el-radio-button>
        <el-radio-button value="MOM">
          <el-tooltip content="与上一周期对比" placement="top">
            <span>环比</span>
          </el-tooltip>
        </el-radio-button>
      </el-radio-group>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed } from 'vue'

export type TimeUnit = 'DAY' | 'WEEK' | 'MONTH' | 'YEAR'
export type CompareMode = 'NONE' | 'YOY' | 'MOM'

const props = withDefaults(
  defineProps<{
    timeUnit?: TimeUnit
    compareMode?: CompareMode
    /** 可用的时间维度列表，默认全部 */
    units?: TimeUnit[]
    /** 是否显示时间维度切换器 */
    showTimeUnit?: boolean
    /** 是否显示同比/环比切换器 */
    showCompare?: boolean
  }>(),
  {
    timeUnit: 'MONTH',
    compareMode: 'NONE',
    units: () => ['DAY', 'WEEK', 'MONTH', 'YEAR'],
    showTimeUnit: true,
    showCompare: true,
  },
)

const emit = defineEmits<{
  'update:timeUnit': [val: TimeUnit]
  'update:compareMode': [val: CompareMode]
  /** 任意切换后触发，携带最新值 */
  change: [timeUnit: TimeUnit, compareMode: CompareMode]
}>()

function onUnitChange(val: TimeUnit) {
  emit('update:timeUnit', val)
  emit('change', val, props.compareMode!)
}

function onCompareModeChange(val: CompareMode) {
  emit('update:compareMode', val)
  emit('change', props.timeUnit!, val)
}
</script>

<style scoped lang="scss">
.time-dim-switch {
  display: inline-flex;
  align-items: center;
  gap: 20px;
  flex-wrap: wrap;
}
.switch-group {
  display: inline-flex;
  align-items: center;
  gap: 8px;
}
.switch-label {
  font-size: 13px;
  color: #606266;
  white-space: nowrap;
}
</style>
