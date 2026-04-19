<script setup>
import { computed, onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { requireLogin } from '../utils/requireAuth'
import { addGalleryItem, listGallery, listStoreup } from '../services/lynxDb'

const router = useRouter()

const keyword = ref('')
const isAddOpen = ref(false)
const form = ref({
  title: '',
  photoUrl: '',
  note: '',
  takenAt: '',
  location: '',
})

const list = ref([])
function refresh() {
  list.value = listGallery()
}

onMounted(refresh)

const filtered = computed(() => {
  const kw = keyword.value.trim().toLowerCase()
  if (!kw) return list.value
  return list.value.filter((g) => {
    const hay = [g.title, g.note, g.location, g.takenAt].filter(Boolean).join(' ').toLowerCase()
    return hay.includes(kw)
  })
})

const storeups = computed(() => listStoreup())

function openAdd() {
  if (!requireLogin(router, { message: '新增拾记需先登录', redirect: router.currentRoute.value.fullPath })) return
  isAddOpen.value = true
}

function submit() {
  if (!requireLogin(router, { message: '请先登录后再提交', redirect: router.currentRoute.value.fullPath })) return
  if (!form.value.photoUrl?.trim()) {
    ElMessage.warning('请先填写图片链接（photoUrl）')
    return
  }
  addGalleryItem({ ...form.value })
  isAddOpen.value = false
  form.value = { title: '', photoUrl: '', note: '', takenAt: '', location: '' }
  refresh()
  ElMessage.success('已添加到光影拾记')
}

function quickFromStoreup(row) {
  if (!requireLogin(router, { message: '从收藏生成拾记需先登录', redirect: router.currentRoute.value.fullPath })) return
  addGalleryItem({
    title: row.name,
    photoUrl: row.picture,
    note: `来自收藏：${row.tablename} #${row.refid}`,
    takenAt: '',
    location: '',
  })
  refresh()
  ElMessage.success('已从收藏生成拾记')
}
</script>

<template>
  <section class="page">
    <header class="hero lynx-card">
      <div class="hero__left">
        <h2 class="hero__title lynx-h">光影拾记</h2>
      </div>
      <div class="hero__right">
        <div class="hero__metric">
          <div class="hero__metricNum">{{ filtered.length }}</div>
          <div class="hero__metricLabel">条拾记</div>
        </div>
      </div>
    </header>

    <div class="toolbar lynx-card lynx-card--glass">
      <div class="toolbar__row">
        <el-input v-model="keyword" clearable placeholder="搜索：标题 / 备注 / 地点 / 时间" />
        <el-button type="primary" @click="openAdd">新增拾记</el-button>
      </div>
    </div>

    <div class="layout">
      <div class="grid">
        <article v-for="row in filtered" :key="row.id" class="tile lynx-card lynx-card--glass">
          <div class="tile__imgWrap">
            <el-image class="tile__img" :src="row.photoUrl" fit="cover" :alt="row.title" />
          </div>
          <div class="tile__body">
            <div class="tile__title">{{ row.title }}</div>
            <div class="tile__meta">
              <span v-if="row.location" class="pill">{{ row.location }}</span>
              <span v-if="row.takenAt" class="pill pill--soft">{{ row.takenAt }}</span>
            </div>
            <p v-if="row.note" class="tile__note">{{ row.note }}</p>
          </div>
        </article>

        <div v-if="!filtered.length" class="empty lynx-card lynx-card--glass">
          <el-empty description="还没有拾记，先从收藏生成一条吧" />
        </div>
      </div>

      <aside class="side lynx-card lynx-card--glass">
        <div class="side__hd">
          <div class="side__title">从收藏生成拾记</div>
        </div>
        <div class="side__list">
          <div v-if="!storeups.length" class="side__empty">暂无收藏。可先去「旅游路线」点收藏。</div>
          <button v-for="s in storeups" :key="s.id" class="side__item" type="button" @click="quickFromStoreup(s)">
            <el-image class="side__img" :src="s.picture" fit="cover" :alt="s.name" />
            <div class="side__info">
              <div class="side__name">{{ s.name }}</div>
              <div class="side__meta">{{ s.tablename }} #{{ s.refid }}</div>
            </div>
            <span class="side__cta">生成</span>
          </button>
        </div>
      </aside>
    </div>

    <el-dialog v-model="isAddOpen" title="新增拾记" width="560px">
      <div class="dialogBody">
        <el-form label-position="top">
          <el-form-item label="标题（title）">
            <el-input v-model="form.title" placeholder="例如：武当山的清晨" />
          </el-form-item>
          <el-form-item label="图片链接（photoUrl）">
            <el-input v-model="form.photoUrl" placeholder="粘贴图片 URL（可用上传目录的 URL）" />
          </el-form-item>
          <div class="dialogRow">
            <el-form-item label="拍摄时间（takenAt）">
              <el-input v-model="form.takenAt" placeholder="例如：2026-04-18" />
            </el-form-item>
            <el-form-item label="地点（location）">
              <el-input v-model="form.location" placeholder="例如：十堰 · 武当山" />
            </el-form-item>
          </div>
          <el-form-item label="备注（note）">
            <el-input v-model="form.note" type="textarea" :rows="4" placeholder="写下一句话，把那一刻留住。" />
          </el-form-item>
        </el-form>
        <div v-if="form.photoUrl" class="preview lynx-card lynx-card--glass">
          <el-image class="preview__img" :src="form.photoUrl" fit="cover" />
        </div>
      </div>
      <template #footer>
        <el-button @click="isAddOpen = false">取消</el-button>
        <el-button type="primary" @click="submit">保存</el-button>
      </template>
    </el-dialog>
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
  background: radial-gradient(920px 320px at 20% 10%, rgba(249, 115, 22, 0.16), transparent 60%),
    radial-gradient(920px 320px at 82% 28%, rgba(14, 165, 233, 0.14), transparent 60%),
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
  max-width: 62ch;
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
  grid-template-columns: 1fr 120px;
  gap: 10px;
  align-items: center;
}

