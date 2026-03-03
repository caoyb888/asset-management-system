<template>
  <div class="rpt-home">
    <!-- 页头 -->
    <div class="home-header">
      <div class="home-header-left">
        <h2 class="home-title">报表中心</h2>
        <span class="home-subtitle">共 {{ allReports.length }} 个报表</span>
      </div>
      <el-input
        v-model="searchKeyword"
        placeholder="搜索报表名称..."
        :prefix-icon="Search"
        clearable
        class="search-input"
      />
    </div>

    <!-- 搜索结果 -->
    <template v-if="searchKeyword.trim()">
      <div class="section">
        <div class="section-title">搜索结果</div>
        <div v-if="searchResults.length === 0" class="empty-tip">未找到匹配的报表</div>
        <div v-else class="report-grid">
          <div
            v-for="item in searchResults"
            :key="item.routePath"
            class="report-card clickable"
            @click="navigateTo(item)"
          >
            <el-icon class="card-icon" :style="{ color: categoryColor(item.category) }">
              <component :is="item.icon" />
            </el-icon>
            <div class="card-info">
              <div class="card-name">{{ item.name }}</div>
              <el-tag :type="categoryTagType(item.category)" size="small">
                {{ categoryLabel(item.category) }}
              </el-tag>
            </div>
            <el-icon
              class="star-btn"
              :class="{ active: isFavorited(item.reportCode) }"
              @click.stop="toggleFavorite(item)"
            >
              <Star />
            </el-icon>
          </div>
        </div>
      </div>
    </template>

    <template v-else>
      <!-- 四大类报表入口卡片 -->
      <div class="category-banner">
        <div
          v-for="cat in reportCategories"
          :key="cat.id"
          class="category-entry-card"
          :style="{ '--cat-color': cat.color, '--cat-light': cat.lightColor }"
          @click="navigateTo({ routePath: cat.dashboardPath, name: cat.name, reportCode: cat.dashboardCode, category: cat.id as any, icon: cat.icon })"
        >
          <div class="cat-icon-wrap">
            <el-icon><component :is="cat.icon" /></el-icon>
          </div>
          <div class="cat-body">
            <div class="cat-name">{{ cat.name }}</div>
            <div class="cat-desc">{{ cat.desc }}</div>
          </div>
          <div class="cat-badge">{{ cat.reports.length }} 个报表</div>
          <el-icon class="cat-arrow"><ArrowRight /></el-icon>
        </div>
      </div>

      <!-- 我的收藏 -->
      <div class="section">
        <div class="section-header">
          <div class="section-title">
            <el-icon><Star /></el-icon>
            我的收藏
          </div>
          <span class="section-tip">拖拽可排序</span>
        </div>
        <div v-if="favorites.length === 0" class="empty-tip">
          暂无收藏，点击报表右侧 ★ 收藏常用报表
        </div>
        <VueDraggable
          v-else
          v-model="favorites"
          class="report-grid"
          :animation="200"
          item-key="id"
          @end="onDragEnd"
        >
          <div
            v-for="item in favorites"
            :key="item.id"
            class="report-card clickable"
            @click="navigateTo({ routePath: item.routePath, name: item.reportName, reportCode: item.reportCode, category: item.category, icon: categoryDefaultIcon(item.category) })"
          >
            <el-icon class="card-icon drag-handle" :style="{ color: categoryColor(item.category) }">
              <component :is="categoryDefaultIcon(item.category)" />
            </el-icon>
            <div class="card-info">
              <div class="card-name">{{ item.reportName }}</div>
              <el-tag :type="categoryTagType(item.category)" size="small">
                {{ categoryLabel(item.category) }}
              </el-tag>
            </div>
            <el-icon class="star-btn active" @click.stop="unfavorite(item)">
              <Star />
            </el-icon>
          </div>
        </VueDraggable>
      </div>

      <!-- 最近浏览 -->
      <div class="section" v-if="recentViews.length > 0">
        <div class="section-header">
          <div class="section-title">
            <el-icon><Clock /></el-icon>
            最近浏览
          </div>
          <el-button link size="small" @click="clearRecent">清空</el-button>
        </div>
        <div class="report-grid">
          <div
            v-for="item in recentViews"
            :key="item.routePath + item.time"
            class="report-card clickable"
            @click="navigateTo({ routePath: item.routePath, name: item.name, reportCode: item.reportCode, category: item.category, icon: categoryDefaultIcon(item.category) })"
          >
            <el-icon class="card-icon" :style="{ color: categoryColor(item.category) }">
              <component :is="categoryDefaultIcon(item.category)" />
            </el-icon>
            <div class="card-info">
              <div class="card-name">{{ item.name }}</div>
              <div class="card-time">{{ formatTime(item.time) }}</div>
            </div>
            <el-icon
              class="star-btn"
              :class="{ active: isFavorited(item.reportCode) }"
              @click.stop="toggleFavoriteByRecent(item)"
            >
              <Star />
            </el-icon>
          </div>
        </div>
      </div>

      <!-- 各类报表详情 -->
      <div
        v-for="cat in reportCategories"
        :key="cat.id"
        class="section"
      >
        <div class="section-title" :style="{ borderLeftColor: cat.color }">
          <el-icon><component :is="cat.icon" /></el-icon>
          {{ cat.name }}
        </div>
        <div class="report-grid">
          <div
            v-for="item in cat.reports"
            :key="item.routePath"
            class="report-card clickable"
            @click="navigateTo(item)"
          >
            <el-icon class="card-icon" :style="{ color: cat.color }">
              <component :is="item.icon" />
            </el-icon>
            <div class="card-info">
              <div class="card-name">{{ item.name }}</div>
              <div class="card-desc">{{ item.desc }}</div>
            </div>
            <el-icon
              class="star-btn"
              :class="{ active: isFavorited(item.reportCode) }"
              @click.stop="toggleFavorite(item)"
            >
              <Star />
            </el-icon>
          </div>
        </div>
      </div>
    </template>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import {
  Search, Star, Clock, ArrowRight,
  DataAnalysis, TrendCharts, DataLine, PieChart, Shop,
  Filter, Money, Histogram,
  Document, MapLocation,
  List,
} from '@element-plus/icons-vue'
import { VueDraggable } from 'vue-draggable-plus'
import { listFavorites, addFavorite, removeFavorite, updateFavoriteSort, type FavoriteVO } from '@/api/rpt/favorite'

