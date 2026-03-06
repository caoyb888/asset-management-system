<template>
  <div class="revenue-form-page">
    <!-- 页面头部 -->
    <div class="page-header-bar">
      <div class="bar-left">
        <el-button :icon="ArrowLeft" text @click="$router.back()">返回</el-button>
        <span class="bar-divider" />
        <span class="bar-title">营收日历填报</span>
      </div>
    </div>

    <el-row :gutter="16">
      <!-- 左侧：条件选择 + 日历 -->
      <el-col :span="17">
        <el-card shadow="never" class="calendar-card">
          <!-- 合同 + 月份选择 -->
          <el-form ref="formRef" :model="form" :rules="formRules" inline label-width="80px" style="margin-bottom:8px">
            <el-form-item label="合同ID" prop="contractId" required>
              <el-input-number
                v-model="form.contractId"
                :min="1"
                :controls="false"
                placeholder="输入合同ID"
                style="width:150px"
                @change="onContractChange"
              />
            </el-form-item>
            <el-form-item label="月份" prop="reportMonth" required>
              <el-date-picker
                v-model="form.reportMonth"
                type="month"
                value-format="YYYY-MM"
                placeholder="选择月份"
                style="width:150px"
                @change="loadDailyData"
              />
            </el-form-item>
          </el-form>

          <!-- 图例说明 -->
          <div class="legend-row">
            <span><span class="dot dot-filled" />已填报</span>
            <span><span class="dot dot-missing" />未填报</span>
            <span><span class="dot dot-future" />未来日期</span>
            <span><span class="dot dot-today" />今日</span>
          </div>

          <!-- 日历网格 -->
          <div v-if="form.contractId && form.reportMonth" v-loading="calendarLoading">
            <!-- 星期头 -->
            <div class="calendar-grid">
              <div v-for="d in ['日','一','二','三','四','五','六']" :key="d" class="cal-weekday">{{ d }}</div>
              <!-- 空白格 -->
              <div v-for="n in leadingBlanks" :key="'b'+n" class="cal-cell cal-blank" />
              <!-- 日期格 -->
              <div
                v-for="day in daysInMonth"
                :key="day"
                class="cal-cell"
                :class="cellClass(day)"
                @click="openDayDialog(day)"
              >
                <div class="cal-day-num">{{ day }}</div>
                <div v-if="dailyData[dateKey(day)]" class="cal-amount">
                  ¥{{ formatK(dailyData[dateKey(day)]) }}
                </div>
              </div>
            </div>
          </div>
          <el-empty v-else description="请先选择合同ID和月份" :image-size="80" />
        </el-card>
      </el-col>

      <!-- 右侧：月度汇总 -->
      <el-col :span="7">
        <el-card shadow="never" class="summary-card">
          <div class="summary-header">月度汇总</div>
          <el-descriptions :column="1" border size="small">
            <el-descriptions-item label="月份">{{ form.reportMonth || '—' }}</el-descriptions-item>
            <el-descriptions-item label="已填报天数">
              <el-tag type="primary">{{ filledDays }} / {{ daysInMonth }}</el-tag>
            </el-descriptions-item>
            <el-descriptions-item label="完整度">
              <el-progress
                :percentage="completePct"
                :color="completePct === 100 ? '#67C23A' : '#E6A23C'"
                :stroke-width="8"
                style="width:140px"
              />
            </el-descriptions-item>
            <el-descriptions-item label="月累计营业额">
              <span class="monthly-total">¥ {{ formatAmount(monthlyTotal) }}</span>
            </el-descriptions-item>
          </el-descriptions>

          <div style="margin-top:16px">
            <el-button
              type="success"
              style="width:100%"
              :disabled="completePct < 100"
              @click="triggerFloatingRent"
            >
              {{ completePct === 100 ? '触发浮动租金计算' : `还差 ${daysInMonth - filledDays} 天完整` }}
            </el-button>
          </div>
        </el-card>
      </el-col>
    </el-row>

    <!-- 单日录入弹窗 -->
    <el-dialog
      v-model="dayDialog.visible"
      :title="`录入营业额 - ${dayDialog.date}`"
      width="380px"
    >
      <el-form :model="dayDialog" label-width="90px">
        <el-form-item label="填报日期">
          <el-input :value="dayDialog.date" disabled />
        </el-form-item>
        <el-form-item label="营业额（元）" required>
          <el-input-number
            v-model="dayDialog.amount"
            :precision="2"
            :min="0"
            style="width:100%"
            placeholder="请输入当日营业额"
          />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dayDialog.visible = false">取消</el-button>
        <el-button type="primary" :loading="dayDialog.saving" @click="saveDayAmount">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, computed } from 'vue'
