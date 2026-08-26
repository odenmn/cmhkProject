<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { api } from '../api/admin'

const statusOptions = [
  { value: 0, label: '待处理' },
  { value: 1, label: '跟进中' },
  { value: 2, label: '待资料' },
  { value: 3, label: '办理中' },
  { value: 4, label: '待激活' },
  { value: 5, label: '已激活' },
  { value: 6, label: '已完成' },
  { value: 9, label: '无效' }
]

const router = useRouter()
const rows = ref<any[]>([])
const channels = ref<any[]>([])
const loading = ref(false)
const visible = ref(false)
const editing = ref<any>(null)
const filters = reactive<{ keyword: string; type: string; status?: number }>({ keyword: '', type: '', status: undefined })
const emptyForm = () => ({ name: '', phone: '', contactMethod: '', customerType: 'DIRECT', customerCategory: '', channelId: null, intendedPlan: '', requirementSummary: '', currentStatus: 0 })
const form = reactive<any>(emptyForm())

function statusLabel(value: number) {
  const numericValue = Number(value)
  return statusOptions.find(item => item.value === numericValue)?.label || `未知(${value})`
}

function channelLabel(channelId?: number) {
  return channels.value.find(item => item.id === Number(channelId))?.channelName || (channelId ? `渠道 ${channelId}` : '—')
}

async function load() {
  loading.value = true
  try {
    const result = await Promise.all([api.customers(filters), api.customerChannels()]) as any
    rows.value = result[0]
    channels.value = result[1]
  } catch (error: any) {
    ElMessage.error(error.message)
  } finally {
    loading.value = false
  }
}

function open(row?: any) {
  editing.value = row || null
  Object.assign(form, emptyForm(), row || {})
  visible.value = true
}

async function save() {
  try {
    if (editing.value) {
      await api.updateCustomer(editing.value.id, form)
    } else {
      await api.createCustomer(form)
    }
    ElMessage.success('客户已保存')
    visible.value = false
    await load()
  } catch (error: any) {
    ElMessage.error(error.message)
  }
}

onMounted(load)
</script>

<template>
  <div class="page">
    <div class="page-heading"><div><h1>客户管理</h1><p>客户档案是渠道、订单、ICCID 与对账数据的关联入口。</p></div><el-button type="primary" @click="open()">新增客户</el-button></div>
    <div class="filter-bar">
      <el-input v-model="filters.keyword" placeholder="姓名或手机号" clearable />
      <el-select v-model="filters.type" placeholder="归属类型" clearable><el-option label="自营客户" value="DIRECT" /><el-option label="渠道客户" value="CHANNEL" /></el-select>
      <el-select v-model="filters.status" placeholder="当前状态" clearable><el-option v-for="item in statusOptions" :key="item.value" :label="item.label" :value="item.value" /></el-select>
      <el-button @click="load">查询</el-button>
    </div>
    <div class="table-card">
      <div class="table-toolbar"><span class="table-title">客户档案</span><span class="table-meta">共 {{ rows.length }} 位客户</span></div>
      <el-table :data="rows" v-loading="loading">
        <el-table-column prop="name" label="客户姓名" min-width="120" />
        <el-table-column label="上台号码" min-width="140"><template #default="scope">{{ scope.row.serviceNumber || '—' }}</template></el-table-column>
        <el-table-column prop="contactMethod" label="联系方式" min-width="140" />
        <el-table-column label="归属类型" width="110"><template #default="scope">{{ scope.row.customerType === 'CHANNEL' ? '渠道客户' : '自营客户' }}</template></el-table-column>
        <el-table-column prop="customerCategory" label="客户类别" width="120" />
        <el-table-column label="渠道" min-width="150"><template #default="scope">{{ channelLabel(scope.row.channelId) }}</template></el-table-column>
        <el-table-column prop="intendedPlan" label="意向套餐" min-width="160" />
        <el-table-column label="当前状态" width="120"><template #default="scope">{{ statusLabel(scope.row.currentStatus) }}</template></el-table-column>
        <el-table-column prop="createdAt" label="创建时间" min-width="165" />
        <el-table-column label="操作" fixed="right" width="150"><template #default="scope"><el-button link type="primary" @click="router.push(`/customers/${scope.row.id}`)">详情</el-button><el-button link @click="open(scope.row)">编辑</el-button></template></el-table-column>
      </el-table>
    </div>
    <el-dialog v-model="visible" :title="editing ? '编辑客户' : '新增客户'" width="680">
      <el-form label-position="top" class="customer-form">
        <el-row :gutter="18">
          <el-col :span="12"><el-form-item label="客户姓名"><el-input v-model="form.name" /></el-form-item></el-col>
          <el-col :span="12"><el-form-item label="手机号"><el-input v-model="form.phone" /></el-form-item></el-col>
          <el-col :span="12"><el-form-item label="联系方式"><el-input v-model="form.contactMethod" placeholder="企业微信 / Email" /></el-form-item></el-col>
          <el-col :span="12"><el-form-item label="归属类型"><el-select v-model="form.customerType" style="width: 100%"><el-option label="自营客户" value="DIRECT" /><el-option label="渠道客户" value="CHANNEL" /></el-select></el-form-item></el-col>
          <el-col :span="12"><el-form-item label="客户类别"><el-input v-model="form.customerCategory" placeholder="留学生 / 地产客户" /></el-form-item></el-col>
          <el-col :span="12"><el-form-item label="渠道"><el-select v-model="form.channelId" clearable style="width: 100%"><el-option v-for="channel in channels" :key="channel.id" :label="channel.channelName" :value="channel.id" /></el-select></el-form-item></el-col>
          <el-col :span="12"><el-form-item label="当前状态"><el-select v-model="form.currentStatus" style="width: 100%"><el-option v-for="item in statusOptions" :key="item.value" :label="item.label" :value="item.value" /></el-select></el-form-item></el-col>
          <el-col :span="24"><el-form-item label="意向套餐"><el-input v-model="form.intendedPlan" /></el-form-item></el-col>
          <el-col :span="24"><el-form-item label="需求摘要"><el-input v-model="form.requirementSummary" type="textarea" :rows="3" /></el-form-item></el-col>
        </el-row>
      </el-form>
      <template #footer><div class="dialog-footer"><el-button @click="visible = false">取消</el-button><el-button type="primary" @click="save">保存</el-button></div></template>
    </el-dialog>
  </div>
</template>
