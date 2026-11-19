<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { listAreas, listLotOptions } from '../../api/area'
import {
  changeSpaceStatus,
  createSpace,
  deleteSpace,
  listSpaces,
  updateSpace,
} from '../../api/space'

const lots = ref([])
const lotId = ref(null)
const areas = ref([])
const areaId = ref('')
const statusFilter = ref('')
const spaces = ref([])
const loading = ref(false)
const lotsLoading = ref(false)

const dialogVisible = ref(false)
const dialogMode = ref('create')
const submitting = ref(false)
const editingId = ref(null)
const formRef = ref(null)
const form = reactive({ areaId: null, spaceCode: '', description: '' })

const rules = {
  areaId: [{ required: true, message: '请选择所属区域', trigger: 'change' }],
  spaceCode: [
    { required: true, message: '请输入车位编号，例如 011', trigger: 'blur' },
    { max: 20, message: '车位编号不能超过 20 个字符', trigger: 'blur' },
  ],
  description: [{ max: 255, message: '说明不能超过 255 个字符', trigger: 'blur' }],
}

const stats = computed(() => ({
  total: spaces.value.length,
  free: spaces.value.filter((item) => item.status === 'FREE').length,
  occupied: spaces.value.filter((item) => item.status === 'OCCUPIED').length,
  disabled: spaces.value.filter((item) => item.status === 'DISABLED').length,
}))

function statusTagType(status) {
  if (status === 'FREE') return 'success'
  if (status === 'OCCUPIED') return 'warning'
  if (status === 'DISABLED') return 'info'
  return 'danger'
}

function statusLabel(status) {
  if (status === 'FREE') return '空闲'
  if (status === 'OCCUPIED') return '占用'
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
      await loadAreas()
      await loadSpaces()
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
  try {
    areas.value = await listAreas({ lotId: lotId.value })
  } catch (e) {
    ElMessage.error(e.message)
  }
}

async function loadSpaces() {
  if (lotId.value == null) {
    spaces.value = []
    return
  }
  loading.value = true
  try {
    spaces.value = await listSpaces({
      lotId: lotId.value,
      areaId: areaId.value || undefined,
      status: statusFilter.value || undefined,
    })
  } catch (e) {
    ElMessage.error(e.message)
  } finally {
    loading.value = false
  }
}

async function handleLotChange() {
  areaId.value = ''
  await loadAreas()
  await loadSpaces()
}

function openCreate() {
  if (lotId.value == null) {
    ElMessage.warning('请先选择停车场')
    return
  }
  if (areas.value.length === 0) {
    ElMessage.warning('该停车场还没有区域，请先创建区域')
    return
  }
  dialogMode.value = 'create'
  editingId.value = null
  form.areaId = areaId.value || null
  form.spaceCode = ''
  form.description = ''
  dialogVisible.value = true
}

function openEdit(row) {
  dialogMode.value = 'edit'
  editingId.value = row.id
  form.areaId = row.areaId
  form.spaceCode = row.spaceCode
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
      await createSpace({
        areaId: form.areaId,
        spaceCode: form.spaceCode,
        description: form.description || null,
      })
      ElMessage.success('车位新增成功')
    } else {
      await updateSpace(editingId.value, {
        spaceCode: form.spaceCode,
        description: form.description || null,
      })
      ElMessage.success('车位修改成功')
    }
    dialogVisible.value = false
    await loadSpaces()
  } catch (e) {
    ElMessage.error(e.message)
  } finally {
    submitting.value = false
  }
}

async function toggleStatus(row) {
  const target = row.status === 'FREE' ? 'DISABLED' : 'FREE'
  const actionText = target === 'DISABLED' ? '停用' : '启用'
  try {
    await ElMessageBox.confirm(
      `确认${actionText}车位「${row.areaCode}-${row.spaceCode}」吗？`,
      `${actionText}车位`,
      { type: 'warning', confirmButtonText: '确认', cancelButtonText: '取消' },
    )
  } catch {
    return
  }
  try {
    await changeSpaceStatus(row.id, target)
    ElMessage.success(`${actionText}成功`)
    await loadSpaces()
  } catch (e) {
    ElMessage.error(e.message)
  }
}

