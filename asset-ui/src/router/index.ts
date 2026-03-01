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
        path: 'opr/revenue-reports/form',
        name: 'OprRevenueReportForm',
        component: () => import('@/views/opr/revenue/form.vue'),
        meta: { title: '营收日历填报', icon: 'TrendCharts' },
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
      {
        path: 'opr/terminations/:id',
        name: 'OprTerminationDetail',
        component: () => import('@/views/opr/termination/detail.vue'),
        meta: { title: '解约单详情', icon: 'CircleClose' },
      },
      // ── 财务管理 ──
      {
        path: 'fin/dashboard',
        name: 'FinDashboard',
        component: () => import('@/views/fin/dashboard/index.vue'),
        meta: { title: '财务看板', icon: 'DataAnalysis' },
      },
      {
        path: 'fin/receivables',
        name: 'FinReceivable',
        component: () => import('@/views/fin/receivable/index.vue'),
        meta: { title: '应收管理', icon: 'List' },
      },
      {
        path: 'fin/receipts',
        name: 'FinReceipt',
        component: () => import('@/views/fin/receipt/index.vue'),
        meta: { title: '收款管理', icon: 'CreditCard' },
      },
      {
        path: 'fin/write-offs',
        name: 'FinWriteOff',
        component: () => import('@/views/fin/write-off/index.vue'),
        meta: { title: '核销管理', icon: 'CircleCheck' },
      },
      {
        path: 'fin/vouchers',
        name: 'FinVoucher',
        component: () => import('@/views/fin/voucher/index.vue'),
        meta: { title: '凭证管理', icon: 'Postcard' },
      },
      {
        path: 'fin/deposits',
        name: 'FinDeposit',
        component: () => import('@/views/fin/deposit/index.vue'),
        meta: { title: '保证金管理', icon: 'Wallet' },
      },
      {
        path: 'fin/prepayments',
        name: 'FinPrepayment',
        component: () => import('@/views/fin/prepayment/index.vue'),
        meta: { title: '预收款管理', icon: 'CollectionTag' },
      },
      // ── 系统管理 ──
      {
        path: 'sys/users',
        name: 'SysUser',
        component: () => import('@/views/sys/user/index.vue'),
        meta: { title: '用户管理', icon: 'User' },
      },
      {
        path: 'sys/depts',
        name: 'SysDept',
        component: () => import('@/views/sys/dept/index.vue'),
        meta: { title: '机构管理', icon: 'Tree' },
      },
      {
        path: 'sys/posts',
        name: 'SysPost',
        component: () => import('@/views/sys/post/index.vue'),
        meta: { title: '岗位管理', icon: 'Briefcase' },
      },
      {
        path: 'sys/roles',
        name: 'SysRole',
        component: () => import('@/views/sys/role/index.vue'),
        meta: { title: '角色管理', icon: 'Key' },
      },
      {
        path: 'sys/menus',
        name: 'SysMenu',
        component: () => import('@/views/sys/menu/index.vue'),
        meta: { title: '菜单管理', icon: 'Menu' },
      },
      {
        path: 'sys/dict',
        name: 'SysDict',
        component: () => import('@/views/sys/dict/index.vue'),
        meta: { title: '业务字典', icon: 'Files' },
      },
      {
        path: 'sys/code-rules',
        name: 'SysCodeRule',
        component: () => import('@/views/sys/code/index.vue'),
        meta: { title: '编码规则', icon: 'Tickets' },
      },
      {
        path: 'sys/categories',
        name: 'SysCategory',
        component: () => import('@/views/sys/category/index.vue'),
        meta: { title: '分类管理', icon: 'FolderOpened' },
      },
      {
        path: 'sys/logs',
        name: 'SysLog',
        component: () => import('@/views/sys/log/index.vue'),
        meta: { title: '操作日志', icon: 'Document' },
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
