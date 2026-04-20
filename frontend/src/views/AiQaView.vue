<script setup>
import { nextTick, onBeforeUnmount, onMounted, ref, watch } from 'vue'
import { ElMessage } from 'element-plus'
import DOMPurify from 'dompurify'
import { marked } from 'marked'
import { listNews, listRoutes } from '../services/lynxDb'
import lingxiLogo from '../../../picturee/LOGO.png'

marked.use({
  gfm: true,
  breaks: true,
})

const API_BASE = import.meta.env.VITE_API_BASE_URL || 'http://127.0.0.1:8080'

const DRAFT_KEY = 'lynxtrip.draft.aiqa.v1'

const suggestions = [
  {
    title: '路线推荐',
    desc: '帮我推荐一个 3 天游玩的红色+人文线路',
    prompt: '帮我推荐一个 3 天游玩的红色+人文线路，并给出价格、出发地、目的地、交通方式和出行时间建议。',
    variant: 'v1',
  },
  {
    title: '资讯速读',
    desc: '把最近一篇旅游资讯总结成 5 条要点',
    prompt: '请把最近一篇旅游资讯总结成 5 条要点，并给我一个适合分享的标题。',
    variant: 'v2',
  },
  {
    title: '行前清单',
    desc: '去武当山三日游需要带什么？',
    prompt: '我要去武当山三日游，请给我一份行前清单（衣物/药品/证件/装备/小物）。',
    variant: 'v3',
  },
  {
    title: '预算规划',
    desc: '预算 1500 元，适合哪些线路？',
    prompt: '预算 1500 元以内，推荐 3 条线路（含分类、景点名称、出发地、目的地、交通方式、价格）。',
    variant: 'v4',
  },
]

const feedRef = ref(null)
const inputRef = ref(null)

const composer = ref({
  text: '',
  files: [],
})

const isChatMode = ref(false)
const isGenerating = ref(false)
let typingTimer = null

const messages = ref([]) // {id, role: 'user'|'assistant', text, html, ts}

function loadDraft() {
  try {
    const raw = sessionStorage.getItem(DRAFT_KEY)
    if (!raw) return
    const parsed = JSON.parse(raw)
    if (parsed && typeof parsed.text === 'string') composer.value.text = parsed.text
  } catch {
    // ignore
  }
}

function saveDraft() {
  try {
    sessionStorage.setItem(DRAFT_KEY, JSON.stringify({ text: composer.value.text || '' }))
  } catch {
    // ignore
  }
}

function nowTs() {
  return new Date().toLocaleTimeString('zh-CN', { hour: '2-digit', minute: '2-digit' })
}

function scrollToBottom() {
  const el = feedRef.value
  if (!el) return
  el.scrollTop = el.scrollHeight
}

function escapeHtml(str) {
  return String(str)
    .replaceAll('&', '&amp;')
    .replaceAll('<', '&lt;')
    .replaceAll('>', '&gt;')
    .replaceAll('"', '&quot;')
    .replaceAll("'", '&#039;')
}

/** 大模型 Markdown → 安全 HTML（列表/标题/加粗等不再露出原始符号） */
function renderAiMarkdown(src) {
  const raw = String(src ?? '')
  try {
    const html = marked.parse(raw, { async: false })
    return DOMPurify.sanitize(html, { USE_PROFILES: { html: true } })
  } catch (e) {
    console.warn('[灵犀] Markdown 解析失败，已退回纯文本', e)
    return DOMPurify.sanitize(escapeHtml(raw).replace(/\n/g, '<br>'))
  }
}

function formatMoney(n) {
  return `¥ ${Number(n || 0).toFixed(0)}`
}

/** 出行时间展示（避免直接露出 ISO 原始串） */
function formatTripTime(raw) {
  if (raw == null || raw === '' || raw === '—') return '—'
  const s = String(raw)
  const t = Date.parse(s)
  if (!Number.isNaN(t)) {
    return new Date(t).toLocaleString('zh-CN', {
      year: 'numeric',
      month: '2-digit',
      day: '2-digit',
      hour: '2-digit',
      minute: '2-digit',
    })
  }
  return s.length > 40 ? s.slice(0, 40) + '…' : s
}

function takeTop(list, n) {
  return list.slice(0, n)
}

