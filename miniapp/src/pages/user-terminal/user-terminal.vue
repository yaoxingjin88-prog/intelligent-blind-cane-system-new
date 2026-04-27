<template>
  <view class="user-terminal-page">
    <scroll-view class="content" scroll-y>
      <view class="hero-card">
        <view class="hero-copy">
          <text class="hero-kicker">User Terminal</text>
          <text class="hero-title">用户端陪伴与提醒</text>
          <text class="hero-desc">用于演示老人侧接收家属安抚、目的地与风险提醒，并支持语音播报。</text>
        </view>
        <view class="hero-pill" :class="statusLevel">
          <text>{{ statusHeadline }}</text>
        </view>
      </view>

      <view v-if="loading" class="loading-state">
        <text class="loading-icon">⏳</text>
        <text class="loading-text">正在同步用户端数据...</text>
      </view>

      <view v-else-if="!currentDevice || !currentDevice.deviceId" class="empty-card">
        <text class="empty-icon">📱</text>
        <text class="empty-title">尚未绑定设备</text>
        <text class="empty-desc">请先绑定盲杖设备，才能演示用户端接收安抚语音和目的地提醒。</text>
        <view class="empty-btn" @click="openDeviceManager">前往设备管理</view>
      </view>

      <view v-else class="terminal-body">
        <view class="profile-card">
          <view>
            <text class="profile-kicker">Profile</text>
            <text class="profile-name">{{ elderName }}</text>
            <text class="profile-desc">{{ elderAddress }}</text>
          </view>
          <view class="profile-side">
            <text class="profile-side-label">设备</text>
            <text class="profile-side-value">{{ currentDevice.deviceId }}</text>
          </view>
        </view>

        <view class="status-card">
          <view class="status-header">
            <view>
              <text class="section-kicker">Live Status</text>
              <text class="section-title">当前状态</text>
            </view>
            <text class="status-pill" :class="statusLevel">{{ statusHeadline }}</text>
          </view>
          <text class="status-summary">{{ statusSummary }}</text>
          <view class="status-grid">
            <view class="status-metric">
              <text class="status-label">当前位置</text>
              <text class="status-value">{{ locationText }}</text>
            </view>
            <view class="status-metric">
              <text class="status-label">最近更新</text>
              <text class="status-value">{{ latestUpdateText }}</text>
            </view>
            <view class="status-metric compact">
              <text class="status-label">守护家属</text>
              <text class="status-value">{{ guardianName }}</text>
            </view>
          </view>
        </view>

        <view class="action-row">
          <view class="action-card primary" @click="replayComfort">
            <text class="action-icon">🔊</text>
            <text class="action-title">播报安抚语音</text>
            <text class="action-desc">重新播放最近一条家属安抚</text>
          </view>
          <view class="action-card secondary" @click="replayDestination">
            <text class="action-icon">🧭</text>
            <text class="action-title">播报当前目的地</text>
            <text class="action-desc">语音确认当前导航协同目标</text>
          </view>
          <view class="action-card danger" @click="handleEmergencyAssist">
            <text class="action-icon">🆘</text>
            <text class="action-title">一键求助</text>
            <text class="action-desc">演示模式下发起 SOS 协助提示</text>
          </view>
        </view>

        <view class="message-section">
          <view class="message-card destination">
            <view class="message-top">
              <text class="message-tag">Destination</text>
              <text class="message-time">{{ destinationTimeText }}</text>
            </view>
            <text class="message-title">当前目的地</text>
            <text class="message-content">{{ destinationText }}</text>
            <text class="message-foot">{{ destinationHint }}</text>
          </view>

          <view class="message-card comfort">
            <view class="message-top">
              <text class="message-tag">Comfort</text>
              <text class="message-time">{{ comfortTimeText }}</text>
            </view>
            <text class="message-title">最近家属安抚</text>
            <text class="message-content">{{ comfortText }}</text>
            <text class="message-foot">{{ comfortHint }}</text>
          </view>

          <view class="message-card alert">
            <view class="message-top">
              <text class="message-tag">Alert</text>
              <text class="message-time">{{ alertTimeText }}</text>
            </view>
            <text class="message-title">风险提醒</text>
            <text class="message-content">{{ alertText }}</text>
            <text class="message-foot">{{ alertHint }}</text>
          </view>
        </view>

        <view class="overview-section">
          <view class="section-header">
            <view>
              <text class="section-kicker">Overview</text>
              <text class="section-title">用户端实时数据</text>
            </view>
            <text class="section-link" @click="navigateToAlarm">查看报警</text>
          </view>
          <view class="overview-grid">
            <view class="overview-item battery">
              <text class="overview-label">设备电量</text>
              <text class="overview-value">{{ batteryText }}</text>
            </view>
            <view class="overview-item steps">
              <text class="overview-label">今日步数</text>
              <text class="overview-value">{{ stepText }}</text>
            </view>
            <view class="overview-item obstacle">
              <text class="overview-label">前方障碍</text>
              <text class="overview-value">{{ obstacleText }}</text>
            </view>
            <view class="overview-item still">
              <text class="overview-label">静止时长</text>
              <text class="overview-value">{{ stationaryText }}</text>
            </view>
          </view>
        </view>

        <view class="support-section">
          <view class="support-card" @click="navigateToCrossingAssist">
            <view>
              <text class="support-title">路口辅助演示</text>
              <text class="support-desc">继续前往现有路口辅助页面，验证用户端的语音提醒体验。</text>
            </view>
            <text class="support-arrow">›</text>
          </view>
        </view>
      </view>
    </scroll-view>

    <view v-if="sosFullscreenVisible" class="sos-fullscreen-mask">
      <view class="sos-fullscreen-card">
        <text class="sos-fullscreen-kicker">Emergency SOS</text>
        <text class="sos-fullscreen-title">求助已发出</text>
        <text class="sos-fullscreen-desc">家属端已收到求助提醒，请在安全区域稍作等待，保持设备在线并注意周边环境。</text>
        <view class="sos-fullscreen-panel">
          <view class="sos-panel-item">
            <text class="sos-panel-label">当前位置</text>
            <text class="sos-panel-value">{{ sosFullscreenLocation }}</text>
          </view>
          <view class="sos-panel-item compact">
            <text class="sos-panel-label">发起时间</text>
            <text class="sos-panel-value">{{ sosFullscreenTime }}</text>
          </view>
        </view>
        <view class="sos-fullscreen-actions">
          <view class="sos-fullscreen-btn light" @click="playPrompt('已发起求助，请在安全区域稍作等待，家属会尽快查看。')">再次播报</view>
          <view class="sos-fullscreen-btn dark" @click="dismissSosFullscreen">我已知晓</view>
        </view>
      </view>
    </view>
  </view>
