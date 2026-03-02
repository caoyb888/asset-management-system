import request from '@/api/request'

// ─────────────────────────── 查询参数 ───────────────────────────

export interface AssetQueryParam {
  projectId?: number | null
  buildingId?: number | null
  floorId?: number | null
  formatType?: string
  startDate?: string
  endDate?: string
  statDate?: string
  timeUnit?: 'DAY' | 'WEEK' | 'MONTH' | 'YEAR'
  compareMode?: 'NONE' | 'YOY' | 'MOM'
  pageNum?: number
  pageSize?: number
  orderBy?: string
}

// ─────────────────────────── VO 类型 ───────────────────────────

/** 趋势数据点 */
export interface RateTrendVO {
  timeDim: string
  value: number | null
  prevValue?: number | null
  growthRate?: number | null
  totalShops?: number | null
  rentedShops?: number | null
  totalArea?: number | null
  rentedArea?: number | null
}

/** 项目对比数据 */
export interface ProjectCompareVO {
  projectId: number
  vacancyRate: number | null
  rentalRate: number | null
  openingRate: number | null
  totalArea: number | null
  rentedArea: number | null
  vacantArea: number | null
  totalShops: number | null
  rentedShops: number | null
  vacantShops: number | null
  openedShops: number | null
}

/** 资产数据看板 */
export interface AssetDashboardVO {
  latestDate?: string
  totalShops?: number | null
  rentedShops?: number | null
  vacantShops?: number | null
  decoratingShops?: number | null
  openedShops?: number | null
  totalArea?: number | null
  rentedArea?: number | null
  vacantArea?: number | null
  vacancyRate?: number | null
  rentalRate?: number | null
  openingRate?: number | null
  vacancyRateYoY?: number | null
  rentalRateYoY?: number | null
  openingRateYoY?: number | null
  vacancyRateMoM?: number | null
  rentalRateMoM?: number | null
  openingRateMoM?: number | null
  vacancyTrend?: RateTrendVO[]
  rentalTrend?: RateTrendVO[]
  openingTrend?: RateTrendVO[]
  projectComparison?: ProjectCompareVO[]
}

/** 品牌/业态分布 */
export interface BrandDistributionVO {
  formatType?: string | null
  formatName?: string | null
  totalShops?: number | null
  rentedShops?: number | null
  totalArea?: number | null
  shopPercentage?: number | null
  areaPercentage?: number | null
}

/** 商铺租赁信息（楼栋/楼层粒度） */
export interface ShopRentalVO {
  projectId?: number | null
  projectName?: string | null
  buildingId?: number | null
  buildingName?: string | null
  floorId?: number | null
  floorName?: string | null
  formatType?: string | null
  totalShops?: number | null
  rentedShops?: number | null
  vacantShops?: number | null
  decoratingShops?: number | null
  openedShops?: number | null
  totalArea?: number | null
  rentedArea?: number | null
  vacancyRate?: number | null
  rentalRate?: number | null
  openingRate?: number | null
}

/** 分页结果 */
export interface PageResult<T> {
  records: T[]
  total: number
  size: number
  current: number
  pages: number
}

// ─────────────────────────── API 方法 ───────────────────────────

/** 资产数据看板（聚合接口） */
export function getDashboard(params?: AssetQueryParam) {
  return request.get<any, AssetDashboardVO>('/rpt/asset/dashboard', { params })
}

/** 空置率趋势统计 */
export function getVacancyRate(params?: AssetQueryParam) {
  return request.get<any, RateTrendVO[]>('/rpt/asset/vacancy-rate', { params })
}

/** 出租率趋势统计 */
export function getRentalRate(params?: AssetQueryParam) {
  return request.get<any, RateTrendVO[]>('/rpt/asset/rental-rate', { params })
}

/** 开业率趋势统计 */
export function getOpeningRate(params?: AssetQueryParam) {
  return request.get<any, RateTrendVO[]>('/rpt/asset/opening-rate', { params })
}

/** 品牌/业态分布报表 */
export function getBrandDistribution(params?: AssetQueryParam) {
  return request.get<any, BrandDistributionVO[]>('/rpt/asset/brand-distribution', { params })
}

/** 商铺租赁信息报表（分页） */
export function getShopRental(params?: AssetQueryParam) {
  return request.get<any, PageResult<ShopRentalVO>>('/rpt/asset/shop-rental', { params })
}
