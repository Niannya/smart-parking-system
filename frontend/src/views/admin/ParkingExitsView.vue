<template>
  <div>
    <div class="panel">
      <h2>离场缴费</h2>
      <div class="row">
        <div class="field">
          <label>车牌号</label>
          <input v-model="query.licensePlate" placeholder="京A12345" @keyup.enter="search" />
        </div>
        <div class="field">
          <label>或 记录编号</label>
          <input v-model="query.recordNo" placeholder="R..." @keyup.enter="search" />
        </div>
        <button class="primary" @click="search" :disabled="searching">{{ searching ? '查询中…' : '查询在场记录' }}</button>
      </div>
      <p class="muted" style="margin-bottom:0">车牌与记录编号二者填一个即可。</p>
    </div>

    <div v-if="error" class="error">{{ error }}</div>
    <div v-if="success" class="success">{{ success }}</div>

    <div class="panel" v-if="record">
      <h3>在场记录</h3>
      <table>
        <tbody>
          <tr><th style="width:120px">记录编号</th><td class="mono">{{ record.recordNo }}</td></tr>
          <tr><th>车牌号</th><td>{{ record.licensePlate }}</td></tr>
          <tr><th>车位</th><td>{{ record.areaCode }} 区 / {{ record.spaceCode }}</td></tr>
          <tr><th>入场时间</th><td>{{ formatTime(record.entryTime) }}</td></tr>
        </tbody>
      </table>
      <div class="row" style="margin-top:14px">
        <button class="ghost" @click="loadQuote" :disabled="quoting">{{ quoting ? '计算中…' : '刷新费用预览' }}</button>
        <button class="danger" @click="confirmAbnormal" :disabled="abnormaling">异常离场</button>
      </div>
    </div>

    <div class="panel" v-if="quote">
      <h3>费用预览</h3>
      <table>
        <tbody>
          <tr><th style="width:120px">停车时长</th><td>{{ quote.durationMinutes }} 分钟</td></tr>
          <tr><th>预览时刻</th><td>{{ formatTime(quote.quotedExitTime) }}</td></tr>
          <tr><th>应收金额</th><td class="amount">¥{{ quote.amountDue }}</td></tr>
        </tbody>
      </table>
      <p class="muted">预览仅供参考，确认离场时按服务器当时时刻重新计算。</p>
    </div>

    <div class="panel" v-if="record && quote">
      <h3>确认离场</h3>
      <div class="row">
        <div class="field">
          <label>支付方式</label>
          <select v-model="payForm.paymentMethod">
            <option value="CASH">现金</option>
            <option value="ELECTRONIC">电子支付</option>
            <option value="FREE_RELEASE">免费放行</option>
          </select>
        </div>
        <div class="field" v-if="payForm.paymentMethod !== 'FREE_RELEASE'">
          <label>实收金额（元）</label>
          <input v-model="payForm.amountPaid" placeholder="必须等于应收金额" />
        </div>
        <div class="field" v-if="payForm.paymentMethod === 'FREE_RELEASE'">
          <label>放行原因（必填）</label>
          <input v-model="payForm.remark" placeholder="如：人工核实后减免" />
        </div>
        <button class="primary" @click="confirmExit" :disabled="exiting">{{ exiting ? '提交中…' : '确认离场' }}</button>
      </div>
      <div class="form-error">{{ payError }}</div>
    </div>

    <div class="panel" v-if="abnormalOpen">
      <h3>异常离场（不生成支付记录）</h3>
      <div class="row">
        <div class="field" style="flex:1;min-width:280px">
          <label>异常原因（必填）</label>
          <input v-model="abnormalReason" placeholder="如：道闸故障人工放行" />
        </div>
        <button class="danger" @click="submitAbnormal" :disabled="abnormaling">确认异常离场</button>
        <button class="ghost" @click="abnormalOpen = false">取消</button>
      </div>
      <p class="muted">异常离场将记录 ABNORMAL 并释放车位，不产生支付记录，请谨慎操作。</p>
    </div>
  </div>
</template>