</template>

<script setup>
import { computed, onMounted, onUnmounted, ref } from 'vue'
import { onShow } from '@dcloudio/uni-app'
import { useAlarmStore, useDeviceStore, useElderStore, useUserStore } from '@/store'
import { getBlindProfile } from '@/api/blind'
import { triggerGuardianSos } from '@/api/guardian'
import { formatDateTime, formatRelativeTime } from '@/utils'

const userStore = useUserStore()
const deviceStore = useDeviceStore()
const elderStore = useElderStore()
const alarmStore = useAlarmStore()

const loading = ref(true)
const latestGuardianAlert = ref(null)
const latestGuardianComfort = ref(null)
const latestGuardianDestination = ref(null)
const sosFullscreenVisible = ref(false)
const sosFullscreenMeta = ref(null)

let refreshTimer = null

const currentDevice = computed(() => deviceStore.currentDevice)
const deviceStatus = computed(() => deviceStore.deviceStatus)
const deviceLocation = computed(() => deviceStore.deviceLocation)
const sensorData = computed(() => deviceStore.sensorData)
const pendingAlarmCount = computed(() => (alarmStore.alarmList || []).filter(item => item.status === 'pending' || item.status === '0').length)

const elderName = computed(() => elderStore.elderInfo?.name || currentDevice.value?.deviceName || '老人用户')
const elderAddress = computed(() => elderStore.elderInfo?.address || deviceLocation.value?.address || '暂未完善联系地址')
const guardianName = computed(() => userStore.userInfo?.name || elderStore.elderInfo?.emergencyContact || '家属')

