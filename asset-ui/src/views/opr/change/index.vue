<template>
  <div class="page-container">
    <el-card>
      <template #header>
        <div style="display:flex;justify-content:space-between;align-items:center">
          <span>合同变更</span>
          <el-button type="primary" :icon="Plus" @click="$router.push('/opr/contract-changes/form')">新增变更</el-button>
        </div>
      </template>
      <!-- TODO 阶段二：状态筛选 + 变更类型标签（多值来自 opr_contract_change_type） -->
      <el-empty description="合同变更列表 - 阶段二实现" />
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { Plus } from '@element-plus/icons-vue'
// TODO 阶段二实现：调用 GET /api/opr/contract-changes 接口
</script>
