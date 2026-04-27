<template>
  <view class="home-page">
    <!-- 主体内容 -->
    <scroll-view class="content" scroll-y>
      <view class="home-hero">
        <view class="home-hero-copy">
          <text class="hero-kicker">Smart Cane</text>
          <text class="hero-title">守护出行与安全陪伴</text>
          <text class="hero-desc">查看设备位置、健康动态与 AI 辅助能力，实时掌握盲杖状态。</text>
        </view>
        <view class="hero-status-pill" :class="currentDevice && currentDevice.deviceId ? 'ready' : 'idle'">
          <text>{{ currentDevice && currentDevice.deviceId ? '设备已连接' : '等待绑定设备' }}</text>
        </view>
      </view>

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

        <view v-if="activeSosAlert" class="sos-alert-card">
          <view class="sos-alert-head">
            <view class="sos-alert-copy">
              <text class="sos-alert-kicker">SOS Alert</text>
              <text class="sos-alert-title">用户端正在发起一键呼救</text>
              <text class="sos-alert-desc">{{ latestGuardianAlert.message }}</text>
            </view>
            <text class="sos-alert-time">{{ sosAlertTimeText }}</text>
          </view>
          <view class="sos-alert-meta">
            <view class="sos-alert-meta-item">
              <text class="sos-meta-label">位置</text>
              <text class="sos-meta-value">{{ sosLocationText }}</text>
            </view>
            <view class="sos-alert-meta-item compact">
              <text class="sos-meta-label">状态</text>
              <text class="sos-meta-value">待家属确认</text>
            </view>
          </view>
          <view class="sos-alert-actions">
            <view class="sos-action-btn primary" @click="handleSosCall">
              <text class="sos-action-text">立即联系</text>
            </view>
            <view class="sos-action-btn secondary" @click="handleSosLocate">
              <text class="sos-action-text">查看位置</text>
            </view>
            <view class="sos-action-btn ghost" @click="handleSosResolve">
              <text class="sos-action-text">标记已处理</text>
            </view>
          </view>
        </view>

        <view class="status-overview">
          <view class="overview-chip device">
            <text class="overview-icon">🪪</text>
            <text class="overview-label">设备编号</text>
            <text class="overview-value">{{ (currentDevice && currentDevice.deviceId) ? currentDevice.deviceId : '--' }}</text>
          </view>
          <view class="overview-chip steps">
            <text class="overview-icon">👟</text>
            <text class="overview-label">今日步数</text>
            <text class="overview-value">{{ todaySteps || 0 }}</text>
          </view>
          <view class="overview-chip guard">
            <text class="overview-icon">🛡️</text>
            <text class="overview-label">守护状态</text>
            <text class="overview-value">{{ (deviceStatus && deviceStatus.status === 'online') ? '已开启' : '待连接' }}</text>
          </view>
        </view>

        <!-- 传感器数据 -->
        <view class="sensor-grid">
          <view class="sensor-item obstacle">
            <text class="sensor-icon">📏</text>
            <text class="sensor-label">前方障碍</text>
            <text class="sensor-value">{{ (sensorData && sensorData.obstacleDistance) ? sensorData.obstacleDistance : 0 }}<text class="unit">m</text></text>
          </view>
          <view class="sensor-item steps">
            <text class="sensor-icon">🚶</text>
            <text class="sensor-label">今日步数</text>
            <text class="sensor-value">{{ todaySteps || 0 }}</text>
          </view>
          <view class="sensor-item rest">
            <text class="sensor-icon">🪑</text>
            <text class="sensor-label">静止时长</text>
            <text class="sensor-value">{{ stationaryTime || 0 }}<text class="unit">min</text></text>
          </view>
        </view>

        <view class="guardian-section">
          <view class="guardian-header">
            <view class="section-caption compact guardian-caption">
              <text class="section-kicker">Guardian Care</text>
              <text class="section-title">家属远程协同守护</text>
            </view>
            <text class="guardian-status-pill" :class="guardianStatusLevel">{{ guardianStatusText }}</text>
          </view>
          <text class="guardian-desc">{{ guardianStatusDescription }}</text>
          <view class="guardian-grid">
            <view class="guardian-metric location">
              <text class="guardian-metric-label">当前位置</text>
              <text class="guardian-metric-value">{{ guardianLocationText }}</text>
            </view>
            <view class="guardian-metric time">
              <text class="guardian-metric-label">最后更新时间</text>
              <text class="guardian-metric-value">{{ guardianUpdateText }}</text>
            </view>
            <view class="guardian-metric battery">
              <text class="guardian-metric-label">设备电量</text>
              <text class="guardian-metric-value">{{ guardianBatteryText }}</text>
            </view>
          </view>
          <view class="guardian-tags" v-if="guardianTags.length">
            <text v-for="(tag, index) in guardianTags" :key="index" class="guardian-tag" :class="tag.tone || 'neutral'">{{ tag.label }}</text>
          </view>
          <view class="guardian-note">
            <text class="guardian-note-label">{{ guardianLatestEventLabel }}</text>
            <text class="guardian-note-value">{{ guardianLatestEventText }}</text>
          </view>
          <view class="guardian-actions">
            <view class="guardian-btn primary" @click="sendGuardianComfortAction">
              <text class="guardian-btn-icon">🔊</text>
              <text class="guardian-btn-text">发送安抚语音</text>
            </view>
            <view class="guardian-btn secondary" @click="chooseGuardianDestination">
              <text class="guardian-btn-icon">🧭</text>
              <text class="guardian-btn-text">发送目的地</text>
            </view>
          </view>
        </view>

        <view class="user-terminal-entry" @click="navigateToUserTerminal">
          <view class="user-terminal-entry-left">
            <view class="user-terminal-icon">🧑</view>
            <view class="user-terminal-info">
              <text class="user-terminal-kicker">Demo Entry</text>
              <text class="user-terminal-title">用户端演示</text>
              <text class="user-terminal-desc">查看老人侧接收提醒与 AI 安抚的展示效果</text>
            </view>
          </view>
          <text class="user-terminal-arrow">›</text>
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

      <view class="crossing-entry" @click="navigateToCrossingAssist">
        <view class="crossing-entry-left">
          <view class="crossing-icon">🚦</view>
          <view class="crossing-info">
            <text class="crossing-title">路口安全通行辅助</text>
            <text class="crossing-desc">识别红绿灯、斑马线方向并进行语音提醒</text>
          </view>
        </view>
        <text class="crossing-arrow">›</text>
      </view>

      <!-- 安全守护 -->
      <view class="safety-section">
        <view class="section-caption">
          <text class="section-kicker">Safety</text>
          <text class="section-title">安全守护</text>
        </view>
        <view class="safety-grid">
          <view class="safety-card fall" @click="navigateToAlarm">
            <view class="safety-icon">🚨</view>
            <view class="safety-info">
              <view class="safety-title-row">
                <text class="safety-title">跌倒监测</text>
                <text class="safety-badge" :class="fallSafetyLevel">{{ fallSafetyLabel }}</text>
              </view>
              <text class="safety-desc">{{ fallSafetyDesc }}</text>
              <text class="safety-meta">{{ fallSafetyMeta }}</text>
            </view>
          </view>
          <view class="safety-card sos" @click="navigateToAlarm">
            <view class="safety-icon">🔔</view>
            <view class="safety-info">
              <view class="safety-title-row">
                <text class="safety-title">紧急提醒</text>
                <text class="safety-badge" :class="emergencySafetyLevel">{{ emergencySafetyLabel }}</text>
              </view>
              <text class="safety-desc">{{ emergencySafetyDesc }}</text>
              <text class="safety-meta">{{ emergencySafetyMeta }}</text>
            </view>
          </view>
        </view>
      </view>

      <!-- 最新动态 -->
      <view class="activity-section">
        <view class="section-header">
          <view class="section-caption compact">
            <text class="section-kicker">Activity</text>
            <text class="section-title">最新动态</text>
          </view>
          <text class="more-btn" @click="navigateToAlarm">查看更多</text>
        </view>
        <view class="activity-list">
          <view v-for="item in recentActivities" :key="item.id" class="activity-item" @click="navigateToAlarm">
            <view class="activity-icon" :class="item.type">
              <text>{{ item.icon }}</text>
            </view>
            <view class="activity-content">
              <view class="activity-meta-row">
                <text class="activity-type-tag" :class="item.type">{{ item.typeLabel }}</text>
                <text class="activity-status-tag" :class="item.statusClass">{{ item.statusLabel }}</text>
              </view>
              <text class="activity-title">{{ item.title }}</text>
              <text class="activity-desc">{{ item.desc }}</text>
              <text class="activity-time">{{ item.time || '刚刚更新' }}</text>
            </view>
            <text class="activity-arrow">›</text>
          </view>
        </view>
      </view>
    </scroll-view>
  </view>
