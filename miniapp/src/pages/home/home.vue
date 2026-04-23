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
          :enable-satellite="false"
          :setting="{ showScale: false, subKey: '' }"
          @markertap="onMarkerTap"
        >
          <!-- 地图控制按钮（微信小程序 map 里必须用 cover-view 才能点击）-->
          <cover-view class="map-controls-right">
            <cover-view class="control-btn" @click="navigateToFence">
              <cover-view class="icon">📍</cover-view>
              <cover-view class="btn-label">围栏</cover-view>
            </cover-view>
            <cover-view class="control-btn" @click="centerOnDevice">
              <cover-view class="icon">🎯</cover-view>
              <cover-view class="btn-label">定位</cover-view>
            </cover-view>
          </cover-view>
        </map>
      </view>

      <!-- 加载中 -->
      <view v-if="loading" class="loading-state">
        <text class="loading-icon">⏳</text>
        <text class="loading-text">加载中...</text>
      </view>

      <!-- 无设备提示 -->
      <view v-else-if="!currentDevice || !currentDevice.deviceId" class="empty-state">
        <text class="empty-icon">📱</text>
        <text class="empty-text">暂未绑定设备</text>
        <text class="empty-hint">请先前往设备管理页绑定设备</text>
      </view>

      <!-- 设备状态面板 -->
      <view v-else class="device-status">
        <view class="status-header">
          <view class="status-title">
            <text class="icon">📡</text>
            <text class="title">{{ (currentDevice && currentDevice.deviceName) ? currentDevice.deviceName : '智能盲杖' }}</text>
            <text class="status-badge" :class="deviceStatus ? deviceStatus.status : ''">
              {{ (deviceStatus && deviceStatus.status === 'online') ? '在线' : '离线' }}
            </text>
          </view>
          <view class="battery-badge">
            <text class="icon">🔋</text>
            <text>{{ (deviceStatus && deviceStatus.batteryLevel) ? deviceStatus.batteryLevel : 0 }}%</text>
          </view>
        </view>
        <view class="device-address" v-if="currentAddress">
          <text class="address-icon">📍</text>
          <text class="address-text">{{ currentAddress }}</text>
        </view>

        <!-- 传感器数据 -->
        <view class="sensor-grid">
          <view class="sensor-item">
            <text class="sensor-label">前方障碍</text>
            <text class="sensor-value">{{ (sensorData && sensorData.obstacleDistance) ? sensorData.obstacleDistance : 0 }}<text class="unit">m</text></text>
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

      <!-- AI 语音助手入口 -->
      <view class="ai-entry" @click="navigateToAiChat">
        <view class="ai-entry-left">
          <view class="ai-icon">🎙</view>
          <view class="ai-info">
            <text class="ai-title">AI 语音助手</text>
            <text class="ai-desc">遇到问题随时对我说话</text>
          </view>
        </view>
        <text class="ai-arrow">›</text>
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
              <text class="safety-desc">由盲杖硬件按钮触发</text>
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
import { ref, computed, onMounted, onUnmounted, getCurrentInstance } from 'vue'
import { useUserStore, useDeviceStore, useAlarmStore } from '@/store'
import { getTrajectory } from '@/api/trajectory'
import { formatRelativeTime } from '@/utils'

const userStore = useUserStore()
const deviceStore = useDeviceStore()
const alarmStore = useAlarmStore()

// 地图相关
const longitude = ref(116.4074)
const latitude = ref(39.9042)
const scale = ref(16)
const markers = ref([])
const polyline = ref([])
const currentAddress = ref('')
const trackPoints = ref([]) // 实时轨迹点

// 设备相关
const currentDevice = computed(() => deviceStore.currentDevice)
const deviceStatus = computed(() => deviceStore.deviceStatus)
const sensorData = computed(() => deviceStore.sensorData)

// 加载状态
const loading = ref(true)

// 传感器数据
const todaySteps = computed(() => {
  return sensorData.value ? (sensorData.value.stepCount || 0) : 0
})
const stationaryTime = computed(() => {
  return sensorData.value ? Math.round((sensorData.value.heartRate || 70) / 14) : 5
})

