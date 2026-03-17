import { computed } from 'vue'
import { useAppStore } from '@/store/modules/app'

/** 每套主题的 ECharts 图表配色方案 */
const CHART_PALETTES: Record<string, string[]> = {
  '':       ['#2e75b6', '#67c23a', '#e6a23c', '#f56c6c', '#909399', '#4a90d9', '#1a5c99', '#f59e0b'],
  teal:    ['#0d9488', '#67c23a', '#e6a23c', '#f56c6c', '#909399', '#14b8a6', '#0f766e', '#f59e0b'],
  indigo:  ['#6366f1', '#67c23a', '#e6a23c', '#f56c6c', '#909399', '#818cf8', '#4f46e5', '#f59e0b'],
  dark:    ['#3b82f6', '#4ade80', '#fbbf24', '#f87171', '#94a3b8', '#60a5fa', '#2563eb', '#fb923c'],
}

/** 每套主题的品牌色 */
const PRIMARY_COLORS: Record<string, string> = {
  '':       '#2e75b6',
  teal:    '#0d9488',
  indigo:  '#6366f1',
  dark:    '#3b82f6',
}

/** 每套主题的渐变色（用于横幅等场景） */
const GRADIENTS: Record<string, string> = {
  '':       'linear-gradient(135deg, #1e3a5f 0%, #1d4ed8 50%, #2563eb 100%)',
  teal:    'linear-gradient(135deg, #042f2e 0%, #0d9488 50%, #14b8a6 100%)',
  indigo:  'linear-gradient(135deg, #1e1b4b 0%, #4f46e5 50%, #6366f1 100%)',
  dark:    'linear-gradient(135deg, #0f172a 0%, #1e3a5f 50%, #2563eb 100%)',
}

/**
 * 主题色 composable — 为 ECharts、内联 style 等无法使用 CSS 变量的场景提供响应式颜色
 */
export function useThemeColors() {
  const appStore = useAppStore()

  const theme = computed(() => appStore.theme)
  const isDark = computed(() => appStore.theme === 'dark')
  const primaryColor = computed(() => PRIMARY_COLORS[appStore.theme] ?? PRIMARY_COLORS[''])
  const chartPalette = computed(() => CHART_PALETTES[appStore.theme] ?? CHART_PALETTES[''])
  const gradient = computed(() => GRADIENTS[appStore.theme] ?? GRADIENTS[''])

  /** 文字颜色（用于 ECharts 轴标签等） */
  const textColor = computed(() => isDark.value ? '#cbd5e1' : '#606266')
  const textPrimaryColor = computed(() => isDark.value ? '#f1f5f9' : '#303133')

  /** 分割线颜色（ECharts grid 线） */
  const splitLineColor = computed(() => isDark.value ? '#374151' : '#e0e0e0')

  /** 卡片/面板背景色 */
  const cardBg = computed(() => isDark.value ? '#1f2937' : '#ffffff')

  /** 面板 hover 背景 */
  const panelHoverBg = computed(() => isDark.value ? '#374151' : '#f8fafc')

  /**
   * 创建主色的面积渐变（ECharts areaStyle 用）
   */
  function primaryAreaGradient(opacity1 = 0.3, opacity2 = 0) {
    const c = primaryColor.value
    const rgb = hexToRgb(c)
    return {
      type: 'linear' as const,
      x: 0, y: 0, x2: 0, y2: 1,
      colorStops: [
        { offset: 0, color: `rgba(${rgb},${opacity1})` },
        { offset: 1, color: `rgba(${rgb},${opacity2})` },
      ],
    }
  }

  return {
    theme,
    isDark,
    primaryColor,
    chartPalette,
    gradient,
    textColor,
    textPrimaryColor,
    splitLineColor,
    cardBg,
    panelHoverBg,
    primaryAreaGradient,
  }
}

function hexToRgb(hex: string): string {
  const h = hex.replace('#', '')
  const r = parseInt(h.substring(0, 2), 16)
  const g = parseInt(h.substring(2, 4), 16)
  const b = parseInt(h.substring(4, 6), 16)
  return `${r},${g},${b}`
}
