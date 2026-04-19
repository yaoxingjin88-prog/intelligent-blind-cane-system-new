<template>
  <view class="dnd-settings-page">
    <scroll-view class="content" scroll-y>
      <!-- 说明文字 -->
      <view class="tips">
        <text class="tips-text">开启后，在设定时间段内将暂停除SOS和跌倒外的常规通知推送。</text>
      </view>

      <!-- 定时免打扰 -->
      <view class="card">
        <view class="setting-item">
          <text class="setting-label">定时免打扰</text>
          <view class="switch" :class="{ on: dndSettings.enabled }" @click="toggleDnd">
            <view class="switch-dot"></view>
          </view>
        </view>

        <view v-if="dndSettings.enabled" class="time-settings">
          <view class="time-item">
            <text class="time-label">开始时间</text>
            <picker mode="time" :value="dndSettings.startTime" @change="onStartTimeChange">
              <view class="time-picker">{{ dndSettings.startTime }}</view>
            </picker>
          </view>
          <view class="time-item">
            <text class="time-label">结束时间</text>
            <picker mode="time" :value="dndSettings.endTime" @change="onEndTimeChange">
              <view class="time-picker">{{ dndSettings.endTime }}</view>
            </picker>
          </view>
        </view>
      </view>

      <!-- 消息通知设置 -->
      <view class="card">
        <text class="card-title">消息通知设置</text>
        <view class="setting-item">
          <view class="setting-info">
            <text class="setting-label">报警通知</text>
            <text class="setting-desc">接收跌倒、越界等紧急报警</text>
          </view>
          <view class="switch" :class="{ on: notificationSettings.alarmEnabled }" @click="toggleAlarm">
            <view class="switch-dot"></view>
          </view>
        </view>
        <view class="setting-item">
          <view class="setting-info">
            <text class="setting-label">位置更新通知</text>
            <text class="setting-desc">接收设备位置更新推送</text>
          </view>
          <view class="switch" :class="{ on: notificationSettings.locationUpdateEnabled }" @click="toggleLocationUpdate">
            <view class="switch-dot"></view>
          </view>
        </view>
        <view class="setting-item">
          <view class="setting-info">
            <text class="setting-label">系统通知</text>
            <text class="setting-desc">接收系统消息和更新提醒</text>
          </view>
          <view class="switch" :class="{ on: notificationSettings.systemNotificationEnabled }" @click="toggleSystemNotification">
            <view class="switch-dot"></view>
          </view>
        </view>
      </view>

      <!-- 保存按钮 -->
      <button class="save-btn" @click="handleSave">保存设置</button>
    </scroll-view>
  </view>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useSettingsStore } from '@/store'

const settingsStore = useSettingsStore()

// 免打扰设置
const dndSettings = ref({
  enabled: false,
  startTime: '22:00',
  endTime: '07:00'
})

// 消息通知设置
const notificationSettings = ref({
  alarmEnabled: true,
  locationUpdateEnabled: true,
  systemNotificationEnabled: true
})

// 初始化
onMounted(() => {
  settingsStore.restoreFromStorage()
  if (settingsStore.dndSettings) {
    dndSettings.value = { ...dndSettings.value, ...settingsStore.dndSettings }
  }
  if (settingsStore.notificationSettings) {
    notificationSettings.value = { ...notificationSettings.value, ...settingsStore.notificationSettings }
  }
})

// 切换免打扰
const toggleDnd = () => {
  dndSettings.value.enabled = !dndSettings.value.enabled
}

// 切换报警通知
const toggleAlarm = () => {
  notificationSettings.value.alarmEnabled = !notificationSettings.value.alarmEnabled
}

// 切换位置更新通知
const toggleLocationUpdate = () => {
  notificationSettings.value.locationUpdateEnabled = !notificationSettings.value.locationUpdateEnabled
}

// 切换系统通知
const toggleSystemNotification = () => {
  notificationSettings.value.systemNotificationEnabled = !notificationSettings.value.systemNotificationEnabled
}

// 开始时间选择
const onStartTimeChange = (e) => {
  dndSettings.value.startTime = e.detail.value
}

// 结束时间选择
const onEndTimeChange = (e) => {
  dndSettings.value.endTime = e.detail.value
}

// 切换免打扰保存
const handleSave = () => {
  settingsStore.setDndSettings(dndSettings.value)
  settingsStore.setNotificationSettings(notificationSettings.value)
  
  uni.showToast({
    title: '保存成功',
    icon: 'success'
  })
  
  setTimeout(() => {
    uni.navigateBack()
  }, 1500)
}
</script>

<style lang="scss" scoped>
.dnd-settings-page {
  min-height: 100vh;
  background: #f7f8fa;
  box-sizing: border-box;
}

.content {
  padding: 32rpx;
  padding-bottom: 120rpx;
  width: 100%;
  box-sizing: border-box;
}

.tips {
  background: #fffbeb;
  border-radius: 16rpx;
  padding: 24rpx;
  margin-bottom: 32rpx;
  border: 1rpx solid #fcd34d;
  width: 100%;
  box-sizing: border-box;

  .tips-text {
    font-size: 24rpx;
    color: #92400e;
    line-height: 1.6;
  }
}

.card {
  background: #ffffff;
  border-radius: 24rpx;
  padding: 32rpx;
  margin-bottom: 24rpx;
  box-shadow: 0 4rpx 12rpx rgba(0, 0, 0, 0.06);
  border: 1rpx solid #f3f4f6;
  width: 100%;
  box-sizing: border-box;

  .card-title {
    font-size: 28rpx;
    font-weight: 600;
    color: #1f2937;
    display: block;
    margin-bottom: 32rpx;
  }

  .setting-item {
    display: flex;
    justify-content: space-between;
    align-items: center;
    padding: 24rpx 0;
    border-bottom: 1rpx solid #f3f4f6;

    &:last-child {
      border-bottom: none;
      padding-top: 24rpx;
      padding-bottom: 0;
    }

    .setting-info {
      flex: 1;

      .setting-label {
        display: block;
        font-size: 28rpx;
        color: #1f2937;
        margin-bottom: 8rpx;
      }

      .setting-desc {
        display: block;
        font-size: 24rpx;
        color: #9ca3af;
      }
    }

    .setting-label {
      font-size: 28rpx;
      color: #1f2937;
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

  .time-settings {
    margin-top: 32rpx;
    padding-top: 32rpx;
    border-top: 1rpx solid #f3f4f6;

    .time-item {
      display: flex;
      justify-content: space-between;
      align-items: center;
      padding: 16rpx 0;

      .time-label {
        font-size: 28rpx;
        color: #1f2937;
      }

      .time-picker {
        padding: 16rpx 24rpx;
        background: #f9fafb;
        border-radius: 12rpx;
        font-size: 28rpx;
        color: #1f2937;
        border: 1rpx solid #e5e7eb;
      }
    }
  }
}

.save-btn {
  width: 100%;
  height: 88rpx;
  background: linear-gradient(135deg, #07c160 0%, #05a050 100%);
  color: #ffffff;
  border-radius: 24rpx;
  font-size: 32rpx;
  font-weight: 600;
  border: none;
  margin-top: 24rpx;
}
</style>
