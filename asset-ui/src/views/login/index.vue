<template>
  <div class="login-page">
    <!-- 动态背景网格 -->
    <div class="bg-grid" />
    <!-- 光晕装饰 -->
    <div class="glow glow-1" />
    <div class="glow glow-2" />
    <div class="glow glow-3" />

    <!-- 左侧品牌区 -->
    <div class="brand-panel">
      <div class="brand-content">
        <div class="brand-logo">
          <svg width="48" height="48" viewBox="0 0 48 48" fill="none">
            <rect width="48" height="48" rx="12" fill="url(#logoGrad)" />
            <path d="M10 34V20l14-8 14 8v14l-14 8-14-8z" fill="none" stroke="white" stroke-width="2" stroke-linejoin="round" />
            <path d="M10 20l14 8 14-8" stroke="white" stroke-width="2" stroke-linejoin="round" />
            <line x1="24" y1="28" x2="24" y2="42" stroke="white" stroke-width="2" />
            <defs>
              <linearGradient id="logoGrad" x1="0" y1="0" x2="48" y2="48">
                <stop offset="0%" stop-color="#3b82f6" />
                <stop offset="100%" stop-color="#1d4ed8" />
              </linearGradient>
            </defs>
          </svg>
        </div>
        <h1 class="brand-name">产城资产管理系统</h1>
        <p class="brand-tagline">智慧园区 · 数字资产 · 高效运营</p>

        <div class="feature-list">
          <div class="feature-item">
            <div class="feature-icon">
              <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                <path d="M3 9l9-7 9 7v11a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2z" />
                <polyline points="9 22 9 12 15 12 15 22" />
              </svg>
            </div>
            <div>
              <div class="feature-title">全周期资产管理</div>
              <div class="feature-desc">从楼栋到商铺，全维度资产档案管理</div>
            </div>
          </div>
          <div class="feature-item">
            <div class="feature-icon">
              <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                <path d="M17 21v-2a4 4 0 0 0-4-4H5a4 4 0 0 0-4 4v2" />
                <circle cx="9" cy="7" r="4" />
                <path d="M23 21v-2a4 4 0 0 0-3-3.87" />
                <path d="M16 3.13a4 4 0 0 1 0 7.75" />
              </svg>
            </div>
            <div>
              <div class="feature-title">招商运营一体化</div>
              <div class="feature-desc">意向到签约，合同全生命周期管控</div>
            </div>
          </div>
          <div class="feature-item">
            <div class="feature-icon">
              <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                <line x1="12" y1="1" x2="12" y2="23" />
                <path d="M17 5H9.5a3.5 3.5 0 0 0 0 7h5a3.5 3.5 0 0 1 0 7H6" />
              </svg>
            </div>
            <div>
              <div class="feature-title">智能财务核算</div>
              <div class="feature-desc">应收管理、核销凭证，精准财务数据</div>
            </div>
          </div>
          <div class="feature-item">
            <div class="feature-icon">
              <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                <polyline points="22 12 18 12 15 21 9 3 6 12 2 12" />
              </svg>
            </div>
            <div>
              <div class="feature-title">实时数据看板</div>
              <div class="feature-desc">出租率、应收账款、营收趋势一屏掌握</div>
            </div>
          </div>
        </div>

        <div class="brand-stats">
          <div class="stat-item">
            <div class="stat-num">8<span>+</span></div>
            <div class="stat-label">核心模块</div>
          </div>
          <div class="stat-divider" />
          <div class="stat-item">
            <div class="stat-num">100<span>%</span></div>
            <div class="stat-label">全流程覆盖</div>
          </div>
          <div class="stat-divider" />
          <div class="stat-item">
            <div class="stat-num">7×24<span>h</span></div>
            <div class="stat-label">实时监控</div>
          </div>
        </div>
      </div>
    </div>

    <!-- 右侧登录区 -->
    <div class="form-panel">
      <div class="login-card">
        <div class="card-header">
          <h2 class="card-title">欢迎回来</h2>
          <p class="card-subtitle">请登录您的账户以继续使用</p>
        </div>

        <el-form
          ref="formRef"
          :model="form"
          :rules="rules"
          size="large"
          @keyup.enter="handleLogin"
        >
          <el-form-item prop="username">
            <el-input
              v-model="form.username"
              placeholder="请输入用户名"
              :prefix-icon="User"
              data-testid="username"
              clearable
              class="custom-input"
            />
          </el-form-item>

          <el-form-item prop="password">
            <el-input
              v-model="form.password"
              type="password"
              placeholder="请输入密码"
              :prefix-icon="Lock"
              data-testid="password"
              show-password
              class="custom-input"
            />
          </el-form-item>

          <div class="form-options">
            <el-checkbox v-model="rememberMe">记住登录状态</el-checkbox>
          </div>

          <el-form-item>
            <el-button
              type="primary"
              :loading="loading"
              class="login-btn"
              data-testid="login-btn"
              @click="handleLogin"
            >
              <span v-if="!loading">登&nbsp;&nbsp;&nbsp;录</span>
              <span v-else>登录中...</span>
            </el-button>
          </el-form-item>
        </el-form>

        <div class="login-footer">
          <span class="hint-text">演示账号：admin / admin123</span>
        </div>
      </div>

      <div class="copyright">
        © 2026 产城公司 · 资产管理平台 v1.0.0
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, type FormInstance, type FormRules } from 'element-plus'
import { User, Lock } from '@element-plus/icons-vue'
import { useUserStore } from '@/store/modules/user'

