type OidcDiscoveryDocument = {
  authorization_endpoint: string
  token_endpoint: string
  end_session_endpoint?: string
}

type StoredSession = { accessToken: string; tokenType: string; expiresAt: number }
type TokenResponse = { access_token?: unknown; token_type?: unknown; expires_in?: unknown }
type OidcClient = ReturnType<typeof createBrowserOidcClient>

const EXPIRY_SAFETY_WINDOW_MS = 30_000

function createBrowserOidcClient(storagePrefix: string) {
  const sessionKey = `${storagePrefix}.oidc.session`
  const transactionKey = `${storagePrefix}.oidc.transaction`
  let accessTokenValue: string | null = null
  let accessTokenType = 'Bearer'
  let accessTokenExpiresAt = 0
  let discoveryRequest: Promise<OidcDiscoveryDocument> | null = null
  let callbackRequest: Promise<void> | null = null

  function envValue(...keys: string[]): string {
    const env = ((import.meta as ImportMeta & { env?: Record<string, unknown> }).env ?? {})
    for (const key of keys) {
      const value = String(env[key] ?? '').trim()
      if (value) return value
    }
    return ''
  }

  function isOidcAuthEnabled(): boolean {
    return envValue('VITE_COMMERCEFLOW_AUTH_MODE', 'VITE_AUTH_MODE').toUpperCase() === 'OIDC'
  }

  function isOidcConfigured(): boolean {
    return Boolean(envValue('VITE_COMMERCEFLOW_AUTH_ISSUER_URI', 'VITE_OIDC_ISSUER') && envValue('VITE_COMMERCEFLOW_AUTH_CLIENT_ID', 'VITE_OIDC_CLIENT_ID'))
  }

  function requireBrowser(): Window {
    if (typeof window === 'undefined') throw new Error('OIDC 登录只能在浏览器环境中启动。')
    return window
  }

  function requireConfiguration() {
    const issuer = envValue('VITE_COMMERCEFLOW_AUTH_ISSUER_URI', 'VITE_OIDC_ISSUER').replace(/\/+$/, '')
    const clientId = envValue('VITE_COMMERCEFLOW_AUTH_CLIENT_ID', 'VITE_OIDC_CLIENT_ID')
    if (!issuer || !clientId) throw new Error('OIDC 登录尚未配置 issuer 或 client id，请先补齐前端部署环境变量。')
    return {
      issuer,
      clientId,
      audience: envValue('VITE_COMMERCEFLOW_AUTH_AUDIENCE', 'VITE_OIDC_AUDIENCE'),
      scope: envValue('VITE_COMMERCEFLOW_AUTH_SCOPE', 'VITE_OIDC_SCOPE') || 'openid profile email'
    }
  }

  function base64Url(bytes: Uint8Array): string {
    let binary = ''
    for (const byte of bytes) binary += String.fromCharCode(byte)
    return btoa(binary).replace(/\+/g, '-').replace(/\//g, '_').replace(/=+$/g, '')
  }

  function randomString(byteLength = 32): string {
    const bytes = new Uint8Array(byteLength)
    crypto.getRandomValues(bytes)
    return base64Url(bytes)
  }

  async function pkceChallenge(verifier: string): Promise<string> {
    const digest = await crypto.subtle.digest('SHA-256', new TextEncoder().encode(verifier))
    return base64Url(new Uint8Array(digest))
  }

  function storage(): Storage | null {
    try { return typeof window === 'undefined' ? null : window.sessionStorage } catch { return null }
  }

  function clearStoredSession() {
    try { storage()?.removeItem(sessionKey) } catch { /* Ignore privacy-mode storage failures. */ }
  }

  function dispatchAuthChange() {
    if (typeof window !== 'undefined') window.dispatchEvent(new CustomEvent(`${storagePrefix}:auth-change`))
  }

  function setSession(payload: TokenResponse) {
    const accessToken = typeof payload.access_token === 'string' ? payload.access_token.trim() : ''
    if (!accessToken) throw new Error('OIDC token 响应缺少 access_token。')
    const expiresIn = typeof payload.expires_in === 'number' && Number.isFinite(payload.expires_in) ? Math.max(payload.expires_in, 60) : 300
    accessTokenValue = accessToken
    accessTokenType = typeof payload.token_type === 'string' && payload.token_type.trim() ? payload.token_type : 'Bearer'
    accessTokenExpiresAt = Date.now() + expiresIn * 1000
    const stored: StoredSession = { accessToken, tokenType: accessTokenType, expiresAt: accessTokenExpiresAt }
    try { storage()?.setItem(sessionKey, JSON.stringify(stored)) } catch { /* Memory-only fallback remains valid for this page. */ }
    dispatchAuthChange()
  }

  function restoreSession(): boolean {
    if (accessTokenValue && accessTokenExpiresAt > Date.now() + EXPIRY_SAFETY_WINDOW_MS) return true
    accessTokenValue = null
    accessTokenExpiresAt = 0
    const raw = storage()?.getItem(sessionKey)
    if (!raw) return false
    try {
      const parsed = JSON.parse(raw) as Partial<StoredSession>
      if (typeof parsed.accessToken !== 'string' || typeof parsed.expiresAt !== 'number' || parsed.expiresAt <= Date.now() + EXPIRY_SAFETY_WINDOW_MS) {
        clearStoredSession()
        return false
      }
      accessTokenValue = parsed.accessToken
      accessTokenType = typeof parsed.tokenType === 'string' && parsed.tokenType ? parsed.tokenType : 'Bearer'
      accessTokenExpiresAt = parsed.expiresAt
      return true
    } catch {
      clearStoredSession()
      return false
    }
  }

  function callbackParameters(): URLSearchParams | null {
    if (typeof window === 'undefined') return null
    const query = new URLSearchParams(window.location.search)
    if (query.has('code') || query.has('error')) return query
    const hashQuery = window.location.hash.split('?')[1]
    if (!hashQuery) return null
    const hashParams = new URLSearchParams(hashQuery)
    return hashParams.has('code') || hashParams.has('error') ? hashParams : null
  }

  function removeCallbackParameters() {
    if (typeof window === 'undefined') return
    const hashPath = window.location.hash.split('?')[0]
    window.history.replaceState(null, globalThis.document.title, `${window.location.origin}${window.location.pathname}${hashPath}`)
  }

  async function discovery(): Promise<OidcDiscoveryDocument> {
    if (!discoveryRequest) {
      const { issuer } = requireConfiguration()
      discoveryRequest = fetch(`${issuer}/.well-known/openid-configuration`, { headers: { Accept: 'application/json' } })
        .then(async response => {
          if (!response.ok) throw new Error(`OIDC discovery 失败（HTTP ${response.status}）。`)
          const document = await response.json() as Partial<OidcDiscoveryDocument>
          if (!document.authorization_endpoint || !document.token_endpoint) throw new Error('OIDC discovery 缺少授权或 token endpoint。')
          return document as OidcDiscoveryDocument
        })
        .catch(error => { discoveryRequest = null; throw error })
    }
    return discoveryRequest
  }

  function redirectUri(): string {
    const browser = requireBrowser()
    return `${browser.location.origin}${browser.location.pathname}`
  }

  async function handleCallback(parameters: URLSearchParams) {
    const browser = requireBrowser()
    const transactionRaw = storage()?.getItem(transactionKey)
    let transaction: { state: string; verifier: string; redirectUri: string } | null = null
    try { transaction = transactionRaw ? JSON.parse(transactionRaw) : null } catch { transaction = null }
    try {
      const error = parameters.get('error')
      if (error) throw new Error(`OIDC 登录被拒绝：${parameters.get('error_description') || error}。`)
      const code = parameters.get('code')
      if (!code || !transaction || parameters.get('state') !== transaction.state) throw new Error('OIDC 登录状态校验失败，请重新登录。')
      const { clientId } = requireConfiguration()
      const discoveryDocument = await discovery()
      const body = new URLSearchParams({ grant_type: 'authorization_code', client_id: clientId, code, redirect_uri: transaction.redirectUri, code_verifier: transaction.verifier })
      const response = await fetch(discoveryDocument.token_endpoint, { method: 'POST', headers: { 'Content-Type': 'application/x-www-form-urlencoded', Accept: 'application/json' }, body })
      const payload = await response.json().catch(() => ({})) as TokenResponse & { error?: string; error_description?: string }
      if (!response.ok) throw new Error(`OIDC token 交换失败：${payload.error_description || payload.error || `HTTP ${response.status}`}。`)
      setSession(payload)
      const cleanHash = browser.location.hash.split('?')[0]
      browser.history.replaceState(null, globalThis.document.title, `${browser.location.origin}${browser.location.pathname}${cleanHash}`)
    } finally {
      try { storage()?.removeItem(transactionKey) } catch { /* Ignore unavailable storage. */ }
      removeCallbackParameters()
    }
  }

  function prepareOidcSession(): Promise<void> {
    if (!isOidcAuthEnabled() || typeof window === 'undefined') return Promise.resolve()
    if (callbackRequest) return callbackRequest
    const parameters = callbackParameters()
    if (!parameters) { restoreSession(); return Promise.resolve() }
    callbackRequest = handleCallback(parameters).finally(() => { callbackRequest = null })
    return callbackRequest
  }

  function getAccessToken(): string | null { return restoreSession() ? accessTokenValue : null }
  function getAuthorizationHeader(): string | null { const token = getAccessToken(); return token ? `${accessTokenType} ${token}` : null }
  function isSignedIn(): boolean { return Boolean(getAccessToken()) }

  async function startOidcLogin() {
    const browser = requireBrowser()
    if (!isOidcAuthEnabled()) throw new Error('当前构建未启用 OIDC 登录。')
    const { clientId, audience, scope } = requireConfiguration()
    const discoveryDocument = await discovery()
    const verifier = randomString(48)
    const state = randomString(32)
    const callback = redirectUri()
    try { storage()?.setItem(transactionKey, JSON.stringify({ state, verifier, redirectUri: callback, createdAt: Date.now() })) }
    catch { throw new Error('浏览器无法保存 OIDC 登录状态，请检查隐私模式设置。') }
    const params = new URLSearchParams({ response_type: 'code', client_id: clientId, redirect_uri: callback, scope, state, code_challenge: await pkceChallenge(verifier), code_challenge_method: 'S256' })
    if (audience) params.set('audience', audience)
    browser.location.assign(`${discoveryDocument.authorization_endpoint}?${params.toString()}`)
  }

  function clearOidcSession() {
    accessTokenValue = null
    accessTokenType = 'Bearer'
    accessTokenExpiresAt = 0
    clearStoredSession()
    try { storage()?.removeItem(transactionKey) } catch { /* Ignore unavailable storage. */ }
    dispatchAuthChange()
  }

  return { isOidcAuthEnabled, isOidcConfigured, prepareOidcSession, getAccessToken, getAuthorizationHeader, isSignedIn, startOidcLogin, clearOidcSession, authChangeEvent: `${storagePrefix}:auth-change` }
}

export type { OidcClient }
export { createBrowserOidcClient }