const router = useRouter()

// ─────────────────────────── 报表定义 ───────────────────────────

interface ReportItem {
  reportCode: string
  name: string
  desc: string
  routePath: string
  category: 1 | 2 | 3 | 4
  icon: any
}

const reportCategories = [
  {
    id: 1,
    name: '资产类报表',
    desc: '空置率 · 出租率 · 商铺租赁 · 品牌分布',
    color: '#2E75B6',
    lightColor: '#EBF3FB',
    icon: DataAnalysis,
    dashboardCode: 'AST_DASHBOARD',
    dashboardPath: '/rpt/asset/dashboard',
    reports: [
      { reportCode: 'AST_DASHBOARD', name: '资产数据看板', desc: '空置率/出租率/开业率综合看板', routePath: '/rpt/asset/dashboard', category: 1, icon: DataAnalysis },
      { reportCode: 'AST_VACANCY', name: '空置率统计', desc: '空置率趋势与项目对比', routePath: '/rpt/asset/vacancy', category: 1, icon: TrendCharts },
      { reportCode: 'AST_RATES', name: '出租率/开业率', desc: '多指标趋势与同期对比', routePath: '/rpt/asset/rates', category: 1, icon: DataLine },
      { reportCode: 'AST_BRAND_DIST', name: '品牌业态分布', desc: '品牌/业态占比分析', routePath: '/rpt/asset/brand-dist', category: 1, icon: PieChart },
      { reportCode: 'AST_SHOP_RENTAL', name: '商铺租赁信息', desc: '商铺状态与租金明细', routePath: '/rpt/asset/shop-rental', category: 1, icon: Shop },
    ] as ReportItem[],
  },
  {
    id: 2,
    name: '招商类报表',
    desc: '客户漏斗 · 业绩对比 · 合同签约 · 租金分析',
    color: '#17A589',
    lightColor: '#E8F8F5',
    icon: Filter,
    dashboardCode: 'INV_DASHBOARD',
    dashboardPath: '/rpt/inv/dashboard',
    reports: [
      { reportCode: 'INV_DASHBOARD', name: '招商数据看板', desc: '漏斗/签约/转化率综合看板', routePath: '/rpt/inv/dashboard', category: 2, icon: DataAnalysis },
      { reportCode: 'INV_FUNNEL', name: '客户漏斗分析', desc: '意向→签约各阶段转化', routePath: '/rpt/inv/funnel', category: 2, icon: Filter },
      { reportCode: 'INV_PERFORMANCE', name: '招商业绩对比', desc: '目标完成率/项目人员对比', routePath: '/rpt/inv/performance', category: 2, icon: TrendCharts },
      { reportCode: 'INV_RENT_LEVEL', name: '租金水平分析', desc: '均价热力图/楼层业态分组', routePath: '/rpt/inv/rent-level', category: 2, icon: Money },
    ] as ReportItem[],
  },
  {
    id: 3,
    name: '营运类报表',
    desc: '营收汇总 · 合同变更 · 地区对比 · 客流分析',
    color: '#E67E22',
    lightColor: '#FEF5EC',
    icon: DataLine,
    dashboardCode: 'OPR_DASHBOARD',
    dashboardPath: '/rpt/opr/dashboard',
    reports: [
      { reportCode: 'OPR_DASHBOARD', name: '营运数据看板', desc: '营收/客流/变更/到期综合看板', routePath: '/rpt/opr/dashboard', category: 3, icon: DataAnalysis },
      { reportCode: 'OPR_REVENUE', name: '营收汇总分析', desc: '同比/环比趋势与业态分布', routePath: '/rpt/opr/revenue', category: 3, icon: TrendCharts },
      { reportCode: 'OPR_CHANGES', name: '合同变更分析', desc: '变更类型/趋势/金额影响', routePath: '/rpt/opr/changes', category: 3, icon: Document },
      { reportCode: 'OPR_REGION', name: '地区业务对比', desc: '多项目雷达图/柱状图对比', routePath: '/rpt/opr/region-compare', category: 3, icon: MapLocation },
    ] as ReportItem[],
  },
  {
    id: 4,
    name: '财务类报表',
    desc: '应收欠款 · 账龄分析 · 收缴率 · 财务看板',
    color: '#8E44AD',
    lightColor: '#F5EEF8',
    icon: List,
    dashboardCode: 'FIN_DASHBOARD',
    dashboardPath: '/rpt/fin/dashboard',
    reports: [
      { reportCode: 'FIN_DASHBOARD', name: '财务数据看板', desc: '应收/欠款/逾期率综合看板', routePath: '/rpt/fin/dashboard', category: 4, icon: DataAnalysis },
      { reportCode: 'FIN_OUTSTANDING', name: '欠款统计分析', desc: '欠款汇总与账龄分布', routePath: '/rpt/fin/outstanding', category: 4, icon: TrendCharts },
      { reportCode: 'FIN_AGING', name: '账龄分析', desc: '账龄分档堆叠图与商家排行', routePath: '/rpt/fin/aging', category: 4, icon: DataLine },
      { reportCode: 'FIN_COLLECTION', name: '收缴率趋势', desc: '月度收缴率与项目对比', routePath: '/rpt/fin/collection', category: 4, icon: Histogram },
    ] as ReportItem[],
  },
]

