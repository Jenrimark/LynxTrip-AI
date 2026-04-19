<script setup>
import { computed } from 'vue'
import { listRoutes, listNews } from '../services/lynxDb'
import heroBg from '../assets/background.png'
import brandLogo from '../assets/logo2.png'

const counts = computed(() => {
  const routes = listRoutes('lvyouxianlu').length + listRoutes('zuixinxianlu').length
  const news = listNews().length
  return { routes, news }
})
</script>

<template>
  <section class="page">
    <header class="aboutHero" :style="{ backgroundImage: `url(${heroBg})` }">
      <div class="aboutHero__mask" />
      <div class="aboutHero__content">
        <div class="brand">
          <img class="brand__logo" :src="brandLogo" alt="灵犀旅行" />
          <div class="brand__text">
            <div class="brand__name">灵犀AI旅行助手</div>
            <div class="brand__tag">红色传承 · 振兴乡村 · 文化守护</div>
          </div>
        </div>
      </div>
    </header>

    <div class="layout">
      <main class="main">
        <section class="section lynx-card lynx-card--glass">
          <h2 class="section__title">我们的使命</h2>
          <div class="slogan">
            <div>让文化瑰宝走进日常！</div>
            <div>让红色精神光耀时代！！</div>
            <div>让乡土故事声声不息！！！</div>
          </div>
        </section>

        <section class="featureGrid">
          <article class="feature lynx-card lynx-card--glass is-red">
            <header class="feature__hd">
              <span class="feature__icon" aria-hidden="true">
                <svg viewBox="0 0 24 24">
                  <path
                    fill="none"
                    stroke="currentColor"
                    stroke-linecap="round"
                    stroke-linejoin="round"
                    stroke-width="1.8"
                    d="M6 21V7a2 2 0 0 1 2-2h8v16M9 9h6M9 13h6M9 17h6"
                  />
                </svg>
              </span>
              <h3 class="feature__title">红色旅游数字化</h3>
            </header>
            <ul class="feature__list">
              <li>郧阳革命旧址 AR 复原：增强现实重现历史场景</li>
              <li>智能红色研学系统：自动生成定制化党史学习路线</li>
              <li>口述史数据库：沉淀珍贵影像资料与叙事索引</li>
            </ul>
          </article>

          <article class="feature lynx-card lynx-card--glass is-green">
            <header class="feature__hd">
              <span class="feature__icon" aria-hidden="true">
                <svg viewBox="0 0 24 24">
                  <path
                    fill="none"
                    stroke="currentColor"
                    stroke-linecap="round"
                    stroke-linejoin="round"
                    stroke-width="1.8"
                    d="M12 21c5-3 8-7 8-12c-4 0-7 2-8 6c-1-4-4-6-8-6c0 5 3 9 8 12Z"
                  />
                </svg>
              </span>
              <h3 class="feature__title">振兴赋能平台</h3>
            </header>
            <ul class="feature__list">
              <li>助农电商系统：特产溯源与可信展示</li>
              <li>智慧认养农业：可视化跟踪作物生长过程</li>
              <li>乡村旅游 O2O：对接特色民宿与农家乐资源</li>
            </ul>
          </article>

          <article class="feature lynx-card lynx-card--glass is-blue">
            <header class="feature__hd">
              <span class="feature__icon" aria-hidden="true">
                <svg viewBox="0 0 24 24">
                  <path
                    fill="none"
                    stroke="currentColor"
                    stroke-linecap="round"
                    stroke-linejoin="round"
                    stroke-width="1.8"
                    d="M4 10.5V21h16V10.5M6.5 10.5V7a5.5 5.5 0 0 1 11 0v3.5M9 21v-6h6v6"
                  />
                </svg>
              </span>
              <h3 class="feature__title">遗产保护者</h3>
            </header>
            <ul class="feature__list">
              <li>古建筑数字孪生：高精度三维建模与结构档案</li>
              <li>非遗技艺教学：沉浸式互动与工艺流程拆解</li>
              <li>文化基因图谱：人物/事件/地点的关联检索</li>
            </ul>
          </article>
        </section>

        <section class="section lynx-card lynx-card--glass">
          <h2 class="section__title">核心技术支撑</h2>
          <div class="techGrid">
            <div class="tech">
              <div class="tech__media is-a" aria-hidden="true" />
              <div class="tech__name">区块链溯源</div>
              <div class="tech__desc">农产品全流程数据上链存证</div>
            </div>
            <div class="tech">
              <div class="tech__media is-b" aria-hidden="true" />
              <div class="tech__name">空间计算</div>
              <div class="tech__desc">厘米级精度 AR 场景重建</div>
            </div>
            <div class="tech">
              <div class="tech__media is-c" aria-hidden="true" />
              <div class="tech__name">智能推荐</div>
              <div class="tech__desc">多维用户画像精准匹配</div>
            </div>
          </div>
        </section>
      </main>

      <aside class="side">
        <section class="sideCard lynx-card lynx-card--glass">
          <h3 class="sideCard__title">本地景点</h3>
          <div class="pillGrid">
            <span class="pill">武当山 · 15</span>
            <span class="pill">丹江口 · 8</span>
            <span class="pill">竹山县 · 12</span>
            <span class="pill">郧阳区 · 9</span>
            <span class="pill">青龙山 · 6</span>
            <span class="pill">太极湖 · 4</span>
          </div>
        </section>

        <section class="sideCard lynx-card lynx-card--glass">
          <h3 class="sideCard__title">特色路线</h3>
          <div class="sideList">
            <div class="sideItem">武当文化三日游</div>
            <div class="sideItem">红色研学二日游</div>
          </div>
        </section>

        <section class="sideCard lynx-card lynx-card--glass">
          <h3 class="sideCard__title">特色工具</h3>
          <div class="toolGrid">
            <button class="tool" type="button">
              <span class="tool__icon" aria-hidden="true">
                <svg viewBox="0 0 24 24">
                  <path
                    fill="none"
                    stroke="currentColor"
                    stroke-linecap="round"
                    stroke-linejoin="round"
                    stroke-width="1.8"
                    d="M12 21s7-4.5 7-11a7 7 0 1 0-14 0c0 6.5 7 11 7 11Zm0-9a2 2 0 1 0 0-4a2 2 0 0 0 0 4Z"
                  />
                </svg>
              </span>
              <span class="tool__text">特产地图</span>
            </button>
            <button class="tool" type="button">
              <span class="tool__icon" aria-hidden="true">
                <svg viewBox="0 0 24 24">
                  <path
                    fill="none"
                    stroke="currentColor"
                    stroke-linecap="round"
                    stroke-linejoin="round"
                    stroke-width="1.8"
                    d="M7 20h10M9 20V9a3 3 0 0 1 6 0v11M6 9h12"
                  />
                </svg>
              </span>
              <span class="tool__text">非遗展示</span>
            </button>
          </div>
        </section>
      </aside>
    </div>
  </section>
