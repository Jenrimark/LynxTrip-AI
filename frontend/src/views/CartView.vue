<script setup>
import { computed, onMounted, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  listCart,
  listAddresses,
  updateCartQuantity,
  removeCartItem,
  checkout,
  addAddress,
  setDefaultAddress,
} from '../services/lynxDb'

const cart = ref([])
const addresses = ref([])
const addressId = ref(null)
const isAddAddrOpen = ref(false)
const addrForm = ref({ name: '', phone: '', address: '', isdefault: '是' })

function refresh() {
  cart.value = listCart()
  addresses.value = listAddresses()
  const def = addresses.value.find((a) => a.isdefault === '是')
  addressId.value = def?.id ?? addresses.value[0]?.id ?? null
}

onMounted(refresh)

const subtotal = computed(() => cart.value.reduce((sum, r) => sum + Number(r.price || 0) * Number(r.buynumber || 1), 0))

const selectedAddressText = computed(() => {
  const row = addresses.value.find((a) => Number(a.id) === Number(addressId.value))
  if (!row) return ''
  return `${row.address}（${row.name} ${row.phone}）`
})

async function changeQty(row, n) {
  updateCartQuantity(row.id, n)
  refresh()
}

async function remove(row) {
  await ElMessageBox.confirm('确认移除该商品吗？', '提示', { type: 'warning' })
  removeCartItem(row.id)
  refresh()
  ElMessage.success('已移除')
}

function doCheckout() {
  if (!cart.value.length) {
    ElMessage.warning('购物车为空')
    return
  }
  if (!selectedAddressText.value) {
    ElMessage.warning('请先选择/新增收货地址')
    return
  }
  checkout({ addressText: selectedAddressText.value })
  refresh()
  ElMessage.success('已生成订单（orders）')
}

function openAddAddr() {
  isAddAddrOpen.value = true
}

function saveAddr() {
  if (!addrForm.value.name || !addrForm.value.phone || !addrForm.value.address) {
    ElMessage.warning('请填写完整：收货人/电话/地址')
    return
  }
  addAddress({ ...addrForm.value })
  isAddAddrOpen.value = false
  addrForm.value = { name: '', phone: '', address: '', isdefault: '是' }
  refresh()
  ElMessage.success('地址已保存')
}

function chooseDefault(id) {
  setDefaultAddress(id)
  refresh()
  ElMessage.success('已设为默认地址')
}
</script>

