import { flushPromises, mount } from '@vue/test-utils'
import { afterEach, describe, expect, it, vi } from 'vitest'
import AiCustomerServiceWorkbench from './AiCustomerServiceWorkbench.vue'
import {
  askCustomerService,
  createClientRequestId,
  filterSkus,
  flattenSkus,
  preferredSku,
  type CustomerServiceAnswer,
} from './aiCustomerService'

const products = [
  {
    id: 101,
    productCode: 'PROD-1001',
    name: '轻盈棉质基础 T 恤',
    description: '柔软棉质基础短袖。',
    categoryName: '日常服饰',
    status: 'ON_SALE',
    coverImagePath: '/assets/products/product-tshirt-gray.png',
    skus: [
      { id: 10001, skuCode: 'T-SHIRT-BLACK-M', color: '黑色', size: 'M', salePrice: 129, currency: 'CNY', availableStock: 96, imagePath: '/assets/products/product-tshirt-black.png' },
      { id: 10004, skuCode: 'T-SHIRT-GRAY-L', color: '灰色', size: 'L', salePrice: 129, currency: 'CNY', availableStock: 28, imagePath: '/assets/products/product-tshirt-gray.png' },
      { id: 10005, skuCode: 'T-SHIRT-NAVY-XL', color: '藏青色', size: 'XL', salePrice: 129, currency: 'CNY', availableStock: 0, imagePath: '/assets/products/product-tshirt-navy.png' },
    ],
  },
  {
    id: 102,
    productCode: 'PROD-1002',
    name: '简约通勤托特包',
    description: '简洁轻便的通勤托特包。',
    categoryName: '通勤配件',
    status: 'ON_SALE',
    coverImagePath: '/assets/products/product-tote-beige.png',
    skus: [
      { id: 10003, skuCode: 'TOTE-BEIGE-ONE', color: '米色', size: 'One Size', salePrice: 199, currency: 'CNY', availableStock: 128, imagePath: '/assets/products/product-tote-beige.png' },
    ],
  },
]

function jsonResponse(body: unknown, status = 200, headers: Record<string, string> = {}) {
  return new Response(JSON.stringify(body), { status, headers: { 'Content-Type': 'application/json', ...headers } })
}

function answer(overrides: Partial<CustomerServiceAnswer> = {}): CustomerServiceAnswer {
  return {
    traceId: 'trace-p4c-001',
    clientRequestId: 'p4c-test-001',
    answer: '灰色 L 码当前库存为 28 件，可以购买，售价为 ¥129.00。',
    answerStatus: 'ANSWERED',
    provider: { name: 'commerceflow-mock', mode: 'MOCK', model: null },
    evidence: [
      { evidenceType: 'PRODUCT_FACT', field: 'productName', displayName: '商品名称', value: '轻盈棉质基础 T 恤', sourceType: 'PRODUCT', sourceId: 101 },
      { evidenceType: 'SKU_FACT', field: 'skuCode', displayName: 'SKU 编码', value: 'T-SHIRT-GRAY-L', sourceType: 'SKU', sourceId: 10004 },
      { evidenceType: 'INVENTORY_FACT', field: 'availableStock', displayName: '当前库存', value: '28', sourceType: 'INVENTORY', sourceId: 10004 },
    ],
    businessFacts: {
      question: '这件灰色 L 码 T 恤现在还有库存吗？', productId: 101, productCode: 'PROD-1001', productName: '轻盈棉质基础 T 恤', productStatus: 'ON_SALE', productImagePath: '/assets/products/product-tshirt-gray.png', skuId: 10004, skuCode: 'T-SHIRT-GRAY-L', color: '灰色', size: 'L', skuStatus: 'ON_SALE', unitPrice: '129.00', currency: 'CNY', availableStock: 28, knowledgeSnippets: [], queriedAt: '2026-07-26T09:00:00Z',
    },
    trace: [
      { step: 'REQUEST_RECEIVED', status: 'COMPLETED', displayName: '已接收请求', durationMs: 0, detail: '已校验请求参数' },
      { step: 'BUSINESS_FACTS_LOADED', status: 'COMPLETED', displayName: '已加载商品事实', durationMs: 0, detail: '来自 Product、SKU 与 Inventory' },
      { step: 'PYTHON_REQUEST_SENT', status: 'COMPLETED', displayName: '已调用 AI 服务', durationMs: 2, detail: '发送受限 businessFacts' },
      { step: 'PROVIDER_COMPLETED', status: 'COMPLETED', displayName: 'AI 服务已完成', durationMs: 6, detail: '已收到结构化回答' },
      { step: 'RESPONSE_VALIDATED', status: 'COMPLETED', displayName: '已校验 AI 响应', durationMs: 0, detail: 'Java 已校验结果' },
      { step: 'RESPONSE_RETURNED', status: 'COMPLETED', displayName: '已返回结果', durationMs: 0, detail: '已附加 Java Evidence 与 Trace' },
    ],
    latencyMs: 18,
    fallbackUsed: false,
    warning: null,
    createdAt: '2026-07-26T09:00:01Z',
    ...overrides,
  }
}

