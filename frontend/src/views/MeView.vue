<script setup>
import { computed, onBeforeUnmount, onMounted, ref } from 'vue'
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
import { formatUserIdDisplay, deleteTrips, deleteUserAccount, listTrips, listStoreup, toggleStoreup, updateTrip } from '../services/lynxDb'

const router = useRouter()
const user = ref(null)
const userIdDisplay = computed(() => formatUserIdDisplay(user.value?.id))
const tripItems = ref([])
const storeupItems = ref([])
const travelTab = ref('trip')
const avatarFileInput = ref(null)
const profileEditing = ref(false)
const introDraft = ref('')
const introStorageKey = computed(() => `lynxtrip.me.intro.${user.value?.id || 'guest'}`)
const profileForm = ref({ xingming: '', xingbie: '', lianxidianhua: '', touxiang: '' })

function defaultIntro(nextUser) {
  return `你好，我是${nextUser?.xingming || nextUser?.yonghuming || '旅行者'}，热爱探索每一段旅程。`
}

onMounted(async () => {
  if (!(await isLoggedInRemote())) {
    router.replace({ name: 'login', query: { redirect: '/me' } })
    return
  }
  const [nextUser, trips, storeups] = await Promise.all([fetchMe(true), listTrips(), listStoreup()])
  user.value = nextUser
  profileForm.value = {
    xingming: String(nextUser?.xingming || nextUser?.displayName || ''),
    xingbie: String(nextUser?.xingbie || nextUser?.gender || ''),
    lianxidianhua: String(nextUser?.lianxidianhua || nextUser?.phone || ''),
    touxiang: String(nextUser?.touxiang || nextUser?.avatarUrl || ''),
  }
  try {
    const savedIntro = localStorage.getItem(introStorageKey.value)
    introDraft.value = savedIntro || defaultIntro(nextUser)
  } catch {
    introDraft.value = defaultIntro(nextUser)
  }
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
const favoritedTripIds = computed(() =>
  orderedStoreups.value
    .filter((s) => String(s?.tableName || s?.tablename || '').trim() === 'trip_plans')
    .map((s) => Number(s?.refId ?? s?.refid ?? 0))
    .filter((x) => Number.isFinite(x) && x > 0),
)
const favoritedTrips = computed(() => {
  const byId = new Map(orderedTrips.value.map((t) => [Number(t?.id), t]))
  const out = []
  for (const id of favoritedTripIds.value) {
    const hit = byId.get(id)
    if (hit) out.push(hit)
  }
  return out
})
const travelCards = computed(() => (travelTab.value === 'trip' ? orderedTrips.value : favoritedTrips.value))

const selectionMode = ref(false)
const selectedTripIds = ref(new Set())
const ctxMenu = ref({ open: false, x: 0, y: 0, tripId: null })

function toggleSelectionMode() {
  selectionMode.value = !selectionMode.value
  selectedTripIds.value = new Set()
  ctxMenu.value = { open: false, x: 0, y: 0, tripId: null }
}

function isTripSelected(id) {
  return selectedTripIds.value.has(Number(id))
}

function toggleTripSelected(id) {
  const key = Number(id)
  const next = new Set(selectedTripIds.value)
  if (next.has(key)) next.delete(key)
  else next.add(key)
  selectedTripIds.value = next
}

async function refreshTripsAndStoreups() {
  const [trips, storeups] = await Promise.all([listTrips(), listStoreup()])
  tripItems.value = Array.isArray(trips) ? trips : []
  storeupItems.value = Array.isArray(storeups) ? storeups : []
}

async function doDeleteTrips(ids) {
  const list = (Array.isArray(ids) ? ids : [ids]).map((x) => Number(x)).filter((x) => Number.isFinite(x) && x > 0)
  if (!list.length) return
  try {
    await ElMessageBox.confirm(`确定删除选中的 ${list.length} 条行程吗？删除后不可恢复。`, '删除行程', {
      type: 'warning',
      confirmButtonText: '删除',
      cancelButtonText: '取消',
    })
  } catch {
    return
  }
  await deleteTrips(list)
  ElMessage.success('已删除')
  await refreshTripsAndStoreups()
  selectedTripIds.value = new Set()
  selectionMode.value = false
  ctxMenu.value = { open: false, x: 0, y: 0, tripId: null }
}

function openTripContextMenu(e, tripId) {
  if (travelTab.value !== 'trip') return
  e.preventDefault()
  if (selectionMode.value) return
  ctxMenu.value = { open: true, x: e.clientX, y: e.clientY, tripId: Number(tripId) }
}

function closeTripContextMenu() {
  ctxMenu.value = { open: false, x: 0, y: 0, tripId: null }
}

function openTripWorkspaceById(tripId) {
  const id = Number(tripId)
  if (!Number.isFinite(id) || id <= 0) {
    ElMessage.warning('行程数据异常，无法打开')
    return
  }
  router.push({
    name: 'my-itinerary-workspace',
    query: { tripId: String(id) },
  })
}

function openWorkspaceFromStoreup(item) {
  // 行程收藏：直接按 refId 打开工作台
  const tableName = String(item?.tableName || item?.tablename || '').trim()
  const refId = Number(item?.refId ?? item?.refid ?? 0)
  if (tableName === 'trip_plans' && Number.isFinite(refId) && refId > 0) {
    openTripWorkspaceById(refId)
    return
  }
  ElMessage.warning('该收藏不是行程数据，暂不支持打开工作台')
}

function openTravelCard(item) {
  if (travelTab.value === 'trip') {
    if (selectionMode.value) {
      toggleTripSelected(item.id)
      return
    }
    openTripWorkspaceById(item?.id)
    return
  }
  // 收藏页不支持“批量管理”，但仍做一致性保护
  if (selectionMode.value) {
    return
  }
  openTripWorkspaceById(item?.id)
}

function isTripFavorited(item) {
  const tripId = Number(item?.id || 0)
  if (!Number.isFinite(tripId) || tripId <= 0) return false
  return storeupItems.value.some((s) => {
    const tableName = String(s?.tableName || s?.tablename || '').trim()
    const refId = Number(s?.refId ?? s?.refid ?? 0)
    return tableName === 'trip_plans' && refId === tripId
  })
}

async function toggleTripFavorite(item) {
  const tripId = Number(item?.id || 0)
  if (!Number.isFinite(tripId) || tripId <= 0) {
    ElMessage.warning('行程数据异常，无法收藏')
    return
  }
  try {
    const resp = await toggleStoreup({
      tablename: 'trip_plans',
      refid: tripId,
      name: String(item?.title || travelCardTitle(item) || '我的行程'),
      picture: String(coverOf(item) || ''),
    })
    const fav = !!resp?.fav
    const rows = await listStoreup()
    storeupItems.value = Array.isArray(rows) ? rows : []
    ElMessage.success(fav ? '已收藏该行程' : '已取消收藏')
  } catch {
    ElMessage.error('收藏操作失败，请稍后重试')
  }
}

function keyPlaceTags(item) {
  const p = item?.payload || {}
  const list = Array.isArray(p?.key_places) ? p.key_places : Array.isArray(p?.keyPlaces) ? p.keyPlaces : []
  return (Array.isArray(list) ? list : [])
    .map((x) => String(x || '').trim())
    .filter(Boolean)
}

function keyPlaceTagsView(item, limit = 3) {
  const all = keyPlaceTags(item)
  const shown = all.slice(0, limit)
  const hasMore = all.length > limit
  return { shown, hasMore }
}

async function editKeyPlaceTags(item) {
  if (!item?.id) return
  const current = keyPlaceTags(item)
  const { value } = await ElMessageBox.prompt('用逗号分隔关键地点标签（例如：博物馆, 中山公园）', '编辑关键地点', {
    inputValue: current.join(', '),
    confirmButtonText: '保存',
    cancelButtonText: '取消',
    inputPlaceholder: '博物馆, 中山公园',
    closeOnClickModal: false,
  }).catch(() => ({ value: null }))

  if (value == null) return
  const next = String(value || '')
    .split(/[，,、/|；;\n\r\t]+/)
    .map((x) => x.trim())
    .filter(Boolean)
    .slice(0, 12)

  const payload = item?.payload && typeof item.payload === 'object' ? { ...item.payload } : {}
  payload.key_places = next
  try {
    const resp = await updateTrip({ id: item.id, payload })
    if (!resp?.ok) {
      ElMessage.error('保存失败，请稍后重试')
      return
    }
    item.payload = payload
    ElMessage.success('已更新关键地点')
  } catch {
    ElMessage.error('保存失败，请检查登录状态')
  }
}

function handleGlobalPointerDown(ev) {
  if (!ctxMenu.value.open) return
  const target = ev?.target
  if (target && typeof target.closest === 'function' && target.closest('.ctxMenu')) {
    return
  }
  closeTripContextMenu()
}

function handleGlobalKeydown(ev) {
  if (ev.key === 'Escape') {
    closeTripContextMenu()
    selectionMode.value = false
    selectedTripIds.value = new Set()
  }
}

onMounted(() => {
  window.addEventListener('pointerdown', handleGlobalPointerDown, { capture: true })
  window.addEventListener('keydown', handleGlobalKeydown)
})

onBeforeUnmount(() => {
  window.removeEventListener('pointerdown', handleGlobalPointerDown, { capture: true })
  window.removeEventListener('keydown', handleGlobalKeydown)
})

function travelCardTitle(item) {
  return String(item?.title || item?.name || item?.goodname || '未命名记录')
}

function travelCardMeta(item) {
  if (travelTab.value === 'trip') {
    const p = item?.payload || {}
    const days = Number(p?.total_days || p?.days || 0)
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
  // 新 AI 行程：从 itinerary 里推断城市（首尾）
  if (!places.length && Array.isArray(p.itinerary) && p.itinerary.length) {
    const firstCity = String(p.itinerary[0]?.city || '').trim()
    const lastCity = String(p.itinerary[p.itinerary.length - 1]?.city || '').trim()
    if (firstCity) places.push(firstCity)
    if (lastCity && lastCity !== firstCity) places.push(lastCity)
  }
  if (!places.length) places.push(...parsePlacesFromTitle(item?.title || item?.name || ''))
  const unique = [...new Set(places)].slice(0, 3)
  if (!unique.length) return travelCardTitle(item)
  if (unique.length === 1) return `${unique[0]} 🇨🇳`
  return travelCardTitle(item)
}

function preferenceTags(item) {
  const p = item?.payload || {}
  const tags = []
  // 新 AI 行程 schema：preferences 为数组
  if (Array.isArray(p.preferences)) {
    p.preferences
      .map((x) => String(x || '').trim())
      .filter(Boolean)
      .forEach((x) => tags.push(x))
  }
  // 旧 schema：preference 为文本
  const prefText = String(p.preference || '').trim()
  if (prefText) {
    prefText
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
  const days = Number(p?.total_days || p?.days || 0)
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

const isPwdOpen = ref(false)
const pwdForm = ref({ oldMima: '', newMima: '', newMima2: '' })

function openPwd() {
  pwdForm.value = { oldMima: '', newMima: '', newMima2: '' }
  isPwdOpen.value = true
}

function startProfileEdit() {
  profileForm.value = {
    xingming: String(user.value?.xingming || user.value?.displayName || ''),
    xingbie: String(user.value?.xingbie || user.value?.gender || ''),
    lianxidianhua: String(user.value?.lianxidianhua || user.value?.phone || ''),
    touxiang: String(user.value?.touxiang || user.value?.avatarUrl || ''),
  }
  profileEditing.value = true
}

function cancelProfileEdit() {
  profileEditing.value = false
  try {
    introDraft.value = localStorage.getItem(introStorageKey.value) || defaultIntro(user.value)
  } catch {
    introDraft.value = defaultIntro(user.value)
  }
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
    const keepGender = String(user.value?.xingbie || user.value?.gender || '').trim()
    const keepPhone = String(user.value?.lianxidianhua || user.value?.phone || '').trim()
    const nextGender = String(profileForm.value.xingbie || '').trim() || keepGender
    const nextPhone = String(profileForm.value.lianxidianhua || '').trim() || keepPhone
    const { user: nextUser } = await updateProfileRemote({
      xingming: profileForm.value.xingming,
      xingbie: nextGender,
      lianxidianhua: nextPhone,
      touxiang: profileForm.value.touxiang,
    })
    user.value = nextUser || user.value
    try {
      localStorage.setItem(introStorageKey.value, String(introDraft.value || '').trim())
    } catch {
      // ignore storage error and still treat as saved profile
    }
    profileEditing.value = false
    ElMessage.success('资料已保存')
  } catch (err) {
    ElMessage.error(getErrorMessage(err, '资料保存失败'))
  }
}

async function quickChangeAvatar() {
  const el = avatarFileInput.value
  if (!el) return
  el.click()
}

async function handleAvatarFileChange(e) {
  const file = e?.target?.files?.[0]
  if (!file) return
  if (!String(file.type || '').startsWith('image/')) {
    ElMessage.warning('请选择图片文件')
    e.target.value = ''
    return
  }
  if (file.size > 3 * 1024 * 1024) {
    ElMessage.warning('图片过大（建议小于 3MB）')
    e.target.value = ''
    return
  }
  const dataUrl = await new Promise((resolve, reject) => {
    const reader = new FileReader()
    reader.onload = () => resolve(String(reader.result || ''))
    reader.onerror = () => reject(new Error('read_failed'))
    reader.readAsDataURL(file)
  }).catch(() => '')

  e.target.value = ''
  if (!dataUrl) {
    ElMessage.error('读取图片失败，请重试')
    return
  }
  try {
    const { user: nextUser } = await updateProfileRemote({ touxiang: dataUrl })
    user.value = nextUser || { ...(user.value || {}), touxiang: dataUrl }
    ElMessage.success('头像已更新')
  } catch (err) {
    ElMessage.error(getErrorMessage(err, '头像更新失败'))
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
        <button class="avatarBtn avatar--inline" type="button" @click="quickChangeAvatar" aria-label="更换头像">
          <el-avatar class="el-avatar--circle avatar" :size="234" :src="user?.touxiang" />
          <span class="avatarOverlay" aria-hidden="true">
            <svg viewBox="0 0 24 24" class="avatarCam" aria-hidden="true">
              <path
                fill="currentColor"
                d="M9.5 4.5h5l1.5 2H19a3 3 0 0 1 3 3v7a3 3 0 0 1-3 3H5a3 3 0 0 1-3-3v-7a3 3 0 0 1 3-3h3L9.5 4.5Zm2.5 13a4 4 0 1 0 0-8a4 4 0 0 0 0 8Zm0-2a2 2 0 1 1 0-4a2 2 0 0 1 0 4Z"
              />
            </svg>
          </span>
        </button>
        <input ref="avatarFileInput" class="avatarFile" type="file" accept="image/*" @change="handleAvatarFileChange" />
        <h2 v-if="!profileEditing" class="name">{{ user?.xingming || user?.yonghuming || '未命名用户' }}</h2>
        <el-input
          v-else
          v-model="profileForm.xingming"
          class="nameInput"
          maxlength="64"
          placeholder="请输入姓名"
        />
        <div class="introBox">
          <textarea
            v-model="introDraft"
            class="introBox__textarea"
            :readonly="!profileEditing"
            maxlength="140"
            placeholder="介绍一下你自己和旅行偏好..."
          />
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
            <button
              v-if="travelTab === 'trip' && travelCards.length"
              type="button"
              class="travelPane__tab travelPane__tab--ghost"
              @click="toggleSelectionMode"
            >
              {{ selectionMode ? `取消管理（已选 ${selectedTripIds.size}）` : '批量管理' }}
            </button>
            <button
              v-if="selectionMode && selectedTripIds.size"
              type="button"
              class="travelPane__tab travelPane__tab--danger"
              @click="doDeleteTrips(Array.from(selectedTripIds))"
            >
              删除选中（{{ selectedTripIds.size }}）
            </button>
          </div>
        </div>
        <div v-if="!travelCards.length" class="travelPane__empty">暂无内容</div>
        <div v-else class="travelPane__grid">
          <article
            v-for="item in travelCards"
            :key="`${travelTab}-${item.id}`"
            class="card lynx-card lynx-card--glass"
            :class="{
              'is-selectable': selectionMode && travelTab === 'trip',
              'is-selected': selectionMode && travelTab === 'trip' && isTripSelected(item.id),
              'is-clickable': !(selectionMode && travelTab === 'trip'),
            }"
            @contextmenu="(e) => openTripContextMenu(e, item.id)"
            @click="openTravelCard(item)"
          >
            <button
              v-if="item?.payload"
              type="button"
              class="card__favBtn"
              :aria-pressed="isTripFavorited(item)"
              :title="isTripFavorited(item) ? '取消收藏' : '收藏行程'"
              @click.stop="toggleTripFavorite(item)"
            >
              <span class="card__favStar" :class="{ 'is-on': isTripFavorited(item) }" aria-hidden="true">⭐</span>
            </button>
            <button
              v-if="selectionMode && travelTab === 'trip'"
              type="button"
              class="card__check"
              :class="{ 'is-on': isTripSelected(item.id) }"
              @click.stop="toggleTripSelected(item.id)"
              aria-label="选择该行程"
            />
            <div class="card__media" :class="{ 'has-img': !!coverOf(item) }">
              <el-image v-if="coverOf(item)" class="card__img" :src="coverOf(item)" fit="cover" :alt="travelCardTitle(item)">
                <template #error>
                  <div class="card__ph" aria-hidden="true" />
                </template>
              </el-image>
              <div v-else class="card__ph" aria-hidden="true" />
            </div>

            <div class="card__body">
              <div class="card__place">{{ placeLine(item) }}</div>
              <div class="card__tags">
                <span v-for="tag in keyPlaceTagsView(item).shown" :key="`${item.id}-${tag}`" class="tag">{{ tag }}</span>
                <span v-if="keyPlaceTagsView(item).hasMore" class="tag tag--muted">…</span>
                <span v-if="!keyPlaceTagsView(item).shown.length" class="tag tag--muted">关键地点</span>
                <button class="tagEditBtn" type="button" title="编辑关键地点" @click.stop="editKeyPlaceTags(item)">✎</button>
              </div>
              <div class="card__dayRow">
                <div class="card__days">{{ daysLine(item) }}</div>
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
        <article class="railCard railCard--info">
          <div class="railHead">
            <span class="label">账户信息</span>
            <button class="railEditBtn" type="button" @click="profileEditing ? cancelProfileEdit() : startProfileEdit()">
              {{ profileEditing ? '取消编辑' : '编辑资料' }}
            </button>
          </div>
          <div class="railInfoBlock">
            <div class="railInfoItem">
              <span class="railInfoKey">用户名</span>
              <strong class="railInfoVal">{{ user?.yonghuming || '—' }}</strong>
            </div>
            <div class="railInfoItem">
              <span class="railInfoKey">用户ID</span>
              <strong class="railInfoVal railMono">{{ userIdDisplay }}</strong>
            </div>
            <div class="railInfoItem">
              <span class="railInfoKey">电话</span>
              <strong v-if="!profileEditing" class="railInfoVal railMono">{{ user?.lianxidianhua || '—' }}</strong>
              <el-input
                v-else
                v-model="profileForm.lianxidianhua"
                class="railInput"
                maxlength="32"
                placeholder="请输入电话"
              />
            </div>
            <div class="railInfoItem">
              <span class="railInfoKey">性别</span>
              <strong v-if="!profileEditing" class="railInfoVal">{{ user?.xingbie || '—' }}</strong>
              <el-select v-else v-model="profileForm.xingbie" class="railInput" placeholder="请选择" clearable>
                <el-option label="男" value="男" />
                <el-option label="女" value="女" />
                <el-option label="保密" value="保密" />
              </el-select>
            </div>
            <div class="railInfoItem">
              <span class="railInfoKey">实名认证</span>
              <strong class="railInfoVal">{{ realNameText }}</strong>
            </div>
            <div class="railInfoItem">
              <span class="railInfoKey">邮箱状态</span>
              <strong class="railInfoVal">{{ emailStatusText }}</strong>
            </div>
          </div>
          <div class="railActionStrip">
            <button v-if="profileEditing" class="pill pill--primary railActionBtn" type="button" @click="saveProfile">保存资料</button>
            <button class="pill railActionBtn" type="button" @click="openPwd">修改密码</button>
            <button class="pill pill--soft railActionBtn" type="button" @click="handleLogoutSession">退出登录</button>
            <button class="pill pill--danger railActionBtn" type="button" @click="handleLogoutAccount">注销账号</button>
          </div>
        </article>
      </aside>
    </div>
    </div>

    <div
      v-if="ctxMenu.open"
      class="ctxMenu"
      :style="{ left: `${ctxMenu.x}px`, top: `${ctxMenu.y}px` }"
      role="menu"
    >
      <button class="ctxMenu__item ctxMenu__item--danger" type="button" @click="doDeleteTrips([ctxMenu.tripId])">删除行程</button>
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
  margin-right: 0;
}

.avatarBtn {
  position: absolute;
  left: -10px;
  top: -115px;
  padding: 0;
  border: 0;
  background: transparent;
  cursor: pointer;
}
.avatarOverlay {
  position: absolute;
  inset: 0;
  border-radius: 999px;
  display: grid;
  place-items: center;
  background: rgba(2, 6, 23, 0.5);
  opacity: 0;
  transition: opacity 160ms ease;
}
.avatarCam {
  width: 44px;
  height: 44px;
  color: rgba(255, 255, 255, 0.95);
  filter: drop-shadow(0 10px 18px rgba(2, 6, 23, 0.22));
}
.avatarBtn:hover .avatarOverlay,
.avatarBtn:focus-visible .avatarOverlay {
  opacity: 1;
}

.avatarFile {
  position: absolute;
  width: 1px;
  height: 1px;
  padding: 0;
  margin: -1px;
  overflow: hidden;
  clip: rect(0, 0, 0, 0);
  white-space: nowrap;
  border: 0;
}

.name {
  margin: 0 0 4px 0;
  font-size: 42px;
  line-height: 1;
  font-weight: 900;
  color: #0f172a;
}

.nameInput {
  max-width: 360px;
  margin-bottom: 6px;
}
.nameInput :deep(.el-input__wrapper) {
  border-radius: 12px;
}
.introBox {
  margin-top: 8px;
}
.introBox__textarea {
  width: 90%;
  max-width: 100%;
  min-height: 72px;
  resize: vertical;
  border: 1px solid rgba(148, 163, 184, 0.3);
  border-radius: 12px;
  background: #fff;
  color: #334155;
  font-size: 14px;
  line-height: 1.6;
  padding: 10px 12px;
}
.introBox__textarea[readonly] {
  background: rgba(248, 250, 252, 0.85);
}

.railInfoBlock {
  margin-top: 10px;
  display: grid;
  gap: 10px;
  padding: 12px;
  border-radius: 14px;
  border: 1px solid rgba(148, 163, 184, 0.2);
  background: rgba(248, 250, 252, 0.82);
}
.railInfoItem {
  display: flex;
  justify-content: space-between;
  align-items: baseline;
  gap: 12px;
}
.railInfoKey {
  font-size: 14px;
  font-weight: 800;
  color: #475569;
  white-space: nowrap;
}
.railInfoVal {
  font-size: 16px;
  font-weight: 900;
  color: #0f172a;
  min-width: 0;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
.railMono {
  font-variant-numeric: tabular-nums;
}

.railCard--info {
  padding: 16px;
}
.railCard--info .label {
  color: #0f172a;
  font-weight: 900;
  font-size: 16px;
}
.railHead {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 8px;
}
.railEditBtn {
  border: none;
  background: transparent;
  color: #0284c7;
  font-size: 12px;
  font-weight: 700;
  cursor: pointer;
  padding: 0;
}
.railInput {
  width: 150px;
}
.railInput :deep(.el-input__wrapper),
.railInput :deep(.el-select__wrapper) {
  min-height: 32px;
  border-radius: 8px;
}
.railActionStrip {
  margin-top: 12px;
  display: flex;
  flex-wrap: wrap;
  gap: 6px;
  padding-top: 10px;
  border-top: 1px solid rgba(148, 163, 184, 0.2);
}
.railActionBtn {
  flex: 1 1 100%;
  min-width: 0;
  padding: 0 8px;
  font-size: 12px;
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
  right: 16px;
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
.travelPane__tab--ghost {
  min-width: 96px;
  margin-left: auto;
  border-color: rgba(14, 165, 233, 0.28);
  color: #0369a1;
}
.travelPane__tab--danger {
  min-width: 124px;
  border-color: rgba(220, 38, 38, 0.35);
  color: #991b1b;
  background: rgba(220, 38, 38, 0.08);
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
  position: relative;
  border-radius: 16px;
  border: 1px solid rgba(15, 23, 42, 0.08);
  background: rgba(255, 255, 255, 0.92);
  transition: transform 180ms ease, box-shadow 180ms ease, border-color 180ms ease, background 180ms ease;
}
.card.is-selectable {
  cursor: pointer;
}
.card.is-clickable {
  cursor: pointer;
}
.card.is-clickable:hover {
  transform: translateY(-1px);
  border-color: rgba(249, 115, 22, 0.22);
  box-shadow: 0 10px 28px rgba(15, 23, 42, 0.07);
}
.card.is-selected {
  border-color: rgba(14, 165, 233, 0.5);
  box-shadow: 0 0 0 3px rgba(14, 165, 233, 0.14);
}
.card__favBtn {
  position: absolute;
  top: 14px;
  left: 14px;
  z-index: 3;
  width: auto;
  height: auto;
  border: 0;
  background: transparent;
  display: grid;
  place-items: center;
  cursor: pointer;
  padding: 0;
  transition: transform 160ms ease;
}
.card__favBtn:hover {
  transform: translateY(-1px) scale(1.05);
}
.card__favBtn:focus-visible {
  outline: 3px solid rgba(249, 115, 22, 0.22);
  outline-offset: 2px;
}
.card__favStar {
  font-size: 21px;
  line-height: 1;
  display: inline-block;
  transform: rotate(-8deg);
  filter: grayscale(1) brightness(1.15);
  opacity: 0.7;
  transition: transform 160ms ease, filter 160ms ease, opacity 160ms ease;
}
.card__favStar.is-on {
  filter: none;
  opacity: 1;
  transform: rotate(-8deg) scale(1.05);
}
.card__check {
  position: absolute;
  z-index: 2;
  top: 10px;
  right: 10px;
  width: 26px;
  height: 26px;
  border-radius: 999px;
  border: 2px solid rgba(255, 255, 255, 0.9);
  background: rgba(15, 23, 42, 0.22);
  cursor: pointer;
}
.card__check.is-on {
  background: rgba(14, 165, 233, 0.95);
}
.card__check.is-on::after {
  content: '';
  position: absolute;
  inset: 0;
  margin: auto;
  width: 10px;
  height: 6px;
  border-left: 3px solid #fff;
  border-bottom: 3px solid #fff;
  transform: rotate(-45deg) translate(1px, -1px);
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

.ctxMenu {
  position: fixed;
  z-index: 9999;
  min-width: 140px;
  padding: 6px;
  border-radius: 12px;
  border: 1px solid rgba(148, 163, 184, 0.26);
  background: rgba(255, 255, 255, 0.98);
  box-shadow: 0 18px 44px rgba(15, 23, 42, 0.16);
  backdrop-filter: blur(10px);
}
.ctxMenu__item {
  width: 100%;
  height: 34px;
  border: none;
  border-radius: 10px;
  background: transparent;
  text-align: left;
  padding: 0 10px;
  font-size: 13px;
  font-weight: 800;
  cursor: pointer;
}
.ctxMenu__item:hover {
  background: rgba(15, 23, 42, 0.06);
}
.ctxMenu__item--danger {
  color: #991b1b;
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
  align-items: center;
  min-height: 26px;
}
.tagEditBtn {
  width: 26px;
  height: 26px;
  border-radius: 999px;
  border: 1px solid rgba(15, 23, 42, 0.1);
  background: rgba(255, 255, 255, 0.92);
  color: #64748b;
  font-size: 12px;
  font-weight: 900;
  display: grid;
  place-items: center;
  cursor: pointer;
  transition: border-color 160ms ease, box-shadow 160ms ease, transform 160ms ease, color 160ms ease, background 160ms ease;
}
.tagEditBtn:hover {
  transform: translateY(-1px);
  border-color: rgba(249, 115, 22, 0.26);
  box-shadow: 0 10px 22px rgba(15, 23, 42, 0.08);
  color: #0f172a;
  background: #fff;
}
.tagEditBtn:focus-visible {
  outline: 3px solid rgba(249, 115, 22, 0.2);
  outline-offset: 2px;
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
  .introBox__textarea {
    width: 100%;
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

