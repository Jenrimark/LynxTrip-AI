<script setup>
import { computed, onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  fetchMe,
  getErrorMessage,
  isLoggedInRemote,
  logout,
  updatePassword as updatePasswordRemote,
  updateProfile as updateProfileRemote,
} from '../services/auth'
import { formatUserIdDisplay, deleteUserAccount } from '../services/lynxDb'

const router = useRouter()
const user = ref(null)
const userIdDisplay = computed(() => formatUserIdDisplay(user.value?.id))

onMounted(async () => {
  if (!(await isLoggedInRemote())) {
    router.replace({ name: 'login', query: { redirect: '/me' } })
    return
  }
  user.value = await fetchMe(true)
})

const moneyText = computed(() => {
  const m = Number(user.value?.money || 0)
  return m.toFixed(2)
})

const userEmailText = computed(() => {
  return String(user.value?.youxiang || user.value?.email || '未绑定')
})

const realNameText = computed(() => {
  return String(user.value?.shimingrenzheng || '未认证')
})

const emailStatusText = computed(() => (userEmailText.value === '未绑定' ? '未绑定邮箱' : '邮箱已绑定'))

async function handleCopyId() {
  if (!userIdDisplay.value || userIdDisplay.value === '000000') {
    ElMessage.warning('暂无可复制的用户ID')
    return
  }
  try {
    await navigator.clipboard.writeText(userIdDisplay.value)
    ElMessage.success('用户ID已复制')
  } catch {
    ElMessage.error('复制失败，请手动复制')
  }
}

function handleGoRegister() {
  logout().finally(() => {
    router.replace({ name: 'login', query: { redirect: '/me' } })
  })
}


const isPwdOpen = ref(false)
const pwdForm = ref({ oldMima: '', newMima: '', newMima2: '' })
const isProfileOpen = ref(false)
const profileForm = ref({ xingming: '', xingbie: '', lianxidianhua: '', touxiang: '' })

function openPwd() {
  pwdForm.value = { oldMima: '', newMima: '', newMima2: '' }
  isPwdOpen.value = true
}

