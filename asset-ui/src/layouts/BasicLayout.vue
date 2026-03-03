<template>
  <el-container class="layout-container">
    <!-- 侧边栏 -->
    <el-aside :width="sidebarWidth" class="layout-sidebar">
      <div class="sidebar-logo">
        <span v-if="!appStore.sidebarCollapsed" class="logo-text">资产管理系统</span>
        <span v-else class="logo-icon">资</span>
      </div>
      <el-menu
        :default-active="activeMenu"
        :collapse="appStore.sidebarCollapsed"
        :collapse-transition="false"
        background-color="#001529"
        text-color="rgba(255,255,255,0.85)"
        active-text-color="#ffffff"
        router
      >
        <el-menu-item index="/dashboard">
          <el-icon><Monitor /></el-icon>
          <template #title>工作台</template>
        </el-menu-item>

        <!-- 基础数据管理 -->
        <el-sub-menu index="base">
          <template #title>
            <el-icon><Grid /></el-icon>
            <span>基础数据管理</span>
          </template>
          <el-menu-item index="/base/projects">
            <el-icon><OfficeBuilding /></el-icon>
            <template #title>项目管理</template>
          </el-menu-item>
          <el-menu-item index="/base/buildings">
            <el-icon><House /></el-icon>
            <template #title>楼栋管理</template>
          </el-menu-item>
          <el-menu-item index="/base/floors">
            <el-icon><Management /></el-icon>
            <template #title>楼层管理</template>
          </el-menu-item>
          <el-menu-item index="/base/shops">
            <el-icon><Shop /></el-icon>
            <template #title>商铺管理</template>
          </el-menu-item>
          <el-menu-item index="/base/brands">
            <el-icon><Star /></el-icon>
            <template #title>品牌管理</template>
          </el-menu-item>
          <el-menu-item index="/base/merchants">
            <el-icon><User /></el-icon>
            <template #title>商家管理</template>
          </el-menu-item>
          <el-menu-item index="/base/notices">
            <el-icon><Bell /></el-icon>
            <template #title>通知公告</template>
          </el-menu-item>
          <el-menu-item index="/base/news">
            <el-icon><Document /></el-icon>
            <template #title>新闻资讯</template>
          </el-menu-item>
        </el-sub-menu>

        <!-- 招商管理 -->
        <el-sub-menu index="inv">
          <template #title>
            <el-icon><Briefcase /></el-icon>
            <span>招商管理</span>
          </template>
          <el-menu-item index="/inv/config/rent-schemes">
            <el-icon><Setting /></el-icon>
            <template #title>计租方案</template>
          </el-menu-item>
          <el-menu-item index="/inv/config/fee-items">
            <el-icon><Coin /></el-icon>
            <template #title>收款费项</template>
          </el-menu-item>
          <el-menu-item index="/inv/intentions">
            <el-icon><EditPen /></el-icon>
            <template #title>意向协议</template>
          </el-menu-item>
          <el-menu-item index="/inv/contracts">
            <el-icon><Tickets /></el-icon>
            <template #title>招商合同</template>
          </el-menu-item>
          <el-menu-item index="/inv/opening-approvals">
            <el-icon><Checked /></el-icon>
            <template #title>开业审批</template>
          </el-menu-item>
          <el-menu-item index="/inv/rent-policies">
            <el-icon><DataLine /></el-icon>
            <template #title>租决政策</template>
          </el-menu-item>
          <el-menu-item index="/inv/rent-decomps">
            <el-icon><PieChart /></el-icon>
            <template #title>租金分解</template>
          </el-menu-item>
        </el-sub-menu>

        <!-- 营运管理 -->
        <el-sub-menu index="opr">
          <template #title>
            <el-icon><Operation /></el-icon>
            <span>营运管理</span>
          </template>
          <el-menu-item index="/opr/ledgers">
            <el-icon><Memo /></el-icon>
            <template #title>合同台账</template>
          </el-menu-item>
          <el-menu-item index="/opr/contract-changes">
            <el-icon><Switch /></el-icon>
            <template #title>合同变更</template>
          </el-menu-item>
          <el-menu-item index="/opr/revenue-reports">
            <el-icon><TrendCharts /></el-icon>
            <template #title>营收填报</template>
          </el-menu-item>
          <el-menu-item index="/opr/floating-rent">
            <el-icon><DataLine /></el-icon>
            <template #title>浮动租金</template>
          </el-menu-item>
          <el-menu-item index="/opr/passenger-flows">
            <el-icon><UserFilled /></el-icon>
            <template #title>客流填报</template>
          </el-menu-item>
          <el-menu-item index="/opr/terminations">
            <el-icon><CircleClose /></el-icon>
            <template #title>合同解约</template>
          </el-menu-item>
          <el-menu-item index="/opr/alerts/contract-expiry">
            <el-icon><Warning /></el-icon>
            <template #title>到期预警</template>
          </el-menu-item>
        </el-sub-menu>

        <!-- 财务管理 -->
        <el-sub-menu index="fin">
          <template #title>
            <el-icon><Money /></el-icon>
            <span>财务管理</span>
          </template>
          <el-menu-item index="/fin/dashboard">
            <el-icon><DataAnalysis /></el-icon>
            <template #title>财务看板</template>
          </el-menu-item>
          <el-menu-item index="/fin/receivables">
            <el-icon><List /></el-icon>
            <template #title>应收管理</template>
          </el-menu-item>
          <el-menu-item index="/fin/receipts">
            <el-icon><CreditCard /></el-icon>
            <template #title>收款管理</template>
          </el-menu-item>
          <el-menu-item index="/fin/write-offs">
            <el-icon><CircleCheck /></el-icon>
            <template #title>核销管理</template>
          </el-menu-item>
          <el-menu-item index="/fin/vouchers">
            <el-icon><Postcard /></el-icon>
            <template #title>凭证管理</template>
          </el-menu-item>
          <el-menu-item index="/fin/deposits">
            <el-icon><Wallet /></el-icon>
            <template #title>保证金管理</template>
          </el-menu-item>
          <el-menu-item index="/fin/prepayments">
            <el-icon><CollectionTag /></el-icon>
            <template #title>预收款管理</template>
          </el-menu-item>
        </el-sub-menu>
        <!-- 报表管理 -->
        <el-sub-menu index="rpt">
          <template #title>
            <el-icon><DataAnalysis /></el-icon>
            <span>报表管理</span>
          </template>
          <el-menu-item index="/rpt/home">
            <el-icon><HomeFilled /></el-icon>
            <template #title>报表中心</template>
          </el-menu-item>
          <el-menu-item index="/rpt/asset/dashboard">
            <el-icon><Monitor /></el-icon>
            <template #title>资产数据看板</template>
          </el-menu-item>
          <el-menu-item index="/rpt/asset/vacancy">
            <el-icon><TrendCharts /></el-icon>
            <template #title>空置率统计</template>
          </el-menu-item>
          <el-menu-item index="/rpt/asset/rates">
            <el-icon><DataLine /></el-icon>
            <template #title>出租率/开业率</template>
          </el-menu-item>
          <el-menu-item index="/rpt/asset/brand-dist">
            <el-icon><PieChart /></el-icon>
            <template #title>品牌业态分布</template>
          </el-menu-item>
          <el-menu-item index="/rpt/asset/shop-rental">
            <el-icon><Shop /></el-icon>
            <template #title>商铺租赁信息</template>
          </el-menu-item>
          <el-menu-item index="/rpt/inv/dashboard">
            <el-icon><DataAnalysis /></el-icon>
            <template #title>招商数据看板</template>
          </el-menu-item>
          <el-menu-item index="/rpt/inv/funnel">
            <el-icon><Filter /></el-icon>
            <template #title>客户漏斗分析</template>
          </el-menu-item>
          <el-menu-item index="/rpt/inv/performance">
            <el-icon><TrendCharts /></el-icon>
            <template #title>招商业绩对比</template>
          </el-menu-item>
          <el-menu-item index="/rpt/inv/rent-level">
            <el-icon><Money /></el-icon>
            <template #title>租金水平分析</template>
          </el-menu-item>
          <el-menu-item index="/rpt/opr/dashboard">
            <el-icon><DataAnalysis /></el-icon>
            <template #title>营运数据看板</template>
          </el-menu-item>
          <el-menu-item index="/rpt/opr/revenue">
            <el-icon><TrendCharts /></el-icon>
            <template #title>营收汇总分析</template>
          </el-menu-item>
          <el-menu-item index="/rpt/opr/changes">
            <el-icon><Document /></el-icon>
            <template #title>合同变更分析</template>
          </el-menu-item>
          <el-menu-item index="/rpt/opr/region-compare">
            <el-icon><MapLocation /></el-icon>
            <template #title>地区业务对比</template>
          </el-menu-item>
          <el-menu-item index="/rpt/fin/dashboard">
            <el-icon><DataAnalysis /></el-icon>
            <template #title>财务数据看板</template>
          </el-menu-item>
          <el-menu-item index="/rpt/fin/outstanding">
            <el-icon><TrendCharts /></el-icon>
            <template #title>欠款统计分析</template>
          </el-menu-item>
          <el-menu-item index="/rpt/fin/aging">
            <el-icon><DataLine /></el-icon>
            <template #title>账龄分析</template>
          </el-menu-item>
          <el-menu-item index="/rpt/fin/collection">
            <el-icon><Histogram /></el-icon>
            <template #title>收缴率趋势</template>
          </el-menu-item>
        </el-sub-menu>
        <!-- 系统管理 -->
        <el-sub-menu index="sys">
          <template #title>
            <el-icon><Setting /></el-icon>
            <span>系统管理</span>
          </template>
          <el-menu-item index="/sys/users">
            <el-icon><User /></el-icon>
            <template #title>用户管理</template>
          </el-menu-item>
          <el-menu-item index="/sys/depts">
            <el-icon><Grid /></el-icon>
            <template #title>机构管理</template>
          </el-menu-item>
          <el-menu-item index="/sys/posts">
            <el-icon><Briefcase /></el-icon>
            <template #title>岗位管理</template>
          </el-menu-item>
          <el-menu-item index="/sys/roles">
            <el-icon><Key /></el-icon>
            <template #title>角色管理</template>
          </el-menu-item>
          <el-menu-item index="/sys/menus">
            <el-icon><Menu /></el-icon>
            <template #title>菜单管理</template>
          </el-menu-item>
          <el-menu-item index="/sys/dict">
            <el-icon><Files /></el-icon>
            <template #title>业务字典</template>
          </el-menu-item>
          <el-menu-item index="/sys/code-rules">
            <el-icon><Tickets /></el-icon>
            <template #title>编码规则</template>
          </el-menu-item>
          <el-menu-item index="/sys/categories">
            <el-icon><FolderOpened /></el-icon>
            <template #title>分类管理</template>
          </el-menu-item>
          <el-menu-item index="/sys/fee-algorithms">
            <el-icon><Coin /></el-icon>
            <template #title>租费算法</template>
          </el-menu-item>
          <el-menu-item index="/sys/config">
            <el-icon><Setting /></el-icon>
            <template #title>系统配置</template>
          </el-menu-item>
          <el-menu-item index="/sys/logs">
            <el-icon><Document /></el-icon>
            <template #title>操作日志</template>
          </el-menu-item>
          <el-menu-item index="/sys/online">
            <el-icon><Monitor /></el-icon>
            <template #title>在线用户</template>
          </el-menu-item>
        </el-sub-menu>
      </el-menu>
    </el-aside>

    <el-container class="layout-main">
      <!-- 顶部栏 -->
      <el-header class="layout-header">
        <div class="header-left">
          <el-icon class="toggle-btn" @click="appStore.toggleSidebar()">
            <Fold v-if="!appStore.sidebarCollapsed" />
            <Expand v-else />
          </el-icon>
          <el-breadcrumb separator="/">
            <el-breadcrumb-item :to="{ path: '/' }">首页</el-breadcrumb-item>
            <el-breadcrumb-item>{{ appStore.pageTitle }}</el-breadcrumb-item>
          </el-breadcrumb>
        </div>

        <div class="header-right">
          <el-dropdown @command="handleCommand">
            <div class="user-info">
              <el-avatar :size="32" :src="userStore.userInfo?.avatar">
                {{ userStore.nickname.charAt(0) }}
              </el-avatar>
              <span class="user-name">{{ userStore.nickname }}</span>
              <el-icon><ArrowDown /></el-icon>
            </div>
            <template #dropdown>
              <el-dropdown-menu>
                <el-dropdown-item command="logout">退出登录</el-dropdown-item>
              </el-dropdown-menu>
            </template>
          </el-dropdown>
        </div>
      </el-header>

      <!-- 内容区 -->
      <el-main class="layout-content">
        <router-view v-slot="{ Component }">
          <transition name="fade" mode="out-in">
            <component :is="Component" />
          </transition>
        </router-view>
      </el-main>
    </el-container>
  </el-container>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessageBox } from 'element-plus'
