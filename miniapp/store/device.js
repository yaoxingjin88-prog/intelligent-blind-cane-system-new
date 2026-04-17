import { defineStore } from 'pinia'
import { ref } from 'vue'
import { getDeviceList, getDeviceStatus, getDeviceLocation, getSensorData } from '@/api/device'

export const useDeviceStore = defineStore('device', () => {
  // 当前选中的设备
  const currentDevice = ref(null)
  // 设备列表
  const deviceList = ref([])
  // 设备状态
  const deviceStatus = ref(null)
  // 实时位置
  const deviceLocation = ref(null)
  // 传感器数据
  const sensorData = ref(null)

  // 设置当前设备
  const setCurrentDevice = (device) => {
    currentDevice.value = device
    uni.setStorageSync('currentDevice', device)
  }

  // 获取设备列表
  const fetchDeviceList = async () => {
    try {
      const res = await getDeviceList()
      deviceList.value = res.data || []
      return res
    } catch (error) {
      console.error('获取设备列表失败', error)
      throw error
    }
  }

  // 获取设备状态
  const fetchDeviceStatus = async (deviceId) => {
    try {
      const res = await getDeviceStatus(deviceId)
      deviceStatus.value = res.data
      return res
    } catch (error) {
      console.error('获取设备状态失败', error)
      throw error
    }
  }

  // 获取实时位置
  const fetchDeviceLocation = async (deviceId) => {
    try {
      const res = await getDeviceLocation(deviceId)
      deviceLocation.value = res.data
      return res
    } catch (error) {
      console.error('获取设备位置失败', error)
      throw error
    }
  }

  // 获取传感器数据
  const fetchSensorData = async (deviceId) => {
    try {
      const res = await getSensorData(deviceId)
      sensorData.value = res.data
      return res
    } catch (error) {
      console.error('获取传感器数据失败', error)
      throw error
    }
  }

  // 从本地存储恢复
  const restoreFromStorage = () => {
    const storedDevice = uni.getStorageSync('currentDevice')
    if (storedDevice) {
      currentDevice.value = storedDevice
    }
  }

  return {
    currentDevice,
    deviceList,
    deviceStatus,
    deviceLocation,
    sensorData,
    setCurrentDevice,
    fetchDeviceList,
    fetchDeviceStatus,
    fetchDeviceLocation,
    fetchSensorData,
    restoreFromStorage
  }
})
