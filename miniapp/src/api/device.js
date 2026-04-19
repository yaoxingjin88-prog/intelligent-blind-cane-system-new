import { get, post, put, del } from './index'

// 获取设备列表
export const getDeviceList = () => {
  return get('/mini/devices')
}

// 绑定设备
export const bindDevice = (data) => {
  return post('/mini/devices/bind', data)
}

// 解绑设备
export const unbindDevice = (id) => {
  return del(`/mini/devices/${id}`)
}

// 获取设备详情
export const getDeviceDetail = (id) => {
  return get(`/mini/devices/${id}`)
}

// 获取设备状态
export const getDeviceStatus = (id) => {
  return get(`/mini/devices/${id}/status`)
}

// 获取实时位置
export const getDeviceLocation = (id) => {
  return get(`/mini/devices/${id}/location`)
}

// 获取传感器数据
export const getSensorData = (id) => {
  return get(`/mini/devices/${id}/sensor-data`)
}

// 获取设备健康度
export const getDeviceHealth = (id) => {
  return get(`/mini/devices/${id}/health`)
}
