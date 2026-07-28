import { DEMO_USER_ID } from '../config/runtime'
import { request } from './runtime'
import type { CartItem } from './types'

const userQuery = `userId=${DEMO_USER_ID}`
export const getCart = () => request<CartItem[]>(`/cart?${userQuery}`)
export const addCartItem = (skuId: number, quantity = 1) => request<CartItem[]>(`/cart/items?${userQuery}`, {method: 'POST', data: {skuId, quantity}})
export const updateCartItem = (itemId: number, quantity: number) => request<CartItem[]>(`/cart/items/${itemId}?${userQuery}`, {method: 'PUT', data: {quantity}})
export const deleteCartItem = (itemId: number) => request<CartItem[]>(`/cart/items/${itemId}?${userQuery}`, {method: 'DELETE'})
