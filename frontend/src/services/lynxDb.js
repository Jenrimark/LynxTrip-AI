/**
 * Solo 模式本地数据层：
 * - 以 LynxTrip.sql 表结构为准做 seed
 * - 用 localStorage 作为“可写数据库”，避免依赖后端接口
 * - 未来接入真实 API 时，只需替换本模块实现（页面不动）
 */
const STORAGE_KEY = 'lynxtrip.solo.db.v1'

function nowTs() {
  return new Date().toISOString()
}

function safeJsonParse(text, fallback) {
  try {
    return JSON.parse(text)
  } catch {
    return fallback
  }
}

function loadDb() {
  if (typeof window === 'undefined') return migrateUserIdScheme(seedDb())
  const raw = window.localStorage.getItem(STORAGE_KEY)
  if (!raw) return migrateUserIdScheme(seedDb())
  const parsed = safeJsonParse(raw, null)
  if (!parsed || typeof parsed !== 'object') return migrateUserIdScheme(seedDb())
  return migrateUserIdScheme(mergeWithSeed(parsed))
}

function saveDb(db) {
  if (typeof window === 'undefined') return
  window.localStorage.setItem(STORAGE_KEY, JSON.stringify(db))
}

function mergeWithSeed(db) {
  const seed = seedDb()
  // 仅做浅层合并：用户写入覆盖 seed；缺失集合用 seed 补齐
  return {
    meta: {
      ...seed.meta,
      ...(db.meta ?? {}),
      freeUserIds: Array.isArray(db.meta?.freeUserIds) ? db.meta.freeUserIds : seed.meta.freeUserIds,
      nextUserSeq: typeof db.meta?.nextUserSeq === 'number' ? db.meta.nextUserSeq : seed.meta.nextUserSeq,
      userIdScheme: db.meta?.userIdScheme ?? seed.meta.userIdScheme,
    },
    currentUserId: db.currentUserId ?? seed.currentUserId,
    tables: {
      ...seed.tables,
      ...(db.tables ?? {}),
      // 确保每张表都存在
      yonghu: (db.tables?.yonghu ?? seed.tables.yonghu) || seed.tables.yonghu,
      address: (db.tables?.address ?? seed.tables.address) || seed.tables.address,
      lvyouxianlu: (db.tables?.lvyouxianlu ?? seed.tables.lvyouxianlu) || seed.tables.lvyouxianlu,
      zuixinxianlu: (db.tables?.zuixinxianlu ?? seed.tables.zuixinxianlu) || seed.tables.zuixinxianlu,
      xianlufenlei: (db.tables?.xianlufenlei ?? seed.tables.xianlufenlei) || seed.tables.xianlufenlei,
      news: (db.tables?.news ?? seed.tables.news) || seed.tables.news,
      cart: (db.tables?.cart ?? seed.tables.cart) || seed.tables.cart,
      orders: (db.tables?.orders ?? seed.tables.orders) || seed.tables.orders,
      storeup: (db.tables?.storeup ?? seed.tables.storeup) || seed.tables.storeup,
      chat: (db.tables?.chat ?? seed.tables.chat) || seed.tables.chat,
      gallery: (db.tables?.gallery ?? seed.tables.gallery) || seed.tables.gallery,
      trips: (db.tables?.trips ?? seed.tables.trips) || seed.tables.trips,
    },
  }
}

const LEGACY_SOLO_USER_ID = 1746166399778

function computeNextUserSeq(db) {
  const ids = (db.tables.yonghu || []).map((u) => Number(u.id)).filter((id) => id > 0)
  if (!ids.length) return 1
  return Math.max(...ids) + 1
}

function remapUserForeignKeys(db, fromId, toId) {
  const tables = ['address', 'cart', 'orders', 'storeup', 'gallery', 'chat', 'trips']
  for (const t of tables) {
    db.tables[t] = (db.tables[t] || []).map((row) => {
      if (row.userid != null && Number(row.userid) === fromId) return { ...row, userid: toId }
      return row
    })
  }
}

