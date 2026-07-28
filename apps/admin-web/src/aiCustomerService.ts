import { formatMoney, stockLabel, stockState, type Product, type Sku, type StockState } from './catalog'

export const API_BASE = import.meta.env.VITE_API_BASE || 'http://localhost:8080/api'
export const DEMO_USER_ID = 1
export const MAX_QUESTION_LENGTH = 500

export type LoadState = 'loading' | 'ready' | 'empty' | 'error'

export type SelectedSku = {
  product: Product
  sku: Sku
}

export type AskRequest = {
  userId: number
  productId: number
  skuId: number
  question: string
  clientRequestId: string
}

export type Provider = {
  name: string
  mode: 'MOCK' | 'REAL_OPENAI_COMPATIBLE' | 'FALLBACK'
  model?: string | null
}

export type BusinessFacts = {
  question: string
  productId: number
  productCode: string | null
  productName: string
  productStatus: string
  productImagePath: string | null
  skuId: number
  skuCode: string
  color: string
  size: string
  skuStatus: string
  unitPrice: string
  currency: string
  availableStock: number
  knowledgeSnippets: string[]
  queriedAt: string
}

export type Evidence = {
  evidenceType: string
  field: string
  displayName: string
  value: string
  sourceType: 'PRODUCT' | 'SKU' | 'INVENTORY'
  sourceId: number
}

export type TraceStep = {
  step: 'REQUEST_RECEIVED' | 'BUSINESS_FACTS_LOADED' | 'PYTHON_REQUEST_SENT' | 'PROVIDER_COMPLETED' | 'RESPONSE_VALIDATED' | 'RESPONSE_RETURNED'
  status: 'COMPLETED' | 'FALLBACK' | 'ERROR' | string
  displayName: string
  durationMs: number
  detail: string
}

export type CustomerServiceAnswer = {
  traceId: string
  clientRequestId: string
  answer: string
  answerStatus: 'ANSWERED' | 'UNSUPPORTED_QUESTION' | 'INSUFFICIENT_CONTEXT' | 'PROVIDER_ERROR' | 'FALLBACK_ANSWER'
  provider: Provider
  evidence: Evidence[]
  businessFacts: BusinessFacts
  trace: TraceStep[]
  latencyMs: number
  fallbackUsed: boolean
  warning: string | null
  createdAt: string
}

export type RateLimitMetadata = {
  mode: 'redis' | 'degraded' | 'disabled'
  limit: number | null
  remaining: number | null
  resetEpochSeconds: number | null
}

export type AskCustomerServiceResult = {
  answer: CustomerServiceAnswer
  rateLimit: RateLimitMetadata
}

export class AiRequestError extends Error {
  constructor(
    message: string,
    public readonly status: number,
    public readonly code: string | null,
    public readonly retryAfterSeconds: number | null,
    public readonly rateLimit: RateLimitMetadata,
  ) {
    super(message)
  }
}

export type SkuFilters = {
  query: string
  saleStatus: 'all' | 'on-sale'
  stock: 'all' | StockState
}

let fallbackCounter = 0

export function flattenSkus(products: Product[]): SelectedSku[] {
  return products.flatMap((product) => product.skus.map((sku) => ({ product, sku })))
}

export function filterSkus(selections: SelectedSku[], filters: SkuFilters): SelectedSku[] {
  const query = filters.query.trim().toLocaleLowerCase()
  return selections.filter(({ product, sku }) => {
    const matchesQuery = !query
      || product.name.toLocaleLowerCase().includes(query)
      || sku.skuCode.toLocaleLowerCase().includes(query)
    const matchesSaleStatus = filters.saleStatus === 'all' || product.status === 'ON_SALE'
    const matchesStock = filters.stock === 'all' || stockState(sku.availableStock) === filters.stock
    return matchesQuery && matchesSaleStatus && matchesStock
  })
}

export function preferredSku(selections: SelectedSku[]): SelectedSku | null {
  return selections.find(({ sku }) => sku.id === 10004) ?? selections[0] ?? null
}

