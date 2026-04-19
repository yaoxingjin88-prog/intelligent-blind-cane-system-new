import { get, post, put, del } from './index'

// 获取围栏列表
export const getFenceList = (deviceId) => {
  return get('/mini/fences', { deviceId })
}

// 创建围栏
export const createFence = (data) => {
  return post('/mini/fences', data)
}

// 更新围栏
export const updateFence = (id, data) => {
  return put(`/mini/fences/${id}`, data)
}

// 删除围栏
export const deleteFence = (id) => {
  return del(`/mini/fences/${id}`)
}

// 获取围栏状态
export const getFenceStatus = (id) => {
  return get(`/mini/fences/${id}/status`)
}
