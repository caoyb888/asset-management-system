<template>
  <div class="holo-login">
    <!-- 主 Canvas：粒子数字流 + 波形 -->
    <canvas ref="bgCanvas" class="bg-canvas" />

    <!-- 扫描线遮罩 -->
    <div class="scanlines" />

    <!-- 全屏 HUD 装饰层 -->
    <div class="hud-overlay">
      <!-- 四角 HUD 框 -->
      <div class="corner corner-tl" />
      <div class="corner corner-tr" />
      <div class="corner corner-bl" />
      <div class="corner corner-br" />

      <!-- 顶部状态栏 -->
      <div class="hud-topbar">
        <div class="hud-section">
          <span class="hud-label">SYS</span>
          <span class="hud-value blink-slow">ONLINE</span>
        </div>
        <div class="hud-center-title">
          <span class="sys-id">CZAMS-OS v1.0.0</span>
          <span class="sys-status">// 产城资产管理系统 //</span>
        </div>
        <div class="hud-section">
          <span class="hud-label">TIME</span>
          <span class="hud-value">{{ currentTime }}</span>
        </div>
      </div>

      <!-- 底部状态栏 -->
      <div class="hud-bottombar">
        <div class="data-stream">
          <span v-for="(n, i) in streamNums" :key="i" class="stream-char" :style="{ animationDelay: i * 0.1 + 's' }">{{ n }}</span>
        </div>
        <span class="sys-ready">SYSTEM READY · AUTH MODULE ACTIVE</span>
        <div class="data-stream">
          <span v-for="(n, i) in streamNums2" :key="i" class="stream-char" :style="{ animationDelay: i * 0.08 + 's' }">{{ n }}</span>
        </div>
      </div>
    </div>

    <!-- 主体内容区 -->
    <div class="main-content">
      <!-- 左侧全息数据面板 -->
      <div class="holo-panel">
        <!-- 旋转雷达环 -->
        <div class="radar-container">
          <div class="radar-ring ring-1" />
          <div class="radar-ring ring-2" />
          <div class="radar-ring ring-3" />
          <div class="radar-sweep" />
          <div class="radar-center">
            <svg width="56" height="56" viewBox="0 0 48 48" fill="none">
              <rect width="48" height="48" rx="10" fill="rgba(0,240,255,0.1)" stroke="rgba(0,240,255,0.4)" stroke-width="1" />
              <path d="M10 34V20l14-8 14 8v14l-14 8-14-8z" fill="none" stroke="#00f0ff" stroke-width="1.5" stroke-linejoin="round" />
              <path d="M10 20l14 8 14-8" stroke="#00f0ff" stroke-width="1.5" stroke-linejoin="round" />
              <line x1="24" y1="28" x2="24" y2="42" stroke="#00f0ff" stroke-width="1.5" />
            </svg>
          </div>
          <!-- 雷达点 -->
          <div v-for="(dot, i) in radarDots" :key="i" class="radar-dot" :style="dot.style" />
        </div>

        <h1 class="holo-title">
          <span class="glitch" data-text="产城资产管理系统">产城资产管理系统</span>
        </h1>
        <p class="holo-subtitle">URBAN · ASSET · INTELLIGENCE PLATFORM</p>

        <!-- 波形图 -->
        <div class="waveform-container">
          <div class="wf-label">DATA FLOW</div>
          <svg class="waveform" viewBox="0 0 400 60" preserveAspectRatio="none">
            <defs>
              <linearGradient id="wfGrad" x1="0" y1="0" x2="1" y2="0">
                <stop offset="0%" stop-color="transparent" />
                <stop offset="30%" stop-color="#00f0ff" />
                <stop offset="70%" stop-color="#00f0ff" />
                <stop offset="100%" stop-color="transparent" />
              </linearGradient>
            </defs>
            <polyline :points="wavePoints" fill="none" stroke="url(#wfGrad)" stroke-width="1.5" />
            <polyline :points="wavePoints2" fill="none" stroke="rgba(0,240,255,0.3)" stroke-width="1" />
          </svg>
        </div>

        <!-- 实时数据指标 -->
        <div class="metrics-grid">
          <div v-for="m in metrics" :key="m.key" class="metric-card">
            <div class="metric-label">{{ m.label }}</div>
            <div class="metric-bar">
              <div class="metric-fill" :style="{ width: m.value + '%', background: m.color }" />
            </div>
            <div class="metric-value" :style="{ color: m.color }">{{ m.display }}</div>
          </div>
        </div>

        <!-- 底部统计 -->
        <div class="holo-stats">
          <div v-for="s in stats" :key="s.label" class="holo-stat">
            <div class="stat-icon">{{ s.icon }}</div>
            <div class="stat-num">
              <span class="count-up">{{ s.count }}</span><span class="stat-unit">{{ s.unit }}</span>
            </div>
            <div class="stat-label">{{ s.label }}</div>
          </div>
        </div>
      </div>

      <!-- 中间分隔线 -->
      <div class="holo-divider">
        <div class="divider-line" />
        <div class="divider-hex">⬡</div>
        <div class="divider-line" />
      </div>

      <!-- 右侧登录面板 -->
      <div class="auth-panel">
        <!-- 面板头部 -->
        <div class="auth-header">
          <div class="auth-badge">
            <span class="badge-dot" />
            <span>AUTH TERMINAL</span>
          </div>
          <h2 class="auth-title">身份验证</h2>
          <p class="auth-subtitle">IDENTITY VERIFICATION REQUIRED</p>
        </div>

        <!-- 扫描进度条 -->
        <div class="scan-bar">
          <div class="scan-fill" />
          <span class="scan-text">SCANNING...</span>
        </div>

        <!-- 登录表单 -->
        <el-form
          ref="formRef"
          :model="form"
          :rules="rules"
          size="large"
          class="auth-form"
          @keyup.enter="handleLogin"
        >
          <div class="field-group">
            <div class="field-label">
              <span class="field-icon">◈</span>
              <span>USER IDENTIFIER</span>
            </div>
            <el-form-item prop="username">
              <el-input
                v-model="form.username"
                placeholder="输入用户名"
                data-testid="username"
                clearable
                class="holo-input"
              >
                <template #prefix>
                  <span class="input-prefix-icon">⬡</span>
                </template>
              </el-input>
            </el-form-item>
          </div>

          <div class="field-group">
            <div class="field-label">
              <span class="field-icon">◈</span>
              <span>ACCESS CODE</span>
            </div>
            <el-form-item prop="password">
              <el-input
                v-model="form.password"
                type="password"
                placeholder="输入密码"
                data-testid="password"
                show-password
                class="holo-input"
              >
                <template #prefix>
                  <span class="input-prefix-icon">⬡</span>
                </template>
              </el-input>
            </el-form-item>
          </div>

          <div class="form-meta">
            <el-checkbox v-model="rememberMe" class="holo-check">保持登录</el-checkbox>
            <span class="security-badge">
              <span class="lock-icon">🔒</span> SM2 加密
            </span>
          </div>

          <el-form-item>
            <button
              class="holo-btn"
              :class="{ loading: loading }"
              :disabled="loading"
              data-testid="login-btn"
              @click.prevent="handleLogin"
            >
              <span class="btn-bg" />
              <span class="btn-scan" />
              <span class="btn-text">
                <template v-if="!loading">
                  <span class="btn-icon">▶</span>
                  INITIATE LOGIN
                </template>
                <template v-else>
                  <span class="btn-spinner">◌</span>
                  AUTHENTICATING...
                </template>
              </span>
            </button>
          </el-form-item>
        </el-form>

        <!-- 底部提示 -->
        <div class="auth-footer">
          <div class="demo-hint">
            <span class="hint-label">DEMO ACCESS</span>
            <span class="hint-val">admin / admin123</span>
          </div>
          <div class="security-level">
            <div v-for="i in 5" :key="i" class="sec-bar" :class="{ active: i <= 4 }" />
            <span>SEC LV.4</span>
          </div>
        </div>

        <div class="copyright">© 2026 产城公司 · CZAMS PLATFORM</div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted, onUnmounted, computed } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, type FormInstance, type FormRules } from 'element-plus'
