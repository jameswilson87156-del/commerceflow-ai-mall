export function normalizeBaseUrl(value, fallback = '/api') {
  const raw = String(value ?? '').trim()
  if (!raw) return fallback.replace(/\/+$/, '')
  return raw.replace(/\/+$/, '')
}

export function joinUrl(base, path) {
  const normalizedBase = normalizeBaseUrl(base, '')
  const normalizedPath = String(path ?? '').replace(/^\/+/, '')
  return normalizedBase ? `${normalizedBase}/${normalizedPath}` : `/${normalizedPath}`
}

export function formatCents(value) {
  const text = String(value ?? '0').trim()
  const match = text.match(/^(-?)(\d+)(?:\.(\d{1,2}))?$/)
  if (!match) return '0.00'
  const cents = `${match[3] || ''}00`.slice(0, 2)
  return `${match[1]}${match[2]}.${cents}`
}

export function addCents(total, unitPrice, quantity) {
  const toCents = (value) => {
    const formatted = formatCents(value)
    const [yuan, cents] = formatted.replace('-', '').split('.')
    const amount = Number(yuan) * 100 + Number(cents)
    return formatted.startsWith('-') ? -amount : amount
  }
  const result = toCents(total) + toCents(unitPrice) * Number(quantity || 0)
  return `${result < 0 ? '-' : ''}${Math.floor(Math.abs(result) / 100)}.${String(Math.abs(result) % 100).padStart(2, '0')}`
}

export function createClientIdempotencyKey(random = Math.random().toString(36).slice(2)) {
  const cryptoPart = random.replace(/[^a-z0-9]/gi, '').slice(0, 24) || 'fallback'
  return `mobile-${Date.now().toString(36)}-${cryptoPart}`
}