import { ElMessage } from 'element-plus'
import { useRouter } from 'vue-router'
import { ArrowLeft } from '@element-plus/icons-vue'
import { revenueReportApi, floatingRentApi } from '@/api/opr/revenue'

const router = useRouter()

// ── 表单 ────────────────────────────────────────────────────────
const formRef = ref()
const form = reactive<{ contractId: number | null; reportMonth: string }>({
  contractId: null,
  reportMonth: new Date().toISOString().slice(0, 7),
})

const formRules = {
  contractId: [{ required: true, message: '请输入合同ID', trigger: 'change' }],
  reportMonth: [{ required: true, message: '请选择月份', trigger: 'change' }],
}

// 日历数据：key=YYYY-MM-DD, value=营业额
const dailyData     = ref<Record<string, number>>({})
const calendarLoading = ref(false)

// ── 日历计算 ────────────────────────────────────────────────────
const daysInMonth = computed<number>(() => {
  if (!form.reportMonth) return 0
  const [y, m] = form.reportMonth.split('-').map(Number)
  return new Date(y, m, 0).getDate()
})

/** 月份第一天是星期几（0=日） */
const leadingBlanks = computed<number>(() => {
  if (!form.reportMonth) return 0
  const [y, m] = form.reportMonth.split('-').map(Number)
  return new Date(y, m - 1, 1).getDay()
})

const today = new Date().toISOString().slice(0, 10)

function dateKey(day: number): string {
  return `${form.reportMonth}-${String(day).padStart(2, '0')}`
}

function cellClass(day: number): Record<string, boolean> {
  const key = dateKey(day)
  const isFuture = key > today
  const isToday  = key === today
  return {
    'cal-filled':  !!dailyData.value[key],
    'cal-missing': !dailyData.value[key] && !isFuture,
    'cal-future':  isFuture && !isToday,
    'cal-today':   isToday,
    'cal-clickable': !isFuture || isToday,
  }
}

// ── 统计 ────────────────────────────────────────────────────────
const filledDays = computed(() => Object.keys(dailyData.value).length)
const monthlyTotal = computed(() => Object.values(dailyData.value).reduce((s, v) => s + Number(v), 0))
const completePct  = computed(() =>
  daysInMonth.value > 0 ? Math.round((filledDays.value / daysInMonth.value) * 100) : 0)

// ── 数据加载 ────────────────────────────────────────────────────
async function loadDailyData() {
  if (!form.contractId || !form.reportMonth) return
  calendarLoading.value = true
  try {
    const res = await revenueReportApi.dailyDetail(form.contractId, form.reportMonth) as any
    dailyData.value = res || {}
  } finally {
    calendarLoading.value = false
  }
}

function onContractChange() { loadDailyData() }

// ── 单日录入 ────────────────────────────────────────────────────
const dayDialog = reactive<{ visible: boolean; date: string; amount: number; saving: boolean; existId: number | null }>({
  visible: false, date: '', amount: 0, saving: false, existId: null,
})

function openDayDialog(day: number) {
  const key = dateKey(day)
  if (key > today) return   // 未来日期不可录入
  dayDialog.date    = key
  dayDialog.amount  = dailyData.value[key] ? Number(dailyData.value[key]) : 0
  dayDialog.visible = true
}

