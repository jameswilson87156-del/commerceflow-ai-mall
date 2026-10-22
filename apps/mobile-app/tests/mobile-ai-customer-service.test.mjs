import test from 'node:test'
import assert from 'node:assert/strict'
import {
  MAX_QUESTION_LENGTH,
  canSubmitQuestion,
  customerServiceFailureMessage,
  cooldownSeconds,
  clearQuestionAfterSuccess,
  createAskPayload,
  createClientRequestId,
  hasRedisQuota,
  isFailOpen,
  isCurrentRequestActive,
  isCustomerServiceTimeout,
  isSupportedQuestion,
  nextCooldown,
  pendingQuestionForAttempt,
  normalizeCustomerServiceAnswer,
  retryQuestion,
  traceLabel
} from '../src/ui/mobile-ai-customer-service.mjs'

test('请求体只提交已锁定的四个字段，不带 userId 或前端事实', () => {
  assert.deepEqual(createAskPayload({ userId: 999, productId: 101, skuId: 10004, question: '  有库存吗？ ', clientRequestId: 'mobile-test' }), {
    productId: 101, skuId: 10004, question: '有库存吗？', clientRequestId: 'mobile-test'
  })
})

test('clientRequestId 优先使用可验证的 UUID，并有安全回退', () => {
  assert.equal(createClientRequestId(() => 'abc-123'), 'mobile-abc-123')
  assert.match(createClientRequestId(() => ''), /^mobile-[A-Za-z0-9._-]+$/)
})

test('问题最大长度与空白处理保持 API 边界', () => {
  assert.equal(MAX_QUESTION_LENGTH, 500)
  assert.equal(createAskPayload({ userId: 1, productId: 1, skuId: 1, question: '   ', clientRequestId: 'x' }).question, '')
})

test('发送前保存 trim 后的待重试问题，429 后输入框可复用同一文本', () => {
  const pending = pendingQuestionForAttempt('  现在还有库存吗？  ')
  assert.equal(pending, '现在还有库存吗？')
  assert.equal(retryQuestion(pending), '现在还有库存吗？')
})

test('成功响应才清空输入与 pendingQuestion', () => {
  assert.deepEqual(clearQuestionAfterSuccess(), { question: '', pendingQuestion: '' })
})

test('空 pendingQuestion 不会成为重试请求文本', () => {
  assert.equal(retryQuestion('   '), '')
})

test('429 倒计时优先 Retry-After 响应头', () => {
  assert.equal(cooldownSeconds({ retryAfter: 35 }, { retryAfterSeconds: 46 }), 35)
})

test('429 缺响应头时回退 body，再回退安全默认值', () => {
  assert.equal(cooldownSeconds({}, { retryAfterSeconds: 46 }), 46)
  assert.equal(cooldownSeconds({}, {}), 1)
})

test('倒计时不会变为负数，并能归零恢复', () => {
  assert.equal(nextCooldown(2), 1)
  assert.equal(nextCooldown(1), 0)
  assert.equal(nextCooldown(0), 0)
})

test('只有真实 redis 响应头齐全时才展示配额数值', () => {
  assert.equal(hasRedisQuota({ mode: 'redis', limit: 5, remaining: 4, reset: 123 }), true)
  assert.equal(hasRedisQuota({ mode: 'redis', limit: 5, remaining: 4 }), false)
  assert.equal(hasRedisQuota({ mode: 'degraded', limit: 5, remaining: 4, reset: 123 }), false)
})

test('degraded 明确表示 FAIL_OPEN，且不虚构配额', () => {
  assert.equal(isFailOpen({ mode: 'degraded' }), true)
  assert.equal(isFailOpen({ mode: 'redis' }), false)
})

test('运输、支付、退款与脚本问题不作为快捷问答支持范围', () => {
  assert.equal(isSupportedQuestion('什么时候发货？'), false)
  assert.equal(isSupportedQuestion('可以退款吗？'), false)
  assert.equal(isSupportedQuestion('用 script 执行一下'), false)
  assert.equal(isSupportedQuestion('灰色 L 码还有库存吗？'), true)
})

test('Trace 仅映射已返回的六个真实链路步骤', () => {
  assert.equal(traceLabel('REQUEST_RECEIVED'), '接收请求')
  assert.equal(traceLabel('BUSINESS_FACTS_LOADED'), '加载商品事实')
  assert.equal(traceLabel('RESPONSE_RETURNED'), '组合回答与证据')
  assert.equal(traceLabel('UNKNOWN'), 'UNKNOWN')
})

test('加载、无 SKU、请求中和限流期间不能重复提交', () => {
  assert.equal(canSubmitQuestion({ pageState: 'loading', hasSku: true, sending: false, cooldownRemaining: 0 }), false)
  assert.equal(canSubmitQuestion({ pageState: 'ready', hasSku: false, sending: false, cooldownRemaining: 0 }), false)
  assert.equal(canSubmitQuestion({ pageState: 'ready', hasSku: true, sending: true, cooldownRemaining: 0 }), false)
  assert.equal(canSubmitQuestion({ pageState: 'ready', hasSku: true, sending: false, cooldownRemaining: 1 }), false)
  assert.equal(canSubmitQuestion({ pageState: 'ready', hasSku: true, sending: false, cooldownRemaining: 0 }), true)
})

test('只接受含非空回答和页面必需字段的 AI 成功响应', () => {
  const answer = {
    traceId: 'trace-1', answer: '售价 ¥129.00，库存 27 件。', answerStatus: 'ANSWERED',
    provider: { name: 'commerceflow-mock', mode: 'MOCK' }, evidence: [], businessFacts: {}, trace: []
  }
  assert.equal(normalizeCustomerServiceAnswer(answer), answer)
  assert.throws(() => normalizeCustomerServiceAnswer(null), /暂未返回有效回答/)
  assert.throws(() => normalizeCustomerServiceAnswer({ ...answer, answer: '   ' }), /暂未返回有效回答/)
  assert.throws(() => normalizeCustomerServiceAnswer({ ...answer, provider: null }), /暂未返回有效回答/)
})

test('请求超时显示可重试的识别结果', () => {
  assert.equal(isCustomerServiceTimeout(new Error('request:fail timeout')), true)
  assert.equal(isCustomerServiceTimeout(new Error('upstream timed out')), true)
  assert.equal(isCustomerServiceTimeout(new Error('服务暂不可用')), false)
  assert.equal(customerServiceFailureMessage(new Error('request:fail timeout')), '请求超时，尚未收到客服回答，请稍后重试。')
  assert.equal(customerServiceFailureMessage(new Error('服务不可用'), 'Backend Unavailable'), 'Backend Unavailable')
  const backendUnavailable = Object.assign(new Error('Backend Unavailable: TEST_ONLY_UPSTREAM_SECRET_SENTINEL'), { statusCode: 503 })
  assert.equal(customerServiceFailureMessage(backendUnavailable, 'Backend Unavailable'), '商品客服服务暂时不可用，请稍后重试。')
})

test('已卸载页面或已失效请求不会再应用异步结果', () => {
  assert.equal(isCurrentRequestActive(true, 2, 2), true)
  assert.equal(isCurrentRequestActive(false, 2, 2), false)
  assert.equal(isCurrentRequestActive(true, 1, 2), false)
})
