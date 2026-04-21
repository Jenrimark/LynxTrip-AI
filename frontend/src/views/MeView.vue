<script setup>
import { computed, onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { fetchMe, getErrorMessage, isLoggedInRemote, logout, updatePassword as updatePasswordRemote } from '../services/auth'
import {
  listOrders,
  listAddresses,
  listStoreup,
  addAddress,
  setDefaultAddress,
  updateOrderStatus,
  removeOrder,
  formatUserIdDisplay,
  deleteUserAccount,
} from '../services/lynxDb'

const router = useRouter()
const user = ref(null)
const orders = ref([])
const addresses = ref([])
const storeups = ref([])

const isAddAddrOpen = ref(false)
const addrForm = ref({ name: '', phone: '', address: '', isdefault: '否' })
const activeOrderTab = ref('all')

const orderTabs = [
  { key: 'all', label: '全部' },
  { key: '未支付', label: '未支付' },
  { key: '已支付', label: '已支付' },
  { key: '已发货', label: '已发货' },
  { key: '已完成', label: '已完成' },
  { key: '已退款', label: '已退款' },
  { key: '已取消', label: '已取消' },
]

async function refresh() {
  try {
    orders.value = await listOrders()
    addresses.value = await listAddresses()
    storeups.value = await listStoreup()
  } catch {
    orders.value = []
    addresses.value = []
    storeups.value = []
  }
}

const userIdDisplay = computed(() => formatUserIdDisplay(user.value?.id))

onMounted(async () => {
  if (!(await isLoggedInRemote())) {
    router.replace({ name: 'login', query: { redirect: '/me' } })
    return
  }
  user.value = await fetchMe(true)
  await refresh()
})

const moneyText = computed(() => {
  const m = Number(user.value?.money || 0)
  return m.toFixed(2)
})

const orderTabCount = computed(() =>
  orderTabs.reduce((acc, tab) => {
    acc[tab.key] = tab.key === 'all' ? orders.value.length : orders.value.filter((o) => String(o.status) === tab.key).length
    return acc
  }, {})
)

const filteredOrders = computed(() => {
  if (activeOrderTab.value === 'all') return orders.value
  return orders.value.filter((o) => String(o.status) === activeOrderTab.value)
})

function formatMoney(v) {
  return Number(v || 0).toFixed(2)
}

function getOrderActions(status) {
  if (status === '未支付') return ['去支付', '取消订单']
  if (status === '已支付') return ['申请退款', '联系客服']
  if (status === '已发货') return ['查看物流', '确认收货']
  if (status === '已完成') return ['再次购买']
  if (status === '已退款') return ['查看退款详情', '再次购买']
  if (status === '已取消') return ['删除记录', '再次购买']
  return ['联系客服']
}

async function handleOrderAction(order, action) {
  const cur = Number(user.value?.id ?? 0)
  if (action === '去支付') {
    await updateOrderStatus(order.id, '已支付', cur)
    ElMessage.success('订单已支付')
    refresh()
    return
  }
  if (action === '取消订单') {
    await updateOrderStatus(order.id, '已取消', cur)
    ElMessage.success('订单已取消')
    refresh()
    return
  }
  if (action === '申请退款') {
    await updateOrderStatus(order.id, '已退款', cur)
    ElMessage.success('退款申请已提交')
    refresh()
    return
  }
  if (action === '确认收货') {
    await updateOrderStatus(order.id, '已完成', cur)
    ElMessage.success('已确认收货')
    refresh()
    return
  }
  if (action === '删除记录') {
    await removeOrder(order.id, cur)
    ElMessage.success('订单记录已删除')
    refresh()
    return
  }
  ElMessage.info(`${action}功能待接入`)
}

function openAddAddr() {
  isAddAddrOpen.value = true
}

async function saveAddr() {
  if (!addrForm.value.name || !addrForm.value.phone || !addrForm.value.address) {
    ElMessage.warning('请填写完整：收货人/电话/地址')
    return
  }
  await addAddress({ userId: Number(user.value?.id ?? 0), ...addrForm.value })
  isAddAddrOpen.value = false
  addrForm.value = { name: '', phone: '', address: '', isdefault: '否' }
  await refresh()
  ElMessage.success('地址已保存')
}

async function chooseDefault(id) {
  await setDefaultAddress(id, Number(user.value?.id ?? 0))
  await refresh()
  ElMessage.success('已设为默认地址')
}

const isPwdOpen = ref(false)
const pwdForm = ref({ oldMima: '', newMima: '', newMima2: '' })

function openPwd() {
  pwdForm.value = { oldMima: '', newMima: '', newMima2: '' }
  isPwdOpen.value = true
}

async function savePwd() {
  if (pwdForm.value.newMima !== pwdForm.value.newMima2) {
    ElMessage.warning('两次新密码不一致')
    return
  }
  try {
    await updatePasswordRemote(pwdForm.value.oldMima, pwdForm.value.newMima)
  } catch (err) {
    ElMessage.error(getErrorMessage(err, '原密码错误或新密码无效'))
    return
  }
  isPwdOpen.value = false
  ElMessage.success('密码已修改')
}

function resetPwdForm() {
  pwdForm.value = { oldMima: '', newMima: '', newMima2: '' }
}

async function handleLogoutSession() {
  await logout()
  ElMessage.success('已退出登录')
  router.replace({ name: 'login', query: { redirect: '/me' } })
}

async function handleLogoutAccount() {
  const id = Number(user.value?.id ?? 0)
  if (id === 0) {
    ElMessage.warning('管理员账号不可注销')
    return
  }
  try {
    await ElMessageBox.confirm(
      '注销后将清空该账号在本地的订单、地址、收藏等数据，用户ID会回收，下次注册可复用该号码。是否继续？',
      '确认注销',
      { type: 'warning', confirmButtonText: '注销', cancelButtonText: '取消' }
    )
  } catch {
    return
  }
  await deleteUserAccount(id)
  ElMessage.success('账号已注销')
  router.replace({ name: 'login', query: { redirect: '/me' } })
}
</script>

<template>
  <section class="page">
    <header class="hero lynx-card">
      <div class="hero__left">
        <h2 class="hero__title lynx-h">个人中心</h2>
      </div>
    </header>

    <div class="top">
      <div class="profile lynx-card lynx-card--glass profile--wide">
        <div class="profile__top">
          <div class="profile__intro">
            <el-avatar class="profile__avatar" :size="88" :src="user?.touxiang" />
            <div class="profile__introText">
              <div class="profile__name">{{ user?.xingming || '未命名用户' }}</div>
            </div>
          </div>
          <div class="profile__actions">
            <el-button @click="openPwd">修改密码</el-button>
            <el-button type="primary" plain @click="handleLogoutSession">退出登录</el-button>
            <el-button type="danger" plain @click="handleLogoutAccount">注销账号</el-button>
          </div>
        </div>

        <div class="profile__grid">
          <div class="kv">
            <span class="kv__k">用户ID</span>
            <span class="kv__v kv__v--id">{{ userIdDisplay }}</span>
          </div>
          <div class="kv">
            <span class="kv__k">性别</span>
            <span class="kv__v">{{ user?.xingbie || '—' }}</span>
          </div>
          <div class="kv">
            <span class="kv__k">用户名</span>
            <span class="kv__v">{{ user?.yonghuming || '—' }}</span>
          </div>
          <div class="kv">
            <span class="kv__k">实名认证</span>
            <span class="kv__v">{{ user?.shimingrenzheng || '未认证' }}</span>
          </div>
          <div class="kv">
            <span class="kv__k">电话</span>
            <span class="kv__v">{{ user?.lianxidianhua || '—' }}</span>
          </div>
          <div class="kv">
            <span class="kv__k">余额</span>
            <span class="kv__v kv__v--money">¥ {{ moneyText }}</span>
          </div>
        </div>
      </div>
    </div>

    <div class="layout">
      <div class="panel lynx-card lynx-card--glass">
        <div class="panel__hd">
          <div class="panel__title">我的订单</div>
        </div>

        <div class="orderTabs">
          <button
            v-for="tab in orderTabs"
            :key="tab.key"
            type="button"
            class="orderTab"
            :class="{ 'orderTab--active': activeOrderTab === tab.key }"
            @click="activeOrderTab = tab.key"
          >
            {{ tab.label }}（{{ orderTabCount[tab.key] || 0 }}）
          </button>
        </div>

        <div v-if="!filteredOrders.length" class="panel__empty">
          <el-empty description="暂无订单。可去「购物车」结算生成 orders。" />
        </div>

        <div v-else class="orderList">
          <div v-for="o in filteredOrders" :key="o.id" class="order">
            <el-image class="order__img" :src="o.picture" fit="cover" :alt="o.goodname" />
            <div class="order__info">
              <div class="order__head">
                <div class="order__meta">订单编号：{{ o.orderid || '—' }}</div>
                <span class="status">{{ o.status || '—' }}</span>
              </div>
              <div class="order__name">商品：{{ o.goodname }}</div>
              <div class="order__grid">
                <div class="order__meta">价格：¥ {{ formatMoney(o.price) }}</div>
                <div class="order__meta">数量：{{ Number(o.buynumber || 1) }}</div>
                <div class="order__meta">总价：¥ {{ formatMoney(o.total) }}</div>
              </div>
              <div class="order__meta">地址：{{ o.address || '—' }}</div>
              <div class="order__actions">
                <el-button
                  v-for="action in getOrderActions(o.status)"
                  :key="`${o.id}-${action}`"
                  size="small"
                  :type="action === '去支付' || action === '确认收货' ? 'primary' : undefined"
                  @click="handleOrderAction(o, action)"
                >
                  {{ action }}
                </el-button>
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
          <div v-if="!addresses.length" class="side__empty">暂无地址。</div>
          <div v-else class="addrList">
            <div v-for="a in addresses" :key="a.id" class="addr">
              <div class="addr__top">
                <div class="addr__name">{{ a.name }}</div>
                <span v-if="a.isdefault === '是'" class="tag">默认</span>
              </div>
              <div class="addr__meta">{{ a.phone }}</div>
              <div class="addr__text">{{ a.address }}</div>
              <div class="addr__bottom">
                <span class="addr__id">#{{ a.id }}</span>
                <el-button size="small" text type="primary" @click="chooseDefault(a.id)">设为默认</el-button>
              </div>
            </div>
          </div>
        </div>

        <div class="panel lynx-card lynx-card--glass">
          <div class="panel__hd">
            <div class="panel__title">我的收藏</div>
          </div>
          <div v-if="!storeups.length" class="side__empty">暂无收藏。</div>
          <div v-else class="favList">
            <div v-for="s in storeups.slice(0, 6)" :key="s.id" class="fav">
              <el-image class="fav__img" :src="s.picture" fit="cover" :alt="s.name" />
              <div class="fav__info">
                <div class="fav__name">{{ s.name }}</div>
                <div class="fav__meta">{{ s.tablename }} #{{ s.refid }}</div>
              </div>
            </div>
          </div>
        </div>
      </aside>
    </div>

    <el-dialog v-model="isPwdOpen" title="修改密码" width="480px" @closed="resetPwdForm">
      <el-form label-position="top">
        <el-form-item label="当前密码">
          <el-input v-model="pwdForm.oldMima" type="password" show-password autocomplete="off" />
        </el-form-item>
        <el-form-item label="新密码">
          <el-input v-model="pwdForm.newMima" type="password" show-password autocomplete="new-password" />
        </el-form-item>
        <el-form-item label="确认新密码">
          <el-input v-model="pwdForm.newMima2" type="password" show-password autocomplete="new-password" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="isPwdOpen = false">取消</el-button>
        <el-button type="primary" @click="savePwd">保存</el-button>
      </template>
    </el-dialog>

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
  background: radial-gradient(920px 320px at 20% 10%, rgba(249, 115, 22, 0.16), transparent 60%),
    radial-gradient(920px 320px at 82% 28%, rgba(14, 165, 233, 0.14), transparent 60%),
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
  max-width: 70ch;
  font-size: 13px;
}
.hero__switchLabel {
  font-size: 12px;
  color: #64748b;
}
.top {
  display: block;
}

