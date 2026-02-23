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
import { Monitor, Fold, Expand, ArrowDown, Grid, OfficeBuilding, House, Management, Shop, Star, User, Bell, Document } from '@element-plus/icons-vue'
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
