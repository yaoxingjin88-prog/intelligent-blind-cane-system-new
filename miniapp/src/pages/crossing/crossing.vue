<template>
  <view class="crossing-page">
    <scroll-view class="content" scroll-y>
      <view class="hero-card">
        <view class="hero-glow hero-glow-left"></view>
        <view class="hero-glow hero-glow-right"></view>
        <view class="hero-top">
          <view class="hero-copy">
            <text class="hero-kicker">Crossing Assist</text>
            <text class="hero-title">路口安全通行辅助</text>
            <text class="hero-subtitle">接收视觉模块识别结果，进行语音与震动提醒</text>
          </view>
          <view class="status-tag" :class="statusClass">
            <text>{{ recommendationLabel }}</text>
          </view>
        </view>
        <view class="hero-message">
          <text class="hero-message-label">当前播报建议</text>
          <text class="hero-message-text">{{ snapshot.message || '暂未收到识别结果' }}</text>
        </view>
        <view class="hero-summary">
          <view class="hero-summary-item">
            <text class="hero-summary-label">设备</text>
            <text class="hero-summary-value">{{ currentDeviceName }}</text>
          </view>
          <view class="hero-summary-item">
            <text class="hero-summary-label">更新时间</text>
            <text class="hero-summary-value">{{ snapshot.updateTime || '--' }}</text>
          </view>
          <view class="hero-summary-item">
            <text class="hero-summary-label">数据来源</text>
            <text class="hero-summary-value">{{ snapshot.source || 'vision-module' }}</text>
          </view>
        </view>
        <view class="hero-note">
          <text>{{ recommendationDetail }}</text>
        </view>
      </view>

      <view class="panel-grid">
        <view class="panel-item" :class="trafficLightClass">
          <text class="panel-icon">🚦</text>
          <text class="panel-label">红绿灯</text>
          <text class="panel-value">{{ trafficLightLabel }}</text>
          <text class="panel-meta">{{ (snapshot.trafficLightStatus || '').toUpperCase() === 'UNKNOWN' ? '等待视觉识别信号' : '根据当前识别结果实时更新' }}</text>
        </view>
        <view class="panel-item" :class="zebraClass">
          <text class="panel-icon">🛣️</text>
          <text class="panel-label">斑马线</text>
          <text class="panel-value">{{ snapshot.zebraCrossingDetected ? '已检测' : '未检测到' }}</text>
          <text class="panel-meta">{{ snapshot.zebraCrossingDetected ? '已锁定可参考通行区域' : '建议继续缓慢调整方向' }}</text>
        </view>
        <view class="panel-item" :class="directionClass">
          <text class="panel-icon">🧭</text>
          <text class="panel-label">方向提示</text>
          <text class="panel-value">{{ directionLabel }}</text>
          <text class="panel-meta">{{ (snapshot.zebraCrossingDirection || '').toUpperCase() === 'CENTER' ? '当前方向较理想，保持更稳妥' : '按提示微调后再继续判断' }}</text>
        </view>
        <view class="panel-item" :class="vehicleClass">
          <text class="panel-icon">🚗</text>
          <text class="panel-label">车辆粗提醒</text>
          <text class="panel-value">{{ snapshot.vehicleApproaching ? '有车接近' : '未见接近车辆' }}</text>
          <text class="panel-meta">{{ snapshot.vehicleApproaching ? '优先等待车辆通过更安全' : '周边车流暂时相对平稳' }}</text>
        </view>
      </view>

      <view class="confidence-card">
        <view class="section-head">
          <view>
            <text class="section-eyebrow">识别稳定度</text>
            <text class="section-title">当前识别置信度</text>
          </view>
          <view class="confidence-badge">{{ confidencePercent }}</view>
        </view>
        <view class="confidence-bar">
          <view class="confidence-fill" :style="{ width: confidencePercent }"></view>
        </view>
        <view class="confidence-footer">
          <view class="confidence-chip">
            <text class="confidence-chip-label">来源</text>
            <text class="confidence-chip-value">{{ snapshot.source || 'vision-module' }}</text>
          </view>
          <view class="confidence-chip soft">
            <text class="confidence-chip-label">更新时间</text>
            <text class="confidence-chip-value">{{ snapshot.updateTime || '--' }}</text>
          </view>
        </view>
      </view>

      <view class="action-card">
        <view class="section-head">
          <view>
            <text class="section-eyebrow">快捷操作</text>
            <text class="section-title">一键演示与语音播报</text>
          </view>
        </view>
        <view class="action-grid">
          <view class="action-btn primary action-btn-wide" @click="playPrompt">
            <text class="action-emoji">🔊</text>
            <view class="action-copy">
              <text class="action-title">播报提示</text>
              <text class="action-desc">复用全局 TTS，播报当前通行建议</text>
            </view>
          </view>
          <view class="action-btn secondary" @click="refreshSnapshot">
            <text class="action-emoji">🔄</text>
            <view class="action-copy">
              <text class="action-title">刷新结果</text>
              <text class="action-desc">获取最新识别结果</text>
            </view>
          </view>
          <view class="action-btn accent" @click="mockSnapshot">
            <text class="action-emoji">✨</text>
            <view class="action-copy">
              <text class="action-title">生成演示结果</text>
              <text class="action-desc">切换演示场景更直观</text>
            </view>
          </view>
        </view>
      </view>

      <view class="tips-card">
        <view class="section-head">
          <view>
            <text class="section-eyebrow">演示建议</text>
            <text class="section-title">更适合现场展示的顺序</text>
          </view>
        </view>
        <view class="tips-list">
          <view class="tips-item">
            <text class="tips-index">01</text>
            <text class="tips-text">先点击“生成演示结果”，展示红绿灯、方向与车辆接近状态的变化。</text>
          </view>
          <view class="tips-item">
            <text class="tips-index">02</text>
            <text class="tips-text">再点击“播报提示”，说明系统会把视觉识别结果转成语音与震动提醒。</text>
          </view>
          <view class="tips-item">
            <text class="tips-index">03</text>
            <text class="tips-text">最后点击“刷新结果”，展示 mock 场景轮换或实时识别结果同步更新。</text>
          </view>
        </view>
      </view>
    </scroll-view>
  </view>
