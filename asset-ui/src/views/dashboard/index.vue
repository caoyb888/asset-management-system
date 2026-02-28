<template>
  <div class="dashboard">
    <!-- 顶部欢迎横幅 -->
    <div class="welcome-banner">
      <div class="welcome-left">
        <div class="welcome-avatar">{{ nickname.charAt(0) }}</div>
        <div>
          <div class="welcome-greeting">{{ greeting }}，{{ nickname }} <span class="wave">👋</span></div>
          <div class="welcome-date">{{ dateStr }} · {{ weatherTip }}</div>
        </div>
      </div>
      <div class="welcome-right">
        <div class="quick-action" @click="goTo('/opr/ledgers')">
          <el-icon><Memo /></el-icon>合同台账
        </div>
        <div class="quick-action" @click="goTo('/fin/receipts')">
          <el-icon><CreditCard /></el-icon>收款录入
        </div>
        <div class="quick-action primary" @click="goTo('/inv/intentions')">
          <el-icon><EditPen /></el-icon>新增意向
        </div>
      </div>
    </div>

    <!-- 核心指标卡片 -->
    <el-row :gutter="16" class="stat-row">
      <el-col v-for="card in statCards" :key="card.label" :xs="12" :sm="12" :md="6">
        <div class="stat-card" :style="{ '--card-color': card.color, '--card-bg': card.bg }">
          <div class="stat-card-body">
            <div class="stat-icon-wrap">
              <el-icon class="stat-icon"><component :is="card.icon" /></el-icon>
            </div>
            <div class="stat-info">
              <div class="stat-label">{{ card.label }}</div>
              <div class="stat-value">{{ card.value }}</div>
            </div>
          </div>
          <div class="stat-footer">
            <span class="stat-trend" :class="card.trend >= 0 ? 'up' : 'down'">
              <el-icon v-if="card.trend >= 0"><Top /></el-icon>
              <el-icon v-else><Bottom /></el-icon>
              较上月 {{ Math.abs(card.trend) }}%
            </span>
            <span class="stat-tag">{{ card.tag }}</span>
          </div>
        </div>
      </el-col>
    </el-row>

    <!-- 中部：功能模块快捷入口 + 待办事项 -->
    <el-row :gutter="16" class="mid-row">
      <!-- 功能模块入口 -->
      <el-col :span="16">
        <div class="panel">
          <div class="panel-header">
            <span class="panel-title">功能模块</span>
            <span class="panel-sub">快速跳转</span>
          </div>
          <div class="module-grid">
            <div
              v-for="mod in modules"
              :key="mod.name"
              class="module-card"
              :style="{ '--mod-color': mod.color }"
              @click="goTo(mod.path)"
            >
              <div class="module-icon-wrap">
                <el-icon class="module-icon"><component :is="mod.icon" /></el-icon>
              </div>
              <div class="module-name">{{ mod.name }}</div>
              <div class="module-desc">{{ mod.desc }}</div>
            </div>
          </div>
        </div>
      </el-col>

      <!-- 待办事项 -->
      <el-col :span="8">
        <div class="panel" style="height:100%">
          <div class="panel-header">
            <span class="panel-title">待办事项</span>
            <el-badge :value="todoList.length" class="badge-item" type="danger">
              <span class="panel-sub">全部</span>
            </el-badge>
          </div>
          <div v-if="todoList.length" class="todo-list">
            <div v-for="item in todoList" :key="item.id" class="todo-item" @click="goTo(item.path)">
              <div class="todo-dot" :class="item.type" />
              <div class="todo-content">
                <div class="todo-title">{{ item.title }}</div>
                <div class="todo-time">{{ item.time }}</div>
              </div>
              <el-icon class="todo-arrow"><ArrowRight /></el-icon>
            </div>
          </div>
          <div v-else class="panel-empty">
            <el-icon class="empty-icon"><CircleCheck /></el-icon>
            <p>暂无待办事项</p>
          </div>
        </div>
      </el-col>
    </el-row>

    <!-- 底部：系统公告 + 系统概览 -->
    <el-row :gutter="16" class="bottom-row">
      <!-- 系统公告 -->
      <el-col :span="12">
        <div class="panel">
          <div class="panel-header">
            <span class="panel-title">系统公告</span>
            <span class="panel-sub link" @click="goTo('/base/notices')">更多 →</span>
          </div>
          <div v-if="notices.length" class="notice-list">
            <div v-for="n in notices" :key="n.id" class="notice-item">
              <el-tag :type="n.type" size="small" class="notice-tag">{{ n.badge }}</el-tag>
              <span class="notice-title">{{ n.title }}</span>
              <span class="notice-date">{{ n.date }}</span>
            </div>
          </div>
          <div v-else class="panel-empty">
            <el-icon class="empty-icon"><Bell /></el-icon>
            <p>暂无公告</p>
          </div>
        </div>
      </el-col>

      <!-- 系统概览 -->
      <el-col :span="12">
        <div class="panel">
          <div class="panel-header">
            <span class="panel-title">系统概览</span>
          </div>
          <div class="overview-list">
            <div v-for="item in overview" :key="item.label" class="overview-item">
              <span class="overview-label">{{ item.label }}</span>
              <div class="overview-bar-wrap">
                <div class="overview-bar">
                  <div class="overview-bar-fill" :style="{ width: item.percent + '%', background: item.color }" />
                </div>
                <span class="overview-val">{{ item.val }}</span>
              </div>
            </div>
          </div>
        </div>
      </el-col>
    </el-row>
  </div>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import { useRouter } from 'vue-router'
