<script setup>
defineProps({
  title: { type: String, required: true },
})
</script>

<template>
  <div class="wrap">
    <div class="card lynx-card lynx-card--glass">
      <div class="title lynx-h">{{ title }}</div>
      <div class="desc">此页面为占位，后续接入真实内容与接口。</div>
    </div>
  </div>
</template>

<style scoped lang="scss">
.wrap {
  min-height: calc(100vh - 56px - 2 * var(--space-lg));
  display: grid;
  place-items: center;
}
.card {
  width: min(900px, 100%);
  padding: var(--space-lg);
}
.title {
  font-weight: 900;
  font-size: 20px;
}
.desc {
  margin-top: 10px;
  color: var(--lynx-muted);
  line-height: 1.7;
}
</style>