// 所有报表扁平列表（用于搜索）
const allReports: ReportItem[] = reportCategories.flatMap(c => c.reports)

// ─────────────────────────── 搜索 ───────────────────────────

const searchKeyword = ref('')
const searchResults = computed(() => {
  const kw = searchKeyword.value.trim().toLowerCase()
  if (!kw) return []
  return allReports.filter(r =>
    r.name.toLowerCase().includes(kw) || r.desc.toLowerCase().includes(kw)
  )
})

// ─────────────────────────── 收藏 ───────────────────────────

const favorites = ref<FavoriteVO[]>([])
const favoriteCodes = computed(() => new Set(favorites.value.map(f => f.reportCode)))

function isFavorited(reportCode: string) {
  return favoriteCodes.value.has(reportCode)
}

async function loadFavorites() {
  try {
    const res = await listFavorites()
    favorites.value = (res as any).data ?? res
  } catch {
    // 静默失败，不影响主页展示
  }
}

async function toggleFavorite(item: ReportItem) {
  if (isFavorited(item.reportCode)) {
    const fav = favorites.value.find(f => f.reportCode === item.reportCode)
    if (fav) await doUnfavorite(fav.id)
  } else {
    await doAddFavorite(item)
  }
}

async function toggleFavoriteByRecent(item: RecentItem) {
  const report = allReports.find(r => r.reportCode === item.reportCode)
  if (!report) return
  if (isFavorited(item.reportCode)) {
    const fav = favorites.value.find(f => f.reportCode === item.reportCode)
    if (fav) await doUnfavorite(fav.id)
  } else {
    await doAddFavorite(report)
  }
}

