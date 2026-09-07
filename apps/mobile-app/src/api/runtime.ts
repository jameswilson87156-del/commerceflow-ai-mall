import { API_BASE_URL, ASSET_BASE_URL, REQUEST_TIMEOUT_MS } from '../config/runtime'
import { classifyHttpFailure } from './failure-policy.mjs'
import { getAuthorizationHeader, prepareOidcSession } from './oidc'
import { joinUrl } from './runtime-helpers.mjs'
import type { ApiErrorShape, RateLimitMeta } from './types'

declare const uni: any

export type HttpMethod = 'GET' | 'POST' | 'PUT' | 'PATCH' | 'DELETE'
export type ApiFailureKind = 'unauthorized' | 'forbidden' | 'not-found' | 'backend-unavailable' | 'request-failed'

export interface RequestOptions {
  method?: HttpMethod
  data?: Record<string, unknown> | unknown[]
  headers?: Record<string, string>
  timeout?: number
  onTask?: (task: { abort: () => void }) => void
}

export class ApiError extends Error {
  statusCode: number
  body: ApiErrorShape | null
  headers: Record<string, string>
  rateLimit: RateLimitMeta
  kind: ApiFailureKind

  constructor(message: string, statusCode = 0, body: ApiErrorShape | null = null, headers: Record<string, string> = {}) {
    super(message)
    this.name = 'ApiError'
    this.statusCode = statusCode
    this.body = body
    this.headers = headers
    this.rateLimit = readRateLimitMeta(headers, body)
    this.kind = classifyApiFailure(statusCode)
  }
}

export function classifyApiFailure(statusCode: number): ApiFailureKind {
  return classifyHttpFailure(statusCode) as ApiFailureKind
}

export function apiErrorMessage(error: unknown, fallback: string): string {
  if (!(error instanceof ApiError)) return error instanceof Error && error.message ? error.message : fallback
  if (error.statusCode === 429) return error.message
  if (error.kind === 'unauthorized') return 'Unauthorized：当前接口需要已确认的用户身份，请先完成登录后重试。'
  if (error.kind === 'forbidden') return 'Forbidden：当前身份无权访问该资源。'
  if (error.kind === 'not-found') return 'Not Found：请求的资源不存在。'
  if (error.kind === 'backend-unavailable') return 'Backend Unavailable：后端暂时不可用，未使用 Mock 数据回填，请确认服务后重试。'
  return error.message || fallback
}

function normalizeHeaders(input: Record<string, unknown> = {}) {
  return Object.fromEntries(Object.entries(input).map(([key, value]) => [key.toLowerCase(), String(value)]))
}

function parseBody(value: unknown): ApiErrorShape | null {
  if (value && typeof value === 'object') return value as ApiErrorShape
  if (typeof value === 'string') {
    try { return JSON.parse(value) as ApiErrorShape } catch { return { message: value } }
  }
  return null
}

export function readRateLimitMeta(headers: Record<string, string>, body: ApiErrorShape | null = null): RateLimitMeta {
  const value = (name: string) => headers[name.toLowerCase()]
  const numberValue = (raw?: string) => raw === undefined ? undefined : Number(raw)
  const retryAfter = value('retry-after') ?? (body?.retryAfterSeconds === undefined ? undefined : String(body.retryAfterSeconds))
  return {
    mode: value('x-ratelimit-mode'),
    limit: numberValue(value('x-ratelimit-limit')),
    remaining: numberValue(value('x-ratelimit-remaining')),
    reset: numberValue(value('x-ratelimit-reset')),
    retryAfter: numberValue(retryAfter)
  }
}

export function resolveImageUrl(path?: string | null) {
  if (!path) return ''
  if (/^https?:\/\//i.test(path)) return path
  if (path.startsWith('/assets/')) return joinUrl(ASSET_BASE_URL, path.slice('/assets/'.length))
  return joinUrl(ASSET_BASE_URL, path.replace(/^\/+/, ''))
}

export function request<T>(path: string, options: RequestOptions = {}): Promise<{ data: T; statusCode: number; headers: Record<string, string> }> {
  return new Promise((resolve, reject) => {
    void prepareOidcSession().then(() => {
      if (typeof uni === 'undefined' || typeof uni.request !== 'function') {
        reject(new ApiError('当前运行环境不支持网络请求'))
        return
      }
      const header = {'Content-Type': 'application/json', ...(options.headers || {})}
      const authorization = getAuthorizationHeader()
      if (authorization && !Object.keys(header).some((key) => key.toLowerCase() === 'authorization')) header.Authorization = authorization
      const task = uni.request({
        url: joinUrl(API_BASE_URL, path),
        method: options.method || 'GET',
        data: options.data,
        header,
        timeout: options.timeout || REQUEST_TIMEOUT_MS,
        success: (response: any) => {
          const headers = normalizeHeaders(response.header || {})
          const body = parseBody(response.data)
          if (response.statusCode >= 200 && response.statusCode < 300) {
            resolve({data: response.data as T, statusCode: response.statusCode, headers})
            return
          }
          reject(new ApiError(body?.message || `请求失败（${response.statusCode}）`, response.statusCode, body, headers))
        },
        fail: (error: any) => reject(new ApiError(error?.errMsg || '网络请求失败'))
      })
      options.onTask?.(task)
    }).catch((error) => reject(new ApiError(error instanceof Error ? error.message : 'OIDC 登录回调处理失败。')))
  })
}
