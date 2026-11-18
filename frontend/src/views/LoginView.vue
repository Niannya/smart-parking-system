<template>
  <div class="login-wrap">
    <form class="login-panel" @submit.prevent="submit">
      <h2>智能停车场管理系统</h2>
      <p class="muted">请使用演示账号登录（admin / owner1 / owner2）</p>
      <div class="field">
        <label>用户名</label>
        <input v-model="form.username" autocomplete="username" placeholder="admin" />
      </div>
      <div class="field">
        <label>密码</label>
        <input v-model="form.password" type="password" autocomplete="current-password" placeholder="••••••" />
      </div>
      <div class="form-error">{{ error }}</div>
      <button class="primary" type="submit" :disabled="loading">{{ loading ? '登录中…' : '登录' }}</button>
    </form>
  </div>
</template>

<script setup>
import { reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import http from '../http'
import { saveSession } from '../auth'

const form = reactive({ username: '', password: '' })
const error = ref('')
const loading = ref(false)
const router = useRouter()

async function submit() {
  error.value = ''
  if (!form.username || !form.password) {
    error.value = '用户名和密码不能为空'
    return
  }
  loading.value = true
  try {
    const res = await http.post('/api/auth/login', {
      username: form.username,
      password: form.password
    })
    saveSession(res.data)
    router.push(res.data.role === 'ADMIN' ? '/admin/fee-rules' : '/owner/parking-records')
  } catch (e) {
    error.value = e.message || '登录失败'
  } finally {
    loading.value = false
  }
}
</script>

<style scoped>
.login-wrap { min-height: 100vh; display: flex; align-items: center; justify-content: center; background: #f5f6f8; }
.login-panel { width: 340px; background: #fff; border: 1px solid #e5e7eb; border-radius: 10px; padding: 28px; display: flex; flex-direction: column; gap: 14px; }
.login-panel h2 { margin: 0; font-size: 19px; text-align: center; }
.login-panel .muted { margin: 0; text-align: center; }
.login-panel input { width: 100%; }
.login-panel button { width: 100%; }
</style>
