<script setup>
import { computed, onMounted, ref, watch } from 'vue'
import { useRoute } from 'vue-router'
import { ElMessage } from 'element-plus'
import { listRoutes, saveTrip, listTrips, upsertCartItem } from '../services/lynxDb'

// ====== 按你的新规范重构：基础信息 + 手风琴个性化 + 比例饼图 ======
const departureExamples = ['北京', '上海', '广州', '深圳', '成都', '重庆', '杭州', '武汉', '西安', '南京', '长沙', '合肥']
const destinationExamples = ['三亚', '大理', '丽江', '厦门', '青岛', '张家界', '桂林', '黄山', '九寨沟', '敦煌', '拉萨', '北京']

const travelTypes = ['政府企业团建', '高校研学', '亲子家庭', '个人游']
const seasons = ['春', '夏', '秋', '冬']

const form = ref({
  // 基本信息
  departure: '',
  destination: '',

  // 个性化选择
  days: 4, // 1-15
  style: 'style-normal', // style-fast | style-normal | style-slow
  budgetText: '5000元',
  people: 1,
  travelType: '个人游',
  season: '春',

  // 红绿古比例（原始输入，展示用；会归一化给饼图）
  ratios: {
    red: 34,
    green: 33,
    ancient: 33,
  },

  preference: '',

  // 可选追加需求
  plannerInput: '',
})

const accordion = ref(['personalize'])
const route = useRoute()

const result = ref(null)
const history = ref([])
const activeItineraryDay = ref(1)

onMounted(() => {
  history.value = listTrips()
  hydrateFromRouteQuery()
})

const normalizedRatios = computed(() => {
  const r = Math.max(0, Number(form.value.ratios.red || 0))
  const g = Math.max(0, Number(form.value.ratios.green || 0))
  const a = Math.max(0, Number(form.value.ratios.ancient || 0))
  const t = r + g + a
  if (!t) return { red: 34, green: 33, ancient: 33, total: 0 }
  return { red: (r / t) * 100, green: (g / t) * 100, ancient: (a / t) * 100, total: t }
})

const daysValue = computed(() => Number(form.value.days || 1))
const redPercent = computed(() => Math.round(normalizedRatios.value.red))
const greenPercent = computed(() => Math.round(normalizedRatios.value.green))
const ancientPercent = computed(() => Math.round(normalizedRatios.value.ancient))

function clampRatios() {
  form.value.ratios.red = Math.min(100, Math.max(0, Number(form.value.ratios.red || 0)))
  form.value.ratios.green = Math.min(100, Math.max(0, Number(form.value.ratios.green || 0)))
  form.value.ratios.ancient = Math.min(100, Math.max(0, Number(form.value.ratios.ancient || 0)))
}

/** 与 AppShell「多城市」弹窗内 range 一致：--ratio 控制轨道填充 */
function rangeStylePct(value) {
  const v = Math.min(100, Math.max(0, Number(value) || 0))
  return { '--ratio': `${v}%` }
}

function rangeStyleDays(days) {
  const d = Math.min(15, Math.max(1, Number(days) || 1))
  const pct = ((d - 1) / 14) * 100
  return { '--ratio': `${pct}%` }
}

// 归一化 & 饼图路径计算（等价 updatePieChart）
function polarToCartesian(cx, cy, r, angleDeg) {
  const rad = ((angleDeg - 90) * Math.PI) / 180
  return { x: cx + r * Math.cos(rad), y: cy + r * Math.sin(rad) }
}

function describeArc(cx, cy, r, startAngle, endAngle) {
  const start = polarToCartesian(cx, cy, r, endAngle)
  const end = polarToCartesian(cx, cy, r, startAngle)
  const largeArcFlag = endAngle - startAngle <= 180 ? '0' : '1'
  return ['M', cx, cy, 'L', start.x, start.y, 'A', r, r, 0, largeArcFlag, 0, end.x, end.y, 'Z'].join(' ')
}

const pie = computed(() => {
  const r = normalizedRatios.value.red
  const g = normalizedRatios.value.green
  const a = normalizedRatios.value.ancient
  let start = 0
  const redEnd = start + (r / 100) * 360
  const greenEnd = redEnd + (g / 100) * 360
  const ancientEnd = greenEnd + (a / 100) * 360
  const cx = 60
  const cy = 60
  const radius = 52
  return {
    redPath: describeArc(cx, cy, radius, start, redEnd),
    greenPath: describeArc(cx, cy, radius, redEnd, greenEnd),
    ancientPath: describeArc(cx, cy, radius, greenEnd, ancientEnd),
  }
})

