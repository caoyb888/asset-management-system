<template>
  <div class="page-container">
    <el-card>
      <template #header>
        <div style="display:flex;justify-content:space-between;align-items:center">
          <span>营收填报</span>
          <div>
            <el-button :icon="Upload">导入</el-button>
            <el-button :icon="Download">导出</el-button>
            <el-button type="primary" :icon="Plus" @click="$router.push('/opr/revenue-reports/form')">新增填报</el-button>
          </div>
        </div>
      </template>
      <!--
        TODO 阶段三：日期范围筛选 + 导入/导出按钮 + 状态过滤
        日历视图：按月展示，已填报日期标绿，缺失日期标红
      -->
      <el-empty description="营收填报列表 - 阶段三实现" />
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { Plus, Upload, Download } from '@element-plus/icons-vue'
// TODO 阶段三实现：调用 GET /api/opr/revenue-reports 接口
</script>
