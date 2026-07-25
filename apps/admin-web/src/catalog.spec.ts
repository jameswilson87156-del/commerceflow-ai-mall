import { describe, expect, it } from 'vitest'
import { filterProducts, LOW_STOCK_THRESHOLD, stockLabel, stockState, type Product } from './catalog'

const products: Product[] = [
  { id: 101, name: 'Essential Cotton Shirt', description: 'shirt', categoryName: 'Everyday Wear', skus: [
    { id: 10001, skuCode: 'SHIRT-BLK-M', color: 'Black', size: 'M', salePrice: 129, currency: 'CNY', availableStock: LOW_STOCK_THRESHOLD },
    { id: 10002, skuCode: 'SHIRT-WHT-L', color: 'White', size: 'L', salePrice: 129, currency: 'CNY', availableStock: 0 },
  ] },
  { id: 102, name: 'Structured Work Tote', description: 'tote', categoryName: 'Work Essentials', skus: [
    { id: 10003, skuCode: 'TOTE-TAN-ONE', color: 'Tan', size: 'One Size', salePrice: 299, currency: 'CNY', availableStock: 2 },
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
    expect(filterProducts(products, { query: 'shirt', category: 'all', stockState: 'all' })).toHaveLength(1)
    expect(filterProducts(products, { query: 'TOTE-TAN-ONE', category: 'all', stockState: 'all' })[0].id).toBe(102)
    expect(filterProducts(products, { query: '', category: 'Everyday Wear', stockState: 'out' })[0].id).toBe(101)
    expect(filterProducts(products, { query: '', category: 'Work Essentials', stockState: 'low' })[0].id).toBe(102)
  })
})
