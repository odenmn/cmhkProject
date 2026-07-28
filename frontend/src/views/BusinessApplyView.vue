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
    planCode: 'CMHK_5G_128',
    planName: '5G 畅享 128 套餐',
    monthlyFee: 128,
    dataQuota: '30GB 本地数据',
    voiceQuota: '1000 分钟本地通话',
    contractPeriod: '12 个月',
    description: '适合日常通讯、视频和社交使用。',
    sortOrder: 10,
    enabled: 1
  },
  {
    id: 2,
    planCode: 'CMHK_5G_198',
    planName: '5G 畅享 198 套餐',
    monthlyFee: 198,
    dataQuota: '80GB 本地数据',
    voiceQuota: '2000 分钟本地通话',
    contractPeriod: '12 个月',
    description: '适合高频上网、热点共享和商务使用。',
    sortOrder: 20,
    enabled: 1
  },
  {
    id: 3,
    planCode: 'CMHK_5G_298',
    planName: '5G 尊享 298 套餐',
    monthlyFee: 298,
    dataQuota: '150GB 本地数据',
    voiceQuota: '无限本地通话',
    contractPeriod: '24 个月',
    description: '适合重度数据用户和家庭共享场景。',
    sortOrder: 30,
    enabled: 1
  }
]

onMounted(async () => {
  try {
    const response = await fetchMobilePlans()
    if (response.code === 0) {
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

function selectPlan(plan: MobilePlan) {
  router.push({
    name: 'business-confirm',
    params: {
      code: 'MOBILE_PLAN'
    },
    query: {
      planCode: plan.planCode,
      planName: plan.planName,
      monthlyFee: String(plan.monthlyFee),
      dataQuota: plan.dataQuota,
      voiceQuota: plan.voiceQuota,
      contractPeriod: plan.contractPeriod
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
              <p class="eyebrow">{{ plan.contractPeriod }}</p>
              <h2>{{ plan.planName }}</h2>
            </div>
            <div class="price-block">
              <strong>${{ plan.monthlyFee }}</strong>
              <span>/月</span>
            </div>
          </div>
          <div class="plan-features">
            <span>{{ plan.dataQuota }}</span>
            <span>{{ plan.voiceQuota }}</span>
          </div>
          <p class="plan-desc">{{ plan.description }}</p>
          <button class="primary-button full" type="button" @click="selectPlan(plan)">选择此套餐</button>
        </article>
      </section>
    </div>
  </main>
</template>

