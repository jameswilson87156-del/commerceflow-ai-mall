export function classifyHttpFailure(statusCode) {
  if (statusCode === 401) return 'unauthorized'
  if (statusCode === 403) return 'forbidden'
  if (statusCode === 404) return 'not-found'
  if (statusCode === 0 || statusCode >= 500) return 'backend-unavailable'
  return 'request-failed'
}
