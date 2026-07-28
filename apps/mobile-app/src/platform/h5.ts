type RequestOption = {
  url: string
  method?: string
  data?: Record<string, unknown> | unknown[]
  header?: Record<string, string>
  timeout?: number
  success?: (response: any) => void
  fail?: (error: any) => void
}

const defaultPath = '/pages/index/index'

function parseHash() {
  if (typeof window === 'undefined') return defaultPath
  return (window.location.hash.replace(/^#/, '') || defaultPath).split('?')[0] || defaultPath
}

export function currentPath() { return parseHash() }
export function currentHash() { return typeof window === 'undefined' ? defaultPath : window.location.hash }

export function queryParam(name: string) {
  if (typeof window === 'undefined') return ''
  return new URLSearchParams(window.location.hash.split('?')[1] || '').get(name) || ''
}

export function startNavigationListener(onPathChange: (path: string) => void) {
  if (typeof window === 'undefined') return
  const update = () => onPathChange(currentPath())
  window.addEventListener('hashchange', update)
  update()
}

function setPath(url: string) {
  if (typeof window === 'undefined') return
  window.location.hash = url.startsWith('/') ? url : `/${url}`
}

function request(option: RequestOption) {
  const controller = new AbortController()
  const method = (option.method || 'GET').toUpperCase()
  const url = new URL(option.url, window.location.origin)
  if (method === 'GET' && option.data && typeof option.data === 'object' && !Array.isArray(option.data)) {
    Object.entries(option.data).forEach(([key, value]) => url.searchParams.set(key, String(value)))
  }
  const body = method === 'GET' || option.data === undefined ? undefined : JSON.stringify(option.data)
  const timer = window.setTimeout(() => controller.abort(), option.timeout || 15000)
  fetch(url, {method, headers: option.header || {}, body, signal: controller.signal})
    .then(async response => {
      const text = await response.text()
      let data: any = null
      try { data = text ? JSON.parse(text) : null } catch { data = text }
      const header: Record<string, string> = {}
      response.headers.forEach((value, key) => { header[key] = value })
      option.success?.({data, statusCode: response.status, header})
    })
    .catch(error => option.fail?.({errMsg: error?.name === 'AbortError' ? 'request:fail timeout' : String(error)}))
    .finally(() => window.clearTimeout(timer))
  return {abort: () => controller.abort()}
}

if (typeof window !== 'undefined' && !(globalThis as any).uni) {
  ;(globalThis as any).uni = {
    request,
    navigateTo: ({url}: {url: string}) => setPath(url),
    switchTab: ({url}: {url: string}) => setPath(url),
    redirectTo: ({url}: {url: string}) => setPath(url),
    reLaunch: ({url}: {url: string}) => setPath(url),
    navigateBack: () => window.history.back(),
    showToast: ({title}: {title: string}) => window.dispatchEvent(new CustomEvent('commerceflow:toast', {detail: title})),
    stopPullDownRefresh: () => undefined
  }
}
