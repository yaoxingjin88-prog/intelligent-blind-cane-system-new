<template>
  <view class="home-page">
    <!-- 主体内容 -->
    <scroll-view class="content" scroll-y>
      <!-- 地图区域 -->
      <view class="map-section">
        <map
          id="map"
          class="map"
          :longitude="longitude"
          :latitude="latitude"
          :scale="scale"
          :markers="markers"
          :polyline="polyline"
          :show-location="true"
          @markertap="onMarkerTap"
        >
          <!-- 地图控制按钮 -->
          <view class="map-controls">
            <view class="control-btn" @click="switchMapType">
              <text class="icon">🗺️</text>
            </view>
          </view>
          <view class="map-controls-right">
            <view class="control-btn" @click="navigateToFence">
              <text class="icon">📍</text>
            </view>
            <view class="control-btn" @click="centerOnDevice">
              <text class="icon">🎯</text>
            </view>
          </view>
        </map>
        
        <!-- 位置信息卡片 -->
        <view class="location-card">
          <text class="location-text">{{ currentAddress || '正在获取位置...' }}</text>
        </view>
      </view>

      <!-- 设备状态面板 -->
      <view class="device-status">
        <view class="status-header">
          <view class="status-title">
            <text class="icon">📡</text>
            <text class="title">{{ currentDevice?.deviceName || '智能盲杖' }}</text>
            <text class="status-badge" :class="deviceStatus?.status">
              {{ deviceStatus?.status === 'online' ? '在线' : '离线' }}
            </text>
          </view>
          <view class="battery-badge">
            <text class="icon">🔋</text>
            <text>{{ deviceStatus?.batteryLevel || 0 }}%</text>
          </view>
        </view>

        <!-- 传感器数据 -->
        <view class="sensor-grid">
          <view class="sensor-item">
            <text class="sensor-label">前方障碍</text>
            <text class="sensor-value">{{ sensorData?.obstacleDistance || 0 }}<text class="unit">m</text></text>
          </view>
          <view class="sensor-item">
            <text class="sensor-label">今日步数</text>
            <text class="sensor-value">{{ todaySteps || 0 }}</text>
          </view>
          <view class="sensor-item">
            <text class="sensor-label">静止时长</text>
            <text class="sensor-value">{{ stationaryTime || 0 }}<text class="unit">min</text></text>
          </view>
        </view>
      </view>

      <!-- 安全守护 -->
      <view class="safety-section">
        <text class="section-title">安全守护</text>
        <view class="safety-grid">
          <view class="safety-card fall">
            <view class="safety-icon">🚨</view>
            <view class="safety-info">
              <text class="safety-title">跌倒检测</text>
              <text class="safety-desc">系统已开启</text>
            </view>
          </view>
          <view class="safety-card sos">
            <view class="safety-icon">🔔</view>
            <view class="safety-info">
              <text class="safety-title">一键 SOS</text>
              <text class="safety-desc">长按盲杖按钮</text>
            </view>
          </view>
        </view>
      </view>

      <!-- 最新动态 -->
      <view class="activity-section">
        <view class="section-header">
          <text class="section-title">最新动态</text>
          <text class="more-btn" @click="navigateToAlarm">查看更多</text>
        </view>
        <view class="activity-list">
          <view v-for="item in recentActivities" :key="item.id" class="activity-item">
            <view class="activity-icon" :class="item.type">
              <text>{{ item.icon }}</text>
            </view>
            <view class="activity-content">
              <text class="activity-title">{{ item.title }}</text>
              <text class="activity-time">{{ item.time }}</text>
            </view>
          </view>
        </view>
      </view>
    </scroll-view>
  </view>
</template>

<script setup>
import { ref, computed, onMounted, onUnmounted } from 'vue'
import { useUserStore, useDeviceStore, useAlarmStore } from '@/store'
import { fetchDeviceLocation, fetchSensorData, fetchDeviceStatus } from '@/api/device'
import { formatRelativeTime } from '@/utils'

const userStore = useUserStore()
const deviceStore = useDeviceStore()
const alarmStore = useAlarmStore()

// 地图相关
const longitude = ref(116.4074)
const latitude = ref(39.9042)
const scale = ref(14)
const markers = ref([])
const polyline = ref([])
const currentAddress = ref('')

// 设备相关
const currentDevice = computed(() => deviceStore.currentDevice)
const deviceStatus = computed(() => deviceStore.deviceStatus)
const sensorData = computed(() => deviceStore.sensorData)

// 传感器数据
const todaySteps = ref(4521)
const stationaryTime = ref(5)

// 最新动态
const recentActivities = ref([
  { id: 1, type: 'alarm', icon: '🚨', title: '跌倒报警已处理', time: '10分钟前' },
  { id: 2, type: 'location', icon: '📍', title: '到达社区公园', time: '1小时前' },
  { id: 3, type: 'battery', icon: '🔋', title: '设备充电完成', time: '昨天 18:30' }
])