import { useUserStore } from '@/store/modules/user'

const router = useRouter()
const userStore = useUserStore()

const formRef = ref<FormInstance>()
const bgCanvas = ref<HTMLCanvasElement>()
const loading = ref(false)
const rememberMe = ref(false)

const form = reactive({ username: '', password: '' })

const rules: FormRules = {
  username: [{ required: true, message: '请输入用户名', trigger: 'blur' }],
  password: [
    { required: true, message: '请输入密码', trigger: 'blur' },
    { min: 6, message: '密码长度不能少于6位', trigger: 'blur' },
  ],
}

// ── 时间 ────────────────────────────────────────────────
const currentTime = ref('')
let timeTimer: ReturnType<typeof setInterval>

function updateTime() {
  const now = new Date()
  currentTime.value = now.toLocaleTimeString('en-US', { hour12: false })
}

// ── 数字流 ──────────────────────────────────────────────
const chars = '0123456789ABCDEF'
function randChars(n: number) {
  return Array.from({ length: n }, () => chars[Math.floor(Math.random() * chars.length)])
}
const streamNums = ref(randChars(20))
const streamNums2 = ref(randChars(20))
let streamTimer: ReturnType<typeof setInterval>

// ── 雷达点 ──────────────────────────────────────────────
const radarDots = computed(() =>
  Array.from({ length: 6 }, (_, i) => {
    const angle = (i * 60 + 15) * (Math.PI / 180)
    const r = 55 + Math.random() * 30
    const x = 50 + r * Math.cos(angle)
    const y = 50 + r * Math.sin(angle)
    return {
      style: {
        left: x + '%',
        top: y + '%',
        animationDelay: (i * 0.4) + 's',
      },
    }
  })
)