function searchCorpus(query) {
  const q = query.trim().toLowerCase()
  const routes = [
    ...listRoutes('lvyouxianlu').map((r) => ({ ...r, __table: 'lvyouxianlu' })),
    ...listRoutes('zuixinxianlu').map((r) => ({ ...r, __table: 'zuixinxianlu' })),
  ]

  const news = listNews()

  const routeHits = routes
    .map((r) => {
      const hay = [r.xianlumingcheng, r.xianlufenlei, r.jingdianmingcheng, r.chufadi, r.mudedi, r.jiaotongfangshi]
        .filter(Boolean)
        .join(' ')
        .toLowerCase()
      const score = (q && hay.includes(q) ? 10 : 0) + Math.min(6, Number(r.clicknum || 0) / 4)
      return { r, score }
    })
    .sort((a, b) => b.score - a.score)
    .filter((x) => (q ? x.score > 0 : true))
    .slice(0, 5)
    .map((x) => x.r)

  const newsHits = news
    .map((n) => {
      const hay = [n.title, n.introduction, n.content].filter(Boolean).join(' ').toLowerCase()
      const score = q && hay.includes(q) ? 10 : 0
      return { n, score }
    })
    .sort((a, b) => b.score - a.score)
    .filter((x) => (q ? x.score > 0 : true))
    .slice(0, 3)
    .map((x) => x.n)

  return { routeHits, newsHits }
}

function buildAnswer(userText, filesText) {
  const merged = [userText, filesText].filter(Boolean).join('\n\n')
  const { routeHits, newsHits } = searchCorpus(merged)

  // 简易意图识别
  const wantsBudget = /预算|元|以内|不超过/.test(merged)
  const wantsSummary = /总结|要点|概括|提炼/.test(merged)
  const wantsPacking = /清单|带什么|准备什么|行前/.test(merged)

  const blocks = []

  if (wantsPacking) {
    blocks.push(`
      <div class="lx-card">
        <div class="lx-card__hd">行前清单（通用）</div>
        <ul class="lx-ul">
          <li><b>证件</b>：身份证/学生证/老年证（如有）</li>
          <li><b>衣物</b>：轻薄外套+雨具；山地景区早晚温差大</li>
          <li><b>鞋袜</b>：防滑运动鞋；备用袜 2-3 双</li>
          <li><b>药品</b>：创可贴/肠胃药/晕车药/过敏药</li>
          <li><b>电子</b>：充电器/充电宝/数据线；相机可选</li>
        </ul>
      </div>
    `)
  }

  if (wantsSummary && newsHits.length) {
    const top = newsHits[0]
    const intro = escapeHtml(top.introduction || '').slice(0, 240)
    blocks.push(`
      <div class="lx-card">
        <div class="lx-card__hd">资讯速读：${escapeHtml(top.title)}</div>
        <div class="lx-muted">${intro}${intro.length >= 240 ? '…' : ''}</div>
        <div class="lx-kv">
          <div class="lx-kv__k">建议分享标题</div>
          <div class="lx-kv__v">${escapeHtml(top.title.split('——')[0] || top.title)}</div>
        </div>
        <div class="lx-kv">
          <div class="lx-kv__k">5条要点</div>
          <div class="lx-kv__v">
            <ol class="lx-ol">
              <li>围绕“红色/乡村/生态/人文”的叙事主线组织内容。</li>
              <li>用简介中的关键名词做路线关键词与打卡点。</li>
              <li>提取 1-2 个可体验项目（手作/研学/民俗）。</li>
              <li>补充“适合人群/季节/时长”便于决策。</li>
              <li>结尾给出 1 句行动号召（收藏/转发/周末出发）。</li>
            </ol>
          </div>
        </div>
      </div>
    `)
  }

  if (routeHits.length) {
    let picked = routeHits
    if (wantsBudget) {
      const m = merged.match(/(\d+)\s*元/)
      const budget = m ? Number(m[1]) : null
      if (budget) picked = picked.filter((r) => Number(r.price || 0) <= budget)
    }
    picked = takeTop(picked, 3)
    blocks.push(`
      <div class="lx-card">
        <div class="lx-card__hd">相关线路</div>
        <div class="lx-grid">
          ${picked
            .map(
              (r) => `
              <div class="lx-route">
                <div class="lx-route__title">${escapeHtml(r.xianlumingcheng)}</div>
                <div class="lx-route__meta">${escapeHtml(r.xianlufenlei)} · ${formatMoney(r.price)} · 点击 ${Number(r.clicknum || 0)}</div>
                <div class="lx-route__meta">${escapeHtml(r.chufadi || '—')} → ${escapeHtml(r.mudedi || '—')} · ${escapeHtml(r.jiaotongfangshi || '—')}</div>
                <div class="lx-route__meta">出行时间：${escapeHtml(formatTripTime(r.chuxingshijian))}</div>
              </div>
            `,
            )
            .join('')}
        </div>
      </div>
    `)
  } else {
    blocks.push(`
      <div class="lx-card">
        <div class="lx-card__hd">我需要更多线索</div>
        <div class="lx-muted">你可以补充：目的地/天数/预算/偏好（红绿古比例）/出发地，方便我更准地推荐。</div>
      </div>
    `)
  }

  if (filesText) {
    blocks.push(`
      <div class="lx-card">
        <div class="lx-card__hd">你上传的文本（已读）</div>
        <pre class="lx-pre">${escapeHtml(filesText.slice(0, 900))}${filesText.length > 900 ? '\n…(已截断)' : ''}</pre>
      </div>
    `)
  }

  return blocks.join('\n')
}

