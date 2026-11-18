<template>
  <div>
    <div class="panel">
      <h2>我的停车记录</h2>
      <div class="row">
        <div class="field">
          <label>状态</label>
          <select v-model="status" @change="search">
            <option value="">全部</option>
            <option value="PARKING">在场</option>
            <option value="COMPLETED">已完成</option>
            <option value="ABNORMAL">异常</option>
          </select>
        </div>
        <button class="ghost" @click="search" :disabled="loading">{{ loading ? '查询中…' : '刷新' }}</button>
      </div>
      <p class="muted" style="margin-bottom:0">仅显示与您绑定车辆关联的记录；车辆停用后历史记录仍可查看。</p>
    </div>

    <div v-if="error" class="error">{{ error }}</div>

    <div class="panel">
      <table>
        <thead>
          <tr>
            <th>ID</th><th>记录编号</th><th>车牌号</th><th>停车场</th><th>车位</th>
            <th>入场时间</th><th>离场时间</th><th>时长(分)</th><th>状态</th><th>支付</th>
            <th>预估/应收</th><th></th>
          </tr>
        </thead>
        <tbody>
          <tr v-for="row in records" :key="row.id">
            <td class="mono">{{ row.id }}</td>
            <td class="mono">{{ row.recordNo }}</td>
            <td>{{ row.licensePlate }}</td>
            <td>{{ row.lotName }}</td>
            <td>{{ row.areaCode }}/{{ row.spaceCode }}</td>
            <td>{{ formatTime(row.entryTime) }}</td>
            <td>{{ formatTime(row.exitTime) }}</td>
            <td>{{ row.durationMinutes }}</td>
            <td><span :class="['tag', statusClass(row.status)]">{{ statusLabel(row.status) }}</span></td>
            <td>{{ paymentLabel(row) }}</td>
            <td class="amount">¥{{ row.estimatedAmountDue !== null && row.estimatedAmountDue !== undefined ? row.estimatedAmountDue : (row.payment ? row.payment.amountDue : '—') }}</td>
            <td><button class="ghost" @click="showDetail(row.id)">详情</button></td>
          </tr>
          <tr v-if="records.length === 0">
            <td colspan="12" class="muted">暂无记录</td>
          </tr>
        </tbody>
      </table>
    </div>

    <div class="panel" v-if="detail">
      <h3>记录详情</h3>
      <table>
        <tbody>
          <tr><th style="width:130px">记录编号</th><td class="mono">{{ detail.recordNo }}</td></tr>
          <tr><th>车牌号</th><td>{{ detail.licensePlate }}</td></tr>
          <tr><th>停车场</th><td>{{ detail.lotName }}</td></tr>
          <tr><th>车位</th><td>{{ detail.areaCode }} 区 / {{ detail.spaceCode }}</td></tr>
          <tr><th>入场时间</th><td>{{ formatTime(detail.entryTime) }}</td></tr>
          <tr><th>离场时间</th><td>{{ formatTime(detail.exitTime) }}</td></tr>
          <tr><th>停车时长</th><td>{{ detail.durationMinutes }} 分钟</td></tr>
          <tr><th>状态</th><td>{{ statusLabel(detail.status) }}</td></tr>
          <tr><th>预估/应收金额</th><td class="amount">¥{{ detail.estimatedAmountDue !== null && detail.estimatedAmountDue !== undefined ? detail.estimatedAmountDue : (detail.payment ? detail.payment.amountDue : '—') }}</td></tr>
          <tr><th>支付状态</th><td>{{ detail.paymentStatus || '—' }}</td></tr>
          <tr v-if="detail.payment"><th>实收金额</th><td>¥{{ detail.payment.amountPaid }}（{{ detail.payment.paymentMethod }}）</td></tr>
          <tr v-if="detail.abnormalReason"><th>异常原因</th><td>{{ detail.abnormalReason }}</td></tr>
        </tbody>
      </table>
    </div>
  </div>
</template>

<script setup>
import { onMounted, ref } from 'vue'
import { listOwnRecords, getOwnRecord } from '../../api/ownerRecord'

const records = ref([])
const detail = ref(null)
const error = ref('')
const loading = ref(false)
const status = ref('')

function formatTime(value) {
  return value ? new Date(value).toLocaleString('zh-CN') : '—'
}

function statusLabel(value) {
  return { PARKING: '在场', COMPLETED: '已完成', ABNORMAL: '异常' }[value] || value
}

function statusClass(value) {
  return { PARKING: 'parking', COMPLETED: 'completed', ABNORMAL: 'abnormal' }[value] || 'inactive'
}

function paymentLabel(row) {
  if (row.paymentStatus === 'UNPAID') return '未支付'
  if (row.paymentStatus === 'PAID') return '已收款'
  if (row.paymentStatus === 'WAIVED') return '免费放行'
  if (row.paymentStatus === 'ABNORMAL') return '异常离场'
  return '—'
}

async function search() {
  error.value = ''
  loading.value = true
  try {
    const res = await listOwnRecords(status.value || undefined)
    records.value = res.data || []
    detail.value = null
  } catch (e) {
    error.value = e.message || '查询失败'
  } finally {
    loading.value = false
  }
}

async function showDetail(id) {
  error.value = ''
  try {
    const res = await getOwnRecord(id)
    detail.value = res.data
  } catch (e) {
    error.value = e.message || '详情加载失败'
  }
}

onMounted(search)
</script>