function openProfile() {
  profileForm.value = {
    xingming: String(user.value?.xingming || ''),
    xingbie: String(user.value?.xingbie || ''),
    lianxidianhua: String(user.value?.lianxidianhua || ''),
    touxiang: String(user.value?.touxiang || ''),
  }
  isProfileOpen.value = true
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

async function saveProfile() {
  try {
    const { user: nextUser } = await updateProfileRemote({
      xingming: profileForm.value.xingming,
      xingbie: profileForm.value.xingbie,
      lianxidianhua: profileForm.value.lianxidianhua,
      touxiang: profileForm.value.touxiang,
    })
    user.value = nextUser || user.value
    isProfileOpen.value = false
    ElMessage.success('资料已保存')
  } catch (err) {
    ElMessage.error(getErrorMessage(err, '资料保存失败'))
  }
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
    <div class="profilePage">
      <div class="cover"></div>
      <div class="identityCard">
    <div class="profileInfo">
      <div class="profileInfo__main">
        <el-avatar class="el-avatar--circle avatar avatar--inline" :size="234" :src="user?.touxiang" />
        <h2 class="name">{{ user?.xingming || '未命名用户' }}</h2>
        <p class="line">性别 {{ user?.xingbie || '—' }} ｜ 用户ID {{ userIdDisplay }}</p>
        <p class="line">电话 {{ user?.lianxidianhua || '—' }} ｜ 用户名 {{ user?.yonghuming || '—' }}</p>
        <p class="desc">欢迎回来，{{ user?.yonghuming || '用户' }}。在这里可以统一管理账号信息、密码与安全操作。</p>
        <div class="actionRow">
          <button class="pill pill--primary" type="button" @click="openProfile">编辑资料</button>
          <button class="pill" type="button" @click="openPwd">修改密码</button>
          <button class="pill" type="button" @click="handleCopyId">复制ID</button>
          <button class="pill pill--soft" type="button" @click="handleGoRegister">注册账号</button>
          <button class="pill pill--soft" type="button" @click="handleLogoutSession">退出登录</button>
          <button class="pill pill--danger" type="button" @click="handleLogoutAccount">注销账号</button>
        </div>
      </div>
    </div>
    </div>

    <div class="contentGrid">
      <aside class="leftRail">
        <article class="railCard">
          <span class="label">余额</span>
          <strong class="money">¥ {{ moneyText }}</strong>
        </article>
        <article class="railCard">
          <span class="label">账户信息</span>
          <div class="fact"><span>用户ID</span><strong>{{ userIdDisplay }}</strong></div>
          <div class="fact"><span>实名认证</span><strong>{{ realNameText }}</strong></div>
          <div class="fact"><span>邮箱状态</span><strong>{{ emailStatusText }}</strong></div>
        </article>
      </aside>

      <article class="mainCard">
        <div class="quickTabs">
          <button type="button" class="quickTabs__item">我的行程</button>
          <button type="button" class="quickTabs__item">账户安全</button>
        </div>
        <h3>功能导览</h3>
        <p>进入你的灵犀旅行中枢，管理行程、收藏、订单和客服记录。</p>
        <div class="moduleActions">
          <button type="button">我的行程工作台</button>
          <button type="button">收藏目的地</button>
          <button type="button">订单与售后</button>
          <button type="button">在线客服支持</button>
        </div>
      </article>

      <article class="safeCard">
        <h3>安全中心</h3>
        <p>强化账号安全设置，保护你的旅行数据与支付信息。</p>
        <ul class="safeList">
          <li>实名状态：{{ realNameText }}</li>
          <li>邮箱状态：{{ emailStatusText }}</li>
          <li>建议：定期修改密码</li>
        </ul>
        <button class="safeBtn" type="button" @click="openPwd">立即设置</button>
      </article>
    </div>
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

    <el-dialog v-model="isProfileOpen" title="编辑资料" width="560px">
    <el-form label-position="top">
      <div class="dialogRow">
        <el-form-item label="姓名">
          <el-input v-model="profileForm.xingming" maxlength="64" />
        </el-form-item>
        <el-form-item label="性别">
          <el-select v-model="profileForm.xingbie" placeholder="请选择" clearable>
            <el-option label="男" value="男" />
            <el-option label="女" value="女" />
            <el-option label="保密" value="保密" />
          </el-select>
        </el-form-item>
      </div>
      <div class="dialogRow">
        <el-form-item label="电话">
          <el-input v-model="profileForm.lianxidianhua" maxlength="32" />
        </el-form-item>
        <el-form-item label="头像链接">
          <el-input v-model="profileForm.touxiang" maxlength="255" placeholder="https://..." />
        </el-form-item>
      </div>
    </el-form>
    <template #footer>
      <el-button @click="isProfileOpen = false">取消</el-button>
      <el-button type="primary" @click="saveProfile">保存修改</el-button>
    </template>
    </el-dialog>
  </section>
</template>

<style scoped lang="scss">
.page {
  display: block;
  margin: -24px -24px 0;
}

.profilePage {
  position: relative;
  overflow: visible;
  --cover-height: 220px;
  --avatar-size: 234px;
  --golden: 0.618;
  --hero-width: 75%;
  background: transparent;
  border: none;
  box-shadow: none;
}

.cover {
  height: var(--cover-height, 220px);
  width: 100%;
  margin: 0;
  border-radius: 0;
  background:
    linear-gradient(135deg, rgba(7, 89, 133, 0.48), rgba(15, 23, 42, 0.2)),
    url('../assets/background.png') center/cover no-repeat;
}

.identityCard {
  position: relative;
  width: 100%;
  margin: 0;
  padding: 0 0 24px;
  background: #fff;
  border: 1px solid rgba(148, 163, 184, 0.2);
  border-top: none;
  border-radius: 0;
}

.avatar {
  border: 5px solid #fff;
  box-shadow: 0 14px 36px rgba(15, 23, 42, 0.18);
  flex-shrink: 0;
}
.avatar--inline {
  width: var(--avatar-size, 234px) !important;
  height: var(--avatar-size, 234px) !important;
}

.profileInfo {
  width: 100%;
  margin: 0;
  margin-top: 0;
  background: transparent;
  border: 0;
  border-radius: 0;
  padding: 16px calc((100% - var(--hero-width, 75%)) / 2);
  display: grid;
  grid-template-columns: minmax(0, 1fr) auto;
  align-items: start;
  gap: 16px;
}
.profileInfo__main {
  position: relative;
  min-width: 0;
  width: 75%;
  max-width: 100%;
  box-sizing: border-box;
  min-height: 0;
  padding-left: calc(var(--avatar-size, 234px) + 32px);
  padding-top: 12px;
}

.profileInfo__main .avatar--inline {
  position: absolute;
  left: -10px;
  top: -115px;
  margin-right: 0;
}

.name {
  margin: 0 0 4px 0;
  font-size: 42px;
  line-height: 1;
  font-weight: 900;
  color: #0f172a;
}

.line {
  margin: 2px 0;
  font-size: 14px;
  color: #475569;
}

.desc {
  margin: 6px 0 0 0;
  font-size: 14px;
  color: #334155;
}

.actionRow {
  margin-top: 8px;
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}
.actionRow--minor {
  margin-top: 6px;
}

.pill {
  height: 34px;
  border-radius: 999px;
  border: 1px solid rgba(148, 163, 184, 0.36);
  background: #fff;
  color: #0f172a;
  padding: 0 14px;
  font-size: 14px;
  font-weight: 700;
  cursor: pointer;
  transition: 0.2s ease;
}

.pill:hover {
  transform: translateY(-1px);
  border-color: rgba(14, 165, 233, 0.4);
}

.pill--primary {
  background: #ea580c;
  border-color: #ea580c;
  color: #fff;
}

.pill--soft {
  background: #f0f9ff;
  color: #0c4a6e;
}

.pill--danger {
  border-color: rgba(220, 38, 38, 0.45);
  color: #991b1b;
}

.contentGrid {
  width: 75%;
  margin: 16px auto 0;
  margin-top: 16px;
  display: grid;
  grid-template-columns: 180px minmax(0, 1fr) 220px;
  gap: 14px;
}

.leftRail {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.railCard {
  border-radius: 14px;
  border: 1px solid rgba(148, 163, 184, 0.24);
  background: #ffffff;
  padding: 14px;
}

.label {
  display: block;
  font-size: 14px;
  color: #64748b;
}

.money {
  margin-top: 4px;
  display: block;
  font-size: 40px;
  line-height: 1;
  color: #991b1b;
}

.hint {
  margin-top: 2px;
  display: block;
  color: #64748b;
  font-size: 12px;
}
.fact {
  margin-top: 8px;
  display: flex;
  justify-content: space-between;
  gap: 8px;
  font-size: 12px;
  color: #475569;
}
.fact strong {
  color: #0f172a;
  font-size: 13px;
}

.mainCard,
.safeCard {
  border-radius: 14px;
  border: 1px solid rgba(148, 163, 184, 0.24);
  padding: 18px;
}

.mainCard {
  background: linear-gradient(130deg, #dbeafe, #ede9fe);
}
.quickTabs {
  display: flex;
  gap: 8px;
  margin-bottom: 12px;
}
.quickTabs__item {
  height: 30px;
  min-width: 124px;
  border-radius: 8px;
  border: 2px solid rgba(239, 68, 68, 0.7);
  background: rgba(255, 255, 255, 0.9);
  color: #1e293b;
  font-size: 13px;
  font-weight: 800;
  cursor: pointer;
}

.mainCard h3,
.safeCard h3 {
  margin: 0;
  font-size: 36px;
  line-height: 1;
  font-weight: 900;
  color: #3730a3;
}

.mainCard p,
.safeCard p {
  margin: 10px 0 0 0;
  font-size: 14px;
  color: #334155;
  line-height: 1.6;
}

.moduleActions {
  margin-top: 14px;
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 8px;
}

.moduleActions button {
  height: 40px;
  border-radius: 10px;
  border: 1px solid rgba(79, 70, 229, 0.2);
  background: rgba(255, 255, 255, 0.78);
  color: #3730a3;
  font-size: 13px;
  font-weight: 700;
  cursor: pointer;
}

.safeCard {
  background: linear-gradient(160deg, #f97316, #f59e0b);
}

.safeCard h3,
.safeCard p {
  color: #fff;
}

.safeBtn {
  margin-top: 14px;
  width: 100%;
  height: 38px;
  border: none;
  border-radius: 999px;
  font-weight: 800;
  color: #ea580c;
  background: #fff;
  cursor: pointer;
}
.safeList {
  margin: 10px 0 0 0;
  padding-left: 18px;
  color: #fff;
  font-size: 13px;
  line-height: 1.6;
}

.dialogRow {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 10px;
}

@media (max-width: 1200px) {
  .contentGrid {
    grid-template-columns: 1fr;
  }
  .leftRail {
    grid-template-columns: repeat(2, minmax(0, 1fr));
    display: grid;
  }
}
@media (max-width: 900px) {
  .profilePage {
    --cover-height: 180px;
    --avatar-size: 180px;
    --hero-width: 100%;
  }
  .profileInfo,
  .contentGrid {
    width: 100%;
  }
  .profileInfo {
    margin-top: 10px;
    border-radius: 16px;
    padding: 16px 14px;
    grid-template-columns: 1fr;
  }
  .profileInfo__main {
    width: 100%;
    min-height: auto;
    padding-left: 0;
    padding-top: 0;
  }
  .profileInfo__main .avatar--inline {
    position: static;
    margin-right: 0;
    margin-bottom: 10px;
  }
  .cover {
    height: var(--cover-height);
  }
  .profileInfo {
    flex-direction: column;
    align-items: flex-start;
  }
  .name {
    font-size: 32px;
  }
}
@media (max-width: 520px) {
  .page {
    margin: -12px -12px 0;
  }
  .profilePage {
    --avatar-size: 140px;
  }
  .identityCard {
    padding: 0 0 14px;
  }
  .moduleActions {
    grid-template-columns: 1fr;
  }
  .quickTabs {
    flex-direction: column;
  }
  .quickTabs__item {
    width: 100%;
  }
  .leftRail {
    grid-template-columns: 1fr;
  }
  .money {
    font-size: 32px;
  }
}
@media (max-width: 720px) {
  .dialogRow {
    grid-template-columns: 1fr;
  }
}
</style>

