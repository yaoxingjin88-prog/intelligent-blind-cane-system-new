import { createApp } from 'vue'
import App from './App.vue'
import router from './router'
import ElementPlus from 'element-plus'
import 'element-plus/dist/index.css'
import axios from 'axios'

// 配置Axios
axios.defaults.baseURL = import.meta.env.VITE_API_BASE_URL || ''

const app = createApp(App)
app.config.globalProperties.$axios = axios
app.use(ElementPlus)
app.use(router)
app.mount('#app')