</template>

<script setup>
import { ref, computed, onMounted, onUnmounted, getCurrentInstance } from 'vue'
import { onShow } from '@dcloudio/uni-app'
import { useUserStore, useDeviceStore, useAlarmStore, useElderStore } from '@/store'
import { getBlindProfile } from '@/api/blind'
import { handleAlarm as handleAlarmApi } from '@/api/alarm'
import { getTrajectory } from '@/api/trajectory'
import { getGuardianCareOverview, sendGuardianComfort, sendGuardianDestination } from '@/api/guardian'
import { formatRelativeTime } from '@/utils'

const userStore = useUserStore()
const deviceStore = useDeviceStore()
const alarmStore = useAlarmStore()
const elderStore = useElderStore()

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
const guardianOverview = ref({ tags: [] })
const latestGuardianAlert = ref(null)
const latestGuardianComfort = ref(null)
const latestGuardianDestination = ref(null)

// 加载状态
const loading = ref(true)

// 传感器数据
const todaySteps = computed(() => {
  return sensorData.value ? (sensorData.value.stepCount || 0) : 0
})
const stationaryTime = computed(() => {
  return sensorData.value ? Math.round((sensorData.value.heartRate || 70) / 14) : 5
})

const guardianStatusLevel = computed(() => guardianOverview.value?.statusLevel || 'safe')
const guardianStatusText = computed(() => guardianOverview.value?.statusText || '守护中')
const guardianStatusDescription = computed(() => guardianOverview.value?.statusDescription || '家属可随时查看当前位置并发起远程关怀。')
const guardianLocationText = computed(() => guardianOverview.value?.locationText || currentAddress.value || '暂无位置数据')
const guardianUpdateText = computed(() => {
  const value = guardianOverview.value?.updateTime
  if (!value) {
    return '暂无更新'
  }
  return /\d{4}-\d{2}-\d{2}/.test(value) ? formatRelativeTime(value) : value
})
const guardianBatteryText = computed(() => {
  const batteryLevel = guardianOverview.value?.batteryLevel
  if (batteryLevel == null) {
    return '守护中'
  }
  return `电量 ${batteryLevel}%`
})
const guardianTags = computed(() => guardianOverview.value?.tags || [])
const activeSosAlert = computed(() => latestGuardianAlert.value?.alertType === 'SOS' && !latestGuardianAlert.value?.handled)
const sosLocationText = computed(() => latestGuardianAlert.value?.locationText || guardianLocationText.value || currentAddress.value || '暂无位置信息')
const sosAlertTimeText = computed(() => {
  const value = latestGuardianAlert.value?.timestamp
  if (!value) {
    return '刚刚'
  }
  return typeof value === 'number' ? formatRelativeTime(value) : (/\d{4}-\d{2}-\d{2}/.test(value) ? formatRelativeTime(value) : value)
})
const sosContactPhone = computed(() => elderStore.elderInfo?.phone || elderStore.elderInfo?.emergencyPhone || '')
const guardianLatestEventLabel = computed(() => {
  if (latestGuardianDestination.value?.destination) return '最近下发目的地'
  if (latestGuardianComfort.value?.content) return '最近安抚语音'
  if (latestGuardianAlert.value?.message) return '最近自动提醒'
  return '协同状态'
})
const guardianLatestEventText = computed(() => {
  if (latestGuardianDestination.value?.destination) {
    return latestGuardianDestination.value.destination
  }
  if (latestGuardianComfort.value?.content) {
    return latestGuardianComfort.value.content
  }
  if (latestGuardianAlert.value?.message) {
    return latestGuardianAlert.value.message
  }
  return guardianStatusDescription.value
})

