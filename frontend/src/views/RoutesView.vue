<script setup>
import { computed, onMounted, ref, watch } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { requireLogin } from '../utils/requireAuth'
import {
  bumpRouteClick,
  listCategories,
  listRoutes,
  toggleStoreup,
  listStoreup,
  upsertCartItem,
} from '../services/lynxDb'

const router = useRouter()

const tab = ref('lvyouxianlu')
const keyword = ref('')
const category = ref('')
const sortKey = ref('hot') // hot | priceAsc | priceDesc | timeAsc | timeDesc

const drawerOpen = ref(false)
const active = ref(null)
const routesLv = ref([])
const routesNew = ref([])
const categoriesList = ref([])

const categories = computed(() => categoriesList.value.map((c) => c.xianlufenlei))

const storeupSet = ref(new Set())
async function refreshStoreup() {
  const items = await listStoreup()
  storeupSet.value = new Set(items.map((s) => `${s.tablename}:${s.refid}`))
}

onMounted(async () => {
  categoriesList.value = await listCategories()
  routesLv.value = await listRoutes('lvyouxianlu')
  routesNew.value = await listRoutes('zuixinxianlu')
  await refreshStoreup()
})

const rawList = computed(() => (tab.value === 'zuixinxianlu' ? routesNew.value : routesLv.value))

const filtered = computed(() => {
  const kw = keyword.value.trim()
  return rawList.value.filter((r) => {
    if (category.value && String(r.xianlufenlei) !== String(category.value)) return false
    if (!kw) return true
    const hay = [
      r.xianlumingcheng,
      r.xianlufenlei,
      r.jingdianmingcheng,
      r.chufadi,
      r.mudedi,
      r.jiaotongfangshi,
    ]
      .filter(Boolean)
      .join(' ')
    return hay.toLowerCase().includes(kw.toLowerCase())
  })
})

const sorted = computed(() => {
  const list = [...filtered.value]
  if (sortKey.value === 'priceAsc') list.sort((a, b) => Number(a.price || 0) - Number(b.price || 0))
  if (sortKey.value === 'priceDesc') list.sort((a, b) => Number(b.price || 0) - Number(a.price || 0))
  if (sortKey.value === 'timeAsc') list.sort((a, b) => new Date(a.chuxingshijian || 0) - new Date(b.chuxingshijian || 0))
  if (sortKey.value === 'timeDesc') list.sort((a, b) => new Date(b.chuxingshijian || 0) - new Date(a.chuxingshijian || 0))
  if (sortKey.value === 'hot') list.sort((a, b) => Number(b.clicknum || 0) - Number(a.clicknum || 0))
  return list
})

async function openDetail(row) {
  if (!(await requireLogin(router, { message: '查看路线详情请先登录', redirect: router.currentRoute.value.fullPath }))) return
  active.value = row
  drawerOpen.value = true
  await bumpRouteClick(tab.value, row.id)
}

function formatDate(dt) {
  if (!dt) return '—'
  const d = new Date(dt)
  if (Number.isNaN(d.getTime())) return String(dt)
  return `${d.getFullYear()}-${String(d.getMonth() + 1).padStart(2, '0')}-${String(d.getDate()).padStart(2, '0')}`
}

function isFav(row) {
  return storeupSet.value.has(`${tab.value}:${row.id}`)
}

async function toggleFav(row) {
  if (!(await requireLogin(router, { message: '收藏功能需登录后使用', redirect: router.currentRoute.value.fullPath }))) return
  await toggleStoreup({
    tablename: tab.value,
    refid: row.id,
    name: row.xianlumingcheng,
    picture: row.fengmiantu,
  })
  await refreshStoreup()
  ElMessage.success(isFav(row) ? '已收藏' : '已取消收藏')
}

async function addToCart(row) {
  if (!(await requireLogin(router, { message: '加入购物车需先登录', redirect: router.currentRoute.value.fullPath }))) return
  await upsertCartItem({ tablename: tab.value, good: row })
  ElMessage.success('已加入购物车')
}

watch(tab, () => {
  category.value = ''
  keyword.value = ''
  sortKey.value = 'hot'
})
</script>

