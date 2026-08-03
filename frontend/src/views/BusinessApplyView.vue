<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { type MobilePlan, fetchMobilePlans } from '../api/http'

const router = useRouter()
const loading = ref(true)
const errorMessage = ref('')
const plans = ref<MobilePlan[]>([])

const fallbackPlans: MobilePlan[] = [
  {
    id: 1,
    planCode: 'STUDENT_SLASH_30GB_24M',
    planName: '学生 Slash 30GB',
    planType: '学生套餐',
    monthlyFee: 98,
    channelPriceText: 'HK$98/月',
    effectiveMonthlyFee: 62,
    effectivePriceText: '约HK$62/月',
    officialMonthlyFee: 98,
    officialPriceText: 'HK$98/月',
    dataQuota: '30GB',
    voiceQuota: '香港本地无限通话',
    roamingBenefit: '赠3GB',
    contractPeriod: '24个月',
    promotionEndDate: '2026-07-31',
    sourceVersion: '202607',
    discountFormula: '(HK$98 x 24个月 - HK$600话费券 - HK$260渠道补贴) / 24个月',
    description: '留学生上台优惠，24 个月折实月费更低。',
    sortOrder: 30,
    enabled: 1,
    offers: [
      { id: 1, planCode: 'STUDENT_SLASH_30GB_24M', offerType: 'POINTS', offerName: '积分合计', offerValue: '共60,000分，可抵HK$600话费券', sortOrder: 10, enabled: 1 },
      { id: 2, planCode: 'STUDENT_SLASH_30GB_24M', offerType: 'SUBSIDY', offerName: '渠道额外补贴', offerValue: 'HK$260', sortOrder: 20, enabled: 1 },
      { id: 3, planCode: 'STUDENT_SLASH_30GB_24M', offerType: 'SOCIAL_DATA', offerName: '社交娱乐数据', offerValue: 'WhatsApp、WeChat、YouTube、Netflix 等', sortOrder: 30, enabled: 1 }
    ]
  },
  {
    id: 2,
    planCode: 'STUDENT_SLASH_50GB_24M',
    planName: '学生 Slash 50GB',
    planType: '学生套餐',
    monthlyFee: 138,
    channelPriceText: 'HK$138/月',
    effectiveMonthlyFee: 102,
    effectivePriceText: '约HK$102/月',
    officialMonthlyFee: 138,
    officialPriceText: 'HK$138/月',
    dataQuota: '50GB + 限时额外50GB，最高100GB 香港本地数据',
    voiceQuota: '香港本地无限通话',
    roamingBenefit: '最高6GB 中国内地及澳门数据',
    contractPeriod: '24个月',
    promotionEndDate: '2026-07-31',
    sourceVersion: '202607',
    discountFormula: '(HK$138 x 24个月 - HK$600电子缴费券 - HK$260渠道补贴) / 24个月',
    description: '秋季校园优惠主推款，适合学生长期使用。',
    sortOrder: 50,
    enabled: 1,
    offers: [
      { id: 4, planCode: 'STUDENT_SLASH_50GB_24M', offerType: 'POINTS', offerName: '积分合计', offerValue: '60,000分，可抵HK$600电子缴费券', sortOrder: 10, enabled: 1 },
      { id: 5, planCode: 'STUDENT_SLASH_50GB_24M', offerType: 'SUBSIDY', offerName: '渠道额外补贴', offerValue: 'HK$260', sortOrder: 20, enabled: 1 },
      { id: 6, planCode: 'STUDENT_SLASH_50GB_24M', offerType: 'SUBSIDY', offerName: '购机补贴', offerValue: 'HK$600', sortOrder: 30, enabled: 1 }
    ]
  }
]

onMounted(async () => {
  try {
    const response = await fetchMobilePlans()
    if (response.code === 1) {
      plans.value = response.data
      return
    }
    errorMessage.value = response.message
    plans.value = fallbackPlans
  } catch (error) {
    errorMessage.value = '当前使用本地演示套餐，后端启动后会自动读取真实套餐。'
    plans.value = fallbackPlans
  } finally {
    loading.value = false
  }
})

function displayPrice(plan: MobilePlan) {
  return plan.effectivePriceText || plan.channelPriceText || `HK$${plan.monthlyFee}/月`
}

function selectPlan(plan: MobilePlan) {
  router.push({
    name: 'business-confirm',
    params: {
      code: 'MOBILE_PLAN'
    },
    query: {
      planCode: plan.planCode
    }
  })
}
</script>

<template>
  <main class="mobile-page">
    <header class="page-header">
      <button class="back-button" type="button" @click="router.back()">‹</button>
      <div>
        <p class="eyebrow">移动套餐办理</p>
        <h1>选择套餐</h1>
      </div>
    </header>

    <section class="flow-steps">
      <span class="active">选套餐</span>
      <span>确认办理</span>
      <span>转人工</span>
    </section>

    <div v-if="loading" class="status-box">正在加载套餐...</div>
    <div v-else>
      <div v-if="errorMessage" class="status-box soft">{{ errorMessage }}</div>

      <section class="plan-list">
        <article v-for="plan in plans" :key="plan.planCode" class="plan-card">
          <div class="plan-head">
            <div>
              <p class="eyebrow">{{ plan.planType }} · {{ plan.contractPeriod }}</p>
              <h2>{{ plan.planName }}</h2>
            </div>
            <div class="price-block">
              <strong>{{ displayPrice(plan) }}</strong>
              <span v-if="plan.effectivePriceText">原价 {{ plan.channelPriceText }}</span>
            </div>
          </div>

          <div class="plan-features">
            <span>{{ plan.dataQuota }}</span>
            <span>{{ plan.voiceQuota }}</span>
            <span v-if="plan.roamingBenefit">{{ plan.roamingBenefit }}</span>
            <span v-if="plan.promotionEndDate">优惠至 {{ plan.promotionEndDate }}</span>
          </div>

          <div v-if="plan.offers?.length" class="offer-list">
            <span v-for="offer in plan.offers.slice(0, 4)" :key="offer.id">
              {{ offer.offerName }}：{{ offer.offerValue }}
            </span>
          </div>

          <p class="plan-desc">{{ plan.description }}</p>
          <button class="primary-button full" type="button" @click="selectPlan(plan)">选择此套餐</button>
        </article>
      </section>
    </div>
  </main>
</template>
