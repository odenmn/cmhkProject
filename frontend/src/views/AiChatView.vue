<script setup lang="ts">
import { ref } from 'vue'
import { useRouter } from 'vue-router'

const router = useRouter()
const inputText = ref('')
const messages = ref([
  {
    role: 'assistant',
    text: '你好，我是 CMHK AI 客服。当前聊天功能为前端展示版本，后续会接入真实智能客服能力。'
  },
  {
    role: 'assistant',
    text: '你可以先选择移动套餐，确认办理后系统会转人工客服继续处理。'
  }
])

function sendDemoMessage() {
  if (!inputText.value.trim()) {
    return
  }
  messages.value.push({
    role: 'user',
    text: inputText.value
  })
  messages.value.push({
    role: 'assistant',
    text: '这是展示版回复。真实 AI 客服会在后续网站流程稳定后接入。'
  })
  inputText.value = ''
}
</script>

<template>
  <main class="mobile-page chat-page">
    <header class="page-header">
      <button class="back-button" type="button" @click="router.back()">‹</button>
      <div>
        <p class="eyebrow">AI 客服</p>
        <h1>在线咨询</h1>
      </div>
    </header>

    <section class="chat-list">
      <div v-for="(message, index) in messages" :key="index" class="chat-bubble" :class="message.role">
        {{ message.text }}
      </div>
    </section>

    <footer class="chat-input-bar">
      <input v-model="inputText" placeholder="输入想咨询的问题" @keyup.enter="sendDemoMessage" />
      <button type="button" @click="sendDemoMessage">发送</button>
    </footer>
  </main>
</template>

