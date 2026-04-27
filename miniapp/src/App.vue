<template>
  <view></view>
</template>

<script setup>
import { onLaunch, onShow, onHide } from '@dcloudio/uni-app'
import { WS_URL, BASE_URL } from '@/config/env'

// ====== 全局 WebSocket ======
let socketTask = null
let reconnectTimer = null
let heartbeatTimer = null
let crossingAudioContext = null

const stopCrossingAudio = () => {
  if (crossingAudioContext) {
    try { crossingAudioContext.stop() } catch (e) {}
    try { crossingAudioContext.destroy() } catch (e) {}
    crossingAudioContext = null
  }
}

const playCrossingPrompt = async (text, options = {}) => {
  if (!text) {
    return
  }
  const waitForCompletion = !!options.waitForCompletion
  try {
    const token = uni.getStorageSync('token')
    const res = await new Promise((resolve, reject) => {
      uni.request({
        url: BASE_URL + '/ai/tts',
        method: 'POST',
        data: { text },
        responseType: 'arraybuffer',
        header: {
          'Content-Type': 'application/json',
          'Authorization': token ? `Bearer ${token}` : ''
        },
        success: resolve,
        fail: reject
      })
    })
    if (res.statusCode !== 200 || !res.data) {
      throw new Error(`TTS 请求失败: ${res.statusCode || 'unknown'}`)
    }
    // #ifdef MP-WEIXIN
    await new Promise((resolve) => {
      const fs = wx.getFileSystemManager()
      const filePath = `${wx.env.USER_DATA_PATH}/crossing_ws_${Date.now()}.mp3`
      fs.writeFileSync(filePath, res.data, 'binary')
      stopCrossingAudio()
      crossingAudioContext = uni.createInnerAudioContext()
      crossingAudioContext.obeyMuteSwitch = false
      crossingAudioContext.src = filePath
      let resolved = false
      const finish = () => {
        if (resolved) return
        resolved = true
        stopCrossingAudio()
        resolve()
      }
      crossingAudioContext.onEnded(() => { finish() })
      crossingAudioContext.onError(() => { finish() })
      crossingAudioContext.onCanplay(() => {
        setTimeout(() => {
          try {
            crossingAudioContext && crossingAudioContext.play()
            if (!waitForCompletion) {
              resolve()
            }
          } catch (e) {
            finish()
          }
        }, 80)
      })
      if (!waitForCompletion) {
        setTimeout(() => {
          if (!resolved) {
            resolved = true
            resolve()
          }
        }, 120)
      }
    })
    // #endif
  } catch (e) {
    console.warn('[WS] 路口辅助语音播报失败', e)
  }
}

const connectWebSocket = () => {
  const token = uni.getStorageSync('token')
  const device = uni.getStorageSync('currentDevice')
  if (!token || !device || !device.deviceId) {
    console.log('[WS] 未登录或未绑定设备，跳过连接')
    return
  }
  // 已连接则跳过
  if (socketTask) {
    try { socketTask.close() } catch (e) {}
    socketTask = null
  }

  const url = `${WS_URL}?deviceId=${encodeURIComponent(device.deviceId)}`
  // WS_URL 例如 ws://192.168.x.x:8081/ws/alarm
  console.log('[WS] 连接:', url)

  socketTask = uni.connectSocket({
    url,
    complete: () => {}
  })

  socketTask.onOpen(() => {
    console.log('[WS] 已连接')
    // 启动心跳
    clearInterval(heartbeatTimer)
    heartbeatTimer = setInterval(() => {
      if (socketTask) {
        try {
          socketTask.send({ data: JSON.stringify({ type: 'PING' }) })
        } catch (e) {}
      }
    }, 25000)
  })

  socketTask.onMessage((res) => {
    try {
      const msg = JSON.parse(res.data)
      console.log('[WS] 收到:', msg.type)
      handleWsMessage(msg)
    } catch (e) {
      console.warn('[WS] 解析失败', res.data)
    }
  })

  socketTask.onError((err) => {
    console.error('[WS] 错误', err)
  })

  socketTask.onClose(() => {
    console.log('[WS] 已断开，5 秒后重连')
    socketTask = null
    clearInterval(heartbeatTimer)
    clearTimeout(reconnectTimer)
    reconnectTimer = setTimeout(connectWebSocket, 5000)
  })
}

