const sleep = (ms) => new Promise((resolve) => setTimeout(resolve, ms))

const isRetryableStatus = (status) => status === 502 || status === 503 || status === 504

/**
 * 带重试的 JSON 请求；502/503/504 时自动重试，避免网关偶发错误导致页面空白
 */
export async function fetchJson(url, options = {}) {
  const {
    retries = 2,
    retryDelay = 800,
    cache = 'no-cache',
    ...fetchOptions
  } = options

  const fullUrl = url.startsWith('http')
    ? url
    : `${import.meta.env.VITE_API_BASE_URL || ''}${url}`

  let lastError = null

  for (let attempt = 0; attempt <= retries; attempt += 1) {
    try {
      const response = await fetch(fullUrl, { cache, ...fetchOptions })
      const contentType = response.headers.get('content-type') || ''

      if (!response.ok) {
        const text = await response.text()
        const error = new Error(`HTTP ${response.status}${text ? `: ${text.slice(0, 120)}` : ''}`)
        error.status = response.status
        throw error
      }

      if (!contentType.includes('application/json')) {
        const text = await response.text()
        throw new Error(`服务器返回非 JSON（${response.status}）: ${text.slice(0, 120)}`)
      }

      return await response.json()
    } catch (error) {
      lastError = error
      const status = error?.status
      if (attempt < retries && (isRetryableStatus(status) || !status)) {
        await sleep(retryDelay * (attempt + 1))
        continue
      }
      throw error
    }
  }

  throw lastError
}
