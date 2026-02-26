<template>
  <div class="page-container">
    <el-card>
      <template #header>
        <div style="display:flex;justify-content:space-between;align-items:center">
          <span>合同解约</span>
          <el-button type="primary" :icon="Plus" @click="$router.push('/opr/terminations/form')">新增解约</el-button>
        </div>
      </template>
      <!--
        TODO 阶段五：
        - 状态筛选
        - 解约类型标签（到期/提前/重签不同颜色）
      -->
      <el-empty description="合同解约列表 - 阶段五实现" />
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { Plus } from '@element-plus/icons-vue'
// TODO 阶段五实现：调用 GET /api/opr/terminations 接口
</script>
