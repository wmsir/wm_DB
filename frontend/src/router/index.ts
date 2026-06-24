import { createRouter, createWebHistory } from 'vue-router'
import Login from '../views/Login.vue'
import TicketDetail from '../views/TicketDetail.vue'

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
    // Typically requires auth metadata here
  }
]

const router = createRouter({
  history: createWebHistory(),
  routes
})

export default router
