<template>
  <template v-if="formFields.length > 0">
    <el-form-item
      v-for="field in formFields"
      :key="field.fieldKey"
      :label="field.fieldLabel"
      :prop="`extFields.${field.fieldKey}`"
      :rules="field.required ? [{ required: true, message: `请输入${field.fieldLabel}`, trigger: 'blur' }] : []"
    >
      <!-- 单行文本 -->
      <el-input
        v-if="field.fieldType === 'text'"
        v-model="localValue[field.fieldKey]"
        :placeholder="field.placeholder || `请输入${field.fieldLabel}`"
        :maxlength="field.maxLength"
        show-word-limit
        @change="emitUpdate"
      />

      <!-- 多行文本 -->
      <el-input
        v-else-if="field.fieldType === 'textarea'"
        v-model="localValue[field.fieldKey]"
        type="textarea"
        :rows="3"
        :placeholder="field.placeholder || `请输入${field.fieldLabel}`"
        :maxlength="field.maxLength"
        show-word-limit
        @change="emitUpdate"
      />

      <!-- 数字 -->
      <el-input-number
        v-else-if="field.fieldType === 'number'"
        v-model="localValue[field.fieldKey]"
        :placeholder="field.placeholder"
        :min="field.minVal"
        :max="field.maxVal"
        controls-position="right"
        style="width: 100%"
        @change="emitUpdate"
      />

      <!-- 日期 -->
      <el-date-picker
        v-else-if="field.fieldType === 'date'"
        v-model="localValue[field.fieldKey]"
        type="date"
        :placeholder="field.placeholder || `请选择${field.fieldLabel}`"
        value-format="YYYY-MM-DD"
        style="width: 100%"
        @change="emitUpdate"
      />

      <!-- 下拉单选 -->
      <el-select
        v-else-if="field.fieldType === 'select'"
        v-model="localValue[field.fieldKey]"
        :placeholder="field.placeholder || `请选择${field.fieldLabel}`"
        clearable
        style="width: 100%"
        @change="emitUpdate"
      >
        <el-option
          v-for="opt in field.optionsJson"
          :key="opt.value"
          :label="opt.label"
          :value="opt.value"
        />
      </el-select>

      <!-- 单选按钮 -->
      <el-radio-group
        v-else-if="field.fieldType === 'radio'"
        v-model="localValue[field.fieldKey]"
        @change="emitUpdate"
      >
        <el-radio
          v-for="opt in field.optionsJson"
          :key="opt.value"
          :value="opt.value"
        >{{ opt.label }}</el-radio>
      </el-radio-group>

      <!-- 多选 -->
      <el-checkbox-group
        v-else-if="field.fieldType === 'checkbox'"
        v-model="localValue[field.fieldKey]"
        @change="emitUpdate"
      >
        <el-checkbox
          v-for="opt in field.optionsJson"
          :key="opt.value"
          :value="opt.value"
        >{{ opt.label }}</el-checkbox>
      </el-checkbox-group>
    </el-form-item>
  </template>
</template>

<script setup lang="ts">
import { reactive, watch } from 'vue'
import { useExtFields } from '@/composables/useExtFields'

const props = defineProps<{
  moduleCode: string
  modelValue: Record<string, any>
}>()

const emit = defineEmits<{
  'update:modelValue': [value: Record<string, any>]
}>()

const { formFields } = useExtFields(props.moduleCode)

// 本地副本，与 modelValue 双向同步
const localValue = reactive<Record<string, any>>({ ...props.modelValue })

watch(
  () => props.modelValue,
  (val) => {
    if (val) {
      Object.keys(val).forEach(k => { localValue[k] = val[k] })
    }
  },
  { deep: true }
)

function emitUpdate() {
  emit('update:modelValue', { ...localValue })
}
</script>
