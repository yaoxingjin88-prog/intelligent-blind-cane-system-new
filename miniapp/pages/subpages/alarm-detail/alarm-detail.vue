<template>
  <view class="alarm-detail-page">
    <!-- 顶部地图区域 -->
    <view class="map-section">
      <map
        id="alarm-map"
        class="location-map"
        :latitude="mapLatitude"
        :longitude="mapLongitude"
        :markers="mapMarkers"
        :scale="16"
        :show-location="true"
      />
    </view>

    <!-- 详情信息卡片 -->
    <view class="detail-card">
      <!-- 头部信息 -->
      <view class="card-header">
        <view class="header-left">
          <view class="icon-circle" :class="alarmLevel">
            <text class="icon-text">{{ getAlarmIcon(alarmData?.alarmType) }}</text>
          </view>
          <view class="title-section">
            <text class="alarm-title">{{ getAlarmTitle(alarmData?.alarmType) }}</text>
            <text class="alarm-time">🕐 {{ formatTime(alarmData?.alarmTime) }}</text>
          </view>
        </view>
        <view class="status-badge" :class="alarmLevel">
          <text>待处理</text>
        </view>
      </view>

      <!-- 报警内容 -->
      <view class="content-section">
        <text class="section-label">报警内容</text>
        <view class="content-box">
          <text class="content-text">{{ alarmData?.description || getAlarmDesc(alarmData?.alarmType) }}</text>
        </view>
      </view>

      <!-- 跌倒报警分析 -->
      <view class="analysis-section" v-if="isFallAlarm">
        <text class="section-label">系统初步分析</text>
        <view class="analysis-box">
          <text class="analysis-icon">ℹ️</text>
          <text class="analysis-text">设备检测到异常的加速度突变（可能是撞击），并且随后长时间处于倾倒静止状态。请尽快联系老人确认安全。</text>
        </view>
      </view>

      <!-- 设备信息 -->
      <view class="info-section" v-if="alarmData?.deviceId">
        <text class="section-label">设备信息</text>
        <view class="info-row">
          <text class="info-label">设备ID</text>
          <text class="info-value">{{ alarmData.deviceId }}</text>
        </view>
      </view>
    </view>

    <!-- 底部操作栏 -->
    <view class="action-bar">
      <button class="action-btn call-btn" @click="handleCall">
        <text class="btn-icon">📞</text>
        <text>呼叫</text>
      </button>
      <button class="action-btn handle-btn" @click="handleMarkDone">
        标记已处理
      </button>
    </view>
  </view>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { handleAlarm as handleAlarmApi } from '@/api/alarm'
import { formatRelativeTime } from '@/utils'

const alarmData = ref(null)
const alarmId = ref('')

// 报警类型映射
const alarmTypeMap = {
  '摔倒': { title: '跌倒报警', icon: '🚨', desc: '设备检测到老人可能发生跌倒，请立即确认！' },
  '跌倒': { title: '跌倒报警', icon: '🚨', desc: '设备检测到老人可能发生跌倒，请立即确认！' },
  '障碍物': { title: '障碍物报警', icon: '⚠️', desc: '检测到前方障碍物距离过近。' },
  '电池': { title: '低电量报警', icon: '🔋', desc: '当前设备电量过低，请提醒充电。' },
  '低电量': { title: '低电量报警', icon: '🔋', desc: '当前设备电量过低，请提醒充电。' },
  '电子围栏越界报警': { title: '越界提醒', icon: '📍', desc: '设备已离开电子围栏区域。' },
  '越界': { title: '越界提醒', icon: '📍', desc: '设备已离开电子围栏区域。' },
  '静止': { title: '长时间静止', icon: '⏰', desc: '设备已连续静止超过设定时间。' },
  'fall': { title: '跌倒报警', icon: '🚨', desc: '设备检测到老人可能发生跌倒，请立即确认！' },
  'out_of_bounds': { title: '越界提醒', icon: '📍', desc: '设备已离开电子围栏区域。' },
  'low_battery': { title: '低电量报警', icon: '🔋', desc: '当前设备电量过低，请提醒充电。' },
  'stationary': { title: '长时间静止', icon: '⏰', desc: '设备已连续静止超过设定时间。' },
  'obstacle': { title: '障碍物报警', icon: '⚠️', desc: '检测到前方障碍物距离过近。' },
  'sos': { title: 'SOS求助', icon: '🔔', desc: '老人触发SOS紧急求助。' }
}

// 报警等级
const alarmLevel = computed(() => {
  const type = alarmData.value?.alarmType
  const highLevelTypes = ['fall', 'sos', '摔倒', '跌倒']
  const mediumLevelTypes = ['out_of_bounds', 'obstacle', '障碍物', '越界', '电子围栏越界报警']
  
  if (highLevelTypes.includes(type)) return 'high'
  if (mediumLevelTypes.includes(type)) return 'medium'
  return 'low'
})