// 定时器
let locationTimer = null
let dataTimer = null

// 初始化
onMounted(() => {
  deviceStore.restoreFromStorage()
  loadDeviceData()
  startAutoRefresh()
})

onUnmounted(() => {
  stopAutoRefresh()
})

// 加载设备数据
const loadDeviceData = async () => {
  if (!currentDevice.value?.deviceId) {
    // 如果没有设备，显示提示但不跳转
    return
  }

  try {
    await Promise.all([
      deviceStore.fetchDeviceStatus(currentDevice.value.deviceId),
      deviceStore.fetchDeviceLocation(currentDevice.value.deviceId),
      deviceStore.fetchSensorData(currentDevice.value.deviceId)
    ])

    // 更新地图位置
    if (deviceStore.deviceLocation) {
      longitude.value = deviceStore.deviceLocation.longitude
      latitude.value = deviceStore.deviceLocation.latitude
      currentAddress.value = deviceStore.deviceLocation.address || '未知位置'
      
      // 添加标记
      markers.value = [{
        id: 1,
        longitude: deviceStore.deviceLocation.longitude,
        latitude: deviceStore.deviceLocation.latitude,
        iconPath: '/static/images/marker.png',
        width: 32,
        height: 32,
        callout: {
          content: currentDevice.value.deviceName,
          color: '#333333',
          fontSize: 12,
          borderRadius: 8,
          bgColor: '#ffffff',
          padding: 8,
          display: 'ALWAYS'
        }
      }]
    }
  } catch (error) {
    console.error('加载设备数据失败', error)
  }
}

// 开始自动刷新
const startAutoRefresh = () => {
  // 每30秒刷新位置
  locationTimer = setInterval(() => {
    loadDeviceData()
  }, 30000)

  // 每60秒刷新传感器数据
  dataTimer = setInterval(() => {
    if (currentDevice.value?.deviceId) {
      deviceStore.fetchSensorData(currentDevice.value.deviceId)
    }
  }, 60000)
}

// 停止自动刷新
const stopAutoRefresh = () => {
  if (locationTimer) {
    clearInterval(locationTimer)
    locationTimer = null
  }
  if (dataTimer) {
    clearInterval(dataTimer)
    dataTimer = null
  }
}

// 切换地图类型
const switchMapType = () => {
  uni.showToast({
    title: '切换地图类型',
    icon: 'none'
  })
}

// 跳转到围栏页面
const navigateToFence = () => {
  uni.switchTab({
    url: '/pages/fence/fence'
  })
}

// 定位到设备
const centerOnDevice = () => {
  if (deviceStore.deviceLocation) {
    longitude.value = deviceStore.deviceLocation.longitude
    latitude.value = deviceStore.deviceLocation.latitude
  }
}

// 跳转到报警页面
const navigateToAlarm = () => {
  uni.switchTab({
    url: '/pages/alarm/alarm'
  })
}

// 标记点击
const onMarkerTap = (e) => {
  console.log('Marker tapped', e)
}
</script>

<style lang="scss" scoped>
.home-page {
  height: 100vh;
  background: #f7f8fa;
  display: flex;
  flex-direction: column;
}

.content {
  flex: 1;
  padding-bottom: 32rpx;
}

.map-section {
  height: 512rpx;
  position: relative;

  .map {
    width: 100%;
    height: 100%;
  }

  .map-controls {
    position: absolute;
    top: 24rpx;
    right: 24rpx;
    display: flex;
    flex-direction: column;
    gap: 16rpx;

    .control-btn {
      width: 72rpx;
      height: 72rpx;
      background: rgba(255, 255, 255, 0.95);
      border-radius: 50%;
      display: flex;
      align-items: center;
      justify-content: center;
      box-shadow: 0 4rpx 12rpx rgba(0, 0, 0, 0.1);

      .icon {
        font-size: 32rpx;
      }
    }
  }

  .map-controls-right {
    position: absolute;
    bottom: 24rpx;
    right: 24rpx;
    display: flex;
    flex-direction: column;
    gap: 16rpx;

    .control-btn {
      width: 72rpx;
      height: 72rpx;
      background: rgba(255, 255, 255, 0.95);
      border-radius: 50%;
      display: flex;
      align-items: center;
      justify-content: center;
      box-shadow: 0 4rpx 12rpx rgba(0, 0, 0, 0.1);

      .icon {
        font-size: 32rpx;
      }
    }
  }

  .location-card {
    position: absolute;
    bottom: 120rpx;
    left: 50%;
    transform: translateX(-50%);
    background: rgba(255, 255, 255, 0.95);
    padding: 16rpx 32rpx;
    border-radius: 48rpx;
    box-shadow: 0 4rpx 12rpx rgba(0, 0, 0, 0.1);

    .location-text {
      font-size: 24rpx;
      color: #374151;
      font-weight: 600;
    }
  }
}

