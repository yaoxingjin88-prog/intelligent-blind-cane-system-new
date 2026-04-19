<template>
  <view class="system-settings-page">
    <scroll-view class="content" scroll-y>
      <!-- 设置列表 -->
      <view class="card">
        <view v-for="(item, index) in menuItems" :key="index" class="menu-item" :class="{ 'menu-item-last': index === menuItems.length - 1 }" @click="item.handler">
          <text class="menu-title">{{ item.title }}</text>
          <text v-if="item.desc" class="menu-desc">{{ item.desc }}</text>
          <text v-else class="menu-arrow">›</text>
        </view>
      </view>

      <!-- 退出登录 -->
      <button class="logout-btn" @click="handleLogout">退出登录</button>

      <!-- 版权信息 -->
      <view class="copyright">
        <text class="copyright-text">Copyright © 2024 智能盲杖团队</text>
      </view>
    </scroll-view>
  </view>
</template>

<script setup>
import { ref } from 'vue'
import { useUserStore, useDeviceStore } from '@/store'
import { logout } from '@/api/auth'

const userStore = useUserStore()
const deviceStore = useDeviceStore()

// 导航到子页面
const navigateTo = (page) => {
  uni.navigateTo({
    url: `/pages/subpages/${page}/${page}`
  })
}

// 意见反馈
const onFeedback = () => {
  navigateTo('feedback')
}

// 菜单列表（按企业级规范排序：关于产品 -> 功能相关 -> 反馈渠道 -> 法律合规 -> 数据管理）
const menuItems = ref([
  { title: '关于智能盲杖系统', desc: '版本 1.0.0', handler: () => navigateTo('about-us') },
  { title: '意见反馈', handler: onFeedback },
  { title: '用户协议与隐私政策', handler: () => navigateTo('privacy-policy') },
  { title: '清除缓存', desc: '12.5 MB', handler: () => {} }
])

// 退出登录
const handleLogout = () => {
  uni.showModal({
    title: '提示',
    content: '确定要退出登录吗？',
    success: async (res) => {
      if (res.confirm) {
        try {
          await logout()
        } catch (error) {
          console.error('退出登录失败', error)
        }
        
        userStore.clearUserInfo()
        deviceStore.currentDevice = null
        
        uni.showToast({
          title: '已退出登录',
          icon: 'success'
        })
        
        setTimeout(() => {
          uni.reLaunch({
            url: '/pages/login/login'
          })
        }, 1500)
      }
    }
  })
}
</script>

<style lang="scss" scoped>
.system-settings-page {
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

.card {
  background: #ffffff;
  border-radius: 24rpx;
  margin-bottom: 24rpx;
  box-shadow: 0 4rpx 12rpx rgba(0, 0, 0, 0.06);
  border: 1rpx solid #f3f4f6;
  overflow: hidden;
  width: 100%;
  box-sizing: border-box;

  .menu-item {
    display: flex;
    justify-content: space-between;
    align-items: center;
    padding: 32rpx;
    border-bottom: 1rpx solid #f3f4f6;
    background: #ffffff;
    transition: background 0.3s;

    &:active {
      background: #f9fafb;
    }

    &.menu-item-last {
      border-bottom: none;
    }

    .menu-title {
      font-size: 28rpx;
      color: #1f2937;
    }

    .menu-desc {
      font-size: 24rpx;
      color: #9ca3af;
    }

    .menu-arrow {
      font-size: 48rpx;
      color: #d1d5db;
    }
  }
}

.logout-btn {
  width: 100%;
  height: 88rpx;
  background: #ffffff;
  color: #ee0a24;
  border-radius: 24rpx;
  font-size: 28rpx;
  font-weight: 600;
  border: 1rpx solid #fee2e2;
  margin-top: 24rpx;
  box-shadow: 0 4rpx 12rpx rgba(0, 0, 0, 0.06);
  box-sizing: border-box;
}

.copyright {
  text-align: center;
  margin-top: 48rpx;
  width: 100%;
  box-sizing: border-box;

  .copyright-text {
    font-size: 24rpx;
    color: #9ca3af;
  }
}
</style>
