<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { api } from '../api/admin'

const route = useRoute()
const router = useRouter()
const data = ref<any>()
const followUpForm = reactive<any>({ followUpType: 'GENERAL', content: '', nextFollowUpAt: null })
const savingFollowUp = ref(false)
const statusLabels: Record<number, string> = { 0: '待处理', 1: '跟进中', 2: '待资料', 3: '办理中', 4: '待激活', 5: '已激活', 6: '已完成', 9: '无效' }
const orderStatusLabels: Record<string, string> = { PENDING: '待处理', FOLLOWING: '跟进中', SUBMITTED_UMALL: '已提交UMALL', UNDER_REVIEW: '审核中', NEED_SUPPLEMENT: '待补件', WAITING_ACTIVATION: '待激活', ACTIVATED: '已激活', COMPLETED: '已完成', AFTER_SALES: '售后中', CANCELLED: '已取消' }

async function addFollowUp() {
  if (!followUpForm.content.trim()) return ElMessage.warning('请输入跟进内容')
  savingFollowUp.value = true
  try {
    await api.addCustomerFollowUp(Number(route.params.id), followUpForm)
    data.value = await api.customer(Number(route.params.id))
    followUpForm.content = ''
    followUpForm.nextFollowUpAt = null
    ElMessage.success('跟进记录已添加')
  } catch (error: any) {
    ElMessage.error(error.message)
  } finally {
    savingFollowUp.value = false
  }
}

onMounted(async () => {
  try {
    data.value = await api.customer(Number(route.params.id))
  } catch (error: any) {
    ElMessage.error(error.message)
  }
})
</script>

<template>
  <div v-if="data" class="page">
    <div class="page-heading"><div><h1>{{ data.customer.name || '未命名客户' }}</h1><p>客户完整业务链：渠道、订单、ICCID、UMALL、对账和二级渠道结算。</p></div><el-button @click="router.back()">返回列表</el-button></div>
    <div class="detail-grid">
      <div v-for="item in [
        ['手机号', data.customer.phone],
        ['归属类型', data.customer.customerType === 'CHANNEL' ? '渠道客户' : '自营客户'],
        ['客户类别', data.customer.customerCategory],
        ['渠道', data.channel?.channelName || (data.customer.channelId ? `渠道 ${data.customer.channelId}` : null)],
        ['内部负责人', data.owner?.displayName || data.owner?.username],
        ['意向套餐', data.customer.intendedPlan],
        ['当前状态', statusLabels[Number(data.customer.currentStatus)] || `未知(${data.customer.currentStatus})`],
        ['需求摘要', data.customer.requirementSummary],
        ['联系方式', data.customer.contactMethod],
        ['创建时间', data.customer.createdAt],
        ['更新时间', data.customer.updatedAt]
      ]" :key="item[0]" class="detail-item">
        <div class="detail-label">{{ item[0] }}</div><div class="detail-value">{{ item[1] || '—' }}</div>
      </div>
    </div>
    <div class="table-card section-gap"><div class="table-toolbar"><span class="table-title">客户跟进</span><span class="table-meta">{{ data.followUps?.length || 0 }} 条</span></div><div style="padding:16px"><el-row :gutter="12"><el-col :span="4"><el-select v-model="followUpForm.followUpType" style="width:100%"><el-option label="常规跟进" value="GENERAL" /><el-option label="电话沟通" value="PHONE" /><el-option label="资料提醒" value="DOCUMENT_REMINDER" /></el-select></el-col><el-col :span="12"><el-input v-model="followUpForm.content" placeholder="填写业务沟通摘要，不得记录证件资料" /></el-col><el-col :span="5"><el-date-picker v-model="followUpForm.nextFollowUpAt" type="datetime" value-format="YYYY-MM-DDTHH:mm:ss" placeholder="下次跟进时间" style="width:100%" /></el-col><el-col :span="3"><el-button type="primary" :loading="savingFollowUp" @click="addFollowUp">添加</el-button></el-col></el-row></div><el-table :data="data.followUps"><el-table-column prop="createdAt" label="跟进时间" width="180" /><el-table-column prop="followUpType" label="类型" width="130" /><el-table-column prop="content" label="内容" min-width="300" /><el-table-column prop="nextFollowUpAt" label="下次跟进" width="180" /><el-table-column prop="operatorName" label="操作人" width="120" /></el-table></div>
    <div class="table-card section-gap"><div class="table-toolbar"><span class="table-title">办理订单</span><span class="table-meta">{{ data.orders.length }} 条</span></div><el-table :data="data.orders"><el-table-column prop="orderNo" label="内部订单号" min-width="180" /><el-table-column prop="umallOrderNo" label="UMALL订单号" min-width="160" /><el-table-column prop="planName" label="套餐" min-width="170" /><el-table-column label="办理状态" width="120"><template #default="scope">{{ orderStatusLabels[scope.row.status] || scope.row.status }}</template></el-table-column><el-table-column prop="reviewStatus" label="审核状态" width="110" /><el-table-column prop="supplementStatus" label="补件状态" width="110" /><el-table-column prop="activationStatus" label="激活状态" width="110" /><el-table-column prop="contractStatus" label="合约状态" width="110" /><el-table-column prop="reconciliationStatus" label="对账状态" width="110" /></el-table></div>
    <div class="table-card section-gap"><div class="table-toolbar"><span class="table-title">ICCID 配对</span></div><el-table :data="data.iccids"><el-table-column prop="iccid" label="ICCID" /><el-table-column prop="status" label="状态" /><el-table-column prop="currentOrderId" label="订单ID" /><el-table-column prop="assignedAt" label="分配时间" /></el-table></div>
    <div class="table-card section-gap"><div class="table-toolbar"><span class="table-title">推荐号码接龙</span><span class="table-meta">{{ data.referralNumbers?.length || 0 }} 条</span></div><el-table :data="data.referralNumbers"><el-table-column prop="referralNumber" label="推荐号码" min-width="140" /><el-table-column prop="status" label="状态" width="120" /><el-table-column prop="chainId" label="接龙ID" width="100" /><el-table-column prop="assignedOrderId" label="占用订单ID" min-width="130" /><el-table-column prop="sourceOrderId" label="来源订单ID" min-width="130" /><el-table-column prop="reservedAt" label="占用时间" min-width="180" /><el-table-column prop="usedAt" label="使用时间" min-width="180" /></el-table></div>
    <div class="table-card section-gap"><div class="table-toolbar"><span class="table-title">对账与渠道结算</span></div><el-table :data="data.reconciliationRows"><el-table-column prop="umallOrderNo" label="UMALL订单号" /><el-table-column prop="matchMethod" label="匹配方式" /><el-table-column prop="matchStatus" label="匹配状态" /><el-table-column prop="activationStatus" label="激活状态" /><el-table-column prop="contractStatus" label="合约状态" /></el-table><el-table :data="data.commissionRecords" style="border-top: 1px solid #eee"><el-table-column prop="channelId" label="二级渠道ID" /><el-table-column prop="channelPayable" label="渠道应结" /><el-table-column prop="finalAmount" label="最终金额" /><el-table-column prop="status" label="结算状态" /></el-table></div>
  </div>
</template>
