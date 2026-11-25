import { createRouter, createWebHistory } from 'vue-router'
import { setUnauthorizedHandler } from '../api/http'
import { clearSession, restoreSession } from '../auth/session'

const NoticeView = () => import('../views/RouteNoticeView.vue')
const pending = (path, name, title) => ({
  path, name, component: NoticeView, props: { title, message: '该模块尚未接入。' },
})

export const menus = {
  ADMIN: [
    ['admin-home', '管理首页'],
    ['admin-users', '账号管理'],
    ['admin-lots', '停车场'],
    ['admin-areas', '停车区域'],
    ['admin-spaces', '停车位'],
    ['admin-entries', '车辆入场'],
    ['admin-exits', '车辆离场'],
    ['admin-rules', '收费规则'],
    ['admin-records', '停车记录'],
  ],
  OWNER: [
    ['owner-vehicles', '我的车辆'],
    ['owner-records', '我的停车记录'],
  ],
}

const router = createRouter({
  history: createWebHistory(),
  routes: [
    { path: '/', redirect: { name: 'login' } },
    {
      path: '/login', name: 'login', meta: { guestOnly: true },
      component: () => import('../views/auth/LoginView.vue'),
    },
    {
      path: '/register', name: 'register', meta: { guestOnly: true },
      component: () => import('../views/auth/RegisterView.vue'),
    },
    {
      path: '/admin',
      meta: { role: 'ADMIN' },
      children: [
        {
          path: '', name: 'admin-home', component: NoticeView,
          props: { title: '管理首页', message: '统计模块尚未接入。可从菜单进入账号、停车场或停车区域管理。' },
        },
        { path: 'users', name: 'admin-users', component: () => import('../views/admin/UsersView.vue') },
        { path: 'lots', name: 'admin-lots', component: () => import('../views/admin/ParkingLotsView.vue') },
        { path: 'areas', name: 'admin-areas', component: () => import('../views/parking/AreaManage.vue') },
        pending('spaces', 'admin-spaces', '停车位'),
        pending('entries', 'admin-entries', '车辆入场'),
        pending('exits', 'admin-exits', '车辆离场'),
        pending('rules', 'admin-rules', '收费规则'),
        pending('records', 'admin-records', '停车记录'),
      ],
    },
    {
      path: '/owner',
      meta: { role: 'OWNER' },
      children: [
        { path: '', redirect: { name: 'owner-vehicles' } },
        pending('vehicles', 'owner-vehicles', '我的车辆'),
        pending('records', 'owner-records', '我的停车记录'),
      ],
    },
    {
      path: '/403', name: 'forbidden', component: NoticeView,
      props: { title: '无权访问', message: '当前账号没有访问该页面的权限。', showBack: true },
    },
    {
      path: '/:pathMatch(.*)*', name: 'not-found', component: NoticeView,
      props: { title: '页面不存在', message: '请检查地址，或从菜单选择其他页面。', showBack: true },
    },
  ],
})

export function postLoginRoute(user, redirect) {
  // 只回到与当前角色匹配的已知站内页面，不接受外部地址。
  if (typeof redirect === 'string' && redirect.startsWith('/')
      && !redirect.startsWith('//') && !redirect.includes('\\')) {
    const target = router.resolve(redirect)
    if (target.meta.role === user.role && target.name) {
      return { name: target.name, query: target.query, hash: target.hash }
    }
  }
  return { name: user.role === 'ADMIN' ? 'admin-home' : 'owner-vehicles' }
}

function toLogin(to, extraQuery = {}) {
  return { name: 'login', query: { redirect: to.fullPath, ...extraQuery } }
}

router.beforeEach(async (to) => {
  if (!to.meta.role && !to.meta.guestOnly) return true

  let user
  try {
    user = await restoreSession()
  } catch {
    // 网络故障不伪装成 401；仍可打开登录页，并提供连接失败提示。
    return to.meta.role ? toLogin(to, { offline: '1' }) : true
  }

  if (to.meta.guestOnly && user) return postLoginRoute(user, to.query.redirect)
  if (to.meta.role && !user) return toLogin(to)
  if (to.meta.role && to.meta.role !== user.role) return { name: 'forbidden' }
  return true
})

setUnauthorizedHandler(() => {
  clearSession()
  const route = router.currentRoute.value
  if (route.meta.role) {
    void router.replace(toLogin(route)).catch(console.error)
  }
})

export default router
