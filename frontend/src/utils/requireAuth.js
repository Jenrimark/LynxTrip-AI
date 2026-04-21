import { ElMessage } from 'element-plus'
import { isLoggedInRemote } from '../services/auth'

/**
 * 未登录时提示并跳转登录页；已登录返回 true。
 * @param {import('vue-router').Router} router
 * @param {{ message?: string, redirect?: string }} [options]
 */
export async function requireLogin(router, options = {}) {
  if (await isLoggedInRemote()) return true
  const msg = options.message ?? '请先登录后继续'
  const redir = options.redirect ?? router.currentRoute.value.fullPath
  ElMessage.warning(msg)
  router.push({ name: 'login', query: { redirect: redir } })
  return false
}
