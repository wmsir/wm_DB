import { createApp } from 'vue'
import './style.css'
import App from './App.vue'
import router from './router'
import ElementPlus from 'element-plus'
import 'element-plus/dist/index.css'
import { watermark } from './directives/watermark'

const app = createApp(App)

app.use(router)
app.use(ElementPlus)
app.directive('watermark', watermark)

app.mount('#app')
