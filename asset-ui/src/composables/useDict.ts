/**
 * 业务字典加载 Composable
 *
 * 用法：
 * ```ts
 * // 加载一个字典
 * const { options } = useDict('sys_user_status')
 *
 * // 同时加载多个字典
 * const { dicts } = useDicts('sys_user_status', 'biz_property_type')
 * // dicts.value['sys_user_status'] → DictOption[]
 *
 * // 工具函数：根据 value 查标签
 * dictLabel('sys_user_status', 1) → '正常'
 * ```
 */
import { ref, onMounted } from 'vue'
import { dictApi, type SysDictData } from '@/api/sys/dict'

export interface DictOption {
  label: string
  value: string
  cssClass?: string
  sortOrder?: number
}

/** 前端内存缓存（SPA 生命周期内有效）*/
const memCache: Record<string, DictOption[]> = {}

async function fetchDict(dictType: string): Promise<DictOption[]> {
  if (memCache[dictType]) return memCache[dictType]
  try {
    const data = await dictApi.listData(dictType) as any as SysDictData[]
    const options: DictOption[] = (data ?? []).map(d => ({
      label: d.dictLabel,
      value: d.dictValue,
      cssClass: d.cssClass,
      sortOrder: d.sortOrder,
    }))
    memCache[dictType] = options
    return options
  } catch {
    return []
  }
}

/** 加载单个字典 */
export function useDict(dictType: string) {
  const options = ref<DictOption[]>([])
  onMounted(async () => { options.value = await fetchDict(dictType) })
  return { options }
}

/** 同时加载多个字典，返回以 dictType 为 key 的 map */
export function useDicts(...dictTypes: string[]) {
  const dicts = ref<Record<string, DictOption[]>>({})
  onMounted(async () => {
    const results = await Promise.all(dictTypes.map(t => fetchDict(t).then(opts => ({ t, opts }))))
    const map: Record<string, DictOption[]> = {}
    results.forEach(({ t, opts }) => { map[t] = opts })
    dicts.value = map
  })
  return { dicts }
}

/**
 * 根据字典类型和值查找标签（同步，需已加载过）
 * @returns 标签字符串；未找到时返回 value 本身
 */
export function dictLabel(dictType: string, value: string | number): string {
  const opts = memCache[dictType] ?? []
  const found = opts.find(o => o.value === String(value))
  return found ? found.label : String(value)
}

/**
 * 根据字典类型和值查找 cssClass（el-tag type）
 */
export function dictCssClass(dictType: string, value: string | number): string {
  const opts = memCache[dictType] ?? []
  const found = opts.find(o => o.value === String(value))
  return found?.cssClass ?? ''
}

/** 清除前端内存缓存（字典更新后调用） */
export function clearDictCache(dictType?: string) {
  if (dictType) {
    delete memCache[dictType]
  } else {
    Object.keys(memCache).forEach(k => delete memCache[k])
  }
}