.layout {
  display: grid;
  grid-template-columns: 1fr 360px;
  gap: 14px;
  align-items: start;
}

.grid {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 14px;
}

.tile {
  overflow: hidden;
  border-radius: 18px;
}
.tile__imgWrap {
  height: 190px;
  background: #f8fafc;
}
.tile__img {
  width: 100%;
  height: 190px;
  display: block;
}
.tile__body {
  padding: 12px;
}
.tile__title {
  font-weight: 900;
  color: #0f172a;
  line-height: 1.3;
}
.tile__meta {
  margin-top: 8px;
  display: flex;
  gap: 8px;
  flex-wrap: wrap;
}
.pill {
  display: inline-flex;
  align-items: center;
  height: 24px;
  padding: 0 10px;
  border-radius: 999px;
  font-size: 12px;
  font-weight: 700;
  color: #0f172a;
  background: rgba(255, 255, 255, 0.9);
  border: 1px solid rgba(15, 23, 42, 0.08);
}
.pill--soft {
  color: #7c2d12;
  border-color: rgba(249, 115, 22, 0.22);
}
.tile__note {
  margin: 10px 0 0 0;
  color: #475569;
  line-height: 1.7;
  font-size: 13px;
  display: -webkit-box;
  -webkit-line-clamp: 3;
  -webkit-box-orient: vertical;
  overflow: hidden;
  min-height: 3.6em;
}

.empty {
  grid-column: 1 / -1;
  padding: 18px;
}

.side {
  border-radius: 18px;
  padding: 12px;
  position: sticky;
  top: calc(56px + var(--space-lg));
}
.side__hd {
  padding: 4px 4px 10px 4px;
}
.side__title {
  font-weight: 900;
  color: #0f172a;
}
.side__hint {
  margin-top: 6px;
  font-size: 12px;
  color: #475569;
  line-height: 1.6;
}
.side__list {
  display: flex;
  flex-direction: column;
  gap: 10px;
}
.side__empty {
  font-size: 13px;
  color: #64748b;
  padding: 10px 6px;
}
.side__item {
  border: 1px solid rgba(15, 23, 42, 0.08);
  background: rgba(255, 255, 255, 0.86);
  border-radius: 16px;
  padding: 10px;
  display: grid;
  grid-template-columns: 56px 1fr auto;
  gap: 10px;
  align-items: center;
  cursor: pointer;
  transition: transform 180ms ease, border-color 180ms ease, box-shadow 180ms ease;
}
.side__item:hover {
  transform: translateY(-1px);
  border-color: rgba(255, 136, 57, 0.35);
  box-shadow: 0 10px 22px rgba(15, 23, 42, 0.06);
}
.side__img {
  width: 56px;
  height: 44px;
  border-radius: 12px;
}
.side__info {
  min-width: 0;
  text-align: left;
}
.side__name {
  font-weight: 900;
  color: #0f172a;
  line-height: 1.25;
  font-size: 13px;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
}
.side__meta {
  margin-top: 6px;
  font-size: 12px;
  color: #64748b;
}
.side__cta {
  font-size: 12px;
  font-weight: 900;
  color: var(--lynx-brand-hover);
}

.dialogRow {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 10px;
}
.preview {
  margin-top: 8px;
  border-radius: 18px;
  overflow: hidden;
}
.preview__img {
  width: 100%;
  height: 180px;
}

@media (max-width: 1200px) {
  .layout {
    grid-template-columns: 1fr;
  }
  .side {
    position: relative;
    top: 0;
  }
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
  .dialogRow {
    grid-template-columns: 1fr;
  }
}
</style>

