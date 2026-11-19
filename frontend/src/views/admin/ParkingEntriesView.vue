<script setup>
import { onMounted, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { createEntry, listParkingLotOptions } from '../../api/entry'

const lots = ref([])
const lotId = ref(null)
const plate = ref('')
const loading = ref(false)
const submitting = ref(false)
const result = ref(null)

function normalizePlate(value) {
  return (value || '').replace(/\s+/g, '').toUpperCase()
}

function formatTime(value) {
  return value ? new Date(value).toLocaleString('zh-CN', { hour12: false }) : ''
}

async function loadLots() {
  loading.value = true
  try {
    const data = await listParkingLotOptions()
    lots.value = Array.isArray(data) ? data : []
    if (lots.value.length && lotId.value == null) {
      lotId.value = lots.value[0].id
    }
  } catch (e) {
    ElMessage.error(`停车场选项加载失败：${e.message}`)
  } finally {
    loading.value = false
  }
}

async function submit() {
  if (!lotId.value) {
    ElMessage.warning('请选择停车场')
    return
  }
  const normalized = normalizePlate(plate.value)
  if (!normalized) {
    ElMessage.warning('请输入车牌')
    return
  }

  submitting.value = true
  result.value = null
  try {
    result.value = await createEntry({ lotId: lotId.value, licensePlate: normalized })
    ElMessage.success('入场成功')
  } catch (e) {
    ElMessage.error(e.message)
  } finally {
    submitting.value = false
  }
}

onMounted(loadLots)
</script>

<template>
  <div class="page">
    <header class="page-header">
      <div>
        <h2>车辆入场</h2>
        <p class="subtitle">
          选择停车场并输入车牌，系统按区域码、车位码顺序自动分配空闲车位；同牌在场、无空位、无生效规则会返回服务端错误。
        </p>
      </div>
      <el-button @click="loadLots">刷新停车场</el-button>
    </header>

    <el-card shadow="never" class="entry-card">
      <el-form label-width="88px" @submit.prevent>
        <el-form-item label="停车场">
          <el-select
            v-model="lotId"
            :loading="loading"
            placeholder="请选择启用的停车场"
            class="lot-select"
          >
            <el-option
              v-for="lot in lots"
              :key="lot.id"
              :value="lot.id"
              :label="`${lot.lotCode} ${lot.lotName}`"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="车牌号码">
          <el-input
            v-model="plate"
            placeholder="例如 京A12345"
            maxlength="8"
            clearable
            class="plate-input"
            @keyup.enter="submit"
          />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" :loading="submitting" @click="submit">办理入场</el-button>
          <span class="hint">未绑定或已停用的车辆按临时车入场，记录保留车牌快照。</span>
        </el-form-item>
      </el-form>
    </el-card>

    <el-card v-if="result" shadow="never" class="result-card">
      <template #header>
        <div class="result-header">
          <span>入场成功</span>
          <el-tag type="success">{{ result.status }}</el-tag>
        </div>
      </template>
      <el-descriptions :column="2" border>
        <el-descriptions-item label="记录编号">{{ result.recordNo }}</el-descriptions-item>
        <el-descriptions-item label="车牌号码">{{ result.licensePlate }}</el-descriptions-item>
        <el-descriptions-item label="停车场 ID">{{ result.lotId }}</el-descriptions-item>
        <el-descriptions-item label="区域 / 车位">{{ result.areaCode }} / {{ result.spaceCode }}</el-descriptions-item>
        <el-descriptions-item label="车位 ID">{{ result.spaceId }}</el-descriptions-item>
        <el-descriptions-item label="规则 ID">{{ result.feeRuleId }}</el-descriptions-item>
        <el-descriptions-item label="入场时间">{{ formatTime(result.entryTime) }}</el-descriptions-item>
        <el-descriptions-item label="记录 ID">{{ result.id }}</el-descriptions-item>
      </el-descriptions>
      <p class="footnote">凭记录编号或记录 ID 可在离场页办理计费与离场。</p>
    </el-card>

    <el-empty v-else-if="!loading && lots.length === 0" description="暂无启用的停车场，无法办理入场" />
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

.entry-card {
  margin-bottom: 16px;
}

.lot-select,
.plate-input {
  width: 280px;
}

.hint {
  margin-left: 12px;
  font-size: 12px;
  color: #909399;
}

.result-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.footnote {
  margin: 12px 0 0;
  font-size: 12px;
  color: #909399;
}
</style>