// ── 波形 ────────────────────────────────────────────────
const waveOffset = ref(0)
let waveTimer: ReturnType<typeof setInterval>

const wavePoints = computed(() => {
  const pts: string[] = []
  for (let x = 0; x <= 400; x += 4) {
    const y = 30
      + Math.sin((x / 40) + waveOffset.value) * 12
      + Math.sin((x / 15) + waveOffset.value * 1.7) * 5
      + Math.sin((x / 8) + waveOffset.value * 0.5) * 3
    pts.push(`${x},${y}`)
  }
  return pts.join(' ')
})

const wavePoints2 = computed(() => {
  const pts: string[] = []
  for (let x = 0; x <= 400; x += 4) {
    const y = 30
      + Math.cos((x / 35) + waveOffset.value * 1.2) * 8
      + Math.sin((x / 20) + waveOffset.value * 0.9) * 4
    pts.push(`${x},${y}`)
  }
  return pts.join(' ')
})

// ── 指标数据 ────────────────────────────────────────────
const metrics = reactive([
  { key: 'rental', label: 'RENTAL RATE', value: 87, display: '87.3%', color: '#00f0ff' },
  { key: 'occupy', label: 'OCCUPANCY', value: 92, display: '92.1%', color: '#00ff88' },
  { key: 'collect', label: 'COLLECTION', value: 78, display: '78.6%', color: '#ff8800' },
])

// ── 底部统计 ────────────────────────────────────────────
const stats = [
  { icon: '◉', count: '8', unit: '+', label: '核心模块' },
  { icon: '◉', count: '100', unit: '%', label: '全流程覆盖' },
  { icon: '◉', count: '7×24', unit: 'h', label: '实时监控' },
]

// ── Canvas 粒子数字雨 ───────────────────────────────────
let animFrame: number
let canvasCtx: CanvasRenderingContext2D | null = null
const columns: number[] = []
const fontSize = 12
let canvasW = 0
let canvasH = 0

function initCanvas() {
  const canvas = bgCanvas.value
  if (!canvas) return
  canvasW = canvas.width = window.innerWidth
  canvasH = canvas.height = window.innerHeight
  canvasCtx = canvas.getContext('2d')
  const colCount = Math.floor(canvasW / fontSize)
  columns.length = 0
  for (let i = 0; i < colCount; i++) {
    columns.push(Math.floor(Math.random() * canvasH / fontSize) * -1)
  }
}

function drawCanvas() {
  const ctx = canvasCtx
  if (!ctx) return

  ctx.fillStyle = 'rgba(0, 6, 20, 0.05)'
  ctx.fillRect(0, 0, canvasW, canvasH)

  for (let i = 0; i < columns.length; i++) {
    const char = chars[Math.floor(Math.random() * chars.length)]
    const x = i * fontSize
    const y = columns[i] * fontSize

    // 主字符青色高亮
    const alpha = Math.random() > 0.9 ? 1 : 0.3 + Math.random() * 0.3
    ctx.fillStyle = `rgba(0, 240, 255, ${alpha})`
    ctx.font = `${fontSize}px "Courier New", monospace`
    ctx.fillText(char, x, y)

    if (y > canvasH && Math.random() > 0.975) {
      columns[i] = 0
    }
    columns[i]++
  }

  animFrame = requestAnimationFrame(drawCanvas)
}

