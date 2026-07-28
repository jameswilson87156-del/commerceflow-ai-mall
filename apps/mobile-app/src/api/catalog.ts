import { request } from './runtime'
import type { Product } from './types'

export const getProducts = () => request<Product[]>('/products')
export const getProduct = (productId: number) => request<Product>(`/products/${productId}`)
