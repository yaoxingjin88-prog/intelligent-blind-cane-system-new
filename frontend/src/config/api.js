// API 配置
// 根据环境自动切换 API 地址

export const API_BASE_URL = import.meta.env.VITE_API_BASE_URL || ''

// 封装请求方法
export const apiRequest = async (url, options = {}) => {
  const fullUrl = `${API_BASE_URL}${url}`
  console.log('API Request:', fullUrl)
  const response = await fetch(fullUrl, options)
  return response
}

// 便捷请求方法
export const apiGet = (url) => apiRequest(url, { cache: 'no-cache' })

export const apiPost = (url, data) => apiRequest(url, {
  method: 'POST',
  headers: { 'Content-Type': 'application/json' },
  body: JSON.stringify(data)
})

export const apiPut = (url, data) => apiRequest(url, {
  method: 'PUT',
  headers: { 'Content-Type': 'application/json' },
  body: JSON.stringify(data)
})

export const apiDelete = (url) => apiRequest(url, { method: 'DELETE' })