/** 未登录会话（退出登录后） */
export const SESSION_LOGGED_OUT = -1

/** 旧版长整型 ID / 无效会话：统一回到管理员 0；已退出（-1）不改动 */
function ensureValidCurrentUser(db) {
  let dirty = false
  const cid = Number(db.currentUserId)
  if (cid === SESSION_LOGGED_OUT) return
  const hasUser = (id) => db.tables.yonghu.some((u) => Number(u.id) === Number(id))
  if (cid === LEGACY_SOLO_USER_ID || cid > 999999) {
    db.currentUserId = 0
    dirty = true
  } else if (!hasUser(cid)) {
    db.currentUserId = 0
    dirty = true
  }
  if (dirty && typeof window !== 'undefined') saveDb(db)
}

/** Solo：用户 ID 从 0 起（展示为 000000），管理员为 0；普通用户递增；注销的号进入 freeUserIds 复用 */
function migrateUserIdScheme(db) {
  if (db.meta?.userIdScheme === 'v2') {
    if (!Array.isArray(db.meta.freeUserIds)) db.meta.freeUserIds = []
    if (typeof db.meta.nextUserSeq !== 'number') {
      db.meta.nextUserSeq = computeNextUserSeq(db)
      if (typeof window !== 'undefined') saveDb(db)
    }
    ensureValidCurrentUser(db)
    return db
  }
  if (db.tables.yonghu.some((u) => Number(u.id) === LEGACY_SOLO_USER_ID)) {
    db.tables.yonghu = db.tables.yonghu.map((u) => (Number(u.id) === LEGACY_SOLO_USER_ID ? { ...u, id: 0 } : u))
    remapUserForeignKeys(db, LEGACY_SOLO_USER_ID, 0)
    if (Number(db.currentUserId) === LEGACY_SOLO_USER_ID) db.currentUserId = 0
  }
  db.meta = {
    ...db.meta,
    userIdScheme: 'v2',
    freeUserIds: [],
    nextUserSeq: computeNextUserSeq(db),
  }
  ensureValidCurrentUser(db)
  if (typeof window !== 'undefined') saveDb(db)
  return db
}

function nextId() {
  // 接近 SQL 里 bigint 造型，但不追求一致
  return Number(`${Date.now()}${Math.floor(Math.random() * 1000)}`.slice(0, 18))
}

