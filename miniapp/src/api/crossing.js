import { get, post } from './index'

export const getCrossingAssist = (deviceId) => {
  return get(`/mini/devices/${deviceId}/crossing-assist`)
}

export const updateCrossingAssist = (deviceId, data) => {
  return post(`/mini/devices/${deviceId}/crossing-assist`, data)
}

export const mockCrossingAssist = (deviceId) => {
  return post(`/mini/devices/${deviceId}/crossing-assist/mock`)
}
