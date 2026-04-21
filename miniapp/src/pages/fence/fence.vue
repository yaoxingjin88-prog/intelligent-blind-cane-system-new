<template>
  <view class="fence-page">
    <!-- 模块切换 -->
    <view class="module-tabs">
      <view 
        class="tab-item" 
        :class="{ active: activeModule === 'fence' }"
        @click="switchModule('fence')"
      >
        电子围栏
      </view>
      <view 
        class="tab-item" 
        :class="{ active: activeModule === 'trajectory' }"
        @click="switchModule('trajectory')"
      >
        轨迹回放
      </view>
    </view>

    <!-- 电子围栏模块 -->
    <scroll-view v-if="activeModule === 'fence'" class="fence-content" scroll-y>
      <view class="fence-header">
        <text class="fence-title">常用守护区域 ({{ fenceList.length }}/5)</text>
        <view class="add-btn" @click="showCreateFence = true">
          <text class="icon">➕</text>
          <text>新建围栏</text>
        </view>
      </view>

      <view class="fence-list">
        <view v-for="fence in fenceList" :key="fence.id" class="fence-item">
          <view class="fence-icon" :class="fence.type">
            <text>{{ getFenceIcon(fence.type) }}</text>
          </view>
          <view class="fence-info">
            <text class="fence-name">{{ fence.name }}</text>
            <text class="fence-desc">{{ getFenceDesc(fence) }}</text>
          </view>
          <view class="fence-switch" @click="toggleFenceStatus(fence)">
            <view class="switch" :class="{ on: fence.isAlarmEnabled }">
              <view class="switch-dot"></view>
            </view>
          </view>
        </view>
      </view>

      <!-- 围栏地图预览 -->
      <view class="fence-map-preview">
        <map
          id="fenceMap"
          class="fence-map"
          :longitude="mapCenter.longitude"
          :latitude="mapCenter.latitude"
          :scale="14"
          :circles="fenceCircles"
          :polygons="fencePolygons"
          :markers="fenceMarkers"
          :show-location="true"
        ></map>
      </view>

      <!-- 围栏状态卡片 -->
      <view class="fence-status-card" :class="{ 'out-of-fence': !inSafeZone }">
        <view class="status-row">
          <text class="status-icon">{{ inSafeZone ? '✅' : '⚠️' }}</text>
          <view class="status-info">
            <text class="status-title">{{ inSafeZone ? '当前位置安全' : '已越出守护区域' }}</text>
            <text class="status-sub">{{ statusSubtitle }}</text>
          </view>
        </view>
        <view class="status-detail">
          <view class="detail-item">
            <text class="detail-label">📍 当前位置</text>
            <text class="detail-value">{{ currentLocationText }}</text>
          </view>
          <view class="detail-item">
            <text class="detail-label">🛡 最近围栏</text>
            <text class="detail-value">{{ nearestFenceText }}</text>
          </view>
          <view class="detail-item">
            <text class="detail-label">📏 相对距离</text>
            <text class="detail-value">{{ distanceText }}</text>
          </view>
        </view>
      </view>

      <!-- 最近越界记录 -->
      <view class="recent-breach-section" v-if="recentBreaches.length > 0">
        <text class="section-title">最近越界记录</text>
        <view class="breach-list">
          <view v-for="(item, idx) in recentBreaches" :key="idx" class="breach-item">
            <text class="breach-icon">🚨</text>
            <view class="breach-info">
              <text class="breach-title">{{ item.title }}</text>
              <text class="breach-time">{{ item.time }}</text>
            </view>
          </view>
        </view>
      </view>
    </scroll-view>

    <!-- 轨迹回放模块 -->
    <scroll-view v-if="activeModule === 'trajectory'" class="trajectory-content" scroll-y>
      <view class="trajectory-header">
        <text class="trajectory-title">轨迹查询</text>
      </view>

      <!-- 时间选择 -->
      <view class="time-selector">
        <picker mode="date" :value="startDateStr" @change="onStartDateChange">
          <view class="time-item">
            <text class="time-label">开始时间</text>
            <text class="time-value">{{ startDateStr }}</text>
          </view>
        </picker>
        <text class="to">至</text>
        <picker mode="date" :value="endDateStr" @change="onEndDateChange">
          <view class="time-item">
            <text class="time-label">结束时间</text>
            <text class="time-value">{{ endDateStr }}</text>
          </view>
        </picker>
      </view>

      <!-- 播放控制 -->
      <view class="playback-controls">
        <button class="control-btn" @click="startPlayback">
          <text>{{ isPlaying ? '⏸️' : '▶️' }}</text>
        </button>
        <view class="speed-selector">
          <text 
            v-for="speed in speeds" 
            :key="speed"
            class="speed-item"
            :class="{ active: currentSpeed === speed }"
            @click="currentSpeed = speed"
          >
            {{ speed }}x
          </text>
        </view>
      </view>

      <!-- 轨迹地图 -->
      <view class="trajectory-map-container">
        <map
          id="trajectoryMap"
          class="trajectory-map"
          :longitude="mapCenter.longitude"
          :latitude="mapCenter.latitude"
          :scale="14"
          :polyline="trajectoryPolyline"
          :markers="trajectoryMarkers"
        ></map>
      </view>

      <!-- 轨迹统计 -->
      <view class="trajectory-stats">
        <view class="stat-item">
          <text class="stat-label">移动距离</text>
          <text class="stat-value">{{ totalDistance }}<text class="unit">km</text></text>
        </view>
        <view class="stat-item">
          <text class="stat-label">活动时长</text>
          <text class="stat-value">{{ activityDuration }}<text class="unit">h</text></text>
        </view>
        <view class="stat-item">
          <text class="stat-label">平均速度</text>
          <text class="stat-value">{{ avgSpeed }}<text class="unit">km/h</text></text>
        </view>
      </view>
    </scroll-view>

    <!-- 创建围栏弹窗（仅在电子围栏模块显示） -->
    <view v-if="showCreateFence && activeModule === 'fence'" class="popup-mask" @click="showCreateFence = false">
      <view class="create-fence-popup" @click.stop>
        <view class="popup-header">
          <text class="title">创建电子围栏</text>
          <text class="close" @click="showCreateFence = false">✕</text>
        </view>
        <view class="popup-body">
          <view class="form-item">
            <text class="label">围栏名称</text>
            <input v-model="newFence.name" class="input" placeholder="如：家周边、社区公园" />
          </view>
          <view class="form-item">
            <text class="label">围栏类型</text>
            <view class="type-selector">
              <view 
                class="type-item" 
                :class="{ active: newFence.type === 'circle' }"
                @click="newFence.type = 'circle'"
              >
                <text>🔵 圆形围栏</text>
              </view>
              <view 
                class="type-item" 
                :class="{ active: newFence.type === 'rectangle' }"
                @click="newFence.type = 'rectangle'"
              >
                <text>⬜ 矩形围栏</text>
              </view>
            </view>
          </view>
          <view class="form-item" v-if="newFence.type === 'circle'">
            <text class="label">半径（米）</text>
            <input v-model.number="newFence.radius" type="number" class="input" placeholder="请输入半径" />
          </view>
          <view class="form-item">
            <text class="label">是否启用越界报警</text>
            <view class="switch" :class="{ on: newFence.isAlarmEnabled }" @click="newFence.isAlarmEnabled = !newFence.isAlarmEnabled">
              <view class="switch-dot"></view>
            </view>
          </view>
        </view>
        <view class="popup-footer">
          <button class="cancel-btn" @click="showCreateFence = false">取消</button>
          <button class="confirm-btn" @click="createFence">创建</button>
        </view>
      </view>
    </view>

  </view>