const pendingAlarms = computed(() => {
  const alarms = alarmStore.alarmList || []
  return alarms.filter(item => item.status === '0' || item.status === 'pending')
})

const latestPendingAlarm = computed(() => pendingAlarms.value[0] || null)
const latestSosAlarm = computed(() => {
  const alarms = alarmStore.alarmList || []
  return alarms.find(item => {
    const rawType = item?.alarmType || ''
    return (item.status === '0' || item.status === 'pending') && (rawType.toLowerCase().includes('sos') || rawType.includes('求助'))
  }) || null
})

const fallSafetyLevel = computed(() => (sensorData.value?.isFall ? 'danger' : 'safe'))
const fallSafetyLabel = computed(() => (sensorData.value?.isFall ? '风险' : '正常'))
const fallSafetyDesc = computed(() => {
  if (sensorData.value?.isFall) {
    return '检测到跌倒风险，请尽快查看并确认设备状态'
  }
  return '当前未检测到跌倒风险，盲杖状态稳定'
})
const fallSafetyMeta = computed(() => {
  if (sensorData.value?.fallConfidence != null) {
    return `跌倒置信度 ${Math.round(Number(sensorData.value.fallConfidence) * 100)}%`
  }
  return sensorData.value?.isFall ? '请及时处理最新告警' : '点击可查看历史告警记录'
})

const emergencySafetyLevel = computed(() => (pendingAlarms.value.length > 0 ? 'danger' : 'safe'))
const emergencySafetyLabel = computed(() => (pendingAlarms.value.length > 0 ? '待处理' : '平稳'))
const emergencySafetyDesc = computed(() => {
  if (pendingAlarms.value.length > 0) {
    return `当前有 ${pendingAlarms.value.length} 条待处理提醒，请及时确认`
  }
  return '当前暂无紧急提醒，守护链路运行正常'
})
const emergencySafetyMeta = computed(() => {
  if (latestPendingAlarm.value?.alarmType) {
    return `最近提醒：${latestPendingAlarm.value.alarmType}`
  }
  return '点击可进入报警页查看全部记录'
})

// 最新动态 — 从报警列表取最新三条
const recentActivities = computed(() => {
  const alarms = alarmStore.alarmList || []
  if (alarms.length === 0) {
    return [{
      id: 0,
      type: 'info',
      icon: '✅',
      typeLabel: '系统',
      statusLabel: '平稳',
      statusClass: 'handled',
      title: '暂无最新动态',
      desc: '当前没有新的报警或安全提醒，系统运行正常。',
      time: ''
    }]
  }
  return alarms.slice(0, 3).map((a, i) => {
    const rawType = a.alarmType || '系统通知'
    const isPending = a.status === '0' || a.status === 'pending'
    const isFence = rawType.includes('围栏') || rawType.includes('越界')
    const isFall = rawType.includes('跌倒') || rawType.includes('摔倒')
    const isBattery = rawType.includes('电')
    const isSos = rawType.toLowerCase().includes('sos') || rawType.includes('求助')

    let type = 'alarm'
    let icon = '🔔'
    let typeLabel = '提醒'

    if (isFence) {
      type = 'location'
      icon = '📍'
      typeLabel = '围栏'
    } else if (isBattery) {
      type = 'battery'
      icon = '�'
      typeLabel = '电量'
    } else if (isFall) {
      type = 'alarm'
      icon = '🚨'
      typeLabel = '跌倒'
    } else if (isSos) {
      type = 'alarm'
      icon = '🆘'
      typeLabel = '求助'
    }

    return {
      id: a.id || i,
      type,
      icon,
      typeLabel,
      statusLabel: isPending ? '待处理' : '已处理',
      statusClass: isPending ? 'pending' : 'handled',
      title: rawType,
      desc: isPending ? '检测到新的安全提醒，建议尽快进入报警页确认。' : '该提醒已完成处理，可进入报警页查看详情。',
      time: a.alarmTime ? formatRelativeTime(a.alarmTime) : ''
    }
  })
})

// 定时器
let locationTimer = null
let dataTimer = null

