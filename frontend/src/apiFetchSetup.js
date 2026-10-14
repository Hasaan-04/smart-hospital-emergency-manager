// Existing Sessions 2-7 use fetch('/api/...') directly. Spring Security requires
// a CSRF header on POST/PATCH/DELETE requests. Install a wrapper once.
const originalFetch = window.fetch.bind(window)

window.fetch = async (input, init = {}) => {
  const url = typeof input === 'string' ? input : input.url
  const method = String(init.method || (typeof input !== 'string' ? input.method : 'GET')).toUpperCase()
  const protectedWrite = typeof url === 'string'
    && url.startsWith('/api/')
    && ['POST', 'PUT', 'PATCH', 'DELETE'].includes(method)

  if (!protectedWrite) return originalFetch(input, init)

  const csrfRes = await originalFetch('/api/auth/csrf', { credentials: 'same-origin' })
  if (!csrfRes.ok) return csrfRes
  const token = await csrfRes.json()
  const headers = new Headers(init.headers || (typeof input !== 'string' ? input.headers : undefined))
  headers.set(token.headerName, token.token)
  return originalFetch(input, { ...init, headers, credentials: 'same-origin' })
}
