<script setup>
import { computed, onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { getCurrentUserId, getUserById, listStoreup, listTrips } from '../services/lynxDb'
import heroBg from '../assets/background.png'

const router = useRouter()

const trips = ref([])
const storeups = ref([])
const activeMenu = ref('itinerary')

function refresh() {
  trips.value = listTrips()
  storeups.value = listStoreup()
}

onMounted(refresh)

function goNew() {
  router.push({ name: 'my-itinerary-workspace' })
}

function openTrip(t) {
  router.push({ name: 'my-itinerary-workspace', query: { tripId: String(t.id) } })
}

const me = computed(() => getUserById(getCurrentUserId()))
const displayName = computed(() => me.value?.xingming || me.value?.yonghuming || '用户')
const handleText = computed(() => (me.value?.yonghuming ? `@${me.value.yonghuming}` : ''))
const avatarUrl = computed(() => String(me.value?.touxiang || '').trim())
const avatarFallback = computed(() => String(displayName.value || '用户').slice(0, 1).toUpperCase())

function coverOf(t) {
  const p = t?.payload
  const rec = Array.isArray(p?.recommended) ? p.recommended : []
  const first = rec.find((x) => x && x.cover) || rec[0]
  return first?.cover || ''
}

function metaLine(t) {
  const p = t?.payload
  const title = String(p?.title || t?.title || '').trim()
  if (title) return title
  const from = String(p?.departure || '').trim()
  const to = String(p?.destination || '').trim()
  if (from || to) return `${from || '出发地'} → ${to || '目的地'}`
  return '未命名行程'
}

function daysLine(t) {
  const p = t?.payload
  const days = Number(p?.days || 0)
  if (Number.isFinite(days) && days > 0) return `${days} 天`
  return '—'
}

function createdDateLine(t) {
  const raw = String(t?.addtime || '').trim()
  if (!raw) return '—'
  const d = new Date(raw)
  if (Number.isNaN(d.getTime())) {
    return raw.slice(0, 10) || '—'
  }
  const y = d.getFullYear()
  const m = String(d.getMonth() + 1).padStart(2, '0')
  const day = String(d.getDate()).padStart(2, '0')
  return `${y}-${m}-${day}`
}

function parsePlacesFromTitle(title) {
  const text = String(title || '').trim()
  if (!text) return []
  const main = text.split('·')[0] || text
  return main
    .split('→')
    .map((x) => x.trim())
    .filter(Boolean)
}

function placeLine(t) {
  const p = t?.payload || {}
  const from = String(p.departure || '').trim()
  const to = String(p.destination || '').trim()
  const places = []
  if (from) places.push(from)
  if (to && to !== from) places.push(to)
  if (!places.length) {
    places.push(...parsePlacesFromTitle(p.title || t?.title || ''))
  }
  const unique = [...new Set(places)].slice(0, 3)
  if (!unique.length) return '中国 🇨🇳'
  return unique.map((x) => `${x} 🇨🇳`).join(' · ')
}

function preferenceTags(t) {
  const p = t?.payload || {}
  const tags = []
  const pref = String(p.preference || '').trim()
  if (pref) {
    pref
      .replace(/^多城市路线[:：]/, '')
      .split(/[，,、/|；;\s]+/)
      .map((x) => x.trim())
      .filter(Boolean)
      .forEach((x) => tags.push(x))
  }
  if (p.travelType) tags.push(String(p.travelType))
  if (p.season) tags.push(`${p.season}季`)
  const uniq = [...new Set(tags.filter(Boolean))]
  return uniq.slice(0, 4)
}

const ordered = computed(() => [...(trips.value || [])].sort((a, b) => Number(b.id) - Number(a.id)))
const orderedStoreups = computed(() => [...(storeups.value || [])].sort((a, b) => Number(b.id) - Number(a.id)))

const tripCount = computed(() => ordered.value.length)
const multiCityCount = computed(
  () =>
    ordered.value.filter((t) => {
      const pref = String(t?.payload?.preference || '')
      return pref.includes('多城市路线')
    }).length,
)
const points = computed(() => tripCount.value * 20 + multiCityCount.value * 30)

const badges = computed(() => {
  const n = tripCount.value
  const mc = multiCityCount.value
  return [
    { id: 'first-trip', name: '初次点亮', desc: '完成第一次行程生成并保存', unlocked: n >= 1, icon: 'spark' },
    { id: 'collector-3', name: '小小收藏家', desc: '累计保存 3 条行程记录', unlocked: n >= 3, icon: 'stack' },
    { id: 'collector-5', name: '行程达人', desc: '累计保存 5 条行程记录', unlocked: n >= 5, icon: 'badge' },
    { id: 'collector-10', name: '旅程馆长', desc: '累计保存 10 条行程记录', unlocked: n >= 10, icon: 'crown' },
    { id: 'multi-city', name: '多城漫游', desc: '生成过至少 1 次多城市行程', unlocked: mc >= 1, icon: 'route' },
    { id: 'multi-city-3', name: '城市串联者', desc: '生成过 3 次多城市行程', unlocked: mc >= 3, icon: 'nodes' },
  ]
})

function iconPath(kind) {
  if (kind === 'spark')
    return 'M10 1l1.4 5.1L16.5 7.5l-5.1 1.4L10 14l-1.4-5.1L3.5 7.5l5.1-1.4L10 1Zm6.2 9.2l.9 3.2l3.2.9l-3.2.9l-.9 3.2l-.9-3.2l-3.2-.9l3.2-.9l.9-3.2Z'
  if (kind === 'stack') return 'M10 3l8 4l-8 4L2 7l8-4Zm0 10l8 4l-8 4l-8-4l8-4Zm0-3l-7-3.5V10l7 3.5L17 10V6.5L10 10Z'
  if (kind === 'badge') return 'M10 2l6 3v6c0 4-2.6 7.4-6 8.5C6.6 18.4 4 15 4 11V5l6-3Zm0 4.2L6 7.9V11c0 3 1.8 5.6 4 6.5c2.2-.9 4-3.5 4-6.5V7.9l-4-1.7Z'
  if (kind === 'crown') return 'M3 7l4 3l3-6l3 6l4-3v10H3V7Zm2 8h10v-5.6l-3 2.2l-2-4l-2 4l-3-2.2V15Z'
  if (kind === 'route') return 'M6 4a2 2 0 1 1 0 4a2 2 0 0 1 0-4Zm8 2a2 2 0 1 1 0 4a2 2 0 0 1 0-4ZM6 8c2 0 3 1 4 2s2 2 4 2h2v2h-2c-2 0-3-1-4-2s-2-2-4-2H4V8h2Zm8 8a2 2 0 1 1 0 4a2 2 0 0 1 0-4Z'
  if (kind === 'nodes') return 'M6 5a2 2 0 1 1 0 4a2 2 0 0 1 0-4Zm10 0a2 2 0 1 1 0 4a2 2 0 0 1 0-4ZM6 11h10v2H6v-2Zm0 4a2 2 0 1 1 0 4a2 2 0 0 1 0-4Zm10 0a2 2 0 1 1 0 4a2 2 0 0 1 0-4Z'
  return ''
}
</script>

<template>
  <section class="hub">
    <header class="hero" :style="{ backgroundImage: `url(${heroBg})` }">
      <div class="hero__mask" />
      <div class="hero__content">
        <div class="profile">
          <div class="avatar" :class="{ 'has-img': !!avatarUrl }">
            <img v-if="avatarUrl" :src="avatarUrl" :alt="displayName" />
            <span v-else class="avatar__fallback" aria-hidden="true">{{ avatarFallback }}</span>
          </div>
          <div class="profile__text">
            <div class="profile__row">
              <div class="profile__name">{{ displayName }}</div>
              <span class="pill pill--points" :title="`成就积分：${points}`">{{ points }}</span>
              <span v-if="handleText" class="pill pill--handle">{{ handleText }}</span>
            </div>
            <div class="profile__sub">行程历史已自动保存；点亮徽章，记录你的每一次出发。</div>
          </div>
        </div>

        <div class="badgeWall" aria-label="徽章墙">
          <div class="badgeWall__title">徽章墙</div>
          <div class="badgeWall__grid">
            <div
              v-for="b in badges"
              :key="b.id"
              class="badge"
              :class="{ 'is-on': b.unlocked }"
              :title="`${b.name}：${b.desc}`"
              role="img"
              :aria-label="b.name"
            >
              <svg viewBox="0 0 20 20" aria-hidden="true">
                <path :d="iconPath(b.icon)" fill="currentColor" />
              </svg>
            </div>
          </div>
        </div>
      </div>
    </header>

    <div class="miniMenu lynx-card lynx-card--glass">
      <button class="miniMenu__item" :class="{ 'is-active': activeMenu === 'itinerary' }" type="button" @click="activeMenu = 'itinerary'">
        我的行程
      </button>
      <button class="miniMenu__item" :class="{ 'is-active': activeMenu === 'storeup' }" type="button" @click="activeMenu = 'storeup'">
        我的收藏
      </button>
    </div>

    <div v-if="activeMenu === 'itinerary' && !ordered.length" class="empty lynx-card lynx-card--glass">
      <div class="empty__title">还没有行程记录</div>
      <div class="empty__desc">点击「去新建」开始生成，系统会自动保存到这里。</div>
      <button class="primaryBtn" type="button" @click="goNew">去新建</button>
    </div>

    <div v-else-if="activeMenu === 'itinerary'" class="grid">
      <article
        v-for="t in ordered"
        :key="t.id"
        class="card lynx-card lynx-card--glass"
        role="button"
        tabindex="0"
        @click="openTrip(t)"
        @keydown.enter.prevent="openTrip(t)"
      >
        <div class="card__media" :class="{ 'has-img': !!coverOf(t) }">
          <el-image v-if="coverOf(t)" class="card__img" :src="coverOf(t)" fit="cover" :alt="metaLine(t)">
            <template #error>
              <div class="card__ph" aria-hidden="true" />
            </template>
          </el-image>
          <div v-else class="card__ph" aria-hidden="true" />
        </div>
        <div class="card__body">
          <div class="card__place">{{ placeLine(t) }}</div>
          <div class="card__tags">
            <span v-for="tag in preferenceTags(t)" :key="`${t.id}-${tag}`" class="tag">{{ tag }}</span>
            <span v-if="!preferenceTags(t).length" class="tag tag--muted">我的偏爱标签</span>
          </div>
          <div class="card__dayRow">
            <div class="card__days">{{ daysLine(t) }}</div>
            <div class="card__date">{{ createdDateLine(t) }}</div>
          </div>
        </div>
      </article>
    </div>

    <div v-else-if="!orderedStoreups.length" class="empty lynx-card lynx-card--glass">
      <div class="empty__title">还没有收藏记录</div>
      <div class="empty__desc">你可以在主题简旅或山河印记页面收藏感兴趣的内容。</div>
    </div>

    <div v-else class="grid">
      <article v-for="s in orderedStoreups" :key="s.id" class="card lynx-card lynx-card--glass">
        <div class="card__media">
          <el-image v-if="s.picture" class="card__img" :src="s.picture" fit="cover" :alt="s.name || '收藏项'">
            <template #error>
              <div class="card__ph" aria-hidden="true" />
            </template>
          </el-image>
          <div v-else class="card__ph" aria-hidden="true" />
        </div>
        <div class="card__body">
          <div class="card__place">{{ s.name || '未命名收藏' }}</div>
          <div class="card__dayRow">
            <div class="card__days">收藏项</div>
            <div class="card__date">{{ createdDateLine(s) }}</div>
          </div>
          <div class="card__tags">
            <span class="tag">ref: {{ s.refid }}</span>
          </div>
        </div>
      </article>
    </div>
  </section>
</template>

<style scoped lang="scss">
.hub {
  display: flex;
  flex-direction: column;
  gap: var(--space-lg);
}

.hero {
  position: relative;
  height: 260px;
  border-radius: 18px;
  overflow: hidden;
  background-size: cover;
  background-position: center;
}
.hero__mask {
  position: absolute;
  inset: 0;
  background:
    linear-gradient(180deg, rgba(2, 6, 23, 0.35) 0%, rgba(2, 6, 23, 0.55) 100%),
    linear-gradient(90deg, rgba(2, 6, 23, 0.55), rgba(2, 6, 23, 0.14));
}
.hero__content {
  position: relative;
  z-index: 1;
  height: 100%;
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 18px;
  gap: 12px;
}
.hero__content > * {
  min-width: 0;
}

.profile {
  display: flex;
  align-items: center;
  gap: 12px;
  color: #fff;
  min-width: 0;
  padding: 12px 14px;
  border-radius: 16px;
  border: 1px solid rgba(255, 255, 255, 0.18);
  background: rgba(2, 6, 23, 0.22);
  backdrop-filter: blur(10px);
  box-shadow: 0 14px 30px rgba(2, 6, 23, 0.18);
}

.avatar {
  width: 56px;
  height: 56px;
  border-radius: 16px;
  overflow: hidden;
  border: 1px solid rgba(255, 255, 255, 0.28);
  background: rgba(255, 255, 255, 0.12);
  box-shadow: 0 16px 30px rgba(2, 6, 23, 0.22);
  display: grid;
  place-items: center;
  flex-shrink: 0;
}
.avatar img {
  width: 100%;
  height: 100%;
  object-fit: cover;
  display: block;
}
.avatar__fallback {
  font-size: 22px;
  font-weight: 900;
  letter-spacing: 0.5px;
}

.profile__text {
  min-width: 0;
}
.profile__row {
  display: flex;
  align-items: center;
  gap: 8px;
  flex-wrap: wrap;
}
.profile__name {
  font-size: 26px;
  font-weight: 900;
  letter-spacing: 0.3px;
  text-shadow: 0 10px 24px rgba(0, 0, 0, 0.22);
}
.profile__sub {
  margin-top: 6px;
  font-size: 13px;
  opacity: 0.9;
  font-weight: 600;
}

.pill {
  height: 24px;
  padding: 0 10px;
  border-radius: 999px;
  border: 1px solid rgba(255, 255, 255, 0.22);
  background: rgba(255, 255, 255, 0.12);
  backdrop-filter: blur(10px);
  color: rgba(255, 255, 255, 0.92);
  font-size: 12px;
  font-weight: 900;
}
.pill--points {
  border-color: rgba(255, 136, 57, 0.55);
  background: rgba(255, 136, 57, 0.16);
}
.pill--handle {
  border-color: rgba(56, 189, 248, 0.45);
  background: rgba(56, 189, 248, 0.14);
  font-weight: 800;
}

.badgeWall {
  display: flex;
  flex-direction: column;
  gap: 10px;
  align-items: flex-end;
  flex-shrink: 0;
  padding: 12px 12px;
  border-radius: 16px;
  border: 1px solid rgba(255, 255, 255, 0.18);
  background: rgba(2, 6, 23, 0.18);
  backdrop-filter: blur(10px);
}
.badgeWall__title {
  font-weight: 900;
  color: rgba(255, 255, 255, 0.92);
  font-size: 13px;
  letter-spacing: 0.4px;
}
.badgeWall__grid {
  display: grid;
  grid-template-columns: repeat(6, 34px);
  gap: 8px;
}
.badge {
  width: 34px;
  height: 34px;
  border-radius: 12px;
  border: 1px solid rgba(255, 255, 255, 0.22);
  background: rgba(255, 255, 255, 0.08);
  display: grid;
  place-items: center;
  color: rgba(255, 255, 255, 0.4);
  transition: transform 180ms ease, box-shadow 180ms ease, background 180ms ease, border-color 180ms ease, color 180ms ease;
  backdrop-filter: blur(10px);
}
.badge svg {
  width: 18px;
  height: 18px;
  display: block;
}
.badge:hover {
  transform: translateY(-1px);
  border-color: rgba(255, 255, 255, 0.35);
  background: rgba(255, 255, 255, 0.12);
}
.badge.is-on {
  color: rgba(255, 255, 255, 0.98);
  border-color: rgba(255, 136, 57, 0.55);
  background: linear-gradient(135deg, rgba(255, 136, 57, 0.22), rgba(56, 189, 248, 0.14));
  box-shadow: 0 12px 24px rgba(2, 6, 23, 0.18), 0 0 0 3px rgba(255, 136, 57, 0.12);
}

.primaryBtn {
  height: 40px;
  padding: 0 14px;
  border: 0;
  border-radius: 12px;
  background: linear-gradient(135deg, #ff8839, #f97316);
  color: #fff;
  font-weight: 900;
  cursor: pointer;
  box-shadow: 0 10px 24px rgba(249, 115, 22, 0.22);
  flex-shrink: 0;
}
.primaryBtn:hover {
  filter: brightness(0.98);
}

.miniMenu {
  padding: 12px 14px;
  border-radius: 18px;
  display: flex;
  align-items: center;
  justify-content: flex-start;
  gap: 12px;
}
.miniMenu__item {
  height: 34px;
  padding: 0 14px;
  border-radius: 999px;
  border: 1px solid rgba(15, 23, 42, 0.12);
  background: rgba(255, 255, 255, 0.92);
  color: #334155;
  font-size: 12px;
  font-weight: 900;
  cursor: pointer;
}
.miniMenu__item.is-active {
  color: #7c2d12;
  border-color: rgba(249, 115, 22, 0.35);
  background: rgba(249, 115, 22, 0.14);
}
.empty {
  padding: 18px;
  border-radius: 18px;
  display: flex;
  flex-direction: column;
  gap: 10px;
}
.empty__title {
  font-weight: 900;
  color: #0f172a;
  font-size: 16px;
}
.empty__desc {
  color: #64748b;
  font-size: 13px;
  line-height: 1.6;
}

.grid {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 16px;
}

.card {
  overflow: hidden;
  cursor: pointer;
  border-radius: 16px;
  border: 1px solid rgba(15, 23, 42, 0.08);
  background: rgba(255, 255, 255, 0.92);
  transition: transform 180ms ease, box-shadow 180ms ease, border-color 180ms ease, background 180ms ease;
}
.card:hover {
  transform: translateY(-3px);
  box-shadow: 0 14px 30px rgba(15, 23, 42, 0.1);
  border-color: rgba(255, 136, 57, 0.32);
  background: rgba(255, 255, 255, 0.98);
}

.card__media {
  position: relative;
  height: 178px;
  background: #f8fafc;
  border-bottom: 1px solid rgba(15, 23, 42, 0.06);
}
.card__ph {
  width: 100%;
  height: 100%;
  background: radial-gradient(520px 220px at 30% 20%, rgba(56, 189, 248, 0.18), transparent 60%),
    radial-gradient(520px 220px at 80% 35%, rgba(249, 115, 22, 0.18), transparent 60%),
    rgba(248, 250, 252, 1);
}
.card__img {
  width: 100%;
  height: 100%;
  display: block;
}
.card__body {
  padding: 14px 14px 12px;
}
.card__place {
  font-size: 17px;
  font-weight: 900;
  color: #0f172a;
  line-height: 1.35;
  letter-spacing: 0.1px;
  display: -webkit-box;
  -webkit-line-clamp: 1;
  -webkit-box-orient: vertical;
  overflow: hidden;
}
.card__days {
  font-size: 28px;
  font-weight: 900;
  color: #0f172a;
  line-height: 1;
}
.card__dayRow {
  margin-top: 12px;
  display: flex;
  align-items: flex-end;
  justify-content: space-between;
  gap: 10px;
}
.card__date {
  margin-left: auto;
  text-align: right;
  font-size: 12px;
  font-weight: 800;
  color: #94a3b8;
  font-variant-numeric: tabular-nums;
}
.card__tags {
  margin-top: 10px;
  display: flex;
  gap: 8px;
  flex-wrap: wrap;
  justify-content: flex-end;
}
.tag {
  height: 24px;
  padding: 0 10px;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  border-radius: 999px;
  border: 1px solid rgba(99, 102, 241, 0.18);
  background: rgba(99, 102, 241, 0.08);
  font-size: 11px;
  font-weight: 800;
  line-height: 1;
  color: #4f46e5;
}
.tag--muted {
  color: #64748b;
  border-color: rgba(15, 23, 42, 0.08);
  background: rgba(15, 23, 42, 0.04);
}

@media (max-width: 1100px) {
  .grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
}
@media (max-width: 720px) {
  .hero {
    height: 280px;
  }
  .grid {
    grid-template-columns: 1fr;
  }
  .hero__content {
    flex-direction: column;
    align-items: flex-start;
  }
  .badgeWall {
    align-items: flex-start;
    width: 100%;
  }
  .badgeWall__grid {
    grid-template-columns: repeat(6, 32px);
  }
  .badge {
    width: 32px;
    height: 32px;
  }
}
</style>

