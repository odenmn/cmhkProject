<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { api } from '../api/admin'

const statusOptions = [{value:'PENDING',label:'待处理'},{value:'FOLLOWING',label:'跟进中'},{value:'SUBMITTED_UMALL',label:'已提交UMALL'},{value:'UNDER_REVIEW',label:'审核中'},{value:'NEED_SUPPLEMENT',label:'待补件'},{value:'WAITING_ACTIVATION',label:'待激活'},{value:'ACTIVATED',label:'已激活'},{value:'COMPLETED',label:'已完成'},{value:'AFTER_SALES',label:'售后中'},{value:'CANCELLED',label:'已取消'}]
const statusLabels = Object.fromEntries(statusOptions.map(item => [item.value, item.label]))
const rows = ref<any[]>([])
const customers = ref<any[]>([])
const loading = ref(false)
const visible = ref(false)
const historyVisible = ref(false)
const historyRows = ref<any[]>([])
const editing = ref<any>()
const filters = reactive<any>({ keyword: '', status: '', customerId: null })
const empty = () => ({ customerId: null, customerName: '', contactPhone: '', planCode: '', planName: '', planType: '', monthlyFee: 0, contractPeriod: '', umallOrderNo: '', serviceNumber: '', reviewStatus: '', supplementStatus: '', activationStatus: '', contractStatus: '', umallStatus: '', orderSource: 'ADMIN', status: 'PENDING', reconciliationStatus: '待对账' })
const form = reactive<any>(empty())

async function load() {
  loading.value = true
  try {
    [rows.value, customers.value] = await Promise.all([api.orders(filters), api.customers()]) as any
  } catch (error: any) {
    ElMessage.error(error.message)
  } finally {
    loading.value = false
  }
}

function open(row?: any) {
  editing.value = row
  Object.assign(form, empty(), row || {})
  visible.value = true
}

function customerChanged(id: number) {
  const customer = customers.value.find(item => item.id === id)
  if (customer) {
    form.customerName = customer.name
    form.contactPhone = customer.phone
  }
}

async function openHistory(row: any) {
  try {
    historyRows.value = await api.orderStatusHistory(row.id) as any
    historyVisible.value = true
  } catch (error: any) {
    ElMessage.error(error.message)
  }
}

async function save() {
  try {
    if (editing.value) await api.updateOrder(editing.value.id, form)
    else await api.createOrder(form)
    ElMessage.success('订单已保存')
    visible.value = false
    await load()
  } catch (error: any) {
    ElMessage.error(error.message)
  }
}

