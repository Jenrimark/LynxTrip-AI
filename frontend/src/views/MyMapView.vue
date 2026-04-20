<script setup>
import { computed, nextTick, onBeforeUnmount, onMounted, ref } from 'vue'

const BAIDU_MAP_AK = import.meta.env.VITE_BAIDU_MAP_AK || ''
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