</template>

<script setup>
import { computed, ref, onMounted, onUnmounted } from 'vue'
import { onShow } from '@dcloudio/uni-app'
import { useDeviceStore } from '@/store'
import { getCrossingAssist, mockCrossingAssist } from '@/api/crossing'

const deviceStore = useDeviceStore()
const snapshot = ref({})
const handleCrossingAssistUpdate = (latest) => {
  if (!latest) {
    return
  }
  if (!currentDevice.value?.deviceId || latest.deviceId === currentDevice.value.deviceId) {
    snapshot.value = latest
  }
}

const currentDevice = computed(() => deviceStore.currentDevice)
const currentDeviceName = computed(() => currentDevice.value?.deviceName || '智能盲杖')
const recommendationDetail = computed(() => {
  switch (snapshot.value.recommendation) {
    case 'WAIT':
      return '当前建议以等待与确认环境为主，系统会优先保障通行安全。'
    case 'PROCEED_CAUTION':
      return '当前环境具备谨慎通行条件，请继续结合语音提示和周边车流判断。'
    case 'ALIGN_FIRST':
      return '当前重点是先校准斑马线方向，再继续判断是否适合通过。'
    case 'SEARCH_ZEBRA':
      return '系统尚未锁定理想通行区域，建议继续缓慢搜索斑马线。'
    default:
      return '等待新的识别结果后，系统会给出更明确的通行建议。'
  }
})
const recommendationLabel = computed(() => {
  switch (snapshot.value.recommendation) {
    case 'WAIT':
      return '建议等待'
    case 'PROCEED_CAUTION':
      return '谨慎通行'
    case 'ALIGN_FIRST':
      return '先校准方向'
    case 'SEARCH_ZEBRA':
      return '寻找斑马线'
    default:
      return '等待识别'
  }
})
const statusClass = computed(() => {
  switch (snapshot.value.recommendation) {
    case 'WAIT':
      return 'danger'
    case 'PROCEED_CAUTION':
      return 'success'
    case 'ALIGN_FIRST':
      return 'warning'
    default:
      return 'neutral'
  }
})
const trafficLightClass = computed(() => {
  switch ((snapshot.value.trafficLightStatus || '').toUpperCase()) {
    case 'RED':
      return 'tone-danger'
    case 'GREEN':
      return 'tone-success'
    case 'YELLOW':
      return 'tone-warning'
    default:
      return 'tone-neutral'
  }
})
const zebraClass = computed(() => snapshot.value.zebraCrossingDetected ? 'tone-success' : 'tone-warning')
const trafficLightLabel = computed(() => {
  switch ((snapshot.value.trafficLightStatus || '').toUpperCase()) {
    case 'RED':
      return '红灯'
    case 'GREEN':
      return '绿灯'
    case 'YELLOW':
      return '黄灯'
    default:
      return '未知'
  }
})
const directionClass = computed(() => {
  switch ((snapshot.value.zebraCrossingDirection || '').toUpperCase()) {
    case 'CENTER':
      return 'tone-success'
    case 'LEFT':
    case 'RIGHT':
      return 'tone-warning'
    default:
      return 'tone-neutral'
  }
})
const directionLabel = computed(() => {
  switch ((snapshot.value.zebraCrossingDirection || '').toUpperCase()) {
    case 'LEFT':
      return '偏左'
    case 'RIGHT':
      return '偏右'
    case 'CENTER':
      return '居中'
    default:
      return '未知'
  }
})
const vehicleClass = computed(() => snapshot.value.vehicleApproaching ? 'tone-danger' : 'tone-success')
const confidencePercent = computed(() => `${Math.round((snapshot.value.confidence || 0) * 100)}%`)

