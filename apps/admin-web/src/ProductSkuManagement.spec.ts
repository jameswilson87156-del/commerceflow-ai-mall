import { flushPromises, mount } from '@vue/test-utils'
import { afterEach, describe, expect, it, vi } from 'vitest'
import ProductSkuManagement from './ProductSkuManagement.vue'

const products = [
  {
    id: 101,
    productCode: 'PROD-1001',
    name: '轻盈棉质基础 T 恤',
    description: '日常基础款短袖，适合春夏穿搭。',
    categoryName: '服饰 / 上衣',
    status: 'ON_SALE',
    coverImagePath: '/assets/products/product-tshirt-white.png',
    skus: [
      { id: 10001, skuCode: 'T-SHIRT-BLACK-M', color: '黑色', size: 'M', salePrice: 129, currency: 'CNY', availableStock: 96, imagePath: '/assets/products/product-tshirt-black.png' },
      { id: 10004, skuCode: 'T-SHIRT-GRAY-L', color: '灰色', size: 'L', salePrice: 129, currency: 'CNY', availableStock: 28, imagePath: '/assets/products/product-tshirt-gray.png' },
      { id: 10005, skuCode: 'T-SHIRT-BLUE-XL', color: '藏青色', size: 'XL', salePrice: 129, currency: 'CNY', availableStock: 0, imagePath: '/assets/products/product-tshirt-navy.png' },
    ],
  },
  {
    id: 102,
    productCode: 'PROD-1002',
    name: '简约通勤托特包',
    description: '轻便大容量，适合日常通勤使用。',
    categoryName: '配饰 / 包袋',
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

afterEach(() => vi.unstubAllGlobals())

describe('ProductSkuManagement', () => {
  it('renders loading before the product response resolves', () => {
    vi.stubGlobal('fetch', vi.fn(() => new Promise(() => {})))
    const wrapper = mount(ProductSkuManagement)
    expect(wrapper.get('[data-testid="loading-state"]').text()).toContain('正在加载真实商品数据')
  })

  it('renders an error state and retries the product request', async () => {
    const fetchMock = vi.fn()
      .mockResolvedValueOnce(jsonResponse({}, 500))
      .mockResolvedValueOnce(jsonResponse(products))
      .mockResolvedValueOnce(jsonResponse(products[0]))
    vi.stubGlobal('fetch', fetchMock)
    const wrapper = mount(ProductSkuManagement)
    await flushPromises()
    expect(wrapper.get('[data-testid="error-state"]').text()).toContain('商品数据加载失败')
    await wrapper.get('button').trigger('click')
    await flushPromises()
    expect(wrapper.text()).toContain('轻盈棉质基础 T 恤')
    expect(fetchMock).toHaveBeenCalledTimes(3)
  })

  it('renders an empty state without inventing products', async () => {
    vi.stubGlobal('fetch', vi.fn().mockResolvedValue(jsonResponse([])))
    const wrapper = mount(ProductSkuManagement)
    await flushPromises()
    expect(wrapper.get('[data-testid="empty-state"]').text()).toContain('当前没有可展示的商品')
  })

  it('renders API images, switches products, and updates the SKU table', async () => {
    const fetchMock = vi.fn()
      .mockResolvedValueOnce(jsonResponse(products))
      .mockResolvedValueOnce(jsonResponse(products[0]))
      .mockResolvedValueOnce(jsonResponse(products[1]))
    vi.stubGlobal('fetch', fetchMock)
    const wrapper = mount(ProductSkuManagement)
    await flushPromises()
    expect(wrapper.get('[data-testid="product-image-101"]').attributes('src')).toBe('/assets/products/product-tshirt-white.png')
    expect(wrapper.get('[data-testid="sku-image-10001"]').attributes('src')).toBe('/assets/products/product-tshirt-black.png')
    expect(wrapper.get('[data-testid="sku-10004"]').text()).toContain('库存偏低')
    expect(wrapper.get('[data-testid="sku-10005"]').text()).toContain('缺货')
    await wrapper.get('[data-testid="product-102"]').trigger('click')
    await flushPromises()
    expect(wrapper.get('[data-testid="product-main-image"]').attributes('src')).toBe('/assets/products/product-tote-beige.png')
    expect(wrapper.get('[data-testid="sku-10003"]').text()).toContain('TOTE-BEIGE-ONE')
    expect(fetchMock).toHaveBeenCalledWith(expect.stringContaining('/products/102'))
  })
})