function seedDb() {
  // 精简 seed：字段名与 LynxTrip.sql 对齐（便于“数据库内容匹配界面”）
  return {
    meta: {
      version: 1,
      seededAt: nowTs(),
      updatedAt: nowTs(),
      userIdScheme: 'v2',
      freeUserIds: [],
      nextUserSeq: 1,
    },
    currentUserId: SESSION_LOGGED_OUT,
    tables: {
      yonghu: [
        {
          id: 0,
          addtime: '2025-05-02T14:13:19.000Z',
          yonghuming: 'admin',
          mima: 'admin',
          xingming: '管理员',
          touxiang: 'http://localhost:8080/springbootmt74k/upload/1747117207072.jpg',
          xingbie: '男',
          lianxidianhua: '16688489420',
          money: 9809.2,
          shimingrenzheng: '已认证',
        },
      ],
      address: [
        {
          id: 1746166637824,
          addtime: '2025-05-02T14:17:17.000Z',
          userid: 0,
          address: '中国地质大学（武汉）',
          name: 'Jenrimark',
          phone: '18471609769',
          isdefault: '是',
        },
      ],
      xianlufenlei: [
        { id: 21, addtime: '2025-05-17T15:14:31.000Z', xianlufenlei: '乡村风景' },
        { id: 22, addtime: '2025-05-17T15:14:31.000Z', xianlufenlei: '度假旅游' },
        { id: 23, addtime: '2025-05-17T15:14:31.000Z', xianlufenlei: '探险考察' },
        { id: 24, addtime: '2025-05-17T15:14:31.000Z', xianlufenlei: '文化底蕴' },
        { id: 25, addtime: '2025-05-17T15:14:31.000Z', xianlufenlei: '短程旅游' },
        { id: 26, addtime: '2025-05-17T15:44:20.000Z', xianlufenlei: '红色基地' },
      ],
      lvyouxianlu: [
        {
          id: 1747413672433,
          addtime: '2025-05-17T00:41:11.000Z',
          xianlumingcheng: '问道武当·田园拾趣·红色记忆三日游',
          xianlufenlei: '文化旅游',
          fengmiantu: 'http://localhost:8080/springbootmt74k/upload/1747413668046.png',
          jingdianmingcheng: '武当山风景区（5A级）-竹山县圣水湖柑橘采摘园-郧阳革命烈士陵园-青龙山恐龙蛋化石群地质公园',
          chufadi: '武汉',
          mudedi: '十堰市',
          jiaotongfangshi: '高铁',
          chuxingshijian: '2025-06-01T00:00:00.000Z',
          feiyongbaohan:
            '武汉-十堰往返高铁二等座\n2晚特色住宿（武当山民宿+竹山农家乐）\n行程所列景点首道门票\n当地旅游大巴全程接送\n2早餐+3正餐（含农家特色餐）\n专业导游服务\n旅游意外险',
          xingchengluxian:
            '<p>Day1：武汉-十堰-武当山</p><p>15:14 武汉站乘坐高铁前往十堰</p><p>17:30 抵达十堰，专车接往武当山</p><p>18:30 入住武当山特色民宿</p><p>19:00 晚餐（道家养生宴）</p><p>&nbsp;</p><p>Day2：武当山-竹山县</p><p>08:00 游览武当山（紫霄宫、太子坡、金顶）</p><p>12:00 景区内午餐</p><p>14:00 乘车前往竹山县</p><p>15:30 圣水湖柑橘采摘体验</p><p>18:00 入住农家乐，晚餐品尝农家菜</p><p>19:30 民歌欣赏+糍粑制作体验</p><p>&nbsp;</p><p>Day3：竹山县-郧阳区-武汉</p><p>08:00 前往郧阳革命烈士陵园</p><p>10:00 参观青龙山恐龙蛋化石群</p><p>12:00 午餐（当地特色餐）</p><p>14:00 选购特产（武当道茶、房县黄酒）</p><p>15:00 送站返回武汉</p><p>17:30 抵达武汉站，行程结束</p>',
          clicktime: '2025-05-17T00:55:12.000Z',
          clicknum: 6,
          price: 1280,
        },
      ],
      zuixinxianlu: [
        {
          id: 1614757619035,
          addtime: '2021-03-03T15:46:58.000Z',
          xianlumingcheng: '桂林4天3晚',
          xianlufenlei: '文化底蕴',
          fengmiantu: 'http://localhost:8080/springbootmt74k/upload/1614757585872.png',
          jingdianmingcheng: '漓江',
          chufadi: '梅州',
          mudedi: '桂林',
          jiaotongfangshi: '高铁',
          chuxingshijian: '2021-03-03T07:46:40.000Z',
          feiyongbaohan: '住宿费来回车费等',
          xingchengluxian: '<p><img src="http://localhost:8080/springbootmt74k/upload/1614757617098.png"></p>',
          price: 1200,
        },
      ],
      news: [
        {
          id: 101,
          addtime: '2021-03-03T15:14:31.000Z',
          title: '将军故里·诗意田园——黄冈市红安县七里坪镇',
          introduction:
            '红安县是"中国第一将军县"，七里坪镇保存完好的明清老街与300多处革命遗址交织。春有万亩油菜花海，秋有稻浪翻滚，长胜街上的青砖黛瓦间藏着20余处苏维埃政权旧址。',
          picture: 'http://localhost:8080/springbootmt74k/upload/1747403442749.jpg',
          content:
            '<p>在湖北省红安县七里坪镇的长胜街上，有一家传统的铁匠铺，主人张铁山打铁已有四十年……</p><p><img src="http://localhost:8080/springbootmt74k/upload/1747412433347.png"></p>',
        },
        {
          id: 1747412966230,
          addtime: '2025-05-17T00:29:26.000Z',
          title: '大别山下的田园诗——黄冈市罗田县圣人堂村',
          introduction:
            '圣人堂村坐落在天堂寨风景区腹地，以"红叶第一村"闻名遐迩。每年深秋，乌桕树将山谷染成赭红，与白墙黛瓦的徽派民居构成绝美画卷。',
          picture: 'http://localhost:8080/springbootmt74k/upload/1747412964601.png',
          content: '<p>罗田县圣人堂村通过"农旅融合+文化IP"的发展模式……</p>',
        },
      ],
      cart: [],
      orders: [],
      storeup: [],
      chat: [],
      // SQL 中没有该表：用于“光影拾记”本地内容承载
      gallery: [],
      // SQL 中没有该表：用于“AI规划行程”保存结果
      trips: [],
    },
  }
}

