import axios from 'axios'
import { fetchMe, getCachedMe } from './auth'
import { deleteAccount as deleteAccountRemote } from './auth'

export const SESSION_LOGGED_OUT = -1

function mustLoginId() {
  const me = getCachedMe()
  return Number(me?.id ?? SESSION_LOGGED_OUT)
}

async function get(url, params) {
  const { data } = await axios.get(url, { params, withCredentials: true })
  return data
}

async function post(url, body) {
  const { data } = await axios.post(url, body, { withCredentials: true })
  return data
}

async function patch(url, body) {
  const { data } = await axios.patch(url, body, { withCredentials: true })
  return data
}

async function del(url, params) {
  const { data } = await axios.delete(url, { params, withCredentials: true })
  return data
}

function normalizeCategory(item) {
  return {
    ...item,
    name: item?.name ?? item?.xianlufenlei ?? '',
    xianlufenlei: item?.xianlufenlei ?? item?.name ?? '',
    createdAt: item?.createdAt ?? item?.created_at ?? item?.addtime ?? '',
    addtime: item?.addtime ?? item?.created_at ?? item?.createdAt ?? '',
  }
}

function normalizeRoute(item) {
  return {
    ...item,
    name: item?.name ?? item?.xianlumingcheng ?? '',
    xianlumingcheng: item?.xianlumingcheng ?? item?.name ?? '',
    category: item?.category ?? item?.xianlufenlei ?? '',
    xianlufenlei: item?.xianlufenlei ?? item?.category ?? '',
    coverUrl: item?.coverUrl ?? item?.cover_url ?? item?.fengmiantu ?? '',
    fengmiantu: item?.fengmiantu ?? item?.cover_url ?? item?.coverUrl ?? '',
    attractionName: item?.attractionName ?? item?.attraction_name ?? item?.jingdianmingcheng ?? '',
    jingdianmingcheng: item?.jingdianmingcheng ?? item?.attraction_name ?? item?.attractionName ?? '',
    departure: item?.departure ?? item?.chufadi ?? '',
    chufadi: item?.chufadi ?? item?.departure ?? '',
    destination: item?.destination ?? item?.mudedi ?? '',
    mudedi: item?.mudedi ?? item?.destination ?? '',
    transport: item?.transport ?? item?.jiaotongfangshi ?? '',
    jiaotongfangshi: item?.jiaotongfangshi ?? item?.transport ?? '',
    clickCount: item?.clickCount ?? item?.click_count ?? item?.clicknum ?? 0,
    clicknum: item?.clicknum ?? item?.click_count ?? item?.clickCount ?? 0,
  }
}

function normalizeRecord(item) {
  return {
    ...item,
    tablename: item?.tablename ?? item?.table_name ?? item?.tableName ?? '',
    tableName: item?.tableName ?? item?.table_name ?? item?.tablename ?? '',
    refid: item?.refid ?? item?.ref_id ?? item?.refId ?? null,
    refId: item?.refId ?? item?.ref_id ?? item?.refid ?? null,
    goodid: item?.goodid ?? item?.product_id ?? item?.productId ?? null,
    productId: item?.productId ?? item?.product_id ?? item?.goodid ?? null,
    goodname: item?.goodname ?? item?.product_name ?? item?.productName ?? '',
    productName: item?.productName ?? item?.product_name ?? item?.goodname ?? '',
    buynumber: item?.buynumber ?? item?.buy_number ?? item?.buyNumber ?? 0,
    buyNumber: item?.buyNumber ?? item?.buy_number ?? item?.buynumber ?? 0,
    discountprice: item?.discountprice ?? item?.discount_price ?? item?.discountPrice ?? 0,
    discounttotal: item?.discounttotal ?? item?.discount_total ?? item?.discountTotal ?? 0,
    isdefault: item?.isdefault ?? item?.is_default ?? item?.isDefault ?? '否',
    isDefault: item?.isDefault ?? item?.is_default ?? item?.isdefault ?? '否',
    photoUrl: item?.photoUrl ?? item?.photo_url ?? '',
    takenAt: item?.takenAt ?? item?.taken_at ?? '',
    addtime: item?.addtime ?? item?.created_at ?? item?.createdAt ?? '',
  }
}

export function getCurrentUserId() {
  return Number(getCachedMe()?.id ?? SESSION_LOGGED_OUT)
}

export function setCurrentUserId() {
  return null
}

export function getUserById(id) {
  const me = getCachedMe()
  return Number(me?.id) === Number(id) ? me : null
}

export function isLoggedIn() {
  return getCurrentUserId() !== SESSION_LOGGED_OUT
}

