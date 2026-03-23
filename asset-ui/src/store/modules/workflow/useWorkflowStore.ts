import { defineStore } from 'pinia'
import { ref } from 'vue'
import { getTodoCount } from '@/api/workflow/task'

export const useWorkflowStore = defineStore('workflow', () => {
  const todoCount = ref(0)

  async function fetchTodoCount() {
    try {
      todoCount.value = await getTodoCount()
    } catch {
      // 静默失败，不影响主流程
    }
  }

  let timer: ReturnType<typeof setInterval> | null = null

  function startPolling() {
    fetchTodoCount()
    if (timer) clearInterval(timer)
    timer = setInterval(fetchTodoCount, 60_000)
  }

  function stopPolling() {
    if (timer) {
      clearInterval(timer)
      timer = null
    }
  }

  return { todoCount, fetchTodoCount, startPolling, stopPolling }
})
