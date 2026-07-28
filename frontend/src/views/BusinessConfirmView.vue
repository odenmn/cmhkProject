<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { createMobilePlanOrder, fetchMobilePlan, type MobilePlan } from '../api/http'

const route = useRoute()
const router = useRouter()
const plan = ref<MobilePlan | null>(null)
const loading = ref(true)
const customerName = ref('')
const contactPhone = ref('')
const customerIdentity = ref(0)
const hasOffer = ref(0)
const hasPassOrHkid = ref(0)
const expectedStartDate = ref('')
const idType = ref('HKID')
const idNo = ref('')
const referrerPhone = ref('')
const preferredContactTime = ref('')
const remark = ref('')
const submitting = ref(false)
const errorMessage = ref('')

const planCode = computed(() => String(route.query.planCode || ''))
const planName = computed(() => plan.value?.planName || '移动套餐')
const planType = computed(() => plan.value?.planType || '移动套餐')
const channelPriceText = computed(() => plan.value?.channelPriceText || `HK$${plan.value?.monthlyFee || 0}/月`)
const effectivePriceText = computed(() => plan.value?.effectivePriceText || '')
const dataQuota = computed(() => plan.value?.dataQuota || '')
const voiceQuota = computed(() => plan.value?.voiceQuota || '')
const roamingBenefit = computed(() => plan.value?.roamingBenefit || '')
const contractPeriod = computed(() => plan.value?.contractPeriod || '')
const promotionEndDate = computed(() => plan.value?.promotionEndDate || '')
const discountFormula = computed(() => plan.value?.discountFormula || '')

onMounted(async () => {
  if (!planCode.value) {
    errorMessage.value = '缺少套餐编码，请返回重新选择套餐。'
    loading.value = false
    return
  }

  try {
    const response = await fetchMobilePlan(planCode.value)
    if (response.code !== 0 || !response.data) {
      errorMessage.value = response.message || '套餐不存在或已下架，请返回重新选择套餐。'
      return
    }

    plan.value = response.data
    customerIdentity.value = response.data.planType?.includes('学生') ? 1 : 0
  } catch (error) {
    errorMessage.value = '套餐信息加载失败，请确认后端服务已启动后再重试。'
  } finally {
    loading.value = false
  }
})

async function confirmApply() {
  if (!plan.value) {
    errorMessage.value = '套餐信息未加载成功，请返回重新选择套餐。'
    return
  }

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
      customerIdentity: customerIdentity.value,
      hasOffer: customerIdentity.value === 1 ? hasOffer.value : 0,
      hasPassOrHkid: hasPassOrHkid.value,
      expectedStartDate: expectedStartDate.value || undefined,
      idType: idType.value,
      idNo: idNo.value,
      referrerPhone: referrerPhone.value,
      preferredContactTime: preferredContactTime.value,
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
      orderNo
    }
  })
}
</script>

<template>
  <main class="mobile-page with-bottom-action">
    <header class="page-header">
      <button class="back-button" type="button" @click="router.back()">‹</button>
      <div>
        <p class="eyebrow">确认办理 · {{ planType }}</p>
        <h1>{{ planName }}</h1>
      </div>
    </header>

    <section class="flow-steps">
      <span>选套餐</span>
      <span class="active">确认办理</span>
      <span>转人工</span>
    </section>

    <div v-if="loading" class="status-box">正在加载套餐信息...</div>

    <section v-if="plan" class="summary-card">
      <div>
        <span>展示价</span>
        <strong>{{ channelPriceText }}</strong>
      </div>
      <div>
        <span>折实月费</span>
        <strong>{{ effectivePriceText || channelPriceText }}</strong>
      </div>
      <div>
        <span>数据</span>
        <strong>{{ dataQuota }}</strong>
      </div>
      <div>
        <span>通话</span>
        <strong>{{ voiceQuota }}</strong>
      </div>
      <div v-if="roamingBenefit">
        <span>漫游/额外权益</span>
        <strong>{{ roamingBenefit }}</strong>
      </div>
      <div>
        <span>合约期</span>
        <strong>{{ contractPeriod }}</strong>
      </div>
      <div v-if="promotionEndDate">
        <span>优惠截止</span>
        <strong>{{ promotionEndDate }}</strong>
      </div>
    </section>

    <section v-if="plan && discountFormula" class="notice-card">
      <h2>折算参考</h2>
      <p>{{ discountFormula }}</p>
    </section>

    <section v-if="plan" class="form-card">
      <h2>客户办理信息</h2>
      <label class="field-row">
        <span>客户身份</span>
        <select v-model.number="customerIdentity">
          <option :value="0">自营客户</option>
          <option :value="1">留学生</option>
        </select>
      </label>
      <label class="field-row">
        <span>客户姓名</span>
        <input v-model="customerName" placeholder="可选" />
      </label>
      <label class="field-row">
        <span>联系电话</span>
        <input v-model="contactPhone" placeholder="请输入手机号" inputmode="tel" />
      </label>
      <label v-if="customerIdentity === 1" class="field-row">
        <span>目前是否有 offer？</span>
        <select v-model.number="hasOffer">
          <option :value="0">没有</option>
          <option :value="1">有</option>
        </select>
      </label>
      <label class="field-row">
        <span>目前是否有通行证 / HKID？</span>
        <select v-model.number="hasPassOrHkid">
          <option :value="0">没有</option>
          <option :value="1">有</option>
        </select>
      </label>
      <label class="field-row">
        <span>预计什么时候开始使用？</span>
        <input v-model="expectedStartDate" type="date" />
      </label>
      <label class="field-row">
        <span>证件类型</span>
        <select v-model="idType">
          <option value="HKID">香港身份证</option>
          <option value="PASSPORT">护照</option>
          <option value="MAINLAND_ID">内地身份证</option>
          <option value="OTHER">其他证件</option>
        </select>
      </label>
      <label class="field-row">
        <span>证件号码</span>
        <input v-model="idNo" placeholder="可选，人工办理时用于核对" />
      </label>
      <label class="field-row">
        <span>推荐人号码</span>
        <input v-model="referrerPhone" placeholder="可选" inputmode="tel" />
      </label>
      <label class="field-row">
        <span>方便联系时间</span>
        <input v-model="preferredContactTime" placeholder="例如：今天 18:00 后" />
      </label>
      <label class="field-row">
        <span>办理备注</span>
        <textarea v-model="remark" placeholder="可填写其他办理需求"></textarea>
      </label>
    </section>

    <div v-if="errorMessage" class="status-box soft">{{ errorMessage }}</div>

    <footer class="bottom-action">
      <button class="primary-button full" type="button" :disabled="submitting || loading || !plan" @click="confirmApply">
        {{ submitting ? '正在提交...' : '确认办理并转人工' }}
      </button>
    </footer>
  </main>
</template>
