<script setup>
import { onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { listUsers, setUserStatus } from '../../api/auth'
import { currentUser } from '../../auth/session'

const filters = reactive({ role: '', status: '' })
const users = ref([])
const loading = ref(false)
const changing = ref(null)
const error = ref('')

async function loadUsers() {
  if (loading.value) return
  loading.value = true
  error.value = ''
  try {
    users.value = await listUsers({
      role: filters.role || undefined,
      status: filters.status || undefined,
    })
  } catch (e) {
    error.value = e.message
  } finally {
    loading.value = false
  }
}

async function toggleStatus(user) {
  if (changing.value !== null || user.id === currentUser.value?.id) return
  const status = user.status === 'ACTIVE' ? 'DISABLED' : 'ACTIVE'
  const label = status === 'DISABLED' ? '停用' : '启用'
  changing.value = user.id
  try {
    await ElMessageBox.confirm(
      `确认${label}账号「${user.username}」吗？`, '修改账号状态',
      { confirmButtonText: '确认', cancelButtonText: '取消', type: 'warning' },
    )
    await setUserStatus(user.id, status)
    ElMessage.success(`${label}成功`)
    await loadUsers()
  } catch (e) {
    if (e !== 'cancel' && e !== 'close') ElMessage.error(e.message || '操作失败')
  } finally {
    changing.value = null
  }
}

onMounted(loadUsers)
</script>

<template>
  <section>
    <h1>账号管理</h1>
    <el-form class="users-filters" inline @submit.prevent="loadUsers">
      <el-form-item label="角色">
        <el-select v-model="filters.role" clearable placeholder="全部角色" :disabled="loading || changing !== null">
          <el-option label="管理员" value="ADMIN" />
          <el-option label="车主" value="OWNER" />
        </el-select>
      </el-form-item>
      <el-form-item label="状态">
        <el-select v-model="filters.status" clearable placeholder="全部状态" :disabled="loading || changing !== null">
          <el-option label="启用" value="ACTIVE" />
          <el-option label="停用" value="DISABLED" />
        </el-select>
      </el-form-item>
      <el-form-item>
        <el-button type="primary" native-type="submit" :loading="loading" :disabled="changing !== null">查询</el-button>
      </el-form-item>
    </el-form>
    <el-alert v-if="error" :title="error" type="error" :closable="false" show-icon />
    <el-table v-loading="loading" :data="users" row-key="id" empty-text="没有符合条件的账号">
      <el-table-column prop="username" label="用户名" min-width="130" />
      <el-table-column prop="displayName" label="显示名称" min-width="130" />
      <el-table-column label="角色" width="105">
        <template #default="{ row }">{{ row.role === 'ADMIN' ? '管理员' : '车主' }}</template>
      </el-table-column>
      <el-table-column label="状态" width="100">
        <template #default="{ row }">
          <el-tag :type="row.status === 'ACTIVE' ? 'success' : 'info'">
            {{ row.status === 'ACTIVE' ? '启用' : '停用' }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column label="操作" width="140">
        <template #default="{ row }">
          <span v-if="row.id === currentUser?.id">当前账号</span>
          <el-button v-else link :type="row.status === 'ACTIVE' ? 'danger' : 'primary'"
                     :loading="changing === row.id" :disabled="loading || changing !== null" @click="toggleStatus(row)">
            {{ row.status === 'ACTIVE' ? '停用' : '启用' }}
          </el-button>
        </template>
      </el-table-column>
    </el-table>
  </section>
</template>

<style scoped>
.users-filters .el-select {
  width: 135px;
}
</style>
