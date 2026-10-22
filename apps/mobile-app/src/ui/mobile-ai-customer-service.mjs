export const MAX_QUESTION_LENGTH = 500

export function canSubmitQuestion({ pageState, hasSku, sending, cooldownRemaining }) {
  return pageState === 'ready' && Boolean(hasSku) && !sending && Number(cooldownRemaining || 0) === 0
}

export function isCurrentRequestActive(pageActive, requestId, currentRequestId) {
  return Boolean(pageActive) && requestId === currentRequestId
}

export function normalizeCustomerServiceAnswer(value) {
  const answer = value && typeof value === 'object' ? value : null
  const provider = answer?.provider && typeof answer.provider === 'object' ? answer.provider : null
  if (
    !answer
    || typeof answer.traceId !== 'string' || !answer.traceId.trim()
    || typeof answer.answer !== 'string' || !answer.answer.trim()
    || typeof answer.answerStatus !== 'string' || !answer.answerStatus.trim()
    || typeof provider?.name !== 'string' || !provider.name.trim()
    || typeof provider?.mode !== 'string' || !provider.mode.trim()
    || !Array.isArray(answer.evidence)
    || !answer.businessFacts || typeof answer.businessFacts !== 'object'
    || !Array.isArray(answer.trace)
  ) {
    throw new Error('AI 客服暂未返回有效回答，请重试。')
  }
  return answer
}

export function isCustomerServiceTimeout(error) {
  const message = typeof error?.message === 'string' ? error.message : String(error || '')
  return /timeout|timed\s*out|超时/i.test(message)
}

export function customerServiceFailureMessage(error, fallbackMessage) {
  if (isCustomerServiceTimeout(error)) return '请求超时，尚未收到客服回答，请稍后重试。'
  if (Number(error?.statusCode) >= 500) return '商品客服服务暂时不可用，请稍后重试。'
  return fallbackMessage || '商品客服服务暂时不可用，请稍后重试。'
}

export function createClientRequestId(randomUuid) {
  const uuid = typeof randomUuid === 'function' ? randomUuid() : ''
  if (typeof uuid === 'string' && /^[A-Za-z0-9._-]{1,80}$/.test(uuid)) return `mobile-${uuid}`
  return `mobile-${Date.now()}-${Math.random().toString(36).slice(2, 12)}`
}

export function createAskPayload({ productId, skuId, question, clientRequestId }) {
  return {
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
