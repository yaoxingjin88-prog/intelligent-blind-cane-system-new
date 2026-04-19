import { defineStore } from 'pinia'
import { ref } from 'vue'

export const useSettingsStore = defineStore('settings', () => {
  // 免打扰设置
  const dndSettings = ref({
    enabled: false,
    startTime: '22:00',
    endTime: '07:00'
  })

  // 消息通知设置
  const notificationSettings = ref({
    alarmEnabled: true,
    locationUpdateEnabled: true,
    systemNotificationEnabled: true
  })

  // 地图设置
  const mapSettings = ref({
    mapType: 'standard', // standard, satellite
    refreshInterval: 30 // 秒
  })

  // 设置免打扰
  const setDndSettings = (settings) => {
    dndSettings.value = { ...dndSettings.value, ...settings }
    uni.setStorageSync('dndSettings', dndSettings.value)
  }

  // 设置消息通知
  const setNotificationSettings = (settings) => {
    notificationSettings.value = { ...notificationSettings.value, ...settings }
    uni.setStorageSync('notificationSettings', notificationSettings.value)
  }

  // 设置地图
  const setMapSettings = (settings) => {
    mapSettings.value = { ...mapSettings.value, ...settings }
    uni.setStorageSync('mapSettings', mapSettings.value)
  }

  // 从本地存储恢复
  const restoreFromStorage = () => {
    const storedDndSettings = uni.getStorageSync('dndSettings')
    const storedNotificationSettings = uni.getStorageSync('notificationSettings')
    const storedMapSettings = uni.getStorageSync('mapSettings')
    
    if (storedDndSettings) {
      dndSettings.value = storedDndSettings
    }
    if (storedNotificationSettings) {
      notificationSettings.value = storedNotificationSettings
    }
    if (storedMapSettings) {
      mapSettings.value = storedMapSettings
    }
  }

  return {
    dndSettings,
    notificationSettings,
    mapSettings,
    setDndSettings,
    setNotificationSettings,
    setMapSettings,
    restoreFromStorage
  }
}, {
  persist: {
    enabled: true,
    strategies: [
      {
        key: 'settings',
        storage: {
          getItem: uni.getStorageSync,
          setItem: uni.setStorageSync
        }
      }
    ]
  }
})