<script setup>
import { reactive, ref } from 'vue'
import { findActiveRecord, getQuote, exitRecord, abnormalExit } from '../../api/exit'

const query = reactive({ licensePlate: '', recordNo: '' })
const record = ref(null)
const quote = ref(null)
const error = ref('')
const success = ref('')
const searching = ref(false)
const quoting = ref(false)
const exiting = ref(false)
const abnormaling = ref(false)
const payError = ref('')
const abnormalOpen = ref(false)
const abnormalReason = ref('')

const payForm = reactive({ paymentMethod: 'CASH', amountPaid: '', remark: '' })

function formatTime(value) {
  return value ? new Date(value).toLocaleString('zh-CN') : '—'
}

function resetResult() {
  record.value = null
  quote.value = null
  error.value = ''
  success.value = ''
  payError.value = ''
  abnormalOpen.value = false
  abnormalReason.value = ''
}

async function search() {
  payError.value = ''
  const plate = query.licensePlate.trim()
  const recordNo = query.recordNo.trim()
  if (plate && recordNo) {
    error.value = '车牌与记录编号只能填写一个'
    return
  }
  if (!plate && !recordNo) {
    error.value = '请填写车牌号或记录编号'
    return
  }
  resetResult()
  searching.value = true
  try {
    const res = await findActiveRecord({ licensePlate: plate, recordNo })
    record.value = res.data
    // 查到后自动带出费用预览
    await loadQuote()
  } catch (e) {
    error.value = e.message || '查询失败'
  } finally {
    searching.value = false
  }
}

async function loadQuote() {
  if (!record.value) {
    return
  }
  quoting.value = true
  try {
    const res = await getQuote(record.value.id)
    quote.value = res.data
    payForm.amountPaid = String(res.data.amountDue)
  } catch (e) {
    quote.value = null
    error.value = e.message || '费用预览失败'
  } finally {
    quoting.value = false
  }
}

async function confirmExit() {
  payError.value = ''
  error.value = ''
  success.value = ''
  if (payForm.paymentMethod === 'FREE_RELEASE' && !payForm.remark.trim()) {
    payError.value = '免费放行必须填写原因'
    return
  }
  if (payForm.paymentMethod !== 'FREE_RELEASE') {
    if (!payForm.amountPaid || Number.isNaN(Number(payForm.amountPaid))) {
      payError.value = '请填写实收金额'
      return
    }
    if (quote.value && Number(payForm.amountPaid) !== Number(quote.value.amountDue)) {
      payError.value = `实收金额必须等于应收金额 ¥${quote.value.amountDue}`
      return
    }
  }
  exiting.value = true
  try {
    const res = await exitRecord(record.value.id, {
      paymentMethod: payForm.paymentMethod,
      amountPaid: payForm.paymentMethod === 'FREE_RELEASE' ? 0 : Number(payForm.amountPaid),
      remark: payForm.paymentMethod === 'FREE_RELEASE' ? payForm.remark.trim() : (payForm.remark.trim() || null)
    })
    success.value = `离场成功，支付编号 ${res.data.paymentNo}，应收 ¥${res.data.amountDue}` +
      (res.data.amountPaid !== null ? `，实收 ¥${res.data.amountPaid}` : '')
    record.value = null
    quote.value = null
  } catch (e) {
    payError.value = e.message || '离场失败'
  } finally {
    exiting.value = false
  }
}

function confirmAbnormal() {
  abnormalOpen.value = true
  abnormalReason.value = ''
}

async function submitAbnormal() {
  payError.value = ''
  error.value = ''
  success.value = ''
  if (!abnormalReason.value.trim()) {
    payError.value = '异常原因必填'
    return
  }
  abnormaling.value = true
  try {
    await abnormalExit(record.value.id, abnormalReason.value.trim())
    success.value = '已按异常离场处理，车位已释放，未生成支付记录'
    record.value = null
    quote.value = null
    abnormalOpen.value = false
  } catch (e) {
    payError.value = e.message || '操作失败'
  } finally {
    abnormaling.value = false
  }
}
</script>