const comfortTemplates = [
  '别着急，家属正在关注你，请先在安全区域稍作等待。',
  '请保持原地，我正在查看你的位置并联系你。',
  '先慢慢停下，确认周围安全后再继续行动。'
]
const comfortActionItems = [...comfortTemplates, '自定义输入...']
const destinationTemplates = [
  '回家',
  '附近商场',
  '社区服务站'
]
const destinationActionItems = [...destinationTemplates, '自定义输入...']

const refreshGuardianCaches = () => {
  const app = getApp()
  latestGuardianAlert.value = app?.globalData?.latestGuardianAlert || uni.getStorageSync('latestGuardianAlert') || null
  latestGuardianComfort.value = app?.globalData?.latestGuardianComfort || uni.getStorageSync('latestGuardianComfort') || null
  latestGuardianDestination.value = app?.globalData?.latestGuardianDestination || uni.getStorageSync('latestGuardianDestination') || null
}

const syncGuardianCache = (key, value) => {
  const app = getApp()
  if (app) {
    app.globalData = app.globalData || {}
    app.globalData[key] = value
  }
  uni.setStorageSync(key, value)
  refreshGuardianCaches()
}

const loadGuardianOverview = async () => {
  if (!currentDevice.value || !currentDevice.value.deviceId) {
    guardianOverview.value = { tags: [] }
    refreshGuardianCaches()
    return
  }
  try {
    const res = await getGuardianCareOverview(currentDevice.value.deviceId)
    guardianOverview.value = res.data || { tags: [] }
  } catch (error) {
    console.error('加载家属守护概览失败', error)
  } finally {
    refreshGuardianCaches()
  }
}

const loadBlindProfileData = async () => {
  try {
    const res = await getBlindProfile()
    if (res?.data) {
      elderStore.setElderInfo(res.data)
    }
  } catch (error) {
    console.error('加载盲人档案失败', error)
  }
}

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
  userStore.restoreFromStorage()
  deviceStore.restoreFromStorage()
  elderStore.restoreFromStorage()
  refreshGuardianCaches()
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
  
  loadBlindProfileData()
  loadDeviceData()
  startAutoRefresh()
})

