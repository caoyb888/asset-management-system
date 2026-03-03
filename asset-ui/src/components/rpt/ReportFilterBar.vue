<template>
  <el-card class="rpt-filter-bar" shadow="never">
    <el-form inline :model="modelValue">
      <!-- 主要筛选项（始终显示） -->
      <el-form-item v-if="hasField('project')" label="项目">
        <el-select
          :model-value="modelValue.projectId"
          placeholder="全部项目"
          clearable
          style="width: 200px"
          @change="emit('update:modelValue', { ...modelValue, projectId: $event })"
        >
          <el-option v-for="p in projectList" :key="p.id" :label="p.projectName" :value="p.id" />
        </el-select>
      </el-form-item>

      <el-form-item v-if="hasField('monthRange')" label="月份范围">
        <el-date-picker
          :model-value="modelValue.monthRange"
          type="monthrange"
          value-format="YYYY-MM"
          start-placeholder="开始月份"
          end-placeholder="结束月份"
          style="width: 240px"
          @update:model-value="emit('update:modelValue', { ...modelValue, monthRange: $event })"
        />
      </el-form-item>

      <el-form-item v-if="hasField('statMonth')" label="统计月份">
        <el-date-picker
          :model-value="modelValue.statMonth"
          type="month"
          value-format="YYYY-MM"
          placeholder="选择月份"
          style="width: 160px"
          @update:model-value="emit('update:modelValue', { ...modelValue, statMonth: $event })"
        />
      </el-form-item>

      <el-form-item v-if="hasField('dateRange')" label="日期范围">
        <el-date-picker
          :model-value="modelValue.dateRange"
          type="daterange"
          value-format="YYYY-MM-DD"
          start-placeholder="开始日期"
          end-placeholder="结束日期"
          style="width: 240px"
          @update:model-value="emit('update:modelValue', { ...modelValue, dateRange: $event })"
        />
      </el-form-item>

      <!-- 扩展筛选项（可折叠） -->
      <template v-if="!collapsed || !collapsible">
        <el-form-item v-if="hasField('building')" label="楼栋">
          <el-select
            :model-value="modelValue.buildingId"
            placeholder="全部楼栋"
            clearable
            style="width: 150px"
            @change="emit('update:modelValue', { ...modelValue, buildingId: $event })"
          >
            <el-option v-for="b in buildingList" :key="b.id" :label="b.buildingName" :value="b.id" />
          </el-select>
        </el-form-item>

        <el-form-item v-if="hasField('floor')" label="楼层">
          <el-input
            :model-value="modelValue.floorId"
            placeholder="全部楼层"
            clearable
            style="width: 120px"
            @input="emit('update:modelValue', { ...modelValue, floorId: $event || null })"
          />
        </el-form-item>

        <el-form-item v-if="hasField('formatType')" label="业态">
          <el-input
            :model-value="modelValue.formatType"
            placeholder="全部业态"
            clearable
            style="width: 130px"
            @input="emit('update:modelValue', { ...modelValue, formatType: $event })"
          />
        </el-form-item>

        <el-form-item v-if="hasField('feeItemType')" label="费项类型">
          <el-input
            :model-value="modelValue.feeItemType"
            placeholder="全部费项"
            clearable
            style="width: 130px"
            @input="emit('update:modelValue', { ...modelValue, feeItemType: $event })"
          />
        </el-form-item>

        <el-form-item v-if="hasField('merchant')" label="商家ID">
          <el-input-number
            :model-value="modelValue.merchantId"
            placeholder="商家ID"
            :min="1"
            :controls="false"
            clearable
            style="width: 120px"
            @change="emit('update:modelValue', { ...modelValue, merchantId: $event ?? null })"
          />
        </el-form-item>
      </template>

      <!-- 对比模式（始终显示） -->
      <el-form-item v-if="hasField('compareMode')" label="对比">
        <el-radio-group
          :model-value="modelValue.compareMode"
          @change="emit('update:modelValue', { ...modelValue, compareMode: $event as FilterState['compareMode'] }); emit('search')"
        >
          <el-radio-button value="NONE">不对比</el-radio-button>
          <el-radio-button value="YOY">同比</el-radio-button>
          <el-radio-button value="MOM">环比</el-radio-button>
        </el-radio-group>
      </el-form-item>

      <!-- 操作按钮 -->
      <el-form-item>
        <el-button type="primary" :loading="loading" @click="emit('search')">查询</el-button>
        <el-button @click="handleReset">重置</el-button>
        <el-button
          v-if="collapsible"
          link
          type="info"
          @click="collapsed = !collapsed"
        >
          {{ collapsed ? '展开' : '收起' }}
          <el-icon class="toggle-icon" :class="{ rotated: !collapsed }"><ArrowDown /></el-icon>
        </el-button>
      </el-form-item>

      <!-- 数据更新时间提示 -->
      <span v-if="latestDate" class="latest-tip">
        <el-icon><Clock /></el-icon>
        数据更新至：<strong>{{ latestDate }}</strong>
      </span>
    </el-form>
  </el-card>
