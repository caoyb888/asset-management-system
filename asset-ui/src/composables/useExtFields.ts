/**
 * 扩展字段加载 Composable
 *
 * 用法：
 * ```ts
 * const { fieldDefs, formFields, listFields, loading, getDefaults } = useExtFields('project')
 * ```
 *
 * - fieldDefs：全部字段定义
 * - formFields：showInForm=true 的字段（表单渲染用）
 * - listFields：showInList=true 的字段（列表列渲染用）
 * - loading：加载状态
 * - getDefaults()：返回以 fieldKey 为 key、defaultVal 为 value 的初始空对象
 */
import { ref, computed, onMounted } from 'vue'
import { extFieldApi, type ExtFieldDef } from '@/api/sys/extField'

/** SPA 内存缓存（按 moduleCode 缓存） */
const memCache = new Map<string, ExtFieldDef[]>()

async function fetchDefs(moduleCode: string): Promise<ExtFieldDef[]> {
  if (memCache.has(moduleCode)) return memCache.get(moduleCode)!
  try {
    const data = await extFieldApi.list(moduleCode)
    memCache.set(moduleCode, data ?? [])
    return data ?? []
  } catch {
    return []
  }
}

/** 主动清除某模块的内存缓存（字段配置变更后调用） */
export function clearExtFieldCache(moduleCode?: string) {
  if (moduleCode) {
    memCache.delete(moduleCode)
  } else {
    memCache.clear()
  }
}

export function useExtFields(moduleCode: string) {
  const fieldDefs = ref<ExtFieldDef[]>([])
  const loading = ref(false)

  const formFields = computed(() => fieldDefs.value.filter(f => f.showInForm))
  const listFields = computed(() => fieldDefs.value.filter(f => f.showInList))

  /** 生成表单初始值对象（所有字段 key → 默认值或 null） */
  function getDefaults(): Record<string, any> {
    const defaults: Record<string, any> = {}
    fieldDefs.value.forEach(f => {
      if (f.fieldType === 'checkbox') {
        defaults[f.fieldKey] = f.defaultVal ? [f.defaultVal] : []
      } else {
        defaults[f.fieldKey] = f.defaultVal ?? null
      }
    })
    return defaults
  }

  /** 手动重新加载（字段配置变更后调用） */
  async function reload() {
    clearExtFieldCache(moduleCode)
    loading.value = true
    try {
      fieldDefs.value = await fetchDefs(moduleCode)
    } finally {
      loading.value = false
    }
  }

  onMounted(async () => {
    loading.value = true
    try {
      fieldDefs.value = await fetchDefs(moduleCode)
    } finally {
      loading.value = false
    }
  })

  return { fieldDefs, formFields, listFields, loading, getDefaults, reload }
}
