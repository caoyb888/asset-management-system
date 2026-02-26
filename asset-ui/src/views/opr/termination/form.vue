<template>
  <div class="page-container">
    <el-card>
      <template #header>
        <el-button :icon="ArrowLeft" text @click="$router.back()">返回</el-button>
        <span>合同解约 - 新增/编辑</span>
      </template>
      <!--
        TODO 阶段五：三步流程
          Step1: 合同选择（ContractSelector 组件）
          Step2: 解约信息（类型/日期/原因）
          Step3: 自动清算结果确认（明细表格：未收租金/违约金/保证金退还）→ 提交审批
      -->
      <el-steps :active="0" finish-status="success">
        <el-step title="选择合同" />
        <el-step title="解约信息" />
        <el-step title="清算确认" />
      </el-steps>
      <el-empty description="合同解约表单 - 阶段五实现" style="margin-top:40px" />
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { ArrowLeft } from '@element-plus/icons-vue'
// TODO 阶段五实现
</script>
