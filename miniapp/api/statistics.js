import { get } from './index'

// 获取活动统计
export const getActivityStatistics = (deviceId) => {
  return get(`/mini/devices/${deviceId}/activity`)
}

// 获取健康报告
export const getHealthReport = (deviceId) => {
  return get(`/mini/devices/${deviceId}/report`)
}

// 获取图表数据
export const getChartData = (deviceId, type) => {
  return get(`/mini/devices/${deviceId}/charts`, { type })
}
