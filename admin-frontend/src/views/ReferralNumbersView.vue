<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox, type UploadFile } from 'element-plus'
import { api } from '../api/admin'

const chains = ref<any[]>([])
const numbers = ref<any[]>([])
const orders = ref<any[]>([])
const diagnostics = ref<any>({})
const trace = ref<any>()
const selectedChainId = ref<number>()
const loading = ref(false)
const createVisible = ref(false)
const reserveVisible = ref(false)
const candidateVisible = ref(false)
const importVisible = ref(false)
const createForm = reactive({ chainName: '', initialReferralNumber: '', remark: '' })
const reserveForm = reactive<any>({ orderId: null, reason: '' })
const candidateForm = reactive({ referralNumber: '', sourceReference: '' })
const importState = reactive<any>({ file: null, preview: null })
const filters = reactive({ status: '', keyword: '' })
const selectedChain = computed(() => chains.value.find(item => item.chain.id === selectedChainId.value))
const studentOrders = computed(() => orders.value)
const chainStatusLabel: Record<string, string> = { ACTIVE: '启用', PAUSED: '暂停', CLOSED: '关闭' }
const numberStatusLabel: Record<string, string> = { AVAILABLE: '可用龙头', RESERVED: '已占用', USED: '已使用', DISABLED: '候选/停用' }

async function load() {
  loading.value = true
  try {
    const [chainRows, diagnosticRows, orderRows] = await Promise.all([api.referralChains(), api.resourceDiagnostics(), api.eligibleReferralOrders()]) as any
    chains.value = chainRows
    diagnostics.value = diagnosticRows
    orders.value = orderRows
    if (!selectedChainId.value && chains.value.length) selectedChainId.value = chains.value[0].chain.id
    await Promise.all([loadTrace(), loadNumbers()])
  } catch (error: any) {
    ElMessage.error(error.message)
  } finally {
    loading.value = false
  }
}

async function loadNumbers() {
  numbers.value = await api.referralNumbers({ chainId: selectedChainId.value, ...filters }) as any[]
}

async function loadTrace() {
  trace.value = selectedChainId.value
    ? await api.referralChainTrace(selectedChainId.value)
    : undefined
}

async function openChain(item: any) {
  selectedChainId.value = item.chain.id
  loading.value = true
  try {
    await Promise.all([loadTrace(), loadNumbers()])
  } catch (error: any) {
    ElMessage.error(error.message)
  } finally {
    loading.value = false
  }
}

async function createChain() {
  try {
    await api.createReferralChain(createForm)
    createVisible.value = false
    Object.assign(createForm, { chainName: '', initialReferralNumber: '', remark: '' })
    ElMessage.success('接龙已创建，初始号码已设为第一任龙头')
    await load()
  } catch (error: any) { ElMessage.error(error.message) }
}

async function changeChainStatus(status: string) {
  if (!selectedChainId.value) return
  try {
    const result = await ElMessageBox.prompt('请输入状态变更原因', `将接龙设为${chainStatusLabel[status]}`, { inputValidator: value => !!value.trim() || '必须填写原因' })
    await api.changeReferralChainStatus(selectedChainId.value, { status, reason: result.value })
    await load()
  } catch (error: any) { if (error !== 'cancel' && error !== 'close' && error?.message) ElMessage.error(error.message) }
}

async function addCandidate() {
  if (!selectedChainId.value) return
  try {
    await api.addReferralCandidate(selectedChainId.value, candidateForm)
    candidateVisible.value = false
    Object.assign(candidateForm, { referralNumber: '', sourceReference: '' })
    ElMessage.success('候选号码已录入')
    await load()
  } catch (error: any) { ElMessage.error(error.message) }
}

async function designate(row: any) {
  if (!selectedChainId.value) return
  try {
    const result = await ElMessageBox.prompt('指定龙头会停用原可用龙头；已占用龙头不能更换。请输入原因。', '指定龙头', { inputValidator: value => !!value.trim() || '必须填写原因' })
    await api.designateReferralHead(selectedChainId.value, { numberId: row.number.id, reason: result.value })
    ElMessage.success('龙头已更新')
    await load()
  } catch (error: any) { if (error !== 'cancel' && error !== 'close' && error?.message) ElMessage.error(error.message) }
}

async function reserve() {
  if (!selectedChainId.value) return
  try {
    await api.reserveReferral(selectedChainId.value, reserveForm)
    reserveVisible.value = false
    Object.assign(reserveForm, { orderId: null, reason: '' })
    ElMessage.success('当前龙头已分配给学生订单')
    await load()
  } catch (error: any) { ElMessage.error(error.message) }
}

