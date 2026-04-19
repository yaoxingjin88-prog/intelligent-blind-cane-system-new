<template>
  <view class="device-page">
    <scroll-view class="content" scroll-y>
      <!-- 设备列表 -->
      <view v-if="deviceList.length > 0" class="device-list">
        <view 
          v-for="device in deviceList" 
          :key="device.id" 
          class="device-card"
          :class="{ active: currentDevice && currentDevice.id === device.id }"
          @click="selectDevice(device)"
        >
          <view class="device-icon" :class="device.status">
            <text>📱</text>
          </view>
          <view class="device-info">
            <text class="device-name">{{ device.deviceName }}</text>
            <text class="device-id">设备ID: {{ device.deviceId }}</text>
            <view class="device-meta">
              <text class="device-status" :class="device.status">
                {{ device.status === 'online' ? '在线' : '离线' }}
              </text>
              <text class="device-battery">电量 {{ device.batteryLevel }}%</text>
            </view>
          </view>
          <view class="device-actions">
            <text class="action-btn" @click.stop="unbindDevice(device)">解绑</text>
          </view>
        </view>
      </view>

      <!-- 空状态 -->
      <view v-else class="empty-state">
        <text class="empty-icon">📱</text>
        <text class="empty-text">暂无绑定设备</text>
        <button class="bind-btn" @click="showBindModal = true">绑定设备</button>
      </view>
    </scroll-view>

    <!-- 绑定设备弹窗 -->
    <view v-if="showBindModal && !showManualBind" class="popup-mask" @click="showBindModal = false">
      <view class="bind-modal" @click.stop>
        <view class="modal-header">
          <text class="title">绑定设备</text>
          <text class="close" @click="showBindModal = false">✕</text>
        </view>
        <view class="modal-body">
          <view class="bind-methods">
            <view class="bind-method" @click="scanQRCode">
              <text class="method-icon">📷</text>
              <text class="method-title">扫描二维码</text>
              <text class="method-desc">扫描设备二维码快速绑定</text>
            </view>
            <view class="bind-method" @click="manualBind">
              <text class="method-icon">⌨️</text>
              <text class="method-title">手动输入</text>
              <text class="method-desc">输入设备ID进行绑定</text>
            </view>
          </view>
        </view>
      </view>
    </view>

    <!-- 手动绑定弹窗 -->
    <view v-if="showManualBind" class="popup-mask" @click="showManualBind = false">
      <view class="manual-bind-modal" @click.stop>
        <view class="modal-header">
          <text class="title">手动输入设备ID</text>
          <text class="close" @click="showManualBind = false">✕</text>
        </view>
        <view class="modal-body">
          <view class="form-item">
            <text class="label">设备ID</text>
            <input v-model="deviceIdInput" class="input" placeholder="请输入设备ID" />
          </view>
          <view class="form-item">
            <text class="label">设备名称</text>
            <input v-model="deviceNameInput" class="input" placeholder="请输入设备名称" />
          </view>
        </view>
        <view class="modal-footer">
          <button class="cancel-btn" @click="showManualBind = false">取消</button>
          <button class="confirm-btn" @click="confirmBind">确认绑定</button>
        </view>
      </view>
    </view>
  </view>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { useDeviceStore, useUserStore } from '@/store'
import { bindDevice, unbindDevice as unbindDeviceApi } from '@/api/device'

const deviceStore = useDeviceStore()
const userStore = useUserStore()

// 弹窗状态
const showBindModal = ref(false)
const showManualBind = ref(false)

// 输入数据
const deviceIdInput = ref('')
const deviceNameInput = ref('')

// 设备列表
const deviceList = computed(() => deviceStore.deviceList)

// 当前设备
const currentDevice = computed(() => deviceStore.currentDevice)

// 初始化
onMounted(() => {
  deviceStore.restoreFromStorage()
  if (userStore.isLoggedIn) {
    deviceStore.fetchDeviceList()
  }
})

// 选择设备
const selectDevice = (device) => {
  deviceStore.setCurrentDevice(device)
  uni.showToast({
    title: '已切换设备',
    icon: 'success'
  })
}

// 扫描二维码
const scanQRCode = () => {
  uni.scanCode({
    success: (res) => {
      // 假设二维码内容是设备ID
      deviceIdInput.value = res.result
      showBindModal.value = false
      showManualBind.value = true
    },
    fail: () => {
      uni.showToast({
        title: '扫描失败',
        icon: 'none'
      })
    }
  })
}

// 手动绑定
const manualBind = () => {
  showBindModal.value = false
  showManualBind.value = true
}

