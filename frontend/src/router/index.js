import { createRouter, createWebHistory } from 'vue-router'
import HomeView from '../views/HomeView.vue'
import PlaceholderView from '../views/PlaceholderView.vue'
import RoutesView from '../views/RoutesView.vue'
import NewsView from '../views/NewsView.vue'
import GalleryView from '../views/GalleryView.vue'
import CartView from '../views/CartView.vue'
import AiTripView from '../views/AiTripView.vue'
import AiQaView from '../views/AiQaView.vue'
import ProductView from '../views/ProductView.vue'
import SupportView from '../views/SupportView.vue'
import MeView from '../views/MeView.vue'
import LoginView from '../views/LoginView.vue'

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
    { path: '/cart', name: 'cart', component: CartView },
    { path: '/ai-trip', name: 'ai-trip', component: AiTripView },
    { path: '/ai-qa', name: 'ai-qa', component: AiQaView },
    { path: '/product', name: 'product', component: ProductView },
    { path: '/support', name: 'support', component: SupportView },
    { path: '/me', name: 'me', component: MeView },
    { path: '/login', name: 'login', component: LoginView },
  ],
})

export default router

