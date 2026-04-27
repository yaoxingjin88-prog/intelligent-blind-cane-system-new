import { get, post } from './index'

export const getGuardianCareOverview = (deviceId) => {
  return get(`/mini/devices/${deviceId}/guardian-care/overview`)
}

export const sendGuardianComfort = (deviceId, data = {}) => {
  return post(`/mini/devices/${deviceId}/guardian-care/comfort`, data)
}

export const sendGuardianDestination = (deviceId, data = {}) => {
  return post(`/mini/devices/${deviceId}/guardian-care/destination`, data)
}

export const triggerGuardianSos = (deviceId, data = {}) => {
  return post(`/mini/devices/${deviceId}/guardian-care/sos`, data)
}
