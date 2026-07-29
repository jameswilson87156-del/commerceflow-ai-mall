import { flushPromises, mount } from '@vue/test-utils'
import { afterEach, describe, expect, it, vi } from 'vitest'
import OperationsOverview from './OperationsOverview.vue'

const overview = {
  summary: { productCount: 2, skuCount: 5, onSaleProductCount: 2, availableStockTotal: 434, lowStockSkuCount: 2, createdOrderCount: 1, createdOrderAmount: 328, aiInteractionCount: 2 },
  recentOrders: [{ orderNo: 'CF100', status: 'CREATED', totalAmount: 328, currency: 'CNY', itemCount: 2, createdAt: '2026-07-29T00:00:00Z' }],
  lowStockSkus: [{ skuId: 4, skuCode: 'TSHIRT-NAVY-L', productName: '轻盈棉质基础T恤', color: '藏青色', size: 'L', availableStock: 0, stockLevel: 'OUT_OF_STOCK' }],
  aiSummary: { provider: 'commerceflow-mock', providerMode: 'MOCK', interactionCount: 2, answeredCount: 1, unsupportedCount: 0, fallbackCount: 1, providerErrorCount: 0, latestInteractionAt: '2026-07-29T00:00:00Z' },
  runtimeBoundary: { dataScope: 'LOCAL_SHOWCASE', authenticationMode: 'DEMO_USER', aiMode: 'MOCK', orderStatusScope: 'CREATED_ONLY', rateLimitAlgorithm: 'REDIS_LUA_FIXED_WINDOW', rateLimitLimit: 5, rateLimitWindowSeconds: 60, rateLimitFailurePolicy: 'FAIL_OPEN', mobileRuntime: 'H5_VERIFIED', nonH5Runtime: 'COMPILE_ONLY' },
  generatedAt: '2026-07-29T00:00:00Z',
}

afterEach(() => vi.unstubAllGlobals())
describe('OperationsOverview', () => {
  it('renders only real overview facts and explicit runtime boundaries', async () => {
    vi.stubGlobal('fetch', vi.fn(() => Promise.resolve(new Response(JSON.stringify(overview), { status: 200 }))))
    const wrapper = mount(OperationsOverview)
    await flushPromises()
    expect(wrapper.text()).toContain('本地订单金额')
    expect(wrapper.text()).toContain('¥328.00')
    expect(wrapper.text()).toContain('commerceflow-mock / MOCK')
    expect(wrapper.text()).toContain('H5_VERIFIED')
    expect(wrapper.text()).not.toContain('GMV')
  })

  it('shows error and retries the real endpoint', async () => {
    const fetch = vi.fn().mockResolvedValueOnce(new Response('{}', { status: 503 })).mockResolvedValueOnce(new Response(JSON.stringify(overview), { status: 200 }))
    vi.stubGlobal('fetch', fetch)
    const wrapper = mount(OperationsOverview)
    await flushPromises()
    expect(wrapper.find('[data-testid="operations-error"]').exists()).toBe(true)
    await wrapper.get('button').trigger('click')
    await flushPromises()
    expect(wrapper.find('[data-testid="operations-summary"]').exists()).toBe(true)
    expect(fetch).toHaveBeenCalledTimes(2)
  })
})
