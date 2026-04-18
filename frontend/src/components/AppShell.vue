<script setup>
import { computed, onMounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import axios from 'axios'

const router = useRouter()
const route = useRoute()

const active = computed(() => String(route.name ?? 'home'))

const healthText = ref('后端：未检测')
const healthOk = ref(false)
const healthTagType = computed(() => (healthOk.value ? 'success' : 'warning'))

const sidebarCollapsed = ref(false)
const SIDEBAR_STORAGE_KEY = 'lynxtrip.sidebar.collapsed'

const isFullBleed = computed(() => active.value === 'home' || active.value === 'create-trip')

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

onMounted(checkHealth)

onMounted(() => {
  try {
    const stored = localStorage.getItem(SIDEBAR_STORAGE_KEY)
    if (stored === 'true' || stored === 'false') sidebarCollapsed.value = stored === 'true'
  } catch {
    // ignore
  }
})
</script>

<template>
  <div class="uipro-shell">
    <div class="uipro-main">
      <aside class="uipro-sidebar" :class="{ 'is-collapsed': sidebarCollapsed }">
        <div class="uipro-sidebar__brand">
          <button class="uipro-logo" type="button" @click="go('home')" aria-label="返回首页">
            <span class="uipro-logo__mark" aria-hidden="true" />
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
            <el-button size="small" type="primary" plain>Multi - City</el-button>
          </div>
        </header>

        <main class="uipro-content" :class="{ 'is-fullbleed': isFullBleed }">
          <slot />
        </main>
      </section>
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
  display: inline-flex;
  align-items: center;
  gap: 10px;
  font-weight: 900;
  cursor: pointer;
  user-select: none;
  border: 0;
  padding: 0;
  background: transparent;
  color: inherit;
}

.uipro-logo__mark {
  width: 28px;
  height: 28px;
  border-radius: 8px;
  background: linear-gradient(180deg, var(--lynx-brand), var(--lynx-brand-hover));
  box-shadow: 0 0 0 4px rgba(255, 136, 57, 0.16);
}

.uipro-logo__text {
  letter-spacing: 0.2px;
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
  padding: var(--space-sm) 8px 6px 8px;
  background: var(--lynx-brand);
  border-bottom: 1px solid rgba(255, 255, 255, 0.16);
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
  height: 48px;
  font-size: 20px;
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

@media (max-width: 980px) {
  .uipro-search {
    width: min(360px, 44vw);
  }
}
</style>

