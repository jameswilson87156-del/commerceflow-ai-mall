import { DEMO_USER_ID } from '../config/runtime'
import { request } from './runtime'
import type { CartItem, OrderSummary } from './types'

const userQuery = `userId=${DEMO_USER_ID}`
export const submitOrder = (items: CartItem[], idempotencyKey: string) => request<OrderSummary>(`/orders?${userQuery}`, {
  method: 'POST',
  headers: {'Idempotency-Key': idempotencyKey},
  data: {items: items.map(item => ({skuId: item.skuId, quantity: item.quantity}))}
})
export const getOrders = () => request<OrderSummary[]>(`/orders?${userQuery}`)
export const getOrder = (orderNo: string) => request<OrderSummary>(`/orders/${encodeURIComponent(orderNo)}`)
