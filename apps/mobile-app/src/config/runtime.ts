import { normalizeBaseUrl } from '../api/runtime-helpers.mjs'

const env = (import.meta as any).env || {}
const defaultAssetBase = env.DEV ? '/src/static/assets' : '/static/assets'

export const API_BASE_URL = normalizeBaseUrl(env.VITE_MOBILE_API_BASE_URL, '/api')
export const ASSET_BASE_URL = normalizeBaseUrl(env.VITE_MOBILE_ASSET_BASE_URL, defaultAssetBase)
export const DEMO_USER_ID = Number(env.VITE_DEMO_USER_ID || 1) || 1
export const REQUEST_TIMEOUT_MS = Number(env.VITE_MOBILE_REQUEST_TIMEOUT_MS || 8000) || 8000
