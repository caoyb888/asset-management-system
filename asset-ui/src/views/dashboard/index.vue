<template>
  <div class="dashboard">
    <!-- 统计卡片 -->
    <el-row :gutter="16" class="stat-cards">
      <el-col v-for="card in statCards" :key="card.label" :span="6">
        <el-card shadow="never" class="stat-card">
          <div class="stat-content">
            <div class="stat-info">
              <p class="stat-label">{{ card.label }}</p>
              <p class="stat-value">{{ card.value }}</p>
              <p class="stat-desc" :class="card.trend > 0 ? 'up' : 'down'">
                较上月 {{ card.trend > 0 ? '+' : '' }}{{ card.trend }}%
              </p>
            </div>
            <el-icon class="stat-icon" :style="{ color: card.color }">
              <component :is="card.icon" />
            </el-icon>
          </div>
        </el-card>
      </el-col>
    </el-row>

    <!-- 待办事项 -->
    <el-row :gutter="16" class="content-row">
      <el-col :span="12">
        <el-card shadow="never" header="待办事项">
          <el-empty description="暂无待办事项" :image-size="80" />
        </el-card>
      </el-col>
      <el-col :span="12">
        <el-card shadow="never" header="系统公告">
          <el-empty description="暂无公告" :image-size="80" />
        </el-card>
      </el-col>
    </el-row>
  </div>
</template>

<script setup lang="ts">
import { reactive } from 'vue'
import { OfficeBuilding, ShoppingCart, Money, TrendCharts } from '@element-plus/icons-vue'
import { useAppStore } from '@/store/modules/app'

const appStore = useAppStore()
appStore.setPageTitle('工作台')

const statCards = reactive([
  { label: '项目总数', value: '--', trend: 0, icon: OfficeBuilding, color: '#2e75b6' },
  { label: '在租商铺', value: '--', trend: 0, icon: ShoppingCart, color: '#67c23a' },
  { label: '本月应收', value: '--', trend: 0, icon: Money, color: '#e6a23c' },
  { label: '出租率', value: '--%', trend: 0, icon: TrendCharts, color: '#f56c6c' },
])
</script>

<style scoped lang="scss">
.dashboard {
  .stat-cards {
    margin-bottom: 16px;
  }

  .stat-card {
    .stat-content {
      display: flex;
      align-items: center;
      justify-content: space-between;
    }

    .stat-label {
      margin: 0 0 8px;
      font-size: 13px;
      color: #909399;
    }

    .stat-value {
      margin: 0 0 6px;
      font-size: 28px;
      font-weight: 700;
      color: #303133;
    }

    .stat-desc {
      margin: 0;
      font-size: 12px;

      &.up {
        color: #67c23a;
      }

      &.down {
        color: #f56c6c;
      }
    }

    .stat-icon {
      font-size: 48px;
      opacity: 0.15;
    }
  }

  .content-row {
    :deep(.el-card__header) {
      font-weight: 600;
    }
  }
}
</style>
