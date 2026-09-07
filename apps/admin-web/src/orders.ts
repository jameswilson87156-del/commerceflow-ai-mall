import { fetchAdminJson } from './adminApi'

export type OrderItemSnapshot = {
  productNameSnapshot: string
  skuCodeSnapshot: string
  colorSnapshot: string
  sizeSnapshot: string
  imagePathSnapshot: string | null
  unitPrice: number
  quantity: number
}

export type OrderSummary = {
  orderNo: string
  userId: number
  totalAmount: number
  currency: string
  status: string
  createdAt: string
  items: OrderItemSnapshot[]
}

export type InventoryMovement = {
  movementId: number
  skuId: number
  skuCode: string
  movementType: string
  quantity: number
  stockBefore: number
  stockAfter: number
  createdAt: string
}

export type OrderExecutionEvidence = {
  orderNo: string
  userId: number
  status: string
  totalAmount: number
  currency: string
  createdAt: string
  idempotencyKey: string
  requestResult: string
  firstCreation: boolean
  idempotencyReplay: boolean
  items: Array<OrderItemSnapshot & { subtotal: number }>
  inventoryMovements: InventoryMovement[]
}

export function formatOrderMoney(amount: number, currency: string): string {
  return new Intl.NumberFormat('zh-CN', { style: 'currency', currency, minimumFractionDigits: 2 }).format(amount)
}

export function orderStatusLabel(status: string): string {
  return status === 'CREATED' ? '已创建' : status
}

export function requestResultLabel(result: string): string {
  return result === 'FIRST_CREATED' ? '首次创建' : result
}

export function formatOrderTime(value: string): string {
  return new Intl.DateTimeFormat('zh-CN', { dateStyle: 'medium', timeStyle: 'medium', hour12: false }).format(new Date(value))
}

export function fetchOperatorOrders(): Promise<OrderSummary[]> {
  return fetchAdminJson<OrderSummary[]>('/v1/operator/orders')
}

export function fetchOperatorOrder(orderNo: string): Promise<OrderSummary> {
  return fetchAdminJson<OrderSummary>(`/v1/operator/orders/${encodeURIComponent(orderNo)}`)
}

export function fetchOperatorOrderEvidence(orderNo: string): Promise<OrderExecutionEvidence> {
  return fetchAdminJson<OrderExecutionEvidence>(`/v1/operator/orders/${encodeURIComponent(orderNo)}/execution-evidence`)
}