.device-status {
  margin: -32rpx 32rpx 32rpx;
  background: #ffffff;
  border-radius: 24rpx;
  padding: 32rpx;
  box-shadow: 0 4rpx 12rpx rgba(0, 0, 0, 0.06);
  position: relative;
  z-index: 10;

  .status-header {
    display: flex;
    justify-content: space-between;
    align-items: center;
    margin-bottom: 32rpx;

    .status-title {
      display: flex;
      align-items: center;
      gap: 12rpx;

      .icon {
        font-size: 32rpx;
      }

      .title {
        font-size: 28rpx;
        font-weight: 600;
        color: #1f2937;
      }

      .status-badge {
        padding: 8rpx 16rpx;
        border-radius: 16rpx;
        font-size: 20rpx;
        font-weight: 600;

        &.online {
          background: #d1fae5;
          color: #059669;
        }

        &.offline {
          background: #fee2e2;
          color: #dc2626;
        }
      }
    }

    .battery-badge {
      display: flex;
      align-items: center;
      gap: 8rpx;
      padding: 8rpx 16rpx;
      background: #ecfdf5;
      border-radius: 16rpx;
      font-size: 24rpx;
      color: #059669;
      font-weight: 600;

      .icon {
        font-size: 24rpx;
      }
    }
  }

  .sensor-grid {
    display: grid;
    grid-template-columns: repeat(3, 1fr);
    gap: 24rpx;

    .sensor-item {
      background: #f9fafb;
      border-radius: 16rpx;
      padding: 24rpx;
      text-align: center;

      .sensor-label {
        display: block;
        font-size: 24rpx;
        color: #9ca3af;
        margin-bottom: 8rpx;
      }

      .sensor-value {
        font-size: 32rpx;
        font-weight: bold;
        color: #1f2937;

        .unit {
          font-size: 20rpx;
          font-weight: normal;
          color: #6b7280;
        }
      }
    }
  }
}

.safety-section {
  padding: 0 32rpx 32rpx;

  .section-title {
    font-size: 28rpx;
    font-weight: 600;
    color: #1f2937;
    display: block;
    margin-bottom: 24rpx;
  }

  .safety-grid {
    display: grid;
    grid-template-columns: repeat(2, 1fr);
    gap: 24rpx;

    .safety-card {
      padding: 32rpx;
      border-radius: 24rpx;
      display: flex;
      align-items: center;
      justify-content: space-between;
      border: 1rpx solid #f3f4f6;

      &.fall {
        background: linear-gradient(135deg, #fee2e2 0%, #ffffff 100%);
        border-color: #fecaca;
      }

      &.sos {
        background: linear-gradient(135deg, #dbeafe 0%, #ffffff 100%);
        border-color: #bfdbfe;
      }

      .safety-icon {
        width: 80rpx;
        height: 80rpx;
        border-radius: 50%;
        display: flex;
        align-items: center;
        justify-content: center;
        font-size: 36rpx;
      }

      .fall .safety-icon {
        background: #fee2e2;
      }

      .sos .safety-icon {
        background: #dbeafe;
      }

      .safety-info {
        flex: 1;
        margin-left: 24rpx;

        .safety-title {
          display: block;
          font-size: 28rpx;
          font-weight: 600;
          color: #1f2937;
          margin-bottom: 8rpx;
        }

        .safety-desc {
          display: block;
          font-size: 24rpx;
          color: #6b7280;
        }
      }
    }
  }
}

.activity-section {
  padding: 0 32rpx;

  .section-header {
    display: flex;
    justify-content: space-between;
    align-items: center;
    margin-bottom: 24rpx;

    .section-title {
      font-size: 28rpx;
      font-weight: 600;
      color: #1f2937;
    }

    .more-btn {
      font-size: 24rpx;
      color: #07c160;
    }
  }

  .activity-list {
    display: flex;
    flex-direction: column;
    gap: 24rpx;

    .activity-item {
      display: flex;
      align-items: center;
      gap: 24rpx;
      padding: 24rpx;
      background: #ffffff;
      border-radius: 16rpx;
      border: 1rpx solid #f3f4f6;

      .activity-icon {
        width: 64rpx;
        height: 64rpx;
        border-radius: 50%;
        display: flex;
        align-items: center;
        justify-content: center;
        font-size: 32rpx;

        &.alarm {
          background: #fee2e2;
        }

        &.location {
          background: #dbeafe;
        }

        &.battery {
          background: #fef3c7;
        }
      }

      .activity-content {
        flex: 1;

        .activity-title {
          display: block;
          font-size: 28rpx;
          color: #1f2937;
          margin-bottom: 8rpx;
        }

        .activity-time {
          display: block;
          font-size: 24rpx;
          color: #9ca3af;
        }
      }
    }
  }
}
</style>
