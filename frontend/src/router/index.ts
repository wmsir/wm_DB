/**
 * Vue Router 路由配置
 *
 * 定义系统中的所有页面跳转规则。
 */
import { createRouter, createWebHistory } from 'vue-router'
import Login from '../views/Login.vue'
import TicketDetail from '../views/TicketDetail.vue'
import WorkflowDesigner from '../views/WorkflowDesigner.vue'

// 路由规则表
const routes = [
  {
    path: '/',
    redirect: '/login'
  },
  {
    path: '/login',
    name: 'Login',
    component: Login
  },
  {
    path: '/ticket/:id',
    name: 'TicketDetail',
    component: TicketDetail,
    // 实际项目中此处应配置 auth metadata 路由守卫
  },
  {
    path: '/workflow-designer',
    name: 'WorkflowDesigner',
    component: WorkflowDesigner
  }
]

const router = createRouter({
  history: createWebHistory(),
  routes
})

export default router