async function action(row: any, type: 'release' | 'complete' | 'disable') {
  const title = type === 'release' ? '释放龙头' : type === 'complete' ? '完成上台并换龙头' : '停用号码'
  try {
    const result = await ElMessageBox.prompt('请输入操作原因', title, { inputValidator: value => !!value.trim() || '必须填写原因' })
    if (type === 'release') await api.releaseReferral(row.number.id, { reason: result.value })
    if (type === 'complete') await api.completeReferral(row.number.id, { reason: result.value })
    if (type === 'disable') await api.disableReferral(row.number.id, { reason: result.value })
    ElMessage.success('操作完成')
    await load()
  } catch (error: any) { if (error !== 'cancel' && error !== 'close' && error?.message) ElMessage.error(error.message) }
}

function onImportFile(file: UploadFile) {
  importState.file = file.raw || null
  importState.preview = null
}

async function previewImport() {
  if (!selectedChainId.value || !importState.file) return ElMessage.warning('请选择接龙和文件')
  const data = new FormData()
  data.append('file', importState.file)
  try { importState.preview = await api.previewReferralImport(selectedChainId.value, data) } catch (error: any) { ElMessage.error(error.message) }
}

async function confirmImport() {
  if (!selectedChainId.value || !importState.file || !importState.preview) return
  const data = new FormData()
  data.append('file', importState.file)
  data.append('fileHash', importState.preview.fileHash)
  try {
    const result: any = await api.confirmReferralImport(selectedChainId.value, data)
    ElMessage.success(`导入完成：成功 ${result.success}，冲突 ${result.conflicts.length}`)
    importVisible.value = false
    importState.preview = null
    await load()
  } catch (error: any) { ElMessage.error(error.message) }
}

function formatDate(value?: string) {
  if (!value) return '-'
  const date = new Date(value)
  if (Number.isNaN(date.getTime())) return value
  const pad = (number: number) => String(number).padStart(2, '0')
  return `${date.getFullYear()}-${pad(date.getMonth() + 1)}-${pad(date.getDate())} ${pad(date.getHours())}:${pad(date.getMinutes())}`
}

onMounted(load)
</script>

