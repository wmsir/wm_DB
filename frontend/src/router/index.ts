/**
 * Vue Router 路由配置
 *
 * 定义系统中的所有页面跳转规则。
 */
import { createRouter, createWebHistory } from 'vue-router'
import Login from '../views/Login.vue'
import TicketDetail from '../views/TicketDetail.vue'
import WorkflowDesigner from '../views/WorkflowDesigner.vue'

import { useUserStore } from '../store/user'

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
    meta: { requiresAuth: true }
  },
  {
    path: '/workflow-designer',
    name: 'WorkflowDesigner',
    component: WorkflowDesigner,
    meta: { requiresAuth: true }
  }
]

const router = createRouter({
  history: createWebHistory(),
  routes
})

// 全局前置路由守卫
router.beforeEach((to, _from, next) => {
  const userStore = useUserStore()

  if (to.meta.requiresAuth && !userStore.isAuthenticated) {
    // 若路由需要鉴权且未登录，重定向到登录页
    next({ name: 'Login' })
  } else if (to.name === 'Login' && userStore.isAuthenticated) {
    // 若已登录且目标是登录页，直接跳转主页或具体工单（此处简化跳至示例）
    next({ path: '/ticket/1' })
  } else {
    next()
  }
})

export default router