function withDb(mutator) {
  const db = loadDb()
  const next = mutator(structuredClone(db)) || db
  next.meta = { ...(next.meta ?? {}), updatedAt: nowTs() }
  saveDb(next)
  return next
}

export function getCurrentUserId() {
  return loadDb().currentUserId
}

export function setCurrentUserId(userId) {
  return withDb((db) => {
    const n = Number(userId)
    db.currentUserId = Number.isNaN(n) ? SESSION_LOGGED_OUT : n
    return db
  })
}

export function getUserById(id) {
  const db = loadDb()
  return db.tables.yonghu.find((u) => Number(u.id) === Number(id)) || null
}

export function isLoggedIn() {
  const raw = loadDb().currentUserId
  if (raw === undefined || raw === null) return false
  const n = Number(raw)
  if (Number.isNaN(n)) return false
  return n !== SESSION_LOGGED_OUT
}

/** 登录用手机号：只保留数字；支持 +86 前缀 */
function normalizePhone(raw) {
  let digits = String(raw ?? '')
    .trim()
    .replace(/\s+/g, '')
    .replace(/-/g, '')
    .replace(/\D/g, '')
  if (digits.startsWith('86') && digits.length === 13) digits = digits.slice(2)
  return digits
}

/**
 * 手机号/用户名 + 密码登录
 * @param {string} accountRaw 手机号或用户名（可含空格、横线）
 * @param {string} mima 密码
 */
export function loginWithPassword(accountRaw, mima) {
  const db = loadDb()
  const account = String(accountRaw ?? '').trim()
  const pass = String(mima ?? '')
  if (!account || !pass) return { ok: false, message: '请输入账号和密码' }
  // 尝试作为手机号匹配
  const phone = normalizePhone(accountRaw)
  let u = null
  if (phone && /^1\d{10}$/.test(phone)) {
    u = db.tables.yonghu.find((x) => normalizePhone(x.lianxidianhua) === phone && String(x.mima) === pass)
  }
  // 如果手机号未匹配到，尝试作为用户名匹配
  if (!u) {
    u = db.tables.yonghu.find((x) => String(x.yonghuming).trim() === account && String(x.mima) === pass)
  }
  if (!u) return { ok: false, message: '账号或密码错误' }
  setCurrentUserId(u.id)
  return { ok: true }
}

/** 展示用：000000 起，管理员为 000000 */
export function formatUserIdDisplay(id) {
  const n = Number(id)
  if (n === SESSION_LOGGED_OUT || Number.isNaN(n)) return '—'
  const v = Number.isFinite(n) && n >= 0 ? Math.floor(n) : 0
  if (v > 999999) return String(v)
  return String(v).padStart(6, '0')
}

function allocateUserId(db) {
  if (!db.meta.freeUserIds) db.meta.freeUserIds = []
  db.meta.freeUserIds = [...db.meta.freeUserIds].sort((a, b) => a - b)
  if (db.meta.freeUserIds.length) {
    return db.meta.freeUserIds.shift()
  }
  const id = db.meta.nextUserSeq
  db.meta.nextUserSeq = id + 1
  return id
}

