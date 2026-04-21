<script setup>
import { computed, onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { requireLogin } from '../utils/requireAuth'
import { listNews } from '../services/lynxDb'

const router = useRouter()

const keyword = ref('')
const drawerOpen = ref(false)
const active = ref(null)

const list = ref([])
onMounted(() => {
  listNews().then((rows) => {
    list.value = rows
  })
})

const filtered = computed(() => {
  const kw = keyword.value.trim().toLowerCase()
  if (!kw) return list.value
  return list.value.filter((n) => {
    const hay = [n.title, n.introduction, n.content].filter(Boolean).join(' ').toLowerCase()
    return hay.includes(kw)
  })
})

async function openDetail(row) {
  if (!(await requireLogin(router, { message: '阅读全文需先登录', redirect: router.currentRoute.value.fullPath }))) return
  active.value = row
  drawerOpen.value = true
}

function formatDate(ts) {
  if (!ts) return '—'
  const d = new Date(ts)
  if (Number.isNaN(d.getTime())) return String(ts)
  return `${d.getFullYear()}-${String(d.getMonth() + 1).padStart(2, '0')}-${String(d.getDate()).padStart(2, '0')}`
}
</script>

<template>
  <section class="page">
    <header class="hero lynx-card">
      <div class="hero__left">
        <h2 class="hero__title lynx-h">旅游资讯</h2>
      </div>
      <div class="hero__right">
        <div class="hero__metric">
          <div class="hero__metricNum">{{ filtered.length }}</div>
          <div class="hero__metricLabel">篇资讯</div>
        </div>
      </div>
    </header>

    <div class="toolbar lynx-card lynx-card--glass">
      <div class="toolbar__row">
        <el-input v-model="keyword" clearable placeholder="搜索：标题 / 简介 / 正文" />
      </div>
    </div>

    <div v-if="!filtered.length" class="empty lynx-card lynx-card--glass">
      <el-empty description="暂无资讯" />
    </div>

    <div class="grid">
      <article v-for="row in filtered" :key="row.id" class="card lynx-card lynx-card--glass" @click="openDetail(row)">
        <div class="card__cover">
          <el-image class="card__img" :src="row.picture" fit="cover" :alt="row.title" />
          <div class="card__badges">
            <span class="badge badge--soft">{{ formatDate(row.addtime) }}</span>
          </div>
        </div>
        <div class="card__body">
          <div class="card__title">{{ row.title }}</div>
          <p class="card__intro">
            {{ row.introduction }}
          </p>
          <div class="card__footer">
            <span class="cta">阅读全文 →</span>
          </div>
        </div>
      </article>
    </div>

    <el-drawer v-model="drawerOpen" size="640px" :with-header="false" class="drawer">
      <div v-if="active" class="drawer__wrap">
        <div class="drawer__top">
          <div class="drawer__meta">
            <span class="badge badge--soft">{{ formatDate(active.addtime) }}</span>
          </div>
          <div class="drawer__title lynx-h">{{ active.title }}</div>
          <p class="drawer__intro">{{ active.introduction }}</p>
        </div>

        <div class="drawer__cover lynx-card lynx-card--glass">
          <el-image class="drawer__img" :src="active.picture" fit="cover" :alt="active.title" />
        </div>

        <div class="drawer__content lynx-card">
          <div class="drawer__contentHd">正文</div>
          <div class="drawer__rich" v-html="active.content" />
        </div>
      </div>
    </el-drawer>
  </section>
</template>

<style scoped lang="scss">
.page {
  display: flex;
  flex-direction: column;
  gap: var(--space-lg);
}

.hero {
  padding: 18px;
  border-radius: 18px;
  background: radial-gradient(920px 320px at 18% 10%, rgba(56, 189, 248, 0.16), transparent 60%),
    radial-gradient(920px 320px at 80% 35%, rgba(249, 115, 22, 0.16), transparent 60%),
    rgba(255, 255, 255, 0.92);
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 18px;
}

.hero__kicker {
  color: var(--lynx-muted);
  font-size: 13px;
}
.hero__title {
  margin: 6px 0;
  font-size: 22px;
  font-weight: 900;
  color: #0f172a;
}
.hero__desc {
  margin: 0;
  color: #334155;
  line-height: 1.7;
  max-width: 60ch;
  font-size: 13px;
}
.hero__metric {
  min-width: 120px;
  text-align: right;
}
.hero__metricNum {
  font-size: 32px;
  font-weight: 900;
  color: #0f172a;
}
.hero__metricLabel {
  font-size: 12px;
  color: var(--lynx-muted);
}

.toolbar {
  padding: 14px;
  border-radius: 18px;
}
.toolbar__row {
  display: grid;
  grid-template-columns: 1fr;
}

.empty {
  padding: 18px;
}

.grid {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 14px;
}

.card {
  overflow: hidden;
  cursor: pointer;
  transition: transform 180ms ease, box-shadow 180ms ease, border-color 180ms ease;
}
.card:hover {
  transform: translateY(-2px);
  box-shadow: 0 10px 24px rgba(15, 23, 42, 0.08);
  border-color: rgba(56, 189, 248, 0.35);
}

.card__cover {
  position: relative;
  height: 156px;
  background: #f8fafc;
}
.card__img {
  width: 100%;
  height: 156px;
  display: block;
}
.card__badges {
  position: absolute;
  left: 12px;
  top: 12px;
  display: flex;
  gap: 8px;
}

.badge {
  display: inline-flex;
  align-items: center;
  height: 26px;
  padding: 0 10px;
  border-radius: 999px;
  font-size: 12px;
  font-weight: 700;
  color: #0f172a;
  background: rgba(255, 255, 255, 0.92);
  border: 1px solid rgba(15, 23, 42, 0.08);
  backdrop-filter: blur(10px);
}
.badge--soft {
  color: #0c4a6e;
  border-color: rgba(14, 165, 233, 0.22);
}

.card__body {
  padding: 12px;
}
.card__title {
  font-weight: 900;
  color: #0f172a;
  line-height: 1.3;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
  min-height: 2.6em;
}
.card__intro {
  margin: 8px 0 0 0;
  color: #475569;
  line-height: 1.7;
  font-size: 13px;
  display: -webkit-box;
  -webkit-line-clamp: 3;
  -webkit-box-orient: vertical;
  overflow: hidden;
  min-height: 4.8em;
}
.card__footer {
  margin-top: 10px;
  display: flex;
  justify-content: flex-end;
}
.cta {
  color: var(--lynx-brand-hover);
  font-weight: 800;
  font-size: 12px;
}

.drawer :deep(.el-drawer__body) {
  padding: 0;
}
.drawer__wrap {
  padding: 16px;
  display: flex;
  flex-direction: column;
  gap: 12px;
}
.drawer__top {
  padding: 12px 12px 4px 12px;
}
.drawer__title {
  margin-top: 10px;
  font-weight: 900;
  font-size: 20px;
  color: #0f172a;
}
.drawer__intro {
  margin: 8px 0 0 0;
  color: #475569;
  line-height: 1.75;
  font-size: 13px;
}
.drawer__cover {
  overflow: hidden;
  border-radius: 18px;
}
.drawer__img {
  width: 100%;
  height: 220px;
}
.drawer__content {
  padding: 12px;
  border-radius: 18px;
}
.drawer__contentHd {
  font-weight: 900;
  color: #0f172a;
  margin-bottom: 8px;
}
.drawer__rich {
  color: #334155;
  line-height: 1.8;
  font-size: 14px;
}
.drawer__rich :deep(img) {
  max-width: 100%;
  height: auto;
  border-radius: 12px;
  border: 1px solid rgba(15, 23, 42, 0.08);
}

@media (max-width: 1100px) {
  .grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
}
@media (max-width: 720px) {
  .grid {
    grid-template-columns: 1fr;
  }
}
</style>

