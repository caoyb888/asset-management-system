<template>
  <div class="page-container">
    <el-card>
      <template #header>
        <div class="card-header">
          <el-button :icon="ArrowLeft" text @click="router.back()">返回</el-button>
          <span>意向转合同</span>
          <div />
        </div>
      </template>

      <!-- 意向协议预览 -->
      <el-alert v-if="intentionId" title="以下为意向协议数据预览，请补录合同专属信息后提交" type="info" :closable="false" class="mb-4" />

      <el-form :model="form" label-width="120px" class="form-body">
        <el-divider content-position="left">关联意向</el-divider>
        <el-form-item label="意向编号">
          <el-input :value="intentionId ? `来自意向 #${intentionId}` : ''" disabled />
        </el-form-item>

        <el-divider content-position="left">合同专属信息</el-divider>
        <el-form-item label="合同类型" required>
          <el-select v-model="form.contractType" placeholder="请选择" style="width: 200px">
            <el-option label="标准租赁合同" :value="1" />
            <el-option label="临时租赁合同" :value="2" />
            <el-option label="补充协议" :value="3" />
          </el-select>
        </el-form-item>
        <el-form-item label="合同名称" required>
          <el-input v-model="form.contractName" placeholder="请输入合同名称" style="width: 360px" />
        </el-form-item>
      </el-form>

      <div class="form-actions">
        <el-button type="primary" @click="handleSubmit">确认转合同</el-button>
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
const intentionId = computed(() => route.query.intentionId)

const form = ref({ contractType: undefined as number | undefined, contractName: '' })

function handleSubmit() {
  ElMessage.success('转合同成功')
  router.push('/inv/contracts')
}
</script>

<style scoped>
.card-header { display: flex; justify-content: space-between; align-items: center; }
.mb-4 { margin-bottom: 16px; }
.form-body { max-width: 700px; }
.form-actions { margin-top: 24px; display: flex; gap: 12px; justify-content: center; }
</style>