/**
 * Register: support phone or username
 * @param {{ account: string, mima: string, xingming?: string, xingbie?: string }} p
 */
export function registerUser({ account, mima, xingming, xingbie }) {
  const acc = String(account ?? '').trim()
  if (!acc) return null
  const dbCheck = loadDb()
  // Check if phone or username already exists
  const phone = normalizePhone(acc)
  if (phone && /^1\d{10}$/.test(phone)) {
    if (dbCheck.tables.yonghu.some((x) => normalizePhone(x.lianxidianhua) === phone)) return null
  }
  if (dbCheck.tables.yonghu.some((x) => String(x.yonghuming).trim() === acc)) return null
  let newId = null
  withDb((db) => {
    const id = allocateUserId(db)
    newId = id
    db.tables.yonghu.push({
      id,
      addtime: nowTs(),
      yonghuming: acc,
      mima: mima || '123456',
      xingming: xingming || '',
      touxiang: '',
      xingbie: xingbie || '---',
      lianxidianhua: phone && /^1\d{10}$/.test(phone) ? phone : '',
      money: 0,
      shimingrenzheng: 'Not certified',
    })
    return db
  })
  if (newId != null) setCurrentUserId(newId)
  return newId
}

export function updateUserPassword(userId, oldMima, newMima) {
  const u = getUserById(userId)
  if (!u) return false
  if (u.mima !== oldMima) return false
  const next = String(newMima ?? '')
  if (next.length < 1) return false
  withDb((db) => {
    const row = db.tables.yonghu.find((x) => Number(x.id) === Number(userId))
    if (row) row.mima = next
    return db
  })
  return true
}

export function deleteUserAccount(userId) {
  const uid = Number(userId)
  if (uid === 0) return false
  if (!getUserById(uid)) return false
  withDb((db) => {
    db.tables.yonghu = db.tables.yonghu.filter((u) => Number(u.id) !== uid)
    if (!db.meta.freeUserIds) db.meta.freeUserIds = []
    db.meta.freeUserIds.push(uid)
    db.meta.freeUserIds = [...new Set(db.meta.freeUserIds)].sort((a, b) => a - b)
    db.tables.cart = (db.tables.cart || []).filter((c) => Number(c.userid) !== uid)
    db.tables.address = (db.tables.address || []).filter((a) => Number(a.userid) !== uid)
    db.tables.orders = (db.tables.orders || []).filter((o) => Number(o.userid) !== uid)
    db.tables.storeup = (db.tables.storeup || []).filter((s) => Number(s.userid) !== uid)
    db.tables.gallery = (db.tables.gallery || []).filter((g) => Number(g.userid) !== uid)
    db.tables.chat = (db.tables.chat || []).filter((c) => Number(c.userid) !== uid)
    db.tables.trips = (db.tables.trips || []).filter((t) => Number(t.userid) !== uid)
    if (Number(db.currentUserId) === uid) db.currentUserId = SESSION_LOGGED_OUT
    return db
  })
  return true
}

export function listCategories() {
  return loadDb().tables.xianlufenlei
}

export function listRoutes(kind = 'lvyouxianlu') {
  const db = loadDb()
  const list = db.tables[kind] || []
  return [...list].sort((a, b) => (Number(b.clicknum || 0) - Number(a.clicknum || 0)) || (Number(b.id) - Number(a.id)))
}

export function bumpRouteClick(kind, id) {
  return withDb((db) => {
    const row = (db.tables[kind] || []).find((r) => Number(r.id) === Number(id))
    if (!row) return db
    row.clicknum = Number(row.clicknum || 0) + 1
    row.clicktime = nowTs()
    return db
  })
}

export function listNews() {
  const db = loadDb()
  return [...db.tables.news].sort((a, b) => Number(b.id) - Number(a.id))
}

