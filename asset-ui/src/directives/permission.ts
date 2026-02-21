import { type Directive } from 'vue'
import { useUserStore } from '@/store/modules/user'

/**
 * 按钮级权限指令：v-permission="'sys:user:add'"
 * 或多权限：v-permission="['sys:user:add', 'sys:user:edit']"（满足其一即可）
 */
export const permission: Directive<HTMLElement, string | string[]> = {
  mounted(el, binding) {
    const required = Array.isArray(binding.value)
      ? binding.value
      : [binding.value]

    const userStore = useUserStore()
    const hasPermission = required.some((perm) =>
      userStore.permissions.includes(perm),
    )

    if (!hasPermission) {
      el.parentNode?.removeChild(el)
    }
  },
}
