<script setup>
import { onMounted, ref } from 'vue'
import axios from 'axios'

const instructors = ref([])
const loading = ref(true)
const error = ref('')

onMounted(async () => {
  try {
    const response = await axios.get(
        'http://localhost:8081/api/instructors'
    )
    instructors.value = response.data
  } catch (e) {
    error.value = '教师数据加载失败'
    console.error(e)
  } finally {
    loading.value = false
  }
})
</script>

<template>
  <main>
    <h1>教师信息</h1>

    <el-alert
        v-if="error"
        :title="error"
        type="error"
        show-icon
        :closable="false"
    />

    <el-table
        v-else
        v-loading="loading"
        :data="instructors"
        border
    >
      <el-table-column prop="id" label="编号" />
      <el-table-column prop="name" label="姓名" />
      <el-table-column prop="deptName" label="院系" />
      <el-table-column prop="salary" label="工资" />
    </el-table>
  </main>
</template>

<style scoped>
main {
  width: min(100% - 40px, 1000px);
  margin: 48px auto;
}

h1 {
  margin: 0 0 24px;
  font-size: 28px;
}
</style>