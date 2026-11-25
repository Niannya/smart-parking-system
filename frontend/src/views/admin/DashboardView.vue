<script setup>
import { computed, onMounted, ref } from 'vue'
import { getDashboardRevenue, getDashboardSummary } from '../../api/dashboard'
import { listParkingLots } from '../../api/parkingLot'

const ALL_LOTS = 'all'
const BEIJING_OFFSET = '+08:00'
const shanghaiDate = new Intl.DateTimeFormat('en-CA', {
  timeZone: 'Asia/Shanghai',
  year: 'numeric',
  month: '2-digit',
  day: '2-digit',
})
const currency = new Intl.NumberFormat('zh-CN', { style: 'currency', currency: 'CNY' })
const percent = new Intl.NumberFormat('zh-CN', {
  minimumFractionDigits: 2,
  maximumFractionDigits: 2,
})

function businessToday() {
  const parts = Object.fromEntries(shanghaiDate.formatToParts(new Date()).map(({ type, value }) => [type, value]))
  return `${parts.year}-${parts.month}-${parts.day}`
}

function nextDate(date) {
  const calendarDate = new Date(`${date}T00:00:00Z`)
  calendarDate.setUTCDate(calendarDate.getUTCDate() + 1)
  return calendarDate.toISOString().slice(0, 10)
}

const today = businessToday()
const lots = ref([])
const selectedLot = ref(ALL_LOTS)
const lotsLoading = ref(false)
const lotsError = ref('')
const summaryDate = ref(today)
const summary = ref(null)
const summaryLoading = ref(false)
const summaryError = ref('')
const revenueFrom = ref(`${today}T00:00`)
const revenueTo = ref(`${nextDate(today)}T00:00`)
const revenue = ref(null)
const revenueLoading = ref(false)
const revenueError = ref('')
const revenueLabel = ref('')

let summaryRequest = 0
let revenueRequest = 0

const lotId = computed(() => selectedLot.value === ALL_LOTS ? undefined : selectedLot.value)
const metrics = computed(() => {
  const data = summary.value
  if (!data) return []
  return [
    ['总车位', data.totalSpaces],
    ['空闲车位', data.freeSpaces],
    ['占用车位', data.occupiedSpaces],
    ['停用车位', data.disabledSpaces],
    ['占用率', `${percent.format(data.occupancyRate)}%`],
    ['当日入场', data.entryCount],
    ['当日离场', data.exitCount],
    ['当日实收', currency.format(data.totalRevenue)],
  ]
})

async function loadLots() {
  if (lotsLoading.value) return
  lotsLoading.value = true
  lotsError.value = ''
  try {
    // 历史收入也可查询停用停车场，因此这里取全量列表而非启用场下拉接口。
    lots.value = await listParkingLots()
  } catch (error) {
    lotsError.value = error.message
  } finally {
    lotsLoading.value = false
  }
}

async function loadSummary() {
  const request = ++summaryRequest
  summary.value = null
  summaryError.value = ''
  if (!summaryDate.value) {
    summaryLoading.value = false
    summaryError.value = '请选择统计日期。'
    return
  }

  summaryLoading.value = true
  try {
    const data = await getDashboardSummary({ lotId: lotId.value, date: summaryDate.value })
    if (request === summaryRequest) summary.value = data
  } catch (error) {
    if (request === summaryRequest) summaryError.value = error.message
  } finally {
    if (request === summaryRequest) summaryLoading.value = false
  }
}

function dateTimeToUtc(value) {
  const timestamp = Date.parse(`${value}${BEIJING_OFFSET}`)
  return value && Number.isFinite(timestamp) ? new Date(timestamp).toISOString() : null
}

async function loadRevenue() {
  const request = ++revenueRequest
  revenue.value = null
  revenueLabel.value = ''
  revenueError.value = ''

  const localFrom = revenueFrom.value
  const localTo = revenueTo.value
  const from = dateTimeToUtc(localFrom)
  const to = dateTimeToUtc(localTo)
  if (!from || !to || Date.parse(from) >= Date.parse(to)) {
    revenueLoading.value = false
    revenueError.value = '请填写有效的起止时间，结束时间须晚于开始时间。'
    return
  }

  revenueLoading.value = true
  try {
    const data = await getDashboardRevenue({ lotId: lotId.value, from, to })
    if (request === revenueRequest) {
      revenue.value = data
      revenueLabel.value = `${localFrom.replace('T', ' ')} 至 ${localTo.replace('T', ' ')}`
    }
  } catch (error) {
    if (request === revenueRequest) revenueError.value = error.message
  } finally {
    if (request === revenueRequest) revenueLoading.value = false
  }
}