async function ensureChatMode() {
  if (isChatMode.value) return
  isChatMode.value = true
  await nextTick()
  inputRef.value?.focus?.()
}

function pushUser(text) {
  messages.value.push({ id: `${Date.now()}_${Math.random().toString(36).slice(2)}`, role: 'user', text, ts: nowTs() })
}

function pushAssistantShell() {
  const id = `${Date.now()}_${Math.random().toString(36).slice(2)}`
  messages.value.push({ id, role: 'assistant', html: '', ts: nowTs() })
  return id
}

function setAssistantHtml(id, html) {
  const msg = messages.value.find((m) => m.id === id)
  if (msg) msg.html = html
}

function stop(silent = false) {
  if (typingTimer) clearInterval(typingTimer)
  typingTimer = null
  isGenerating.value = false
  if (!silent) ElMessage.info('已停止生成')
}

async function send(text) {
  const t = (text ?? composer.value.text).trim()
  if (!t && composer.value.files.length === 0) return
  if (isGenerating.value) return

  if (typingTimer) {
    clearInterval(typingTimer)
    typingTimer = null
  }

  await ensureChatMode()

  const filesText = composer.value.files.map((f) => `【${f.name}】\n${f.text}`).join('\n\n')
  composer.value.text = ''
  composer.value.files = []
  saveDraft()

  pushUser(t || '（发送了附件）')
  const aiId = pushAssistantShell()
  await nextTick()
  scrollToBottom()

  // Get local data as context
  const { routeHits, newsHits } = searchCorpus([t, filesText].filter(Boolean).join('\n\n'))

  isGenerating.value = true

  try {
    // Call backend AI API
    const response = await fetch(`${API_BASE}/api/ai/chat`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({
        message: t,
        routeHits: routeHits.map(r => ({
          id: r.id,
          xianlumingcheng: r.xianlumingcheng,
          xianlufenlei: r.xianlufenlei,
          price: r.price,
          chufadi: r.chufadi,
          mudedi: r.mudedi,
          jiaotongfangshi: r.jiaotongfangshi,
          jingdianmingcheng: r.jingdianmingcheng,
          clicknum: r.clicknum
        })),
        newsHits: newsHits.map(n => ({
          id: n.id,
          title: n.title,
          introduction: n.introduction,
          content: n.content
        })),
        filesText
      })
    })

    const rawText = await response.text()
    let data = {}
    try {
      data = rawText ? JSON.parse(rawText) : {}
    } catch {
      console.warn('[灵犀] /api/ai/chat 返回非 JSON', response.status, rawText?.slice?.(0, 200))
    }

    if (!response.ok) {
      console.warn('[灵犀] 接口 HTTP', response.status, data?.content || rawText?.slice?.(0, 300))
    } else if (data && data.success === false) {
      console.warn('[灵犀] 未使用大模型（后端返回失败）:', data.content || '(无详情)')
    }

    if (data.success && data.source === 'ai') {
      const rawMd = String(data.content ?? '')
      if (!rawMd.length) {
        setAssistantHtml(aiId, '<div class="lx-ai-content lx-ai-content--md"></div>')
        isGenerating.value = false
      } else {
        let i = 0
        const step = 12
        const runTick = () => {
          i += step
          if (i > rawMd.length) i = rawMd.length
          const slice = rawMd.slice(0, i)
          const body = renderAiMarkdown(slice)
          const tail = i < rawMd.length ? '<span class="lx-cursor">▋</span>' : ''
          setAssistantHtml(aiId, `<div class="lx-ai-content lx-ai-content--md">${body}${tail}</div>`)
          nextTick(() => scrollToBottom())
          if (i >= rawMd.length) {
            if (typingTimer) {
              clearInterval(typingTimer)
              typingTimer = null
            }
            isGenerating.value = false
            setAssistantHtml(aiId, `<div class="lx-ai-content lx-ai-content--md">${renderAiMarkdown(rawMd)}</div>`)
          }
        }
        runTick()
        typingTimer = setInterval(runTick, 18)
      }
    } else {
      // 接口未配置或失败：前端兜底展示
      const fullHtml = buildAnswer(t, filesText)
      let i = 0
      const step = 18
      typingTimer = setInterval(async () => {
        i += step
        setAssistantHtml(aiId, fullHtml.slice(0, i) + (i < fullHtml.length ? '<span class="lx-cursor">▋</span>' : ''))
        await nextTick()
        scrollToBottom()
        if (i >= fullHtml.length) {
          clearInterval(typingTimer)
          typingTimer = null
          isGenerating.value = false
          setAssistantHtml(aiId, fullHtml)
        }
      }, 18)
    }
  } catch (err) {
    console.error('[灵犀] 请求失败（将使用页面兜底）:', err)
    const fullHtml = buildAnswer(t, filesText)
    let i = 0
    const step = 18
    typingTimer = setInterval(async () => {
      i += step
      setAssistantHtml(aiId, fullHtml.slice(0, i) + (i < fullHtml.length ? '<span class="lx-cursor">▋</span>' : ''))
      await nextTick()
      scrollToBottom()
      if (i >= fullHtml.length) {
        clearInterval(typingTimer)
        typingTimer = null
        isGenerating.value = false
        setAssistantHtml(aiId, fullHtml)
      }
    }, 18)
  }
}