</template>

<script setup>
import { ref, computed, onMounted, watch } from 'vue'
import { useDeviceStore, useAlarmStore } from '@/store'
import { getFenceList, createFence as createFenceApi, updateFence } from '@/api/fence'
import { getTrajectory, getTrajectoryStatistics } from '@/api/trajectory'
import { formatDateTime, formatDistance } from '@/utils'

const deviceStore = useDeviceStore()

// 当前模块
const activeModule = ref('fence')
const showCreateFence = ref(false)

// 切换模块时关闭弹窗
const switchModule = (module) => {
  activeModule.value = module
  showCreateFence.value = false
}

// 地图中心
const mapCenter = ref({
  longitude: 116.4074,
  latitude: 39.9042
})

// 围栏列表
const fenceList = ref([])

// 加载围栏列表
const loadFenceList = async () => {
  const deviceId = deviceStore.currentDevice ? deviceStore.currentDevice.deviceId : ''
  if (!deviceId) {
    fenceList.value = []
    return
  }
  try {
    const res = await getFenceList(deviceId)
    fenceList.value = res.data || []
  } catch (error) {
    console.error('加载围栏列表失败', error)
  }
}

onMounted(async () => {
  deviceStore.restoreFromStorage()
  
  // 自动获取设备列表和位置
  if (!deviceStore.currentDevice || !deviceStore.currentDevice.deviceId) {
    try {
      await deviceStore.fetchDeviceList()
      if (deviceStore.deviceList.length > 0) {
        deviceStore.setCurrentDevice(deviceStore.deviceList[0])
      }
    } catch (e) { /* ignore */ }
  }
  
  if (deviceStore.currentDevice && deviceStore.currentDevice.deviceId) {
    try {
      await deviceStore.fetchDeviceLocation(deviceStore.currentDevice.deviceId)
      // 将地图中心移到设备位置
      if (deviceStore.deviceLocation) {
        mapCenter.value = {
          longitude: deviceStore.deviceLocation.longitude,
          latitude: deviceStore.deviceLocation.latitude
        }
      }
    } catch (e) { /* ignore */ }
  }
  
  loadFenceList()
})

