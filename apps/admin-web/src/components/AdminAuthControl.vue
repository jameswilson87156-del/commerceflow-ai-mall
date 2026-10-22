<script setup lang="ts">
import { onMounted, onUnmounted, ref } from 'vue'
import { authChangeEvent, clearOidcSession, isOidcAuthEnabled, isOidcConfigured, isSignedIn, startOidcLogin } from '../oidc'

const enabled = isOidcAuthEnabled()
const configured = isOidcConfigured()
const signedIn = ref(enabled && isSignedIn())
const busy = ref(false)
const errorMessage = ref('')

function sync() { signedIn.value = enabled && isSignedIn() }
async function login() {
  errorMessage.value = ''
  busy.value = true
  try { await startOidcLogin() }
  catch (error) { errorMessage.value = error instanceof Error ? error.message : '登录启动失败。'; busy.value = false }
}
function logout() { clearOidcSession(); sync() }
onMounted(() => window.addEventListener(authChangeEvent, sync))
onUnmounted(() => window.removeEventListener(authChangeEvent, sync))
</script>

<template>
  <div v-if="enabled" class="admin-auth-control" aria-live="polite">
    <span v-if="!configured" class="admin-auth-control__warning">OIDC 未配置</span>
    <span v-else-if="signedIn" class="admin-auth-control__state"><span class="admin-auth-control__dot" aria-hidden="true"></span>已登录</span>
    <button v-if="configured && !signedIn" type="button" class="admin-auth-control__button" :disabled="busy" @click="login">{{ busy ? '跳转中…' : '管理端登录' }}</button>
    <button v-else-if="configured && signedIn" type="button" class="admin-auth-control__button admin-auth-control__button--quiet" @click="logout">退出</button>
    <span v-if="errorMessage" class="admin-auth-control__error" role="alert">{{ errorMessage }}</span>
  </div>
</template>

<style scoped>
.admin-auth-control { position: relative; display: inline-flex; align-items: center; gap: 8px; }
.admin-auth-control__button { min-height: 30px; border: 1px solid var(--brand-primary); border-radius: var(--radius-control); background: var(--brand-primary); padding: 0 12px; color: #fff; font-size: 11px; font-weight: 650; white-space: nowrap; }
.admin-auth-control__button:disabled { cursor: wait; opacity: .55; }
.admin-auth-control__button--quiet { border-color: var(--border-subtle); background: var(--surface-panel); color: var(--text-primary); }
.admin-auth-control__state, .admin-auth-control__warning { display: inline-flex; align-items: center; gap: 5px; font-size: 10px; white-space: nowrap; }
.admin-auth-control__state { color: var(--state-success); }.admin-auth-control__warning { color: var(--state-warning); }
.admin-auth-control__dot { width: 6px; height: 6px; border-radius: 50%; background: currentColor; }
.admin-auth-control__error { position: absolute; top: calc(100% + 7px); right: 0; z-index: 3; width: 280px; border: 1px solid #e8bbbb; border-radius: var(--radius-control); background: var(--state-danger-soft); padding: 8px 10px; color: var(--state-danger); font-size: 10px; line-height: 1.4; }
@media (max-width: 820px) { .admin-auth-control__state, .admin-auth-control__warning { display: none; } .admin-auth-control__error { right: -6px; } }
</style>
