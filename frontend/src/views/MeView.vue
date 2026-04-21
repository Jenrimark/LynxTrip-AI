<script setup>
import { computed, onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  fetchMe,
  getErrorMessage,
  isLoggedInRemote,
  logout,
  updatePassword as updatePasswordRemote,
  updateProfile as updateProfileRemote,
} from '../services/auth'
import { formatUserIdDisplay, deleteUserAccount, listTrips, listStoreup, listRoutes, saveTrip, toggleStoreup } from '../services/lynxDb'

const router = useRouter()
const user = ref(null)
const userIdDisplay = computed(() => formatUserIdDisplay(user.value?.id))
const tripItems = ref([])
const storeupItems = ref([])
const travelTab = ref('trip')
const seeding = ref(false)

function normalizeRoute(routeItem) {
  if (!routeItem) return null
  return {
    tablename: routeItem.__table || 'lvyouxianlu',
    id: routeItem.id,
    name: routeItem.xianlumingcheng || '未命名路线',
    cover: routeItem.fengmiantu || '',
    price: Number(routeItem.price || 0),
    from: routeItem.chufadi || '',
    to: routeItem.mudedi || '',
    traffic: routeItem.jiaotongfangshi || '',
    category: routeItem.xianlufenlei || '',
  }
}

async function refreshTravel() {
  const [trips, storeups] = await Promise.all([listTrips(), listStoreup()])
  tripItems.value = Array.isArray(trips) ? trips : []
  storeupItems.value = Array.isArray(storeups) ? storeups : []
}

onMounted(async () => {
  if (!(await isLoggedInRemote())) {
    router.replace({ name: 'login', query: { redirect: '/me' } })
    return
  }
  const [nextUser, trips, storeups] = await Promise.all([fetchMe(true), listTrips(), listStoreup()])
  user.value = nextUser
  tripItems.value = Array.isArray(trips) ? trips : []
  storeupItems.value = Array.isArray(storeups) ? storeups : []
})

const moneyText = computed(() => {
  const m = Number(user.value?.money || 0)
  return m.toFixed(2)
})

const userEmailText = computed(() => {
  return String(user.value?.youxiang || user.value?.email || '未绑定')
})

const realNameText = computed(() => {
  return String(user.value?.shimingrenzheng || '未认证')
})

const emailStatusText = computed(() => (userEmailText.value === '未绑定' ? '未绑定邮箱' : '邮箱已绑定'))
const orderedTrips = computed(() => [...tripItems.value].sort((a, b) => Number(b.id) - Number(a.id)))
const orderedStoreups = computed(() => [...storeupItems.value].sort((a, b) => Number(b.id) - Number(a.id)))
const travelCards = computed(() => (travelTab.value === 'trip' ? orderedTrips.value : orderedStoreups.value))

function travelCardTitle(item) {
  return String(item?.title || item?.name || item?.goodname || '未命名记录')
}

