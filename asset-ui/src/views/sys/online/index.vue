<template>
  <div class="page-container">
    <div class="page-header">
      <div class="page-header-left">
        <h2 class="page-title">在线用户</h2>
        <p class="page-desc">查看当前已登录的在线用户，支持强制下线操作</p>
      </div>
      <div class="page-header-right">
        <el-button :icon="Refresh" @click="load">刷新</el-button>
      </div>
    </div>

    <el-card shadow="never" class="main-card">
      <el-table :data="list" v-loading="loading" stripe>
        <el-table-column type="index" label="#" width="55" align="center" />
        <el-table-column prop="username" label="用户名" width="140" />
        <el-table-column prop="loginIp" label="登录IP" width="150" />
        <el-table-column prop="loginTime" label="登录时间" min-width="180">
          <template #default="{ row }">
            {{ formatTime(row.loginTime) }}
          </template>
        </el-table-column>
        <el-table-column label="在线时长" width="130" align="center">
          <template #default="{ row }">
            {{ calcDuration(row.loginTime) }}
          </template>
        </el-table-column>
        <el-table-column label="操作" width="100" align="center" fixed="right">
          <template #default="{ row }">
            <el-button
              type="danger"
              link
              size="small"
              @click="doKick(row)"
            >
              强制下线
            </el-button>
          </template>
        </el-table-column>
      </el-table>

      <div class="online-count">
        共 <b>{{ list.length }}</b> 位用户在线
      </div>
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Refresh } from '@element-plus/icons-vue'
import { onlineApi, type OnlineUserVO } from '@/api/sys/online'

const loading = ref(false)
const list = ref<OnlineUserVO[]>([])

async function load() {
  loading.value = true
  try {
    const res: any = await onlineApi.list()
    list.value = Array.isArray(res) ? res : (res?.records ?? [])
  } finally {
    loading.value = false
  }
}

async function doKick(row: OnlineUserVO) {
  await ElMessageBox.confirm(
    `确认强制下线用户【${row.username}】？其登录状态将立即失效。`,
    '确认操作',
    { type: 'warning' }
  )
  await onlineApi.kick(row.userId)
  ElMessage.success(`用户【${row.username}】已强制下线`)
  load()
}

/** 格式化 ISO 时间字符串 */
function formatTime(iso: string): string {
  if (!iso) return '-'
  return iso.replace('T', ' ').substring(0, 19)
}

/** 计算在线时长 */
function calcDuration(iso: string): string {
  if (!iso) return '-'
  const diff = Date.now() - new Date(iso).getTime()
  const m = Math.floor(diff / 60000)
  if (m < 1) return '不足1分钟'
  if (m < 60) return `${m} 分钟`
  const h = Math.floor(m / 60)
  const rm = m % 60
  if (h < 24) return rm > 0 ? `${h} 小时 ${rm} 分钟` : `${h} 小时`
  const d = Math.floor(h / 24)
  return `${d} 天`
}

onMounted(load)
</script>

<style scoped lang="scss">
.main-card {
  margin-top: 16px;
}

.online-count {
  margin-top: 14px;
  font-size: 13px;
  color: var(--el-text-color-secondary);
  text-align: right;
  b {
    color: var(--el-color-primary);
  }
}
</style>