async function saveDayAmount() {
  if (!form.contractId) { ElMessage.warning('请先选择合同'); return }
  if (dayDialog.amount === null || dayDialog.amount === undefined) {
    ElMessage.warning('请输入营业额')
    return
  }
  dayDialog.saving = true
  try {
    await revenueReportApi.create({
      contractId: form.contractId,
      reportDate: dayDialog.date,
      revenueAmount: dayDialog.amount,
    })
    ElMessage.success('保存成功')
    dailyData.value[dayDialog.date] = dayDialog.amount
    dayDialog.visible = false
  } catch (e: any) {
    // 已存在时忽略（业务异常由拦截器处理）
  } finally {
    dayDialog.saving = false
  }
}

// ── 触发浮动租金 ────────────────────────────────────────────────
async function triggerFloatingRent() {
  await formRef.value.validate()
  if (!form.contractId || !form.reportMonth) return
  try {
    const res = await floatingRentApi.generate({
      contractId: form.contractId,
      calcMonth: form.reportMonth,
    }) as any
    ElMessage.success('浮动租金计算成功，记录ID=' + res)
    router.push(`/opr/floating-rent?contractId=${form.contractId}&calcMonth=${form.reportMonth}`)
  } catch { /* 错误由拦截器统一提示 */ }
}

// ── 格式化 ───────────────────────────────────────────────────────
function formatAmount(val: number) {
  return Number(val).toLocaleString('zh-CN', { minimumFractionDigits: 2 })
}
function formatK(val: number) {
  return Number(val) >= 10000
    ? (Number(val) / 10000).toFixed(1) + 'w'
    : Number(val).toFixed(0)
}
</script>

<style scoped lang="scss">
.revenue-form-page { display: flex; flex-direction: column; gap: 16px; }

.page-header-bar {
  display: flex; align-items: center;
  padding: 12px 20px; background: #fff;
  border-radius: 12px; border: 1px solid rgba(0, 0, 0, 0.06);
  box-shadow: 0 1px 4px rgba(0, 0, 0, 0.04);
  .bar-left { display: flex; align-items: center; gap: 12px; }
  .bar-divider { width: 1px; height: 16px; background: #e2e8f0; }
  .bar-title { font-size: 16px; font-weight: 600; color: #1e293b; }
}

.calendar-card {
  border-radius: 12px !important;
  border: 1px solid rgba(0, 0, 0, 0.06) !important;
}

.summary-card {
  border-radius: 12px !important;
  border: 1px solid rgba(0, 0, 0, 0.06) !important;
  position: sticky;
  top: 20px;
  .summary-header {
    font-size: 15px; font-weight: 600; color: #1e293b;
    margin-bottom: 14px; padding-left: 8px;
    border-left: 3px solid #3b82f6;
  }
  .monthly-total { color: #2E75B6; font-weight: 700; font-size: 16px; }
}

.legend-row {
  display: flex; gap: 16px; margin-bottom: 12px; font-size: 13px; color: #64748b;
}

.calendar-grid {
  display: grid;
  grid-template-columns: repeat(7, 1fr);
  gap: 4px;
}
.cal-weekday {
  text-align: center; font-size: 12px; color: #909399;
  padding: 4px 0; font-weight: 600;
}
.cal-blank { background: transparent; }
.cal-cell {
  min-height: 64px; border-radius: 6px; padding: 6px 8px;
  cursor: default; border: 1px solid #ebeef5; transition: all 0.15s;
}
.cal-clickable { cursor: pointer; }
.cal-clickable:hover { border-color: #409EFF; box-shadow: 0 0 0 2px rgba(64,158,255,0.2); }
.cal-filled  { background: #f0f9eb; border-color: #b3e19d; }
.cal-missing { background: #fef0f0; border-color: #fbc4c4; }
.cal-future  { background: #f5f7fa; color: #C0C4CC; }
.cal-today   { background: #ecf5ff; border-color: #409EFF; border-width: 2px; }
.cal-day-num { font-size: 13px; font-weight: 600; color: inherit; }
.cal-amount  { font-size: 11px; color: #67C23A; margin-top: 2px; }
.dot {
  display: inline-block; width: 10px; height: 10px;
  border-radius: 50%; margin-right: 4px; vertical-align: middle;
}
.dot-filled  { background: #67C23A; }
.dot-missing { background: #F56C6C; }
.dot-future  { background: #C0C4CC; }
.dot-today   { background: #409EFF; }
</style>
