<template>
  <div class="page-container">
    <el-card>
      <template #header>
        <el-button :icon="ArrowLeft" text @click="$router.back()">返回</el-button>
        <span>台账详情</span>
      </template>
      <!-- TODO 阶段一：Tab布局（合同信息/应收计划/双签管理/审核记录），各Tab懒加载 -->
      <el-tabs>
        <el-tab-pane label="合同信息"><el-empty description="待实现" /></el-tab-pane>
        <el-tab-pane label="应收计划"><el-empty description="待实现" /></el-tab-pane>
        <el-tab-pane label="双签管理"><el-empty description="待实现" /></el-tab-pane>
        <el-tab-pane label="审核记录"><el-empty description="待实现" /></el-tab-pane>
      </el-tabs>
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { ArrowLeft } from '@element-plus/icons-vue'
// TODO 阶段一实现：调用 GET /api/opr/ledgers/:id 接口
</script>