// 新建围栏
const newFence = ref({
  name: '',
  type: 'circle',
  radius: 500,
  isAlarmEnabled: true
})

// 围栏圆圈
const fenceCircles = computed(() => {
  return fenceList.value
    .filter(fence => fence.type === 'circle' && fence.isAlarmEnabled)
    .map(fence => ({
      longitude: fence.center.longitude,
      latitude: fence.center.latitude,
      radius: fence.radius,
      fillColor: '#07c16033',
      color: '#07c160',
      strokeWidth: 2
    }))
})

// 围栏多边形
const fencePolygons = computed(() => {
  return fenceList.value
    .filter(fence => fence.type !== 'circle' && fence.isAlarmEnabled)
    .map(fence => ({
      points: fence.points,
      fillColor: '#07c16033',
      color: '#07c160',
      strokeWidth: 2
    }))
})

// 围栏标记（含设备位置）
const fenceMarkers = computed(() => {
  const markers = fenceList.value.map(fence => ({
    id: fence.id,
    longitude: fence.center.longitude,
    latitude: fence.center.latitude,
    width: 32,
    height: 32,
    callout: {
      content: fence.name,
      color: '#333333',
      fontSize: 12,
      borderRadius: 8,
      bgColor: '#ffffff',
      padding: 8
    }
  }))
  // 添加设备位置标记
  if (deviceStore.deviceLocation) {
    markers.push({
      id: 99999,
      longitude: deviceStore.deviceLocation.longitude,
      latitude: deviceStore.deviceLocation.latitude,
      width: 28,
      height: 36,
      callout: {
        content: '盲杖位置',
        color: '#ffffff',
        fontSize: 12,
        borderRadius: 8,
        bgColor: '#07c160',
        padding: 6,
        display: 'ALWAYS'
      }
    })
  }
  return markers
})

// ===== 围栏状态计算（盲杖是否在守护区域内）=====
// 两点距离（米），Haversine 公式
const haversine = (lat1, lon1, lat2, lon2) => {
  const toRad = (d) => (d * Math.PI) / 180
  const R = 6371000
  const dLat = toRad(lat2 - lat1)
  const dLon = toRad(lon2 - lon1)
  const a = Math.sin(dLat / 2) ** 2 + Math.cos(toRad(lat1)) * Math.cos(toRad(lat2)) * Math.sin(dLon / 2) ** 2
  return R * 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a))
}