async function onPickSuggestion(s) {
  composer.value.text = s.prompt
  await nextTick()
  await send(s.prompt)
}

function resetChat() {
  stop()
  messages.value = []
  isChatMode.value = false
  composer.value.text = ''
  composer.value.files = []
}

async function onFilesChange(e) {
  const files = Array.from(e.target.files || [])
  e.target.value = ''
  if (!files.length) return

  const reads = files.map(async (f) => {
    const isTextLike =
      f.type.startsWith('text/') ||
      /\.(txt|md|json|csv|sql|js|ts|java|css|html|yml|yaml|xml)$/i.test(f.name)
    if (!isTextLike) return { name: f.name, text: '[不支持的文件类型：仅演示文本类附件]' }
    const text = await f.text()
    return { name: f.name, text }
  })
  composer.value.files = await Promise.all(reads)
  ElMessage.success(`已添加附件 ${composer.value.files.length} 个`)
  await ensureChatMode()
  await nextTick()
  inputRef.value?.focus?.()
}

function onKeydown(e) {
  if (e.key === 'Enter' && !e.shiftKey && !e.isComposing) {
    e.preventDefault()
    send()
  }
}

onMounted(() => {
  loadDraft()
})

watch(
  () => composer.value.text,
  () => saveDraft(),
  { flush: 'post' },
)

onBeforeUnmount(() => {
  // 切换页面时静默停止，不弹“已停止生成”
  stop(true)
  saveDraft()
})
</script>

<template>
  <section class="lx-page">
    <div class="lx-shell lynx-card lynx-card--glass">
      <header class="lx-topbar">
        <div class="lx-topbar__left">
          <div class="lx-title">灵犀 AI 助手</div>
        </div>
        <div class="lx-topbar__right">
          <el-button size="small" :disabled="!messages.length" @click="resetChat">清除对话</el-button>
        </div>
      </header>

      <main class="lx-main">
        <div class="lx-chat" ref="feedRef">
          <div v-if="!isChatMode" class="lx-welcome">
            <div class="lx-avatar">
              <div
                class="lx-avatar__ring"
                :style="{
                  backgroundImage: `url(${lingxiLogo})`,
                }"
              />
            </div>
            <h1 class="lx-welcome__title">你好，我是灵犀</h1>
            <p class="lx-welcome__desc">心有灵犀一点通，你想要的我都有！</p>

            <div class="lx-suggest">
              <button v-for="s in suggestions" :key="s.title" class="lx-cardBtn" type="button" @click="onPickSuggestion(s)">
                <div class="lx-cardBtn__icon" :class="`is-${s.variant}`" aria-hidden="true">
                  <svg v-if="s.variant === 'v1'" class="lx-cardBtn__svg" viewBox="0 0 24 24">
                    <path
                      fill="none"
                      stroke="currentColor"
                      stroke-linecap="round"
                      stroke-linejoin="round"
                      stroke-width="1.8"
                      d="M3 18h6M3 6h6M3 12h4m12-8v16m0 0l-3-3m3 3l3-3M9 12l3 3 6-6"
                    />
                  </svg>
                  <svg v-else-if="s.variant === 'v2'" class="lx-cardBtn__svg" viewBox="0 0 24 24">
                    <path
                      fill="none"
                      stroke="currentColor"
                      stroke-linecap="round"
                      stroke-linejoin="round"
                      stroke-width="1.8"
                      d="M5 4h14v16H5zM8 8h8M8 12h8M8 16h5"
                    />
                  </svg>
                  <svg v-else-if="s.variant === 'v3'" class="lx-cardBtn__svg" viewBox="0 0 24 24">
                    <path
                      fill="none"
                      stroke="currentColor"
                      stroke-linecap="round"
                      stroke-linejoin="round"
                      stroke-width="1.8"
                      d="M6 4h12v16H6zM9 8h6M9 12h6M9 16h4M18 7l2-2"
                    />
                  </svg>
                  <svg v-else class="lx-cardBtn__svg" viewBox="0 0 24 24">
                    <path
                      fill="none"
                      stroke="currentColor"
                      stroke-linecap="round"
                      stroke-linejoin="round"
                      stroke-width="1.8"
                      d="M4 19h16M7 16V9m5 7V5m5 11v-6"
                    />
                  </svg>
                </div>
                <div class="lx-cardBtn__title">{{ s.title }}</div>
                <div class="lx-cardBtn__desc">{{ s.desc }}</div>
              </button>
            </div>
          </div>

          <div v-else class="lx-list">
            <div v-for="m in messages" :key="m.id" class="lx-msg" :class="`is-${m.role}`">
              <div class="lx-msg__avatar" aria-hidden="true">
                <span v-if="m.role === 'assistant'">灵</span>
                <span v-else>我</span>
              </div>
              <div class="lx-msg__content">
                <div v-if="m.role === 'assistant'" class="lx-bubble lx-bubble--ai" v-html="m.html" />
                <div v-else class="lx-bubble lx-bubble--user">{{ m.text }}</div>
                <div class="lx-ts">{{ m.ts }}</div>
              </div>
            </div>
          </div>
        </div>

        <footer class="lx-composer">
          <div v-if="composer.files.length" class="lx-files">
            <span v-for="f in composer.files" :key="f.name" class="lx-fileChip">{{ f.name }}</span>
          </div>
          <div class="lx-inputRow">
            <textarea
              ref="inputRef"
              v-model="composer.text"
              class="lx-input"
              rows="1"
              placeholder="发送消息给灵犀...（Enter 发送，Shift+Enter 换行）"
              @keydown="onKeydown"
            />
            <div class="lx-actions">
              <label class="lx-attach" title="上传文本/SQL 等附件">
                <input class="lx-file" type="file" multiple @change="onFilesChange" />
                <span class="lx-attach__ico">📎</span>
              </label>
              <button v-if="!isGenerating" class="lx-send" type="button" @click="send">发送</button>
              <button v-else class="lx-stop" type="button" @click="stop">停止</button>
            </div>
          </div>
        </footer>
      </main>
    </div>
  </section>