const loadSnapshot = async () => {
  if (!currentDevice.value?.deviceId) {
    snapshot.value = {
      message: '请先绑定设备后再使用路口辅助功能'
    }
    return
  }
  try {
    const res = await getCrossingAssist(currentDevice.value.deviceId)
    snapshot.value = res.data || {}
  } catch (error) {
    console.error('获取路口辅助结果失败', error)
  }
}

const refreshSnapshot = async () => {
  if (!currentDevice.value?.deviceId) {
    uni.showToast({ title: '请先绑定设备', icon: 'none' })
    return
  }
  try {
    if (snapshot.value.source === 'mock-demo') {
      const res = await mockCrossingAssist(currentDevice.value.deviceId)
      snapshot.value = res.data || {}
      uni.showToast({ title: '已切换演示场景', icon: 'success' })
      return
    }
    await loadSnapshot()
    uni.showToast({ title: '已刷新', icon: 'success' })
  } catch (error) {
    console.error('刷新路口辅助结果失败', error)
    uni.showToast({ title: '刷新失败', icon: 'none' })
  }
}

const mockSnapshot = async () => {
  if (!currentDevice.value?.deviceId) {
    uni.showToast({ title: '请先绑定设备', icon: 'none' })
    return
  }
  try {
    const res = await mockCrossingAssist(currentDevice.value.deviceId)
    snapshot.value = res.data || {}
    uni.showToast({ title: '已生成演示结果', icon: 'success' })
  } catch (error) {
    console.error('生成演示结果失败', error)
  }
}

const playPrompt = async () => {
  const text = snapshot.value.message
  if (!text) {
    uni.showToast({ title: '暂无可播报内容', icon: 'none' })
    return
  }
  try {
    const app = getApp()
    const globalPlay = getApp.__$playCrossingPrompt || app?.globalData?.playCrossingPrompt
    if (typeof globalPlay === 'function') {
      await globalPlay(text)
    } else {
      uni.showToast({ title: '播报服务未就绪', icon: 'none' })
    }
    try {
      if (snapshot.value.recommendation === 'WAIT') {
        uni.vibrateLong({ fail: () => {} })
      } else {
        uni.vibrateShort({ fail: () => {} })
      }
    } catch (e) {}
  } catch (error) {
    console.error('播报失败', error)
    uni.showToast({ title: '播报失败', icon: 'none' })
  }
}

onMounted(async () => {
  deviceStore.restoreFromStorage()
  uni.$on('crossingAssistUpdate', handleCrossingAssistUpdate)
  const cached = uni.getStorageSync('latestCrossingAssist')
  if (cached && (!currentDevice.value?.deviceId || cached.deviceId === currentDevice.value.deviceId)) {
    snapshot.value = cached
  }
  if (!deviceStore.currentDevice?.deviceId) {
    try {
      await deviceStore.fetchDeviceList()
      if (deviceStore.deviceList.length > 0) {
        deviceStore.setCurrentDevice(deviceStore.deviceList[0])
      }
    } catch (error) {
      console.error('加载设备失败', error)
    }
  }
  await loadSnapshot()
})

onUnmounted(() => {
  uni.$off('crossingAssistUpdate', handleCrossingAssistUpdate)
})

onShow(() => {
  const app = getApp()
  if (app?.globalData?.latestCrossingAssist) {
    const latest = app.globalData.latestCrossingAssist
    if (!currentDevice.value?.deviceId || latest.deviceId === currentDevice.value.deviceId) {
      snapshot.value = latest
    }
  }
})
</script>

