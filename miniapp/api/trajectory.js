import { get } from './index'

// 获取轨迹数据
export const getTrajectory = (deviceId, params) => {
  return get(`/mini/devices/${deviceId}/trajectory`, params)
}

// 获取轨迹统计
export const getTrajectoryStatistics = (deviceId, params) => {
  return get(`/mini/devices/${deviceId}/statistics`, params)
}