// 最近的启用围栏（按到中心距离排序）
const nearestFence = computed(() => {
  const loc = deviceStore.deviceLocation
  if (!loc || !fenceList.value.length) return null
  const enabled = fenceList.value.filter(f => f.isAlarmEnabled && f.center)
  if (!enabled.length) return null
  let best = null
  let bestDist = Infinity
  for (const f of enabled) {
    const d = haversine(loc.latitude, loc.longitude, f.center.latitude, f.center.longitude)
    if (d < bestDist) {
      bestDist = d
      best = { fence: f, distance: d }
    }
  }
  return best
})

// 是否在安全区内：到任一启用围栏中心距离 ≤ 半径
const inSafeZone = computed(() => {
  const loc = deviceStore.deviceLocation
  if (!loc || !fenceList.value.length) return true
  const enabled = fenceList.value.filter(f => f.isAlarmEnabled && f.center && f.type === 'circle')
  if (!enabled.length) return true
  return enabled.some(f => haversine(loc.latitude, loc.longitude, f.center.latitude, f.center.longitude) <= (f.radius || 0))
})

const statusSubtitle = computed(() => {
  if (!deviceStore.deviceLocation) return '等待设备定位...'
  if (!fenceList.value.length) return '尚未设置任何守护区域'
  return inSafeZone.value ? '盲杖正在守护区域内' : '请关注盲人当前动态'
})

const currentLocationText = computed(() => {
  const loc = deviceStore.deviceLocation
  if (!loc || !loc.longitude) return '暂无位置数据'
  return `${loc.latitude.toFixed(4)}, ${loc.longitude.toFixed(4)}`
})

const nearestFenceText = computed(() => {
  return nearestFence.value ? nearestFence.value.fence.name : '无启用中的围栏'
})

const distanceText = computed(() => {
  if (!nearestFence.value) return '-'
  const d = nearestFence.value.distance
  const r = nearestFence.value.fence.radius || 0
  const offset = d - r
  if (offset <= 0) return `在围栏内（距中心 ${Math.round(d)} 米）`
  return `超出边界 ${Math.round(offset)} 米`
})

// 最近越界记录（从报警记录里取围栏相关的）
const alarmStore = useAlarmStore()
const recentBreaches = computed(() => {
  const list = alarmStore.alarmList || []
  return list
    .filter(a => (a.alarmType || '').includes('围栏'))
    .slice(0, 3)
    .map(a => ({
      title: a.alarmType || '围栏越界',
      time: a.alarmTime || ''
    }))
})

// 轨迹相关
const startTime = ref(new Date(Date.now() - 24 * 60 * 60 * 1000))
const endTime = ref(new Date())
const isPlaying = ref(false)

// 格式化日期字符串 YYYY-MM-DD
const toDateStr = (date) => {
  const d = new Date(date)
  const y = d.getFullYear()
  const m = String(d.getMonth() + 1).padStart(2, '0')
  const day = String(d.getDate()).padStart(2, '0')
  return y + '-' + m + '-' + day
}
const startDateStr = computed(() => toDateStr(startTime.value))
const endDateStr = computed(() => toDateStr(endTime.value))

const onStartDateChange = (e) => {
  startTime.value = new Date(e.detail.value + ' 00:00:00')
}
const onEndDateChange = (e) => {
  endTime.value = new Date(e.detail.value + ' 23:59:59')
}
const currentSpeed = ref(1)
const speeds = [1, 2, 4]

// 播放中切速度时重启定时器
watch(currentSpeed, () => {
  if (isPlaying.value && playbackTimer) {
    clearInterval(playbackTimer)
    playbackTimer = null
    runAnimation()
  }
})

const totalDistance = ref(0)
const activityDuration = ref(0)
const avgSpeed = ref(0)

// 轨迹线
const trajectoryPolyline = ref([])

// 轨迹标记
const trajectoryMarkers = ref([])

// 获取围栏图标
const getFenceIcon = (type) => {
  const icons = {
    circle: '🏠',
    rectangle: '🏢',
    custom: '📍'
  }
  return icons[type] || '📍'
}

// 获取围栏描述
const getFenceDesc = (fence) => {
  if (fence.type === 'circle') {
    return `半径 ${fence.radius}米 | ${fence.isAlarmEnabled ? '越界报警开启' : '越界报警关闭'}`
  }
  return `${fence.isAlarmEnabled ? '越界报警开启' : '越界报警关闭'}`
}

