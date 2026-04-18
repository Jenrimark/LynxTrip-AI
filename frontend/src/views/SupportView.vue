<script setup>
import { computed, nextTick, onBeforeUnmount, onMounted, ref, watch } from 'vue'
import { ElMessage } from 'element-plus'
import { listChat, sendChat } from '../services/lynxDb'
import lingxiLogo from '../../../picturee/LOGO.png'

const DRAFT_KEY = 'lynxtrip.draft.support.v1'

const input = ref('')
const list = ref([])
const feedRef = ref(null)
const isGenerating = ref(false)
let typingTimer = null

function refresh() {
  list.value = listChat()
}

onMounted(async () => {
  refresh()
  try {
    const raw = sessionStorage.getItem(DRAFT_KEY)
    if (raw) input.value = String(raw)
  } catch {
    // ignore
  }
  await nextTick()
  scrollToBottom()
})

const normalized = computed(() =>
  list.value.map((m) => ({
    id: m.id,
    ts: m.addtime,
    from: m.ask ? 'user' : 'agent',
    text: m.ask ?? m.reply ?? '',
  })),
)

function scrollToBottom() {
  const el = feedRef.value
  if (!el) return
  el.scrollTop = el.scrollHeight
}

watch(
  input,
  () => {
    try {
      sessionStorage.setItem(DRAFT_KEY, input.value || '')
    } catch {
      // ignore
    }
  },
  { flush: 'post' },
)

async function submit() {
  const text = input.value.trim()
  if (!text) return
  if (isGenerating.value) return

  input.value = ''
  isGenerating.value = true
  sendChat({ ask: text })
  refresh()
  await nextTick()
  scrollToBottom()
  ElMessage.success('已发送')

  // 简短“生成态”模拟（跟问答页一致的停止按钮逻辑）
  typingTimer = setTimeout(async () => {
    isGenerating.value = false
    refresh()
    await nextTick()
    scrollToBottom()
  }, 420)
}

function stop(silent = false) {
  if (typingTimer) clearTimeout(typingTimer)
  typingTimer = null
  isGenerating.value = false
  if (!silent) ElMessage.info('已停止')
}

function onKeydown(e) {
  if (e.key === 'Enter' && !e.shiftKey) {
    e.preventDefault()
    submit()
  }
}

onBeforeUnmount(() => {
  // 切换页面时静默停止，不弹提示
  stop(true)
})
</script>

<template>
  <section class="lx-page">
    <div class="lx-shell lynx-card lynx-card--glass">
      <header class="lx-topbar">
        <div class="lx-topbar__left">
          <div class="lx-title">在线客服</div>
        </div>
        <div class="lx-topbar__right">
          <el-button size="small" :disabled="!normalized.length" @click="() => (input = '')">清空输入</el-button>
        </div>
      </header>

      <main class="lx-main">
        <div class="lx-chat" ref="feedRef">
          <div v-if="!normalized.length" class="lx-welcome">
            <div class="lx-avatar">
              <div class="lx-avatar__ring" :style="{ backgroundImage: `url(${lingxiLogo})` }" />
            </div>
            <h1 class="lx-welcome__title">你好，我是客服小灵</h1>
            <p class="lx-welcome__desc">我会尽快帮你处理订单、路线与账户问题。</p>

            <div class="lx-suggest">
              <button class="lx-cardBtn" type="button" @click="input = '我想咨询线路推荐与出行时间建议。'">
                <div class="lx-cardBtn__icon is-v1" aria-hidden="true">
                  <svg class="lx-cardBtn__svg" viewBox="0 0 24 24">
                    <path fill="none" stroke="currentColor" stroke-linecap="round" stroke-linejoin="round" stroke-width="1.8" d="M4 6h16M7 12h10M10 18h4" />
                  </svg>
                </div>
                <div class="lx-cardBtn__title">线路咨询</div>
                <div class="lx-cardBtn__desc">帮我看看哪条线路更适合</div>
              </button>
              <button class="lx-cardBtn" type="button" @click="input = '我的订单状态/支付/退款怎么处理？'">
                <div class="lx-cardBtn__icon is-v2" aria-hidden="true">
                  <svg class="lx-cardBtn__svg" viewBox="0 0 24 24">
                    <path fill="none" stroke="currentColor" stroke-linecap="round" stroke-linejoin="round" stroke-width="1.8" d="M6 4h12v16H6zM8 8h8M8 12h6M8 16h5" />
                  </svg>
                </div>
                <div class="lx-cardBtn__title">订单问题</div>
                <div class="lx-cardBtn__desc">状态、支付、退款与发票</div>
              </button>
              <button class="lx-cardBtn" type="button" @click="input = '我的账号信息/地址/余额怎么修改？'">
                <div class="lx-cardBtn__icon is-v3" aria-hidden="true">
                  <svg class="lx-cardBtn__svg" viewBox="0 0 24 24">
                    <path
                      fill="none"
                      stroke="currentColor"
                      stroke-linecap="round"
                      stroke-linejoin="round"
                      stroke-width="1.8"
                      d="M12 12a4 4 0 1 0-4-4a4 4 0 0 0 4 4Zm-7 9a7 7 0 0 1 14 0"
                    />
                  </svg>
                </div>
                <div class="lx-cardBtn__title">账户与地址</div>
                <div class="lx-cardBtn__desc">个人中心相关问题</div>
              </button>
            </div>
          </div>

          <div v-else class="lx-list">
            <div v-for="m in normalized" :key="m.id" class="lx-msg" :class="m.from === 'user' ? 'is-user' : 'is-assistant'">
              <div class="lx-msg__avatar" aria-hidden="true">
                <span v-if="m.from !== 'user'">灵</span>
                <span v-else>我</span>
              </div>
              <div class="lx-msg__content">
                <div v-if="m.from !== 'user'" class="lx-bubble lx-bubble--ai">{{ m.text }}</div>
                <div v-else class="lx-bubble lx-bubble--user">{{ m.text }}</div>
                <div class="lx-ts">{{ m.ts }}</div>
              </div>
            </div>
          </div>
        </div>

        <footer class="lx-composer">
          <div class="lx-inputRow">
            <textarea
              v-model="input"
              class="lx-input"
              rows="1"
              placeholder="发送消息给客服...（Enter 发送，Shift+Enter 换行）"
              @keydown="onKeydown"
            />
            <div class="lx-actions">
              <button v-if="!isGenerating" class="lx-send" type="button" @click="submit">发送</button>
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
  font-size: 40px;
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
  width: min(820px, 100%);
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
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
  white-space: pre-wrap;
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
.lx-actions {
  display: flex;
  align-items: center;
  gap: 8px;
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
  transition: transform 0.15s ease, box-shadow 0.15s ease;
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

@media (max-width: 980px) {
  .lx-chat,
  .lx-composer {
    padding-left: 4%;
    padding-right: 4%;
  }
  .lx-suggest {
    grid-template-columns: 1fr;
  }
  .lx-welcome__title {
    font-size: 34px;
  }
}
</style>