function onResize() {
  initCanvas()
}

// ── 登录 ────────────────────────────────────────────────
async function handleLogin() {
  const valid = await formRef.value?.validate().catch(() => false)
  if (!valid) return

  loading.value = true
  try {
    await userStore.login({ username: form.username, password: form.password })
    await userStore.getInfo()
    router.push('/dashboard')
  } catch {
    ElMessage.error('用户名或密码错误')
  } finally {
    loading.value = false
  }
}

// ── 生命周期 ────────────────────────────────────────────
onMounted(() => {
  updateTime()
  timeTimer = setInterval(updateTime, 1000)

  streamTimer = setInterval(() => {
    streamNums.value = randChars(20)
    streamNums2.value = randChars(20)
  }, 120)

  waveTimer = setInterval(() => {
    waveOffset.value += 0.08
  }, 30)

  // 指标随机抖动
  setInterval(() => {
    metrics[0].value = 85 + Math.random() * 5
    metrics[0].display = metrics[0].value.toFixed(1) + '%'
    metrics[1].value = 90 + Math.random() * 4
    metrics[1].display = metrics[1].value.toFixed(1) + '%'
    metrics[2].value = 76 + Math.random() * 5
    metrics[2].display = metrics[2].value.toFixed(1) + '%'
  }, 2000)

  initCanvas()
  drawCanvas()
  window.addEventListener('resize', onResize)
})

onUnmounted(() => {
  clearInterval(timeTimer)
  clearInterval(streamTimer)
  clearInterval(waveTimer)
  cancelAnimationFrame(animFrame)
  window.removeEventListener('resize', onResize)
})
</script>

<style scoped lang="scss">
/* ═══════════════════════════════════════════
   基础变量
═══════════════════════════════════════════ */
:root {
  --cyan: #00f0ff;
  --cyan-dim: rgba(0, 240, 255, 0.15);
  --green: #00ff88;
  --orange: #ff8800;
  --bg: #000614;
}

/* ═══════════════════════════════════════════
   根容器
═══════════════════════════════════════════ */
.holo-login {
  min-height: 100vh;
  background: #000614;
  position: relative;
  overflow: hidden;
  display: flex;
  flex-direction: column;
  font-family: 'Courier New', 'Consolas', monospace;
  color: #00f0ff;
}

/* ═══════════════════════════════════════════
   Canvas 背景
═══════════════════════════════════════════ */
.bg-canvas {
  position: fixed;
  inset: 0;
  z-index: 0;
  opacity: 0.35;
}

/* ═══════════════════════════════════════════
   扫描线
═══════════════════════════════════════════ */
.scanlines {
  position: fixed;
  inset: 0;
  z-index: 1;
  pointer-events: none;
  background: repeating-linear-gradient(
    0deg,
    transparent,
    transparent 2px,
    rgba(0, 240, 255, 0.015) 2px,
    rgba(0, 240, 255, 0.015) 4px
  );
  animation: scanMove 8s linear infinite;
}

@keyframes scanMove {
  0% { background-position: 0 0; }
  100% { background-position: 0 100px; }
}

/* ═══════════════════════════════════════════
   HUD 遮罩层
═══════════════════════════════════════════ */
.hud-overlay {
  position: fixed;
  inset: 0;
  z-index: 2;
  pointer-events: none;
}

/* 四角装饰 */
.corner {
  position: absolute;
  width: 40px;
  height: 40px;
  border-color: rgba(0, 240, 255, 0.6);
  border-style: solid;
}
.corner-tl { top: 16px; left: 16px; border-width: 2px 0 0 2px; }
.corner-tr { top: 16px; right: 16px; border-width: 2px 2px 0 0; }
.corner-bl { bottom: 16px; left: 16px; border-width: 0 0 2px 2px; }
.corner-br { bottom: 16px; right: 16px; border-width: 0 2px 2px 0; }

/* 顶部状态栏 */
.hud-topbar {
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  height: 48px;
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 0 60px;
  background: linear-gradient(to bottom, rgba(0, 240, 255, 0.05), transparent);
  border-bottom: 1px solid rgba(0, 240, 255, 0.1);
}

