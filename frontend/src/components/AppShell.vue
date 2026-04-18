<script setup>
import { computed, onBeforeUnmount, onMounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import axios from 'axios'
import logoUrl from '../assets/logo2.png'

const router = useRouter()
const route = useRoute()

const active = computed(() => String(route.name ?? 'home'))

const healthText = ref('后端：未检测')
const healthOk = ref(false)
const healthTagType = computed(() => (healthOk.value ? 'success' : 'warning'))

const sidebarCollapsed = ref(false)
const SIDEBAR_STORAGE_KEY = 'lynxtrip.sidebar.collapsed'

const isFullBleed = computed(() => active.value === 'home' || active.value === 'create-trip')
const isMulticityOpen = ref(false)
const multicityForm = ref({
  title: '',
  description: '',
  city: '',
  days: '4',
  styleMix: {
    red: 34,
    green: 33,
    ancient: 33,
  },
  interests: [],
})
const interestOptions = [
  '历史文化',
  '城市漫游',
  '户外探险',
  '亲子研学',
  '运动健康',
  '红色教育',
  '自然风光',
  '美食探店',
  '购物消费',
  '艺术美学',
  '乡村田园',
]
const ratioPreview = computed(() => {
  const red = Number(multicityForm.value.styleMix.red) || 0
  const green = Number(multicityForm.value.styleMix.green) || 0
  const ancient = Number(multicityForm.value.styleMix.ancient) || 0
  const total = red + green + ancient
  if (total <= 0) return { red: 34, green: 33, ancient: 33, total: 0 }
  return {
    red: (red / total) * 100,
    green: (green / total) * 100,
    ancient: (ancient / total) * 100,
    total,
  }
})

async function checkHealth() {
  try {
    const { data } = await axios.get('/api/health')
    healthOk.value = true
    healthText.value = `后端：已连通（${data?.service ?? 'service'}）`
  } catch {
    healthOk.value = false
    healthText.value = '后端：未连通'
  }
}

function go(name) {
  router.push({ name })
}

function toggleSidebar() {
  sidebarCollapsed.value = !sidebarCollapsed.value
  try {
    localStorage.setItem(SIDEBAR_STORAGE_KEY, String(sidebarCollapsed.value))
  } catch {
    // ignore storage failures (private mode, etc.)
  }
}

function openMulticityModal() {
  isMulticityOpen.value = true
}

function closeMulticityModal() {
  isMulticityOpen.value = false
}

function submitMulticity() {
  closeMulticityModal()
  go('create-trip')
}

function rangeStyle(value) {
  return { '--ratio': `${Number(value) || 0}%` }
}

function handleEscClose(event) {
  if (event.key === 'Escape' && isMulticityOpen.value) {
    closeMulticityModal()
  }
}

onMounted(checkHealth)

onMounted(() => {
  try {
    const stored = localStorage.getItem(SIDEBAR_STORAGE_KEY)
    if (stored === 'true' || stored === 'false') sidebarCollapsed.value = stored === 'true'
  } catch {
    // ignore
  }
})

onMounted(() => {
  window.addEventListener('keydown', handleEscClose)
})

onBeforeUnmount(() => {
  window.removeEventListener('keydown', handleEscClose)
})
</script>

<template>
  <div class="uipro-shell">
    <div class="uipro-main">
      <aside class="uipro-sidebar" :class="{ 'is-collapsed': sidebarCollapsed }">
        <div class="uipro-sidebar__brand">
          <button class="uipro-logo" type="button" @click="go('home')" aria-label="返回首页">
            <img class="uipro-logo__img" :src="logoUrl" alt="灵犀旅行 logo" />
            <span class="uipro-logo__text">灵犀旅行</span>
          </button>
        </div>

        <div class="uipro-create">
          <el-button class="uipro-create__btn" type="primary" @click="go('create-trip')">
            <span class="uipro-create__plus" aria-hidden="true">+</span>
            <span class="uipro-item__label">创建行程</span>
          </el-button>
        </div>

        <div class="uipro-sidebar__scroll" aria-label="侧栏菜单">
          <div class="uipro-group">
            <div class="uipro-group__label">规划您的旅行</div>
            <nav class="uipro-nav" aria-label="规划您的旅行">
              <button class="uipro-item" :class="{ 'is-active': active === 'routes' }" :title="sidebarCollapsed ? '旅游路线' : undefined" @click="go('routes')">
                <span class="uipro-item__icon" aria-hidden="true">
                  <svg class="uipro-ico" viewBox="0 0 24 24">
                    <path
                      fill="currentColor"
                      d="M7 2a1 1 0 0 1 1 1v18.17l2.7-2.7a1 1 0 0 1 1.41 0l2.89 2.89V3a1 1 0 1 1 2 0v20a1 1 0 0 1-1.71.71L12 19.41l-5.29 5.3A1 1 0 0 1 5 24V3a1 1 0 0 1 1-1h1Z"
                    />
                  </svg>
                </span>
                <span class="uipro-item__label">旅游路线</span>
              </button>
              <button class="uipro-item" :class="{ 'is-active': active === 'news' }" :title="sidebarCollapsed ? '旅游资讯' : undefined" @click="go('news')">
                <span class="uipro-item__icon" aria-hidden="true">
                  <svg class="uipro-ico" viewBox="0 0 24 24">
                    <path
                      fill="currentColor"
                      d="M5 4a2 2 0 0 1 2-2h8a2 2 0 0 1 2 2v1h2a2 2 0 0 1 2 2v13a2 2 0 0 1-2 2H7a2 2 0 0 1-2-2V4Zm12 3v13h2V7h-2ZM7 4v16h8V4H7Zm2 3h4a1 1 0 1 1 0 2H9a1 1 0 1 1 0-2Zm0 4h4a1 1 0 1 1 0 2H9a1 1 0 1 1 0-2Z"
                    />
                  </svg>
                </span>
                <span class="uipro-item__label">旅游资讯</span>
              </button>
              <button class="uipro-item" :class="{ 'is-active': active === 'gallery' }" :title="sidebarCollapsed ? '光影拾记' : undefined" @click="go('gallery')">
                <span class="uipro-item__icon" aria-hidden="true">
                  <svg class="uipro-ico" viewBox="0 0 24 24">
                    <path
                      fill="currentColor"
                      d="M4 5a3 3 0 0 1 3-3h10a3 3 0 0 1 3 3v14a3 3 0 0 1-3 3H7a3 3 0 0 1-3-3V5Zm3-1a1 1 0 0 0-1 1v11.5l2.8-2.8a1 1 0 0 1 1.4 0l2.3 2.3l3.8-3.8a1 1 0 0 1 1.4 0L20 14.5V5a1 1 0 0 0-1-1H7Zm13 13.33l-3.5-3.5l-3.8 3.8a1 1 0 0 1-1.4 0l-2.3-2.3L6 18.6V19a1 1 0 0 0 1 1h12a1 1 0 0 0 1-1v-1.67ZM9 7.5A1.5 1.5 0 1 1 12 7.5A1.5 1.5 0 0 1 9 7.5Z"
                    />
                  </svg>
                </span>
                <span class="uipro-item__label">光影拾记</span>
              </button>
              <button class="uipro-item" :class="{ 'is-active': active === 'cart' }" :title="sidebarCollapsed ? '购物车' : undefined" @click="go('cart')">
                <span class="uipro-item__icon" aria-hidden="true">
                  <svg class="uipro-ico" viewBox="0 0 24 24">
                    <path
                      fill="currentColor"
                      d="M6 6a1 1 0 0 1 1-1h14a1 1 0 0 1 .98 1.2l-1.5 7A1 1 0 0 1 19.5 14H8a1 1 0 0 1 0-2h10.7l1.07-5H7v1a1 1 0 0 1-2 0V6Zm2.5 16a1.5 1.5 0 1 1 0-3a1.5 1.5 0 0 1 0 3Zm10 0a1.5 1.5 0 1 1 0-3a1.5 1.5 0 0 1 0 3ZM3 4a1 1 0 0 1 1-1h2a1 1 0 0 1 .96.73L8.9 11.1a1 1 0 0 1-1.92.55L5.24 5H4a1 1 0 0 1-1-1Z"
                    />
                  </svg>
                </span>
                <span class="uipro-item__label">购物车</span>
              </button>
            </nav>
          </div>

          <div class="uipro-group">
            <div class="uipro-group__label">AI助手</div>
            <nav class="uipro-nav" aria-label="AI助手">
              <button class="uipro-item" :class="{ 'is-active': active === 'ai-trip' }" :title="sidebarCollapsed ? 'AI规划行程' : undefined" @click="go('ai-trip')">
                <span class="uipro-item__icon" aria-hidden="true">
                  <svg class="uipro-ico" viewBox="0 0 24 24">
                    <path
                      fill="currentColor"
                      d="M12 2a1 1 0 0 1 1 1v1.06A7 7 0 0 1 19.94 11H21a1 1 0 1 1 0 2h-1.06A7 7 0 0 1 13 19.94V21a1 1 0 1 1-2 0v-1.06A7 7 0 0 1 4.06 13H3a1 1 0 1 1 0-2h1.06A7 7 0 0 1 11 4.06V3a1 1 0 0 1 1-1Zm0 4a5 5 0 1 0 0 10a5 5 0 0 0 0-10Zm0 2a1 1 0 0 1 1 1v2h2a1 1 0 1 1 0 2h-3a1 1 0 0 1-1-1V9a1 1 0 0 1 1-1Z"
                    />
                  </svg>
                </span>
                <span class="uipro-item__label">AI规划行程</span>
              </button>
              <button class="uipro-item" :class="{ 'is-active': active === 'ai-qa' }" :title="sidebarCollapsed ? 'AI问答助手' : undefined" @click="go('ai-qa')">
                <span class="uipro-item__icon" aria-hidden="true">
                  <svg class="uipro-ico" viewBox="0 0 24 24">
                    <path
                      fill="currentColor"
                      d="M4 4a3 3 0 0 1 3-3h10a3 3 0 0 1 3 3v8a3 3 0 0 1-3 3H10l-4.3 3.44A1 1 0 0 1 4 17.66V4Zm3-1a1 1 0 0 0-1 1v11.58L9.3 13a1 1 0 0 1 .7-.28h7a1 1 0 0 0 1-1V4a1 1 0 0 0-1-1H7Zm3 3h4a1 1 0 1 1 0 2h-4a1 1 0 1 1 0-2Zm0 4h6a1 1 0 1 1 0 2h-6a1 1 0 1 1 0-2Z"
                    />
                  </svg>
                </span>
                <span class="uipro-item__label">AI问答助手</span>
              </button>
            </nav>
          </div>

          <div class="uipro-group">
            <div class="uipro-group__label">其他</div>
            <nav class="uipro-nav" aria-label="其他">
              <button class="uipro-item" :class="{ 'is-active': active === 'product' }" :title="sidebarCollapsed ? '产品介绍' : undefined" @click="go('product')">
                <span class="uipro-item__icon" aria-hidden="true">
                  <svg class="uipro-ico" viewBox="0 0 24 24">
                    <path
                      fill="currentColor"
                      d="M12 2a10 10 0 1 1 0 20a10 10 0 0 1 0-20Zm0 2a8 8 0 1 0 0 16a8 8 0 0 0 0-16Zm0 6a1 1 0 0 1 1 1v5a1 1 0 1 1-2 0v-5a1 1 0 0 1 1-1Zm0-3a1.25 1.25 0 1 1 0 2.5A1.25 1.25 0 0 1 12 7Z"
                    />
                  </svg>
                </span>
                <span class="uipro-item__label">产品介绍</span>
              </button>
              <button class="uipro-item" :class="{ 'is-active': active === 'support' }" :title="sidebarCollapsed ? '联系客服' : undefined" @click="go('support')">
                <span class="uipro-item__icon" aria-hidden="true">
                  <svg class="uipro-ico" viewBox="0 0 24 24">
                    <path
                      fill="currentColor"
                      d="M12 2a8 8 0 0 1 8 8v1a3 3 0 0 1-3 3h-1a2 2 0 0 1-2-2V9a2 2 0 0 1 2-2h1.08A6 6 0 0 0 6 10v1h1a2 2 0 0 1 2 2v3a2 2 0 0 1-2 2H6a3 3 0 0 1-3-3v-1a8 8 0 0 1 8-8Zm4 7v3h1a1 1 0 0 0 1-1v-1a6.03 6.03 0 0 0-.17-1H16ZM6 12H4.17c-.11.32-.17.66-.17 1v1a1 1 0 0 0 1 1h1V12Zm5.5 8.5a1 1 0 0 1 1-1H15a1 1 0 1 1 0 2h-2.5a1 1 0 0 1-1-1Z"
                    />
                  </svg>
                </span>
                <span class="uipro-item__label">联系客服</span>
              </button>
            </nav>
          </div>
        </div>

        <div class="uipro-bottom">
          <button class="uipro-item uipro-item--bottom" :class="{ 'is-active': active === 'me' }" :title="sidebarCollapsed ? '个人中心' : undefined" @click="go('me')">
            <span class="uipro-item__icon" aria-hidden="true">
              <svg class="uipro-ico" viewBox="0 0 24 24">
                <path
                  fill="currentColor"
                  d="M12 12a4.5 4.5 0 1 1 0-9a4.5 4.5 0 0 1 0 9Zm0-2a2.5 2.5 0 1 0 0-5a2.5 2.5 0 0 0 0 5Zm-8 12a8 8 0 0 1 16 0a1 1 0 1 1-2 0a6 6 0 0 0-12 0a1 1 0 1 1-2 0Z"
                />
              </svg>
            </span>
            <span class="uipro-item__label">个人中心</span>
          </button>
        </div>
      </aside>

      <section class="uipro-right">
        <header class="uipro-topbar">
          <div class="uipro-topbar__left">
            <button class="uipro-iconbtn" type="button" :aria-pressed="sidebarCollapsed" aria-label="切换侧边栏收放" @click="toggleSidebar">
              <svg class="uipro-ico" viewBox="0 0 24 24" aria-hidden="true">
                <path
                  fill="currentColor"
                  d="M4 6.75A1.75 1.75 0 0 1 5.75 5h12.5A1.75 1.75 0 0 1 20 6.75v10.5A1.75 1.75 0 0 1 18.25 19H5.75A1.75 1.75 0 0 1 4 17.25V6.75Zm2 0a.25.25 0 0 0-.25.25v10.5c0 .138.112.25.25.25H9V6H6Zm5 0v12h7.25a.25.25 0 0 0 .25-.25V7a.25.25 0 0 0-.25-.25H11Z"
                />
              </svg>
            </button>
            <div class="uipro-search">
              <el-input placeholder="搜索城市" clearable>
                <template #prefix>
                  <svg class="uipro-search__icon" viewBox="0 0 24 24" aria-hidden="true">
                    <path
                      fill="currentColor"
                      d="M10 4a6 6 0 1 1 3.75 10.69l4.28 4.28a1 1 0 0 1-1.42 1.42l-4.28-4.28A6 6 0 0 1 10 4Zm0 2a4 4 0 1 0 0 8a4 4 0 0 0 0-8Z"
                    />
                  </svg>
                </template>
              </el-input>
            </div>
          </div>

          <div class="uipro-topbar__right">
            <el-tag :type="healthTagType" effect="light" round>{{ healthText }}</el-tag>
            <el-button size="small" type="primary" @click="checkHealth">刷新</el-button>
            <el-button class="uipro-multicity" size="small" @click="openMulticityModal">
              <span>Multi - City</span>
              <span class="uipro-multicity__caret" aria-hidden="true">▼</span>
            </el-button>
          </div>
        </header>

        <main class="uipro-content" :class="{ 'is-fullbleed': isFullBleed }">
          <slot />
        </main>
      </section>

      <div v-if="isMulticityOpen" class="multicity-modal-mask" @click="closeMulticityModal" />
      <aside v-if="isMulticityOpen" class="multicity-modal" role="dialog" aria-modal="true" aria-label="创建您的行程">
        <div class="multicity-modal__header">
          <span>创建您的行程</span>
        </div>
        <button class="multicity-modal-close" type="button" aria-label="关闭" @click="closeMulticityModal">
          ×
        </button>

        <div class="multicity-modal-content">
          <form class="multicity-form" @submit.prevent="submitMulticity">
            <div class="multicity-field">
              <label for="project-name">标题</label>
              <input id="project-name" v-model="multicityForm.title" autocomplete="off" placeholder="请输入行程标题" />
            </div>

            <div class="multicity-field">
              <label for="project-desc">行程描述</label>
              <textarea
                id="project-desc"
                v-model="multicityForm.description"
                maxlength="500"
                rows="4"
                placeholder="请描述您理想的旅行，包括任何您想包含的具体活动或体验。您提供的细节越多，您的行程将越个性化！"
              />
            </div>

            <div class="multicity-row">
              <div class="multicity-field">
                <label for="project-city">目的地城市</label>
                <input id="project-city" v-model="multicityForm.city" placeholder="例如：韶山" />
              </div>
              <div class="multicity-field">
                <label for="project-days">旅游天数</label>
                <select id="project-days" v-model="multicityForm.days">
                  <option value="2">2天</option>
                  <option value="3">3天</option>
                  <option value="4">4天</option>
                  <option value="5">5天</option>
                  <option value="7">7天</option>
                </select>
              </div>
            </div>

            <section class="multicity-section">
              <h4>红绿古三色倾向</h4>
              <p class="multicity-section__hint">红色=红色征程/党建相关，绿色=自然生态，古色=古韵建筑/历史人文（预览条会自动归一）。</p>
              <div class="multicity-ratio-card">
                <div class="multicity-ratio-row">
                  <label>红色征程</label>
                  <input v-model.number="multicityForm.styleMix.red" :style="rangeStyle(multicityForm.styleMix.red)" type="range" min="0" max="100" />
                  <span>{{ multicityForm.styleMix.red }}%</span>
                </div>
                <div class="multicity-ratio-row">
                  <label>绿色生态</label>
                  <input v-model.number="multicityForm.styleMix.green" :style="rangeStyle(multicityForm.styleMix.green)" type="range" min="0" max="100" />
                  <span>{{ multicityForm.styleMix.green }}%</span>
                </div>
                <div class="multicity-ratio-row">
                  <label>古韵人文</label>
                  <input v-model.number="multicityForm.styleMix.ancient" :style="rangeStyle(multicityForm.styleMix.ancient)" type="range" min="0" max="100" />
                  <span>{{ multicityForm.styleMix.ancient }}%</span>
                </div>
                <div class="multicity-ratio-preview">
                  <i :style="{ width: `${ratioPreview.red}%` }" class="is-red" />
                  <i :style="{ width: `${ratioPreview.green}%` }" class="is-green" />
                  <i :style="{ width: `${ratioPreview.ancient}%` }" class="is-ancient" />
                </div>
                <p class="multicity-ratio-note">当前输入总和：{{ ratioPreview.total }}（预览条已自动归一）</p>
              </div>
            </section>

            <section class="multicity-section">
              <h4>兴趣（可选）</h4>
              <div class="multicity-interest-grid">
                <label v-for="interest in interestOptions" :key="interest" class="multicity-interest-item">
                  <input v-model="multicityForm.interests" :value="interest" type="checkbox" />
                  <span>{{ interest }}</span>
                </label>
              </div>
            </section>

            <div class="multicity-actions">
              <button type="submit" class="create-button">创建</button>
            </div>
          </form>
        </div>
      </aside>
    </div>
  </div>
</template>

<style scoped lang="scss">
.uipro-shell {
  min-height: 100vh;
  background: var(--lynx-bg);
}

.uipro-topbar {
  position: sticky;
  top: 0;
  z-index: 60;
  height: 56px;
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 0 var(--space-md);
  background: rgba(255, 255, 255, 0.86);
  border-bottom: 1px solid var(--lynx-border);
  backdrop-filter: blur(10px);
}

.uipro-topbar__left {
  display: flex;
  align-items: center;
  gap: var(--space-md);
  min-width: 0;
}

.uipro-topbar__right {
  display: flex;
  align-items: center;
  gap: var(--space-sm);
}

.uipro-multicity {
  --el-button-bg-color: rgba(248, 78, 60, 1) !important;
  --el-button-border-color: rgba(248, 78, 60, 1) !important;
  --el-button-text-color: #ffffff !important;
  --el-button-hover-bg-color: rgba(232, 70, 52, 1) !important;
  --el-button-hover-border-color: rgba(232, 70, 52, 1) !important;
  --el-button-active-bg-color: rgba(248, 78, 60, 1) !important;
  --el-button-active-border-color: rgba(248, 78, 60, 1) !important;
  border-radius: 5px;
  font-size: 16px;
  font-weight: 400;
  padding: 8px 18px;
  cursor: pointer;
  transition: background 0.2s;
  display: inline-flex;
  align-items: center;
  margin-left: 16px;
  height: 40px;
}

.uipro-multicity,
.uipro-multicity:hover,
.uipro-multicity:focus,
.uipro-multicity:active {
  color: #fff !important;
}

.uipro-multicity :deep(span) {
  color: #fff !important;
}

.uipro-multicity__caret {
  margin-left: 6px;
  font-size: 12px;
  line-height: 1;
  font-weight: 400;
}

.uipro-ico {
  width: 20px;
  height: 20px;
  display: block;
}

.uipro-iconbtn {
  width: 36px;
  height: 36px;
  border: 1px solid var(--lynx-border);
  background: rgba(255, 255, 255, 0.8);
  border-radius: 12px;
  display: grid;
  place-items: center;
  cursor: pointer;
  transition: background 180ms ease, border-color 180ms ease;
}

.uipro-iconbtn:hover {
  background: rgba(255, 255, 255, 0.95);
  border-color: rgba(255, 176, 122, 0.8);
}

.uipro-iconbtn:focus-visible {
  outline: 3px solid rgba(255, 136, 57, 0.28);
  outline-offset: 2px;
}

.uipro-logo {
  width: 100%;
  display: inline-flex;
  align-items: center;
  gap: 8px;
  font-weight: 900;
  font-size: 28px;
  cursor: pointer;
  user-select: none;
  border: 0;
  padding: 10px 8px;
  background: transparent;
  color: #fff;
  justify-content: center;
}

.uipro-logo__img {
  width: 58px;
  height: 58px;
  object-fit: contain;
  border-radius: 0;
  background: transparent;
}

.uipro-logo__text {
  letter-spacing: 1.2px;
  text-shadow: 0 1px 2px rgba(0, 0, 0, 0.16);
}

.uipro-search {
  width: min(520px, 46vw);
  min-width: 220px;
}

.uipro-search__icon {
  color: var(--lynx-muted);
  width: 16px;
  height: 16px;
}

.uipro-main {
  display: flex;
  min-height: 100vh;
}

.uipro-right {
  flex: 1;
  min-width: 0;
  min-height: 100vh;
  display: flex;
  flex-direction: column;
}

.uipro-sidebar {
  width: 220px;
  flex: 0 0 220px;
  position: sticky;
  top: 0;
  height: 100vh;
  overflow: hidden;
  padding: 0;
  background: var(--lynx-brand);
  border-right: 1px solid rgba(255, 255, 255, 0.18);
  transition:
    width 180ms ease,
    flex-basis 180ms ease,
    padding 180ms ease,
    border-color 180ms ease,
    opacity 180ms ease;
  color: #fff;
  display: flex;
  flex-direction: column;
}

.uipro-sidebar.is-collapsed {
  width: 0;
  flex-basis: 0;
  padding: 0;
  border-right-color: transparent;
  opacity: 0;
  overflow: hidden;
  pointer-events: none;
}

.uipro-create {
  padding: var(--space-sm) 6px;
  background: var(--lynx-brand);
  display: flex;
  justify-content: center;
}

.uipro-sidebar__brand {
  padding: 18px 10px 12px 10px;
  background: var(--lynx-brand);
  border-bottom: 1px solid rgba(255, 255, 255, 0.22);
}

.uipro-create__btn {
  width: min(196px, 100%);
  margin: 0;
  justify-content: center;
  --el-color-primary: #ffffff;
  --el-button-text-color: var(--lynx-brand-hover);
  --el-button-bg-color: rgba(255, 255, 255, 0.92);
  --el-button-border-color: rgba(255, 255, 255, 0.55);
  border-radius: 14px;
  height: 54px;
  font-size: 22px;
  font-weight: 800;
  transition: filter 180ms ease, background-color 180ms ease, border-color 180ms ease;
}

.uipro-create__plus {
  font-size: 20px;
  line-height: 1;
  margin-right: 8px;
}

.uipro-create__btn:hover {
  filter: brightness(0.98);
}

.uipro-group {
  padding: 6px 4px;
}

.uipro-group__label {
  font-size: 12px;
  color: rgba(255, 255, 255, 0.72);
  margin: 6px 0 8px 0;
}

.uipro-nav {
  display: flex;
  flex-direction: column;
  gap: 6px;
}

.uipro-item {
  width: calc(100% - 12px);
  margin: 0 6px;
  display: flex;
  align-items: center;
  gap: 10px;
  text-align: left;
  border: 1px solid transparent;
  background: transparent;
  color: #fff;
  border-radius: 14px;
  padding: 10px 10px;
  box-sizing: border-box;
  cursor: pointer;
  font-size: 18px;
  transition: background 180ms ease, border-color 180ms ease, color 180ms ease;
}

.uipro-item:hover {
  background: rgba(255, 255, 255, 0.16);
  border-color: rgba(255, 255, 255, 0.28);
}

.uipro-item.is-active {
  background: rgba(255, 255, 255, 0.96);
  border-color: rgba(255, 255, 255, 0.96);
  color: var(--lynx-brand-hover);
}

.uipro-item:focus-visible {
  outline: 3px solid rgba(255, 136, 57, 0.22);
  outline-offset: 2px;
}

.uipro-item__icon {
  width: 22px;
  height: 22px;
  display: grid;
  place-items: center;
  color: currentColor;
}

.uipro-item__label {
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

/* 折叠态为“完全收起”，无需保留 mini 模式样式 */

.uipro-bottom {
  position: sticky;
  bottom: 0;
  z-index: 2;
  padding: var(--space-md) var(--space-sm) var(--space-md) var(--space-sm);
  background: var(--lynx-brand);
  border-top: 1px solid rgba(255, 255, 255, 0.18);
  backdrop-filter: blur(10px);
}

.uipro-item--bottom {
  font-weight: 800;
}

.uipro-content {
  flex: 1;
  min-width: 0;
  padding: var(--space-lg);
  min-height: calc(100vh - 56px);
}

.uipro-content.is-fullbleed {
  padding: 0;
}

.uipro-sidebar__scroll {
  flex: 1;
  min-height: 0;
  overflow: auto;
  padding: 4px;
}

.multicity-modal-mask {
  position: fixed;
  inset: 0;
  background: rgba(255, 107, 157, 0.08);
  z-index: 1000;
  transition: background 0.2s;
}

.multicity-modal {
  position: fixed;
  top: 0;
  right: 0;
  height: 100vh;
  width: 40vw;
  min-width: 340px;
  max-width: 480px;
  background: #fff;
  box-shadow: -4px 0 32px rgba(255, 107, 157, 0.12);
  z-index: 1001;
  display: flex;
  flex-direction: column;
  padding: 32px 24px 24px 24px;
  border-radius: 16px 0 0 16px;
  animation: slideInRight 0.28s ease;
  color: #7a3a1d;
  font-size: 14px;
}

.multicity-modal__header {
  background: rgba(255, 136, 57, 1);
  height: 56px;
  border-radius: 16px 0 0 0;
  display: flex;
  align-items: center;
  padding-left: 32px;
  margin: -32px -24px 0 -24px;
  color: #fff;
  font-size: 1.25rem;
  font-weight: 700;
}

.multicity-modal-close {
  position: absolute;
  top: 12px;
  right: 12px;
  background: rgba(255, 136, 57, 1);
  color: #fff;
  border: none;
  border-radius: 50%;
  width: 32px;
  height: 32px;
  font-size: 20px;
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
  box-shadow: 0 2px 8px rgba(255, 107, 157, 0.08);
  transition: background 0.2s;
}

.multicity-modal-close:hover {
  background: rgba(248, 78, 60, 1);
}

.multicity-modal-content {
  overflow-y: auto;
  max-height: 80vh;
  padding: 0 18px 28px 18px;
  margin-top: 30px;
}

.multicity-form {
  display: flex;
  flex-direction: column;
  gap: 1.1rem;
}

.multicity-field label {
  color: rgba(255, 136, 57, 1);
  font-size: 14px;
  font-weight: 600;
  display: block;
  margin-bottom: 6px;
}

.multicity-field input,
.multicity-field textarea,
.multicity-field select {
  width: 100%;
  padding: 10px 12px;
  border: 2px solid #ffc0d5;
  border-radius: 8px;
  font-size: 14px;
  background-color: #fff;
  box-sizing: border-box;
  outline: none;
  transition: border-color 0.2s, box-shadow 0.2s;
}

.multicity-field input:focus,
.multicity-field textarea:focus,
.multicity-field select:focus {
  border-color: rgba(255, 136, 57, 1);
  box-shadow: 0 0 0 3px rgba(255, 136, 57, 0.18);
}

.multicity-row {
  display: grid;
  grid-template-columns: 1fr 140px;
  gap: 12px;
}

.multicity-section h4 {
  margin: 4px 0 8px 0;
  color: rgba(255, 136, 57, 1);
  font-size: 16px;
  line-height: 1.2;
  font-weight: 700;
}

.multicity-section__hint {
  margin: 0 0 10px 0;
  color: #b07a5a;
  font-size: 13px;
}

.multicity-ratio-card {
  border: 2px solid #ffc0d5;
  border-radius: 14px;
  padding: 12px;
  background: #fffafc;
}

.multicity-ratio-row {
  display: grid;
  grid-template-columns: 84px 1fr 52px;
  gap: 10px;
  align-items: center;
  margin-bottom: 10px;
}

.multicity-ratio-row label {
  color: #ff6a3d;
  font-size: 14px;
  font-weight: 600;
}

.multicity-ratio-row span {
  color: #ff6a3d;
  text-align: right;
  font-size: 14px;
  font-weight: 700;
}

.multicity-ratio-row input[type='range'] {
  width: 100%;
  height: 14px;
  margin: 0;
  border-radius: 999px;
  background: transparent;
  accent-color: #ff8839;
  -webkit-appearance: none;
  appearance: none;
}

.multicity-ratio-row input[type='range']::-webkit-slider-runnable-track {
  height: 4px;
  border-radius: 999px;
  background: linear-gradient(to right, #ff8839 0%, #ff8839 var(--ratio), #ffd7c1 var(--ratio), #ffd7c1 100%);
}

.multicity-ratio-row input[type='range']::-webkit-slider-thumb {
  -webkit-appearance: none;
  width: 14px;
  height: 14px;
  margin-top: -5px;
  border-radius: 50%;
  border: 2px solid #fff;
  background: #ff8839;
  box-shadow: 0 2px 8px rgba(255, 136, 57, 0.35);
  cursor: pointer;
}

.multicity-ratio-row input[type='range']::-moz-range-track {
  height: 4px;
  border-radius: 999px;
  background: #ffd7c1;
}

.multicity-ratio-row input[type='range']::-moz-range-progress {
  height: 4px;
  border-radius: 999px;
  background: #ff8839;
}

.multicity-ratio-row input[type='range']::-moz-range-thumb {
  width: 14px;
  height: 14px;
  border-radius: 50%;
  border: 2px solid #fff;
  background: #ff8839;
  box-shadow: 0 2px 8px rgba(255, 136, 57, 0.35);
  cursor: pointer;
}

.multicity-ratio-preview {
  margin-top: 4px;
  border-radius: 999px;
  overflow: hidden;
  display: flex;
  height: 14px;
  background: #ffe7ef;
}

.multicity-ratio-preview i {
  display: block;
  height: 100%;
}

.multicity-ratio-preview .is-red {
  background: #f0513e;
}

.multicity-ratio-preview .is-green {
  background: #1fba62;
}

.multicity-ratio-preview .is-ancient {
  background: #915122;
}

.multicity-ratio-note {
  margin: 8px 0 0 0;
  color: #a97456;
  font-size: 12px;
}

.multicity-interest-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 10px 22px;
}

.multicity-interest-item {
  display: inline-flex;
  align-items: center;
  gap: 8px;
  color: #ff7a2a;
  font-size: 16px;
  font-weight: 500;
  cursor: pointer;
}

.multicity-interest-item input[type='checkbox'] {
  width: 18px;
  height: 18px;
  -webkit-appearance: none;
  appearance: none;
  border: 2px solid rgba(255, 136, 57, 0.55);
  border-radius: 4px;
  background: #fff;
  display: grid;
  place-items: center;
  cursor: pointer;
  transition: background 0.18s ease, border-color 0.18s ease;
}

.multicity-interest-item input[type='checkbox']:checked {
  background: #ff8839;
  border-color: #ff8839;
}

.multicity-interest-item input[type='checkbox']:checked::after {
  content: '';
  width: 9px;
  height: 5px;
  border-left: 3px solid #fff;
  border-bottom: 3px solid #fff;
  transform: translateY(-1px) rotate(-45deg);
  pointer-events: none;
}

.multicity-actions {
  text-align: center;
  margin-top: 10px;
}

.create-button {
  background-color: rgba(255, 136, 57, 1);
  color: #fff;
  border: none;
  padding: 12px 25px;
  border-radius: 8px;
  cursor: pointer;
  font-size: 16px;
  font-weight: 700;
  transition: all 0.3s;
  min-width: 140px;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  line-height: normal;
  box-sizing: border-box;
  appearance: none;
}

.create-button:hover {
  background-color: #e84393;
  transform: translateY(-2px);
}

@keyframes slideInRight {
  from {
    right: -40vw;
    opacity: 0;
  }
  to {
    right: 0;
    opacity: 1;
  }
}

@media (max-width: 980px) {
  .uipro-search {
    width: min(360px, 44vw);
  }

  .multicity-modal {
    width: min(92vw, 480px);
    min-width: 0;
  }

  .multicity-row {
    grid-template-columns: 1fr;
  }

  .multicity-interest-grid {
    grid-template-columns: 1fr;
  }
}
</style>

