import axios from 'axios'

let cachedMe = null
let meLoaded = false

function toMessage(err, fallback = '请求失败') {
  if (axios.isAxiosError(err)) {
    const msg = err.response?.data?.message
    if (typeof msg === 'string' && msg.trim()) return msg
    if (typeof err.message === 'string' && err.message.trim()) return err.message
  }
  return fallback
}

export async function login(account, mima) {
  const { data } = await axios.post('/api/auth/login', { account, mima }, { withCredentials: true })
  cachedMe = data?.user ?? null
  meLoaded = true
  return data
}

export async function register(payload) {
  const { data } = await axios.post('/api/auth/register', payload, { withCredentials: true })
  cachedMe = data?.user ?? null
  meLoaded = true
  return data
}

export async function fetchMe(force = false) {
  if (!force && meLoaded) return cachedMe
  try {
    const { data } = await axios.get('/api/auth/me', { withCredentials: true })
    cachedMe = data?.user ?? null
    meLoaded = true
    return cachedMe
  } catch (err) {
    cachedMe = null
    meLoaded = true
    if (axios.isAxiosError(err) && err.response?.status === 401) return null
    throw new Error(toMessage(err, '获取登录态失败'))
  }
}

export async function isLoggedInRemote(force = false) {
  const me = await fetchMe(force)
  return !!me
}

export function getCachedMe() {
  return cachedMe
}

export async function logout() {
  try {
    await axios.post('/api/auth/logout', {}, { withCredentials: true })
  } finally {
    cachedMe = null
    meLoaded = true
  }
}

export async function updatePassword(oldMima, newMima) {
  const { data } = await axios.patch('/api/auth/password', { oldMima, newMima }, { withCredentials: true })
  cachedMe = data?.user ?? cachedMe
  return data
}

export async function updateProfile(payload) {
  const { data } = await axios.patch('/api/auth/profile', payload, { withCredentials: true })
  cachedMe = data?.user ?? cachedMe
  meLoaded = true
  return data
}

export async function deleteAccount() {
  await axios.delete('/api/auth/account', { withCredentials: true })
  cachedMe = null
  meLoaded = true
}

export function getErrorMessage(err, fallback) {
  return toMessage(err, fallback)
}
