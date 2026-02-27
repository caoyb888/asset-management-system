<template>
  <el-select
    :model-value="modelValue"
    filterable
    remote
    clearable
    :remote-method="handleRemoteSearch"
    :loading="loading"
    :disabled="disabled"
    :placeholder="placeholder"
    value-key="id"
    style="width: 100%"
    @change="handleChange"
    @clear="handleClear"
  >
    <el-option
      v-for="item in options"
      :key="item.id"
      :value="item.id"
      :label="optionLabel(item)"
    >
      <div class="selector-option">
        <div class="selector-option__main">
          <el-tag size="small" type="info" style="margin-right:6px">{{ item.ledgerCode }}</el-tag>
          <span class="merchant-name">{{ item.merchantName || '—' }}</span>
        </div>
        <div class="selector-option__sub">
          <span v-if="item.contractCode">合同号：{{ item.contractCode }}</span>
          <el-divider direction="vertical" v-if="item.contractCode && item.shopCode" />
          <span v-if="item.shopCode">商铺：{{ item.shopCode }}</span>
          <el-divider direction="vertical" v-if="item.contractStart" />
          <span v-if="item.contractStart">
            {{ item.contractStart }} 至 {{ item.contractEnd || '—' }}
          </span>
        </div>
      </div>
    </el-option>

    <!-- 无搜索结果时的空状态 -->
    <template #empty>
      <div style="padding: 10px 12px; color: #909399; font-size: 13px; text-align: center;">
        {{ loading ? '搜索中...' : '暂无匹配台账' }}
      </div>
    </template>
  </el-select>
</template>

<script setup lang="ts">
import { ref } from 'vue'
import { searchLedgers, type LedgerSelectorVO } from '@/api/opr/ledger'

// ─── Props ────────────────────────────────────────────────
const props = withDefaults(defineProps<{
  /** 当前选中的台账ID（v-model） */
  modelValue?: number | null
  /** 是否禁用 */
  disabled?: boolean
  /** 输入框占位文本 */
  placeholder?: string
}>(), {
  modelValue: null,
  disabled: false,
  placeholder: '请输入台账编号/合同编号/商家名搜索',
})

// ─── Emits ────────────────────────────────────────────────
const emit = defineEmits<{
  /** v-model 更新 */
  (e: 'update:modelValue', val: number | null): void
  /** 选中完整 VO 对象（方便父组件取商家名等信息） */
  (e: 'select', vo: LedgerSelectorVO | null): void
}>()

// ─── 状态 ─────────────────────────────────────────────────
const loading = ref(false)
const options = ref<LedgerSelectorVO[]>([])
let debounceTimer: ReturnType<typeof setTimeout> | null = null

// ─── 搜索（防抖500ms） ─────────────────────────────────────
function handleRemoteSearch(keyword: string) {
  if (debounceTimer) clearTimeout(debounceTimer)
  debounceTimer = setTimeout(async () => {
    loading.value = true
    try {
      const res = await searchLedgers(keyword.trim() || undefined, 10)
      options.value = (res.data as any)?.data ?? res.data ?? []
    } catch {
      options.value = []
    } finally {
      loading.value = false
    }
  }, 500)
}

// ─── 事件处理 ─────────────────────────────────────────────
function handleChange(id: number | null) {
  emit('update:modelValue', id)
  const vo = id != null ? options.value.find(o => o.id === id) ?? null : null
  emit('select', vo)
}

function handleClear() {
  emit('update:modelValue', null)
  emit('select', null)
  options.value = []
}

// ─── 工具 ─────────────────────────────────────────────────
function optionLabel(item: LedgerSelectorVO): string {
  return `${item.ledgerCode}${item.merchantName ? ' · ' + item.merchantName : ''}`
}
</script>

<style scoped>
.selector-option {
  padding: 2px 0;
}

.selector-option__main {
  display: flex;
  align-items: center;
  font-size: 14px;
  line-height: 1.5;
}

.merchant-name {
  font-weight: 600;
  color: #303133;
}

.selector-option__sub {
  display: flex;
  align-items: center;
  gap: 4px;
  font-size: 12px;
  color: #909399;
  margin-top: 2px;
}
</style>
