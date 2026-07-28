<script setup lang="ts">
import { computed, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { createMobilePlanOrder } from '../api/http'

const route = useRoute()
const router = useRouter()
const customerName = ref('')
const contactPhone = ref('')
const remark = ref('')
const submitting = ref(false)
const errorMessage = ref('')

const planCode = computed(() => String(route.query.planCode || ''))
const planName = computed(() => String(route.query.planName || '移动套餐'))
const monthlyFee = computed(() => Number(route.query.monthlyFee || 0))
const dataQuota = computed(() => String(route.query.dataQuota || ''))
const voiceQuota = computed(() => String(route.query.voiceQuota || ''))
const contractPeriod = computed(() => String(route.query.contractPeriod || ''))

async function confirmApply() {
  if (!contactPhone.value.trim()) {
    errorMessage.value = '请填写联系电话，人工客服会通过这个号码联系客户。'
    return
  }

  submitting.value = true
  errorMessage.value = ''

  try {
    const response = await createMobilePlanOrder({
      planCode: planCode.value,
      customerName: customerName.value,
      contactPhone: contactPhone.value,
      remark: remark.value
    })
    if (response.code === 0) {
      goTransfer(response.data.orderNo)
      return
    }
    errorMessage.value = response.message
  } catch (error) {
    goTransfer(`LOCAL${Date.now()}`)
  } finally {
    submitting.value = false
  }
}

function goTransfer(orderNo: string) {
  router.push({
    name: 'human-transfer',
    params: {
      code: 'MOBILE_PLAN'
    },
    query: {
      orderNo,
      planName: planName.value,
      contactPhone: contactPhone.value
    }
  })
}
</script>

<template>
  <main class="mobile-page with-bottom-action">
    <header class="page-header">
      <button class="back-button" type="button" @click="router.back()">‹</button>
      <div>
        <p class="eyebrow">确认办理</p>
        <h1>{{ planName }}</h1>
      </div>
    </header>

    <section class="flow-steps">
      <span>选套餐</span>
      <span class="active">确认办理</span>
      <span>转人工</span>
    </section>

    <section class="summary-card">
      <div>
        <span>月费</span>
        <strong>${{ monthlyFee }}/月</strong>
      </div>
      <div>
        <span>数据</span>
        <strong>{{ dataQuota }}</strong>
      </div>
      <div>
        <span>通话</span>
        <strong>{{ voiceQuota }}</strong>
      </div>
      <div>
        <span>合约期</span>
        <strong>{{ contractPeriod }}</strong>
      </div>
    </section>

    <section class="form-card">
      <h2>客户联系信息</h2>
      <label class="field-row">
        <span>客户姓名</span>
        <input v-model="customerName" placeholder="可选" />
      </label>
      <label class="field-row">
        <span>联系电话</span>
        <input v-model="contactPhone" placeholder="请输入手机号" inputmode="tel" />
      </label>
      <label class="field-row">
        <span>办理备注</span>
        <textarea v-model="remark" placeholder="可填写办理需求或方便联系时间"></textarea>
      </label>
    </section>

    <div v-if="errorMessage" class="status-box soft">{{ errorMessage }}</div>

    <footer class="bottom-action">
      <button class="primary-button full" type="button" :disabled="submitting" @click="confirmApply">
        {{ submitting ? '正在提交...' : '确认办理并转人工' }}
      </button>
    </footer>
  </main>
</template>

