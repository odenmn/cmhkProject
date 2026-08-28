<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { api } from '../api/admin'
import { session } from '../api/http'
import { useRoute } from 'vue-router'

const rows = ref<any[]>([])
const owners = ref<any[]>([])
const detail = ref<any>()
const loading = ref(false)
const detailVisible = ref(false)
const route = useRoute()
const filters = reactive({ taskStatus: String(route.query.taskStatus || ''), taskType: String(route.query.taskType || ''), keyword: '' })
const statusLabels: Record<string, string> = { PENDING: '待处理', PROCESSING: '处理中', DONE: '已完成', CLOSED: '已关闭' }
const typeLabels: Record<string, string> = { CUSTOMER_FOLLOW_UP: '客户跟进', SUPPLEMENT: '补件', REVIEW_EXCEPTION: '审核异常', ACTIVATION_EXCEPTION: '激活异常', RESOURCE_SHORTAGE: '资源不足', RECONCILIATION_MATCH_EXCEPTION: '对账匹配异常', CASHBACK_EXCEPTION: '返现异常', AFTER_SALES: '售后' }
const current = session()
const ownerHint = computed(() => owners.value.map(item => `${item.id} · ${item.displayName || item.username}`).join('；'))

async function load() {
  loading.value = true
  try {
    rows.value = await api.tasks(filters) as any[]
  } catch (error: any) {
    ElMessage.error(error.message)
  } finally {
    loading.value = false
  }
}

async function open(row: any) {
  try {
    detail.value = await api.task(row.task.id)
    detailVisible.value = true
  } catch (error: any) {
    ElMessage.error(error.message)
  }
}

async function claim(row: any) {
  try {
    await api.claimTask(row.task.id, { reason: '运营人员领取任务' })
    ElMessage.success('任务已领取')
    await load()
  } catch (error: any) {
    ElMessage.error(error.message)
  }
}

async function changeTask(row: any, action: 'process' | 'complete' | 'close') {
  const title = action === 'process' ? '记录处理进展' : action === 'complete' ? '完成任务' : '关闭任务'
  try {
    const result = await ElMessageBox.prompt('请填写处理说明', title, { inputValidator: value => !!value.trim() || '必须填写处理说明' })
    if (action === 'process') await api.processTask(row.task.id, { reason: result.value })
    if (action === 'complete') await api.completeTask(row.task.id, { reason: result.value })
    if (action === 'close') await api.closeTask(row.task.id, { reason: result.value })
    ElMessage.success('任务已更新')
    await load()
    if (detail.value?.task?.id === row.task.id) await open(row)
  } catch (error: any) {
    if (error !== 'cancel' && error !== 'close' && error?.message) ElMessage.error(error.message)
  }
}

async function reassign(row: any) {
  try {
    const result = await ElMessageBox.prompt(`可转派人员：${ownerHint.value || '加载中'}。请输入接收人的用户 ID`, '转派任务', { inputValidator: value => /^\d+$/.test(value) || '请输入有效用户 ID' })
    await api.reassignTask(row.task.id, { assigneeUserId: Number(result.value), reason: '管理员转派任务' })
    ElMessage.success('任务已转派')
    await load()
  } catch (error: any) {
    if (error !== 'cancel' && error !== 'close' && error?.message) ElMessage.error(error.message)
  }
}

async function refreshResources() {
  try {
    const result: any = await api.refreshResourceTasks()
    ElMessage.success(`资源巡检完成，新建 ${result.created} 个任务`)
    await load()
  } catch (error: any) {
    ElMessage.error(error.message)
  }
}

onMounted(async () => {
  owners.value = await api.customerOwners() as any[]
  await load()
})
</script>