async function unfavorite(fav: FavoriteVO) {
  await doUnfavorite(fav.id)
}

async function doAddFavorite(item: ReportItem) {
  try {
    await addFavorite({
      reportCode: item.reportCode,
      reportName: item.name,
      routePath: item.routePath,
      category: item.category,
    })
    ElMessage.success(`已收藏「${item.name}」`)
    await loadFavorites()
  } catch {
    ElMessage.error('收藏失败，请稍后重试')
  }
}

async function doUnfavorite(id: number) {
  try {
    await removeFavorite(id)
    ElMessage.success('已取消收藏')
    await loadFavorites()
  } catch {
    ElMessage.error('取消收藏失败，请稍后重试')
  }
}

async function onDragEnd() {
  const ids = favorites.value.map(f => f.id)
  try {
    await updateFavoriteSort(ids)
  } catch {
    // 排序持久化失败时不提示，本地顺序已更新
  }
}

// ─────────────────────────── 最近浏览 ───────────────────────────

const RECENT_KEY = 'rpt_recent_views'
const MAX_RECENT = 8

interface RecentItem {
  name: string
  routePath: string
  reportCode: string
  category: 1 | 2 | 3 | 4
  time: number
}

const recentViews = ref<RecentItem[]>([])

function loadRecent() {
  try {
    const raw = localStorage.getItem(RECENT_KEY)
    recentViews.value = raw ? JSON.parse(raw) : []
  } catch {
    recentViews.value = []
  }
}

function clearRecent() {
  localStorage.removeItem(RECENT_KEY)
  recentViews.value = []
}

function formatTime(ts: number): string {
  const diff = Date.now() - ts
  const min = Math.floor(diff / 60000)
  if (min < 1) return '刚刚'
  if (min < 60) return `${min} 分钟前`
  const hour = Math.floor(min / 60)
  if (hour < 24) return `${hour} 小时前`
  return `${Math.floor(hour / 24)} 天前`
}

// ─────────────────────────── 导航（含浏览记录写入）───────────────────────────

function navigateTo(item: Pick<ReportItem, 'routePath' | 'name' | 'reportCode' | 'category' | 'icon'>) {
  const record: RecentItem = {
    name: item.name,
    routePath: item.routePath,
    reportCode: item.reportCode,
    category: item.category,
    time: Date.now(),
  }
  try {
    const raw = localStorage.getItem(RECENT_KEY)
    let list: RecentItem[] = raw ? JSON.parse(raw) : []
    list = list.filter(r => r.routePath !== item.routePath)
    list.unshift(record)
    if (list.length > MAX_RECENT) list = list.slice(0, MAX_RECENT)
    localStorage.setItem(RECENT_KEY, JSON.stringify(list))
  } catch {
    // ignore
  }
  router.push(item.routePath)
}

// ─────────────────────────── 辅助 ───────────────────────────

function categoryColor(cat: number) {
  return ['', '#2E75B6', '#17A589', '#E67E22', '#8E44AD'][cat] ?? '#909399'
}

function categoryLabel(cat: number) {
  return ['', '资产', '招商', '营运', '财务'][cat] ?? '其他'
}

function categoryTagType(cat: number): 'success' | 'primary' | 'warning' | 'info' | undefined {
  const map: Record<number, 'success' | 'primary' | 'warning' | 'info'> = {
    1: 'primary',
    2: 'success',
    3: 'warning',
    4: 'info',
  }
  return map[cat]
}

function categoryDefaultIcon(cat: number) {
  return [DataAnalysis, DataAnalysis, Filter, DataLine, List][cat] ?? DataAnalysis
}

// ─────────────────────────── 初始化 ───────────────────────────

onMounted(() => {
  loadFavorites()
  loadRecent()
})
</script>

<style scoped lang="scss">
.rpt-home {
  padding: 4px 0;
}

