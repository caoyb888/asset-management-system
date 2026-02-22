import { createRouter, createWebHistory, type RouteRecordRaw } from 'vue-router'
import NProgress from 'nprogress'
import { getToken } from '@/utils/auth'
import { useUserStore } from '@/store/modules/user'

const routes: RouteRecordRaw[] = [
  {
    path: '/login',
    component: () => import('@/layouts/BlankLayout.vue'),
    children: [
      {
        path: '',
        name: 'Login',
        component: () => import('@/views/login/index.vue'),
        meta: { title: '登录' },
      },
    ],
  },
  {
    path: '/',
    component: () => import('@/layouts/BasicLayout.vue'),
    redirect: '/dashboard',
    meta: { requiresAuth: true },
    children: [
      {
        path: 'dashboard',
        name: 'Dashboard',
        component: () => import('@/views/dashboard/index.vue'),
        meta: { title: '工作台', icon: 'Monitor' },
      },
      // ── 基础数据管理 ──
      {
        path: 'base/projects',
        name: 'BaseProject',
        component: () => import('@/views/base/project/index.vue'),
        meta: { title: '项目管理', icon: 'OfficeBuilding' },
      },
      {
        path: 'base/buildings',
        name: 'BaseBuilding',
        component: () => import('@/views/base/building/index.vue'),
        meta: { title: '楼栋管理', icon: 'House' },
      },
      {
        path: 'base/floors',
        name: 'BaseFloor',
        component: () => import('@/views/base/floor/index.vue'),
        meta: { title: '楼层管理', icon: 'Grid' },
      },
      {
        path: 'base/shops',
        name: 'BaseShop',
        component: () => import('@/views/base/shop/index.vue'),
        meta: { title: '商铺管理', icon: 'Shop' },
      },
      {
        path: 'base/brands',
        name: 'BaseBrand',
        component: () => import('@/views/base/brand/index.vue'),
        meta: { title: '品牌管理', icon: 'Star' },
      },
      {
        path: 'base/merchants',
        name: 'BaseMerchant',
        component: () => import('@/views/base/merchant/index.vue'),
        meta: { title: '商家管理', icon: 'User' },
      },
    ],
  },
  {
    path: '/:pathMatch(.*)*',
    name: 'NotFound',
    component: () => import('@/views/error/404.vue'),
  },
]

const router = createRouter({
  history: createWebHistory(),
  routes,
  scrollBehavior: () => ({ top: 0 }),
})

// 路由守卫
router.beforeEach(async (to, _from, next) => {
  NProgress.start()
  const token = getToken()

  if (!token) {
    // 未登录：只允许访问登录页
    to.path === '/login' ? next() : next('/login')
    return
  }

  if (to.path === '/login') {
    next('/dashboard')
    return
  }

  // 已登录但未获取用户信息时拉取
  const userStore = useUserStore()
  if (!userStore.userInfo) {
    try {
      await userStore.getInfo()
    } catch {
      userStore.resetState()
      next('/login')
      return
    }
  }

  next()
})

router.afterEach((to) => {
  NProgress.done()
  if (to.meta?.title) {
    document.title = `${to.meta.title} - 资产管理系统`
  }
})

export default router
