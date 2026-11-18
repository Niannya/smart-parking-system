import { createApp } from 'vue'
import { createRouter, createWebHistory } from 'vue-router'
import App from './App.vue'
import { loadSession } from './auth'

// 路由守卫：未登录先去登录页；ADMIN/OWNER 按角色分流
const routes = [
  { path: '/', redirect: '/login' },
  {
    path: '/login',
    name: 'login',
    component: () => import('./views/LoginView.vue'),
    meta: { public: true }
  },
  {
    path: '/admin/fee-rules',
    name: 'admin-fee-rules',
    component: () => import('./views/admin/FeeRulesView.vue'),
    meta: { role: 'ADMIN' }
  },
  {
    path: '/admin/parking-exits',
    name: 'admin-parking-exits',
    component: () => import('./views/admin/ParkingExitsView.vue'),
    meta: { role: 'ADMIN' }
  },
  {
    path: '/admin/parking-records',
    name: 'admin-parking-records',
    component: () => import('./views/admin/ParkingRecordsView.vue'),
    meta: { role: 'ADMIN' }
  },
  {
    path: '/owner/parking-records',
    name: 'owner-parking-records',
    component: () => import('./views/owner/ParkingRecordsView.vue'),
    meta: { role: 'OWNER' }
  }
]

const router = createRouter({
  history: createWebHistory(),
  routes
})

router.beforeEach((to) => {
  const session = loadSession()
  if (to.meta.public) {
    if (session) {
      return session.role === 'ADMIN' ? '/admin/fee-rules' : '/owner/parking-records'
    }
    return true
  }
  if (!session) {
    return { path: '/login', query: { redirect: to.fullPath } }
  }
  if (to.meta.role && to.meta.role !== session.role) {
    return session.role === 'ADMIN' ? '/admin/fee-rules' : '/owner/parking-records'
  }
  return true
})

createApp(App).use(router).mount('#app')