const disconnectWebSocket = () => {
  clearInterval(heartbeatTimer)
  clearTimeout(reconnectTimer)
  if (socketTask) {
    try { socketTask.close() } catch (e) {}
    socketTask = null
  }
}

const updateGuardianCache = (key, value) => {
  const app = getApp()
  if (app) {
    app.globalData = app.globalData || {}
    app.globalData[key] = value
  }
  uni.setStorageSync(key, value)
}

const getCurrentRoute = () => {
  try {
    const pages = getCurrentPages()
    const currentPage = pages && pages.length ? pages[pages.length - 1] : null
    return currentPage?.route || ''
  } catch (e) {
    return ''
  }
}

const setSosAnnouncementState = (pending, defer) => {
  const app = getApp()
  if (app) {
    app.globalData = app.globalData || {}
    app.globalData.pendingGuardianSosAnnouncement = pending
    app.globalData.deferGuardianSosAnnouncement = defer
  }
  uni.setStorageSync('pendingGuardianSosAnnouncement', pending)
  uni.setStorageSync('deferGuardianSosAnnouncement', defer)
}

// 处理来自后端的消息
const handleWsMessage = (msg) => {
  switch (msg.type) {
    case 'AI_WAKE':
      // 盲杖按键唤醒：震动 + 跳转 AI 页 + 自动开始对话
      try { uni.vibrateLong({ fail: () => {} }) } catch (e) {}
      const app = getApp()
      if (app) {
        app.globalData = app.globalData || {}
        app.globalData.aiWakeTrigger = true
      }
      // 跳转到 AI 聊天页（reLaunch 关闭所有页面）
      uni.reLaunch({ url: '/pages/ai-chat/ai-chat' })
      break
    case 'ALARM':
      // 可选：收到报警也提示一下
      uni.showToast({ title: msg.message || '收到报警', icon: 'none' })
      break
    case 'CROSSING_ASSIST': {
      const app = getApp()
      if (app) {
        app.globalData = app.globalData || {}
        app.globalData.latestCrossingAssist = msg.crossingAssist || null
      }
      if (msg.crossingAssist) {
        uni.setStorageSync('latestCrossingAssist', msg.crossingAssist)
        uni.$emit('crossingAssistUpdate', msg.crossingAssist)
      }
      const recommendation = msg.crossingAssist?.recommendation
      try {
        if (recommendation === 'WAIT') {
          uni.vibrateLong({ fail: () => {} })
        } else {
          uni.vibrateShort({ fail: () => {} })
        }
      } catch (e) {}
      uni.showToast({ title: msg.message || '收到路口辅助提醒', icon: 'none', duration: 2000 })
      playCrossingPrompt(msg.message)
      break
    }
    case 'GUARDIAN_ALERT':
      updateGuardianCache('latestGuardianAlert', msg)
      try {
        if (msg.level === 'danger') {
          uni.vibrateLong({ fail: () => {} })
        } else {
          uni.vibrateShort({ fail: () => {} })
        }
      } catch (e) {}
      if (msg.alertType === 'SOS') {
        const app = getApp()
        const currentRoute = getCurrentRoute()
        const deferGuardianSosAnnouncement = !!(app?.globalData?.deferGuardianSosAnnouncement || uni.getStorageSync('deferGuardianSosAnnouncement'))
        const shouldDefer = deferGuardianSosAnnouncement || currentRoute.includes('pages/user-terminal/user-terminal') || currentRoute.includes('pages/ai-chat/ai-chat')
        if (shouldDefer) {
          setSosAnnouncementState(true, true)
          break
        }
      }
      uni.showToast({ title: msg.alertType === 'SOS' ? '收到 SOS 求助' : (msg.message || '收到守护提醒'), icon: 'none', duration: 2200 })
      playCrossingPrompt(msg.message)
      break
    case 'GUARDIAN_COMFORT':
      updateGuardianCache('latestGuardianComfort', msg)
      uni.showToast({ title: '收到家属安抚', icon: 'none', duration: 2200 })
      playCrossingPrompt(msg.content || msg.message)
      break
    case 'GUARDIAN_DESTINATION':
      updateGuardianCache('latestGuardianDestination', msg)
      try { uni.vibrateShort({ fail: () => {} }) } catch (e) {}
      uni.showToast({ title: '收到家属目的地', icon: 'none', duration: 2200 })
      playCrossingPrompt(msg.message || `新的目的地：${msg.destination || ''}`)
      break
    default:
      break
  }
}

