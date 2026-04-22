<script setup>
import { computed, nextTick, onBeforeUnmount, onMounted, ref, watch } from 'vue'
import { useRoute } from 'vue-router'

const BAIDU_MAP_AK = import.meta.env.VITE_BAIDU_MAP_AK || ''
const route = useRoute()
const mapWrapRef = ref(null)
const mapHostRef = ref(null)
const mapReady = ref(false)
const mapLoading = ref(false)
const mapError = ref('')
const bmapGlobal = ref(false)

const mapDebug = computed(() => {
  const host = typeof window !== 'undefined' ? window.location.host : '(ssr)'
  const hasAk = !!BAIDU_MAP_AK
  const hasBMap = bmapGlobal.value
  return `host=${host} | ak=${hasAk ? 'ok' : 'missing'} | BMapGL=${hasBMap ? 'yes' : 'no'}`
})

let mapInstance = null
let baiduMapLoader = null
let resizeObserver = null
let resizeRaf = 0
let focusMarker = null
let routeMarkers = []
let routeOverlays = []
let moveAnimRaf = 0

function loadBaiduMapSdk() {
  if (typeof window === 'undefined') return Promise.reject(new Error('当前环境不支持地图加载'))
  if (window.BMapGL) {
    bmapGlobal.value = true
    return Promise.resolve(window.BMapGL)
  }
  if (baiduMapLoader) return baiduMapLoader
  if (!BAIDU_MAP_AK) return Promise.reject(new Error('未配置百度地图 AK（VITE_BAIDU_MAP_AK）'))

  baiduMapLoader = new Promise((resolve, reject) => {
    const timeoutMs = 20000
    let done = false

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
    throw err
  })

  return baiduMapLoader
}

async function initMap() {
  if (mapLoading.value) return
  // HMR/快速切换场景：实例仍在但状态被重置，直接恢复显示
  if (mapInstance) {
    mapReady.value = true
    mapError.value = ''
    return
  }
  if (!mapHostRef.value) return
  mapLoading.value = true
  try {
    mapError.value = ''
    const BMapGL = await loadBaiduMapSdk()
    bmapGlobal.value = typeof window !== 'undefined' ? !!window.BMapGL : false
    mapInstance = new BMapGL.Map(mapHostRef.value)
    mapInstance.enableScrollWheelZoom(true)
    mapInstance.addControl(new BMapGL.ScaleControl())
    const center = new BMapGL.Point(110.93, 32.84)
    mapInstance.centerAndZoom(center, 6)
    mapReady.value = true
  } catch (err) {
    mapReady.value = false
    mapError.value = err instanceof Error ? err.message : '地图初始化失败'
  } finally {
    mapLoading.value = false
  }
}

function setFocusPoint(lng, lat, label = '') {
  if (!mapInstance || !window.BMapGL) return
  const BMapGL = window.BMapGL
  const point = new BMapGL.Point(lng, lat)
  smoothMoveTo(point, 12)
  try {
    if (focusMarker) {
      mapInstance.removeOverlay(focusMarker)
      focusMarker = null
    }
    focusMarker = new BMapGL.Marker(point)
    mapInstance.addOverlay(focusMarker)
    if (label) {
      const labelObj = new BMapGL.Label(label, { offset: new BMapGL.Size(18, -10) })
      focusMarker.setLabel(labelObj)
    }
  } catch {
    // ignore marker failures
  }
}

function getDistanceMeters(a, b) {
  if (!a || !b) return 0
  try {
    if (typeof mapInstance?.getDistance === 'function') {
      const d = Number(mapInstance.getDistance(a, b))
      if (Number.isFinite(d)) return d
    }
  } catch {
    // ignore
  }
  const toRad = (x) => (x * Math.PI) / 180
  const lat1 = toRad(Number(a.lat))
  const lat2 = toRad(Number(b.lat))
  const dLat = lat2 - lat1
  const dLng = toRad(Number(b.lng) - Number(a.lng))
  const h =
    Math.sin(dLat / 2) * Math.sin(dLat / 2) +
    Math.cos(lat1) * Math.cos(lat2) * Math.sin(dLng / 2) * Math.sin(dLng / 2)
  return 6371000 * 2 * Math.atan2(Math.sqrt(h), Math.sqrt(1 - h))
}