onMounted(load)
</script>
<template><div class="page"><div class="page-heading"><div><h1>订单管理</h1><p>管理 JOINCOM 标准办理状态，并保留 UMALL 原始状态和完整变更历史。</p></div><el-button type="primary" @click="open()">新增订单</el-button></div><div class="filter-bar"><el-input v-model="filters.keyword" placeholder="订单号 / 上台号码" clearable/><el-select v-model="filters.status" placeholder="办理状态" clearable><el-option v-for="item in statusOptions" :key="item.value" :label="item.label" :value="item.value"/></el-select><el-select v-model="filters.customerId" placeholder="客户" clearable filterable><el-option v-for="c in customers" :key="c.id" :label="`${c.name||'未命名'} · ${c.phone}`" :value="c.id"/></el-select><el-button @click="load">查询</el-button></div><div class="table-card"><div class="table-toolbar"><span class="table-title">业务订单</span><span class="table-meta">共 {{rows.length}} 条</span></div><div class="table-scroll"><el-table class="desktop-data-table desktop-data-table--orders" :data="rows" v-loading="loading"><el-table-column prop="orderNo" label="内部订单号" min-width="130"/><el-table-column prop="umallOrderNo" label="UMALL订单号" min-width="140"/><el-table-column prop="customerName" label="客户" min-width="90"/><el-table-column prop="planName" label="套餐" min-width="130"/><el-table-column label="月费" width="75"><template #default="s">HK${{Number(s.row.monthlyFee||0).toFixed(2)}}</template></el-table-column><el-table-column prop="serviceNumber" label="上台号码" min-width="110"/><el-table-column label="办理状态" width="105"><template #default="s">{{statusLabels[s.row.status]||s.row.status}}</template></el-table-column><el-table-column prop="reviewStatus" label="审核" width="80"/><el-table-column prop="activationStatus" label="激活" width="80"/><el-table-column prop="reconciliationStatus" label="对账" width="80"/><el-table-column label="操作" width="120"><template #default="s"><el-button link type="primary" @click="open(s.row)">编辑</el-button><el-button link @click="openHistory(s.row)">历史</el-button></template></el-table-column></el-table></div></div><el-dialog v-model="visible" :title="editing?'编辑订单':'新增订单'" width="780"><div class="hint-box" style="margin-bottom:18px">这里只记录外围业务信息，不上传身份证件、地址证明或 UMALL 正式身份资料。</div><el-form label-position="top"><el-row :gutter="18"><el-col :span="12"><el-form-item label="客户"><el-select v-model="form.customerId" filterable style="width:100%" @change="customerChanged"><el-option v-for="c in customers" :key="c.id" :label="`${c.name||'未命名'} · ${c.phone}`" :value="c.id"/></el-select></el-form-item></el-col><el-col :span="12"><el-form-item label="联系电话"><el-input v-model="form.contactPhone"/></el-form-item></el-col><el-col :span="12"><el-form-item label="套餐编码"><el-input v-model="form.planCode"/></el-form-item></el-col><el-col :span="12"><el-form-item label="套餐名称"><el-input v-model="form.planName"/></el-form-item></el-col><el-col :span="8"><el-form-item label="月费"><el-input-number v-model="form.monthlyFee" :precision="2" :min="0" style="width:100%"/></el-form-item></el-col><el-col :span="8"><el-form-item label="合约期"><el-input v-model="form.contractPeriod" placeholder="例如 24个月"/></el-form-item></el-col><el-col :span="8"><el-form-item label="办理状态"><el-select v-model="form.status" style="width:100%"><el-option v-for="item in statusOptions" :key="item.value" :label="item.label" :value="item.value"/></el-select></el-form-item></el-col><el-col :span="12"><el-form-item label="UMALL订单号"><el-input v-model="form.umallOrderNo"/></el-form-item></el-col><el-col :span="12"><el-form-item label="上台号码"><el-input v-model="form.serviceNumber"/></el-form-item></el-col><el-col :span="8"><el-form-item label="UMALL原始状态"><el-input v-model="form.umallStatus"/></el-form-item></el-col><el-col :span="8"><el-form-item label="审核状态"><el-input v-model="form.reviewStatus"/></el-form-item></el-col><el-col :span="8"><el-form-item label="补件状态"><el-input v-model="form.supplementStatus"/></el-form-item></el-col><el-col :span="8"><el-form-item label="激活状态"><el-input v-model="form.activationStatus"/></el-form-item></el-col><el-col :span="8"><el-form-item label="合约状态"><el-input v-model="form.contractStatus"/></el-form-item></el-col><el-col :span="8"><el-form-item label="对账状态"><el-input v-model="form.reconciliationStatus"/></el-form-item></el-col></el-row></el-form><template #footer><div class="dialog-footer"><el-button @click="visible=false">取消</el-button><el-button type="primary" @click="save">保存</el-button></div></template></el-dialog><el-drawer v-model="historyVisible" title="订单状态历史" size="620px"><el-table :data="historyRows"><el-table-column prop="createdAt" label="时间" width="175"/><el-table-column prop="statusType" label="状态类型" width="130"/><el-table-column prop="beforeStatus" label="变更前" min-width="120"/><el-table-column prop="afterStatus" label="变更后" min-width="120"/><el-table-column prop="sourceType" label="来源" width="110"/><el-table-column prop="operatorName" label="操作人" width="100"/></el-table></el-drawer></div></template>

