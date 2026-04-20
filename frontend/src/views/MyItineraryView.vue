<script setup>
import { computed, nextTick, onMounted, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import axios from 'axios'
import { getCurrentUserId, getUserById, listRoutes, listTrips, saveTrip, upsertCartItem } from '../services/lynxDb'
import heroBg from '../assets/background.png'

const route = useRoute()
const router = useRouter()

const form = ref({
  departure: '',
  destination: '',
  days: 3,
  budgetText: '5000',
  people: 1,
  plannerInput: '',
  preference: '',
})
const activeTab = ref('activities') // activities | map | edit | settings
const result = ref(null)
const citiesChain = ref([])
const openDays = ref(new Set([1]))
const editableRows = ref([])
const privacy = ref('public')

const BAIDU_MAP_AK = import.meta.env.VITE_BAIDU_MAP_AK || ''
const mapContainerRef = ref(null)
const mapReady = ref(false)
const mapError = ref('')
const mapLoading = ref(false)
const bmapGlobal = ref(false)
const mapDebug = computed(() => {
  const host = typeof window !== 'undefined' ? window.location.host : '(ssr)'
  const hasAk = !!BAIDU_MAP_AK
  const hasBMap = bmapGlobal.value
  return `host=${host} | ak=${hasAk ? 'ok' : 'missing'} | BMapGL=${hasBMap ? 'yes' : 'no'}`
})
let mapInstance = null
let baiduMapLoader = null
let baiduMapLoaderStartedAt = 0
const lastTab = ref(activeTab.value)

const me = computed(() => getUserById(getCurrentUserId()))
const allRoutes = computed(() => [...listRoutes('lvyouxianlu'), ...listRoutes('zuixinxianlu')])

const budget = computed(() => {
  const m = String(form.value.budgetText || '').match(/\d+/)
  return m ? Number(m[0]) : 0
})

const headerTitle = computed(() => result.value?.title || `${form.value.departure || '出发地'} → ${form.value.destination || '目的地'}`)
const headerDays = computed(() => Number(result.value?.days || form.value.days || 1))

const itineraryDays = computed(() => {
  if (!result.value) return []
  const totalDays = Math.max(1, Number(result.value.days || 1))
  const slots = ['上午', '中午', '下午', '傍晚']
  return Array.from({ length: totalDays }, (_, i) => {
    const dayNo = i + 1
    const activities = (result.value.recommended || [])
      .filter((_, idx) => idx % totalDays === i)
      .map((item, idx) => ({
        ...item,
        slot: slots[idx % slots.length],
        idKey: `${item.id}-${idx}-${dayNo}`,
      }))
    const location = activities[0]?.to || result.value.destination || '中国'
    return { dayNo, title: `天 ${dayNo}`, location, activities }
  })
})

const cityRouteText = computed(() => {
  if (citiesChain.value.length) return citiesChain.value.join(' → ')
  return `${form.value.departure || '出发地'} → ${form.value.destination || '目的地'}`
})

function loadBaiduMapSdk() {
  if (typeof window === 'undefined') {
    return Promise.reject(new Error('当前环境不支持地图加载'))
  }
  if (window.BMapGL) {
    bmapGlobal.value = true
    return Promise.resolve(window.BMapGL)
  }
  if (baiduMapLoader) {
    // 兜底：如果之前一次加载卡死（HMR 期间常见），超过阈值就重置重试
    if (baiduMapLoaderStartedAt && Date.now() - baiduMapLoaderStartedAt > 15000) {
      baiduMapLoader = null
      baiduMapLoaderStartedAt = 0
    } else {
      return baiduMapLoader
    }
  }
  if (!BAIDU_MAP_AK) return Promise.reject(new Error('未配置百度地图 AK（VITE_BAIDU_MAP_AK）'))

  baiduMapLoader = new Promise((resolve, reject) => {
    baiduMapLoaderStartedAt = Date.now()
    const timeoutMs = 20000
    let done = false
    // 重要：不要加载 api.map.baidu.com/api 这层包装（内部 document.write 在 SPA 动态加载下常失效）
    // 直接加载 getscript，并补上 bmap.css
    const cssId = 'lynxtrip-baidu-map-css'
    if (!document.getElementById(cssId)) {
      const link = document.createElement('link')
      link.id = cssId
      link.rel = 'stylesheet'
      link.type = 'text/css'
      link.href = 'https://api.map.baidu.com/res/webgl/10/bmap.css'
      document.head.appendChild(link)
    }

    const script = document.createElement('script')
    script.dataset.lynxtrip = 'baidu-map-sdk'
    script.src = `https://api.map.baidu.com/getscript?type=webgl&v=1.0&ak=${encodeURIComponent(BAIDU_MAP_AK)}&services=`
    script.async = true
    const timer = window.setTimeout(() => {
      if (done) return
      done = true
      try {
        script.remove()
      } catch {
        // ignore
      }
      reject(new Error('百度地图 SDK 加载超时（请检查网络/AK/浏览器控制台报错）'))
    }, timeoutMs)

    script.onload = () => {
      // onload 不保证 BMapGL 已就绪；轮询等待一小段时间
      const start = Date.now()
      const tick = () => {
        if (done) return
        if (window.BMapGL) {
          done = true
          window.clearTimeout(timer)
          bmapGlobal.value = true
          resolve(window.BMapGL)
          return
        }
        if (Date.now() - start > 10000) {
          done = true
          window.clearTimeout(timer)
          reject(new Error('百度地图 SDK 已加载但未初始化（BMapGL 仍不存在，通常是 Referer 白名单或脚本被拦截）'))
          return
        }
        window.setTimeout(tick, 50)
      }
      tick()
    }
    script.onerror = () => {
      if (done) return
      done = true
      window.clearTimeout(timer)
      reject(new Error('百度地图脚本请求失败（可能被网络/安全策略拦截）'))
    }
    document.head.appendChild(script)
  }).catch((err) => {
    baiduMapLoader = null
    baiduMapLoaderStartedAt = 0
    throw err
  })

  return baiduMapLoader
}

async function suggestPoint(name) {
  const q = String(name || '').trim()
  if (!q) return null
  try {
    const { data } = await axios.get('/api/maps/suggest', {
      params: { q, region: '全国' },
      timeout: 8000,
    })
    const list = Array.isArray(data?.data) ? data.data : []
    const hit = list.find((x) => x && x.lng != null && x.lat != null) || null
    if (!hit) return null
    return { name: hit.name || q, lng: Number(hit.lng), lat: Number(hit.lat) }
  } catch {
    return null
  }
}

async function buildMapPoints() {
  // 优先：多城市串联；否则用每天 location；最后用目的地
  const rawPlaces = []
  if (citiesChain.value.length) rawPlaces.push(...citiesChain.value)
  if (itineraryDays.value.length) rawPlaces.push(...itineraryDays.value.map((d) => d.location))
  if (form.value.destination) rawPlaces.push(form.value.destination)

  const places = [...new Set(rawPlaces.map((x) => String(x || '').trim()).filter(Boolean))].slice(0, 10)
  if (!places.length) return []

  const points = []
  for (const p of places) {
    // eslint-disable-next-line no-await-in-loop
    const hit = await suggestPoint(p)
    if (hit) points.push(hit)
  }
  return points
}

function renderRouteOnMap(BMapGL, points) {
  if (!mapInstance) return
  mapInstance.clearOverlays()
  if (!points.length) return

  const bPoints = points.map((p) => new BMapGL.Point(p.lng, p.lat))
  bPoints.forEach((pt, idx) => {
    const marker = new BMapGL.Marker(pt)
    mapInstance.addOverlay(marker)
    const label = new BMapGL.Label(`${idx + 1}. ${points[idx].name || '地点'}`, {
      position: pt,
      offset: new BMapGL.Size(14, -10),
    })
    label.setStyle({
      color: '#0f172a',
      borderColor: 'rgba(15,23,42,0.10)',
      borderRadius: '10px',
      padding: '2px 8px',
      backgroundColor: 'rgba(255,255,255,0.92)',
      fontSize: '12px',
      fontWeight: '700',
      boxShadow: '0 10px 22px rgba(15,23,42,0.10)',
    })
    mapInstance.addOverlay(label)
  })

  if (bPoints.length >= 2) {
    const line = new BMapGL.Polyline(bPoints, {
      strokeColor: '#ff8839',
      strokeWeight: 4,
      strokeOpacity: 0.8,
    })
    mapInstance.addOverlay(line)
  }

  mapInstance.setViewport(bPoints, { margins: [40, 40, 40, 40] })
}

async function initMapIfNeeded() {
  if (activeTab.value !== 'map') return
  if (!mapContainerRef.value) return
  if (mapLoading.value) return
  mapLoading.value = true
  try {
    mapError.value = ''
    const BMapGL = await loadBaiduMapSdk()
    bmapGlobal.value = typeof window !== 'undefined' ? !!window.BMapGL : false

    if (!mapInstance) {
      mapInstance = new BMapGL.Map(mapContainerRef.value)
      mapInstance.enableScrollWheelZoom(true)
      mapInstance.addControl(new BMapGL.ScaleControl())
    }

    const points = await buildMapPoints()
    if (!points.length) {
      const center = new BMapGL.Point(110.93, 32.84)
      mapInstance.centerAndZoom(center, 6)
      mapReady.value = true
      return
    }

    renderRouteOnMap(BMapGL, points)
    mapReady.value = true
  } catch (err) {
    mapReady.value = false
    mapError.value = err instanceof Error ? err.message : '地图初始化失败'
  } finally {
    mapLoading.value = false
  }
}

async function retryMap() {
  mapError.value = ''
  mapReady.value = false
  await initMapIfNeeded()
}

function toggleDay(dayNo) {
  const next = new Set(openDays.value)
  if (next.has(dayNo)) next.delete(dayNo)
  else next.add(dayNo)
  openDays.value = next
}

function isDayOpen(dayNo) {
  return openDays.value.has(dayNo)
}

function parseBudget(raw) {
  const text = String(raw || '')
  const m = text.match(/\d+/)
  return m ? Number(m[0]) : 0
}

function pickRoutes() {
  const kw = `${form.value.departure} ${form.value.destination} ${form.value.preference}`.toLowerCase()
  const maxBudget = budget.value
  return allRoutes.value
    .filter((r) => (maxBudget ? Number(r.price || 0) <= maxBudget : true))
    .map((r) => {
      const hay = [r.xianlumingcheng, r.chufadi, r.mudedi, r.xianlufenlei, r.jingdianmingcheng]
        .filter(Boolean)
        .join(' ')
        .toLowerCase()
      const score = (kw && hay.includes(kw) ? 10 : 0) + Math.min(10, Number(r.clicknum || 0) / 3)
      return { r, score }
    })
    .sort((a, b) => b.score - a.score)
    .slice(0, 12)
    .map((x) => x.r)
}

function buildPlanFromForm() {
  const picks = pickRoutes()
  return {
    title: `${form.value.departure || '出发地'} → ${form.value.destination || '目的地'}`,
    departure: form.value.departure,
    destination: form.value.destination,
    days: Math.max(1, Number(form.value.days || 1)),
    people: Number(form.value.people || 1),
    budget: parseBudget(form.value.budgetText),
    preference: form.value.preference,
    recommended: picks.map((p) => ({
      tablename: p.__table || 'lvyouxianlu',
      id: p.id,
      name: p.xianlumingcheng,
      cover: p.fengmiantu,
      price: p.price,
      from: p.chufadi,
      to: p.mudedi,
      traffic: p.jiaotongfangshi,
      category: p.xianlufenlei,
    })),
  }
}

function generate() {
  result.value = buildPlanFromForm()
  const firstDays = itineraryDays.value.map((d) => d.dayNo)
  openDays.value = new Set(firstDays.slice(0, 1))
  saveTrip({
    title: `${result.value.title} · ${result.value.days}天`,
    payload: result.value,
  })
  ElMessage.success('已生成并保存行程')
}

function hydrateEditableRows() {
  const rows = []
  itineraryDays.value.forEach((day) => {
    day.activities.forEach((a, idx) => {
      rows.push({
        id: `${day.dayNo}-${idx}-${a.id}`,
        dayNo: day.dayNo,
        order: idx + 1,
        name: a.name || '',
        desc: `${a.from || ''} → ${a.to || ''} · ${a.traffic || ''}`.trim(),
        slot: a.slot || '上午',
        location: a.to || a.from || '',
      })
    })
  })
  editableRows.value = rows
}

watch(itineraryDays, hydrateEditableRows, { immediate: true })

function saveEditedRows() {
  if (!result.value) return
  const sorted = [...editableRows.value].sort((a, b) => Number(a.dayNo) - Number(b.dayNo) || Number(a.order) - Number(b.order))
  result.value.recommended = sorted.map((r, idx) => ({
    tablename: 'lvyouxianlu',
    id: Date.now() + idx,
    name: r.name || `活动${idx + 1}`,
    cover: '',
    price: 0,
    from: form.value.departure || '',
    to: r.location || form.value.destination || '',
    traffic: '',
    category: '行程活动',
  }))
  saveTrip({
    title: `${result.value.title} · ${result.value.days}天`,
    payload: result.value,
  })
  ElMessage.success('编辑内容已保存')
}

function removeRow(id) {
  editableRows.value = editableRows.value.filter((r) => r.id !== id)
}

function addRow() {
  editableRows.value.push({
    id: `${Date.now()}-${Math.random()}`,
    dayNo: 1,
    order: 1,
    name: '',
    desc: '',
    slot: '上午',
    location: form.value.destination || '',
  })
}

function addToCart(rec) {
  const good = allRoutes.value.find((r) => Number(r.id) === Number(rec.id))
  if (!good) return
  upsertCartItem({ tablename: rec.tablename || 'lvyouxianlu', good })
  ElMessage.success('已加入购物车')
}

function copyBrief() {
  if (!result.value) return
  const lines = [`${result.value.title} · ${result.value.days}天`, `预算：¥${result.value.budget} / 人`]
  itineraryDays.value.forEach((d) => {
    lines.push(`${d.title} ${d.location}`)
    d.activities.forEach((a) => lines.push(`- ${a.slot} ${a.name}`))
  })
  navigator.clipboard?.writeText(lines.join('\n')).then(() => ElMessage.success('已复制行程摘要'))
}

function copyInviteLink() {
  navigator.clipboard?.writeText(window.location.href).then(() => ElMessage.success('邀请链接已复制'))
}

function downloadPdf() {
  if (!result.value) {
    ElMessage.warning('暂无可导出的行程')
    return
  }

  const escapeHtml = (s) =>
    String(s ?? '')
      .replaceAll('&', '&amp;')
      .replaceAll('<', '&lt;')
      .replaceAll('>', '&gt;')
      .replaceAll('"', '&quot;')
      .replaceAll("'", '&#39;')

  const fmtDate = (ms) => {
    try {
      const d = new Date(ms || Date.now())
      const y = d.getFullYear()
      const m = String(d.getMonth() + 1).padStart(2, '0')
      const dd = String(d.getDate()).padStart(2, '0')
      return `${y}-${m}-${dd}`
    } catch {
      return ''
    }
  }

  // 先准备静态地图（后端代理，避免 AK 暴露）
  buildMapPoints()
    .then((pts) => {
      const pointStr = (pts || []).map((p) => `${Number(p.lng)},${Number(p.lat)}`).join('|')
      const staticMapUrl = pointStr
        ? `/api/maps/staticimage?points=${encodeURIComponent(pointStr)}&w=1200&h=800&zoom=6`
        : ''

      const dayBlocks = itineraryDays.value
        .map((d) => {
          const rows = (d.activities || [])
            .map(
              (a) => `
              <tr>
                <td class="t-slot">${escapeHtml(a.slot || '')}</td>
                <td class="t-name">${escapeHtml(a.name || '')}</td>
                <td class="t-desc">${escapeHtml(a.desc || a.description || '')}</td>
                <td class="t-loc">${escapeHtml(a.location || d.location || '')}</td>
              </tr>
            `,
            )
            .join('')
          return `
            <section class="day">
              <div class="day-head">
                <div class="day-title">${escapeHtml(d.title || `第${d.dayNo}天`)}</div>
                <div class="day-meta">${escapeHtml(d.location || '')}</div>
              </div>
              <table class="tbl">
                <thead>
                  <tr>
                    <th style="width: 88px;">时段</th>
                    <th style="width: 200px;">活动</th>
                    <th>描述</th>
                    <th style="width: 160px;">地点</th>
                  </tr>
                </thead>
                <tbody>${rows || '<tr><td colspan="4" class="empty">暂无活动</td></tr>'}</tbody>
              </table>
            </section>
          `
        })
        .join('')

      const title = escapeHtml(result.value.title || headerTitle.value || '我的行程')
      const subtitle = escapeHtml(`${form.value.destination || ''} · ${headerDays.value}天`)
      const today = fmtDate(Date.now())

      const html = `<!doctype html>
<html>
  <head>
    <meta charset="utf-8" />
    <meta name="viewport" content="width=device-width, initial-scale=1" />
    <title>${title} - PDF</title>
    <style>
      @page { size: A4; margin: 14mm 12mm; }
      html, body { height: 100%; }
      body { font-family: -apple-system, BlinkMacSystemFont, "Segoe UI", Roboto, "PingFang SC", "Hiragino Sans GB", "Microsoft YaHei", Arial, sans-serif; color: #0f172a; }
      .wrap { max-width: 980px; margin: 0 auto; }
      .head { padding: 8px 0 10px; border-bottom: 1px solid #e2e8f0; }
      .h1 { font-size: 20px; font-weight: 900; margin: 0; }
      .sub { margin-top: 4px; color: #475569; font-size: 12px; display: flex; justify-content: space-between; gap: 12px; }
      .pill { display: inline-flex; align-items: center; gap: 8px; }
      .map { margin: 12px 0 10px; border: 1px solid #e2e8f0; border-radius: 10px; overflow: hidden; }
      .map img { width: 100%; display: block; }
      .map .hint { padding: 8px 10px; font-size: 11px; color: #64748b; background: #f8fafc; border-top: 1px solid #e2e8f0; }
      .day { margin: 12px 0 14px; page-break-inside: avoid; }
      .day-head { display: flex; justify-content: space-between; gap: 12px; align-items: baseline; margin-bottom: 8px; }
      .day-title { font-weight: 900; font-size: 14px; }
      .day-meta { font-size: 12px; color: #475569; }
      .tbl { width: 100%; border-collapse: collapse; }
      .tbl th, .tbl td { border: 1px solid #e2e8f0; padding: 8px 10px; vertical-align: top; }
      .tbl th { background: #f8fafc; font-size: 12px; text-align: left; }
      .tbl td { font-size: 12px; line-height: 1.5; }
      .t-slot { color: #334155; white-space: nowrap; }
      .t-name { font-weight: 800; }
      .t-loc { color: #334155; }
      .empty { text-align: center; color: #64748b; padding: 16px 10px; }
      .foot { margin-top: 12px; padding-top: 10px; border-top: 1px solid #e2e8f0; color: #94a3b8; font-size: 11px; }
      @media print { .no-print { display: none !important; } }
    </style>
  </head>
  <body>
    <div class="wrap">
      <header class="head">
        <h1 class="h1">${title}</h1>
        <div class="sub">
          <div class="pill">${subtitle}</div>
          <div class="pill">导出日期：${today}</div>
        </div>
      </header>
      ${staticMapUrl ? `<section class="map"><img src="${staticMapUrl}" alt="路线地图" /><div class="hint">路线静态图（由后端代理生成）。</div></section>` : ''}
      ${dayBlocks}
      <footer class="foot">灵犀旅行 · 行程导出</footer>
    </div>
    <script>
      // 等图片加载完再触发打印，避免 PDF 缺图
      (function(){
        const imgs = Array.from(document.images || []);
        if (!imgs.length) { window.print(); return; }
        let left = imgs.length;
        const done = () => { left--; if (left <= 0) window.print(); };
        imgs.forEach(img => {
          if (img.complete) return done();
          img.addEventListener('load', done, { once: true });
          img.addEventListener('error', done, { once: true });
        });
        setTimeout(() => window.print(), 2500);
      })();
    <\\/script>
  </body>
</html>`

      const w = window.open('', '_blank')
      if (!w) {
        ElMessage.error('浏览器拦截了弹窗，请允许后重试')
        return
      }
      w.document.open()
      w.document.write(html)
      w.document.close()
    })
    .catch(() => {
      ElMessage.error('PDF 导出失败，请稍后重试')
    })
}

function findFlight() {
  window.open('https://flights.ctrip.com/online/channel/domestic', '_blank')
}

function findHotel() {
  window.open('https://www.ctrip.com/', '_blank')
}

function deleteCurrentPlan() {
  result.value = null
  editableRows.value = []
  ElMessage.success('当前页面内容已清空')
}

function hydrateFromQuery() {
  const q = route.query
  const tripId = String(q.tripId || '').trim()
  if (tripId) {
    const hit = listTrips().find((t) => String(t.id) === tripId)
    if (hit?.payload) {
      result.value = hit.payload
      const from = String(hit.payload.departure || hit.payload.from || '').trim()
      const to = String(hit.payload.destination || hit.payload.to || '').trim()
      if (from) form.value.departure = from
      if (to) form.value.destination = to
      if (Number(hit.payload.days) > 0) form.value.days = Number(hit.payload.days)
      return
    }
  }

  const mode = String(q.mode || '')
  const destination = String(q.destination || '').trim()
  const departure = String(q.departure || '').trim()
  const days = Number(q.days || 0)
  if (departure) form.value.departure = departure
  if (destination) form.value.destination = destination
  if (days > 0) form.value.days = Math.max(1, Math.min(15, days))
  if (String(q.plannerInput || '')) form.value.plannerInput = String(q.plannerInput || '')

  if (mode === 'multi') {
    citiesChain.value = String(q.cities || '')
      .split('|')
      .map((x) => x.trim())
      .filter(Boolean)
    const dayList = String(q.days || '')
      .split('|')
      .map((x) => Number(x) || 1)
    if (citiesChain.value.length) {
      form.value.departure = citiesChain.value[0]
      form.value.destination = citiesChain.value[citiesChain.value.length - 1]
      form.value.days = Math.max(1, Math.min(15, dayList.reduce((a, b) => a + b, 0)))
      form.value.preference = `多城市路线：${citiesChain.value.join(' → ')}`
    }
  }
  if (String(q.autogen || '') === '1') generate()
}

onMounted(hydrateFromQuery)

watch(activeTab, async (next) => {
  const prev = lastTab.value
  lastTab.value = next

  // 离开地图 Tab：容器会被销毁（v-else-if），必须丢弃旧实例，否则回切会空白
  if (prev === 'map' && next !== 'map') {
    try {
      mapContainerRef.value && (mapContainerRef.value.innerHTML = '')
    } catch {
      // ignore
    }
    mapInstance = null
    mapReady.value = false
    mapError.value = ''
    return
  }

  if (next !== 'map') return
  // 切 Tab 时 map 容器是 v-else-if 动态挂载的，必须等 DOM 就绪再初始化
  await nextTick()
  initMapIfNeeded()
})

watch(itineraryDays, () => {
  if (activeTab.value === 'map') initMapIfNeeded()
})
</script>

<template>
  <section class="tripPage">
    <header class="header" :style="{ backgroundImage: `url(${heroBg})` }">
      <div class="header__mask" />
      <div class="header__content">
        <h2>{{ headerTitle }}</h2>
        <div class="header__sub">{{ form.destination || 'China' }} · {{ headerDays }}天</div>
      </div>
    </header>

    <section class="card lynx-card lynx-card--glass">
      <div class="tools">
        <button class="toolBtn" @click="downloadPdf">下载PDF</button>
        <button class="toolBtn" @click="copyBrief">复制摘要</button>
        <button class="toolBtn" @click="copyInviteLink">复制链接</button>
        <button class="toolBtn" @click="findFlight">查找航班</button>
        <button class="toolBtn" @click="findHotel">查找酒店</button>
      </div>

      <div class="tabBar">
        <button class="tab" :class="{ 'is-active': activeTab === 'activities' }" @click="activeTab = 'activities'">活动</button>
        <button class="tab" :class="{ 'is-active': activeTab === 'map' }" @click="activeTab = 'map'">地图</button>
        <button class="tab" :class="{ 'is-active': activeTab === 'edit' }" @click="activeTab = 'edit'">编辑</button>
        <button class="tab" :class="{ 'is-active': activeTab === 'settings' }" @click="activeTab = 'settings'">设置</button>
      </div>

      <div v-if="activeTab === 'activities'" class="panelArea">
        <div v-if="!itineraryDays.length" class="emptyHint">暂无行程，先生成或从历史进入。</div>
        <article v-for="day in itineraryDays" :key="day.dayNo" class="dayCard" :class="{ open: isDayOpen(day.dayNo) }">
          <header class="dayHeader" @click="toggleDay(day.dayNo)">
            <div class="dayHeader__title">
              <strong>{{ day.title }}</strong>
              <span>{{ day.location }}, China</span>
            </div>
            <button class="collapseBtn">{{ isDayOpen(day.dayNo) ? '▼' : '▶' }}</button>
          </header>
          <div v-if="isDayOpen(day.dayNo)" class="dayBody">
            <div v-for="a in day.activities" :key="a.idKey" class="actItem">
              <div class="actItem__head">{{ a.slot }}：{{ a.name }}</div>
              <div class="actItem__desc">{{ a.from || '—' }} → {{ a.to || '—' }} · {{ a.traffic || '交通待定' }}</div>
              <div class="actItem__ops">
                <button class="minor">购买活动</button>
                <button class="minor">图片</button>
                <button class="minor">Videos</button>
                <button class="minor" @click="addToCart(a)">加入购物车</button>
              </div>
            </div>
          </div>
        </article>
      </div>

      <div v-else-if="activeTab === 'map'" class="panelArea">
        <div class="mapBox">
          <div class="mapCanvas" :class="{ 'is-error': !!mapError }">
            <div v-if="mapError" class="mapState mapState--error">
              <div>{{ mapError }}</div>
              <el-button size="small" type="danger" plain @click="retryMap">重试加载</el-button>
            </div>
            <div v-else-if="!mapReady || mapLoading" class="mapState">地图加载中...</div>
            <div ref="mapContainerRef" class="mapHost" />
            <div v-if="mapError || !bmapGlobal" class="mapDebug">{{ mapDebug }}</div>
          </div>
          <div class="mapLegend">
            <div>{{ cityRouteText }}</div>
            <div>支持导游导览、景点讲解、路径推荐</div>
          </div>
        </div>
      </div>

      <div v-else-if="activeTab === 'edit'" class="panelArea">
        <div class="editBar">
          <button class="minor" @click="addRow">新增行</button>
          <button class="minor minor--primary" @click="saveEditedRows">保存</button>
        </div>
        <div class="tableWrap">
          <table class="editTable">
            <thead>
              <tr>
                <th>天</th>
                <th>顺序</th>
                <th>活动</th>
                <th>描述</th>
                <th>时段</th>
                <th>地点</th>
                <th></th>
              </tr>
            </thead>
            <tbody>
              <tr v-for="row in editableRows" :key="row.id">
                <td><input v-model.number="row.dayNo" type="number" min="1" /></td>
                <td><input v-model.number="row.order" type="number" min="1" /></td>
                <td><input v-model="row.name" type="text" /></td>
                <td><input v-model="row.desc" type="text" /></td>
                <td><input v-model="row.slot" type="text" /></td>
                <td><input v-model="row.location" type="text" /></td>
                <td><button class="delBtn" @click="removeRow(row.id)">🗑</button></td>
              </tr>
            </tbody>
          </table>
        </div>
      </div>

      <div v-else-if="activeTab === 'settings'" class="panelArea">
        <div class="settingBlock">
          <div class="settingTitle">标题</div>
          <input class="input" type="text" :value="headerTitle" readonly />
        </div>
        <div class="settingBlock">
          <div class="settingTitle">成员</div>
          <div class="member">{{ me?.xingming || me?.yonghuming || '用户' }}</div>
        </div>
        <div class="settingBlock">
          <div class="settingTitle">隐私</div>
          <label class="radio"><input v-model="privacy" type="radio" value="public" /> 公开访问</label>
          <label class="radio"><input v-model="privacy" type="radio" value="member" /> 仅行程成员可见</label>
        </div>
        <div class="inviteBox">
          <div>复制邀请链接，授予拥有链接的用户协作权限。</div>
          <button class="minor minor--primary" @click="copyInviteLink">复制 →</button>
        </div>
        <div class="dangerRow">
          <button class="dangerBtn" @click="deleteCurrentPlan">删除当前页面内容</button>
        </div>
      </div>

    </section>
  </section>
</template>

<style scoped lang="scss">
.tripPage { display: flex; flex-direction: column; gap: 14px; }
.header {
  position: relative;
  height: 210px;
  border-radius: 16px;
  overflow: hidden;
  background-size: cover;
  background-position: center;
}
.header__mask { position: absolute; inset: 0; background: linear-gradient(90deg, rgba(2, 6, 23, 0.56), rgba(2, 6, 23, 0.2)); }
.header__content { position: relative; z-index: 1; color: #fff; height: 100%; display: flex; flex-direction: column; justify-content: center; padding: 22px; }
.header__content h2 { margin: 0; font-size: 34px; font-weight: 900; }
.header__sub { margin-top: 8px; opacity: 0.95; font-weight: 700; }

.card { border-radius: 16px; padding: 14px; }
.tools { display: flex; flex-wrap: wrap; gap: 10px; margin-bottom: 12px; }
.toolBtn {
  height: 36px; padding: 0 14px; border-radius: 10px; border: 1px solid rgba(15, 23, 42, 0.12);
  background: #fff; font-size: 13px; font-weight: 800; color: #334155; cursor: pointer;
}
.tabBar { display: flex; background: #f5f8ff; border-radius: 10px; overflow: hidden; margin-bottom: 14px; }
.tab {
  flex: 1; height: 42px; border: 0; background: transparent; color: #64748b; font-size: 14px; font-weight: 900; cursor: pointer;
  border-bottom: 2px solid transparent;
}
.tab.is-active { color: #ff8839; background: #fff; border-bottom-color: #ff8839; }
.panelArea { min-height: 180px; }
.emptyHint { color: #64748b; font-size: 13px; padding: 12px; }

.dayCard { border: 1px solid rgba(15, 23, 42, 0.08); border-radius: 12px; background: #fff; margin-bottom: 12px; overflow: hidden; }
.dayHeader { height: 56px; padding: 0 16px; display: flex; align-items: center; justify-content: space-between; cursor: pointer; }
.dayHeader__title { display: flex; align-items: baseline; gap: 10px; }
.dayHeader__title strong { color: #0f172a; }
.dayHeader__title span { color: #64748b; font-size: 13px; }
.collapseBtn { border: 0; background: transparent; color: #64748b; font-weight: 800; cursor: pointer; }
.dayBody { padding: 0 16px 14px; }
.actItem { border-top: 1px solid rgba(15, 23, 42, 0.06); padding: 12px 0; }
.actItem__head { font-weight: 900; color: #0f172a; }
.actItem__desc { margin-top: 6px; color: #64748b; font-size: 13px; }
.actItem__ops { margin-top: 8px; display: flex; gap: 8px; flex-wrap: wrap; }
.minor {
  height: 30px; border-radius: 8px; border: 1px solid rgba(15, 23, 42, 0.1);
  background: #f8fafc; color: #475569; font-size: 12px; font-weight: 800; padding: 0 10px; cursor: pointer;
}
.minor--primary { background: #ff8839; color: #fff; border-color: #ff8839; }

.mapBox { border: 1px solid rgba(15, 23, 42, 0.08); border-radius: 12px; overflow: hidden; background: #fff; }
.mapCanvas {
  position: relative;
  width: 100%;
  aspect-ratio: 4 / 3;
  min-height: 320px;
  max-height: 560px;
  background: linear-gradient(145deg, #f8fafc, #eef2ff);
}
.mapCanvas.is-error { background: rgba(254, 242, 242, 1); }
.mapHost { position: absolute; inset: 0; }
.mapState {
  position: absolute;
  inset: 0;
  z-index: 2;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 10px;
  font-size: 13px;
  color: #64748b;
  padding: 0 12px;
  text-align: center;
  pointer-events: none;
}
.mapState :deep(.el-button) { pointer-events: auto; }
.mapState--error { color: #991b1b; }
.mapLegend { padding: 12px; color: #64748b; font-size: 13px; line-height: 1.7; }
.mapDebug {
  position: absolute;
  right: 10px;
  bottom: 10px;
  z-index: 3;
  padding: 6px 10px;
  border-radius: 999px;
  font-size: 12px;
  color: rgba(15, 23, 42, 0.72);
  background: rgba(255, 255, 255, 0.82);
  border: 1px solid rgba(15, 23, 42, 0.10);
  backdrop-filter: blur(8px);
  pointer-events: none;
}

.editBar { display: flex; justify-content: flex-end; gap: 8px; margin-bottom: 10px; }
.tableWrap { overflow: auto; border: 1px solid rgba(15, 23, 42, 0.08); border-radius: 10px; }
.editTable { width: 100%; border-collapse: collapse; background: #fff; min-width: 900px; }
.editTable th, .editTable td { border-bottom: 1px solid rgba(15, 23, 42, 0.07); padding: 8px; }
.editTable th { background: #f8fafc; color: #64748b; font-size: 12px; text-align: left; }
.editTable input { width: 100%; border: 0; background: transparent; outline: none; color: #334155; font-size: 13px; }
.delBtn { border: 0; background: transparent; cursor: pointer; }

.settingBlock { margin-bottom: 14px; }
.settingTitle { font-weight: 900; color: #ff8839; margin-bottom: 8px; }
.input {
  width: 100%; height: 42px; border: 1px solid rgba(15, 23, 42, 0.12); border-radius: 10px; padding: 0 12px;
  background: #fff; color: #334155; box-sizing: border-box;
}
.member { color: #0f172a; font-weight: 800; }
.radio { display: block; margin-bottom: 8px; color: #475569; }
.inviteBox { border: 1px solid rgba(14, 165, 233, 0.18); background: rgba(14, 165, 233, 0.08); border-radius: 10px; padding: 12px; display: flex; justify-content: space-between; align-items: center; gap: 10px; }
.dangerRow { margin-top: 12px; text-align: right; }
.dangerBtn { height: 34px; border: 0; border-radius: 8px; background: #ef4444; color: #fff; font-weight: 800; padding: 0 12px; cursor: pointer; }

@media (max-width: 760px) {
  .tabBar { flex-wrap: wrap; }
  .tab { flex: 1 1 50%; }
  .header__content h2 { font-size: 26px; }
}
</style>