const router = useRouter()
const userStore = useUserStore()

const formRef = ref<FormInstance>()
const loading = ref(false)
const rememberMe = ref(false)

const form = reactive({
  username: '',
  password: '',
})

const rules: FormRules = {
  username: [{ required: true, message: '请输入用户名', trigger: 'blur' }],
  password: [
    { required: true, message: '请输入密码', trigger: 'blur' },
    { min: 6, message: '密码长度不能少于6位', trigger: 'blur' },
  ],
}

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
</script>

<style scoped lang="scss">
.login-page {
  min-height: 100vh;
  display: flex;
  background: #030d1e;
  position: relative;
  overflow: hidden;
}

/* 动态背景网格 */
.bg-grid {
  position: absolute;
  inset: 0;
  background-image:
    linear-gradient(rgba(59, 130, 246, 0.07) 1px, transparent 1px),
    linear-gradient(90deg, rgba(59, 130, 246, 0.07) 1px, transparent 1px);
  background-size: 50px 50px;
  pointer-events: none;
}

/* 光晕 */
.glow {
  position: absolute;
  border-radius: 50%;
  filter: blur(120px);
  pointer-events: none;
  animation: pulse 8s ease-in-out infinite;
}
.glow-1 {
  width: 600px;
  height: 600px;
  top: -200px;
  left: -100px;
  background: rgba(37, 99, 235, 0.2);
  animation-delay: 0s;
}
.glow-2 {
  width: 400px;
  height: 400px;
  top: 60%;
  left: 25%;
  background: rgba(99, 102, 241, 0.15);
  animation-delay: 3s;
}
.glow-3 {
  width: 300px;
  height: 300px;
  top: 20%;
  right: 5%;
  background: rgba(14, 165, 233, 0.1);
  animation-delay: 5s;
}
@keyframes pulse {
  0%, 100% { transform: scale(1); opacity: 1; }
  50% { transform: scale(1.1); opacity: 0.7; }
}

/* 左侧品牌区 */
.brand-panel {
  flex: 1;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 60px 64px;
  position: relative;
  z-index: 1;

  &::after {
    content: '';
    position: absolute;
    right: 0;
    top: 10%;
    height: 80%;
    width: 1px;
    background: linear-gradient(to bottom, transparent, rgba(59, 130, 246, 0.4), transparent);
  }
}

.brand-content {
  max-width: 520px;
}

.brand-logo {
  margin-bottom: 24px;
  display: flex;
  align-items: center;
  gap: 12px;

  svg {
    border-radius: 12px;
    box-shadow: 0 0 30px rgba(59, 130, 246, 0.4);
  }
}

