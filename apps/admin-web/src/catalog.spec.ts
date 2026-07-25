import { describe, expect, it } from 'vitest'
import { filterProducts, LOW_STOCK_THRESHOLD, stockLabel, stockState, type Product } from './catalog'

const products: Product[] = [
  { id: 101, name: '轻盈棉质基础T恤', description: '基础短袖', categoryName: '日常服饰', skus: [
    { id: 10001, skuCode: 'SHIRT-BLK-M', color: '黑色', size: 'M', salePrice: 129, currency: 'CNY', availableStock: LOW_STOCK_THRESHOLD },
    { id: 10002, skuCode: 'SHIRT-WHT-L', color: '白色', size: 'L', salePrice: 129, currency: 'CNY', availableStock: 0 },
  ] },
  { id: 102, name: '简约通勤托特包', description: '通勤收纳', categoryName: '通勤配件', skus: [
    { id: 10003, skuCode: 'TOTE-TAN-ONE', color: '卡其色', size: 'One Size', salePrice: 299, currency: 'CNY', availableStock: 2 },
  ] },
]

describe('catalog inventory rules', () => {
  it('uses one threshold for all display labels', () => {
    expect(stockState(0)).toBe('out')
    expect(stockState(LOW_STOCK_THRESHOLD - 1)).toBe('low')
    expect(stockState(LOW_STOCK_THRESHOLD)).toBe('normal')
    expect(stockLabel(0)).toBe('缺货')
    expect(stockLabel(2)).toBe('库存偏低')
    expect(stockLabel(5)).toBe('库存正常')
  })

  it('filters real returned products by product name, sku code, category and derived stock state', () => {
    expect(filterProducts(products, { query: '基础T恤', category: 'all', stockState: 'all' })).toHaveLength(1)
    expect(filterProducts(products, { query: 'TOTE-TAN-ONE', category: 'all', stockState: 'all' })[0].id).toBe(102)
    expect(filterProducts(products, { query: '', category: '日常服饰', stockState: 'out' })[0].id).toBe(101)
    expect(filterProducts(products, { query: '', category: '通勤配件', stockState: 'low' })[0].id).toBe(102)
  })
})
