<script setup>

</script>

<template>
  <div class="login-container">
    <h2>智慧停车场管理系统</h2>
    <!-- 输入框绑定变量 -->
    <input v-model="loginForm.username" placeholder="请输入管理员账号" />
    <input v-model="loginForm.password" type="password" placeholder="请输入密码" />

    <!-- 按钮绑定点击事件 -->
    <button @click="handleLogin">登录</button>
  </div>
</template>

<style scoped>

</style>