import {
  OfficeBuilding, ShoppingCart, Money, TrendCharts,
  Memo, CreditCard, EditPen, Tickets, Operation,
  DataAnalysis, Top, Bottom, ArrowRight, CircleCheck, Bell,
  Grid, Switch,
} from '@element-plus/icons-vue'
import { useAppStore } from '@/store/modules/app'
import { useUserStore } from '@/store/modules/user'

const appStore = useAppStore()
const userStore = useUserStore()
const router = useRouter()
appStore.setPageTitle('工作台')

const nickname = computed(() => userStore.nickname || '用户')

// 问候语
const hour = new Date().getHours()
const greeting = hour < 12 ? '上午好' : hour < 18 ? '下午好' : '晚上好'

// 日期
const now = new Date()
const weekMap = ['日', '一', '二', '三', '四', '五', '六']
const dateStr = `${now.getFullYear()}年${now.getMonth() + 1}月${now.getDate()}日 星期${weekMap[now.getDay()]}`
const weatherTip = '祝您工作愉快'

function goTo(path: string) {
  router.push(path)
}

// 统计卡片
const statCards = [
  {
    label: '项目总数',
    value: '--',
    trend: 0,
    tag: '个园区项目',
    icon: OfficeBuilding,
    color: '#3b82f6',
    bg: 'rgba(59,130,246,0.08)',
  },
  {
    label: '在租商铺',
    value: '--',
    trend: 0,
    tag: '间商铺在租',
    icon: ShoppingCart,
    color: '#10b981',
    bg: 'rgba(16,185,129,0.08)',
  },
  {
    label: '本月应收',
    value: '--',
    trend: 0,
    tag: '元待收款项',
    icon: Money,
    color: '#f59e0b',
    bg: 'rgba(245,158,11,0.08)',
  },
  {
    label: '综合出租率',
    value: '--%',
    trend: 0,
    tag: '商业占用率',
    icon: TrendCharts,
    color: '#8b5cf6',
    bg: 'rgba(139,92,246,0.08)',
  },
]

// 功能模块
const modules = [
  { name: '基础数据', desc: '项目楼栋商铺管理', path: '/base/projects', icon: Grid, color: '#3b82f6' },
  { name: '招商管理', desc: '意向协议合同签约', path: '/inv/contracts', icon: Tickets, color: '#10b981' },
  { name: '营运管理', desc: '合同台账变更解约', path: '/opr/ledgers', icon: Operation, color: '#f59e0b' },
  { name: '财务管理', desc: '应收收款核销凭证', path: '/fin/dashboard', icon: DataAnalysis, color: '#8b5cf6' },
  { name: '合同台账', desc: '台账生成推送管理', path: '/opr/ledgers', icon: Memo, color: '#06b6d4' },
  { name: '合同变更', desc: '变更申请审批流程', path: '/opr/contract-changes', icon: Switch, color: '#ec4899' },
  { name: '应收管理', desc: '账单明细欠费统计', path: '/fin/receivables', icon: Money, color: '#ef4444' },
  { name: '收款录入', desc: '收款单核销管理', path: '/fin/receipts', icon: CreditCard, color: '#84cc16' },
]

