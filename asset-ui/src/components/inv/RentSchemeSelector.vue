<template>
  <div class="rent-scheme-selector">
    <div v-if="loading" class="loading-wrap">
      <el-skeleton :rows="2" animated />
    </div>

    <div v-else class="scheme-grid">
      <div
        v-for="scheme in schemes"
        :key="scheme.id"
        class="scheme-card"
        :class="{ 'is-selected': modelValue === scheme.id, 'is-disabled': disabled }"
        @click="!disabled && handleSelect(scheme)"
      >
        <!-- 选中角标 -->
        <div v-if="modelValue === scheme.id" class="selected-badge">
          <el-icon><Check /></el-icon>
        </div>

        <!-- 收费方式标签 -->
        <el-tag :type="chargeTypeColor(scheme.chargeType)" size="small" class="charge-tag">
          {{ CHARGE_TYPE_MAP[scheme.chargeType] ?? '-' }}
        </el-tag>

        <!-- 方案名称 -->
        <div class="scheme-name">{{ scheme.schemeName }}</div>

        <!-- 方案参数 -->
        <div class="scheme-meta">
          <span>{{ PAYMENT_CYCLE_MAP[scheme.paymentCycle] ?? '-' }}</span>
          <el-divider direction="vertical" />
          <span>{{ BILLING_MODE_MAP[scheme.billingMode] ?? '-' }}</span>
        </div>

        <!-- 说明 -->
        <div class="scheme-desc" :title="scheme.description">{{ scheme.description }}</div>
      </div>
    </div>

    <!-- 无数据 -->
    <el-empty v-if="!loading && schemes.length === 0" description="暂无可用计租方案" :image-size="60" />

    <!-- 已选中信息 -->
    <div v-if="selectedScheme" class="selected-info">
      <el-icon color="#409eff"><InfoFilled /></el-icon>
      已选：<strong>{{ selectedScheme.schemeName }}</strong>
      &nbsp;·&nbsp;{{ CHARGE_TYPE_MAP[selectedScheme.chargeType] }}
      &nbsp;·&nbsp;{{ PAYMENT_CYCLE_MAP[selectedScheme.paymentCycle] }}
      &nbsp;·&nbsp;{{ BILLING_MODE_MAP[selectedScheme.billingMode] }}
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { Check, InfoFilled } from '@element-plus/icons-vue'
import { getRentSchemeList, type RentSchemeVO } from '@/api/inv/config'

// ─── Props & Emits ───
const props = withDefaults(defineProps<{
  modelValue?: number | null   // 选中的方案ID
  disabled?: boolean
}>(), {
  modelValue: null,
  disabled: false,
})

const emit = defineEmits<{
  'update:modelValue': [id: number | null]
  /** 选中方案后同时抛出完整 VO，方便父组件读取 chargeType 等字段 */
  'select': [scheme: RentSchemeVO]
}>()

// ─── 字典 ───
const CHARGE_TYPE_MAP: Record<number, string> = {
  1: '固定租金', 2: '固定提成', 3: '阶梯提成', 4: '两者取高', 5: '一次性',
}
const PAYMENT_CYCLE_MAP: Record<number, string> = {
  1: '月付', 2: '两月付', 3: '季付', 4: '四月付', 5: '半年付', 6: '年付',
}
const BILLING_MODE_MAP: Record<number, string> = {
  1: '预付', 2: '当期', 3: '后付',
}
type TagType = 'primary' | 'success' | 'warning' | 'danger' | 'info' | undefined
const chargeTypeColor = (t: number): TagType => {
  const map: Record<number, TagType> = { 1: undefined, 2: 'success', 3: 'warning', 4: 'danger', 5: 'info' }
  return map[t]
}

// ─── 数据 ───
const loading = ref(false)
const schemes = ref<RentSchemeVO[]>([])

const selectedScheme = computed(() =>
  schemes.value.find((s) => s.id === props.modelValue) ?? null,
)

async function loadSchemes() {
  loading.value = true
  try { schemes.value = await getRentSchemeList() }
  finally { loading.value = false }
}

function handleSelect(scheme: RentSchemeVO) {
  // 再次点击已选中项则取消选中
  const newVal = props.modelValue === scheme.id ? null : scheme.id
  emit('update:modelValue', newVal)
  if (newVal !== null) emit('select', scheme)
}

onMounted(loadSchemes)

/** 父组件可调用此方法刷新列表 */
defineExpose({ reload: loadSchemes })
</script>

<style scoped>
.rent-scheme-selector { width: 100%; }

.scheme-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(200px, 1fr));
  gap: 12px;
}

.scheme-card {
  position: relative;
  border: 2px solid #e4e7ed;
  border-radius: 8px;
  padding: 14px 16px;
  cursor: pointer;
  transition: all 0.2s;
  background: #fff;
  overflow: hidden;
}

.scheme-card:hover:not(.is-disabled) {
  border-color: #409eff;
  box-shadow: 0 2px 8px rgba(64, 158, 255, 0.2);
}

.scheme-card.is-selected {
  border-color: #409eff;
  background: #ecf5ff;
}

.scheme-card.is-disabled {
  cursor: not-allowed;
  opacity: 0.6;
}

.selected-badge {
  position: absolute;
  top: 0;
  right: 0;
  width: 24px;
  height: 24px;
  background: #409eff;
  border-bottom-left-radius: 8px;
  display: flex;
  align-items: center;
  justify-content: center;
  color: #fff;
  font-size: 12px;
}

.charge-tag { margin-bottom: 8px; }

.scheme-name {
  font-size: 15px;
  font-weight: 600;
  color: #303133;
  margin-bottom: 6px;
}

.scheme-meta {
  font-size: 12px;
  color: #909399;
  margin-bottom: 6px;
  display: flex;
  align-items: center;
  gap: 4px;
}

.scheme-desc {
  font-size: 12px;
  color: #606266;
  line-height: 1.5;
  display: -webkit-box;
  -webkit-box-orient: vertical;
  -webkit-line-clamp: 2;
  overflow: hidden;
}

.selected-info {
  margin-top: 12px;
  display: flex;
  align-items: center;
  gap: 4px;
  font-size: 13px;
  color: #409eff;
}

.loading-wrap { padding: 16px; }
</style>
