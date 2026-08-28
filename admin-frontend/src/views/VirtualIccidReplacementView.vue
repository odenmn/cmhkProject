<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { api } from '../api/admin'

const virtualCards = ref<any[]>([])
const realCards = ref<any[]>([])
const loading = ref(false)
const dialogVisible = ref(false)
const selected = ref<any>()
const form = reactive<any>({ realIccidId: null, reason: '' })

async function load() {
  loading.value = true
  try {
    const [used, available] = await Promise.all([api.iccids({ status: 'USED' }), api.iccids({ status: 'AVAILABLE' })]) as any
    virtualCards.value = used.filter((item: any) => item.inventory.cardType === 'VIRTUAL')
    realCards.value = available.filter((item: any) => (item.inventory.cardType || 'REAL') === 'REAL')
  } catch (error: any) {
    ElMessage.error(error.message)
  } finally {
    loading.value = false
  }
}

function open(row: any) {
  selected.value = row
  Object.assign(form, { realIccidId: null, reason: '' })
  dialogVisible.value = true
}

async function replaceCard() {
  if (!form.realIccidId || !form.reason.trim()) return ElMessage.warning('请选择真实 ICCID 并填写原因')
  try {
    await ElMessageBox.confirm('替换后虚拟卡会标记为已替换，真实卡继承订单、客户和上台号码。', '确认替换', { type: 'warning' })
    await api.replaceVirtualIccid(selected.value.inventory.id, form)
    dialogVisible.value = false
    ElMessage.success('替换完成，虚拟卡和真实卡历史均已记录')
    await load()
  } catch (error: any) {
    if (error !== 'cancel' && error !== 'close' && error?.message) ElMessage.error(error.message)
  }
}

onMounted(load)
</script>

<template>
  <div class="page">
    <div class="page-heading"><div><h1>虚拟 ICCID 替换</h1><p>将使用中的虚拟卡安全替换为真实可用卡，保留原绑定和完整历史。</p></div></div>
    <div class="hint-box">当前待替换 {{ virtualCards.length }} 张，可用真实卡 {{ realCards.length }} 张。替换为事务操作，不会同时保留两张当前绑定卡。</div>
    <div class="table-card section-gap"><div class="table-toolbar"><span class="table-title">待替换虚拟卡</span></div><div class="table-scroll"><el-table class="desktop-data-table replacement-table" :data="virtualCards" v-loading="loading"><el-table-column prop="inventory.iccid" label="虚拟 ICCID" min-width="190" /><el-table-column prop="inventory.serviceNumber" label="上台号码" min-width="130" /><el-table-column prop="customerName" label="客户" min-width="110" /><el-table-column prop="orderNo" label="内部订单号" min-width="160" /><el-table-column prop="inventory.assignedAt" label="绑定时间" min-width="180" /><el-table-column label="操作" width="110"><template #default="scope"><el-button link type="primary" @click="open(scope.row)">替换</el-button></template></el-table-column></el-table></div></div>
    <el-dialog v-model="dialogVisible" title="选择真实 ICCID" width="560"><el-form label-position="top"><el-form-item label="真实可用 ICCID"><el-select v-model="form.realIccidId" filterable style="width:100%"><el-option v-for="item in realCards" :key="item.inventory.id" :label="`${item.inventory.iccid} · ${item.inventory.batchNo || '无批次'}`" :value="item.inventory.id" /></el-select></el-form-item><el-form-item label="替换原因"><el-input v-model="form.reason" type="textarea" /></el-form-item></el-form><template #footer><el-button @click="dialogVisible = false">取消</el-button><el-button type="primary" @click="replaceCard">确认替换</el-button></template></el-dialog>
  </div>
</template>

<style scoped>.replacement-table{min-width:980px}</style>
