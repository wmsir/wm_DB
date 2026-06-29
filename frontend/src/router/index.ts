/**
 * Vue Router 路由配置
 *
 * 定义系统中的所有页面跳转规则。
 */
import { createRouter, createWebHistory } from 'vue-router'
import Login from '../views/Login.vue'
import Layout from '../views/Layout.vue'
import Dashboard from '../views/Dashboard.vue'
import TicketList from '../views/TicketList.vue'
import TicketDetail from '../views/TicketDetail.vue'
import WorkflowDesigner from '../views/WorkflowDesigner.vue'
import License from '../views/License.vue'
import AiSqlReview from '../views/AiSqlReview.vue'

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
    path: '/',
    component: Layout,
    meta: { requiresAuth: true },
    children: [
      {
        path: 'dashboard',
        name: 'Dashboard',
        component: Dashboard
      },
      {
        path: 'ticket-list',
        name: 'TicketList',
        component: TicketList
      },
      {
        path: 'ticket/:id',
        name: 'TicketDetail',
        component: TicketDetail
      },
      {
        path: 'workflow-designer',
        name: 'WorkflowDesigner',
        component: WorkflowDesigner
      },
      {
        path: 'license',
        name: 'License',
        component: License
      },
      {
        path: 'ai-sql-review',
        name: 'AiSqlReview',
        component: AiSqlReview
      }
    ]
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
    // 若已登录且目标是登录页，直接跳转 Dashboard
    next({ name: 'Dashboard' })
  } else {
    next()
  }
})

export default router
