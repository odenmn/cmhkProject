<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { type BusinessType, fetchBusinessTypes } from '../api/http'

const router = useRouter()
const route = useRoute()
const businessTypes = ref<BusinessType[]>([])
const loading = ref(true)
const errorMessage = ref('')

const fallbackBusinessTypes: BusinessType[] = [
  {
    id: 1,
    code: 'MOBILE_PLAN',
    name: '移动套餐办理',
    description: '选择适合你的月费套餐、流量和通话组合。',
    sortOrder: 10,
    enabled: 1,
    createdAt: '',
    updatedAt: ''
  }
]

onMounted(async () => {
  const entryToken = String(route.query.entry_token || '')
  if (entryToken) {
    await router.replace({ name: 'channel-auth', query: { entryToken } })
    return
  }

  try {
    const response = await fetchBusinessTypes()
    if (response.code === 0) {
      businessTypes.value = response.data.filter((item) => item.enabled === 1)
      return
    }
    errorMessage.value = response.message
    businessTypes.value = fallbackBusinessTypes
  } catch (error) {
    errorMessage.value = '当前使用本地演示数据，后端启动后会自动读取真实业务类型。'
    businessTypes.value = fallbackBusinessTypes
  } finally {
    loading.value = false
  }
})

function goApply(item: BusinessType) {
  router.push({
    name: 'business-apply',
    params: {
      code: item.code
    },
    query: {
      name: item.name
    }
  })
}
</script>

<template>
  <main class="mobile-page">
    <header class="home-hero">
      <div class="brand-row">
        <div class="brand-logo">CMHK</div>
        <button class="icon-button" type="button" aria-label="客户中心" @click="router.push('/profile')">
          我的
        </button>
      </div>
      <p class="eyebrow">智能业务办理</p>
      <h1>你想办理什么业务？</h1>
    </header>

    <section class="quick-panel">
      <button class="quick-action" type="button" @click="router.push('/records')">
        <span>办理记录</span>
        <strong>查看进度</strong>
      </button>
      <button class="quick-action" type="button" @click="router.push('/profile')">
        <span>客户中心</span>
        <strong>资料管理</strong>
      </button>
    </section>

    <section class="section-block">
      <div class="section-title">
        <h2>当前业务</h2>
        <span>移动端 H5</span>
      </div>

      <div v-if="loading" class="status-box">正在加载业务类型...</div>
      <div v-else>
        <div v-if="errorMessage" class="status-box soft">{{ errorMessage }}</div>

        <div class="business-list">
          <button
            v-for="item in businessTypes"
            :key="item.id"
            class="business-item"
            type="button"
            @click="goApply(item)"
          >
            <span class="business-icon">{{ item.name.slice(0, 1) }}</span>
            <span class="business-content">
              <strong>{{ item.name }}</strong>
              <small>{{ item.description }}</small>
            </span>
            <span class="chevron">›</span>
          </button>
        </div>
      </div>
    </section>

    <nav class="bottom-tabs" aria-label="底部导航">
      <RouterLink class="tab-item active" to="/">首页</RouterLink>
      <RouterLink class="tab-item" to="/records">记录</RouterLink>
      <RouterLink class="tab-item" to="/profile">我的</RouterLink>
    </nav>
  </main>
</template>