function refreshForLot() {
  void loadSummary()
  void loadRevenue()
}

onMounted(() => {
  void loadLots()
  refreshForLot()
})
</script>

<template>
  <section class="dashboard">
    <header class="dashboard-header">
      <h1>管理首页</h1>
      <div class="lot-filter">
        <label for="dashboard-lot">停车场</label>
        <el-select id="dashboard-lot" v-model="selectedLot" :loading="lotsLoading" @change="refreshForLot">
          <el-option :value="ALL_LOTS" label="全部停车场" />
          <el-option
            v-for="lot in lots"
            :key="lot.id"
            :value="lot.id"
            :label="`${lot.lotCode} · ${lot.lotName}${lot.status === 'DISABLED' ? '（停用）' : ''}`"
          />
        </el-select>
      </div>
    </header>
    <el-alert v-if="lotsError" type="warning" :closable="false" show-icon>
      <template #title>停车场列表加载失败：{{ lotsError }} <el-button link type="primary" @click="loadLots">重试</el-button></template>
    </el-alert>

    <el-card shadow="never">
      <template #header><h2>统计摘要</h2></template>
      <form class="query-form" @submit.prevent="loadSummary">
        <label for="dashboard-date">统计日期</label>
        <input id="dashboard-date" v-model="summaryDate" type="date" required />
        <el-button native-type="submit" :loading="summaryLoading">查询摘要</el-button>
      </form>
      <p class="hint">车位状态取查询时的快照，占用率不含停用车位。入离场和实收按上海时区的所选日期统计。</p>
      <el-alert v-if="summaryError" :title="summaryError" type="error" :closable="false" show-icon />
      <p v-else-if="summaryLoading" class="hint" role="status">正在查询摘要…</p>
      <template v-else-if="summary">
        <p class="result-label">统计日期：{{ summary.date }}</p>
        <dl class="metric-grid">
          <div v-for="[label, value] in metrics" :key="label" class="metric">
            <dt>{{ label }}</dt>
            <dd>{{ value }}</dd>
          </div>
        </dl>
      </template>
    </el-card>

    <el-card shadow="never">
      <template #header><h2>收入区间</h2></template>
      <form class="query-form" @submit.prevent="loadRevenue">
        <label for="revenue-from">开始时间</label>
        <input id="revenue-from" v-model="revenueFrom" type="datetime-local" required />
        <label for="revenue-to">结束时间</label>
        <input id="revenue-to" v-model="revenueTo" type="datetime-local" required />
        <el-button native-type="submit" :loading="revenueLoading">查询收入</el-button>
      </form>
      <p class="hint">按北京时间（UTC+08:00）填写。按支付时间统计，含开始、不含结束；零元放行计入支付笔数。</p>
      <el-alert v-if="revenueError" :title="revenueError" type="error" :closable="false" show-icon />
      <p v-else-if="revenueLoading" class="hint" role="status">正在查询收入…</p>
      <template v-else-if="revenue">
        <p class="result-label">查询区间：{{ revenueLabel }}</p>
        <dl class="metric-grid">
          <div class="metric"><dt>支付笔数</dt><dd>{{ revenue.paymentCount }}</dd></div>
          <div class="metric"><dt>实收金额</dt><dd>{{ currency.format(revenue.totalRevenue) }}</dd></div>
        </dl>
      </template>
    </el-card>
  </section>
</template>

<style scoped>
.dashboard {
  display: grid;
  gap: 16px;
}

.dashboard-header,
.lot-filter,
.query-form {
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  gap: 10px;
}

.dashboard-header {
  justify-content: space-between;
}

.dashboard-header h1,
h2 {
  margin: 0;
}

h2 {
  font-size: 1.1rem;
}

.lot-filter :deep(.el-select) {
  width: min(260px, 68vw);
}

.query-form input {
  max-width: 100%;
  padding: 5px 10px;
  font: inherit;
  color: inherit;
  border: 1px solid #c8d3df;
  border-radius: 4px;
  background: #fff;
}

.hint,
.result-label {
  margin: 10px 0;
  color: #526478;
  font-size: .875rem;
}

.metric-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(135px, 1fr));
  gap: 12px;
  margin: 0;
}

.metric {
  padding: 12px;
  background: #f3f6fa;
  border-radius: 6px;
}

.metric dt {
  color: #526478;
}

.metric dd {
  margin: 4px 0 0;
  font-size: 1.35rem;
  font-weight: 650;
  font-variant-numeric: tabular-nums;
  overflow-wrap: anywhere;
}
</style>