// 是否跌倒报警
const isFallAlarm = computed(() => {
  const type = alarmData.value?.alarmType
  return ['fall', '摔倒', '跌倒'].includes(type)
})

// 地图位置（有报警坐标则使用，否则用默认位置）
const mapLatitude = computed(() => alarmData.value?.latitude || 39.9042)
const mapLongitude = computed(() => alarmData.value?.longitude || 116.4074)

// 地图标记
const mapMarkers = computed(() => [{
  id: 1,
  latitude: mapLatitude.value,
  longitude: mapLongitude.value,
  width: 32,
  height: 32,
  callout: {
    content: getAlarmTitle(alarmData.value?.alarmType),
    color: '#ffffff',
    fontSize: 12,
    borderRadius: 8,
    bgColor: alarmLevel.value === 'high' ? '#ee0a24' : (alarmLevel.value === 'medium' ? '#ff976a' : '#9ca3af'),
    padding: 8,
    display: 'ALWAYS',
    textAlign: 'center'
  }
}])

// 获取报警图标
const getAlarmIcon = (type) => {
  return alarmTypeMap[type]?.icon || '📢'
}

// 获取报警标题
const getAlarmTitle = (type) => {
  return alarmTypeMap[type]?.title || type || '未知报警'
}

// 获取报警描述
const getAlarmDesc = (type) => {
  return alarmTypeMap[type]?.desc || `报警类型: ${type || '未知'}`
}

// 格式化时间
const formatTime = (time) => {
  if (!time) return ''
  return formatRelativeTime(time)
}

// 呼叫设备
const handleCall = () => {
  uni.showToast({
    title: '正在呼叫设备...',
    icon: 'none'
  })
}

// 标记已处理
const handleMarkDone = async () => {
  try {
    await handleAlarmApi(alarmId.value, { status: '1' })
    
    uni.showToast({
      title: '已标记为处理完成',
      icon: 'success'
    })
    
    setTimeout(() => {
      uni.navigateBack()
    }, 1500)
  } catch (error) {
    uni.showToast({
      title: '操作失败',
      icon: 'none'
    })
  }
}

onMounted(() => {
  const pages = getCurrentPages()
  const currentPage = pages[pages.length - 1]
  alarmId.value = currentPage.options?.id || ''
  
  const cachedAlarm = uni.getStorageSync('currentAlarm')
  if (cachedAlarm) {
    try {
      alarmData.value = JSON.parse(cachedAlarm)
    } catch (e) {
      console.error('解析报警数据失败', e)
    }
  }
})
</script>

<style lang="scss" scoped>
.alarm-detail-page {
  min-height: 100vh;
  background: #f7f8fa;
  display: flex;
  flex-direction: column;
}

.map-section {
  height: 512rpx;
  position: relative;
  border-bottom: 1rpx solid #e5e7eb;
}

.location-map {
  width: 100%;
  height: 100%;
}

