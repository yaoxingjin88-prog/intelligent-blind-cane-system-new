import { defineStore } from 'pinia'
import { ref } from 'vue'

export const useElderStore = defineStore('elder', () => {
  // 老人信息
  const elderInfo = ref({
    name: '',
    age: '',
    gender: '男',
    bloodType: '未知',
    phone: '',
    emergencyContact: '',
    emergencyPhone: '',
    address: '',
    medicalHistory: ''
  })

  // 设置老人信息
  const setElderInfo = (info) => {
    elderInfo.value = { ...elderInfo.value, ...info }
    uni.setStorageSync('elderInfo', elderInfo.value)
  }

  // 从本地存储恢复
  const restoreFromStorage = () => {
    const storedElderInfo = uni.getStorageSync('elderInfo')
    if (storedElderInfo) {
      elderInfo.value = storedElderInfo
    }
  }

  return {
    elderInfo,
    setElderInfo,
    restoreFromStorage
  }
}, {
  persist: {
    enabled: true,
    strategies: [
      {
        key: 'elder',
        storage: {
          getItem: uni.getStorageSync,
          setItem: uni.setStorageSync
        }
      }
    ]
  }
})