// 切换围栏状态
const toggleFenceStatus = async (fence) => {
  try {
    await updateFence(fence.id, {
      isAlarmEnabled: !fence.isAlarmEnabled
    })
    fence.isAlarmEnabled = !fence.isAlarmEnabled
    uni.showToast({
      title: fence.isAlarmEnabled ? '已开启报警' : '已关闭报警',
      icon: 'success'
    })
  } catch (error) {
    console.error('更新围栏状态失败', error)
  }
}

// 创建围栏
const createFence = async () => {
  if (!newFence.value.name) {
    uni.showToast({
      title: '请输入围栏名称',
      icon: 'none'
    })
    return
  }

  try {
    const res = await createFenceApi({
      ...newFence.value,
      deviceId: deviceStore.currentDevice ? deviceStore.currentDevice.deviceId : '',
      center: mapCenter.value
    })
    
    fenceList.value.push(res.data)
    showCreateFence.value = false
    
    // 重置表单
    newFence.value = {
      name: '',
      type: 'circle',
      radius: 500,
      isAlarmEnabled: true
    }
    
    uni.showToast({
      title: '创建成功',
      icon: 'success'
    })
  } catch (error) {
    console.error('创建围栏失败', error)
  }
}

// 轨迹动画相关
let trajectoryPoints = []
let playbackTimer = null
let playbackIndex = 0

// 停止播放动画
const stopPlayback = () => {
  if (playbackTimer) {
    clearInterval(playbackTimer)
    playbackTimer = null
  }
  isPlaying.value = false
}

// 开始播放
const startPlayback = async () => {
  // 正在播放则暂停
  if (isPlaying.value) {
    stopPlayback()
    return
  }

  // 如果已有轨迹数据且暂停过，继续播放
  if (trajectoryPoints.length > 0 && playbackIndex > 0 && playbackIndex < trajectoryPoints.length) {
    isPlaying.value = true
    runAnimation()
    return
  }

  // 首次加载轨迹数据
  try {
    const deviceId = deviceStore.currentDevice ? deviceStore.currentDevice.deviceId : null
    if (!deviceId) {
      uni.showToast({ title: '请先绑定设备', icon: 'none' })
      return
    }

    const res = await getTrajectory(deviceId, {
      startTime: formatDateTime(startTime.value, 'YYYY-MM-DD HH:mm:ss'),
      endTime: formatDateTime(endTime.value, 'YYYY-MM-DD HH:mm:ss')
    })
    
    const points = res.data || []
    if (points.length === 0) {
      uni.showToast({ title: '该时间段无轨迹数据', icon: 'none' })
      return
    }

    trajectoryPoints = points
    playbackIndex = 0

    // 先画完整轨迹线（灰色虚线表示全程）
    trajectoryPolyline.value = [{
      points: points.map(p => ({ longitude: p.longitude, latitude: p.latitude })),
      color: '#cccccc',
      width: 3,
      dottedLine: true
    }]

    // 起点标记
    trajectoryMarkers.value = [{
      id: 1,
      longitude: points[0].longitude,
      latitude: points[0].latitude,
      width: 20,
      height: 28,
      callout: { content: '起点', color: '#fff', fontSize: 11, borderRadius: 8, bgColor: '#07c160', padding: 5, display: 'ALWAYS' }
    }, {
      id: 2,
      longitude: points[points.length - 1].longitude,
      latitude: points[points.length - 1].latitude,
      width: 20,
      height: 28,
      callout: { content: '终点', color: '#fff', fontSize: 11, borderRadius: 8, bgColor: '#ee0a24', padding: 5, display: 'ALWAYS' }
    }, {
      id: 3,
      longitude: points[0].longitude,
      latitude: points[0].latitude,
      width: 28,
      height: 28,
      callout: { content: '🚶', fontSize: 16, borderRadius: 50, bgColor: '#07c160', padding: 4, display: 'ALWAYS' }
    }]

    // 获取统计数据
    try {
      const statsRes = await getTrajectoryStatistics(deviceId, {
        startTime: formatDateTime(startTime.value, 'YYYY-MM-DD HH:mm:ss'),
        endTime: formatDateTime(endTime.value, 'YYYY-MM-DD HH:mm:ss')
      })
      const stats = statsRes.data || {}
      totalDistance.value = formatDistance(stats.totalDistance || 0)
      activityDuration.value = Math.round(((stats.duration || 0) / 3600) * 10) / 10
      avgSpeed.value = stats.avgSpeed || 0
    } catch (e) {
      console.error('获取轨迹统计失败', e)
    }

    isPlaying.value = true
    runAnimation()
  } catch (error) {
    console.error('加载轨迹失败', error)
  }
}

