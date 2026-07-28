import { request } from './runtime'
import type { RateLimitMeta } from './types'

export interface CustomerServiceAskRequest {
  userId: number
  productId: number
  skuId: number
  question: string
  clientRequestId: string
}

export interface AiProvider {
  name: string
  mode: 'MOCK' | 'FALLBACK' | 'REAL_OPENAI_COMPATIBLE' | string
  model?: string | null
}

export interface AiEvidence {
  evidenceType: string
  field: string
  displayName: string
  value: string
  sourceType: string
  sourceId: number
}

export interface AiBusinessFacts {
  productId: number
  productCode: string
  productName: string
  productStatus: string
  productImagePath?: string | null
  skuId: number
  skuCode: string
  color: string
  size: string
  skuStatus: string
  unitPrice: string
  currency: string
  availableStock: number
  queriedAt: string
}

export interface AiTraceStep {
  step: string
  status: string
  displayName: string
  durationMs: number
  detail: string
}

export interface CustomerServiceAnswer {
  traceId: string
  clientRequestId: string
  answer: string
  answerStatus: string
  provider: AiProvider
  evidence: AiEvidence[]
  businessFacts: AiBusinessFacts
  trace: AiTraceStep[]
  latencyMs: number
  fallbackUsed: boolean
  warning?: string | null
  createdAt: string
}

export interface AiRequestResult {
  answer: CustomerServiceAnswer
  rateLimit: RateLimitMeta
}

export async function askCustomerService(payload: CustomerServiceAskRequest): Promise<AiRequestResult> {
  const response = await request<CustomerServiceAnswer>('/ai/customer-service/ask', { method: 'POST', data: payload })
  return {
    answer: response.data,
    rateLimit: {
      mode: response.headers['x-ratelimit-mode'],
      limit: numberHeader(response.headers['x-ratelimit-limit']),
      remaining: numberHeader(response.headers['x-ratelimit-remaining']),
      reset: numberHeader(response.headers['x-ratelimit-reset']),
      retryAfter: numberHeader(response.headers['retry-after'])
    }
  }
}

function numberHeader(value?: string) {
  const parsed = Number(value)
  return Number.isFinite(parsed) ? parsed : undefined
}