</template>

<script setup lang="ts">
import { ref, onMounted, watch } from 'vue'
import { Clock, ArrowDown } from '@element-plus/icons-vue'
import { getProjectList } from '@/api/base/project'

// ─────────────────── 类型 ───────────────────

export interface FilterState {
  projectId?: number | null
  monthRange?: [string, string] | null
  statMonth?: string | null
  dateRange?: [string, string] | null
  buildingId?: number | null
  floorId?: string | null
  formatType?: string
  feeItemType?: string
  merchantId?: number | null
  compareMode?: 'NONE' | 'YOY' | 'MOM'
  [key: string]: unknown
}

type FieldName =
  | 'project'
  | 'monthRange'
  | 'statMonth'
  | 'dateRange'
  | 'building'
  | 'floor'
  | 'formatType'
  | 'feeItemType'
  | 'merchant'
  | 'compareMode'

// ─────────────────── Props / Emits ───────────────────

const props = withDefaults(
  defineProps<{
    modelValue: FilterState
    /** 要显示的字段，默认 ['project','monthRange','compareMode'] */
    fields?: FieldName[]
    loading?: boolean
    latestDate?: string | null
    /** 是否支持收缩/展开（默认：当 fields.length > 4 时自动启用） */
    collapsible?: boolean
    /** 重置时恢复的初始值 */
    defaultValue?: FilterState
  }>(),
  {
    fields: () => ['project', 'monthRange', 'compareMode'],
    loading: false,
    latestDate: null,
    collapsible: false,
    defaultValue: () => ({}),
  },
)

const emit = defineEmits<{
  'update:modelValue': [val: FilterState]
  search: []
  reset: []
}>()

// ─────────────────── State ───────────────────

const collapsed = ref(true)
const projectList = ref<{ id: number; projectName: string; projectCode: string }[]>([])
const buildingList = ref<{ id: number; buildingName: string }[]>([])

// ─────────────────── Lifecycle ───────────────────

onMounted(async () => {
  if (hasField('project')) {
    projectList.value = await getProjectList()
  }
})

// 项目切换时清空楼栋（简单实现：楼栋列表由父组件提供或此处留空）
watch(() => props.modelValue.projectId, () => {
  buildingList.value = []
})

// ─────────────────── Methods ───────────────────

function hasField(name: FieldName) {
  return props.fields.includes(name)
}

function handleReset() {
  emit('update:modelValue', { ...props.defaultValue })
  emit('reset')
}
</script>

<style scoped lang="scss">
.rpt-filter-bar {
  :deep(.el-card__body) { padding: 12px 16px; }
}
.latest-tip {
  margin-left: 12px;
  font-size: 13px;
  color: #909399;
  display: inline-flex;
  align-items: center;
  gap: 4px;
  vertical-align: middle;
}
.toggle-icon {
  transition: transform 0.2s;
  &.rotated { transform: rotate(180deg); }
}
</style>