.hud-section {
  display: flex;
  gap: 8px;
  align-items: center;
  font-size: 11px;
}

.hud-label {
  color: rgba(0, 240, 255, 0.4);
  letter-spacing: 2px;
}

.hud-value {
  color: #00f0ff;
  letter-spacing: 1px;
}

.blink-slow {
  animation: blinkSlow 2s step-end infinite;
}
@keyframes blinkSlow {
  0%, 100% { opacity: 1; }
  50% { opacity: 0.3; }
}

.hud-center-title {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 2px;
}

.sys-id {
  font-size: 12px;
  color: rgba(0, 240, 255, 0.8);
  letter-spacing: 3px;
}

.sys-status {
  font-size: 10px;
  color: rgba(0, 240, 255, 0.4);
  letter-spacing: 2px;
}

/* 底部状态栏 */
.hud-bottombar {
  position: absolute;
  bottom: 0;
  left: 0;
  right: 0;
  height: 40px;
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 0 60px;
  background: linear-gradient(to top, rgba(0, 240, 255, 0.05), transparent);
  border-top: 1px solid rgba(0, 240, 255, 0.1);
  font-size: 10px;
}

.data-stream {
  display: flex;
  gap: 2px;
}

.stream-char {
  color: rgba(0, 240, 255, 0.4);
  font-size: 10px;
  animation: streamFlicker 0.5s ease-in-out infinite alternate;
}

@keyframes streamFlicker {
  from { opacity: 0.2; }
  to { opacity: 0.8; }
}

.sys-ready {
  color: rgba(0, 255, 136, 0.6);
  letter-spacing: 2px;
  font-size: 10px;
}

/* ═══════════════════════════════════════════
   主体内容
═══════════════════════════════════════════ */
.main-content {
  position: relative;
  z-index: 3;
  flex: 1;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 0;
  padding: 60px 48px;
  min-height: 100vh;
}

/* ═══════════════════════════════════════════
   左侧全息面板
═══════════════════════════════════════════ */
.holo-panel {
  flex: 1;
  max-width: 520px;
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 28px;
}

/* 雷达 */
.radar-container {
  position: relative;
  width: 180px;
  height: 180px;
  display: flex;
  align-items: center;
  justify-content: center;
}

.radar-ring {
  position: absolute;
  border-radius: 50%;
  border: 1px solid rgba(0, 240, 255, 0.25);

  &.ring-1 { width: 180px; height: 180px; border-color: rgba(0, 240, 255, 0.15); }
  &.ring-2 { width: 130px; height: 130px; border-color: rgba(0, 240, 255, 0.25); animation: ringPulse 3s ease-in-out infinite; }
  &.ring-3 { width: 80px;  height: 80px;  border-color: rgba(0, 240, 255, 0.4);  animation: ringPulse 3s ease-in-out infinite 1.5s; }
}

@keyframes ringPulse {
  0%, 100% { border-color: rgba(0, 240, 255, 0.25); box-shadow: none; }
  50% { border-color: rgba(0, 240, 255, 0.6); box-shadow: 0 0 12px rgba(0, 240, 255, 0.2); }
}

.radar-sweep {
  position: absolute;
  width: 180px;
  height: 180px;
  border-radius: 50%;
  background: conic-gradient(
    from 0deg,
    transparent 0deg,
    rgba(0, 240, 255, 0.15) 40deg,
    rgba(0, 240, 255, 0.05) 80deg,
    transparent 90deg
  );
  animation: radarSpin 4s linear infinite;
}

@keyframes radarSpin {
  from { transform: rotate(0deg); }
  to { transform: rotate(360deg); }
}

.radar-center {
  position: relative;
  z-index: 2;
  filter: drop-shadow(0 0 12px rgba(0, 240, 255, 0.6));
}

.radar-dot {
  position: absolute;
  width: 5px;
  height: 5px;
  border-radius: 50%;
  background: #00ff88;
  box-shadow: 0 0 6px #00ff88;
  transform: translate(-50%, -50%);
  animation: dotBlink 1.5s ease-in-out infinite alternate;
}

@keyframes dotBlink {
  from { opacity: 0.3; transform: translate(-50%, -50%) scale(0.6); }
  to   { opacity: 1;   transform: translate(-50%, -50%) scale(1.2); }
}

