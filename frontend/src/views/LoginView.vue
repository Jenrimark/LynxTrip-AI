<script setup>
import { onMounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { isLoggedIn, loginWithPassword, registerUser } from '../services/lynxDb'

const route = useRoute()
const router = useRouter()

const loginForm = ref({ yonghuming: '', mima: '' })
const regForm = ref({ yonghuming: '', mima: '', mima2: '', xingming: '', lianxidianhua: '', xingbie: '男' })
const showRegister = ref(false)

function redirectAfterAuth() {
  const r = route.query.redirect
  const path = typeof r === 'string' && r.startsWith('/') ? r : '/me'
  router.replace(path)
}

onMounted(() => {
  if (isLoggedIn()) redirectAfterAuth()
})

function submitLogin() {
  const res = loginWithPassword(loginForm.value.yonghuming, loginForm.value.mima)
  if (!res.ok) {
    ElMessage.error(res.message)
    return
  }
  ElMessage.success('登录成功')
  redirectAfterAuth()
}

function submitRegister() {
  if (!regForm.value.yonghuming?.trim() || !regForm.value.mima) {
    ElMessage.warning('请填写用户名与密码')
    return
  }
  if (regForm.value.mima !== regForm.value.mima2) {
    ElMessage.warning('两次密码不一致')
    return
  }
  const id = registerUser({
    yonghuming: regForm.value.yonghuming.trim(),
    mima: regForm.value.mima,
    xingming: regForm.value.xingming?.trim() || '',
    lianxidianhua: regForm.value.lianxidianhua?.trim() || '',
    xingbie: regForm.value.xingbie || '—',
  })
  if (id == null) {
    ElMessage.error('注册失败，用户名可能已存在')
    return
  }
  ElMessage.success('注册成功，已自动登录')
  redirectAfterAuth()
}
</script>

<template>
  <section class="login page">
    <div class="login__card lynx-card lynx-card--glass">
      <h1 class="login__title">登录</h1>

      <el-form v-if="!showRegister" label-position="top" class="login__form" @submit.prevent="submitLogin">
        <el-form-item label="用户名">
          <el-input v-model="loginForm.yonghuming" autocomplete="username" placeholder="请输入用户名" />
        </el-form-item>
        <el-form-item label="密码">
          <el-input v-model="loginForm.mima" type="password" show-password autocomplete="current-password" placeholder="请输入密码" />
        </el-form-item>
        <div class="login__actions">
          <el-button type="primary" native-type="submit" class="login__btn">登录</el-button>
          <el-button text type="primary" @click="showRegister = true">没有账号？注册</el-button>
        </div>
      </el-form>

      <el-form v-else label-position="top" class="login__form" @submit.prevent="submitRegister">
        <el-form-item label="用户名">
          <el-input v-model="regForm.yonghuming" autocomplete="username" />
        </el-form-item>
        <el-form-item label="密码">
          <el-input v-model="regForm.mima" type="password" show-password autocomplete="new-password" />
        </el-form-item>
        <el-form-item label="确认密码">
          <el-input v-model="regForm.mima2" type="password" show-password autocomplete="new-password" />
        </el-form-item>
        <el-form-item label="姓名（选填）">
          <el-input v-model="regForm.xingming" />
        </el-form-item>
        <el-form-item label="电话（选填）">
          <el-input v-model="regForm.lianxidianhua" />
        </el-form-item>
        <el-form-item label="性别">
          <el-radio-group v-model="regForm.xingbie">
            <el-radio label="男">男</el-radio>
            <el-radio label="女">女</el-radio>
          </el-radio-group>
        </el-form-item>
        <div class="login__actions">
          <el-button type="primary" native-type="submit" class="login__btn">注册并登录</el-button>
          <el-button text @click="showRegister = false">返回登录</el-button>
        </div>
      </el-form>
    </div>
  </section>
</template>

<style scoped lang="scss">
.login {
  min-height: calc(100vh - 56px);
  display: flex;
  align-items: center;
  justify-content: center;
  padding: var(--space-lg);
  box-sizing: border-box;
}
.login__card {
  width: 100%;
  max-width: 420px;
  padding: 28px 24px;
  border-radius: 18px;
}
.login__title {
  margin: 0;
  font-size: 22px;
  font-weight: 900;
  color: #0f172a;
}
.login__form {
  margin-top: 16px;
}
.login__actions {
  display: flex;
  flex-direction: column;
  gap: 10px;
  margin-top: 8px;
}
.login__btn {
  width: 100%;
}
</style>