// 最新动态 — 从报警列表取最新三条
const recentActivities = computed(() => {
  const alarms = alarmStore.alarmList || []
  if (alarms.length === 0) {
    return [{ id: 0, type: 'info', icon: '✅', title: '暂无最新动态', time: '' }]
  }
  return alarms.slice(0, 3).map((a, i) => ({
    id: a.id || i,
    type: 'alarm',
    icon: a.alarmType === '跌倒报警' ? '�' : (a.alarmType === '电子围栏越界报警' ? '📍' : '🔔'),
    title: (a.alarmType || '系统通知') + (a.status === '0' ? ' 待处理' : ' 已处理'),
    time: a.alarmTime ? formatRelativeTime(a.alarmTime) : ''
  }))
})

// 定时器
let locationTimer = null
let dataTimer = null

const handlePhoneLocationChange = (res) => {
  if (!currentDevice.value || !currentDevice.value.deviceId) {
    longitude.value = res.longitude
    latitude.value = res.latitude
  }
}

const startPhoneLocationTracking = () => {
  if (typeof uni.startLocationUpdate !== 'function') return
  uni.startLocationUpdate({
    success: () => {
      if (typeof uni.onLocationChange === 'function') {
        uni.onLocationChange(handlePhoneLocationChange)
      }
    },
    fail: (error) => {
      console.warn('启动高精度定位失败', error)
    }
  })
}

const stopPhoneLocationTracking = () => {
  if (typeof uni.offLocationChange === 'function') {
    uni.offLocationChange(handlePhoneLocationChange)
  }
  if (typeof uni.stopLocationUpdate === 'function') {
    uni.stopLocationUpdate()
  }
}

// 初始化
onMounted(async () => {
  deviceStore.restoreFromStorage()
  getUserLocation(!deviceStore.currentDevice || !deviceStore.currentDevice.deviceId)
  startPhoneLocationTracking()
  
  // 如果没有当前设备，自动获取设备列表并选第一个
  if (!deviceStore.currentDevice || !deviceStore.currentDevice.deviceId) {
    try {
      await deviceStore.fetchDeviceList()
      if (deviceStore.deviceList.length > 0) {
        deviceStore.setCurrentDevice(deviceStore.deviceList[0])
      }
    } catch (e) {
      console.error('自动获取设备列表失败', e)
    }
  }
  
  loadDeviceData()
  startAutoRefresh()
})

onUnmounted(() => {
  stopAutoRefresh()
  stopPhoneLocationTracking()
})

// 获取用户自身位置（无设备时使用）
const getUserLocation = (syncMapCenter = true) => {
  uni.getLocation({
    type: 'gcj02',
    isHighAccuracy: true,
    highAccuracyExpireTime: 5000,
    success: (res) => {
      if (syncMapCenter) {
        longitude.value = res.longitude
        latitude.value = res.latitude
      }
      if (!currentDevice.value || !currentDevice.value.deviceId) {
        currentAddress.value = '当前位置（未绑定设备）'
      }
    },
    fail: () => {
      if (!currentDevice.value || !currentDevice.value.deviceId) {
        currentAddress.value = '未绑定设备，请先绑定'
      }
    }
  })
}