/* 标题 */
.holo-title {
  font-size: 26px;
  font-weight: 700;
  color: #00f0ff;
  margin: 0;
  letter-spacing: 2px;
  text-align: center;
  text-shadow: 0 0 20px rgba(0, 240, 255, 0.8), 0 0 40px rgba(0, 240, 255, 0.4);
}

.glitch {
  position: relative;
  display: inline-block;

  &::before,
  &::after {
    content: attr(data-text);
    position: absolute;
    top: 0; left: 0;
    width: 100%;
    overflow: hidden;
  }
  &::before {
    left: 2px;
    color: #ff0080;
    animation: glitch1 4s infinite linear;
    clip-path: polygon(0 20%, 100% 20%, 100% 40%, 0 40%);
    opacity: 0.6;
  }
  &::after {
    left: -2px;
    color: #00ff88;
    animation: glitch2 4s infinite linear;
    clip-path: polygon(0 60%, 100% 60%, 100% 80%, 0 80%);
    opacity: 0.6;
  }
}

@keyframes glitch1 {
  0%, 85%, 100% { transform: translateX(0); opacity: 0; }
  87% { transform: translateX(-3px); opacity: 0.6; }
  89% { transform: translateX(3px); opacity: 0.6; }
  91% { transform: translateX(0); opacity: 0; }
}
@keyframes glitch2 {
  0%, 88%, 100% { transform: translateX(0); opacity: 0; }
  90% { transform: translateX(3px); opacity: 0.6; }
  92% { transform: translateX(-3px); opacity: 0.6; }
  94% { transform: translateX(0); opacity: 0; }
}

.holo-subtitle {
  font-size: 10px;
  color: rgba(0, 240, 255, 0.4);
  letter-spacing: 4px;
  margin: -16px 0 0;
  text-align: center;
}

/* 波形 */
.waveform-container {
  width: 100%;
  position: relative;
}

.wf-label {
  font-size: 9px;
  color: rgba(0, 240, 255, 0.4);
  letter-spacing: 3px;
  margin-bottom: 6px;
}

.waveform {
  width: 100%;
  height: 60px;
  display: block;
  filter: drop-shadow(0 0 4px rgba(0, 240, 255, 0.5));
}