.brand-name {
  font-size: 32px;
  font-weight: 700;
  color: #fff;
  margin: 0 0 10px;
  letter-spacing: 1px;
  background: linear-gradient(135deg, #fff 0%, #93c5fd 100%);
  -webkit-background-clip: text;
  -webkit-text-fill-color: transparent;
  background-clip: text;
}

.brand-tagline {
  font-size: 15px;
  color: rgba(148, 163, 184, 0.9);
  margin: 0 0 48px;
  letter-spacing: 3px;
  display: flex;
  align-items: center;
  gap: 8px;

  &::before, &::after {
    content: '';
    flex: 1;
    max-width: 32px;
    height: 1px;
    background: rgba(148, 163, 184, 0.3);
  }
}

.feature-list {
  display: flex;
  flex-direction: column;
  gap: 24px;
  margin-bottom: 48px;
}

.feature-item {
  display: flex;
  align-items: flex-start;
  gap: 16px;
}

.feature-icon {
  width: 40px;
  height: 40px;
  border-radius: 10px;
  background: rgba(59, 130, 246, 0.15);
  border: 1px solid rgba(59, 130, 246, 0.3);
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
  color: #60a5fa;

  svg {
    width: 18px;
    height: 18px;
  }
}

.feature-title {
  font-size: 14px;
  font-weight: 600;
  color: #e2e8f0;
  margin-bottom: 3px;
}

.feature-desc {
  font-size: 12px;
  color: rgba(148, 163, 184, 0.7);
  line-height: 1.5;
}

.brand-stats {
  display: flex;
  align-items: center;
  gap: 32px;
  padding: 24px;
  background: rgba(255, 255, 255, 0.04);
  border: 1px solid rgba(255, 255, 255, 0.08);
  border-radius: 16px;
  backdrop-filter: blur(10px);
}

.stat-item {
  text-align: center;
  flex: 1;
}

.stat-num {
  font-size: 28px;
  font-weight: 700;
  color: #60a5fa;
  line-height: 1;
  margin-bottom: 6px;

  span {
    font-size: 14px;
    color: rgba(148, 163, 184, 0.6);
  }
}

.stat-label {
  font-size: 11px;
  color: rgba(148, 163, 184, 0.6);
  letter-spacing: 1px;
}

.stat-divider {
  width: 1px;
  height: 40px;
  background: rgba(255, 255, 255, 0.1);
}

/* 右侧登录区 */
.form-panel {
  width: 480px;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 60px 64px;
  position: relative;
  z-index: 1;
  background: rgba(255, 255, 255, 0.02);
}

.login-card {
  width: 100%;
  background: rgba(255, 255, 255, 0.05);
  border: 1px solid rgba(255, 255, 255, 0.1);
  border-radius: 20px;
  padding: 40px 36px;
  backdrop-filter: blur(20px);
  box-shadow: 0 25px 50px rgba(0, 0, 0, 0.4);
}

.card-header {
  margin-bottom: 32px;

  .card-title {
    font-size: 24px;
    font-weight: 700;
    color: #f1f5f9;
    margin: 0 0 8px;
  }

  .card-subtitle {
    font-size: 13px;
    color: rgba(148, 163, 184, 0.7);
    margin: 0;
  }
}

.custom-input {
  :deep(.el-input__wrapper) {
    background: rgba(255, 255, 255, 0.07);
    border: 1px solid rgba(255, 255, 255, 0.15);
    border-radius: 10px;
    box-shadow: none;
    transition: border-color 0.2s, background 0.2s;

    &:hover {
      border-color: rgba(96, 165, 250, 0.5);
      background: rgba(255, 255, 255, 0.1);
    }

    &.is-focus {
      border-color: #60a5fa;
      background: rgba(96, 165, 250, 0.08);
      box-shadow: 0 0 0 3px rgba(96, 165, 250, 0.15);
    }
  }

  :deep(.el-input__inner) {
    color: #f1f5f9;
    font-size: 14px;

    &::placeholder {
      color: rgba(148, 163, 184, 0.5);
    }
  }

  :deep(.el-input__prefix-inner) {
    color: rgba(148, 163, 184, 0.6);
  }
}

:deep(.el-form-item) {
  margin-bottom: 18px;
}

:deep(.el-form-item__error) {
  color: #f87171;
}

.form-options {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 8px;

  :deep(.el-checkbox__label) {
    color: rgba(148, 163, 184, 0.7);
    font-size: 13px;
  }

  :deep(.el-checkbox__inner) {
    background: rgba(255, 255, 255, 0.07);
    border-color: rgba(255, 255, 255, 0.2);
  }
}

.login-btn {
  width: 100%;
  height: 48px;
  font-size: 15px;
  letter-spacing: 2px;
  border-radius: 10px;
  background: linear-gradient(135deg, #2563eb 0%, #1d4ed8 100%);
  border: none;
  box-shadow: 0 4px 20px rgba(37, 99, 235, 0.4);
  transition: all 0.3s;

  &:hover {
    transform: translateY(-1px);
    box-shadow: 0 8px 25px rgba(37, 99, 235, 0.5);
    background: linear-gradient(135deg, #3b82f6 0%, #2563eb 100%);
  }

  &:active {
    transform: translateY(0);
  }
}

.login-footer {
  margin-top: 20px;
  text-align: center;
  padding-top: 18px;
  border-top: 1px solid rgba(255, 255, 255, 0.07);
}

.hint-text {
  font-size: 12px;
  color: rgba(148, 163, 184, 0.45);
}

.copyright {
  margin-top: 32px;
  font-size: 11px;
  color: rgba(148, 163, 184, 0.3);
  text-align: center;
  letter-spacing: 0.5px;
}

/* 响应式 */
@media (max-width: 900px) {
  .brand-panel {
    display: none;
  }
  .form-panel {
    width: 100%;
    padding: 40px 32px;
  }
}
</style>
