export const CATALOG_SORT_MODES = Object.freeze([
  { value: 'recommended', label: '推荐排序' },
  { value: 'price-asc', label: '价格由低到高' },
  { value: 'price-desc', label: '价格由高到低' },
  { value: 'name-asc', label: '商品名称' }
])

export function normalizeCatalogQuery(value) {
  return String(value ?? '').trim().toLocaleLowerCase('zh-CN')
}

export function getCatalogCategories(products = []) {
  const categories = [...new Set(products
    .map(product => String(product?.categoryName ?? '').trim())
    .filter(Boolean))]
  categories.sort((left, right) => left.localeCompare(right, 'zh-CN'))
  return ['全部', ...categories]
}

function lowestPrice(product) {
  const prices = (product?.skus ?? [])
    .map(sku => Number(sku?.salePrice))
    .filter(Number.isFinite)
  return prices.length ? Math.min(...prices) : Number.POSITIVE_INFINITY
}

function searchableText(product) {
  return [product?.name, product?.description, product?.categoryName, product?.productCode]
    .map(value => String(value ?? ''))
    .join(' ')
    .toLocaleLowerCase('zh-CN')
}

export function filterAndSortProducts(products = [], options = {}) {
  const query = normalizeCatalogQuery(options.query)
  const category = String(options.category ?? '全部')
  const sort = String(options.sort ?? 'recommended')

  const result = products.filter(product => {
    const matchesCategory = category === '全部' || String(product?.categoryName ?? '').trim() === category
    const matchesQuery = !query || searchableText(product).includes(query)
    return matchesCategory && matchesQuery
  })

  if (sort === 'price-asc') {
    return result.slice().sort((left, right) => lowestPrice(left) - lowestPrice(right))
  }
  if (sort === 'price-desc') {
    return result.slice().sort((left, right) => lowestPrice(right) - lowestPrice(left))
  }
  if (sort === 'name-asc') {
    return result.slice().sort((left, right) => String(left?.name ?? '').localeCompare(String(right?.name ?? ''), 'zh-CN'))
  }
  return result
}
