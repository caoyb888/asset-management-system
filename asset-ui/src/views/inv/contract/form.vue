<template>
  <div class="page-container">
    <el-card>
      <template #header>
        <div class="card-header">
          <el-button :icon="ArrowLeft" text @click="router.back()">返回</el-button>
          <span>{{ isEdit ? '编辑合同' : '新增合同' }}</span>
          <div />
        </div>
      </template>

      <el-tabs v-model="activeTab">
        <el-tab-pane label="基本信息" name="basic">
          <el-empty description="基本信息 - 开发中" />
        </el-tab-pane>
        <el-tab-pane label="费项配置" name="fees">
          <el-empty description="费项配置 - 开发中" />
        </el-tab-pane>
        <el-tab-pane label="账期列表" name="billing">
          <el-empty description="账期列表 - 开发中" />
        </el-tab-pane>
      </el-tabs>

      <div class="form-actions">
        <el-button type="primary" @click="handleSave">保存</el-button>
        <el-button @click="router.back()">取消</el-button>
      </div>
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { ref, computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { ArrowLeft } from '@element-plus/icons-vue'

const route = useRoute()
const router = useRouter()
const isEdit = computed(() => !!route.query.id)
const activeTab = ref('basic')

function handleSave() {
  ElMessage.success('保存成功')
  router.push('/inv/contracts')
}
</script>

<style scoped>
.card-header { display: flex; justify-content: space-between; align-items: center; }
.form-actions { margin-top: 24px; display: flex; gap: 12px; justify-content: center; }
</style>
