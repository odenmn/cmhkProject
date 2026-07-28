<script setup lang="ts">
import { useRouter } from 'vue-router'

const router = useRouter()
</script>

<template>
  <button class="ai-float-button" type="button" @click="router.push('/ai-chat')">
    <span>AI</span>
    <strong>客服</strong>
  </button>
</template>