.profile {
  border-radius: 18px;
  padding: 16px;
}
.profile--wide {
  display: flex;
  flex-direction: column;
  gap: 18px;
}
.profile__top {
  display: flex;
  flex-wrap: wrap;
  align-items: flex-start;
  justify-content: space-between;
  gap: 16px;
}
.profile__intro {
  display: flex;
  gap: 14px;
  align-items: center;
  flex: 1;
  min-width: min(100%, 280px);
}
.profile__actions {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  justify-content: flex-end;
  align-items: center;
}
.profile__avatar {
  flex-shrink: 0;
}
.profile__introText {
  min-width: 0;
}
.profile__name {
  font-weight: 900;
  color: #0f172a;
  font-size: 18px;
}
.profile__sub {
  margin-top: 6px;
  font-size: 13px;
  color: #64748b;
  line-height: 1.45;
}
.profile__grid {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 10px 12px;
  align-content: start;
}
.kv {
  display: flex;
  flex-direction: column;
  gap: 4px;
  padding: 10px 12px;
  border-radius: 14px;
  border: 1px solid rgba(15, 23, 42, 0.08);
  background: rgba(255, 255, 255, 0.82);
}
.kv__k {
  font-size: 11px;
  font-weight: 800;
  color: #64748b;
  text-transform: uppercase;
  letter-spacing: 0.02em;
}
.kv__v {
  font-size: 14px;
  font-weight: 800;
  color: #0f172a;
  word-break: break-word;
}
.kv__v--money {
  font-size: 20px;
  font-weight: 900;
  color: #c2410c;
}
.kv__v--id {
  font-variant-numeric: tabular-nums;
  letter-spacing: 0.06em;
  font-size: 15px;
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
  line-height: 1.5;
  text-align: right;
}
.panel__empty {
  padding: 10px;
}

