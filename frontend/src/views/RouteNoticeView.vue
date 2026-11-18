<script setup>
defineProps({
  title: { type: String, required: true },
  message: { type: String, required: true },
  showBack: { type: Boolean, default: false },
})
</script>

<template>
  <el-card>
    <h1>{{ title }}</h1>
    <p>{{ message }}</p>
    <RouterLink v-if="showBack" to="/">返回入口</RouterLink>
  </el-card>
</template>
