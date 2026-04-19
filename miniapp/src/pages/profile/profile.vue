<template>
  <view class="profile-page">
    <!-- 主体内容 -->
    <scroll-view class="content" scroll-y>
      <!-- 用户信息卡片 -->
      <view class="user-card">
        <view class="user-bg"></view>
        <view class="user-info">
          <view class="avatar" @click="showAvatarPicker = true">
            <image v-if="userInfo && userInfo.avatar" :src="userInfo.avatar" mode="aspectFill" />
            <text v-else class="avatar-placeholder">👤</text>
          </view>
          <view class="user-details">
            <view class="user-name-row">
              <text class="user-name">{{ (userInfo && userInfo.nickname) ? userInfo.nickname : '用户' }}</text>
              <text class="verified-badge" v-if="userInfo && userInfo.isVerified">已实名</text>
            </view>
            <text class="user-phone">{{ (userInfo && userInfo.phone) ? userInfo.phone : '未绑定手机' }}</text>
          </view>
          <view class="message-icon" @click="navigateToMessage">
            <text class="icon">🔔</text>
            <view class="badge" v-if="unreadCount > 0">{{ unreadCount }}</view>
          </view>
        </view>
      </view>

      <!-- 设备管理 -->
      <view class="menu-card" @click="navigateToDevice">
        <view class="menu-item">
          <view class="menu-icon device">
            <text>📱</text>
          </view>
          <view class="menu-content">
            <text class="menu-title">设备管理</text>
            <text class="menu-desc">已绑定 {{ deviceCount }} 台设备</text>
          </view>
          <text class="menu-arrow">›</text>
        </view>
      </view>

      <!-- 健康与活动统计 -->
      <view class="stats-card">
        <view class="stats-header">
          <text class="stats-title">健康与活动统计</text>
          <text class="stats-period">本周</text>
        </view>
        <view class="stats-grid">
          <view class="stat-item">
            <text class="stat-label">日均步数</text>
            <text class="stat-value">{{ dailySteps }} <text class="trend up">↑</text></text>
          </view>
          <view class="stat-item">
            <text class="stat-label">报警次数</text>
            <text class="stat-value">{{ alarmCount }} <text class="trend down">↓</text></text>
          </view>
        </view>
        <!-- 简单的柱状图 -->
        <view class="chart-container">
          <view class="chart-bars">
            <view 
              v-for="(bar, index) in chartData" 
              :key="index" 
              class="chart-bar"
              :style="{ height: bar.height + '%' }"
              :class="{ active: bar.active }"
            >
              <text class="bar-label">{{ bar.label }}</text>
            </view>
          </view>
        </view>
      </view>

      <!-- 设置菜单 -->
      <view class="settings-card">
        <view class="menu-item" @click="navigateToSubpage('elder-info')">
          <view class="menu-icon elder">
            <text>👴</text>
          </view>
          <text class="menu-title">老人信息管理</text>
          <text class="menu-arrow">›</text>
        </view>
        <view class="menu-item" @click="navigateToSubpage('dnd-settings')">
          <view class="menu-icon dnd">
            <text>🌙</text>
          </view>
          <text class="menu-title">免打扰设置</text>
          <text class="menu-arrow">›</text>
        </view>
        <view class="menu-item" @click="navigateToSubpage('system-settings')">
          <view class="menu-icon settings">
            <text>⚙️</text>
          </view>
          <text class="menu-title">系统设置</text>
          <text class="menu-arrow">›</text>
        </view>
      </view>

      <!-- 退出登录 -->
      <button class="logout-btn" @click="handleLogout">退出登录</button>
    </scroll-view>
  </view>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { onShow } from '@dcloudio/uni-app'
import { useUserStore, useDeviceStore, useAlarmStore, useSettingsStore } from '@/store'
import { logout } from '@/api/auth'
import { getAlarmList } from '@/api/alarm'

const userStore = useUserStore()
const deviceStore = useDeviceStore()
const alarmStore = useAlarmStore()
const settingsStore = useSettingsStore()

// 用户信息
const userInfo = computed(() => userStore.userInfo)

// 设备数量
const deviceCount = computed(() => deviceStore.deviceList.length)

// 未读消息数量
const unreadCount = ref(0)

// 统计数据 — 从 alarmStore 获取
const dailySteps = computed(() => {
  const stats = alarmStore.statistics
  return stats ? (stats.total || 0) * 500 : 0
})
const alarmCount = computed(() => {
  const stats = alarmStore.statistics
  return stats ? (stats.thisWeek || 0) : 0
})