.orderTabs {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  margin-bottom: 10px;
}
.orderTab {
  border: 1px solid rgba(15, 23, 42, 0.1);
  background: rgba(255, 255, 255, 0.82);
  color: #334155;
  border-radius: 999px;
  padding: 6px 12px;
  font-size: 12px;
  font-weight: 700;
  cursor: pointer;
  transition: all 0.2s ease;
}
.orderTab:hover {
  border-color: rgba(249, 115, 22, 0.35);
  color: #7c2d12;
}
.orderTab--active {
  color: #7c2d12;
  border-color: rgba(249, 115, 22, 0.35);
  background: rgba(255, 247, 237, 0.9);
}

.orderList {
  display: flex;
  flex-direction: column;
  gap: 10px;
}
.order {
  border: 1px solid rgba(15, 23, 42, 0.08);
  background: rgba(255, 255, 255, 0.86);
  border-radius: 16px;
  padding: 10px;
  display: grid;
  grid-template-columns: 84px 1fr;
  gap: 10px;
  align-items: center;
}
.order__img {
  width: 84px;
  height: 64px;
  border-radius: 14px;
}
.order__info {
  min-width: 0;
}
.order__head {
  display: flex;
  justify-content: space-between;
  gap: 10px;
  align-items: center;
}
.order__name {
  margin-top: 6px;
  font-weight: 800;
  color: #0f172a;
  line-height: 1.25;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
}
.order__meta {
  font-size: 12px;
  color: #64748b;
  line-height: 1.5;
}
.order__grid {
  margin-top: 6px;
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 8px;
}
.order__actions {
  margin-top: 10px;
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}
.status {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  height: 28px;
  padding: 0 10px;
  border-radius: 999px;
  font-size: 12px;
  font-weight: 900;
  color: #7c2d12;
  border: 1px solid rgba(249, 115, 22, 0.22);
  background: rgba(255, 255, 255, 0.92);
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
  border: 1px solid rgba(15, 23, 42, 0.08);
  background: rgba(255, 255, 255, 0.86);
  border-radius: 16px;
  padding: 10px;
}
.addr__top {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 10px;
}
.addr__name {
  font-weight: 900;
  color: #0f172a;
}
.tag {
  font-size: 12px;
  font-weight: 900;
  color: #7c2d12;
  border: 1px solid rgba(249, 115, 22, 0.22);
  background: rgba(255, 255, 255, 0.92);
  border-radius: 999px;
  padding: 2px 10px;
}
.addr__meta {
  margin-top: 6px;
  font-size: 12px;
  color: #64748b;
}
.addr__text {
  margin-top: 8px;
  color: #475569;
  font-size: 13px;
  line-height: 1.6;
}
.addr__bottom {
  margin-top: 10px;
  display: flex;
  justify-content: space-between;
  align-items: center;
}
.addr__id {
  font-size: 12px;
  color: #94a3b8;
}

.favList {
  display: flex;
  flex-direction: column;
  gap: 10px;
}
.fav {
  border: 1px solid rgba(15, 23, 42, 0.08);
  background: rgba(255, 255, 255, 0.86);
  border-radius: 16px;
  padding: 10px;
  display: grid;
  grid-template-columns: 56px 1fr;
  gap: 10px;
  align-items: center;
}
.fav__img {
  width: 56px;
  height: 44px;
  border-radius: 12px;
}
.fav__name {
  font-weight: 900;
  color: #0f172a;
  line-height: 1.25;
  font-size: 13px;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
}
.fav__meta {
  margin-top: 6px;
  font-size: 12px;
  color: #64748b;
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
}
@media (max-width: 900px) {
  .profile__grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
}
@media (max-width: 520px) {
  .profile__grid {
    grid-template-columns: 1fr;
  }
}
@media (max-width: 720px) {
  .dialogRow {
    grid-template-columns: 1fr;
  }
  .order__grid {
    grid-template-columns: 1fr;
  }
}
</style>