<template>
  <div class="page">
    <div class="page-heading"><div><h1>推荐号码接龙</h1><p>每条龙独立流转：龙头被学生订单占用，上台后该订单的上台号码成为新龙头。</p></div><el-button type="primary" @click="createVisible = true">新建接龙</el-button></div>
    <div class="metric-grid resource-metrics"><div class="metric-card"><div class="metric-label">可用真实 ICCID</div><div class="metric-value">{{ diagnostics.availableRealIccids || 0 }}</div></div><div class="metric-card"><div class="metric-label">待替换虚拟 ICCID</div><div class="metric-value">{{ diagnostics.virtualPendingReplacement || 0 }}</div></div><div class="metric-card"><div class="metric-label">中断接龙</div><div class="metric-value">{{ diagnostics.interruptedChains || 0 }}</div></div><div class="metric-card"><div class="metric-label">等待上台</div><div class="metric-value">{{ diagnostics.reservedHeads || 0 }}</div></div></div>
    <section class="chain-overview section-gap" v-loading="loading">
      <button v-for="item in chains" :key="item.chain.id" class="chain-card" :class="{ active: item.chain.id === selectedChainId }" type="button" @click="openChain(item)">
        <span class="chain-card-title">接龙 {{ item.chainNumber }}</span><span class="chain-card-name">{{ item.chain.chainName }}</span><span class="chain-card-meta">{{ chainStatusLabel[item.chain.status] || item.chain.status }} · {{ item.numberCount }} 个号码</span><span class="chain-card-label">当前最新号码</span><strong class="chain-card-number">{{ item.head?.referralNumber || '暂未指定龙头' }}</strong><span class="chain-card-link">查看整条接龙 ↓</span>
      </button>
      <el-empty v-if="!loading && !chains.length" description="暂无接龙，请先新建接龙" />
    </section>
    <section v-if="selectedChain" class="trace-section section-gap">
      <div class="section-title-row"><div><h2>接龙 {{ selectedChain.chainNumber }} · {{ selectedChain.chain.chainName }}</h2><p>从首个号码到当前龙头的完整流转顺序</p></div><div class="trace-head"><span>当前最新号码</span><strong>{{ selectedChain.head?.referralNumber || '未指定' }}</strong></div></div>
      <div v-if="trace?.entries?.length" class="chain-track"><template v-for="(entry, index) in trace.entries" :key="entry.number.id"><article class="chain-node" :class="{ current: entry.isCurrentHead }"><div class="chain-node-top"><span>第 {{ entry.sequence }} 个</span><el-tag size="small" :type="entry.isCurrentHead ? 'success' : 'info'">{{ numberStatusLabel[entry.number.status] || entry.number.status }}</el-tag></div><strong>{{ entry.number.referralNumber }}</strong><span v-if="entry.orderNo">订单：{{ entry.orderNo }}</span><span v-if="entry.customerName">客户：{{ entry.customerName }}</span><span v-if="entry.number.usedAt">流转：{{ formatDate(entry.number.usedAt) }}</span><span v-if="entry.isCurrentHead" class="current-mark">当前龙头</span></article><div v-if="index < trace.entries.length - 1" class="chain-arrow">→</div></template></div>
      <el-empty v-else description="该接龙尚未录入号码" />
    </section>
    <div class="filter-bar section-gap"><el-select v-model="selectedChainId" placeholder="选择接龙" filterable @change="() => { loadTrace(); loadNumbers() }"><el-option v-for="item in chains" :key="item.chain.id" :label="`接龙${item.chainNumber} · ${item.chain.chainName}`" :value="item.chain.id" /></el-select><el-select v-model="filters.status" placeholder="号码状态" clearable @change="loadNumbers"><el-option v-for="(label, value) in numberStatusLabel" :key="value" :label="label" :value="value" /></el-select><el-input v-model="filters.keyword" placeholder="推荐号码" clearable /><el-button @click="loadNumbers">查询</el-button><div class="filter-actions"><el-button :disabled="!selectedChainId" @click="candidateVisible = true">新增候选</el-button><el-button :disabled="!selectedChainId" @click="importVisible = true">预览导入</el-button><el-button type="primary" :disabled="!selectedChainId || selectedChain?.chain.status !== 'ACTIVE'" @click="reserveVisible = true">分配当前龙头</el-button></div></div>
    <div v-if="selectedChain" class="hint-box">当前接龙：{{ selectedChain.chain.chainName }}；龙头：{{ selectedChain.head?.referralNumber || '未指定（接龙中断）' }}。<el-button link type="primary" @click="changeChainStatus('ACTIVE')">启用</el-button><el-button link @click="changeChainStatus('PAUSED')">暂停</el-button><el-button link type="danger" @click="changeChainStatus('CLOSED')">关闭</el-button></div>
    <div class="table-card section-gap"><div class="table-toolbar"><span class="table-title">号码流转</span><span class="table-meta">{{ numbers.length }} 条</span></div><div class="table-scroll"><el-table class="desktop-data-table referral-table" :data="numbers" v-loading="loading"><el-table-column prop="number.referralNumber" label="推荐号码" min-width="135" /><el-table-column label="状态" width="110"><template #default="scope">{{ numberStatusLabel[scope.row.number.status] || scope.row.number.status }}</template></el-table-column><el-table-column prop="number.sourceType" label="来源" width="130" /><el-table-column prop="orderNo" label="占用订单" min-width="150" /><el-table-column prop="customerName" label="客户" min-width="100" /><el-table-column prop="number.reservedAt" label="占用时间" min-width="170" /><el-table-column prop="number.usedAt" label="使用时间" min-width="170" /><el-table-column label="操作" width="280"><template #default="scope"><div class="ops"><el-button v-if="scope.row.number.status === 'DISABLED'" link type="primary" @click="designate(scope.row)">指定龙头</el-button><el-button v-if="scope.row.number.status === 'AVAILABLE'" link type="danger" @click="action(scope.row, 'disable')">停用</el-button><el-button v-if="scope.row.number.status === 'RESERVED'" link @click="action(scope.row, 'release')">释放</el-button><el-button v-if="scope.row.number.status === 'RESERVED'" link type="primary" @click="action(scope.row, 'complete')">上台换头</el-button></div></template></el-table-column></el-table></div></div>
    <el-dialog v-model="createVisible" title="新建接龙" width="500"><el-form label-position="top"><el-form-item label="接龙名称"><el-input v-model="createForm.chainName" placeholder="例如：香港学生卡接龙" /></el-form-item><el-form-item label="初始推荐号码"><el-input v-model="createForm.initialReferralNumber" placeholder="创建后立即成为第一任龙头" /></el-form-item><el-form-item label="备注"><el-input v-model="createForm.remark" type="textarea" /></el-form-item></el-form><div class="hint-box">系统会自动生成内部接龙编号；初始推荐号码会直接设为该条接龙的当前龙头。</div><template #footer><el-button @click="createVisible = false">取消</el-button><el-button type="primary" @click="createChain">创建</el-button></template></el-dialog>
    <el-dialog v-model="candidateVisible" title="新增候选号码" width="500"><el-form label-position="top"><el-form-item label="推荐号码"><el-input v-model="candidateForm.referralNumber" /></el-form-item><el-form-item label="来源标识"><el-input v-model="candidateForm.sourceReference" /></el-form-item></el-form><template #footer><el-button @click="candidateVisible = false">取消</el-button><el-button type="primary" @click="addCandidate">保存</el-button></template></el-dialog>
    <el-dialog v-model="reserveVisible" title="分配当前龙头" width="560"><el-form label-position="top"><el-form-item label="学生订单"><el-select v-model="reserveForm.orderId" filterable style="width:100%"><el-option v-for="order in studentOrders" :key="order.id" :label="`${order.orderNo} · ${order.customerName || '未命名'} · ${order.planName}`" :value="order.id" /></el-select></el-form-item><el-form-item label="原因"><el-input v-model="reserveForm.reason" /></el-form-item></el-form><template #footer><el-button @click="reserveVisible = false">取消</el-button><el-button type="primary" @click="reserve">确认分配</el-button></template></el-dialog>
    <el-dialog v-model="importVisible" title="推荐号码导入预览" width="720"><div class="hint-box">支持 ICCID 导入相同的表格格式。导入后号码先处于候选/停用状态，必须人工指定龙头，不会自动串接历史。</div><el-upload drag :auto-upload="false" :limit="1" accept=".xls,.xlsx,.xlsm,.csv" :on-change="onImportFile"><div>拖放文件到这里，或点击选择</div></el-upload><div v-if="importState.preview" class="section-gap">共 {{ importState.preview.total }} 行，可导入 {{ importState.preview.valid }} 行，冲突 {{ importState.preview.conflict }} 行。<el-table :data="importState.preview.rows" max-height="260"><el-table-column prop="rowNumber" label="行" width="70" /><el-table-column prop="referralNumber" label="号码" /><el-table-column prop="error" label="异常" /></el-table></div><template #footer><el-button @click="importVisible = false">取消</el-button><el-button @click="previewImport">生成预览</el-button><el-button type="primary" :disabled="!importState.preview" @click="confirmImport">确认导入</el-button></template></el-dialog>
  </div>
