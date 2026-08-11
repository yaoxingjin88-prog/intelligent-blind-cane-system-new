import axios from 'axios'

const sleep = (ms: number) => new Promise((resolve) => setTimeout(resolve, ms))

const isRetryable = (error: any) => {
  const status = error?.response?.status
  if (status === 502 || status === 503 || status === 504) return true
  if (!error?.response && (error?.code === 'ECONNABORTED' || error?.message?.includes('Network Error'))) {
    return true
  }
  return false
}

axios.defaults.baseURL = import.meta.env.VITE_API_BASE_URL || ''
axios.defaults.timeout = 20000

axios.interceptors.response.use(
  (response) => response,
  async (error) => {
    const config = error.config || {}
    const maxRetries = config.__retryCountMax ?? 2
    config.__retryCount = config.__retryCount || 0

    if (config.__retryCount < maxRetries && isRetryable(error)) {
      config.__retryCount += 1
      const delay = 600 * config.__retryCount
      await sleep(delay)
      return axios(config)
    }
    return Promise.reject(error)
  }
)

export default axios
