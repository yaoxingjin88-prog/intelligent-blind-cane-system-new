import { defineStore } from 'pinia'
import { ref, computed } from 'vue'

export { useDeviceStore } from './device'
export { useAlarmStore } from './alarm'
export { useElderStore } from './elder'
export { useSettingsStore } from './settings'

export const useUserStore = defineStore('user', () => {
  // 用户信息
  const userInfo = ref(null)
  const token = ref('')

  // 是否已登录
  const isLoggedIn = computed(() => !!token.value)

  // 设置用户信息
  const setUserInfo = (info) => {
    userInfo.value = info
    uni.setStorageSync('userInfo', info)
  }

  // 设置token
  const setToken = (newToken) => {
    token.value = newToken
    uni.setStorageSync('token', newToken)
  }

  // 清除用户信息
  const clearUserInfo = () => {
    userInfo.value = null
    token.value = ''
    uni.removeStorageSync('userInfo')
    uni.removeStorageSync('token')
  }

  // 从本地存储恢复
  const restoreFromStorage = () => {
    const storedToken = uni.getStorageSync('token')
    const storedUserInfo = uni.getStorageSync('userInfo')
    if (storedToken) {
      token.value = storedToken
    }
    if (storedUserInfo) {
      userInfo.value = storedUserInfo
    }
  }

  return {
    userInfo,
    token,
    isLoggedIn,
    setUserInfo,
    setToken,
    clearUserInfo,
    restoreFromStorage
  }
})
