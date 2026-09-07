import { getAuthorizationHeader, prepareOidcSession } from './oidc'

export const API_BASE = String(import.meta.env.VITE_API_BASE || 'http://localhost:8080/api').replace(/\/+$/, '')

export type AdminAccessState = 'unauthorized' | 'forbidden' | 'not-found' | 'backend-unavailable' | 'request-failed'

export function apiUrl(path: string): string {
  return `${API_BASE}/${path.replace(/^\/+/, '')}`
}

export function classifyAdminAccess(status: number): AdminAccessState {
  if (status === 401) return 'unauthorized'
  if (status === 403) return 'forbidden'
  if (status === 404) return 'not-found'
  if (status === 0 || status >= 500) return 'backend-unavailable'
  return 'request-failed'
}

export function adminAccessStateLabel(state: AdminAccessState): string {
  const labels: Record<AdminAccessState, string> = {
    unauthorized: 'Unauthorized',
    forbidden: 'Forbidden',
    'not-found': 'Not Found',
    'backend-unavailable': 'Backend Unavailable',
    'request-failed': 'Request Failed',
  }
  return labels[state]
}

export function adminAccessStateMessage(state: AdminAccessState): string {
  const messages: Record<AdminAccessState, string> = {
    unauthorized: '当前管理端需要已确认的 Operator 身份，请先完成登录或配置后端管理身份。',
    forbidden: '当前身份无权访问此管理资源；只有 Operator 或 Admin 可读取跨用户运营数据。',
    'not-found': '请求的管理资源不存在，请检查接口版本或资源标识。',
    'backend-unavailable': 'Java 后端暂时不可用，未使用 Mock 数据回填；请确认服务后重试。',
    'request-failed': '管理端请求失败，请检查返回信息后重试。',
  }
  return messages[state]
}

export class AdminApiError extends Error {
  readonly state: AdminAccessState

  constructor(
    message: string,
    readonly status: number,
    readonly code: string | null = null,
    state: AdminAccessState = classifyAdminAccess(status),
  ) {
    super(message)
    this.name = 'AdminApiError'
    this.state = state
  }
}

function bodyMessage(body: unknown): string | null {
  if (!body || typeof body !== 'object') return null
  const message = (body as { message?: unknown }).message
  return typeof message === 'string' && message.trim() ? message : null
}

export async function fetchAdminJson<T>(path: string, init: RequestInit = {}): Promise<T> {
  let response: Response
  try {
    response = await fetch(apiUrl(path), await authorizedRequestInit(init))
  } catch (error) {
    if (error instanceof AdminApiError) throw error
    if (error instanceof Error && error.message.startsWith('OIDC')) {
      throw new AdminApiError(error.message, 401, 'OIDC_CALLBACK_FAILED', 'unauthorized')
    }
    throw new AdminApiError(adminAccessStateMessage('backend-unavailable'), 0, null, 'backend-unavailable')
  }

  const body = await response.json().catch(() => null) as { code?: unknown; message?: unknown } | null
  if (!response.ok) {
    const state = classifyAdminAccess(response.status)
    const code = typeof body?.code === 'string' ? body.code : null
    const message = response.status === 429 || (state === 'request-failed' && response.status < 500)
      ? bodyMessage(body) || adminAccessStateMessage(state)
      : adminAccessStateMessage(state)
    throw new AdminApiError(message, response.status, code, state)
  }
  return body as T
}

export async function authorizedRequestInit(init: RequestInit = {}): Promise<RequestInit> {
  await prepareOidcSession()
  const headers = new Headers(init.headers)
  const authorization = getAuthorizationHeader()
  if (authorization && !headers.has('Authorization')) headers.set('Authorization', authorization)
  return { ...init, headers }
}
