import { request } from './runtime'
import type { CartItem } from './types'

export const getCart = () => request<CartItem[]>('/v1/me/cart')
export const addCartItem = (skuId: number, quantity = 1) => request<CartItem[]>('/v1/me/cart/items', {method: 'POST', data: {skuId, quantity}})
export const updateCartItem = (itemId: number, quantity: number) => request<CartItem[]>(`/v1/me/cart/items/${itemId}`, {method: 'PUT', data: {quantity}})
export const deleteCartItem = (itemId: number) => request<CartItem[]>(`/v1/me/cart/items/${itemId}`, {method: 'DELETE'})