const statusLevel = computed(() => {
  if (!currentDevice.value || !currentDevice.value.deviceId) return 'idle'
  if (deviceStatus.value?.status !== 'online') return 'danger'
  if (latestGuardianAlert.value?.level === 'danger') return 'danger'
  if (latestGuardianAlert.value?.level === 'warning') return 'warning'
  if ((deviceStatus.value?.batteryLevel || 0) <= 20) return 'warning'
  return 'safe'
})

const statusHeadline = computed(() => {
  if (!currentDevice.value || !currentDevice.value.deviceId) return '待连接设备'
  if (statusLevel.value === 'danger') return '请优先关注安全'
  if (statusLevel.value === 'warning') return '建议减速并确认周围环境'
  return '状态平稳'
})

const statusSummary = computed(() => {
  if (latestGuardianAlert.value?.message) return latestGuardianAlert.value.message
  if (latestGuardianDestination.value?.destination) return `家属已协同设置目的地：${latestGuardianDestination.value.destination}`
  if (latestGuardianComfort.value?.content) return `家属留言：${latestGuardianComfort.value.content}`
  return '当前未收到新的高风险提醒，可按语音引导继续出行。'
})

const locationText = computed(() => {
  if (deviceLocation.value?.address) return deviceLocation.value.address
  if (deviceLocation.value?.latitude && deviceLocation.value?.longitude) {
    return `${Number(deviceLocation.value.latitude).toFixed(4)}, ${Number(deviceLocation.value.longitude).toFixed(4)}`
  }
  return '暂无定位信息'
})

const latestUpdateSource = computed(() => {
  return latestGuardianAlert.value?.timestamp || latestGuardianDestination.value?.timestamp || latestGuardianComfort.value?.timestamp || sensorData.value?.dataTime || sensorData.value?.createTime || null
})

const latestUpdateText = computed(() => {
  const value = latestUpdateSource.value
  if (!value) return '暂无更新'
  if (typeof value === 'number') return formatRelativeTime(value)
  return /\d{4}-\d{2}-\d{2}/.test(value) ? formatRelativeTime(value) : value
})

const destinationText = computed(() => latestGuardianDestination.value?.destination || '暂未收到新的目的地')
const destinationHint = computed(() => latestGuardianDestination.value?.message || '家属下发目的地后，会在这里展示并支持再次播报。')
const destinationTimeText = computed(() => formatMessageTime(latestGuardianDestination.value?.timestamp))

const comfortText = computed(() => latestGuardianComfort.value?.content || latestGuardianComfort.value?.message || '暂未收到新的安抚语音')
const comfortHint = computed(() => latestGuardianComfort.value ? '可点击上方按钮重新播报这条安抚语音。' : '家属发送的安抚内容会自动缓存到这里。')
const comfortTimeText = computed(() => formatMessageTime(latestGuardianComfort.value?.timestamp))

const alertText = computed(() => latestGuardianAlert.value?.message || (pendingAlarmCount.value ? `当前有 ${pendingAlarmCount.value} 条待处理提醒` : '当前暂无新的风险提醒'))
const alertHint = computed(() => {
  if (latestGuardianAlert.value?.alertType) return `提醒类型：${latestGuardianAlert.value.alertType}`
  if (pendingAlarmCount.value) return '可前往报警页查看详细处理状态。'
  return '系统会在跌倒、越界、低电量或静止偏久时自动提醒。'
})
const alertTimeText = computed(() => formatMessageTime(latestGuardianAlert.value?.timestamp))
const sosFullscreenLocation = computed(() => sosFullscreenMeta.value?.locationText || locationText.value || '当前位置待确认')
const sosFullscreenTime = computed(() => formatMessageTime(sosFullscreenMeta.value?.timestamp))

const batteryText = computed(() => `${deviceStatus.value?.batteryLevel ?? 0}%`)
const stepText = computed(() => `${sensorData.value?.stepCount ?? 0}`)
const obstacleText = computed(() => `${sensorData.value?.obstacleDistance ?? 0}m`)
const stationaryText = computed(() => {
  const heartRate = sensorData.value?.heartRate
  if (!heartRate) return '0min'
  return `${Math.round(heartRate / 14)}min`
})