<template>
  <section class="page">
    <header class="hero lynx-card">
      <div class="hero__left">
        <h2 class="hero__title lynx-h">购物车</h2>
      </div>
      <div class="hero__right">
        <div class="hero__metric">
          <div class="hero__metricNum">¥ {{ subtotal.toFixed(2) }}</div>
          <div class="hero__metricLabel">当前合计</div>
        </div>
      </div>
    </header>

    <div class="layout">
      <div class="main">
        <div class="panel lynx-card lynx-card--glass">
          <div class="panel__hd">
            <div class="panel__title">商品清单</div>
            <div>{{ cart.length }} 件</div>
          </div>

          <div v-if="!cart.length" class="empty">
            <el-empty description="购物车为空，去「旅游路线」挑选线路加入购物车吧" />
          </div>

          <div v-else class="rows">
            <div v-for="row in cart" :key="row.id" class="row">
              <el-image class="row__img" :src="row.picture" fit="cover" :alt="row.goodname" />
              <div class="row__info">
                <div class="row__name">{{ row.goodname }}</div>
                <div class="row__meta">单价：¥ {{ Number(row.price || 0).toFixed(2) }}</div>
                <div class="row__meta">表：{{ row.tablename }} · ID：{{ row.goodid }}</div>
              </div>
              <div class="row__qty">
                <el-input-number :min="1" :model-value="row.buynumber" @update:model-value="(v) => changeQty(row, v)" />
              </div>
              <div class="row__sum">¥ {{ (Number(row.price || 0) * Number(row.buynumber || 1)).toFixed(2) }}</div>
              <div class="row__act">
                <el-button type="danger" plain @click="remove(row)">移除</el-button>
              </div>
            </div>
          </div>
        </div>
      </div>

      <aside class="side">
        <div class="panel lynx-card lynx-card--glass">
          <div class="panel__hd">
            <div class="panel__title">收货地址</div>
            <el-button size="small" type="primary" @click="openAddAddr">新增</el-button>
          </div>

          <div v-if="!addresses.length" class="side__empty">暂无地址，请先新增。</div>
          <div v-else class="addrList">
            <button
              v-for="a in addresses"
              :key="a.id"
              class="addr"
              type="button"
              :class="{ 'is-active': Number(a.id) === Number(addressId) }"
              @click="addressId = a.id"
            >
              <div class="addr__top">
                <span class="addr__name">{{ a.name }}</span>
                <span class="addr__phone">{{ a.phone }}</span>
                <span v-if="a.isdefault === '是'" class="tag">默认</span>
              </div>
              <div class="addr__text">{{ a.address }}</div>
              <div class="addr__bottom">
                <span class="addr__id">#{{ a.id }}</span>
                <el-button size="small" text type="primary" @click.stop="chooseDefault(a.id)">设为默认</el-button>
              </div>
            </button>
          </div>
        </div>

        <div class="panel lynx-card">
          <div class="pay">
            <div class="pay__row">
              <span class="pay__label">合计</span>
              <span class="pay__value">¥ {{ subtotal.toFixed(2) }}</span>
            </div>
            <div class="pay__row pay__muted">
              <span class="pay__label">地址</span>
              <span class="pay__value pay__addr">{{ selectedAddressText || '未选择' }}</span>
            </div>
            <el-button class="pay__btn" type="primary" size="large" :disabled="!cart.length" @click="doCheckout">去结算</el-button>
            <div class="pay__tip">结算后会写入 `orders` 表结构，并清空当前用户的 `cart`。</div>
          </div>
        </div>
      </aside>
    </div>

    <el-dialog v-model="isAddAddrOpen" title="新增地址" width="560px">
      <el-form label-position="top">
        <div class="dialogRow">
          <el-form-item label="收货人（name）">
            <el-input v-model="addrForm.name" />
          </el-form-item>
          <el-form-item label="电话（phone）">
            <el-input v-model="addrForm.phone" />
          </el-form-item>
        </div>
        <el-form-item label="地址（address）">
          <el-input v-model="addrForm.address" />
        </el-form-item>
        <el-form-item label="是否默认（isdefault）">
          <el-radio-group v-model="addrForm.isdefault">
            <el-radio label="是">是</el-radio>
            <el-radio label="否">否</el-radio>
          </el-radio-group>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="isAddAddrOpen = false">取消</el-button>
        <el-button type="primary" @click="saveAddr">保存</el-button>
      </template>
    </el-dialog>
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
  background: radial-gradient(920px 320px at 20% 10%, rgba(249, 115, 22, 0.18), transparent 60%),
    radial-gradient(920px 320px at 82% 28%, rgba(56, 189, 248, 0.14), transparent 60%),
    rgba(255, 255, 255, 0.92);
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 18px;
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
  max-width: 62ch;
  font-size: 13px;
}
.hero__metric {
  min-width: 160px;
  text-align: right;
}
.hero__metricNum {
  font-size: 22px;
  font-weight: 900;
  color: #0f172a;
}
.hero__metricLabel {
  font-size: 12px;
  color: var(--lynx-muted);
}

.layout {
  display: grid;
  grid-template-columns: 1fr 380px;
  gap: 14px;
  align-items: start;
}

.panel {
  border-radius: 18px;
  padding: 12px;
}
.panel__hd {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 10px;
  padding: 4px 4px 12px 4px;
}
.panel__title {
  font-weight: 900;
  color: #0f172a;
}
.panel__hint {
  font-size: 12px;
  color: #64748b;
}

