<script setup lang="ts">
import { useRouter } from 'vue-router'

const router = useRouter()
</script>

<template>
  <main class="mobile-page">
    <header class="page-header">
      <button class="back-button" type="button" @click="router.back()">‹</button>
      <div>
        <p class="eyebrow">客户中心</p>
        <h1>我的资料</h1>
      </div>
    </header>

    <section class="profile-card">
      <div class="avatar">C</div>
      <div>
        <h2>CMHK 用户</h2>
        <p>登录能力接入后，这里会显示客户资料和号码信息。</p>
      </div>
    </section>

    <section class="menu-list">
      <button type="button">个人资料 <span>›</span></button>
      <button type="button">常用号码 <span>›</span></button>
      <button type="button">服务协议 <span>›</span></button>
    </section>
  </main>
</template>

