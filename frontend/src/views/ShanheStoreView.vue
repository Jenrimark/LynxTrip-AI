<script setup>
import { onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { listRoutes } from '../services/lynxDb'

const router = useRouter()

const goods = ref([])

onMounted(async () => {
  const [a, b] = await Promise.all([listRoutes('lvyouxianlu'), listRoutes('zuixinxianlu')])
  goods.value = [...a, ...b].slice(0, 8)
})

function goRoutes() {
  router.push({ name: 'routes' })
}

function goCart() {
  router.push({ name: 'cart' })
}
</script>

<template>
  <section class="page">
    <header class="hero lynx-card">
      <div>
        <h2 class="hero__title lynx-h">山河印记</h2>
        <p class="hero__desc">这里售卖两类内容：特产/特色服务/周边，以及「主题简旅」旅行方案。</p>
      </div>
      <div class="hero__actions">
        <el-button type="primary" @click="goRoutes">去选主题简旅</el-button>
        <el-button plain @click="goCart">查看购物车</el-button>
      </div>
    </header>

    <section class="grid">
      <article v-for="item in goods" :key="item.id" class="card lynx-card lynx-card--glass" role="button" tabindex="0" @click="goRoutes" @keydown.enter.prevent="goRoutes">
        <el-image class="card__img" :src="item.fengmiantu" fit="cover" :alt="item.xianlumingcheng" />
        <div class="card__body">
          <div class="card__name">{{ item.xianlumingcheng }}</div>
          <div class="card__meta">{{ item.chufadi || '—' }} → {{ item.mudedi || '—' }}</div>
          <div class="card__price">¥{{ Number(item.price || 0).toFixed(0) }}</div>
        </div>
      </article>
    </section>
  </section>
</template>

<style scoped lang="scss">
.page {
  display: flex;
  flex-direction: column;
  gap: 14px;
}
.hero {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 12px;
  padding: 16px;
}
.hero__title {
  margin: 0;
}
.hero__desc {
  margin: 8px 0 0;
  color: #475569;
  font-size: 13px;
}
.hero__actions {
  display: flex;
  gap: 10px;
}
.grid {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 12px;
}
.card__img {
  width: 100%;
  height: 140px;
}
.card__body {
  padding: 10px 12px 12px;
}
.card__name {
  color: #0f172a;
  font-weight: 800;
}
.card__meta {
  margin-top: 6px;
  color: #64748b;
  font-size: 12px;
}
.card__price {
  margin-top: 8px;
  color: #c2410c;
  font-weight: 900;
}
@media (max-width: 1100px) {
  .grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
}
@media (max-width: 700px) {
  .hero {
    flex-direction: column;
    align-items: flex-start;
  }
  .grid {
    grid-template-columns: 1fr;
  }
}
</style>

