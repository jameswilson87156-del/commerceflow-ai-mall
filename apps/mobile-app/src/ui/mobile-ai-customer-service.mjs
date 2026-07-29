export const MAX_QUESTION_LENGTH = 500

export function createClientRequestId(randomUuid) {
  const uuid = typeof randomUuid === 'function' ? randomUuid() : ''
  if (typeof uuid === 'string' && /^[A-Za-z0-9._-]{1,80}$/.test(uuid)) return `mobile-${uuid}`
  return `mobile-${Date.now()}-${Math.random().toString(36).slice(2, 12)}`
}

export function createAskPayload({ userId, productId, skuId, question, clientRequestId }) {
  return {
    userId: Number(userId),
    productId: Number(productId),
    skuId: Number(skuId),
    question: String(question || '').trim(),
    clientRequestId
  }
}

export function pendingQuestionForAttempt(question) {
  return String(question || '').trim()
}

export function clearQuestionAfterSuccess() {
  return { question: '', pendingQuestion: '' }
}

export function retryQuestion(pendingQuestion) {
  return String(pendingQuestion || '').trim()
}

export function cooldownSeconds(rateLimit, body = {}, fallbackSeconds = 1) {
  const headerValue = Number(rateLimit?.retryAfter)
  const bodyValue = Number(body?.retryAfterSeconds)
  if (Number.isFinite(headerValue) && headerValue > 0) return Math.ceil(headerValue)
  if (Number.isFinite(bodyValue) && bodyValue > 0) return Math.ceil(bodyValue)
  return Math.max(1, Math.ceil(Number(fallbackSeconds) || 1))
}

export function nextCooldown(value) {
  return Math.max(0, Number(value || 0) - 1)
}

export function hasRedisQuota(meta) {
  return meta?.mode === 'redis'
    && Number.isFinite(meta.limit)
    && Number.isFinite(meta.remaining)
    && Number.isFinite(meta.reset)
}

export function isFailOpen(meta) {
  return meta?.mode === 'degraded'
}

export function isSupportedQuestion(question) {
  const normalized = String(question || '').toLowerCase()
  return !/(发货|物流|快递|退款|退货|支付|付款|优惠|折扣|订单|其他用户|script|javascript|system prompt|系统提示)/.test(normalized)
}

export function traceLabel(step) {
  const labels = {
    REQUEST_RECEIVED: '接收请求',
    BUSINESS_FACTS_LOADED: '加载商品事实',
    PYTHON_REQUEST_SENT: '调用本地 AI',
    PROVIDER_COMPLETED: 'Provider 返回',
    RESPONSE_VALIDATED: '校验结构化响应',
    RESPONSE_RETURNED: '组合回答与证据'
  }
  return labels[step] || step
}
