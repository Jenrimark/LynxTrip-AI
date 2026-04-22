import axios from 'axios'

let cachedMe = null
let meLoaded = false

function normalizeUser(user) {
  if (!user || typeof user !== 'object') return null
  const username = String(user.username || user.yonghuming || '')
  const displayName = String(user.displayName || user.xingming || '')
  const gender = String(user.gender || user.xingbie || '')
  const phone = String(user.phone || user.lianxidianhua || '')
  const identityStatus = String(user.identityStatus || user.shimingrenzheng || '')
  const avatarUrl = String(user.avatarUrl || user.touxiang || '')
  return {
    ...user,
    username,
    displayName,
    gender,
    phone,
    identityStatus,
    avatarUrl,
    // legacy aliases for existing views
    yonghuming: username,
    xingming: displayName,
    xingbie: gender,
    lianxidianhua: phone,
    shimingrenzheng: identityStatus,
    touxiang: avatarUrl,
  }
}

function normalizeProfilePayload(payload = {}) {
  return {
    displayName: payload.displayName ?? payload.xingming ?? '',
    gender: payload.gender ?? payload.xingbie ?? '',
    phone: payload.phone ?? payload.lianxidianhua ?? '',
    avatarUrl: payload.avatarUrl ?? payload.touxiang ?? '',
  }
}

function normalizeRegisterPayload(payload = {}) {
  return {
    account: payload.account,
    password: payload.password ?? payload.mima ?? '',
    displayName: payload.displayName ?? payload.xingming ?? '',
    gender: payload.gender ?? payload.xingbie ?? '',
  }
}

function toMessage(err, fallback = '请求失败') {
  if (axios.isAxiosError(err)) {
    const msg = err.response?.data?.message
    if (typeof msg === 'string' && msg.trim()) return msg
    if (typeof err.message === 'string' && err.message.trim()) return err.message
  }
  return fallback
}

export async function login(account, mima) {
  const { data } = await axios.post('/api/auth/login', { account, password: mima }, { withCredentials: true })
  cachedMe = normalizeUser(data?.user)
  meLoaded = true
  return { ...data, user: cachedMe }
}

export async function register(payload) {
  const { data } = await axios.post('/api/auth/register', normalizeRegisterPayload(payload), { withCredentials: true })
  cachedMe = normalizeUser(data?.user)
  meLoaded = true
  return { ...data, user: cachedMe }
}

export async function fetchMe(force = false) {
  if (!force && meLoaded) return cachedMe
  try {
    const { data } = await axios.get('/api/auth/me', { withCredentials: true })
    cachedMe = normalizeUser(data?.user)
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
  const { data } = await axios.patch('/api/auth/password', { oldPassword: oldMima, newPassword: newMima }, { withCredentials: true })
  cachedMe = normalizeUser(data?.user) ?? cachedMe
  return { ...data, user: cachedMe }
}

export async function updateProfile(payload) {
  const { data } = await axios.patch('/api/auth/profile', normalizeProfilePayload(payload), { withCredentials: true })
  cachedMe = normalizeUser(data?.user) ?? cachedMe
  meLoaded = true
  return { ...data, user: cachedMe }
}

export async function deleteAccount() {
  await axios.delete('/api/auth/account', { withCredentials: true })
  cachedMe = null
  meLoaded = true
}

export function getErrorMessage(err, fallback) {
  return toMessage(err, fallback)
}