export function listCart(userId = getCurrentUserId()) {
  const db = loadDb()
  return db.tables.cart.filter((c) => Number(c.userid) === Number(userId))
}

export function upsertCartItem({ userId = getCurrentUserId(), tablename, good }) {
  const uid = Number(userId)
  if (uid === SESSION_LOGGED_OUT || Number.isNaN(uid)) return
  return withDb((db) => {
    db.tables.cart = db.tables.cart || []
    const existing = db.tables.cart.find((c) => Number(c.userid) === uid && String(c.tablename) === String(tablename) && Number(c.goodid) === Number(good.id))
    if (existing) {
      existing.buynumber = Number(existing.buynumber || 0) + 1
      return db
    }
    db.tables.cart.push({
      id: nextId(),
      addtime: nowTs(),
      tablename,
      userid: uid,
      goodid: Number(good.id),
      goodname: good.xianlumingcheng || good.goodname || '',
      picture: good.fengmiantu || good.picture || '',
      buynumber: 1,
      price: Number(good.price || 0),
      discountprice: 0,
    })
    return db
  })
}

export function updateCartQuantity(cartId, buynumber) {
  return withDb((db) => {
    const row = db.tables.cart.find((c) => Number(c.id) === Number(cartId))
    if (!row) return db
    row.buynumber = Math.max(1, Number(buynumber || 1))
    return db
  })
}

export function removeCartItem(cartId) {
  return withDb((db) => {
    db.tables.cart = db.tables.cart.filter((c) => Number(c.id) !== Number(cartId))
    return db
  })
}

export function listAddresses(userId = getCurrentUserId()) {
  const db = loadDb()
  return db.tables.address
    .filter((a) => Number(a.userid) === Number(userId))
    .sort((a, b) => (a.isdefault === '是' ? -1 : 1) - (b.isdefault === '是' ? -1 : 1))
}

export function setDefaultAddress(addressId, userId = getCurrentUserId()) {
  return withDb((db) => {
    const uid = Number(userId)
    db.tables.address = db.tables.address.map((a) => (Number(a.userid) !== uid ? a : { ...a, isdefault: Number(a.id) === Number(addressId) ? '是' : '否' }))
    return db
  })
}

export function addAddress({ userId = getCurrentUserId(), address, name, phone, isdefault = '否' }) {
  return withDb((db) => {
    const uid = Number(userId)
    const row = { id: nextId(), addtime: nowTs(), userid: uid, address, name, phone, isdefault }
    db.tables.address.push(row)
    if (isdefault === '是') {
      db.tables.address = db.tables.address.map((a) => (Number(a.userid) !== uid ? a : { ...a, isdefault: Number(a.id) === Number(row.id) ? '是' : '否' }))
    }
    return db
  })
}

export function listOrders(userId = getCurrentUserId()) {
  const db = loadDb()
  return db.tables.orders.filter((o) => Number(o.userid) === Number(userId)).sort((a, b) => Number(b.id) - Number(a.id))
}

export function updateOrderStatus(orderId, status, userId = getCurrentUserId()) {
  return withDb((db) => {
    const uid = Number(userId)
    db.tables.orders = db.tables.orders.map((o) => {
      if (Number(o.id) !== Number(orderId)) return o
      if (Number(o.userid) !== uid) return o
      return { ...o, status }
    })
    return db
  })
}

export function removeOrder(orderId, userId = getCurrentUserId()) {
  return withDb((db) => {
    const uid = Number(userId)
    db.tables.orders = db.tables.orders.filter((o) => !(Number(o.id) === Number(orderId) && Number(o.userid) === uid))
    return db
  })
}

