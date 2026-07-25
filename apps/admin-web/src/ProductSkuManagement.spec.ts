import { flushPromises, mount } from '@vue/test-utils'
import { afterEach, describe, expect, it, vi } from 'vitest'
import ProductSkuManagement from './ProductSkuManagement.vue'

const products = [
  { id: 101, name: 'Essential Cotton Shirt', description: 'Soft cotton shirt.', categoryName: 'Everyday Wear', skus: [{ id: 10001, skuCode: 'SHIRT-BLK-M', color: 'Black', size: 'M', salePrice: 129, currency: 'CNY', availableStock: 12 }] },
  { id: 102, name: 'Structured Work Tote', description: 'Laptop-friendly tote.', categoryName: 'Work Essentials', skus: [{ id: 10003, skuCode: 'TOTE-TAN-ONE', color: 'Tan', size: 'One Size', salePrice: 299, currency: 'CNY', availableStock: 4 }] },
]

function jsonResponse(body: unknown, status = 200) {
  return new Response(JSON.stringify(body), { status, headers: { 'Content-Type': 'application/json' } })
}

afterEach(() => vi.unstubAllGlobals())

describe('ProductSkuManagement', () => {
  it('renders loading before the product response resolves', () => {
    vi.stubGlobal('fetch', vi.fn(() => new Promise(() => {})))
    const wrapper = mount(ProductSkuManagement)
    expect(wrapper.get('[data-testid="loading-state"]').text()).toContain('正在加载真实商品数据')
  })

  it('renders an error state for a failed product request', async () => {
    vi.stubGlobal('fetch', vi.fn().mockResolvedValue(jsonResponse({}, 500)))
    const wrapper = mount(ProductSkuManagement)
    await flushPromises()
    expect(wrapper.get('[data-testid="error-state"]').text()).toContain('商品数据加载失败')
  })

  it('renders an empty state without inventing products', async () => {
    vi.stubGlobal('fetch', vi.fn().mockResolvedValue(jsonResponse([])))
    const wrapper = mount(ProductSkuManagement)
    await flushPromises()
    expect(wrapper.get('[data-testid="empty-state"]').text()).toContain('当前没有可展示的商品')
  })

  it('loads detail data, switches products, and displays the selected SKU table', async () => {
    const fetchMock = vi.fn()
      .mockResolvedValueOnce(jsonResponse(products))
      .mockResolvedValueOnce(jsonResponse(products[0]))
      .mockResolvedValueOnce(jsonResponse(products[1]))
    vi.stubGlobal('fetch', fetchMock)
    const wrapper = mount(ProductSkuManagement)
    await flushPromises()
    expect(wrapper.get('[data-testid="sku-10001"]').text()).toContain('SHIRT-BLK-M')
    await wrapper.get('[data-testid="product-102"]').trigger('click')
    await flushPromises()
    expect(wrapper.get('[data-testid="sku-10003"]').text()).toContain('TOTE-TAN-ONE')
    expect(fetchMock).toHaveBeenCalledWith(expect.stringContaining('/products/102'))
  })
})
