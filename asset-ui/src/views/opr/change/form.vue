<template>
  <div class="page-container">
    <el-card>
      <template #header>
        <el-button :icon="ArrowLeft" text @click="$router.back()">返回</el-button>
        <span>合同变更 - 新增/编辑</span>
      </template>
      <!--
        TODO 阶段二：四步流程
          Step1: 合同选择（ContractSelector 组件）
          Step2: 变更类型多选（Checkbox，RENT/BRAND/TENANT/FEE/CLAUSE/TERM/AREA/COMPANY）
          Step3: 动态表单（按类型渲染对应字段）
          Step4: 影响预览确认（变更前后对比表 + 受影响应收差异汇总）
      -->
      <el-steps :active="0" finish-status="success">
        <el-step title="选择合同" />
        <el-step title="变更类型" />
        <el-step title="填写内容" />
        <el-step title="影响预览" />
      </el-steps>
      <el-empty description="合同变更表单 - 阶段二实现" style="margin-top:40px" />
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { ArrowLeft } from '@element-plus/icons-vue'
// TODO 阶段二实现
</script>
