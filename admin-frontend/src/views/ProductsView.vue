<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { api } from '../api/admin'
import { session } from '../api/http'

const activeTab = ref('plans')
const plans = ref<any[]>([])
const policies = ref<any[]>([])
const channels = ref<any[]>([])
const loading = ref(false)
const planVisible = ref(false)
const offerVisible = ref(false)
const policyVisible = ref(false)
const editingPlan = ref<any>()
const editingOffer = ref<any>()
const editingPolicy = ref<any>()
const selectedPlan = ref<any>()
const canEdit = session()?.role === 'ADMIN'

const emptyPlan = () => ({ planCode: '', planName: '', planType: '', monthlyFee: 0, channelPriceText: '', effectiveMonthlyFee: null, officialMonthlyFee: null, dataQuota: '', voiceQuota: '', roamingBenefit: '', contractPeriod: '', promotionEndDate: null, description: '', sortOrder: 0, enabled: 1 })
const emptyOffer = () => ({ planCode: '', offerType: '', offerName: '', offerValue: '', sortOrder: 0, enabled: 1 })
const emptyPolicy = () => ({ channelId: null, planId: null, promotable: 1, effectiveFrom: null, effectiveTo: null, cashbackRuleRef: '', commissionRuleRef: '' })
const planForm = reactive<any>(emptyPlan())
const offerForm = reactive<any>(emptyOffer())
const policyForm = reactive<any>(emptyPolicy())

async function load() {
  loading.value = true
  try {
    const result = await Promise.all([api.plans(), api.productPolicies(), api.channels()]) as any
    plans.value = result[0]
    policies.value = result[1]
    channels.value = result[2]
  } catch (error: any) {
    ElMessage.error(error.message)
  } finally {
    loading.value = false
  }
}

function openPlan(row?: any) {
  editingPlan.value = row
  Object.assign(planForm, emptyPlan(), row || {})
  planVisible.value = true
}

async function savePlan() {
  try {
    if (editingPlan.value) await api.updatePlan(editingPlan.value.id, planForm)
    else await api.createPlan(planForm)
    ElMessage.success('套餐已保存')
    planVisible.value = false
    await load()
  } catch (error: any) {
    ElMessage.error(error.message)
  }
}

async function disablePlan(row: any) {
  await ElMessageBox.confirm('套餐将下架但不会删除历史订单快照，是否继续？', '确认下架')
  await api.disablePlan(row.id)
  ElMessage.success('套餐已下架')
  await load()
}

function openOffers(plan: any, offer?: any) {
  selectedPlan.value = plan
  editingOffer.value = offer
  Object.assign(offerForm, emptyOffer(), offer || {}, { planCode: plan.planCode })
  offerVisible.value = true
}

async function saveOffer() {
  try {
    if (editingOffer.value) await api.updateOffer(editingOffer.value.id, offerForm)
    else await api.createOffer(offerForm)
    ElMessage.success('权益已保存')
    offerVisible.value = false
    await load()
  } catch (error: any) {
    ElMessage.error(error.message)
  }
}

async function deleteOffer(id: number) {
  await ElMessageBox.confirm('确认删除这条套餐权益？', '删除权益')
  await api.deleteOffer(id)
  ElMessage.success('权益已删除')
  await load()
}

function openPolicy(row?: any) {
  editingPolicy.value = row?.policy
  Object.assign(policyForm, emptyPolicy(), row?.policy || {})
  policyVisible.value = true
}

async function savePolicy() {
  try {
    if (editingPolicy.value) await api.updateProductPolicy(editingPolicy.value.id, policyForm)
    else await api.createProductPolicy(policyForm)
    ElMessage.success('渠道产品政策已保存')
    policyVisible.value = false
    await load()
  } catch (error: any) {
    ElMessage.error(error.message)
  }
}

async function deletePolicy(row: any) {
  await ElMessageBox.confirm('确认删除这条渠道产品政策？', '删除政策')
  await api.deleteProductPolicy(row.policy.id)
  ElMessage.success('政策已删除')
  await load()
}

onMounted(load)
</script>