const refreshMessageCaches = () => {
  const app = getApp()
  latestGuardianAlert.value = app?.globalData?.latestGuardianAlert || uni.getStorageSync('latestGuardianAlert') || null
  latestGuardianComfort.value = app?.globalData?.latestGuardianComfort || uni.getStorageSync('latestGuardianComfort') || null
  latestGuardianDestination.value = app?.globalData?.latestGuardianDestination || uni.getStorageSync('latestGuardianDestination') || null
  sosFullscreenMeta.value = uni.getStorageSync('latestUserSosState') || sosFullscreenMeta.value
}

const ensureDeviceReady = async () => {
  userStore.restoreFromStorage()
  elderStore.restoreFromStorage()
  deviceStore.restoreFromStorage()
  if (currentDevice.value?.deviceId) return
  await deviceStore.fetchDeviceList()
  if (deviceStore.deviceList?.length) {
    deviceStore.setCurrentDevice(deviceStore.deviceList[0])
  }
}

const dismissSosFullscreen = () => {
  sosFullscreenVisible.value = false
  sosFullscreenMeta.value = null
  uni.removeStorageSync('latestUserSosState')
}

const loadBlindProfileData = async () => {
  try {
    const res = await getBlindProfile()
    if (res?.data) {
      elderStore.setElderInfo(res.data)
    }
  } catch (error) {
    console.error('加载用户端档案失败', error)
  }
}

const loadPageData = async () => {
  loading.value = true
  try {
    await ensureDeviceReady()
    if (currentDevice.value?.deviceId) {
      await Promise.all([
        deviceStore.fetchDeviceStatus(currentDevice.value.deviceId),
        deviceStore.fetchDeviceLocation(currentDevice.value.deviceId),
        deviceStore.fetchSensorData(currentDevice.value.deviceId),
        alarmStore.fetchAlarmList(),
        loadBlindProfileData()
      ])
    } else {
      await loadBlindProfileData()
    }
  } catch (error) {
    console.error('加载用户端页面失败', error)
  } finally {
    refreshMessageCaches()
    loading.value = false
  }
}

const startAutoRefresh = () => {
  clearInterval(refreshTimer)
  refreshTimer = setInterval(async () => {
    refreshMessageCaches()
    if (!currentDevice.value?.deviceId) return
    try {
      await Promise.all([
        deviceStore.fetchDeviceStatus(currentDevice.value.deviceId),
        deviceStore.fetchDeviceLocation(currentDevice.value.deviceId),
        deviceStore.fetchSensorData(currentDevice.value.deviceId),
        alarmStore.fetchAlarmList()
      ])
    } catch (error) {
      console.error('刷新用户端数据失败', error)
    }
  }, 5000)
}

const stopAutoRefresh = () => {
  clearInterval(refreshTimer)
  refreshTimer = null
}

const playPrompt = (text, options = {}) => {
  if (!text) {
    uni.showToast({ title: '暂无可播报内容', icon: 'none' })
    return
  }
  const app = getApp()
  const handler = app?.globalData?.playCrossingPrompt || getApp.__$playCrossingPrompt
  if (typeof handler === 'function') {
    return handler(text, options)
  }
}

const replayComfort = () => {
  playPrompt(latestGuardianComfort.value?.content || latestGuardianComfort.value?.message)
}

const replayDestination = () => {
  playPrompt(latestGuardianDestination.value?.message || (latestGuardianDestination.value?.destination ? `当前目的地：${latestGuardianDestination.value.destination}` : '暂未收到新的目的地'))
}