<template>
  <div class="page">
    <div class="page-heading"><div><h1>任务中心</h1><p>任务只记录内部跟进过程，不会自动修改 UMALL、订单或资源事实状态。</p></div><el-button type="primary" @click="refreshResources">资源巡检</el-button></div>
    <div class="filter-bar"><el-select v-model="filters.taskStatus" placeholder="任务状态" clearable><el-option v-for="(label,value) in statusLabels" :key="value" :label="label" :value="value" /></el-select><el-select v-model="filters.taskType" placeholder="任务类型" clearable><el-option v-for="(label,value) in typeLabels" :key="value" :label="label" :value="value" /></el-select><el-input v-model="filters.keyword" placeholder="任务编号或标题" clearable /><el-button @click="load">查询</el-button></div>
    <div class="table-card"><div class="table-toolbar"><span class="table-title">运营任务</span><span class="table-meta">{{ rows.length }} 条</span></div><div class="table-scroll"><el-table class="desktop-data-table task-table" :data="rows" v-loading="loading"><el-table-column prop="task.taskNo" label="任务编号" width="130" /><el-table-column prop="task.title" label="任务标题" min-width="170" /><el-table-column label="类型" width="140"><template #default="scope">{{ typeLabels[scope.row.task.taskType] || scope.row.task.taskType }}</template></el-table-column><el-table-column label="状态" width="105"><template #default="scope"><el-tag :type="scope.row.task.taskStatus === 'DONE' ? 'success' : scope.row.task.taskStatus === 'CLOSED' ? 'info' : scope.row.task.taskStatus === 'PROCESSING' ? 'warning' : 'danger'">{{ statusLabels[scope.row.task.taskStatus] || scope.row.task.taskStatus }}</el-tag></template></el-table-column><el-table-column prop="customer.name" label="客户" min-width="100" /><el-table-column prop="order.orderNo" label="订单" min-width="145" /><el-table-column prop="task.assigneeName" label="负责人" width="110" /><el-table-column prop="task.createdAt" label="创建时间" min-width="165" /><el-table-column label="操作" fixed="right" width="250"><template #default="scope"><el-button link type="primary" @click="open(scope.row)">详情</el-button><el-button v-if="scope.row.task.taskStatus === 'PENDING'" link type="primary" @click="claim(scope.row)">领取</el-button><el-button v-if="scope.row.task.taskStatus === 'PROCESSING'" link @click="changeTask(scope.row,'process')">记录</el-button><el-button v-if="scope.row.task.taskStatus === 'PROCESSING'" link type="success" @click="changeTask(scope.row,'complete')">完成</el-button><el-button v-if="current?.role === 'ADMIN' && ['PENDING','PROCESSING'].includes(scope.row.task.taskStatus)" link type="danger" @click="changeTask(scope.row,'close')">关闭</el-button><el-button v-if="current?.role === 'ADMIN' && ['PENDING','PROCESSING'].includes(scope.row.task.taskStatus)" link @click="reassign(scope.row)">转派</el-button></template></el-table-column></el-table></div></div>
    <el-drawer v-model="detailVisible" title="任务详情" size="680px"><template v-if="detail"><div class="task-detail"><el-descriptions :column="2" border><el-descriptions-item label="任务编号">{{ detail.task.taskNo }}</el-descriptions-item><el-descriptions-item label="状态">{{ statusLabels[detail.task.taskStatus] || detail.task.taskStatus }}</el-descriptions-item><el-descriptions-item label="标题" :span="2">{{ detail.task.title }}</el-descriptions-item><el-descriptions-item label="关联客户">{{ detail.customer?.name || '-' }}</el-descriptions-item><el-descriptions-item label="关联订单">{{ detail.order?.orderNo || '-' }}</el-descriptions-item><el-descriptions-item label="处理说明" :span="2">{{ detail.task.handlingResult || '-' }}</el-descriptions-item></el-descriptions><div class="section-gap"><h3>处理历史</h3><el-timeline><el-timeline-item v-for="item in detail.histories" :key="item.id" :timestamp="item.createdAt"><b>{{ item.actionType }}</b> · {{ item.operatorName }}<div>{{ item.remark || '-' }}</div></el-timeline-item></el-timeline></div></div></template></el-drawer>
  </div>
</template>

<style scoped>
.task-table{min-width:1250px}.task-detail h3{font-size:16px}@media(max-width:1280px){.task-table{min-width:1120px}}
</style>