</template>

<style scoped lang="scss">
.lx-page {
  height: 100%;
  min-height: 0;
}

.lx-shell {
  height: 100%;
  min-height: 0;
  display: flex;
  flex-direction: column;
  border-radius: 18px;
  overflow: hidden;
  background:
    radial-gradient(920px 360px at 18% 10%, rgba(255, 136, 57, 0.16), transparent 60%),
    radial-gradient(920px 360px at 86% 20%, rgba(56, 189, 248, 0.14), transparent 60%),
    rgba(255, 255, 255, 0.82);
}

.lx-topbar {
  height: 56px;
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 0 16px;
  background: rgba(255, 255, 255, 0.86);
  border-bottom: 1px solid rgba(15, 23, 42, 0.08);
  backdrop-filter: blur(12px);
}

.lx-title {
  font-weight: 900;
  color: #0f172a;
}
.lx-subtitle {
  margin-top: 2px;
  font-size: 12px;
  color: #64748b;
}

.lx-main {
  flex: 1;
  min-height: 0;
  display: grid;
  grid-template-rows: 1fr auto;
}

.lx-chat {
  min-height: 0;
  overflow: auto;
  padding: 22px 12%;
}

.lx-welcome {
  min-height: 56vh;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 10px;
}

.lx-avatar {
  width: 142px;
  height: 142px;
  border-radius: 999px;
  position: relative;
  display: grid;
  place-items: center;
  margin-bottom: 4px;
}
.lx-avatar__ring {
  position: absolute;
  inset: 0;
  border-radius: 999px;
  background-position: center;
  background-size: contain;
  background-repeat: no-repeat;
  filter: blur(0.2px);
  animation: floaty 3s ease-in-out infinite;
}
@keyframes floaty {
  0%,
  100% {
    transform: translateY(0);
  }
  50% {
    transform: translateY(-8px);
  }
}

