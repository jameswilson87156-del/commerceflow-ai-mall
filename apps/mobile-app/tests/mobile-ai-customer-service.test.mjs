import test from 'node:test'
import assert from 'node:assert/strict'
import {
  MAX_QUESTION_LENGTH,
  cooldownSeconds,
  clearQuestionAfterSuccess,
  createAskPayload,
  createClientRequestId,
  hasRedisQuota,
  isFailOpen,
  isSupportedQuestion,
  nextCooldown,
  pendingQuestionForAttempt,
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
