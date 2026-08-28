<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { api } from '../api/admin'
import { session } from '../api/http'

const router = useRouter()
const current = session()
const loading = ref(false)
const data = ref<any>({ overview: {}, operations: {}, resources: {}, finance: {}, channelBreakdown: [], definitions: {} })
const channels = ref<any[]>([])
const filters = reactive<any>({ dates: [], channelId: current?.scopeType === 'CHANNEL' ? current.scopeId : null })
const canSelectChannel = computed(() => current?.scopeType !== 'CHANNEL')

const overviewCards = computed(() => [
  { key: 'customers', label: '客户数', value: data.value.overview.customers, hint: '筛选范围内创建', path: '/customers' },
  { key: 'orders', label: '订单数', value: data.value.overview.orders, hint: '筛选范围内创建', path: '/orders' },
  { key: 'onboarded', label: '上台量', value: data.value.overview.onboarded, hint: `上台率 ${formatRate(data.value.overview.onboardingRate)}`, path: '/orders' },
  { key: 'activated', label: '激活量', value: data.value.overview.activated, hint: `激活率 ${formatRate(data.value.overview.activationRate)}`, path: '/orders' }
])

const operationCards = computed(() => [
  { label: '待补件', value: data.value.operations.pendingSupplements, path: '/orders?status=NEED_SUPPLEMENT' },
  { label: '任务积压', value: data.value.operations.pendingTasks, path: '/tasks' },
  { label: '对账异常', value: data.value.operations.reconciliationExceptions, path: '/reconciliation/exceptions' }
])

const resourceCards = computed(() => [
  { label: '可用 ICCID', value: data.value.resources.availableIccids, path: '/resources/iccids?status=AVAILABLE' },
  { label: '已使用 ICCID', value: data.value.resources.usedIccids, path: '/resources/iccids?status=USED' },
  { label: '待替换虚拟卡', value: data.value.resources.virtualPendingReplacement, path: '/resources/virtual-iccid-replacement' }
])

function formatRate(value: any) {
  return `${Number(value || 0).toFixed(2)}%`
}

function formatMoney(value: any) {
  return value == null ? '无金额权限' : `HK$${Number(value).toFixed(2)}`
}

async function load() {
  loading.value = true
  try {
    const params: any = { channelId: filters.channelId || undefined }
    if (filters.dates?.length === 2) {
      params.startDate = filters.dates[0]
      params.endDate = filters.dates[1]
    }
    data.value = await api.analytics(params)
  } catch (error: any) {
    ElMessage.error(error.message)
  } finally {
    loading.value = false
  }
}

function reset() {
  filters.dates = []
  filters.channelId = current?.scopeType === 'CHANNEL' ? current.scopeId : null
  load()
}

onMounted(async () => {
  if (canSelectChannel.value) {
    channels.value = await api.customerChannels() as any[]
  }
  await load()
})
</script>

