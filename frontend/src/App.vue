<script setup>
import { computed, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { useRoute, useRouter } from 'vue-router'
import { currentUser, signOut } from './auth/session'
import { menus } from './router'

const route = useRoute()
const router = useRouter()
const signingOut = ref(false)
const menu = computed(() => menus[currentUser.value?.role] || [])

async function logout() {
  if (signingOut.value) return
  signingOut.value = true
  try {
    await signOut()
    await router.replace({ name: 'login' })
  } catch (error) {
    if (error.status === 401) {
      await router.replace({ name: 'login' })
    } else {
      ElMessage.error(error.message)
    }
  } finally {
    signingOut.value = false
  }
}
</script>

<template>
  <header class="site-header">
    <RouterLink class="brand" to="/">智能停车场管理系统</RouterLink>
    <div v-if="currentUser" class="account">
      <span>{{ currentUser.displayName }} · {{ currentUser.role === 'ADMIN' ? '管理员' : '车主' }}</span>
      <el-button :loading="signingOut" @click="logout">退出登录</el-button>
    </div>
  </header>
  <nav v-if="menu.length" class="site-nav" aria-label="系统菜单">
    <RouterLink v-for="[name, label] in menu" :key="name" :to="{ name }">{{ label }}</RouterLink>
  </nav>
  <main class="page" :class="{ 'guest-page': route.meta.guestOnly }">
    <RouterView />
  </main>
</template>

<style scoped>
.site-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  flex-wrap: wrap;
  gap: 12px;
  min-height: 64px;
  padding: 12px clamp(16px, 4vw, 48px);
  background: #fff;
  border-bottom: 1px solid #dde4ea;
}

.brand {
  color: #1f344b;
  font-weight: 650;
  font-size: 1.1rem;
  text-decoration: none;
}

.account {
  display: flex;
  align-items: center;
  gap: 12px;
  color: #45566a;
}

.site-nav {
  display: flex;
  flex-wrap: wrap;
  gap: 4px;
  padding: 8px clamp(16px, 4vw, 48px);
  background: #fff;
  border-bottom: 1px solid #dde4ea;
}

.site-nav a {
  padding: 6px 10px;
  border-radius: 4px;
  color: #45566a;
  text-decoration: none;
}

.site-nav a:hover,
.site-nav .router-link-exact-active {
  color: #12569d;
  background: #edf4ff;
}

.page {
  width: min(100%, 1240px);
  margin: 0 auto;
  padding: 24px clamp(16px, 4vw, 48px);
}

.guest-page {
  display: grid;
  justify-items: center;
  padding-top: clamp(24px, 8vh, 72px);
}
</style>