<style lang="scss" scoped>
.crossing-page {
  min-height: 100vh;
  background: linear-gradient(180deg, #eef4ff 0%, #f8fafc 32%, #f3f6fb 100%);
}

.content {
  height: 100%;
  padding: 24rpx 24rpx 48rpx;
  box-sizing: border-box;
}

.hero-card,
.confidence-card,
.action-card,
.tips-card {
  position: relative;
  overflow: hidden;
  background: #ffffff;
  border-radius: 28rpx;
  padding: 30rpx;
  box-shadow: 0 18rpx 36rpx rgba(15, 23, 42, 0.07);
  margin-bottom: 24rpx;
}

.hero-card {
  background: linear-gradient(135deg, #081226 0%, #1d4ed8 52%, #14b8a6 100%);
  color: #ffffff;
}

.hero-glow {
  position: absolute;
  width: 220rpx;
  height: 220rpx;
  border-radius: 50%;
  background: rgba(255, 255, 255, 0.12);
}

.hero-glow-left {
  top: -90rpx;
  left: -70rpx;
}

.hero-glow-right {
  right: -60rpx;
  bottom: -90rpx;
}

.hero-top {
  position: relative;
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  gap: 20rpx;
}

.hero-copy {
  flex: 1;
}

.hero-kicker {
  display: inline-flex;
  padding: 8rpx 18rpx;
  border-radius: 999rpx;
  background: rgba(255, 255, 255, 0.14);
  font-size: 20rpx;
  letter-spacing: 2rpx;
  text-transform: uppercase;
}

.hero-title {
  display: block;
  margin-top: 18rpx;
  font-size: 38rpx;
  font-weight: 700;
  line-height: 1.3;
}

.hero-subtitle {
  display: block;
  margin-top: 12rpx;
  font-size: 24rpx;
  color: rgba(255, 255, 255, 0.82);
}

.status-tag {
  flex-shrink: 0;
  padding: 12rpx 18rpx;
  border-radius: 999rpx;
  font-size: 22rpx;
  font-weight: 700;
  background: rgba(255, 255, 255, 0.18);
}

.status-tag.success {
  background: rgba(34, 197, 94, 0.25);
}

.status-tag.warning {
  background: rgba(251, 191, 36, 0.22);
}

.status-tag.danger {
  background: rgba(239, 68, 68, 0.24);
}

.hero-message {
  position: relative;
  margin-top: 28rpx;
  padding: 24rpx;
  border-radius: 20rpx;
  background: rgba(255, 255, 255, 0.12);
  border: 1rpx solid rgba(255, 255, 255, 0.1);
}

.hero-message-label {
  display: block;
  font-size: 22rpx;
  color: rgba(255, 255, 255, 0.74);
}

.hero-message-text {
  display: block;
  margin-top: 12rpx;
  font-size: 28rpx;
  line-height: 1.6;
}

.hero-summary {
  position: relative;
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 16rpx;
  margin-top: 24rpx;
}

.hero-summary-item {
  padding: 20rpx 18rpx;
  border-radius: 20rpx;
  background: rgba(255, 255, 255, 0.1);
}

.hero-summary-label {
  display: block;
  font-size: 20rpx;
  color: rgba(255, 255, 255, 0.72);
}

.hero-summary-value {
  display: block;
  margin-top: 10rpx;
  font-size: 24rpx;
  font-weight: 700;
  line-height: 1.5;
  word-break: break-all;
}

.hero-note {
  position: relative;
  margin-top: 20rpx;
  padding-top: 20rpx;
  border-top: 1rpx solid rgba(255, 255, 255, 0.14);
  font-size: 22rpx;
  line-height: 1.7;
  color: rgba(255, 255, 255, 0.82);
}

.panel-grid {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: 20rpx;
  margin-bottom: 24rpx;
}

.panel-item {
  border-radius: 24rpx;
  padding: 26rpx;
  box-shadow: 0 10rpx 24rpx rgba(15, 23, 42, 0.05);
  border: 2rpx solid #e2e8f0;
  background: linear-gradient(180deg, #ffffff 0%, #f8fafc 100%);
}

.panel-item.tone-success {
  border-color: rgba(34, 197, 94, 0.2);
  background: linear-gradient(180deg, #ffffff 0%, #f0fdf4 100%);
}

.panel-item.tone-warning {
  border-color: rgba(251, 191, 36, 0.26);
  background: linear-gradient(180deg, #ffffff 0%, #fffbeb 100%);
}

.panel-item.tone-danger {
  border-color: rgba(239, 68, 68, 0.24);
  background: linear-gradient(180deg, #ffffff 0%, #fef2f2 100%);
}

.panel-item.tone-neutral {
  border-color: rgba(148, 163, 184, 0.22);
  background: linear-gradient(180deg, #ffffff 0%, #f8fafc 100%);
}

.panel-icon {
  display: inline-flex;
  width: 56rpx;
  height: 56rpx;
  align-items: center;
  justify-content: center;
  border-radius: 16rpx;
  background: rgba(255, 255, 255, 0.7);
  font-size: 30rpx;
}

.panel-label {
  display: block;
  margin-top: 20rpx;
  font-size: 22rpx;
  color: #64748b;
}

.panel-value {
  display: block;
  margin-top: 14rpx;
  font-size: 30rpx;
  font-weight: 700;
  color: #0f172a;
}

.panel-meta {
  display: block;
  margin-top: 12rpx;
  font-size: 22rpx;
  line-height: 1.6;
  color: #64748b;
}

.section-head {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 18rpx;
}

.section-eyebrow {
  display: block;
  font-size: 20rpx;
  color: #64748b;
  letter-spacing: 1rpx;
}

.section-title {
  display: block;
  margin-top: 8rpx;
  font-size: 30rpx;
  font-weight: 700;
  color: #0f172a;
}

.confidence-badge {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  min-width: 112rpx;
  height: 64rpx;
  padding: 0 20rpx;
  border-radius: 999rpx;
  background: linear-gradient(90deg, #dbeafe 0%, #ecfeff 100%);
  color: #1d4ed8;
  font-size: 26rpx;
  font-weight: 700;
}

.confidence-bar {
  height: 18rpx;
  margin-top: 20rpx;
  border-radius: 999rpx;
  background: #e2e8f0;
  overflow: hidden;
}

.confidence-fill {
  height: 100%;
  background: linear-gradient(90deg, #22c55e 0%, #2563eb 100%);
}

.confidence-footer {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: 16rpx;
  margin-top: 20rpx;
}

.confidence-chip {
  padding: 18rpx 20rpx;
  border-radius: 18rpx;
  background: #f8fafc;
}

.confidence-chip.soft {
  background: #eff6ff;
}

.confidence-chip-label {
  display: block;
  font-size: 20rpx;
  color: #64748b;
}

.confidence-chip-value {
  display: block;
  margin-top: 10rpx;
  font-size: 24rpx;
  font-weight: 700;
  color: #0f172a;
  word-break: break-all;
}

.action-grid {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: 18rpx;
  margin-top: 24rpx;
}

.action-btn {
  min-height: 124rpx;
  border-radius: 20rpx;
  display: flex;
  align-items: center;
  gap: 18rpx;
  padding: 24rpx;
  box-sizing: border-box;
}

.action-btn-wide {
  grid-column: 1 / -1;
}

.action-btn.primary {
  background: linear-gradient(135deg, #2563eb 0%, #1d4ed8 100%);
  color: #ffffff;
  box-shadow: 0 16rpx 28rpx rgba(37, 99, 235, 0.2);
}

.action-btn.secondary {
  background: #eff6ff;
  color: #1d4ed8;
}

.action-btn.accent {
  background: linear-gradient(135deg, #ecfeff 0%, #dcfce7 100%);
  color: #0f766e;
}

.action-emoji {
  font-size: 36rpx;
}

.action-copy {
  display: flex;
  flex-direction: column;
  gap: 8rpx;
  flex: 1;
}

.action-title {
  font-size: 28rpx;
  font-weight: 700;
}

.action-desc {
  font-size: 22rpx;
  line-height: 1.5;
  opacity: 0.82;
}

.tips-list {
  display: flex;
  flex-direction: column;
  gap: 18rpx;
  margin-top: 24rpx;
}

.tips-item {
  display: flex;
  gap: 18rpx;
  align-items: flex-start;
  padding: 22rpx;
  border-radius: 20rpx;
  background: linear-gradient(180deg, #f8fafc 0%, #ffffff 100%);
  border: 1rpx solid #e2e8f0;
}

.tips-index {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 56rpx;
  height: 56rpx;
  flex-shrink: 0;
  border-radius: 18rpx;
  background: #dbeafe;
  color: #1d4ed8;
  font-size: 22rpx;
  font-weight: 700;
}

.tips-text {
  display: block;
  font-size: 24rpx;
  line-height: 1.8;
  color: #475569;
}
</style>