<template>
  <section class="page">
    <header class="hero lynx-card">
      <div class="hero__left">
        <h2 class="hero__title lynx-h">旅游路线</h2>
      </div>
      <div class="hero__right">
        <div class="hero__metric">
          <div class="hero__metricNum">{{ sorted.length }}</div>
          <div class="hero__metricLabel">可浏览线路</div>
        </div>
      </div>
    </header>

    <div class="toolbar lynx-card lynx-card--glass">
      <el-tabs v-model="tab" class="toolbar__tabs">
        <el-tab-pane label="旅游线路" name="lvyouxianlu" />
        <el-tab-pane label="最新线路" name="zuixinxianlu" />
      </el-tabs>

      <div class="toolbar__row">
        <el-input v-model="keyword" clearable placeholder="搜索：线路名称 / 景点 / 出发地 / 目的地" />

        <el-select v-model="category" clearable placeholder="线路分类" style="width: 160px">
          <el-option v-for="c in categories" :key="c" :label="c" :value="c" />
        </el-select>

        <el-select v-model="sortKey" placeholder="排序" style="width: 150px">
          <el-option label="热度优先（点击量）" value="hot" />
          <el-option label="价格从低到高" value="priceAsc" />
          <el-option label="价格从高到低" value="priceDesc" />
          <el-option label="出行时间从早到晚" value="timeAsc" />
          <el-option label="出行时间从晚到早" value="timeDesc" />
        </el-select>
      </div>
    </div>

    <div v-if="!sorted.length" class="empty lynx-card lynx-card--glass">
      <el-empty description="暂无符合条件的线路" />
    </div>

    <div class="grid">
      <article v-for="row in sorted" :key="row.id" class="card lynx-card lynx-card--glass" @click="openDetail(row)">
        <div class="card__cover">
          <el-image class="card__img" :src="row.fengmiantu" fit="cover" :alt="row.xianlumingcheng" />
          <div class="card__badges">
            <span class="badge">{{ row.xianlufenlei }}</span>
            <span class="badge badge--soft">¥ {{ Number(row.price || 0).toFixed(0) }}</span>
          </div>
        </div>
        <div class="card__body">
          <div class="card__title">{{ row.xianlumingcheng }}</div>
          <div class="card__meta">
            <span class="metaItem">出发：{{ row.chufadi || '—' }}</span>
            <span class="dot" aria-hidden="true" />
            <span class="metaItem">目的：{{ row.mudedi || '—' }}</span>
          </div>
          <div class="card__meta">
            <span class="metaItem">交通：{{ row.jiaotongfangshi || '—' }}</span>
            <span class="dot" aria-hidden="true" />
            <span class="metaItem">时间：{{ formatDate(row.chuxingshijian) }}</span>
          </div>
          <div class="card__footer">
            <span class="hot">点击 {{ Number(row.clicknum || 0) }}</span>
            <span class="cta">查看详情 →</span>
          </div>
        </div>
      </article>
    </div>

    <el-drawer v-model="drawerOpen" size="520px" :with-header="false" class="drawer">
      <div v-if="active" class="drawer__wrap">
        <div class="drawer__cover">
          <img class="drawer__img" :src="active.fengmiantu" :alt="active.xianlumingcheng" />
          <div class="drawer__overlay" />
          <div class="drawer__head">
            <div class="drawer__tags">
              <span class="badge">{{ active.xianlufenlei }}</span>
              <span class="badge badge--soft">¥ {{ Number(active.price || 0).toFixed(0) }}</span>
              <span class="badge badge--soft">点击 {{ Number(active.clicknum || 0) }}</span>
            </div>
            <div class="drawer__title lynx-h">{{ active.xianlumingcheng }}</div>
            <div class="drawer__sub">
              <span>{{ active.chufadi || '—' }}</span>
              <span class="dot dot--light" aria-hidden="true" />
              <span>{{ active.mudedi || '—' }}</span>
              <span class="dot dot--light" aria-hidden="true" />
              <span>{{ active.jiaotongfangshi || '—' }}</span>
              <span class="dot dot--light" aria-hidden="true" />
              <span>{{ formatDate(active.chuxingshijian) }}</span>
            </div>
          </div>
        </div>

        <div class="drawer__body">
          <div class="drawer__actions">
            <el-button type="primary" @click="addToCart(active)">加入购物车</el-button>
            <el-button :type="isFav(active) ? 'warning' : 'default'" @click="toggleFav(active)">
              {{ isFav(active) ? '已收藏' : '收藏' }}
            </el-button>
          </div>

          <div class="section lynx-card">
            <div class="section__hd">景点名称</div>
            <div class="section__bd">{{ active.jingdianmingcheng }}</div>
          </div>

          <div class="section lynx-card">
            <div class="section__hd">费用包含</div>
            <div class="section__bd pre">{{ active.feiyongbaohan || '—' }}</div>
          </div>

          <div class="section lynx-card">
            <div class="section__hd">行程路线</div>
            <div class="section__bd rich" v-html="active.xingchengluxian || '<p>—</p>'" />
          </div>
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
  padding: 18px 18px;
  border-radius: 18px;
  background: radial-gradient(1000px 320px at 20% 10%, rgba(14, 165, 233, 0.16), transparent 60%),
    radial-gradient(1000px 320px at 85% 30%, rgba(249, 115, 22, 0.18), transparent 60%),
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
  margin: 6px 0 6px 0;
  font-size: 22px;
  font-weight: 900;
  color: #0f172a;
}

