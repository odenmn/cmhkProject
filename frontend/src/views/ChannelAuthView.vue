<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import {
  type ChannelEntryContext,
  loginByPhone,
  resolveChannelEntry,
  sendMockVerificationCode
} from '../api/http'
import { saveAuthSession } from '../auth/session'

const route = useRoute()
const router = useRouter()
const entryToken = computed(() => String(route.query.entryToken || ''))
const entry = ref<ChannelEntryContext | null>(null)
const phone = ref('')
const verificationCode = ref('')
const loading = ref(true)
const sending = ref(false)
const submitting = ref(false)
const codeSent = ref(false)
const errorMessage = ref('')
const successMessage = ref('')

onMounted(async () => {
  if (!entryToken.value) {
    errorMessage.value = '未识别到渠道入口，请使用渠道二维码或链接重新进入。'
    loading.value = false
    return
  }

  try {
    const response = await resolveChannelEntry(entryToken.value)
    if (response.code !== 1 || !response.data) {
      errorMessage.value = response.message || '渠道入口不可用。'
      return
    }
    entry.value = response.data
  } catch {
    errorMessage.value = '渠道入口校验失败，请确认后端服务已启动。'
  } finally {
    loading.value = false
  }
})

async function sendCode() {
  if (!phone.value.trim()) {
    errorMessage.value = '请先输入手机号码。'
    return
  }

  sending.value = true
  errorMessage.value = ''
  successMessage.value = ''
  try {
    const response = await sendMockVerificationCode(entryToken.value, phone.value)
    if (response.code !== 1) {
      errorMessage.value = response.message
      return
    }
    codeSent.value = true
    successMessage.value = '模拟验证码已发送，开发阶段请输入 123456。'
  } catch {
    errorMessage.value = '验证码发送失败，请确认后端服务已启动。'
  } finally {
    sending.value = false
  }
}

async function submitLogin() {
  if (!codeSent.value) {
    errorMessage.value = '请先获取验证码。'
    return
  }

  submitting.value = true
  errorMessage.value = ''
  try {
    const response = await loginByPhone(entryToken.value, phone.value, verificationCode.value)
    if (response.code !== 1 || !response.data) {
      errorMessage.value = response.message || '验证失败，请重试。'
      return
    }
    saveAuthSession(response.data)
    await router.push({ name: 'home' })
  } catch {
    errorMessage.value = '验证失败，请确认后端服务已启动。'
  } finally {
    submitting.value = false
  }
}
</script>

<template>
  <main class="mobile-page auth-page" :class="{ 'elderly-mode': entry?.elderlyMode === 1 }">
    <header class="page-header">
      <button class="back-button" type="button" aria-label="返回首页" @click="router.push('/')">←</button>
      <div>
        <p class="eyebrow">渠道身份验证</p>
        <h1>{{ entry?.elderlyMode === 1 ? '先验证手机号，再为你安排人工服务' : '验证手机号，开始办理' }}</h1>
      </div>
    </header>

    <div v-if="loading" class="status-box">正在校验渠道入口...</div>
    <template v-else-if="entry">
      <section class="notice-card channel-notice">
        <span>当前渠道</span>
        <h2>{{ entry.channelName }}</h2>
        <p v-if="entry.elderlyMode === 1">已为你开启长者关怀模式，后续可随时转人工微信客服。</p>
        <p v-else>完成验证后，系统会保留本次进入渠道，用于后续客服跟进。</p>
      </section>

      <section class="form-card">
        <h2>手机号验证</h2>
        <label class="field-row">
          <span>手机号码</span>
          <input v-model="phone" inputmode="tel" autocomplete="tel" placeholder="请输入手机号码">
        </label>
        <button class="secondary-button full" type="button" :disabled="sending" @click="sendCode">
          {{ sending ? '正在发送...' : codeSent ? '重新获取验证码' : '获取验证码' }}
        </button>
        <label class="field-row">
          <span>验证码</span>
          <input v-model="verificationCode" inputmode="numeric" autocomplete="one-time-code" maxlength="6" placeholder="请输入 6 位验证码">
        </label>
        <p class="auth-hint">当前为开发阶段模拟验证，不会发送真实短信。</p>
      </section>

      <div v-if="successMessage" class="status-box success">{{ successMessage }}</div>
      <div v-if="errorMessage" class="status-box soft">{{ errorMessage }}</div>
      <footer class="bottom-action">
        <button class="primary-button full" type="button" :disabled="submitting || !codeSent" @click="submitLogin">
          {{ submitting ? '正在验证...' : '验证并继续' }}
        </button>
      </footer>
    </template>
    <div v-else class="status-box soft">{{ errorMessage }}</div>
  </main>
</template>