function travelCardMeta(item) {
  if (travelTab.value === 'trip') {
    const days = Number(item?.payload?.days || 0)
    return Number.isFinite(days) && days > 0 ? `${days} 天行程` : '行程记录'
  }
  const tablename = String(item?.tablename || '').trim()
  return tablename ? `收藏于 ${tablename}` : '收藏记录'
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

function placeLine(item) {
  const p = item?.payload || {}
  const from = String(p.departure || '').trim()
  const to = String(p.destination || '').trim()
  const places = []
  if (from) places.push(from)
  if (to && to !== from) places.push(to)
  if (!places.length) places.push(...parsePlacesFromTitle(item?.title || item?.name || ''))
  const unique = [...new Set(places)].slice(0, 3)
  if (!unique.length) return '中国 🇨🇳'
  return unique.map((x) => `${x} 🇨🇳`).join(' · ')
}

function preferenceTags(item) {
  const p = item?.payload || {}
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
  return uniq
}

function preferenceTagsView(item, limit = 3) {
  const all = preferenceTags(item)
  const shown = all.slice(0, limit)
  const hasMore = all.length > limit
  return { shown, hasMore }
}

function daysLine(item) {
  const p = item?.payload
  const days = Number(p?.days || 0)
  if (Number.isFinite(days) && days > 0) return `${days} 天`
  return '—'
}

function createdDateLine(item) {
  const raw = String(item?.addtime || '').trim()
  if (!raw) return '—'
  const d = new Date(raw)
  if (Number.isNaN(d.getTime())) return raw.slice(0, 10) || '—'
  const y = d.getFullYear()
  const m = String(d.getMonth() + 1).padStart(2, '0')
  const day = String(d.getDate()).padStart(2, '0')
  return `${y}-${m}-${day}`
}

function coverOf(item) {
  const p = item?.payload
  const rec = Array.isArray(p?.recommended) ? p.recommended : []
  const first = rec.find((x) => x && x.cover) || rec[0]
  return first?.cover || ''
}

async function createDemoData() {
  if (seeding.value) return
  seeding.value = true
  try {
    const [a, b] = await Promise.all([listRoutes('lvyouxianlu'), listRoutes('zuixinxianlu')])
    const all = [...(a || []), ...(b || [])].filter(Boolean)
    if (!all.length) {
      ElMessage.warning('暂无可用路线数据，无法生成测试行程')
      return
    }

    const pick = (i) => all[i % all.length]
    const demos = [
      {
        title: '北京 → 西安文化漫游',
        departure: '北京',
        destination: '西安',
        days: 3,
        preference: '博物馆 历史街区 美食',
        travelType: '文化深度游',
      },
      {
        title: '上海 → 杭州周末轻旅',
        departure: '上海',
        destination: '杭州',
        days: 2,
        preference: '轻松 亲子 湖景',
        travelType: '周末短途',
      },
      {
        title: '广州 → 桂林山水线',
        departure: '广州',
        destination: '桂林',
        days: 4,
        preference: '自然风光 摄影 徒步',
        travelType: '自然探索',
      },
    ]

    for (let i = 0; i < demos.length; i += 1) {
      const chosen = [pick(i * 2), pick(i * 2 + 1), pick(i * 2 + 2)].map(normalizeRoute).filter(Boolean)
      // eslint-disable-next-line no-await-in-loop
      await saveTrip({
        title: `${demos[i].title} · ${demos[i].days}天`,
        payload: {
          ...demos[i],
          people: 2,
          budget: 4500 + i * 1200,
          recommended: chosen,
        },
      })
    }

    const first = normalizeRoute(pick(0))
    if (first) {
      await toggleStoreup({
        tablename: first.tablename,
        refid: Number(first.id),
        name: first.name,
        picture: first.cover,
      })
    }

    await refreshTravel()
    ElMessage.success('已生成 3 条测试行程与 1 条收藏')
  } catch {
    ElMessage.error('测试数据生成失败，请稍后重试')
  } finally {
    seeding.value = false
  }
}

async function handleCopyId() {
  if (!userIdDisplay.value || userIdDisplay.value === '000000') {
    ElMessage.warning('暂无可复制的用户ID')
    return
  }
  try {
    await navigator.clipboard.writeText(userIdDisplay.value)
    ElMessage.success('用户ID已复制')
  } catch {
    ElMessage.error('复制失败，请手动复制')
  }
}

function handleGoRegister() {
  logout().finally(() => {
    router.replace({ name: 'login', query: { redirect: '/me' } })
  })
}


const isPwdOpen = ref(false)
const pwdForm = ref({ oldMima: '', newMima: '', newMima2: '' })
const isProfileOpen = ref(false)
const profileForm = ref({ xingming: '', xingbie: '', lianxidianhua: '', touxiang: '' })

function openPwd() {
  pwdForm.value = { oldMima: '', newMima: '', newMima2: '' }
  isPwdOpen.value = true
}

function openProfile() {
  profileForm.value = {
    xingming: String(user.value?.xingming || ''),
    xingbie: String(user.value?.xingbie || ''),
    lianxidianhua: String(user.value?.lianxidianhua || ''),
    touxiang: String(user.value?.touxiang || ''),
  }
  isProfileOpen.value = true
}

async function savePwd() {
  if (pwdForm.value.newMima !== pwdForm.value.newMima2) {
    ElMessage.warning('两次新密码不一致')
    return
  }
  try {
    await updatePasswordRemote(pwdForm.value.oldMima, pwdForm.value.newMima)
  } catch (err) {
    ElMessage.error(getErrorMessage(err, '原密码错误或新密码无效'))
    return
  }
  isPwdOpen.value = false
  ElMessage.success('密码已修改')
}

function resetPwdForm() {
  pwdForm.value = { oldMima: '', newMima: '', newMima2: '' }
}

async function saveProfile() {
  try {
    const { user: nextUser } = await updateProfileRemote({
      xingming: profileForm.value.xingming,
      xingbie: profileForm.value.xingbie,
      lianxidianhua: profileForm.value.lianxidianhua,
      touxiang: profileForm.value.touxiang,
    })
    user.value = nextUser || user.value
    isProfileOpen.value = false
    ElMessage.success('资料已保存')
  } catch (err) {
    ElMessage.error(getErrorMessage(err, '资料保存失败'))
  }
}

async function handleLogoutSession() {
  await logout()
  ElMessage.success('已退出登录')
  router.replace({ name: 'login', query: { redirect: '/me' } })
}

async function handleLogoutAccount() {
  const id = Number(user.value?.id ?? 0)
  if (id === 0) {
    ElMessage.warning('管理员账号不可注销')
    return
  }
  try {
    await ElMessageBox.confirm(
      '注销后将清空该账号在本地的订单、地址、收藏等数据，用户ID会回收，下次注册可复用该号码。是否继续？',
      '确认注销',
      { type: 'warning', confirmButtonText: '注销', cancelButtonText: '取消' }
    )
  } catch {
    return
  }
  await deleteUserAccount(id)
  ElMessage.success('账号已注销')
  router.replace({ name: 'login', query: { redirect: '/me' } })
}
</script>

<template>
  <section class="page">
    <div class="profilePage">
      <div class="cover"></div>
      <div class="identityCard">
    <div class="profileInfo">
      <div class="profileInfo__main">
        <el-avatar class="el-avatar--circle avatar avatar--inline" :size="234" :src="user?.touxiang" />
        <h2 class="name">{{ user?.xingming || '未命名用户' }}</h2>
        <p class="line">性别 {{ user?.xingbie || '—' }} ｜ 用户ID {{ userIdDisplay }}</p>
        <p class="line">电话 {{ user?.lianxidianhua || '—' }} ｜ 用户名 {{ user?.yonghuming || '—' }}</p>
        <p class="desc">欢迎回来，{{ user?.yonghuming || '用户' }}。在这里可以统一管理账号信息、密码与安全操作。</p>
        <div class="actionRow">
          <button class="pill pill--primary" type="button" @click="openProfile">编辑资料</button>
          <button class="pill" type="button" @click="openPwd">修改密码</button>
          <button class="pill" type="button" @click="handleCopyId">复制ID</button>
          <button class="pill pill--soft" type="button" @click="handleGoRegister">注册账号</button>
          <button class="pill pill--soft" type="button" @click="handleLogoutSession">退出登录</button>
          <button class="pill pill--danger" type="button" @click="handleLogoutAccount">注销账号</button>
        </div>
      </div>
    </div>
    </div>

    <div class="contentGrid">
      <div class="contentGrid__left">
      <article class="travelPane">
        <div class="travelPane__header">
          <div class="travelPane__menu">
            <button type="button" class="travelPane__tab" :class="{ 'is-active': travelTab === 'trip' }" @click="travelTab = 'trip'">
              我的旅行
            </button>
            <button
              type="button"
              class="travelPane__tab"
              :class="{ 'is-active': travelTab === 'storeup' }"
              @click="travelTab = 'storeup'"
            >
              我的收藏
            </button>
            <button type="button" class="travelPane__seed" :disabled="seeding" @click="createDemoData">
              {{ seeding ? '生成中…' : '创建测试数据' }}
            </button>
          </div>
        </div>
        <div v-if="!travelCards.length" class="travelPane__empty">暂无内容</div>
        <div v-else class="travelPane__grid">
          <article v-for="item in travelCards" :key="`${travelTab}-${item.id}`" class="card lynx-card lynx-card--glass">
            <div v-if="travelTab === 'trip'" class="card__media" :class="{ 'has-img': !!coverOf(item) }">
              <el-image v-if="coverOf(item)" class="card__img" :src="coverOf(item)" fit="cover" :alt="travelCardTitle(item)">
                <template #error>
                  <div class="card__ph" aria-hidden="true" />
                </template>
              </el-image>
              <div v-else class="card__ph" aria-hidden="true" />
            </div>
            <div v-else class="card__media">
              <div class="card__ph" aria-hidden="true" />
            </div>

            <div class="card__body">
              <div class="card__place">{{ travelTab === 'trip' ? placeLine(item) : travelCardTitle(item) }}</div>
              <div class="card__tags">
                <template v-if="travelTab === 'trip'">
                  <span v-for="tag in preferenceTagsView(item).shown" :key="`${item.id}-${tag}`" class="tag">{{ tag }}</span>
                  <span v-if="preferenceTagsView(item).hasMore" class="tag tag--muted">…</span>
                  <span v-if="!preferenceTagsView(item).shown.length" class="tag tag--muted">我的偏爱标签</span>
                </template>
                <template v-else>
                  <span class="tag">ref: {{ item.refid }}</span>
                </template>
              </div>
              <div class="card__dayRow">
                <div class="card__days">{{ travelTab === 'trip' ? daysLine(item) : '收藏项' }}</div>
                <div class="card__date">{{ createdDateLine(item) }}</div>
              </div>
            </div>
          </article>
        </div>
      </article>
      </div>

      <aside class="contentGrid__right leftRail">
        <article class="railCard">
          <span class="label">余额</span>
          <strong class="money">¥ {{ moneyText }}</strong>
        </article>
        <article class="railCard">
          <span class="label">账户信息</span>
          <div class="fact"><span>用户ID</span><strong>{{ userIdDisplay }}</strong></div>
          <div class="fact"><span>实名认证</span><strong>{{ realNameText }}</strong></div>
          <div class="fact"><span>邮箱状态</span><strong>{{ emailStatusText }}</strong></div>
        </article>
      </aside>
    </div>
    </div>

    <el-dialog v-model="isPwdOpen" title="修改密码" width="480px" @closed="resetPwdForm">
    <el-form label-position="top">
      <el-form-item label="当前密码">
        <el-input v-model="pwdForm.oldMima" type="password" show-password autocomplete="off" />
      </el-form-item>
      <el-form-item label="新密码">
        <el-input v-model="pwdForm.newMima" type="password" show-password autocomplete="new-password" />
      </el-form-item>
      <el-form-item label="确认新密码">
        <el-input v-model="pwdForm.newMima2" type="password" show-password autocomplete="new-password" />
      </el-form-item>
    </el-form>
    <template #footer>
      <el-button @click="isPwdOpen = false">取消</el-button>
      <el-button type="primary" @click="savePwd">保存</el-button>
    </template>
    </el-dialog>

    <el-dialog v-model="isProfileOpen" title="编辑资料" width="560px">
    <el-form label-position="top">
      <div class="dialogRow">
        <el-form-item label="姓名">
          <el-input v-model="profileForm.xingming" maxlength="64" />
        </el-form-item>
        <el-form-item label="性别">
          <el-select v-model="profileForm.xingbie" placeholder="请选择" clearable>
            <el-option label="男" value="男" />
            <el-option label="女" value="女" />
            <el-option label="保密" value="保密" />
          </el-select>
        </el-form-item>
      </div>
      <div class="dialogRow">
        <el-form-item label="电话">
          <el-input v-model="profileForm.lianxidianhua" maxlength="32" />
        </el-form-item>
        <el-form-item label="头像链接">
          <el-input v-model="profileForm.touxiang" maxlength="255" placeholder="https://..." />
        </el-form-item>
      </div>
    </el-form>
    <template #footer>
      <el-button @click="isProfileOpen = false">取消</el-button>
      <el-button type="primary" @click="saveProfile">保存修改</el-button>
    </template>
    </el-dialog>
  </section>
</template>

<style scoped lang="scss">
.page {
  display: block;
  margin: -24px -24px 0;
}

.profilePage {
  position: relative;
  overflow: visible;
  --cover-height: 220px;
  --avatar-size: 234px;
  --golden: 0.618;
  --hero-width: 75%;
  background: transparent;
  border: none;
  box-shadow: none;
}

.cover {
  height: var(--cover-height, 220px);
  width: 100%;
  margin: 0;
  border-radius: 0;
  background:
    linear-gradient(135deg, rgba(7, 89, 133, 0.48), rgba(15, 23, 42, 0.2)),
    url('../assets/background.png') center/cover no-repeat;
}

.identityCard {
  position: relative;
  width: 100%;
  margin: 0;
  padding: 0 0 24px;
  background: #fff;
  border: 1px solid rgba(148, 163, 184, 0.2);
  border-top: none;
  border-radius: 0;
}

.avatar {
  border: 5px solid #fff;
  box-shadow: 0 14px 36px rgba(15, 23, 42, 0.18);
  flex-shrink: 0;
}
.avatar--inline {
  width: var(--avatar-size, 234px) !important;
  height: var(--avatar-size, 234px) !important;
}

.profileInfo {
  width: 100%;
  margin: 0;
  margin-top: 0;
  background: transparent;
  border: 0;
  border-radius: 0;
  padding: 16px calc((100% - var(--hero-width, 75%)) / 2);
  display: grid;
  grid-template-columns: minmax(0, 1fr) auto;
  align-items: start;
  gap: 16px;
}
.profileInfo__main {
  position: relative;
  min-width: 0;
  width: 75%;
  max-width: 100%;
  box-sizing: border-box;
  min-height: 0;
  padding-left: calc(var(--avatar-size, 234px) + 32px);
  padding-top: 12px;
}

.profileInfo__main .avatar--inline {
  position: absolute;
  left: -10px;
  top: -115px;
  margin-right: 0;
}

.name {
  margin: 0 0 4px 0;
  font-size: 42px;
  line-height: 1;
  font-weight: 900;
  color: #0f172a;
}

.line {
  margin: 2px 0;
  font-size: 14px;
  color: #475569;
}

.desc {
  margin: 6px 0 0 0;
  font-size: 14px;
  color: #334155;
}

.actionRow {
  margin-top: 8px;
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}
.actionRow--minor {
  margin-top: 6px;
}

.pill {
  height: 34px;
  border-radius: 999px;
  border: 1px solid rgba(148, 163, 184, 0.36);
  background: #fff;
  color: #0f172a;
  padding: 0 14px;
  font-size: 14px;
  font-weight: 700;
  cursor: pointer;
  transition: 0.2s ease;
}

.pill:hover {
  transform: translateY(-1px);
  border-color: rgba(14, 165, 233, 0.4);
}

.pill--primary {
  background: #ea580c;
  border-color: #ea580c;
  color: #fff;
}

.pill--soft {
  background: #f0f9ff;
  color: #0c4a6e;
}

.pill--danger {
  border-color: rgba(220, 38, 38, 0.45);
  color: #991b1b;
}

.contentGrid {
  width: 75%;
  margin: 16px auto 0;
  margin-top: 16px;
  display: grid;
  grid-template-columns: 3fr 1fr;
  gap: 14px;
  align-items: start;
}

.contentGrid__left {
  grid-column: 1;
  min-width: 0;
}

.contentGrid__right {
  grid-column: 2;
}

.leftRail {
  align-self: start;
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.railCard {
  border-radius: 14px;
  border: 1px solid rgba(148, 163, 184, 0.24);
  background: #ffffff;
  padding: 14px;
}

.label {
  display: block;
  font-size: 14px;
  color: #64748b;
}

.money {
  margin-top: 4px;
  display: block;
  font-size: 40px;
  line-height: 1;
  color: #991b1b;
}

.hint {
  margin-top: 2px;
  display: block;
  color: #64748b;
  font-size: 12px;
}
.fact {
  margin-top: 8px;
  display: flex;
  justify-content: space-between;
  gap: 8px;
  font-size: 12px;
  color: #475569;
}
.fact strong {
  color: #0f172a;
  font-size: 13px;
}

.mainCard {
  border-radius: 14px;
  border: 1px solid rgba(148, 163, 184, 0.24);
  padding: 18px;
}

.travelPane {
  grid-column: 1;
  align-self: start;
}
.travelPane__header {
  height: 82px;
  margin-bottom: 10px;
  position: relative;
}
.travelPane__header::before {
  content: '';
  position: absolute;
  inset: 0;
  border-radius: 18px;
  border: 1px solid var(--lynx-border);
  background: radial-gradient(920px 320px at 20% 10%, rgba(249, 115, 22, 0.18), transparent 60%),
    radial-gradient(920px 320px at 82% 28%, rgba(56, 189, 248, 0.14), transparent 60%),
    rgba(255, 255, 255, 0.92);
  box-shadow: 0 1px 2px rgba(15, 23, 42, 0.04);
  pointer-events: none;
}
.travelPane__menu {
  position: absolute;
  top: 20px;
  left: 16px;
  display: flex;
  align-items: center;
  gap: 10px;
  z-index: 1;
}
.travelPane__tab {
  height: 34px;
  min-width: 108px;
  border-radius: 999px;
  border: 1px solid rgba(148, 163, 184, 0.4);
  background: rgba(255, 255, 255, 0.92);
  color: #334155;
  font-size: 13px;
  font-weight: 800;
  cursor: pointer;
}
.travelPane__tab.is-active {
  border-color: rgba(249, 115, 22, 0.42);
  color: #7c2d12;
  background: rgba(249, 115, 22, 0.14);
}
.travelPane__seed {
  margin-left: auto;
  height: 34px;
  padding: 0 12px;
  border-radius: 999px;
  border: 1px solid rgba(14, 165, 233, 0.32);
  background: rgba(255, 255, 255, 0.92);
  color: #0369a1;
  font-size: 12px;
  font-weight: 900;
  cursor: pointer;
}
.travelPane__seed:disabled {
  opacity: 0.6;
  cursor: not-allowed;
}
.travelPane__empty {
  min-height: 170px;
  display: grid;
  place-items: center;
  border-radius: 14px;
  border: 1px dashed rgba(148, 163, 184, 0.4);
  color: #64748b;
}
.travelPane__grid {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 12px;
}

.card {
  overflow: hidden;
  cursor: default;
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

.dialogRow {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 10px;
}

@media (max-width: 1200px) {
  .contentGrid {
    grid-template-columns: 1fr;
  }
  .leftRail {
    grid-template-columns: repeat(2, minmax(0, 1fr));
    display: grid;
  }
}
@media (max-width: 900px) {
  .profilePage {
    --cover-height: 180px;
    --avatar-size: 180px;
    --hero-width: 100%;
  }
  .profileInfo,
  .contentGrid {
    width: 100%;
  }
  .profileInfo {
    margin-top: 10px;
    border-radius: 16px;
    padding: 16px 14px;
    grid-template-columns: 1fr;
  }
  .profileInfo__main {
    width: 100%;
    min-height: auto;
    padding-left: 0;
    padding-top: 0;
  }
  .profileInfo__main .avatar--inline {
    position: static;
    margin-right: 0;
    margin-bottom: 10px;
  }
  .cover {
    height: var(--cover-height);
  }
  .profileInfo {
    flex-direction: column;
    align-items: flex-start;
  }
  .name {
    font-size: 32px;
  }
}
@media (max-width: 520px) {
  .page {
    margin: -12px -12px 0;
  }
  .profilePage {
    --avatar-size: 140px;
  }
  .identityCard {
    padding: 0 0 14px;
  }
  .moduleActions {
    grid-template-columns: 1fr;
  }
  .quickTabs {
    flex-direction: column;
  }
  .quickTabs__item {
    width: 100%;
  }
  .leftRail {
    grid-template-columns: 1fr;
  }
  .money {
    font-size: 32px;
  }
}
@media (max-width: 720px) {
  .dialogRow {
    grid-template-columns: 1fr;
  }
}
</style>

