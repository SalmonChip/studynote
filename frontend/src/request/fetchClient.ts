import type { HttpClient, Options, RequestTuple, Response } from './types'

export const TOKEN_KEY = 'token'
export const AUTH_EXPIRED = 'session-expired'

export class ApiError extends Error {
  constructor(
    message: string,
    public code: number,
  ) {
    super(message)
  }
}

export default class FetchClient implements HttpClient {
  constructor(private baseURL = import.meta.env.VITE_API_BASE_URL || '') {}

  async request<T>([method, path]: RequestTuple, options: Options = {}): Promise<Response<T>> {
    let index = 0
    const resolvedPath = path.replace(/\{\w+\}/g, () => {
      const value = options.pathParams?.[index++]
      if (value === undefined) throw new Error('缺少路径参数')
      return encodeURIComponent(value)
    })
    const url = new URL(this.baseURL.replace(/\/$/, '') + resolvedPath, window.location.origin)
    for (const [key, value] of Object.entries(options.queryParams || {})) {
      if (value !== undefined && value !== null && value !== '')
        url.searchParams.set(key, String(value))
    }
    const headers = new Headers(options.headers)
    const token = localStorage.getItem(TOKEN_KEY)
    if (token) headers.set('Authorization', `Bearer ${token}`)
    let body: BodyInit | undefined
    if (method !== 'GET' && method !== 'HEAD' && options.body !== undefined) {
      if (options.body instanceof FormData) body = options.body
      else {
        headers.set('Content-Type', 'application/json')
        body = JSON.stringify(options.body)
      }
    }
    const controller = new AbortController()
    const abort = () => controller.abort()
    if (options.signal?.aborted) abort()
    options.signal?.addEventListener('abort', abort, { once: true })
    const timer = window.setTimeout(abort, 15000)
    try {
      const response = await fetch(url, { method, headers, body, signal: controller.signal })
      const result = (await response.json().catch(() => null)) as Response<T> | null
      const code = response.ok ? (result?.code ?? response.status) : response.status
      if (!response.ok || !result || code !== 200) {
        if (code === 401) {
          localStorage.removeItem(TOKEN_KEY)
          window.dispatchEvent(new Event(AUTH_EXPIRED))
        }
        throw new ApiError(result?.message || result?.msg || `请求失败 (${code})`, code)
      }
      return result
    } finally {
      clearTimeout(timer)
      options.signal?.removeEventListener('abort', abort)
    }
  }
}
