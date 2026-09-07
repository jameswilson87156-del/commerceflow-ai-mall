<script setup lang="ts">
import { onMounted, onUnmounted, ref } from 'vue'
import { authChangeEvent, clearOidcSession, getAccessToken, isOidcAuthEnabled, isOidcConfigured, startOidcLogin } from '../api/oidc'

withDefaults(defineProps<{
  title: string
  eyebrow?: string
  subtitle?: string
  back?: boolean
  actionLabel?: string
}>(), { eyebrow: 'COMMERCEFLOW AI MALL', back: false })

const emit = defineEmits<{ back: []; action: [] }>()
const oidcEnabled = isOidcAuthEnabled()
const oidcConfigured = isOidcConfigured()
const signedIn = ref(oidcEnabled && Boolean(getAccessToken()))
const loginBusy = ref(false)

function syncAuth() { signedIn.value = oidcEnabled && Boolean(getAccessToken()) }
function showAuthError(message: string) { if (typeof uni !== 'undefined' && typeof uni.showToast === 'function') uni.showToast({ title: message.slice(0, 28), icon: 'none' }) }
async function login() {
  loginBusy.value = true
  try { await startOidcLogin() }
  catch (error) { showAuthError(error instanceof Error ? error.message : '登录启动失败。'); loginBusy.value = false }
}
function logout() { clearOidcSession(); syncAuth() }
onMounted(() => { if (typeof window !== 'undefined') window.addEventListener(authChangeEvent, syncAuth) })
onUnmounted(() => { if (typeof window !== 'undefined') window.removeEventListener(authChangeEvent, syncAuth) })
</script>

<template>
  <view class="mobile-header">
    <button v-if="back" class="icon-button header-back" aria-label="返回" @click="emit('back')"><text class="back-chevron">‹</text></button>
    <view class="header-copy">
      <text class="header-eyebrow">{{ eyebrow }}</text>
      <text class="header-title">{{ title }}</text>
      <text v-if="subtitle" class="header-subtitle">{{ subtitle }}</text>
    </view>
    <view class="header-actions">
      <button v-if="actionLabel" class="header-action" @click="emit('action')">{{ actionLabel }}</button>
      <button v-if="oidcEnabled && oidcConfigured && !signedIn" class="header-auth" :disabled="loginBusy" type="button" @click="login">{{ loginBusy ? '跳转中…' : '登录' }}</button>
      <button v-else-if="oidcEnabled && oidcConfigured && signedIn" class="header-auth header-auth--quiet" type="button" @click="logout">退出</button>
      <text v-else-if="!actionLabel" class="header-balance" />
    </view>
  </view>
</template>