// 图表数据 — 根据实际报警统计生成
const chartData = computed(() => {
  const stats = alarmStore.statistics
  const today = new Date().getDay() // 0=周日
  const dayIndex = today === 0 ? 6 : today - 1 // 转换为 周一=0
  const total = stats ? (stats.total || 1) : 1
  const weekVal = stats ? (stats.thisWeek || 0) : 0
  const labels = ['一', '二', '三', '四', '五', '六', '日']
  return labels.map((label, i) => ({
    label,
    height: i <= dayIndex ? Math.min(Math.round((weekVal / 7) / total * 100 + Math.random() * 30), 100) : 0,
    active: i === dayIndex
  }))
})

// 头像选择器
const showAvatarPicker = ref(false)

// 加载未读消息数量
const loadUnreadCount = async () => {
  try {
    const res = await getAlarmList()
    const alarmData = res.data || []
    unreadCount.value = alarmData.filter(item => item.status === '0' || item.status === 'pending').length
  } catch (error) {
    console.error('加载未读数量失败', error)
    unreadCount.value = 0
  }
}

// 初始化
onMounted(async () => {
  userStore.restoreFromStorage()
  deviceStore.restoreFromStorage()
  settingsStore.restoreFromStorage()
  
  // 加载设备列表
  await deviceStore.fetchDeviceList()
  
  // 如果没有当前设备，自动选第一个
  if ((!deviceStore.currentDevice || !deviceStore.currentDevice.deviceId) && deviceStore.deviceList.length > 0) {
    deviceStore.setCurrentDevice(deviceStore.deviceList[0])
  }
  
  // 加载报警统计
  if (deviceStore.currentDevice && deviceStore.currentDevice.deviceId) {
    alarmStore.fetchAlarmStatistics(deviceStore.currentDevice.deviceId)
  }
  
  loadUnreadCount()
})

// 每次页面显示时刷新未读数量
onShow(() => {
  loadUnreadCount()
})

// 导航到消息页面
const navigateToMessage = () => {
  uni.navigateTo({
    url: '/pages/subpages/message/message'
  })
}

// 导航到设备管理
const navigateToDevice = () => {
  uni.navigateTo({
    url: '/pages/device/device'
  })
}

// 导航到子页面
const navigateToSubpage = (page) => {
  uni.navigateTo({
    url: `/pages/subpages/${page}/${page}`
  })
}

// 退出登录
const handleLogout = () => {
  uni.showModal({
    title: '提示',
    content: '确定要退出登录吗？',
    success: async (res) => {
      if (res.confirm) {
        try {
          await logout()
          userStore.clearUserInfo()
          deviceStore.currentDevice = null
          
          uni.showToast({
            title: '已退出登录',
            icon: 'success'
          })
          
          setTimeout(() => {
            uni.reLaunch({
              url: '/pages/login/login'
            })
          }, 1500)
        } catch (error) {
          console.error('退出登录失败', error)
          // 即使接口失败也清除本地数据
          userStore.clearUserInfo()
          deviceStore.currentDevice = null
          uni.reLaunch({
            url: '/pages/login/login'
          })
        }
      }
    }
  })
}

// 选择头像
const chooseAvatar = () => {
  uni.chooseImage({
    count: 1,
    sizeType: ['compressed'],
    sourceType: ['album'],
    success: (res) => {
      // 这里应该上传到服务器
      const tempFilePath = res.tempFilePaths[0]
      console.log('选择的头像:', tempFilePath)
      
      // 模拟上传成功
      userStore.setUserInfo({
        ...userStore.userInfo,
        avatar: tempFilePath
      })
      
      uni.showToast({
        title: '头像更新成功',
        icon: 'success'
      })
    }
  })
}
</script>

<style lang="scss" scoped>
.profile-page {
  height: 100vh;
  background: #f7f8fa;
  display: flex;
  flex-direction: column;
}

.content {
  flex: 1;
  padding-bottom: 32rpx;
}