const handleEmergencyAssist = async () => {
  if (!currentDevice.value?.deviceId) {
    uni.showToast({ title: '请先绑定设备', icon: 'none' })
    return
  }
  const message = '用户端已主动发起 SOS 求助，请立即查看最新位置并尽快联系。'
  const localSosPrompt = '已发起求助，请不要着急。家属很快就会收到提醒，我也会马上陪你一起处理。'
  try {
    const res = await triggerGuardianSos(currentDevice.value.deviceId, {
      content: message,
      locationText: locationText.value
    })
    latestGuardianAlert.value = {
      type: 'GUARDIAN_ALERT',
      alertType: 'SOS',
      level: 'danger',
      message: res.data?.message || message,
      locationText: res.data?.locationText || locationText.value,
      timestamp: res.data?.sentAt || Date.now()
    }
    const app = getApp()
    if (app) {
      app.globalData = app.globalData || {}
      app.globalData.latestGuardianAlert = latestGuardianAlert.value
      app.globalData.pendingGuardianSosAnnouncement = true
      app.globalData.deferGuardianSosAnnouncement = true
    }
    uni.setStorageSync('latestGuardianAlert', latestGuardianAlert.value)
    uni.setStorageSync('pendingGuardianSosAnnouncement', true)
    uni.setStorageSync('deferGuardianSosAnnouncement', true)
    sosFullscreenMeta.value = {
      locationText: res.data?.locationText || locationText.value,
      timestamp: res.data?.sentAt || Date.now()
    }
    uni.setStorageSync('latestUserSosState', sosFullscreenMeta.value)
    sosFullscreenVisible.value = true
    if (app) {
      app.globalData = app.globalData || {}
      app.globalData.sosAiTrigger = {
        source: 'SOS',
        introText: '别着急，我已经陪着你了。你现在是安全的。请先停在原地，慢慢告诉我你身边的情况，我会一步一步帮你。',
        listenPrompt: '我在听，请慢慢说出你现在遇到的情况。'
      }
    }
    uni.setStorageSync('sosAiTrigger', {
      source: 'SOS',
      introText: '别着急，我已经陪着你了。你现在是安全的。请先停在原地，慢慢告诉我你身边的情况，我会一步一步帮你。',
      listenPrompt: '我在听，请慢慢说出你现在遇到的情况。'
    })
    await playPrompt(localSosPrompt, { waitForCompletion: true })
    alarmStore.fetchAlarmList().catch(() => {})
    uni.navigateTo({ url: '/pages/ai-chat/ai-chat' })
  } catch (error) {
    console.error('发起SOS求助失败', error)
  }
}

const openDeviceManager = () => {
  uni.navigateTo({ url: '/pages/device/device' })
}

const navigateToAlarm = () => {
  uni.switchTab({ url: '/pages/alarm/alarm' })
}

const navigateToCrossingAssist = () => {
  uni.navigateTo({ url: '/pages/crossing/crossing' })
}

function formatMessageTime(value) {
  if (!value) return '暂无'
  if (typeof value === 'number') return formatDateTime(value, 'MM-DD HH:mm')
  if (/\d{4}-\d{2}-\d{2}/.test(value)) return formatRelativeTime(value)
  return value
}

onMounted(async () => {
  await loadPageData()
  startAutoRefresh()
})

onShow(() => {
  refreshMessageCaches()
  sosFullscreenVisible.value = !!sosFullscreenMeta.value
})

onUnmounted(() => {
  stopAutoRefresh()
})
</script>

