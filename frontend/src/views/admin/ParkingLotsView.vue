<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  createParkingLot,
  deleteParkingLot,
  listParkingLots,
  setParkingLotStatus,
  updateParkingLot,
} from '../../api/parkingLot'

const lots = ref([])
const statusFilter = ref('')
const listError = ref('')
const loading = ref(false)
const acting = ref(false)

const editorOpen = ref(false)
const editingId = ref(null)
const editing = computed(() => editingId.value !== null)
const saving = ref(false)
const formError = ref('')
const form = reactive({ lotCode: '', lotName: '', address: '', description: '' })
const busy = computed(() => loading.value || acting.value || saving.value)

async function loadLots() {
  if (loading.value) return
  listError.value = ''
  loading.value = true
  try {
    lots.value = await listParkingLots(statusFilter.value)
  } catch (error) {
    lots.value = []
    listError.value = error.message
  } finally {
    loading.value = false
  }
}

function openEditor(lot = null) {
  if (busy.value) return
  editingId.value = lot?.id ?? null
  formError.value = ''
  Object.assign(form, {
    lotCode: lot?.lotCode ?? '',
    lotName: lot?.lotName ?? '',
    address: lot?.address ?? '',
    description: lot?.description ?? '',
  })
  editorOpen.value = true
}

async function saveLot() {
  if (saving.value) return
  formError.value = ''
  const lotCode = form.lotCode.trim()
  const payload = {
    lotName: form.lotName.trim(),
    address: form.address.trim(),
    description: form.description.trim() || null,
  }
  if ((!editing.value && !lotCode) || !payload.lotName || !payload.address) {
    formError.value = '请填写停车场编号、名称和地址。'
    return
  }

  saving.value = true
  try {
    if (editing.value) {
      await updateParkingLot(editingId.value, payload)
      ElMessage.success('停车场已更新')
    } else {
      await createParkingLot({ ...payload, lotCode })
      ElMessage.success('停车场已新增')
    }
    editorOpen.value = false
    await loadLots()
  } catch (error) {
    formError.value = error.message
  } finally {
    saving.value = false
  }
}

async function confirmChange(title, message, request) {
  if (busy.value) return
  acting.value = true
  try {
    await ElMessageBox.confirm(message, title, {
      type: 'warning',
      confirmButtonText: '确认',
      cancelButtonText: '取消',
    })
    await request()
    ElMessage.success(`${title}成功`)
    await loadLots()
  } catch (error) {
    if (error !== 'cancel' && error !== 'close') {
      ElMessage.error(error.message || '操作失败')
    }
  } finally {
    acting.value = false
  }
}

function toggleStatus(lot) {
  const disabling = lot.status === 'ACTIVE'
  const status = disabling ? 'DISABLED' : 'ACTIVE'
  const action = disabling ? '停用' : '启用'
  const note = disabling ? '有车辆仍在场时，后端会拒绝停用。' : ''
  return confirmChange(`${action}停车场`, `确认${action}「${lot.lotName}」？${note}`,
    () => setParkingLotStatus(lot.id, status))
}

function removeLot(lot) {
  return confirmChange('删除停车场', `确认删除「${lot.lotName}」？有关联区域、收费规则或停车记录时会拒绝删除。`,
    () => deleteParkingLot(lot.id))
}

onMounted(loadLots)
</script>

<template>
  <section>
    <div class="heading">
      <h1>停车场管理</h1>
      <el-button type="primary" :disabled="busy" @click="openEditor()">新增停车场</el-button>
    </div>

    <el-form class="filters" inline @submit.prevent="loadLots">
      <el-form-item label="停车场状态">
        <el-select v-model="statusFilter" clearable placeholder="全部状态" :disabled="busy">
          <el-option label="启用" value="ACTIVE" />
          <el-option label="停用" value="DISABLED" />
        </el-select>
      </el-form-item>
      <el-form-item>
        <el-button native-type="submit" :loading="loading" :disabled="saving || acting">查询</el-button>
      </el-form-item>
    </el-form>

    <el-alert v-if="listError" :title="listError" type="error" :closable="false" show-icon />
    <el-table v-loading="loading" :data="lots" row-key="id" :empty-text="listError ? '列表加载失败' : '没有符合条件的停车场'">
      <el-table-column prop="lotCode" label="编号" min-width="116" />
      <el-table-column prop="lotName" label="名称" min-width="130" />
      <el-table-column prop="address" label="地址" min-width="180" show-overflow-tooltip />
      <el-table-column prop="description" label="说明" min-width="150" show-overflow-tooltip />
      <el-table-column label="状态" width="90">
        <template #default="{ row }">
          <el-tag :type="row.status === 'ACTIVE' ? 'success' : 'info'">
            {{ row.status === 'ACTIVE' ? '启用' : '停用' }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column label="操作" width="194">
        <template #default="{ row }">
          <el-button link type="primary" :disabled="busy" @click="openEditor(row)">编辑</el-button>
          <el-button link type="primary" :disabled="busy" @click="toggleStatus(row)">
            {{ row.status === 'ACTIVE' ? '停用' : '启用' }}
          </el-button>
          <el-button link type="danger" :disabled="busy" @click="removeLot(row)">删除</el-button>
        </template>
      </el-table-column>
    </el-table>

    <el-dialog v-model="editorOpen" :title="editing ? '编辑停车场' : '新增停车场'" width="min(92vw, 520px)"
               :close-on-click-modal="!saving" :close-on-press-escape="!saving" :show-close="!saving">
      <el-alert v-if="formError" class="editor-error" :title="formError" type="error" :closable="false" show-icon />
      <el-form label-position="top" :disabled="saving" @submit.prevent="saveLot">
        <el-form-item label="编号" required>
          <el-input v-model="form.lotCode" :disabled="editing" :maxlength="30" />
        </el-form-item>
        <el-form-item label="名称" required>
          <el-input v-model="form.lotName" :maxlength="100" />
        </el-form-item>
        <el-form-item label="地址" required>
          <el-input v-model="form.address" :maxlength="255" />
        </el-form-item>
        <el-form-item label="说明">
          <el-input v-model="form.description" type="textarea" :rows="3" :maxlength="500" show-word-limit />
        </el-form-item>
        <div class="editor-actions">
          <el-button :disabled="saving" @click="editorOpen = false">取消</el-button>
          <el-button type="primary" native-type="submit" :loading="saving">保存</el-button>
        </div>
      </el-form>
    </el-dialog>
  </section>
</template>

<style scoped>
.heading {
  display: flex;
  align-items: baseline;
  justify-content: space-between;
  flex-wrap: wrap;
  gap: 12px;
}

.filters .el-select {
  width: 150px;
}

.editor-error {
  margin-bottom: 12px;
}

.editor-actions {
  display: flex;
  justify-content: flex-end;
}
</style>