<template>
  <div class="page">
    <div class="page-heading">
      <div><h1>产品管理</h1><p>维护套餐、权益和渠道可推广范围；历史订单继续使用创建时的套餐快照。</p></div>
      <el-button v-if="canEdit" type="primary" @click="activeTab === 'plans' ? openPlan() : openPolicy()">{{ activeTab === 'plans' ? '新增套餐' : '新增政策' }}</el-button>
    </div>
    <el-tabs v-model="activeTab">
      <el-tab-pane label="套餐与权益" name="plans">
        <div class="table-card">
          <el-table :data="plans" v-loading="loading">
            <el-table-column prop="planCode" label="套餐编码" min-width="210" />
            <el-table-column prop="planName" label="套餐名称" min-width="180" />
            <el-table-column prop="planType" label="类型" width="120" />
            <el-table-column label="月费" width="100"><template #default="scope">HK${{ Number(scope.row.monthlyFee || 0).toFixed(2) }}</template></el-table-column>
            <el-table-column prop="contractPeriod" label="合约期" width="100" />
            <el-table-column label="权益" width="80"><template #default="scope">{{ scope.row.offers?.length || 0 }} 项</template></el-table-column>
            <el-table-column label="状态" width="85"><template #default="scope"><el-tag :type="scope.row.enabled ? 'success' : 'info'">{{ scope.row.enabled ? '上架' : '下架' }}</el-tag></template></el-table-column>
            <el-table-column v-if="canEdit" label="操作" min-width="210"><template #default="scope"><el-button link type="primary" @click="openOffers(scope.row)">新增权益</el-button><el-button link @click="openPlan(scope.row)">编辑</el-button><el-button v-if="scope.row.enabled" link type="danger" @click="disablePlan(scope.row)">下架</el-button></template></el-table-column>
          </el-table>
        </div>
        <div v-for="plan in plans.filter(item => item.offers?.length)" :key="plan.id" class="table-card section-gap">
          <div class="table-toolbar"><span class="table-title">{{ plan.planName }} · 权益</span></div>
          <el-table :data="plan.offers"><el-table-column prop="offerType" label="类型" width="130" /><el-table-column prop="offerName" label="名称" min-width="160" /><el-table-column prop="offerValue" label="内容" min-width="260" /><el-table-column label="操作" width="130"><template #default="scope"><el-button v-if="canEdit" link @click="openOffers(plan, scope.row)">编辑</el-button><el-button v-if="canEdit" link type="danger" @click="deleteOffer(scope.row.id)">删除</el-button></template></el-table-column></el-table>
        </div>
      </el-tab-pane>
      <el-tab-pane label="渠道产品政策" name="policies">
        <div class="table-card"><el-table :data="policies" v-loading="loading"><el-table-column prop="channelName" label="渠道" min-width="160" /><el-table-column prop="planName" label="套餐" min-width="180" /><el-table-column label="可推广" width="90"><template #default="scope">{{ scope.row.policy.promotable ? '是' : '否' }}</template></el-table-column><el-table-column prop="policy.effectiveFrom" label="生效日" width="110" /><el-table-column prop="policy.effectiveTo" label="失效日" width="110" /><el-table-column prop="policy.cashbackRuleRef" label="返现规则引用" min-width="140" /><el-table-column prop="policy.commissionRuleRef" label="佣金规则引用" min-width="140" /><el-table-column v-if="canEdit" label="操作" width="130"><template #default="scope"><el-button link @click="openPolicy(scope.row)">编辑</el-button><el-button link type="danger" @click="deletePolicy(scope.row)">删除</el-button></template></el-table-column></el-table></div>
      </el-tab-pane>
    </el-tabs>

    <el-dialog v-model="planVisible" :title="editingPlan ? '编辑套餐' : '新增套餐'" width="800"><el-form label-position="top"><el-row :gutter="16"><el-col :span="12"><el-form-item label="套餐编码"><el-input v-model="planForm.planCode" /></el-form-item></el-col><el-col :span="12"><el-form-item label="套餐名称"><el-input v-model="planForm.planName" /></el-form-item></el-col><el-col :span="8"><el-form-item label="套餐类型"><el-input v-model="planForm.planType" /></el-form-item></el-col><el-col :span="8"><el-form-item label="月费"><el-input-number v-model="planForm.monthlyFee" :precision="2" :min="0" /></el-form-item></el-col><el-col :span="8"><el-form-item label="合约期"><el-input v-model="planForm.contractPeriod" /></el-form-item></el-col><el-col :span="12"><el-form-item label="渠道价格说明"><el-input v-model="planForm.channelPriceText" /></el-form-item></el-col><el-col :span="12"><el-form-item label="流量权益"><el-input v-model="planForm.dataQuota" /></el-form-item></el-col><el-col :span="24"><el-form-item label="描述"><el-input v-model="planForm.description" type="textarea" /></el-form-item></el-col></el-row></el-form><template #footer><el-button @click="planVisible = false">取消</el-button><el-button type="primary" @click="savePlan">保存</el-button></template></el-dialog>
    <el-dialog v-model="offerVisible" :title="editingOffer ? '编辑权益' : `新增权益 · ${selectedPlan?.planName || ''}`" width="620"><el-form label-position="top"><el-form-item label="权益类型"><el-input v-model="offerForm.offerType" /></el-form-item><el-form-item label="权益名称"><el-input v-model="offerForm.offerName" /></el-form-item><el-form-item label="权益内容"><el-input v-model="offerForm.offerValue" type="textarea" /></el-form-item></el-form><template #footer><el-button @click="offerVisible = false">取消</el-button><el-button type="primary" @click="saveOffer">保存</el-button></template></el-dialog>
    <el-dialog v-model="policyVisible" :title="editingPolicy ? '编辑渠道产品政策' : '新增渠道产品政策'" width="680"><el-form label-position="top"><el-row :gutter="16"><el-col :span="12"><el-form-item label="渠道"><el-select v-model="policyForm.channelId" style="width:100%"><el-option v-for="channel in channels" :key="channel.id" :label="channel.channelName" :value="channel.id" /></el-select></el-form-item></el-col><el-col :span="12"><el-form-item label="套餐"><el-select v-model="policyForm.planId" style="width:100%"><el-option v-for="plan in plans" :key="plan.id" :label="plan.planName" :value="plan.id" /></el-select></el-form-item></el-col><el-col :span="12"><el-form-item label="生效日期"><el-date-picker v-model="policyForm.effectiveFrom" type="date" value-format="YYYY-MM-DD" /></el-form-item></el-col><el-col :span="12"><el-form-item label="失效日期"><el-date-picker v-model="policyForm.effectiveTo" type="date" value-format="YYYY-MM-DD" /></el-form-item></el-col><el-col :span="12"><el-form-item label="返现规则引用"><el-input v-model="policyForm.cashbackRuleRef" /></el-form-item></el-col><el-col :span="12"><el-form-item label="佣金规则引用"><el-input v-model="policyForm.commissionRuleRef" /></el-form-item></el-col><el-col :span="12"><el-form-item label="允许推广"><el-switch v-model="policyForm.promotable" :active-value="1" :inactive-value="0" /></el-form-item></el-col></el-row></el-form><template #footer><el-button @click="policyVisible = false">取消</el-button><el-button type="primary" @click="savePolicy">保存</el-button></template></el-dialog>
  </div>
</template>