onShow(() => {
  refreshGuardianCaches()
  const app = getApp()
  const pendingGuardianSosAnnouncement = !!(app?.globalData?.pendingGuardianSosAnnouncement || uni.getStorageSync('pendingGuardianSosAnnouncement'))
  if (pendingGuardianSosAnnouncement && latestGuardianAlert.value?.alertType === 'SOS') {
    uni.showToast({ title: '收到 SOS 求助', icon: 'none', duration: 2200 })
    const playPrompt = app?.globalData?.playCrossingPrompt || getApp.__$playCrossingPrompt
    if (typeof playPrompt === 'function') {
      playPrompt(latestGuardianAlert.value.message)
    }
    if (app) {
      app.globalData = app.globalData || {}
      app.globalData.pendingGuardianSosAnnouncement = false
      app.globalData.deferGuardianSosAnnouncement = false
    }
    uni.setStorageSync('pendingGuardianSosAnnouncement', false)
    uni.setStorageSync('deferGuardianSosAnnouncement', false)
  }
  if (currentDevice.value && currentDevice.value.deviceId) {
    loadGuardianOverview()
  }
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
      alarmStore.fetchAlarmList(),
      loadGuardianOverview()
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
        deviceStore.fetchSensorData(currentDevice.value.deviceId),
        loadGuardianOverview()
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

const navigateToCrossingAssist = () => {
  uni.navigateTo({
    url: '/pages/crossing/crossing'
  })
}

const navigateToUserTerminal = () => {
  uni.navigateTo({
    url: '/pages/user-terminal/user-terminal'
  })
}

const handleSosCall = () => {
  if (!sosContactPhone.value) {
    uni.showToast({ title: '未配置联系电话', icon: 'none' })
    return
  }
  uni.makePhoneCall({
    phoneNumber: String(sosContactPhone.value)
  })
}

const handleSosLocate = () => {
  centerOnDevice()
}

const handleSosResolve = async () => {
  try {
    if (latestSosAlarm.value?.id) {
      await handleAlarmApi(latestSosAlarm.value.id, { status: '1' })
    }
    syncGuardianCache('latestGuardianAlert', {
      ...(latestGuardianAlert.value || {}),
      handled: true
    })
    await alarmStore.fetchAlarmList()
    await loadGuardianOverview()
    uni.showToast({ title: '已标记处理', icon: 'success' })
  } catch (error) {
    console.error('标记SOS处理失败', error)
  }
}

const sendGuardianComfortMessage = async (content) => {
  const normalizedContent = (content || '').trim()
  if (!normalizedContent) {
    uni.showToast({ title: '请输入安抚内容', icon: 'none' })
    return
  }
  try {
    const res = await sendGuardianComfort(currentDevice.value.deviceId, { content: normalizedContent })
    syncGuardianCache('latestGuardianComfort', {
      content: normalizedContent,
      timestamp: res.data?.sentAt || new Date().toISOString()
    })
    uni.showToast({ title: '安抚语音已发送', icon: 'success' })
  } catch (error) {
    console.error('发送安抚语音失败', error)
  }
}

const sendGuardianComfortAction = () => {
  if (!currentDevice.value || !currentDevice.value.deviceId) {
    uni.showToast({ title: '请先绑定设备', icon: 'none' })
    return
  }
  uni.showActionSheet({
    itemList: comfortActionItems,
    success: async ({ tapIndex }) => {
      if (tapIndex === comfortTemplates.length) {
        uni.showModal({
          title: '自定义安抚语音',
          editable: true,
          placeholderText: '请输入想对用户端播报的话',
          success: async (modalRes) => {
            if (!modalRes.confirm) {
              return
            }
            await sendGuardianComfortMessage(modalRes.content)
          }
        })
        return
      }
      await sendGuardianComfortMessage(comfortTemplates[tapIndex])
    }
  })
}

const sendGuardianDestinationMessage = async (destination) => {
  const normalizedDestination = (destination || '').trim()
  if (!normalizedDestination) {
    uni.showToast({ title: '请输入目的地', icon: 'none' })
    return
  }
  try {
    const res = await sendGuardianDestination(currentDevice.value.deviceId, { destination: normalizedDestination })
    syncGuardianCache('latestGuardianDestination', {
      destination: normalizedDestination,
      timestamp: res.data?.sentAt || new Date().toISOString()
    })
    uni.showToast({ title: '目的地已发送', icon: 'success' })
  } catch (error) {
    console.error('发送目的地失败', error)
  }
}

const chooseGuardianDestination = () => {
  if (!currentDevice.value || !currentDevice.value.deviceId) {
    uni.showToast({ title: '请先绑定设备', icon: 'none' })
    return
  }
  uni.showActionSheet({
    itemList: destinationActionItems,
    success: async ({ tapIndex }) => {
      if (tapIndex === destinationTemplates.length) {
        uni.showModal({
          title: '自定义目的地',
          editable: true,
          placeholderText: '请输入想发送给用户端的目的地',
          success: async (modalRes) => {
            if (!modalRes.confirm) {
              return
            }
            await sendGuardianDestinationMessage(modalRes.content)
          }
        })
        return
      }
      await sendGuardianDestinationMessage(destinationTemplates[tapIndex])
    }
  })
}

// 标记点击
const onMarkerTap = (e) => {
  console.log('Marker tapped', e)
}
</script>

<style lang="scss" scoped>
.home-page {
  min-height: 100vh;
  background: linear-gradient(180deg, #eef4ff 0%, #f8fafc 32%, #f4f7fb 100%);
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
  padding: 24rpx 0 40rpx;
}

.home-hero {
  margin: 0 24rpx 24rpx;
  padding: 30rpx;
  border-radius: 30rpx;
  background: linear-gradient(135deg, #081226 0%, #1d4ed8 55%, #14b8a6 100%);
  box-shadow: 0 18rpx 36rpx rgba(29, 78, 216, 0.22);
}

.home-hero-copy {
  display: flex;
  flex-direction: column;
}

.hero-kicker {
  display: inline-flex;
  align-self: flex-start;
  padding: 8rpx 18rpx;
  border-radius: 999rpx;
  background: rgba(255, 255, 255, 0.16);
  color: #ffffff;
  font-size: 20rpx;
  letter-spacing: 2rpx;
}

.hero-title {
  margin-top: 18rpx;
  font-size: 40rpx;
  line-height: 1.3;
  font-weight: 700;
  color: #ffffff;
}

.hero-desc {
  margin-top: 12rpx;
  font-size: 24rpx;
  line-height: 1.7;
  color: rgba(255, 255, 255, 0.82);
}

.hero-status-pill {
  display: inline-flex;
  align-self: flex-start;
  margin-top: 22rpx;
  padding: 12rpx 18rpx;
  border-radius: 999rpx;
  font-size: 22rpx;
  font-weight: 700;
}

.hero-status-pill.ready {
  background: rgba(34, 197, 94, 0.2);
  color: #dcfce7;
}

.hero-status-pill.idle {
  background: rgba(255, 255, 255, 0.16);
  color: #e2e8f0;
}

.map-section {
  height: 512rpx;
  position: relative;
  margin: 0 24rpx;
  border-radius: 32rpx;
  overflow: hidden;
  box-shadow: 0 18rpx 36rpx rgba(15, 23, 42, 0.08);

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
      background: rgba(255, 255, 255, 0.94);
      border-radius: 20rpx;
      display: flex;
      flex-direction: column;
      align-items: center;
      justify-content: center;
      box-shadow: 0 10rpx 24rpx rgba(15, 23, 42, 0.12);

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
  margin: -48rpx 24rpx 32rpx;
  background: linear-gradient(180deg, #ffffff 0%, #f8fbff 100%);
  border-radius: 28rpx;
  padding: 32rpx;
  box-shadow: 0 18rpx 36rpx rgba(15, 23, 42, 0.08);
  position: relative;
  z-index: 10;
  border: 1rpx solid rgba(219, 234, 254, 0.9);

  .device-address {
    display: flex;
    align-items: center;
    gap: 8rpx;
    margin-bottom: 24rpx;
    padding: 16rpx 18rpx;
    background: #eff6ff;
    border-radius: 18rpx;

    .address-icon {
      font-size: 24rpx;
    }

    .address-text {
      font-size: 24rpx;
      color: #374151;
    }
  }

  .status-overview {
    display: grid;
    grid-template-columns: repeat(3, 1fr);
    gap: 16rpx;
    margin-bottom: 24rpx;

    .overview-chip {
      position: relative;
      overflow: hidden;
      padding: 18rpx;
      border-radius: 22rpx;
      background: linear-gradient(180deg, #ffffff 0%, #f8fafc 100%);
      border: 1rpx solid #e2e8f0;
      box-shadow: 0 10rpx 20rpx rgba(15, 23, 42, 0.05);

      &.device {
        background: linear-gradient(180deg, #ffffff 0%, #eff6ff 100%);
        border-color: #dbeafe;
      }

      &.steps {
        background: linear-gradient(180deg, #ffffff 0%, #fefce8 100%);
        border-color: #fde68a;
      }

      &.guard {
        background: linear-gradient(180deg, #ffffff 0%, #ecfdf5 100%);
        border-color: #bbf7d0;
      }

      .overview-icon {
        display: inline-flex;
        align-items: center;
        justify-content: center;
        width: 46rpx;
        height: 46rpx;
        border-radius: 14rpx;
        background: rgba(255, 255, 255, 0.68);
        font-size: 24rpx;
      }

      .overview-label {
        display: block;
        margin-top: 12rpx;
        font-size: 20rpx;
        color: #64748b;
      }

      .overview-value {
        display: block;
        margin-top: 8rpx;
        font-size: 28rpx;
        font-weight: 700;
        color: #0f172a;
        line-height: 1.5;
        word-break: break-all;
      }
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
        font-size: 30rpx;
        font-weight: 700;
        color: #1f2937;
      }

      .status-badge {
        padding: 8rpx 18rpx;
        border-radius: 999rpx;
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
      padding: 10rpx 18rpx;
      background: #ecfdf5;
      border-radius: 999rpx;
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
      position: relative;
      overflow: hidden;
      flex: 1;
      min-width: 0;
      background: linear-gradient(180deg, #ffffff 0%, #f8fafc 100%);
      border-radius: 22rpx;
      padding: 24rpx;
      text-align: center;
      box-shadow: inset 0 0 0 1rpx #e2e8f0;

      &.obstacle {
        background: linear-gradient(180deg, #ffffff 0%, #eff6ff 100%);
      }

      &.steps {
        background: linear-gradient(180deg, #ffffff 0%, #fefce8 100%);
      }

      &.rest {
        background: linear-gradient(180deg, #ffffff 0%, #f5f3ff 100%);
      }

      .sensor-icon {
        display: inline-flex;
        align-items: center;
        justify-content: center;
        width: 54rpx;
        height: 54rpx;
        border-radius: 16rpx;
        background: rgba(255, 255, 255, 0.75);
        font-size: 28rpx;
      }

      .sensor-label {
        display: block;
        margin-top: 16rpx;
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
  margin: 0 24rpx 24rpx;
  padding: 30rpx 32rpx;
  border-radius: 28rpx;
  background: linear-gradient(135deg, #6366f1 0%, #8b5cf6 60%, #ec4899 100%);
  display: flex;
  align-items: center;
  justify-content: space-between;
  box-shadow: 0 18rpx 34rpx rgba(99, 102, 241, 0.24);

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
    transform: translateY(2rpx);
  }
}

.crossing-entry {
  margin: 0 24rpx 24rpx;
  padding: 30rpx 32rpx;
  border-radius: 28rpx;
  background: linear-gradient(135deg, #0f172a 0%, #1d4ed8 55%, #22c55e 100%);
  display: flex;
  align-items: center;
  justify-content: space-between;
  box-shadow: 0 18rpx 34rpx rgba(29, 78, 216, 0.2);

  .crossing-entry-left {
    display: flex;
    align-items: center;
    flex: 1;
  }

  .crossing-icon {
    width: 80rpx;
    height: 80rpx;
    border-radius: 50%;
    background: rgba(255, 255, 255, 0.2);
    display: flex;
    align-items: center;
    justify-content: center;
    font-size: 40rpx;
    margin-right: 24rpx;
    flex-shrink: 0;
  }

  .crossing-info {
    display: flex;
    flex-direction: column;
    gap: 8rpx;
  }

  .crossing-title {
    font-size: 30rpx;
    font-weight: 700;
    color: #ffffff;
  }

  .crossing-desc {
    font-size: 24rpx;
    color: rgba(255, 255, 255, 0.84);
  }

  .crossing-arrow {
    font-size: 40rpx;
    color: rgba(255, 255, 255, 0.84);
    margin-left: 24rpx;
  }

  &:active {
    opacity: 0.85;
    transform: translateY(2rpx);
  }
}

.guardian-section {
  margin-top: 24rpx;
  padding: 30rpx;
  border-radius: 28rpx;
  background: linear-gradient(135deg, #f8fafc 0%, #ffffff 42%, #eef6ff 100%);
  border: 1rpx solid #dbeafe;
  box-shadow: 0 16rpx 30rpx rgba(37, 99, 235, 0.08);

  .guardian-header {
    display: flex;
    align-items: flex-start;
    justify-content: space-between;
    gap: 20rpx;
  }

  .guardian-caption {
    flex: 1;

    .section-title {
      font-size: 30rpx;
      font-weight: 700;
      color: #0f172a;
    }
  }

  .guardian-status-pill {
    padding: 10rpx 18rpx;
    border-radius: 999rpx;
    font-size: 22rpx;
    font-weight: 700;
    white-space: nowrap;

    &.safe {
      background: rgba(34, 197, 94, 0.16);
      color: #15803d;
    }

    &.warning {
      background: rgba(245, 158, 11, 0.16);
      color: #d97706;
    }

    &.danger {
      background: rgba(239, 68, 68, 0.14);
      color: #dc2626;
    }
  }

  .guardian-desc {
    display: block;
    margin-top: 18rpx;
    font-size: 24rpx;
    line-height: 1.7;
    color: #475569;
  }

  .guardian-grid {
    display: grid;
    grid-template-columns: repeat(3, 1fr);
    gap: 18rpx;
    margin-top: 24rpx;
  }

  .guardian-metric {
    padding: 20rpx;
    border-radius: 22rpx;
    background: #ffffff;
    border: 1rpx solid #e5e7eb;

    &.location {
      background: linear-gradient(180deg, #eff6ff 0%, #ffffff 100%);
    }

    &.time {
      background: linear-gradient(180deg, #f5f3ff 0%, #ffffff 100%);
    }

    &.battery {
      background: linear-gradient(180deg, #ecfeff 0%, #ffffff 100%);
    }

    .guardian-metric-label {
      display: block;
      font-size: 22rpx;
      color: #64748b;
    }

    .guardian-metric-value {
      display: block;
      margin-top: 12rpx;
      font-size: 24rpx;
      font-weight: 700;
      color: #0f172a;
      line-height: 1.5;
      word-break: break-all;
    }
  }

  .guardian-tags {
    display: flex;
    flex-wrap: wrap;
    gap: 12rpx;
    margin-top: 20rpx;
  }

  .guardian-tag {
    display: inline-flex;
    align-items: center;
    justify-content: center;
    padding: 8rpx 16rpx;
    border-radius: 999rpx;
    font-size: 20rpx;
    font-weight: 700;

    &.safe {
      background: rgba(34, 197, 94, 0.12);
      color: #15803d;
    }

    &.warning {
      background: rgba(245, 158, 11, 0.14);
      color: #b45309;
    }

    &.danger {
      background: rgba(239, 68, 68, 0.12);
      color: #dc2626;
    }

    &.neutral {
      background: rgba(148, 163, 184, 0.12);
      color: #64748b;
    }
  }

  .guardian-note {
    margin-top: 20rpx;
    padding: 20rpx 22rpx;
    border-radius: 22rpx;
    background: rgba(255, 255, 255, 0.85);
    border: 1rpx solid #e2e8f0;

    .guardian-note-label {
      display: block;
      font-size: 22rpx;
      color: #64748b;
    }

    .guardian-note-value {
      display: block;
      margin-top: 10rpx;
      font-size: 24rpx;
      color: #1e293b;
      line-height: 1.7;
    }
  }

  .guardian-actions {
    display: flex;
    gap: 16rpx;
    margin-top: 24rpx;
  }

  .guardian-btn {
    flex: 1;
    padding: 22rpx 20rpx;
    border-radius: 22rpx;
    display: flex;
    align-items: center;
    justify-content: center;
    gap: 10rpx;
    font-size: 24rpx;
    font-weight: 700;
    border: 1rpx solid transparent;

    &.primary {
      background: linear-gradient(135deg, #2563eb 0%, #1d4ed8 100%);
      color: #ffffff;
      box-shadow: 0 14rpx 28rpx rgba(37, 99, 235, 0.18);
    }

    &.secondary {
      background: rgba(255, 255, 255, 0.9);
      color: #1d4ed8;
      border-color: #bfdbfe;
    }

    &:active {
      transform: translateY(2rpx);
    }
  }

  .guardian-btn-icon {
    font-size: 28rpx;
  }
}

.sos-alert-card {
  margin-top: 24rpx;
  padding: 28rpx;
  border-radius: 28rpx;
  background: linear-gradient(135deg, #991b1b 0%, #dc2626 52%, #ef4444 100%);
  box-shadow: 0 18rpx 34rpx rgba(220, 38, 38, 0.24);
  color: #ffffff;

  .sos-alert-head {
    display: flex;
    justify-content: space-between;
    gap: 20rpx;
  }

  .sos-alert-copy {
    flex: 1;
  }

  .sos-alert-kicker {
    display: block;
    font-size: 22rpx;
    letter-spacing: 2rpx;
    opacity: 0.82;
  }

  .sos-alert-title {
    display: block;
    margin-top: 12rpx;
    font-size: 34rpx;
    font-weight: 700;
  }

  .sos-alert-desc {
    display: block;
    margin-top: 12rpx;
    font-size: 24rpx;
    line-height: 1.7;
    color: rgba(255, 255, 255, 0.9);
  }

  .sos-alert-time {
    font-size: 22rpx;
    white-space: nowrap;
    color: rgba(255, 255, 255, 0.86);
  }

  .sos-alert-meta {
    display: grid;
    grid-template-columns: 2fr 1fr;
    gap: 16rpx;
    margin-top: 22rpx;
  }

  .sos-alert-meta-item {
    padding: 18rpx 20rpx;
    border-radius: 22rpx;
    background: rgba(255, 255, 255, 0.12);

    &.compact {
      text-align: center;
    }
  }

  .sos-meta-label {
    display: block;
    font-size: 20rpx;
    color: rgba(255, 255, 255, 0.78);
  }

  .sos-meta-value {
    display: block;
    margin-top: 10rpx;
    font-size: 24rpx;
    font-weight: 700;
    line-height: 1.6;
    color: #ffffff;
    word-break: break-all;
  }

  .sos-alert-actions {
    display: flex;
    gap: 14rpx;
    margin-top: 22rpx;
  }

  .sos-action-btn {
    flex: 1;
    padding: 22rpx 16rpx;
    border-radius: 20rpx;
    display: flex;
    align-items: center;
    justify-content: center;
    text-align: center;
    border: 1rpx solid transparent;

    &.primary {
      background: #ffffff;
      color: #dc2626;
    }

    &.secondary {
      background: rgba(255, 255, 255, 0.14);
      color: #ffffff;
      border-color: rgba(255, 255, 255, 0.2);
    }

    &.ghost {
      background: rgba(127, 29, 29, 0.28);
      color: #ffffff;
      border-color: rgba(255, 255, 255, 0.12);
    }
  }

  .sos-action-text {
    font-size: 24rpx;
    font-weight: 700;
    line-height: 1.4;
  }
}

.user-terminal-entry {
  margin-top: 24rpx;
  padding: 24rpx 28rpx;
  border-radius: 28rpx;
  background: linear-gradient(135deg, #f8fbff 0%, #eef4ff 100%);
  border: 1rpx solid rgba(59, 130, 246, 0.12);
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 24rpx;
  box-shadow: 0 14rpx 28rpx rgba(59, 130, 246, 0.08);

  .user-terminal-entry-left {
    display: flex;
    align-items: center;
    flex: 1;
    gap: 22rpx;
  }

  .user-terminal-icon {
    width: 72rpx;
    height: 72rpx;
    border-radius: 20rpx;
    background: linear-gradient(135deg, #dbeafe 0%, #bfdbfe 100%);
    display: flex;
    align-items: center;
    justify-content: center;
    font-size: 34rpx;
    flex-shrink: 0;
  }

  .user-terminal-info {
    display: flex;
    flex-direction: column;
    gap: 6rpx;
    flex: 1;
  }

  .user-terminal-kicker {
    font-size: 20rpx;
    letter-spacing: 1.5rpx;
    color: #64748b;
  }

  .user-terminal-title {
    font-size: 28rpx;
    font-weight: 700;
    color: #0f172a;
  }

  .user-terminal-desc {
    font-size: 22rpx;
    line-height: 1.6;
    color: #64748b;
  }

  .user-terminal-arrow {
    font-size: 38rpx;
    color: #94a3b8;
    margin-left: 16rpx;
  }

  &:active {
    opacity: 0.92;
    transform: translateY(2rpx);
  }
}

.section-caption {
  display: flex;
  flex-direction: column;
  gap: 8rpx;
}

.section-caption.compact {
  gap: 4rpx;
}

.section-kicker {
  font-size: 20rpx;
  color: #64748b;
  letter-spacing: 2rpx;
}

.safety-section {
  padding: 0 24rpx 32rpx;

  .section-title {
    font-size: 28rpx;
    font-weight: 700;
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
      border-radius: 26rpx;
      display: flex;
      align-items: center;
      justify-content: space-between;
      border: 1rpx solid #f3f4f6;
      box-shadow: 0 12rpx 26rpx rgba(15, 23, 42, 0.06);

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

        .safety-title-row {
          display: flex;
          align-items: center;
          justify-content: space-between;
          gap: 12rpx;
          margin-bottom: 8rpx;
        }

        .safety-title {
          display: block;
          font-size: 28rpx;
          font-weight: 600;
          color: #1f2937;
        }

        .safety-desc {
          display: block;
          font-size: 24rpx;
          color: #475569;
          line-height: 1.6;
        }

        .safety-meta {
          display: block;
          margin-top: 10rpx;
          font-size: 22rpx;
          color: #64748b;
          line-height: 1.5;
        }

        .safety-badge {
          padding: 6rpx 14rpx;
          border-radius: 999rpx;
          font-size: 20rpx;
          font-weight: 700;
          white-space: nowrap;

          &.safe {
            background: rgba(34, 197, 94, 0.16);
            color: #15803d;
          }

          &.danger {
            background: rgba(239, 68, 68, 0.14);
            color: #dc2626;
          }
        }
      }

      &:active {
        transform: translateY(2rpx);
      }
    }
  }
}

.activity-section {
  padding: 0 24rpx;

  .section-header {
    display: flex;
    justify-content: space-between;
    align-items: center;
    margin-bottom: 24rpx;

    .section-title {
      font-size: 28rpx;
      font-weight: 700;
      color: #1f2937;
    }

    .more-btn {
      font-size: 24rpx;
      color: #2563eb;
      font-weight: 600;
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
      padding: 26rpx;
      background: linear-gradient(180deg, #ffffff 0%, #f8fafc 100%);
      border-radius: 22rpx;
      border: 1rpx solid #e5e7eb;
      box-shadow: 0 12rpx 26rpx rgba(15, 23, 42, 0.05);
      position: relative;
      overflow: hidden;

      &:active {
        transform: translateY(2rpx);
      }

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

        &.info {
          background: #dcfce7;
        }
      }

      .activity-content {
        flex: 1;

        .activity-meta-row {
          display: flex;
          align-items: center;
          gap: 12rpx;
          margin-bottom: 12rpx;
        }

        .activity-type-tag,
        .activity-status-tag {
          display: inline-flex;
          align-items: center;
          justify-content: center;
          padding: 6rpx 14rpx;
          border-radius: 999rpx;
          font-size: 20rpx;
          font-weight: 700;
        }

        .activity-type-tag.alarm {
          background: #fee2e2;
          color: #dc2626;
        }

        .activity-type-tag.location {
          background: #dbeafe;
          color: #2563eb;
        }

        .activity-type-tag.battery {
          background: #fef3c7;
          color: #d97706;
        }

        .activity-type-tag.info {
          background: #dcfce7;
          color: #15803d;
        }

        .activity-status-tag.pending {
          background: rgba(239, 68, 68, 0.12);
          color: #dc2626;
        }

        .activity-status-tag.handled {
          background: rgba(34, 197, 94, 0.12);
          color: #15803d;
        }

        .activity-title {
          display: block;
          font-size: 28rpx;
          color: #0f172a;
          margin-bottom: 8rpx;
          font-weight: 600;
        }

        .activity-desc {
          display: block;
          font-size: 24rpx;
          color: #64748b;
          line-height: 1.6;
          margin-bottom: 10rpx;
        }

        .activity-time {
          display: block;
          font-size: 24rpx;
          color: #9ca3af;
        }
      }

      .activity-arrow {
        font-size: 36rpx;
        color: #cbd5e1;
      }
    }
  }
}
</style>