function smoothMoveTo(targetPoint, targetZoom = 12) {
  if (!mapInstance || !targetPoint) return
  if (moveAnimRaf) cancelAnimationFrame(moveAnimRaf)
  const start = mapInstance.getCenter?.()
  if (!start) {
    mapInstance.centerAndZoom?.(targetPoint, targetZoom)
    return
  }
  const startZoom = Number(mapInstance.getZoom?.() || 6)
  const distM = Math.max(0, getDistanceMeters(start, targetPoint))
  const totalMs = Math.max(1000, Math.min(4200, 1000 + distM / 260))
  const zoomDip = distM > 300000 ? 2.2 : distM > 100000 ? 1.4 : 0.8
  const sLng = Number(start.lng)
  const sLat = Number(start.lat)
  const tLng = Number(targetPoint.lng)
  const tLat = Number(targetPoint.lat)
  if (![sLng, sLat, tLng, tLat].every((x) => Number.isFinite(x))) {
    mapInstance.setCenter?.(targetPoint)
    mapInstance.setZoom?.(targetZoom)
    return
  }
  const startTs = performance.now()
  const easeInOutCubic = (t) => (t < 0.5 ? 4 * t * t * t : 1 - ((-2 * t + 2) ** 3) / 2)
  const tick = (ts) => {
    const p = Math.max(0, Math.min(1, (ts - startTs) / totalMs))
    const eased = easeInOutCubic(p)
    const curLng = sLng + (tLng - sLng) * eased
    const curLat = sLat + (tLat - sLat) * eased
    const curPoint = new window.BMapGL.Point(curLng, curLat)
    const dip = Math.sin(Math.PI * p) * zoomDip
    const curZoom = startZoom + (targetZoom - startZoom) * eased - dip
    // 先 zoom 再 center，避免“只缩放不平移”
    mapInstance.setZoom?.(Math.max(3, curZoom))
    mapInstance.setCenter?.(curPoint)
    if (p < 1) {
      moveAnimRaf = requestAnimationFrame(tick)
      return
    }
    // 动画结束后强制对齐目标点，避免任何像素级偏差
    mapInstance.centerAndZoom?.(targetPoint, targetZoom)
    requestAnimationFrame(() => mapInstance?.centerAndZoom?.(targetPoint, targetZoom))
    moveAnimRaf = 0
  }
  moveAnimRaf = requestAnimationFrame(tick)
}

function clearRouteOverlays() {
  if (!mapInstance) return
  try {
    routeMarkers.forEach((x) => mapInstance.removeOverlay(x))
    routeOverlays.forEach((x) => mapInstance.removeOverlay(x))
  } catch {
    // ignore
  }
  routeMarkers = []
  routeOverlays = []
}

function parseFocusPath(raw) {
  if (typeof raw !== 'string' || !raw.trim()) return []
  try {
    const parsed = JSON.parse(raw)
    if (!Array.isArray(parsed)) return []
    return parsed
      .map((x) => ({
        name: String(x?.name || ''),
        lng: Number(x?.lng),
        lat: Number(x?.lat),
      }))
      .filter((x) => Number.isFinite(x.lng) && Number.isFinite(x.lat))
  } catch {
    return []
  }
}

function renderFocusPath(points) {
  if (!mapInstance || !window.BMapGL || !points.length) return
  const BMapGL = window.BMapGL
  clearRouteOverlays()
  if (focusMarker) {
    try {
      mapInstance.removeOverlay(focusMarker)
    } catch {
      // ignore
    }
    focusMarker = null
  }

  points.forEach((p, idx) => {
    const pt = new BMapGL.Point(p.lng, p.lat)
    const marker = new BMapGL.Marker(pt)
    mapInstance.addOverlay(marker)
    const numberLabel = new BMapGL.Label(String(idx + 1), { position: pt, offset: new BMapGL.Size(12, -20) })
    numberLabel.setStyle({
      color: '#ffffff',
      fontSize: '12px',
      fontWeight: '900',
      lineHeight: '18px',
      textAlign: 'center',
      minWidth: '18px',
      height: '18px',
      borderRadius: '999px',
      border: '1px solid rgba(255,255,255,0.92)',
      background: 'rgba(220, 38, 38, 0.95)',
      boxShadow: '0 4px 12px rgba(15, 23, 42, 0.28)',
      padding: '0 4px',
    })
    mapInstance.addOverlay(numberLabel)
    routeMarkers.push(marker)
    routeOverlays.push(numberLabel)
  })

  for (let i = 0; i < points.length - 1; i += 1) {
    const from = new BMapGL.Point(Number(points[i].lng), Number(points[i].lat))
    const to = new BMapGL.Point(Number(points[i + 1].lng), Number(points[i + 1].lat))
    const line = new BMapGL.Polyline([from, to], {
      strokeColor: '#0ea5e9',
      strokeWeight: 4,
      strokeOpacity: 0.8,
    })
    mapInstance.addOverlay(line)
    routeOverlays.push(line)
  }

  const last = points[points.length - 1]
  setFocusPoint(last.lng, last.lat, last.name || '')
}