.lx-welcome__title {
  margin: 0;
  font-size: 44px;
  line-height: 1.05;
  font-weight: 900;
  background: linear-gradient(135deg, #ea7828 0%, #f59e42 55%, #fbbf24 100%);
  -webkit-background-clip: text;
  background-clip: text;
  -webkit-text-fill-color: transparent;
  text-align: center;
}
.lx-welcome__desc {
  margin: 0;
  font-size: 14px;
  color: #6b5c4a;
  text-align: center;
}

.lx-suggest {
  margin-top: 18px;
  width: min(900px, 100%);
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 14px;
}

.lx-cardBtn {
  text-align: left;
  border: 2px solid transparent;
  border-radius: 24px;
  padding: 18px 16px;
  background: rgba(255, 255, 255, 0.9);
  box-shadow: 0 8px 32px rgba(234, 120, 40, 0.12);
  cursor: pointer;
  position: relative;
  overflow: hidden;
  transition: transform 0.2s ease, box-shadow 0.2s ease, border-color 0.2s ease;
}
.lx-cardBtn:hover {
  transform: translateY(-4px);
  box-shadow: 0 16px 40px rgba(15, 23, 42, 0.12);
  border-color: rgba(255, 136, 57, 0.26);
}
.lx-cardBtn__icon {
  width: 44px;
  height: 44px;
  border-radius: 12px;
  margin-bottom: 12px;
  box-shadow: 0 4px 14px rgba(234, 120, 40, 0.28);
  display: grid;
  place-items: center;
  color: #fff;
}
.lx-cardBtn__svg {
  width: 24px;
  height: 24px;
  display: block;
}
.lx-cardBtn__icon.is-v1 {
  background: linear-gradient(135deg, #ea7828, #f59e42);
}
.lx-cardBtn__icon.is-v2 {
  background: linear-gradient(135deg, #6366f1, #8b5cf6);
  box-shadow: 0 4px 14px rgba(99, 102, 241, 0.3);
}
.lx-cardBtn__icon.is-v3 {
  background: linear-gradient(135deg, #34d399, #06b6d4);
  box-shadow: 0 4px 14px rgba(52, 211, 153, 0.3);
}
.lx-cardBtn__icon.is-v4 {
  background: linear-gradient(135deg, #f472b6, #fb923c);
  box-shadow: 0 4px 14px rgba(244, 114, 182, 0.3);
}
.lx-cardBtn__title {
  font-size: 13px;
  font-weight: 900;
  color: #0f172a;
  margin-bottom: 6px;
}
.lx-cardBtn__desc {
  font-size: 12px;
  color: #64748b;
  line-height: 1.5;
}

.lx-list {
  display: flex;
  flex-direction: column;
  gap: 18px;
}

.lx-msg {
  display: flex;
  gap: 12px;
}
.lx-msg.is-user {
  flex-direction: row-reverse;
}

.lx-msg__avatar {
  width: 36px;
  height: 36px;
  border-radius: 999px;
  display: grid;
  place-items: center;
  font-weight: 900;
  color: #fff;
  flex: 0 0 auto;
}
.lx-msg.is-user .lx-msg__avatar {
  background: linear-gradient(135deg, #34d399, #06b6d4);
  box-shadow: 0 3px 12px rgba(52, 211, 153, 0.22);
}
.lx-msg.is-assistant .lx-msg__avatar {
  background: linear-gradient(135deg, #ea7828, #f59e42);
  box-shadow: 0 3px 12px rgba(234, 120, 40, 0.22);
}

.lx-msg__content {
  max-width: 72%;
  display: flex;
  flex-direction: column;
  gap: 6px;
}
.lx-msg.is-user .lx-msg__content {
  align-items: flex-end;
}

.lx-bubble {
  padding: 12px 16px;
  border-radius: 16px;
  font-size: 14px;
  line-height: 1.7;
  word-break: break-word;
}
.lx-bubble--user {
  background: linear-gradient(135deg, #ea7828, #f59e42);
  color: #fff;
  border-bottom-right-radius: 4px;
  white-space: pre-wrap;
  box-shadow: 0 4px 16px rgba(234, 120, 40, 0.22);
}
.lx-bubble--ai {
  background: rgba(255, 255, 255, 0.96);
  border: 1px solid rgba(240, 224, 208, 1);
  border-bottom-left-radius: 4px;
  box-shadow: 0 4px 24px rgba(234, 120, 40, 0.1);
  color: #1a1208;
}

.lx-ts {
  font-size: 11px;
  color: #a8917a;
  padding: 0 4px;
}

.lx-composer {
  padding: 14px 12%;
  border-top: 1px solid rgba(15, 23, 42, 0.08);
  background: rgba(255, 255, 255, 0.72);
  backdrop-filter: blur(12px);
}

.lx-files {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  margin-bottom: 10px;
}
.lx-fileChip {
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

.lx-inputRow {
  background: rgba(255, 255, 255, 0.92);
  border: 2px solid rgba(240, 224, 208, 1);
  border-radius: 24px;
  padding: 10px 12px;
  display: flex;
  align-items: flex-end;
  gap: 10px;
  box-shadow: 0 4px 24px rgba(234, 120, 40, 0.1);
}
.lx-inputRow:focus-within {
  border-color: rgba(255, 136, 57, 0.7);
  box-shadow: 0 0 0 3px rgba(255, 136, 57, 0.14), 0 4px 24px rgba(234, 120, 40, 0.1);
}

.lx-input {
  flex: 1;
  border: 0;
  outline: none;
  resize: none;
  background: transparent;
  color: #0f172a;
  font-size: 14px;
  line-height: 1.6;
  max-height: 160px;
  min-height: 24px;
  font-family: inherit;
}
.lx-input::placeholder {
  color: #a8917a;
}

.lx-actions {
  display: flex;
  align-items: center;
  gap: 8px;
}

.lx-file {
  display: none;
}
.lx-attach {
  width: 34px;
  height: 34px;
  border-radius: 8px;
  border: 1px solid rgba(240, 224, 208, 1);
  background: rgba(255, 255, 255, 1);
  cursor: pointer;
  display: grid;
  place-items: center;
  transition: transform 0.15s ease, border-color 0.15s ease, background 0.15s ease;
  color: #6b5c4a;
}
.lx-attach:hover {
  transform: scale(1.06);
  border-color: rgba(255, 136, 57, 0.7);
  background: rgba(255, 248, 243, 1);
}
.lx-attach__ico {
  font-size: 16px;
  line-height: 1;
}

.lx-send {
  height: 36px;
  padding: 0 14px;
  border: 0;
  border-radius: 10px;
  cursor: pointer;
  font-weight: 900;
  color: #fff;
  background: linear-gradient(135deg, #ea7828, #f59e42);
  box-shadow: 0 3px 12px rgba(234, 120, 40, 0.35);
  transition: transform 0.15s ease, box-shadow 0.15s ease, opacity 0.15s ease;
}
.lx-send:hover {
  transform: translateY(-1px);
  box-shadow: 0 5px 18px rgba(234, 120, 40, 0.45);
}

.lx-stop {
  height: 36px;
  padding: 0 14px;
  border: 0;
  border-radius: 10px;
  cursor: pointer;
  font-weight: 900;
  color: #fff;
  background: linear-gradient(135deg, #f472b6, #f97316);
  box-shadow: 0 3px 12px rgba(244, 114, 182, 0.35);
  transition: transform 0.15s ease, box-shadow 0.15s ease;
}
.lx-stop:hover {
  transform: translateY(-1px);
  box-shadow: 0 5px 18px rgba(244, 114, 182, 0.45);
}

.lx-hint {
  margin-top: 8px;
  font-size: 11px;
  color: #a8917a;
  text-align: center;
}

/* assistant rich blocks */
.lx-bubble--ai :deep(.lx-h) {
  font-size: 16px;
  font-weight: 900;
  color: #0f172a;
  margin-bottom: 6px;
}
.lx-bubble--ai :deep(.lx-muted) {
  font-size: 12px;
  color: #6b5c4a;
  line-height: 1.6;
}
.lx-bubble--ai :deep(.lx-card) {
  margin-top: 10px;
  border: 1px solid rgba(240, 224, 208, 1);
  border-radius: 16px;
  padding: 10px 12px;
  background: rgba(255, 255, 255, 0.92);
}
.lx-bubble--ai :deep(.lx-card__hd) {
  font-weight: 900;
  color: #0f172a;
  margin-bottom: 8px;
}
.lx-bubble--ai :deep(.lx-ul),
.lx-bubble--ai :deep(.lx-ol) {
  margin: 0;
  padding-left: 18px;
  color: #1a1208;
  line-height: 1.7;
  font-size: 13px;
}
.lx-bubble--ai :deep(.lx-kv) {
  margin-top: 10px;
  display: grid;
  grid-template-columns: 110px 1fr;
  gap: 10px;
}
.lx-bubble--ai :deep(.lx-kv__k) {
  font-size: 12px;
  color: #6b5c4a;
  font-weight: 900;
}
.lx-bubble--ai :deep(.lx-kv__v) {
  font-size: 13px;
  color: #1a1208;
  line-height: 1.7;
}
.lx-bubble--ai :deep(.lx-grid) {
  display: grid;
  grid-template-columns: 1fr;
  gap: 10px;
}
.lx-bubble--ai :deep(.lx-route) {
  border: 1px solid rgba(15, 23, 42, 0.08);
  border-radius: 14px;
  padding: 10px;
  background: rgba(255, 248, 243, 0.7);
}
.lx-bubble--ai :deep(.lx-route__title) {
  font-weight: 900;
  color: #0f172a;
  line-height: 1.25;
}
.lx-bubble--ai :deep(.lx-route__meta) {
  margin-top: 6px;
  font-size: 12px;
  color: #6b5c4a;
  line-height: 1.55;
}
.lx-bubble--ai :deep(.lx-pre) {
  margin: 0;
  background: rgba(15, 23, 42, 0.92);
  color: #e2e8f0;
  border-radius: 14px;
  padding: 12px;
  overflow: auto;
  font-size: 12px;
  line-height: 1.6;
}
.lx-bubble--ai :deep(.lx-ai-content) {
  font-size: 14px;
  line-height: 1.8;
  color: #1a1208;
  white-space: pre-wrap;
}

/* 大模型 Markdown 渲染（标题/列表/加粗等为 HTML，不再显示 **、###、- 等原始符号） */
.lx-bubble--ai :deep(.lx-ai-content--md) {
  white-space: normal;
}
.lx-bubble--ai :deep(.lx-ai-content--md > *:first-child) {
  margin-top: 0;
}
.lx-bubble--ai :deep(.lx-ai-content--md p) {
  margin: 0 0 0.75em;
}
.lx-bubble--ai :deep(.lx-ai-content--md h1),
.lx-bubble--ai :deep(.lx-ai-content--md h2),
.lx-bubble--ai :deep(.lx-ai-content--md h3),
.lx-bubble--ai :deep(.lx-ai-content--md h4) {
  margin: 0.85em 0 0.45em;
  font-weight: 900;
  color: #0f172a;
  line-height: 1.35;
}
.lx-bubble--ai :deep(.lx-ai-content--md h1) {
  font-size: 1.25rem;
}
.lx-bubble--ai :deep(.lx-ai-content--md h2) {
  font-size: 1.12rem;
}
.lx-bubble--ai :deep(.lx-ai-content--md h3),
.lx-bubble--ai :deep(.lx-ai-content--md h4) {
  font-size: 1.02rem;
}
.lx-bubble--ai :deep(.lx-ai-content--md ul),
.lx-bubble--ai :deep(.lx-ai-content--md ol) {
  margin: 0.4em 0 0.75em;
  padding-left: 1.35em;
}
.lx-bubble--ai :deep(.lx-ai-content--md li) {
  margin: 0.28em 0;
}
.lx-bubble--ai :deep(.lx-ai-content--md ul) {
  list-style: disc;
}
.lx-bubble--ai :deep(.lx-ai-content--md ol) {
  list-style: decimal;
}
.lx-bubble--ai :deep(.lx-ai-content--md blockquote) {
  margin: 0.5em 0;
  padding: 0.35em 0 0.35em 0.85em;
  border-left: 3px solid rgba(234, 120, 40, 0.45);
  color: #5c4d3d;
}
.lx-bubble--ai :deep(.lx-ai-content--md code) {
  font-size: 0.9em;
  padding: 0.12em 0.38em;
  border-radius: 6px;
  background: rgba(15, 23, 42, 0.06);
  font-family: ui-monospace, SFMono-Regular, Menlo, Monaco, Consolas, monospace;
}
.lx-bubble--ai :deep(.lx-ai-content--md pre) {
  margin: 0.6em 0;
  padding: 12px 14px;
  border-radius: 12px;
  overflow: auto;
  background: rgba(15, 23, 42, 0.06);
  font-size: 12px;
  line-height: 1.55;
}
.lx-bubble--ai :deep(.lx-ai-content--md pre code) {
  padding: 0;
  background: transparent;
  font-size: inherit;
}
.lx-bubble--ai :deep(.lx-ai-content--md table) {
  width: 100%;
  border-collapse: collapse;
  margin: 0.6em 0;
  font-size: 13px;
}
.lx-bubble--ai :deep(.lx-ai-content--md th),
.lx-bubble--ai :deep(.lx-ai-content--md td) {
  border: 1px solid rgba(15, 23, 42, 0.1);
  padding: 8px 10px;
  text-align: left;
}
.lx-bubble--ai :deep(.lx-ai-content--md th) {
  background: rgba(255, 248, 243, 0.95);
  font-weight: 800;
}
.lx-bubble--ai :deep(.lx-ai-content--md a) {
  color: #ea7828;
  text-decoration: underline;
  text-underline-offset: 2px;
}
.lx-bubble--ai :deep(.lx-ai-content--md hr) {
  margin: 0.85em 0;
  border: 0;
  border-top: 1px solid rgba(15, 23, 42, 0.1);
}
.lx-bubble--ai :deep(.lx-cursor) {
  animation: blink 0.7s infinite;
  color: #ea7828;
}
@keyframes blink {
  0%,
  100% {
    opacity: 1;
  }
  50% {
    opacity: 0;
  }
}

@media (max-width: 980px) {
  .lx-chat,
  .lx-composer {
    padding-left: 4%;
    padding-right: 4%;
  }
  .lx-suggest {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
  .lx-welcome__title {
    font-size: 36px;
  }
}
@media (max-width: 560px) {
  .lx-suggest {
    grid-template-columns: 1fr;
  }
  .lx-msg__content {
    max-width: 86%;
  }
  .lx-bubble--ai :deep(.lx-kv) {
    grid-template-columns: 1fr;
  }
}
</style>

