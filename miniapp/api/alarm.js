import { get, put } from './index'

// 获取报警列表
export const getAlarmList = (params) => {
  return get('/mini/alarms', params)
}

// 获取报警详情
export const getAlarmDetail = (id) => {
  return get(`/mini/alarms/${id}`)
}

// 处理报警
export const handleAlarm = (id, data) => {
  return put(`/mini/alarms/${id}/handle`, data)
}

// 获取报警统计
export const getAlarmStatistics = (deviceId) => {
  return get('/mini/alarms/statistics', { deviceId })
}
