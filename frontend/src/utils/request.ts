/**
 * 统一 HTTP 入口：内部走 utils/http.js（fetch + 重试 + Token）。
 * 对外保持 axios 风格 { data }，便于存量页面少改动。
 */
import { fetchJson } from './http.js'

type RequestConfig = {
  params?: Record<string, unknown>
  signal?: AbortSignal
  headers?: Record<string, string>
}

const buildUrl = (url: string, params?: Record<string, unknown>) => {
  if (!params) return url
  const search = new URLSearchParams()
  Object.entries(params).forEach(([key, value]) => {
    if (value === undefined || value === null) return
    search.set(key, String(value))
  })
  const query = search.toString()
  return query ? `${url}${url.includes('?') ? '&' : '?'}${query}` : url
}

const wrap = async <T = any>(promise: Promise<T>) => {
  const data = await promise
  return { data }
}

const http = {
  get(url: string, config: RequestConfig = {}) {
    return wrap(
      fetchJson(buildUrl(url, config.params), {
        signal: config.signal,
        headers: config.headers
      })
    )
  },

  post(url: string, body?: unknown, config: RequestConfig = {}) {
    return wrap(
      fetchJson(buildUrl(url, config.params), {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json',
          ...(config.headers || {})
        },
        body: body === undefined ? undefined : JSON.stringify(body),
        signal: config.signal
      })
    )
  },

  put(url: string, body?: unknown, config: RequestConfig = {}) {
    return wrap(
      fetchJson(buildUrl(url, config.params), {
        method: 'PUT',
        headers: {
          'Content-Type': 'application/json',
          ...(config.headers || {})
        },
        body: body === undefined ? undefined : JSON.stringify(body),
        signal: config.signal
      })
    )
  },

  delete(url: string, config: RequestConfig = {}) {
    return wrap(
      fetchJson(buildUrl(url, config.params), {
        method: 'DELETE',
        headers: config.headers,
        signal: config.signal
      })
    )
  },

  isCancel(error: any) {
    return error?.name === 'AbortError' || error?.code === 'ERR_CANCELED'
  }
}

export default http