// 待办事项（演示数据，实际应从接口获取）
const todoList: { id: number; title: string; time: string; type: string; path: string }[] = []

// 公告（演示数据）
const notices: { id: number; title: string; date: string; badge: string; type: 'primary' | 'success' | 'warning' | 'danger' | 'info' }[] = []

// 系统概览
const overview = [
  { label: '商铺出租率', val: '--%', percent: 0, color: '#3b82f6' },
  { label: '合同履约率', val: '--%', percent: 0, color: '#10b981' },
  { label: '本月回款率', val: '--%', percent: 0, color: '#f59e0b' },
  { label: '审批通过率', val: '--%', percent: 0, color: '#8b5cf6' },
]
</script>

<style scoped lang="scss">
.dashboard {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

/* 欢迎横幅 */
.welcome-banner {
  display: flex;
  align-items: center;
  justify-content: space-between;
  background: linear-gradient(135deg, #1e3a5f 0%, #1d4ed8 50%, #2563eb 100%);
  border-radius: 14px;
  padding: 24px 32px;
  position: relative;
  overflow: hidden;

  &::before {
    content: '';
    position: absolute;
    right: -60px;
    top: -60px;
    width: 260px;
    height: 260px;
    border-radius: 50%;
    background: rgba(255, 255, 255, 0.06);
  }

  &::after {
    content: '';
    position: absolute;
    right: 80px;
    bottom: -80px;
    width: 180px;
    height: 180px;
    border-radius: 50%;
    background: rgba(255, 255, 255, 0.04);
  }
}

.welcome-left {
  display: flex;
  align-items: center;
  gap: 16px;
  position: relative;
  z-index: 1;
}

.welcome-avatar {
  width: 48px;
  height: 48px;
  border-radius: 50%;
  background: rgba(255, 255, 255, 0.2);
  border: 2px solid rgba(255, 255, 255, 0.4);
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 20px;
  font-weight: 700;
  color: #fff;
}

.welcome-greeting {
  font-size: 18px;
  font-weight: 600;
  color: #fff;
  margin-bottom: 4px;

  .wave {
    font-size: 16px;
  }
}

.welcome-date {
  font-size: 13px;
  color: rgba(255, 255, 255, 0.7);
}

.welcome-right {
  display: flex;
  align-items: center;
  gap: 10px;
  position: relative;
  z-index: 1;
}

.quick-action {
  display: flex;
  align-items: center;
  gap: 6px;
  padding: 8px 16px;
  border-radius: 8px;
  background: rgba(255, 255, 255, 0.12);
  border: 1px solid rgba(255, 255, 255, 0.2);
  color: rgba(255, 255, 255, 0.9);
  font-size: 13px;
  cursor: pointer;
  transition: all 0.2s;
  white-space: nowrap;

  &:hover {
    background: rgba(255, 255, 255, 0.2);
  }

  &.primary {
    background: rgba(255, 255, 255, 0.9);
    color: #1d4ed8;
    font-weight: 600;

    &:hover {
      background: #fff;
    }
  }
}

/* 统计卡片 */
.stat-row {
  margin-bottom: 0;
}

.stat-card {
  background: #fff;
  border-radius: 14px;
  padding: 20px;
  border: 1px solid rgba(0, 0, 0, 0.06);
  transition: transform 0.2s, box-shadow 0.2s;
  cursor: default;

  &:hover {
    transform: translateY(-2px);
    box-shadow: 0 8px 24px rgba(0, 0, 0, 0.1);
  }
}

.stat-card-body {
  display: flex;
  align-items: center;
  gap: 16px;
  margin-bottom: 16px;
}

.stat-icon-wrap {
  width: 52px;
  height: 52px;
  border-radius: 14px;
  background: var(--card-bg);
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}

.stat-icon {
  font-size: 26px;
  color: var(--card-color);
}

.stat-label {
  font-size: 13px;
  color: #909399;
  margin-bottom: 6px;
}

.stat-value {
  font-size: 28px;
  font-weight: 700;
  color: #1e293b;
  line-height: 1;
}

.stat-footer {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding-top: 12px;
  border-top: 1px solid #f1f5f9;
}

.stat-trend {
  display: flex;
  align-items: center;
  gap: 3px;
  font-size: 12px;

  &.up { color: #10b981; }
  &.down { color: #ef4444; }
}

.stat-tag {
  font-size: 11px;
  color: #94a3b8;
}

/* 通用 Panel */
.panel {
  background: #fff;
  border-radius: 14px;
  padding: 20px;
  border: 1px solid rgba(0, 0, 0, 0.06);
}

.panel-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 20px;
}

.panel-title {
  font-size: 15px;
  font-weight: 600;
  color: #1e293b;
}

.panel-sub {
  font-size: 12px;
  color: #94a3b8;

  &.link {
    cursor: pointer;
    color: #3b82f6;

    &:hover { text-decoration: underline; }
  }
}

.panel-empty {
  text-align: center;
  padding: 30px 0;
  color: #94a3b8;

  .empty-icon {
    font-size: 36px;
    margin-bottom: 8px;
    display: block;
  }

  p {
    margin: 0;
    font-size: 13px;
  }
}

/* 功能模块网格 */
.module-grid {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 12px;
}

.module-card {
  display: flex;
  flex-direction: column;
  align-items: center;
  padding: 16px 12px;
  border-radius: 12px;
  border: 1px solid rgba(0, 0, 0, 0.06);
  cursor: pointer;
  transition: all 0.2s;
  background: #fafafa;

  &:hover {
    border-color: var(--mod-color);
    background: rgba(59, 130, 246, 0.04);
    transform: translateY(-2px);
    box-shadow: 0 4px 16px rgba(0, 0, 0, 0.08);

    .module-icon-wrap {
      background: var(--mod-color);

      .module-icon {
        color: #fff;
      }
    }
  }
}

.module-icon-wrap {
  width: 44px;
  height: 44px;
  border-radius: 12px;
  background: rgba(0, 0, 0, 0.06);
  display: flex;
  align-items: center;
  justify-content: center;
  margin-bottom: 10px;
  transition: background 0.2s;
}

.module-icon {
  font-size: 22px;
  color: var(--mod-color);
  transition: color 0.2s;
}

.module-name {
  font-size: 13px;
  font-weight: 600;
  color: #334155;
  margin-bottom: 3px;
}

.module-desc {
  font-size: 11px;
  color: #94a3b8;
  text-align: center;
  line-height: 1.4;
}

/* 待办事项 */
.todo-list {
  display: flex;
  flex-direction: column;
  gap: 2px;
}

.todo-item {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 10px 8px;
  border-radius: 8px;
  cursor: pointer;
  transition: background 0.15s;

  &:hover {
    background: #f8fafc;
  }
}

.todo-dot {
  width: 8px;
  height: 8px;
  border-radius: 50%;
  flex-shrink: 0;

  &.danger { background: #ef4444; }
  &.warning { background: #f59e0b; }
  &.primary { background: #3b82f6; }
  &.success { background: #10b981; }
}

.todo-content {
  flex: 1;
  min-width: 0;
}

.todo-title {
  font-size: 13px;
  color: #334155;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.todo-time {
  font-size: 11px;
  color: #94a3b8;
  margin-top: 2px;
}

.todo-arrow {
  font-size: 12px;
  color: #cbd5e1;
}

/* 公告列表 */
.notice-list {
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.notice-item {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 10px 0;
  border-bottom: 1px solid #f1f5f9;

  &:last-child {
    border-bottom: none;
  }
}

.notice-tag {
  flex-shrink: 0;
}

.notice-title {
  flex: 1;
  font-size: 13px;
  color: #334155;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.notice-date {
  font-size: 11px;
  color: #94a3b8;
  flex-shrink: 0;
}

/* 系统概览 */
.overview-list {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.overview-item {
  display: flex;
  align-items: center;
  gap: 12px;
}

.overview-label {
  width: 90px;
  font-size: 13px;
  color: #64748b;
  flex-shrink: 0;
}

.overview-bar-wrap {
  flex: 1;
  display: flex;
  align-items: center;
  gap: 10px;
}

.overview-bar {
  flex: 1;
  height: 6px;
  background: #f1f5f9;
  border-radius: 99px;
  overflow: hidden;
}

.overview-bar-fill {
  height: 100%;
  border-radius: 99px;
  transition: width 0.8s cubic-bezier(0.4, 0, 0.2, 1);
}

.overview-val {
  width: 40px;
  text-align: right;
  font-size: 13px;
  font-weight: 600;
  color: #334155;
}

.badge-item {
  :deep(.el-badge__content) {
    font-size: 10px;
  }
}

.mid-row,
.bottom-row {
  display: flex;
}
</style>
