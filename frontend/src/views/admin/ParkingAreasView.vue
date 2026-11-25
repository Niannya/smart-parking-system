<script setup>
import { computed, onMounted, reactive, ref, watch } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  changeAreaStatus,
  createArea,
  deleteArea,
  listAreas,
  listLotOptions,
  updateArea,
} from '../../api/area'

const lots = ref([])
const lotId = ref(null)
const statusFilter = ref('')
const areas = ref([])
const loading = ref(false)
const lotsLoading = ref(false)

const dialogVisible = ref(false)
const dialogMode = ref('create')
const submitting = ref(false)
const editingId = ref(null)
const formRef = ref(null)
const form = reactive({ areaCode: '', areaName: '', description: '' })

const rules = {
  areaCode: [
    { required: true, message: '请输入区域编号，例如 A', trigger: 'blur' },
    { max: 20, message: '区域编号不能超过 20 个字符', trigger: 'blur' },
  ],
  areaName: [
    { required: true, message: '请输入区域名称，例如 A 区', trigger: 'blur' },
    { max: 50, message: '区域名称不能超过 50 个字符', trigger: 'blur' },
  ],
  description: [{ max: 255, message: '说明不能超过 255 个字符', trigger: 'blur' }],
}

const activeCount = computed(() => areas.value.filter((item) => item.status === 'ACTIVE').length)

function statusTagType(status) {
  if (status === 'ACTIVE') return 'success'
  if (status === 'DISABLED') return 'info'
  return 'warning'
}

function statusLabel(status) {
  if (status === 'ACTIVE') return '启用'
  if (status === 'DISABLED') return '停用'
  return status
}

function formatTime(value) {
  return value ? new Date(value).toLocaleString('zh-CN', { hour12: false }) : ''
}

async function loadLots() {
  lotsLoading.value = true
  try {
    lots.value = await listLotOptions()
    if (lots.value.length > 0 && lotId.value == null) {
      lotId.value = lots.value[0].id
    }
  } catch (e) {
    ElMessage.error(e.message)
  } finally {
    lotsLoading.value = false
  }
}

async function loadAreas() {
  if (lotId.value == null) {
    areas.value = []
    return
  }
  loading.value = true
  try {
    areas.value = await listAreas({
      lotId: lotId.value,
      status: statusFilter.value || undefined,
    })
  } catch (e) {
    ElMessage.error(e.message)
  } finally {
    loading.value = false
  }
}

watch(lotId, loadAreas)
watch(statusFilter, loadAreas)

function openCreate() {
  if (lotId.value == null) {
    ElMessage.warning('请先选择停车场')
    return
  }
  dialogMode.value = 'create'
  editingId.value = null
  form.areaCode = ''
  form.areaName = ''
  form.description = ''
  dialogVisible.value = true
}

function openEdit(row) {
  dialogMode.value = 'edit'
  editingId.value = row.id
  form.areaCode = row.areaCode
  form.areaName = row.areaName
  form.description = row.description || ''
  dialogVisible.value = true
}

async function submit() {
  if (!formRef.value) return
  const valid = await formRef.value.validate().catch(() => false)
  if (!valid) return

  submitting.value = true
  try {
    if (dialogMode.value === 'create') {
      await createArea({
        lotId: lotId.value,
        areaCode: form.areaCode,
        areaName: form.areaName,
        description: form.description || null,
      })
      ElMessage.success('区域新增成功')
    } else {
      await updateArea(editingId.value, {
        areaName: form.areaName,
        description: form.description || null,
      })
      ElMessage.success('区域修改成功')
    }
    dialogVisible.value = false
    await loadAreas()
  } catch (e) {
    ElMessage.error(e.message)
  } finally {
    submitting.value = false
  }
}

async function toggleStatus(row) {
  const target = row.status === 'ACTIVE' ? 'DISABLED' : 'ACTIVE'
  const actionText = target === 'DISABLED' ? '停用' : '启用'
  try {
    await ElMessageBox.confirm(
      `确认${actionText}区域「${row.areaCode}」吗？${target === 'DISABLED' ? '停用后该区域的车位不再参与自动分配，且存在在场车辆时不允许停用。' : ''}`,
      `${actionText}区域`,
      { type: 'warning', confirmButtonText: '确认', cancelButtonText: '取消' },
    )
  } catch {
    return
  }
  try {
    await changeAreaStatus(row.id, target)
    ElMessage.success(`${actionText}成功`)
    await loadAreas()
  } catch (e) {
    ElMessage.error(e.message)
  }
}

