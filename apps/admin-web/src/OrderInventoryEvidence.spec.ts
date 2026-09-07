import { flushPromises, mount } from '@vue/test-utils'
import { afterEach, describe, expect, it, vi } from 'vitest'
import OrderInventoryEvidence from './OrderInventoryEvidence.vue'

const grayItem = {
  productNameSnapshot: '轻盈棉质基础 T 恤', skuCodeSnapshot: 'T-SHIRT-GRAY-L', colorSnapshot: '灰色', sizeSnapshot: 'L',
  imagePathSnapshot: '/assets/products/product-tshirt-gray.png', unitPrice: 129, quantity: 1,
}
const toteItem = {
  productNameSnapshot: '简约通勤托特包', skuCodeSnapshot: 'TOTE-BEIGE-ONE', colorSnapshot: '米色', sizeSnapshot: 'One Size',
  imagePathSnapshot: '/assets/products/product-tote-beige.png', unitPrice: 199, quantity: 1,
}
const whiteItem = {
  productNameSnapshot: '轻盈棉质基础 T 恤', skuCodeSnapshot: 'T-SHIRT-WHITE-S', colorSnapshot: '白色', sizeSnapshot: 'S',
  imagePathSnapshot: '/assets/products/product-tshirt-white.png', unitPrice: 129, quantity: 1,
}
const orders = [
  { orderNo: 'CF2001', userId: 1, totalAmount: 328, currency: 'CNY', status: 'CREATED', createdAt: '2026-07-26T10:00:00', items: [grayItem, toteItem] },
  { orderNo: 'CF2002', userId: 1, totalAmount: 129, currency: 'CNY', status: 'CREATED', createdAt: '2026-07-26T10:02:00', items: [whiteItem] },
]
const evidence = {
  orderNo: 'CF2001', userId: 1, totalAmount: 328, currency: 'CNY', status: 'CREATED', createdAt: '2026-07-26T10:00:00',
  idempotencyKey: 'p31-dual-order', requestResult: 'FIRST_CREATED', firstCreation: true, idempotencyReplay: false,
  items: [{ ...grayItem, subtotal: 129 }, { ...toteItem, subtotal: 199 }],
  inventoryMovements: [
    { movementId: 1, skuId: 10004, skuCode: 'T-SHIRT-GRAY-L', movementType: 'ORDER_DEDUCT', quantity: 1, stockBefore: 28, stockAfter: 27, createdAt: '2026-07-26T10:00:00' },
    { movementId: 2, skuId: 10003, skuCode: 'TOTE-BEIGE-ONE', movementType: 'ORDER_DEDUCT', quantity: 1, stockBefore: 128, stockAfter: 127, createdAt: '2026-07-26T10:00:00' },
  ],
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
    expect(wrapper.get('[data-testid="order-empty"]').text()).toContain('当前 Operator 范围没有已创建订单')
  })

  it('renders an error state and retries the order API', async () => {
    const fetchMock = vi.fn()
      .mockResolvedValueOnce(jsonResponse({}, 500))
      .mockResolvedValueOnce(jsonResponse(orders))
      .mockResolvedValueOnce(jsonResponse(orders[0]))
      .mockResolvedValueOnce(jsonResponse(evidence))
    vi.stubGlobal('fetch', fetchMock)
    const wrapper = mount(OrderInventoryEvidence)
    await flushPromises()
    expect(wrapper.get('[data-testid="order-error"]').text()).toContain('订单列表加载失败')
    await wrapper.get('[data-testid="order-error"] button').trigger('click')
    await flushPromises()
    expect(wrapper.text()).toContain('CF2001')
  })

  it('renders list thumbnails, a two-item summary, and two real item image paths', async () => {
    vi.stubGlobal('fetch', readyFetch())
    const wrapper = mount(OrderInventoryEvidence)
    await flushPromises()

    expect(wrapper.get('[data-testid="order-thumbnail-CF2001"]').attributes('src')).toBe('/assets/products/product-tshirt-gray.png')
    expect(wrapper.get('[data-testid="order-thumbnail-CF2001-TOTE-BEIGE-ONE"]').attributes('src')).toBe('/assets/products/product-tote-beige.png')
    expect(wrapper.get('[data-testid="order-CF2001"]').text()).toContain('共 2 件')
    expect(wrapper.get('[data-testid="order-list-total"]').text()).toContain('共 2 笔服务端订单')
    expect(wrapper.get('[data-testid="order-item-image-T-SHIRT-GRAY-L"]').attributes('src')).toBe('/assets/products/product-tshirt-gray.png')
    expect(wrapper.get('[data-testid="order-item-image-TOTE-BEIGE-ONE"]').attributes('src')).toBe('/assets/products/product-tote-beige.png')
    expect(wrapper.get('[data-testid="movement-item-image-T-SHIRT-GRAY-L"]').attributes('src')).toBe('/assets/products/product-tshirt-gray.png')
    expect(wrapper.get('[data-testid="movement-item-image-TOTE-BEIGE-ONE"]').attributes('src')).toBe('/assets/products/product-tote-beige.png')
    expect(wrapper.text()).toContain('¥328.00')
    expect(wrapper.text()).toContain('首次创建')
    expect(wrapper.text()).toContain('用户 ID')
    expect(wrapper.text()).toContain('创建时间')
    expect(wrapper.text()).toContain('订单商品快照')
    expect(wrapper.text()).toContain('变动类型')
    expect(wrapper.text()).not.toContain('非重放记录')
  })

  it('updates the item image after selecting another order', async () => {
    const secondEvidence = {
      ...evidence,
      orderNo: 'CF2002', totalAmount: 129, idempotencyKey: 'p31-single-order',
      items: [{ ...whiteItem, subtotal: 129 }],
      inventoryMovements: [{ movementId: 3, skuId: 10002, skuCode: 'T-SHIRT-WHITE-S', movementType: 'ORDER_DEDUCT', quantity: 1, stockBefore: 182, stockAfter: 181, createdAt: '2026-07-26T10:02:00' }],
    }
    const fetchMock = readyFetch()
      .mockResolvedValueOnce(jsonResponse(orders[1]))
      .mockResolvedValueOnce(jsonResponse(secondEvidence))
    vi.stubGlobal('fetch', fetchMock)
    const wrapper = mount(OrderInventoryEvidence)
    await flushPromises()
    await wrapper.get('[data-testid="order-CF2002"]').trigger('click')
    await flushPromises()

    expect(wrapper.get('[data-testid="order-item-image-T-SHIRT-WHITE-S"]').attributes('src')).toBe('/assets/products/product-tshirt-white.png')
    expect(fetchMock.mock.calls.some(([url]) => String(url).includes('/api/v1/operator/orders/CF2002/execution-evidence'))).toBe(true)
  })

  it('shows the missing-image state for a legacy null snapshot', async () => {
    const nullSnapshot = { ...evidence, items: [{ ...grayItem, imagePathSnapshot: null, subtotal: 129 }] }
    const nullOrder = { ...orders[0], items: [{ ...grayItem, imagePathSnapshot: null }] }
    vi.stubGlobal('fetch', vi.fn()
      .mockResolvedValueOnce(jsonResponse([nullOrder]))
      .mockResolvedValueOnce(jsonResponse(nullOrder))
      .mockResolvedValueOnce(jsonResponse(nullSnapshot)))
    const wrapper = mount(OrderInventoryEvidence)
    await flushPromises()

    expect(wrapper.get('[data-testid="image-missing-T-SHIRT-GRAY-L"]').text()).toContain('暂无快照图片')
  })

  it('shows the failed-image state without hiding the item text', async () => {
    vi.stubGlobal('fetch', readyFetch())
    const wrapper = mount(OrderInventoryEvidence)
    await flushPromises()
    await wrapper.get('[data-testid="order-item-image-T-SHIRT-GRAY-L"]').trigger('error')

    expect(wrapper.get('[data-testid="image-failed-T-SHIRT-GRAY-L"]').text()).toContain('图片加载失败')
    expect(wrapper.text()).toContain('轻盈棉质基础 T 恤')
  })
})