</template>

<style scoped>
.resource-metrics{grid-template-columns:repeat(4,minmax(0,1fr))}.resource-metrics .metric-card{min-height:100px;padding:18px}.resource-metrics .metric-value{margin-top:10px;font-size:25px}.chain-overview{display:grid;grid-template-columns:repeat(auto-fit,minmax(245px,1fr));gap:16px}.chain-card{display:flex;min-height:185px;padding:20px;border:1px solid var(--el-border-color-light);border-radius:12px;background:#fff;text-align:left;cursor:pointer;flex-direction:column;align-items:flex-start;transition:.2s}.chain-card:hover,.chain-card.active{border-color:var(--el-color-primary);box-shadow:0 8px 24px rgba(42,113,196,.13)}.chain-card-title{font-size:18px;font-weight:700;color:#1f2937}.chain-card-name{margin-top:5px;color:#606266}.chain-card-meta{margin-top:10px;font-size:13px;color:#909399}.chain-card-label{margin-top:auto;padding-top:18px;font-size:13px;color:#909399}.chain-card-number{margin-top:4px;font-size:20px;color:#1f2937;word-break:break-all}.chain-card-link{margin-top:12px;color:var(--el-color-primary);font-size:13px}.trace-section{padding:20px;border:1px solid var(--el-border-color-light);border-radius:12px;background:#fff}.section-title-row{display:flex;justify-content:space-between;align-items:flex-start;gap:20px}.section-title-row h2{margin:0;font-size:19px}.section-title-row p{margin:6px 0 0;color:#909399;font-size:13px}.trace-head{display:flex;flex-direction:column;align-items:flex-end;gap:5px;color:#909399;font-size:13px}.trace-head strong{font-size:20px;color:var(--el-color-primary);word-break:break-all}.chain-track{display:flex;align-items:center;overflow-x:auto;padding:20px 2px 8px}.chain-node{display:flex;flex:0 0 210px;min-height:152px;padding:16px;border:1px solid #dcdfe6;border-radius:10px;background:#fff;flex-direction:column;gap:7px}.chain-node.current{border:2px solid var(--el-color-success);background:#f0f9eb}.chain-node-top{display:flex;justify-content:space-between;align-items:center;font-size:12px;color:#909399}.chain-node strong{font-size:18px;color:#303133;word-break:break-all}.chain-node>span{font-size:13px;color:#606266}.chain-node .current-mark{color:var(--el-color-success);font-weight:600}.chain-arrow{padding:0 12px;font-size:28px;color:var(--el-color-primary);flex:0 0 auto}.referral-table{min-width:1200px}@media(max-width:1280px){.resource-metrics{grid-template-columns:repeat(2,minmax(0,1fr))}}@media(max-width:640px){.resource-metrics,.chain-overview{grid-template-columns:1fr}.trace-head{display:none}.chain-node{flex-basis:190px}}
</style>