async function remove(row) {
  try {
    await ElMessageBox.confirm(
      `确认删除车位「${row.areaCode}-${row.spaceCode}」吗？只有空闲且没有停车历史的车位可以删除。`,
      '删除车位',
      { type: 'warning', confirmButtonText: '删除', cancelButtonText: '取消' },
    )
  } catch {
    return
  }
  try {
    await deleteSpace(row.id)
    ElMessage.success('删除成功')
    await loadSpaces()
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
        <h2>停车位</h2>
        <p class="subtitle">
          车位只允许在启用停车场、启用区域中新增；占用中的车位不能编辑、停用或删除。
        </p>
      </div>
      <div class="actions">
        <el-button type="primary" :disabled="lotId == null" @click="openCreate">新增车位</el-button>
        <el-button @click="loadSpaces">刷新</el-button>
      </div>
    </header>

    <div class="toolbar">
      <el-select
        v-model="lotId"
        :loading="lotsLoading"
        placeholder="选择停车场"
        style="width: 220px"
        @change="handleLotChange"
      >
        <el-option
          v-for="lot in lots"
          :key="lot.id"
          :label="`${lot.lotName}（${lot.lotCode}）`"
          :value="lot.id"
        />
      </el-select>

      <el-select v-model="areaId" placeholder="全部区域" clearable style="width: 160px" @change="loadSpaces">
        <el-option
          v-for="area in areas"
          :key="area.id"
          :label="`${area.areaCode} 区`"
          :value="area.id"
        />
      </el-select>

      <el-select v-model="statusFilter" placeholder="全部状态" clearable style="width: 140px" @change="loadSpaces">
        <el-option label="空闲" value="FREE" />
        <el-option label="占用" value="OCCUPIED" />
        <el-option label="停用" value="DISABLED" />
      </el-select>

      <div class="stat">
        共 {{ stats.total }} · 空闲 {{ stats.free }} · 占用 {{ stats.occupied }} · 停用 {{ stats.disabled }}
      </div>
    </div>

    <el-alert
      v-if="!lotsLoading && lots.length === 0"
      title="没有可用的停车场。请先由管理员创建停车场，或确认已登录管理员账号。"
      type="warning"
      show-icon
      :closable="false"
    />

    <el-table v-loading="loading" :data="spaces" border stripe>
      <el-table-column label="车位编号" width="150">
        <template #default="{ row }">
          <span class="space-code">{{ row.areaCode }}-{{ row.spaceCode }}</span>
        </template>
      </el-table-column>
      <el-table-column prop="areaCode" label="区域" width="90" align="center" />
      <el-table-column label="状态" width="100" align="center">
        <template #default="{ row }">
          <el-tag :type="statusTagType(row.status)">{{ statusLabel(row.status) }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="description" label="说明" min-width="180" show-overflow-tooltip />
      <el-table-column label="创建时间" width="180">
        <template #default="{ row }">{{ formatTime(row.createdAt) }}</template>
      </el-table-column>
      <el-table-column label="操作" width="230" align="center">
        <template #default="{ row }">
          <el-button link type="primary" :disabled="row.status !== 'FREE'" @click="openEdit(row)">
            编辑
          </el-button>
          <el-button
            v-if="row.status === 'OCCUPIED'"
            link
            disabled
          >
            停用
          </el-button>
          <el-button v-else link type="warning" @click="toggleStatus(row)">
            {{ row.status === 'FREE' ? '停用' : '启用' }}
          </el-button>
          <el-button link type="danger" :disabled="row.status !== 'FREE'" @click="remove(row)">
            删除
          </el-button>
        </template>
      </el-table-column>
      <template #empty>
        <span>{{ lotId == null ? '请先选择停车场' : '没有符合条件的车位' }}</span>
      </template>
    </el-table>

    <el-dialog
      v-model="dialogVisible"
      :title="dialogMode === 'create' ? '新增停车位' : '编辑停车位'"
      width="480px"
    >
      <el-form ref="formRef" :model="form" :rules="rules" label-width="92px">
        <el-form-item label="所属区域" prop="areaId">
          <el-select v-model="form.areaId" :disabled="dialogMode === 'edit'" placeholder="选择区域">
            <el-option
              v-for="area in areas"
              :key="area.id"
              :label="`${area.areaCode} 区（${area.areaName}）`"
              :value="area.id"
            />
          </el-select>
          <div v-if="dialogMode === 'edit'" class="hint">车位所属区域创建后不可修改</div>
        </el-form-item>
        <el-form-item label="车位编号" prop="spaceCode">
          <el-input v-model="form.spaceCode" placeholder="例如 011" maxlength="20" />
        </el-form-item>
        <el-form-item label="说明" prop="description">
          <el-input
            v-model="form.description"
            type="textarea"
            :rows="2"
            placeholder="例如：靠入口"
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

.space-code {
  font-family: ui-monospace, SFMono-Regular, Menlo, Consolas, monospace;
  font-weight: 600;
  letter-spacing: 0.02em;
}

.hint {
  font-size: 12px;
  color: #909399;
  line-height: 1.4;
}
</style>