const allRoutes = computed(() => {
  // 推荐同时考虑两张线路表
  return [
    ...listRoutes('lvyouxianlu').map((r) => ({ ...r, __table: 'lvyouxianlu' })),
    ...listRoutes('zuixinxianlu').map((r) => ({ ...r, __table: 'zuixinxianlu' })),
  ]
})

function pickTop(n = 6) {
  const city = form.value.destination?.trim()
  const maxPrice = parseBudget(form.value.budgetText)
  const kw = city ? city.toLowerCase() : ''

  const list = allRoutes.value
    .filter((r) => (maxPrice ? Number(r.price || 0) <= maxPrice : true))
    .map((r) => {
      const hay = [r.xianlumingcheng, r.jingdianmingcheng, r.chufadi, r.mudedi, r.xianlufenlei].filter(Boolean).join(' ').toLowerCase()
      const cityScore = kw ? (hay.includes(kw) ? 12 : 0) : 0
      const hotScore = Math.min(10, Number(r.clicknum || 0) / 3)
      // 风格：紧凑更偏热度与多点位；休闲更偏价格与舒适（这里只做轻量权重）
      const style = form.value.style
      const priceScoreBase = maxPrice ? Math.max(0, 8 - (Number(r.price || 0) / maxPrice) * 8) : 4
      const priceScore = style === 'style-slow' ? priceScoreBase * 1.1 : priceScoreBase
      const hotScoreAdj = style === 'style-fast' ? hotScore * 1.1 : hotScore
      return { r, score: cityScore + hotScore + priceScore }
    })
    .sort((a, b) => b.score - a.score)
    .slice(0, n)
    .map((x) => x.r)

  return list
}

function parseBudget(text) {
  const s = String(text || '')
  const m = s.match(/(\d+(?:\.\d+)?)/)
  if (!m) return 0
  const num = Number(m[1])
  return Number.isFinite(num) ? num : 0
}

function generate() {
  const title =
    `${form.value.departure || '出发地'} → ${form.value.destination || '目的地'} · ${daysValue.value}天` +
    (form.value.plannerInput?.trim() ? ` · ${form.value.plannerInput.trim().slice(0, 10)}…` : '')
  const picks = pickTop(6)

  const plan = {
    title,
    departure: form.value.departure,
    destination: form.value.destination,
    days: daysValue.value,
    style: form.value.style,
    budgetText: form.value.budgetText,
    budget: parseBudget(form.value.budgetText),
    people: Number(form.value.people || 1),
    travelType: form.value.travelType,
    season: form.value.season,
    ratios: { ...normalizedRatios.value },
    preference: form.value.preference,
    plannerInput: form.value.plannerInput,
    recommended: picks.map((p) => ({
      tablename: p.__table,
      id: p.id,
      name: p.xianlumingcheng,
      price: p.price,
      cover: p.fengmiantu,
      from: p.chufadi,
      to: p.mudedi,
      traffic: p.jiaotongfangshi,
      time: p.chuxingshijian,
      category: p.xianlufenlei,
    })),
  }

  result.value = plan
  activeItineraryDay.value = 1
  saveTrip({ title, payload: plan })
  history.value = listTrips()
  ElMessage.success('已生成并保存到本地 trips')
}

function addToCart(rec) {
  const full = allRoutes.value.find((r) => String(r.__table) === String(rec.tablename) && Number(r.id) === Number(rec.id))
  if (!full) {
    ElMessage.warning('未找到该线路数据')
    return
  }
  upsertCartItem({ tablename: rec.tablename, good: full })
  ElMessage.success('已加入购物车')
}

const itineraryDays = computed(() => {
  const plan = result.value
  if (!plan) return []
  const totalDays = Math.max(1, Number(plan.days || 1))
  const picks = Array.isArray(plan.recommended) ? plan.recommended : []
  const slots = ['09:00', '11:30', '14:30', '17:00']
  return Array.from({ length: totalDays }, (_, dayIndex) => {
    const dayNo = dayIndex + 1
    const dayActivities = picks
      .filter((_, i) => i % totalDays === dayIndex)
      .map((item, idx) => ({
        ...item,
        time: slots[idx % slots.length],
        note: `${item.from || '出发地'} → ${item.to || '目的地'} · ${item.traffic || '交通待定'}`,
      }))
    return {
      dayNo,
      title: `第${dayNo}天`,
      activities: dayActivities,
    }
  })
})

const currentItineraryDay = computed(() => {
  if (!itineraryDays.value.length) return null
  return itineraryDays.value.find((d) => d.dayNo === activeItineraryDay.value) || itineraryDays.value[0]
})