import { Monitor, Fold, Expand, ArrowDown, Grid, OfficeBuilding, House, Management, Shop, Star, User, Bell, Document, Briefcase, Setting, Coin, EditPen, Tickets, Checked, DataLine, PieChart, Operation, Memo, Switch, TrendCharts, UserFilled, CircleClose, Money, List, CreditCard, Postcard, Wallet, CollectionTag, Warning, CircleCheck, DataAnalysis, Key, Files, Menu, FolderOpened, Filter, MapLocation, Histogram, HomeFilled } from '@element-plus/icons-vue'
import { useUserStore } from '@/store/modules/user'
import { useAppStore } from '@/store/modules/app'

const route = useRoute()
const router = useRouter()
const userStore = useUserStore()
const appStore = useAppStore()

const activeMenu = computed(() => route.path)
const sidebarWidth = computed(() =>
  appStore.sidebarCollapsed ? '64px' : '220px',
)

async function handleCommand(command: string) {
  if (command === 'logout') {
    await ElMessageBox.confirm('确认退出登录？', '提示', {
      confirmButtonText: '确认',
      cancelButtonText: '取消',
      type: 'warning',
    })
    await userStore.logout()
    router.push('/login')
  }
}
</script>

<style scoped lang="scss">
@use '@/styles/variables' as *;