.hero__desc {
  margin: 0;
  color: #334155;
  line-height: 1.7;
  max-width: 56ch;
  font-size: 13px;
}

.hero__metric {
  min-width: 130px;
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
  padding: 14px 14px 12px 14px;
  border-radius: 18px;
}

.toolbar__row {
  display: grid;
  grid-template-columns: 1fr 160px 150px;
  gap: 10px;
  align-items: center;
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
  border-color: rgba(255, 136, 57, 0.35);
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
  flex-wrap: wrap;
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
  color: #7c2d12;
  border-color: rgba(249, 115, 22, 0.2);
}

.card__body {
  padding: 12px 12px 12px 12px;
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

.card__meta {
  margin-top: 8px;
  display: flex;
  align-items: center;
  gap: 8px;
  color: #475569;
  font-size: 12px;
}

.metaItem {
  display: inline-flex;
  align-items: center;
  min-width: 0;
}

.dot {
  width: 4px;
  height: 4px;
  border-radius: 999px;
  background: rgba(71, 85, 105, 0.45);
  flex: 0 0 auto;
}
.dot--light {
  background: rgba(255, 255, 255, 0.55);
}

.card__footer {
  margin-top: 10px;
  display: flex;
  align-items: center;
  justify-content: space-between;
  font-size: 12px;
}

.hot {
  color: #64748b;
}
.cta {
  color: var(--lynx-brand-hover);
  font-weight: 800;
}

.drawer :deep(.el-drawer__body) {
  padding: 0;
}

.drawer__wrap {
  height: 100%;
  display: flex;
  flex-direction: column;
}

.drawer__cover {
  position: relative;
  height: 220px;
  overflow: hidden;
}
.drawer__img {
  width: 100%;
  height: 100%;
  object-fit: cover;
  display: block;
}
.drawer__overlay {
  position: absolute;
  inset: 0;
  background: linear-gradient(to bottom, rgba(2, 6, 23, 0.08), rgba(2, 6, 23, 0.58));
}
.drawer__head {
  position: absolute;
  inset: 0;
  display: flex;
  flex-direction: column;
  justify-content: flex-end;
  padding: 16px;
  gap: 8px;
  color: #fff;
}
.drawer__tags {
  display: flex;
  gap: 8px;
  flex-wrap: wrap;
}
.drawer__title {
  font-size: 18px;
  font-weight: 900;
}
.drawer__sub {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  font-size: 12px;
  color: rgba(255, 255, 255, 0.85);
}

.drawer__body {
  padding: 16px;
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.drawer__actions {
  display: flex;
  gap: 10px;
}

.section {
  padding: 12px;
  border-radius: 16px;
}
.section__hd {
  font-weight: 900;
  color: #0f172a;
  margin-bottom: 8px;
}
.section__bd {
  color: #334155;
  line-height: 1.75;
  font-size: 13px;
}
.pre {
  white-space: pre-wrap;
}
.rich :deep(img) {
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
  .toolbar__row {
    grid-template-columns: 1fr;
  }
  .grid {
    grid-template-columns: 1fr;
  }
}
</style>