const budgetOverview = computed(() => {
  const plan = result.value
  if (!plan) return null
  const perPerson = Number(plan.budget || 0)
  const people = Number(plan.people || 1)
  const total = perPerson * people
  return { perPerson, people, total }
})

function copyPlanText() {
  if (!result.value) return
  const lines = [`${result.value.title}`, `预算：¥${budgetOverview.value?.total || 0}`]
  itineraryDays.value.forEach((day) => {
    lines.push(day.title)
    day.activities.forEach((a) => lines.push(`- ${a.time} ${a.name}（${a.note}）`))
  })
  navigator.clipboard
    ?.writeText(lines.join('\n'))
    .then(() => ElMessage.success('已复制行程文案'))
    .catch(() => ElMessage.warning('复制失败，请手动复制'))
}

function hydrateFromRouteQuery() {
  const q = route.query || {}
  const departure = String(q.departure || '').trim()
  const destination = String(q.destination || '').trim()
  const days = Number(q.days || 0)
  if (departure) form.value.departure = departure
  if (destination) form.value.destination = destination
  if (Number.isFinite(days) && days > 0) form.value.days = Math.min(15, Math.max(1, days))

  if (String(q.mode || '') === 'multi') {
    const cities = String(q.cities || '')
      .split('|')
      .map((x) => x.trim())
      .filter(Boolean)
    const dayList = String(q.days || '')
      .split('|')
      .map((x) => Number(x) || 1)
    if (cities.length) {
      form.value.departure = cities[0] || form.value.departure
      form.value.destination = cities[cities.length - 1] || form.value.destination
      form.value.days = Math.min(15, Math.max(1, dayList.reduce((s, n) => s + n, 0)))
      form.value.plannerInput = String(q.plannerInput || '')
      form.value.preference = `多城市路线：${cities.join(' → ')}`
    }
  }
  if (String(q.autogen || '') === '1') {
    generate()
  }
}

watch(
  () => form.value.ratios,
  () => clampRatios(),
  { deep: true },
)

watch(itineraryDays, (days) => {
  if (!days.length) return
  if (!days.some((d) => d.dayNo === activeItineraryDay.value)) {
    activeItineraryDay.value = days[0].dayNo
  }
})
</script>