.rows {
  display: flex;
  flex-direction: column;
  gap: 10px;
}
.row {
  border: 1px solid rgba(15, 23, 42, 0.08);
  background: rgba(255, 255, 255, 0.86);
  border-radius: 16px;
  padding: 10px;
  display: grid;
  grid-template-columns: 84px 1fr 140px 120px 90px;
  gap: 10px;
  align-items: center;
}
.row__img {
  width: 84px;
  height: 64px;
  border-radius: 14px;
}
.row__info {
  min-width: 0;
}
.row__name {
  font-weight: 900;
  color: #0f172a;
  line-height: 1.25;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
}
.row__meta {
  margin-top: 6px;
  font-size: 12px;
  color: #64748b;
}
.row__sum {
  text-align: right;
  font-weight: 900;
  color: #0f172a;
}
.row__act {
  display: flex;
  justify-content: flex-end;
}

.side {
  display: flex;
  flex-direction: column;
  gap: 14px;
  position: sticky;
  top: calc(56px + var(--space-lg));
}

.side__empty {
  padding: 10px 6px;
  color: #64748b;
  font-size: 13px;
}

.addrList {
  display: flex;
  flex-direction: column;
  gap: 10px;
}
.addr {
  text-align: left;
  border: 1px solid rgba(15, 23, 42, 0.08);
  background: rgba(255, 255, 255, 0.86);
  border-radius: 16px;
  padding: 10px;
  cursor: pointer;
  transition: border-color 180ms ease, box-shadow 180ms ease, transform 180ms ease;
}
.addr:hover {
  transform: translateY(-1px);
  border-color: rgba(255, 136, 57, 0.35);
  box-shadow: 0 10px 22px rgba(15, 23, 42, 0.06);
}
.addr.is-active {
  border-color: rgba(249, 115, 22, 0.35);
  box-shadow: 0 0 0 3px rgba(249, 115, 22, 0.12);
}
.addr__top {
  display: flex;
  align-items: center;
  gap: 8px;
  flex-wrap: wrap;
}
.addr__name {
  font-weight: 900;
  color: #0f172a;
}
.addr__phone {
  color: #334155;
  font-size: 12px;
  font-weight: 700;
}
.tag {
  margin-left: auto;
  font-size: 12px;
  font-weight: 900;
  color: #7c2d12;
  border: 1px solid rgba(249, 115, 22, 0.22);
  background: rgba(255, 255, 255, 0.9);
  border-radius: 999px;
  padding: 2px 10px;
}
.addr__text {
  margin-top: 8px;
  color: #475569;
  line-height: 1.6;
  font-size: 13px;
}
.addr__bottom {
  margin-top: 10px;
  display: flex;
  align-items: center;
  justify-content: space-between;
}
.addr__id {
  font-size: 12px;
  color: #94a3b8;
}

.pay {
  display: flex;
  flex-direction: column;
  gap: 10px;
}
.pay__row {
  display: flex;
  align-items: baseline;
  justify-content: space-between;
  gap: 10px;
}
.pay__label {
  color: #475569;
  font-size: 13px;
  font-weight: 700;
}
.pay__value {
  color: #0f172a;
  font-weight: 900;
}
.pay__muted .pay__value {
  color: #334155;
  font-weight: 800;
  font-size: 12px;
}
.pay__addr {
  text-align: right;
  max-width: 240px;
  line-height: 1.5;
}
.pay__btn {
  margin-top: 6px;
}
.pay__tip {
  color: #64748b;
  font-size: 12px;
  line-height: 1.6;
}

.dialogRow {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 10px;
}

@media (max-width: 1200px) {
  .layout {
    grid-template-columns: 1fr;
  }
  .side {
    position: relative;
    top: 0;
  }
  .row {
    grid-template-columns: 84px 1fr;
  }
  .row__qty,
  .row__sum,
  .row__act {
    grid-column: 1 / -1;
    display: flex;
    justify-content: space-between;
    align-items: center;
  }
}
@media (max-width: 720px) {
  .dialogRow {
    grid-template-columns: 1fr;
  }
}
</style>