<style lang="scss" scoped>
.user-terminal-page {
  min-height: 100vh;
  background: linear-gradient(180deg, #eff6ff 0%, #f8fafc 34%, #f3f7fb 100%);
  position: relative;
}

.content {
  height: 100vh;
  box-sizing: border-box;
  padding: 24rpx 24rpx 40rpx;
}

.hero-card {
  padding: 34rpx 30rpx;
  border-radius: 32rpx;
  background: linear-gradient(135deg, #1d4ed8 0%, #2563eb 45%, #38bdf8 100%);
  box-shadow: 0 20rpx 40rpx rgba(37, 99, 235, 0.22);
  color: #ffffff;
  display: flex;
  justify-content: space-between;
  gap: 24rpx;
}

.hero-copy {
  flex: 1;
  display: flex;
  flex-direction: column;
  gap: 12rpx;
}

.hero-kicker {
  font-size: 22rpx;
  letter-spacing: 2rpx;
  opacity: 0.82;
}

.hero-title {
  font-size: 40rpx;
  font-weight: 700;
  line-height: 1.35;
}

.hero-desc {
  font-size: 24rpx;
  line-height: 1.7;
  opacity: 0.92;
}

.hero-pill {
  align-self: flex-start;
  padding: 12rpx 18rpx;
  border-radius: 999rpx;
  background: rgba(255, 255, 255, 0.14);
  font-size: 22rpx;
  font-weight: 700;
}

.hero-pill.safe {
  background: rgba(34, 197, 94, 0.18);
}

.hero-pill.warning {
  background: rgba(245, 158, 11, 0.2);
}

.hero-pill.danger,
.hero-pill.idle {
  background: rgba(239, 68, 68, 0.2);
}

.loading-state,
.empty-card,
.profile-card,
.status-card,
.message-card,
.overview-section,
.support-card {
  margin-top: 24rpx;
  border-radius: 28rpx;
  background: #ffffff;
  box-shadow: 0 16rpx 32rpx rgba(15, 23, 42, 0.06);
}

.loading-state,
.empty-card {
  padding: 48rpx 32rpx;
  text-align: center;
}

.loading-icon,
.empty-icon {
  display: block;
  font-size: 56rpx;
}

.loading-text,
.empty-title {
  display: block;
  margin-top: 16rpx;
  font-size: 30rpx;
  font-weight: 700;
  color: #0f172a;
}

.empty-desc {
  display: block;
  margin-top: 14rpx;
  font-size: 24rpx;
  line-height: 1.7;
  color: #64748b;
}

.empty-btn {
  margin: 24rpx auto 0;
  width: 280rpx;
  padding: 22rpx 0;
  border-radius: 999rpx;
  background: linear-gradient(135deg, #2563eb 0%, #1d4ed8 100%);
  color: #ffffff;
  font-size: 26rpx;
  font-weight: 700;
}

.profile-card {
  padding: 28rpx 30rpx;
  display: flex;
  justify-content: space-between;
  gap: 20rpx;
  background: linear-gradient(135deg, #f8fafc 0%, #ffffff 100%);
}

.profile-kicker,
.section-kicker,
.message-tag,
.profile-side-label {
  display: block;
  font-size: 22rpx;
  color: #64748b;
}

.profile-name,
.section-title,
.message-title {
  display: block;
  margin-top: 10rpx;
  font-size: 30rpx;
  font-weight: 700;
  color: #0f172a;
}

.profile-desc,
.status-summary,
.message-content,
.message-foot,
.support-desc {
  display: block;
  margin-top: 10rpx;
  font-size: 24rpx;
  line-height: 1.7;
  color: #475569;
}

.profile-side {
  min-width: 180rpx;
  padding: 20rpx;
  border-radius: 22rpx;
  background: #eff6ff;
}

.profile-side-value {
  display: block;
  margin-top: 10rpx;
  font-size: 24rpx;
  font-weight: 700;
  color: #1d4ed8;
}

.status-card,
.overview-section {
  padding: 28rpx 30rpx;
}

.status-header,
.section-header,
.message-top,
.support-card {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 20rpx;
}

.status-pill {
  padding: 10rpx 18rpx;
  border-radius: 999rpx;
  font-size: 22rpx;
  font-weight: 700;
}

.status-pill.safe {
  background: rgba(34, 197, 94, 0.14);
  color: #15803d;
}

.status-pill.warning {
  background: rgba(245, 158, 11, 0.16);
  color: #b45309;
}

.status-pill.danger,
.status-pill.idle {
  background: rgba(239, 68, 68, 0.12);
  color: #dc2626;
}

.status-grid,
.overview-grid {
  display: grid;
  gap: 16rpx;
  margin-top: 22rpx;
}

.status-grid {
  grid-template-columns: repeat(3, 1fr);
}

.overview-grid {
  grid-template-columns: repeat(2, 1fr);
}

.status-metric,
.overview-item {
  padding: 20rpx;
  border-radius: 22rpx;
  background: #f8fafc;
}

.status-metric.compact {
  background: #eef6ff;
}

.status-label,
.overview-label {
  display: block;
  font-size: 22rpx;
  color: #64748b;
}

.status-value,
.overview-value {
  display: block;
  margin-top: 10rpx;
  font-size: 24rpx;
  font-weight: 700;
  color: #0f172a;
  line-height: 1.6;
  word-break: break-all;
}

.action-row {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 16rpx;
  margin-top: 24rpx;
}

.action-card {
  padding: 24rpx 20rpx;
  border-radius: 26rpx;
  color: #ffffff;
  box-shadow: 0 14rpx 28rpx rgba(37, 99, 235, 0.12);
}

.action-card.primary {
  background: linear-gradient(135deg, #2563eb 0%, #1d4ed8 100%);
}

.action-card.secondary {
  background: linear-gradient(135deg, #0f766e 0%, #0ea5a4 100%);
}

.action-card.danger {
  background: linear-gradient(135deg, #dc2626 0%, #ef4444 100%);
}

.action-icon {
  display: block;
  font-size: 34rpx;
}

.action-title {
  display: block;
  margin-top: 14rpx;
  font-size: 26rpx;
  font-weight: 700;
}

.action-desc {
  display: block;
  margin-top: 8rpx;
  font-size: 22rpx;
  line-height: 1.6;
  opacity: 0.9;
}

.message-section {
  margin-top: 24rpx;
  display: grid;
  gap: 18rpx;
}

.message-card {
  padding: 26rpx 28rpx;
}

.message-card.destination {
  background: linear-gradient(135deg, #eef6ff 0%, #ffffff 100%);
}

.message-card.comfort {
  background: linear-gradient(135deg, #f5f3ff 0%, #ffffff 100%);
}

.message-card.alert {
  background: linear-gradient(135deg, #fff7ed 0%, #ffffff 100%);
}

.message-time,
.section-link,
.support-arrow {
  font-size: 22rpx;
  color: #64748b;
}

.support-card {
  padding: 28rpx 30rpx;
}

.support-title {
  display: block;
  font-size: 28rpx;
  font-weight: 700;
  color: #0f172a;
}

.support-arrow {
  font-size: 42rpx;
  line-height: 1;
}

.sos-fullscreen-mask {
  position: fixed;
  inset: 0;
  z-index: 999;
  padding: 40rpx 28rpx;
  background: rgba(127, 29, 29, 0.88);
  backdrop-filter: blur(10rpx);
  display: flex;
  align-items: center;
  justify-content: center;
}

.sos-fullscreen-card {
  width: 100%;
  padding: 42rpx 34rpx;
  border-radius: 36rpx;
  background: linear-gradient(160deg, #7f1d1d 0%, #dc2626 56%, #ef4444 100%);
  box-shadow: 0 24rpx 46rpx rgba(0, 0, 0, 0.24);
  color: #ffffff;
}

.sos-fullscreen-kicker {
  display: block;
  font-size: 24rpx;
  letter-spacing: 2rpx;
  opacity: 0.82;
}

.sos-fullscreen-title {
  display: block;
  margin-top: 18rpx;
  font-size: 52rpx;
  font-weight: 700;
}

.sos-fullscreen-desc {
  display: block;
  margin-top: 18rpx;
  font-size: 28rpx;
  line-height: 1.8;
  color: rgba(255, 255, 255, 0.92);
}

.sos-fullscreen-panel {
  display: grid;
  grid-template-columns: 1.4fr 1fr;
  gap: 16rpx;
  margin-top: 28rpx;
}

.sos-panel-item {
  padding: 22rpx 20rpx;
  border-radius: 24rpx;
  background: rgba(255, 255, 255, 0.12);

  &.compact {
    text-align: center;
  }
}

.sos-panel-label {
  display: block;
  font-size: 22rpx;
  color: rgba(255, 255, 255, 0.78);
}

.sos-panel-value {
  display: block;
  margin-top: 12rpx;
  font-size: 28rpx;
  font-weight: 700;
  line-height: 1.6;
  color: #ffffff;
  word-break: break-all;
}

.sos-fullscreen-actions {
  display: flex;
  gap: 16rpx;
  margin-top: 30rpx;
}

.sos-fullscreen-btn {
  flex: 1;
  padding: 24rpx 0;
  border-radius: 999rpx;
  text-align: center;
  font-size: 28rpx;
  font-weight: 700;

  &.light {
    background: #ffffff;
    color: #dc2626;
  }

  &.dark {
    background: rgba(127, 29, 29, 0.32);
    color: #ffffff;
    border: 1rpx solid rgba(255, 255, 255, 0.14);
  }
}
</style>