export function checkout({ userId = getCurrentUserId(), addressText }) {
  return withDb((db) => {
    const uid = Number(userId)
    if (uid === SESSION_LOGGED_OUT || Number.isNaN(uid)) return db
    const cart = db.tables.cart.filter((c) => Number(c.userid) === uid)
    if (!cart.length) return db
    const orderidBase = `${new Date().getFullYear()}${String(Date.now()).slice(-10)}`
    const status = '已支付'
    db.tables.orders = db.tables.orders || []
    cart.forEach((item, idx) => {
      const price = Number(item.price || 0)
      const n = Number(item.buynumber || 1)
      const total = price * n
      db.tables.orders.push({
        id: nextId(),
        addtime: nowTs(),
        orderid: `${orderidBase}${idx}`,
        tablename: item.tablename,
        userid: uid,
        goodid: item.goodid,
        goodname: item.goodname,
        picture: item.picture,
        buynumber: n,
        price,
        discountprice: 0,
        total,
        discounttotal: 0,
        type: 1,
        status,
        address: addressText || '',
      })
    })
    db.tables.cart = db.tables.cart.filter((c) => Number(c.userid) !== uid)
    return db
  })
}

export function listStoreup(userId = getCurrentUserId()) {
  const db = loadDb()
  return db.tables.storeup.filter((s) => Number(s.userid) === Number(userId)).sort((a, b) => Number(b.id) - Number(a.id))
}

export function toggleStoreup({ userId = getCurrentUserId(), tablename, refid, name, picture }) {
  return withDb((db) => {
    const uid = Number(userId)
    db.tables.storeup = db.tables.storeup || []
    const exists = db.tables.storeup.find((s) => Number(s.userid) === uid && String(s.tablename) === String(tablename) && Number(s.refid) === Number(refid))
    if (exists) {
      db.tables.storeup = db.tables.storeup.filter((s) => Number(s.id) !== Number(exists.id))
      return db
    }
    db.tables.storeup.push({
      id: nextId(),
      addtime: nowTs(),
      userid: uid,
      refid: Number(refid),
      tablename,
      name: name || '',
      picture: picture || '',
    })
    return db
  })
}

export function listGallery(userId = getCurrentUserId()) {
  const db = loadDb()
  return db.tables.gallery.filter((g) => Number(g.userid) === Number(userId)).sort((a, b) => Number(b.id) - Number(a.id))
}

export function addGalleryItem({ userId = getCurrentUserId(), title, photoUrl, note, takenAt, location }) {
  return withDb((db) => {
    const uid = Number(userId)
    db.tables.gallery.push({
      id: nextId(),
      addtime: nowTs(),
      userid: uid,
      title: title || '未命名',
      photoUrl,
      note: note || '',
      takenAt: takenAt || '',
      location: location || '',
    })
    return db
  })
}

export function listChat(userId = getCurrentUserId()) {
  const db = loadDb()
  return db.tables.chat.filter((c) => Number(c.userid) === Number(userId)).sort((a, b) => Number(a.id) - Number(b.id))
}

export function sendChat({ userId = getCurrentUserId(), ask }) {
  return withDb((db) => {
    const uid = Number(userId)
    db.tables.chat.push({
      id: nextId(),
      addtime: nowTs(),
      userid: uid,
      adminid: 1,
      ask,
      reply: null,
      isreply: 0,
    })
    // 简易自动回复（solo 模式）
    db.tables.chat.push({
      id: nextId(),
      addtime: nowTs(),
      userid: uid,
      adminid: 1,
      ask: null,
      reply: '已收到，我们会尽快为您安排人工客服。您也可以在「旅游路线」里先挑选心仪线路加入购物车。',
      isreply: 1,
    })
    return db
  })
}

export function listTrips(userId = getCurrentUserId()) {
  const db = loadDb()
  return db.tables.trips.filter((t) => Number(t.userid) === Number(userId)).sort((a, b) => Number(b.id) - Number(a.id))
}

export function saveTrip({ userId = getCurrentUserId(), title, payload }) {
  return withDb((db) => {
    const uid = Number(userId)
    db.tables.trips.push({
      id: nextId(),
      addtime: nowTs(),
      userid: uid,
      title: title || 'AI行程',
      payload,
    })
    return db
  })
}