// 运行动画
const runAnimation = () => {
  const interval = Math.max(100, 800 / currentSpeed.value)
  
  playbackTimer = setInterval(() => {
    playbackIndex++
    
    if (playbackIndex >= trajectoryPoints.length) {
      stopPlayback()
      uni.showToast({ title: '播放完成', icon: 'success' })
      return
    }

    const current = trajectoryPoints[playbackIndex]
    
    // 更新已走过的路径（绿色实线）
    const walkedPoints = trajectoryPoints.slice(0, playbackIndex + 1).map(p => ({
      longitude: p.longitude, latitude: p.latitude
    }))
    
    trajectoryPolyline.value = [
      {
        points: trajectoryPoints.map(p => ({ longitude: p.longitude, latitude: p.latitude })),
        color: '#cccccc',
        width: 3,
        dottedLine: true
      },
      {
        points: walkedPoints,
        color: '#07c160',
        width: 5,
        dottedLine: false
      }
    ]

    // 移动设备标记（id=3）
    const newMarkers = [
      trajectoryMarkers.value[0],
      trajectoryMarkers.value[1],
      {
        id: 3,
        longitude: current.longitude,
        latitude: current.latitude,
        width: 28,
        height: 28,
        callout: { content: '🚶', fontSize: 16, borderRadius: 50, bgColor: '#07c160', padding: 4, display: 'ALWAYS' }
      }
    ]
    trajectoryMarkers.value = newMarkers
  }, interval)
}

</script>

<style lang="scss" scoped>
.fence-page {
  min-height: 100vh;
  background: #f7f8fa;
  display: flex;
  flex-direction: column;
  box-sizing: border-box;
}

.module-tabs {
  background: #ffffff;
  display: flex;
  padding: 16rpx 32rpx;
  gap: 16rpx;
  position: sticky;
  top: 0;
  z-index: 99;

  .tab-item {
    flex: 1;
    height: 72rpx;
    border-radius: 16rpx;
    display: flex;
    align-items: center;
    justify-content: center;
    font-size: 28rpx;
    font-weight: 600;
    border: 1rpx solid #e5e7eb;
    color: #6b7280;

    &.active {
      background: #07c160;
      color: #ffffff;
      border-color: #07c160;
    }
  }
}

.fence-content,
.trajectory-content {
  flex: 1;
  padding: 32rpx;
  width: 100%;
  box-sizing: border-box;
}

.fence-header,
.trajectory-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 32rpx;

  .fence-title,
  .trajectory-title {
    font-size: 28rpx;
    font-weight: 600;
    color: #1f2937;
  }

  .add-btn {
    display: flex;
    align-items: center;
    gap: 8rpx;
    color: #07c160;
    font-size: 28rpx;

    .icon {
      font-size: 24rpx;
    }
  }
}

