<template>
  <div class="page-container">
    <el-card>
      <template #header>
        <div style="display:flex;justify-content:space-between;align-items:center">
          <span>客流填报</span>
          <div>
            <el-button :icon="Upload">导入</el-button>
            <el-button :icon="Download">导出</el-button>
            <el-button type="primary" :icon="Plus">新增填报</el-button>
          </div>
        </div>
      </template>
      <!--
        TODO 阶段四：
        - 日期/项目筛选
        - 折线趋势图（Echarts）展示历史客流走势
        - 三级联动选择器（项目→楼栋→楼层）
      -->
      <el-empty description="客流填报列表 - 阶段四实现" />
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { Plus, Upload, Download } from '@element-plus/icons-vue'
// TODO 阶段四实现：调用 GET /api/opr/passenger-flows 接口
</script>