// ── 页头 ──
.home-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 24px;

  .home-header-left {
    display: flex;
    align-items: baseline;
    gap: 10px;
  }

  .home-title {
    font-size: 20px;
    font-weight: 600;
    color: #303133;
    margin: 0;
  }

  .home-subtitle {
    font-size: 13px;
    color: #909399;
  }

  .search-input {
    width: 320px;
  }
}

// ── 四大类入口卡片 ──
.category-banner {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 16px;
  margin-bottom: 28px;

  @media (max-width: 1100px) {
    grid-template-columns: repeat(2, 1fr);
  }
}

.category-entry-card {
  display: flex;
  align-items: center;
  gap: 14px;
  padding: 20px 18px;
  border-radius: 10px;
  background: var(--cat-light);
  border: 1px solid color-mix(in srgb, var(--cat-color) 20%, white);
  cursor: pointer;
  transition: box-shadow 0.2s, transform 0.15s;
  position: relative;

  &:hover {
    box-shadow: 0 4px 16px color-mix(in srgb, var(--cat-color) 25%, transparent);
    transform: translateY(-2px);

    .cat-arrow {
      opacity: 1;
      transform: translateX(2px);
    }
  }

  .cat-icon-wrap {
    width: 44px;
    height: 44px;
    border-radius: 10px;
    background: var(--cat-color);
    display: flex;
    align-items: center;
    justify-content: center;
    flex-shrink: 0;
    color: #fff;
    font-size: 22px;
  }

  .cat-body {
    flex: 1;
    min-width: 0;

    .cat-name {
      font-size: 15px;
      font-weight: 600;
      color: #303133;
      margin-bottom: 4px;
    }

    .cat-desc {
      font-size: 12px;
      color: #909399;
      white-space: nowrap;
      overflow: hidden;
      text-overflow: ellipsis;
    }
  }

  .cat-badge {
    font-size: 11px;
    font-weight: 500;
    color: var(--cat-color);
    background: color-mix(in srgb, var(--cat-color) 12%, white);
    padding: 2px 8px;
    border-radius: 10px;
    white-space: nowrap;
    flex-shrink: 0;
  }

  .cat-arrow {
    color: var(--cat-color);
    font-size: 14px;
    flex-shrink: 0;
    opacity: 0.4;
    transition: opacity 0.2s, transform 0.2s;
  }
}

// ── 通用 Section ──
.section {
  margin-bottom: 28px;
}

.section-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 12px;
}

.section-title {
  display: flex;
  align-items: center;
  gap: 6px;
  font-size: 15px;
  font-weight: 600;
  color: #303133;
  margin-bottom: 12px;
  padding-left: 10px;
  border-left: 3px solid #2E75B6;
}

.section-tip {
  font-size: 12px;
  color: #909399;
}

.empty-tip {
  color: #909399;
  font-size: 14px;
  padding: 16px 0;
}

// ── 报表卡片 ──
.report-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(220px, 1fr));
  gap: 12px;
}

.report-card {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 12px 14px;
  border: 1px solid #e4e7ed;
  border-radius: 8px;
  background: #fff;
  transition: box-shadow 0.2s, border-color 0.2s;
  cursor: default;

  &.clickable {
    cursor: pointer;

    &:hover {
      border-color: #409eff;
      box-shadow: 0 2px 8px rgba(64, 158, 255, 0.15);

      .star-btn {
        opacity: 1;
      }
    }
  }

  .card-icon {
    font-size: 22px;
    flex-shrink: 0;
  }

  .drag-handle {
    cursor: grab;
    &:active { cursor: grabbing; }
  }

  .card-info {
    flex: 1;
    min-width: 0;

    .card-name {
      font-size: 14px;
      font-weight: 500;
      color: #303133;
      white-space: nowrap;
      overflow: hidden;
      text-overflow: ellipsis;
      margin-bottom: 4px;
    }

    .card-desc {
      font-size: 12px;
      color: #909399;
      white-space: nowrap;
      overflow: hidden;
      text-overflow: ellipsis;
    }

    .card-time {
      font-size: 12px;
      color: #c0c4cc;
    }
  }

  .star-btn {
    font-size: 16px;
    color: #c0c4cc;
    flex-shrink: 0;
    opacity: 0;
    transition: color 0.2s, opacity 0.2s;
    cursor: pointer;

    &.active {
      color: #f5a623;
      opacity: 1;
    }

    &:hover {
      color: #f5a623;
      opacity: 1;
    }
  }
}
</style>
