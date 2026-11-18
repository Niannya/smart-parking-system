<script setup>
import { reactive, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { signIn } from '../../auth/session'
import { postLoginRoute } from '../../router'

const route = useRoute()
const router = useRouter()
const form = reactive({ username: '', password: '' })
const busy = ref(false)
const error = ref('')

async function submit() {
  if (busy.value) return
  error.value = ''
  if (!form.username.trim() || !form.password) {
    error.value = '请输入用户名和密码。'
    return
  }

  busy.value = true
  try {
    const user = await signIn({ username: form.username, password: form.password })
    await router.replace(postLoginRoute(user, route.query.redirect))
  } catch (e) {
    error.value = e.message
  } finally {
    form.password = ''
    busy.value = false
  }
}
</script>

<template>
  <el-card class="auth-card">
    <h1>登录</h1>
    <el-alert
      v-if="error || route.query.offline === '1'"
      class="auth-error"
      :title="error || '暂时无法连接后端，请确认服务已启动后重试。'"
      type="error"
      :closable="false"
      show-icon
    />
    <el-form label-position="top" @submit.prevent="submit">
      <el-form-item label="用户名">
        <el-input v-model="form.username" autocomplete="username" :maxlength="50" />
      </el-form-item>
      <el-form-item label="密码">
        <el-input v-model="form.password" type="password" autocomplete="current-password" show-password />
      </el-form-item>
      <el-button type="primary" native-type="submit" :loading="busy">登录</el-button>
    </el-form>
    <p class="auth-links">没有账号？<RouterLink :to="{ name: 'register', query: { redirect: route.query.redirect } }">注册车主账号</RouterLink></p>
  </el-card>
</template>
