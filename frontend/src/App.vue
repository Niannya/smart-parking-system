<template>
  <div class="app">
    <header v-if="session" class="topbar">
      <span class="brand">智能停车场管理系统</span>
      <nav class="nav">
        <template v-if="session.role === 'ADMIN'">
          <router-link to="/admin/fee-rules">收费规则</router-link>
          <router-link to="/admin/parking-exits">离场缴费</router-link>
          <router-link to="/admin/parking-records">停车记录</router-link>
        </template>
        <template v-else>
          <router-link to="/owner/parking-records">我的停车记录</router-link>
        </template>
      </nav>
      <span class="user">
        {{ session.displayName }}（{{ session.role === 'ADMIN' ? '管理员' : '车主' }}）
        <button class="link" @click="logout">退出</button>
      </span>
    </header>
    <main class="content">
      <router-view />
    </main>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import http from './http'
import { loadSession, clearSession } from './auth'

const session = ref(loadSession())
const router = useRouter()

onMounted(() => {
  // 刷新后以服务端会话为准
  http.get('/api/auth/me').then((res) => {
    if (res && res.data) {
      session.value = res.data
    }
  }).catch(() => {
    session.value = null
  })
})

async function logout() {
  try {
    await http.post('/api/auth/logout')
  } catch (e) {
    // 后端失败也清理本地会话
  }
  clearSession()
  session.value = null
  router.push('/login')
}
</script>

<style>
* { box-sizing: border-box; }
body { margin: 0; font-family: "Microsoft YaHei", "PingFang SC", sans-serif; background: #f5f6f8; color: #24292f; }
.app { min-height: 100vh; }
.topbar { display: flex; align-items: center; gap: 24px; padding: 0 24px; height: 52px; background: #1f2937; color: #fff; }
.topbar .brand { font-weight: 600; font-size: 15px; }
.topbar .nav { display: flex; gap: 16px; flex: 1; }
.topbar .nav a { color: #cbd5e1; text-decoration: none; font-size: 14px; padding: 4px 2px; }
.topbar .nav a.router-link-active { color: #fff; border-bottom: 2px solid #3b82f6; }
.topbar .user { font-size: 13px; color: #e5e7eb; }
.topbar .link { background: none; border: none; color: #93c5fd; cursor: pointer; margin-left: 8px; font-size: 13px; }
.content { padding: 24px; max-width: 1100px; margin: 0 auto; }
.panel { background: #fff; border: 1px solid #e5e7eb; border-radius: 8px; padding: 20px; margin-bottom: 16px; }
.panel h2 { margin: 0 0 16px; font-size: 17px; }
.panel h3 { margin: 0 0 12px; font-size: 15px; }
.row { display: flex; gap: 12px; flex-wrap: wrap; align-items: flex-end; }
.field { display: flex; flex-direction: column; gap: 4px; }
.field label { font-size: 12px; color: #6b7280; }
input, select { padding: 7px 10px; border: 1px solid #d1d5db; border-radius: 6px; font-size: 14px; min-width: 160px; }
button.primary { background: #2563eb; color: #fff; border: none; border-radius: 6px; padding: 8px 16px; font-size: 14px; cursor: pointer; }
button.primary:disabled { background: #93c5fd; cursor: not-allowed; }
button.danger { background: #dc2626; color: #fff; border: none; border-radius: 6px; padding: 8px 16px; font-size: 14px; cursor: pointer; }
button.ghost { background: #fff; color: #374151; border: 1px solid #d1d5db; border-radius: 6px; padding: 8px 16px; font-size: 14px; cursor: pointer; }
table { width: 100%; border-collapse: collapse; font-size: 13px; }
th, td { border-bottom: 1px solid #eef0f2; padding: 8px 10px; text-align: left; white-space: nowrap; }
th { color: #6b7280; font-weight: 500; background: #fafbfc; }
.tag { display: inline-block; padding: 1px 8px; border-radius: 10px; font-size: 12px; }
.tag.parking { background: #fef3c7; color: #92400e; }
.tag.completed { background: #d1fae5; color: #065f46; }
.tag.abnormal { background: #fee2e2; color: #991b1b; }
.tag.active { background: #dbeafe; color: #1e40af; }
.tag.inactive { background: #f3f4f6; color: #6b7280; }
.error { background: #fef2f2; border: 1px solid #fecaca; color: #991b1b; border-radius: 6px; padding: 10px 14px; font-size: 13px; margin-bottom: 12px; }
.success { background: #f0fdf4; border: 1px solid #bbf7d0; color: #166534; border-radius: 6px; padding: 10px 14px; font-size: 13px; margin-bottom: 12px; }
.muted { color: #9ca3af; font-size: 13px; }
.mono { font-family: Consolas, monospace; font-size: 12px; }
.amount { font-weight: 600; color: #b91c1c; }
.form-error { color: #b91c1c; font-size: 12px; min-height: 16px; }
</style>