.fence-list {
  display: flex;
  flex-direction: column;
  gap: 24rpx;
  margin-bottom: 32rpx;

  .fence-item {
    background: #ffffff;
    border-radius: 24rpx;
    padding: 32rpx;
    display: flex;
    align-items: center;
    gap: 24rpx;
    box-shadow: 0 4rpx 12rpx rgba(0, 0, 0, 0.06);
    border: 1rpx solid #f3f4f6;
    width: 100%;
    box-sizing: border-box;

    .fence-icon {
      width: 80rpx;
      height: 80rpx;
      border-radius: 50%;
      display: flex;
      align-items: center;
      justify-content: center;
      font-size: 36rpx;

      &.circle {
        background: #d1fae5;
      }

      &.rectangle {
        background: #dbeafe;
      }

      &.custom {
        background: #fef3c7;
      }
    }

    .fence-info {
      flex: 1;

      .fence-name {
        display: block;
        font-size: 28rpx;
        font-weight: 600;
        color: #1f2937;
        margin-bottom: 8rpx;
      }

      .fence-desc {
        display: block;
        font-size: 24rpx;
        color: #9ca3af;
      }
    }

    .fence-switch {
      .switch {
        width: 80rpx;
        height: 48rpx;
        background: #e5e7eb;
        border-radius: 48rpx;
        position: relative;
        transition: background 0.3s;

        &.on {
          background: #07c160;
        }

        .switch-dot {
          width: 40rpx;
          height: 40rpx;
          background: #ffffff;
          border-radius: 50%;
          position: absolute;
          top: 4rpx;
          left: 4rpx;
          transition: transform 0.3s;
          box-shadow: 0 2rpx 8rpx rgba(0, 0, 0, 0.15);
        }

        &.on .switch-dot {
          transform: translateX(32rpx);
        }
      }
    }
  }
}

.fence-map-preview,
.trajectory-map-container {
  height: 560rpx;
  border-radius: 24rpx;
  overflow: hidden;
  border: 1rpx solid #e5e7eb;
  margin-bottom: 24rpx;

  .fence-map,
  .trajectory-map {
    width: 100%;
    height: 100%;
  }
}