// 确认绑定
const confirmBind = async () => {
  if (!deviceIdInput.value) {
    uni.showToast({
      title: '请输入设备ID',
      icon: 'none'
    })
    return
  }
  if (!deviceNameInput.value) {
    uni.showToast({
      title: '请输入设备名称',
      icon: 'none'
    })
    return
  }

  try {
    await bindDevice({
      deviceId: deviceIdInput.value,
      deviceName: deviceNameInput.value
    })

    uni.showToast({
      title: '绑定成功',
      icon: 'success'
    })

    showManualBind.value = false
    
    // 清空输入
    deviceIdInput.value = ''
    deviceNameInput.value = ''
    
    // 刷新设备列表
    deviceStore.fetchDeviceList()
  } catch (error) {
    console.error('绑定设备失败', error)
  }
}

// 解绑设备
const unbindDevice = (device) => {
  uni.showModal({
    title: '提示',
    content: `确定要解绑设备"${device.deviceName}"吗？`,
    success: async (res) => {
      if (res.confirm) {
        try {
          await unbindDeviceApi(device.id)
          
          uni.showToast({
            title: '解绑成功',
            icon: 'success'
          })
          
          // 如果解绑的是当前设备，清除当前设备
          if (currentDevice.value && currentDevice.value.id === device.id) {
            deviceStore.currentDevice = null
          }
          
          // 刷新设备列表
          deviceStore.fetchDeviceList()
        } catch (error) {
          console.error('解绑设备失败', error)
        }
      }
    }
  })
}

</script>

<style lang="scss" scoped>
.device-page {
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

.device-list {
  display: flex;
  flex-direction: column;
  gap: 24rpx;

  .device-card {
    background: #ffffff;
    border-radius: 24rpx;
    padding: 32rpx;
    display: flex;
    align-items: center;
    gap: 24rpx;
    box-shadow: 0 4rpx 12rpx rgba(0, 0, 0, 0.06);
    border: 1rpx solid #f3f4f6;
    transition: all 0.3s;
    width: 100%;
    box-sizing: border-box;

    &.active {
      border-color: #07c160;
      background: #f0fdf4;
    }

    .device-icon {
      width: 96rpx;
      height: 96rpx;
      border-radius: 50%;
      display: flex;
      align-items: center;
      justify-content: center;
      font-size: 40rpx;

      &.online {
        background: #d1fae5;
      }

      &.offline {
        background: #fee2e2;
      }
    }

    .device-info {
      flex: 1;

      .device-name {
        font-size: 28rpx;
        font-weight: 600;
        color: #1f2937;
        display: block;
        margin-bottom: 8rpx;
      }

      .device-id {
        font-size: 24rpx;
        color: #9ca3af;
        display: block;
        margin-bottom: 12rpx;
      }

      .device-meta {
        display: flex;
        gap: 16rpx;

        .device-status {
          font-size: 24rpx;
          padding: 4rpx 12rpx;
          border-radius: 12rpx;

          &.online {
            background: #d1fae5;
            color: #059669;
          }

          &.offline {
            background: #fee2e2;
            color: #dc2626;
          }
        }

        .device-battery {
          font-size: 24rpx;
          color: #6b7280;
        }
      }
    }

    .device-actions {
      .action-btn {
        font-size: 24rpx;
        color: #9ca3af;
        padding: 16rpx 24rpx;
        border: 1rpx solid #e5e7eb;
        border-radius: 16rpx;
      }
    }
  }
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
    margin-bottom: 48rpx;
  }

  .bind-btn {
    background: #07c160;
    color: #ffffff;
    padding: 24rpx 64rpx;
    border-radius: 32rpx;
    font-size: 28rpx;
    font-weight: 600;
    border: none;
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
  align-items: center;
  justify-content: center;
}

.bind-modal,
.manual-bind-modal {
  width: 600rpx;
  max-width: 90vw;
  padding: 48rpx;
  box-sizing: border-box;
  background: #ffffff;
  border-radius: 24rpx;

  .modal-header {
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

  .modal-body {
    margin-bottom: 48rpx;

    .bind-methods {
      display: flex;
      flex-direction: column;
      gap: 24rpx;

      .bind-method {
        padding: 32rpx;
        background: #f9fafb;
        border-radius: 16rpx;
        text-align: center;
        border: 1rpx solid #e5e7eb;

        .method-icon {
          font-size: 48rpx;
          display: block;
          margin-bottom: 16rpx;
        }

        .method-title {
          font-size: 28rpx;
          font-weight: 600;
          color: #1f2937;
          display: block;
          margin-bottom: 8rpx;
        }

        .method-desc {
          font-size: 24rpx;
          color: #9ca3af;
        }
      }
    }

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
    }
  }

  .modal-footer {
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
