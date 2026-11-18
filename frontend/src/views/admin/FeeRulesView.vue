<template>
  <div>
    <div class="panel">
      <h2>收费规则</h2>
      <div class="row">
        <div class="field">
          <label>停车场</label>
          <select v-model="lotId" @change="loadAll">
            <option v-for="lot in lots" :key="lot.id" :value="lot.id">
              {{ lot.lotName }}（{{ lot.lotCode }}）
            </option>
          </select>
        </div>
        <button class="ghost" @click="loadAll" :disabled="!lotId">刷新</button>
      </div>
      <p class="muted" style="margin-bottom:0">
        历史规则不可改删，只能新建；新规则创建即激活，旧规则自动转历史并按原绑定继续计费。
      </p>
    </div>

    <div v-if="error" class="error">{{ error }}</div>
    <div v-if="success" class="success">{{ success }}</div>

    <div class="panel" v-if="activeRule">
      <h3>当前生效规则</h3>
      <table>
        <thead>
          <tr><th>规则名称</th><th>免费时长</th><th>计费单位</th><th>单价</th><th>24小时封顶</th><th>生效时间</th></tr>
        </thead>
        <tbody>
          <tr>
            <td>{{ activeRule.ruleName }}</td>
            <td>{{ activeRule.freeMinutes }} 分钟</td>
            <td>{{ activeRule.billingUnitMinutes }} 分钟</td>
            <td>¥{{ activeRule.unitPrice }}</td>
            <td>¥{{ activeRule.maxFeePer24h }}</td>
            <td>{{ formatTime(activeRule.effectiveFrom) }}</td>
          </tr>
        </tbody>
      </table>
    </div>

    <div class="panel">
      <h3>新建规则（创建即激活）</h3>
      <div class="row">
        <div class="field">
          <label>规则名称</label>
          <input v-model="form.ruleName" placeholder="如：节假日规则" />
        </div>
        <div class="field">
          <label>免费时长（分钟）</label>
          <input v-model.number="form.freeMinutes" type="number" min="0" />
        </div>
        <div class="field">
          <label>计费单位（分钟）</label>
          <input v-model.number="form.billingUnitMinutes" type="number" min="1" />
        </div>
        <div class="field">
          <label>单价（元/单位）</label>
          <input v-model="form.unitPrice" placeholder="5.00" />
        </div>
        <div class="field">
          <label>24小时封顶（元）</label>
          <input v-model="form.maxFeePer24h" placeholder="50.00" />
        </div>
        <button class="primary" @click="submit" :disabled="submitting">{{ submitting ? '提交中…' : '创建并激活' }}</button>
      </div>
      <div class="form-error">{{ formError }}</div>
    </div>

    <div class="panel">
      <h3>规则历史</h3>
      <table>
        <thead>
          <tr><th>ID</th><th>规则名称</th><th>免费时长</th><th>计费单位</th><th>单价</th><th>24小时封顶</th><th>状态</th><th>创建时间</th></tr>
        </thead>
        <tbody>
          <tr v-for="rule in rules" :key="rule.id">
            <td class="mono">{{ rule.id }}</td>
            <td>{{ rule.ruleName }}</td>
            <td>{{ rule.freeMinutes }} 分钟</td>
            <td>{{ rule.billingUnitMinutes }} 分钟</td>
            <td>¥{{ rule.unitPrice }}</td>
            <td>¥{{ rule.maxFeePer24h }}</td>
            <td><span :class="['tag', rule.isActive ? 'active' : 'inactive']">{{ rule.isActive ? '生效中' : '历史' }}</span></td>
            <td>{{ formatTime(rule.createdAt) }}</td>
          </tr>
          <tr v-if="rules.length === 0">
            <td colspan="8" class="muted">暂无规则</td>
          </tr>
        </tbody>
      </table>
    </div>
  </div>
</template>

<script setup>
import { onMounted, reactive, ref } from 'vue'
import { listLotOptions } from '../../api/lot'
import { listFeeRules, getActiveFeeRule, createFeeRule } from '../../api/feeRule'

const lots = ref([])
const lotId = ref('')
const rules = ref([])
const activeRule = ref(null)
const error = ref('')
const success = ref('')
const formError = ref('')
const submitting = ref(false)

const form = reactive({
  ruleName: '',
  freeMinutes: 30,
  billingUnitMinutes: 60,
  unitPrice: '5.00',
  maxFeePer24h: '50.00'
})

function formatTime(value) {
  return value ? new Date(value).toLocaleString('zh-CN') : '—'
}

async function loadLots() {
  try {
    const res = await listLotOptions()
    lots.value = res.data || []
    if (lots.value.length > 0) {
      lotId.value = lots.value[0].id
    }
  } catch (e) {
    error.value = e.message || '场地加载失败'
  }
}

async function loadAll() {
  error.value = ''
  success.value = ''
  if (!lotId.value) {
    return
  }
  try {
    const [historyRes, activeRes] = await Promise.all([
      listFeeRules(lotId.value),
      getActiveFeeRule(lotId.value).catch(() => null)
    ])
    rules.value = historyRes.data || []
    activeRule.value = activeRes && activeRes.data ? activeRes.data : null
  } catch (e) {
    error.value = e.message || '规则加载失败'
  }
}

async function submit() {
  formError.value = ''
  error.value = ''
  success.value = ''
  if (!form.ruleName || !form.ruleName.trim()) {
    formError.value = '规则名称不能为空'
    return
  }
  if (!(form.freeMinutes >= 0)) {
    formError.value = '免费时长必须大于等于 0'
    return
  }
  if (!(form.billingUnitMinutes > 0)) {
    formError.value = '计费单位必须大于 0'
    return
  }
  if (!/^\d+(\.\d{1,2})?$/.test(String(form.unitPrice)) || !/^\d+(\.\d{1,2})?$/.test(String(form.maxFeePer24h))) {
    formError.value = '单价与封顶金额必须为非负数字（最多两位小数）'
    return
  }
  submitting.value = true
  try {
    await createFeeRule({ ...form, lotId: lotId.value })
    success.value = '新规则已创建并激活'
    form.ruleName = ''
    await loadAll()
  } catch (e) {
    formError.value = e.message || '创建失败'
  } finally {
    submitting.value = false
  }
}

onMounted(async () => {
  await loadLots()
  await loadAll()
})
</script>
