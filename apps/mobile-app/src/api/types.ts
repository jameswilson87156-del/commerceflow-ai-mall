export interface Sku {
  id: number
  skuCode: string
  color: string
  size: string
  salePrice: string | number
  currency: string
  availableStock: number
  imagePath?: string | null
}

export interface Product {
  id: number
  productCode: string
  name: string
  description: string
  categoryName: string
  status: string
  coverImagePath?: string | null
  skus: Sku[]
}

export interface CartItem {
  id: number
  skuId: number
  productName: string
  skuCode: string
  color: string
  size: string
  unitPrice: string | number
  currency: string
  imagePath?: string | null
  quantity: number
  availableStock: number
}

export interface OrderItem {
  productNameSnapshot: string
  skuCodeSnapshot: string
  colorSnapshot: string
  sizeSnapshot: string
  imagePathSnapshot?: string | null
  unitPrice: string | number
  quantity: number
}

export interface OrderSummary {
  orderNo: string
  userId: number
  totalAmount: string | number
  currency: string
  status: string
  createdAt: string
  items: OrderItem[]
}

export interface ApiErrorShape {
  code?: string
  message?: string
  retryAfterSeconds?: number
  [key: string]: unknown
}

export interface RateLimitMeta {
  mode?: string
  limit?: number
  remaining?: number
  reset?: number
  retryAfter?: number
}
