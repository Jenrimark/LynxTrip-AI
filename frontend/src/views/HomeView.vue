<script setup>
import { ref } from 'vue'
import bgUrl from '../assets/background.png'
import sloganUrl from '../assets/signal.png'
import searchLogoUrl from '../assets/search-logo.png'

const tripDays = ref('1')
const dayOptions = ['1', '2', '3', '4', '5', '7', '10']
</script>

<template>
  <div class="home">
    <section class="home__hero" :style="{ backgroundImage: `url(${bgUrl})` }">
      <div class="home__center">
        <div class="home__stack">
          <img class="home__slogan" :src="sloganUrl" alt="灵犀旅行 Slogan" />
          <div class="home__searchCard" role="search">
            <div class="home__searchRow">
              <el-input class="home__input" placeholder="无需等待，旅途就在脚下" size="large" clearable />
            <el-select v-model="tripDays" class="home__days" placeholder="天数" size="large">
              <el-option v-for="day in dayOptions" :key="day" :label="`${day}天`" :value="day" />
            </el-select>
            <button class="home__btn" type="button" aria-label="搜索">
              <img class="home__btnIcon" :src="searchLogoUrl" alt="" aria-hidden="true" />
            </button>
            </div>
          </div>
        </div>
      </div>
    </section>
  </div>
</template>

<style scoped lang="scss">
.home {
  height: calc(100vh - 56px);
}

.home__hero {
  position: relative;
  border-radius: 0;
  overflow: hidden;
  height: 100%;
  background-size: cover;
  background-position: center;
  background-repeat: no-repeat;
}

.home__center {
  position: relative;
  height: 100%;
  display: grid;
  place-items: center;
  padding: var(--space-lg) 0;
}

.home__stack {
  width: 100%;
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 26px;
  transform: translateY(-34px);
}

.home__slogan {
  width: 100%;
  height: auto;
  object-fit: contain;
  margin-bottom: 0;
  filter: drop-shadow(0 8px 18px rgba(0, 0, 0, 0.18));
}

.home__searchCard {
  width: min(395px, 100%);
  margin-top: 8px;
  background: rgba(255, 255, 255, 0.82);
  border: 1px solid rgba(255, 255, 255, 0.7);
  border-radius: 16px;
  padding: 14px;
  backdrop-filter: blur(12px);
}

.home__searchRow {
  display: grid;
  grid-template-columns: 1fr 110px 52px;
  gap: var(--space-sm);
  align-items: center;
}

.home__days {
  width: 110px;
}

.home__btn {
  width: 52px;
  height: 40px;
  padding: 0;
  border: 0;
  background: transparent;
  border-radius: 0;
  display: grid;
  place-items: center;
  box-shadow: none;
  min-height: 40px;
  cursor: pointer;
}

.home__btn:focus-visible {
  outline: 2px solid rgba(255, 136, 57, 0.28);
  outline-offset: 2px;
}

.home__btnIcon {
  width: 28px;
  height: 28px;
  object-fit: contain;
  display: block;
}

@media (max-width: 640px) {
  .home__searchRow {
    grid-template-columns: 1fr;
  }
}
</style>