/* 指标 */
.metrics-grid {
  width: 100%;
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.metric-card {
  display: grid;
  grid-template-columns: 120px 1fr 60px;
  align-items: center;
  gap: 12px;
}

.metric-label {
  font-size: 9px;
  color: rgba(0, 240, 255, 0.5);
  letter-spacing: 2px;
}

.metric-bar {
  height: 4px;
  background: rgba(0, 240, 255, 0.1);
  border-radius: 2px;
  overflow: hidden;
  position: relative;
}

.metric-fill {
  height: 100%;
  border-radius: 2px;
  transition: width 1s ease;
  position: relative;

  &::after {
    content: '';
    position: absolute;
    right: 0; top: 0; bottom: 0;
    width: 6px;
    background: rgba(255, 255, 255, 0.7);
    filter: blur(2px);
  }
}

.metric-value {
  font-size: 11px;
  font-weight: 700;
  text-align: right;
  letter-spacing: 1px;
}

/* 底部统计 */
.holo-stats {
  display: flex;
  gap: 32px;
  padding: 20px 24px;
  border: 1px solid rgba(0, 240, 255, 0.15);
  border-radius: 8px;
  background: rgba(0, 240, 255, 0.03);
  width: 100%;
  justify-content: space-around;
  position: relative;

  &::before {
    content: 'SYSTEM METRICS';
    position: absolute;
    top: -8px;
    left: 16px;
    font-size: 9px;
    color: rgba(0, 240, 255, 0.4);
    background: #000614;
    padding: 0 6px;
    letter-spacing: 2px;
  }
}

.holo-stat {
  text-align: center;
}

.stat-icon {
  font-size: 14px;
  color: rgba(0, 240, 255, 0.4);
  margin-bottom: 4px;
}

.stat-num {
  font-size: 22px;
  font-weight: 700;
  color: #00f0ff;
  line-height: 1;
  text-shadow: 0 0 10px rgba(0, 240, 255, 0.6);
}

.stat-unit {
  font-size: 11px;
  color: rgba(0, 240, 255, 0.5);
}

.stat-label {
  font-size: 9px;
  color: rgba(0, 240, 255, 0.4);
  letter-spacing: 1px;
  margin-top: 4px;
}

/* ═══════════════════════════════════════════
   中间分隔
═══════════════════════════════════════════ */
.holo-divider {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 12px;
  padding: 0 32px;
  align-self: stretch;
  justify-content: center;
}

.divider-line {
  flex: 1;
  width: 1px;
  background: linear-gradient(to bottom, transparent, rgba(0, 240, 255, 0.3), transparent);
}

.divider-hex {
  color: rgba(0, 240, 255, 0.4);
  font-size: 20px;
  animation: hexSpin 8s linear infinite;
}

@keyframes hexSpin {
  from { transform: rotate(0); }
  to { transform: rotate(360deg); }
}

/* ═══════════════════════════════════════════
   右侧登录面板
═══════════════════════════════════════════ */
.auth-panel {
  width: 400px;
  display: flex;
  flex-direction: column;
  gap: 20px;
  position: relative;

  &::before {
    content: '';
    position: absolute;
    inset: -20px;
    border: 1px solid rgba(0, 240, 255, 0.08);
    border-radius: 12px;
    pointer-events: none;
  }
}

.auth-header {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.auth-badge {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 10px;
  color: rgba(0, 240, 255, 0.5);
  letter-spacing: 3px;
}

.badge-dot {
  width: 6px;
  height: 6px;
  border-radius: 50%;
  background: #00ff88;
  box-shadow: 0 0 6px #00ff88;
  animation: dotBlink 1s ease-in-out infinite alternate;
}

.auth-title {
  font-size: 28px;
  font-weight: 700;
  color: #fff;
  margin: 0;
  letter-spacing: 2px;
  text-shadow: 0 0 20px rgba(0, 240, 255, 0.5);
}

.auth-subtitle {
  font-size: 10px;
  color: rgba(0, 240, 255, 0.35);
  margin: 0;
  letter-spacing: 4px;
}

/* 扫描条 */
.scan-bar {
  height: 24px;
  border: 1px solid rgba(0, 240, 255, 0.2);
  border-radius: 3px;
  background: rgba(0, 240, 255, 0.03);
  position: relative;
  overflow: hidden;
  display: flex;
  align-items: center;
}

.scan-fill {
  position: absolute;
  left: -100%;
  top: 0; bottom: 0;
  width: 60%;
  background: linear-gradient(to right, transparent, rgba(0, 240, 255, 0.3), transparent);
  animation: scanFill 2.5s ease-in-out infinite;
}

@keyframes scanFill {
  0% { left: -60%; }
  100% { left: 140%; }
}

.scan-text {
  position: relative;
  z-index: 1;
  font-size: 9px;
  color: rgba(0, 240, 255, 0.5);
  letter-spacing: 3px;
  padding: 0 12px;
  animation: blinkSlow 1.5s step-end infinite;
}

/* 表单 */
.auth-form {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.field-group {
  margin-bottom: 4px;
}

.field-label {
  display: flex;
  align-items: center;
  gap: 6px;
  font-size: 9px;
  color: rgba(0, 240, 255, 0.5);
  letter-spacing: 2px;
  margin-bottom: 6px;
}

.field-icon {
  color: #00f0ff;
}

.holo-input {
  :deep(.el-input__wrapper) {
    background: rgba(0, 240, 255, 0.04);
    border: 1px solid rgba(0, 240, 255, 0.25);
    border-radius: 4px;
    box-shadow: none;
    transition: all 0.3s;
    position: relative;

    &:hover {
      border-color: rgba(0, 240, 255, 0.5);
      background: rgba(0, 240, 255, 0.07);
    }

    &.is-focus {
      border-color: #00f0ff;
      background: rgba(0, 240, 255, 0.08);
      box-shadow: 0 0 0 2px rgba(0, 240, 255, 0.12), 0 0 16px rgba(0, 240, 255, 0.15);
    }
  }

  :deep(.el-input__inner) {
    color: #e0faff;
    font-family: 'Courier New', monospace;
    font-size: 14px;
    letter-spacing: 1px;

    &::placeholder {
      color: rgba(0, 240, 255, 0.25);
      font-size: 12px;
    }
  }

  :deep(.el-input__prefix-inner) {
    color: rgba(0, 240, 255, 0.5);
  }
}

.input-prefix-icon {
  font-size: 14px;
  color: rgba(0, 240, 255, 0.4);
}

:deep(.el-form-item) {
  margin-bottom: 0;
}

:deep(.el-form-item__error) {
  color: #ff4060;
  font-family: 'Courier New', monospace;
  font-size: 11px;
}

.form-meta {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin: 4px 0 12px;

  :deep(.el-checkbox__label) {
    color: rgba(0, 240, 255, 0.5);
    font-family: 'Courier New', monospace;
    font-size: 12px;
    letter-spacing: 1px;
  }

  :deep(.el-checkbox__inner) {
    background: rgba(0, 240, 255, 0.05);
    border-color: rgba(0, 240, 255, 0.3);
    border-radius: 2px;
  }

  :deep(.el-checkbox.is-checked .el-checkbox__inner) {
    background: rgba(0, 240, 255, 0.2);
    border-color: #00f0ff;
  }
}

.security-badge {
  font-size: 11px;
  color: rgba(0, 255, 136, 0.6);
  letter-spacing: 1px;
  display: flex;
  align-items: center;
  gap: 4px;
}

/* 登录按钮 */
.holo-btn {
  width: 100%;
  height: 52px;
  border: 1px solid rgba(0, 240, 255, 0.4);
  border-radius: 4px;
  background: transparent;
  cursor: pointer;
  position: relative;
  overflow: hidden;
  font-family: 'Courier New', monospace;
  font-size: 13px;
  letter-spacing: 3px;
  color: #00f0ff;
  transition: all 0.3s;

  &:hover:not(:disabled) {
    border-color: #00f0ff;
    box-shadow: 0 0 20px rgba(0, 240, 255, 0.3), inset 0 0 20px rgba(0, 240, 255, 0.05);

    .btn-scan {
      animation-duration: 0.8s;
    }
  }

  &:disabled {
    cursor: not-allowed;
    opacity: 0.7;
  }

  &.loading .btn-spinner {
    animation: spin 0.8s linear infinite;
  }
}

.btn-bg {
  position: absolute;
  inset: 0;
  background: linear-gradient(135deg, rgba(0, 240, 255, 0.08), rgba(0, 100, 200, 0.08));
}

.btn-scan {
  position: absolute;
  top: 0; left: -100%; bottom: 0;
  width: 50%;
  background: linear-gradient(to right, transparent, rgba(0, 240, 255, 0.2), transparent);
  animation: btnScan 2s linear infinite;
}

@keyframes btnScan {
  0% { left: -60%; }
  100% { left: 140%; }
}

.btn-text {
  position: relative;
  z-index: 1;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 10px;
}

.btn-icon {
  font-size: 12px;
  animation: btnIconBlink 1s step-end infinite;
}

@keyframes btnIconBlink {
  0%, 100% { opacity: 1; }
  50% { opacity: 0; }
}

@keyframes spin {
  from { transform: rotate(0); }
  to { transform: rotate(360deg); }
}

/* 底部 */
.auth-footer {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 14px 16px;
  border: 1px solid rgba(0, 240, 255, 0.1);
  border-radius: 4px;
  background: rgba(0, 240, 255, 0.02);
}

.demo-hint {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.hint-label {
  font-size: 9px;
  color: rgba(0, 240, 255, 0.3);
  letter-spacing: 2px;
}

.hint-val {
  font-size: 12px;
  color: rgba(0, 240, 255, 0.6);
  letter-spacing: 1px;
}

.security-level {
  display: flex;
  align-items: center;
  gap: 4px;
  font-size: 9px;
  color: rgba(0, 240, 255, 0.4);
  letter-spacing: 1px;
}

.sec-bar {
  width: 6px;
  height: 16px;
  border: 1px solid rgba(0, 240, 255, 0.2);
  border-radius: 2px;

  &.active {
    background: rgba(0, 240, 255, 0.4);
    border-color: rgba(0, 240, 255, 0.6);
    box-shadow: 0 0 4px rgba(0, 240, 255, 0.3);
  }
}

.copyright {
  font-size: 9px;
  color: rgba(0, 240, 255, 0.2);
  text-align: center;
  letter-spacing: 2px;
}

/* ═══════════════════════════════════════════
   响应式
═══════════════════════════════════════════ */
@media (max-width: 960px) {
  .holo-panel { display: none; }
  .holo-divider { display: none; }
  .main-content { justify-content: center; padding: 80px 24px 60px; }
  .auth-panel { width: 100%; max-width: 420px; }
}
</style>
