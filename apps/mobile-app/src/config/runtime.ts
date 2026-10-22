import { normalizeBaseUrl } from '../api/runtime-helpers.mjs'

const env = (import.meta as any).env || {}
const defaultAssetBase = env.DEV ? '/src/static/assets' : '/static/assets'

export const API_BASE_URL = normalizeBaseUrl(env.VITE_MOBILE_API_BASE_URL, '/api')
export const ASSET_BASE_URL = normalizeBaseUrl(env.VITE_MOBILE_ASSET_BASE_URL, defaultAssetBase)
export type ClientDataMode = 'LOCAL_DEMO_FIXTURE' | 'REAL_BACKEND'
const configuredRuntimeMode = String(env.VITE_MOBILE_RUNTIME_MODE || '').trim().toUpperCase()
export const CLIENT_DATA_MODE: ClientDataMode = configuredRuntimeMode === 'LOCAL_DEMO_FIXTURE' || configuredRuntimeMode === 'DEMO'
  ? 'LOCAL_DEMO_FIXTURE'
  : 'REAL_BACKEND'
export const CLIENT_DATA_MODE_LABEL = CLIENT_DATA_MODE === 'LOCAL_DEMO_FIXTURE' ? '本地演示数据' : '服务端数据'
export const REQUEST_TIMEOUT_MS = Number(env.VITE_MOBILE_REQUEST_TIMEOUT_MS || 8000) || 8000
