<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { session } from '../api/http'
import { api } from '../api/admin'

const rules = ref<any[]>([])
const plans = ref<any[]>([])
const mobilePlans = ref<any[]>([])
const installments = ref<any[]>([])
const ruleVisible = ref(false)
const installmentVisible = ref(false)
const editingRule = ref<any>()
const selectedCashbackPlan = ref<any>()
const generatingOrderId = ref<number | null>(null)
const loading = ref(false)
const isAdmin = session()?.role === 'ADMIN'
const ruleForm = reactive<any>(blankRule())

function blankRule() {
  return {
    ruleName: '',
    planId: null,
    contractMonths: 12,
    installmentAmount: 0,
    effectiveFrom: null,
    effectiveTo: null,
    enabled: 1
  }
}

function planLabel(planId: number) {
  const plan = mobilePlans.value.find(item => item.id === planId)
  return plan ? `${plan.planCode} · ${plan.planName}` : `套餐 ${planId}`
}

function amount(value: any) {
  return value == null ? '无金额权限' : `HK$${Number(value).toFixed(2)}`
}

async function load() {
  loading.value = true
  try {
    [rules.value, plans.value, mobilePlans.value] = await Promise.all([
      api.cashbackRules(),
      api.cashbackPlans(),
      api.plans()
    ]) as any
  } catch (error: any) {
    ElMessage.error(error.message)
  } finally {
    loading.value = false
  }
}

function openRule(row?: any) {
  editingRule.value = row
  Object.assign(ruleForm, blankRule(), row || {})
  ruleVisible.value = true
}

async function saveRule() {
  try {
    if (editingRule.value) {
      await api.updateCashbackRule(editingRule.value.id, ruleForm)
    } else {
      await api.createCashbackRule(ruleForm)
    }
    ElMessage.success('返现规则已保存')
    ruleVisible.value = false
    await load()
  } catch (error: any) {
    ElMessage.error(error.message)
  }
}

async function generatePlan() {
  if (!generatingOrderId.value) {
    ElMessage.warning('请输入订单 ID')
    return
  }
  try {
    await api.generateCashbackPlan(generatingOrderId.value)
    ElMessage.success('返现计划已生成')
    generatingOrderId.value = null
    await load()
  } catch (error: any) {
    ElMessage.error(error.message)
  }
}

async function generateExistingPlans() {
  try {
    await ElMessageBox.confirm('将按现有订单的套餐ID或可唯一匹配的套餐快照生成返现计划；未激活订单只生成待激活计划，不生成返现期次。', '按现有订单生成', { type: 'warning' })
    const result: any = await api.generateExistingCashbackPlans()
    ElMessage.success(`已生成 ${result.generated} 条，已存在 ${result.existing} 条，未匹配 ${result.unmatched} 条`)
    await load()
  } catch (error: any) {
    if (error !== 'cancel' && error !== 'close') {
      ElMessage.error(error.message || '批量生成返现计划失败')
    }
  }
}

async function openInstallments(row: any) {
  try {
    selectedCashbackPlan.value = row.cashbackPlan
    installments.value = await api.cashbackInstallments(row.cashbackPlan.id) as any[]
    installmentVisible.value = true
  } catch (error: any) {
    ElMessage.error(error.message)
  }
}

async function confirmInstallment(row: any) {
  try {
    const prompt = await ElMessageBox.prompt('请填写人工确认说明；这不会触发自动付款。', '确认返现期次', {
      inputPattern: /\S+/,
      inputErrorMessage: '确认说明不能为空'
    })
    await api.confirmCashbackInstallment(row.id, { remark: prompt.value })
    ElMessage.success('返现期次已确认')
    installments.value = await api.cashbackInstallments(selectedCashbackPlan.value.id) as any[]
    await load()
  } catch (error: any) {
    if (error !== 'cancel' && error !== 'close') {
      ElMessage.error(error.message || '确认返现期次失败')
    }
  }
}

onMounted(load)
</script>