export function productStatusLabel(status: string): string {
  return status === 'ON_SALE' ? '在售' : '非在售'
}

export function availabilityLabel(selection: SelectedSku): string {
  if (selection.product.status !== 'ON_SALE') return '非在售'
  if (selection.sku.availableStock === 0) return '暂无库存'
  return stockLabel(selection.sku.availableStock)
}

export function aiMoney(amount: string | number, currency: string): string {
  return formatMoney(Number(amount), currency)
}

export function createClientRequestId(): string {
  if (typeof globalThis.crypto?.randomUUID === 'function') {
    return `p4c-${globalThis.crypto.randomUUID()}`
  }

  if (typeof globalThis.crypto?.getRandomValues === 'function') {
    const values = new Uint32Array(4)
    globalThis.crypto.getRandomValues(values)
    return `p4c-${Array.from(values, (value) => value.toString(16)).join('-')}`
  }

  fallbackCounter += 1
  // This correlation id is not an authorization secret or idempotency key.
  return `p4c-local-${Date.now().toString(36)}-${fallbackCounter}`
}

function readIntegerHeader(response: Response, name: string): number | null {
  const raw = response.headers.get(name)
  if (!raw || !/^\d+$/.test(raw)) return null
  return Number(raw)
}

export function readRateLimitMetadata(response: Response): RateLimitMetadata {
  const mode = response.headers.get('X-RateLimit-Mode')
  return {
    mode: mode === 'redis' || mode === 'degraded' || mode === 'disabled' ? mode : 'disabled',
    limit: readIntegerHeader(response, 'X-RateLimit-Limit'),
    remaining: readIntegerHeader(response, 'X-RateLimit-Remaining'),
    resetEpochSeconds: readIntegerHeader(response, 'X-RateLimit-Reset'),
  }
}

async function responseError(response: Response): Promise<AiRequestError> {
  const body = await response.json().catch(() => null) as { code?: unknown; message?: unknown; retryAfterSeconds?: unknown } | null
  const retryAfterSeconds = typeof body?.retryAfterSeconds === 'number' ? body.retryAfterSeconds : readIntegerHeader(response, 'Retry-After')
  const message = typeof body?.message === 'string' ? body.message : `请求失败（HTTP ${response.status}）`
  return new AiRequestError(message, response.status, typeof body?.code === 'string' ? body.code : null, retryAfterSeconds, readRateLimitMetadata(response))
}

export async function requestProducts(): Promise<Product[]> {
  const response = await fetch(`${API_BASE}/products`)
  if (!response.ok) throw await responseError(response)
  return response.json() as Promise<Product[]>
}

export async function askCustomerService(request: AskRequest): Promise<AskCustomerServiceResult> {
  const response = await fetch(`${API_BASE}/ai/customer-service/ask`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify(request),
  })
  if (!response.ok) throw await responseError(response)
  return { answer: await response.json() as CustomerServiceAnswer, rateLimit: readRateLimitMetadata(response) }
}

export function traceStepLabel(step: TraceStep['step'], displayName: string): string {
  const labels: Record<TraceStep['step'], string> = {
    REQUEST_RECEIVED: '接收并校验请求',
    BUSINESS_FACTS_LOADED: '加载商品业务事实',
    PYTHON_REQUEST_SENT: '调用本地 AI 服务',
    PROVIDER_COMPLETED: 'Provider 返回结果',
    RESPONSE_VALIDATED: '校验结构化响应',
    RESPONSE_RETURNED: '组装答案与证据',
  }
  return labels[step] || displayName
}

export function statusLabel(status: CustomerServiceAnswer['answerStatus']): string {
  const labels: Record<CustomerServiceAnswer['answerStatus'], string> = {
    ANSWERED: '已回答',
    UNSUPPORTED_QUESTION: '暂不支持的问题',
    INSUFFICIENT_CONTEXT: '事实不足',
    PROVIDER_ERROR: 'Provider 错误',
    FALLBACK_ANSWER: '安全降级回答',
  }
  return labels[status]
}
