<template>
  <el-select
    :model-value="modelValue"
    :multiple="multiple"
    :placeholder="placeholder"
    :disabled="disabled"
    :clearable="clearable"
    filterable
    style="width: 100%"
    @update:model-value="handleChange"
  >
    <template v-if="grouped">
      <!-- 按类型分组显示 -->
      <el-option-group
        v-for="(items, type) in groupedItems"
        :key="type"
        :label="ITEM_TYPE_MAP[Number(type)]"
      >
        <el-option
          v-for="item in items"
          :key="item.id"
          :label="item.itemName"
          :value="item.id"
        >
          <div class="option-row">
            <span>{{ item.itemName }}</span>
            <el-tag v-if="item.isRequired === 1" type="danger" size="small" class="required-tag">必填</el-tag>
          </div>
        </el-option>
      </el-option-group>
    </template>

    <template v-else>
      <el-option
        v-for="item in items"
        :key="item.id"
        :label="item.itemName"
        :value="item.id"
      >
        <div class="option-row">
          <span>{{ item.itemName }}</span>
          <span class="option-type">{{ ITEM_TYPE_MAP[item.itemType] }}</span>
          <el-tag v-if="item.isRequired === 1" type="danger" size="small" class="required-tag">必填</el-tag>
        </div>
      </el-option>
    </template>
  </el-select>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { getFeeItemList, type FeeItemVO } from '@/api/inv/config'

// ─── Props & Emits ───
const props = withDefaults(defineProps<{
  modelValue?: number | number[] | null
  multiple?: boolean
  placeholder?: string
  disabled?: boolean
  clearable?: boolean
  /** 是否按类型分组显示 */
  grouped?: boolean
}>(), {
  modelValue: null,
  multiple: false,
  placeholder: '请选择收款项目',
  disabled: false,
  clearable: true,
  grouped: true,
})

const emit = defineEmits<{
  'update:modelValue': [val: number | number[] | null]
  /** 选中后抛出完整 VO（单选时） */
  'select': [item: FeeItemVO | undefined]
}>()

// ─── 字典 ───
const ITEM_TYPE_MAP: Record<number, string> = { 1: '租金类', 2: '保证金类', 3: '服务费类' }

// ─── 数据 ───
const items = ref<FeeItemVO[]>([])

const groupedItems = computed(() => {
  const groups: Record<number, FeeItemVO[]> = {}
  items.value.forEach((item) => {
    if (!groups[item.itemType]) groups[item.itemType] = []
    groups[item.itemType].push(item)
  })
  return groups
})

function handleChange(val: number | number[] | null) {
  emit('update:modelValue', val)
  if (!props.multiple) {
    const found = items.value.find((i) => i.id === (val as number))
    emit('select', found)
  }
}

onMounted(async () => {
  items.value = await getFeeItemList()
})

/** 父组件可调用刷新 */
defineExpose({
  reload: async () => { items.value = await getFeeItemList() },
  /** 获取必填项 ID 列表 */
  getRequiredIds: () => items.value.filter((i) => i.isRequired === 1).map((i) => i.id),
})
</script>

<style scoped>
.option-row {
  display: flex;
  align-items: center;
  gap: 6px;
}
.option-type {
  font-size: 12px;
  color: #909399;
}
.required-tag {
  margin-left: auto;
}
</style>
