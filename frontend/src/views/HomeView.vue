<script setup>
import { ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import bgUrl from '../assets/background.png'
import sloganUrl from '../assets/signal.png'
import searchLogoUrl from '../assets/search-logo.png'

const tripDays = ref('1')
const dayOptions = ['1', '2', '3', '4', '5', '7', '10']
const cityKeyword = ref('')
const cityQuery = ref('')
const cityOptions = ref([])
const selectedCity = ref(null)
const suggestLoading = ref(false)
const submitting = ref(false)
const API_BASE = import.meta.env.VITE_API_BASE_URL || 'http://127.0.0.1:8080'
let suggestTimer = null
const router = useRouter()

function remoteSearchCities(queryString) {
  const query = String(queryString || '').trim()
  cityQuery.value = query
  if (!query) {
    cityOptions.value = []
    selectedCity.value = null
    return
  }

  if (suggestTimer) clearTimeout(suggestTimer)
  suggestTimer = setTimeout(async () => {
    suggestLoading.value = true
    try {
      const resp = await fetch(`${API_BASE}/api/maps/suggest?q=${encodeURIComponent(query)}&region=全国`)
      const json = await resp.json()
      const list = Array.isArray(json?.data) ? json.data : []
      cityOptions.value = list.map((it, idx) => ({
        id: `${it?.name || '地点'}-${it?.lng || ''}-${it?.lat || ''}-${idx}`,
        value: it?.name || it?.value || '',
        name: it?.name || '',
        address: it?.address || '',
        district: it?.district || '',
      }))
    } catch {
      cityOptions.value = []
    } finally {
      suggestLoading.value = false
    }
  }, 220)
}

function onCityChange(value) {
  const hit = cityOptions.value.find((it) => it.value === value) || null
  selectedCity.value = hit
}

function submitSearch() {
  if (submitting.value) return
  const text = cityKeyword.value.trim()
  if (!text) {
    ElMessage.warning('请输入城市或地点')
    return
  }
  if (!selectedCity.value) {
    ElMessage.warning('请从下拉建议中选择一个地点')
    return
  }
  submitting.value = true
  router.push({
    name: 'my-itinerary-workspace',
    query: {
      destination: selectedCity.value.name || text,
      departure: cityQuery.value || '',
      days: tripDays.value || '1',
      autogen: '1',
    },
  }).finally(() => {
    // 路由切走后也会销毁组件，这里只是防止极端情况下重复点击
    submitting.value = false
  })
}
</script>

<template>
  <div class="home">
    <section class="home__hero" :style="{ backgroundImage: `url(${bgUrl})` }">
      <div class="home__center">
        <div class="home__stack">
          <img class="home__slogan" :src="sloganUrl" alt="灵犀旅行 Slogan" />
          <div class="home__searchCard" role="search">
            <div class="home__searchRow">
              <el-select
                v-model="cityKeyword"
                class="home__input"
                size="large"
                clearable
                filterable
                remote
                reserve-keyword
                default-first-option
                :remote-method="remoteSearchCities"
                placeholder="无需等待，旅途就在脚下"
                :loading="suggestLoading"
                no-match-text="未找到相关地点"
                no-data-text="输入关键词开始检索"
                popper-class="home-city-popper"
                @change="onCityChange"
              >
                <el-option v-for="item in cityOptions" :key="item.id" :label="item.name || item.value" :value="item.value">
                  <div class="cityOption">
                    <div class="cityOption__name">{{ item.name || item.value }}</div>
                    <div class="cityOption__meta">{{ item.address || item.district || '暂无详细地址' }}</div>
                  </div>
                </el-option>
              </el-select>
              <el-select v-model="tripDays" class="home__days" placeholder="天数" size="large">
                <el-option v-for="day in dayOptions" :key="day" :label="`${day}天`" :value="day" />
              </el-select>
              <button class="home__btn" type="button" aria-label="搜索" :disabled="submitting" @click="submitSearch">
                <img v-if="!submitting" class="home__btnIcon" :src="searchLogoUrl" alt="" aria-hidden="true" />
                <span v-else class="home__btnSpin" aria-hidden="true" />
              </button>
            </div>
          </div>
        </div>
      </div>
    </section>
  </div>
</template>

<style scoped lang="scss">
.home {
  height: calc(100vh - 56px);
}

.home__hero {
  position: relative;
  border-radius: 0;
  overflow: hidden;
  height: 100%;
  background-size: cover;
  background-position: center;
  background-repeat: no-repeat;
}

.home__center {
  position: relative;
  height: 100%;
  display: grid;
  place-items: center;
  padding: var(--space-lg) 0;
}

.home__stack {
  width: 100%;
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 26px;
  transform: translateY(-34px);
}

.home__slogan {
  width: 100%;
  height: auto;
  object-fit: contain;
  margin-bottom: 0;
  filter: drop-shadow(0 8px 18px rgba(0, 0, 0, 0.18));
}

.home__searchCard {
  width: min(395px, 100%);
  margin-top: 8px;
  background: rgba(255, 255, 255, 0.82);
  border: 1px solid rgba(255, 255, 255, 0.7);
  border-radius: 16px;
  padding: 14px;
  backdrop-filter: blur(12px);
}

.home__searchRow {
  display: grid;
  grid-template-columns: 1fr 110px 52px;
  gap: var(--space-sm);
  align-items: center;
}

.home__days {
  width: 110px;
}

:deep(.home__input .el-select__wrapper),
:deep(.home__days .el-select__wrapper) {
  border-radius: 12px;
  border: 1px solid rgba(255, 255, 255, 0.75);
  background: rgba(255, 255, 255, 0.9);
  box-shadow:
    inset 0 1px 0 rgba(255, 255, 255, 0.6),
    0 6px 18px rgba(249, 115, 22, 0.12);
  transition: border-color 180ms ease, box-shadow 180ms ease, background-color 180ms ease;
}

:deep(.home__input .el-select__wrapper.is-focused),
:deep(.home__days .el-select__wrapper.is-focused) {
  border-color: rgba(249, 115, 22, 0.55);
  box-shadow:
    0 0 0 3px rgba(249, 115, 22, 0.16),
    0 10px 24px rgba(249, 115, 22, 0.18);
  background: rgba(255, 255, 255, 0.96);
}

:deep(.home__input .el-select__wrapper.is-hovering),
:deep(.home__days .el-select__wrapper.is-hovering) {
  border-color: rgba(249, 115, 22, 0.42);
  box-shadow:
    inset 0 1px 0 rgba(255, 255, 255, 0.62),
    0 8px 20px rgba(249, 115, 22, 0.16);
}

.home__btn {
  width: 52px;
  height: 40px;
  padding: 0;
  border: 0;
  background: transparent;
  border-radius: 0;
  display: grid;
  place-items: center;
  box-shadow: none;
  min-height: 40px;
  cursor: pointer;
}
.home__btn:disabled {
  opacity: 0.7;
  cursor: default;
}

.home__btn:focus-visible {
  outline: 2px solid rgba(255, 136, 57, 0.28);
  outline-offset: 2px;
}

.home__btnIcon {
  width: 28px;
  height: 28px;
  object-fit: contain;
  display: block;
}
.home__btnSpin {
  width: 22px;
  height: 22px;
  border-radius: 50%;
  border: 3px solid rgba(249, 115, 22, 0.2);
  border-top-color: rgba(249, 115, 22, 0.95);
  animation: spin 0.9s linear infinite;
}
@keyframes spin {
  from { transform: rotate(0deg); }
  to { transform: rotate(360deg); }
}

:deep(.cityOption) {
  line-height: 1.35;
}

:deep(.cityOption__name) {
  color: #0f172a;
  font-weight: 700;
}

:deep(.cityOption__meta) {
  margin-top: 2px;
  color: #64748b;
  font-size: 12px;
}

:global(.home-city-popper) {
  border-radius: 14px !important;
  border: 1px solid rgba(255, 255, 255, 0.68) !important;
  background: rgba(255, 251, 247, 0.95) !important;
  box-shadow: 0 16px 36px rgba(15, 23, 42, 0.14), 0 6px 16px rgba(249, 115, 22, 0.12) !important;
  backdrop-filter: blur(12px);
  overflow: hidden;
}

:global(.home-city-popper .el-select-dropdown__list) {
  padding: 8px;
}

:global(.home-city-popper .el-select-dropdown__item) {
  height: auto;
  line-height: 1.4;
  padding: 8px 10px;
  border-radius: 10px;
  margin-bottom: 4px;
  color: #1f2937;
  transition: background-color 150ms ease, color 150ms ease;
}

:global(.home-city-popper .el-select-dropdown__item.hover),
:global(.home-city-popper .el-select-dropdown__item:hover) {
  background: linear-gradient(135deg, rgba(255, 136, 57, 0.16), rgba(56, 189, 248, 0.12));
  color: #0f172a;
}

:global(.home-city-popper .el-select-dropdown__item.is-selected) {
  background: linear-gradient(135deg, rgba(249, 115, 22, 0.2), rgba(255, 136, 57, 0.16));
  color: #7c2d12;
  font-weight: 800;
}

@media (max-width: 640px) {
  .home__searchRow {
    grid-template-columns: 1fr;
  }
}
</style>