async function remove(row) {
  try {
    await ElMessageBox.confirm(
      `确认删除区域「${row.areaCode}」吗？区域下存在车位时不允许删除。`,
      '删除区域',
      { type: 'warning', confirmButtonText: '删除', cancelButtonText: '取消' },
    )
  } catch {
    return
  }
  try {
    await deleteArea(row.id)
    ElMessage.success('删除成功')
    await loadAreas()
  } catch (e) {
    ElMessage.error(e.message)
  }
}

onMounted(loadLots)
</script>

<template>
  <div class="page">
    <header class="page-header">
      <div>
        <h2>停车区域</h2>
        <p class="subtitle">区域编号在同一停车场内唯一；停用区域的车位不参与自动分配。</p>
      </div>
      <div class="actions">
        <el-button :disabled="lotId == null" type="primary" @click="openCreate">新增区域</el-button>
        <el-button @click="loadAreas">刷新</el-button>
      </div>
    </header>

    <div class="toolbar">
      <el-select
        v-model="lotId"
        :loading="lotsLoading"
        placeholder="选择停车场"
        style="width: 220px"
      >
        <el-option
          v-for="lot in lots"
          :key="lot.id"
          :label="`${lot.lotName}（${lot.lotCode}）`"
          :value="lot.id"
        />
      </el-select>

      <el-select v-model="statusFilter" placeholder="全部状态" clearable style="width: 140px">
        <el-option label="启用" value="ACTIVE" />
        <el-option label="停用" value="DISABLED" />
      </el-select>

      <span class="stat">当前启用区域 {{ activeCount }} 个</span>
    </div>

    <el-alert
      v-if="!lotsLoading && lots.length === 0"
      title="没有可用的停车场。请先由管理员创建停车场，或确认已登录管理员账号。"
      type="warning"
      show-icon
      :closable="false"
    />

    <el-table v-loading="loading" :data="areas" border stripe>
      <el-table-column prop="areaCode" label="区域编号" width="110" />
      <el-table-column prop="areaName" label="区域名称" min-width="140" />
      <el-table-column prop="spaceCount" label="车位数" width="100" align="center" />
      <el-table-column label="状态" width="100" align="center">
        <template #default="{ row }">
          <el-tag :type="statusTagType(row.status)">{{ statusLabel(row.status) }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="description" label="说明" min-width="180" show-overflow-tooltip />
      <el-table-column label="创建时间" width="180">
        <template #default="{ row }">{{ formatTime(row.createdAt) }}</template>
      </el-table-column>
      <el-table-column label="操作" width="210" align="center">
        <template #default="{ row }">
          <el-button link type="primary" @click="openEdit(row)">编辑</el-button>
          <el-button link type="warning" @click="toggleStatus(row)">
            {{ row.status === 'ACTIVE' ? '停用' : '启用' }}
          </el-button>
          <el-button link type="danger" @click="remove(row)">删除</el-button>
        </template>
      </el-table-column>
      <template #empty>
        <span>{{ lotId == null ? '请先选择停车场' : '该停车场下还没有区域' }}</span>
      </template>
    </el-table>

    <el-dialog
      v-model="dialogVisible"
      :title="dialogMode === 'create' ? '新增停车区域' : '编辑停车区域'"
      width="480px"
    >
      <el-form ref="formRef" :model="form" :rules="rules" label-width="92px">
        <el-form-item label="区域编号" prop="areaCode">
          <el-input
            v-model="form.areaCode"
            :disabled="dialogMode === 'edit'"
            placeholder="例如 A、B、C"
            maxlength="20"
          />
          <div v-if="dialogMode === 'edit'" class="hint">区域编号创建后不可修改</div>
        </el-form-item>
        <el-form-item label="区域名称" prop="areaName">
          <el-input v-model="form.areaName" placeholder="例如 A 区" maxlength="50" />
        </el-form-item>
        <el-form-item label="说明" prop="description">
          <el-input
            v-model="form.description"
            type="textarea"
            :rows="2"
            placeholder="例如：靠近入口"
            maxlength="255"
            show-word-limit
          />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="submitting" @click="submit">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<style scoped>
.page {
  width: min(100% - 40px, 1240px);
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
  font-size: 13px;
  color: #909399;
}

.actions {
  display: flex;
  gap: 8px;
}

.toolbar {
  display: flex;
  flex-wrap: wrap;
  gap: 12px;
  align-items: center;
  margin-bottom: 16px;
}

.stat {
  font-size: 13px;
  color: #606266;
}

.hint {
  font-size: 12px;
  color: #909399;
  line-height: 1.4;
}
</style>
