<template>
  <RouterView />
  <AiAssistantButton v-if="$route.name !== 'ai-chat'" />
</template>

<script setup lang="ts">
import AiAssistantButton from './components/AiAssistantButton.vue'
</script>

