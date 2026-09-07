import { request } from './runtime'
import type { CartItem, OrderSummary } from './types'

export const submitOrder = (items: CartItem[], idempotencyKey: string) => request<OrderSummary>('/v1/me/orders', {
  method: 'POST',
  headers: {'Idempotency-Key': idempotencyKey},
  data: {items: items.map(item => ({skuId: item.skuId, quantity: item.quantity}))}
})
export const getOrders = () => request<OrderSummary[]>('/v1/me/orders')
export const getOrder = (orderNo: string) => request<OrderSummary>(`/v1/me/orders/${encodeURIComponent(orderNo)}`)
