<template>
  <el-dialog
    :model-value="visible"
    :title="title"
    width="480px"
    :close-on-click-modal="false"
    @update:model-value="emit('update:visible', $event)"
    @open="onOpen"
    @closed="resetForm"
  >
    <el-form :model="form" label-width="90px" class="approval-form">
      <!-- 审批人选择 -->
      <el-form-item label="审批人">
        <el-select
          v-model="form.approverIds"
          multiple
          filterable
          clearable
          placeholder="请选择审批人（可多选，不选则直接提交）"
          style="width: 100%"
          :loading="usersLoading"
        >
          <el-option
            v-for="u in userOptions"
            :key="u.id"
            :label="u.realName || u.username"
            :value="u.id"
          >
            <span class="option-name">{{ u.realName || u.username }}</span>
            <span class="option-account">{{ u.username }}</span>
          </el-option>
        </el-select>
        <div class="field-tip">审批引擎集成后将强制要求选择审批人</div>
      </el-form-item>

      <!-- 审批备注 -->
      <el-form-item label="审批备注">
        <el-input
          v-model="form.comment"
          type="textarea"
          :rows="3"
          placeholder="填写发起审批的说明（可选）"
          maxlength="200"
          show-word-limit
        />
      </el-form-item>
    </el-form>

    <template #footer>
      <el-button @click="emit('update:visible', false)">取消</el-button>
      <el-button type="primary" :loading="loading" @click="handleConfirm">
        确认提交
      </el-button>
    </template>
  </el-dialog>
</template>

<script setup lang="ts">
import { ref, reactive } from 'vue'
import { getUserList, type UserOption } from '@/api/base/user'

// ─── Props & Emits ────────────────────────────────────────
const props = withDefaults(defineProps<{
  /** 控制弹窗显示 */
  visible: boolean
  /** 弹窗标题 */
  title?: string
  /** 外部提交中状态 */
  loading?: boolean
}>(), {
  title: '提交审批',
  loading: false,
})

const emit = defineEmits<{
  'update:visible': [value: boolean]
  /**
   * 点击「确认提交」时触发
   * @param payload.approverIds 选中的审批人 ID 列表（可为空）
   * @param payload.comment 审批备注
   */
  'confirm': [payload: { approverIds: number[]; comment: string }]
}>()

// ─── 用户列表 ──────────────────────────────────────────────
const usersLoading = ref(false)
const userOptions = ref<UserOption[]>([])

async function loadUsers() {
  if (userOptions.value.length > 0) return  // 已加载过则跳过
  usersLoading.value = true
  try {
    userOptions.value = await getUserList()
  } catch {
    // 加载失败不阻断流程，用户可不选审批人直接提交
  } finally {
    usersLoading.value = false
  }
}

// ─── 表单数据 ──────────────────────────────────────────────
const form = reactive({
  approverIds: [] as number[],
  comment: '',
})

function resetForm() {
  form.approverIds = []
  form.comment = ''
}

// ─── 弹窗打开时加载用户 ────────────────────────────────────
function onOpen() {
  loadUsers()
}

// ─── 确认提交 ──────────────────────────────────────────────
function handleConfirm() {
  emit('confirm', {
    approverIds: [...form.approverIds],
    comment: form.comment.trim(),
  })
}
</script>

<style scoped>
.approval-form {
  padding: 8px 0;
}

.option-name {
  font-size: 14px;
  color: #303133;
}

.option-account {
  float: right;
  font-size: 12px;
  color: #909399;
}

.field-tip {
  font-size: 12px;
  color: #909399;
  margin-top: 4px;
  line-height: 1.4;
}
</style>