<template>
  <div class="page" v-loading="loading">
    <div class="page-heading"><div><h1>基础数据分析</h1><p>按时间和渠道查看客户、履约、资源与收益指标。</p></div></div>
    <div class="filter-bar analytics-filter">
      <el-date-picker v-model="filters.dates" type="daterange" value-format="YYYY-MM-DD" start-placeholder="开始日期" end-placeholder="结束日期" />
      <el-select v-if="canSelectChannel" v-model="filters.channelId" clearable placeholder="全部渠道"><el-option v-for="channel in channels" :key="channel.id" :label="channel.channelName" :value="channel.id" /></el-select>
      <el-button type="primary" @click="load">查询</el-button><el-button @click="reset">重置</el-button>
    </div>

    <div class="metric-grid analytics-grid"><div v-for="item in overviewCards" :key="item.key" class="metric-card clickable" @click="router.push(item.path)"><div class="metric-label">{{ item.label }}</div><div class="metric-value">{{ item.value ?? '—' }}</div><div class="muted metric-hint">{{ item.hint }}</div></div></div>

    <div class="analytics-sections section-gap">
      <section class="table-card"><div class="table-toolbar"><span class="table-title">运营与异常</span></div><div class="mini-grid"><div v-for="item in operationCards" :key="item.label" class="mini-card" @click="router.push(item.path)"><span>{{ item.label }}</span><b>{{ item.value ?? '—' }}</b></div></div></section>
      <section class="table-card"><div class="table-toolbar"><span class="table-title">资源库存</span><span class="table-meta">当前时点</span></div><div class="mini-grid"><div v-for="item in resourceCards" :key="item.label" class="mini-card" @click="router.push(item.path)"><span>{{ item.label }}</span><b>{{ item.value ?? '—' }}</b></div></div></section>
    </div>

    <section class="table-card section-gap"><div class="table-toolbar"><span class="table-title">收益概览</span><span class="table-meta">金额仅管理员可见</span></div><div class="finance-grid"><div><span>佣金记录</span><b>{{ data.finance.commissionRecords ?? '—' }}</b><small>{{ formatMoney(data.finance.commissionAmount) }}</small></div><div><span>待确认结算</span><b>{{ data.finance.pendingSettlements ?? '—' }}</b><small @click="router.push('/secondary/settlements')">查看结算记录</small></div><div><span>返现计划</span><b>{{ data.finance.cashbackPlans ?? '—' }}</b><small>{{ formatMoney(data.finance.cashbackAmount) }}</small></div><div><span>待激活返现</span><b>{{ data.finance.pendingActivationCashbacks ?? '—' }}</b><small @click="router.push('/cashbacks')">查看返现计划</small></div></div></section>

    <section class="table-card section-gap"><div class="table-toolbar"><span class="table-title">渠道转化</span><span class="table-meta">点击渠道可筛选首页</span></div><el-table :data="data.channelBreakdown"><el-table-column prop="channelName" label="渠道" min-width="150"><template #default="scope"><el-button link type="primary" @click="filters.channelId=scope.row.channelId;load()">{{ scope.row.channelName }}</el-button></template></el-table-column><el-table-column prop="customers" label="客户数" width="100"/><el-table-column prop="orders" label="订单数" width="100"/><el-table-column prop="onboarded" label="上台量" width="100"/><el-table-column prop="activated" label="激活量" width="100"/><el-table-column label="上台率" width="110"><template #default="scope">{{ formatRate(scope.row.onboardingRate) }}</template></el-table-column><el-table-column label="激活率" width="110"><template #default="scope">{{ formatRate(scope.row.activationRate) }}</template></el-table-column></el-table></section>

    <el-collapse class="section-gap"><el-collapse-item title="指标口径说明"><div v-for="(text,key) in data.definitions" :key="key" class="definition-row"><b>{{ key }}</b><span>{{ text }}</span></div></el-collapse-item></el-collapse>
  </div>
</template>

<style scoped>
.analytics-filter{flex-wrap:wrap}.analytics-filter .el-select{width:220px}.analytics-grid{grid-template-columns:repeat(4,minmax(160px,1fr))}.clickable,.mini-card{cursor:pointer}.metric-hint{font-size:12px;margin-top:8px}.analytics-sections{display:grid;grid-template-columns:1fr 1fr;gap:18px}.mini-grid{display:grid;grid-template-columns:repeat(3,1fr);gap:12px;padding:0 18px 18px}.mini-card{padding:15px;border:1px solid #eee;border-radius:6px;display:flex;justify-content:space-between}.mini-card b{font-size:22px;color:#c81f1f}.finance-grid{display:grid;grid-template-columns:repeat(4,1fr);gap:16px;padding:0 18px 18px}.finance-grid div{display:flex;flex-direction:column;gap:7px}.finance-grid b{font-size:24px}.finance-grid small{color:#777;cursor:pointer}.definition-row{display:flex;gap:18px;padding:6px 0}.definition-row b{width:140px}@media(max-width:1280px){.analytics-sections{grid-template-columns:1fr}.analytics-grid{grid-template-columns:repeat(2,1fr)}}
</style>

