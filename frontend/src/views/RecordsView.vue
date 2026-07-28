<script setup lang="ts">
import { useRouter } from 'vue-router'

const router = useRouter()
</script>

<template>
  <main class="mobile-page">
    <header class="page-header">
      <button class="back-button" type="button" @click="router.back()">‹</button>
      <div>
        <p class="eyebrow">办理记录</p>
        <h1>查看申请进度</h1>
      </div>
    </header>

    <section class="empty-state">
      <h2>暂无办理记录</h2>
      <p>提交业务申请后，记录会显示在这里。</p>
      <button class="primary-button" type="button" @click="router.push('/')">去办理业务</button>
    </section>
  </main>
</template>