</template>

<style scoped lang="scss">
.page {
  display: flex;
  flex-direction: column;
  gap: var(--space-lg);
}

.aboutHero {
  position: relative;
  height: 360px;
  border-radius: 18px;
  overflow: hidden;
  background-size: cover;
  background-position: center;
  background-repeat: no-repeat;
}
.aboutHero__mask {
  position: absolute;
  inset: 0;
  z-index: 1;
  pointer-events: none;
  /* 与左→右主渐变叠加顶/底压暗，避免亮部背景上白字发虚 */
  background:
    linear-gradient(180deg, rgba(2, 6, 23, 0.58) 0%, transparent 38%),
    linear-gradient(0deg, rgba(2, 6, 23, 0.48) 0%, transparent 36%),
    linear-gradient(90deg, rgba(2, 6, 23, 0.60) 0%, rgba(2, 6, 23, 0.38) 52%, rgba(2, 6, 23, 0.14) 100%);
}
.aboutHero__content {
  position: relative;
  z-index: 2;
  height: 100%;
  padding: 46px 42px;
  display: flex;
  flex-direction: column;
  justify-content: space-between;
  gap: 18px;
  color: #fff;
}
.brand {
  display: flex;
  align-items: center;
  gap: 14px;
}
.brand__logo {
  width: 64px;
  height: 64px;
  object-fit: contain;
  filter: drop-shadow(0 10px 22px rgba(0, 0, 0, 0.28));
}
.brand__name {
  font-size: 44px;
  font-weight: 900;
  letter-spacing: 0.5px;
  text-shadow: 0 10px 28px rgba(255, 136, 57, 0.22);
}
.brand__tag {
  margin-top: 10px;
  font-size: 18px;
  font-weight: 800;
  opacity: 0.92;
}

.layout {
  display: grid;
  grid-template-columns: 1fr 340px;
  gap: 14px;
  align-items: start;
}

.main {
  min-width: 0;
  display: flex;
  flex-direction: column;
  gap: 14px;
}

.section {
  border-radius: 18px;
  padding: 16px;
}
.section__title {
  margin: 0 0 12px 0;
  font-weight: 900;
  font-size: 18px;
  color: #0f172a;
}
.slogan {
  border-radius: 16px;
  border: 1px solid rgba(15, 23, 42, 0.08);
  background: rgba(255, 255, 255, 0.86);
  padding: 16px;
  font-size: 18px;
  font-weight: 900;
  color: #7c2d12;
  line-height: 1.9;
}

