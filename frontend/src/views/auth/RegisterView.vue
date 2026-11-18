<script setup>
import { reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { useRoute, useRouter } from 'vue-router'
import { registerAccount } from '../../api/auth'

const route = useRoute()
const router = useRouter()
const form = reactive({ username: '', displayName: '', password: '', confirmPassword: '' })
const busy = ref(false)
const error = ref('')

async function submit() {
  if (busy.value) return
  error.value = ''
  if (!form.username.trim() || !form.displayName.trim()) {
    error.value = '请输入用户名和显示名称。'
    return
  }
  if (Array.from(form.password).length < 8 || new TextEncoder().encode(form.password).length > 72) {
    error.value = '密码至少 8 个字符，且不超过 72 个 UTF-8 字节。'
    return
  }
  if (form.password !== form.confirmPassword) {
    error.value = '两次输入的密码不一致。'
    return
  }

  busy.value = true
  try {
    await registerAccount({
      username: form.username,
      displayName: form.displayName,
      password: form.password,
    })
    ElMessage.success('注册成功，请登录')
    await router.replace({ name: 'login', query: { redirect: route.query.redirect } })
  } catch (e) {
    error.value = e.message
  } finally {
    busy.value = false
  }
}
</script>

<template>
  <el-card class="auth-card">
    <h1>注册车主账号</h1>
    <el-alert v-if="error" class="auth-error" :title="error" type="error" :closable="false" show-icon />
    <el-form label-position="top" @submit.prevent="submit">
      <el-form-item label="用户名">
        <el-input v-model="form.username" autocomplete="username" :maxlength="50" />
      </el-form-item>
      <el-form-item label="显示名称">
        <el-input v-model="form.displayName" autocomplete="nickname" :maxlength="50" />
      </el-form-item>
      <el-form-item label="密码">
        <el-input v-model="form.password" type="password" autocomplete="new-password" show-password />
      </el-form-item>
      <el-form-item label="确认密码">
        <el-input v-model="form.confirmPassword" type="password" autocomplete="new-password" />
      </el-form-item>
      <el-button type="primary" native-type="submit" :loading="busy">注册</el-button>
    </el-form>
    <p class="auth-links">已有账号？<RouterLink :to="{ name: 'login', query: { redirect: route.query.redirect } }">返回登录</RouterLink></p>
  </el-card>
</template>
