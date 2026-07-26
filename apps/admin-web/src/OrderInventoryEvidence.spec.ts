import { flushPromises, mount } from '@vue/test-utils'
import { afterEach, describe, expect, it, vi } from 'vitest'
import OrderInventoryEvidence from './OrderInventoryEvidence.vue'

const orders = [
  { orderNo: 'CF1001', userId: 1, totalAmount: 129, currency: 'CNY', status: 'CREATED', createdAt: '2026-07-26T10:00:00', items: [] },
  { orderNo: 'CF1002', userId: 1, totalAmount: 199, currency: 'CNY', status: 'CREATED', createdAt: '2026-07-26T10:02:00', items: [] },
]

const evidence = {
  orderNo: 'CF1001', userId: 1, totalAmount: 129, currency: 'CNY', status: 'CREATED', createdAt: '2026-07-26T10:00:00',
  idempotencyKey: 'p3-real-key-1', requestResult: 'FIRST_CREATED', firstCreation: true, idempotencyReplay: false,
  items: [{ productNameSnapshot: '轻盈棉质基础 T 恤', skuCodeSnapshot: 'T-SHIRT-WHITE-S', colorSnapshot: '白色', sizeSnapshot: 'S', unitPrice: 129, quantity: 1, subtotal: 129 }],
  inventoryMovements: [{ movementId: 1, skuId: 10002, skuCode: 'T-SHIRT-WHITE-S', movementType: 'ORDER_DEDUCT', quantity: 1, stockBefore: 182, stockAfter: 181, createdAt: '2026-07-26T10:00:00' }],
}

function jsonResponse(body: unknown, status = 200) {
  return new Response(JSON.stringify(body), { status, headers: { 'Content-Type': 'application/json' } })
}

function readyFetch() {
  return vi.fn()
    .mockResolvedValueOnce(jsonResponse(orders))
    .mockResolvedValueOnce(jsonResponse(orders[0]))
    .mockResolvedValueOnce(jsonResponse(evidence))
}

afterEach(() => vi.unstubAllGlobals())

describe('OrderInventoryEvidence', () => {
  it('renders loading before the real order list resolves', () => {
    vi.stubGlobal('fetch', vi.fn(() => new Promise(() => {})))
    const wrapper = mount(OrderInventoryEvidence)
    expect(wrapper.get('[data-testid="order-loading"]').text()).toContain('正在加载真实订单')
  })

  it('renders empty state when the order API returns no created orders', async () => {
    vi.stubGlobal('fetch', vi.fn().mockResolvedValue(jsonResponse([])))
    const wrapper = mount(OrderInventoryEvidence)
    await flushPromises()
    expect(wrapper.get('[data-testid="order-empty"]').text()).toContain('当前用户没有已创建订单')
  })

  it('renders an error state and retries the order API', async () => {
    const fetchMock = readyFetch().mockRejectedValueOnce(new Error('unused'))
    fetchMock.mockReset()
    fetchMock.mockResolvedValueOnce(jsonResponse({}, 500))
      .mockResolvedValueOnce(jsonResponse(orders))
      .mockResolvedValueOnce(jsonResponse(orders[0]))
      .mockResolvedValueOnce(jsonResponse(evidence))
    vi.stubGlobal('fetch', fetchMock)
    const wrapper = mount(OrderInventoryEvidence)
    await flushPromises()
    expect(wrapper.get('[data-testid="order-error"]').text()).toContain('订单列表加载失败')
    await wrapper.get('[data-testid="order-error"] button').trigger('click')
    await flushPromises()
    expect(wrapper.text()).toContain('CF1001')
  })

  it('renders real item, movement, and idempotency evidence', async () => {
    vi.stubGlobal('fetch', readyFetch())
    const wrapper = mount(OrderInventoryEvidence)
    await flushPromises()
    expect(wrapper.text()).toContain('轻盈棉质基础 T 恤')
    expect(wrapper.text()).toContain('T-SHIRT-WHITE-S')
    expect(wrapper.text()).toContain('182')
    expect(wrapper.text()).toContain('181')
    expect(wrapper.text()).toContain('p3-real-key-1')
    expect(wrapper.text()).toContain('首次创建')
  })

  it('filters loaded order numbers and requests a newly selected order', async () => {
    const secondEvidence = { ...evidence, orderNo: 'CF1002', totalAmount: 199, idempotencyKey: 'p3-real-key-2' }
    const fetchMock = readyFetch()
      .mockResolvedValueOnce(jsonResponse(orders[1]))
      .mockResolvedValueOnce(jsonResponse(secondEvidence))
    vi.stubGlobal('fetch', fetchMock)
    const wrapper = mount(OrderInventoryEvidence)
    await flushPromises()
    await wrapper.get('[data-testid="order-search"]').setValue('CF1002')
    expect(wrapper.find('[data-testid="order-CF1001"]').exists()).toBe(false)
    await wrapper.get('[data-testid="order-CF1002"]').trigger('click')
    await flushPromises()
    expect(wrapper.text()).toContain('p3-real-key-2')
    expect(fetchMock).toHaveBeenCalledWith(expect.stringContaining('/orders/CF1002/execution-evidence'))
  })
})
