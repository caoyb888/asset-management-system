<template>
  <el-button
    :loading="exporting"
    :disabled="exporting"
    type="primary"
    plain
    :icon="Download"
    @click="handleExport"
  >
    {{ exporting ? progressLabel : label }}
  </el-button>
</template>

<script setup lang="ts">
import { ref, computed } from 'vue'
import { ElMessage } from 'element-plus'
import { Download } from '@element-plus/icons-vue'
import {
  submitExport,
  queryExportStatus,
  getDownloadUrl,
  type ExportTaskDTO,
  type ExportTaskStatusVO,
} from '@/api/rpt/export'

// ── props ─────────────────────────────────────────────────────────────────────

const props = withDefaults(
  defineProps<{
    /** 报表编码（ReportExportCodes 中的常量） */
    reportCode: string
    /** 导出参数（与当前页面查询参数一致） */
    params?: Record<string, any>
    /** 导出格式，默认 EXCEL */
    format?: 'EXCEL' | 'PDF'
    /** 按钮文字 */
    label?: string
    /** 轮询间隔（ms），默认 2000 */
    pollInterval?: number
    /** 最大轮询次数，超过后视为超时，默认 60 次（2min） */
    maxPolls?: number
  }>(),
  {
    format: 'EXCEL',
    label: '导出 Excel',
    pollInterval: 2000,
    maxPolls: 60,
  },
)

// ── emits ─────────────────────────────────────────────────────────────────────

const emit = defineEmits<{
  /** 导出成功，参数为 logCode */
  (e: 'success', logCode: string): void
  /** 导出失败，参数为错误信息 */
  (e: 'error', message: string): void
}>()

// ── 状态 ──────────────────────────────────────────────────────────────────────

const exporting = ref(false)
const pollCount = ref(0)
let pollTimer: ReturnType<typeof setTimeout> | null = null

const progressLabel = computed(() => {
  if (pollCount.value === 0) return '提交中…'
  return `生成中（${pollCount.value}）…`
})

// ── 导出主流程 ────────────────────────────────────────────────────────────────

async function handleExport() {
  if (exporting.value) return
  exporting.value = true
  pollCount.value = 0
  clearPoll()

  try {
    const dto: ExportTaskDTO = {
      reportCode: props.reportCode,
      format: props.format,
      params: props.params ?? {},
    }
    const res = await submitExport(dto)
    const logCode: string = (res as any).data ?? res
    if (!logCode) throw new Error('提交失败，未获得任务编号')
    poll(logCode)
  } catch (e: any) {
    exporting.value = false
    const msg = e?.message ?? '导出请求失败'
    ElMessage.error(msg)
    emit('error', msg)
  }
}

// ── 轮询状态 ──────────────────────────────────────────────────────────────────

function poll(logCode: string) {
  pollTimer = setTimeout(async () => {
    pollCount.value += 1

    if (pollCount.value > props.maxPolls) {
      exporting.value = false
      const msg = '导出超时，请稍后在「我的导出记录」中查看结果'
      ElMessage.warning(msg)
      emit('error', msg)
      return
    }

    try {
      const res = await queryExportStatus(logCode)
      const status: ExportTaskStatusVO = (res as any).data ?? res

      if (status.status === 1) {
        // 成功 → 触发下载
        exporting.value = false
        triggerDownload(logCode, status.fileName ?? 'export.xlsx')
        ElMessage.success(`导出成功，共 ${status.dataCount ?? 0} 条数据`)
        emit('success', logCode)
      } else if (status.status === 0) {
        // 失败
        exporting.value = false
        const msg = status.errorMsg ?? '导出失败'
        ElMessage.error(msg)
        emit('error', msg)
      } else {
        // 仍在进行中，继续轮询
        poll(logCode)
      }
    } catch (e: any) {
      // 网络异常不立即放弃，继续轮询
      poll(logCode)
    }
  }, props.pollInterval)
}

// ── 下载触发 ──────────────────────────────────────────────────────────────────

function triggerDownload(logCode: string, fileName: string) {
  const url = getDownloadUrl(logCode)
  const a = document.createElement('a')
  a.href = url
  a.download = fileName
  document.body.appendChild(a)
  a.click()
  document.body.removeChild(a)
}

// ── 清理 ──────────────────────────────────────────────────────────────────────

function clearPoll() {
  if (pollTimer !== null) {
    clearTimeout(pollTimer)
    pollTimer = null
  }
}

import { onUnmounted } from 'vue'
onUnmounted(clearPoll)
</script>