// 暴露给其他页面（如登录后）调用
// eslint-disable-next-line
getApp.__$connectWs = connectWebSocket
// eslint-disable-next-line
getApp.__$playCrossingPrompt = playCrossingPrompt

onLaunch(() => {
  console.log('App Launch')
  const app = getApp()
  if (app) {
    app.globalData = app.globalData || {}
    app.globalData.aiWakeTrigger = false
    app.globalData.latestCrossingAssist = uni.getStorageSync('latestCrossingAssist') || null
    app.globalData.latestGuardianAlert = uni.getStorageSync('latestGuardianAlert') || null
    app.globalData.latestGuardianComfort = uni.getStorageSync('latestGuardianComfort') || null
    app.globalData.latestGuardianDestination = uni.getStorageSync('latestGuardianDestination') || null
    app.globalData.pendingGuardianSosAnnouncement = !!uni.getStorageSync('pendingGuardianSosAnnouncement')
    app.globalData.deferGuardianSosAnnouncement = !!uni.getStorageSync('deferGuardianSosAnnouncement')
    app.globalData.playCrossingPrompt = playCrossingPrompt
    app.globalData.reconnectWs = connectWebSocket
  }
  // eslint-disable-next-line
  getApp.__$playCrossingPrompt = playCrossingPrompt
  // 尝试连接（如已登录）
  setTimeout(connectWebSocket, 500)
})

onShow(() => {
  console.log('App Show')
  // eslint-disable-next-line
  getApp.__$playCrossingPrompt = playCrossingPrompt
  // 前台回来后，确保 WS 在线
  if (!socketTask) connectWebSocket()
})

onHide(() => {
  console.log('App Hide')
  // 不主动断开，让系统决定；避免后台短暂切走即断
})
</script>

<style lang="scss">
@import 'uview-plus/theme.scss';
@import 'uview-plus/index.scss';
@import './static/font_5163035_73o8ucu6bq/iconfont.css';

page {
  background-color: #f7f8fa;
  font-family: -apple-system, BlinkMacSystemFont, 'Helvetica Neue', Helvetica, 'PingFang SC', 'Hiragino Sans GB', 'Microsoft Yahei', sans-serif;
}

/* 全局样式变量 */
:root {
  --primary-color: #07c160;
  --danger-color: #ee0a24;
  --warning-color: #ff976a;
  --info-color: #1989fa;
  --success-color: #07c160;
  --text-primary: #1f2937;
  --text-secondary: #6b7280;
  --text-tertiary: #9ca3af;
  --bg-primary: #ffffff;
  --bg-secondary: #f3f4f6;
  --border-color: #e5e7eb;
}

/* 通用类 */
.container {
  padding: 0 32rpx;
}

.card {
  background-color: #ffffff;
  border-radius: 24rpx;
  padding: 32rpx;
  margin-bottom: 24rpx;
  box-shadow: 0 4rpx 12rpx rgba(0, 0, 0, 0.06);
  border: 1rpx solid #f3f4f6;
}

.btn-primary {
  background-color: var(--primary-color);
  color: #ffffff;
  border-radius: 24rpx;
  padding: 24rpx 48rpx;
  font-size: 28rpx;
  font-weight: 600;
}

.text-primary {
  color: var(--primary-color);
}

.text-danger {
  color: var(--danger-color);
}

.text-warning {
  color: var(--warning-color);
}

.text-success {
  color: var(--success-color);
}

.bg-primary {
  background-color: var(--primary-color);
}

.bg-danger {
  background-color: var(--danger-color);
}

.bg-warning {
  background-color: var(--warning-color);
}

.bg-success {
  background-color: var(--success-color);
}

/* 隐藏滚动条 */
.hide-scrollbar::-webkit-scrollbar {
  display: none;
}

.hide-scrollbar {
  -ms-overflow-style: none;
  scrollbar-width: none;
}

/* 渐变背景 */
.gradient-bg {
  background: linear-gradient(135deg, #e6f8ee 0%, #f7f8fa 100%);
}

/* 淡入动画 */
.fade-in {
  animation: fadeIn 0.3s ease-in-out;
}

@keyframes fadeIn {
  from {
    opacity: 0;
    transform: translateY(20rpx);
  }
  to {
    opacity: 1;
    transform: translateY(0);
  }
}
</style>