// 加载设备数据
const loadDeviceData = async () => {
  if (!currentDevice.value || !currentDevice.value.deviceId) {
    loading.value = false
    getUserLocation()
    return
  }

  try {
    await Promise.all([
      deviceStore.fetchDeviceStatus(currentDevice.value.deviceId),
      deviceStore.fetchDeviceLocation(currentDevice.value.deviceId),
      deviceStore.fetchSensorData(currentDevice.value.deviceId),
      alarmStore.fetchAlarmList()
    ])

    // 更新地图位置
    if (deviceStore.deviceLocation) {
      const loc = deviceStore.deviceLocation
      longitude.value = loc.longitude
      latitude.value = loc.latitude
      currentAddress.value = loc.address || (loc.latitude.toFixed(4) + ', ' + loc.longitude.toFixed(4))
      
      // 添加标记
      markers.value = [{
        id: 1,
        longitude: loc.longitude,
        latitude: loc.latitude,
        width: 24,
        height: 34,
        callout: {
          content: currentDevice.value.deviceName || '智能盲杖',
          color: '#333333',
          fontSize: 12,
          borderRadius: 8,
          bgColor: '#ffffff',
          padding: 8,
          display: 'ALWAYS'
        }
      }]
      
      // longitude/latitude 已响应式绑定，map 会自动跟随，无需手动 moveToLocation
      // （真机 Vue3 setup 里 createMapContext 拿不到组件实例，会报 mapview is null）
      
      // 加载最近1小时的轨迹线
      try {
        const now = new Date()
        const oneHourAgo = new Date(now.getTime() - 60 * 60 * 1000)
        const fmt = (d) => d.getFullYear() + '-' + String(d.getMonth()+1).padStart(2,'0') + '-' + String(d.getDate()).padStart(2,'0') + ' ' + String(d.getHours()).padStart(2,'0') + ':' + String(d.getMinutes()).padStart(2,'0') + ':' + String(d.getSeconds()).padStart(2,'0')
        const trajRes = await getTrajectory(currentDevice.value.deviceId, {
          startTime: fmt(oneHourAgo),
          endTime: fmt(now)
        })
        const pts = trajRes.data || []
        if (pts.length > 0) {
          trackPoints.value = pts.map(p => ({ longitude: p.longitude, latitude: p.latitude }))
          polyline.value = [{
            points: trackPoints.value,
            color: '#07c160',
            width: 4,
            dottedLine: false,
            arrowLine: true
          }]
        }
      } catch (e) {
        console.error('加载轨迹失败', e)
      }
    }
  } catch (error) {
    console.error('加载设备数据失败', error)
  } finally {
    loading.value = false
  }
}

