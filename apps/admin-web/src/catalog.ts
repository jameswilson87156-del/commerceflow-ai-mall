export const LOW_STOCK_THRESHOLD = 5

export type StockState = 'normal' | 'low' | 'out'

export type Sku = {
  id: number
  skuCode: string
  color: string
  size: string
  salePrice: number
  currency: string
  availableStock: number
}

export type Product = {
  id: number
  name: string
  description: string
  categoryName: string
  skus: Sku[]
}

export type CatalogFilters = {
  query: string
  category: string
  stockState: 'all' | StockState
}

export function stockState(availableStock: number): StockState {
  if (availableStock === 0) return 'out'
  if (availableStock < LOW_STOCK_THRESHOLD) return 'low'
  return 'normal'
}

export function stockLabel(availableStock: number): string {
  return { normal: '库存正常', low: '库存偏低', out: '缺货' }[stockState(availableStock)]
}

export function productStock(product: Product): number {
  return product.skus.reduce((total, sku) => total + sku.availableStock, 0)
}

export function filterProducts(products: Product[], filters: CatalogFilters): Product[] {
  const query = filters.query.trim().toLocaleLowerCase()
  return products.filter((product) => {
    const matchesQuery = !query
      || product.name.toLocaleLowerCase().includes(query)
      || product.skus.some((sku) => sku.skuCode.toLocaleLowerCase().includes(query))
    const matchesCategory = filters.category === 'all' || product.categoryName === filters.category
    const matchesStock = filters.stockState === 'all'
      || product.skus.some((sku) => stockState(sku.availableStock) === filters.stockState)
    return matchesQuery && matchesCategory && matchesStock
  })
}

export function formatMoney(amount: number, currency: string): string {
  return new Intl.NumberFormat('zh-CN', {
    style: 'currency',
    currency,
    minimumFractionDigits: 2,
  }).format(amount)
}
