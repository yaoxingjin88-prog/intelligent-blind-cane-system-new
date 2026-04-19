import { defineStore } from 'pinia'
import { ref } from 'vue'
import { getAlarmList, getAlarmStatistics } from '@/api/alarm'

export const useAlarmStore = defineStore('alarm', () => {
  // 报警列表
  const alarmList = ref([])
  // 未读报警数量
  const unreadCount = ref(0)
  // 报警统计
  const statistics = ref(null)

  // 获取报警列表
  const fetchAlarmList = async (params = {}) => {
    try {
      const res = await getAlarmList(params)
      alarmList.value = res.data || []
      // 计算未读数量
      unreadCount.value = alarmList.value.filter(item => item.status === 'pending').length
      return res
    } catch (error) {
      console.error('获取报警列表失败', error)
      throw error
    }
  }

  // 获取报警统计
  const fetchAlarmStatistics = async (deviceId) => {
    try {
      const res = await getAlarmStatistics(deviceId)
      statistics.value = res.data
      return res
    } catch (error) {
      console.error('获取报警统计失败', error)
      throw error
    }
  }

  // 添加新报警（用于WebSocket推送）
  const addAlarm = (alarm) => {
    alarmList.value.unshift(alarm)
    if (alarm.status === 'pending') {
      unreadCount.value++
    }
  }

  // 更新报警状态
  const updateAlarmStatus = (alarmId, status) => {
    const alarm = alarmList.value.find(item => item.id === alarmId)
    if (alarm) {
      alarm.status = status
      if (status !== 'pending' && alarm.status === 'pending') {
        unreadCount.value--
      }
    }
  }

  return {
    alarmList,
    unreadCount,
    statistics,
    fetchAlarmList,
    fetchAlarmStatistics,
    addAlarm,
    updateAlarmStatus
  }
})