<template>
  <section class="page">
    <header class="hero lynx-card">
      <div class="hero__left">
        <h2 class="hero__title lynx-h">AI智能规划</h2>
      </div>
    </header>

    <div class="layout">
      <div class="formCard lynx-card lynx-card--glass layout__main">
        <div class="formHd">
          <span class="formHd__accent" aria-hidden="true" />
          <div>
            <div class="formTitle">行程基本信息</div>
            <p class="formHd__sub">填写出发地与偏好，系统将结合本地线路库生成推荐</p>
          </div>
        </div>

        <div class="baseGrid">
          <div class="field">
            <label for="departure">出发地</label>
            <el-input id="departure" v-model="form.departure" placeholder="请输入出发地" clearable />
            <div class="chips">
              <button v-for="c in departureExamples" :key="c" class="chip" type="button" @click="form.departure = c">{{ c }}</button>
            </div>
          </div>

          <div class="field">
            <label for="destination">目的地</label>
            <el-input id="destination" v-model="form.destination" placeholder="请输入目的地" clearable />
            <div class="chips">
              <button v-for="c in destinationExamples" :key="c" class="chip" type="button" @click="form.destination = c">{{ c }}</button>
            </div>
          </div>
        </div>

        <div class="accordion">
          <el-collapse v-model="accordion" class="planCollapse">
            <el-collapse-item title="个性化选择" name="personalize">
              <div class="personalize">
                <div class="row2">
                  <div class="field">
                    <label for="days">旅游天数</label>
                    <div class="rangeRow">
                      <input
                        id="days"
                        v-model.number="form.days"
                        class="plan-range"
                        type="range"
                        min="1"
                        max="15"
                        :style="rangeStyleDays(form.days)"
                      />
                      <span id="days-value" class="rangeVal">{{ daysValue }} 天</span>
                    </div>
                  </div>
                  <div class="field">
                    <label>行程风格</label>
                    <div class="radioRow">
                      <label class="radio"><input v-model="form.style" type="radio" value="style-fast" />紧凑</label>
                      <label class="radio"><input v-model="form.style" type="radio" value="style-normal" />适中</label>
                      <label class="radio"><input v-model="form.style" type="radio" value="style-slow" />休闲</label>
                    </div>
                  </div>
                </div>

                <div class="row2">
                  <div class="field">
                    <label for="budget">预算</label>
                    <el-input id="budget" v-model="form.budgetText" placeholder="例如：5000元" />
                  </div>
                  <div class="field">
                    <label for="people">随行人数</label>
                    <el-input-number id="people" v-model="form.people" :min="1" />
                  </div>
                </div>

                <div class="row2">
                  <div class="field">
                    <label for="travel-type">旅游类型</label>
                    <el-select id="travel-type" v-model="form.travelType" placeholder="请选择">
                      <el-option v-for="t in travelTypes" :key="t" :label="t" :value="t" />
                    </el-select>
                  </div>
                  <div class="field">
                    <label for="season">旅行季节</label>
                    <el-select id="season" v-model="form.season" placeholder="请选择">
                      <el-option v-for="s in seasons" :key="s" :label="s" :value="s" />
                    </el-select>
                  </div>
                </div>

                <div class="ratioWrap">
                  <div class="ratioLeft">
                    <div class="ratioTitle">红绿古三色比例</div>
                    <svg class="pie" viewBox="0 0 120 120" aria-label="比例饼图">
                      <path id="red-slice" :d="pie.redPath" fill="#F0513E" />
                      <path id="green-slice" :d="pie.greenPath" fill="#1FBA62" />
                      <path id="ancient-slice" :d="pie.ancientPath" fill="#915122" />
                      <circle cx="60" cy="60" r="24" fill="rgba(255,255,255,0.92)" />
                    </svg>
                  </div>

                  <div class="ratioRight">
                    <div class="ratioRow">
                      <label for="red-ratio">红色教育</label>
                      <input
                        id="red-ratio"
                        v-model.number="form.ratios.red"
                        class="plan-range"
                        type="range"
                        min="0"
                        max="100"
                        :style="rangeStylePct(form.ratios.red)"
                      />
                      <span id="red-percent" class="pct">{{ redPercent }}%</span>
                    </div>
                    <div class="ratioRow">
                      <label for="green-ratio">自然风光</label>
                      <input
                        id="green-ratio"
                        v-model.number="form.ratios.green"
                        class="plan-range"
                        type="range"
                        min="0"
                        max="100"
                        :style="rangeStylePct(form.ratios.green)"
                      />
                      <span id="green-percent" class="pct">{{ greenPercent }}%</span>
                    </div>
                    <div class="ratioRow">
                      <label for="ancient-ratio">历史文化</label>
                      <input
                        id="ancient-ratio"
                        v-model.number="form.ratios.ancient"
                        class="plan-range"
                        type="range"
                        min="0"
                        max="100"
                        :style="rangeStylePct(form.ratios.ancient)"
                      />
                      <span id="ancient-percent" class="pct">{{ ancientPercent }}%</span>
                    </div>
                  </div>
                </div>

                <div class="field">
                  <label for="preference">特殊偏好</label>
                  <el-input id="preference" v-model="form.preference" type="textarea" :rows="4" placeholder="例如：带小孩 / 无障碍 / 喜欢美食等" />
                </div>
              </div>
            </el-collapse-item>
          </el-collapse>
        </div>

        <div class="sendArea">
          <div class="field sendArea__field">
            <label for="planner-input">追加需求（可选）</label>
            <el-input id="planner-input" v-model="form.plannerInput" placeholder="请输入您的旅行需求(可选)" clearable />
          </div>
          <el-button id="planner-submit" type="primary" size="large" class="submitBtn" @click="generate">开始规划</el-button>
        </div>
      </div>

      <aside class="resultCol layout__aside" aria-label="推荐与历史">
        <div class="resultCard lynx-card lynx-card--glass sideCard">
          <div class="resultHd">
            <span class="sideCard__dot sideCard__dot--primary" aria-hidden="true" />
            <div>
              <div class="resultTitle">推荐结果</div>
              <p class="sideCard__hint">基于表单与线路库匹配</p>
            </div>
          </div>

          <div v-if="!result" class="resultEmpty">
            <el-empty description="填写左侧行程并点击「开始规划」" :image-size="72" />
          </div>

          <div v-else class="resultBody">
            <div class="resultTop">
              <div class="resultPlanTitle lynx-h">{{ result.title }}</div>
              <div class="resultMeta">
                <span class="pill">{{ result.destination || '—' }}</span>
                <span class="pill pill--soft">{{ result.days }}天</span>
                <span class="pill pill--soft">预算 ≤ ¥{{ result.budget }}</span>
              </div>
              <div class="resultActions">
                <el-button size="small" @click="copyPlanText">复制行程文案</el-button>
              </div>
            </div>

            <div class="recGrid">
              <div v-for="rec in result.recommended" :key="`${rec.tablename}:${rec.id}`" class="rec">
                <el-image class="rec__img" :src="rec.cover" fit="cover" :alt="rec.name" />
                <div class="rec__info">
                  <div class="rec__name">{{ rec.name }}</div>
                  <div class="rec__meta">{{ rec.category }} · ¥{{ Number(rec.price || 0).toFixed(0) }}</div>
                  <div class="rec__meta">{{ rec.from || '—' }} → {{ rec.to || '—' }} · {{ rec.traffic || '—' }}</div>
                </div>
                <el-button type="primary" plain @click="addToCart(rec)">加入购物车</el-button>
              </div>
            </div>
          </div>
        </div>

        <div class="hist lynx-card lynx-card--glass sideCard">
          <div class="hist__hd">
            <span class="sideCard__dot sideCard__dot--muted" aria-hidden="true" />
            <div>
              <div class="hist__title">历史生成</div>
              <p class="sideCard__hint">最近保存的规划记录</p>
            </div>
          </div>
          <div v-if="!history.length" class="hist__empty">暂无历史记录</div>
          <div v-else class="hist__list">
            <div v-for="h in history.slice(0, 6)" :key="h.id" class="hist__item">
              <div class="hist__name">{{ h.title }}</div>
              <div class="hist__meta">{{ h.addtime }}</div>
            </div>
          </div>
        </div>
      </aside>
    </div>

    <section v-if="result" class="itinerary lynx-card lynx-card--glass">
      <div class="itinerary__head">
        <div>
          <h3 class="itinerary__title lynx-h">每日行程</h3>
          <p class="itinerary__sub">参考 Aicotravel 的按天阅读体验，结合当前推荐路线自动拆分。</p>
        </div>
        <div v-if="budgetOverview" class="budgetPill">
          总预算 ¥{{ Number(budgetOverview.total || 0).toFixed(0) }}（{{ budgetOverview.people }}人）
        </div>
      </div>

      <div class="dayTabs">
        <button
          v-for="d in itineraryDays"
          :key="d.dayNo"
          type="button"
          class="dayTab"
          :class="{ 'is-active': d.dayNo === activeItineraryDay }"
          @click="activeItineraryDay = d.dayNo"
        >
          {{ d.title }}
        </button>
      </div>

      <div v-if="currentItineraryDay" class="timeline">
        <article v-for="a in currentItineraryDay.activities" :key="`${a.tablename}:${a.id}:${a.time}`" class="timelineItem">
          <div class="timelineItem__time">{{ a.time }}</div>
          <div class="timelineItem__dot" />
          <div class="timelineItem__card">
            <div class="timelineItem__name">{{ a.name }}</div>
            <div class="timelineItem__meta">{{ a.note }}</div>
            <div class="timelineItem__meta">预算参考：¥{{ Number(a.price || 0).toFixed(0) }}</div>
          </div>
        </article>
        <div v-if="!currentItineraryDay.activities.length" class="timelineEmpty">当天暂无可分配景点，建议补充更多偏好后重新规划。</div>
      </div>
    </section>
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
  background: radial-gradient(920px 320px at 20% 10%, rgba(14, 165, 233, 0.16), transparent 60%),
    radial-gradient(920px 320px at 82% 28%, rgba(249, 115, 22, 0.16), transparent 60%),
    rgba(255, 255, 255, 0.92);
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
  max-width: 70ch;
  font-size: 13px;
}