.fence-status-card {
  background: linear-gradient(135deg, #ecfdf5 0%, #ffffff 100%);
  border: 1rpx solid #a7f3d0;
  border-radius: 24rpx;
  padding: 32rpx;
  margin-bottom: 24rpx;

  &.out-of-fence {
    background: linear-gradient(135deg, #fef2f2 0%, #ffffff 100%);
    border-color: #fecaca;
  }

  .status-row {
    display: flex;
    align-items: center;
    padding-bottom: 24rpx;
    border-bottom: 1rpx solid #f1f5f9;

    .status-icon {
      font-size: 56rpx;
      margin-right: 20rpx;
    }

    .status-info {
      flex: 1;
      display: flex;
      flex-direction: column;

      .status-title {
        font-size: 32rpx;
        font-weight: 700;
        color: #1f2937;
      }

      .status-sub {
        font-size: 24rpx;
        color: #6b7280;
        margin-top: 6rpx;
      }
    }
  }

  .status-detail {
    padding-top: 20rpx;

    .detail-item {
      display: flex;
      justify-content: space-between;
      align-items: center;
      padding: 12rpx 0;

      .detail-label {
        font-size: 26rpx;
        color: #6b7280;
      }

      .detail-value {
        font-size: 26rpx;
        color: #1f2937;
        font-weight: 500;
      }
    }
  }
}

.recent-breach-section {
  background: #ffffff;
  border-radius: 24rpx;
  padding: 24rpx 32rpx;
  margin-bottom: 24rpx;

  .section-title {
    font-size: 28rpx;
    font-weight: 600;
    color: #1f2937;
    display: block;
    margin-bottom: 16rpx;
  }

  .breach-item {
    display: flex;
    align-items: center;
    padding: 16rpx 0;
    border-bottom: 1rpx solid #f3f4f6;

    &:last-child {
      border-bottom: none;
    }

    .breach-icon {
      font-size: 32rpx;
      margin-right: 16rpx;
    }

    .breach-info {
      flex: 1;
      display: flex;
      flex-direction: column;

      .breach-title {
        font-size: 28rpx;
        color: #1f2937;
      }

      .breach-time {
        font-size: 22rpx;
        color: #9ca3af;
        margin-top: 4rpx;
      }
    }
  }
}

.time-selector {
  display: flex;
  align-items: center;
  gap: 24rpx;
  background: #ffffff;
  border-radius: 24rpx;
  padding: 32rpx;
  margin-bottom: 24rpx;

  .time-item {
    flex: 1;
    display: flex;
    flex-direction: column;
    gap: 16rpx;

    .time-label {
      font-size: 24rpx;
      color: #9ca3af;
    }

    .time-value {
      font-size: 28rpx;
      color: #1f2937;
      font-weight: 600;
    }
  }

  .to {
    font-size: 24rpx;
    color: #9ca3af;
  }
}

.playback-controls {
  display: flex;
  align-items: center;
  gap: 24rpx;
  background: #ffffff;
  border-radius: 24rpx;
  padding: 32rpx;
  margin-bottom: 24rpx;

  .control-btn {
    width: 80rpx;
    height: 80rpx;
    border-radius: 50%;
    background: #07c160;
    color: #ffffff;
    font-size: 32rpx;
    border: none;
    display: flex;
    align-items: center;
    justify-content: center;

    &:disabled {
      background: #e5e7eb;
    }
  }

  .speed-selector {
    flex: 1;
    display: flex;
    gap: 16rpx;

    .speed-item {
      flex: 1;
      height: 64rpx;
      border-radius: 16rpx;
      display: flex;
      align-items: center;
      justify-content: center;
      font-size: 24rpx;
      color: #6b7280;
      background: #f9fafb;
      border: 1rpx solid #e5e7eb;

      &.active {
        background: #07c160;
        color: #ffffff;
        border-color: #07c160;
      }
    }
  }
}

.trajectory-stats {
  display: flex;
  gap: 24rpx;
  background: #ffffff;
  border-radius: 24rpx;
  padding: 32rpx;

  .stat-item {
    flex: 1;
    text-align: center;

    .stat-label {
      display: block;
      font-size: 24rpx;
      color: #9ca3af;
      margin-bottom: 8rpx;
    }

    .stat-value {
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

.popup-mask {
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background: rgba(0, 0, 0, 0.5);
  z-index: 999;
  display: flex;
  align-items: flex-end;
  justify-content: center;
}

.create-fence-popup {
  width: 100%;
  background: #ffffff;
  border-radius: 24rpx 24rpx 0 0;
  padding: 48rpx;
  box-sizing: border-box;
  max-height: 80vh;
  overflow-y: auto;

  .popup-header {
    display: flex;
    justify-content: space-between;
    align-items: center;
    margin-bottom: 48rpx;

    .title {
      font-size: 36rpx;
      font-weight: bold;
      color: #1f2937;
    }

    .close {
      font-size: 48rpx;
      color: #9ca3af;
    }
  }

  .popup-body {
    margin-bottom: 48rpx;

    .form-item {
      margin-bottom: 32rpx;

      .label {
        display: block;
        font-size: 28rpx;
        color: #374151;
        margin-bottom: 16rpx;
      }

      .input {
        width: 100%;
        height: 88rpx;
        background: #f9fafb;
        border-radius: 16rpx;
        padding: 0 24rpx;
        font-size: 28rpx;
        color: #1f2937;
        border: 2rpx solid #e5e7eb;
        box-sizing: border-box;
      }

      .type-selector {
        display: flex;
        gap: 16rpx;

        .type-item {
          flex: 1;
          height: 88rpx;
          border-radius: 16rpx;
          display: flex;
          align-items: center;
          justify-content: center;
          font-size: 28rpx;
          border: 1rpx solid #e5e7eb;
          color: #6b7280;

          &.active {
            background: #07c160;
            color: #ffffff;
            border-color: #07c160;
          }
        }
      }

      .switch {
        width: 80rpx;
        height: 48rpx;
        background: #e5e7eb;
        border-radius: 48rpx;
        position: relative;
        transition: background 0.3s;

        &.on {
          background: #07c160;
        }

        .switch-dot {
          width: 40rpx;
          height: 40rpx;
          background: #ffffff;
          border-radius: 50%;
          position: absolute;
          top: 4rpx;
          left: 4rpx;
          transition: transform 0.3s;
          box-shadow: 0 2rpx 8rpx rgba(0, 0, 0, 0.15);
        }

        &.on .switch-dot {
          transform: translateX(32rpx);
        }
      }
    }
  }

  .popup-footer {
    display: flex;
    gap: 24rpx;

    button {
      flex: 1;
      height: 80rpx;
      border-radius: 16rpx;
      font-size: 28rpx;
      font-weight: 600;
      border: none;
    }

    .cancel-btn {
      background: #f3f4f6;
      color: #6b7280;
    }

    .confirm-btn {
      background: #07c160;
      color: #ffffff;
    }
  }
}
</style>
