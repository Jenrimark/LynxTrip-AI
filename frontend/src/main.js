import { createApp } from 'vue'
import App from './App.vue'

import { createPinia } from 'pinia'
import router from './router'

import ElementPlus from 'element-plus'
import 'element-plus/dist/index.css'

import './styles/main.scss'

createApp(App).use(createPinia()).use(router).use(ElementPlus).mount('#app')
