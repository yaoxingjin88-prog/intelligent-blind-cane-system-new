import { createApp } from 'vue'
import App from './App.vue'
import router from './router'
import axios from 'axios'

// Element Plus 样式按需由 unplugin-vue-components 注入；消息类组件样式需全局引入
import 'element-plus/es/components/message/style/css'
import 'element-plus/es/components/message-box/style/css'
import 'element-plus/es/components/notification/style/css'

// 配置Axios
axios.defaults.baseURL = import.meta.env.VITE_API_BASE_URL || ''

const app = createApp(App)
app.config.globalProperties.$axios = axios
app.use(router)
app.mount('#app')
