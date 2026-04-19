<script setup>
import { computed, onMounted, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { isLoggedIn, loginWithPassword, registerUser } from '../services/lynxDb'
import heroTexture from '../assets/background.png'
import logoUrl from '../assets/logo2.png'

const route = useRoute()
const router = useRouter()

const loginForm = ref({ account: '', mima: '' })
const regForm = ref({ account: '', mima: '', mima2: '', xingming: '', xingbie: '男' })

function normalizePhoneDigits(raw) {
  let d = String(raw ?? '').replace(/\D/g, '')
  if (d.startsWith('86') && d.length === 13) d = d.slice(2)
  return d
}

function isValidAccount(raw) {
  const val = String(raw ?? '').trim()
  // 手机号或用户名（2-20位字母数字下划线）
  if (/^1\d{10}$/.test(normalizePhoneDigits(raw))) return true
  if (/^[a-zA-Z][a-zA-Z0-9_]{1,19}$/.test(val)) return true
  return false
}
const showRegister = ref(false)
/** 注册流程：1 账号与安全，2 个人资料 */
const regStep = ref(1)

const panelSubtitle = computed(() =>
  showRegister.value ? '创建账号，保存行程与收藏' : '使用手机号/用户名与密码进入灵犀旅行'
)

watch(showRegister, () => {
  regStep.value = 1
})

function redirectAfterAuth() {
  const r = route.query.redirect
  const path = typeof r === 'string' && r.startsWith('/') ? r : '/me'
  router.replace(path)
}

onMounted(() => {
  if (isLoggedIn()) redirectAfterAuth()
})

function submitLogin() {
  const account = String(loginForm.value.account ?? '').trim()
  if (!account || !loginForm.value.mima) {
    ElMessage.warning('请填写账号与密码')
    return
  }
  const res = loginWithPassword(account, loginForm.value.mima)
  if (!res.ok) {
    ElMessage.error(res.message)
    return
  }
  ElMessage.success('登录成功')
  redirectAfterAuth()
}

function nextRegisterStep() {
  const account = String(regForm.value.account ?? '').trim()
  if (!account || !regForm.value.mima) {
    ElMessage.warning('请填写账号与密码')
    return
  }
  if (!isValidAccount(regForm.value.account)) {
    ElMessage.warning('请输入正确的 11 位手机号或以字母开头的用户名（2-20位字母数字下划线）')
    return
  }
  if (regForm.value.mima !== regForm.value.mima2) {
    ElMessage.warning('两次密码不一致')
    return
  }
  regStep.value = 2
}

function prevRegisterStep() {
  regStep.value = 1
}

function submitRegister() {
  if (regStep.value !== 2) return
  const account = String(regForm.value.account ?? '').trim()
  if (!account || !regForm.value.mima) {
    ElMessage.warning('请填写账号与密码')
    return
  }
  if (!isValidAccount(regForm.value.account)) {
    ElMessage.warning('请输入正确的 11 位手机号或以字母开头的用户名（2-20位字母数字下划线）')
    return
  }
  if (regForm.value.mima !== regForm.value.mima2) {
    ElMessage.warning('两次密码不一致')
    return
  }
  const id = registerUser({
    account: regForm.value.account,
    mima: regForm.value.mima,
    xingming: regForm.value.xingming?.trim() || '',
    xingbie: regForm.value.xingbie || '—',
  })
  if (id == null) {
    ElMessage.error('注册失败，该账号可能已注册')
    return
  }
  ElMessage.success('注册成功，已自动登录')
  redirectAfterAuth()
}
</script>

<template>
  <section class="login page" aria-label="登录或注册">
    <div class="login__shell">
      <aside class="login__hero">
        <div class="login__hero-bg" aria-hidden="true">
          <div
            class="login__hero-photo"
            :style="{ backgroundImage: `url(${heroTexture})` }"
          />
        </div>

        <div class="login__hero-inner">
          <div class="login__brand">
            <img class="login__logo" :src="logoUrl" width="48" height="48" alt="灵犀旅行" />
            <div class="login__brand-text">
              <span class="login__brand-name">灵犀旅行</span>
              <span class="login__brand-tag">灵犀 AI 旅行助手</span>
            </div>
          </div>

          <h2 class="login__hero-title">让旅途，从一声问候开始</h2>
          <p class="login__hero-lead">
            红色传承 · 振兴乡村 · 文化守护——把路线、资讯与光影记忆收进你的行程本。
          </p>

          <ul class="login__features">
            <li class="login__feat">
              <span class="login__feat-icon" aria-hidden="true">
                <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.6">
                  <path stroke-linecap="round" stroke-linejoin="round" d="M9 20l-5.447-2.724A1 1 0 013 16.382V5.618a1 1 0 011.447-.894L9 7m0 13l6-3m-6 3V7m6 10l5.447 2.724A1 1 0 0021 18.382V7.618a1 1 0 00-1.447-.894L15 7m0 13V7" />
                </svg>
              </span>
              <span class="login__feat-copy">
                <strong>精选路线</strong>
                <span>热门与最新线路一网浏览</span>
              </span>
            </li>
            <li class="login__feat">
              <span class="login__feat-icon" aria-hidden="true">
                <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.6">
                  <path stroke-linecap="round" stroke-linejoin="round" d="M19 20H5a2 2 0 01-2-2V6a2 2 0 012-2h10a2 2 0 012 2v1m2 13a2 2 0 01-2-2V7m2 13a2 2 0 002-2V9a2 2 0 00-2-2h-2m-4-3H9M7 16h6M7 12h3m6 8v.01M12 12v.01" />
                </svg>
              </span>
              <span class="login__feat-copy">
                <strong>资讯拾记</strong>
                <span>旅途故事与光影随时记录</span>
              </span>
            </li>
            <li class="login__feat">
              <span class="login__feat-icon" aria-hidden="true">
                <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.6">
                  <path stroke-linecap="round" stroke-linejoin="round" d="M12 15v2m-6 4h12a2 2 0 002-2v-6a2 2 0 00-2-2H6a2 2 0 00-2 2v6a2 2 0 002 2zm10-10V7a4 4 0 00-8 0v4h8z" />
                </svg>
              </span>
              <span class="login__feat-copy">
                <strong>安全登录</strong>
                <span>手机号验证，隐私数据本地护航</span>
              </span>
            </li>
          </ul>

          <p class="login__hero-foot">© 灵犀旅行 LynxTrip</p>
        </div>
      </aside>

      <div class="login__panel">
        <div class="login__card">
          <header class="login__head">
            <h1 class="login__title">{{ showRegister ? '创建账号' : '欢迎回来' }}</h1>
            <p class="login__subtitle">{{ panelSubtitle }}</p>
          </header>

          <el-form v-if="!showRegister" label-position="top" class="login__form" @submit.prevent="submitLogin">
            <el-form-item label="手机号/用户名">
              <el-input v-model="loginForm.account" autocomplete="username" placeholder="请输入手机号或用户名" size="large" class="login__input" />
            </el-form-item>
            <el-form-item label="密码">
              <el-input v-model="loginForm.mima" type="password" show-password autocomplete="current-password" placeholder="请输入密码" size="large" class="login__input" />
            </el-form-item>
            <div class="login__actions">
              <el-button type="primary" native-type="submit" size="large" round class="login__btn">登录</el-button>
              <button type="button" class="login__link" @click="showRegister = true">
                没有账号？<span class="login__link-strong">立即注册</span>
              </button>
            </div>
          </el-form>

          <el-form
            v-else
            label-position="top"
            class="login__form login__form--register"
            @submit.prevent="regStep === 1 ? nextRegisterStep() : submitRegister()"
          >
            <el-steps :active="regStep - 1" align-center finish-status="success" class="login__reg-steps">
              <el-step title="账号与安全" />
              <el-step title="个人资料" />
            </el-steps>

            <div v-show="regStep === 1" class="login__reg-panel" role="region" aria-label="注册第一步：账号与安全">
              <el-form-item label="手机号/用户名">
                <el-input v-model="regForm.account" autocomplete="username" placeholder="请输入手机号或用户名（字母开头）" size="large" class="login__input" />
              </el-form-item>
              <el-form-item label="密码">
                <el-input v-model="regForm.mima" type="password" show-password autocomplete="new-password" placeholder="请输入密码" size="large" class="login__input" />
              </el-form-item>
              <el-form-item label="确认密码">
                <el-input v-model="regForm.mima2" type="password" show-password autocomplete="new-password" placeholder="请再次输入密码" size="large" class="login__input" />
              </el-form-item>
            </div>

            <div v-show="regStep === 2" class="login__reg-panel" role="region" aria-label="注册第二步：个人资料">
              <el-form-item label="姓名（选填）">
                <el-input v-model="regForm.xingming" autocomplete="name" placeholder="选填" size="large" class="login__input" />
              </el-form-item>
              <el-form-item label="性别">
                <el-radio-group v-model="regForm.xingbie">
                  <el-radio label="男">男</el-radio>
                  <el-radio label="女">女</el-radio>
                </el-radio-group>
              </el-form-item>
            </div>

            <div class="login__actions">
              <template v-if="regStep === 1">
                <el-button type="primary" native-type="submit" size="large" round class="login__btn">下一步</el-button>
                <button type="button" class="login__link" @click="showRegister = false">← 返回登录</button>
              </template>
              <template v-else>
                <el-button type="primary" native-type="submit" size="large" round class="login__btn">注册并登录</el-button>
                <button type="button" class="login__link" @click.prevent="prevRegisterStep">← 上一步</button>
                <button type="button" class="login__link" @click="showRegister = false">已有账号？去登录</button>
              </template>
            </div>
          </el-form>
        </div>
      </div>
    </div>
  </section>
</template>

<style scoped lang="scss">
$login-ink: #0f172a;
$login-muted: #475569;

.login.page {
  min-height: calc(100vh - 56px);
  box-sizing: border-box;
}

.login__shell {
  display: grid;
  grid-template-columns: minmax(280px, 1fr) minmax(320px, 1fr);
  min-height: calc(100vh - 56px);
  width: 100%;
}

.login__hero {
  position: relative;
  overflow: hidden;
  color: #f8fafc;
}

.login__hero-bg {
  position: absolute;
  inset: 0;
  pointer-events: none;
}

.login__hero-photo {
  position: absolute;
  inset: 0;
  background-size: cover;
  background-position: center;
  background-repeat: no-repeat;

  &::after {
    content: '';
    position: absolute;
    inset: 0;
    pointer-events: none;
    background: rgba(0, 0, 0, 0.6);
  }
}

.login__hero-inner {
  position: relative;
  z-index: 1;
  display: flex;
  flex-direction: column;
  justify-content: center;
  min-height: 100%;
  padding: clamp(28px, 5vw, 56px) clamp(24px, 4vw, 48px);
  max-width: 520px;
}

.login__brand {
  display: flex;
  align-items: center;
  gap: 14px;
  margin-bottom: 28px;
}

.login__logo {
  border-radius: 14px;
  box-shadow: 0 8px 24px rgba(0, 0, 0, 0.25);
}

.login__brand-text {
  display: flex;
  flex-direction: column;
  gap: 2px;
}

.login__brand-name {
  font-size: 1.35rem;
  font-weight: 800;
  letter-spacing: 0.02em;
  text-shadow: 0 2px 12px rgba(0, 0, 0, 0.45);
}

.login__brand-tag {
  font-size: 0.8125rem;
  color: rgba(248, 250, 252, 0.92);
  font-weight: 500;
  text-shadow: 0 1px 8px rgba(0, 0, 0, 0.4);
}

.login__hero-title {
  margin: 0 0 12px;
  font-size: clamp(1.5rem, 2.4vw, 1.85rem);
  font-weight: 800;
  line-height: 1.35;
  letter-spacing: -0.02em;
  text-shadow: 0 2px 18px rgba(0, 0, 0, 0.5);
}

.login__hero-lead {
  margin: 0 0 32px;
  font-size: 0.9375rem;
  line-height: 1.75;
  color: rgba(248, 250, 252, 0.95);
  max-width: 38ch;
  text-shadow: 0 1px 10px rgba(0, 0, 0, 0.45);
}

.login__features {
  list-style: none;
  margin: 0;
  padding: 0;
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.login__feat {
  display: flex;
  align-items: flex-start;
  gap: 14px;
}

.login__feat-icon {
  flex-shrink: 0;
  width: 40px;
  height: 40px;
  border-radius: 12px;
  background: rgba(255, 255, 255, 0.1);
  border: 1px solid rgba(255, 255, 255, 0.14);
  display: grid;
  place-items: center;
  color: rgba(255, 213, 180, 0.95);

  svg {
    width: 22px;
    height: 22px;
  }
}

.login__feat-copy {
  display: flex;
  flex-direction: column;
  gap: 2px;
  font-size: 0.8125rem;
  color: rgba(248, 250, 252, 0.92);
  line-height: 1.45;
  text-shadow: 0 1px 8px rgba(0, 0, 0, 0.4);

  strong {
    font-size: 0.9375rem;
    font-weight: 700;
    color: #fff;
    text-shadow: 0 2px 10px rgba(0, 0, 0, 0.45);
  }
}

.login__hero-foot {
  margin: 40px 0 0;
  font-size: 0.75rem;
  color: rgba(248, 250, 252, 0.72);
  text-shadow: 0 1px 6px rgba(0, 0, 0, 0.4);
}

.login__panel {
  display: flex;
  align-items: center;
  justify-content: center;
  padding: clamp(20px, 4vw, 40px);
  background: linear-gradient(180deg, var(--lynx-bg) 0%, var(--lynx-warm-white) 100%);
}

.login__card {
  width: 100%;
  max-width: 440px;
  padding: clamp(28px, 4vw, 40px) clamp(22px, 3vw, 36px);
  border-radius: 22px;
  border: 1px solid var(--lynx-border);
  background: rgba(255, 255, 255, 0.94);
  box-shadow:
    var(--shadow-lg),
    0 0 0 1px rgba(255, 255, 255, 0.8) inset;
  backdrop-filter: blur(14px);
}

.login__head {
  margin-bottom: 8px;
}

.login__title {
  margin: 0;
  font-size: 1.625rem;
  font-weight: 800;
  color: $login-ink;
  letter-spacing: -0.03em;
}

.login__subtitle {
  margin: 10px 0 0;
  font-size: 0.9375rem;
  color: $login-muted;
  line-height: 1.55;
}

.login__form {
  margin-top: 22px;

  :deep(.el-form-item__label) {
    font-weight: 600;
    color: #334155;
  }

  :deep(.el-input__wrapper) {
    border-radius: 12px;
    box-shadow: 0 0 0 1px rgba(255, 227, 207, 0.9) inset;
    transition: box-shadow 0.2s ease;

    &:hover {
      box-shadow: 0 0 0 1px rgba(255, 136, 57, 0.35) inset;
    }

    &.is-focus {
      box-shadow: 0 0 0 1px var(--lynx-brand) inset;
    }
  }
}

.login__form--register {
  margin-top: 18px;
}

.login__reg-steps {
  margin-bottom: 10px;

  :deep(.el-step__title) {
    font-size: 12px;
    font-weight: 600;
  }

  :deep(.el-step__icon.is-text) {
    border-radius: 10px;
  }
}

.login__reg-panel {
  margin-top: 10px;
}

.login__actions {
  display: flex;
  flex-direction: column;
  gap: 12px;
  margin-top: 12px;
}

.login__btn {
  width: 100%;
  font-weight: 700;
  letter-spacing: 0.02em;

  &:deep(span) {
    font-weight: 700;
  }

  &.el-button--primary {
    background: linear-gradient(135deg, var(--lynx-brand) 0%, var(--lynx-brand-hover) 100%);
    border: none;
    box-shadow: 0 4px 14px rgba(255, 136, 57, 0.35);

    &:hover {
      filter: brightness(1.05);
    }
  }
}

.login__link {
  cursor: pointer;
  border: none;
  background: none;
  padding: 8px 4px;
  font-size: 0.875rem;
  color: $login-muted;
  transition: color 0.2s ease;

  &:hover {
    color: var(--lynx-brand);
  }
}

.login__link-strong {
  font-weight: 700;
  color: var(--lynx-brand);
}

@media (max-width: 900px) {
  .login__shell {
    grid-template-columns: 1fr;
    min-height: auto;
  }

  .login__hero-inner {
    min-height: unset;
    padding-top: 36px;
    padding-bottom: 28px;
  }

  .login__hero-title {
    font-size: 1.35rem;
  }

  .login__hero-foot {
    margin-top: 28px;
  }

  .login__panel {
    padding-bottom: max(28px, env(safe-area-inset-bottom));
  }
}
</style>
