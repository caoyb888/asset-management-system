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
      {
        path: 'base/notices',
        name: 'BaseNotice',
        component: () => import('@/views/base/notice/index.vue'),
        meta: { title: '通知公告', icon: 'Bell' },
      },
      {
        path: 'base/news',
        name: 'BaseNews',
        component: () => import('@/views/base/news/index.vue'),
        meta: { title: '新闻资讯', icon: 'Document' },
      },
      // ── 招商配置 ──
      {
        path: 'inv/config/rent-schemes',
        name: 'InvCfgRentScheme',
        component: () => import('@/views/inv/config/rent-scheme/index.vue'),
        meta: { title: '计租方案管理', icon: 'Setting' },
      },
      {
        path: 'inv/config/fee-items',
        name: 'InvCfgFeeItem',
        component: () => import('@/views/inv/config/fee-item/index.vue'),
        meta: { title: '收款项目管理', icon: 'Coin' },
      },
      // ── 招商管理 ──
      {
        path: 'inv/intentions',
        name: 'InvIntention',
        component: () => import('@/views/inv/intention/index.vue'),
        meta: { title: '意向协议', icon: 'DocumentAdd' },
      },
      {
        path: 'inv/intentions/form',
        name: 'InvIntentionForm',
        component: () => import('@/views/inv/intention/form.vue'),
        meta: { title: '意向协议 - 新增/编辑', icon: 'DocumentAdd' },
      },
      {
        path: 'inv/contracts',
        name: 'InvContract',
        component: () => import('@/views/inv/contract/index.vue'),
        meta: { title: '招商合同', icon: 'Document' },
      },
      {
        path: 'inv/contracts/form',
        name: 'InvContractForm',
        component: () => import('@/views/inv/contract/form.vue'),
        meta: { title: '合同 - 新增/编辑', icon: 'Document' },
      },
      {
        path: 'inv/contracts/from-intention',
        name: 'InvContractFromIntention',
        component: () => import('@/views/inv/contract/from-intention.vue'),
        meta: { title: '意向转合同', icon: 'Document' },
      },
      {
        path: 'inv/opening-approvals',
        name: 'InvOpeningApproval',
        component: () => import('@/views/inv/opening-approval/index.vue'),
        meta: { title: '开业审批', icon: 'Stamp' },
      },
      {
        path: 'inv/opening-approvals/form',
        name: 'InvOpeningApprovalForm',
        component: () => import('@/views/inv/opening-approval/form.vue'),
        meta: { title: '开业审批 - 新增', icon: 'Stamp' },
      },
      {
        path: 'inv/rent-policies',
        name: 'InvRentPolicy',
        component: () => import('@/views/inv/rent-policy/index.vue'),
        meta: { title: '租决政策', icon: 'PriceTag' },
      },
      {
        path: 'inv/rent-policies/form',
        name: 'InvRentPolicyForm',
        component: () => import('@/views/inv/rent-policy/form.vue'),
        meta: { title: '租决政策 - 新增/编辑', icon: 'PriceTag' },
      },
      {
        path: 'inv/rent-decomps',
        name: 'InvRentDecomp',
        component: () => import('@/views/inv/rent-decomp/index.vue'),
        meta: { title: '租金分解', icon: 'DataLine' },
      },
      {
        path: 'inv/rent-decomps/form',
        name: 'InvRentDecompForm',
        component: () => import('@/views/inv/rent-decomp/form.vue'),
        meta: { title: '租金分解 - 新增/编辑', icon: 'DataLine' },
      },
      // ── 营运管理 ──
      {
        path: 'opr/ledgers',
        name: 'OprLedger',
        component: () => import('@/views/opr/ledger/index.vue'),
        meta: { title: '合同台账', icon: 'Memo' },
      },
      {
        path: 'opr/ledgers/:id',
        name: 'OprLedgerDetail',
        component: () => import('@/views/opr/ledger/detail.vue'),
        meta: { title: '台账详情', icon: 'Memo' },
      },
      {
        path: 'opr/alerts/contract-expiry',
        name: 'OprContractExpiryAlert',
        component: () => import('@/views/opr/alerts/contract-expiry.vue'),
        meta: { title: '合同到期预警', icon: 'Warning' },
      },
      {
        path: 'opr/contract-changes',
        name: 'OprContractChange',
        component: () => import('@/views/opr/change/index.vue'),
        meta: { title: '合同变更', icon: 'Switch' },
      },
      {
        path: 'opr/contract-changes/form',
        name: 'OprContractChangeForm',
        component: () => import('@/views/opr/change/form.vue'),
        meta: { title: '变更 - 新增/编辑', icon: 'Switch' },
      },
      {
        path: 'opr/contract-changes/:id',
        name: 'OprContractChangeDetail',
        component: () => import('@/views/opr/change/detail.vue'),
        meta: { title: '变更详情', icon: 'Switch' },
      },
      {
        path: 'opr/revenue-reports',
        name: 'OprRevenueReport',
        component: () => import('@/views/opr/revenue/index.vue'),
        meta: { title: '营收填报', icon: 'TrendCharts' },
      },
      {
        path: 'opr/floating-rent',
        name: 'OprFloatingRent',
        component: () => import('@/views/opr/revenue/floating-rent.vue'),
        meta: { title: '浮动租金', icon: 'DataLine' },
      },
      {
        path: 'opr/passenger-flows',
        name: 'OprPassengerFlow',
        component: () => import('@/views/opr/flow/index.vue'),
        meta: { title: '客流填报', icon: 'User' },
      },
      {
        path: 'opr/terminations',
        name: 'OprTermination',
        component: () => import('@/views/opr/termination/index.vue'),
        meta: { title: '合同解约', icon: 'CircleClose' },
      },
      {
        path: 'opr/terminations/form',
        name: 'OprTerminationForm',
        component: () => import('@/views/opr/termination/form.vue'),
        meta: { title: '解约 - 新增/编辑', icon: 'CircleClose' },
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
