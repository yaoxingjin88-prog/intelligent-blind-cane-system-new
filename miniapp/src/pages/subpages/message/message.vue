<template>
  <view class="message-page">
    <scroll-view class="content" scroll-y>
      <!-- 消息列表 -->
      <view v-if="messages.length === 0" class="empty-state">
        <text class="empty-icon">📭</text>
        <text class="empty-text">暂无消息</text>
      </view>

      <view v-for="message in messages" :key="message.id" class="message-card" :class="{ unread: !message.read }" @click="handleMessageClick(message)">
        <view class="message-icon" :class="message.type">
          <text>{{ getMessageIcon(message.type) }}</text>
        </view>
        <view class="message-content">
          <view class="message-header">
            <text class="message-title">{{ message.title }}</text>
            <text class="message-time">{{ formatRelativeTime(message.createTime) }}</text>
          </view>
          <text class="message-desc">{{ message.content }}</text>
        </view>
        <view class="message-indicator" v-if="!message.read"></view>
      </view>
    </scroll-view>
  </view>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { getAlarmList, handleAlarm } from '@/api/alarm'
import { formatRelativeTime } from '@/utils'

const messages = ref([])

// 报警类型映射
const alarmTypeMap = {
  '摔倒': { title: '跌倒报警', icon: '🚨' },
  '跌倒': { title: '跌倒报警', icon: '🚨' },
  '障碍物': { title: '障碍物报警', icon: '⚠️' },
  '电池': { title: '低电量报警', icon: '🔋' },
  '低电量': { title: '低电量报警', icon: '🔋' },
  '电子围栏越界报警': { title: '越界提醒', icon: '📍' },
  '越界': { title: '越界提醒', icon: '�' },
  '静止': { title: '长时间静止', icon: '⏰' },
  'fall': { title: '跌倒报警', icon: '🚨' },
  'out_of_bounds': { title: '越界提醒', icon: '�' },
  'low_battery': { title: '低电量报警', icon: '🔋' },
  'stationary': { title: '长时间静止', icon: '⏰' },
  'obstacle': { title: '障碍物报警', icon: '⚠️' },
  'sos': { title: 'SOS求助', icon: '🔔' }
}

// 获取消息图标
const getMessageIcon = (type) => {
  const info = alarmTypeMap[type]
  return info ? info.icon : '📢'
}

// 获取消息标题
const getMessageTitle = (type) => {
  const info = alarmTypeMap[type]
  return info ? info.title : (type || '系统消息')
}

// 加载报警列表作为消息
const loadMessages = async () => {
  try {
    const res = await getAlarmList()
    const alarmData = res.data || []
    
    // 将报警数据转换为消息格式，只保留未读的
    messages.value = alarmData
      .filter(alarm => alarm.status === '0' || alarm.status === 'pending')
      .map(alarm => ({
        id: alarm.id,
        title: getMessageTitle(alarm.alarmType),
        content: alarm.description || `报警类型: ${alarm.alarmType}`,
        type: alarm.alarmType,
        read: false,
        createTime: alarm.alarmTime,
        alarmData: alarm
      }))
  } catch (error) {
    console.error('加载消息列表失败', error)
    messages.value = []
  }
}

// 点击消息 - 跳转到报警详情页面
const handleMessageClick = (message) => {
  // 先标记为已读
  if (!message.read && message.alarmData) {
    handleAlarm(message.id, { status: '1' }).then(() => {
      message.read = true
    }).catch(() => {})
  }
  
  // 将报警数据存储到全局，供详情页使用
  uni.setStorageSync('currentAlarm', JSON.stringify(message.alarmData))
  
  // 跳转到报警详情页
  uni.navigateTo({
    url: `/pages/subpages/alarm-detail/alarm-detail?id=${message.id}`
  })
}

onMounted(() => {
  loadMessages()
})
</script>

<style lang="scss" scoped>
.message-page {
  min-height: 100vh;
  background: #f7f8fa;
  box-sizing: border-box;
}

.content {
  padding: 32rpx;
  width: 100%;
  box-sizing: border-box;
}

.empty-state {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 120rpx 0;

  .empty-icon {
    font-size: 120rpx;
    margin-bottom: 24rpx;
    opacity: 0.5;
  }

  .empty-text {
    font-size: 28rpx;
    color: #9ca3af;
  }
}

.message-card {
  background: #ffffff;
  border-radius: 24rpx;
  padding: 32rpx;
  margin-bottom: 24rpx;
  box-shadow: 0 4rpx 12rpx rgba(0, 0, 0, 0.06);
  border: 1rpx solid #f3f4f6;
  display: flex;
  align-items: flex-start;
  gap: 24rpx;
  position: relative;
  width: 100%;
  box-sizing: border-box;

  &.unread {
    background: #fef2f2;
    border-color: #fecaca;
  }

  .message-icon {
    width: 80rpx;
    height: 80rpx;
    border-radius: 50%;
    display: flex;
    align-items: center;
    justify-content: center;
    font-size: 32rpx;
    flex-shrink: 0;

    &.system {
      background: #dbeafe;
    }

    &.alarm {
      background: #fee2e2;
    }

    &.device {
      background: #d1fae5;
    }

    &.notification {
      background: #fef3c7;
    }
  }

  .message-content {
    flex: 1;
    min-width: 0;

    .message-header {
      display: flex;
      justify-content: space-between;
      align-items: flex-start;
      margin-bottom: 12rpx;

      .message-title {
        font-size: 28rpx;
        font-weight: 600;
        color: #1f2937;
        flex: 1;
      }

      .message-time {
        font-size: 24rpx;
        color: #9ca3af;
        margin-left: 16rpx;
        flex-shrink: 0;
      }
    }

    .message-desc {
      font-size: 26rpx;
      color: #6b7280;
      line-height: 1.5;
      display: -webkit-box;
      -webkit-box-orient: vertical;
      -webkit-line-clamp: 2;
      overflow: hidden;
    }
  }

  .message-indicator {
    position: absolute;
    top: 24rpx;
    right: 24rpx;
    width: 16rpx;
    height: 16rpx;
    background: #ee0a24;
    border-radius: 50%;
  }
}
</style>