.layout-container {
  height: 100vh;
}

.layout-sidebar {
  background-color: $sidebar-bg;
  transition: width 0.3s;
  overflow: hidden;

  .sidebar-logo {
    height: $header-height;
    display: flex;
    align-items: center;
    justify-content: center;
    color: #fff;
    font-size: 16px;
    font-weight: 600;
    border-bottom: 1px solid rgba(255, 255, 255, 0.1);
    overflow: hidden;
    white-space: nowrap;

    .logo-icon {
      font-size: 22px;
    }
  }

  .el-menu {
    border-right: none;
  }
}

.layout-header {
  height: $header-height;
  background: $header-bg;
  border-bottom: 1px solid $header-border;
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 0 16px;

  .header-left {
    display: flex;
    align-items: center;
    gap: 12px;

    .toggle-btn {
      font-size: 18px;
      cursor: pointer;
      color: $text-regular;

      &:hover {
        color: $primary-color;
      }
    }
  }

  .header-right {
    .user-info {
      display: flex;
      align-items: center;
      gap: 8px;
      cursor: pointer;
      color: $text-primary;

      .user-name {
        font-size: 14px;
      }
    }
  }
}

.layout-content {
  background: $content-bg;
  overflow-y: auto;
  padding: 16px;
}

.fade-enter-active,
.fade-leave-active {
  transition: opacity 0.15s ease;
}

.fade-enter-from,
.fade-leave-to {
  opacity: 0;
}
</style>
