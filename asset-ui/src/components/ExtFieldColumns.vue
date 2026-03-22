<template>
  <el-table-column
    v-for="field in listFields"
    :key="field.fieldKey"
    :label="field.fieldLabel"
    :prop="`extFields.${field.fieldKey}`"
    min-width="120"
  >
    <template #default="{ row }">
      <span>{{ formatValue(field, row.extFields?.[field.fieldKey]) }}</span>
    </template>
  </el-table-column>
</template>

<script setup lang="ts">
import { useExtFields } from '@/composables/useExtFields'
import type { ExtFieldDef } from '@/api/sys/extField'

const props = defineProps<{
  moduleCode: string
}>()

const { listFields } = useExtFields(props.moduleCode)

function formatValue(field: ExtFieldDef, value: any): string {
  if (value === null || value === undefined || value === '') return '-'

  if (field.fieldType === 'checkbox') {
    // 数组转逗号分隔标签
    const arr = Array.isArray(value) ? value : [value]
    if (!field.optionsJson?.length) return arr.join(', ')
    return arr
      .map(v => field.optionsJson!.find(o => o.value === v)?.label ?? v)
      .join(', ')
  }

  if (field.fieldType === 'select' || field.fieldType === 'radio') {
    const opt = field.optionsJson?.find(o => o.value === String(value))
    return opt?.label ?? String(value)
  }

  return String(value)
}
</script>
