export type OperationsOverview = {
  summary: { productCount: number; skuCount: number; onSaleProductCount: number; availableStockTotal: number; lowStockSkuCount: number; createdOrderCount: number; createdOrderAmount: number; aiInteractionCount: number }
  recentOrders: Array<{ orderNo: string; status: string; totalAmount: number; currency: string; itemCount: number; createdAt: string }>
  lowStockSkus: Array<{ skuId: number; skuCode: string; productName: string; color: string; size: string; availableStock: number; stockLevel: string }>
  aiSummary: { provider: string; providerMode: string; interactionCount: number; answeredCount: number; unsupportedCount: number; fallbackCount: number; providerErrorCount: number; latestInteractionAt: string | null }
  runtimeBoundary: { dataScope: string; authenticationMode: string; aiMode: string; orderStatusScope: string; rateLimitAlgorithm: string; rateLimitLimit: number; rateLimitWindowSeconds: number; rateLimitFailurePolicy: string; mobileRuntime: string; nonH5Runtime: string }
  generatedAt: string
}

export async function fetchOperationsOverview(signal?: AbortSignal): Promise<OperationsOverview> {
  const response = await fetch('/api/operations/overview', { signal })
  if (!response.ok) throw new Error(`运营总览请求失败（HTTP ${response.status}）`)
  return response.json()
}

export function formatOperationsMoney(amount: number, currency: string): string {
  return new Intl.NumberFormat('zh-CN', { style: 'currency', currency, minimumFractionDigits: 2 }).format(amount)
}