.layout {
  display: grid;
  grid-template-columns: minmax(0, 2fr) minmax(280px, 1fr);
  gap: 20px;
  align-items: start;
}

.layout__main {
  min-width: 0;
}

.layout__aside {
  min-width: 0;
  position: sticky;
  top: calc(56px + var(--space-md));
  max-height: calc(100vh - 56px - 32px);
  overflow-y: auto;
  padding-right: 2px;
  scrollbar-gutter: stable;
}

.formCard {
  border-radius: 18px;
  padding: 18px 18px 20px;
}
.formHd {
  display: flex;
  align-items: flex-start;
  gap: 12px;
  padding: 4px 4px 14px;
  margin-bottom: 4px;
  border-bottom: 1px solid rgba(15, 23, 42, 0.06);
}
.formHd__accent {
  width: 4px;
  height: 44px;
  border-radius: 999px;
  background: linear-gradient(180deg, #0ea5e9 0%, #f97316 100%);
  flex-shrink: 0;
  margin-top: 2px;
}
.formTitle {
  font-weight: 900;
  color: #0f172a;
  font-size: 17px;
}
.formHd__sub {
  margin: 6px 0 0;
  font-size: 13px;
  color: #64748b;
  line-height: 1.45;
  font-weight: 500;
}

.baseGrid {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 12px;
  margin-top: 8px;
}

.field label {
  display: block;
  font-size: 13px;
  font-weight: 900;
  color: #0f172a;
  margin: 0 0 8px 0;
}

.chips {
  margin-top: 10px;
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}
.chip {
  height: 28px;
  padding: 0 10px;
  border-radius: 999px;
  border: 1px solid rgba(15, 23, 42, 0.08);
  background: rgba(255, 255, 255, 0.9);
  color: #334155;
  font-size: 12px;
  font-weight: 800;
  cursor: pointer;
  transition: border-color 180ms ease, transform 180ms ease, box-shadow 180ms ease;
}
.chip:hover {
  transform: translateY(-1px);
  border-color: rgba(255, 136, 57, 0.28);
  box-shadow: 0 12px 22px rgba(15, 23, 42, 0.06);
}

.accordion {
  margin-top: 16px;
}

.planCollapse :deep(.el-collapse) {
  border: none;
  --el-collapse-border-color: transparent;
}
.planCollapse :deep(.el-collapse-item__wrap) {
  border-bottom: none;
}
.planCollapse :deep(.el-collapse-item__header) {
  border: 1px solid rgba(15, 23, 42, 0.08);
  background: rgba(255, 255, 255, 0.88);
  border-radius: 14px;
  padding: 12px 14px;
  font-weight: 800;
  color: #0f172a;
  transition: border-color 0.2s ease, box-shadow 0.2s ease;
}
.planCollapse :deep(.el-collapse-item__header:hover) {
  border-color: rgba(249, 115, 22, 0.25);
  box-shadow: 0 8px 24px rgba(15, 23, 42, 0.06);
}
.planCollapse :deep(.el-collapse-item__arrow) {
  font-weight: 700;
}
.planCollapse :deep(.el-collapse-item__content) {
  padding: 14px 4px 4px;
}

.personalize {
  display: flex;
  flex-direction: column;
  gap: 14px;
  padding-top: 6px;
}

.row2 {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 14px;
}

.rangeRow {
  display: flex;
  align-items: center;
  gap: 12px;
}
.rangeRow .plan-range {
  flex: 1;
  min-width: 0;
}
.rangeVal {
  min-width: 62px;
  text-align: right;
  font-weight: 700;
  font-size: 14px;
  color: #ff6a3d;
}

.radioRow {
  display: flex;
  gap: 10px;
  flex-wrap: wrap;
}
.radio {
  display: inline-flex;
  align-items: center;
  gap: 8px;
  font-size: 13px;
  font-weight: 800;
  color: #334155;
}

.ratioWrap {
  display: grid;
  grid-template-columns: 160px 1fr;
  gap: 14px;
  align-items: center;
  border: 1px solid rgba(15, 23, 42, 0.08);
  background: rgba(255, 255, 255, 0.82);
  border-radius: 16px;
  padding: 12px;
}
.ratioTitle {
  font-weight: 900;
  color: #0f172a;
  margin-bottom: 10px;
}
.pie {
  width: 120px;
  height: 120px;
  display: block;
}
.ratioRow {
  display: grid;
  grid-template-columns: 84px 1fr 52px;
  gap: 10px;
  align-items: center;
  margin-bottom: 10px;
}
.ratioRow label {
  font-size: 14px;
  font-weight: 600;
  color: #ff6a3d;
}
.pct {
  text-align: right;
  font-weight: 700;
  font-size: 14px;
  color: #ff6a3d;
}

/* 与 AppShell 多城市弹窗 .multicity-ratio-row input[type='range'] 同视觉 */
.plan-range {
  width: 100%;
  height: 14px;
  margin: 0;
  border-radius: 999px;
  background: transparent;
  accent-color: #ff8839;
  -webkit-appearance: none;
  appearance: none;
}
.plan-range::-webkit-slider-runnable-track {
  height: 4px;
  border-radius: 999px;
  background: linear-gradient(to right, #ff8839 0%, #ff8839 var(--ratio), #ffd7c1 var(--ratio), #ffd7c1 100%);
}
.plan-range::-webkit-slider-thumb {
  -webkit-appearance: none;
  width: 14px;
  height: 14px;
  margin-top: -5px;
  border-radius: 50%;
  border: 2px solid #fff;
  background: #ff8839;
  box-shadow: 0 2px 8px rgba(255, 136, 57, 0.35);
  cursor: pointer;
}
.plan-range::-moz-range-track {
  height: 4px;
  border-radius: 999px;
  background: #ffd7c1;
}
.plan-range::-moz-range-progress {
  height: 4px;
  border-radius: 999px;
  background: #ff8839;
}
.plan-range::-moz-range-thumb {
  width: 14px;
  height: 14px;
  border-radius: 50%;
  border: 2px solid #fff;
  background: #ff8839;
  box-shadow: 0 2px 8px rgba(255, 136, 57, 0.35);
  cursor: pointer;
}

.sendArea {
  margin-top: 18px;
  border-top: 1px solid rgba(15, 23, 42, 0.08);
  padding-top: 18px;
  display: grid;
  grid-template-columns: 1fr minmax(140px, 180px);
  gap: 16px;
  align-items: end;
}
.sendArea__field {
  min-width: 0;
}
.submitBtn {
  width: 100%;
  height: 44px;
  font-weight: 800;
  border-radius: 12px;
  box-shadow: 0 10px 24px rgba(249, 115, 22, 0.22);
}

.resultCol {
  display: flex;
  flex-direction: column;
  gap: 16px;
}
.sideCard {
  padding: 14px 14px 16px;
}
.sideCard__dot {
  width: 10px;
  height: 10px;
  border-radius: 50%;
  flex-shrink: 0;
  margin-top: 6px;
}
.sideCard__dot--primary {
  background: linear-gradient(135deg, #0ea5e9, #0284c7);
  box-shadow: 0 0 0 3px rgba(14, 165, 233, 0.2);
}
.sideCard__dot--muted {
  background: #94a3b8;
  box-shadow: 0 0 0 3px rgba(148, 163, 184, 0.2);
}
.sideCard__hint {
  margin: 4px 0 0;
  font-size: 12px;
  color: #64748b;
  line-height: 1.4;
  font-weight: 500;
}
.resultCard {
  border-radius: 18px;
}
.resultHd {
  padding: 4px 4px 12px;
  display: flex;
  align-items: flex-start;
  gap: 10px;
  border-bottom: 1px solid rgba(15, 23, 42, 0.06);
  margin-bottom: 10px;
}
.resultTitle {
  font-weight: 900;
  color: #0f172a;
  font-size: 15px;
}
.resultHint {
  font-size: 12px;
  color: #64748b;
}
.resultEmpty {
  padding: 8px 4px 4px;
}
.resultEmpty :deep(.el-empty__description) {
  margin-top: 8px;
}
.resultEmpty :deep(.el-empty__description p) {
  font-size: 13px;
  color: #64748b;
  line-height: 1.5;
}
.resultBody {
  display: flex;
  flex-direction: column;
  gap: 12px;
}
.resultTop {
  border: 1px solid rgba(15, 23, 42, 0.08);
  background: linear-gradient(145deg, rgba(255, 255, 255, 0.95) 0%, rgba(248, 250, 252, 0.92) 100%);
  border-radius: 14px;
  padding: 12px 14px;
}
.resultActions {
  margin-top: 10px;
}
.resultPlanTitle {
  font-weight: 900;
  color: #0f172a;
}
.resultMeta {
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
  font-weight: 800;
  color: #0f172a;
  background: rgba(255, 255, 255, 0.92);
  border: 1px solid rgba(15, 23, 42, 0.08);
}
.pill--soft {
  color: #7c2d12;
  border-color: rgba(249, 115, 22, 0.22);
}

.recGrid {
  display: grid;
  grid-template-columns: 1fr;
  gap: 10px;
}
.rec {
  border: 1px solid rgba(15, 23, 42, 0.07);
  background: rgba(255, 255, 255, 0.92);
  border-radius: 14px;
  padding: 10px 12px;
  display: grid;
  grid-template-columns: 76px 1fr auto;
  gap: 12px;
  align-items: center;
  transition: border-color 0.2s ease, box-shadow 0.2s ease, transform 0.2s ease;
}
.rec:hover {
  border-color: rgba(249, 115, 22, 0.22);
  box-shadow: 0 10px 28px rgba(15, 23, 42, 0.07);
  transform: translateY(-1px);
}
.rec__img {
  width: 76px;
  height: 58px;
  border-radius: 12px;
}
.rec__info {
  min-width: 0;
}
.rec__name {
  font-weight: 900;
  color: #0f172a;
  line-height: 1.25;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
}
.rec__meta {
  margin-top: 6px;
  font-size: 12px;
  color: #64748b;
  line-height: 1.5;
}

.hist {
  border-radius: 18px;
}
.hist__hd {
  display: flex;
  align-items: flex-start;
  gap: 10px;
  padding: 4px 4px 12px;
  border-bottom: 1px solid rgba(15, 23, 42, 0.06);
  margin-bottom: 10px;
}
.hist__title {
  font-weight: 900;
  color: #0f172a;
  font-size: 15px;
}
.hist__hint {
  margin-top: 6px;
  font-size: 12px;
  color: #64748b;
}
.hist__empty {
  padding: 12px 6px;
  font-size: 13px;
  color: #64748b;
  text-align: center;
}
.hist__list {
  display: flex;
  flex-direction: column;
  gap: 8px;
}
.hist__item {
  border: 1px solid rgba(15, 23, 42, 0.07);
  background: rgba(255, 255, 255, 0.9);
  border-radius: 12px;
  padding: 10px 12px;
  border-left: 3px solid rgba(14, 165, 233, 0.45);
  transition: box-shadow 0.2s ease;
}
.hist__item:hover {
  box-shadow: 0 6px 18px rgba(15, 23, 42, 0.06);
}
.hist__name {
  font-weight: 800;
  color: #0f172a;
  font-size: 13px;
  line-height: 1.35;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
}
.hist__meta {
  margin-top: 6px;
  font-size: 11px;
  color: #94a3b8;
  font-variant-numeric: tabular-nums;
}

.itinerary {
  border-radius: 18px;
  padding: 16px;
}
.itinerary__head {
  display: flex;
  justify-content: space-between;
  gap: 12px;
  align-items: flex-start;
}
.itinerary__title {
  margin: 0;
  color: #0f172a;
  font-size: 18px;
}
.itinerary__sub {
  margin: 6px 0 0;
  color: #64748b;
  font-size: 12px;
}
.budgetPill {
  height: 30px;
  border-radius: 999px;
  padding: 0 12px;
  display: inline-flex;
  align-items: center;
  background: rgba(249, 115, 22, 0.12);
  color: #9a3412;
  font-size: 12px;
  font-weight: 800;
}
.dayTabs {
  margin-top: 12px;
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}
.dayTab {
  height: 30px;
  padding: 0 12px;
  border-radius: 999px;
  border: 1px solid rgba(15, 23, 42, 0.1);
  background: rgba(255, 255, 255, 0.88);
  color: #334155;
  font-size: 12px;
  font-weight: 800;
  cursor: pointer;
}
.dayTab.is-active {
  border-color: rgba(249, 115, 22, 0.35);
  background: rgba(249, 115, 22, 0.14);
  color: #7c2d12;
}
.timeline {
  margin-top: 14px;
  display: flex;
  flex-direction: column;
  gap: 10px;
}
.timelineItem {
  display: grid;
  grid-template-columns: 52px 16px 1fr;
  gap: 8px;
  align-items: start;
}
.timelineItem__time {
  color: #f97316;
  font-size: 12px;
  font-weight: 800;
  margin-top: 8px;
}
.timelineItem__dot {
  width: 10px;
  height: 10px;
  border-radius: 999px;
  background: #f97316;
  margin-top: 10px;
  box-shadow: 0 0 0 3px rgba(249, 115, 22, 0.2);
}
.timelineItem__card {
  border: 1px solid rgba(15, 23, 42, 0.08);
  background: rgba(255, 255, 255, 0.9);
  border-radius: 12px;
  padding: 10px 12px;
}
.timelineItem__name {
  color: #0f172a;
  font-weight: 900;
}
.timelineItem__meta {
  margin-top: 5px;
  color: #64748b;
  font-size: 12px;
}
.timelineEmpty {
  color: #64748b;
  font-size: 12px;
  padding: 8px 0;
}

@media (max-width: 1200px) {
  .layout {
    grid-template-columns: 1fr;
  }
  .layout__aside {
    position: relative;
    top: 0;
    max-height: none;
    overflow: visible;
  }
}
@media (max-width: 720px) {
  .baseGrid {
    grid-template-columns: 1fr;
  }
  .row2 {
    grid-template-columns: 1fr;
  }
  .ratioWrap {
    grid-template-columns: 1fr;
  }
  .sendArea {
    grid-template-columns: 1fr;
  }
  .itinerary__head {
    flex-direction: column;
  }
  .rec {
    grid-template-columns: 84px 1fr;
  }
  .rec :deep(.el-button) {
    grid-column: 1 / -1;
    width: 100%;
  }
}
</style>

