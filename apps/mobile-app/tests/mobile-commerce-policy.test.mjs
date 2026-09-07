import test from 'node:test'
import assert from 'node:assert/strict'
import {
  LOW_STOCK_THRESHOLD,
  getStockPresentation,
  hasHorizontalOverflow,
  isDecodedImage,
  isPurchasableSku
} from '../src/ui/mobile-commerce-policy.mjs'
import {
  filterAndSortProducts,
  getCatalogCategories,
  normalizeCatalogQuery
} from '../src/ui/mobile-catalog-policy.mjs'

test('库存阈值集中为 10', () => {
  assert.equal(LOW_STOCK_THRESHOLD, 10)
})

test('零库存 SKU 显示缺货且不可购买', () => {
  assert.deepEqual(getStockPresentation(0), { tone: 'danger', label: '缺货', available: false })
  assert.equal(isPurchasableSku(0), false)
})

test('负库存不会被当作可购买库存', () => {
  assert.deepEqual(getStockPresentation(-1), { tone: 'danger', label: '缺货', available: false })
  assert.equal(isPurchasableSku(-1), false)
})

test('低于阈值的库存显示库存偏低', () => {
  assert.deepEqual(getStockPresentation(9), { tone: 'warning', label: '库存偏低', available: true })
  assert.equal(isPurchasableSku(9), true)
})

test('阈值及以上库存显示库存正常', () => {
  assert.deepEqual(getStockPresentation(10), { tone: 'success', label: '库存正常', available: true })
  assert.deepEqual(getStockPresentation(88), { tone: 'success', label: '库存正常', available: true })
})

test('空值库存安全降级为缺货', () => {
  assert.deepEqual(getStockPresentation(undefined), { tone: 'danger', label: '缺货', available: false })
})

test('相等宽度不判定为水平溢出', () => {
  assert.equal(hasHorizontalOverflow(390, 390), false)
})

test('一像素误差不判定为水平溢出', () => {
  assert.equal(hasHorizontalOverflow(391, 390), false)
})

test('超过容差的内容宽度判定为水平溢出', () => {
  assert.equal(hasHorizontalOverflow(392, 390), true)
})

test('已解码的真实图片可展示', () => {
  assert.equal(isDecodedImage({ complete: true, naturalWidth: 1254, naturalHeight: 1254 }), true)
})

test('未完成加载的图片不能视为有效', () => {
  assert.equal(isDecodedImage({ complete: false, naturalWidth: 1254, naturalHeight: 1254 }), false)
})

test('自然尺寸为零的图片不能视为有效', () => {
  assert.equal(isDecodedImage({ complete: true, naturalWidth: 0, naturalHeight: 0 }), false)
})

const catalogProducts = [
  { id: 1, productCode: 'PROD-1001', name: '经典白 T 恤', description: '轻盈棉质日常上衣', categoryName: '服装', skus: [{ salePrice: '129.00' }] },
  { id: 2, productCode: 'PROD-1002', name: '原色帆布袋', description: '耐用通勤收纳', categoryName: '配件', skus: [{ salePrice: '89.00' }] },
  { id: 3, productCode: 'PROD-1003', name: '深海蓝 T 恤', description: '柔软棉质日常上衣', categoryName: '服装', skus: [{ salePrice: '159.00' }] }
]

test('商品搜索词会去除首尾空格并按中文小写归一化', () => {
  assert.equal(normalizeCatalogQuery('  T 恤  '), 't 恤')
})

test('分类只来自真实商品响应并保留全部入口', () => {
  assert.deepEqual(getCatalogCategories(catalogProducts), ['全部', '服装', '配件'])
})

test('目录筛选同时支持分类和商品信息搜索', () => {
  assert.deepEqual(
    filterAndSortProducts(catalogProducts, { category: '服装', query: '蓝' }).map(product => product.id),
    [3]
  )
})

test('价格排序不改变原始商品数组', () => {
  const sorted = filterAndSortProducts(catalogProducts, { sort: 'price-asc' })
  assert.deepEqual(sorted.map(product => product.id), [2, 1, 3])
  assert.deepEqual(catalogProducts.map(product => product.id), [1, 2, 3])
})
