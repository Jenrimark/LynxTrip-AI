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
  return await get('/api/data/categories')
}

export async function listRoutes(kind = 'lvyouxianlu') {
  return await get('/api/data/routes', { kind })
}

export async function bumpRouteClick(kind, id) {
  return await post('/api/data/routes/click', { kind, id })
}

export async function listNews() {
  return await get('/api/data/news')
}

export async function listCart() {
  await fetchMe()
  return await get('/api/data/cart')
}

export async function upsertCartItem({ tablename, good }) {
  await fetchMe()
  return await post('/api/data/cart', {
    tablename,
    goodid: Number(good.id),
    goodname: good.xianlumingcheng || good.goodname || '',
    picture: good.fengmiantu || good.picture || '',
    price: Number(good.price || 0),
  })
}

export async function updateCartQuantity(cartId, buynumber) {
  await fetchMe()
  return await patch('/api/data/cart/quantity', { id: cartId, buynumber })
}

export async function removeCartItem(cartId) {
  await fetchMe()
  return await del('/api/data/cart', { id: cartId })
}

export async function listAddresses() {
  await fetchMe()
  return await get('/api/data/addresses')
}

export async function setDefaultAddress(addressId) {
  await fetchMe()
  return await patch('/api/data/addresses/default', { id: addressId })
}

export async function addAddress({ address, name, phone, isdefault = '否' }) {
  await fetchMe()
  return await post('/api/data/addresses', { address, name, phone, isdefault })
}

export async function listOrders() {
  await fetchMe()
  return await get('/api/data/orders')
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
  return await get('/api/data/storeups')
}

export async function toggleStoreup({ tablename, refid, name, picture }) {
  await fetchMe()
  return await post('/api/data/storeups/toggle', { tablename, refid, name, picture })
}

export async function listGallery() {
  await fetchMe()
  return await get('/api/data/gallery')
}

export async function addGalleryItem({ title, photoUrl, note, takenAt, location }) {
  await fetchMe()
  return await post('/api/data/gallery', { title, photoUrl, note, takenAt, location })
}

export async function listChat() {
  await fetchMe()
  return await get('/api/data/chat')
}

export async function sendChat({ ask }) {
  await fetchMe()
  return await post('/api/data/chat', { ask })
}

export async function listTrips() {
  await fetchMe()
  return await get('/api/data/trips')
}

export async function saveTrip({ title, payload }) {
  await fetchMe()
  return await post('/api/data/trips', { title, payload: payload || {} })
}
