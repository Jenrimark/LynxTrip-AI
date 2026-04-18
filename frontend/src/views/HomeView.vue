<script setup>
import { onMounted, ref } from 'vue'
import axios from 'axios'

const status = ref('未检测')
const detail = ref(null)
const loading = ref(false)

async function checkHealth() {
  loading.value = true
  status.value = '检测中...'
  detail.value = null

  try {
    const { data } = await axios.get('/api/health')
    status.value = '后端已连通'
    detail.value = data
  } catch (e) {
    status.value = '后端未连通（请先启动后端）'
    detail.value = {
      message: e?.message ?? String(e),
    }
  } finally {
    loading.value = false
  }
}

onMounted(checkHealth)
</script>

<template>
  <div class="page">
    <div class="card">
      <div class="title">LynxTrip 本地启动检查</div>

      <el-alert
        :title="status"
        type="info"
        :closable="false"
        show-icon
        class="alert"
      />

      <div class="actions">
        <el-button type="primary" :loading="loading" @click="checkHealth">
          重新检测后端
        </el-button>
      </div>

      <el-divider />

      <div class="mono">
        <pre>{{ detail }}</pre>
      </div>
    </div>
  </div>
</template>

<style scoped lang="scss">
.page {
  min-height: 100vh;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 24px;
  background: #fffaf5;
}

.card {
  width: min(920px, 100%);
  border: 1px solid #ffe3cf;
  border-radius: 16px;
  padding: 20px;
  background: #ffffff;
}

.title {
  font-size: 20px;
  font-weight: 700;
  color: #222;
  margin-bottom: 12px;
}

.alert {
  margin: 12px 0;
}

.actions {
  display: flex;
  gap: 12px;
  margin-top: 12px;
}

.mono {
  font-family: ui-monospace, SFMono-Regular, Menlo, Monaco, Consolas, 'Liberation Mono',
    'Courier New', monospace;
  font-size: 13px;
  color: #333;
  background: #fff7f1;
  border: 1px solid #ffe3cf;
  border-radius: 12px;
  padding: 12px;
  overflow: auto;
  max-height: 360px;
}

pre {
  margin: 0;
  white-space: pre-wrap;
  word-break: break-word;
}
</style>