watch(
  () => [route.query.focusLng, route.query.focusLat, route.query.focusLabel, route.query.focusPath],
  async () => {
    const pathPoints = parseFocusPath(route.query.focusPath)
    if (pathPoints.length) {
      if (!mapReady.value) {
        await nextTick()
        await initMap()
      }
      renderFocusPath(pathPoints)
      return
    }
    clearRouteOverlays()
    const lng = Number(route.query.focusLng)
    const lat = Number(route.query.focusLat)
    const label = typeof route.query.focusLabel === 'string' ? route.query.focusLabel : ''
    if (!Number.isFinite(lng) || !Number.isFinite(lat)) return
    if (!mapReady.value) {
      await nextTick()
      await initMap()
    }
    setFocusPoint(lng, lat, label)
  },
  { immediate: true }
)

async function retry() {
  mapError.value = ''
  mapReady.value = false
  mapInstance = null
  baiduMapLoader = null
  await initMap()
}

function requestResize() {
  if (!mapInstance) return
  if (resizeRaf) cancelAnimationFrame(resizeRaf)
  resizeRaf = requestAnimationFrame(() => {
    try {
      mapInstance?.checkResize?.()
    } catch {
      // ignore
    }
  })
}

onMounted(async () => {
  await nextTick()
  await initMap()
  requestResize()

  // 侧栏收起/路由切换可能导致容器尺寸变化：用 ResizeObserver 保证地图不“白屏”
  try {
    if (mapWrapRef.value && typeof ResizeObserver !== 'undefined') {
      resizeObserver = new ResizeObserver(() => requestResize())
      resizeObserver.observe(mapWrapRef.value)
    }
  } catch {
    resizeObserver = null
  }
})

onMounted(() => {
  // 兜底：全局变量非响应式，主动同步一次显示状态
  try {
    bmapGlobal.value = typeof window !== 'undefined' ? !!window.BMapGL : false
  } catch {
    bmapGlobal.value = false
  }
})

onBeforeUnmount(() => {
  try {
    resizeObserver?.disconnect?.()
  } catch {
    // ignore
  }
  resizeObserver = null
  if (resizeRaf) cancelAnimationFrame(resizeRaf)
  resizeRaf = 0
  if (moveAnimRaf) cancelAnimationFrame(moveAnimRaf)
  moveAnimRaf = 0
  mapInstance = null
})
</script>

<template>
  <section class="myMapPage">
    <div ref="mapWrapRef" class="mapWrap" :class="{ 'is-error': !!mapError }">
      <div v-if="mapError" class="mapState mapState--error">
        <div>{{ mapError }}</div>
        <el-button size="small" type="danger" plain @click="retry">重试加载</el-button>
      </div>
      <div v-else-if="!mapReady || mapLoading" class="mapState">地图加载中...</div>
      <div ref="mapHostRef" class="mapHost" />
      <div v-if="mapError || !bmapGlobal" class="mapDebug">{{ mapDebug }}</div>
    </div>
  </section>
</template>

<style scoped lang="scss">
.myMapPage {
  height: 100%;
  display: flex;
  flex-direction: column;
  background: #0b1220;
}

.mapWrap {
  position: relative;
  flex: 1 1 auto;
  min-height: 420px;
  background: linear-gradient(145deg, #0b1220, #0f172a);
}
.mapWrap.is-error { background: rgba(127, 29, 29, 0.18); }
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
  color: rgba(255, 255, 255, 0.75);
  padding: 0 12px;
  text-align: center;
  pointer-events: none;
}
.mapState :deep(.el-button) { pointer-events: auto; }
.mapState--error { color: rgba(254, 226, 226, 1); }

.mapDebug {
  position: absolute;
  right: 10px;
  bottom: 10px;
  z-index: 3;
  padding: 6px 10px;
  border-radius: 999px;
  font-size: 12px;
  color: rgba(255, 255, 255, 0.78);
  background: rgba(2, 6, 23, 0.56);
  border: 1px solid rgba(255, 255, 255, 0.12);
  backdrop-filter: blur(10px);
  pointer-events: none;
}
</style>

