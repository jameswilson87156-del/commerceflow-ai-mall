import { flushPromises, mount } from '@vue/test-utils'
import { afterEach, describe, expect, it, vi } from 'vitest'
import AiCustomerServiceWorkbench from './AiCustomerServiceWorkbench.vue'
import {
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

function jsonResponse(body: unknown, status = 200) {
  return new Response(JSON.stringify(body), { status, headers: { 'Content-Type': 'application/json' } })
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

afterEach(() => vi.unstubAllGlobals())

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
    expect(body).toMatchObject({ userId: 1, productId: 101, skuId: 10004, question: '这件灰色 L 码 T 恤现在还有库存吗？' })
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
