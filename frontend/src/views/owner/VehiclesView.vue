<script setup>
import { onMounted, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  changeVehicleStatus,
  createVehicle,
  listMyVehicles,
} from '../../api/ownerVehicle'

const PLATE_PATTERN = /^[京津沪渝冀豫云辽黑湘皖鲁新苏浙赣鄂桂甘晋蒙陕吉闽贵粤青藏川宁琼][A-Z][A-HJ-NP-Z0-9]{5,6}$/

const vehicles = ref([])
const loading = ref(false)
const plate = ref('')
const adding = ref(false)

function normalizePlate(value) {
  return (value || '').replace(/\s+/g, '').toUpperCase()
}

function formatTime(value) {
  return value ? new Date(value).toLocaleString('zh-CN', { hour12: false }) : ''
}

async function loadVehicles() {
  loading.value = true
  try {
    vehicles.value = await listMyVehicles()
  } catch (e) {
    ElMessage.error(e.message)
  } finally {
    loading.value = false
  }
}

async function bindVehicle() {
  const normalized = normalizePlate(plate.value)
  if (!PLATE_PATTERN.test(normalized)) {
    ElMessage.warning('车牌格式不正确，示例：京A12345')
    return
  }

  adding.value = true
  try {
    const vehicle = await createVehicle({ licensePlate: normalized })
    ElMessage.success(`绑定成功：${vehicle.licensePlate}`)
    plate.value = ''
    await loadVehicles()
  } catch (e) {
    ElMessage.error(e.message)
  } finally {
    adding.value = false
  }
}

async function toggleStatus(row) {
  const target = row.status === 'ACTIVE' ? 'DISABLED' : 'ACTIVE'
  const actionText = target === 'DISABLED' ? '停用' : '启用'
  try {
    await ElMessageBox.confirm(
      `确认${actionText}车辆「${row.licensePlate}」吗？${target === 'DISABLED' ? '停用后新入场不再自动关联该车辆，已有停车记录仍保留归属。' : ''}`,
      `${actionText}车辆`,
      { type: 'warning', confirmButtonText: '确认', cancelButtonText: '取消' },
    )
  } catch {
    return
  }
  try {
    await changeVehicleStatus(row.id, target)
    ElMessage.success(`${actionText}成功`)
    await loadVehicles()
  } catch (e) {
    ElMessage.error(e.message)
  }
}

onMounted(loadVehicles)
</script>

<template>
  <div class="page">
    <header class="page-header">
      <div>
        <h2>我的车辆</h2>
        <p class="subtitle">
          车牌在全系统唯一，绑定后管理员登记入场时会自动关联到本人账号；停用只影响后续入场，历史停车记录仍归本人。
        </p>
      </div>
      <el-button @click="loadVehicles">刷新</el-button>
    </header>

    <el-card shadow="never" class="bind-card">
      <div class="bind-row">
        <el-input
          v-model="plate"
          placeholder="输入车牌，例如 京A12345"
          maxlength="8"
          clearable
          class="plate-input"
          @keyup.enter="bindVehicle"
        />
        <el-button type="primary" :loading="adding" @click="bindVehicle">绑定车辆</el-button>
        <span class="hint">提交时自动去空格并转大写；特殊车牌不在本课设范围。</span>
      </div>
    </el-card>

    <el-table v-loading="loading" :data="vehicles" border stripe>
      <el-table-column label="车牌号码" min-width="160">
        <template #default="{ row }">
          <span class="plate">{{ row.licensePlate }}</span>
        </template>
      </el-table-column>
      <el-table-column label="状态" width="110" align="center">
        <template #default="{ row }">
          <el-tag :type="row.status === 'ACTIVE' ? 'success' : 'info'">
            {{ row.status === 'ACTIVE' ? '启用' : '停用' }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column label="绑定时间" width="190">
        <template #default="{ row }">{{ formatTime(row.createdAt) }}</template>
      </el-table-column>
      <el-table-column label="更新时间" width="190">
        <template #default="{ row }">{{ formatTime(row.updatedAt) }}</template>
      </el-table-column>
      <el-table-column label="操作" width="120" align="center">
        <template #default="{ row }">
          <el-button link type="warning" @click="toggleStatus(row)">
            {{ row.status === 'ACTIVE' ? '停用' : '启用' }}
          </el-button>
        </template>
      </el-table-column>
      <template #empty>
        <span>还没有绑定车辆，在上方输入车牌即可绑定</span>
      </template>
    </el-table>

    <p class="footnote">
      说明：本页只提供绑定与启停，不提供删除或换绑；如需变更归属请联系管理员处理。
    </p>
  </div>
</template>

<style scoped>
.page {
  width: min(100% - 40px, 1040px);
  margin: 24px auto;
}

.page-header {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 16px;
  margin-bottom: 16px;
}

.page-header h2 {
  margin: 0 0 4px;
  font-size: 20px;
  font-weight: 600;
}

.subtitle {
  margin: 0;
  max-width: 720px;
  font-size: 13px;
  color: #909399;
  line-height: 1.6;
}

.bind-card {
  margin-bottom: 16px;
}

.bind-row {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 12px;
}

.plate-input {
  width: 260px;
}

.plate {
  font-family: ui-monospace, SFMono-Regular, Menlo, Consolas, monospace;
  font-size: 15px;
  font-weight: 600;
  letter-spacing: 0.04em;
}

.hint {
  font-size: 12px;
  color: #909399;
}

.footnote {
  margin: 12px 0 0;
  font-size: 12px;
  color: #909399;
}
</style>