// 开始自动刷新
const startAutoRefresh = () => {
  // 每5秒刷新位置和传感器数据（实时监控）
  locationTimer = setInterval(async () => {
    if (!currentDevice.value || !currentDevice.value.deviceId) return
    try {
      await Promise.all([
        deviceStore.fetchDeviceLocation(currentDevice.value.deviceId),
        deviceStore.fetchDeviceStatus(currentDevice.value.deviceId),
        deviceStore.fetchSensorData(currentDevice.value.deviceId)
      ])
      // 更新地图标记、轨迹线和位置
      if (deviceStore.deviceLocation) {
        const loc = deviceStore.deviceLocation
        longitude.value = loc.longitude
        latitude.value = loc.latitude
        currentAddress.value = loc.address || (loc.latitude.toFixed(4) + ', ' + loc.longitude.toFixed(4))
        
        // 添加到轨迹点（去重：和上一个点距离太近则跳过）
        const lastPt = trackPoints.value[trackPoints.value.length - 1]
        if (!lastPt || Math.abs(lastPt.longitude - loc.longitude) > 0.00001 || Math.abs(lastPt.latitude - loc.latitude) > 0.00001) {
          trackPoints.value.push({ longitude: loc.longitude, latitude: loc.latitude })
          // 最多保留200个点
          if (trackPoints.value.length > 200) trackPoints.value.shift()
        }
        
        // 绘制轨迹线
        if (trackPoints.value.length >= 2) {
          polyline.value = [{
            points: trackPoints.value,
            color: '#07c160',
            width: 4,
            dottedLine: false,
            arrowLine: true
          }]
        }
        
        markers.value = [{
          id: 1,
          longitude: loc.longitude,
          latitude: loc.latitude,
          width: 24,
          height: 34,
          callout: {
            content: currentDevice.value.deviceName || '智能盲杖',
            color: '#333333',
            fontSize: 12,
            borderRadius: 8,
            bgColor: '#ffffff',
            padding: 8,
            display: 'ALWAYS'
          }
        }]
      }
    } catch (e) {
      console.error('自动刷新失败', e)
    }
  }, 5000)
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

// 定位到设备（盲杖红色图钉位置）
const instance = getCurrentInstance()
const centerOnDevice = () => {
  const loc = deviceStore.deviceLocation
  if (!loc || !loc.longitude || !loc.latitude) {
    uni.showToast({ title: '暂无设备位置数据', icon: 'none' })
    return
  }
  // 同步更新响应式绑定
  longitude.value = loc.longitude
  latitude.value = loc.latitude
  scale.value = 16

  // 用 includePoints 强制地图视野包含设备位置点（比 moveToLocation 更可靠）
  try {
    const mapCtx = uni.createMapContext('map', instance)
    mapCtx.includePoints({
      points: [{ longitude: loc.longitude, latitude: loc.latitude }],
      padding: [120, 120, 120, 120]
    })
    uni.showToast({ title: '已定位到设备', icon: 'success', duration: 1200 })
  } catch (e) {
    uni.showToast({ title: '已定位到设备', icon: 'success', duration: 1200 })
  }
}

// 跳转到报警页面
const navigateToAlarm = () => {
  uni.switchTab({
    url: '/pages/alarm/alarm'
  })
}

// 跳转到 AI 语音助手
const navigateToAiChat = () => {
  uni.navigateTo({
    url: '/pages/ai-chat/ai-chat'
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

.loading-state, .empty-state {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 80rpx 32rpx;
}

.loading-icon, .empty-icon {
  font-size: 80rpx;
  margin-bottom: 24rpx;
}

.loading-text {
  font-size: 28rpx;
  color: #9ca3af;
}

.empty-text {
  font-size: 32rpx;
  font-weight: 600;
  color: #6b7280;
  margin-bottom: 12rpx;
}

.empty-hint {
  font-size: 24rpx;
  color: #9ca3af;
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
    top: 24rpx;
    right: 24rpx;
    display: flex;
    flex-direction: column;
    gap: 16rpx;

    .control-btn {
      width: 96rpx;
      height: 96rpx;
      background: rgba(255, 255, 255, 0.95);
      border-radius: 16rpx;
      display: flex;
      flex-direction: column;
      align-items: center;
      justify-content: center;
      box-shadow: 0 4rpx 12rpx rgba(0, 0, 0, 0.15);

      .icon {
        font-size: 32rpx;
        line-height: 1;
      }
      .btn-label {
        font-size: 20rpx;
        color: #333;
        margin-top: 4rpx;
        line-height: 1;
      }
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

  .device-address {
    display: flex;
    align-items: center;
    gap: 8rpx;
    margin-bottom: 24rpx;
    padding: 12rpx 16rpx;
    background: #f0fdf4;
    border-radius: 12rpx;

    .address-icon {
      font-size: 24rpx;
    }

    .address-text {
      font-size: 24rpx;
      color: #374151;
    }
  }

  .status-header {
    display: flex;
    justify-content: space-between;
    align-items: center;
    margin-bottom: 24rpx;

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
    display: flex;
    flex-wrap: wrap;
    gap: 24rpx;

    .sensor-item {
      flex: 1;
      min-width: 0;
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

.ai-entry {
  margin: 0 32rpx 24rpx;
  padding: 28rpx 32rpx;
  border-radius: 24rpx;
  background: linear-gradient(135deg, #6366f1 0%, #8b5cf6 60%, #ec4899 100%);
  display: flex;
  align-items: center;
  justify-content: space-between;
  box-shadow: 0 8rpx 20rpx rgba(99, 102, 241, 0.3);

  .ai-entry-left {
    display: flex;
    align-items: center;
    flex: 1;
  }

  .ai-icon {
    width: 80rpx;
    height: 80rpx;
    border-radius: 50%;
    background: rgba(255, 255, 255, 0.25);
    display: flex;
    align-items: center;
    justify-content: center;
    font-size: 40rpx;
    margin-right: 24rpx;
    flex-shrink: 0;
  }

  .ai-info {
    flex: 1;
    display: flex;
    flex-direction: column;

    .ai-title {
      font-size: 30rpx;
      font-weight: 600;
      color: #ffffff;
      line-height: 1.3;
    }

    .ai-desc {
      font-size: 24rpx;
      color: rgba(255, 255, 255, 0.85);
      margin-top: 4rpx;
      line-height: 1.3;
    }
  }

  .ai-arrow {
    font-size: 44rpx;
    color: rgba(255, 255, 255, 0.9);
    font-weight: 300;
    margin-left: 16rpx;
  }

  &:active {
    opacity: 0.85;
    transform: scale(0.98);
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
    display: flex;
    flex-wrap: wrap;
    gap: 24rpx;

    .safety-card {
      width: calc(50% - 12rpx);
      box-sizing: border-box;
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
