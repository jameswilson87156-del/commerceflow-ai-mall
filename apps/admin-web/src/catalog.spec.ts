import { describe, expect, it } from 'vitest'
import {
  filterProducts,
  LOW_STOCK_THRESHOLD,
  stockLabel,
  stockState,
  type Product,
} from './catalog'

const products: Product[] = [
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

describe('catalog inventory rules', () => {
  it('uses one threshold for every display label', () => {
    expect(stockState(0)).toBe('out')
    expect(stockState(LOW_STOCK_THRESHOLD - 1)).toBe('low')
    expect(stockState(LOW_STOCK_THRESHOLD)).toBe('normal')
    expect(stockLabel(0)).toBe('缺货')
    expect(stockLabel(28)).toBe('库存偏低')
    expect(stockLabel(30)).toBe('库存正常')
  })

  it('filters API-returned products by name, sku code, category, and stock state', () => {
    expect(filterProducts(products, { query: '基础 T 恤', category: 'all', stockState: 'all' })).toHaveLength(1)
    expect(filterProducts(products, { query: 'TOTE-BEIGE-ONE', category: 'all', stockState: 'all' })[0].id).toBe(102)
    expect(filterProducts(products, { query: '', category: '服饰 / 上衣', stockState: 'out' })[0].id).toBe(101)
    expect(filterProducts(products, { query: '', category: '服饰 / 上衣', stockState: 'low' })[0].id).toBe(101)
    expect(filterProducts(products, { query: '', category: '配饰 / 包袋', stockState: 'normal' })[0].id).toBe(102)
  })
})