export async function loginWithPassword() {
  throw new Error('请使用 auth.js 的 login()')
}

export async function registerUser() {
  throw new Error('请使用 auth.js 的 register()')
}

export async function updateUserPassword() {
  throw new Error('请使用 auth.js 的 updatePassword()')
}

export async function deleteUserAccount() {
  await deleteAccountRemote()
  return true
}

export function formatUserIdDisplay(id) {
  const n = Number(id)
  if (n === SESSION_LOGGED_OUT || Number.isNaN(n)) return '—'
  return String(Math.max(0, Math.floor(n))).padStart(6, '0')
}

export async function listCategories() {
  const rows = await get('/api/data/categories')
  return (Array.isArray(rows) ? rows : []).map(normalizeCategory)
}

export async function listRoutes(kind = 'lvyouxianlu') {
  const rows = await get('/api/data/routes', { kind })
  return (Array.isArray(rows) ? rows : []).map(normalizeRoute)
}

export async function bumpRouteClick(kind, id) {
  return await post('/api/data/routes/click', { kind, id })
}

export async function listNews() {
  return await get('/api/data/news')
}

export async function listCart() {
  await fetchMe()
  const rows = await get('/api/data/cart')
  return (Array.isArray(rows) ? rows : []).map(normalizeRecord)
}

export async function upsertCartItem({ tablename, good }) {
  await fetchMe()
  return await post('/api/data/cart', {
    tableName: tablename,
    productId: Number(good.id),
    productName: good.name || good.xianlumingcheng || good.goodname || '',
    picture: good.fengmiantu || good.picture || '',
    price: Number(good.price || 0),
  })
}

export async function updateCartQuantity(cartId, buynumber) {
  await fetchMe()
  return await patch('/api/data/cart/quantity', { id: cartId, buyNumber: buynumber })
}

export async function removeCartItem(cartId) {
  await fetchMe()
  return await del('/api/data/cart', { id: cartId })
}

export async function listAddresses() {
  await fetchMe()
  const rows = await get('/api/data/addresses')
  return (Array.isArray(rows) ? rows : []).map(normalizeRecord)
}

export async function setDefaultAddress(addressId) {
  await fetchMe()
  return await patch('/api/data/addresses/default', { id: addressId })
}

export async function addAddress({ address, name, phone, isdefault = '否' }) {
  await fetchMe()
  return await post('/api/data/addresses', { address, name, phone, isDefault: isdefault })
}

export async function listOrders() {
  await fetchMe()
  const rows = await get('/api/data/orders')
  return (Array.isArray(rows) ? rows : []).map(normalizeRecord)
}

export async function updateOrderStatus(orderId, status) {
  await fetchMe()
  return await patch('/api/data/orders/status', { id: orderId, status })
}

export async function removeOrder(orderId) {
  await fetchMe()
  return await del('/api/data/orders', { id: orderId })
}

export async function checkout({ addressText }) {
  await fetchMe()
  return await post('/api/data/checkout', { addressText })
}

export async function listStoreup() {
  await fetchMe()
  const rows = await get('/api/data/storeups')
  return (Array.isArray(rows) ? rows : []).map(normalizeRecord)
}

export async function toggleStoreup({ tablename, refid, name, picture }) {
  await fetchMe()
  return await post('/api/data/storeups/toggle', { tableName: tablename, refId: refid, name, picture })
}

export async function listGallery() {
  await fetchMe()
  const rows = await get('/api/data/gallery')
  return (Array.isArray(rows) ? rows : []).map(normalizeRecord)
}

export async function addGalleryItem({ title, photoUrl, note, takenAt, location }) {
  await fetchMe()
  return await post('/api/data/gallery', { title, photoUrl, note, takenAt, location })
}

export async function listChat() {
  await fetchMe()
  const rows = await get('/api/data/chat')
  return (Array.isArray(rows) ? rows : []).map(normalizeRecord)
}

export async function sendChat({ ask }) {
  await fetchMe()
  return await post('/api/data/chat', { ask })
}

export async function listTrips() {
  await fetchMe()
  const rows = await get('/api/data/trips')
  return (Array.isArray(rows) ? rows : []).map(normalizeRecord)
}

export async function saveTrip({ title, payload }) {
  await fetchMe()
  return await post('/api/data/trips', { title, payload: payload || {} })
}

export async function deleteTrips(ids) {
  await fetchMe()
  const list = Array.isArray(ids) ? ids : [ids]
  const cleaned = list
    .map((x) => Number(x))
    .filter((x) => Number.isFinite(x) && x > 0)
  if (!cleaned.length) return { ok: true }
  return await del('/api/data/trips', { ids: cleaned.join(',') })
}