.featureGrid {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 14px;
}
.feature {
  border-radius: 18px;
  padding: 16px;
  overflow: hidden;
}
.feature__hd {
  display: flex;
  align-items: center;
  gap: 10px;
  margin-bottom: 10px;
}
.feature__icon {
  width: 40px;
  height: 40px;
  border-radius: 12px;
  display: grid;
  place-items: center;
  color: #fff;
}
.feature__icon svg {
  width: 22px;
  height: 22px;
}
.feature__title {
  margin: 0;
  font-size: 16px;
  font-weight: 900;
  color: #0f172a;
}
.feature__list {
  margin: 0;
  padding-left: 18px;
  color: #334155;
  line-height: 1.75;
  font-size: 13px;
}
.feature__list li + li {
  margin-top: 8px;
}
.feature.is-red .feature__icon {
  background: linear-gradient(135deg, #ef4444, #f97316);
}
.feature.is-green .feature__icon {
  background: linear-gradient(135deg, #22c55e, #06b6d4);
}
.feature.is-blue .feature__icon {
  background: linear-gradient(135deg, #60a5fa, #6366f1);
}

.techGrid {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 14px;
}
.tech {
  border: 1px solid rgba(15, 23, 42, 0.08);
  background: rgba(255, 255, 255, 0.86);
  border-radius: 16px;
  padding: 12px;
}
.tech__media {
  height: 120px;
  border-radius: 14px;
  border: 1px solid rgba(15, 23, 42, 0.08);
  background: radial-gradient(260px 120px at 20% 20%, rgba(255, 136, 57, 0.22), transparent 60%),
    radial-gradient(260px 120px at 80% 30%, rgba(56, 189, 248, 0.18), transparent 60%),
    rgba(248, 250, 252, 1);
}
.tech__media.is-a {
  background: radial-gradient(260px 120px at 20% 20%, rgba(99, 102, 241, 0.22), transparent 60%),
    radial-gradient(260px 120px at 80% 30%, rgba(249, 115, 22, 0.16), transparent 60%),
    rgba(248, 250, 252, 1);
}
.tech__media.is-b {
  background: radial-gradient(260px 120px at 20% 20%, rgba(34, 211, 238, 0.2), transparent 60%),
    radial-gradient(260px 120px at 80% 30%, rgba(34, 197, 94, 0.18), transparent 60%),
    rgba(248, 250, 252, 1);
}
.tech__media.is-c {
  background: radial-gradient(260px 120px at 20% 20%, rgba(244, 114, 182, 0.18), transparent 60%),
    radial-gradient(260px 120px at 80% 30%, rgba(96, 165, 250, 0.18), transparent 60%),
    rgba(248, 250, 252, 1);
}
.tech__name {
  margin-top: 10px;
  font-weight: 900;
  color: #0f172a;
}
.tech__desc {
  margin-top: 6px;
  font-size: 13px;
  color: #64748b;
  line-height: 1.6;
}

.side {
  position: sticky;
  top: calc(56px + var(--space-lg));
  display: flex;
  flex-direction: column;
  gap: 14px;
}
.sideCard {
  border-radius: 18px;
  padding: 14px;
}
.sideCard__title {
  margin: 0 0 12px 0;
  font-size: 15px;
  font-weight: 900;
  color: #0f172a;
}
.pillGrid {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 10px;
}
.pill {
  height: 34px;
  border-radius: 12px;
  display: grid;
  place-items: center;
  border: 1px solid rgba(15, 23, 42, 0.08);
  background: rgba(255, 255, 255, 0.86);
  color: #334155;
  font-size: 12px;
  font-weight: 800;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}
.sideList {
  display: grid;
  gap: 10px;
}
.sideItem {
  border: 1px solid rgba(15, 23, 42, 0.08);
  background: rgba(255, 255, 255, 0.86);
  border-radius: 14px;
  padding: 12px;
  font-weight: 900;
  color: #0f172a;
}
.toolGrid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 10px;
}
.tool {
  border: 1px solid rgba(15, 23, 42, 0.08);
  background: rgba(255, 255, 255, 0.86);
  border-radius: 14px;
  padding: 12px;
  display: flex;
  align-items: center;
  gap: 10px;
  cursor: pointer;
  transition: transform 180ms ease, box-shadow 180ms ease, border-color 180ms ease;
}
.tool:hover {
  transform: translateY(-1px);
  box-shadow: 0 12px 22px rgba(15, 23, 42, 0.06);
  border-color: rgba(255, 136, 57, 0.28);
}
.tool__icon {
  width: 26px;
  height: 26px;
  border-radius: 10px;
  display: grid;
  place-items: center;
  color: #7c2d12;
  background: rgba(255, 136, 57, 0.14);
}
.tool__icon svg {
  width: 18px;
  height: 18px;
}
.tool__text {
  font-weight: 900;
  color: #0f172a;
}

@media (max-width: 1180px) {
  .layout {
    grid-template-columns: 1fr;
  }
  .side {
    position: relative;
    top: 0;
  }
  .featureGrid {
    grid-template-columns: 1fr;
  }
  .techGrid {
    grid-template-columns: 1fr;
  }
}

@media (max-width: 720px) {
  .aboutHero {
    height: 320px;
  }
  .aboutHero__content {
    padding: 28px 18px;
  }
  .brand__name {
    font-size: 30px;
  }
  .brand__tag {
    font-size: 14px;
  }
  .pillGrid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
}
</style>

