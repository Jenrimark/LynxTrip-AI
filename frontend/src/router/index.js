import { createRouter, createWebHistory } from 'vue-router'
import HomeView from '../views/HomeView.vue'
import PlaceholderView from '../views/PlaceholderView.vue'

const router = createRouter({
  history: createWebHistory(),
  routes: [
    {
      path: '/',
      name: 'home',
      component: HomeView,
    },
    { path: '/create-trip', name: 'create-trip', component: HomeView },
    { path: '/routes', name: 'routes', component: PlaceholderView, props: { title: '旅游路线' } },
    { path: '/news', name: 'news', component: PlaceholderView, props: { title: '旅游资讯' } },
    { path: '/gallery', name: 'gallery', component: PlaceholderView, props: { title: '光影拾记' } },
    { path: '/cart', name: 'cart', component: PlaceholderView, props: { title: '购物车' } },
    { path: '/ai-trip', name: 'ai-trip', component: PlaceholderView, props: { title: 'AI规划行程' } },
    { path: '/ai-qa', name: 'ai-qa', component: PlaceholderView, props: { title: 'AI问答助手' } },
    { path: '/product', name: 'product', component: PlaceholderView, props: { title: '产品介绍' } },
    { path: '/support', name: 'support', component: PlaceholderView, props: { title: '联系客服' } },
    { path: '/me', name: 'me', component: PlaceholderView, props: { title: '个人中心' } },
  ],
})

export default router