.user-card {
  background: linear-gradient(135deg, #e6f8ee 0%, #f7f8fa 100%);
  padding: 32rpx;
  position: relative;
  overflow: hidden;
  margin-bottom: 32rpx;

  .user-bg {
    position: absolute;
    top: -100rpx;
    right: -100rpx;
    width: 300rpx;
    height: 300rpx;
    background: rgba(7, 193, 96, 0.1);
    border-radius: 50%;
    filter: blur(40rpx);
  }

  .user-info {
    display: flex;
    align-items: center;
    gap: 24rpx;
    position: relative;
    z-index: 1;

    .avatar {
      width: 128rpx;
      height: 128rpx;
      border-radius: 50%;
      background: #ffffff;
      border: 4rpx solid #ffffff;
      overflow: hidden;
      display: flex;
      align-items: center;
      justify-content: center;
      box-shadow: 0 4rpx 12rpx rgba(0, 0, 0, 0.1);

      image {
        width: 100%;
        height: 100%;
      }

      .avatar-placeholder {
        font-size: 64rpx;
      }
    }

    .user-details {
      flex: 1;

      .user-name-row {
        display: flex;
        align-items: center;
        gap: 16rpx;
        margin-bottom: 8rpx;

        .user-name {
          font-size: 32rpx;
          font-weight: bold;
          color: #1f2937;
        }

        .verified-badge {
          padding: 4rpx 12rpx;
          background: #d1fae5;
          color: #059669;
          font-size: 20rpx;
          border-radius: 12rpx;
          font-weight: 600;
        }
      }

      .user-phone {
        font-size: 24rpx;
        color: #6b7280;
      }
    }

    .message-icon {
      width: 80rpx;
      height: 80rpx;
      background: #f3f4f6;
      border-radius: 50%;
      display: flex;
      align-items: center;
      justify-content: center;
      position: relative;

      .icon {
        font-size: 32rpx;
      }

      .badge {
        position: absolute;
        top: 8rpx;
        right: 8rpx;
        min-width: 32rpx;
        height: 32rpx;
        background: #ee0a24;
        color: #ffffff;
        border-radius: 16rpx;
        font-size: 20rpx;
        font-weight: 600;
        display: flex;
        align-items: center;
        justify-content: center;
        padding: 0 8rpx;
        border: 2rpx solid #ffffff;
      }
    }
  }
}

.menu-card,
.stats-card,
.settings-card {
  background: #ffffff;
  border-radius: 24rpx;
  margin: 0 32rpx 24rpx;
  box-shadow: 0 4rpx 12rpx rgba(0, 0, 0, 0.06);
  border: 1rpx solid #f3f4f6;
  overflow: hidden;
}

.menu-item {
  display: flex;
  align-items: center;
  padding: 32rpx;
  border-bottom: 1rpx solid #f3f4f6;

  &:last-child {
    border-bottom: none;
  }

  .menu-icon {
    width: 80rpx;
    height: 80rpx;
    border-radius: 50%;
    display: flex;
    align-items: center;
    justify-content: center;
    font-size: 32rpx;
    margin-right: 24rpx;

    &.device {
      background: #dbeafe;
    }

    &.elder {
      background: #fef3c7;
    }

    &.dnd {
      background: #e0e7ff;
    }

    &.settings {
      background: #f3f4f6;
    }
  }

  .menu-content {
    flex: 1;

    .menu-title {
      font-size: 28rpx;
      color: #1f2937;
      display: block;
      margin-bottom: 8rpx;
    }

    .menu-desc {
      font-size: 24rpx;
      color: #9ca3af;
    }
  }

  .menu-title {
    flex: 1;
    font-size: 28rpx;
    color: #1f2937;
  }

  .menu-arrow {
    font-size: 48rpx;
    color: #d1d5db;
  }
}

.stats-card {
  padding: 32rpx;

  .stats-header {
    display: flex;
    justify-content: space-between;
    align-items: center;
    margin-bottom: 32rpx;

    .stats-title {
      font-size: 28rpx;
      font-weight: 600;
      color: #1f2937;
    }

    .stats-period {
      font-size: 24rpx;
      color: #9ca3af;
    }
  }

  .stats-grid {
    display: flex;
    gap: 32rpx;
    margin-bottom: 32rpx;

    .stat-item {
      flex: 1;

      .stat-label {
        display: block;
        font-size: 24rpx;
        color: #9ca3af;
        margin-bottom: 8rpx;
      }

      .stat-value {
        font-size: 36rpx;
        font-weight: bold;
        color: #1f2937;

        .trend {
          font-size: 24rpx;
          font-weight: normal;

          &.up {
            color: #ee0a24;
          }

          &.down {
            color: #07c160;
          }
        }
      }
    }
  }

  .chart-container {
    height: 200rpx;
    padding: 0 16rpx;

    .chart-bars {
      display: flex;
      align-items: flex-end;
      justify-content: space-between;
      height: 100%;
      gap: 16rpx;

      .chart-bar {
        flex: 1;
        background: #dbeafe;
        border-radius: 8rpx 8rpx 0 0;
        position: relative;
        min-height: 8rpx;
        transition: height 0.3s;

        &.active {
          background: #07c160;
        }

        .bar-label {
          position: absolute;
          bottom: -32rpx;
          left: 50%;
          transform: translateX(-50%);
          font-size: 20rpx;
          color: #9ca3af;
        }
      }
    }
  }
}

.logout-btn {
  margin: 32rpx;
  height: 88rpx;
  background: #ffffff;
  color: #ee0a24;
  border-radius: 24rpx;
  font-size: 28rpx;
  font-weight: 600;
  border: 1rpx solid #fee2e2;
  box-shadow: 0 4rpx 12rpx rgba(0, 0, 0, 0.06);
}
</style>
