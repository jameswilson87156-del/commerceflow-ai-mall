export const LOW_STOCK_THRESHOLD = 10

export function getStockPresentation(availableStock, lowStockThreshold = LOW_STOCK_THRESHOLD) {
  const stock = Number(availableStock) || 0
  if (stock <= 0) return { tone: 'danger', label: '缺货', available: false }
  if (stock < lowStockThreshold) return { tone: 'warning', label: '库存偏低', available: true }
  return { tone: 'success', label: '库存正常', available: true }
}

export function isPurchasableSku(availableStock) {
  return getStockPresentation(availableStock).available
}

export function hasHorizontalOverflow(scrollWidth, clientWidth, tolerance = 1) {
  return Number(scrollWidth) > Number(clientWidth) + tolerance
}

export function isDecodedImage(image) {
  return Boolean(image?.complete && image?.naturalWidth > 0 && image?.naturalHeight > 0)
}
