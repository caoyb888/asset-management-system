<template>
  <div class="page-container">
    <el-card>
      <template #header>
        <div class="card-header">
          <el-button :icon="ArrowLeft" text @click="router.back()">返回</el-button>
          <span>{{ isEdit ? '编辑意向协议' : '新增意向协议' }}</span>
          <div />
        </div>
      </template>

      <!-- 向导步骤条 -->
      <el-steps :active="currentStep" align-center class="mb-6" finish-status="success">
        <el-step title="计租方案" />
        <el-step title="基础信息" />
        <el-step title="商务信息" />
        <el-step title="费项配置" />
        <el-step title="分铺计租" />
        <el-step title="费用生成" />
        <el-step title="账期设置" />
      </el-steps>

      <!-- 步骤内容占位 -->
      <div class="step-content">
        <el-empty :description="`步骤 ${currentStep + 1} 开发中`" />
      </div>

      <!-- 步骤按钮 -->
      <div class="step-actions">
        <el-button v-if="currentStep > 0" @click="currentStep--">上一步</el-button>
        <el-button type="primary" @click="handleNext">
          {{ currentStep < 6 ? '下一步' : '提交' }}
        </el-button>
        <el-button @click="handleSaveDraft">暂存</el-button>
      </div>
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { ref, computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { ArrowLeft } from '@element-plus/icons-vue'
import { useIntentionStore } from '@/store/modules/inv/intention'

const route = useRoute()
const router = useRouter()
const intentionStore = useIntentionStore()

const isEdit = computed(() => !!route.query.id)
const currentStep = ref(0)

function handleNext() {
  if (currentStep.value < 6) {
    currentStep.value++
  } else {
    ElMessage.success('意向协议提交成功')
    router.push('/inv/intentions')
  }
}

function handleSaveDraft() {
  ElMessage.success('已暂存')
}
</script>

<style scoped>
.card-header { display: flex; justify-content: space-between; align-items: center; }
.mb-6 { margin-bottom: 24px; }
.step-content { min-height: 300px; display: flex; align-items: center; justify-content: center; }
.step-actions { display: flex; gap: 12px; justify-content: center; margin-top: 24px; }
</style>
