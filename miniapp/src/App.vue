<template>
  <view></view>
</template>

<script setup>
import { onLaunch, onShow, onHide } from '@dcloudio/uni-app'

// ====== 后端地址（与 api/index.js 保持一致）======
const BACKEND_HOST = '192.168.122.214:8081'
const WS_URL = `ws://${BACKEND_HOST}/ws/alarm`

// ====== 全局 WebSocket ======
let socketTask = null
let reconnectTimer = null
let heartbeatTimer = null

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

// 处理来自后端的消息
const handleWsMessage = (msg) => {
  switch (msg.type) {
    case 'AI_WAKE':
      // 盲杖按键唤醒：震动 + 跳转 AI 页 + 自动开始对话
      try { uni.vibrateLong() } catch (e) {}
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
    default:
      break
  }
}

// 暴露给其他页面（如登录后）调用
// eslint-disable-next-line
getApp.__$connectWs = connectWebSocket

onLaunch(() => {
  console.log('App Launch')
  const app = getApp()
  if (app) {
    app.globalData = app.globalData || {}
    app.globalData.aiWakeTrigger = false
    app.globalData.reconnectWs = connectWebSocket
  }
  // 尝试连接（如已登录）
  setTimeout(connectWebSocket, 500)
})

onShow(() => {
  console.log('App Show')
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
