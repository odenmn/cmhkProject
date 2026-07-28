<script setup lang="ts">
import { computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'

const route = useRoute()
const router = useRouter()

const orderNo = computed(() => String(route.query.orderNo || ''))
</script>

<template>
  <main class="mobile-page">
    <header class="page-header">
      <button class="back-button" type="button" @click="router.push('/')">‹</button>
      <div>
        <p class="eyebrow">已转人工</p>
        <h1>人工客服将继续处理</h1>
      </div>
    </header>

    <section class="flow-steps">
      <span>选套餐</span>
      <span>确认办理</span>
      <span class="active">转人工</span>
    </section>

    <section class="result-card">
      <div class="result-icon">✓</div>
      <h2>办理请求已提交</h2>
      <p>系统已将客户选择的套餐和联系方式转交人工客服。</p>
    </section>

    <section class="summary-list">
      <div>
        <span>订单编号</span>
        <strong>{{ orderNo }}</strong>
      </div>
      <div>
        <span>订单信息</span>
        <strong>已随订单保存</strong>
      </div>
      <div>
        <span>当前状态</span>
        <strong>等待人工客服跟进</strong>
      </div>
    </section>

    <button class="primary-button full page-action" type="button" @click="router.push('/')">返回首页</button>
  </main>
</template>