<template>
  <div class="page">
    <div class="page-heading">
      <div>
        <h1>客户返现</h1>
        <p>实际激活日起满一个月生成首期；返现易到账与提现文件暂未接入。</p>
      </div>
      <div v-if="isAdmin" class="actions">
        <el-button @click="generateExistingPlans">按现有订单生成</el-button>
        <el-input-number v-model="generatingOrderId" :min="1" :precision="0" placeholder="订单ID" controls-position="right" />
        <el-button type="primary" @click="generatePlan">为历史订单生成计划</el-button>
      </div>
    </div>

    <el-tabs>
      <el-tab-pane label="返现计划">
        <div class="table-card">
          <el-table :data="plans" v-loading="loading">
            <el-table-column prop="cashbackPlan.planNo" label="计划编号" width="130" />
            <el-table-column label="客户" min-width="110"><template #default="scope">{{ scope.row.customer?.name || '—' }}</template></el-table-column>
            <el-table-column label="订单" min-width="150"><template #default="scope">{{ scope.row.order?.orderNo || scope.row.cashbackPlan.orderId }}</template></el-table-column>
            <el-table-column label="套餐" min-width="180"><template #default="scope">{{ scope.row.order?.planName || '—' }}</template></el-table-column>
            <el-table-column prop="cashbackPlan.activatedAt" label="实际激活时间" width="170" />
            <el-table-column label="计划总额" width="120"><template #default="scope">{{ amount(scope.row.cashbackPlan.totalAmount) }}</template></el-table-column>
            <el-table-column prop="cashbackPlan.installmentCount" label="期数" width="80" />
            <el-table-column label="状态" width="110"><template #default="scope">{{ scope.row.cashbackPlan.status === 'PENDING_ACTIVATION' ? '待激活' : scope.row.cashbackPlan.status === 'ACTIVE' ? '返现中' : scope.row.cashbackPlan.status === 'COMPLETED' ? '已完成' : scope.row.cashbackPlan.status }}</template></el-table-column>
            <el-table-column label="操作" width="100"><template #default="scope"><el-button link type="primary" @click="openInstallments(scope.row)">查看期次</el-button></template></el-table-column>
          </el-table>
        </div>
      </el-tab-pane>
      <el-tab-pane label="返现规则">
        <div class="table-card">
          <div class="table-toolbar"><span class="table-title">套餐返现规则</span><el-button v-if="isAdmin" type="primary" @click="openRule()">新增规则</el-button></div>
          <el-table :data="rules" v-loading="loading">
            <el-table-column prop="ruleName" label="规则名称" min-width="160" />
            <el-table-column label="套餐" min-width="210"><template #default="scope">{{ planLabel(scope.row.planId) }}</template></el-table-column>
            <el-table-column prop="contractMonths" label="合约期（月）" width="110" />
            <el-table-column label="每期返现" width="120"><template #default="scope">{{ amount(scope.row.installmentAmount) }}</template></el-table-column>
            <el-table-column prop="effectiveFrom" label="生效日" width="110" />
            <el-table-column prop="effectiveTo" label="失效日" width="110" />
            <el-table-column label="状态" width="90"><template #default="scope">{{ scope.row.enabled ? '启用' : '停用' }}</template></el-table-column>
            <el-table-column v-if="isAdmin" label="操作" width="80"><template #default="scope"><el-button link type="primary" @click="openRule(scope.row)">编辑</el-button></template></el-table-column>
          </el-table>
        </div>
      </el-tab-pane>
    </el-tabs>

    <el-dialog v-model="ruleVisible" :title="editingRule ? '编辑返现规则' : '新增返现规则'" width="680">
      <el-form label-position="top">
        <el-row :gutter="16">
          <el-col :span="12"><el-form-item label="规则名称"><el-input v-model="ruleForm.ruleName" placeholder="例如 学生Slash 30GB 12个月返现" /></el-form-item></el-col>
          <el-col :span="12"><el-form-item label="套餐"><el-select v-model="ruleForm.planId" filterable style="width:100%"><el-option v-for="plan in mobilePlans.filter(item => item.enabled === 1)" :key="plan.id" :label="`${plan.planCode} · ${plan.planName}`" :value="plan.id" /></el-select></el-form-item></el-col>
          <el-col :span="8"><el-form-item label="合约期（月）"><el-input-number v-model="ruleForm.contractMonths" :min="1" :precision="0" style="width:100%" /></el-form-item></el-col>
          <el-col :span="8"><el-form-item label="每期返现"><el-input-number v-model="ruleForm.installmentAmount" :min="0.01" :precision="2" style="width:100%" /></el-form-item></el-col>
          <el-col :span="8"><el-form-item label="是否启用"><el-switch v-model="ruleForm.enabled" :active-value="1" :inactive-value="0" /></el-form-item></el-col>
          <el-col :span="12"><el-form-item label="生效日期"><el-date-picker v-model="ruleForm.effectiveFrom" type="date" value-format="YYYY-MM-DD" style="width:100%" /></el-form-item></el-col>
          <el-col :span="12"><el-form-item label="失效日期"><el-date-picker v-model="ruleForm.effectiveTo" type="date" value-format="YYYY-MM-DD" style="width:100%" /></el-form-item></el-col>
        </el-row>
      </el-form>
      <template #footer><el-button @click="ruleVisible = false">取消</el-button><el-button type="primary" @click="saveRule">保存</el-button></template>
    </el-dialog>

    <el-drawer v-model="installmentVisible" :title="`${selectedCashbackPlan?.planNo || ''} 返现期次`" size="720px">
      <p class="hint">首期计划日为实际激活日满一个月；人工确认仅记录内部确认，不自动付款。</p>
      <el-table :data="installments">
        <el-table-column prop="installmentNo" label="期次" width="80" />
        <el-table-column prop="plannedDate" label="计划日期" width="120" />
        <el-table-column label="计划金额" width="120"><template #default="scope">{{ amount(scope.row.plannedAmount) }}</template></el-table-column>
        <el-table-column prop="status" label="状态" width="100" />
        <el-table-column prop="confirmedAt" label="确认时间" width="170" />
        <el-table-column prop="confirmationRemark" label="确认说明" min-width="180" />
        <el-table-column v-if="isAdmin" label="操作" width="90"><template #default="scope"><el-button v-if="scope.row.status === 'PENDING'" link type="primary" @click="confirmInstallment(scope.row)">确认</el-button></template></el-table-column>
      </el-table>
    </el-drawer>
  </div>
</template>

<style scoped>
.actions { display: flex; gap: 10px; align-items: center; }
.hint { margin: 0 0 16px; color: #777; }
</style>