function apiMock(answerResponse = answer()) {
  return vi.fn(async (input: RequestInfo | URL, init?: RequestInit) => {
    const url = String(input)
    if (url.endsWith('/products')) return jsonResponse(products)
    if (url.endsWith('/ai/customer-service/ask') && init?.method === 'POST') return jsonResponse(answerResponse)
    return jsonResponse({}, 404)
  })
}

afterEach(() => {
  vi.useRealTimers()
  vi.unstubAllGlobals()
})

describe('AI customer service workbench', () => {
  it('shows catalog loading before the real product request resolves', () => {
    vi.stubGlobal('fetch', vi.fn(() => new Promise(() => {})))
    const wrapper = mount(AiCustomerServiceWorkbench)
    expect(wrapper.get('[data-testid="ai-catalog-loading"]').text()).toContain('正在加载真实商品与 SKU')
  })

  it('shows catalog error and retries the real product request', async () => {
    const fetchMock = vi.fn().mockResolvedValueOnce(jsonResponse({}, 500)).mockResolvedValueOnce(jsonResponse(products))
    vi.stubGlobal('fetch', fetchMock)
    const wrapper = mount(AiCustomerServiceWorkbench)
    await flushPromises()
    expect(wrapper.get('[data-testid="ai-catalog-error"]').text()).toContain('商品与 SKU 加载失败')
    await wrapper.get('[data-testid="ai-catalog-retry"]').trigger('click')
    await flushPromises()
    expect(wrapper.get('[data-testid="ai-sku-10004"]').classes()).toContain('selected')
    expect(fetchMock).toHaveBeenCalledTimes(2)
  })

  it('shows catalog empty without inventing a SKU', async () => {
    vi.stubGlobal('fetch', vi.fn().mockResolvedValue(jsonResponse([])))
    const wrapper = mount(AiCustomerServiceWorkbench)
    await flushPromises()
    expect(wrapper.get('[data-testid="ai-catalog-empty"]').text()).toContain('不会填充示例商品')
  })

  it('renders API image paths and defaults to real gray L SKU 10004', async () => {
    vi.stubGlobal('fetch', apiMock())
    const wrapper = mount(AiCustomerServiceWorkbench)
    await flushPromises()
    const selected = wrapper.get('[data-testid="ai-sku-10004"]')
    expect(selected.classes()).toContain('selected')
    expect(selected.get('img').attributes('src')).toBe('/assets/products/product-tshirt-gray.png')
    expect(wrapper.get('[data-testid="ai-sku-10005"]').text()).toContain('暂无库存')
  })

  it('filters real SKU cards by product name, SKU code, and inventory state', async () => {
    vi.stubGlobal('fetch', apiMock())
    const wrapper = mount(AiCustomerServiceWorkbench)
    await flushPromises()
    await wrapper.get('[data-testid="ai-search-input"]').setValue('TOTE-BEIGE-ONE')
    expect(wrapper.findAll('.ai-sku-card')).toHaveLength(1)
    await wrapper.get('[data-testid="ai-search-input"]').setValue('')
    await wrapper.get('[data-testid="ai-stock-filter"]').setValue('out')
    expect(wrapper.findAll('.ai-sku-card')).toHaveLength(1)
    expect(wrapper.get('[data-testid="ai-sku-10005"]').text()).toContain('暂无库存')
  })

  it('fills a quick question without fabricating a response', async () => {
    vi.stubGlobal('fetch', apiMock())
    const wrapper = mount(AiCustomerServiceWorkbench)
    await flushPromises()
    await wrapper.get('.quick-question-list button').trigger('click')
    expect((wrapper.get('[data-testid="ai-question-input"]').element as HTMLTextAreaElement).value).toBe('这个商品多少钱？')
    expect(wrapper.find('[data-testid="ai-welcome"]').exists()).toBe(true)
  })

  it('rejects blank and overlength questions before calling the API', async () => {
    const fetchMock = apiMock()
    vi.stubGlobal('fetch', fetchMock)
    const wrapper = mount(AiCustomerServiceWorkbench)
    await flushPromises()
    await wrapper.get('[data-testid="ai-send-button"]').trigger('submit')
    expect(fetchMock).toHaveBeenCalledTimes(1)
    await wrapper.get('[data-testid="ai-question-input"]').setValue('x'.repeat(501))
    await wrapper.get('form').trigger('submit')
    expect(wrapper.text()).toContain('500')
    expect(fetchMock).toHaveBeenCalledTimes(1)
  })

  it('sends only the required selection and question fields to the public Java endpoint', async () => {
    const fetchMock = apiMock()
    vi.stubGlobal('fetch', fetchMock)
    const wrapper = mount(AiCustomerServiceWorkbench)
    await flushPromises()
    await wrapper.get('[data-testid="ai-question-input"]').setValue('  这件灰色 L 码 T 恤现在还有库存吗？  ')
    await wrapper.get('form').trigger('submit')
    await flushPromises()
    const [, init] = fetchMock.mock.calls[1] as [string, RequestInit]
    const body = JSON.parse(String(init.body))
    expect(body).toMatchObject({ productId: 101, skuId: 10004, question: '这件灰色 L 码 T 恤现在还有库存吗？' })
    expect(body).not.toHaveProperty('userId')
    expect(body.clientRequestId).toMatch(/^p4c-/)
    expect(body).not.toHaveProperty('availableStock')
    expect(body).not.toHaveProperty('salePrice')
    expect(body).not.toHaveProperty('status')
  })

  it('renders an answered response with actual provider, Evidence, and six trace steps', async () => {
    vi.stubGlobal('fetch', apiMock())
    const wrapper = mount(AiCustomerServiceWorkbench)
    await flushPromises()
    await wrapper.get('[data-testid="ai-question-input"]').setValue('库存还有吗？')
    await wrapper.get('form').trigger('submit')
    await flushPromises()
    expect(wrapper.text()).toContain('commerceflow-mock')
    expect(wrapper.text()).toContain('MOCK')
    expect(wrapper.text()).toContain('本地确定性 Mock，未调用外部大模型')
    expect(wrapper.text()).toContain('当前库存')
    expect(wrapper.findAll('.ai-trace-list li')).toHaveLength(6)
  })

  it('keeps the three factual panels mounted and makes long technical ids shrink inside their own panel', async () => {
    const longTraceId = 'trace-p4e-6f3b1d6f-09d4-4c7d-9d7d-31c8c9ad0d67-with-extra-diagnostic-context'
    vi.stubGlobal('fetch', apiMock(answer({ traceId: longTraceId })))
    const wrapper = mount(AiCustomerServiceWorkbench)
    await flushPromises()
    await wrapper.get('[data-testid="ai-question-input"]').setValue('这件灰色L码T恤现在还有库存吗？')
    await wrapper.get('form').trigger('submit')
    await flushPromises()

    expect(wrapper.get('[data-testid="ai-three-column-layout"]').classes()).toContain('ai-workbench-layout')
    expect(wrapper.get('[data-testid="ai-sku-selector-panel"]').text()).toContain('商品与 SKU')
    expect(wrapper.get('[data-testid="ai-chat-panel"]').text()).toContain('AI 客服工作区')
    expect(wrapper.get('[data-testid="ai-facts-evidence-panel"]').text()).toContain('事实证据')
    const traceId = wrapper.get('.ai-trace-id')
    expect(traceId.attributes('title')).toBe(longTraceId)
    expect(traceId.classes()).toContain('ai-trace-id')
  })

  it('keeps a long customer question as normal text content without adding a hard-coded answer', async () => {
    const longQuestion = '这件灰色L码T恤现在还有库存吗？我还想确认当前展示的库存是否来自本地真实商品接口。'
    vi.stubGlobal('fetch', apiMock())
    const wrapper = mount(AiCustomerServiceWorkbench)
    await flushPromises()
    await wrapper.get('[data-testid="ai-question-input"]').setValue(longQuestion)
    await wrapper.get('form').trigger('submit')
    await flushPromises()

    expect(wrapper.get('.ai-message.user .ai-message-text').text()).toBe(longQuestion)
    expect(wrapper.get('.ai-message.assistant .ai-message-text').text()).toContain('库存为 28 件')
    expect(wrapper.get('[data-testid="ai-facts-evidence-panel"]').findAll('.ai-trace-list li')).toHaveLength(6)
  })

  it('renders unsupported and fallback results as distinct response-driven boundaries', async () => {
    const unsupported = answer({ answerStatus: 'UNSUPPORTED_QUESTION', answer: '当前商品客服暂不支持发货问题。' })
    vi.stubGlobal('fetch', apiMock(unsupported))
    const wrapper = mount(AiCustomerServiceWorkbench)
    await flushPromises()
    await wrapper.get('[data-testid="ai-question-input"]').setValue('什么时候发货？')
    await wrapper.get('form').trigger('submit')
    await flushPromises()
    expect(wrapper.text()).toContain('当前仅支持商品价格、规格、库存、SKU 和可购买状态问题。')
    expect(wrapper.text()).toContain('暂不支持的问题')

    const fallback = answer({ answerStatus: 'FALLBACK_ANSWER', provider: { name: 'java-fact-fallback', mode: 'FALLBACK', model: null }, fallbackUsed: true, warning: 'AI 服务暂时不可用' })
    vi.stubGlobal('fetch', apiMock(fallback))
    const fallbackWrapper = mount(AiCustomerServiceWorkbench)
    await flushPromises()
    await fallbackWrapper.get('[data-testid="ai-question-input"]').setValue('库存还有吗？')
    await fallbackWrapper.get('form').trigger('submit')
    await flushPromises()
    expect(fallbackWrapper.text()).toContain('安全降级回答')
    expect(fallbackWrapper.text()).toContain('java-fact-fallback')
  })

  it('renders unsupported Java fallback with its returned warning instead of a stock answer', async () => {
    const unsupportedFallback = answer({
      answer: '当前本地商品事实客服只支持价格、颜色尺码、库存、是否可购买和 SKU 编码问题；暂不支持该问题。',
      answerStatus: 'UNSUPPORTED_QUESTION',
      provider: { name: 'java-fact-fallback', mode: 'FALLBACK', model: null },
      fallbackUsed: true,
      warning: 'AI 服务暂时不可用。该问题不属于本地商品事实客服支持范围。',
      trace: answer().trace.map((step) => step.step === 'PROVIDER_COMPLETED'
        ? { ...step, status: 'FALLBACK', durationMs: 17, detail: 'AI_SERVICE_UNAVAILABLE' }
        : step),
    })
    vi.stubGlobal('fetch', apiMock(unsupportedFallback))
    const wrapper = mount(AiCustomerServiceWorkbench)
    await flushPromises()
    await wrapper.get('[data-testid="ai-question-input"]').setValue('什么时候发货？')
    await wrapper.get('form').trigger('submit')
    await flushPromises()

    expect(wrapper.text()).toContain('暂不支持的问题')
    expect(wrapper.text()).toContain('AI 服务暂时不可用。该问题不属于本地商品事实客服支持范围。')
    expect(wrapper.text()).not.toContain('当前库存为 28 件')
    expect(wrapper.get('[data-testid="ai-trace-PROVIDER_COMPLETED"]').classes()).toContain('amber')
  })

  it('renders the backend-provided trace durations without replacing them with fixed values', async () => {
    const dynamicTrace = answer().trace.map((step, index) => ({ ...step, durationMs: (index + 1) * 13 }))
    vi.stubGlobal('fetch', apiMock(answer({ trace: dynamicTrace, latencyMs: 91 })))
    const wrapper = mount(AiCustomerServiceWorkbench)
    await flushPromises()
    await wrapper.get('[data-testid="ai-question-input"]').setValue('库存还有吗？')
    await wrapper.get('form').trigger('submit')
    await flushPromises()

    expect(wrapper.get('[data-testid="ai-trace-PROVIDER_COMPLETED"]').text()).toContain('52 ms')
    expect(wrapper.get('[data-testid="ai-trace-RESPONSE_RETURNED"]').text()).toContain('78 ms')
    expect(wrapper.text()).toContain('91 ms')
  })

  it('keeps the original question and provides retry after a network error', async () => {
    const fetchMock = vi.fn()
      .mockResolvedValueOnce(jsonResponse(products))
      .mockRejectedValueOnce(new Error('network down'))
      .mockResolvedValueOnce(jsonResponse(answer()))
    vi.stubGlobal('fetch', fetchMock)
    const wrapper = mount(AiCustomerServiceWorkbench)
    await flushPromises()
    await wrapper.get('[data-testid="ai-question-input"]').setValue('库存还有吗？')
    await wrapper.get('form').trigger('submit')
    await flushPromises()
    expect((wrapper.get('[data-testid="ai-question-input"]').element as HTMLTextAreaElement).value).toBe('库存还有吗？')
    await wrapper.get('.ai-message.assistant .secondary-button').trigger('click')
    await flushPromises()
    expect(wrapper.text()).toContain('commerceflow-mock')
    expect(fetchMock).toHaveBeenCalledTimes(3)
  })

  it('renders real rate-limit headers after a successful AI answer', async () => {
    const fetchMock = vi.fn()
      .mockResolvedValueOnce(jsonResponse(products))
      .mockResolvedValueOnce(jsonResponse(answer(), 200, {
        'X-RateLimit-Mode': 'redis',
        'X-RateLimit-Limit': '5',
        'X-RateLimit-Remaining': '4',
        'X-RateLimit-Reset': '1785056460',
      }))
    vi.stubGlobal('fetch', fetchMock)
    const wrapper = mount(AiCustomerServiceWorkbench)
    await flushPromises()
    await wrapper.get('[data-testid="ai-question-input"]').setValue('库存还有吗？')
    await wrapper.get('form').trigger('submit')
    await flushPromises()
    expect(wrapper.get('[data-testid="ai-rate-limit-card"]').text()).toContain('Redis 限流正常')
    expect(wrapper.get('[data-testid="ai-rate-limit-card"]').text()).toContain('限额 5 次，剩余 4 次')
    expect(wrapper.get('[data-testid="ai-rate-limit-inline"]').text()).toContain('本窗口限额 5 次')
    expect(wrapper.get('[data-testid="ai-rate-limit-inline"]').text()).toContain('剩余 4 次')
  })

  it('uses one header-first 429 cooldown for the page, blocks every send path, and reinitializes after a new 429', async () => {
    vi.useFakeTimers()
    const fetchMock = vi.fn()
      .mockResolvedValueOnce(jsonResponse(products))
      .mockResolvedValueOnce(jsonResponse({
        code: 'AI_RATE_LIMIT_EXCEEDED',
        message: '请求过于频繁，请在 46 秒后重试。',
        retryAfterSeconds: 46,
        limit: 5,
        remaining: 0,
      }, 429, {
        'Retry-After': '3',
        'X-RateLimit-Mode': 'redis',
        'X-RateLimit-Limit': '5',
        'X-RateLimit-Remaining': '0',
        'X-RateLimit-Reset': '1785056460',
      }))
      .mockResolvedValueOnce(jsonResponse({
        code: 'AI_RATE_LIMIT_EXCEEDED',
        message: '请求过于频繁，请在 99 秒后重试。',
        retryAfterSeconds: 99,
        limit: 5,
        remaining: 0,
      }, 429, {
        'Retry-After': '5',
        'X-RateLimit-Mode': 'redis',
        'X-RateLimit-Limit': '5',
        'X-RateLimit-Remaining': '0',
        'X-RateLimit-Reset': '1785056465',
      }))
    vi.stubGlobal('fetch', fetchMock)
    const wrapper = mount(AiCustomerServiceWorkbench)
    await flushPromises()
    await wrapper.get('[data-testid="ai-question-input"]').setValue('库存还有吗？')
    await wrapper.get('form').trigger('submit')
    await flushPromises()
    expect(wrapper.get('[data-testid="ai-rate-limit-error-title"]').text()).toBe('请求过于频繁')
    expect(wrapper.text()).toContain('当前请求已被 Redis 限流保护拦截，请等待倒计时结束后重试。')
    expect(wrapper.text()).not.toContain('请在 46 秒后重试。')
    expect(wrapper.get('[data-testid="ai-rate-limit-countdown"]').text()).toContain('3 秒后')
    expect(wrapper.get('[data-testid="ai-rate-limit-inline"]').text()).toContain('本窗口限额 5 次')
    expect(wrapper.get('[data-testid="ai-rate-limit-inline"]').text()).toContain('剩余 0 次')
    expect(wrapper.get('[data-testid="ai-inline-countdown"]').text()).toContain('Retry-After 3 秒')
    expect(wrapper.get('.ai-message.assistant .secondary-button').text()).toContain('3 秒后可发送')
    expect((wrapper.get('[data-testid="ai-question-input"]').element as HTMLTextAreaElement).value).toBe('库存还有吗？')
    expect(wrapper.find('.ai-message.assistant.complete').exists()).toBe(false)
    expect(wrapper.get('[data-testid="ai-send-button"]').attributes('disabled')).toBeDefined()
    const callsBeforeDisabledRetry = fetchMock.mock.calls.length
    await wrapper.get('.ai-message.assistant .secondary-button').trigger('click')
    expect(fetchMock).toHaveBeenCalledTimes(callsBeforeDisabledRetry)
    await wrapper.get('[data-testid="ai-question-input"]').trigger('keydown', { key: 'Enter' })
    expect(fetchMock).toHaveBeenCalledTimes(callsBeforeDisabledRetry)
    await vi.advanceTimersByTimeAsync(1000)
    await flushPromises()
    expect(wrapper.get('[data-testid="ai-rate-limit-countdown"]').text()).toContain('2 秒后')
    expect(wrapper.get('[data-testid="ai-inline-countdown"]').text()).toContain('Retry-After 2 秒')
    expect(wrapper.get('.ai-message.assistant .secondary-button').text()).toContain('2 秒后可发送')
    await vi.advanceTimersByTimeAsync(2000)
    await flushPromises()
    expect(wrapper.find('[data-testid="ai-rate-limit-countdown"]').exists()).toBe(false)
    expect(wrapper.get('[data-testid="ai-send-button"]').attributes('disabled')).toBeUndefined()
    await wrapper.get('form').trigger('submit')
    await flushPromises()
    expect(wrapper.get('[data-testid="ai-rate-limit-countdown"]').text()).toContain('5 秒后')
    expect(wrapper.get('[data-testid="ai-inline-countdown"]').text()).toContain('Retry-After 5 秒')
    expect(wrapper.get('.ai-message.assistant .secondary-button').text()).toContain('5 秒后可发送')
    wrapper.unmount()
  })

  it('uses the body retryAfterSeconds only when the Retry-After header is absent', async () => {
    const fetchMock = vi.fn().mockResolvedValueOnce(jsonResponse({
      code: 'AI_RATE_LIMIT_EXCEEDED',
      message: '请求过于频繁，请稍后重试。',
      retryAfterSeconds: 7,
    }, 429, {
      'X-RateLimit-Mode': 'redis',
      'X-RateLimit-Limit': '5',
      'X-RateLimit-Remaining': '0',
    }))
    vi.stubGlobal('fetch', fetchMock)
    await expect(askCustomerService({ productId: 101, skuId: 10004, question: '库存还有吗？', clientRequestId: 'p5d-body-fallback' }))
      .rejects.toMatchObject({ status: 429, retryAfterSeconds: 7 })
  })

  it('uses a one-second safe cooldown only when a 429 has neither header nor body retry metadata', async () => {
    vi.useFakeTimers()
    const fetchMock = vi.fn()
      .mockResolvedValueOnce(jsonResponse(products))
      .mockResolvedValueOnce(jsonResponse({ code: 'AI_RATE_LIMIT_EXCEEDED', message: '请求过于频繁。' }, 429, {
        'X-RateLimit-Mode': 'redis',
        'X-RateLimit-Limit': '5',
        'X-RateLimit-Remaining': '0',
      }))
    vi.stubGlobal('fetch', fetchMock)
    const wrapper = mount(AiCustomerServiceWorkbench)
    await flushPromises()
    await wrapper.get('[data-testid="ai-question-input"]').setValue('库存还有吗？')
    await wrapper.get('form').trigger('submit')
    await flushPromises()
    expect(wrapper.get('[data-testid="ai-rate-limit-countdown"]').text()).toContain('1 秒后')
    wrapper.unmount()
  })

  it('does not show a previous answer as current Provider, Evidence, or Trace after a later 429', async () => {
    vi.useFakeTimers()
    const fetchMock = vi.fn()
      .mockResolvedValueOnce(jsonResponse(products))
      .mockResolvedValueOnce(jsonResponse(answer(), 200, {
        'X-RateLimit-Mode': 'redis',
        'X-RateLimit-Limit': '5',
        'X-RateLimit-Remaining': '1',
      }))
      .mockResolvedValueOnce(jsonResponse({ code: 'AI_RATE_LIMIT_EXCEEDED', message: '请求过于频繁，请在 8 秒后重试。' }, 429, {
        'Retry-After': '3',
        'X-RateLimit-Mode': 'redis',
        'X-RateLimit-Limit': '5',
        'X-RateLimit-Remaining': '0',
      }))
    vi.stubGlobal('fetch', fetchMock)
    const wrapper = mount(AiCustomerServiceWorkbench)
    await flushPromises()
    await wrapper.get('[data-testid="ai-question-input"]').setValue('库存还有吗？')
    await wrapper.get('form').trigger('submit')
    await flushPromises()
    expect(wrapper.get('[data-testid="ai-provider-card"]').text()).toContain('commerceflow-mock')
    await wrapper.get('[data-testid="ai-question-input"]').setValue('灰色 L 码还能购买吗？')
    await wrapper.get('form').trigger('submit')
    await flushPromises()
    expect(wrapper.get('[data-testid="ai-provider-card"]').text()).toContain('等待首次请求')
    expect(wrapper.find('[data-testid="ai-evidence-empty"]').exists()).toBe(true)
    expect(wrapper.find('[data-testid="ai-trace-empty"]').exists()).toBe(true)
    wrapper.unmount()
  })

  it('clears the active cooldown interval when the workbench unmounts', async () => {
    vi.useFakeTimers()
    const fetchMock = vi.fn()
      .mockResolvedValueOnce(jsonResponse(products))
      .mockResolvedValueOnce(jsonResponse({ code: 'AI_RATE_LIMIT_EXCEEDED', message: '请求过于频繁。' }, 429, {
        'Retry-After': '4',
        'X-RateLimit-Mode': 'redis',
        'X-RateLimit-Limit': '5',
        'X-RateLimit-Remaining': '0',
      }))
    vi.stubGlobal('fetch', fetchMock)
    const wrapper = mount(AiCustomerServiceWorkbench)
    await flushPromises()
    await wrapper.get('[data-testid="ai-question-input"]').setValue('库存还有吗？')
    await wrapper.get('form').trigger('submit')
    await flushPromises()
    expect(vi.getTimerCount()).toBeGreaterThan(0)
    wrapper.unmount()
    expect(vi.getTimerCount()).toBe(0)
  })

  it('shows a safe fail-open notice without inventing remaining quota', async () => {
    const fetchMock = vi.fn()
      .mockResolvedValueOnce(jsonResponse(products))
      .mockResolvedValueOnce(jsonResponse(answer(), 200, { 'X-RateLimit-Mode': 'degraded' }))
    vi.stubGlobal('fetch', fetchMock)
    const wrapper = mount(AiCustomerServiceWorkbench)
    await flushPromises()
    await wrapper.get('[data-testid="ai-question-input"]').setValue('库存还有吗？')
    await wrapper.get('form').trigger('submit')
    await flushPromises()
    const card = wrapper.get('[data-testid="ai-rate-limit-card"]').text()
    expect(card).toContain('限流保护暂时降级')
    expect(card).not.toContain('限额 5 次')
    expect(card).not.toMatch(/剩余\s+\d+\s+次/)
    expect(wrapper.get('[data-testid="ai-rate-limit-inline"]').text()).toContain('限流保护暂时降级')
    expect(wrapper.get('[data-testid="ai-rate-limit-inline"]').text()).not.toMatch(/剩余\s+\d+\s+次/)
    expect(wrapper.text()).toContain('commerceflow-mock')
  })

  it('keeps chat history in its own scroll container without changing the window position', async () => {
    const windowScrollTo = vi.spyOn(window, 'scrollTo')
    vi.stubGlobal('fetch', apiMock())
    const wrapper = mount(AiCustomerServiceWorkbench)
    await flushPromises()
    await wrapper.get('[data-testid="ai-question-input"]').setValue('库存还有吗？')
    await wrapper.get('form').trigger('submit')
    await flushPromises()

    expect(wrapper.get('[data-testid="ai-message-list"]').classes()).toContain('ai-message-list')
    expect(windowScrollTo).not.toHaveBeenCalled()
    windowScrollTo.mockRestore()
  })

  it('clears browser-only conversation when the selected SKU changes', async () => {
    vi.stubGlobal('fetch', apiMock())
    const wrapper = mount(AiCustomerServiceWorkbench)
    await flushPromises()
    await wrapper.get('[data-testid="ai-question-input"]').setValue('库存还有吗？')
    await wrapper.get('form').trigger('submit')
    await flushPromises()
    await wrapper.get('[data-testid="ai-sku-10003"]').trigger('click')
    expect(wrapper.text()).toContain('已切换 SKU，已清空当前浏览器会话')
    expect(wrapper.find('.ai-message-list').exists()).toBe(false)
  })

  it('uses a text-only image failure fallback and renders untrusted question text safely', async () => {
    vi.stubGlobal('fetch', apiMock())
    const wrapper = mount(AiCustomerServiceWorkbench)
    await flushPromises()
    await wrapper.get('[data-testid="ai-sku-10004"] img').trigger('error')
    expect(wrapper.get('[data-testid="ai-image-placeholder"]').text()).toBe('图片不可用')
    await wrapper.get('[data-testid="ai-question-input"]').setValue('<script>alert(1)</script>库存还有吗？')
    await wrapper.get('form').trigger('submit')
    await flushPromises()
    expect(wrapper.text()).toContain('<script>alert(1)</script>库存还有吗？')
    expect(wrapper.find('script').exists()).toBe(false)
  })

  it('renders fact-limit and provider-error contracts without inventing an answer state', async () => {
    const insufficientContext = answer({
      answerStatus: 'INSUFFICIENT_CONTEXT',
      answer: '当前商品事实不足，无法生成可信回答。',
    })
    vi.stubGlobal('fetch', apiMock(insufficientContext))
    const insufficientWrapper = mount(AiCustomerServiceWorkbench)
    await flushPromises()
    await insufficientWrapper.get('[data-testid="ai-question-input"]').setValue('这件商品适合什么场景？')
    await insufficientWrapper.get('form').trigger('submit')
    await flushPromises()
    expect(insufficientWrapper.text()).toContain('事实不足')

    const providerError = answer({
      answerStatus: 'PROVIDER_ERROR',
      answer: 'Provider 未返回可用结果。',
    })
    vi.stubGlobal('fetch', apiMock(providerError))
    const providerErrorWrapper = mount(AiCustomerServiceWorkbench)
    await flushPromises()
    await providerErrorWrapper.get('[data-testid="ai-question-input"]').setValue('库存还有吗？')
    await providerErrorWrapper.get('form').trigger('submit')
    await flushPromises()
    expect(providerErrorWrapper.text()).toContain('Provider 错误')
  })

  it('prevents duplicate sends while a real public request is pending', async () => {
    let resolveAsk: (response: Response) => void = () => {}
    const fetchMock = vi.fn((input: RequestInfo | URL, init?: RequestInit) => {
      const url = String(input)
      if (url.endsWith('/products')) return Promise.resolve(jsonResponse(products))
      if (url.endsWith('/ai/customer-service/ask') && init?.method === 'POST') {
        return new Promise<Response>((resolve) => { resolveAsk = resolve })
      }
      return Promise.resolve(jsonResponse({}, 404))
    })
    vi.stubGlobal('fetch', fetchMock)
    const wrapper = mount(AiCustomerServiceWorkbench)
    await flushPromises()
    await wrapper.get('[data-testid="ai-question-input"]').setValue('库存还有吗？')
    await wrapper.get('form').trigger('submit')
    await wrapper.get('form').trigger('submit')
    expect(fetchMock).toHaveBeenCalledTimes(2)
    expect(wrapper.get('[data-testid="ai-send-button"]').attributes('disabled')).toBeDefined()
    resolveAsk(jsonResponse(answer()))
    await flushPromises()
    expect(wrapper.text()).toContain('commerceflow-mock')
  })
})

describe('AI customer service helpers', () => {
  it('keeps the default selection and filtering tied to API-returned SKU data', () => {
    const selections = flattenSkus(products)
    expect(preferredSku(selections)?.sku.id).toBe(10004)
    expect(filterSkus(selections, { query: '简约通勤托特包', saleStatus: 'all', stock: 'all' })).toHaveLength(1)
    expect(filterSkus(selections, { query: '', saleStatus: 'on-sale', stock: 'out' })[0].sku.id).toBe(10005)
  })

  it('generates a bounded client request id without exposing product facts', () => {
    const id = createClientRequestId()
    expect(id).toMatch(/^p4c-/)
    expect(id.length).toBeLessThanOrEqual(80)
  })
})
