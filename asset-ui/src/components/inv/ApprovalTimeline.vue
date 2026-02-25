<template>
  <div class="approval-timeline">
    <!-- 当前状态徽标 -->
    <div class="status-header">
      <span class="status-label">当前状态：</span>
      <el-tag :type="CURRENT_STATUS_MAP[currentStatus]?.type ?? 'info'" size="large">
        {{ CURRENT_STATUS_MAP[currentStatus]?.label ?? '未知' }}
      </el-tag>
    </div>

    <!-- 时间线主体 -->
    <el-timeline class="timeline-body">
      <el-timeline-item
        v-for="(item, idx) in displayItems"
        :key="idx"
        :timestamp="item.time || ''"
        :color="item.color"
        placement="top"
        size="large"
      >
        <div class="timeline-card" :class="`timeline-card--${item.type}`">
          <div class="timeline-card__header">
            <el-icon class="timeline-card__icon" :size="16">
              <component :is="item.iconComponent" />
            </el-icon>
            <span class="timeline-card__action">{{ item.label }}</span>
          </div>
          <div v-if="item.operator" class="timeline-card__meta">
            <span class="meta-item">
              <el-icon :size="12"><User /></el-icon>
              {{ item.operator }}
            </span>
          </div>
          <div v-if="item.comment" class="timeline-card__comment">
            {{ item.comment }}
          </div>
        </div>
      </el-timeline-item>
    </el-timeline>

    <el-empty
      v-if="displayItems.length === 0"
      description="暂无审批记录"
      :image-size="50"
    />
  </div>
</template>

<script setup lang="ts">
import { computed, resolveComponent } from 'vue'
import { User, CircleCheck, CircleClose, Clock, EditPen } from '@element-plus/icons-vue'

// ─── 类型定义（可被父组件复用） ────────────────────────────
export interface ApprovalRecord {
  /** 操作类型 */
  action: 'create' | 'submit' | 'approve' | 'reject'
  /** 操作人姓名 */
  operator?: string
  /** 审批意见/备注 */
  comment?: string
  /** 操作时间（ISO 字符串） */
  time?: string
}

// ─── Props ────────────────────────────────────────────────
const props = withDefaults(defineProps<{
  /** 审批历史记录列表；为空时基于 currentStatus 派生简化时间线 */
  records?: ApprovalRecord[]
  /** 当前单据状态：0草稿 1审批中 2已通过 3已驳回 */
  currentStatus?: number
}>(), {
  records: () => [],
  currentStatus: 0,
})

// ─── 状态字典 ─────────────────────────────────────────────
type TagType = 'primary' | 'success' | 'warning' | 'danger' | 'info' | undefined

const CURRENT_STATUS_MAP: Record<number, { label: string; type: TagType }> = {
  0: { label: '草稿', type: 'info' },
  1: { label: '审批中', type: 'warning' },
  2: { label: '已通过', type: 'success' },
  3: { label: '已驳回', type: 'danger' },
}

// ─── 节点配置 ─────────────────────────────────────────────
interface TimelineDisplayItem {
  label: string
  type: 'create' | 'submit' | 'approve' | 'reject' | 'pending'
  color: string
  iconComponent: unknown
  operator?: string
  comment?: string
  time?: string
}

const ACTION_CONFIG: Record<string, Omit<TimelineDisplayItem, 'operator' | 'comment' | 'time'>> = {
  create: {
    label: '单据已创建',
    type: 'create',
    color: '#909399',
    iconComponent: EditPen,
  },
  submit: {
    label: '已提交审批',
    type: 'submit',
    color: '#409eff',
    iconComponent: Clock,
  },
  approve: {
    label: '审批已通过',
    type: 'approve',
    color: '#67c23a',
    iconComponent: CircleCheck,
  },
  reject: {
    label: '审批已驳回',
    type: 'reject',
    color: '#f56c6c',
    iconComponent: CircleClose,
  },
  pending: {
    label: '等待审批结果',
    type: 'pending',
    color: '#e6a23c',
    iconComponent: Clock,
  },
}

// ─── 派生显示节点 ─────────────────────────────────────────
const displayItems = computed<TimelineDisplayItem[]>(() => {
  // ① 有真实记录时直接渲染
  if (props.records && props.records.length > 0) {
    return props.records.map((r) => {
      const cfg = ACTION_CONFIG[r.action] ?? ACTION_CONFIG.create
      return {
        ...cfg,
        operator: r.operator,
        comment: r.comment,
        time: r.time ? formatTime(r.time) : undefined,
      }
    })
  }

  // ② 无记录时基于 currentStatus 派生简化时间线
  const items: TimelineDisplayItem[] = [
    { ...ACTION_CONFIG.create },
  ]

  if (props.currentStatus >= 1) {
    items.push({ ...ACTION_CONFIG.submit })
  }

  if (props.currentStatus === 1) {
    items.push({ ...ACTION_CONFIG.pending })
  } else if (props.currentStatus === 2) {
    items.push({ ...ACTION_CONFIG.approve })
  } else if (props.currentStatus === 3) {
    items.push({ ...ACTION_CONFIG.reject })
  }

  return items
})

// ─── 工具 ─────────────────────────────────────────────────
function formatTime(iso: string): string {
  try {
    const d = new Date(iso)
    const pad = (n: number) => String(n).padStart(2, '0')
    return `${d.getFullYear()}-${pad(d.getMonth() + 1)}-${pad(d.getDate())} ` +
      `${pad(d.getHours())}:${pad(d.getMinutes())}`
  } catch {
    return iso
  }
}
</script>

<style scoped>
.approval-timeline {
  padding: 8px 0;
}

.status-header {
  display: flex;
  align-items: center;
  gap: 10px;
  margin-bottom: 20px;
}

.status-label {
  font-size: 14px;
  color: #606266;
  font-weight: 500;
}

.timeline-body {
  padding-left: 8px;
}

/* 时间线节点卡片 */
.timeline-card {
  background: #fafafa;
  border: 1px solid #e4e7ed;
  border-radius: 6px;
  padding: 10px 14px;
  margin-bottom: 2px;
  transition: border-color 0.2s;
}

.timeline-card--approve {
  border-color: #b3e19d;
  background: #f0f9eb;
}

.timeline-card--reject {
  border-color: #fab6b6;
  background: #fef0f0;
}

.timeline-card--submit,
.timeline-card--pending {
  border-color: #a0cfff;
  background: #ecf5ff;
}

.timeline-card__header {
  display: flex;
  align-items: center;
  gap: 6px;
}

.timeline-card__icon {
  flex-shrink: 0;
}

.timeline-card__action {
  font-size: 14px;
  font-weight: 600;
  color: #303133;
}

.timeline-card__meta {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-top: 6px;
}

.meta-item {
  display: inline-flex;
  align-items: center;
  gap: 3px;
  font-size: 12px;
  color: #909399;
}

.timeline-card__comment {
  margin-top: 8px;
  padding: 6px 10px;
  background: rgba(0, 0, 0, 0.03);
  border-left: 3px solid #dcdfe6;
  border-radius: 2px;
  font-size: 13px;
  color: #606266;
  line-height: 1.6;
  white-space: pre-wrap;
  word-break: break-all;
}
</style>