.map-placeholder {
  width: 100%;
  height: 100%;
  background: linear-gradient(135deg, #e0f2fe 0%, #bae6fd 100%);
  background-image: radial-gradient(#93c5fd 1px, transparent 1px);
  background-size: 40rpx 40rpx;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  position: relative;
}

.marker-container {
  position: relative;
  margin-bottom: 48rpx;
}

.marker-ping {
  position: absolute;
  top: -32rpx;
  left: -32rpx;
  width: 128rpx;
  height: 128rpx;
  border-radius: 50%;
  animation: ping 1.5s cubic-bezier(0, 0, 0.2, 1) infinite;

  &.high {
    background: rgba(238, 10, 36, 0.3);
  }

  &.medium {
    background: rgba(255, 151, 106, 0.3);
  }

  &.low {
    background: rgba(156, 163, 175, 0.3);
  }
}

.marker-dot {
  width: 64rpx;
  height: 64rpx;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  position: relative;
  z-index: 10;
  border: 4rpx solid #ffffff;
  box-shadow: 0 8rpx 16rpx rgba(0, 0, 0, 0.15);

  &.high {
    background: #ee0a24;
  }

  &.medium {
    background: #ff976a;
  }

  &.low {
    background: #9ca3af;
  }
}

.marker-icon {
  font-size: 32rpx;
}

.location-bubble {
  position: absolute;
  bottom: 32rpx;
  left: 50%;
  transform: translateX(-50%);
  background: rgba(255, 255, 255, 0.95);
  backdrop-filter: blur(8rpx);
  padding: 16rpx 32rpx;
  border-radius: 48rpx;
  display: flex;
  align-items: center;
  gap: 12rpx;
  box-shadow: 0 4rpx 12rpx rgba(0, 0, 0, 0.08);
}

.bubble-icon {
  font-size: 28rpx;

  &.high {
    color: #ee0a24;
  }

  &.medium {
    color: #07c160;
  }
}

.bubble-text {
  font-size: 24rpx;
  font-weight: 600;
  color: #1f2937;
  white-space: nowrap;
}

.detail-card {
  background: #ffffff;
  border-radius: 24rpx;
  padding: 40rpx;
  margin: -48rpx 32rpx 32rpx;
  position: relative;
  z-index: 10;
  box-shadow: 0 4rpx 12rpx rgba(0, 0, 0, 0.06);
  border: 1rpx solid #f3f4f6;
  box-sizing: border-box;
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  padding-bottom: 32rpx;
  border-bottom: 1rpx solid #f3f4f6;
  margin-bottom: 32rpx;
}

.header-left {
  display: flex;
  align-items: center;
  gap: 24rpx;
  flex: 1;
}

.icon-circle {
  width: 96rpx;
  height: 96rpx;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;

  &.high {
    background: #fef2f2;
  }

  &.medium {
    background: #fff7ed;
  }

  &.low {
    background: #f9fafb;
  }
}

.icon-text {
  font-size: 40rpx;
}

.title-section {
  display: flex;
  flex-direction: column;
  gap: 8rpx;
}

.alarm-title {
  font-size: 36rpx;
  font-weight: 600;
  color: #1f2937;
}

.alarm-time {
  font-size: 24rpx;
  color: #9ca3af;
}

.status-badge {
  padding: 8rpx 16rpx;
  border-radius: 8rpx;
  font-size: 20rpx;
  font-weight: 600;
  flex-shrink: 0;

  &.high {
    background: #fef2f2;
    color: #ee0a24;
    border: 1rpx solid #fecaca;
  }

  &.medium {
    background: #fff7ed;
    color: #ff976a;
    border: 1rpx solid #fed7aa;
  }

  &.low {
    background: #f9fafb;
    color: #9ca3af;
    border: 1rpx solid #e5e7eb;
  }
}

.content-section,
.analysis-section,
.info-section {
  margin-bottom: 32rpx;

  &:last-child {
    margin-bottom: 0;
  }
}

.section-label {
  font-size: 24rpx;
  color: #9ca3af;
  display: block;
  margin-bottom: 16rpx;
}

.content-box {
  background: #f9fafb;
  padding: 24rpx;
  border-radius: 16rpx;
}

.content-text {
  font-size: 28rpx;
  color: #374151;
  line-height: 1.6;
}

.analysis-box {
  display: flex;
  align-items: flex-start;
  gap: 16rpx;
  background: #eff6ff;
  padding: 24rpx;
  border-radius: 16rpx;
}

.analysis-icon {
  font-size: 32rpx;
  flex-shrink: 0;
  margin-top: 4rpx;
}

.analysis-text {
  font-size: 26rpx;
  color: #374151;
  line-height: 1.6;
}

.info-row {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 24rpx 0;
  border-bottom: 1rpx solid #f3f4f6;

  &:last-child {
    border-bottom: none;
  }
}

.info-label {
  font-size: 28rpx;
  color: #6b7280;
}

.info-value {
  font-size: 28rpx;
  color: #1f2937;
  font-weight: 500;
}

.action-bar {
  position: fixed;
  bottom: 0;
  left: 0;
  right: 0;
  background: #ffffff;
  border-top: 1rpx solid #f3f4f6;
  padding: 32rpx;
  padding-bottom: calc(32rpx + env(safe-area-inset-bottom));
  display: flex;
  gap: 24rpx;
  box-shadow: 0 -16rpx 32rpx rgba(0, 0, 0, 0.04);
  z-index: 100;
}

.action-btn {
  height: 88rpx;
  border-radius: 24rpx;
  font-size: 30rpx;
  font-weight: 600;
  border: none;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 12rpx;
  line-height: 88rpx;

  &.call-btn {
    flex: 0.4;
    background: #ffffff;
    color: #374151;
    border: 2rpx solid #e5e7eb;
  }

  &.handle-btn {
    flex: 0.6;
    background: #07c160;
    color: #ffffff;
    box-shadow: 0 8rpx 24rpx rgba(7, 193, 96, 0.25);
  }
}

.btn-icon {
  font-size: 32rpx;
}

@keyframes ping {
  0% {
    transform: scale(0.5);
    opacity: 1;
  }
  75%, 100% {
    transform: scale(1);
    opacity: 0;
  }
}
</style>
