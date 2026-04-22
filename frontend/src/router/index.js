import { createRouter, createWebHistory } from 'vue-router'
import { isLoggedInRemote } from '../services/auth'
import HomeView from '../views/HomeView.vue'
import PlaceholderView from '../views/PlaceholderView.vue'
import RoutesView from '../views/RoutesView.vue'
import NewsView from '../views/NewsView.vue'
import GalleryView from '../views/GalleryView.vue'
import CartView from '../views/CartView.vue'
import AiQaView from '../views/AiQaView.vue'
import ProductView from '../views/ProductView.vue'
import SupportView from '../views/SupportView.vue'
import MeView from '../views/MeView.vue'
import LoginView from '../views/LoginView.vue'
import ShanheStoreView from '../views/ShanheStoreView.vue'
import MyItineraryView from '../views/MyItineraryView.vue'
import MyMapView from '../views/MyMapView.vue'

const router = createRouter({
  history: createWebHistory(),
  routes: [
    {
      path: '/',
      name: 'home',
      component: HomeView,
    },
    { path: '/create-trip', name: 'create-trip', component: HomeView },
    { path: '/routes', name: 'routes', component: RoutesView },
    { path: '/news', name: 'news', component: NewsView },
    { path: '/gallery', name: 'gallery', component: GalleryView },
    { path: '/shanhe-store', name: 'shanhe-store', component: ShanheStoreView },
    { path: '/cart', name: 'cart', component: CartView },
    { path: '/ai-trip', redirect: '/my-itinerary/workspace' },
    { path: '/my-itinerary/workspace', name: 'my-itinerary-workspace', component: MyItineraryView },
    { path: '/my-map', name: 'my-map', component: MyMapView },
    { path: '/ai-qa', name: 'ai-qa', component: AiQaView },
    { path: '/product', name: 'product', component: ProductView },
    { path: '/support', name: 'support', component: SupportView },
    { path: '/me', name: 'me', component: MeView },
    { path: '/login', name: 'login', component: LoginView },
  ],
})

/** 未登录仅可浏览：主题简旅、资讯、光影拾记、产品介绍；登录页始终可进 */
const GUEST_ROUTE_NAMES = new Set(['routes', 'news', 'gallery', 'product', 'shanhe-store', 'my-map'])

router.beforeEach(async (to, _from, next) => {
  if (to.name === 'login') {
    next()
    return
  }
  if (await isLoggedInRemote()) {
    next()
    return
  }
  const name = String(to.name ?? '')
  if (name === 'home' || to.path === '/') {
    next({ path: '/routes', replace: true })
    return
  }
  if (GUEST_ROUTE_NAMES.has(name)) {
    next()
    return
  }
  next({ name: 'login', query: { redirect: to.fullPath } })
})

export default router

